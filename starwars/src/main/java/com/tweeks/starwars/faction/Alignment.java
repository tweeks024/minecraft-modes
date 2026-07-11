package com.tweeks.starwars.faction;

/**
 * Pure alignment math. Positive score = light-side player, negative = dark.
 * Harming EMPIRE raises the score; harming LIGHT lowers it. A faction turns
 * hostile once the player is a champion of its enemy (|score| >= threshold
 * on that side). Lives outside MC classes for unit testing.
 */
public final class Alignment {
    private Alignment() {}

    public static final int HOSTILE_THRESHOLD = 50;
    public static final int KILL_DELTA = 5;
    public static final int HIT_DELTA = 1;
    public static final int POWER_DELTA = 2;
    public static final int MIN = -100;
    public static final int MAX = 100;

    public static int clamp(int score) {
        return Math.max(MIN, Math.min(MAX, score));
    }

    public static int deltaForKill(SwFaction victim) {
        return switch (victim) {
            case EMPIRE -> KILL_DELTA;
            case LIGHT -> -KILL_DELTA;
            case NEUTRAL -> 0;
        };
    }

    public static int deltaForHit(SwFaction victim) {
        return switch (victim) {
            case EMPIRE -> HIT_DELTA;
            case LIGHT -> -HIT_DELTA;
            case NEUTRAL -> 0;
        };
    }

    public static int deltaForPower(boolean lightSide) {
        return lightSide ? POWER_DELTA : -POWER_DELTA;
    }

    /** True if {@code faction} auto-targets a player with {@code score}. */
    public static boolean isHostileTo(int score, SwFaction faction) {
        return switch (faction) {
            case EMPIRE -> score >= HOSTILE_THRESHOLD;
            case LIGHT -> score <= -HOSTILE_THRESHOLD;
            case NEUTRAL -> false;
        };
    }
}
