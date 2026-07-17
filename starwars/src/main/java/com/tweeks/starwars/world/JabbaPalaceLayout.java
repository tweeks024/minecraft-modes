package com.tweeks.starwars.world;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Jabba's Palace (Tatooine): a 19x17x21 domed sandstone fortress staged as
 * two levels, matching the movie's geography without ever digging below the
 * world surface (the whole box sits at/above ground, like every other
 * scattered-feature structure here).
 *
 * <p>Front to back (low z = the gated front):
 * <ul>
 *   <li><b>Ground level</b> — a tall <i>entry hall</i> (front) with a grand
 *       staircase climbing to the throne floor, walled off from the
 *       <i>rancor pit</i> that fills the rest of the ground floor: a
 *       stone-brick arena with bones, the rancor, and a treasure pedestal at
 *       the back beneath the dais.</li>
 *   <li><b>Upper level</b> — the <i>throne room</i> laid over the pit, its
 *       floor pierced by an iron-bar grate (with a drop-chute) so Jabba's
 *       court looks straight down onto the beast. Jabba holds court on a
 *       raised dais at the back, directly above his hoard.</li>
 *   <li><b>Roof</b> — a rounded sandstone dome, lit at the apex.</li>
 * </ul>
 *
 * <p>Pure data (no MC types) so every shape invariant — bounds, a single
 * rancor and single Jabba marker with headroom, a reachable chest, a
 * grate over the pit — is unit-testable without an engine bootstrap.
 */
public final class JabbaPalaceLayout {
    private JabbaPalaceLayout() {}

    public static final int SIZE_X = 19;
    public static final int SIZE_Y = 17;
    public static final int SIZE_Z = 21;

    public enum Kind {
        WALL,       // sandstone outer shell
        SMOOTH,     // smooth-sandstone floors and slabs
        CUT,        // cut-sandstone dome
        PILLAR,     // chiseled-sandstone columns / throne backing
        PIT_WALL,   // stone-brick lining of the arena
        GRATE,      // iron bars: the throne-floor grate over the pit
        CARPET,     // orange carpet runner
        DAIS,       // cut red sandstone: Jabba's raised platform
        TORCH,      // standing torch
        LANTERN,    // apex light
        BONE,       // bones littering the pit floor
        CHEST,      // the treasure
        RANCOR,     // beast spawn marker (carved to air)
        JABBA,      // Jabba spawn marker (carved to air)
        GATE_AIR,   // doorway / gateway openings
        AIR         // carved interior
    }

    public record Placement(int dx, int dy, int dz, Kind kind) {}

    public static final int CENTER_X = 9;

    // Vertical planes.
    private static final int GROUND = 1;        // ground-floor slab
    private static final int GROUND_CEIL = 6;   // top of the ground-floor rooms
    private static final int UPPER = 7;          // throne-room floor
    private static final int WALL_TOP = 11;      // where the walls meet the dome
    private static final int DOME_BASE = 12;

    // Depth (z) planes.
    private static final int PIT_FRONT_WALL = 7; // divides entry hall from pit
    private static final int PIT_BACK = 19;      // last interior z of the pit
    private static final int BACK_WALL = 20;

    // The throne-floor grate over the pit, with a central drop-chute.
    private static final int GRATE_X0 = 6, GRATE_X1 = 12;
    private static final int GRATE_Z0 = 11, GRATE_Z1 = 15;
    private static final int CHUTE_X0 = 8, CHUTE_X1 = 9;
    private static final int CHUTE_Z0 = 12, CHUTE_Z1 = 13;

    // Dome ring radii per level, centered on (CENTER_X, DOME_CZ).
    private static final int DOME_CZ = 10;
    private static final int[] DOME_RX = {9, 8, 6, 4, 2};
    private static final int[] DOME_RZ = {10, 9, 7, 5, 2};

    public static List<Placement> placements() {
        Map<Long, Placement> out = new LinkedHashMap<>();

        buildShellAndFloors(out);
        buildDome(out);
        carveInteriors(out);      // after the shell, before the fittings
        buildEntryStair(out);
        buildPitFittings(out);
        buildThroneRoom(out);
        decorate(out);

        return List.copyOf(out.values());
    }

    /** Outer sandstone walls, the ground slab, and the upper (throne) floor. */
    private static void buildShellAndFloors(Map<Long, Placement> out) {
        // Perimeter walls, ground up to the dome base.
        for (int y = GROUND; y <= WALL_TOP; y++) {
            for (int x = 0; x < SIZE_X; x++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    boolean edge = x == 0 || x == SIZE_X - 1 || z == 0 || z == BACK_WALL;
                    if (edge) put(out, x, y, z, Kind.WALL);
                }
            }
        }
        // Ground-floor slab across the interior.
        for (int x = 1; x < SIZE_X - 1; x++) {
            for (int z = 1; z <= PIT_BACK; z++) {
                put(out, x, GROUND, z, Kind.SMOOTH);
            }
        }
        // Upper (throne-room) floor across the interior, with the grate over
        // the pit and the stairwell mouth left open.
        for (int x = 1; x < SIZE_X - 1; x++) {
            for (int z = 1; z <= PIT_BACK; z++) {
                if (isStairMouth(x, z)) continue;      // stairs emerge here
                boolean grate = x >= GRATE_X0 && x <= GRATE_X1 && z >= GRATE_Z0 && z <= GRATE_Z1;
                boolean chute = x >= CHUTE_X0 && x <= CHUTE_X1 && z >= CHUTE_Z0 && z <= CHUTE_Z1;
                if (chute) continue;                    // open drop into the pit
                put(out, x, UPPER, z, grate ? Kind.GRATE : Kind.SMOOTH);
            }
        }
        // Front gateway: a 3-wide, 4-tall arch through the front wall.
        for (int x = CENTER_X - 1; x <= CENTER_X + 1; x++) {
            for (int y = GROUND; y <= GROUND + 3; y++) {
                put(out, x, y, 0, Kind.GATE_AIR);
            }
        }
        // Inner wall between the entry hall and the pit (ground level only).
        for (int x = 1; x < SIZE_X - 1; x++) {
            for (int y = GROUND; y <= GROUND_CEIL; y++) {
                put(out, x, y, PIT_FRONT_WALL, Kind.PIT_WALL);
            }
        }
    }

    /** A rounded sandstone dome capping the throne room. */
    private static void buildDome(Map<Long, Placement> out) {
        for (int x = 1; x < SIZE_X - 1; x++) {
            for (int z = 1; z <= PIT_BACK; z++) {
                int top = domeLevel(x, z);
                if (top < 0) continue;
                put(out, x, top, z, Kind.CUT);
            }
        }
    }

    /** Hollow out the two ground-floor rooms and the throne room. */
    private static void carveInteriors(Map<Long, Placement> out) {
        // Entry hall: open full height to the dome (a grand foyer).
        fillAir(out, 1, SIZE_X - 2, GROUND + 1, WALL_TOP, 1, PIT_FRONT_WALL - 1);
        // Rancor pit: open shaft from the floor up to the throne floor.
        fillAir(out, 1, SIZE_X - 2, GROUND + 1, UPPER - 1, PIT_FRONT_WALL + 1, PIT_BACK);
        // Throne room: above the upper floor, up to the dome.
        fillAir(out, 1, SIZE_X - 2, UPPER + 1, WALL_TOP, 1, PIT_BACK);
    }

    /** The grand staircase rising from the entry to the throne floor. */
    private static void buildEntryStair(Map<Long, Placement> out) {
        // Five-wide treads climbing one block per z, dz 1..6 -> dy 1..6, then
        // the dy7 mouth (isStairMouth) lets you step onto the throne floor.
        for (int z = 1; z <= 6; z++) {
            int topY = z;                       // tread surface height at this z
            for (int x = CENTER_X - 2; x <= CENTER_X + 2; x++) {
                for (int y = GROUND; y <= topY; y++) {
                    put(out, x, y, z, Kind.SMOOTH);
                }
            }
        }
    }

    /** Pit lining, bones, the treasure pedestal, the doorways, the beast. */
    private static void buildPitFittings(Map<Long, Placement> out) {
        // Stone-brick skirting around the arena base for a dungeon read.
        for (int z = PIT_FRONT_WALL + 1; z <= PIT_BACK; z++) {
            put(out, 1, GROUND, z, Kind.PIT_WALL);
            put(out, SIZE_X - 2, GROUND, z, Kind.PIT_WALL);
        }
        for (int x = 1; x < SIZE_X - 1; x++) {
            put(out, x, GROUND, PIT_BACK, Kind.PIT_WALL);
        }
        // Two one-wide doorways through the inner wall, flanking the stair. A
        // single-block gap passes the 0.6-wide player freely but never the
        // 2.4-wide rancor, so the beast stays caged while you come and go —
        // no drop-in trap, no escaped monster. (Left above the floor course so
        // the walking surface stays continuous across the threshold.)
        for (int doorX : new int[]{2, SIZE_X - 3}) {           // x=2 and x=16
            for (int y = GROUND + 1; y <= GROUND + 3; y++) {
                put(out, doorX, y, PIT_FRONT_WALL, Kind.GATE_AIR);
            }
        }
        // Bones littering the floor — the last guests.
        int[][] bones = {{4, 10}, {13, 12}, {6, 15}, {11, 9}, {8, 16}};
        for (int[] b : bones) put(out, b[0], GROUND + 1, b[1], Kind.BONE);

        // Treasure pedestal at the back, directly beneath the dais.
        put(out, CENTER_X, GROUND, 18, Kind.SMOOTH);
        put(out, CENTER_X, GROUND + 1, 18, Kind.CHEST);

        // The rancor, mid-arena with headroom to the throne floor.
        put(out, CENTER_X, GROUND + 1, 13, Kind.RANCOR);
    }

    /** Jabba's dais, throne backing, and the crime lord himself. */
    private static void buildThroneRoom(Map<Long, Placement> out) {
        // Two-high dais at the back of the throne room.
        for (int y = UPPER + 1; y <= UPPER + 2; y++) {
            for (int x = CENTER_X - 2; x <= CENTER_X + 2; x++) {
                for (int z = 16; z <= 18; z++) {
                    put(out, x, y, z, Kind.DAIS);
                }
            }
        }
        // Chiseled throne backing against the rear wall.
        for (int y = UPPER + 1; y <= UPPER + 4; y++) {
            for (int x = CENTER_X - 1; x <= CENTER_X + 1; x++) {
                put(out, x, y, PIT_BACK, Kind.PILLAR);
            }
        }
        // Jabba, atop the dais, facing the grate and the pit beyond.
        put(out, CENTER_X, UPPER + 3, 17, Kind.JABBA);
    }

    /** Carpets, torches, columns, and the apex lantern. */
    private static void decorate(Map<Long, Placement> out) {
        // Carpet runner from the gate, up the stair, to the dais.
        for (int z = 1; z <= 6; z++) {
            put(out, CENTER_X, z + 1, z, Kind.CARPET);        // on each tread
        }
        for (int z = PIT_FRONT_WALL; z <= 18; z++) {
            if (isChute(CENTER_X, z) || isGrate(CENTER_X, z)) continue;
            put(out, CENTER_X, UPPER + 1, z, Kind.CARPET);    // throne-room aisle
        }
        // Throne-room columns flanking the aisle.
        for (int z : new int[]{9, 13, 17}) {
            for (int x : new int[]{CENTER_X - 4, CENTER_X + 4}) {
                for (int y = UPPER + 1; y <= WALL_TOP - 1; y++) {
                    put(out, x, y, z, Kind.PILLAR);
                }
            }
        }
        // Wall torches (as standing torches on ledges) around both levels.
        int[][] torches = {
            {2, GROUND + 3, 3}, {SIZE_X - 3, GROUND + 3, 3},          // entry hall
            {2, UPPER + 2, 9}, {SIZE_X - 3, UPPER + 2, 9},            // throne room
            {2, UPPER + 2, 15}, {SIZE_X - 3, UPPER + 2, 15},
        };
        for (int[] t : torches) put(out, t[0], t[1], t[2], Kind.TORCH);
        // Apex light inside the dome.
        put(out, CENTER_X, DOME_BASE + 3, DOME_CZ, Kind.LANTERN);
    }

    // ---------------- helpers ----------------

    private static boolean isStairMouth(int x, int z) {
        return z == 6 && x >= CENTER_X - 2 && x <= CENTER_X + 2;
    }

    private static boolean isGrate(int x, int z) {
        return x >= GRATE_X0 && x <= GRATE_X1 && z >= GRATE_Z0 && z <= GRATE_Z1;
    }

    private static boolean isChute(int x, int z) {
        return x >= CHUTE_X0 && x <= CHUTE_X1 && z >= CHUTE_Z0 && z <= CHUTE_Z1;
    }

    /** Highest dome level covering this column, or -1 if outside the dome. */
    private static int domeLevel(int x, int z) {
        int found = -1;
        for (int i = 0; i < DOME_RX.length; i++) {
            if (inEllipse(x, z, DOME_RX[i], DOME_RZ[i])) {
                found = DOME_BASE + i;
            }
        }
        return found;
    }

    private static boolean inEllipse(int x, int z, int rx, int rz) {
        int dx = x - CENTER_X;
        int dz = z - DOME_CZ;
        return dx * dx * rz * rz + dz * dz * rx * rx <= rx * rx * rz * rz;
    }

    private static void fillAir(Map<Long, Placement> out, int x0, int x1, int y0, int y1, int z0, int z1) {
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++)
                for (int z = z0; z <= z1; z++)
                    put(out, x, y, z, Kind.AIR);
    }

    /** Last write wins, mirroring the build-then-carve-then-fit ordering. */
    private static void put(Map<Long, Placement> out, int x, int y, int z, Kind kind) {
        out.put(((long) x << 40) | ((long) (y + 8) << 20) | z, new Placement(x, y, z, kind));
    }
}
