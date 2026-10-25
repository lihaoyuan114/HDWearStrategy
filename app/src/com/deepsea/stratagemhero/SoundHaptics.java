package com.deepsea.stratagemhero;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.util.Random;

/**
 * Game-audio bank extracted from HELLDIVERS 2 plus watch haptics.
 * Stratagem Hero cues are used for the arcade loop; call-in voice lines are
 * grouped the same way as the original ship's Mission Control announcements.
 */
final class SoundHaptics {
    private final SoundPool soundPool;
    private final Vibrator vibrator;
    private final Random random = new Random();

    private final int[] inputBeeps;
    private final int inputOkId;
    private final int inputFailId;
    private final int heroReadyId;
    private final int roundCompleteA;
    private final int roundCompleteB;
    private final int roundFailedId;
    private final int ballLandId;
    private final int heroLoopId;

    private final int[] voiceSupport;
    private final int[] voiceEquipment;
    private final int[] voiceSentry;
    private final int[] voiceFortification;
    private final int[] voiceEagle;
    private final int[] voiceOrbital;
    private final int[] voiceBarrage;
    private final int voiceEmplacement;
    private final int voiceResupply;
    private final int voiceReinforce;
    private final int voiceSos;

    private boolean enabled = true;
    private int beepCursor;
    private int loopStream;

    SoundHaptics(Context context) {
        Context app = context.getApplicationContext();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(attrs)
                .build();

        inputBeeps = new int[]{
                load(app, R.raw.input_beep_1),
                load(app, R.raw.input_beep_2),
                load(app, R.raw.input_beep_3),
                load(app, R.raw.input_beep_4)
        };
        inputOkId = load(app, R.raw.input_ok);
        inputFailId = load(app, R.raw.input_fail);
        heroReadyId = load(app, R.raw.hero_ready);
        roundCompleteA = load(app, R.raw.hero_round_complete_1);
        roundCompleteB = load(app, R.raw.hero_round_complete_2);
        roundFailedId = load(app, R.raw.hero_round_failed);
        ballLandId = load(app, R.raw.ball_land);
        heroLoopId = load(app, R.raw.hero_loop);

        voiceSupport = new int[]{
                load(app, R.raw.voice_support_1),
                load(app, R.raw.voice_support_2)
        };
        voiceEquipment = new int[]{
                load(app, R.raw.voice_equipment_1),
                load(app, R.raw.voice_equipment_2)
        };
        voiceSentry = new int[]{
                load(app, R.raw.voice_sentry_1),
                load(app, R.raw.voice_sentry_2)
        };
        voiceFortification = new int[]{
                load(app, R.raw.voice_fort_1),
                load(app, R.raw.voice_fort_2)
        };
        voiceEagle = new int[]{
                load(app, R.raw.voice_eagle_1),
                load(app, R.raw.voice_eagle_2)
        };
        voiceOrbital = new int[]{
                load(app, R.raw.voice_orbital_1),
                load(app, R.raw.voice_orbital_2)
        };
        voiceBarrage = new int[]{load(app, R.raw.voice_barrage)};
        voiceEmplacement = load(app, R.raw.voice_emplacement);
        voiceResupply = load(app, R.raw.voice_resupply);
        voiceReinforce = load(app, R.raw.voice_reinforce);
        voiceSos = load(app, R.raw.voice_sos);

        vibrator = (Vibrator) app.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private int load(Context context, int resId) {
        return soundPool.load(context, resId, 1);
    }

    void setEnabled(boolean on) {
        enabled = on;
        if (!on) stopHeroLoop();
    }

    void startHeroLoop() {
        if (!enabled || loopStream != 0) return;
        loopStream = soundPool.play(heroLoopId, 0.24f, 0.24f, 1, -1, 1.0f);
    }

    void pauseHeroLoop() {
        if (!enabled || loopStream == 0) return;
        soundPool.pause(loopStream);
    }

    void resumeHeroLoop() {
        if (!enabled) return;
        if (loopStream != 0) soundPool.resume(loopStream);
        else startHeroLoop();
    }

    void stopHeroLoop() {
        if (loopStream == 0) return;
        soundPool.stop(loopStream);
        loopStream = 0;
    }

    boolean isEnabled() {
        return enabled;
    }

    private void play(int id, float volume, float rate) {
        if (!enabled || id == 0) return;
        soundPool.play(id, volume, volume, 1, 0, rate);
    }

    void tick() {
        int id = inputBeeps[beepCursor++ % inputBeeps.length];
        play(id, 0.55f, 1.0f);
        vibrateOneShot(9, 75);
    }

    void success() {
        play(inputOkId, 0.72f, 1.0f);
        successBuzz();
    }

    void error() {
        play(inputFailId, 0.68f, 1.0f);
        errorBuzz();
    }

    void start() {
        play(heroReadyId, 0.8f, 1.0f);
        vibrateOneShot(22, 125);
    }

    void countdown(boolean last) {
        play(inputBeeps[last ? 3 : 1], 0.45f, last ? 1.04f : 0.9f);
        vibrateOneShot(last ? 25 : 13, last ? 140 : 90);
    }

    void gameOver() {
        play(roundFailedId, 0.85f, 1.0f);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] timing = {0, 90, 55, 180};
        int[] amps = {0, 150, 0, 190};
        vibrator.vibrate(VibrationEffect.createWaveform(timing, amps, -1));
    }

    void roundComplete() {
        play(random.nextBoolean() ? roundCompleteA : roundCompleteB, 0.82f, 1.0f);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] timing = {0, 18, 35, 28, 45, 45};
        int[] amps = {0, 120, 0, 140, 0, 190};
        vibrator.vibrate(VibrationEffect.createWaveform(timing, amps, -1));
    }

    /** A HELLDIVERS 2 call-in: beacon/ball landing plus a grouped Mission Control voice line. */
    void callIn(Stratagem s) {
        play(ballLandId, 0.82f, 1.0f);
        int voice = voiceFor(s);
        play(voice, 0.9f, 1.0f);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] timing = {0, 14, 30, 18, 45, 90};
        int[] amps = {0, 90, 0, 120, 0, 160};
        vibrator.vibrate(VibrationEffect.createWaveform(timing, amps, -1));
    }

    private int voiceFor(Stratagem s) {
        if (s == null) return 0;
        if ("resupply".equals(s.id)) return voiceResupply;
        if ("reinforce".equals(s.id)) return voiceReinforce;
        if ("sos_beacon".equals(s.id)) return voiceSos;
        if (s.hasCategory(StratagemCatalog.SUPPORT)) return pick(voiceSupport);
        if (s.hasCategory(StratagemCatalog.BACKPACK)) return pick(voiceEquipment);
        if (s.hasCategory(StratagemCatalog.SENTRY)) return pick(voiceSentry);
        if (s.hasCategory(StratagemCatalog.DEFENSIVE)) {
            return "hmg_emplacement".equals(s.id) ? voiceEmplacement : pick(voiceFortification);
        }
        if (s.hasCategory(StratagemCatalog.OFFENSIVE)) {
            if (s.id.startsWith("eagle_")) return pick(voiceEagle);
            if (s.id.startsWith("orbital_")) {
                if (s.id.contains("barrage") || s.id.contains("gatling")
                        || s.id.contains("walking") || s.id.contains("napalm")
                        || s.id.contains("380") || s.id.contains("120")) {
                    return pick(voiceBarrage);
                }
                return pick(voiceOrbital);
            }
        }
        return pick(voiceEquipment);
    }

    private int pick(int[] ids) {
        if (ids == null || ids.length == 0) return 0;
        return ids[random.nextInt(ids.length)];
    }

    private void successBuzz() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] timing = {0, 12, 28, 16};
        int[] amps = {0, 120, 0, 75};
        vibrator.vibrate(VibrationEffect.createWaveform(timing, amps, -1));
    }

    private void errorBuzz() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] timing = {0, 32, 28, 42};
        int[] amps = {0, 180, 0, 140};
        vibrator.vibrate(VibrationEffect.createWaveform(timing, amps, -1));
    }

    private void vibrateOneShot(long ms, int amplitude) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        vibrator.vibrate(VibrationEffect.createOneShot(ms, amplitude));
    }

    void release() {
        stopHeroLoop();
        soundPool.release();
    }
}
