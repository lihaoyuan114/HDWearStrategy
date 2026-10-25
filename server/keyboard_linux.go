//go:build linux

package main

import (
	"encoding/binary"
	"fmt"
	"os"
	"sync"
	"syscall"
	"time"
	"unsafe"
)

const (
	evKey        = 1
	evSyn        = 0
	synReport    = 0
	uiSetEvBit   = 0x40045564
	uiSetKeyBit  = 0x40045565
	uiDevSetup   = 0x405c5503
	uiDevCreate  = 0x5501
	uiDevDestroy = 0x5502
	busUSB       = 0x03
)

type inputID struct {
	BusType uint16
	Vendor  uint16
	Product uint16
	Version uint16
}

type uinputSetup struct {
	ID           inputID
	Name         [80]byte
	FFEffectsMax uint32
}

type inputEvent struct {
	Sec   int64
	Usec  int64
	Type  uint16
	Code  uint16
	Value int32
}

type linuxKeyboard struct {
	mu   sync.Mutex
	file *os.File
}

func newPlatformKeyboard() (Keyboard, error) {
	f, err := os.OpenFile("/dev/uinput", os.O_WRONLY|syscall.O_NONBLOCK, 0)
	if err != nil {
		return nil, fmt.Errorf("open /dev/uinput failed (run as root or install a udev rule): %w", err)
	}
	k := &linuxKeyboard{file: f}
	if err := k.setup(); err != nil {
		f.Close()
		return nil, err
	}
	return k, nil
}

func (k *linuxKeyboard) setup() error {
	fd := k.file.Fd()
	if err := ioctl(fd, uiSetEvBit, uintptr(evKey)); err != nil {
		return fmt.Errorf("UI_SET_EVBIT: %w", err)
	}
	// Register a full keyboard key range so any configured mapping is usable.
	for code := uintptr(1); code <= 248; code++ {
		if err := ioctl(fd, uiSetKeyBit, code); err != nil {
			return fmt.Errorf("UI_SET_KEYBIT %d: %w", code, err)
		}
	}
	var setup uinputSetup
	setup.ID = inputID{BusType: busUSB, Vendor: 0x1209, Product: 0x0001, Version: 1}
	copy(setup.Name[:], []byte("Stratagem Hero Bridge"))
	if err := ioctl(fd, uiDevSetup, uintptr(unsafe.Pointer(&setup))); err != nil {
		return fmt.Errorf("UI_DEV_SETUP: %w", err)
	}
	if err := ioctl(fd, uiDevCreate, 0); err != nil {
		return fmt.Errorf("UI_DEV_CREATE: %w", err)
	}
	time.Sleep(120 * time.Millisecond)
	return nil
}

func ioctl(fd, req, arg uintptr) error {
	_, _, errno := syscall.Syscall(syscall.SYS_IOCTL, fd, req, arg)
	if errno != 0 {
		return errno
	}
	return nil
}

func (k *linuxKeyboard) Tap(key string) error {
	if err := k.Down(key); err != nil {
		return err
	}
	time.Sleep(platformHold)
	return k.Up(key)
}

func (k *linuxKeyboard) Down(key string) error {
	k.mu.Lock()
	defer k.mu.Unlock()
	code, err := linuxKeyCode(key)
	if err != nil {
		return err
	}
	return k.writeKey(code, 1)
}

func (k *linuxKeyboard) Up(key string) error {
	k.mu.Lock()
	defer k.mu.Unlock()
	code, err := linuxKeyCode(key)
	if err != nil {
		return err
	}
	return k.writeKey(code, 0)
}

func (k *linuxKeyboard) writeKey(code uint16, value int32) error {
	if err := k.writeEvent(evKey, code, value); err != nil {
		return err
	}
	return k.writeEvent(evSyn, synReport, 0)
}

func (k *linuxKeyboard) writeEvent(typ, code uint16, value int32) error {
	now := time.Now()
	ev := inputEvent{
		Sec:   now.Unix(),
		Usec:  int64(now.Nanosecond() / 1000),
		Type:  typ,
		Code:  code,
		Value: value,
	}
	return binary.Write(k.file, binary.LittleEndian, ev)
}

func (k *linuxKeyboard) Close() error {
	k.mu.Lock()
	defer k.mu.Unlock()
	if k.file == nil {
		return nil
	}
	_ = ioctl(k.file.Fd(), uiDevDestroy, 0)
	err := k.file.Close()
	k.file = nil
	return err
}

func linuxKeyCode(key string) (uint16, error) {
	if code, ok := linuxLetters[key]; ok {
		return code, nil
	}
	if code, ok := linuxSpecial[key]; ok {
		return code, nil
	}
	if len(key) == 1 && key[0] >= '0' && key[0] <= '9' {
		return uint16(2 + (key[0] - '0')), nil
	}
	return 0, fmt.Errorf("unknown Linux key %q", key)
}

var linuxLetters = map[string]uint16{
	"Q": 16, "W": 17, "E": 18, "R": 19, "T": 20, "Y": 21, "U": 22,
	"I": 23, "O": 24, "P": 25, "A": 30, "S": 31, "D": 32, "F": 33,
	"G": 34, "H": 35, "J": 36, "K": 37, "L": 38, "Z": 44, "X": 45,
	"C": 46, "V": 47, "B": 48, "N": 49, "M": 50,
}

var linuxSpecial = map[string]uint16{
	"LEFTCTRL": 29, "RIGHTCTRL": 97, "LEFTSHIFT": 42, "RIGHTSHIFT": 54,
	"LEFTALT": 56, "RIGHTALT": 100, "SPACE": 57, "ENTER": 28, "ESC": 1,
	"TAB": 15, "BACKSPACE": 14, "DELETE": 111, "HOME": 102, "END": 107,
	"PAGEUP": 104, "PAGEDOWN": 109, "UP": 103, "DOWN": 108, "LEFT": 105,
	"RIGHT": 106,
	"F1":    59, "F2": 60, "F3": 61, "F4": 62, "F5": 63, "F6": 64,
	"F7": 65, "F8": 66, "F9": 67, "F10": 68, "F11": 87, "F12": 88,
	"MINUS": 12, "EQUAL": 13, "LEFTBRACE": 26, "RIGHTBRACE": 27,
	"BACKSLASH": 43, "SEMICOLON": 39, "APOSTROPHE": 40, "GRAVE": 41,
	"COMMA": 51, "DOT": 52, "SLASH": 53,
}
