package com.tweeks.starwars.faction;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AlignmentTest {

    @Test
    void constants_matchSpec() {
        assertEquals(50, Alignment.HOSTILE_THRESHOLD);
        assertEquals(5, Alignment.KILL_DELTA);
        assertEquals(1, Alignment.HIT_DELTA);
        assertEquals(2, Alignment.POWER_DELTA);
        assertEquals(-100, Alignment.MIN);
        assertEquals(100, Alignment.MAX);
    }

    @Test
    void harmingEmpire_movesTowardLight() {
        assertEquals(5, Alignment.deltaForKill(SwFaction.EMPIRE));
        assertEquals(1, Alignment.deltaForHit(SwFaction.EMPIRE));
    }

    @Test
    void harmingLight_movesTowardDark() {
        assertEquals(-5, Alignment.deltaForKill(SwFaction.LIGHT));
        assertEquals(-1, Alignment.deltaForHit(SwFaction.LIGHT));
    }

    @Test
    void harmingNeutral_noChange() {
        assertEquals(0, Alignment.deltaForKill(SwFaction.NEUTRAL));
        assertEquals(0, Alignment.deltaForHit(SwFaction.NEUTRAL));
    }

    @Test
    void powerUse_movesAlignment() {
        assertEquals(2, Alignment.deltaForPower(true));
        assertEquals(-2, Alignment.deltaForPower(false));
    }

    @Test
    void clamp_boundsScore() {
        assertEquals(100, Alignment.clamp(150));
        assertEquals(-100, Alignment.clamp(-150));
        assertEquals(7, Alignment.clamp(7));
    }

    @Test
    void hostility_thresholds() {
        // Player at +50 (light champion): Empire attacks, Light does not.
        assertTrue(Alignment.isHostileTo(50, SwFaction.EMPIRE));
        assertFalse(Alignment.isHostileTo(50, SwFaction.LIGHT));
        // Player at -50 (dark sider): Light attacks, Empire does not.
        assertTrue(Alignment.isHostileTo(-50, SwFaction.LIGHT));
        assertFalse(Alignment.isHostileTo(-50, SwFaction.EMPIRE));
        // Neutral band: nobody auto-targets.
        assertFalse(Alignment.isHostileTo(49, SwFaction.EMPIRE));
        assertFalse(Alignment.isHostileTo(-49, SwFaction.LIGHT));
        assertFalse(Alignment.isHostileTo(0, SwFaction.EMPIRE));
        // NEUTRAL faction never auto-targets.
        assertFalse(Alignment.isHostileTo(100, SwFaction.NEUTRAL));
        assertFalse(Alignment.isHostileTo(-100, SwFaction.NEUTRAL));
    }
}
