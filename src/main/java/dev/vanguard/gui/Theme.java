package dev.vanguard.gui;

public final class Theme {
    public static final int[] ACCENTS = {0xFF7C5CFF, 0xFFC800FF, 0xFF00D9FF, 0xFFFF3B5C, 0xFF3DFF8A, 0xFFFFB000};
    public static int index = 0;

    public static final int BG = 0xF00A0A10;
    public static final int PANEL = 0xFF0F0F16;
    public static final int CARD = 0xFF12121A;
    public static final int CARD_BORDER = 0xFF1C1C26;
    public static final int OFF = 0xFF2B2B38;
    public static final int TEXT = 0xFFF1F1F6;
    public static final int DIM = 0xFF6E6E80;

    private Theme() {}

    public static int accent() {
        return ACCENTS[index % ACCENTS.length];
    }

    public static void next() {
        index = (index + 1) % ACCENTS.length;
    }
}
