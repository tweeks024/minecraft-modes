package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.faction.SwFaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RallyMathTest {

    @Test
    public void onlyLightFactionIsEligible() {
        assertTrue(RallyMath.isEligibleFaction(SwFaction.LIGHT));
        assertFalse(RallyMath.isEligibleFaction(SwFaction.EMPIRE));
        assertFalse(RallyMath.isEligibleFaction(SwFaction.NEUTRAL));
    }

    @Test
    public void playerScoreMustBeStrictlyPositive() {
        assertTrue(RallyMath.isEligibleScore(1));
        assertFalse(RallyMath.isEligibleScore(0));
        assertFalse(RallyMath.isEligibleScore(-1));
    }

    @Test
    public void radiusBoundaryIsInclusive() {
        double r = RallyMath.RALLY_RADIUS;
        assertTrue(RallyMath.isWithinRadius(r * r));
        assertTrue(RallyMath.isWithinRadius(0.0));
        assertFalse(RallyMath.isWithinRadius(r * r + 0.001));
    }

    @Test
    public void cooldownGatesOnGameTimeDelta() {
        assertTrue(RallyMath.isReady(240, 0));
        assertTrue(RallyMath.isReady(1000, 760));
        assertFalse(RallyMath.isReady(239, 0));
        // First-ever pulse: lastPulseGameTime 0 at world start still gates
        // until tick 240 — acceptable (worst case one idle interval).
        assertTrue(RallyMath.isReady(0, -RallyMath.RALLY_INTERVAL_TICKS));
    }
}
