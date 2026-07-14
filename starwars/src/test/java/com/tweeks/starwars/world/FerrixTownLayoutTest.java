package com.tweeks.starwars.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FerrixTownLayoutTest {

    @Test
    void exactlyOneChest() {
        long chests = FerrixTownLayout.placements().stream()
            .filter(p -> p.kind() == FerrixTownLayout.Kind.CHEST).count();
        assertEquals(1, chests);
    }

    @Test
    void exactlyTwoStormtrooperMarkers() {
        long troopers = FerrixTownLayout.placements().stream()
            .filter(p -> p.kind() == FerrixTownLayout.Kind.STORMTROOPER).count();
        assertEquals(2, troopers);
    }

    @Test
    void exactlyOneBell() {
        long bells = FerrixTownLayout.placements().stream()
            .filter(p -> p.kind() == FerrixTownLayout.Kind.BELL).count();
        assertEquals(1, bells);
    }

    @Test
    void bellSitsAtTowerTopOnSupport() {
        var placements = FerrixTownLayout.placements();
        var bell = placements.stream()
            .filter(p -> p.kind() == FerrixTownLayout.Kind.BELL).findFirst().orElseThrow();
        assertEquals(FerrixTownLayout.SIZE_Y - 1, bell.dy());
        // The belfry slab directly below keeps the floor-attached bell valid.
        assertTrue(placements.stream().anyMatch(p ->
            p.kind() == FerrixTownLayout.Kind.TOWER_WALL
                && p.dx() == bell.dx() && p.dy() == bell.dy() - 1 && p.dz() == bell.dz()));
    }

    @Test
    void allPlacementsInsideDeclaredBounds() {
        for (var p : FerrixTownLayout.placements()) {
            assertTrue(p.dx() >= 0 && p.dx() < FerrixTownLayout.SIZE_X, p.toString());
            assertTrue(p.dy() >= 0 && p.dy() < FerrixTownLayout.SIZE_Y, p.toString());
            assertTrue(p.dz() >= 0 && p.dz() < FerrixTownLayout.SIZE_Z, p.toString());
        }
    }

    @Test
    void streetDoorsAreOpen() {
        // Both house doors and the tower door: 2-tall DOOR_AIR openings, and no
        // WALL/TOWER_WALL cell occupies a doorway cell.
        var placements = FerrixTownLayout.placements();
        long doorCells = placements.stream()
            .filter(p -> p.kind() == FerrixTownLayout.Kind.DOOR_AIR).count();
        assertEquals(6, doorCells);
        assertTrue(placements.stream()
            .filter(p -> p.kind() == FerrixTownLayout.Kind.WALL
                || p.kind() == FerrixTownLayout.Kind.TOWER_WALL)
            .noneMatch(w -> placements.stream().anyMatch(d ->
                d.kind() == FerrixTownLayout.Kind.DOOR_AIR
                    && d.dx() == w.dx() && d.dy() == w.dy() && d.dz() == w.dz())));
    }

    @Test
    void hasStreetFacingWindows() {
        long windows = FerrixTownLayout.placements().stream()
            .filter(p -> p.kind() == FerrixTownLayout.Kind.WINDOW).count();
        assertEquals(8, windows);
    }

    @Test
    void hasInteriorAir() {
        assertTrue(FerrixTownLayout.placements().stream()
            .anyMatch(p -> p.kind() == FerrixTownLayout.Kind.AIR));
    }

    @Test
    void placementsAreDeterministic() {
        assertEquals(FerrixTownLayout.placements(), FerrixTownLayout.placements());
    }
}
