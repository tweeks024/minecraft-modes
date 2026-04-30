package com.tweeks.wildwest;

import com.tweeks.wildwest.Hitscan.Candidate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HitscanTest {

    @Test
    void noCandidates_andNoBlock_returnsEmpty() {
        Optional<Candidate> hit = Hitscan.firstHitWithinRange(
            Double.POSITIVE_INFINITY, List.of());
        assertTrue(hit.isEmpty());
    }

    @Test
    void closerEntityWinsOverFartherEntity() {
        Candidate near = new Candidate("near", 4.0);
        Candidate far  = new Candidate("far",  10.0);
        Optional<Candidate> hit = Hitscan.firstHitWithinRange(
            Double.POSITIVE_INFINITY, List.of(far, near));
        assertEquals("near", hit.orElseThrow().id());
    }

    @Test
    void blockCloserThanEntity_returnsEmpty() {
        Candidate entity = new Candidate("entity", 8.0);
        Optional<Candidate> hit = Hitscan.firstHitWithinRange(
            5.0, List.of(entity));
        assertTrue(hit.isEmpty());
    }

    @Test
    void entityCloserThanBlock_returnsEntity() {
        Candidate entity = new Candidate("entity", 3.0);
        Optional<Candidate> hit = Hitscan.firstHitWithinRange(
            5.0, List.of(entity));
        assertEquals("entity", hit.orElseThrow().id());
    }

    @Test
    void rangeFiltering_isCallerResponsibility() {
        Candidate entity = new Candidate("entity", 100.0);
        Optional<Candidate> hit = Hitscan.firstHitWithinRange(
            Double.POSITIVE_INFINITY, List.of(entity));
        assertEquals("entity", hit.orElseThrow().id());
    }
}
