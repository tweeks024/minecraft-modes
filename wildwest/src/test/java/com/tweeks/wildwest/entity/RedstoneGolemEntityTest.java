package com.tweeks.wildwest.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Constants smoke test. Catches accidental drift of HP / speed / damage
 * values that the spec promises. Full spawn flow is exercised manually
 * in dev-client.
 */
class RedstoneGolemEntityTest {

    @Test
    void constants_matchSpec() {
        assertEquals(280.0, RedstoneGolemEntity.MAX_HEALTH);
        assertEquals(10.0, RedstoneGolemEntity.ATTACK_DAMAGE);
        assertEquals(0.22, RedstoneGolemEntity.MOVEMENT_SPEED);
        assertEquals(14.0, RedstoneGolemEntity.ARMOR);
        assertEquals(1.0, RedstoneGolemEntity.KNOCKBACK_RESISTANCE);
        assertEquals(48.0, RedstoneGolemEntity.FOLLOW_RANGE);
        assertEquals(100, RedstoneGolemEntity.XP_DROP);
    }

    @Test
    void boss_bar_color_red_notched_10() {
        assertEquals("RED", RedstoneGolemEntity.BOSS_BAR_COLOR_NAME);
        assertEquals("NOTCHED_10", RedstoneGolemEntity.BOSS_BAR_OVERLAY_NAME);
    }
}
