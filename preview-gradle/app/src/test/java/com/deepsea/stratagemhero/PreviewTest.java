package com.deepsea.stratagemhero;

import static org.robolectric.annotation.GraphicsMode.Mode.NATIVE;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowSystemClock;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(NATIVE)
@Config(sdk = 30, qualifiers = "w450dp-h450dp-xxhdpi")
public class PreviewTest {
    private static final int SIZE = 450;
    private Context context;
    private File outDir;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("stratagem_hero", Context.MODE_PRIVATE)
                .edit().clear().commit();
        L10n.en = false;
        outDir = new File(System.getProperty("preview.out", "preview/render"));
        if (!outDir.exists()) outDir.mkdirs();
    }

    @Test
    public void renderScreens() throws Exception {
        GameView menu = makeView();
        save(menu, "01_menu.png");

        tap(menu, 225f, 392f);
        save(menu, "08_countdown.png");
        drainCountdown(menu);
        save(menu, "02_game_resupply.png");

        // Complete the first Resupply code: down, down, up, right.
        tap(menu, 225f, 405f);
        tap(menu, 225f, 405f);
        tap(menu, 225f, 45f);
        tap(menu, 405f, 225f);
        draw(menu);
        save(menu, "03_game_success.png");

        GameSession session = (GameSession) field(menu, "session");
        assertEquals(1, session.getCompleted());
        assertEquals(120, session.getScore());
        assertEquals(0, session.getEnteredCount());

        tap(menu, 225f, 86f);
        assertEquals("PAUSED", field(menu, "screen").toString());
        draw(menu);
        save(menu, "04_pause.png");

        GameView help = makeView();
        tap(help, 345f, 74f);
        assertEquals("HELP", field(help, "screen").toString());
        draw(help);
        save(help, "05_help.png");

        GameView swipe = makeView();
        tap(swipe, 199f, 322f);
        assertEquals(Boolean.TRUE, field(swipe, "swipeMode"));
        tap(swipe, 225f, 392f);
        drainCountdown(swipe);
        // Leave a visible trail while swiping down.
        swipeStart(swipe, 225f, 145f);
        swipeMove(swipe, 225f, 255f);
        draw(swipe);
        save(swipe, "06_swipe.png");
        swipeEnd(swipe, 225f, 255f);

        GameSession swipeSession = (GameSession) field(swipe, "session");
        assertEquals(1, swipeSession.getEnteredCount());

        context.getSharedPreferences("stratagem_hero", Context.MODE_PRIVATE)
                .edit().putBoolean("swipe_mode", false)
                .putBoolean("free_call_mode", false).commit();
        GameView over = makeView();
        tap(over, 225f, 392f);
        drainCountdown(over);
        for (int i = 0; i < 40; i++) {
            tap(over, 225f, 45f);
        }
        draw(over);
        assertEquals("OVER", field(over, "screen").toString());
        save(over, "07_gameover.png");

        System.out.println("PREVIEW OK -> " + outDir.getAbsolutePath());
    }

    @Test
    public void renderFreeCallMode() throws Exception {
        context.getSharedPreferences("stratagem_hero", Context.MODE_PRIVATE)
                .edit().clear().commit();
        GameView free = makeView();
        tap(free, 270f, 322f);
        assertEquals(Boolean.TRUE, field(free, "freeCallMode"));
        save(free, "11_menu_free.png");

        tap(free, 225f, 392f);
        save(free, "12_free_game.png");

        // Enter Resupply from a blank screen: down, down, up, right.
        tap(free, 225f, 405f);
        tap(free, 225f, 405f);
        save(free, "12b_free_input.png");
        tap(free, 225f, 45f);
        tap(free, 405f, 225f);
        assertNotNull(field(free, "revealStratagem"));
        assertEquals("resupply",
                ((Stratagem) field(free, "revealStratagem")).id);

        ShadowSystemClock.advanceBy(Duration.ofMillis(300));
        draw(free);
        save(free, "13_free_reveal.png");
        assertEquals(0f, ((GameSession) field(free, "session")).getTimeLeft(), 0.0001f);

        ShadowSystemClock.advanceBy(Duration.ofMillis(2500));
        draw(free);
        assertEquals(null, field(free, "revealStratagem"));
        save(free, "14_free_after.png");

        GameSession session = (GameSession) field(free, "session");
        assertEquals("resupply", session.getLastCompleted().id);
        assertEquals(1, session.getCompleted());
        assertEquals(0, session.getScore());
        assertEquals(0, session.getCombo());
    }

    @Test
    public void renderRemoteMenu() throws Exception {
        context.getSharedPreferences("stratagem_hero", Context.MODE_PRIVATE)
                .edit().clear().commit();
        GameView view = makeView();
        tap(view, 313f, 322f);
        assertEquals(Boolean.TRUE, field(view, "remoteMode"));
        assertEquals(Boolean.FALSE, field(view, "freeCallMode"));
        save(view, "17_remote_menu.png");
    }
    @Test
    public void renderEnglishUI() throws Exception {
        GameView v = makeView();
        L10n.en = true;
        save(v, "21_en_menu.png");
        tap(v, 345f, 74f);
        save(v, "22_en_help.png");
        tap(v, 225f, 379f);
        tap(v, 225f, 392f);
        drainCountdown(v);
        save(v, "23_en_game.png");
        tap(v, 225f, 86f);
        save(v, "24_en_pause.png");
        tap(v, 225f, 364f);
        tap(v, 270f, 322f);
        save(v, "25_en_free_menu.png");
        tap(v, 313f, 322f);
        save(v, "26_en_keyboard_menu.png");
    }
    @Test
    public void renderSquareScreens() throws Exception {
        context.getSharedPreferences("stratagem_hero", Context.MODE_PRIVATE)
                .edit().clear().commit();
        GameView square = makeSquareView(450);
        assertTrue((Boolean) field(square, "squareMode"));
        save(square, "18_menu_square.png");
        tap(square, 225f, 392f);
        drainCountdown(square);
        save(square, "19_game_square.png");

        context.getSharedPreferences("stratagem_hero", Context.MODE_PRIVATE)
                .edit().clear().commit();
        GameView small = makeSquareView(396);
        assertTrue((Boolean) field(small, "squareMode"));
        save(small, "20_menu_square_396.png", 396);
    }
    @Test
    public void renderGroupFlow() throws Exception {
        context.getSharedPreferences("stratagem_hero", Context.MODE_PRIVATE)
                .edit().clear().commit();
        GameView group = makeView();
        tap(group, 225f, 392f);
        drainCountdown(group);

        for (int i = 0; i < 5; i++) {
            completeCurrentByTap(group);
            ShadowSystemClock.advanceBy(Duration.ofMillis(230));
            draw(group);
        }
        assertEquals("GROUP_RESULT", field(group, "screen").toString());
        GameSession session = (GameSession) field(group, "session");
        assertTrue(session.isGroupCleared());
        assertEquals(5, session.getGroupDone());
        assertTrue(session.getGroupScore() > 0);
        draw(group);
        save(group, "15_group_clear.png");

        tap(group, 225f, 336f);
        ShadowSystemClock.advanceBy(Duration.ofMillis(450));
        draw(group);
        assertEquals("PLAYING", field(group, "screen").toString());
        assertEquals(2, session.getGroupNumber());
        assertEquals(0, session.getGroupDone());
        assertEquals(28f, session.getGroupDuration(), 0.001f);
        assertTrue(session.getScore() > session.getGroupScore());
        save(group, "16_group2.png");
    }

    private void completeCurrentByTap(GameView v) {
        GameSession session = (GameSession) field(v, "session");
        Stratagem current = session.getCurrent();
        assertNotNull(current);
        for (Direction d : current.sequence) {
            tapDirection(v, d);
        }
    }

    private void tapDirection(GameView v, Direction d) {
        switch (d) {
            case UP: tap(v, 225f, 45f); break;
            case RIGHT: tap(v, 405f, 225f); break;
            case DOWN: tap(v, 225f, 405f); break;
            default: tap(v, 45f, 225f); break;
        }
    }
    @Test
    public void renderSmallWatch() throws Exception {
        context.getSharedPreferences("stratagem_hero", Context.MODE_PRIVATE)
                .edit().clear().commit();
        GameView small = makeView(396);
        save(small, "09_menu_396.png", 396);
        java.lang.reflect.Method start = GameView.class.getDeclaredMethod("startRun");
        start.setAccessible(true);
        start.invoke(small);
        drainCountdown(small, 396);
        save(small, "10_game_396.png", 396);
        System.out.println("saved 396x396 gallery");
    }

    @Test
    public void launchesActivity() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        assertNotNull(activity);
        View content = activity.findViewById(android.R.id.content);
        assertNotNull(content);
        assertTrue(((ViewGroup) content).getChildCount() > 0);
        activity.finish();
    }

    private static Object field(Object target, String name) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private GameView makeView() {
        return makeView(SIZE);
    }

    private GameView makeView(int size) {
        GameView v = new GameView(context, true);
        int spec = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
        v.measure(spec, spec);
        v.layout(0, 0, size, size);
        return v;
    }

    private GameView makeSquareView(int size) {
        GameView v = new GameView(context, false);
        int spec = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
        v.measure(spec, spec);
        v.layout(0, 0, size, size);
        return v;
    }

    private void drainCountdown(GameView v) {
        drainCountdown(v, SIZE);
    }

    private void drainCountdown(GameView v, int size) {
        for (int i = 0; i < 36; i++) {
            ShadowSystemClock.advanceBy(Duration.ofMillis(100));
            draw(v, size);
        }
    }

    private void draw(GameView v) {
        draw(v, SIZE);
    }

    private void draw(GameView v, int size) {
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.draw(c);
        b.recycle();
    }

    private void save(GameView v, String name) throws Exception {
        save(v, name, SIZE);
    }

    private void save(GameView v, String name, int size) throws Exception {
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.draw(c);
        FileOutputStream out = new FileOutputStream(new File(outDir, name));
        b.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.close();
        b.recycle();
        System.out.println("saved " + name);
    }

    private void tap(GameView v, float x, float y) {
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        v.dispatchTouchEvent(down);
        down.recycle();
        MotionEvent up = MotionEvent.obtain(now, now + 30, MotionEvent.ACTION_UP, x, y, 0);
        v.dispatchTouchEvent(up);
        up.recycle();
        ShadowSystemClock.advanceBy(Duration.ofMillis(45));
    }

    private void swipeStart(GameView v, float x, float y) {
        long now = SystemClock.uptimeMillis();
        MotionEvent e = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        v.dispatchTouchEvent(e);
        e.recycle();
    }

    private void swipeMove(GameView v, float x, float y) {
        long now = SystemClock.uptimeMillis();
        MotionEvent e = MotionEvent.obtain(now, now + 80, MotionEvent.ACTION_MOVE, x, y, 0);
        v.dispatchTouchEvent(e);
        e.recycle();
        ShadowSystemClock.advanceBy(Duration.ofMillis(30));
    }

    private void swipeEnd(GameView v, float x, float y) {
        long now = SystemClock.uptimeMillis();
        MotionEvent e = MotionEvent.obtain(now, now + 120, MotionEvent.ACTION_UP, x, y, 0);
        v.dispatchTouchEvent(e);
        e.recycle();
        ShadowSystemClock.advanceBy(Duration.ofMillis(45));
    }
}
