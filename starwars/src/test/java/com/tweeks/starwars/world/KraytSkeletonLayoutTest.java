package com.tweeks.starwars.world;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class KraytSkeletonLayoutTest {

    @Test
    void everyPlacementIsBone() {
        assertTrue(KraytSkeletonLayout.placements().stream()
            .allMatch(p -> p.kind() == KraytSkeletonLayout.Kind.BONE));
        assertFalse(KraytSkeletonLayout.placements().isEmpty());
    }

    @Test
    void allPlacementsInsideDeclaredBounds() {
        for (var p : KraytSkeletonLayout.placements()) {
            assertTrue(p.dx() >= 0 && p.dx() < KraytSkeletonLayout.SIZE_X, p.toString());
            assertTrue(p.dy() >= 0 && p.dy() < KraytSkeletonLayout.SIZE_Y, p.toString());
            assertTrue(p.dz() >= 0 && p.dz() < KraytSkeletonLayout.SIZE_Z, p.toString());
        }
    }

    /** Vertebral column cells: on the spine line, below the rib-arch tops. */
    private static List<KraytSkeletonLayout.Placement> spineCells() {
        return KraytSkeletonLayout.placements().stream()
            .filter(p -> p.dx() == KraytSkeletonLayout.SPINE_X && p.dy() <= 3)
            .toList();
    }

    @Test
    void spineIsContinuousFromSkullToTailTip() {
        var spine = spineCells();
        // Attachment point on the back of the skull.
        assertTrue(spine.stream().anyMatch(p -> p.dz() == 4 && p.dy() == 2));
        for (int z = 5; z < KraytSkeletonLayout.SIZE_Z; z++) {
            final int fz = z;
            List<Integer> ys = spine.stream()
                .filter(p -> p.dz() == fz).map(KraytSkeletonLayout.Placement::dy).toList();
            assertFalse(ys.isEmpty(), "spine gap at z=" + z);
            if (z > 5) {
                final int pz = z - 1;
                List<Integer> prev = spine.stream()
                    .filter(p -> p.dz() == pz).map(KraytSkeletonLayout.Placement::dy).toList();
                int step = ys.stream()
                    .mapToInt(a -> prev.stream().mapToInt(b -> Math.abs(a - b)).min().orElseThrow())
                    .min().orElseThrow();
                assertTrue(step <= 1, "spine breaks between z=" + pz + " and z=" + z);
            }
        }
    }

    @Test
    void ribArcsAreMirroredInX() {
        var placements = KraytSkeletonLayout.placements();
        for (int z : KraytSkeletonLayout.RIB_Z) {
            final int fz = z;
            // Rib cells at this z: everything except the vertebral column.
            var rib = placements.stream()
                .filter(p -> p.dz() == fz
                    && !(p.dx() == KraytSkeletonLayout.SPINE_X && p.dy() <= 3))
                .toList();
            assertFalse(rib.isEmpty(), "no rib at z=" + z);
            for (var p : rib) {
                int mirrored = KraytSkeletonLayout.SIZE_X - 1 - p.dx();
                assertTrue(rib.stream().anyMatch(q ->
                        q.dx() == mirrored && q.dy() == p.dy()),
                    "no mirror for " + p);
            }
        }
    }

    @Test
    void ribsArcOverTheSpine() {
        // Each rib reaches the top layer above the spine line.
        var placements = KraytSkeletonLayout.placements();
        for (int z : KraytSkeletonLayout.RIB_Z) {
            final int fz = z;
            assertTrue(placements.stream().anyMatch(p ->
                    p.dz() == fz && p.dx() == KraytSkeletonLayout.SPINE_X
                        && p.dy() == KraytSkeletonLayout.SIZE_Y - 1),
                "rib at z=" + z + " does not arc over the spine");
        }
    }

    @Test
    void someVertebraeAreHalfSunk() {
        assertTrue(KraytSkeletonLayout.placements().stream()
            .anyMatch(p -> p.dx() == KraytSkeletonLayout.SPINE_X && p.dy() == 0));
    }

    @Test
    void skullHasEyeSocketGaps() {
        var placements = KraytSkeletonLayout.placements();
        // Front face (z=0) eye positions are gaps, flanking solid bone at x=4.
        assertTrue(placements.stream().noneMatch(p ->
            p.dz() == 0 && p.dy() == 2 && (p.dx() == 3 || p.dx() == 5)));
        assertTrue(placements.stream().anyMatch(p ->
            p.dz() == 0 && p.dy() == 2 && p.dx() == 4));
    }

    @Test
    void placementsAreDeterministic() {
        assertEquals(KraytSkeletonLayout.placements(), KraytSkeletonLayout.placements());
    }
}
