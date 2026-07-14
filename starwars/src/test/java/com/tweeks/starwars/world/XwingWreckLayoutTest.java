package com.tweeks.starwars.world;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class XwingWreckLayoutTest {

    @Test
    void exactlyOneChest() {
        long chests = XwingWreckLayout.placements().stream()
            .filter(p -> p.kind() == XwingWreckLayout.Kind.CHEST).count();
        assertEquals(1, chests);
    }

    @Test
    void wingsMirroredInX() {
        List<XwingWreckLayout.Placement> wings = wings();
        for (var p : wings) {
            int mirrored = XwingWreckLayout.SIZE_X - 1 - p.dx();
            assertTrue(wings.stream().anyMatch(q ->
                    q.dx() == mirrored && q.dy() == p.dy() && q.dz() == p.dz()),
                "no x-mirror for " + p);
        }
    }

    @Test
    void wingsMirroredInY() {
        // The X splay: the two raised wings reflect onto the two dipped wings
        // across the hull midline.
        List<XwingWreckLayout.Placement> wings = wings();
        int minY = wings.stream().mapToInt(XwingWreckLayout.Placement::dy).min().orElseThrow();
        int maxY = wings.stream().mapToInt(XwingWreckLayout.Placement::dy).max().orElseThrow();
        int sum = minY + maxY;
        assertTrue(maxY > minY, "wings do not splay vertically");
        for (var p : wings) {
            assertTrue(wings.stream().anyMatch(q ->
                    q.dx() == p.dx() && q.dy() == sum - p.dy() && q.dz() == p.dz()),
                "no y-mirror for " + p);
        }
    }

    @Test
    void allFourWingQuadrantsPresent() {
        List<XwingWreckLayout.Placement> wings = wings();
        int mid = (wings.stream().mapToInt(XwingWreckLayout.Placement::dy).min().orElseThrow()
            + wings.stream().mapToInt(XwingWreckLayout.Placement::dy).max().orElseThrow());
        assertTrue(wings.stream().anyMatch(p -> p.dx() < XwingWreckLayout.SPINE_X && 2 * p.dy() > mid));
        assertTrue(wings.stream().anyMatch(p -> p.dx() > XwingWreckLayout.SPINE_X && 2 * p.dy() > mid));
        assertTrue(wings.stream().anyMatch(p -> p.dx() < XwingWreckLayout.SPINE_X && 2 * p.dy() < mid));
        assertTrue(wings.stream().anyMatch(p -> p.dx() > XwingWreckLayout.SPINE_X && 2 * p.dy() < mid));
    }

    @Test
    void noseSitsBelowTheSurface() {
        // Half-sunk: the buried nose reaches the layout's minimum dy.
        assertEquals(XwingWreckLayout.MIN_Y, XwingWreckLayout.placements().stream()
            .mapToInt(XwingWreckLayout.Placement::dy).min().orElseThrow());
        assertTrue(XwingWreckLayout.placements().stream().anyMatch(p ->
            p.kind() == XwingWreckLayout.Kind.FUSELAGE && p.dy() < 0));
    }

    @Test
    void hullAccentsAndCanopy() {
        var placements = XwingWreckLayout.placements();
        assertEquals(4, placements.stream()
            .filter(p -> p.kind() == XwingWreckLayout.Kind.ORANGE).count());
        assertEquals(2, placements.stream()
            .filter(p -> p.kind() == XwingWreckLayout.Kind.COCKPIT).count());
    }

    @Test
    void layoutNeverClearsWater() {
        // Air-free by design: every kind maps to a solid placement, so the
        // piece never carves the swamp around the sunken wreck.
        for (var kind : XwingWreckLayout.Kind.values()) {
            assertNotEquals("AIR", kind.name());
        }
    }

    @Test
    void allPlacementsInsideDeclaredBounds() {
        for (var p : XwingWreckLayout.placements()) {
            assertTrue(p.dx() >= 0 && p.dx() < XwingWreckLayout.SIZE_X, p.toString());
            assertTrue(p.dy() >= XwingWreckLayout.MIN_Y
                && p.dy() < XwingWreckLayout.MIN_Y + XwingWreckLayout.SIZE_Y, p.toString());
            assertTrue(p.dz() >= 0 && p.dz() < XwingWreckLayout.SIZE_Z, p.toString());
        }
    }

    @Test
    void placementsAreDeterministic() {
        assertEquals(XwingWreckLayout.placements(), XwingWreckLayout.placements());
    }

    private static List<XwingWreckLayout.Placement> wings() {
        return XwingWreckLayout.placements().stream()
            .filter(p -> p.kind() == XwingWreckLayout.Kind.WING).toList();
    }
}
