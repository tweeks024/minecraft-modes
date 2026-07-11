package com.tweeks.starwars.world;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EscapePodLayoutTest {

    @Test
    void exactlyOneChest() {
        long chests = EscapePodLayout.placements().stream()
            .filter(p -> p.kind() == EscapePodLayout.Kind.CHEST).count();
        assertEquals(1, chests);
    }

    @Test
    void allPlacementsInsideDeclaredBounds() {
        for (var p : EscapePodLayout.placements()) {
            assertTrue(p.dx() >= 0 && p.dx() < EscapePodLayout.SIZE_X, p.toString());
            assertTrue(p.dy() >= 0 && p.dy() < EscapePodLayout.SIZE_Y, p.toString());
            assertTrue(p.dz() >= 0 && p.dz() < EscapePodLayout.SIZE_Z, p.toString());
        }
    }

    @Test
    void shellIsMirroredInX() {
        List<EscapePodLayout.Placement> shell = EscapePodLayout.placements().stream()
            .filter(p -> p.kind() == EscapePodLayout.Kind.SHELL).toList();
        for (var p : shell) {
            int mirrored = EscapePodLayout.SIZE_X - 1 - p.dx();
            assertTrue(shell.stream().anyMatch(q ->
                    q.dx() == mirrored && q.dy() == p.dy() && q.dz() == p.dz()),
                "no mirror for " + p);
        }
    }

    @Test
    void hasInteriorAir() {
        assertTrue(EscapePodLayout.placements().stream()
            .anyMatch(p -> p.kind() == EscapePodLayout.Kind.AIR));
    }
}
