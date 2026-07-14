package com.tweeks.starwars.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Tatooine moisture farm (Lars-style homestead): a 13x8x13 plot — a hollow
 * smooth-sandstone dome (radius ~4) over a sandstone floor disc, a south
 * doorway carved through the shell, one loot chest inside, interior air, and
 * four vaporator markers on sandstone pads at the plot corners (the piece
 * builds each marker into a 3-tall GX-8 style pillar).
 * Pure data so shape invariants are unit-testable without MC bootstrap.
 */
public final class MoistureFarmLayout {
    private MoistureFarmLayout() {}

    public static final int SIZE_X = 13;
    public static final int SIZE_Y = 8;
    public static final int SIZE_Z = 13;

    public enum Kind { DOME, FLOOR, CHEST, VAPORATOR, AIR }

    public record Placement(int dx, int dy, int dz, Kind kind) {}

    private static final int CENTER_X = 6;
    private static final int CENTER_Z = 6;
    /** Squared-distance band of the dome shell: R^2 - R .. R^2 + R for R = 4. */
    private static final int SHELL_INNER_SQ = 12;
    private static final int SHELL_OUTER_SQ = 20;

    /** Vaporator plot corners, all well outside the dome shell. */
    private static final int[][] VAPORATORS = { {1, 1}, {11, 1}, {1, 11}, {11, 11} };

    public static List<Placement> placements() {
        List<Placement> out = new ArrayList<>();

        // Sandstone floor disc (y0) under the dome footprint.
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                int flat = sq(x - CENTER_X) + sq(z - CENTER_Z);
                if (flat <= SHELL_OUTER_SQ) {
                    out.add(new Placement(x, 0, z, Kind.FLOOR));
                }
            }
        }

        // Hollow hemispherical dome shell (y1+) centered on the floor center,
        // with a doorway carved through the south (low-z) side of the shell.
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                for (int y = 1; y < SIZE_Y; y++) {
                    int d2 = sq(x - CENTER_X) + y * y + sq(z - CENTER_Z);
                    boolean door = z < CENTER_Z && x >= 5 && x <= 7 && y <= 2;  // south doorway
                    if (d2 >= SHELL_INNER_SQ && d2 <= SHELL_OUTER_SQ) {
                        if (!door) {                        // door gap through the shell
                            out.add(new Placement(x, y, z, Kind.DOME));
                        }
                    } else if (d2 < SHELL_INNER_SQ) {
                        out.add(new Placement(x, y, z, Kind.AIR));
                    }
                }
            }
        }

        // Loot chest against the north interior of the dome (interior air
        // above, so the lid can open).
        out.removeIf(p -> p.dx() == 6 && p.dy() == 1 && p.dz() == 8);
        out.add(new Placement(6, 1, 8, Kind.CHEST));

        // Vaporators: sandstone pad + marker (the piece builds each marker into
        // a 3-tall pillar: two wall blocks topped with a lightning rod).
        for (int[] v : VAPORATORS) {
            out.add(new Placement(v[0], 0, v[1], Kind.FLOOR));
            out.add(new Placement(v[0], 1, v[1], Kind.VAPORATOR));
        }

        return out;
    }

    private static int sq(int v) {
        return v * v;
    }
}
