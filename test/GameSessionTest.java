import com.deepsea.stratagemhero.Direction;
import com.deepsea.stratagemhero.GameSession;
import com.deepsea.stratagemhero.Stratagem;
import com.deepsea.stratagemhero.StratagemCatalog;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Desktop-only checks for the pure game logic. No Android SDK required. */
public final class GameSessionTest {
    private static int checks;

    public static void main(String[] args) {
        testCatalog();
        testFirstGroup();
        testWrongInput();
        testGroupProgression();
        testTimer();
        testRandomStress();
        testAccuracy();
        testFreeCall();
        System.out.println("PASS: " + checks + " checks");
    }

    private static void testCatalog() {
        check(StratagemCatalog.ALL.size() == 45, "catalog has 45 stratagems");
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (Stratagem s : StratagemCatalog.ALL) {
            check(ids.add(s.id), "unique id " + s.id);
            check(names.add(s.name), "unique name " + s.name);
            check(s.sequence.length == s.code.length(), "sequence length " + s.id);
            check(s.sequence.length >= 3 && s.sequence.length <= 7, "length in range " + s.id);
            for (Direction d : s.sequence) check(d != null, "direction " + s.id);
            check(s.nameZh != null && !s.nameZh.isEmpty(), "zh name " + s.id);
            check(s.category.equals(StratagemCatalog.MISSION)
                    || s.category.equals(StratagemCatalog.SUPPORT)
                    || s.category.equals(StratagemCatalog.OFFENSIVE)
                    || s.category.equals(StratagemCatalog.BACKPACK)
                    || s.category.equals(StratagemCatalog.SENTRY)
                    || s.category.equals(StratagemCatalog.DEFENSIVE), "category " + s.id);
        }
    }

    private static void testFirstGroup() {
        GameSession s = new GameSession(StratagemCatalog.ALL, new Random(1));
        s.start();
        check(s.getState() == GameSession.RUNNING, "running after start");
        check("resupply".equals(s.getCurrent().id), "first target is resupply");
        check(s.getGroupNumber() == 1, "first group number");
        check(s.getGroupDone() == 0, "empty group");
        check(s.getGroupSize() == 5, "group size five");
        check(Math.abs(s.getGroupDuration() - 30f) < 0.001f, "first group duration");
        check(Math.abs(s.getTimeLeft() - s.getGroupDuration()) < 0.001f, "full group bar");
        check(!s.isGroupCleared(), "not cleared at start");
        check(s.getEnteredCount() == 0, "zero entered");
    }

    private static void testWrongInput() {
        GameSession s = new GameSession(StratagemCatalog.ALL, new Random(2));
        s.start();
        Direction expected = s.getCurrent().sequence[0];
        Direction wrong = expected == Direction.UP ? Direction.DOWN : Direction.UP;
        float before = s.getTimeLeft();
        check(s.input(wrong) == GameSession.Result.WRONG, "wrong returns WRONG");
        check(s.getMistakes() == 1, "mistake counted");
        check(s.getEnteredCount() == 0, "wrong resets progress");
        check(s.getCombo() == 0, "wrong resets combo");
        check(s.getTimeLeft() < before - 1.1f, "wrong costs group time");
    }

    private static void testGroupProgression() {
        GameSession s = new GameSession(StratagemCatalog.ALL, new Random(3));
        s.start();
        check(s.maxLength() == 4, "group 1 max length 4");

        for (int i = 0; i < 5; i++) {
            check(s.getCurrent().length() <= 4, "group 1 difficulty cap");
            completeCurrent(s);
        }
        check(s.getGroupDone() == 5, "five completed in group");
        check(s.isGroupCleared(), "group cleared");
        check(!s.isOver(), "group clear is not a fail");
        check(s.getGroupScore() > 0, "group score positive");
        check(s.getBestCombo() >= 5, "combo across group");
        float stopped = s.getTimeLeft();
        s.update(5f);
        check(Math.abs(stopped - s.getTimeLeft()) < 0.001f, "group timer stops on clear");

        check(s.continueGroup(), "continue allowed");
        check(s.getGroupNumber() == 2, "second group");
        check(s.getGroupDone() == 0, "second group empty");
        check(Math.abs(s.getGroupDuration() - 28f) < 0.001f, "second group faster");
        check(s.getCombo() == 0, "combo reset per group");
        check(!s.isGroupCleared(), "second group running");

        for (int i = 0; i < 5; i++) {
            check(s.getCurrent().length() <= 5, "group 2 difficulty cap");
            completeCurrent(s);
        }
        check(s.isGroupCleared(), "second group cleared");
        check(s.continueGroup(), "continue to third group");
        check(s.getGroupNumber() == 3, "third group");
        check(Math.abs(s.getGroupDuration() - 26f) < 0.001f, "third group faster still");
        check(s.getScore() > s.getGroupScore(), "score carries across groups");
    }

    private static void testTimer() {
        GameSession s = new GameSession(StratagemCatalog.ALL, new Random(4));
        s.start();
        while (s.getTimeLeft() > 0f) s.update(0.1f);
        for (int i = 0; i < 10; i++) s.update(0.1f);
        check(s.isOver(), "group timer ends run");
        check(s.getTimeLeft() == 0f, "time clamps to zero");
    }

    private static void testRandomStress() {
        Random rng = new Random(5);
        GameSession s = new GameSession(StratagemCatalog.ALL, rng);
        s.start();
        for (int i = 0; i < 12000 && !s.isOver(); i++) {
            if (s.isGroupCleared()) {
                check(s.continueGroup(), "stress continue group");
            }
            if (i % 3 == 0) s.update(0.05f);
            Direction d = Direction.values()[rng.nextInt(4)];
            s.input(d);
            check(s.getTimeLeft() >= 0f, "time bounds");
            check(s.getEnteredCount() >= 0, "entered non-negative");
            if (s.getCurrent() != null) {
                check(s.getEnteredCount() <= s.getCurrent().length(), "entered bounded");
            }
        }
        check(s.getScore() >= 0, "score non-negative");

        GameSession free = new GameSession(StratagemCatalog.ALL, new Random(6), true);
        free.start();
        for (int i = 0; i < 5000; i++) {
            free.update(1f);
            free.input(Direction.values()[rng.nextInt(4)]);
        }
        check(!free.isOver(), "free-call never times out");
        check(free.getTimeLeft() == 0f, "free-call has no timer");
    }

    private static void testAccuracy() {
        GameSession s = new GameSession(StratagemCatalog.ALL, new Random(7));
        s.start();
        completeCurrent(s);
        check(s.getLastCompleted() != null, "last completed recorded");
        check("resupply".equals(s.getLastCompleted().id), "last completed is resupply");
        check(s.getAccuracy() > 0.99f, "perfect accuracy after completion");
        Direction expected = s.getCurrent().sequence[0];
        Direction wrong = expected == Direction.UP ? Direction.DOWN : Direction.UP;
        s.input(wrong);
        check(s.getAccuracy() < 1f, "accuracy drops after mistake");
    }

    private static void testFreeCall() {
        GameSession s = new GameSession(StratagemCatalog.ALL, new Random(8), true);
        s.start();
        check(s.isFreeCall(), "free-call mode");
        check(s.getCurrent() == null, "free-call has no target");
        check(s.getInputCode().isEmpty(), "free-call starts blank");
        check(s.getTimeLeft() == 0f, "free-call has no timer");
        s.update(999f);
        check(!s.isOver(), "free-call update does not expire");

        check(s.input(Direction.LEFT) == GameSession.Result.WRONG,
                "unmatched arrow is rejected");
        check(s.getInputCode().isEmpty(), "invalid input clears buffer");
        check(s.getTimeLeft() == 0f, "free-call wrong has no time penalty");

        check(s.input(Direction.DOWN) == GameSession.Result.CORRECT,
                "valid prefix accepted");
        check("D".equals(s.getInputCode()), "prefix buffer");
        check(s.input(Direction.DOWN) == GameSession.Result.CORRECT,
                "second prefix arrow accepted");
        check(s.input(Direction.UP) == GameSession.Result.CORRECT,
                "third prefix arrow accepted");
        check(s.input(Direction.RIGHT) == GameSession.Result.COMPLETE,
                "full code calls stratagem");
        check(s.getLastCompleted() != null, "free-call completed target recorded");
        check("resupply".equals(s.getLastCompleted().id),
                "free-call matched resupply");
        check(s.getInputCode().isEmpty(), "free-call buffer clears after call");
        check(s.getCompleted() == 1, "free-call completion counted");
        check(s.getScore() == 0, "free-call has no score");
        check(s.getCombo() == 0, "free-call has no combo");
        check(s.getLastGain() == 0, "free-call has no gain");
        check(!s.isOver(), "free-call stays running");
    }

    private static void completeCurrent(GameSession s) {
        Stratagem current = s.getCurrent();
        if (current == null) throw new AssertionError("no current target");
        for (int i = 0; i < current.sequence.length; i++) {
            GameSession.Result r = s.input(current.sequence[i]);
            if (i == current.sequence.length - 1) {
                check(r == GameSession.Result.COMPLETE, "last input completes " + current.id);
            } else {
                check(r == GameSession.Result.CORRECT, "correct input " + current.id + " #" + i);
            }
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError("FAIL: " + message);
        }
    }
}
