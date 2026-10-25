#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
export GOCACHE="${GOCACHE:-$ROOT/../.gocache}"
export GOPATH="${GOPATH:-$ROOT/../.gopath}"
mkdir -p dist
echo "[1/3] linux/amd64"
GOOS=linux GOARCH=amd64 CGO_ENABLED=0 go build -trimpath -ldflags "-s -w" -o dist/stratagem-server-linux-amd64 .
echo "[2/3] windows/amd64"
GOOS=windows GOARCH=amd64 CGO_ENABLED=0 go build -trimpath -ldflags "-s -w" -o dist/stratagem-server-windows-amd64.exe .
echo "[3/3] config"
cp server-config.json dist/server-config.json
printf '\nBuilt:\n'
ls -lh dist
