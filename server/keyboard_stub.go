//go:build !linux && !windows

package main

import "fmt"

func newPlatformKeyboard() (Keyboard, error) {
	return nil, fmt.Errorf("unsupported OS")
}
