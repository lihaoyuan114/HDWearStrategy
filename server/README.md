# Stratagem Hero LAN Keyboard Bridge

把 Galaxy Watch4 的箭头 / CTRL 输入映射成电脑键盘。标准模式、自由模式和新的
「键盘」模式共用同一个 APK；只有「键盘」模式会连接电脑。

## 1. 启动服务端

### Windows

双击或在 PowerShell 中运行：

```powershell
.\stratagem-server-windows-amd64.exe
```

首次运行时 Windows 防火墙会询问网络权限，需要允许**专用网络**。
如果《绝地潜兵 2》以管理员身份运行，服务端也要以管理员身份运行。

### Linux

Linux 后端使用 `/dev/uinput` 创建虚拟键盘，因此需要 root 或 udev 权限：

```bash
sudo ./stratagem-server-linux-amd64
```

只想先测试配对/网络、不注入按键：

```bash
./stratagem-server-linux-amd64 --dry-run
```

程序会打印本机 IP、UDP 45891、TCP 45892 和当前映射。

## 2. 配对

1. 手表主菜单选择 **键盘** → **连接电脑**。
2. 手表显示 6 位配对码。
3. 服务端会打印检测到的手表和同一个配对码。
4. 在服务端终端输入手表上的 6 位数字并回车。
5. 手表显示 **LINKED**，服务端显示 `[LINK] 已连接`。

如果不想每次输入，可加 `--auto-pair`，但同一局域网内任何人都能连接。

## 3. 操作

- 四个方向：点击或滑动，服务端分别按一下对应键。
- 左下角 **CTRL**：点一下发送 `menu` 键 **按下**，再点一下发送 **抬起**；
  默认是左 Ctrl，用来打开/关闭游戏里的战备菜单。
- 右上角暂停：可继续、重新配对或退出。
- 暂停 / 退出 / 断线时会自动释放 Ctrl，避免卡键。

默认映射（和游戏 PC 默认方向键一致）：

| 手表动作 | 默认键 |
|---|---|
| Up | W |
| Down | S |
| Left | A |
| Right | D |
| 战备菜单 | Left Ctrl |

## 4. 修改映射

编辑 `server-config.json`：

```json
{
  "udp_port": 45891,
  "tcp_port": 45892,
  "auto_pair": false,
  "key_hold_ms": 18,
  "bindings": {
    "up": "W",
    "down": "S",
    "left": "A",
    "right": "D",
    "menu": "LEFTCTRL"
  }
}
```

也可以用命令行覆盖，例如：

```bash
./stratagem-server-linux-amd64 --up UP --down DOWN --left LEFT --right RIGHT --menu LEFTCTRL
./stratagem-server-windows-amd64.exe --menu RIGHTCTRL
```

`--list-keys` 可列出支持的键名。改完配置后重启服务端生效。

## 5. 常见问题

- **手表搜不到电脑**：确认手表和电脑在同一 Wi-Fi/网段；允许 Windows 防火墙的
  UDP 45891、TCP 45892；Linux 防火墙同理。公司/酒店 Wi-Fi 常禁止广播。
- **Linux 没有 /dev/uinput**：用 `sudo` 运行，或配置 udev 规则让当前用户可写。
- **按键进了聊天框/无效**：先点一下游戏窗口让它获得前台焦点；管理员权限游戏
  需要管理员权限服务端。
- **Wayland**：uinput 在 X11/Wayland 下都工作；`--dry-run` 可先验证网络。
- **配对码**：每次重新生成会换码；服务端必须输入手表当前显示的码。

## 6. 协议（便于排查）

- 手表 → UDP 45891 广播：`HD2PAIR1|<6位码>|<设备名>`
- 服务端 → 手表：`HD2PAIR1_OK|<token>|<tcp端口>`
- 手表 → TCP：`HD2HELLO1|<token>|<设备名>`
- 控制：`DIR|U/D/L/R`、`MENU|1/0`、`PING`
- 服务端 token 保存在内存；服务端重启后手表需要重新配对。
