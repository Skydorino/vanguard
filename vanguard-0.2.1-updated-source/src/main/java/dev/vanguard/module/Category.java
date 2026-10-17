package dev.vanguard.module;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    WORLD("World"),
    VISUALS("Visuals");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
