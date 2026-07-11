package com.tweeks.starwars.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Imperial garrison outpost: an 11x7x11 fortified compound — a blackstone
 * platform, four polished-blackstone corner pillars, gray-concrete perimeter
 * walls with a 3-wide south gate, an alternating (crenellated) polished-
 * blackstone-slab roof ring on the wall tops, interior air, one loot chest,
 * and garrison markers (spawned as living entities at generation time).
 * Pure data so shape invariants are unit-testable without MC bootstrap.
 */
public final class ImperialOutpostLayout {
    private ImperialOutpostLayout() {}

    public static final int SIZE_X = 11;
    public static final int SIZE_Y = 7;
    public static final int SIZE_Z = 11;

    public enum Kind { PLATFORM, PILLAR, WALL, ROOF, CHEST, AIR, GARRISON_TROOPER, GARRISON_DROID }

    public record Placement(int dx, int dy, int dz, Kind kind) {}

    private static final int[][] PILLARS = { {1, 1}, {1, 9}, {9, 1}, {9, 9} };

    private static boolean isPillar(int x, int z) {
        for (int[] c : PILLARS) {
            if (c[0] == x && c[1] == z) return true;
        }
        return false;
    }

    /** Interior (x,z) columns reserved for the chest/garrison at y=1. */
    private static boolean isReserved(int x, int z) {
        return (x == 2 && z == 8)      // chest
            || (x == 3 && z == 5)      // trooper
            || (x == 7 && z == 5)      // trooper
            || (x == 5 && z == 7)      // trooper
            || (x == 4 && z == 3)      // droid
            || (x == 6 && z == 3);     // droid
    }

    public static List<Placement> placements() {
        List<Placement> out = new ArrayList<>();

        // 1-block blackstone platform (y0).
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                out.add(new Placement(x, 0, z, Kind.PLATFORM));
            }
        }

        // Four polished-blackstone corner pillars (y1..5).
        for (int[] c : PILLARS) {
            for (int y = 1; y <= 5; y++) {
                out.add(new Placement(c[0], y, c[1], Kind.PILLAR));
            }
        }

        // Perimeter walls (y1..3) with a 3-wide south gate (z=0, x=4..6), a
        // crenellated roof ring (y4, alternating) on the wall tops, and
        // interior air.
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                boolean edge = x == 0 || x == SIZE_X - 1 || z == 0 || z == SIZE_Z - 1;
                if (edge) {
                    boolean gate = z == 0 && x >= 4 && x <= 6;
                    for (int y = 1; y <= 3; y++) {
                        if (gate) continue;                 // south gate opening
                        out.add(new Placement(x, y, z, Kind.WALL));
                    }
                    if ((x + z) % 2 == 0) {                 // crenellation
                        out.add(new Placement(x, 4, z, Kind.ROOF));
                    }
                } else if (!isPillar(x, z)) {
                    for (int y = 1; y <= 3; y++) {
                        if (y == 1 && isReserved(x, z)) continue;
                        out.add(new Placement(x, y, z, Kind.AIR));
                    }
                }
            }
        }

        // Loot chest against the interior back wall.
        out.add(new Placement(2, 1, 8, Kind.CHEST));

        // Garrison markers (spawned as living entities at generation time).
        out.add(new Placement(3, 1, 5, Kind.GARRISON_TROOPER));
        out.add(new Placement(7, 1, 5, Kind.GARRISON_TROOPER));
        out.add(new Placement(5, 1, 7, Kind.GARRISON_TROOPER));
        out.add(new Placement(4, 1, 3, Kind.GARRISON_DROID));
        out.add(new Placement(6, 1, 3, Kind.GARRISON_DROID));

        return out;
    }
}
