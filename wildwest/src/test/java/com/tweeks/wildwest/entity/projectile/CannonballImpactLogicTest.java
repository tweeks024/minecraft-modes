package com.tweeks.wildwest.entity.projectile;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CannonballImpactLogicTest {

    @Test
    void emptyCandidates_returnsEmpty() {
        Set<String> victims = CannonballImpactLogic.victims(
            0, 0, 0, 4.0, List.of(), null);
        assertTrue(victims.isEmpty());
    }

    @Test
    void candidateInsideRadius_isVictim() {
        var c = new CannonballImpactLogic.Candidate("a", 1, 0, 0);
        Set<String> victims = CannonballImpactLogic.victims(
            0, 0, 0, 4.0, List.of(c), null);
        assertEquals(Set.of("a"), victims);
    }

    @Test
    void candidateOutsideRadius_isExcluded() {
        var c = new CannonballImpactLogic.Candidate("a", 10, 0, 0);
        Set<String> victims = CannonballImpactLogic.victims(
            0, 0, 0, 4.0, List.of(c), null);
        assertTrue(victims.isEmpty());
    }

    @Test
    void candidateOnRadiusBoundary_isVictim() {
        // distance exactly = radius → included (use ≤ not <).
        var c = new CannonballImpactLogic.Candidate("edge", 4, 0, 0);
        Set<String> victims = CannonballImpactLogic.victims(
            0, 0, 0, 4.0, List.of(c), null);
        assertEquals(Set.of("edge"), victims);
    }

    @Test
    void directHit_isExcludedFromAoe() {
        var direct = new CannonballImpactLogic.Candidate("direct", 0, 0, 0);
        var other = new CannonballImpactLogic.Candidate("other", 2, 0, 0);
        Set<String> victims = CannonballImpactLogic.victims(
            0, 0, 0, 4.0, List.of(direct, other), "direct");
        assertEquals(Set.of("other"), victims);
    }

    @Test
    void nullDirectId_doesNotExclude_anyone() {
        var a = new CannonballImpactLogic.Candidate("a", 1, 0, 0);
        var b = new CannonballImpactLogic.Candidate("b", 2, 0, 0);
        Set<String> victims = CannonballImpactLogic.victims(
            0, 0, 0, 4.0, List.of(a, b), null);
        assertEquals(Set.of("a", "b"), victims);
    }
}
