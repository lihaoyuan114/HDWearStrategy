package com.deepsea.stratagemhero;

/** Immutable data for one Helldivers 2 stratagem. */
public final class Stratagem {
    public final String id;
    public final String name;
    public final String nameZh;
    public final String category;
    public final String code;
    public final String assetName;
    public final Direction[] sequence;

    public Stratagem(String id, String name, String nameZh, String category, String code, String assetName) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("id");
        if (code == null || code.length() < 3 || code.length() > 9) {
            throw new IllegalArgumentException("Bad code for " + id + ": " + code);
        }
        this.id = id;
        this.name = name;
        this.nameZh = nameZh;
        this.category = category;
        this.code = code;
        this.assetName = assetName;
        this.sequence = new Direction[code.length()];
        for (int i = 0; i < code.length(); i++) {
            sequence[i] = Direction.fromCode(code.charAt(i));
        }
    }

    public int length() {
        return sequence.length;
    }

    public boolean hasCategory(String c) {
        return category.equals(c);
    }
}
