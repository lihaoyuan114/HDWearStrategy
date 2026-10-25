package com.deepsea.stratagemhero;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * LAN bridge client. The watch broadcasts a pairing code; the desktop server
 * confirms it and returns a token. After that, direction/menu actions are sent
 * as short newline commands over TCP.
 */
final class KeyboardClient {
    interface Listener {
        void onStatus(String text);
        void onConnected(String endpoint, String token);
        void onDisconnected(String reason);
    }

    static final int UDP_PORT = 45891;

    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean running;
    private volatile Thread thread;
    private volatile Thread heartbeat;
    private volatile Thread writerThread;
    private final BlockingQueue<String> outbox = new LinkedBlockingQueue<>();
    private volatile DatagramSocket udpSocket;
    private volatile Socket socket;
    private volatile BufferedWriter writer;
    private volatile boolean connected;

    KeyboardClient(Listener listener) {
        this.listener = listener;
    }

    void start(String savedHost, int savedPort, String savedToken, String code, String deviceName) {
        close();
        running = true;
        outbox.clear();
        writerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writerLoop();
            }
        }, "lan-writer");
        writerThread.setDaemon(true);
        writerThread.start();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                KeyboardClient.this.run(savedHost, savedPort, savedToken, code, deviceName);
            }
        }, "lan-keyboard");
        thread.setDaemon(true);
        thread.start();
    }

    void close() {
        running = false;
        connected = false;
        writer = null;
        Socket s = socket;
        socket = null;
        if (s != null) try { s.close(); } catch (Exception ignored) {}
        DatagramSocket u = udpSocket;
        udpSocket = null;
        if (u != null) try { u.close(); } catch (Exception ignored) {}
        Thread h = heartbeat;
        heartbeat = null;
        if (h != null) h.interrupt();
        Thread w = writerThread;
        writerThread = null;
        if (w != null) w.interrupt();
        Thread t = thread;
        thread = null;
        if (t != null) t.interrupt();
    }

    boolean isConnected() {
        return connected && writer != null;
    }

    boolean sendDirection(char d) {
        return send("DIR|" + d);
    }

    boolean sendMenu(boolean open) {
        return send("MENU|" + (open ? "1" : "0"));
    }

    private boolean send(String line) {
        if (!connected || writer == null) return false;
        return outbox.offer(line);
    }

    private void writerLoop() {
        while (running) {
            String line;
            try {
                line = outbox.take();
            } catch (InterruptedException e) {
                return;
            }
            BufferedWriter w = writer;
            if (w == null) continue;
            try {
                w.write(line);
                w.write("\n");
                w.flush();
            } catch (Exception e) {
                disconnected(L10n.t("发送失败"));
                return;
            }
        }
    }

    private void run(String host, int port, String token, String code, String deviceName) {
        try {
            if (host != null && !host.isEmpty() && token != null && !token.isEmpty()) {
                status(L10n.t("正在连接上次的电脑…"));
                if (tryConnect(host, port, token, deviceName)) return;
                if (!running) return;
                status(L10n.t("上次的电脑未响应，开始配对…"));
            }
            pair(code, deviceName);
        } catch (Exception e) {
            if (running) status(L10n.t("网络错误：") + shortError(e));
        }
    }

    private void pair(String code, String deviceName) throws Exception {
        DatagramSocket udp = new DatagramSocket();
        udpSocket = udp;
        udp.setBroadcast(true);
        udp.setSoTimeout(900);
        byte[] payload = ("HD2PAIR1|" + code + "|" + deviceName + "\n")
                .getBytes(StandardCharsets.UTF_8);
        byte[] buffer = new byte[512];
        long lastSend = 0L;
        boolean announced = false;
        status(L10n.t("正在搜索电脑…"));
        while (running) {
            long now = android.os.SystemClock.uptimeMillis();
            if (now - lastSend > 1200L) {
                lastSend = now;
                DatagramPacket out = new DatagramPacket(payload, payload.length,
                        InetAddress.getByName("255.255.255.255"), UDP_PORT);
                udp.send(out);
                if (!announced) {
                    announced = true;
                    status(L10n.t("等待电脑确认配对码…"));
                }
            }
            DatagramPacket in = new DatagramPacket(buffer, buffer.length);
            try {
                udp.receive(in);
            } catch (java.net.SocketTimeoutException timeout) {
                continue;
            } catch (Exception e) {
                if (!running) return;
                throw e;
            }
            String line = new String(in.getData(), 0, in.getLength(), StandardCharsets.UTF_8).trim();
            String[] parts = line.split("\\|");
            if (parts.length < 3 || !"HD2PAIR1_OK".equals(parts[0])) continue;
            String token = parts[1];
            int port;
            try {
                port = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                continue;
            }
            String host = in.getAddress().getHostAddress();
            udp.close();
            udpSocket = null;
            status(L10n.t("已配对，正在连接…"));
            if (tryConnect(host, port, token, deviceName)) {
                return;
            }
            if (!running) return;
            status(L10n.t("连接失败，重新搜索…"));
            udp = new DatagramSocket();
            udpSocket = udp;
            udp.setBroadcast(true);
            udp.setSoTimeout(900);
            announced = false;
            lastSend = 0L;
        }
    }

    private boolean tryConnect(String host, int port, String token, String deviceName) {
        Socket s = null;
        try {
            s = new Socket();
            s.connect(new InetSocketAddress(host, port), 2600);
            s.setTcpNoDelay(true);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter w = new BufferedWriter(
                    new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
            w.write("HD2HELLO1|" + token + "|" + deviceName + "\n");
            w.flush();
            String reply = reader.readLine();
            if (reply == null || !reply.startsWith("HD2HELLO1_OK")) {
                try { s.close(); } catch (Exception ignored) {}
                return false;
            }
            if (!running) {
                try { s.close(); } catch (Exception ignored) {}
                return false;
            }
            socket = s;
            writer = w;
            connected = true;
            startHeartbeat();
            main.post(new Runnable() {
                @Override
                public void run() {
                    listener.onConnected(host + ":" + port, token);
                }
            });
            readLoop(reader);
            return true;
        } catch (Exception e) {
            if (s != null) try { s.close(); } catch (Exception ignored) {}
            return false;
        }
    }

    private void startHeartbeat() {
        Thread h = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    try {
                        Thread.sleep(2500L);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (!running || !connected) return;
                    send("PING");
                }
            }
        }, "lan-heartbeat");
        h.setDaemon(true);
        heartbeat = h;
        h.start();
    }

    private void readLoop(BufferedReader reader) {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                // PONG/ack lines are currently informational only.
                if (line.startsWith("PONG")) continue;
            }
        } catch (Exception ignored) {
        } finally {
            if (running) disconnected(L10n.t("电脑连接已断开"));
        }
    }

    private void disconnected(String reason) {
        if (!running) return;
        running = false;
        connected = false;
        writer = null;
        Socket s = socket;
        socket = null;
        if (s != null) try { s.close(); } catch (Exception ignored) {}
        Thread w = writerThread;
        if (w != null) w.interrupt();
        main.post(new Runnable() {
            @Override
            public void run() {
                listener.onDisconnected(reason);
            }
        });
    }

    private void status(String text) {
        main.post(new Runnable() {
            @Override
            public void run() {
                listener.onStatus(text);
            }
        });
    }

    private static String shortError(Exception e) {
        String m = e.getMessage();
        if (m == null || m.isEmpty()) return e.getClass().getSimpleName();
        return m.length() > 40 ? m.substring(0, 40) : m;
    }
}
