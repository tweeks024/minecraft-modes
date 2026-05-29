package com.tweeks.wildwest.entity;

public final class CrabEntityConstants {
    private CrabEntityConstants() {}

    public static final double MAX_HEALTH = 6.0;
    public static final double ATTACK_DAMAGE = 2.0;
    public static final double MOVEMENT_SPEED = 0.5;
    public static final double FOLLOW_RANGE = 16.0;
    public static final double SWARM_RADIUS = 8.0;

    public static final int ANGER_SECONDS_MIN = 20;
    public static final int ANGER_SECONDS_MAX = 39;

    public static final int SPAWN_WEIGHT = 6;
    public static final int SPAWN_GROUP_MIN = 2;
    public static final int SPAWN_GROUP_MAX = 4;

    // 61 (not 60): byte 60 is used by LivingEntity for makePoofParticles (death event).
    // Intercepting it would suppress crab death particles.
    public static final byte EVENT_ID_PINCH = (byte) 61;
}
