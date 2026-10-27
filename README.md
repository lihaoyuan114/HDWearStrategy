# Stratagem Hero · HDWearStrategy

[![Wear OS](https://img.shields.io/badge/Wear%20OS-3%2B-3DDC84?logo=wearos&logoColor=white)](https://www.google.com/search?q=Wear+OS)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Release](https://img.shields.io/badge/release-v1.6.1-FFD24A)](https://github.com/lihaoyuan114/HDWearStrategy/releases/tag/v1.6.1)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

**中文** · [English](README.en.md)

> 非官方粉丝作品，免费、无广告。  
> HELLDIVERS 2 及其图标、名称、指令、语音与音效版权归 Arrowhead Game Studios / Sony Interactive Entertainment 所有。

---

## 下载 · Release

所有 APK 与服务端压缩包放在 GitHub Release：

**[v1.6.1 Release](https://github.com/lihaoyuan114/HDWearStrategy/releases/tag/v1.6.1)**

| 文件 | 包名 | 大小 |
|---|---|---|
| [中文 APK](https://github.com/lihaoyuan114/HDWearStrategy/releases/download/v1.6.1/StratagemHero-Watch4-v1.6.1-zh.apk) | `com.deepsea.stratagemhero` | 2,574,107 B |
| [English APK](https://github.com/lihaoyuan114/HDWearStrategy/releases/download/v1.6.1/StratagemHero-Watch4-v1.6.1-en.apk) | `com.deepsea.stratagemhero.en` | 2,574,107 B |
| [键盘服务端 ZIP](https://github.com/lihaoyuan114/HDWearStrategy/releases/download/v1.6.1/stratagem-server-v1.0.0.zip) | Windows + Linux | 2.5 MB |

两个 APK 包名不同，可同时安装。版本 `1.6.1` / `versionCode 9`。

```
中文 APK   SHA-256 7ea0c3403071d597615a42f69016fe1d253a6b91430f4d6df12b83f492ebe055
English    SHA-256 d05171b0811f6256a23edfb82185b768db49d84b4e8d2ca1a552fabcab2a90d3
Server ZIP SHA-256 07a62628dc075571bfa6de7e52dd992100ed8acb52a613971650eea4a3612600
```

---

## 功能

- **标准演练**：每组 5 个随机战备，顶部组进度条，组末结算；组号越大进度条越快。
- **自由呼叫**：纯空白、不计时、无分数/连击；匹配指令后播放游戏语音与战备球音效。
- **键盘模式**：局域网配对码连接 PC，箭头和 CTRL 直接映射为键盘。
- **圆屏 / 方屏**：圆屏保留仪表环；方屏使用全屏圆角面板和四边按键。
- **中文 / English 独立版本**：不同包名，可同时安装。
- **游戏原声**：输入、确认、失败、开始、组完成/失败、Stratagem Hero BGM，以及分类 Mission Control 语音。

---

## 界面

### 圆屏

| 主菜单 | 标准组进度 | 组结算 |
|---|---|---|
| ![menu](preview/render/01_menu.png) | ![standard](preview/render/02_game_resupply.png) | ![group](preview/render/15_group_clear.png) |

| 自由空白 | 输入残影 | 自由揭晓 |
|---|---|---|
| ![free](preview/render/12_free_game.png) | ![input](preview/render/12b_free_input.png) | ![reveal](preview/render/13_free_reveal.png) |

### 方屏

| 450 方屏菜单 | 450 方屏游戏 | 396 方屏菜单 |
|---|---|---|
| ![square menu](preview/render/18_menu_square.png) | ![square game](preview/render/19_game_square.png) | ![square 396](preview/render/20_menu_square_396.png) |

### 键盘模式实机

| 配对码 | 已连接 | CTRL 锁定 |
|---|---|---|
| ![pair](preview/device/v150_pair.png) | ![linked](preview/device/v150_remote_click.png) | ![ctrl](preview/device/v150_remote_ctrl.png) |

键盘暂停 / 重新配对：[v150_remote_pause.png](preview/device/v150_remote_pause.png)

---

## 玩法

### 标准演练
- 5 个一组，第一组第一个固定补给；顶部进度条 30s → 28s → 26s … 最低 14s。
- 组末结算：本组得分、总分、最高连击；继续/退出。
- 错一个方向清空并扣 1.2 秒；时间耗尽结束。标准模式播放 BGM。

### 自由呼叫
- 不显示分数、连击、呼叫次数、标题或空输入线；只留暂停和按键。
- 不计时、无倒计时、输错不扣时；揭晓无分数，内部也不计分。

### 键盘模式
- 主菜单 → **键盘** → **连接电脑**；手表显示 6 位配对码。
- PC 服务端输入该码；连接后显示 LINKED。
- 方向 → W/S/A/D（可配置）；左下角 **CTRL** 点一下按下、再点一下抬起。
- 暂停/退出/断线自动释放 Ctrl；暂停菜单可继续、重新配对、退出。

### 输入反馈
- 半透明方向残影约 14px / 0.45s 淡出。
- 圆屏点击用外圈弧键；方屏点击用四边条键；滑动模式不显示假按钮。
- 左右阈值约 16px，垂直约 24px，系统取消触摸时按位移补偿。
- **右滑兼容**：API 29/30 只使用左右各 98dp 的手势排除区（避免超过 200dp 限制导致整组失效）；API 31+ 使用全屏排除区；即使系统把滑动识别成返回，也会用 `ACTION_CANCEL` / Back 回调补偿方向。

---

## 键盘服务端

下载 [stratagem-server-v1.0.0.zip](https://github.com/lihaoyuan114/HDWearStrategy/releases/download/v1.6.1/stratagem-server-v1.0.0.zip)，或查看 [服务端说明](server/README.md)。

- Windows：`stratagem-server-windows-amd64.exe`
- Linux：`stratagem-server-linux-amd64`（uinput，需要 root/udev 权限）
- 默认 `Up=W`, `Down=S`, `Left=A`, `Right=D`, `Menu=LEFTCTRL`
- 配置 `server-config.json`，或命令行 `--up/--down/--left/--right/--menu`
- UDP 45891 配对，TCP 45892 控制；同一 Wi-Fi/网段；Windows 需放行防火墙
- `--dry-run` 只测试网络/映射；`--auto-pair` 跳过手动确认

---

## 安装

下载 APK 后：

```bash
adb install -r StratagemHero-Watch4-v1.6.1-zh.apk
adb install -r StratagemHero-Watch4-v1.6.1-en.apk
```

```bash
adb shell am start -n com.deepsea.stratagemhero/com.deepsea.stratagemhero.MainActivity
adb shell am start -n com.deepsea.stratagemhero.en/com.deepsea.stratagemhero.MainActivity
```

---

## 构建与测试

需要 Android SDK 34、Build-Tools 34.0.0、JDK 17。

```bash
./build.sh both     # 中文 + English
./build.sh zh
./build.sh en
./server/build-server.sh
```

- 纯逻辑测试：`# PASS: 710 checks`
- Robolectric：450 圆屏、396 圆屏、450 方屏、396 方屏、英文 UI、五段菜单。
- 实机 v1.5.0（SM-R890 / Android 14）：配对码、LINKED、CTRL 按下/抬起、方向 → D / ↓ → S、服务端 dry-run 已验证。
- v1.6.1 双语/方屏包已完成构建与渲染回归；实机在 v1.5.0 测试后离线，未覆盖安装 v1.6.1。
- 服务端 Windows/Linux 已交叉编译；实际注入需要 Windows 前台/管理员或 Linux `/dev/uinput` 权限。

---

## 素材与声明

- 图标：[igromanru/Helldivers-2-Stratagems-icons-png](https://github.com/igromanru/Helldivers-2-Stratagems-icons-png)
- 名称/指令：[Helldivers Wiki](https://helldivers.wiki.gg/wiki/Stratagems)
- 音频/语音：[aido727/HD2-buttons-soundboard-app](https://github.com/aido727/HD2-buttons-soundboard-app)、[BubbleDuckyBro/helldivers-2-stratagem-hero-audio-files](https://github.com/BubbleDuckyBro/helldivers-2-stratagem-hero-audio-files)
- 代码 MIT（见 [LICENSE](LICENSE)）；游戏素材/音频不适用 MIT。
- 与 Arrowhead Game Studios、Sony Interactive Entertainment、Samsung 无关联。
