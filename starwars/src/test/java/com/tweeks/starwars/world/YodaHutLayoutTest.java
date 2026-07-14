package com.tweeks.starwars.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class YodaHutLayoutTest {

    @Test
    void exactlyOneChest() {
        long chests = YodaHutLayout.placements().stream()
            .filter(p -> p.kind() == YodaHutLayout.Kind.CHEST).count();
        assertEquals(1, chests);
    }

    @Test
    void exactlyOneYodaMarkerInsideTheHut() {
        var yodas = YodaHutLayout.placements().stream()
            .filter(p -> p.kind() == YodaHutLayout.Kind.YODA).toList();
        assertEquals(1, yodas.size());
        // Inside: strictly within the shell radius, on the floor level.
        var yoda = yodas.getFirst();
        assertEquals(1, yoda.dy());
        int d2 = sq(yoda.dx() - 4) + yoda.dy() * yoda.dy() + sq(yoda.dz() - 4);
        assertTrue(d2 < 9, "yoda outside the dome interior: " + yoda);
    }

    @Test
    void domeWeavesMudAndRoots() {
        var placements = YodaHutLayout.placements();
        assertTrue(placements.stream().anyMatch(p -> p.kind() == YodaHutLayout.Kind.DOME_MUD));
        assertTrue(placements.stream().anyMatch(p -> p.kind() == YodaHutLayout.Kind.DOME_ROOTS));
    }

    @Test
    void tinyInteriorFurnishings() {
        var placements = YodaHutLayout.placements();
        assertEquals(2, placements.stream()
            .filter(p -> p.kind() == YodaHutLayout.Kind.PALLET).count());
        assertEquals(1, placements.stream()
            .filter(p -> p.kind() == YodaHutLayout.Kind.POT).count());
    }

    @Test
    void chestLidCanOpen() {
        var placements = YodaHutLayout.placements();
        var chest = placements.stream()
            .filter(p -> p.kind() == YodaHutLayout.Kind.CHEST).findFirst().orElseThrow();
        assertTrue(placements.stream().anyMatch(p ->
            p.kind() == YodaHutLayout.Kind.AIR
                && p.dx() == chest.dx() && p.dy() == chest.dy() + 1 && p.dz() == chest.dz()));
    }

    @Test
    void doorGapIsOpen() {
        // The rounded door arch on the low-z side: no shell cell blocks the
        // 3-wide base or the 1-wide crown of the opening.
        var placements = YodaHutLayout.placements();
        assertTrue(placements.stream().noneMatch(p ->
            (p.kind() == YodaHutLayout.Kind.DOME_MUD || p.kind() == YodaHutLayout.Kind.DOME_ROOTS)
                && p.dz() < 4 && p.dy() == 1 && Math.abs(p.dx() - 4) <= 1));
        assertTrue(placements.stream().noneMatch(p ->
            (p.kind() == YodaHutLayout.Kind.DOME_MUD || p.kind() == YodaHutLayout.Kind.DOME_ROOTS)
                && p.dz() < 4 && p.dy() == 2 && p.dx() == 4));
    }

    @Test
    void allPlacementsInsideDeclaredBounds() {
        for (var p : YodaHutLayout.placements()) {
            assertTrue(p.dx() >= 0 && p.dx() < YodaHutLayout.SIZE_X, p.toString());
            assertTrue(p.dy() >= 0 && p.dy() < YodaHutLayout.SIZE_Y, p.toString());
            assertTrue(p.dz() >= 0 && p.dz() < YodaHutLayout.SIZE_Z, p.toString());
        }
    }

    @Test
    void hasInteriorAir() {
        assertTrue(YodaHutLayout.placements().stream()
            .anyMatch(p -> p.kind() == YodaHutLayout.Kind.AIR));
    }

    @Test
    void placementsAreDeterministic() {
        assertEquals(YodaHutLayout.placements(), YodaHutLayout.placements());
    }

    private static int sq(int v) {
        return v * v;
    }
}
