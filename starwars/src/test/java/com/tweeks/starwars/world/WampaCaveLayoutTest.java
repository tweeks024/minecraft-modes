package com.tweeks.starwars.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WampaCaveLayoutTest {

    @Test
    void exactlyOneWampaDeepInside() {
        var wampas = WampaCaveLayout.placements().stream()
            .filter(p -> p.kind() == WampaCaveLayout.Kind.WAMPA).toList();
        assertEquals(1, wampas.size());
        // Deep inside: beyond the mound center, far from the low-z entrance.
        var wampa = wampas.getFirst();
        assertTrue(wampa.dz() > WampaCaveLayout.SIZE_Z / 2, wampa.toString());
    }

    @Test
    void wampaHasHeadroom() {
        var placements = WampaCaveLayout.placements();
        var wampa = placements.stream()
            .filter(p -> p.kind() == WampaCaveLayout.Kind.WAMPA).findFirst().orElseThrow();
        for (int dy = 1; dy <= 2; dy++) {
            int y = wampa.dy() + dy;
            assertTrue(placements.stream().anyMatch(p ->
                    p.kind() == WampaCaveLayout.Kind.AIR
                        && p.dx() == wampa.dx() && p.dy() == y && p.dz() == wampa.dz()),
                "no headroom at +" + dy);
        }
    }

    @Test
    void noChestInTheLair() {
        for (var kind : WampaCaveLayout.Kind.values()) {
            assertFalse(kind.name().contains("CHEST"));
        }
    }

    @Test
    void bonesLitterTheLairFloor() {
        // Three loose bones plus the five-block skeleton tableau.
        long bones = WampaCaveLayout.placements().stream()
            .filter(p -> p.kind() == WampaCaveLayout.Kind.BONE).count();
        assertEquals(8, bones);
    }

    @Test
    void jaggedEntranceIsOpenAndSpiked() {
        var placements = WampaCaveLayout.placements();
        // The tunnel mouth is carved: walkable air at the low-z entrance.
        for (int z = 0; z <= 2; z++) {
            final int fz = z;
            for (int y = 0; y <= 1; y++) {
                final int fy = y;
                assertTrue(placements.stream().anyMatch(p ->
                        p.kind() == WampaCaveLayout.Kind.AIR
                            && p.dx() == 5 && p.dy() == fy && p.dz() == fz),
                    "entrance blocked at z=" + fz + " y=" + fy);
            }
        }
        // Stepped blue-ice spikes flank the mouth.
        assertEquals(6, placements.stream()
            .filter(p -> p.kind() == WampaCaveLayout.Kind.SPIKE).count());
    }

    @Test
    void lairIsHollow() {
        assertTrue(WampaCaveLayout.placements().stream()
            .anyMatch(p -> p.kind() == WampaCaveLayout.Kind.AIR));
        assertTrue(WampaCaveLayout.placements().stream()
            .anyMatch(p -> p.kind() == WampaCaveLayout.Kind.SHELL));
    }

    @Test
    void allPlacementsInsideDeclaredBounds() {
        for (var p : WampaCaveLayout.placements()) {
            assertTrue(p.dx() >= 0 && p.dx() < WampaCaveLayout.SIZE_X, p.toString());
            assertTrue(p.dy() >= 0 && p.dy() < WampaCaveLayout.SIZE_Y, p.toString());
            assertTrue(p.dz() >= 0 && p.dz() < WampaCaveLayout.SIZE_Z, p.toString());
        }
    }

    @Test
    void placementsAreDeterministic() {
        assertEquals(WampaCaveLayout.placements(), WampaCaveLayout.placements());
    }
}
