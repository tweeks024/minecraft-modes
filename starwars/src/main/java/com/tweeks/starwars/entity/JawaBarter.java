package com.tweeks.starwars.entity;

import java.util.random.RandomGenerator;

/**
 * Pure jawa barter logic: the per-jawa cooldown decision and the weighted
 * tech-loot roll table. Lives outside MC classes (no Minecraft imports) so
 * it can be unit-tested without booting the FML loader — the entity maps
 * {@link TechItem} to real {@code Item}s and handles the world side
 * (consuming the ingot, spawning the drop, playing the sound).
 */
public final class JawaBarter {
    private JawaBarter() {}

    /** 10 s between barters, per jawa. */
    public static final int COOLDOWN_TICKS = 200;

    /**
     * Sentinel for "never bartered": ready immediately at game time 0.
     * (Not {@code Long.MIN_VALUE} — {@code now - last} must not overflow.)
     */
    public static final long NEVER_BARTERED = -COOLDOWN_TICKS;

    /** Weighted table entries. Weights sum to {@link #TOTAL_WEIGHT}. */
    public enum TechItem {
        REDSTONE(30),
        COPPER_INGOT(30),
        QUARTZ(20),
        IRON_NUGGET(15),
        GLOWSTONE_DUST(5);   // the rare one

        public final int weight;

        TechItem(int weight) { this.weight = weight; }
    }

    public static final int TOTAL_WEIGHT = 100;

    /** One barter result: which tech item and how many. */
    public record Roll(TechItem item, int count) {}

    /** True once {@link #COOLDOWN_TICKS} have elapsed since the last barter. */
    public static boolean isReady(long lastBarterGameTime, long now) {
        return now - lastBarterGameTime >= COOLDOWN_TICKS;
    }

    /**
     * Maps a uniform roll in {@code [0, TOTAL_WEIGHT)} onto the weighted
     * table. Deterministic — exposed for direct boundary tests.
     */
    public static TechItem pick(int weightRoll) {
        if (weightRoll < 0 || weightRoll >= TOTAL_WEIGHT) {
            throw new IllegalArgumentException("weightRoll out of range: " + weightRoll);
        }
        int cursor = 0;
        for (TechItem item : TechItem.values()) {
            cursor += item.weight;
            if (weightRoll < cursor) return item;
        }
        throw new IllegalStateException("weights do not sum to TOTAL_WEIGHT");
    }

    /** Count range per item: redstone 2-4, copper 2-3, nuggets 4, rest 1. */
    public static int countFor(TechItem item, RandomGenerator random) {
        return switch (item) {
            case REDSTONE -> 2 + random.nextInt(3);       // 2-4
            case COPPER_INGOT -> 2 + random.nextInt(2);   // 2-3
            case QUARTZ -> 1;
            case IRON_NUGGET -> 4;
            case GLOWSTONE_DUST -> 1;
        };
    }

    /** Full weighted roll: pick the item, then its count. */
    public static Roll roll(RandomGenerator random) {
        TechItem item = pick(random.nextInt(TOTAL_WEIGHT));
        return new Roll(item, countFor(item, random));
    }
}
