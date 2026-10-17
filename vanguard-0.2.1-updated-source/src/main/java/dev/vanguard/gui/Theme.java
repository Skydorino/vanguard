package dev.vanguard.gui;

public final class Theme {
    public static final int[] ACCENTS = {0xFFC800FF, 0xFF00D9FF, 0xFFFF3B5C, 0xFF3DFF8A, 0xFFFFB000};
    public static int index = 0;

    public static final int BG = 0xFF08080B;
    public static final int PANEL = 0xFF0F0F14;
    public static final int CARD = 0xFF13131A;
    public static final int CARD_BORDER = 0xFF1F1F29;
    public static final int OFF = 0xFF2B2B36;
    public static final int TEXT = 0xFFEDEDF2;
    public static final int DIM = 0xFF8A8A98;

    private Theme() {}

    public static int accent() {
        return ACCENTS[index % ACCENTS.length];
    }

    public static void next() {
        index = (index + 1) % ACCENTS.length;
    }
}
