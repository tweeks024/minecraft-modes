package com.tweeks.starwars.world;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class MosEisleyLayoutTest {

    @Test
    void exactlyOneCantinaChest() {
        long chests = MosEisleyLayout.placements().stream()
            .filter(p -> p.kind() == MosEisleyLayout.Kind.CHEST_CANTINA).count();
        assertEquals(1, chests);
    }

    @Test
    void exactlyOneDockingChest() {
        long chests = MosEisleyLayout.placements().stream()
            .filter(p -> p.kind() == MosEisleyLayout.Kind.CHEST_DOCKING).count();
        assertEquals(1, chests);
    }

    @Test
    void streetLifeMarkerCounts() {
        var placements = MosEisleyLayout.placements();
        assertEquals(3, placements.stream()
            .filter(p -> p.kind() == MosEisleyLayout.Kind.JAWA).count());
        assertEquals(2, placements.stream()
            .filter(p -> p.kind() == MosEisleyLayout.Kind.STORMTROOPER).count());
        assertEquals(1, placements.stream()
            .filter(p -> p.kind() == MosEisleyLayout.Kind.ASTROMECH).count());
    }

    @Test
    void cantinaHasJukeboxTablesAndDimLighting() {
        var placements = MosEisleyLayout.placements();
        assertEquals(1, placements.stream()
            .filter(p -> p.kind() == MosEisleyLayout.Kind.JUKEBOX).count());
        assertEquals(4, placements.stream()
            .filter(p -> p.kind() == MosEisleyLayout.Kind.TABLE).count());
        assertTrue(placements.stream()
            .filter(p -> p.kind() == MosEisleyLayout.Kind.LANTERN).count() >= 4);
    }

    @Test
    void cantinaChestSitsBesideTheJukebox() {
        var placements = MosEisleyLayout.placements();
        var jukebox = placements.stream()
            .filter(p -> p.kind() == MosEisleyLayout.Kind.JUKEBOX).findFirst().orElseThrow();
        var chest = placements.stream()
            .filter(p -> p.kind() == MosEisleyLayout.Kind.CHEST_CANTINA).findFirst().orElseThrow();
        assertEquals(jukebox.dy(), chest.dy());
        assertEquals(1, Math.abs(jukebox.dx() - chest.dx()) + Math.abs(jukebox.dz() - chest.dz()));
    }

    @Test
    void fourVaporatorMarkersWithHeadroom() {
        var vaporators = MosEisleyLayout.placements().stream()
            .filter(p -> p.kind() == MosEisleyLayout.Kind.VAPORATOR).toList();
        assertEquals(4, vaporators.size());
        // The piece builds two more blocks above each marker (wall, wall, rod).
        for (var v : vaporators) {
            assertTrue(v.dy() + 2 < MosEisleyLayout.SIZE_Y, v.toString());
        }
    }

    @Test
    void streetLampsStandClearOnPosts() {
        var placements = MosEisleyLayout.placements();
        var lamps = placements.stream()
            .filter(p -> p.kind() == MosEisleyLayout.Kind.LAMP).toList();
        assertEquals(6, lamps.size());
        for (var l : lamps) {
            // The piece builds two more blocks above each marker (post, torch).
            assertTrue(l.dy() + 2 < MosEisleyLayout.SIZE_Y, l.toString());
            // Nothing occupies the post/torch cells above the marker.
            assertTrue(placements.stream().noneMatch(p ->
                    p.dx() == l.dx() && p.dz() == l.dz() && p.dy() > l.dy()),
                "lamp post obstructed: " + l);
        }
    }

    @Test
    void tableTopsAreUnobstructed() {
        var placements = MosEisleyLayout.placements();
        var tables = placements.stream()
            .filter(p -> p.kind() == MosEisleyLayout.Kind.TABLE).toList();
        for (var t : tables) {
            assertTrue(placements.stream().noneMatch(p ->
                    p.dx() == t.dx() && p.dz() == t.dz() && p.dy() == t.dy() + 1),
                "tabletop obstructed: " + t);
        }
    }

    @Test
    void dockingBayHasPadAndRingWall() {
        var placements = MosEisleyLayout.placements();
        assertTrue(placements.stream().anyMatch(p -> p.kind() == MosEisleyLayout.Kind.PAD));
        assertTrue(placements.stream().anyMatch(p -> p.kind() == MosEisleyLayout.Kind.PAD_WALL));
        // The astromech waits on the pad itself.
        var droid = placements.stream()
            .filter(p -> p.kind() == MosEisleyLayout.Kind.ASTROMECH).findFirst().orElseThrow();
        assertTrue(placements.stream().anyMatch(p ->
            p.kind() == MosEisleyLayout.Kind.PAD
                && p.dx() == droid.dx() && p.dz() == droid.dz() && p.dy() == droid.dy() - 1));
    }

    @Test
    void townIsBellFree() {
        for (var kind : MosEisleyLayout.Kind.values()) {
            assertNotEquals("BELL", kind.name());
        }
    }

    @Test
    void noDuplicateCells() {
        Set<Long> seen = new HashSet<>();
        for (var p : MosEisleyLayout.placements()) {
            long key = ((long) p.dx() << 40) | ((long) (p.dy() + 8) << 20) | p.dz();
            assertTrue(seen.add(key), "duplicate cell: " + p);
        }
    }

    @Test
    void allPlacementsInsideDeclaredBounds() {
        for (var p : MosEisleyLayout.placements()) {
            assertTrue(p.dx() >= 0 && p.dx() < MosEisleyLayout.SIZE_X, p.toString());
            assertTrue(p.dy() >= 0 && p.dy() < MosEisleyLayout.SIZE_Y, p.toString());
            assertTrue(p.dz() >= 0 && p.dz() < MosEisleyLayout.SIZE_Z, p.toString());
        }
    }

    @Test
    void hasInteriorAir() {
        assertTrue(MosEisleyLayout.placements().stream()
            .anyMatch(p -> p.kind() == MosEisleyLayout.Kind.AIR));
    }

    @Test
    void placementsAreDeterministic() {
        assertEquals(MosEisleyLayout.placements(), MosEisleyLayout.placements());
    }
}
