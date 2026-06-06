package com.tweeks.wildwest.item;

/**
 * Pure helpers for the per-stone cooldown {@code long[]} stored on the
 * gauntlet stack's {@link ModDataComponents#COOLDOWNS} component. Tests
 * cover these directly; the {@code Item} class is a thin wrapper.
 */
public final class InfinityCooldowns {
    private InfinityCooldowns() {}

    public static final int SLOT_COUNT = 6;

    public static boolean isOnCooldown(long[] cooldowns, int stoneIndex, long nowTick) {
        if (cooldowns == null || stoneIndex < 0 || stoneIndex >= cooldowns.length) {
            return false;
        }
        return nowTick < cooldowns[stoneIndex];
    }

    /**
     * Returns a new {@code long[]} of length {@link #SLOT_COUNT} with
     * {@code [stoneIndex]} set to {@code nowTick + cooldownTicks}. Other
     * entries copied from {@code cooldowns} (zero-padded if shorter).
     */
    public static long[] applyCooldown(long[] cooldowns, int stoneIndex, long nowTick, int cooldownTicks) {
        long[] next = new long[SLOT_COUNT];
        if (cooldowns != null) {
            System.arraycopy(cooldowns, 0, next, 0, Math.min(cooldowns.length, SLOT_COUNT));
        }
        if (stoneIndex >= 0 && stoneIndex < SLOT_COUNT) {
            next[stoneIndex] = nowTick + cooldownTicks;
        }
        return next;
    }

    public static long[] emptyCooldowns() {
        return new long[SLOT_COUNT];
    }
}
