package com.tweeks.starwars;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HitscanTest {

    @Test
    void picksNearestCandidate() {
        var hit = Hitscan.firstHitWithinRange(16.0, List.of(
            new Hitscan.Candidate("far", 10.0),
            new Hitscan.Candidate("near", 4.0)));
        assertTrue(hit.isPresent());
        assertEquals("near", hit.get().id());
    }

    @Test
    void wallBlocksCandidatesBehindIt() {
        var hit = Hitscan.firstHitWithinRange(5.0, List.of(
            new Hitscan.Candidate("behind_wall", 8.0)));
        assertTrue(hit.isEmpty());
    }

    @Test
    void emptyCandidates_missesCleanly() {
        assertTrue(Hitscan.firstHitWithinRange(16.0, List.of()).isEmpty());
    }

    @Test
    void candidateExactlyAtWallDistance_blocked() {
        var hit = Hitscan.firstHitWithinRange(6.0, List.of(
            new Hitscan.Candidate("at_wall", 6.0)));
        assertTrue(hit.isEmpty());
    }
}
