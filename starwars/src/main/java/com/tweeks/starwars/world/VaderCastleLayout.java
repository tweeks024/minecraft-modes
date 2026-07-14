package com.tweeks.starwars.world;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vader's castle (Mustafar): a 20x28x20 black fortress on the lava plains. A
 * blackstone keep with a gated south front rings a raised throne dais of crying
 * obsidian; two tall basalt towers rise from the back of the keep to y25 with an
 * open central gap between them, giving the fortress its iconic tuning-fork
 * silhouette that frames the throne against the sky. Braziers (fire on
 * netherrack) and magma-block veins throw a lava glow across the
 * polished-blackstone floor. Two stormtroopers hold the gate and one flanks the
 * throne; a single Vader marker sits at the throne but is NOT spawned by the
 * piece — Vader is a singleton owned by {@code NamedCharacterSpawner}, so the
 * piece renders the marker as air. Pure data so shape invariants are
 * unit-testable without MC bootstrap.
 */
public final class VaderCastleLayout {
    private VaderCastleLayout() {}

    public static final int SIZE_X = 20;
    public static final int SIZE_Y = 28;
    public static final int SIZE_Z = 20;

    public enum Kind {
        WALL, PILLAR, FLOOR, GATE_AIR, BRAZIER, THRONE, MAGMA,
        CHEST, STORMTROOPER, VADER_SPAWN, AIR
    }

    public record Placement(int dx, int dy, int dz, Kind kind) {}

    private static final int KEEP_MIN = 3;
    private static final int KEEP_MAX = 16;
    private static final int WALL_TOP_Y = 7;                 // keep walls y1..7
    private static final int ROOF_Y = 8;

    /** Twin basalt towers: footprints (X) and their shared depth (Z) and height. */
    private static final int TOWER_TOP_Y = 25;               // towers y1..25 (~24 tall)
    private static final int TOWER_MIN_Z = 11;
    private static final int TOWER_MAX_Z = 14;
    private static final int LTOWER_MIN_X = 4, LTOWER_MAX_X = 6;
    private static final int RTOWER_MIN_X = 13, RTOWER_MAX_X = 15;
    /** Open gap between the towers (the fork), where the throne sits. */
    private static final int GAP_MIN_X = 7, GAP_MAX_X = 12;

    public static List<Placement> placements() {
        Map<Long, Placement> out = new LinkedHashMap<>();

        keep(out);
        towers(out);
        throneRoom(out);
        accents(out);

        return List.copyOf(out.values());
    }

    /**
     * Polished-blackstone floor, blackstone perimeter walls with a south gate,
     * interior air, and a flat roof left open over the throne gap so the towers
     * frame the throne against the sky.
     */
    private static void keep(Map<Long, Placement> out) {
        for (int x = KEEP_MIN; x <= KEEP_MAX; x++) {
            for (int z = KEEP_MIN; z <= KEEP_MAX; z++) {
                put(out, x, 0, z, Kind.FLOOR);
                boolean perimeter = x == KEEP_MIN || x == KEEP_MAX || z == KEEP_MIN || z == KEEP_MAX;
                for (int y = 1; y <= WALL_TOP_Y; y++) {
                    boolean gate = z == KEEP_MIN && x >= 8 && x <= 11 && y <= 4;
                    if (!perimeter) {
                        put(out, x, y, z, Kind.AIR);
                    } else if (gate) {
                        put(out, x, y, z, Kind.GATE_AIR);
                    } else {
                        put(out, x, y, z, Kind.WALL);
                    }
                }
                boolean openGap = x >= GAP_MIN_X && x <= GAP_MAX_X && z >= TOWER_MIN_Z;
                if (!openGap) {
                    put(out, x, ROOF_Y, z, Kind.WALL);       // flat roof
                }
            }
        }
    }

    /** Twin solid basalt towers rising from the keep floor to y25. */
    private static void towers(Map<Long, Placement> out) {
        for (int y = 1; y <= TOWER_TOP_Y; y++) {
            for (int x = LTOWER_MIN_X; x <= LTOWER_MAX_X; x++) {
                for (int z = TOWER_MIN_Z; z <= TOWER_MAX_Z; z++) {
                    put(out, x, y, z, Kind.PILLAR);
                }
            }
            for (int x = RTOWER_MIN_X; x <= RTOWER_MAX_X; x++) {
                for (int z = TOWER_MIN_Z; z <= TOWER_MAX_Z; z++) {
                    put(out, x, y, z, Kind.PILLAR);
                }
            }
        }
    }

    /**
     * The inner chamber: a raised polished-blackstone dais in the gap between
     * the towers, a crying-obsidian throne on it, the throne-room chest beside
     * it, the (unspawned) Vader marker on the seat, and the honor guard.
     */
    private static void throneRoom(Map<Long, Placement> out) {
        // Raised dais at the back, in the gap between the towers.
        for (int x = 8; x <= 11; x++) {
            for (int z = 12; z <= 15; z++) {
                put(out, x, 1, z, Kind.FLOOR);
            }
        }
        // Crying-obsidian throne seat, centered on the dais back.
        put(out, 9, 2, 14, Kind.THRONE);
        put(out, 10, 2, 14, Kind.THRONE);
        // Vader's seat marker (rendered as air by the piece — see class doc).
        put(out, 9, 3, 14, Kind.VADER_SPAWN);
        // Throne-room loot chest beside the throne (air above so the lid opens).
        put(out, 11, 2, 14, Kind.CHEST);
        // Honor guard: one trooper on the dais, two holding the gate.
        put(out, 8, 2, 13, Kind.STORMTROOPER);
        put(out, 8, 1, 4, Kind.STORMTROOPER);
        put(out, 11, 1, 4, Kind.STORMTROOPER);
    }

    /** Braziers (fire on netherrack) and magma-block veins for the lava glow. */
    private static void accents(Map<Long, Placement> out) {
        put(out, 5, 1, 4, Kind.BRAZIER);        // gate flanks
        put(out, 14, 1, 4, Kind.BRAZIER);
        put(out, 8, 2, 15, Kind.BRAZIER);       // dais back corners
        put(out, 11, 2, 15, Kind.BRAZIER);

        int[][] magma = { {4, 4}, {15, 4}, {4, 15}, {15, 15}, {9, 9}, {10, 9} };
        for (int[] m : magma) {
            put(out, m[0], 0, m[1], Kind.MAGMA);
        }
    }

    /** Last write wins, mirroring the removeIf-then-add convention. */
    private static void put(Map<Long, Placement> out, int x, int y, int z, Kind kind) {
        out.put(((long) x << 40) | ((long) (y + 8) << 20) | z, new Placement(x, y, z, kind));
    }
}
