package com.tweeks.wildwest.item;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure helpers for the per-stone cooldown {@code List<Long>} stored on
 * the gauntlet stack's {@link ModDataComponents#COOLDOWNS} component.
 *
 * <p>Why {@code List<Long>} and not {@code long[]}: NeoForge's
 * {@code CommonHooks.validateComponent} rejects raw arrays because Java
 * arrays use identity equality, while data-component sync requires
 * value equality to detect changes. {@code List} is value-equal by
 * contract and immutable variants are easy to produce.
 */
public final class InfinityCooldowns {
    private InfinityCooldowns() {}

    public static final int SLOT_COUNT = 6;

    public static boolean isOnCooldown(List<Long> cooldowns, int stoneIndex, long nowTick) {
        if (cooldowns == null || stoneIndex < 0 || stoneIndex >= cooldowns.size()) {
            return false;
        }
        Long expiry = cooldowns.get(stoneIndex);
        return expiry != null && nowTick < expiry;
    }

    /** Returns the absolute expiry tick at {@code stoneIndex}, or 0 if unset. */
    public static long getExpiry(List<Long> cooldowns, int stoneIndex) {
        if (cooldowns == null || stoneIndex < 0 || stoneIndex >= cooldowns.size()) {
            return 0L;
        }
        Long expiry = cooldowns.get(stoneIndex);
        return expiry == null ? 0L : expiry;
    }

    /**
     * Returns a new immutable {@code List<Long>} of length {@link #SLOT_COUNT}
     * with {@code [stoneIndex]} set to {@code nowTick + cooldownTicks}.
     * Other entries copied from {@code cooldowns} (zero-padded if shorter).
     */
    public static List<Long> applyCooldown(List<Long> cooldowns, int stoneIndex, long nowTick, int cooldownTicks) {
        List<Long> next = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (i == stoneIndex) {
                next.add(nowTick + cooldownTicks);
            } else if (cooldowns != null && i < cooldowns.size()) {
                Long v = cooldowns.get(i);
                next.add(v == null ? 0L : v);
            } else {
                next.add(0L);
            }
        }
        return List.copyOf(next);
    }

    public static List<Long> emptyCooldowns() {
        return List.of(0L, 0L, 0L, 0L, 0L, 0L);
    }
}
