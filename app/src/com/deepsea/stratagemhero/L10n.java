package com.deepsea.stratagemhero;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/** Build-variant localisation. Chinese package = zh, *.en package = English. */
final class L10n {
    static boolean en;

    private static final Map<String, String> EN = new HashMap<>();

    static {
        // Menu
        put("战略配备英雄", "TACTICAL TRAINER");
        put("最高分", "BEST");
        put("历史最高", "BEST SCORE");
        put("标准", "STD");
        put("自由", "FREE");
        put("键盘", "KEY");
        put("点击", "TAP");
        put("滑动", "SWIPE");
        put("标准：先看战备，再输入", "STANDARD: SEE IT, THEN INPUT");
        put("自由：空白输入，匹配呼叫", "FREE: BLANK INPUT, MATCH A CALL-IN");
        put("键盘：连接电脑，映射按键", "KEYBOARD: LINK A PC, MAP KEYS");
        put("开 始 训 练", "START TRAINING");
        put("连 接 电 脑", "CONNECT PC");

        // Countdown / game
        put("战术部署", "TACTICAL DEPLOYMENT");
        put("准备输入", "GET READY");
        put("任务配备", "MISSION");
        put("支援武器", "SUPPORT");
        put("进攻配备", "OFFENSIVE");
        put("背包", "BACKPACK");
        put("哨戒炮", "SENTRY");
        put("防御阵地", "DEFENSIVE");
        put("部署成功", "DEPLOYED");
        put("序列中断", "SEQUENCE LOST");
        put("无匹配指令", "NO MATCHING CODE");
        put("连击", "COMBO");
        put("战备呼叫确认", "CALL-IN CONFIRMED");
        put("自由模式不计时，也不会扣时间。", "Free mode is untimed and has no time penalty.");
        put("自由：空白输入，匹配战备即呼叫。", "Free: blank input; a full code calls in.");
        put("标准：5 个一组，组末结算分数。", "Standard: 5 per group; score on clear.");
        put("标准：错误输入清空并扣 1.2 秒。", "Standard: a wrong arrow clears and costs 1.2s.");

        // Pause
        put("战术暂停", "TACTICAL PAUSE");
        put("得分", "SCORE");
        put("剩余", "LEFT");
        put("音效反馈", "SOUND");
        put("开", "ON");
        put("关", "OFF");
        put("继 续 训 练", "RESUME");
        put("重 新 开 始", "RESTART");
        put("退 出 任 务", "QUIT");

        // Results
        put("训 练 结 束", "TRAINING COMPLETE");
        put("本局得分", "FINAL SCORE");
        put("命中", "ACCURACY");
        put("完成", "COMPLETED");
        put("★  新 纪 录  ★", "★  NEW RECORD  ★");
        put("再 次 部 署", "REDEPLOY");
        put("返 回 主 菜 单", "MAIN MENU");
        put("本组得分", "GROUP SCORE");
        put("总分", "TOTAL");
        put("最高连击", "BEST COMBO");
        put("下一组：进度条更快", "NEXT GROUP: FASTER BAR");
        put("继 续 行 动", "CONTINUE");
        put("退 出 训 练", "EXIT");

        // Help
        put("作战手册", "FIELD MANUAL");
        put("点击 / 滑动：输入箭头。", "Tap / swipe: enter arrows.");
        put("标准：先看战备，再输入。", "Standard: see the stratagem first.");
        put("返回手势已屏蔽；点顶部按钮暂停。", "Back gesture disabled; tap top to pause.");
        put("非官方粉丝作品 · 素材版权归原作者", "Unofficial fan project · assets belong to their owners");

        // Keyboard mode
        put("键盘暂停", "KEYBOARD PAUSE");
        put("电脑键盘桥接", "PC KEYBOARD BRIDGE");
        put("手 表 配 对 码", "WATCH PAIRING CODE");
        put("在电脑服务端输入此码确认", "Enter this code on the PC server");
        put("同一 Wi-Fi · 服务端需放行 UDP/TCP", "Same Wi-Fi · allow UDP/TCP on the server");
        put("等待电脑确认配对码…", "Waiting for PC confirmation…");
        put("正在搜索电脑…", "Searching for PC…");
        put("正在准备…", "Preparing…");
        put("正在连接上次的电脑…", "Connecting to last PC…");
        put("上次的电脑未响应，开始配对…", "Last PC did not respond; pairing…");
        put("已配对，正在连接…", "Paired; connecting…");
        put("连接失败，重新搜索…", "Connection failed; searching…");
        put("发送失败", "Send failed");
        put("电脑连接已断开", "PC disconnected");
        put("网络错误：", "Network error: ");
        put("已连接", "LINKED");
        put("连接断开", "DISCONNECTED");
        put("暂停后可重新配对", "Pause to re-pair");
        put("电脑未连接", "PC not connected");
        put("菜单", "MENU");
        put("继 续 控 制", "RESUME CONTROL");
        put("重 新 配 对", "RE-PAIR");
        put("退 出 模 式", "EXIT MODE");
        put("未连接 · 可重新配对", "OFFLINE · RE-PAIR");
        put("重 新 生 成", "NEW CODE");
        put("返 回 主 菜 单", "MAIN MENU");
    }

    private static void put(String zh, String enText) {
        EN.put(zh, enText);
    }

    static void init(Context context) {
        en = context.getPackageName().endsWith(".en");
    }

    static String t(String zh) {
        if (!en) return zh;
        String value = EN.get(zh);
        return value == null ? zh : value;
    }

    static String name(Stratagem s) {
        return en ? s.name : s.nameZh;
    }

    static String category(String category) {
        return en ? categoryEn(category) : categoryZh(category);
    }

    static String categoryZh(String category) {
        if (StratagemCatalog.SUPPORT.equals(category)) return "支援武器";
        if (StratagemCatalog.OFFENSIVE.equals(category)) return "进攻配备";
        if (StratagemCatalog.BACKPACK.equals(category)) return "背包";
        if (StratagemCatalog.SENTRY.equals(category)) return "哨戒炮";
        if (StratagemCatalog.DEFENSIVE.equals(category)) return "防御阵地";
        return "任务配备";
    }

    static String categoryEn(String category) {
        if (StratagemCatalog.SUPPORT.equals(category)) return "SUPPORT";
        if (StratagemCatalog.OFFENSIVE.equals(category)) return "OFFENSIVE";
        if (StratagemCatalog.BACKPACK.equals(category)) return "BACKPACK";
        if (StratagemCatalog.SENTRY.equals(category)) return "SENTRY";
        if (StratagemCatalog.DEFENSIVE.equals(category)) return "DEFENSIVE";
        return "MISSION";
    }

    static String groupClear(int group) {
        return en ? "GROUP " + String.format(java.util.Locale.US, "%02d", group) + " CLEAR"
                : "第  " + String.format(java.util.Locale.US, "%02d", group) + "  组完成";
    }
}
