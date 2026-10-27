# Stratagem Hero · HDWearStrategy

[![Wear OS](https://img.shields.io/badge/Wear%20OS-3%2B-3DDC84?logo=wearos&logoColor=white)](https://www.google.com/search?q=Wear+OS)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Release](https://img.shields.io/badge/release-v1.6.1-FFD24A)](https://github.com/lihaoyuan114/HDWearStrategy/releases/tag/v1.6.1)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

[中文](README.md) · **English**

> Unofficial fan project. Free, no ads.  
> HELLDIVERS 2 and all game assets, names, codes, voices and sounds belong to Arrowhead Game Studios / Sony Interactive Entertainment.

---

## Download · Release

All APKs and the server archive live in the GitHub Release:

**[v1.6.1 Release](https://github.com/lihaoyuan114/HDWearStrategy/releases/tag/v1.6.1)**

| File | Package | Size |
|---|---|---|
| [Chinese APK](https://github.com/lihaoyuan114/HDWearStrategy/releases/download/v1.6.1/StratagemHero-Watch4-v1.6.1-zh.apk) | `com.deepsea.stratagemhero` | 2,574,107 B |
| [English APK](https://github.com/lihaoyuan114/HDWearStrategy/releases/download/v1.6.1/StratagemHero-Watch4-v1.6.1-en.apk) | `com.deepsea.stratagemhero.en` | 2,574,107 B |
| [Keyboard server ZIP](https://github.com/lihaoyuan114/HDWearStrategy/releases/download/v1.6.1/stratagem-server-v1.0.0.zip) | Windows + Linux | 2.5 MB |

The two APKs use different package IDs and can be installed side by side. Version `1.6.1` / `versionCode 9`.

```
Chinese APK SHA-256 7ea0c3403071d597615a42f69016fe1d253a6b91430f4d6df12b83f492ebe055
English APK SHA-256 d05171b0811f6256a23edfb82185b768db49d84b4e8d2ca1a552fabcab2a90d3
Server ZIP  SHA-256 07a62628dc075571bfa6de7e52dd992100ed8acb52a613971650eea4a3612600
```

---

## Features

- **Standard**: 5 random stratagems per group, top group timer, settlement after each group; faster bars at higher group indexes.
- **Free call**: truly blank, untimed, no score/combo; match a full code to hear the call-in voice and reveal the stratagem.
- **Keyboard**: pair over LAN and map watch arrows to PC keys; bottom-left CTRL holds/releases the in-game stratagem menu.
- **Round + square**: circular instrument layout on round screens; full rounded panel and edge keys on square screens.
- **Chinese / English builds**: separate packages, installable together.
- **Original game audio**: input/confirm/error cues, Stratagem Hero round cues and BGM, beacon and category-grouped Mission Control voice lines.

---

## Screenshots

### Round

| Menu | Standard group | Group clear |
|---|---|---|
| ![menu](preview/render/01_menu.png) | ![standard](preview/render/02_game_resupply.png) | ![group](preview/render/15_group_clear.png) |

| Free blank | Input ghost | Free reveal |
|---|---|---|
| ![free](preview/render/12_free_game.png) | ![input](preview/render/12b_free_input.png) | ![reveal](preview/render/13_free_reveal.png) |

### Square

| 450 menu | 450 game | 396 menu |
|---|---|---|
| ![square menu](preview/render/18_menu_square.png) | ![square game](preview/render/19_game_square.png) | ![square 396](preview/render/20_menu_square_396.png) |

### Keyboard on device

| Pairing code | LINKED | CTRL held |
|---|---|---|
| ![pair](preview/device/v150_pair.png) | ![linked](preview/device/v150_remote_click.png) | ![ctrl](preview/device/v150_remote_ctrl.png) |

Keyboard pause / re-pair: [v150_remote_pause.png](preview/device/v150_remote_pause.png)

---

## Gameplay

### Standard
- 5 per group; the first target of group 1 is always Resupply.
- Top bar is the group timer: 30s → 28s → 26s … minimum 14s.
- Group settlement: group score, total score, best combo; continue or exit.
- A wrong arrow clears the code and costs 1.2s; timeout ends the run. BGM plays in standard mode.

### Free call
- No score, combo, call count, title, or empty input line; only pause and keys.
- No timer, no countdown, no time penalty; reveal has no score and free mode does not accumulate score internally.

### Keyboard
- Menu → **Keyboard** → **Connect PC**; the watch shows a 6-digit pairing code.
- Enter that code in the PC server; the watch shows LINKED.
- Arrows → W/S/A/D (configurable); bottom-left **CTRL** taps down/up.
- Pause, exit and disconnect auto-release Ctrl; the pause menu offers resume, re-pair and exit.

### Input feedback
- A translucent direction ghost slides about 14px and fades over 0.45s.
- Round: arc keys; square: edge bars; swipe mode shows no fake arrow buttons.
- Horizontal threshold ~16px, vertical ~24px, with cancel/up compensation.
- **Right-swipe compatibility**: API 29/30 uses only 98dp edge exclusions (a full-screen rect can exceed the 200dp budget and invalidate the whole list); API 31+ uses a full-screen exclusion; if the system turns the swipe into Back, the `ACTION_CANCEL` / Back callback feeds the direction back.

---

## Keyboard server

Download [stratagem-server-v1.0.0.zip](https://github.com/lihaoyuan114/HDWearStrategy/releases/download/v1.6.1/stratagem-server-v1.0.0.zip), or read [server/README.md](server/README.md).

- Windows: `stratagem-server-windows-amd64.exe`
- Linux: `stratagem-server-linux-amd64` (uinput; root/udev permission required)
- Defaults: `Up=W`, `Down=S`, `Left=A`, `Right=D`, `Menu=LEFTCTRL`
- Configure `server-config.json` or use `--up/--down/--left/--right/--menu`
- UDP 45891 pairing, TCP 45892 control; same Wi-Fi/subnet; allow Windows Firewall
- `--dry-run` tests networking/mapping without injecting keys; `--auto-pair` skips confirmation

---

## Install

```bash
adb install -r StratagemHero-Watch4-v1.6.1-zh.apk
adb install -r StratagemHero-Watch4-v1.6.1-en.apk
```

```bash
adb shell am start -n com.deepsea.stratagemhero/com.deepsea.stratagemhero.MainActivity
adb shell am start -n com.deepsea.stratagemhero.en/com.deepsea.stratagemhero.MainActivity
```

---

## Build & test

Requires Android SDK 34, Build-Tools 34.0.0 and JDK 17.

```bash
./build.sh both     # Chinese + English
./build.sh zh
./build.sh en
./server/build-server.sh
```

- Pure-logic tests: `# PASS: 710 checks`.
- Robolectric: 450/396 round, 450/396 square, English UI, five-mode menu.
- v1.5.0 was tested on a real SM-R890 (pairing, LINKED, CTRL down/up, D/S mapping, server dry-run). v1.6.1 bilingual/square builds passed build and rendering checks; the watch went offline before v1.6.1 could be sideloaded.
- Windows/Linux server binaries are cross-compiled; real injection needs Windows foreground/admin or Linux `/dev/uinput` permission.

---

## Credits

- Icons: [igromanru/Helldivers-2-Stratagems-icons-png](https://github.com/igromanru/Helldivers-2-Stratagems-icons-png)
- Names/codes: [Helldivers Wiki](https://helldivers.wiki.gg/wiki/Stratagems)
- Audio/voice: [aido727/HD2-buttons-soundboard-app](https://github.com/aido727/HD2-buttons-soundboard-app), [BubbleDuckyBro/helldivers-2-stratagem-hero-audio-files](https://github.com/BubbleDuckyBro/helldivers-2-stratagem-hero-audio-files)
- Code: MIT (see [LICENSE](LICENSE)); game assets/audio are not MIT.
- Not affiliated with Arrowhead Game Studios, Sony Interactive Entertainment or Samsung.
