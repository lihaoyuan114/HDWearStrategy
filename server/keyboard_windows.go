//go:build windows

package main

import (
	"encoding/binary"
	"fmt"
	"sync"
	"syscall"
	"time"
	"unsafe"
)

const (
	inputKeyboard  = 1
	keyeventfKeyUp = 0x0002
)

type windowsInput struct {
	Type uint32
	_    uint32
	Data [32]byte
}

type windowsKeyboard struct {
	mu sync.Mutex
}

var (
	user32        = syscall.NewLazyDLL("user32.dll")
	procSendInput = user32.NewProc("SendInput")
)

func newPlatformKeyboard() (Keyboard, error) {
	return &windowsKeyboard{}, nil
}

func (k *windowsKeyboard) Tap(key string) error {
	if err := k.Down(key); err != nil {
		return err
	}
	time.Sleep(platformHold)
	return k.Up(key)
}

func (k *windowsKeyboard) Down(key string) error {
	k.mu.Lock()
	defer k.mu.Unlock()
	vk, err := windowsKeyCode(key)
	if err != nil {
		return err
	}
	return sendKey(vk, false)
}

func (k *windowsKeyboard) Up(key string) error {
	k.mu.Lock()
	defer k.mu.Unlock()
	vk, err := windowsKeyCode(key)
	if err != nil {
		return err
	}
	return sendKey(vk, true)
}

func sendKey(vk uint16, up bool) error {
	var in windowsInput
	in.Type = inputKeyboard
	binary.LittleEndian.PutUint16(in.Data[0:2], vk)
	flags := uint32(0)
	if up {
		flags = keyeventfKeyUp
	}
	binary.LittleEndian.PutUint32(in.Data[4:8], flags)
	r, _, err := procSendInput.Call(1, uintptr(unsafe.Pointer(&in)), unsafe.Sizeof(in))
	if r == 0 {
		return fmt.Errorf("SendInput failed: %v", err)
	}
	return nil
}

func (k *windowsKeyboard) Close() error { return nil }

func windowsKeyCode(key string) (uint16, error) {
	if len(key) == 1 {
		c := key[0]
		if c >= 'A' && c <= 'Z' {
			return uint16(c), nil
		}
		if c >= '0' && c <= '9' {
			return uint16(c), nil
		}
	}
	if code, ok := windowsSpecial[key]; ok {
		return code, nil
	}
	return 0, fmt.Errorf("unknown Windows key %q", key)
}

var windowsSpecial = map[string]uint16{
	"LEFTCTRL": 0xA2, "RIGHTCTRL": 0xA3, "LEFTSHIFT": 0xA0, "RIGHTSHIFT": 0xA1,
	"LEFTALT": 0xA4, "RIGHTALT": 0xA5, "SPACE": 0x20, "ENTER": 0x0D,
	"ESC": 0x1B, "TAB": 0x09, "BACKSPACE": 0x08, "DELETE": 0x2E,
	"HOME": 0x24, "END": 0x23, "PAGEUP": 0x21, "PAGEDOWN": 0x22,
	"UP": 0x26, "DOWN": 0x28, "LEFT": 0x25, "RIGHT": 0x27,
	"F1": 0x70, "F2": 0x71, "F3": 0x72, "F4": 0x73, "F5": 0x74,
	"F6": 0x75, "F7": 0x76, "F8": 0x77, "F9": 0x78, "F10": 0x79,
	"F11": 0x7A, "F12": 0x7B,
	"MINUS": 0xBD, "EQUAL": 0xBB, "LEFTBRACE": 0xDB, "RIGHTBRACE": 0xDD,
	"BACKSLASH": 0xDC, "SEMICOLON": 0xBA, "APOSTROPHE": 0xDE,
	"GRAVE": 0xC0, "COMMA": 0xBC, "DOT": 0xBE, "SLASH": 0xBF,
}
