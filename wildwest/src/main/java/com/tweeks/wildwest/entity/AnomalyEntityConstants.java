package com.tweeks.wildwest.entity;

/**
 * Pure constants extracted from {@link AnomalyEntity} so the values can be
 * asserted in plain JUnit (loading AnomalyEntity itself would trigger
 * Minecraft bootstrap and fail in the unit-test JVM — same reason
 * {@link com.tweeks.wildwest.effect.AnomalyBleedSchedule} exists separately
 * from {@code AnomalyBleedEffect}).
 *
 * AnomalyEntity re-exports these as its own public static fields so
 * implementation code reads e.g. {@code AnomalyEntity.MAX_HEALTH}, not the
 * helper class. The helper exists purely for testability.
 */
public final class AnomalyEntityConstants {
    public static final double MAX_HEALTH = 40.0;
    public static final double ATTACK_DAMAGE = 8.0;
    public static final double SPEED_REVEALED = 0.30;
    public static final double SPEED_DISGUISED = 0.20;
    public static final double KNOCKBACK_RESISTANCE = 0.0;
    public static final double FOLLOW_RANGE = 24.0;
    public static final int RE_DISGUISE_TICKS = 200;        // 10 s
    public static final int DAMAGE_GRACE_TICKS = 40;        // 2 s — damage within this window resets the re-disguise timer
    public static final float DISGUISED_DAMAGE_MULTIPLIER = 0.75f;

    private AnomalyEntityConstants() {}
}
