package com.tweeks.starwars.world;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ewok treetop village (Endor): a 28x14x28 cluster of five stilt-raised spruce
 * huts standing seven blocks off the forest floor, their conical thatch roofs
 * built as shrinking stacked hay layers, linked by plank rope-bridge walkways
 * with fence rails and ringed around a central ground-level bonfire (a campfire
 * on a stone-brick base). Log ladders climb from the forest floor up to the
 * bridge landings. The chief's hut (the seven-wide front hut) holds the loot
 * chest and an Ewok; three more Ewoks keep the other decks. Pure data so shape
 * invariants are unit-testable without MC bootstrap.
 */
public final class EwokVillageLayout {
    private EwokVillageLayout() {}

    public static final int SIZE_X = 28;
    public static final int SIZE_Y = 14;
    public static final int SIZE_Z = 28;

    public enum Kind {
        STILT, FLOOR, WALL, ROOF, BRIDGE, RAIL, LADDER, BONFIRE, CHEST, EWOK, AIR
    }

    public record Placement(int dx, int dy, int dz, Kind kind) {}

    /** Platform deck height: huts stand on six-block stilts with the floor at y7. */
    private static final int PLATFORM_Y = 7;
    private static final int WALL_TOP_Y = 10;                // walls y8..10
    private static final int ROOF_Y = 11;                    // roof layers y11..13

    /** The clearing the huts ring, and the bonfire that lights it. */
    private static final int CENTER_X = 14;
    private static final int CENTER_Z = 14;

    public static List<Placement> placements() {
        Map<Long, Placement> out = new LinkedHashMap<>();

        // Five huts on stilts. The chief's hut (front-north) is a 7x7 deck
        // holding the chest; the others are 6x6.
        hut(out, 11, 17, 2, 8);      // chief hut (north-center)
        hut(out, 2, 7, 11, 16);      // west hut
        hut(out, 20, 25, 11, 16);    // east hut
        hut(out, 11, 16, 20, 25);    // south hut
        hut(out, 19, 24, 19, 24);    // south-east hut

        // Central bonfire on the forest floor, framed by the huts (the piece
        // lays a stone-brick base under the campfire).
        put(out, CENTER_X, 1, CENTER_Z, Kind.BONFIRE);

        // Rope-bridge walkways radiating from the clearing toward the primary
        // huts: a plank strip at deck height with fence rails on both flanks.
        bridgeZ(out, 14, 9, 10);     // toward the chief hut (north)
        bridgeX(out, 8, 10, 14);     // toward the west hut
        bridgeX(out, 17, 19, 14);    // toward the east hut
        bridgeZ(out, 14, 17, 19);    // toward the south hut

        // Log ladders down from the bridge landings to the forest floor, so a
        // climber steps straight off the ladder onto the walkway.
        ladder(out, 14, 9);
        ladder(out, 8, 14);
        ladder(out, 19, 14);
        ladder(out, 14, 17);

        // The chief's loot chest and the village Ewoks stand on the decks (one
        // block above the plank floor, interior air above so the lid opens).
        put(out, 13, PLATFORM_Y + 1, 5, Kind.CHEST);
        put(out, 15, PLATFORM_Y + 1, 5, Kind.EWOK);   // chief's hut
        put(out, 4, PLATFORM_Y + 1, 13, Kind.EWOK);   // west hut
        put(out, 22, PLATFORM_Y + 1, 13, Kind.EWOK);  // east hut
        put(out, 13, PLATFORM_Y + 1, 22, Kind.EWOK);  // south hut

        return List.copyOf(out.values());
    }

    /**
     * One stilt hut: four corner log stilts from the floor up to the deck, a
     * plank deck, plank perimeter walls (three tall) with a doorway facing the
     * clearing, interior air, and a conical thatch roof of shrinking hay layers.
     */
    private static void hut(Map<Long, Placement> out, int minX, int maxX, int minZ, int maxZ) {
        // Corner stilts down to the forest floor.
        int[][] corners = { {minX, minZ}, {maxX, minZ}, {minX, maxZ}, {maxX, maxZ} };
        for (int[] c : corners) {
            for (int y = 1; y < PLATFORM_Y; y++) {
                put(out, c[0], y, c[1], Kind.STILT);
            }
        }

        // Plank deck.
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                put(out, x, PLATFORM_Y, z, Kind.FLOOR);
            }
        }

        // Doorway faces the village clearing: the wall on the side nearest the
        // center, a single 2-tall opening.
        int hcx = (minX + maxX) / 2;
        int hcz = (minZ + maxZ) / 2;
        boolean doorOnX = Math.abs(hcx - CENTER_X) >= Math.abs(hcz - CENTER_Z);
        int doorX = doorOnX ? (hcx > CENTER_X ? minX : maxX) : hcx;
        int doorZ = doorOnX ? hcz : (hcz > CENTER_Z ? minZ : maxZ);

        // Walls (with the doorway) and interior air.
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean perimeter = x == minX || x == maxX || z == minZ || z == maxZ;
                for (int y = PLATFORM_Y + 1; y <= WALL_TOP_Y; y++) {
                    boolean door = x == doorX && z == doorZ && y <= PLATFORM_Y + 2;
                    if (!perimeter || door) {
                        put(out, x, y, z, Kind.AIR);
                    } else {
                        put(out, x, y, z, Kind.WALL);
                    }
                }
            }
        }

        // Conical thatch roof: stacked full-block hay layers, each inset one
        // block per side until it caps out or runs out of vertical room.
        for (int layer = 0; ; layer++) {
            int rx0 = minX + layer, rx1 = maxX - layer;
            int rz0 = minZ + layer, rz1 = maxZ - layer;
            int y = ROOF_Y + layer;
            if (rx0 > rx1 || rz0 > rz1 || y >= SIZE_Y) break;
            for (int x = rx0; x <= rx1; x++) {
                for (int z = rz0; z <= rz1; z++) {
                    put(out, x, y, z, Kind.ROOF);
                }
            }
        }
    }

    /** Plank walkway running in Z at column x, with fence rails on both flanks. */
    private static void bridgeZ(Map<Long, Placement> out, int x, int z0, int z1) {
        for (int z = z0; z <= z1; z++) {
            put(out, x, PLATFORM_Y, z, Kind.BRIDGE);
            put(out, x - 1, PLATFORM_Y + 1, z, Kind.RAIL);
            put(out, x + 1, PLATFORM_Y + 1, z, Kind.RAIL);
        }
    }

    /** Plank walkway running in X at row z, with fence rails on both flanks. */
    private static void bridgeX(Map<Long, Placement> out, int x0, int x1, int z) {
        for (int x = x0; x <= x1; x++) {
            put(out, x, PLATFORM_Y, z, Kind.BRIDGE);
            put(out, x, PLATFORM_Y + 1, z - 1, Kind.RAIL);
            put(out, x, PLATFORM_Y + 1, z + 1, Kind.RAIL);
        }
    }

    /** Log ladder from the forest floor up to deck height. */
    private static void ladder(Map<Long, Placement> out, int x, int z) {
        for (int y = 1; y < PLATFORM_Y; y++) {
            put(out, x, y, z, Kind.LADDER);
        }
    }

    /** Last write wins, mirroring the removeIf-then-add convention. */
    private static void put(Map<Long, Placement> out, int x, int y, int z, Kind kind) {
        out.put(((long) x << 40) | ((long) (y + 8) << 20) | z, new Placement(x, y, z, kind));
    }
}
