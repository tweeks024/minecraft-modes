package com.tweeks.starwars.world;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class VaderCastleLayoutTest {

    private static long key(int x, int y, int z) {
        return ((long) x << 40) | ((long) (y + 8) << 20) | z;
    }

    @Test
    void exactlyOneChest() {
        long chests = VaderCastleLayout.placements().stream()
            .filter(p -> p.kind() == VaderCastleLayout.Kind.CHEST).count();
        assertEquals(1, chests);
    }

    @Test
    void guardMarkerCountIsTwoToThree() {
        long troopers = VaderCastleLayout.placements().stream()
            .filter(p -> p.kind() == VaderCastleLayout.Kind.STORMTROOPER).count();
        assertTrue(troopers >= 2 && troopers <= 3, "stormtrooper guards: " + troopers);
    }

    @Test
    void exactlyOneVaderMarker() {
        long vader = VaderCastleLayout.placements().stream()
            .filter(p -> p.kind() == VaderCastleLayout.Kind.VADER_SPAWN).count();
        assertEquals(1, vader);
    }

    @Test
    void towersRiseAtLeastTwentyFourBlocks() {
        int top = VaderCastleLayout.placements().stream()
            .filter(p -> p.kind() == VaderCastleLayout.Kind.PILLAR)
            .mapToInt(VaderCastleLayout.Placement::dy).max().orElseThrow();
        assertTrue(top >= 24, "tallest basalt tower reaches only y" + top);
    }

    @Test
    void twinTowersFlankAnOpenCentralGap() {
        var pillars = VaderCastleLayout.placements().stream()
            .filter(p -> p.kind() == VaderCastleLayout.Kind.PILLAR).toList();
        // Pillars exist on both the left and right of the fortress center...
        int mid = VaderCastleLayout.SIZE_X / 2;
        assertTrue(pillars.stream().anyMatch(p -> p.dx() < mid), "no left tower");
        assertTrue(pillars.stream().anyMatch(p -> p.dx() > mid), "no right tower");
        // ...separated by a real gap (the tuning-fork silhouette).
        int leftMax = pillars.stream().filter(p -> p.dx() < mid)
            .mapToInt(VaderCastleLayout.Placement::dx).max().orElseThrow();
        int rightMin = pillars.stream().filter(p -> p.dx() > mid)
            .mapToInt(VaderCastleLayout.Placement::dx).min().orElseThrow();
        assertTrue(rightMin - leftMax >= 3, "towers not separated by a central gap");
    }

    @Test
    void pillarsAreMirroredInX() {
        var placements = VaderCastleLayout.placements();
        Set<Long> pillars = new HashSet<>();
        for (var p : placements) {
            if (p.kind() == VaderCastleLayout.Kind.PILLAR) pillars.add(key(p.dx(), p.dy(), p.dz()));
        }
        for (var p : placements) {
            if (p.kind() != VaderCastleLayout.Kind.PILLAR) continue;
            int mirror = VaderCastleLayout.SIZE_X - 1 - p.dx();
            assertTrue(pillars.contains(key(mirror, p.dy(), p.dz())), "no mirror pillar for " + p);
        }
    }

    @Test
    void wallsAreMirroredInX() {
        var placements = VaderCastleLayout.placements();
        Set<Long> walls = new HashSet<>();
        for (var p : placements) {
            if (p.kind() == VaderCastleLayout.Kind.WALL) walls.add(key(p.dx(), p.dy(), p.dz()));
        }
        for (var p : placements) {
            if (p.kind() != VaderCastleLayout.Kind.WALL) continue;
            int mirror = VaderCastleLayout.SIZE_X - 1 - p.dx();
            assertTrue(walls.contains(key(mirror, p.dy(), p.dz())), "no mirror wall for " + p);
        }
    }

    @Test
    void hasThroneAndGate() {
        var placements = VaderCastleLayout.placements();
        assertTrue(placements.stream().anyMatch(p -> p.kind() == VaderCastleLayout.Kind.THRONE));
        assertTrue(placements.stream().anyMatch(p -> p.kind() == VaderCastleLayout.Kind.GATE_AIR));
    }

    @Test
    void hasBraziersAndMagmaAccents() {
        var placements = VaderCastleLayout.placements();
        assertTrue(placements.stream().anyMatch(p -> p.kind() == VaderCastleLayout.Kind.BRAZIER));
        assertTrue(placements.stream().anyMatch(p -> p.kind() == VaderCastleLayout.Kind.MAGMA));
    }

    @Test
    void chestLidCanOpen() {
        var placements = VaderCastleLayout.placements();
        var chest = placements.stream()
            .filter(p -> p.kind() == VaderCastleLayout.Kind.CHEST).findFirst().orElseThrow();
        assertTrue(placements.stream().anyMatch(p ->
            p.kind() == VaderCastleLayout.Kind.AIR
                && p.dx() == chest.dx() && p.dy() == chest.dy() + 1 && p.dz() == chest.dz()));
    }

    @Test
    void noDuplicateCells() {
        Set<Long> seen = new HashSet<>();
        for (var p : VaderCastleLayout.placements()) {
            assertTrue(seen.add(key(p.dx(), p.dy(), p.dz())), "duplicate cell: " + p);
        }
    }

    @Test
    void allPlacementsInsideDeclaredBounds() {
        for (var p : VaderCastleLayout.placements()) {
            assertTrue(p.dx() >= 0 && p.dx() < VaderCastleLayout.SIZE_X, p.toString());
            assertTrue(p.dy() >= 0 && p.dy() < VaderCastleLayout.SIZE_Y, p.toString());
            assertTrue(p.dz() >= 0 && p.dz() < VaderCastleLayout.SIZE_Z, p.toString());
        }
    }

    @Test
    void hasInteriorAir() {
        assertTrue(VaderCastleLayout.placements().stream()
            .anyMatch(p -> p.kind() == VaderCastleLayout.Kind.AIR));
    }

    @Test
    void placementsAreDeterministic() {
        assertEquals(VaderCastleLayout.placements(), VaderCastleLayout.placements());
    }
}
