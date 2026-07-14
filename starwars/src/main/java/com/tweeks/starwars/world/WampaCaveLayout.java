package com.tweeks.starwars.world;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wampa cave (Hoth): an 11x7x13 packed-ice mound — stacked shrinking ellipse
 * layers hollowed into a lair, a jagged low-z entrance flanked by stepped
 * blue-ice spikes, bone blocks scattered across the lair floor plus a small
 * skeleton tableau of arranged remains, and one wampa marker deep inside.
 * No chest. Pure data so shape invariants are unit-testable without MC
 * bootstrap.
 */
public final class WampaCaveLayout {
    private WampaCaveLayout() {}

    public static final int SIZE_X = 11;
    public static final int SIZE_Y = 7;
    public static final int SIZE_Z = 13;

    public enum Kind { SHELL, SPIKE, BONE, AIR, WAMPA }

    public record Placement(int dx, int dy, int dz, Kind kind) {}

    private static final int CENTER_X = 5;
    private static final int CENTER_Z = 6;

    /** Mound ellipse radii per layer (x, z), tapering to a crown. */
    private static final int[] MOUND_RX = { 5, 5, 4, 3, 2, 1 };
    private static final int[] MOUND_RZ = { 6, 6, 5, 4, 3, 1 };

    /** Lair cavity ellipse radii per layer. */
    private static final int[] CAVE_RX = { 3, 3, 2, 1 };
    private static final int[] CAVE_RZ = { 4, 4, 3, 2 };

    /** Stepped blue-ice spikes flanking the entrance: {x, y, z}. */
    private static final int[][] SPIKES = {
        {3, 0, 1}, {3, 1, 1}, {7, 0, 1}, {7, 1, 1}, {2, 0, 2}, {8, 0, 2},
    };

    /** Loose bones scattered across the lair floor. */
    private static final int[][] BONE_SCATTER = { {4, 0, 4}, {6, 0, 6}, {6, 0, 8} };

    /** The skeleton tableau: remains arranged against the back wall. */
    private static final int[][] BONE_TABLEAU = {
        {3, 0, 7}, {4, 0, 7}, {3, 0, 8}, {4, 0, 8}, {4, 1, 8},
    };

    public static List<Placement> placements() {
        Map<Long, Placement> out = new LinkedHashMap<>();

        // Solid ice mound layers, hollowed by the lair cavity.
        for (int y = 0; y < MOUND_RX.length; y++) {
            for (int x = 0; x < SIZE_X; x++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    if (!inEllipse(x, z, MOUND_RX[y], MOUND_RZ[y])) continue;
                    boolean cave = y < CAVE_RX.length && inEllipse(x, z, CAVE_RX[y], CAVE_RZ[y]);
                    put(out, x, y, z, cave ? Kind.AIR : Kind.SHELL);
                }
            }
        }

        // Jagged entrance: a 3-wide, 2-tall tunnel carved through the low-z
        // shell into the cavity (the shell arch above stays, so the mouth
        // reads as a toothy overhang with the spikes below).
        for (int x = CENTER_X - 1; x <= CENTER_X + 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 2; z++) {
                    put(out, x, y, z, Kind.AIR);
                }
            }
        }

        // Stepped blue-ice spikes at the mouth.
        for (int[] s : SPIKES) {
            put(out, s[0], s[1], s[2], Kind.SPIKE);
        }

        // Bones: loose scatter plus the arranged remains of an earlier meal.
        for (int[] b : BONE_SCATTER) {
            put(out, b[0], b[1], b[2], Kind.BONE);
        }
        for (int[] b : BONE_TABLEAU) {
            put(out, b[0], b[1], b[2], Kind.BONE);
        }

        // The wampa, deep in the lair (far end from the entrance).
        put(out, CENTER_X, 0, 9, Kind.WAMPA);

        return List.copyOf(out.values());
    }

    /** Integer ellipse-membership test around the mound center. */
    private static boolean inEllipse(int x, int z, int rx, int rz) {
        int dx = x - CENTER_X;
        int dz = z - CENTER_Z;
        return dx * dx * rz * rz + dz * dz * rx * rx <= rx * rx * rz * rz;
    }

    /** Last write wins, mirroring the removeIf-then-add convention. */
    private static void put(Map<Long, Placement> out, int x, int y, int z, Kind kind) {
        out.put(((long) x << 40) | ((long) (y + 8) << 20) | z, new Placement(x, y, z, kind));
    }
}
