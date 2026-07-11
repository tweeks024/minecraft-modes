package com.tweeks.starwars.item;

/** Lightsaber blade colors. Ordinal is persisted via {@link ModDataComponents#BLADE_COLOR}. */
public enum SaberColor {
    BLUE(0xFF3060FF, "blue"),
    GREEN(0xFF30E050, "green"),
    RED(0xFFFF2020, "red"),
    PURPLE(0xFFA030E0, "purple");

    private final int argb;
    private final String suffix;

    SaberColor(int argb, String suffix) {
        this.argb = argb;
        this.suffix = suffix;
    }

    public int argb() { return argb; }
    public String suffix() { return suffix; }

    public static SaberColor byIndex(int index) {
        return (index < 0 || index >= values().length) ? BLUE : values()[index];
    }
}
