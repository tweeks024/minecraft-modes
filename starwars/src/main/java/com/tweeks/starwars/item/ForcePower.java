package com.tweeks.starwars.item;

import com.tweeks.starwars.faction.Alignment;

public enum ForcePower {
    PUSH(60, 0),
    PULL(60, 0),
    LEAP(120, Alignment.POWER_DELTA),
    MIND_TRICK(300, Alignment.POWER_DELTA),
    LIGHTNING(200, -Alignment.POWER_DELTA);

    private final int cooldownTicks;
    private final int alignmentDelta;

    ForcePower(int cooldownTicks, int alignmentDelta) {
        this.cooldownTicks = cooldownTicks;
        this.alignmentDelta = alignmentDelta;
    }

    public int cooldownTicks() { return cooldownTicks; }
    public int alignmentDelta() { return alignmentDelta; }
    public String translationKey() {
        return "force_power.starwars." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static ForcePower byIndex(int index) {
        return (index < 0 || index >= values().length) ? PUSH : values()[index];
    }
}
