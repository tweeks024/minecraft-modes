package com.tweeks.thief.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevealStateTest {

    @Test
    void byteRoundTrip_preservesState() {
        for (RevealState s : RevealState.values()) {
            assertEquals(s, RevealState.fromByte(s.toByte()));
        }
    }

    @Test
    void disguised_isNotHostile() {
        assertFalse(RevealState.DISGUISED.isHostile());
        assertFalse(RevealState.SUSPICIOUS.isHostile());
    }

    @Test
    void revealedAndFleeing_areHostile() {
        assertTrue(RevealState.REVEALED_RANGED.isHostile());
        assertTrue(RevealState.REVEALED_MELEE.isHostile());
        assertTrue(RevealState.FLEEING.isHostile());
    }

    @Test
    void disguised_canRevealToRangedOrMelee() {
        assertTrue(RevealState.DISGUISED.canTransitionTo(RevealState.REVEALED_RANGED));
        assertTrue(RevealState.DISGUISED.canTransitionTo(RevealState.REVEALED_MELEE));
        assertTrue(RevealState.DISGUISED.canTransitionTo(RevealState.SUSPICIOUS));
    }

    @Test
    void revealed_cannotReturnToDisguised() {
        assertFalse(RevealState.REVEALED_RANGED.canTransitionTo(RevealState.DISGUISED));
        assertFalse(RevealState.REVEALED_MELEE.canTransitionTo(RevealState.DISGUISED));
        assertFalse(RevealState.FLEEING.canTransitionTo(RevealState.DISGUISED));
        assertFalse(RevealState.REVEALED_RANGED.canTransitionTo(RevealState.SUSPICIOUS));
    }

    @Test
    void revealedRanged_canSwapToRevealedMelee() {
        assertTrue(RevealState.REVEALED_RANGED.canTransitionTo(RevealState.REVEALED_MELEE));
        assertTrue(RevealState.REVEALED_MELEE.canTransitionTo(RevealState.REVEALED_RANGED));
    }

    @Test
    void anyRevealedState_canEnterFleeing() {
        assertTrue(RevealState.REVEALED_RANGED.canTransitionTo(RevealState.FLEEING));
        assertTrue(RevealState.REVEALED_MELEE.canTransitionTo(RevealState.FLEEING));
    }

    @Test
    void suspicious_canReturnToDisguised() {
        assertTrue(RevealState.SUSPICIOUS.canTransitionTo(RevealState.DISGUISED));
    }
}
