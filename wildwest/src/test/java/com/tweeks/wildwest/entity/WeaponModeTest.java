package com.tweeks.wildwest.entity;

import org.junit.jupiter.api.Test;

import static com.tweeks.wildwest.entity.WeaponMode.MELEE;
import static com.tweeks.wildwest.entity.WeaponMode.RANGED;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WeaponModeTest {

    @Test
    void closeRange_swapAllowed_picksMelee() {
        assertEquals(MELEE, WeaponMode.choose(2.0, RANGED, 0));
    }

    @Test
    void closeRange_locked_keepsCurrent() {
        assertEquals(RANGED, WeaponMode.choose(2.0, RANGED, 5));
    }

    @Test
    void farRange_swapAllowed_picksRanged() {
        assertEquals(RANGED, WeaponMode.choose(8.0, MELEE, 0));
    }

    @Test
    void atBoundary_4blk_staysMelee() {
        assertEquals(MELEE, WeaponMode.choose(4.0, MELEE, 0));
    }

    @Test
    void justPastBoundary_swapsToRanged() {
        assertEquals(RANGED, WeaponMode.choose(4.001, MELEE, 0));
    }

    @Test
    void desiredEqualsCurrent_isNoOp() {
        assertEquals(RANGED, WeaponMode.choose(8.0, RANGED, 0));
        assertEquals(MELEE, WeaponMode.choose(2.0, MELEE, 0));
    }
}
