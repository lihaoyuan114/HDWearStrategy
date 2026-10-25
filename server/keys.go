package main

var knownKeys = map[string]bool{}

func init() {
	for c := 'A'; c <= 'Z'; c++ {
		knownKeys[string(c)] = true
	}
	for c := '0'; c <= '9'; c++ {
		knownKeys[string(c)] = true
	}
	for _, k := range []string{
		"LEFTCTRL", "RIGHTCTRL", "LEFTSHIFT", "RIGHTSHIFT",
		"LEFTALT", "RIGHTALT", "SPACE", "ENTER", "ESC", "TAB",
		"BACKSPACE", "DELETE", "HOME", "END", "PAGEUP", "PAGEDOWN",
		"UP", "DOWN", "LEFT", "RIGHT",
		"F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12",
		"MINUS", "EQUAL", "LEFTBRACE", "RIGHTBRACE", "BACKSLASH",
		"SEMICOLON", "APOSTROPHE", "GRAVE", "COMMA", "DOT", "SLASH",
	} {
		knownKeys[k] = true
	}
}

func validKey(key string) bool {
	return knownKeys[key]
}
