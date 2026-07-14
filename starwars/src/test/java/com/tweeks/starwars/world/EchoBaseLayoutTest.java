package com.tweeks.starwars.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EchoBaseLayoutTest {

    @Test
    void exactlyOneChest() {
        long chests = EchoBaseLayout.placements().stream()
            .filter(p -> p.kind() == EchoBaseLayout.Kind.CHEST).count();
        assertEquals(1, chests);
    }

    @Test
    void garrisonMarkerCounts() {
        var placements = EchoBaseLayout.placements();
        assertEquals(4, placements.stream()
            .filter(p -> p.kind() == EchoBaseLayout.Kind.REBEL).count());
        assertEquals(1, placements.stream()
            .filter(p -> p.kind() == EchoBaseLayout.Kind.ASTROMECH).count());
    }

    @Test
    void hangarOpeningIsFourWideAndFramed() {
        var placements = EchoBaseLayout.placements();
        // 4-wide x 3-tall mouth on the south face.
        assertEquals(12, placements.stream()
            .filter(p -> p.kind() == EchoBaseLayout.Kind.HANGAR_AIR).count());
        // Iron frame: two 3-tall jambs plus a 6-wide lintel.
        assertEquals(12, placements.stream()
            .filter(p -> p.kind() == EchoBaseLayout.Kind.FRAME).count());
    }

    @Test
    void wallsMixSnowAndPackedIce() {
        var placements = EchoBaseLayout.placements();
        assertTrue(placements.stream().anyMatch(p -> p.kind() == EchoBaseLayout.Kind.WALL));
        assertTrue(placements.stream().anyMatch(p -> p.kind() == EchoBaseLayout.Kind.ICE));
    }

    @Test
    void barracksBedsPairWhiteAndRedWool() {
        var placements = EchoBaseLayout.placements();
        var heads = placements.stream()
            .filter(p -> p.kind() == EchoBaseLayout.Kind.BED_HEAD).toList();
        var feet = placements.stream()
            .filter(p -> p.kind() == EchoBaseLayout.Kind.BED_FOOT).toList();
        assertEquals(4, heads.size());
        assertEquals(4, feet.size());
        // Every head has its foot alongside.
        for (var head : heads) {
            assertTrue(feet.stream().anyMatch(f ->
                    f.dy() == head.dy() && f.dz() == head.dz()
                        && Math.abs(f.dx() - head.dx()) == 1),
                "bed head without a foot: " + head);
        }
    }

    @Test
    void generatorHasRedstoneCoreInIronCasing() {
        var placements = EchoBaseLayout.placements();
        var cores = placements.stream()
            .filter(p -> p.kind() == EchoBaseLayout.Kind.GEN_CORE).toList();
        assertEquals(1, cores.size());
        var core = cores.getFirst();
        long adjacentCasing = placements.stream()
            .filter(p -> p.kind() == EchoBaseLayout.Kind.GEN_CASE)
            .filter(p -> Math.abs(p.dx() - core.dx()) + Math.abs(p.dy() - core.dy())
                + Math.abs(p.dz() - core.dz()) == 1)
            .count();
        assertEquals(5, adjacentCasing);
    }

    @Test
    void chestLidCanOpen() {
        var placements = EchoBaseLayout.placements();
        var chest = placements.stream()
            .filter(p -> p.kind() == EchoBaseLayout.Kind.CHEST).findFirst().orElseThrow();
        assertTrue(placements.stream().anyMatch(p ->
            p.kind() == EchoBaseLayout.Kind.AIR
                && p.dx() == chest.dx() && p.dy() == chest.dy() + 1 && p.dz() == chest.dz()));
    }

    @Test
    void allPlacementsInsideDeclaredBounds() {
        for (var p : EchoBaseLayout.placements()) {
            assertTrue(p.dx() >= 0 && p.dx() < EchoBaseLayout.SIZE_X, p.toString());
            assertTrue(p.dy() >= 0 && p.dy() < EchoBaseLayout.SIZE_Y, p.toString());
            assertTrue(p.dz() >= 0 && p.dz() < EchoBaseLayout.SIZE_Z, p.toString());
        }
    }

    @Test
    void hasInteriorAir() {
        assertTrue(EchoBaseLayout.placements().stream()
            .anyMatch(p -> p.kind() == EchoBaseLayout.Kind.AIR));
    }

    @Test
    void placementsAreDeterministic() {
        assertEquals(EchoBaseLayout.placements(), EchoBaseLayout.placements());
    }
}
