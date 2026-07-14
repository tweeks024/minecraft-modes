package com.tweeks.starwars.entity.ai;

/**
 * Probe-droid alarm escalation state machine. Pure logic (no engine
 * imports) so it unit-tests without an MC bootstrap.
 *
 * <p>While the probe holds a live player target the charge counts up; after
 * {@link #ARM_TICKS} consecutive ticks it fires {@link Result#SUMMON} exactly
 * once, then enters a {@link #COOLDOWN_TICKS} lockout before it can re-arm.
 * Losing the target at any point before the summon resets the charge to
 * zero — the player has to stay "seen" for the full window to trigger a
 * garrison drop.
 */
public final class ProbeAlarm {

    /** Consecutive ticks with a live player target needed to summon (~30s). */
    public static final int ARM_TICKS = 600;
    /** Lockout after a summon before the alarm can re-arm (~2min). */
    public static final int COOLDOWN_TICKS = 2400;

    public enum Result { IDLE, SUMMON }

    private int charge;
    private int cooldown;

    /**
     * Advance one tick.
     *
     * @param hasLivePlayerTarget true when the probe currently has a living
     *                            player target
     * @return {@link Result#SUMMON} on the single tick the alarm trips,
     *         otherwise {@link Result#IDLE}
     */
    public Result tick(boolean hasLivePlayerTarget) {
        if (cooldown > 0) {
            // Locked out after a summon: bleed the cooldown and never
            // accumulate charge (so re-arming always starts from zero).
            cooldown--;
            charge = 0;
            return Result.IDLE;
        }
        if (!hasLivePlayerTarget) {
            charge = 0;
            return Result.IDLE;
        }
        charge++;
        if (charge >= ARM_TICKS) {
            charge = 0;
            cooldown = COOLDOWN_TICKS;
            return Result.SUMMON;
        }
        return Result.IDLE;
    }

    /** Consecutive-target charge, for tests/inspection. */
    public int charge() {
        return charge;
    }

    /** Remaining post-summon lockout, for tests/inspection. */
    public int cooldown() {
        return cooldown;
    }
}
