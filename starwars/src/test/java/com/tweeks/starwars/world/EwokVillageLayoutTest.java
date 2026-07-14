package com.tweeks.starwars.world;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class EwokVillageLayoutTest {

    @Test
    void exactlyOneChest() {
        long chests = EwokVillageLayout.placements().stream()
            .filter(p -> p.kind() == EwokVillageLayout.Kind.CHEST).count();
        assertEquals(1, chests);
    }

    @Test
    void ewokMarkerCountIsThreeToFour() {
        long ewoks = EwokVillageLayout.placements().stream()
            .filter(p -> p.kind() == EwokVillageLayout.Kind.EWOK).count();
        assertTrue(ewoks >= 3 && ewoks <= 4, "ewok markers: " + ewoks);
    }

    @Test
    void exactlyOneBonfire() {
        long bonfires = EwokVillageLayout.placements().stream()
            .filter(p -> p.kind() == EwokVillageLayout.Kind.BONFIRE).count();
        assertEquals(1, bonfires);
    }

    @Test
    void hutsStandOnRaisedDecks() {
        var placements = EwokVillageLayout.placements();
        // Plank decks sit well above the forest floor (six-block stilts).
        assertTrue(placements.stream().anyMatch(p ->
            p.kind() == EwokVillageLayout.Kind.FLOOR && p.dy() >= 6));
        // Nothing calls the ground level a deck.
        assertTrue(placements.stream().noneMatch(p ->
            p.kind() == EwokVillageLayout.Kind.FLOOR && p.dy() == 0));
    }

    @Test
    void stiltsRunFromFloorUpToTheDeck() {
        var placements = EwokVillageLayout.placements();
        // At least one stilt starts on the forest floor...
        assertTrue(placements.stream().anyMatch(p ->
            p.kind() == EwokVillageLayout.Kind.STILT && p.dy() == 1));
        // ...and rises to just under the deck.
        assertTrue(placements.stream().anyMatch(p ->
            p.kind() == EwokVillageLayout.Kind.STILT && p.dy() == 6));
    }

    @Test
    void roofsCapTheHutsAboveTheWalls() {
        var placements = EwokVillageLayout.placements();
        int roofMin = placements.stream()
            .filter(p -> p.kind() == EwokVillageLayout.Kind.ROOF)
            .mapToInt(EwokVillageLayout.Placement::dy).min().orElseThrow();
        int wallMax = placements.stream()
            .filter(p -> p.kind() == EwokVillageLayout.Kind.WALL)
            .mapToInt(EwokVillageLayout.Placement::dy).max().orElseThrow();
        assertTrue(roofMin > wallMax, "roof (" + roofMin + ") not above walls (" + wallMax + ")");
    }

    @Test
    void roofLayersShrinkAsTheyRise() {
        var placements = EwokVillageLayout.placements();
        // The lowest roof layer must be strictly wider (more cells) than the
        // highest, i.e. the thatch tapers toward a cap.
        int roofMin = placements.stream()
            .filter(p -> p.kind() == EwokVillageLayout.Kind.ROOF)
            .mapToInt(EwokVillageLayout.Placement::dy).min().orElseThrow();
        int roofMax = placements.stream()
            .filter(p -> p.kind() == EwokVillageLayout.Kind.ROOF)
            .mapToInt(EwokVillageLayout.Placement::dy).max().orElseThrow();
        assertTrue(roofMax > roofMin, "roof has no vertical taper");
        long bottom = placements.stream()
            .filter(p -> p.kind() == EwokVillageLayout.Kind.ROOF && p.dy() == roofMin).count();
        long top = placements.stream()
            .filter(p -> p.kind() == EwokVillageLayout.Kind.ROOF && p.dy() == roofMax).count();
        assertTrue(bottom > top, "roof does not taper (bottom " + bottom + " <= top " + top + ")");
    }

    @Test
    void bridgesHaveFenceRails() {
        var placements = EwokVillageLayout.placements();
        assertTrue(placements.stream().anyMatch(p -> p.kind() == EwokVillageLayout.Kind.BRIDGE));
        assertTrue(placements.stream().anyMatch(p -> p.kind() == EwokVillageLayout.Kind.RAIL));
        // Rails ride one block above the deck (handrail height).
        int deckY = placements.stream()
            .filter(p -> p.kind() == EwokVillageLayout.Kind.BRIDGE)
            .mapToInt(EwokVillageLayout.Placement::dy).min().orElseThrow();
        assertTrue(placements.stream()
            .filter(p -> p.kind() == EwokVillageLayout.Kind.RAIL)
            .allMatch(p -> p.dy() == deckY + 1));
    }

    @Test
    void laddersReachTheGround() {
        var placements = EwokVillageLayout.placements();
        assertTrue(placements.stream().anyMatch(p ->
            p.kind() == EwokVillageLayout.Kind.LADDER && p.dy() == 1));
    }

    @Test
    void chestLidCanOpen() {
        var placements = EwokVillageLayout.placements();
        var chest = placements.stream()
            .filter(p -> p.kind() == EwokVillageLayout.Kind.CHEST).findFirst().orElseThrow();
        assertTrue(placements.stream().anyMatch(p ->
            p.kind() == EwokVillageLayout.Kind.AIR
                && p.dx() == chest.dx() && p.dy() == chest.dy() + 1 && p.dz() == chest.dz()));
    }

    @Test
    void noDuplicateCells() {
        Set<Long> seen = new HashSet<>();
        for (var p : EwokVillageLayout.placements()) {
            long key = ((long) p.dx() << 40) | ((long) (p.dy() + 8) << 20) | p.dz();
            assertTrue(seen.add(key), "duplicate cell: " + p);
        }
    }

    @Test
    void allPlacementsInsideDeclaredBounds() {
        for (var p : EwokVillageLayout.placements()) {
            assertTrue(p.dx() >= 0 && p.dx() < EwokVillageLayout.SIZE_X, p.toString());
            assertTrue(p.dy() >= 0 && p.dy() < EwokVillageLayout.SIZE_Y, p.toString());
            assertTrue(p.dz() >= 0 && p.dz() < EwokVillageLayout.SIZE_Z, p.toString());
        }
    }

    @Test
    void hasInteriorAir() {
        assertTrue(EwokVillageLayout.placements().stream()
            .anyMatch(p -> p.kind() == EwokVillageLayout.Kind.AIR));
    }

    @Test
    void placementsAreDeterministic() {
        assertEquals(EwokVillageLayout.placements(), EwokVillageLayout.placements());
    }
}
