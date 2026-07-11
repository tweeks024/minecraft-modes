package com.tweeks.starwars.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImperialOutpostLayoutTest {

    @Test
    void exactlyOneChest() {
        long chests = ImperialOutpostLayout.placements().stream()
            .filter(p -> p.kind() == ImperialOutpostLayout.Kind.CHEST).count();
        assertEquals(1, chests);
    }

    @Test
    void threeTrooperMarkers() {
        long troopers = ImperialOutpostLayout.placements().stream()
            .filter(p -> p.kind() == ImperialOutpostLayout.Kind.GARRISON_TROOPER).count();
        assertEquals(3, troopers);
    }

    @Test
    void twoDroidMarkers() {
        long droids = ImperialOutpostLayout.placements().stream()
            .filter(p -> p.kind() == ImperialOutpostLayout.Kind.GARRISON_DROID).count();
        assertEquals(2, droids);
    }

    @Test
    void allPlacementsInsideDeclaredBounds() {
        for (var p : ImperialOutpostLayout.placements()) {
            assertTrue(p.dx() >= 0 && p.dx() < ImperialOutpostLayout.SIZE_X, p.toString());
            assertTrue(p.dy() >= 0 && p.dy() < ImperialOutpostLayout.SIZE_Y, p.toString());
            assertTrue(p.dz() >= 0 && p.dz() < ImperialOutpostLayout.SIZE_Z, p.toString());
        }
    }

    @Test
    void southGateIsOpen() {
        // 3-wide gate at z=0, x=4..6: no WALL block in the gate opening.
        assertFalse(ImperialOutpostLayout.placements().stream()
            .anyMatch(p -> p.kind() == ImperialOutpostLayout.Kind.WALL
                && p.dz() == 0 && p.dy() == 1 && p.dx() == 5));
    }

    @Test
    void hasInteriorAir() {
        assertTrue(ImperialOutpostLayout.placements().stream()
            .anyMatch(p -> p.kind() == ImperialOutpostLayout.Kind.AIR));
    }
}
