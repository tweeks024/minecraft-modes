package com.tweeks.starwars.world;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mos Eisley spaceport ("a wretched hive of scum and villainy"): a 48x14x48
 * walled-corner desert town — two crossing sand streets, a south town wall
 * with a gated checkpoint (two stormtrooper markers), seven sandstone
 * dome-and-box buildings, four GX-8 vaporator markers, six street-lamp
 * markers, Docking Bay 94 (a smooth-stone pad ring with an astromech marker),
 * three jawa markers in the streets, and THE CANTINA centerpiece: a stepped
 * dome with a recessed entrance, bar counter, jukebox + record chest, four
 * table markers, booth seats, and dim lantern lighting. A second loot chest
 * sits in the dockyard office. Pure data so shape invariants are
 * unit-testable without MC bootstrap.
 */
public final class MosEisleyLayout {
    private MosEisleyLayout() {}

    public static final int SIZE_X = 48;
    public static final int SIZE_Y = 14;
    public static final int SIZE_Z = 48;

    public enum Kind {
        STREET, FLOOR, WALL, DOME, ROOF, WINDOW, DOOR_AIR, AIR,
        LAMP, VAPORATOR, BAR, SEAT, TABLE, JUKEBOX, LANTERN,
        PAD, PAD_WALL, CHEST_CANTINA, CHEST_DOCKING,
        JAWA, STORMTROOPER, ASTROMECH
    }

    public record Placement(int dx, int dy, int dz, Kind kind) {}

    // Crossing streets: north-south x 21..26, east-west z 21..26.
    private static final int STREET_MIN = 21;
    private static final int STREET_MAX = 26;

    // Cantina footprint (the town centerpiece, ~14x7x12).
    private static final int CANTINA_MIN_X = 5;
    private static final int CANTINA_MAX_X = 18;
    private static final int CANTINA_MIN_Z = 28;
    private static final int CANTINA_MAX_Z = 39;

    // Docking Bay 94: smooth-stone pad circle with a low ring wall.
    private static final int PAD_CENTER_X = 37;
    private static final int PAD_CENTER_Z = 37;
    private static final int PAD_SQ = 30;        // pad disc: d^2 <= 30 (~12-diameter)
    private static final int RING_SQ = 40;       // ring wall band: 30 < d^2 <= 40

    /** Vaporator plots (pad at y0, marker at y1), all on open ground. */
    private static final int[][] VAPORATORS = { {2, 18}, {18, 2}, {45, 28}, {2, 42} };

    /** Street lamps (pad at y0, marker at y1) at street corners and lanes. */
    private static final int[][] LAMPS = { {20, 20}, {27, 20}, {20, 27}, {34, 27}, {27, 8}, {20, 40} };

    public static List<Placement> placements() {
        Map<Long, Placement> out = new LinkedHashMap<>();

        streets(out);
        townWall(out);
        boxBuilding(out, 9, 15, 5, 11, 12, 11, 12, 5);       // box A (SW quadrant)
        boxBuilding(out, 6, 12, 42, 46, 9, 42, 9, 46);       // box C (north of cantina)
        domeBuilding(out, 33, 10);                           // dome D (SE quadrant)
        boxBuilding(out, 40, 46, 7, 13, 40, 10, 43, 13);     // box E (east side)
        boxBuilding(out, 40, 46, 15, 20, 43, 20, 40, 17);    // box F (east side, at street)
        dockOffice(out);
        dockingBay(out);
        cantina(out);

        // Vaporators and street lamps: sandstone pad + marker (the piece
        // builds the vaporator into a 3-tall condenser mast and the lamp into
        // a torch-topped sandstone post).
        for (int[] v : VAPORATORS) {
            put(out, v[0], 0, v[1], Kind.FLOOR);
            put(out, v[0], 1, v[1], Kind.VAPORATOR);
        }
        for (int[] l : LAMPS) {
            put(out, l[0], 0, l[1], Kind.FLOOR);
            put(out, l[0], 1, l[1], Kind.LAMP);
            remove(out, l[0], 2, l[1]);                      // keep the post clear of street air
        }

        // Street life: jawas wandering the streets, a stormtrooper checkpoint
        // just inside the main gate, an astromech on the docking pad.
        put(out, 23, 1, 10, Kind.JAWA);
        put(out, 10, 1, 23, Kind.JAWA);
        put(out, 23, 1, 33, Kind.JAWA);
        put(out, 22, 1, 2, Kind.STORMTROOPER);
        put(out, 25, 1, 2, Kind.STORMTROOPER);
        put(out, PAD_CENTER_X, 1, PAD_CENTER_Z, Kind.ASTROMECH);

        return List.copyOf(out.values());
    }

    /** Two crossing sand streets, carved 2 high so lanes stay walkable. */
    private static void streets(Map<Long, Placement> out) {
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                boolean ns = x >= STREET_MIN && x <= STREET_MAX;
                boolean ew = z >= STREET_MIN && z <= STREET_MAX;
                if (!ns && !ew) continue;
                put(out, x, 0, z, Kind.STREET);
                put(out, x, 1, z, Kind.AIR);
                put(out, x, 2, z, Kind.AIR);
            }
        }
    }

    /** South town wall with the main gate, plus short corner returns. */
    private static void townWall(Map<Long, Placement> out) {
        for (int x = 4; x <= 43; x++) {
            boolean gate = x >= STREET_MIN && x <= STREET_MAX;
            for (int y = 1; y <= 4; y++) {
                if (gate && y <= 3) {
                    put(out, x, y, 0, Kind.DOOR_AIR);        // gate opening
                } else {
                    put(out, x, y, 0, Kind.WALL);            // wall / gate lintel
                }
            }
        }
        for (int z = 1; z <= 4; z++) {                       // walled corners
            for (int y = 1; y <= 4; y++) {
                put(out, 4, y, z, Kind.WALL);
                put(out, 43, y, z, Kind.WALL);
            }
        }
    }

    /** Flat-roofed box building with one 2-tall door and one window. */
    private static void boxBuilding(Map<Long, Placement> out, int minX, int maxX,
                                    int minZ, int maxZ, int doorX, int doorZ,
                                    int windowX, int windowZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                put(out, x, 0, z, Kind.FLOOR);
                boolean perimeter = x == minX || x == maxX || z == minZ || z == maxZ;
                for (int y = 1; y <= 3; y++) {
                    if (!perimeter) {
                        put(out, x, y, z, Kind.AIR);
                    } else if (x == doorX && z == doorZ && y <= 2) {
                        put(out, x, y, z, Kind.DOOR_AIR);
                    } else if (x == windowX && z == windowZ && y == 2) {
                        put(out, x, y, z, Kind.WINDOW);
                    } else {
                        put(out, x, y, z, Kind.WALL);
                    }
                }
                put(out, x, 4, z, Kind.ROOF);                // flat roof
            }
        }
    }

    /** Hemispherical dome dwelling (MoistureFarm-style shell, north door). */
    private static void domeBuilding(Map<Long, Placement> out, int cx, int cz) {
        for (int x = cx - 4; x <= cx + 4; x++) {
            for (int z = cz - 4; z <= cz + 4; z++) {
                int flat = sq(x - cx) + sq(z - cz);
                if (flat <= 20) {
                    put(out, x, 0, z, Kind.FLOOR);
                }
                for (int y = 1; y <= 4; y++) {
                    int d2 = flat + y * y;
                    boolean door = z > cz && x >= cx - 1 && x <= cx + 1 && y <= 2;
                    if (d2 >= 12 && d2 <= 20) {
                        if (!door) {
                            put(out, x, y, z, Kind.DOME);
                        }
                    } else if (d2 < 12) {
                        put(out, x, y, z, Kind.AIR);
                    }
                }
            }
        }
    }

    /** Dockyard office beside Docking Bay 94, holding the port loot chest. */
    private static void dockOffice(Map<Long, Placement> out) {
        boxBuilding(out, 27, 33, 27, 31, 27, 29, 29, 27);
        put(out, 31, 2, 27, Kind.WINDOW);                    // second street window
        put(out, 32, 1, 28, Kind.CHEST_DOCKING);
    }

    /** Docking Bay 94: smooth-stone pad disc + low ring wall, west opening. */
    private static void dockingBay(Map<Long, Placement> out) {
        for (int x = PAD_CENTER_X - 6; x <= PAD_CENTER_X + 6; x++) {
            for (int z = PAD_CENTER_Z - 6; z <= PAD_CENTER_Z + 6; z++) {
                int dx = x - PAD_CENTER_X;
                int dz = z - PAD_CENTER_Z;
                int d2 = sq(dx) + sq(dz);
                if (d2 <= PAD_SQ) {
                    put(out, x, 0, z, Kind.PAD);
                } else if (d2 <= RING_SQ) {
                    boolean opening = dx < 0 && Math.abs(dz) <= 2;   // faces the street
                    put(out, x, 0, z, Kind.PAD);
                    if (!opening) {
                        put(out, x, 1, z, Kind.PAD_WALL);
                    }
                }
            }
        }
    }

    /**
     * The cantina: a stepped dome (ziggurat rings at y4/y5/y6) over a 14x12
     * hall, recessed stepped entrance on the street side, bar counter with
     * jukebox + record chest behind it, four table clusters, booth seats
     * along the side walls, and dim lantern light.
     */
    private static void cantina(Map<Long, Placement> out) {
        for (int x = CANTINA_MIN_X; x <= CANTINA_MAX_X; x++) {
            for (int z = CANTINA_MIN_Z; z <= CANTINA_MAX_Z; z++) {
                put(out, x, 0, z, Kind.FLOOR);
                int inset = Math.min(Math.min(x - CANTINA_MIN_X, CANTINA_MAX_X - x),
                    Math.min(z - CANTINA_MIN_Z, CANTINA_MAX_Z - z));
                int domeY = 4 + Math.min(inset, 2);          // stepped dome ring height
                for (int y = 1; y < domeY; y++) {
                    if (inset == 0) {
                        boolean entrance = z == CANTINA_MIN_Z && x >= 10 && x <= 13 && y <= 3;
                        put(out, x, y, z, entrance ? Kind.DOOR_AIR : Kind.WALL);
                    } else {
                        put(out, x, y, z, Kind.AIR);
                    }
                }
                put(out, x, domeY, z, Kind.DOME);
            }
        }

        // Recessed entrance: an inner door wall one block back, plus a step
        // pad outside the opening.
        for (int x = 10; x <= 13; x++) {
            for (int y = 1; y <= 3; y++) {
                boolean gap = (x == 11 || x == 12) && y <= 2;
                put(out, x, y, CANTINA_MIN_Z + 1, gap ? Kind.DOOR_AIR : Kind.WALL);
            }
        }
        put(out, 11, 0, CANTINA_MIN_Z - 1, Kind.FLOOR);
        put(out, 12, 0, CANTINA_MIN_Z - 1, Kind.FLOOR);

        // Bar counter along the back, jukebox + record chest behind it.
        for (int x = 8; x <= 14; x++) {
            put(out, x, 1, 36, Kind.BAR);
        }
        put(out, 9, 1, 37, Kind.JUKEBOX);
        put(out, 10, 1, 37, Kind.CHEST_CANTINA);

        // Four table clusters (fence + pressure-plate builds); the cell above
        // each marker is cleared so the tabletop survives the interior air.
        int[][] tables = { {8, 31}, {14, 31}, {8, 34}, {14, 34} };
        for (int[] t : tables) {
            put(out, t[0], 1, t[1], Kind.TABLE);
            remove(out, t[0], 2, t[1]);
        }

        // Booth seats along the east and west walls.
        int[] seatZ = { 30, 31, 33, 34, 36, 37 };
        for (int z : seatZ) {
            put(out, CANTINA_MIN_X + 1, 1, z, Kind.SEAT);
            put(out, CANTINA_MAX_X - 1, 1, z, Kind.SEAT);
        }

        // Dim lighting: standing lanterns in the corners and on the bar.
        put(out, 7, 1, 30, Kind.LANTERN);
        put(out, 16, 1, 30, Kind.LANTERN);
        put(out, 7, 1, 37, Kind.LANTERN);
        put(out, 16, 1, 37, Kind.LANTERN);
        put(out, 8, 2, 36, Kind.LANTERN);
        put(out, 14, 2, 36, Kind.LANTERN);
    }

    /** Last write wins, mirroring the removeIf-then-add convention. */
    private static void put(Map<Long, Placement> out, int x, int y, int z, Kind kind) {
        out.put(key(x, y, z), new Placement(x, y, z, kind));
    }

    private static void remove(Map<Long, Placement> out, int x, int y, int z) {
        out.remove(key(x, y, z));
    }

    private static long key(int x, int y, int z) {
        return ((long) x << 40) | ((long) (y + 8) << 20) | z;
    }

    private static int sq(int v) {
        return v * v;
    }
}
