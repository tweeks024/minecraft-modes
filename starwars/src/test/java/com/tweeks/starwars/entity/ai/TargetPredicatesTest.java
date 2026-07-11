package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.faction.SwFaction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TargetPredicatesTest {

    @Test
    void empireTargetsLightCombatant() {
        assertTrue(TargetPredicates.shouldTarget(
            SwFaction.EMPIRE, true, SwFaction.LIGHT, false, 0, false));
    }

    @Test
    void empireIgnoresOwnFaction_andNeutrals() {
        assertFalse(TargetPredicates.shouldTarget(
            SwFaction.EMPIRE, true, SwFaction.EMPIRE, false, 0, false));
        assertFalse(TargetPredicates.shouldTarget(
            SwFaction.EMPIRE, true, SwFaction.NEUTRAL, false, 0, false));
    }

    @Test
    void lightTargetsEmpireCombatant() {
        assertTrue(TargetPredicates.shouldTarget(
            SwFaction.LIGHT, true, SwFaction.EMPIRE, false, 0, false));
    }

    @Test
    void neutralFactionNeverAutoTargets() {
        assertFalse(TargetPredicates.shouldTarget(
            SwFaction.NEUTRAL, true, SwFaction.EMPIRE, false, 0, false));
        assertFalse(TargetPredicates.shouldTarget(
            SwFaction.NEUTRAL, false, SwFaction.NEUTRAL, true, -100, false));
    }

    @Test
    void playerTargetedOnlyPastThreshold() {
        // Light champion (+50): Empire hostile, Light not.
        assertTrue(TargetPredicates.shouldTarget(
            SwFaction.EMPIRE, false, SwFaction.NEUTRAL, true, 50, false));
        assertFalse(TargetPredicates.shouldTarget(
            SwFaction.LIGHT, false, SwFaction.NEUTRAL, true, 50, false));
        // Dark sider (-50): Light hostile.
        assertTrue(TargetPredicates.shouldTarget(
            SwFaction.LIGHT, false, SwFaction.NEUTRAL, true, -50, false));
        // Neutral-band player untargeted.
        assertFalse(TargetPredicates.shouldTarget(
            SwFaction.EMPIRE, false, SwFaction.NEUTRAL, true, 0, false));
    }

    @Test
    void stormtrooperDisguiseHidesPlayerFromEmpireOnly() {
        assertFalse(TargetPredicates.shouldTarget(
            SwFaction.EMPIRE, false, SwFaction.NEUTRAL, true, 100, true));
        assertTrue(TargetPredicates.shouldTarget(
            SwFaction.LIGHT, false, SwFaction.NEUTRAL, true, -50, true));
    }

    @Test
    void nonCombatantNonPlayer_neverTargeted() {
        assertFalse(TargetPredicates.shouldTarget(
            SwFaction.EMPIRE, false, SwFaction.NEUTRAL, false, 0, false));
    }
}
