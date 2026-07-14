package com.tweeks.starwars.world;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SandcrawlerLayoutTest {

    @Test
    void exactlyOneChest() {
        long chests = SandcrawlerLayout.placements().stream()
            .filter(p -> p.kind() == SandcrawlerLayout.Kind.CHEST).count();
        assertEquals(1, chests);
    }

    @Test
    void exactlyTwoAstromechMarkers() {
        long markers = SandcrawlerLayout.placements().stream()
            .filter(p -> p.kind() == SandcrawlerLayout.Kind.ASTROMECH).count();
        assertEquals(2, markers);
    }

    @Test
    void allPlacementsInsideDeclaredBounds() {
        for (var p : SandcrawlerLayout.placements()) {
            assertTrue(p.dx() >= 0 && p.dx() < SandcrawlerLayout.SIZE_X, p.toString());
            assertTrue(p.dy() >= 0 && p.dy() < SandcrawlerLayout.SIZE_Y, p.toString());
            assertTrue(p.dz() >= 0 && p.dz() < SandcrawlerLayout.SIZE_Z, p.toString());
        }
    }

    @Test
    void treadsAreTheBottomTwoLayers() {
        var placements = SandcrawlerLayout.placements();
        assertTrue(placements.stream()
            .filter(p -> p.kind() == SandcrawlerLayout.Kind.TREAD)
            .allMatch(p -> p.dy() <= 1));
        // Full footprint on both layers.
        long treads = placements.stream()
            .filter(p -> p.kind() == SandcrawlerLayout.Kind.TREAD).count();
        assertEquals(2L * SandcrawlerLayout.SIZE_X * SandcrawlerLayout.SIZE_Z, treads);
    }

    @Test
    void slopeRisesFromFrontTowardMid() {
        var slope = SandcrawlerLayout.placements().stream()
            .filter(p -> p.kind() == SandcrawlerLayout.Kind.SLOPE).toList();
        assertFalse(slope.isEmpty());
        // Each slope step is exactly one higher than the previous z step.
        for (var p : slope) {
            assertEquals(3 + p.dz(), p.dy(), p.toString());
        }
    }

    @Test
    void slopeAndTreadsAreMirroredInX() {
        List<SandcrawlerLayout.Placement> rigid = SandcrawlerLayout.placements().stream()
            .filter(p -> p.kind() == SandcrawlerLayout.Kind.SLOPE
                || p.kind() == SandcrawlerLayout.Kind.TREAD).toList();
        for (var p : rigid) {
            int mirrored = SandcrawlerLayout.SIZE_X - 1 - p.dx();
            assertTrue(rigid.stream().anyMatch(q ->
                    q.kind() == p.kind() && q.dx() == mirrored
                        && q.dy() == p.dy() && q.dz() == p.dz()),
                "no mirror for " + p);
        }
    }

    @Test
    void hasInteriorAir() {
        assertTrue(SandcrawlerLayout.placements().stream()
            .anyMatch(p -> p.kind() == SandcrawlerLayout.Kind.AIR));
    }

    @Test
    void placementsAreDeterministic() {
        assertEquals(SandcrawlerLayout.placements(), SandcrawlerLayout.placements());
    }
}
