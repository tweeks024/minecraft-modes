package com.tweeks.starwars.world;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Echo Base (Hoth): a 26x9x20 rebel bunker dug into the snow — snow-block
 * walls veined with packed ice, an iron-framed 4-wide hangar opening on the
 * south face, a main hall with barracks bed pallets (white/red wool pairs)
 * along the west wall, a partition wall with a doorway into the command room
 * (loot chest + a redstone-cored, iron-cased power generator), four rebel
 * trooper markers and one astromech marker. Pure data so shape invariants
 * are unit-testable without MC bootstrap.
 */
public final class EchoBaseLayout {
    private EchoBaseLayout() {}

    public static final int SIZE_X = 26;
    public static final int SIZE_Y = 9;
    public static final int SIZE_Z = 20;

    public enum Kind {
        WALL, ICE, FLOOR, ROOF, FRAME, HANGAR_AIR, DOOR_AIR, AIR,
        BED_HEAD, BED_FOOT, GEN_CORE, GEN_CASE, CHEST, REBEL, ASTROMECH
    }

    public record Placement(int dx, int dy, int dz, Kind kind) {}

    private static final int WALL_TOP_Y = 5;                 // walls y1..5
    private static final int ROOF_Y = 6;
    /** Hangar opening on the south face: x 11..14, y 1..3. */
    private static final int HANGAR_MIN_X = 11;
    private static final int HANGAR_MAX_X = 14;
    private static final int HANGAR_TOP_Y = 3;
    /** Partition between the main hall and the command room. */
    private static final int PARTITION_X = 17;

    /** Barracks bed rows along the west wall (head x1, foot x2). */
    private static final int[] BED_Z = { 3, 6, 9, 12 };

    public static List<Placement> placements() {
        Map<Long, Placement> out = new LinkedHashMap<>();

        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                put(out, x, 0, z, Kind.FLOOR);
                boolean perimeter = x == 0 || x == SIZE_X - 1 || z == 0 || z == SIZE_Z - 1;
                for (int y = 1; y <= WALL_TOP_Y; y++) {
                    if (perimeter) {
                        put(out, x, y, z, mix(x, y, z));
                    } else if (x == PARTITION_X) {
                        boolean doorway = (z == 9 || z == 10) && y <= 2;
                        put(out, x, y, z, doorway ? Kind.DOOR_AIR : Kind.WALL);
                    } else {
                        put(out, x, y, z, Kind.AIR);
                    }
                }
                put(out, x, ROOF_Y, z, Kind.ROOF);           // buried flat roof
            }
        }

        // Hangar mouth: a 4-wide, 3-tall opening in the south wall, framed in
        // iron blocks (jambs and lintel).
        for (int x = HANGAR_MIN_X - 1; x <= HANGAR_MAX_X + 1; x++) {
            put(out, x, HANGAR_TOP_Y + 1, 0, Kind.FRAME);    // lintel
        }
        for (int y = 1; y <= HANGAR_TOP_Y; y++) {
            put(out, HANGAR_MIN_X - 1, y, 0, Kind.FRAME);    // west jamb
            put(out, HANGAR_MAX_X + 1, y, 0, Kind.FRAME);    // east jamb
            for (int x = HANGAR_MIN_X; x <= HANGAR_MAX_X; x++) {
                put(out, x, y, 0, Kind.HANGAR_AIR);          // the opening itself
            }
        }

        // Barracks: white/red wool bed pallets along the west wall.
        for (int z : BED_Z) {
            put(out, 1, 1, z, Kind.BED_HEAD);
            put(out, 2, 1, z, Kind.BED_FOOT);
        }

        // Power generator in the command room: redstone core, iron casing.
        put(out, 21, 1, 4, Kind.GEN_CORE);
        put(out, 20, 1, 4, Kind.GEN_CASE);
        put(out, 22, 1, 4, Kind.GEN_CASE);
        put(out, 21, 1, 3, Kind.GEN_CASE);
        put(out, 21, 1, 5, Kind.GEN_CASE);
        put(out, 21, 2, 4, Kind.GEN_CASE);

        // Command-room loot chest (interior air above, so the lid can open).
        put(out, 23, 1, 16, Kind.CHEST);

        // The garrison: four rebel troopers and an astromech in the hall.
        put(out, 6, 1, 4, Kind.REBEL);
        put(out, 6, 1, 15, Kind.REBEL);
        put(out, 12, 1, 10, Kind.REBEL);
        put(out, 20, 1, 10, Kind.REBEL);
        put(out, 10, 1, 6, Kind.ASTROMECH);

        return List.copyOf(out.values());
    }

    /** Deterministic snow/packed-ice vein mix for the bunker shell. */
    private static Kind mix(int x, int y, int z) {
        return (x + y + z) % 4 == 0 ? Kind.ICE : Kind.WALL;
    }

    /** Last write wins, mirroring the removeIf-then-add convention. */
    private static void put(Map<Long, Placement> out, int x, int y, int z, Kind kind) {
        out.put(((long) x << 40) | ((long) (y + 8) << 20) | z, new Placement(x, y, z, kind));
    }
}
