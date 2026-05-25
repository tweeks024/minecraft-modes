package com.tweeks.wildwest.entity.projectile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedstoneBombEntityTest {

    @Test
    void constants_matchSpec() {
        assertEquals(3.0f, RedstoneBombEntity.EXPLOSION_RADIUS);
        assertEquals(100, RedstoneBombEntity.FUSE_TICKS);
        assertEquals(6.0f, RedstoneBombEntity.BASE_DAMAGE);
        assertEquals(1.2, RedstoneBombEntity.KNOCKBACK_STRENGTH);
    }
}
