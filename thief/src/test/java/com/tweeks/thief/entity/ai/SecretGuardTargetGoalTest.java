package com.tweeks.thief.entity.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretGuardTargetGoalTest {

    @Test
    void noPlayerNearby_isHidden() {
        boolean hidden = SecretGuardTargetGoal.isHiddenFromAllPlayers(
            /*nearestPlayerExists=*/ false,
            /*anyPlayerHasLineOfSight=*/ false);
        assertTrue(hidden);
    }

    @Test
    void playerNearbyButNoLineOfSight_isHidden() {
        boolean hidden = SecretGuardTargetGoal.isHiddenFromAllPlayers(
            /*nearestPlayerExists=*/ true,
            /*anyPlayerHasLineOfSight=*/ false);
        assertTrue(hidden);
    }

    @Test
    void playerNearbyWithLineOfSight_isNotHidden() {
        boolean hidden = SecretGuardTargetGoal.isHiddenFromAllPlayers(
            /*nearestPlayerExists=*/ true,
            /*anyPlayerHasLineOfSight=*/ true);
        assertFalse(hidden);
    }
}
