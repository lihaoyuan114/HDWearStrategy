package com.deepsea.stratagemhero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure game logic. Standard mode runs in groups of five random stratagems with a
 * shrinking group timer. Free-call mode is untimed and matches any known code.
 */
public final class GameSession {
    public static final int READY = 0;
    public static final int RUNNING = 1;
    public static final int OVER = 2;

    public static final int GROUP_SIZE = 5;
    public static final float BASE_GROUP_SECONDS = 30.0f;
    public static final float MIN_GROUP_SECONDS = 14.0f;
    public static final float WRONG_PENALTY = 1.2f;

    public enum Result {
        IGNORED,
        CORRECT,
        COMPLETE,
        WRONG
    }

    private final List<Stratagem> catalog;
    private final Random random;
    private final boolean freeCall;
    private final StringBuilder freeBuffer = new StringBuilder();
    private final List<Stratagem> groupTargets = new ArrayList<>();

    private int state = READY;
    private Stratagem current;
    private int entered;
    private int score;
    private int combo;
    private int bestCombo;
    private int completed;
    private int mistakes;
    private int correctSteps;
    private int totalSteps;
    private float timeLeft;
    private int lastGain;
    private float lastTimeBonus;
    private Stratagem lastCompleted;

    private int groupIndex;
    private int groupDone;
    private int groupStartScore;
    private int groupBestCombo;
    private float groupDuration;
    private boolean groupCleared;

    public GameSession(List<Stratagem> catalog, Random random) {
        this(catalog, random, false);
    }

    public GameSession(List<Stratagem> catalog, Random random, boolean freeCall) {
        this.catalog = new ArrayList<>(catalog);
        this.random = random;
        this.freeCall = freeCall;
        if (this.catalog.isEmpty()) {
            throw new IllegalArgumentException("Stratagem catalog is empty");
        }
    }

    public void start() {
        state = RUNNING;
        freeBuffer.setLength(0);
        entered = 0;
        score = 0;
        combo = 0;
        bestCombo = 0;
        completed = 0;
        mistakes = 0;
        correctSteps = 0;
        totalSteps = 0;
        timeLeft = 0f;
        lastGain = 0;
        lastTimeBonus = 0f;
        lastCompleted = null;
        groupIndex = 0;
        groupDone = 0;
        groupStartScore = 0;
        groupBestCombo = 0;
        groupCleared = false;
        groupTargets.clear();
        current = null;
        if (!freeCall) {
            startGroup(0);
        }
    }

    private void startGroup(int index) {
        groupIndex = index;
        groupDone = 0;
        groupCleared = false;
        groupStartScore = score;
        groupBestCombo = 0;
        combo = 0;
        groupDuration = Math.max(MIN_GROUP_SECONDS, BASE_GROUP_SECONDS - index * 2.0f);
        timeLeft = groupDuration;
        entered = 0;
        state = RUNNING;
        buildGroup(index);
        current = groupTargets.get(0);
    }

    private void buildGroup(int index) {
        int maxLength;
        if (index == 0) maxLength = 4;
        else if (index < 3) maxLength = 5;
        else if (index < 5) maxLength = 6;
        else maxLength = 7;

        groupTargets.clear();
        List<Stratagem> pool = new ArrayList<>();
        for (Stratagem s : catalog) {
            if (s.length() <= maxLength) pool.add(s);
        }
        if (index == 0) {
            Stratagem resupply = StratagemCatalog.find("resupply");
            if (resupply != null) {
                groupTargets.add(resupply);
                pool.remove(resupply);
            }
        }
        Collections.shuffle(pool, random);
        int need = GROUP_SIZE - groupTargets.size();
        for (int i = 0; i < need && i < pool.size(); i++) {
            groupTargets.add(pool.get(i));
        }
        // Defensive fallback for very small catalogs.
        int cursor = 0;
        while (groupTargets.size() < GROUP_SIZE && !catalog.isEmpty()) {
            Stratagem s = catalog.get(cursor++ % catalog.size());
            if (!groupTargets.contains(s)) groupTargets.add(s);
            if (cursor > catalog.size() * 3) break;
        }
    }

    public void update(float dtSeconds) {
        if (state != RUNNING || freeCall || groupCleared) return;
        if (dtSeconds <= 0f) return;
        if (dtSeconds > 0.10f) dtSeconds = 0.10f;
        timeLeft -= dtSeconds;
        if (timeLeft <= 0f) {
            timeLeft = 0f;
            state = OVER;
        }
    }

    public Result input(Direction dir) {
        if (state != RUNNING || dir == null) return Result.IGNORED;
        if (freeCall) return inputFree(dir);
        if (groupCleared || current == null) return Result.IGNORED;

        totalSteps++;
        Direction expected = current.sequence[entered];
        if (dir != expected) {
            mistakes++;
            combo = 0;
            entered = 0;
            timeLeft = Math.max(0f, timeLeft - WRONG_PENALTY);
            if (timeLeft <= 0f) {
                timeLeft = 0f;
                state = OVER;
            }
            return Result.WRONG;
        }

        correctSteps++;
        entered++;
        if (entered < current.sequence.length) return Result.CORRECT;

        award(current);
        groupDone++;
        if (groupDone >= GROUP_SIZE) {
            groupCleared = true;
            current = null;
        } else {
            current = groupTargets.get(groupDone);
            entered = 0;
        }
        return Result.COMPLETE;
    }

    private Result inputFree(Direction dir) {
        totalSteps++;
        freeBuffer.append(dir.code);
        String code = freeBuffer.toString();

        Stratagem exact = null;
        boolean isPrefix = false;
        for (Stratagem s : catalog) {
            if (s.code.startsWith(code)) {
                isPrefix = true;
                if (s.code.equals(code)) {
                    exact = s;
                    break;
                }
            }
        }

        if (exact == null && !isPrefix) {
            mistakes++;
            combo = 0;
            freeBuffer.setLength(0);
            return Result.WRONG;
        }

        correctSteps++;
        if (exact != null) {
            award(exact);
            freeBuffer.setLength(0);
            return Result.COMPLETE;
        }
        return Result.CORRECT;
    }

    private void award(Stratagem s) {
        completed++;
        lastCompleted = s;
        if (freeCall) {
            // Free-call is a pure sandbox: no score, no combo, no time.
            lastGain = 0;
            lastTimeBonus = 0f;
            return;
        }
        combo++;
        if (combo > bestCombo) bestCombo = combo;
        if (combo > groupBestCombo) groupBestCombo = combo;
        int base = 100 + (s.length() - 3) * 20;
        float multiplier = 1.0f + 0.15f * Math.min(combo - 1, 10);
        lastGain = Math.round(base * multiplier);
        lastTimeBonus = 0f;
        score += lastGain;
    }

    public boolean continueGroup() {
        if (freeCall || !groupCleared) return false;
        startGroup(groupIndex + 1);
        return true;
    }

    public int maxLength() {
        if (groupIndex == 0) return 4;
        if (groupIndex < 3) return 5;
        if (groupIndex < 5) return 6;
        return 7;
    }

    public int getState() { return state; }
    public boolean isOver() { return state == OVER; }
    public Stratagem getCurrent() { return current; }
    public int getEnteredCount() { return freeCall ? freeBuffer.length() : entered; }
    public String getInputCode() { return freeBuffer.toString(); }
    public boolean isFreeCall() { return freeCall; }
    public int getScore() { return score; }
    public int getCombo() { return combo; }
    public int getBestCombo() { return bestCombo; }
    public int getCompleted() { return completed; }
    public int getMistakes() { return mistakes; }
    public int getCorrectSteps() { return correctSteps; }
    public int getTotalSteps() { return totalSteps; }
    public float getTimeLeft() { return freeCall ? 0f : timeLeft; }
    public int getLastGain() { return lastGain; }
    public float getLastTimeBonus() { return lastTimeBonus; }
    public Stratagem getLastCompleted() { return lastCompleted; }
    public int getMaxLength() { return maxLength(); }

    public int getGroupIndex() { return groupIndex; }
    public int getGroupNumber() { return groupIndex + 1; }
    public int getGroupDone() { return groupDone; }
    public int getGroupSize() { return GROUP_SIZE; }
    public int getGroupScore() { return score - groupStartScore; }
    public int getGroupBestCombo() { return groupBestCombo; }
    public float getGroupDuration() { return groupDuration; }
    public boolean isGroupCleared() { return groupCleared; }

    public float getGroupTimeFraction() {
        if (freeCall || groupDuration <= 0f) return 0f;
        return Math.max(0f, Math.min(1f, timeLeft / groupDuration));
    }

    public float getAccuracy() {
        if (totalSteps == 0) return 1f;
        return correctSteps / (float) totalSteps;
    }

    /** A small letter grade shown on the results screen. */
    public char getRank() {
        if (score >= 9000) return 'S';
        if (score >= 6000) return 'A';
        if (score >= 3500) return 'B';
        if (score >= 1800) return 'C';
        return 'D';
    }

    /** 0..1 progress through the current code, or through a free-call prefix. */
    public float getProgress() {
        if (freeCall) return Math.min(1f, freeBuffer.length() / 7f);
        if (current == null || current.length() == 0) return 0f;
        return entered / (float) current.length();
    }
}
