package com.tweeks.starwars.world.planet;

/**
 * Pure layout math for the Coruscant city generator. Carves the world into a
 * 24-block grid: a 5-wide street lattice with 19x19 building plots between.
 * Every decision is a deterministic function of (seed, coordinates) so chunks
 * generate independently and streets/buildings line up across chunk borders.
 * No Minecraft imports — unit-testable; the chunk generator maps Kind + cell
 * palette to concrete block states.
 */
public final class CityLayout {
    /** y of the street surface block. */
    public static final int STREET_Y = 64;
    /** Bottom of the dimension (bedrock layer). */
    public static final int BOTTOM_Y = 0;
    /** Grid period: one street + one plot. */
    public static final int CELL = 24;
    /** Street lattice width (local coords 0..4 on each axis). */
    public static final int ROAD_W = 5;
    /** Building plot edge (CELL - ROAD_W). */
    public static final int PLOT = CELL - ROAD_W;
    public static final int MIN_TOWER = 24;
    public static final int MAX_TOWER = 120;
    /** Landing pad deck height above the street. */
    public static final int PAD_RISE = 16;

    public enum Kind {
        AIR, BEDROCK, FOUNDATION,
        STREET, STREET_LINE, SIDEWALK, LAMP_POST, LAMP_LIGHT,
        WALL, WALL_ACCENT, WINDOW, WINDOW_LIT, ROOF, ROOF_EDGE, SPIRE,
        PAD_DECK, PAD_MARK, PYLON,
        PLAZA_FLOOR, PLAZA_ACCENT, MONUMENT
    }

    public enum CellType {
        TOWER, PLAZA, PAD
    }

    /**
     * Everything decided per city cell. {@code margin} is the tower's setback
     * from the plot edge; {@code spire} is the antenna height (0 = none).
     */
    public record CellSpec(CellType type, int height, int palette, int margin, int spire) {
    }

    private CityLayout() {
    }

    // ------------------------------------------------------------------
    // Hashing

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static long cellHash(long seed, int cellX, int cellZ, int salt) {
        long h = seed ^ 0x5EEDC0DEL * salt;
        h = mix(h ^ cellX * 0x9E3779B97F4A7C15L);
        h = mix(h ^ cellZ * 0xC2B2AE3D27D4EB4FL);
        return h;
    }

    private static int bucket(long hash, int shift, int bound) {
        return (int) Long.remainderUnsigned(hash >>> shift, bound);
    }

    // ------------------------------------------------------------------
    // Cell specs

    public static CellSpec cellSpec(long seed, int cellX, int cellZ) {
        long kindHash = cellHash(seed, cellX, cellZ, 1);
        long shapeHash = cellHash(seed, cellX, cellZ, 2);
        int roll = bucket(kindHash, 0, 221);
        CellType type = roll < 17 ? CellType.PLAZA : roll < 30 ? CellType.PAD : CellType.TOWER;
        int height = MIN_TOWER + bucket(shapeHash, 0, 49) + bucket(shapeHash, 8, 49);
        int palette = bucket(shapeHash, 16, 3);
        int margin = 1 + bucket(shapeHash, 24, 2);
        int spire = height >= 90 && bucket(shapeHash, 32, 3) == 0 ? 3 + bucket(shapeHash, 40, 4) : 0;
        return new CellSpec(type, height, palette, margin, spire);
    }

    /** ~30% of window panes glow. Stable per pane so relogs don't flicker. */
    private static boolean windowLit(long seed, int cellX, int cellZ, int face, int t, int floor) {
        long h = cellHash(seed, cellX, cellZ, 100 + face);
        return bucket(mix(h ^ t * 0x100000001B3L ^ (long) floor << 17), 0, 10) < 3;
    }

    public static int cellOf(int worldCoord) {
        return Math.floorDiv(worldCoord, CELL);
    }

    // ------------------------------------------------------------------
    // Column queries

    /** Highest non-air y in this column (>= STREET_Y everywhere). */
    public static int topY(long seed, int wx, int wz) {
        int lx = Math.floorMod(wx, CELL);
        int lz = Math.floorMod(wz, CELL);
        if (lx < ROAD_W || lz < ROAD_W) {
            return isLampColumn(lx, lz) ? STREET_Y + 5 : STREET_Y;
        }
        CellSpec spec = cellSpec(seed, cellOf(wx), cellOf(wz));
        int px = lx - ROAD_W;
        int pz = lz - ROAD_W;
        return switch (spec.type()) {
            case TOWER -> {
                int m = spec.margin();
                if (px < m || px > PLOT - 1 - m || pz < m || pz > PLOT - 1 - m) {
                    yield STREET_Y;
                }
                int roofY = STREET_Y + spec.height();
                if (onFootprintEdge(px, pz, m)) {
                    yield roofY + 1; // parapet lip
                }
                yield px == PLOT / 2 && pz == PLOT / 2 ? roofY + spec.spire() : roofY;
            }
            case PAD -> {
                if (px >= 1 && px <= PLOT - 2 && pz >= 1 && pz <= PLOT - 2) {
                    yield STREET_Y + PAD_RISE;
                }
                yield STREET_Y;
            }
            case PLAZA -> {
                if (px == PLOT / 2 && pz == PLOT / 2) {
                    yield STREET_Y + 5; // monument column + light
                }
                if (isMonumentRing(px, pz)) {
                    yield STREET_Y + 1;
                }
                yield isPlazaLamp(px, pz) ? STREET_Y + 3 : STREET_Y;
            }
        };
    }

    /** The block kind at an absolute world position. */
    public static Kind kindAt(long seed, int wx, int y, int wz) {
        if (y == BOTTOM_Y) {
            return Kind.BEDROCK;
        }
        if (y < STREET_Y) {
            return Kind.FOUNDATION;
        }
        int lx = Math.floorMod(wx, CELL);
        int lz = Math.floorMod(wz, CELL);
        if (lx < ROAD_W || lz < ROAD_W) {
            return roadKind(wx, y, wz, lx, lz);
        }
        CellSpec spec = cellSpec(seed, cellOf(wx), cellOf(wz));
        int px = lx - ROAD_W;
        int pz = lz - ROAD_W;
        return switch (spec.type()) {
            case TOWER -> towerKind(seed, wx, y, wz, px, pz, spec);
            case PAD -> padKind(y, px, pz);
            case PLAZA -> plazaKind(y, px, pz);
        };
    }

    // ------------------------------------------------------------------
    // Streets

    private static boolean isLampColumn(int lx, int lz) {
        // Two diagonal lamp posts per intersection square.
        return lx < ROAD_W && lz < ROAD_W && ((lx == 0 && lz == 0) || (lx == 4 && lz == 4));
    }

    private static Kind roadKind(int wx, int y, int wz, int lx, int lz) {
        if (y > STREET_Y) {
            if (isLampColumn(lx, lz)) {
                if (y <= STREET_Y + 4) {
                    return Kind.LAMP_POST;
                }
                if (y == STREET_Y + 5) {
                    return Kind.LAMP_LIGHT;
                }
            }
            return Kind.AIR;
        }
        // y == STREET_Y: surface paint.
        boolean nsRoad = lx < ROAD_W;
        boolean ewRoad = lz < ROAD_W;
        if (nsRoad && ewRoad) {
            boolean curb = (lx == 0 || lx == 4) && (lz == 0 || lz == 4);
            return curb ? Kind.SIDEWALK : Kind.STREET;
        }
        if (nsRoad) {
            if (lx == 0 || lx == 4) {
                return Kind.SIDEWALK;
            }
            return lx == 2 && Math.floorMod(wz, 4) < 2 ? Kind.STREET_LINE : Kind.STREET;
        }
        if (lz == 0 || lz == 4) {
            return Kind.SIDEWALK;
        }
        return lz == 2 && Math.floorMod(wx, 4) < 2 ? Kind.STREET_LINE : Kind.STREET;
    }

    // ------------------------------------------------------------------
    // Towers

    private static boolean onFootprintEdge(int px, int pz, int margin) {
        int max = PLOT - 1 - margin;
        boolean inside = px >= margin && px <= max && pz >= margin && pz <= max;
        return inside && (px == margin || px == max || pz == margin || pz == max);
    }

    private static Kind towerKind(long seed, int wx, int y, int wz, int px, int pz, CellSpec spec) {
        int m = spec.margin();
        int max = PLOT - 1 - m;
        boolean insideFootprint = px >= m && px <= max && pz >= m && pz <= max;
        if (!insideFootprint) {
            return y == STREET_Y ? Kind.SIDEWALK : Kind.AIR;
        }
        int roofY = STREET_Y + spec.height();
        if (y == STREET_Y) {
            return Kind.WALL; // ground plinth
        }
        if (y > roofY) {
            if (y == roofY + 1 && onFootprintEdge(px, pz, m)) {
                return Kind.ROOF_EDGE;
            }
            if (px == PLOT / 2 && pz == PLOT / 2 && y <= roofY + spec.spire()) {
                return Kind.SPIRE;
            }
            return Kind.AIR;
        }
        if (y == roofY) {
            return Kind.ROOF;
        }
        boolean onEdge = onFootprintEdge(px, pz, m);
        if (!onEdge) {
            return Kind.WALL; // solid core
        }
        // Facade: corner pillars stay solid; between them, 3-high window rows
        // every 6 with a masonry band on the floor line.
        boolean xEdge = px == m || px == max;
        boolean zEdge = pz == m || pz == max;
        if (xEdge && zEdge) {
            return Kind.WALL;
        }
        int floorBand = Math.floorMod(y - (STREET_Y + 1), 6);
        if (floorBand == 5) {
            return Kind.WALL_ACCENT;
        }
        if (floorBand < 2) {
            return Kind.WALL;
        }
        int t = xEdge ? pz : px;
        if ((t - m) % 3 == 0) {
            return Kind.WALL; // mullion pillar
        }
        int face = xEdge ? (px == m ? 0 : 1) : (pz == m ? 2 : 3);
        int floorIndex = (y - (STREET_Y + 1)) / 6;
        return windowLit(seed, cellOf(wx), cellOf(wz), face, t, floorIndex) ? Kind.WINDOW_LIT : Kind.WINDOW;
    }

    // ------------------------------------------------------------------
    // Landing pads

    private static Kind padKind(int y, int px, int pz) {
        int deckY = STREET_Y + PAD_RISE;
        boolean onDeckPlan = px >= 1 && px <= PLOT - 2 && pz >= 1 && pz <= PLOT - 2;
        boolean inPylon = px >= 7 && px <= 11 && pz >= 7 && pz <= 11;
        if (y == STREET_Y) {
            return Kind.SIDEWALK;
        }
        if (y < deckY) {
            return inPylon ? Kind.PYLON : Kind.AIR;
        }
        if (y == deckY && onDeckPlan) {
            int dx = px - PLOT / 2;
            int dz = pz - PLOT / 2;
            int r2 = dx * dx + dz * dz;
            return (r2 <= 1 || (r2 >= 25 && r2 <= 36)) ? Kind.PAD_MARK : Kind.PAD_DECK;
        }
        return Kind.AIR;
    }

    // ------------------------------------------------------------------
    // Plazas

    private static boolean isPlazaLamp(int px, int pz) {
        return (px == 4 || px == 14) && (pz == 4 || pz == 14);
    }

    private static boolean isMonumentRing(int px, int pz) {
        return px >= PLOT / 2 - 1 && px <= PLOT / 2 + 1
            && pz >= PLOT / 2 - 1 && pz <= PLOT / 2 + 1;
    }

    private static Kind plazaKind(int y, int px, int pz) {
        int c = PLOT / 2;
        if (y == STREET_Y) {
            return (px + pz) % 6 == 0 ? Kind.PLAZA_ACCENT : Kind.PLAZA_FLOOR;
        }
        if (px == c && pz == c) {
            if (y <= STREET_Y + 4) {
                return Kind.MONUMENT;
            }
            return y == STREET_Y + 5 ? Kind.LAMP_LIGHT : Kind.AIR;
        }
        if (y == STREET_Y + 1 && isMonumentRing(px, pz)) {
            return Kind.MONUMENT;
        }
        if (isPlazaLamp(px, pz)) {
            if (y <= STREET_Y + 2) {
                return Kind.LAMP_POST;
            }
            return y == STREET_Y + 3 ? Kind.LAMP_LIGHT : Kind.AIR;
        }
        return Kind.AIR;
    }
}
