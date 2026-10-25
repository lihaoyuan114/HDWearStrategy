package main

import (
	"fmt"
	"log"
	"sync"
	"time"
)

type Keyboard interface {
	Tap(key string) error
	Down(key string) error
	Up(key string) error
	Close() error
}

type dryKeyboard struct {
	mu   sync.Mutex
	hold time.Duration
}

func newDryKeyboard(hold time.Duration) Keyboard {
	return &dryKeyboard{hold: hold}
}

func (d *dryKeyboard) Tap(key string) error {
	log.Printf("[DRY] tap %s", key)
	time.Sleep(d.hold)
	return nil
}
func (d *dryKeyboard) Down(key string) error {
	log.Printf("[DRY] down %s", key)
	return nil
}
func (d *dryKeyboard) Up(key string) error {
	log.Printf("[DRY] up %s", key)
	return nil
}
func (d *dryKeyboard) Close() error { return nil }

var platformHold = 18 * time.Millisecond

func newKeyboardForOS(dry bool, hold time.Duration) (Keyboard, error) {
	platformHold = hold
	if dry {
		return newDryKeyboard(hold), nil
	}
	kb, err := newPlatformKeyboard()
	if err != nil {
		return nil, fmt.Errorf("keyboard backend: %w", err)
	}
	return kb, nil
}
