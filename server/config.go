package main

import (
	"encoding/json"
	"fmt"
	"os"
	"strings"
)

type Config struct {
	UDPPort   int               `json:"udp_port"`
	TCPPort   int               `json:"tcp_port"`
	AutoPair  bool              `json:"auto_pair"`
	KeyHoldMS int               `json:"key_hold_ms"`
	Bindings  map[string]string `json:"bindings"`
}

func defaultConfig() Config {
	return Config{
		UDPPort:   45891,
		TCPPort:   45892,
		AutoPair:  false,
		KeyHoldMS: 18,
		Bindings: map[string]string{
			"up":    "W",
			"down":  "S",
			"left":  "A",
			"right": "D",
			"menu":  "LEFTCTRL",
		},
	}
}

func loadConfig(path string) (Config, error) {
	cfg := defaultConfig()
	data, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			return cfg, nil
		}
		return cfg, err
	}
	if err := json.Unmarshal(data, &cfg); err != nil {
		return cfg, fmt.Errorf("parse %s: %w", path, err)
	}
	if cfg.Bindings == nil {
		cfg.Bindings = defaultConfig().Bindings
	}
	for action, key := range cfg.Bindings {
		cfg.Bindings[action] = normalizeKey(key)
	}
	if cfg.UDPPort <= 0 || cfg.UDPPort > 65535 {
		return cfg, fmt.Errorf("invalid udp_port")
	}
	if cfg.TCPPort <= 0 || cfg.TCPPort > 65535 {
		return cfg, fmt.Errorf("invalid tcp_port")
	}
	if cfg.KeyHoldMS < 4 {
		cfg.KeyHoldMS = 4
	}
	if cfg.KeyHoldMS > 250 {
		cfg.KeyHoldMS = 250
	}
	for _, action := range []string{"up", "down", "left", "right", "menu"} {
		key := cfg.Bindings[action]
		if key == "" {
			return cfg, fmt.Errorf("binding %q is empty", action)
		}
		if !validKey(key) {
			return cfg, fmt.Errorf("binding %q: unknown key %q", action, key)
		}
	}
	return cfg, nil
}

func normalizeKey(key string) string {
	key = strings.TrimSpace(strings.ToUpper(key))
	key = strings.ReplaceAll(key, " ", "")
	key = strings.ReplaceAll(key, "-", "")
	key = strings.ReplaceAll(key, "_", "")
	switch key {
	case "CTRL", "CONTROL", "LEFTCTRL", "LCTRL":
		return "LEFTCTRL"
	case "RIGHTCTRL", "RCTRL":
		return "RIGHTCTRL"
	case "SHIFT", "LEFTSHIFT", "LSHIFT":
		return "LEFTSHIFT"
	case "RIGHTSHIFT", "RSHIFT":
		return "RIGHTSHIFT"
	case "ALT", "LEFTALT", "LALT":
		return "LEFTALT"
	case "RIGHTALT", "RALT":
		return "RIGHTALT"
	case "ESC", "ESCAPE":
		return "ESC"
	case "RETURN":
		return "ENTER"
	case "SPACEBAR":
		return "SPACE"
	}
	return key
}
