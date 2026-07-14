package com.tweeks.starwars.entity.ai;

/**
 * Grogu's "Force nap" power as a pure state machine (no engine imports, so
 * it unit-tests without an MC bootstrap — same pattern as {@link ProbeAlarm}).
 *
 * <p>While ready, the first tick that reports a nearby hostile fires
 * {@link Result#NAP} exactly once and then locks out for
 * {@link #COOLDOWN_TICKS} ticks. During the lockout every tick returns
 * {@link Result#IDLE} regardless of what is nearby; once it elapses the
 * power re-arms and can nap again if a hostile is still present.
 */
public final class GroguForceNap {

    /** Long lockout after a nap before the Force can be called again (~30s). */
    public static final int COOLDOWN_TICKS = 600;

    public enum Result { IDLE, NAP }

    private int cooldown;

    /**
     * Advance one tick.
     *
     * @param hasNearbyHostile true when a hostile mob is currently within
     *                         range (the caller only bothers scanning while
     *                         {@link #isReady()})
     * @return {@link Result#NAP} on the single tick the power triggers,
     *         otherwise {@link Result#IDLE}
     */
    public Result tick(boolean hasNearbyHostile) {
        if (cooldown > 0) {
            cooldown--;
            return Result.IDLE;
        }
        if (hasNearbyHostile) {
            cooldown = COOLDOWN_TICKS;
            return Result.NAP;
        }
        return Result.IDLE;
    }

    /** True when the power is off cooldown and could nap this tick. */
    public boolean isReady() {
        return cooldown == 0;
    }

    /** Remaining post-nap lockout, for tests/inspection. */
    public int cooldown() {
        return cooldown;
    }
}
