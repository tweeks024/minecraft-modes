package com.tweeks.starwars.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Jedi ruin: a 9x5x9 ruined rotunda — a circular mossy-stone-brick floor (the
 * four far corners eroded away), six broken stone-brick pillars of deterministic
 * varied heights ringing the floor (each pillar's top cube cracked), a central
 * raised 3x1x3 chiseled-stone-brick dais, one loot chest on the dais, and two
 * Jedi guardian markers flanking it (spawned as living entities at generation
 * time). Pure data so shape invariants are unit-testable without MC bootstrap.
 */
public final class JediRuinLayout {
    private JediRuinLayout() {}

    public static final int SIZE_X = 9;
    public static final int SIZE_Y = 5;
    public static final int SIZE_Z = 9;

    public enum Kind { FLOOR, PILLAR, PILLAR_CRACKED, DAIS, CHEST, GUARDIAN_JEDI }

    public record Placement(int dx, int dy, int dz, Kind kind) {}

    private static final int CENTER_X = 4;
    private static final int CENTER_Z = 4;

    private static final int[][] PILLARS = { {1, 1}, {1, 7}, {4, 0}, {4, 8}, {7, 1}, {7, 7} };
    private static final int[] PILLAR_HEIGHTS = { 3, 2, 4, 1, 3, 2 };

    public static List<Placement> placements() {
        List<Placement> out = new ArrayList<>();

        // Circular mossy-stone-brick floor (y0): keep cells within manhattan
        // distance 6 of center (4,4), eroding the four far corners.
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                if (Math.abs(x - CENTER_X) + Math.abs(z - CENTER_Z) > 6) continue;
                out.add(new Placement(x, 0, z, Kind.FLOOR));
            }
        }

        // Six broken pillars (y1..height). Each pillar's top cube is cracked.
        for (int i = 0; i < PILLARS.length; i++) {
            int px = PILLARS[i][0];
            int pz = PILLARS[i][1];
            int h = PILLAR_HEIGHTS[i];
            for (int y = 1; y <= h; y++) {
                Kind kind = (y == h) ? Kind.PILLAR_CRACKED : Kind.PILLAR;
                out.add(new Placement(px, y, pz, kind));
            }
        }

        // Central raised 3x1x3 chiseled-stone-brick dais (y1), centered on (4,4).
        for (int x = CENTER_X - 1; x <= CENTER_X + 1; x++) {
            for (int z = CENTER_Z - 1; z <= CENTER_Z + 1; z++) {
                out.add(new Placement(x, 1, z, Kind.DAIS));
            }
        }

        // Loot chest on the dais center (y2).
        out.add(new Placement(CENTER_X, 2, CENTER_Z, Kind.CHEST));

        // Two Jedi guardian markers (spawned as living entities at generation time).
        out.add(new Placement(2, 1, 4, Kind.GUARDIAN_JEDI));
        out.add(new Placement(6, 1, 4, Kind.GUARDIAN_JEDI));

        return out;
    }
}
