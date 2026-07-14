package com.tweeks.starwars.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Krayt dragon skeleton: a 9x6x26 dune-bleached landmark, bone blocks only —
 * a skull mass with eye sockets and an open jaw at the low-z end, a two-thick
 * vertebral ridge running along z at y1..2 (grounded every third segment by a
 * half-sunk vertebra at y0), mirrored rib arcs curving up and over every third
 * spine segment, and a tail that tapers down into the sand (y0) at the far
 * end. No chest, no mobs. Pure data so shape invariants are unit-testable
 * without MC bootstrap.
 */
public final class KraytSkeletonLayout {
    private KraytSkeletonLayout() {}

    public static final int SIZE_X = 9;
    public static final int SIZE_Y = 6;
    public static final int SIZE_Z = 26;

    public enum Kind { BONE }

    public record Placement(int dx, int dy, int dz, Kind kind) {}

    /** Center line of the skeleton. */
    public static final int SPINE_X = 4;

    /** Rib arcs sit over every third spine segment. */
    public static final int[] RIB_Z = { 6, 9, 12, 15, 18 };

    /** One rib arc: (x, y) cells, mirrored around SPINE_X. */
    private static final int[][] ARCH = {
        {0, 0}, {0, 1},
        {1, 2}, {1, 3},
        {2, 4},
        {3, 5}, {4, 5}, {5, 5},
        {6, 4},
        {7, 3}, {7, 2},
        {8, 1}, {8, 0},
    };

    public static List<Placement> placements() {
        List<Placement> out = new ArrayList<>();

        // Skull mass (~5x4x5) at the low-z end, half-sunk (base at y0), with a
        // rounded crown, two eye-socket gaps on the front face, and an open jaw.
        for (int x = 2; x <= 6; x++) {
            for (int y = 0; y <= 3; y++) {
                for (int z = 0; z <= 4; z++) {
                    boolean crown = (x == 2 || x == 6) && y == 3;
                    boolean eye = z == 0 && y == 2 && (x == 3 || x == 5);
                    boolean jaw = z <= 1 && y == 0 && x >= 3 && x <= 5;
                    if (crown || eye || jaw) continue;
                    out.add(new Placement(x, y, z, Kind.BONE));
                }
            }
        }

        // Vertebral ridge: two-thick spine (y1..2) behind the skull, with a
        // half-sunk vertebra (y0) grounding every third segment.
        for (int z = 5; z <= 19; z++) {
            out.add(new Placement(SPINE_X, 2, z, Kind.BONE));
            out.add(new Placement(SPINE_X, 1, z, Kind.BONE));
            if (z % 3 == 2) {
                out.add(new Placement(SPINE_X, 0, z, Kind.BONE));
            }
        }

        // Tail: tapers to a single line, sinking into the sand.
        out.add(new Placement(SPINE_X, 1, 20, Kind.BONE));
        out.add(new Placement(SPINE_X, 1, 21, Kind.BONE));
        for (int z = 22; z <= 25; z++) {
            out.add(new Placement(SPINE_X, 0, z, Kind.BONE));
        }

        // Rib arcs, mirrored in x around the spine, every third segment.
        for (int z : RIB_Z) {
            for (int[] cell : ARCH) {
                out.add(new Placement(cell[0], cell[1], z, Kind.BONE));
            }
        }

        return out;
    }
}
