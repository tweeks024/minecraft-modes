package com.tweeks.starwars.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Yoda's hut (Dagobah): a 9x6x9 organic mud dome — a hemispherical shell
 * mixing mud and muddy mangrove roots (two kinds, deterministically woven),
 * a round door gap on the south side, and a tiny interior: a two-block
 * sleeping pallet, a cooking pot, one loot chest, and a single Yoda marker.
 * Pure data so shape invariants are unit-testable without MC bootstrap.
 */
public final class YodaHutLayout {
    private YodaHutLayout() {}

    public static final int SIZE_X = 9;
    public static final int SIZE_Y = 6;
    public static final int SIZE_Z = 9;

    public enum Kind { DOME_MUD, DOME_ROOTS, FLOOR, PALLET, POT, CHEST, AIR, YODA }

    public record Placement(int dx, int dy, int dz, Kind kind) {}

    private static final int CENTER_X = 4;
    private static final int CENTER_Z = 4;
    /** Squared-distance band of the dome shell: R^2 - R .. R^2 + R for R ~ 3.5. */
    private static final int SHELL_INNER_SQ = 9;
    private static final int SHELL_OUTER_SQ = 16;

    public static List<Placement> placements() {
        List<Placement> out = new ArrayList<>();

        // Packed-mud floor disc (y0) under the dome footprint.
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                int flat = sq(x - CENTER_X) + sq(z - CENTER_Z);
                if (flat <= SHELL_OUTER_SQ) {
                    out.add(new Placement(x, 0, z, Kind.FLOOR));
                }
            }
        }

        // Organic dome shell: mud woven with muddy mangrove roots, with a
        // round door gap carved through the south (low-z) side — a 3-wide
        // base arching to a 1-wide crown.
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                for (int y = 1; y < SIZE_Y; y++) {
                    int d2 = sq(x - CENTER_X) + y * y + sq(z - CENTER_Z);
                    boolean doorColumn = z < CENTER_Z && Math.abs(x - CENTER_X) <= 1 && y <= 2;
                    boolean doorCorner = Math.abs(x - CENTER_X) == 1 && y == 2;
                    boolean door = doorColumn && !doorCorner;   // rounded arch
                    if (d2 >= SHELL_INNER_SQ && d2 <= SHELL_OUTER_SQ) {
                        if (!door) {
                            out.add(new Placement(x, y, z, weave(x, y, z)));
                        }
                    } else if (d2 < SHELL_INNER_SQ) {
                        out.add(new Placement(x, y, z, Kind.AIR));
                    }
                }
            }
        }

        // Sleeping pallet against the west interior wall.
        replace(out, 2, 1, 4, Kind.PALLET);
        replace(out, 2, 1, 5, Kind.PALLET);
        // Cooking pot near the middle of the hut.
        replace(out, 6, 1, 5, Kind.POT);
        // Loot chest against the north interior (interior air above, so the
        // lid can open).
        replace(out, 4, 1, 6, Kind.CHEST);
        // Yoda himself, inside by the door (his singleton logic dedupes).
        replace(out, 5, 1, 3, Kind.YODA);

        return out;
    }

    /** Deterministic mud/roots weave for the organic shell. */
    private static Kind weave(int x, int y, int z) {
        return (x + y + z) % 3 == 0 ? Kind.DOME_ROOTS : Kind.DOME_MUD;
    }

    private static void replace(List<Placement> out, int x, int y, int z, Kind kind) {
        out.removeIf(p -> p.dx() == x && p.dy() == y && p.dz() == z);
        out.add(new Placement(x, y, z, kind));
    }

    private static int sq(int v) {
        return v * v;
    }
}
