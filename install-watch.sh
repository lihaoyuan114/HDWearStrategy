#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
LANG_TAG="${1:-zh}"
case "$LANG_TAG" in
  zh) APK="$ROOT/dist/StratagemHero-Watch4-v1.6.0-zh.apk"; PKG="com.deepsea.stratagemhero" ;;
  en) APK="$ROOT/dist/StratagemHero-Watch4-v1.6.0-en.apk"; PKG="com.deepsea.stratagemhero.en" ;;
  *) echo "usage: $0 [zh|en]" >&2; exit 2 ;;
esac
ADB="${ADB:-adb}"
if [[ ! -f "$APK" ]]; then
  echo "APK not found: $APK" >&2
  echo "Run ./build.sh both first." >&2
  exit 1
fi
echo "Connected devices:"
"$ADB" devices -l
echo
echo "Installing $APK"
"$ADB" install -r "$APK"
echo
echo "Launching on watch..."
"$ADB" shell am start -n "$PKG/com.deepsea.stratagemhero.MainActivity"
