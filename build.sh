#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
SDK="${ANDROID_SDK_ROOT:-$ROOT/sdk}"
BT="$SDK/build-tools/34.0.0"
PLATFORM="$SDK/platforms/android-34/android.jar"
DIST="$ROOT/dist"
JDK="$ROOT/jdk17"
VERSION_CODE=8
VERSION_NAME=1.6.0

if [[ ! -x "$BT/aapt2" || ! -f "$PLATFORM" || ! -x "$JDK/bin/javac" ]]; then
  echo "Android SDK 34 not found. Set ANDROID_SDK_ROOT or run the SDK setup first." >&2
  exit 1
fi

export JAVA_HOME="$JDK"
export PATH="$JDK/bin:$PATH"
mkdir -p "$DIST"

trap 'cp "$ROOT/app/labels/zh.xml" "$ROOT/app/res/values/strings.xml"' EXIT

build_one() {
  local lang="$1" pkg="$2" label="$3" outname="$4"
  local out="$ROOT/build-$lang"
  echo
  echo "===== building $lang ($pkg) ====="
  cp "$ROOT/app/labels/$lang.xml" "$ROOT/app/res/values/strings.xml"
  rm -rf "$out"
  mkdir -p "$out/res" "$out/gen" "$out/classes" "$out/dex"

  "$BT/aapt2" compile --dir "$ROOT/app/res" -o "$out/res/resources.zip"

  local link_args=(
    -o "$out/base.apk"
    -I "$PLATFORM"
    --manifest "$ROOT/app/AndroidManifest.xml"
    -R "$out/res/resources.zip"
    -A "$ROOT/app/assets"
    --java "$out/gen"
    --custom-package com.deepsea.stratagemhero
    --auto-add-overlay
    --min-sdk-version 26
    --target-sdk-version 34
    --version-code "$VERSION_CODE"
    --version-name "$VERSION_NAME"
  )
  if [[ "$lang" == "en" ]]; then
    link_args+=(--rename-manifest-package com.deepsea.stratagemhero.en)
  fi
  "$BT/aapt2" link "${link_args[@]}"

  mapfile -t SOURCES < <(find "$ROOT/app/src" "$out/gen" -name '*.java' -print | sort)
  "$JDK/bin/javac" -encoding UTF-8 -source 8 -target 8 \
    -bootclasspath "$PLATFORM" \
    -d "$out/classes" \
    "${SOURCES[@]}"

  mapfile -t CLASSES < <(find "$out/classes" -name '*.class' -print | sort)
  "$BT/d8" --release --min-api 26 --lib "$PLATFORM" --output "$out/dex" "${CLASSES[@]}"

  cp "$out/base.apk" "$out/unaligned.apk"
  ( cd "$out/dex" && zip -q -9 "$out/unaligned.apk" classes.dex )
  "$BT/zipalign" -f -p 4 "$out/unaligned.apk" "$out/aligned.apk"

  KEYSTORE="$ROOT/keystore/release.jks"
  STORE_PASS="stratagemhero2026"
  KEY_PASS="stratagemhero2026"
  if [[ ! -f "$KEYSTORE" ]]; then
    mkdir -p "$ROOT/keystore"
    keytool -genkeypair -v \
      -keystore "$KEYSTORE" \
      -alias stratagemhero \
      -keyalg RSA -keysize 2048 -validity 10000 \
      -storepass "$STORE_PASS" -keypass "$KEY_PASS" \
      -dname "CN=DeepSea Stratagem Hero, OU=Fan Project, O=DeepSea, L=Shanghai, ST=Shanghai, C=CN" \
      >/dev/null 2>&1
  fi

  local apk="$DIST/$outname"
  "$BT/apksigner" sign \
    --ks "$KEYSTORE" \
    --ks-key-alias stratagemhero \
    --ks-pass "pass:$STORE_PASS" \
    --key-pass "pass:$KEY_PASS" \
    --v4-signing-enabled false \
    --out "$apk" \
    "$out/aligned.apk"

  "$BT/apksigner" verify --verbose "$apk" | sed -n '1,6p'
  "$BT/aapt2" dump badging "$apk" | sed -n '1,12p'
  echo "APK: $apk"
  sha256sum "$apk"
}

MODE="${1:-both}"
case "$MODE" in
  zh) build_one zh com.deepsea.stratagemhero "战略配备英雄" "StratagemHero-Watch4-v${VERSION_NAME}-zh.apk" ;;
  en) build_one en com.deepsea.stratagemhero.en "Stratagem Hero" "StratagemHero-Watch4-v${VERSION_NAME}-en.apk" ;;
  both)
    build_one zh com.deepsea.stratagemhero "战略配备英雄" "StratagemHero-Watch4-v${VERSION_NAME}-zh.apk"
    build_one en com.deepsea.stratagemhero.en "Stratagem Hero" "StratagemHero-Watch4-v${VERSION_NAME}-en.apk"
    ;;
  *) echo "usage: $0 [zh|en|both]" >&2; exit 2 ;;
esac
