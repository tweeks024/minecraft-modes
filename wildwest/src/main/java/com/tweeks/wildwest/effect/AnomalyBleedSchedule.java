package com.tweeks.wildwest.effect;

/**
 * Pure schedule for the anomaly bleed DoT, extracted from
 * {@link AnomalyBleedEffect} so its behaviour can be unit-tested without
 * bootstrapping Minecraft (any class extending {@code MobEffect} drags
 * {@code BuiltInRegistries} into its &lt;clinit&gt; and refuses to load
 * outside the FML/Bootstrap environment).
 */
public final class AnomalyBleedSchedule {

    /** Bleed lasts 4 seconds = 80 ticks after each hit. */
    public static final int BLEED_DURATION_TICKS = 80;

    /** Half a heart per second. */
    public static final float DAMAGE_PER_TICK = 1.0f;

    /** Once per second = once per 20 server ticks. */
    static final int TICK_INTERVAL = 20;

    private AnomalyBleedSchedule() {}

    /** Returns true on the tick boundaries where bleed damage should fire. */
    public static boolean shouldTickAt(int duration) {
        return duration % TICK_INTERVAL == 0;
    }
}
