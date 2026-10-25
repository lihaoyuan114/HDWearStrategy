package main

import (
	"bufio"
	"crypto/rand"
	"encoding/hex"
	"flag"
	"fmt"
	"log"
	"net"
	"os"
	"os/signal"
	"strings"
	"sync"
	"time"
)

const version = "1.0.0"

type pendingPair struct {
	code string
	name string
	addr *net.UDPAddr
	seen time.Time
}

type bridgeServer struct {
	cfg     Config
	kb      Keyboard
	udp     *net.UDPConn
	tcp     *net.TCPListener
	dry     bool
	verbose bool

	mu       sync.Mutex
	pending  map[string]*pendingPair
	tokens   map[string]string
	active   net.Conn
	menuDown bool
	latest   *pendingPair
}

func main() {
	configPath := flag.String("config", "server-config.json", "path to JSON config")
	udpPort := flag.Int("udp", 0, "override UDP pairing port")
	tcpPort := flag.Int("tcp", 0, "override TCP control port")
	autoPair := flag.Bool("auto-pair", false, "accept pairing requests without console confirmation")
	dryRun := flag.Bool("dry-run", false, "log keys instead of injecting them")
	verbose := flag.Bool("verbose", false, "log every received action")
	holdMS := flag.Int("key-hold-ms", 0, "override key tap hold time in milliseconds")
	up := flag.String("up", "", "override Up binding")
	down := flag.String("down", "", "override Down binding")
	left := flag.String("left", "", "override Left binding")
	right := flag.String("right", "", "override Right binding")
	menu := flag.String("menu", "", "override menu (open stratagem menu) binding")
	listKeys := flag.Bool("list-keys", false, "list accepted key names and exit")
	showVersion := flag.Bool("version", false, "print version and exit")
	flag.Parse()

	if *showVersion {
		fmt.Println("stratagem-server", version)
		return
	}
	if *listKeys {
		for k := range knownKeys {
			fmt.Print(k, " ")
		}
		fmt.Println()
		return
	}

	cfg, err := loadConfig(*configPath)
	if err != nil {
		log.Fatalf("config: %v", err)
	}
	if *udpPort != 0 {
		cfg.UDPPort = *udpPort
	}
	if *tcpPort != 0 {
		cfg.TCPPort = *tcpPort
	}
	if *holdMS != 0 {
		cfg.KeyHoldMS = *holdMS
	}
	if *autoPair {
		cfg.AutoPair = true
	}
	overrides := map[string]string{"up": *up, "down": *down, "left": *left, "right": *right, "menu": *menu}
	for action, key := range overrides {
		if strings.TrimSpace(key) != "" {
			cfg.Bindings[action] = normalizeKey(key)
		}
	}
	for _, action := range []string{"up", "down", "left", "right", "menu"} {
		key := cfg.Bindings[action]
		if !validKey(key) {
			log.Fatalf("binding %s: unknown key %q", action, key)
		}
	}

	kb, err := newKeyboardForOS(*dryRun, time.Duration(cfg.KeyHoldMS)*time.Millisecond)
	if err != nil {
		log.Fatalf("keyboard: %v", err)
	}
	defer kb.Close()

	s := &bridgeServer{
		cfg: cfg, kb: kb, dry: *dryRun, verbose: *verbose,
		pending: make(map[string]*pendingPair),
		tokens:  make(map[string]string),
	}
	if err := s.listen(); err != nil {
		log.Fatalf("listen: %v", err)
	}
	defer s.close()

	s.printBanner()
	go s.udpLoop()
	go s.tcpLoop()
	go s.consoleLoop()

	sig := make(chan os.Signal, 1)
	signal.Notify(sig, os.Interrupt)
	<-sig
	fmt.Println("\n[bridge] shutting down")
}

func (s *bridgeServer) listen() error {
	udpAddr := &net.UDPAddr{IP: net.IPv4zero, Port: s.cfg.UDPPort}
	udp, err := net.ListenUDP("udp4", udpAddr)
	if err != nil {
		return fmt.Errorf("UDP %d: %w", s.cfg.UDPPort, err)
	}
	tcpAddr, err := net.ResolveTCPAddr("tcp4", fmt.Sprintf("0.0.0.0:%d", s.cfg.TCPPort))
	if err != nil {
		udp.Close()
		return err
	}
	tcp, err := net.ListenTCP("tcp4", tcpAddr)
	if err != nil {
		udp.Close()
		return fmt.Errorf("TCP %d: %w", s.cfg.TCPPort, err)
	}
	s.udp = udp
	s.tcp = tcp
	return nil
}

func (s *bridgeServer) close() {
	if s.udp != nil {
		s.udp.Close()
	}
	if s.tcp != nil {
		s.tcp.Close()
	}
	s.mu.Lock()
	if s.menuDown {
		_ = s.kb.Up(s.cfg.Bindings["menu"])
		s.menuDown = false
	}
	if s.active != nil {
		s.active.Close()
		s.active = nil
	}
	s.mu.Unlock()
}

func (s *bridgeServer) printBanner() {
	mode := "确认配对"
	if s.cfg.AutoPair {
		mode = "自动配对（--auto-pair）"
	}
	if s.dry {
		mode += " / DRY-RUN（不注入按键）"
	}
	fmt.Println("============================================================")
	fmt.Println(" Stratagem Hero LAN Keyboard Bridge", version)
	fmt.Println("============================================================")
	fmt.Printf(" UDP 配对端口 : %d\n", s.cfg.UDPPort)
	fmt.Printf(" TCP 控制端口 : %d\n", s.cfg.TCPPort)
	fmt.Printf(" 配对模式     : %s\n", mode)
	fmt.Printf(" 按键映射     : Up=%s  Down=%s  Left=%s  Right=%s  Menu=%s\n",
		s.cfg.Bindings["up"], s.cfg.Bindings["down"], s.cfg.Bindings["left"],
		s.cfg.Bindings["right"], s.cfg.Bindings["menu"])
	for _, ip := range localIPv4() {
		fmt.Printf(" 本机地址     : %s\n", ip)
	}
	fmt.Println("------------------------------------------------------------")
	fmt.Println(" 手表：主菜单选「键盘」→ 开始，屏幕会显示 6 位配对码。")
	if s.cfg.AutoPair {
		fmt.Println(" 等待手表广播；收到后自动接受。")
	} else {
		fmt.Println(" 收到手表后，在下面输入手表上的 6 位配对码并回车。")
	}
	fmt.Println(" 输入 list 查看等待项，q 退出；Windows 防火墙需放行 UDP/TCP。")
	fmt.Println("============================================================")
}

func localIPv4() []string {
	var out []string
	addrs, _ := net.InterfaceAddrs()
	for _, a := range addrs {
		if ipnet, ok := a.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
			if v4 := ipnet.IP.To4(); v4 != nil {
				out = append(out, v4.String())
			}
		}
	}
	if len(out) == 0 {
		out = append(out, "127.0.0.1")
	}
	return out
}

func (s *bridgeServer) udpLoop() {
	buf := make([]byte, 2048)
	for {
		n, addr, err := s.udp.ReadFromUDP(buf)
		if err != nil {
			return
		}
		line := strings.TrimSpace(string(buf[:n]))
		parts := strings.Split(line, "|")
		if len(parts) < 3 || parts[0] != "HD2PAIR1" {
			continue
		}
		code := strings.TrimSpace(parts[1])
		name := sanitizeName(parts[2])
		if !sixDigits(code) {
			continue
		}
		p := &pendingPair{code: code, name: name, addr: addr, seen: time.Now()}
		s.mu.Lock()
		existing := s.pending[code]
		fresh := existing == nil || existing.addr.String() != addr.String()
		if fresh {
			s.pending[code] = p
		}
		s.latest = p
		s.mu.Unlock()
		if fresh {
			fmt.Printf("\n[PAIR] 手表 %q  %s  请求配对\n", name, addr.IP.String())
			fmt.Printf("       手表配对码: %s\n", code)
			if !s.cfg.AutoPair {
				fmt.Print("       输入该配对码后回车（y=接受最近一个）> ")
			}
		}
		if s.cfg.AutoPair {
			s.acceptPair(p)
		}
	}
}

func (s *bridgeServer) acceptPair(p *pendingPair) {
	s.mu.Lock()
	if _, ok := s.pending[p.code]; !ok {
		s.mu.Unlock()
		return
	}
	delete(s.pending, p.code)
	token, err := randomToken()
	if err != nil {
		s.mu.Unlock()
		log.Printf("token: %v", err)
		return
	}
	s.tokens[token] = p.name
	s.mu.Unlock()

	msg := fmt.Sprintf("HD2PAIR1_OK|%s|%d\n", token, s.cfg.TCPPort)
	if _, err := s.udp.WriteToUDP([]byte(msg), p.addr); err != nil {
		log.Printf("pair reply: %v", err)
		return
	}
	fmt.Printf("\n[PAIR] 已配对 %q (%s)，token=%s…\n", p.name, p.addr.IP.String(), token[:8])
	fmt.Print("> ")
}

func (s *bridgeServer) consoleLoop() {
	scanner := bufio.NewScanner(os.Stdin)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" {
			continue
		}
		switch strings.ToLower(line) {
		case "q", "quit", "exit":
			os.Exit(0)
		case "list":
			s.listPending()
			continue
		case "y", "yes":
			s.mu.Lock()
			p := s.latest
			s.mu.Unlock()
			if p != nil {
				s.acceptPair(p)
			} else {
				fmt.Println("没有等待配对的手表")
			}
			continue
		}
		s.mu.Lock()
		p := s.pending[line]
		s.mu.Unlock()
		if p == nil {
			fmt.Println("没有等待这个配对码的手表；输入 list 查看。")
			continue
		}
		s.acceptPair(p)
	}
}

func (s *bridgeServer) listPending() {
	s.mu.Lock()
	defer s.mu.Unlock()
	if len(s.pending) == 0 {
		fmt.Println("没有等待配对的手表")
		return
	}
	for _, p := range s.pending {
		fmt.Printf("  %s  %s  %s\n", p.code, p.name, p.addr.IP.String())
	}
}

func (s *bridgeServer) tcpLoop() {
	for {
		conn, err := s.tcp.AcceptTCP()
		if err != nil {
			return
		}
		go s.handleClient(conn)
	}
}

func (s *bridgeServer) handleClient(conn *net.TCPConn) {
	defer conn.Close()
	_ = conn.SetDeadline(time.Now().Add(12 * time.Second))
	reader := bufio.NewReader(conn)
	line, err := reader.ReadString('\n')
	if err != nil {
		return
	}
	parts := strings.Split(strings.TrimSpace(line), "|")
	if len(parts) < 3 || parts[0] != "HD2HELLO1" {
		conn.Write([]byte("HD2HELLO1_ERR|bad hello\n"))
		return
	}
	token := parts[1]
	name := sanitizeName(parts[2])
	s.mu.Lock()
	known := s.tokens[token]
	if known == "" {
		s.mu.Unlock()
		conn.Write([]byte("HD2HELLO1_ERR|bad token\n"))
		fmt.Printf("\n[LINK] token 无效，要求手表重新配对\n> ")
		return
	}
	old := s.active
	s.active = conn
	s.mu.Unlock()
	if old != nil && old != conn {
		old.Close()
	}
	_ = conn.SetReadDeadline(time.Time{})
	_ = conn.SetWriteDeadline(time.Time{})
	fmt.Printf("\n[LINK] 已连接 %q (%s)\n", name, conn.RemoteAddr())
	if s.verbose {
		fmt.Printf("       映射 Up=%s Down=%s Left=%s Right=%s Menu=%s\n",
			s.cfg.Bindings["up"], s.cfg.Bindings["down"], s.cfg.Bindings["left"],
			s.cfg.Bindings["right"], s.cfg.Bindings["menu"])
	}
	fmt.Print("> ")
	conn.Write([]byte("HD2HELLO1_OK|StratagemHeroBridge\n"))

	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			break
		}
		cmd := strings.TrimSpace(line)
		if cmd == "" {
			continue
		}
		s.handleCommand(cmd)
	}

	s.mu.Lock()
	if s.active == conn {
		s.active = nil
		if s.menuDown {
			_ = s.kb.Up(s.cfg.Bindings["menu"])
			s.menuDown = false
		}
	}
	s.mu.Unlock()
	fmt.Printf("\n[LINK] 断开 %s\n> ", conn.RemoteAddr())
}

func (s *bridgeServer) handleCommand(cmd string) {
	parts := strings.Split(cmd, "|")
	switch parts[0] {
	case "PING":
		s.mu.Lock()
		if s.active != nil {
			s.active.Write([]byte("PONG\n"))
		}
		s.mu.Unlock()
	case "DIR":
		if len(parts) < 2 {
			return
		}
		var action string
		switch parts[1] {
		case "U":
			action = "up"
		case "D":
			action = "down"
		case "L":
			action = "left"
		case "R":
			action = "right"
		default:
			return
		}
		key := s.cfg.Bindings[action]
		if err := s.kb.Tap(key); err != nil {
			log.Printf("key tap %s: %v", key, err)
		} else if s.verbose {
			fmt.Printf("[KEY] tap %s\n", key)
		}
	case "MENU":
		if len(parts) < 2 {
			return
		}
		s.mu.Lock()
		defer s.mu.Unlock()
		if parts[1] == "1" && !s.menuDown {
			if err := s.kb.Down(s.cfg.Bindings["menu"]); err != nil {
				log.Printf("menu down: %v", err)
			} else {
				s.menuDown = true
				if s.verbose {
					fmt.Printf("[KEY] down %s (menu open)\n", s.cfg.Bindings["menu"])
				}
			}
		} else if parts[1] == "0" && s.menuDown {
			if err := s.kb.Up(s.cfg.Bindings["menu"]); err != nil {
				log.Printf("menu up: %v", err)
			} else {
				s.menuDown = false
				if s.verbose {
					fmt.Printf("[KEY] up %s (menu close)\n", s.cfg.Bindings["menu"])
				}
			}
		}
	}
}

func sixDigits(s string) bool {
	if len(s) != 6 {
		return false
	}
	for _, c := range s {
		if c < '0' || c > '9' {
			return false
		}
	}
	return true
}

func sanitizeName(s string) string {
	s = strings.ReplaceAll(s, "|", " ")
	s = strings.TrimSpace(s)
	if len(s) > 40 {
		s = s[:40]
	}
	if s == "" {
		s = "Watch"
	}
	return s
}

func randomToken() (string, error) {
	buf := make([]byte, 16)
	if _, err := rand.Read(buf); err != nil {
		return "", err
	}
	return hex.EncodeToString(buf), nil
}
