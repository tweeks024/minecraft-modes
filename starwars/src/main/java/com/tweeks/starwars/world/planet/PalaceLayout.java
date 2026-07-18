package com.tweeks.starwars.world.planet;

/**
 * Pure layout math for the Imperial Palace — the Emperor's seat, a fixed
 * landmark carved into the Coruscant city at {@link #ORIGIN_X}/{@link #ORIGIN_Z}.
 * Same shape as {@link CityLayout}: {@code kindAt}/{@code topY} are deterministic
 * functions of local coordinates, so the chunk generator can query it
 * column-by-column and it is unit-testable without an engine bootstrap.
 *
 * <p>The palace is a dark blackstone keep fronted by a monumental stairway that
 * climbs — one step per row, no elevator, no puzzle — from the plaza up to an
 * elevated <b>throne pavilion</b>: a windowed chamber with the Emperor's throne
 * at the back (where {@link #throneX}/{@link #throneY}/{@link #throneZ} seat
 * Palpatine) and a spire above. Reachable on foot by anyone.
 */
public final class PalaceLayout {
    private PalaceLayout() {}

    /** Street/plaza surface, matching {@link CityLayout#STREET_Y}. */
    public static final int STREET_Y = CityLayout.STREET_Y;

    public static final int SIZE_X = 39;
    public static final int SIZE_Z = 41;
    public static final int CENTER_X = 19;

    /** World min-corner of the palace footprint (well north of the arrival plaza). */
    public static final int ORIGIN_X = -19;   // CENTER_X lands on world x=0
    public static final int ORIGIN_Z = 24;

    // Grand stairway: climbs one block per z across this depth band.
    private static final int STAIR_Z0 = 2;
    private static final int STAIR_Z1 = 24;
    private static final int STAIR_HALF = 4;              // 9-wide (CENTER_X +/- 4)

    // The keep + throne pavilion occupy the back band.
    private static final int KEEP_Z0 = 25;
    private static final int KEEP_Z1 = 38;
    private static final int KEEP_MARGIN = 3;             // keep footprint inset from the edges
    public static final int THRONE_FLOOR_Y = STREET_Y + 24;
    private static final int ROOM_MARGIN = 6;             // throne-room walls inset
    private static final int CEIL_Y = THRONE_FLOOR_Y + 8;

    // Throne dais at the back of the pavilion.
    private static final int DAIS_Z0 = 34;
    private static final int DAIS_Z1 = 36;
    private static final int DAIS_HALF = 3;

    private static final int SPIRE_TOP = CEIL_Y + 7;

    public enum Kind {
        AIR,
        PLAZA,        // polished-blackstone base
        WALL,         // blackstone keep + pavilion walls
        ACCENT,       // chiseled/gilded banding
        STAIR,        // the grand stairway treads
        FLOOR,        // pavilion floor
        WINDOW,       // tinted glass
        BANNER,       // Imperial red
        PILLAR,       // flanking columns
        DAIS,         // throne platform
        THRONE,       // the Emperor's seat
        LAMP,         // eerie light
        SPIRE,        // dark antenna
        THRONE_MARKER // Palpatine spawn (carved to air)
    }

    /** Local z of the Emperor's seat marker — the middle of the dais. */
    private static final int MARKER_Z = (DAIS_Z0 + DAIS_Z1) / 2;
    /** y of the seat marker: standing on the 1-high dais above the pavilion floor. */
    private static final int MARKER_Y = THRONE_FLOOR_Y + 2;

    /** Whether a world column falls inside the palace footprint. */
    public static boolean contains(int wx, int wz) {
        int lx = wx - ORIGIN_X;
        int lz = wz - ORIGIN_Z;
        return lx >= 0 && lx < SIZE_X && lz >= 0 && lz < SIZE_Z;
    }

    // ---- world-space spawn seat for the Emperor (shared with the spawner) ----
    // Exactly the THRONE_MARKER cell, so the spawner seats him on his throne.
    public static int throneX() { return ORIGIN_X + CENTER_X; }
    public static int throneY() { return MARKER_Y; }
    public static int throneZ() { return ORIGIN_Z + MARKER_Z; }

    /** Highest occupied y in this local column (>= STREET_Y). */
    public static int topY(int lx, int lz) {
        if (lx == CENTER_X && lz >= DAIS_Z0 && lz <= KEEP_Z1) {
            return SPIRE_TOP;                            // spire rises over the throne
        }
        if (inRoomFootprint(lx, lz)) {
            return CEIL_Y;
        }
        if (inKeepFootprint(lx, lz)) {
            return THRONE_FLOOR_Y - 1;                   // solid keep top
        }
        if (inStair(lx, lz)) {
            return treadTop(lz);
        }
        if (isFlankPillar(lx, lz)) {
            return STREET_Y + 10;
        }
        return STREET_Y;                                 // plaza
    }

    /** Kind at a local position (y in world coords, >= STREET_Y). */
    public static Kind kindAt(int lx, int y, int lz) {
        if (y == STREET_Y && !inKeepFootprint(lx, lz) && !inStair(lx, lz)) {
            return Kind.PLAZA;
        }

        // Flanking colonnade along the stairway.
        if (isFlankPillar(lx, lz)) {
            if (y > STREET_Y && y <= STREET_Y + 8) return Kind.PILLAR;
            if (y == STREET_Y + 9) return Kind.LAMP;
            if (y == STREET_Y + 10) return Kind.ACCENT;
        }

        // Grand stairway: solid stepped ramp.
        if (inStair(lx, lz)) {
            if (y >= STREET_Y && y <= treadTop(lz)) return Kind.STAIR;
            return Kind.AIR;
        }

        // Keep + throne pavilion.
        if (inKeepFootprint(lx, lz)) {
            if (y < THRONE_FLOOR_Y) {
                return keepBodyKind(lx, y, lz);
            }
            return pavilionKind(lx, y, lz);
        }

        return Kind.AIR;
    }

    // ------------------------------------------------------------------
    // Keep body (solid plinth with a facade of windows)

    private static Kind keepBodyKind(int lx, int y, int lz) {
        boolean edge = lx == KEEP_MARGIN || lx == SIZE_X - 1 - KEEP_MARGIN
            || lz == KEEP_Z0 || lz == KEEP_Z1;
        if (!edge) return Kind.WALL;                     // solid core
        // Facade: masonry bands every 6, tinted windows between the piers.
        int band = Math.floorMod(y - (STREET_Y + 1), 6);
        if (band == 5) return Kind.ACCENT;
        if (band < 2) return Kind.WALL;
        int t = (lz == KEEP_Z0 || lz == KEEP_Z1) ? lx : lz;
        if (t % 3 == 0) return Kind.WALL;                // mullion pier
        return Kind.WINDOW;
    }

    // ------------------------------------------------------------------
    // Throne pavilion (hollow chamber on top of the keep)

    private static Kind pavilionKind(int lx, int y, int lz) {
        if (y == THRONE_FLOOR_Y) return Kind.FLOOR;
        if (y == CEIL_Y) {
            // Ceiling, but leave a shaft at center for the spire base + skylight.
            return (lx == CENTER_X && lz == (DAIS_Z0 + DAIS_Z1) / 2) ? Kind.AIR : Kind.WALL;
        }
        if (y > CEIL_Y) {
            return (lx == CENTER_X && lz == (DAIS_Z0 + DAIS_Z1) / 2 && y <= SPIRE_TOP)
                ? Kind.SPIRE : Kind.AIR;
        }

        // Interior (THRONE_FLOOR_Y < y < CEIL_Y).
        boolean wall = lx == ROOM_MARGIN || lx == SIZE_X - 1 - ROOM_MARGIN
            || lz == KEEP_Z1;                            // back + side walls
        boolean frontWall = lz == KEEP_Z0;
        if (wall) return wallOrWindow(lx, y, lz);
        if (frontWall) {
            // Front opens where the stairway arrives; walls flank the doorway.
            boolean doorway = lx >= CENTER_X - STAIR_HALF && lx <= CENTER_X + STAIR_HALF
                && y <= THRONE_FLOOR_Y + 4;
            return doorway ? Kind.AIR : wallOrWindow(lx, y, lz);
        }

        // Throne dais at the back.
        if (lz >= DAIS_Z0 && lz <= DAIS_Z1
            && lx >= CENTER_X - DAIS_HALF && lx <= CENTER_X + DAIS_HALF) {
            if (y <= THRONE_FLOOR_Y + 1) return Kind.DAIS;          // 1-high platform
            if (lx == CENTER_X && lz == DAIS_Z1 && y >= MARKER_Y && y <= MARKER_Y + 3) {
                return Kind.THRONE;                      // a high-backed seat behind him
            }
            if (lx == CENTER_X && lz == MARKER_Z && y == MARKER_Y) {
                return Kind.THRONE_MARKER;               // the Emperor sits here
            }
        }
        // Imperial banners hanging on the back wall flanking the throne.
        if (lz == KEEP_Z1 - 1 && (lx == CENTER_X - DAIS_HALF - 1 || lx == CENTER_X + DAIS_HALF + 1)
            && y >= THRONE_FLOOR_Y + 2 && y <= THRONE_FLOOR_Y + 5) {
            return Kind.BANNER;
        }
        return Kind.AIR;
    }

    private static Kind wallOrWindow(int lx, int y, int lz) {
        // Tall tinted windows between piers; solid at the corners and floorline.
        boolean corner = (lx == ROOM_MARGIN || lx == SIZE_X - 1 - ROOM_MARGIN)
            && (lz == KEEP_Z0 || lz == KEEP_Z1);
        if (corner) return Kind.WALL;
        if (y <= THRONE_FLOOR_Y + 1 || y >= CEIL_Y - 1) return Kind.WALL;
        int t = (lx == ROOM_MARGIN || lx == SIZE_X - 1 - ROOM_MARGIN) ? lz : lx;
        return t % 3 == 0 ? Kind.PILLAR : Kind.WINDOW;
    }

    // ------------------------------------------------------------------
    // Zone helpers

    private static boolean inStair(int lx, int lz) {
        return lz >= STAIR_Z0 && lz <= STAIR_Z1
            && lx >= CENTER_X - STAIR_HALF && lx <= CENTER_X + STAIR_HALF;
    }

    private static int treadTop(int lz) {
        return STREET_Y + (lz - STAIR_Z0 + 1);           // one step up per row
    }

    private static boolean inKeepFootprint(int lx, int lz) {
        return lz >= KEEP_Z0 && lz <= KEEP_Z1
            && lx >= KEEP_MARGIN && lx <= SIZE_X - 1 - KEEP_MARGIN;
    }

    private static boolean inRoomFootprint(int lx, int lz) {
        return lz >= KEEP_Z0 && lz <= KEEP_Z1
            && lx >= ROOM_MARGIN && lx <= SIZE_X - 1 - ROOM_MARGIN;
    }

    private static boolean isFlankPillar(int lx, int lz) {
        boolean onRail = lx == CENTER_X - STAIR_HALF - 2 || lx == CENTER_X + STAIR_HALF + 2;
        return onRail && lz >= STAIR_Z0 && lz <= STAIR_Z1 && lz % 4 == 0;
    }
}
