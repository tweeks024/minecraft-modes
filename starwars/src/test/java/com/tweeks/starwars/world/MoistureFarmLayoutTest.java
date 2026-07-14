package com.tweeks.starwars.world;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MoistureFarmLayoutTest {

    @Test
    void exactlyOneChest() {
        long chests = MoistureFarmLayout.placements().stream()
            .filter(p -> p.kind() == MoistureFarmLayout.Kind.CHEST).count();
        assertEquals(1, chests);
    }

    @Test
    void fourVaporatorMarkers() {
        long vaporators = MoistureFarmLayout.placements().stream()
            .filter(p -> p.kind() == MoistureFarmLayout.Kind.VAPORATOR).count();
        assertEquals(4, vaporators);
    }

    @Test
    void vaporatorsStandOutsideTheDome() {
        // No dome shell cell shares a column with a vaporator marker.
        var placements = MoistureFarmLayout.placements();
        var vaporators = placements.stream()
            .filter(p -> p.kind() == MoistureFarmLayout.Kind.VAPORATOR).toList();
        for (var v : vaporators) {
            assertTrue(placements.stream().noneMatch(p ->
                    p.kind() == MoistureFarmLayout.Kind.DOME
                        && p.dx() == v.dx() && p.dz() == v.dz()),
                "vaporator under dome shell: " + v);
        }
    }

    @Test
    void allPlacementsInsideDeclaredBounds() {
        for (var p : MoistureFarmLayout.placements()) {
            assertTrue(p.dx() >= 0 && p.dx() < MoistureFarmLayout.SIZE_X, p.toString());
            assertTrue(p.dy() >= 0 && p.dy() < MoistureFarmLayout.SIZE_Y, p.toString());
            assertTrue(p.dz() >= 0 && p.dz() < MoistureFarmLayout.SIZE_Z, p.toString());
        }
    }

    @Test
    void vaporatorPillarsFitInsideBounds() {
        // The piece builds two more blocks above each marker (wall, wall, rod).
        for (var p : MoistureFarmLayout.placements()) {
            if (p.kind() != MoistureFarmLayout.Kind.VAPORATOR) continue;
            assertTrue(p.dy() + 2 < MoistureFarmLayout.SIZE_Y, p.toString());
        }
    }

    @Test
    void domeIsMirroredInX() {
        List<MoistureFarmLayout.Placement> dome = MoistureFarmLayout.placements().stream()
            .filter(p -> p.kind() == MoistureFarmLayout.Kind.DOME).toList();
        for (var p : dome) {
            int mirrored = MoistureFarmLayout.SIZE_X - 1 - p.dx();
            assertTrue(dome.stream().anyMatch(q ->
                    q.dx() == mirrored && q.dy() == p.dy() && q.dz() == p.dz()),
                "no mirror for " + p);
        }
    }

    @Test
    void hasInteriorAir() {
        assertTrue(MoistureFarmLayout.placements().stream()
            .anyMatch(p -> p.kind() == MoistureFarmLayout.Kind.AIR));
    }

    @Test
    void placementsAreDeterministic() {
        assertEquals(MoistureFarmLayout.placements(), MoistureFarmLayout.placements());
    }
}
