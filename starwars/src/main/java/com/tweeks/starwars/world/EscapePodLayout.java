package com.tweeks.starwars.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Crashed escape pod: a 5x4x5 rounded shell with a torn-open front (the
 * "crash damage" gap at z=0, x=1..3), floor, one loot chest, interior air.
 * Pure data so shape invariants are unit-testable without MC bootstrap.
 */
public final class EscapePodLayout {
    private EscapePodLayout() {}

    public static final int SIZE_X = 5;
    public static final int SIZE_Y = 4;
    public static final int SIZE_Z = 5;

    public enum Kind { SHELL, FLOOR, CHEST, AIR, ASTROMECH }

    public record Placement(int dx, int dy, int dz, Kind kind) {}

    public static List<Placement> placements() {
        List<Placement> out = new ArrayList<>();
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                boolean corner = (x == 0 || x == SIZE_X - 1) && (z == 0 || z == SIZE_Z - 1);
                if (corner) continue;                       // rounded corners
                out.add(new Placement(x, 0, z, Kind.FLOOR));
                boolean edge = x == 0 || x == SIZE_X - 1 || z == 0 || z == SIZE_Z - 1;
                for (int y = 1; y < SIZE_Y; y++) {
                    boolean roof = y == SIZE_Y - 1;
                    boolean tornFront = z == 0 && x >= 1 && x <= 3 && y <= 2;   // symmetric crash opening
                    if (tornFront) continue;                // crash opening
                    if (edge || roof) {
                        out.add(new Placement(x, y, z, Kind.SHELL));
                    } else {
                        out.add(new Placement(x, y, z, Kind.AIR));
                    }
                }
            }
        }
        // Loot chest against the back wall interior.
        out.removeIf(p -> p.dx() == 2 && p.dy() == 1 && p.dz() == 3);
        out.add(new Placement(2, 1, 3, Kind.CHEST));
        // Astromech marker: interior, near the torn front — the droid that
        // "spawns nearby" the crash per spec.
        out.removeIf(p -> p.dx() == 2 && p.dy() == 1 && p.dz() == 1);
        out.add(new Placement(2, 1, 1, Kind.ASTROMECH));
        return out;
    }
}
