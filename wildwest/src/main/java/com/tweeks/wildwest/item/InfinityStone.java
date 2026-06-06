package com.tweeks.wildwest.item;

/**
 * The six Infinity Stones. Each constant carries the static config for
 * its ability: cooldown, durability cost, color (used for wedge fill +
 * particles), and a translation-key suffix.
 *
 * <p>The actual {@code cast(...)} logic lives in {@link InfinityGauntletItem}
 * keyed off the enum — the enum stays a pure value type for testability.
 */
public enum InfinityStone {

    POWER  (400, 2, 0xA020F0, "power"),
    SPACE  (300, 3, 0x1E90FF, "space"),
    TIME   (600, 4, 0x32CD32, "time"),
    MIND   (500, 3, 0xFFD700, "mind"),
    REALITY(200, 1, 0xFF4500, "reality"),
    SOUL   (240, 2, 0xFFA500, "soul");

    private final int cooldownTicks;
    private final int durabilityCost;
    private final int colorRgb;
    private final String translationSuffix;

    InfinityStone(int cooldownTicks, int durabilityCost, int colorRgb, String translationSuffix) {
        this.cooldownTicks = cooldownTicks;
        this.durabilityCost = durabilityCost;
        this.colorRgb = colorRgb;
        this.translationSuffix = translationSuffix;
    }

    public int cooldownTicks() { return cooldownTicks; }
    public int durabilityCost() { return durabilityCost; }
    public int colorRgb() { return colorRgb; }
    public String translationSuffix() { return translationSuffix; }

    public static InfinityStone byIndex(int index) {
        InfinityStone[] all = values();
        if (index < 0 || index >= all.length) return POWER;
        return all[index];
    }
}
