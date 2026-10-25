package com.deepsea.stratagemhero;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Entire watch UI, drawn in a 450 x 450 design space and scaled to the round display.
 * The layout deliberately keeps content inside a 225 px radius safe circle.
 */
public final class GameView extends View {
    private static final float CX = 225f;
    private static final float CY = 225f;
    private static final float RAD = 225f;
    private static final Direction[] DIRS = Direction.values();

    // Helldivers-inspired console palette.
    private static final int C_BG = 0xFF030607;
    private static final int C_PANEL_LIGHT = 0xFF172225;
    private static final int C_CREAM = 0xFFF1EEDC;
    private static final int C_MUTED = 0xFF9AA69D;
    private static final int C_DIM = 0xFF66736C;
    private static final int C_GOLD = 0xFFFFD24A;
    private static final int C_GOLD_SOFT = 0xFFFFE7A1;
    private static final int C_GREEN = 0xFF9BE58B;
    private static final int C_BLUE = 0xFF75D6FF;
    private static final int C_CORAL = 0xFFFF8E7A;
    private static final int C_RED = 0xFFFF5D52;
    private static final int C_BLACK = 0xFF050808;

    private static final int CAT_MISSION = C_GOLD;
    private static final int CAT_SUPPORT = C_BLUE;
    private static final int CAT_OFFENSIVE = C_CORAL;
    private static final int CAT_BACKPACK = C_GREEN;
    private static final int CAT_SENTRY = C_GREEN;
    private static final int CAT_DEFENSIVE = C_GREEN;

    private static final RectF R_MENU_MODE = new RectF(118f, 304f, 332f, 340f);
    private static final RectF R_MENU_START = new RectF(122f, 370f, 328f, 415f);
    private static final RectF R_PAUSE_SOUND = new RectF(126f, 190f, 324f, 226f);
    private static final RectF R_PAUSE_RESUME = new RectF(126f, 244f, 324f, 284f);
    private static final RectF R_PAUSE_RESTART = new RectF(126f, 294f, 324f, 334f);
    private static final RectF R_PAUSE_QUIT = new RectF(126f, 344f, 324f, 384f);
    private static final RectF R_OVER_RETRY = new RectF(127f, 316f, 323f, 357f);
    private static final RectF R_OVER_MENU = new RectF(127f, 369f, 323f, 410f);
    private static final RectF R_GROUP_CONTINUE = new RectF(127f, 316f, 323f, 357f);
    private static final RectF R_GROUP_EXIT = new RectF(127f, 369f, 323f, 410f);
    private static final RectF R_HELP_BACK = new RectF(137f, 359f, 313f, 399f);
    private static final RectF R_PAIR_REGENERATE = new RectF(127f, 316f, 323f, 357f);
    private static final RectF R_PAIR_BACK = new RectF(127f, 369f, 323f, 410f);
    private static final RectF R_REMOTE_CTRL = new RectF(58f, 334f, 138f, 376f);
    private static final RectF ARC_RECT = new RectF(27f, 27f, 423f, 423f);
    private static final RectF RIM_OUTER = new RectF(13f, 13f, 437f, 437f);
    private static final RectF RIM_INNER = new RectF(41f, 41f, 409f, 409f);

    private enum Screen {
        MENU, COUNTDOWN, PLAYING, PAUSED, OVER, GROUP_RESULT, HELP,
        KEYBOARD_PAIR, REMOTE
    }

    private GameSession session;
    private final SharedPreferences prefs;
    private final SoundHaptics feedback;
    private final AssetManager assets;
    private final WifiManager.WifiLock wifiLock;
    private final Map<String, Bitmap> iconCache = new HashMap<>();

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint dangerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Typeface display = Typeface.create("sans-serif-condensed", Typeface.BOLD);
    private final Typeface body = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Typeface bodyMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface mono = Typeface.create("monospace", Typeface.BOLD);

    private final Path arrowPath = new Path();
    private final Path tmpPath = new Path();
    private final Path clipPath = new Path();
    private final RectF tmpRect = new RectF();

    private Screen screen = Screen.MENU;
    private Screen pauseReturn = Screen.PLAYING;
    private boolean swipeMode;
    private boolean squareMode;
    private boolean freeCallMode;
    private boolean remoteMode;
    private boolean soundOn;
    private int bestScore;

    private KeyboardClient keyboard;
    private String remoteCode = "";
    private String remoteStatus = "";
    private String remoteEndpoint = "";
    private boolean remoteConnected;
    private boolean remoteMenuDown;

    private final KeyboardClient.Listener lanListener = new KeyboardClient.Listener() {
        @Override
        public void onStatus(String text) {
            remoteStatus = text == null ? "" : text;
            invalidate();
        }

        @Override
        public void onConnected(String endpoint, String token) {
            remoteConnected = true;
            remoteEndpoint = endpoint;
            remoteStatus = "已连接";
            remoteMenuDown = false;
            screen = Screen.REMOTE;
            pauseReturn = Screen.REMOTE;
            String host = endpoint;
            int port = 45892;
            int split = endpoint.lastIndexOf(':');
            if (split > 0) {
                host = endpoint.substring(0, split);
                try {
                    port = Integer.parseInt(endpoint.substring(split + 1));
                } catch (NumberFormatException ignored) {}
            }
            prefs.edit().putString("remote_host", host)
                    .putInt("remote_port", port)
                    .putString("remote_token", token).apply();
            lastFrame = SystemClock.uptimeMillis();
            refreshGestureExclusion();
            feedback.tick();
            postInvalidateOnAnimation();
        }

        @Override
        public void onDisconnected(String reason) {
            remoteConnected = false;
            remoteMenuDown = false;
            remoteStatus = reason == null ? "连接断开" : reason;
            lastFrame = SystemClock.uptimeMillis();
            postInvalidateOnAnimation();
        }
    };

    private float scale = 1f;
    private float offsetX;
    private float offsetY;
    private Bitmap backdrop;

    private long lastFrame;
    private float countdown;
    private int lastCountdownNumber = 3;
    private String activeButton = "";
    private float downX;
    private float downY;
    private Direction pressedDir;
    private long pressedAt;

    private boolean swipeTracking;
    private boolean swipeConsumed;
    private float swipeStartX;
    private float swipeStartY;
    private float trailStartX;
    private float trailStartY;
    private float trailEndX;
    private float trailEndY;
    private long trailUpdatedAt;
    private boolean trailVisible;

    private long inputLockUntil;
    private long lastInputAt;
    private Direction lastInputDir;
    private long successUntil;
    private long wrongUntil;
    private long shakeUntil;
    private Direction ghostDir;
    private long ghostAt;
    private int ghostColor = C_GOLD;

    private boolean newRecord;
    private Stratagem revealStratagem;
    private long revealStart;
    private long revealUntil;

    public GameView(Context context) {
        this(context, null);
    }

    GameView(Context context, Boolean roundOverride) {
        super(context);
        L10n.init(context);
        squareMode = roundOverride != null ? !roundOverride
                : !getResources().getConfiguration().isScreenRound();
        setBackgroundColor(C_BG);
        setFocusable(true);
        setClickable(true);
        setKeepScreenOn(true);

        prefs = context.getSharedPreferences("stratagem_hero", Context.MODE_PRIVATE);
        feedback = new SoundHaptics(context);
        assets = context.getAssets();
        WifiManager wifi = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifi == null ? null
                : wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                        "StratagemHero:lan");
        if (wifiLock != null) wifiLock.setReferenceCounted(false);
        session = new GameSession(StratagemCatalog.ALL, new Random(), freeCallMode);

        bestScore = prefs.getInt("best_score", 0);
        swipeMode = prefs.getBoolean("swipe_mode", false);
        remoteMode = prefs.getBoolean("remote_mode", false);
        freeCallMode = !remoteMode && prefs.getBoolean("free_call_mode", false);
        soundOn = prefs.getBoolean("sound_on", true);
        feedback.setEnabled(soundOn);

        buildArrowPath();
        buildDangerPaint();
        lastFrame = SystemClock.uptimeMillis();
    }

    private void buildArrowPath() {
        arrowPath.moveTo(12f, 1.6f);
        arrowPath.lineTo(22.3f, 11.9f);
        arrowPath.lineTo(16.9f, 11.9f);
        arrowPath.lineTo(16.9f, 22.4f);
        arrowPath.lineTo(7.1f, 22.4f);
        arrowPath.lineTo(7.1f, 11.9f);
        arrowPath.lineTo(1.7f, 11.9f);
        arrowPath.close();
    }

    private void buildDangerPaint() {
        dangerPaint.setShader(new RadialGradient(CX, CY, 228f,
                new int[]{0x00FF3F35, 0x00FF3F35, 0x99FF3F35},
                new float[]{0f, 0.62f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        scale = Math.min(w, h) / 450f;
        offsetX = (w - 450f * scale) * 0.5f;
        offsetY = (h - 450f * scale) * 0.5f;
        buildBackdrop();
        refreshGestureExclusion();
        lastFrame = SystemClock.uptimeMillis();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(new Runnable() {
            @Override
            public void run() {
                refreshGestureExclusion();
            }
        });
    }

    /**
     * Best-effort system back-gesture suppression. Android does not let a regular app
     * completely disable the OS gesture, but a wide exclusion band on both edges keeps
     * swipe gameplay from being stolen by the system.
     */
    void refreshGestureExclusion() {
        if (Build.VERSION.SDK_INT < 29) return;
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        float density = getResources().getDisplayMetrics().density;
        int edge = Math.min((int) (90f * density), w / 2);
        List<Rect> rects = new ArrayList<>(3);
        rects.add(new Rect(0, 0, edge, h));
        rects.add(new Rect(w - edge, 0, w, h));
        // During a run, also exclude the middle: Wear OS can claim horizontal
        // swipes as its own back gesture even when they begin away from an edge.
        if (screen == Screen.PLAYING || screen == Screen.COUNTDOWN
                || screen == Screen.REMOTE) {
            rects.add(new Rect(4, 4, w - 4, h - 4));
        }
        setSystemGestureExclusionRects(rects);
    }

    private void buildSquareBackdrop(Canvas c) {
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setShader(new LinearGradient(0f, 0f, 0f, 450f,
                new int[]{0xFF0B191A, 0xFF071012, 0xFF030607},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0f, 0f, 450f, 450f, fillPaint);
        fillPaint.setShader(null);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.4f);
        linePaint.setColor(0x667C8C82);
        c.drawRoundRect(new RectF(8f, 8f, 442f, 442f), 26f, 26f, linePaint);
        linePaint.setStrokeWidth(1f);
        linePaint.setColor(0x223F5048);
        c.drawRoundRect(new RectF(16f, 16f, 434f, 434f), 20f, 20f, linePaint);

        // Corner brackets.
        linePaint.setStrokeWidth(2f);
        linePaint.setColor(0x55FFD24A);
        c.drawLine(24f, 46f, 24f, 24f, linePaint); c.drawLine(24f, 24f, 46f, 24f, linePaint);
        c.drawLine(426f, 24f, 404f, 24f, linePaint); c.drawLine(426f, 24f, 426f, 46f, linePaint);
        c.drawLine(24f, 404f, 24f, 426f, linePaint); c.drawLine(24f, 426f, 46f, 426f, linePaint);
        c.drawLine(426f, 426f, 404f, 426f, linePaint); c.drawLine(426f, 426f, 426f, 404f, linePaint);

        // Edge ticks.
        linePaint.setStrokeWidth(1f);
        for (int i = 40; i < 450; i += 18) {
            int alpha = (i % 90 == 40) ? 0x33FFD24A : 0x1C7C8C82;
            linePaint.setColor(alpha);
            c.drawLine(i, 8f, i, (i % 90 == 40) ? 20f : 15f, linePaint);
            c.drawLine(i, 442f, i, (i % 90 == 40) ? 430f : 435f, linePaint);
            c.drawLine(8f, i, (i % 90 == 40) ? 20f : 15f, i, linePaint);
            c.drawLine(442f, i, (i % 90 == 40) ? 430f : 435f, i, linePaint);
        }

        // Scanlines.
        linePaint.setColor(0x08000000);
        for (int y = 12; y < 440; y += 5) c.drawLine(0f, y, 450f, y, linePaint);

        Random grain = new Random(8841L);
        for (int i = 0; i < 520; i++) {
            float x = grain.nextFloat() * 440f + 5f;
            float y = grain.nextFloat() * 440f + 5f;
            fillPaint.setColor((i % 5 == 0) ? 0x14FFE7A1 : 0x0DBCE0CC);
            fillPaint.setStyle(Paint.Style.FILL);
            c.drawCircle(x, y, (i % 7 == 0) ? 0.9f : 0.55f, fillPaint);
        }

        fillPaint.setShader(new RadialGradient(CX, CY, 320f,
                new int[]{0x00000000, 0x00000000, 0xB0030607},
                new float[]{0f, 0.62f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0f, 0f, 450f, 450f, fillPaint);
        fillPaint.setShader(null);

        clipPath.reset();
        clipPath.addRoundRect(new RectF(4f, 4f, 446f, 446f), 28f, 28f, Path.Direction.CW);
    }
    private void fillPanel(Canvas c, int color) {
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(color);
        if (squareMode) {
            c.drawRoundRect(new RectF(0f, 0f, 450f, 450f), 26f, 26f, fillPaint);
        } else {
            c.drawCircle(CX, CY, RAD, fillPaint);
        }
    }
    private void buildBackdrop() {
        if (backdrop != null) backdrop.recycle();
        backdrop = Bitmap.createBitmap(450, 450, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(backdrop);
        c.drawColor(C_BG);
        if (squareMode) {
            buildSquareBackdrop(c);
            return;
        }

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setShader(new RadialGradient(CX, CY - 15f, 260f,
                new int[]{0xFF0C1A1B, 0xFF071012, 0xFF030607},
                new float[]{0f, 0.62f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(CX, CY, RAD, fillPaint);
        fillPaint.setShader(null);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1f);
        linePaint.setColor(0x1E718276);
        c.drawCircle(CX, CY, 208f, linePaint);
        linePaint.setColor(0x103F5048);
        c.drawCircle(CX, CY, 180f, linePaint);
        linePaint.setColor(0x0D3F5048);
        c.drawCircle(CX, CY, 136f, linePaint);

        // Instrument bezel ticks.
        for (int i = 0; i < 72; i++) {
            float angle = (float) Math.toRadians(i * 5 - 90);
            float r1 = (i % 6 == 0) ? 211f : 216f;
            float r2 = 222f;
            linePaint.setColor((i % 6 == 0) ? 0x397C8C82 : 0x1C7C8C82);
            linePaint.setStrokeWidth((i % 6 == 0) ? 1.5f : 1f);
            c.drawLine(CX + (float) Math.cos(angle) * r1, CY + (float) Math.sin(angle) * r1,
                    CX + (float) Math.cos(angle) * r2, CY + (float) Math.sin(angle) * r2,
                    linePaint);
        }

        // Sparse console scanlines.
        linePaint.setStrokeWidth(1f);
        linePaint.setColor(0x08000000);
        for (int y = 12; y < 440; y += 5) {
            c.drawLine(0f, y, 450f, y, linePaint);
        }

        // A few illuminated bezel arcs.
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.6f);
        linePaint.setColor(0x55FFD24A);
        c.drawArc(new RectF(3f, 3f, 447f, 447f), 222f, 96f, false, linePaint);
        linePaint.setColor(0x2AFFD24A);
        c.drawArc(new RectF(3f, 3f, 447f, 447f), 52f, 24f, false, linePaint);
        c.drawArc(new RectF(3f, 3f, 447f, 447f), 128f, 24f, false, linePaint);

        // Sparse dust for a worn military display.
        Random grain = new Random(8841L);
        for (int i = 0; i < 520; i++) {
            float rr = 6f + grain.nextFloat() * 214f;
            float aa = grain.nextFloat() * 6.2831853f;
            float x = CX + (float) Math.cos(aa) * rr;
            float y = CY + (float) Math.sin(aa) * rr;
            fillPaint.setColor((i % 5 == 0) ? 0x14FFE7A1 : 0x0DBCE0CC);
            fillPaint.setStyle(Paint.Style.FILL);
            c.drawCircle(x, y, (i % 7 == 0) ? 0.9f : 0.55f, fillPaint);
        }

        // Vignette.
        fillPaint.setShader(new RadialGradient(CX, CY, 230f,
                new int[]{0x00000000, 0x00000000, 0xC8030607},
                new float[]{0f, 0.66f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(CX, CY, RAD, fillPaint);
        fillPaint.setShader(null);

        clipPath.reset();
        if (squareMode) {
            clipPath.addRoundRect(new RectF(4f, 4f, 446f, 446f), 28f, 28f,
                    Path.Direction.CW);
        } else {
            clipPath.addCircle(CX, CY, 224f, Path.Direction.CW);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = SystemClock.uptimeMillis();
        float dt = (now - lastFrame) / 1000f;
        if (dt < 0f) dt = 0f;
        if (dt > 0.10f) dt = 0.10f;
        lastFrame = now;

        if (screen == Screen.COUNTDOWN) {
            countdown -= dt;
            int n = (int) Math.ceil(countdown);
            if (n >= 1 && n <= 3 && n != lastCountdownNumber) {
                lastCountdownNumber = n;
                feedback.countdown(n == 1);
            }
            if (countdown <= 0f) {
                countdown = 0f;
                screen = Screen.PLAYING;
                feedback.countdown(true);
                lastFrame = now;
            }
        }

        if (revealStratagem != null && now >= revealUntil) {
            revealStratagem = null;
        }
        boolean showingReveal = freeCallMode && revealStratagem != null;

        if (screen == Screen.PLAYING) {
            if (!showingReveal) {
                session.update(dt);
                if (session.isOver()) finishRun(now);
            }
        }

        canvas.drawColor(C_BG);
        float shakeX = 0f;
        float shakeY = 0f;
        if (now < shakeUntil) {
            float k = (shakeUntil - now) / 300f;
            shakeX = (float) Math.sin(now * 0.085f) * 7f * k;
            shakeY = (float) Math.cos(now * 0.113f) * 5f * k;
        }

        canvas.save();
        canvas.translate(offsetX + shakeX * scale, offsetY + shakeY * scale);
        canvas.scale(scale, scale);
        canvas.clipPath(clipPath);
        if (backdrop != null) canvas.drawBitmap(backdrop, 0f, 0f, null);

        switch (screen) {
            case MENU:
                drawMenu(canvas, now);
                break;
            case COUNTDOWN:
                drawGame(canvas, now, true);
                drawCountdown(canvas, now);
                break;
            case PLAYING:
                drawGame(canvas, now, true);
                if (showingReveal) drawReveal(canvas, now);
                break;
            case GROUP_RESULT:
                drawGame(canvas, now, false);
                drawGroupResult(canvas, now);
                break;
            case PAUSED:
                if (pauseReturn == Screen.REMOTE) {
                    drawRemote(canvas, now, false);
                    drawRemotePause(canvas, now);
                } else {
                    drawGame(canvas, now, false);
                    drawPause(canvas, now);
                }
                break;
            case KEYBOARD_PAIR:
                drawPairing(canvas, now);
                break;
            case REMOTE:
                drawRemote(canvas, now, true);
                break;
            case OVER:
                drawGame(canvas, now, false);
                drawGameOver(canvas, now);
                break;
            case HELP:
                drawMenu(canvas, now);
                drawHelp(canvas, now);
                break;
        }

        canvas.restore();

        if (screen == Screen.PLAYING || screen == Screen.COUNTDOWN
                || screen == Screen.REMOTE) {
            postInvalidateOnAnimation();
        } else {
            postInvalidateDelayed(32L);
        }
    }

    // ---------------------------------------------------------------------
    // Menu
    // ---------------------------------------------------------------------

    private void drawMenu(Canvas c, long now) {
        drawBezelButton(c, 105f, 74f, "sound", "sound".equals(activeButton));
        drawBezelButton(c, 345f, 74f, "help", "help".equals(activeButton));

        float rotation = (now % 9000L) / 9000f * 360f;
        drawIconFrame(c, CX, 120f, 47f,
                iconFor("reinforce"), CAT_MISSION, rotation);

        text(c, "STRATAGEM", CX, 203f, 44f, C_CREAM, display,
                Paint.Align.CENTER, 0.10f);
        text(c, "HERO", CX, 250f, 62f, C_GOLD, display,
                Paint.Align.CENTER, 0.16f);
        text(c, L10n.t("战略配备英雄"), CX, 273f, 19.5f, C_MUTED, bodyMedium,
                Paint.Align.CENTER, 0.06f);

        drawHighScore(c, 294f);
        drawModeSwitch(c, R_MENU_MODE, 16.5f);

        String hint = L10n.t(remoteMode
                ? "键盘：连接电脑，映射按键"
                : (freeCallMode ? "自由：空白输入，匹配呼叫"
                        : "标准：先看战备，再输入"));
        text(c, hint, CX, 360f, 14.5f, C_MUTED, body, Paint.Align.CENTER, 0.012f);

        boolean startPressed = "start".equals(activeButton);
        drawButton(c, R_MENU_START, L10n.t(remoteMode ? "连 接 电 脑" : "开 始 训 练"),
                true, startPressed, true);
    }

    private void drawHighScore(Canvas c, float y) {
        String value = bestScore > 0 ? pad5(bestScore) : "-----";
        textPaint.setTypeface(bodyMedium);
        textPaint.setTextSize(14f);
        textPaint.setLetterSpacing(0.02f);
        String scoreLabel = L10n.t("最高分");
        float labelWidth = textPaint.measureText(scoreLabel);
        textPaint.setTypeface(mono);
        textPaint.setTextSize(19f);
        textPaint.setLetterSpacing(0.03f);
        float valueWidth = textPaint.measureText(value);
        float gap = 10f;
        float groupWidth = labelWidth + gap + valueWidth;
        float left = CX - groupWidth * 0.5f;
        text(c, scoreLabel, left, y, 14f, C_MUTED, bodyMedium, Paint.Align.LEFT, 0.02f);
        text(c, value, left + labelWidth + gap, y, 19f,
                bestScore > 0 ? C_GOLD_SOFT : C_DIM, mono, Paint.Align.LEFT, 0.03f);
    }

    private void drawBezelButton(Canvas c, float x, float y, String kind, boolean pressed) {
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(pressed ? 0xFF25302B : 0xB30B1214);
        c.drawCircle(x, y, 16.5f, fillPaint);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1f);
        linePaint.setColor(pressed ? C_GOLD : 0x663B4A43);
        c.drawCircle(x, y, 16.5f, linePaint);

        if ("sound".equals(kind)) {
            drawSpeaker(c, x, y, soundOn, pressed ? C_GOLD : C_MUTED);
        } else {
            text(c, "?", x, y + 6.5f, 18f, pressed ? C_GOLD : C_MUTED,
                    display, Paint.Align.CENTER, 0f);
        }
    }

    private void drawSpeaker(Canvas c, float x, float y, boolean on, int color) {
        tmpPath.reset();
        tmpPath.moveTo(x - 6.5f, y - 2.5f);
        tmpPath.lineTo(x - 3f, y - 2.5f);
        tmpPath.lineTo(x + 1.5f, y - 6.5f);
        tmpPath.lineTo(x + 1.5f, y + 6.5f);
        tmpPath.lineTo(x - 3f, y + 2.5f);
        tmpPath.lineTo(x - 6.5f, y + 2.5f);
        tmpPath.close();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(color);
        c.drawPath(tmpPath, fillPaint);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.4f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setColor(color);
        if (on) {
            c.drawArc(new RectF(x + 1f, y - 6f, x + 9f, y + 6f), -58f, 116f, false, linePaint);
            c.drawArc(new RectF(x + 4f, y - 8.5f, x + 13f, y + 8.5f), -52f, 104f, false, linePaint);
        } else {
            c.drawLine(x + 4f, y - 6f, x + 12f, y + 6f, linePaint);
            c.drawLine(x + 12f, y - 6f, x + 4f, y + 6f, linePaint);
        }
    }

    private void drawModeSwitch(Canvas c, RectF r, float textSize) {
        drawChamfer(c, r, 8f, 0xCC0A1112, 0x883B4A43, 1.2f);
        float left = r.left + 2f;
        float right = r.right - 2f;
        float cell = (right - left) / 5f;
        String[] labels = {L10n.t("点击"), L10n.t("滑动"), L10n.t("标准"),
                L10n.t("自由"), L10n.t("键盘")};
        String[] ids = {"input_click", "input_swipe", "play_standard", "play_free", "play_remote"};
        boolean[] selected = {!swipeMode, swipeMode, !freeCallMode && !remoteMode,
                freeCallMode, remoteMode};

        buildChamfer(tmpPath, r, 8f);
        c.save();
        c.clipPath(tmpPath);
        for (int i = 0; i < 5; i++) {
            boolean pressed = ids[i].equals(activeButton);
            if (selected[i] || pressed) {
                fillPaint.setStyle(Paint.Style.FILL);
                fillPaint.setColor(selected[i]
                        ? (pressed ? C_GOLD_SOFT : C_GOLD)
                        : 0x33FFD24A);
                c.drawRect(left + i * cell + 1f, r.top + 1.5f,
                        left + (i + 1) * cell - 1f, r.bottom - 1.5f, fillPaint);
            }
        }
        c.restore();

        linePaint.setStyle(Paint.Style.STROKE);
        for (int i = 1; i < 5; i++) {
            boolean groupBreak = i == 2;
            linePaint.setStrokeWidth(groupBreak ? 1.4f : 1f);
            linePaint.setColor(groupBreak ? 0xAAFFD24A : 0x333B4A43);
            c.drawLine(left + i * cell, r.top + 7f,
                    left + i * cell, r.bottom - 7f, linePaint);
        }

        float labelSize = L10n.en ? 12.5f : textSize;
        for (int i = 0; i < 5; i++) {
            boolean pressed = ids[i].equals(activeButton);
            int color = selected[i] ? C_BLACK : (pressed ? C_CREAM : C_MUTED);
            text(c, labels[i], left + (i + 0.5f) * cell,
                    r.centerY() + labelSize * 0.36f, labelSize, color,
                    bodyMedium, Paint.Align.CENTER, 0.01f);
        }
    }

    private void drawIconFrame(Canvas c, float x, float y, float radius,
                               Bitmap icon, int accent, float rotation) {
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(0xE6070E10);
        c.drawCircle(x, y, radius, fillPaint);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.3f);
        linePaint.setColor(0x553B4A43);
        c.drawCircle(x, y, radius, linePaint);

        linePaint.setStrokeWidth(2.1f);
        linePaint.setColor(0xAAFFD24A);
        c.drawArc(new RectF(x - radius - 4f, y - radius - 4f,
                        x + radius + 4f, y + radius + 4f),
                rotation, 43f, false, linePaint);
        linePaint.setColor(0x55FFD24A);
        c.drawArc(new RectF(x - radius - 4f, y - radius - 4f,
                        x + radius + 4f, y + radius + 4f),
                rotation + 180f, 27f, false, linePaint);

        // Corner brackets.
        linePaint.setStrokeWidth(1.4f);
        linePaint.setColor(0x66FFE7A1);
        float s = radius * 0.70f;
        float tick = 8f;
        c.drawLine(x - s, y - s, x - s + tick, y - s, linePaint);
        c.drawLine(x - s, y - s, x - s, y - s + tick, linePaint);
        c.drawLine(x + s, y + s, x + s - tick, y + s, linePaint);
        c.drawLine(x + s, y + s, x + s, y + s - tick, linePaint);

        if (icon != null) {
            float size = radius * 1.62f;
            c.drawBitmap(icon, null,
                    new RectF(x - size * 0.5f, y - size * 0.5f,
                            x + size * 0.5f, y + size * 0.5f), iconPaint);
        }
    }

    // ---------------------------------------------------------------------
    // Game HUD and target
    // ---------------------------------------------------------------------

    private void drawGame(Canvas c, long now, boolean controls) {
        if (controls && !swipeMode) {
            drawClickControls(c, now);
        }
        drawHud(c, now);
        if (freeCallMode) {
            drawFreeCall(c, now);
        } else {
            Stratagem current = session.getCurrent();
            drawTargetCard(c, current, now);
            drawSequence(c, current, now);
        }

        float time = session.getTimeLeft();
        if (now < successUntil) {
            text(c, L10n.t("部署成功") + "   +" + session.getLastGain(),
                    CX, 315f, 19f, C_GREEN, bodyMedium, Paint.Align.CENTER, 0.015f);
        } else if (now < wrongUntil) {
            text(c, freeCallMode ? L10n.t("无匹配指令")
                            : L10n.t("序列中断") + "   -1.2s",
                    CX, 315f, 19f, C_RED, bodyMedium,
                    Paint.Align.CENTER, 0.02f);
        } else if (session.getCombo() > 0) {
            text(c, L10n.t("连击") + "  x" + twoDigits(session.getCombo()), CX, 315f, 19f,
                    C_GOLD, bodyMedium, Paint.Align.CENTER, 0.06f);
        }

        drawEffects(c, now, time);
        drawInputGhost(c, now);
    }

    private void drawInputGhost(Canvas c, long now) {
        if (ghostDir == null) return;
        long age = now - ghostAt;
        if (age < 0L || age > 450L) return;
        float k = 1f - age / 450f;
        float distance = 14f * (1f - k);
        float dx = 0f;
        float dy = 0f;
        switch (ghostDir) {
            case UP: dy = -distance; break;
            case RIGHT: dx = distance; break;
            case DOWN: dy = distance; break;
            default: dx = -distance; break;
        }
        drawArrow(c, ghostDir, CX + dx, 300f + dy, 34f,
                withAlpha(ghostColor, (int) (150 * k)));
    }

    private void drawReveal(Canvas c, long now) {
        if (revealStratagem == null) return;
        float t = (now - revealStart) / 2600f;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        int accent = categoryColor(revealStratagem.category);
        float appear = Math.min(1f, t * 9f);

        fillPanel(c, 0xFF040809);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.2f);
        linePaint.setColor(withAlpha(accent, (int) (70 * appear)));
        c.drawCircle(CX, CY, 218f, linePaint);

        // Orbital drop beam.
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(withAlpha(accent, (int) (14 * appear)));
        c.drawRect(174f, 58f, 276f, 196f, fillPaint);
        fillPaint.setColor(withAlpha(accent, (int) (26 * appear)));
        c.drawRect(195f, 58f, 255f, 196f, fillPaint);
        fillPaint.setColor(withAlpha(accent, (int) (44 * appear)));
        c.drawRect(213f, 58f, 237f, 196f, fillPaint);
        line(c, CX, 58f, CX, 132f, withAlpha(accent, (int) (125 * appear)), 1.4f);
        float scanY = 64f + 116f * ((now % 900L) / 900f);
        line(c, 185f, scanY, 265f, scanY, withAlpha(accent, (int) (80 * appear)), 1.4f);
        float wave = (now % 1000L) / 1000f;
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.2f);
        linePaint.setColor(withAlpha(accent, (int) (70 * (1f - wave) * appear)));
        c.drawCircle(CX, 184f, 34f + 82f * wave, linePaint);

        text(c, L10n.t("战备呼叫确认"), CX, 78f, 25f, C_CREAM, bodyMedium,
                Paint.Align.CENTER, 0.08f);
        text(c, L10n.en ? "DEPLOYMENT AUTHORIZED" : "CALL-IN CONFIRMED",
                CX, 96f, 10f, accent, display, Paint.Align.CENTER, 0.2f);

        float ring = 58f + 7f * (float) Math.sin(t * Math.PI * 2f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.5f);
        linePaint.setColor(withAlpha(accent, (int) (120 * appear)));
        c.drawCircle(CX, 183f, ring + 10f, linePaint);
        linePaint.setStrokeWidth(1f);
        linePaint.setColor(withAlpha(accent, (int) (55 * appear)));
        c.drawCircle(CX, 183f, ring + 21f, linePaint);

        tmpRect.set(158f, 118f, 292f, 248f);
        drawChamfer(c, tmpRect, 13f, 0xFA080F10,
                withAlpha(accent, (int) (190 * appear)), 1.6f);

        float iconSize = 106f * (0.92f + 0.08f * Math.min(1f, t * 5f));
        Bitmap icon = iconFor(revealStratagem.assetName);
        if (icon != null) {
            c.drawBitmap(icon, null,
                    new RectF(CX - iconSize * 0.5f, 183f - iconSize * 0.5f,
                            CX + iconSize * 0.5f, 183f + iconSize * 0.5f), iconPaint);
        }

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.5f);
        linePaint.setColor(withAlpha(accent, (int) (175 * appear)));
        c.drawLine(148f, 118f, 170f, 118f, linePaint);
        c.drawLine(148f, 118f, 148f, 140f, linePaint);
        c.drawLine(302f, 248f, 280f, 248f, linePaint);
        c.drawLine(302f, 248f, 302f, 226f, linePaint);

        drawTextFit(c, revealStratagem.name, CX, L10n.en ? 292f : 276f,
                25f, C_CREAM, display, Paint.Align.CENTER, 0.02f, 274f);
        if (!L10n.en) {
            drawTextFit(c, revealStratagem.nameZh, CX, 297f, 16.5f, C_MUTED,
                    bodyMedium, Paint.Align.CENTER, 0.01f, 268f);
        }
        text(c, L10n.en ? L10n.category(revealStratagem.category)
                        : categoryZh(revealStratagem.category) + "  //  "
                        + categoryEn(revealStratagem.category),
                CX, 315f, 11f, accent, display, Paint.Align.CENTER, 0.08f);

        text(c, "STRATAGEM INBOUND", CX, 346f, 10f, accent, display,
                Paint.Align.CENTER, 0.2f);
    }
    private void drawPauseButton(Canvas c) {
        boolean pressed = "pause".equals(activeButton);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(pressed ? 0xFF25302B : 0x550A1112);
        c.drawCircle(CX, 86f, 13f, fillPaint);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1f);
        linePaint.setColor(pressed ? C_GOLD : 0x663B4A43);
        c.drawCircle(CX, 86f, 13f, linePaint);
        fillPaint.setColor(pressed ? C_GOLD : C_MUTED);
        c.drawRect(CX - 4.5f, 81f, CX - 1.5f, 91f, fillPaint);
        c.drawRect(CX + 1.5f, 81f, CX + 4.5f, 91f, fillPaint);
    }
    private void drawGroupBar(Canvas c, long now) {
        float time = session.getTimeLeft();
        float frac = session.getGroupTimeFraction();
        boolean low = time <= 6f;
        int barColor = low ? blend(C_RED, C_GOLD,
                (0.6f + 0.4f * (float) Math.sin(now * 0.018f)) * 0.35f) : C_GOLD;
        String label = "GROUP " + twoDigits(session.getGroupNumber())
                + "   ·   " + session.getGroupDone() + " / " + session.getGroupSize();
        text(c, label, CX, 52f, 9.5f, low ? C_RED : C_GOLD, display,
                Paint.Align.CENTER, 0.14f);

        float left = 150f;
        float right = 300f;
        float y = 57f;
        linePaint.setStyle(Paint.Style.FILL);
        linePaint.setColor(0x6634413C);
        c.drawRect(left, y, right, y + 5f, linePaint);
        linePaint.setColor(barColor);
        c.drawRect(left, y, left + (right - left) * frac, y + 5f, linePaint);
        linePaint.setColor(0x553B4A43);
        for (int i = 1; i < 5; i++) {
            float x = left + (right - left) * i / 5f;
            c.drawRect(x, y, x + 1f, y + 5f, linePaint);
        }
    }

    private void drawHud(Canvas c, long now) {
        if (freeCallMode) {
            if (screen == Screen.PLAYING) drawPauseButton(c);
            return;
        }
        float time = session.getTimeLeft();
        drawGroupBar(c, now);

        text(c, pad5(session.getScore()), 105f, 86f, 28f, C_CREAM,
                mono, Paint.Align.LEFT, 0.02f);
        text(c, "SCORE", 105f, 104f, 10.5f, 0xAA9AA69D, display,
                Paint.Align.LEFT, 0.2f);

        int timeColor = time <= 10f ? C_RED : (time <= 20f ? C_GOLD : C_CREAM);
        if (time <= 10f) {
            float pulse = 0.65f + 0.35f * (float) Math.sin(now * 0.018f);
            timeColor = blend(C_RED, C_GOLD, pulse * 0.35f);
        }
        text(c, String.format(Locale.US, "%.1f", time) + "s", 345f, 86f, 27f,
                timeColor, mono, Paint.Align.RIGHT, 0.015f);
        text(c, "TIME", 345f, 104f, 10.5f, 0xAA9AA69D, display,
                Paint.Align.RIGHT, 0.2f);

        if (screen == Screen.PLAYING) {
            drawPauseButton(c);
        }
    }

    private void drawTargetCard(Canvas c, Stratagem s, long now) {
        if (s == null) return;
        int accent = categoryColor(s.category);
        tmpRect.set(104f, 113f, 346f, 248f);
        drawChamfer(c, tmpRect, 11f, 0xF60B1214, (accent & 0x00FFFFFF) | 0x55000000, 1.2f);

        // Small corner brackets.
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.5f);
        linePaint.setColor((accent & 0x00FFFFFF) | 0x88000000);
        c.drawLine(113f, 122f, 127f, 122f, linePaint);
        c.drawLine(113f, 122f, 113f, 136f, linePaint);
        c.drawLine(337f, 239f, 323f, 239f, linePaint);
        c.drawLine(337f, 239f, 337f, 225f, linePaint);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(accent);
        c.drawCircle(117f, 125f, 2.2f, fillPaint);
        text(c, L10n.en ? L10n.category(s.category)
                        : categoryZh(s.category) + "  //  " + categoryEn(s.category),
                125f, 129f, 11.5f, accent, display, Paint.Align.LEFT, 0.08f);

        String combo = session.getCombo() > 0
                ? "COMBO " + twoDigits(session.getCombo()) : "STANDBY";
        text(c, combo, 333f, 129f, 10.5f,
                session.getCombo() > 0 ? C_GOLD : C_DIM, display,
                Paint.Align.RIGHT, 0.12f);

        Bitmap icon = iconFor(s.assetName);
        if (icon != null) {
            c.drawBitmap(icon, null, new RectF(190f, 132f, 260f, 202f), iconPaint);
        }

        drawTextFit(c, s.name, CX, L10n.en ? 232f : 222f, 22f, C_CREAM,
                display, Paint.Align.CENTER, 0.02f, 232f);
        if (!L10n.en) {
            drawTextFit(c, s.nameZh, CX, 240f, 16.5f, C_MUTED, bodyMedium,
                    Paint.Align.CENTER, 0.01f, 236f);
        }

        // Progress line along the bottom of the card.
        float progress = session.getProgress();
        linePaint.setStyle(Paint.Style.FILL);
        linePaint.setColor(0x5534413C);
        c.drawRect(116f, 245f, 334f, 247f, linePaint);
        linePaint.setColor(accent);
        c.drawRect(116f, 245f, 116f + 218f * progress, 247f, linePaint);

        if (now < successUntil) {
            float age = (successUntil - now) / 650f;
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(2f);
            linePaint.setColor(withAlpha(accent, (int) (150 * age)));
            c.drawRoundRect(tmpRect, 10f, 10f, linePaint);
        }
    }

    private void drawFreeCall(Canvas c, long now) {
        int accent = C_BLUE;
        String code = session.getInputCode();
        if (code.isEmpty()) return;

        int n = code.length();
        float cellW = 34f;
        float gap = 6f;
        float total = n * cellW + (n - 1) * gap;
        float start = CX - total * 0.5f;
        for (int i = 0; i < n; i++) {
            char ch = code.charAt(i);
            Direction dir = Direction.fromCode(ch);
            float left = start + i * (cellW + gap);
            tmpRect.set(left, 255f, left + cellW, 293f);
            drawChamfer(c, tmpRect, 5f, (accent & 0x00FFFFFF) | 0xDD000000,
                    accent, 1.2f);
            drawArrow(c, dir, left + cellW * 0.5f, 274f, 20f, C_BLACK);
            if (i == n - 1) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(now * 0.014f);
                linePaint.setStyle(Paint.Style.STROKE);
                linePaint.setStrokeWidth(1.6f);
                linePaint.setColor(withAlpha(C_CREAM, (int) (70 + 110 * pulse)));
                c.drawRoundRect(tmpRect, 6f, 6f, linePaint);
            }
        }
    }
    private void drawSequence(Canvas c, Stratagem s, long now) {
        if (s == null) return;
        int accent = freeCallMode ? C_BLUE : categoryColor(s.category);
        int n = s.length();
        float cellW = 30f;
        float gap = 5f;
        float total = n * cellW + (n - 1) * gap;
        float x = CX - total * 0.5f;
        float y = 274f;
        int entered = session.getEnteredCount();

        for (int i = 0; i < n; i++) {
            boolean done = i < entered;
            boolean next = i == entered;
            float left = x + i * (cellW + gap);
            tmpRect.set(left, y - 17.5f, left + cellW, y + 17.5f);
            int border;
            int fill;
            int arrowColor;
            if (done) {
                fill = (accent & 0x00FFFFFF) | 0xD9000000;
                border = accent;
                arrowColor = C_BLACK;
            } else if (next) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(now * 0.012f);
                fill = 0xCC101A1B;
                border = blend(accent, C_CREAM, 0.25f * pulse);
                arrowColor = C_CREAM;
            } else {
                fill = 0xB30B1112;
                border = 0x773B4A43;
                arrowColor = 0xFF849087;
            }
            drawChamfer(c, tmpRect, 4f, fill, border, next ? 1.8f : 1f);
            drawArrow(c, s.sequence[i], left + cellW * 0.5f, y, 18f, arrowColor);
        }
    }

    private void drawSquareControls(Canvas c, long now) {
        drawSquareBar(c, Direction.UP, new RectF(150f, 8f, 300f, 44f), now);
        drawSquareBar(c, Direction.DOWN, new RectF(150f, 406f, 300f, 442f), now);
        drawSquareBar(c, Direction.LEFT, new RectF(8f, 150f, 44f, 300f), now);
        drawSquareBar(c, Direction.RIGHT, new RectF(406f, 150f, 442f, 300f), now);
    }

    private void drawSquareBar(Canvas c, Direction d, RectF r, long now) {
        boolean flash = lastInputDir == d && now - lastInputAt < 145;
        boolean pressed = pressedDir == d && now - pressedAt < 150;
        boolean lit = flash || pressed;
        drawChamfer(c, r, 9f, lit ? 0xFF25302B : 0xE60A1112,
                lit ? C_GOLD : 0x553B4A43, lit ? 1.8f : 1.1f);
        if (lit) {
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(8f);
            linePaint.setStrokeCap(Paint.Cap.ROUND);
            linePaint.setColor(0x33FFD24A);
            c.drawRoundRect(new RectF(r.left - 2f, r.top - 2f, r.right + 2f, r.bottom + 2f),
                    11f, 11f, linePaint);
        }
        drawArrow(c, d, r.centerX(), r.centerY(), 26f,
                lit ? C_GOLD : 0xFF849087);
    }
    private void drawClickControls(Canvas c, long now) {
        if (squareMode) {
            drawSquareControls(c, now);
            return;
        }
        for (Direction d : DIRS) {
            float centerAngle = angleFor(d);
            boolean flash = lastInputDir == d && now - lastInputAt < 145;
            boolean pressed = pressedDir == d && now - pressedAt < 150;
            int base = 0xFF10191B;
            int arrow = 0xBFF1EEDC;

            if (pressed) {
                base = C_GOLD;
                arrow = C_BLACK;
            } else if (flash) {
                base = blend(currentAccent(), C_PANEL_LIGHT, 0.45f);
                arrow = C_BLACK;
            }

            if (pressed || flash) {
                linePaint.setStyle(Paint.Style.STROKE);
                linePaint.setStrokeWidth(50f);
                linePaint.setStrokeCap(Paint.Cap.ROUND);
                linePaint.setColor((base & 0x00FFFFFF) | 0x33000000);
                c.drawArc(ARC_RECT, centerAngle - 31f, 62f, false, linePaint);
            }

            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(37f);
            linePaint.setStrokeCap(Paint.Cap.ROUND);
            linePaint.setColor(base);
            c.drawArc(ARC_RECT, centerAngle - 30f, 60f, false, linePaint);

            linePaint.setStrokeWidth(1f);
            linePaint.setColor(pressed ? 0xAAFFE7A1 : 0x443B4A43);
            c.drawArc(RIM_OUTER, centerAngle - 30f, 60f, false, linePaint);
            c.drawArc(RIM_INNER, centerAngle - 30f, 60f, false, linePaint);

            float ar = 198f;
            float ax = CX + (float) Math.cos(Math.toRadians(centerAngle)) * ar;
            float ay = CY + (float) Math.sin(Math.toRadians(centerAngle)) * ar;
            drawArrow(c, d, ax, ay, 27f, arrow);
        }
    }


    private void drawEffects(Canvas c, long now, float time) {
        if (now < wrongUntil) {
            float k = (wrongUntil - now) / 450f;
            dangerPaint.setAlpha((int) (190 * k));
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setShader(dangerPaint.getShader());
            if (squareMode) c.drawRoundRect(new RectF(0f, 0f, 450f, 450f), 26f, 26f, fillPaint);
            else c.drawCircle(CX, CY, RAD, fillPaint);
            fillPaint.setShader(null);
        }
        if (now < successUntil) {
            float k = (successUntil - now) / 650f;
            int accent = currentAccent();
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(2.5f);
            linePaint.setColor(withAlpha(accent, (int) (180 * k)));
            c.drawCircle(CX, CY, 220f - 14f * (1f - k), linePaint);
        }
        if (time <= 10f && time > 0f) {
            float pulse = 0.25f + 0.25f * (float) Math.sin(now * 0.015f);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(7f);
            linePaint.setColor(withAlpha(C_RED, (int) (90 * pulse)));
            c.drawCircle(CX, CY, 216f, linePaint);
        }
    }

    // ---------------------------------------------------------------------
    // Countdown / pause / results / manual
    // ---------------------------------------------------------------------

    private void drawCountdown(Canvas c, long now) {
        fillPanel(c, 0xFF040809);

        int n = Math.max(1, (int) Math.ceil(countdown));
        float frac = countdown - (float) Math.floor(countdown);
        float pulse = 0.92f + 0.10f * (1f - frac);
        text(c, L10n.t("战术部署"), CX, 132f, 25f, C_CREAM, bodyMedium,
                Paint.Align.CENTER, 0.08f);
        text(c, "DEPLOYMENT PROTOCOL", CX, 152f, 10.5f, C_GOLD,
                display, Paint.Align.CENTER, 0.18f);

        float ringY = 235f;
        float ringR = 64f;
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
        linePaint.setColor(0x33FFD24A);
        c.drawCircle(CX, ringY, ringR, linePaint);
        linePaint.setColor(C_GOLD);
        c.drawArc(new RectF(CX - ringR, ringY - ringR, CX + ringR, ringY + ringR),
                -90f, 360f * Math.max(0f, countdown / 3f), false, linePaint);

        text(c, Integer.toString(n), CX, 263f, 98f * pulse, C_GOLD,
                display, Paint.Align.CENTER, 0.02f);
        text(c, L10n.t("准备输入"), CX, 318f, 18f, C_MUTED, bodyMedium,
                Paint.Align.CENTER, 0.08f);

        Stratagem s = session.getCurrent();
        if (s != null) {
            text(c, L10n.name(s), CX, 344f, 13f, 0x88F1EEDC, body,
                    Paint.Align.CENTER, 0.02f);
        }
    }

    private void drawGroupResult(Canvas c, long now) {
        fillPanel(c, 0xFF040809);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.5f);
        linePaint.setColor(0x66FFD24A);
        c.drawArc(new RectF(35f, 35f, 415f, 415f), 205f, 130f, false, linePaint);

        text(c, "GROUP CLEAR", CX, 98f, 10.5f, C_GOLD, display,
                Paint.Align.CENTER, 0.2f);
        text(c, L10n.groupClear(session.getGroupNumber()),
                CX, 136f, 31f, C_CREAM, bodyMedium,
                Paint.Align.CENTER, L10n.en ? 0.02f : 0.06f);

        text(c, L10n.t("本组得分"), CX, 172f, 12f, C_MUTED, bodyMedium,
                Paint.Align.CENTER, 0.12f);
        text(c, "+" + session.getGroupScore(), CX, 216f, 47f, C_GOLD_SOFT,
                mono, Paint.Align.CENTER, 0.02f);

        text(c, L10n.t("总分") + "  " + pad5(session.getScore()), CX, 244f, 16f,
                C_CREAM, bodyMedium, Paint.Align.CENTER, 0.06f);
        text(c, L10n.t("最高连击") + "  x" + twoDigits(session.getGroupBestCombo())
                        + "    ·    " + session.getGroupSize() + " / " + session.getGroupSize(),
                CX, 270f, 12.5f, C_MUTED, body, Paint.Align.CENTER, 0.02f);
        text(c, L10n.t("下一组：进度条更快"), CX, 294f, 10.5f, 0xAA9AA69D,
                display, Paint.Align.CENTER, 0.12f);

        drawButton(c, R_GROUP_CONTINUE, L10n.t("继 续 行 动"), true,
                "group_continue".equals(activeButton), true);
        drawButton(c, R_GROUP_EXIT, L10n.t("退 出 训 练"), false,
                "group_exit".equals(activeButton), true);
    }
    private void drawPairing(Canvas c, long now) {
        fillPanel(c, 0xFF040809);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.5f);
        linePaint.setColor(0x6675D6FF);
        c.drawArc(new RectF(35f, 35f, 415f, 415f), 205f, 130f, false, linePaint);

        text(c, "LAN KEYBOARD", CX, 94f, 10f, C_BLUE, display,
                Paint.Align.CENTER, 0.2f);
        text(c, L10n.t("电脑键盘桥接"), CX, 132f, 30f, C_CREAM, bodyMedium,
                Paint.Align.CENTER, 0.06f);
        text(c, L10n.t("手 表 配 对 码"), CX, 174f, 11f, C_MUTED, bodyMedium,
                Paint.Align.CENTER, 0.16f);

        String shown = remoteCode.length() == 6
                ? remoteCode.substring(0, 3) + "  " + remoteCode.substring(3)
                : remoteCode;
        text(c, shown, CX, 220f, 40f, C_GOLD_SOFT, mono,
                Paint.Align.CENTER, 0.08f);
        text(c, L10n.t("在电脑服务端输入此码确认"), CX, 254f, 13.5f, C_CREAM,
                body, Paint.Align.CENTER, 0.02f);
        text(c, L10n.t("同一 Wi-Fi · 服务端需放行 UDP/TCP"), CX, 274f, 10.5f, C_MUTED,
                body, Paint.Align.CENTER, 0.02f);

        int statusColor = remoteConnected ? C_GREEN : C_BLUE;
        text(c, remoteStatus.isEmpty() ? L10n.t("正在搜索电脑…") : L10n.t(remoteStatus),
                CX, 304f, 12.5f, statusColor, bodyMedium,
                Paint.Align.CENTER, 0.02f);

        drawButton(c, R_PAIR_REGENERATE, L10n.t("重 新 生 成"), false,
                "pair_regen".equals(activeButton), true);
        drawButton(c, R_PAIR_BACK, L10n.t("返 回 主 菜 单"), false,
                "pair_back".equals(activeButton), true);
    }

    private void drawRemote(Canvas c, long now, boolean controls) {
        if (controls && !swipeMode) drawClickControls(c, now);
        if (controls) drawPauseButton(c);

        int statusColor = remoteConnected ? C_GREEN : C_RED;
        text(c, "PC KEYBOARD", CX, 112f, 9.5f, statusColor, display,
                Paint.Align.CENTER, 0.2f);
        text(c, remoteConnected ? "LINKED" : "OFFLINE", CX, 127f, 8.5f,
                statusColor, display, Paint.Align.CENTER, 0.18f);
        if (remoteConnected && !remoteEndpoint.isEmpty()) {
            text(c, remoteEndpoint, CX, 143f, 8.5f, C_MUTED, mono,
                    Paint.Align.CENTER, 0.02f);
        } else if (!remoteConnected) {
            text(c, L10n.t("暂停后可重新配对"), CX, 143f, 9f, C_MUTED, body,
                    Paint.Align.CENTER, 0.02f);
        }

        drawCtrlButton(c);
        drawInputGhost(c, now);
    }

    private void drawCtrlButton(Canvas c) {
        boolean pressed = "remote_ctrl".equals(activeButton);
        boolean on = remoteMenuDown;
        drawChamfer(c, R_REMOTE_CTRL, 8f,
                on ? 0xEEFFD24A : (pressed ? 0xCC25302B : 0xCC0A1112),
                on ? C_GOLD : (pressed ? C_GOLD_SOFT : 0x773B4A43), 1.3f);
        text(c, "CTRL", R_REMOTE_CTRL.centerX(), 355f, 16f,
                on ? C_BLACK : C_CREAM, bodyMedium,
                Paint.Align.CENTER, 0.08f);
        text(c, L10n.t("菜单"), R_REMOTE_CTRL.centerX(), 371f, 9f,
                on ? 0xCC040809 : C_MUTED, body,
                Paint.Align.CENTER, 0.04f);
    }

    private void drawRemotePause(Canvas c, long now) {
        fillPanel(c, 0xFF040809);
        text(c, L10n.t("键盘暂停"), CX, 112f, 32f, C_CREAM, bodyMedium,
                Paint.Align.CENTER, 0.08f);
        text(c, L10n.en ? "CONTROL PAUSED" : "KEYBOARD PAUSE",
                CX, 132f, 10f, C_BLUE, display, Paint.Align.CENTER, 0.2f);
        String info = remoteConnected
                ? L10n.t("已连接") + "  " + remoteEndpoint
                : L10n.t("未连接 · 可重新配对");
        text(c, info, CX, 154f, 12.5f, remoteConnected ? C_GREEN : C_RED,
                body, Paint.Align.CENTER, 0.01f);
        drawSoundRow(c);
        drawButton(c, R_PAUSE_RESUME, L10n.t("继 续 控 制"), true,
                "resume".equals(activeButton), true);
        drawButton(c, R_PAUSE_RESTART, L10n.t("重 新 配 对"), false,
                "restart".equals(activeButton), true);
        drawButton(c, R_PAUSE_QUIT, L10n.t("退 出 模 式"), false,
                "quit".equals(activeButton), true);
    }
    private void drawPause(Canvas c, long now) {
        fillPanel(c, 0xFF040809);

        text(c, L10n.t("战术暂停"), CX, 112f, 32f, C_CREAM, bodyMedium,
                Paint.Align.CENTER, 0.08f);
        text(c, L10n.en ? "CONTROL PAUSED" : "TACTICAL PAUSE",
                CX, 132f, 10f, C_GOLD, display, Paint.Align.CENTER, 0.2f);

        if (!freeCallMode) {
            text(c, L10n.t("得分") + " " + pad5(session.getScore()) + "   ·   "
                            + L10n.t("剩余") + " "
                            + String.format(Locale.US, "%.1f", session.getTimeLeft()) + "s",
                    CX, 154f, 14.5f, C_MUTED,
                    body, Paint.Align.CENTER, 0.01f);
        }

        drawSoundRow(c);

        drawButton(c, R_PAUSE_RESUME, L10n.t("继 续 训 练"), true,
                "resume".equals(activeButton), true);
        drawButton(c, R_PAUSE_RESTART, L10n.t("重 新 开 始"), false,
                "restart".equals(activeButton), true);
        drawButton(c, R_PAUSE_QUIT, L10n.t("退 出 任 务"), false,
                "quit".equals(activeButton), true);
    }

    private void drawSoundRow(Canvas c) {
        drawChamfer(c, R_PAUSE_SOUND, 7f, 0xA60B1214,
                "sound".equals(activeButton) ? C_GOLD : 0x663B4A43, 1.1f);
        text(c, L10n.t("音效反馈"), 141f, 214f, 16f, C_CREAM, bodyMedium,
                Paint.Align.LEFT, 0.02f);

        float pillW = 56f;
        float pillH = 22f;
        float l = 305f - pillW;
        float t = 197f;
        tmpRect.set(l, t, 305f, t + pillH);
        drawChamfer(c, tmpRect, 5f, soundOn ? 0xAAFFD24A : 0x5534413C,
                soundOn ? C_GOLD : 0x773B4A43, 1f);
        text(c, L10n.t(soundOn ? "开" : "关"), (l + 305f) * 0.5f, 214f, 14.5f,
                soundOn ? C_BLACK : C_MUTED, bodyMedium, Paint.Align.CENTER, 0f);
    }

    private void drawGameOver(Canvas c, long now) {
        fillPanel(c, 0xFF040809);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.5f);
        linePaint.setColor(0x66FFD24A);
        c.drawArc(new RectF(35f, 35f, 415f, 415f), 205f, 130f, false, linePaint);

        text(c, L10n.en ? "RUN ENDED" : "TRAINING COMPLETE", CX, 101f, 10.5f,
                C_GOLD, display, Paint.Align.CENTER, 0.18f);
        text(c, L10n.t("训 练 结 束"), CX, 134f, 38f, C_CREAM, bodyMedium,
                Paint.Align.CENTER, 0.08f);

        char rank = session.getRank();
        int rankColor = rank == 'S' ? C_GOLD
                : rank == 'A' ? C_GREEN
                : rank == 'B' ? C_BLUE
                : rank == 'C' ? C_CREAM : C_CORAL;
        float ring = 28f;
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(0x22000000);
        c.drawCircle(CX, 171f, ring + 8f, fillPaint);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
        linePaint.setColor(rankColor);
        c.drawCircle(CX, 171f, ring, linePaint);
        text(c, String.valueOf(rank), CX, 186f, 39f, rankColor, display,
                Paint.Align.CENTER, 0.02f);

        text(c, L10n.t("本局得分"), CX, 214f, 11.5f, C_MUTED, display,
                Paint.Align.CENTER, 0.16f);
        text(c, pad5(session.getScore()), CX, 249f, 43f, C_GOLD_SOFT,
                mono, Paint.Align.CENTER, 0.02f);

        if (newRecord) {
            text(c, L10n.t("★  新 纪 录  ★"), CX, 264f, 12f, C_GOLD, bodyMedium,
                    Paint.Align.CENTER, 0.1f);
        } else {
            text(c, L10n.t("历史最高") + "  " + pad5(bestScore), CX, 264f, 11.5f, C_MUTED,
                    display, Paint.Align.CENTER, 0.08f);
        }

        line(c, 105f, 275f, 345f, 275f, 0x4434413C, 1f);
        String accuracy = session.getTotalSteps() == 0
                ? "--" : Math.round(session.getAccuracy() * 100f) + "%";
        text(c, L10n.t("完成") + "  " + twoDigits(session.getCompleted()), 146f, 291f, 14f,
                C_MUTED, body, Paint.Align.CENTER, 0.02f);
        text(c, L10n.t("连击") + "  " + twoDigits(session.getBestCombo()), 225f, 291f, 14f,
                C_MUTED, body, Paint.Align.CENTER, 0.02f);
        text(c, L10n.t("命中") + "  " + accuracy, 304f, 291f, 14f,
                C_MUTED, body, Paint.Align.CENTER, 0.02f);

        drawButton(c, R_OVER_RETRY, L10n.t("再 次 部 署"), true,
                "retry".equals(activeButton), true);
        drawButton(c, R_OVER_MENU, L10n.t("返 回 主 菜 单"), false,
                "menu".equals(activeButton), true);
    }

    private void drawHelp(Canvas c, long now) {
        fillPanel(c, 0xFF030607);

        text(c, L10n.t("作战手册"), CX, 86f, 30f, C_CREAM, bodyMedium,
                Paint.Align.CENTER, 0.08f);
        text(c, L10n.en ? "TACTICAL GUIDE  //  01" : "FIELD MANUAL  //  01",
                CX, 104f, 10f, C_GOLD, display, Paint.Align.CENTER, 0.2f);

        text(c, L10n.en ? "01   CONTROLS" : "01   操作", 112f, 133f, 13f, C_GOLD, display,
                Paint.Align.LEFT, 0.1f);
        text(c, L10n.t("点击 / 滑动：输入箭头。"), 112f, 154f, L10n.en ? 15f : 16f, C_CREAM,
                body, Paint.Align.LEFT, 0.005f);
        text(c, L10n.t("标准：先看战备，再输入。"), 112f, 174f, L10n.en ? 15f : 16f, C_CREAM,
                body, Paint.Align.LEFT, 0.005f);

        text(c, L10n.en ? "02   RULES" : "02   规则", 112f, 201f, 13f, C_GOLD, display,
                Paint.Align.LEFT, 0.1f);
        text(c, L10n.t("自由：空白输入，匹配战备即呼叫。"), 112f, 222f, L10n.en ? 14f : 16f, C_CREAM,
                body, Paint.Align.LEFT, 0.005f);
        text(c, L10n.t("自由模式不计时，也不会扣时间。"), 112f, 242f, L10n.en ? 14f : 16f, C_CREAM,
                body, Paint.Align.LEFT, 0.005f);
        text(c, L10n.t("标准：5 个一组，组末结算分数。"), 112f, 262f, L10n.en ? 14f : 16f, C_CREAM,
                body, Paint.Align.LEFT, 0.005f);
        text(c, L10n.t("标准：错误输入清空并扣 1.2 秒。"), 112f, 282f, L10n.en ? 14f : 16f, C_CREAM,
                body, Paint.Align.LEFT, 0.005f);
        text(c, L10n.t("返回手势已屏蔽；点顶部按钮暂停。"), 112f, 302f, L10n.en ? 14f : 16f, C_CREAM,
                body, Paint.Align.LEFT, 0.005f);

        text(c, L10n.t("非官方粉丝作品 · 素材版权归原作者"), CX, 326f, 11f,
                0xAA8B988E, body, Paint.Align.CENTER, 0.02f);

        drawButton(c, R_HELP_BACK, L10n.en ? "BACK" : "返 回", false,
                "close_help".equals(activeButton), true);
    }

    // ---------------------------------------------------------------------
    // Generic drawing helpers
    // ---------------------------------------------------------------------

    private void drawButton(Canvas c, RectF r, String label, boolean primary,
                            boolean pressed, boolean enabled) {
        int fill = primary ? (pressed ? C_GOLD_SOFT : C_GOLD) : (pressed ? 0xFF1A2626 : 0xC80C1415);
        int border = primary ? (pressed ? C_GOLD_SOFT : C_GOLD) : (pressed ? C_GOLD : 0x883B4A43);
        int txt = primary ? C_BLACK : (pressed ? C_CREAM : C_CREAM);

        if (primary && !pressed) {
            tmpRect.set(r.left - 3f, r.top - 3f, r.right + 3f, r.bottom + 3f);
            drawChamfer(c, tmpRect, 9f, 0x22FFD24A, 0x00000000, 0f);
        }
        drawChamfer(c, r, 8f, fill, border, 1.2f);
        text(c, label, CX, r.centerY() + 8f, 22f, txt, bodyMedium,
                Paint.Align.CENTER, 0.04f);
    }

    private void drawChamfer(Canvas c, RectF r, float cut, int fill, int border,
                             float borderWidth) {
        buildChamfer(tmpPath, r, cut);
        if ((fill >>> 24) != 0) {
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setColor(fill);
            c.drawPath(tmpPath, fillPaint);
        }
        if (borderWidth > 0f && (border >>> 24) != 0) {
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(borderWidth);
            linePaint.setColor(border);
            c.drawPath(tmpPath, linePaint);
        }
    }

    private void buildChamfer(Path p, RectF r, float cut) {
        p.reset();
        p.moveTo(r.left + cut, r.top);
        p.lineTo(r.right - cut, r.top);
        p.lineTo(r.right, r.top + cut);
        p.lineTo(r.right, r.bottom - cut);
        p.lineTo(r.right - cut, r.bottom);
        p.lineTo(r.left + cut, r.bottom);
        p.lineTo(r.left, r.bottom - cut);
        p.lineTo(r.left, r.top + cut);
        p.close();
    }

    private void drawArrow(Canvas c, Direction d, float x, float y,
                           float size, int color) {
        float angle;
        switch (d) {
            case UP: angle = 0f; break;
            case RIGHT: angle = 90f; break;
            case DOWN: angle = 180f; break;
            default: angle = -90f; break;
        }
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(color);
        c.save();
        c.translate(x, y);
        c.rotate(angle);
        c.scale(size / 24f, size / 24f);
        c.translate(-12f, -12f);
        c.drawPath(arrowPath, fillPaint);
        c.restore();
    }

    private void line(Canvas c, float x1, float y1, float x2, float y2,
                      int color, float width) {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(width);
        linePaint.setColor(color);
        c.drawLine(x1, y1, x2, y2, linePaint);
    }

    private void text(Canvas c, String s, float x, float y, float size,
                      int color, Typeface tf, Paint.Align align, float spacing) {
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(color);
        textPaint.setTextSize(size);
        textPaint.setTypeface(tf);
        textPaint.setTextAlign(align);
        textPaint.setLetterSpacing(spacing);
        textPaint.setTextScaleX(1f);
        c.drawText(s, x, y, textPaint);
    }

    private void drawTextFit(Canvas c, String s, float x, float y, float size,
                             int color, Typeface tf, Paint.Align align,
                             float spacing, float maxWidth) {
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(color);
        textPaint.setTypeface(tf);
        textPaint.setTextAlign(align);
        textPaint.setLetterSpacing(spacing);
        textPaint.setTextScaleX(1f);
        float width = textPaint.measureText(s);
        float target = size;
        if (width > maxWidth) {
            target = Math.max(8f, size * maxWidth / width);
        }
        textPaint.setTextSize(target);
        c.drawText(s, x, y, textPaint);
    }

    private Bitmap iconFor(String id) {
        Bitmap bm = iconCache.get(id);
        if (bm != null) return bm;
        InputStream in = null;
        try {
            in = assets.open("icons/" + id + ".png");
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            opts.inSampleSize = 2;
            bm = BitmapFactory.decodeStream(in, null, opts);
            if (bm != null) iconCache.put(id, bm);
            return bm;
        } catch (IOException e) {
            return null;
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignored) { }
            }
        }
    }

    private static float angleFor(Direction d) {
        switch (d) {
            case UP: return -90f;
            case RIGHT: return 0f;
            case DOWN: return 90f;
            default: return 180f;
        }
    }

    private static Direction directionFor(float dx, float dy) {
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx >= 0f ? Direction.RIGHT : Direction.LEFT;
        }
        return dy >= 0f ? Direction.DOWN : Direction.UP;
    }

    private static String pad5(int value) {
        String s = Integer.toString(Math.max(0, value));
        while (s.length() < 5) s = "0" + s;
        return s;
    }

    private static String twoDigits(int value) {
        if (value < 10) return "0" + value;
        return Integer.toString(value);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((Math.max(0, Math.min(255, alpha))) << 24);
    }

    private static int blend(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private int currentAccent() {
        if (freeCallMode) return C_BLUE;
        Stratagem s = session.getCurrent();
        return s == null ? C_GOLD : categoryColor(s.category);
    }
    private static int categoryColor(String category) {
        if (StratagemCatalog.SUPPORT.equals(category)) return CAT_SUPPORT;
        if (StratagemCatalog.OFFENSIVE.equals(category)) return CAT_OFFENSIVE;
        if (StratagemCatalog.BACKPACK.equals(category)) return CAT_BACKPACK;
        if (StratagemCatalog.SENTRY.equals(category)) return CAT_SENTRY;
        if (StratagemCatalog.DEFENSIVE.equals(category)) return CAT_DEFENSIVE;
        return CAT_MISSION;
    }

    private static String categoryZh(String category) {
        return L10n.categoryZh(category);
    }

    private static String categoryEn(String category) {
        return L10n.categoryEn(category);
    }

    // ---------------------------------------------------------------------
    // Lifecycle and interaction
    // ---------------------------------------------------------------------

    private void startRemoteMode() {
        feedback.stopHeroLoop();
        remoteMode = true;
        freeCallMode = false;
        prefs.edit().putBoolean("remote_mode", true)
                .putBoolean("free_call_mode", false).apply();
        remoteConnected = false;
        remoteMenuDown = false;
        remoteEndpoint = "";
        remoteStatus = "正在准备…";
        remoteCode = newCode();
        screen = Screen.KEYBOARD_PAIR;
        pauseReturn = Screen.REMOTE;
        closeKeyboard();
        if (wifiLock != null && !wifiLock.isHeld()) wifiLock.acquire();
        refreshGestureExclusion();

        keyboard = new KeyboardClient(lanListener);
        String host = prefs.getString("remote_host", "");
        int port = prefs.getInt("remote_port", 45892);
        String token = prefs.getString("remote_token", "");
        keyboard.start(host, port, token, remoteCode, deviceName());
        lastFrame = SystemClock.uptimeMillis();
        postInvalidateOnAnimation();
    }

    private void regeneratePairing() {
        closeKeyboard();
        remoteConnected = false;
        remoteMenuDown = false;
        remoteEndpoint = "";
        remoteStatus = "正在搜索电脑…";
        remoteCode = newCode();
        keyboard = new KeyboardClient(lanListener);
        keyboard.start("", 0, "", remoteCode, deviceName());
        screen = Screen.KEYBOARD_PAIR;
        lastFrame = SystemClock.uptimeMillis();
        refreshGestureExclusion();
        postInvalidateOnAnimation();
    }

    private void releaseRemoteLocks() {
        if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();
    }

    private void closeKeyboard() {
        KeyboardClient k = keyboard;
        keyboard = null;
        if (k != null) {
            if (remoteMenuDown) k.sendMenu(false);
            k.close();
        }
        remoteConnected = false;
        remoteMenuDown = false;
    }

    private String newCode() {
        return String.format(Locale.US, "%06d", new Random().nextInt(1000000));
    }

    private String deviceName() {
        String model = Build.MODEL;
        if (model == null || model.trim().isEmpty()) model = "Galaxy Watch4";
        model = model.replace('|', ' ').trim();
        if (model.length() > 36) model = model.substring(0, 36);
        return model;
    }

    private void remoteInput(Direction d) {
        long now = SystemClock.uptimeMillis();
        if (now < inputLockUntil) return;
        if (!remoteConnected || keyboard == null) {
            remoteStatus = "电脑未连接";
            feedback.error();
            invalidate();
            return;
        }
        inputLockUntil = now + 80L;
        lastInputDir = d;
        lastInputAt = now;
        ghostDir = d;
        ghostAt = now;
        ghostColor = C_BLUE;
        keyboard.sendDirection(d.code);
        feedback.tick();
        invalidate();
    }

    private void toggleRemoteMenu() {
        if (!remoteConnected || keyboard == null) {
            remoteStatus = "电脑未连接";
            feedback.error();
            invalidate();
            return;
        }
        remoteMenuDown = !remoteMenuDown;
        keyboard.sendMenu(remoteMenuDown);
        feedback.tick();
        invalidate();
    }

    private void startRun() {
        closeKeyboard();
        session = new GameSession(StratagemCatalog.ALL, new Random(), freeCallMode);
        session.start();
        newRecord = false;
        if (freeCallMode) {
            screen = Screen.PLAYING;
            countdown = 0f;
        } else {
            screen = Screen.COUNTDOWN;
            countdown = 3.0f;
        }
        lastCountdownNumber = 3;
        activeButton = "";
        pressedDir = null;
        resetSwipe();
        inputLockUntil = 0L;
        successUntil = 0L;
        wrongUntil = 0L;
        shakeUntil = 0L;
        revealStratagem = null;
        revealUntil = 0L;
        lastFrame = SystemClock.uptimeMillis();
        refreshGestureExclusion();
        feedback.stopHeroLoop();
        feedback.start();
        if (!freeCallMode) feedback.startHeroLoop();
        postInvalidateOnAnimation();
    }

    private void finishRun(long now) {
        if (screen == Screen.OVER) return;
        newRecord = session.getScore() > bestScore;
        recordBestScore();
        screen = Screen.OVER;
        activeButton = "";
        pressedDir = null;
        resetSwipe();
        feedback.stopHeroLoop();
        feedback.gameOver();
        refreshGestureExclusion();
        postInvalidateOnAnimation();
    }

    private void recordBestScore() {
        if (freeCallMode || remoteMode) return;
        if (session != null && session.getScore() > bestScore) {
            bestScore = session.getScore();
            prefs.edit().putInt("best_score", bestScore).apply();
        }
    }

    private void pauseGame() {
        if (screen == Screen.PLAYING || screen == Screen.COUNTDOWN
                || screen == Screen.REMOTE) {
            if (screen == Screen.REMOTE && remoteMenuDown && keyboard != null) {
                keyboard.sendMenu(false);
                remoteMenuDown = false;
            }
            pauseReturn = screen;
            screen = Screen.PAUSED;
            activeButton = "";
            pressedDir = null;
            resetSwipe();
            feedback.pauseHeroLoop();
            refreshGestureExclusion();
            postInvalidateOnAnimation();
        }
    }

    private void resumeGame() {
        if (screen != Screen.PAUSED) return;
        screen = pauseReturn;
        lastFrame = SystemClock.uptimeMillis();
        activeButton = "";
        if (!freeCallMode && !remoteMode) feedback.resumeHeroLoop();
        feedback.tick();
        refreshGestureExclusion();
        postInvalidateOnAnimation();
    }

    private void quitToMenu() {
        closeKeyboard();
        releaseRemoteLocks();
        recordBestScore();
        screen = Screen.MENU;
        activeButton = "";
        pressedDir = null;
        resetSwipe();
        lastFrame = SystemClock.uptimeMillis();
        feedback.stopHeroLoop();
        feedback.tick();
        refreshGestureExclusion();
        postInvalidateOnAnimation();
    }

    private void resetSwipe() {
        swipeTracking = false;
        swipeConsumed = false;
        trailVisible = false;
        trailStartX = trailStartY = trailEndX = trailEndY = 0f;
    }

    private void registerInput(Direction d) {
        long now = SystemClock.uptimeMillis();
        if (now < inputLockUntil) return;
        GameSession.Result result = session.input(d);
        if (result == GameSession.Result.IGNORED) return;

        lastInputDir = d;
        lastInputAt = now;
        if (result == GameSession.Result.CORRECT) {
            feedback.tick();
        } else if (result == GameSession.Result.COMPLETE) {
            successUntil = now + 700L;
            wrongUntil = 0L;
            if (freeCallMode && session.getLastCompleted() != null) {
                feedback.callIn(session.getLastCompleted());
                revealStratagem = session.getLastCompleted();
                revealStart = now;
                revealUntil = now + 2600L;
                inputLockUntil = revealUntil + 50L;
            } else if (session.isGroupCleared()) {
                feedback.pauseHeroLoop();
                feedback.roundComplete();
                recordBestScore();
                screen = Screen.GROUP_RESULT;
                refreshGestureExclusion();
                inputLockUntil = now + 250L;
            } else {
                feedback.success();
                inputLockUntil = now + 175L;
            }
        } else if (result == GameSession.Result.WRONG) {
            feedback.error();
            wrongUntil = now + 460L;
            shakeUntil = now + 290L;
        }
        ghostDir = d;
        ghostAt = now;
        ghostColor = result == GameSession.Result.WRONG
                ? C_RED
                : (freeCallMode ? C_BLUE
                        : (session.getCurrent() != null
                                ? categoryColor(session.getCurrent().category)
                                : (session.getLastCompleted() != null
                                        ? categoryColor(session.getLastCompleted().category)
                                        : C_GOLD)));

        if (session.isOver()) {
            finishRun(now);
        }
        invalidate();
    }

    private void setSwipeMode(boolean swipe) {
        if (swipeMode != swipe) {
            swipeMode = swipe;
            prefs.edit().putBoolean("swipe_mode", swipeMode).apply();
            feedback.tick();
            invalidate();
        }
    }

    private void setFreeCallMode(boolean free) {
        if (freeCallMode != free || remoteMode) {
            freeCallMode = free;
            remoteMode = false;
            closeKeyboard();
            prefs.edit().putBoolean("free_call_mode", freeCallMode)
                    .putBoolean("remote_mode", false).apply();
            revealStratagem = null;
            revealUntil = 0L;
            feedback.tick();
            invalidate();
        }
    }

    private void setRemoteMode(boolean remote) {
        if (remoteMode != remote) {
            remoteMode = remote;
            if (remote) freeCallMode = false;
            closeKeyboard();
            prefs.edit().putBoolean("remote_mode", remoteMode)
                    .putBoolean("free_call_mode", freeCallMode).apply();
            revealStratagem = null;
            revealUntil = 0L;
            feedback.tick();
            invalidate();
        }
    }

    private String modeButtonAt(float x, float y, RectF r) {
        if (!r.contains(x, y)) return "";
        float left = r.left + 2f;
        float right = r.right - 2f;
        int idx = (int) ((x - left) / ((right - left) / 5f));
        if (idx < 0) idx = 0;
        if (idx > 4) idx = 4;
        switch (idx) {
            case 0: return "input_click";
            case 1: return "input_swipe";
            case 2: return "play_standard";
            case 3: return "play_free";
            default: return "play_remote";
        }
    }

    private void toggleSound() {
        soundOn = !soundOn;
        feedback.setEnabled(soundOn);
        prefs.edit().putBoolean("sound_on", soundOn).apply();
        if (soundOn) {
            feedback.tick();
            if (!freeCallMode && !remoteMode
                    && (screen == Screen.PLAYING || screen == Screen.COUNTDOWN)) {
                feedback.startHeroLoop();
            }
        }
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = (event.getX() - offsetX) / Math.max(0.0001f, scale);
        float y = (event.getY() - offsetY) / Math.max(0.0001f, scale);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                activeButton = "";
                if (screen == Screen.MENU) {
                    if (R_MENU_MODE.contains(x, y)) {
                        activeButton = modeButtonAt(x, y, R_MENU_MODE);
                    } else if (R_MENU_START.contains(x, y)) {
                        activeButton = "start";
                    } else if (inCircle(x, y, 105f, 74f, 24f)) {
                        activeButton = "sound";
                    } else if (inCircle(x, y, 345f, 74f, 24f)) {
                        activeButton = "help";
                    }
                } else if (screen == Screen.PAUSED) {
                    if (R_PAUSE_SOUND.contains(x, y)) {
                        activeButton = "sound";
                    } else if (R_PAUSE_RESUME.contains(x, y)) {
                        activeButton = "resume";
                    } else if (R_PAUSE_RESTART.contains(x, y)) {
                        activeButton = "restart";
                    } else if (R_PAUSE_QUIT.contains(x, y)) {
                        activeButton = "quit";
                    }
                } else if (screen == Screen.OVER) {
                    if (R_OVER_RETRY.contains(x, y)) activeButton = "retry";
                    else if (R_OVER_MENU.contains(x, y)) activeButton = "menu";
                } else if (screen == Screen.GROUP_RESULT) {
                    if (R_GROUP_CONTINUE.contains(x, y)) activeButton = "group_continue";
                    else if (R_GROUP_EXIT.contains(x, y)) activeButton = "group_exit";
                } else if (screen == Screen.KEYBOARD_PAIR) {
                    if (R_PAIR_REGENERATE.contains(x, y)) activeButton = "pair_regen";
                    else if (R_PAIR_BACK.contains(x, y)) activeButton = "pair_back";
                } else if (screen == Screen.REMOTE) {
                    if (!swipeMode && (x < 18f || x > 432f || y < 18f || y > 432f)) {
                        invalidate();
                        return true;
                    }
                    if (inCircle(x, y, CX, 86f, 22f)) {
                        activeButton = "pause";
                    } else if (R_REMOTE_CTRL.contains(x, y)) {
                        activeButton = "remote_ctrl";
                    } else if (swipeMode) {
                        swipeTracking = true;
                        swipeConsumed = false;
                        swipeStartX = x;
                        swipeStartY = y;
                        trailStartX = x;
                        trailStartY = y;
                        trailEndX = x;
                        trailEndY = y;
                        trailVisible = true;
                        trailUpdatedAt = SystemClock.uptimeMillis();
                    } else {
                        Direction d = arrowAt(x, y);
                        if (d != null) {
                            pressedDir = d;
                            pressedAt = SystemClock.uptimeMillis();
                            remoteInput(d);
                        }
                    }
                } else if (screen == Screen.HELP) {
                    if (R_HELP_BACK.contains(x, y)) activeButton = "close_help";
                } else if (screen == Screen.PLAYING) {
                    // In click mode ignore touches that begin at the very edge, so a
                    // system gesture cannot also press an arrow. Swipe mode needs the
                    // full surface because horizontal swipes are the sensitive axis.
                    if (!swipeMode && (x < 18f || x > 432f || y < 18f || y > 432f)) {
                        invalidate();
                        return true;
                    }
                    if (inCircle(x, y, CX, 86f, 22f)) {
                        activeButton = "pause";
                    } else if (swipeMode) {
                        swipeTracking = true;
                        swipeConsumed = false;
                        swipeStartX = x;
                        swipeStartY = y;
                        trailStartX = x;
                        trailStartY = y;
                        trailEndX = x;
                        trailEndY = y;
                        trailVisible = true;
                        trailUpdatedAt = SystemClock.uptimeMillis();
                    } else {
                        Direction d = arrowAt(x, y);
                        if (d != null) {
                            pressedDir = d;
                            pressedAt = SystemClock.uptimeMillis();
                            registerInput(d);
                        }
                    }
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if ((screen == Screen.PLAYING || screen == Screen.REMOTE)
                        && swipeMode && swipeTracking) {
                    trailEndX = x;
                    trailEndY = y;
                    trailVisible = true;
                    trailUpdatedAt = SystemClock.uptimeMillis();
                    if (!swipeConsumed) {
                        float dx = x - swipeStartX;
                        float dy = y - swipeStartY;
                        float threshold = Math.abs(dx) > Math.abs(dy) ? 16f : 24f;
                        if (Math.abs(dx) + Math.abs(dy) > threshold) {
                            swipeConsumed = true;
                            Direction d = directionFor(dx, dy);
                            if (screen == Screen.REMOTE) remoteInput(d);
                            else registerInput(d);
                        }
                    }
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if ((screen == Screen.PLAYING || screen == Screen.REMOTE) && swipeMode) {
                    feedSwipeFallback(x, y);
                    trailUpdatedAt = SystemClock.uptimeMillis();
                    if (Math.abs(trailEndX - trailStartX) + Math.abs(trailEndY - trailStartY) < 4f) {
                        trailVisible = false;
                    }
                    swipeTracking = false;
                    invalidate();
                }
                if (!activeButton.isEmpty()) {
                    boolean hit = buttonStillHit(activeButton, x, y);
                    String action = activeButton;
                    activeButton = "";
                    if (hit) performAction(action);
                }
                pressedDir = null;
                invalidate();
                return true;

            case MotionEvent.ACTION_CANCEL:
                if ((screen == Screen.PLAYING || screen == Screen.REMOTE) && swipeMode) {
                    feedSwipeFallback(x, y);
                }
                activeButton = "";
                pressedDir = null;
                swipeTracking = false;
                trailVisible = false;
                invalidate();
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    private boolean buttonStillHit(String id, float x, float y) {
        switch (id) {
            case "input_click":
            case "input_swipe":
            case "play_standard":
            case "play_free":
            case "play_remote":
                if (screen == Screen.MENU) return id.equals(modeButtonAt(x, y, R_MENU_MODE));
                return false;
            case "start": return R_MENU_START.contains(x, y);
            case "sound":
                if (screen == Screen.PAUSED) return R_PAUSE_SOUND.contains(x, y);
                return inCircle(x, y, 105f, 74f, 24f);
            case "help": return inCircle(x, y, 345f, 74f, 24f);
            case "resume": return R_PAUSE_RESUME.contains(x, y);
            case "restart": return R_PAUSE_RESTART.contains(x, y);
            case "quit": return R_PAUSE_QUIT.contains(x, y);
            case "retry": return R_OVER_RETRY.contains(x, y);
            case "menu": return R_OVER_MENU.contains(x, y);
            case "group_continue": return R_GROUP_CONTINUE.contains(x, y);
            case "group_exit": return R_GROUP_EXIT.contains(x, y);
            case "pair_regen": return R_PAIR_REGENERATE.contains(x, y);
            case "pair_back": return R_PAIR_BACK.contains(x, y);
            case "remote_ctrl": return R_REMOTE_CTRL.contains(x, y);
            case "close_help": return R_HELP_BACK.contains(x, y);
            case "pause": return inCircle(x, y, CX, 86f, 22f);
            default: return false;
        }
    }

    private void performAction(String id) {
        switch (id) {
            case "input_click":
                setSwipeMode(false);
                break;
            case "input_swipe":
                setSwipeMode(true);
                break;
            case "play_standard":
                setFreeCallMode(false);
                break;
            case "play_free":
                setFreeCallMode(true);
                break;
            case "play_remote":
                setRemoteMode(true);
                break;
            case "start":
                if (remoteMode) startRemoteMode();
                else startRun();
                break;
            case "pause":
                pauseGame();
                break;
            case "sound":
                toggleSound();
                break;
            case "help":
                screen = Screen.HELP;
                feedback.tick();
                invalidate();
                break;
            case "resume":
                resumeGame();
                break;
            case "restart":
                if (remoteMode) regeneratePairing();
                else startRun();
                break;
            case "quit":
                quitToMenu();
                break;
            case "retry":
                startRun();
                break;
            case "menu":
                quitToMenu();
                break;
            case "group_continue":
                if (session.continueGroup()) {
                    screen = Screen.PLAYING;
                    lastFrame = SystemClock.uptimeMillis();
                    activeButton = "";
                    successUntil = 0L;
                    wrongUntil = 0L;
                    revealStratagem = null;
                    feedback.start();
                    feedback.resumeHeroLoop();
                    refreshGestureExclusion();
                    postInvalidateOnAnimation();
                }
                break;
            case "group_exit":
                quitToMenu();
                break;
            case "pair_regen":
                regeneratePairing();
                break;
            case "pair_back":
                quitToMenu();
                break;
            case "remote_ctrl":
                toggleRemoteMenu();
                break;
            case "close_help":
                screen = Screen.MENU;
                lastFrame = SystemClock.uptimeMillis();
                feedback.tick();
                invalidate();
                break;
            default:
                break;
        }
    }

    private void feedSwipeFallback(float x, float y) {
        if (swipeConsumed || !swipeTracking) return;
        float dx = x - swipeStartX;
        float dy = y - swipeStartY;
        float threshold = Math.abs(dx) > Math.abs(dy) ? 12f : 20f;
        if (Math.abs(dx) + Math.abs(dy) > threshold) {
            swipeConsumed = true;
            Direction d = directionFor(dx, dy);
            if (screen == Screen.REMOTE) remoteInput(d);
            else registerInput(d);
        }
    }

    private Direction arrowAt(float x, float y) {
        if (squareMode) {
            if (y < 60f && x > 100f && x < 350f) return Direction.UP;
            if (y > 390f && x > 100f && x < 350f) return Direction.DOWN;
            if (x < 60f && y > 100f && y < 350f) return Direction.LEFT;
            if (x > 390f && y > 100f && y < 350f) return Direction.RIGHT;
            return null;
        }
        float dx = x - CX;
        float dy = y - CY;
        float dist = (float) Math.hypot(dx, dy);
        if (dist < 132f || dist > 230f) return null;
        return directionFor(dx, dy);
    }

    private static boolean inCircle(float x, float y, float cx, float cy, float r) {
        float dx = x - cx;
        float dy = y - cy;
        return dx * dx + dy * dy <= r * r;
    }

    boolean handleBack() {
        if (screen == Screen.MENU) return false;
        if (screen == Screen.HELP) {
            screen = Screen.MENU;
            feedback.tick();
            lastFrame = SystemClock.uptimeMillis();
            postInvalidateOnAnimation();
            return true;
        }
        if (screen == Screen.OVER) {
            quitToMenu();
            return true;
        }
        if (screen == Screen.PAUSED) {
            resumeGame();
            return true;
        }
        if (screen == Screen.PLAYING || screen == Screen.COUNTDOWN
                || screen == Screen.REMOTE || screen == Screen.KEYBOARD_PAIR) {
            // Return gestures are intentionally ignored during a run; use the pause button.
            return true;
        }
        return true;
    }

    void onHostResume() {
        lastFrame = SystemClock.uptimeMillis();
        activeButton = "";
        pressedDir = null;
        resetSwipe();
        postInvalidateOnAnimation();
    }

    void onHostPause() {
        if (screen == Screen.PLAYING || screen == Screen.COUNTDOWN
                || screen == Screen.REMOTE) {
            pauseReturn = screen;
            screen = Screen.PAUSED;
        }
        if (remoteMenuDown && keyboard != null) {
            keyboard.sendMenu(false);
            remoteMenuDown = false;
        }
        feedback.pauseHeroLoop();
        refreshGestureExclusion();
        resetSwipe();
        activeButton = "";
        pressedDir = null;
    }

    void release() {
        closeKeyboard();
        releaseRemoteLocks();
        if (feedback != null) feedback.release();
        for (Bitmap bm : iconCache.values()) {
            if (bm != null) bm.recycle();
        }
        iconCache.clear();
        if (backdrop != null) {
            backdrop.recycle();
            backdrop = null;
        }
    }
}
