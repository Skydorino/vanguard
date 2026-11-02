package dev.vanguard.module;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    VISUALS("Visuals"),
    MISC("Misc"),
    DONUTSMP("DonutSMP"),
    BEDWARS("Bedwars");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
