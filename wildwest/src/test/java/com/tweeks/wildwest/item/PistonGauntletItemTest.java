package com.tweeks.wildwest.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PistonGauntletItemTest {

    @Test
    void constants_matchSpec() {
        assertEquals(30, PistonGauntletItem.COOLDOWN_TICKS);
        assertEquals(4.0, PistonGauntletItem.RAY_DISTANCE);
        assertEquals(4.0f, PistonGauntletItem.HIT_DAMAGE);
        assertEquals(2.0, PistonGauntletItem.HIT_KNOCKBACK);
        assertEquals(1.5, PistonGauntletItem.SELF_LAUNCH_VELOCITY);
        assertEquals(250, PistonGauntletItem.DURABILITY);
    }
}
