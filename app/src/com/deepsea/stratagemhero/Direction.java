package com.deepsea.stratagemhero;

/** The four stratagem input directions, in the same order as the original game. */
public enum Direction {
    UP('U'),
    RIGHT('R'),
    DOWN('D'),
    LEFT('L');

    public final char code;

    Direction(char code) {
        this.code = code;
    }

    public static Direction fromCode(char c) {
        switch (c) {
            case 'U': return UP;
            case 'R': return RIGHT;
            case 'D': return DOWN;
            case 'L': return LEFT;
            default: throw new IllegalArgumentException("Bad stratagem direction: " + c);
        }
    }

    public static Direction fromString(String code) {
        if (code == null || code.length() != 1) {
            throw new IllegalArgumentException("Bad stratagem direction: " + code);
        }
        return fromCode(code.charAt(0));
    }
}
