package com.tweeks.wildwest.entity.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedstoneGolemGoalsTest {

    @Test
    void groundSlam_constants_matchSpec() {
        assertEquals(20, RedstoneGolemGroundSlamGoal.WIND_UP_TICKS);
        assertEquals(160, RedstoneGolemGroundSlamGoal.COOLDOWN_TICKS);
        assertEquals(5.0, RedstoneGolemGroundSlamGoal.TRIGGER_RADIUS);
        assertEquals(4.0, RedstoneGolemGroundSlamGoal.DAMAGE_RADIUS);
        assertEquals(4.0f, RedstoneGolemGroundSlamGoal.DAMAGE);
        assertEquals(2.5, RedstoneGolemGroundSlamGoal.KNOCKBACK_STRENGTH);
    }
}
