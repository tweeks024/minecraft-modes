package com.tweeks.starwars.world.planet;

/**
 * Pure interior-station layout for the Death Star generator. Where
 * {@link CityLayout} carves a city UP out of open sky, this carves rooms and
 * corridors DOWN into solid hull: every column is battle-station plating
 * except the decks, halls, rooms, lift shafts and reactor wells cut into it.
 *
 * <p>Decks stack every {@link #DECK} blocks. On each deck a 12-wide grid lays
 * 3-wide corridors between 9x9 rooms; corridors line up floor-to-floor so you
 * can see down a hall, and lift shafts punch vertically between decks. A few
 * cells are special (detention blocks, supply caches, control rooms), and rare
 * columns open into a full-height reactor well with a glowing core.
 *
 * <p>No Minecraft imports — the generator maps {@link Kind} to block states.
 */
public final class StationLayout {
    public static final int BOTTOM_Y = 0;
    public static final int TOP_Y = 200;
    /** Vertical period of one deck: floor + 6 air + ceiling = 8. */
    public static final int DECK = 8;
    /** Walkable headroom inside a deck. */
    public static final int HEADROOM = 6;
    public static final int CELL = 12;
    public static final int CORRIDOR_W = 3;
    /** Reference deck floor players and gates target. */
    public static final int SPAWN_DECK_FLOOR = 96;

    public enum Kind {
        HULL, BULKHEAD, FLOOR, CEILING, AIR,
        CORRIDOR_LIGHT, ROOM_LIGHT, DOORWAY,
        BARS, BUNK, CONSOLE, REDSTONE_LAMP,
        SUPPLY_IRON, SUPPLY_GOLD, SUPPLY_DIAMOND, SUPPLY_REDSTONE,
        REACTOR_CASING, REACTOR_CORE, LADDER
    }

    public enum RoomType {
        EMPTY, BARRACKS, SUPPLY, CONTROL, DETENTION
    }

    private StationLayout() {
    }

    // ------------------------------------------------------------------
    // Hashing (shared style with CityLayout)

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static long cellHash(long seed, int cellX, int cellZ, int salt) {
        long h = seed ^ 0x0EA7_57A9L * salt;
        h = mix(h ^ cellX * 0x9E3779B97F4A7C15L);
        h = mix(h ^ cellZ * 0xC2B2AE3D27D4EB4FL);
        return h;
    }

    private static int bucket(long hash, int shift, int bound) {
        return (int) Long.remainderUnsigned(hash >>> shift, bound);
    }

    public static int cellOf(int worldCoord) {
        return Math.floorDiv(worldCoord, CELL);
    }

    // ------------------------------------------------------------------
    // Deck + cell classification

    /** Local coordinate within the 12-cell; corridor when < CORRIDOR_W. */
    private static boolean isCorridor(int local) {
        return local < CORRIDOR_W;
    }

    /** True where both axes are corridor — a hall column. */
    private static boolean corridorColumn(int lx, int lz) {
        return isCorridor(lx) || isCorridor(lz);
    }

    /** Index of the deck whose floor is at or below y; floor at deck*DECK. */
    private static int deckOf(int y) {
        return Math.floorDiv(y - BOTTOM_Y, DECK);
    }

    private static int deckFloorY(int deck) {
        return BOTTOM_Y + deck * DECK;
    }

    public static RoomType roomType(long seed, int cellX, int cellZ, int deck) {
        long h = cellHash(seed, cellX, cellZ, 17 + deck);
        int roll = bucket(h, 0, 100);
        if (roll < 8) {
            return RoomType.DETENTION;
        }
        if (roll < 20) {
            return RoomType.SUPPLY;
        }
        if (roll < 32) {
            return RoomType.CONTROL;
        }
        if (roll < 60) {
            return RoomType.BARRACKS;
        }
        return RoomType.EMPTY;
    }

    /** A lift shaft punches through this cell's corner across all decks. */
    private static boolean isLiftCell(long seed, int cellX, int cellZ) {
        return bucket(cellHash(seed, cellX, cellZ, 3), 0, 7) == 0;
    }

    /** A reactor well replaces this cell entirely, full height. */
    public static boolean isReactorCell(long seed, int cellX, int cellZ) {
        return bucket(cellHash(seed, cellX, cellZ, 5), 0, 61) == 0;
    }

    // ------------------------------------------------------------------
    // Column queries

    /** The station is solid to the top; this is the reported surface. */
    public static int topY() {
        return TOP_Y;
    }

    public static Kind kindAt(long seed, int wx, int y, int wz) {
        if (y <= BOTTOM_Y) {
            return Kind.BULKHEAD;
        }
        if (y >= TOP_Y) {
            return Kind.HULL;
        }
        int cellX = cellOf(wx);
        int cellZ = cellOf(wz);

        // Reactor wells: a glowing core column in a shell of casing, open
        // shaft around it, full height.
        if (isReactorCell(seed, cellX, cellZ)) {
            return reactorKind(wx, y, wz);
        }

        int lx = Math.floorMod(wx, CELL);
        int lz = Math.floorMod(wz, CELL);
        int deck = deckOf(y);
        int floorY = deckFloorY(deck);
        int localY = y - floorY;

        // Deck floor and ceiling slabs.
        if (localY == 0) {
            return Kind.FLOOR;
        }
        if (localY == DECK - 1) {
            return Kind.CEILING;
        }
        if (localY > HEADROOM) {
            return Kind.HULL; // service crawlspace between ceiling and next floor
        }

        // Lift shafts: a 3x3 open hole with a ladder, through floor+ceiling.
        if (isLiftCell(seed, cellX, cellZ) && lx >= 1 && lx <= 3 && lz >= 1 && lz <= 3) {
            if (localY == 0 || localY == DECK - 1) {
                // handled above for floor/ceiling; force open here instead
                return lx == 1 && lz == 2 ? Kind.LADDER : Kind.AIR;
            }
            return lx == 1 && lz == 2 ? Kind.LADDER : Kind.AIR;
        }

        boolean corridor = corridorColumn(lx, lz);
        if (corridor) {
            // Ceiling light strips run down the centre lane.
            if (localY == HEADROOM && (lx == 1 || lz == 1)) {
                return Kind.CORRIDOR_LIGHT;
            }
            return Kind.AIR;
        }

        // Room interior. Perimeter (local 3 or 11 on either axis) is wall,
        // with a doorway onto the corridor; inside is furnished by type.
        return roomKind(seed, cellX, cellZ, deck, lx, lz, localY);
    }

    private static Kind reactorKind(int wx, int y, int wz) {
        int lx = Math.floorMod(wx, CELL);
        int lz = Math.floorMod(wz, CELL);
        int cx = CELL / 2;
        int dx = lx - cx;
        int dz = lz - cx;
        int r2 = dx * dx + dz * dz;
        if (r2 <= 1) {
            return Kind.REACTOR_CORE; // glowing spine
        }
        if (r2 <= 4) {
            return Kind.REACTOR_CASING;
        }
        // Open shaft around the core, ringed by casing at the cell edge.
        if (lx == 0 || lz == 0 || lx == CELL - 1 || lz == CELL - 1) {
            return Kind.REACTOR_CASING;
        }
        return Kind.AIR;
    }

    private static Kind roomKind(long seed, int cellX, int cellZ, int deck, int lx, int lz, int localY) {
        int min = CORRIDOR_W; // 3
        int max = CELL - 1;   // 11
        boolean onPerimeter = lx == min || lx == max || lz == min || lz == max;

        // Doorway: a 1-wide gap in the wall facing the low corridor, at
        // stepping height (localY 1..2).
        boolean doorwaySlot = (lx == min && lz == CELL / 2) || (lz == min && lx == CELL / 2);
        if (onPerimeter) {
            if (doorwaySlot && localY <= 2) {
                return Kind.DOORWAY;
            }
            return Kind.BULKHEAD;
        }

        // Interior furnishings by room type. Floor-relative localY: 1..HEADROOM-1
        RoomType type = roomType(seed, cellX, cellZ, deck);
        boolean interiorFloor = localY == 1;
        boolean ceilingLine = localY == HEADROOM;

        if (ceilingLine && lx == CELL / 2 && lz == CELL / 2) {
            return Kind.ROOM_LIGHT;
        }

        return switch (type) {
            case EMPTY -> Kind.AIR;
            case BARRACKS -> (interiorFloor && (lx + lz) % 3 == 0) ? Kind.BUNK : Kind.AIR;
            case CONTROL -> {
                if (interiorFloor && onInnerRing(lx, lz)) {
                    yield Kind.CONSOLE;
                }
                if (localY == 1 && lx == CELL / 2 && lz == CELL / 2) {
                    yield Kind.REDSTONE_LAMP;
                }
                yield Kind.AIR;
            }
            case SUPPLY -> supplyKind(seed, cellX, cellZ, deck, lx, lz, localY);
            case DETENTION -> detentionKind(lx, lz, localY);
        };
    }

    private static boolean onInnerRing(int lx, int lz) {
        return (lx == 4 || lx == 10 || lz == 4 || lz == 10)
            && lx >= 4 && lx <= 10 && lz >= 4 && lz <= 10;
    }

    private static Kind supplyKind(long seed, int cellX, int cellZ, int deck, int lx, int lz, int localY) {
        // A central rack (localY 1..3) studded with valuable blocks to mine.
        if (lx < 5 || lx > 9 || lz < 5 || lz > 9 || localY < 1 || localY > 3) {
            return Kind.AIR;
        }
        long h = cellHash(seed, cellX * 31 + lx, cellZ * 31 + lz, 40 + deck + localY);
        int roll = bucket(h, 0, 100);
        if (roll < 6) {
            return Kind.SUPPLY_DIAMOND;
        }
        if (roll < 30) {
            return Kind.SUPPLY_GOLD;
        }
        if (roll < 55) {
            return Kind.SUPPLY_REDSTONE;
        }
        if (roll < 85) {
            return Kind.SUPPLY_IRON;
        }
        return Kind.AIR;
    }

    private static Kind detentionKind(int lx, int lz, int localY) {
        // Two rows of barred cells split by a central aisle at lz==7.
        if (lz == 7) {
            return Kind.AIR; // guard aisle
        }
        boolean barLine = lz == 6 || lz == 8;
        boolean cellDivider = (lx % 2 == 0);
        if (barLine) {
            // Bars face the aisle, with a gap every other column for a door.
            return cellDivider ? Kind.BARS : Kind.AIR;
        }
        if (cellDivider && (lx == 4 || lx == 6 || lx == 8 || lx == 10)) {
            return localY <= HEADROOM ? Kind.BARS : Kind.AIR; // cell partitions
        }
        return Kind.AIR;
    }

    // ------------------------------------------------------------------
    // Spawn / heightmap helpers

    /**
     * A safe arrival position: snaps (wx, wz) to the nearest corridor centre
     * lane (avoiding reactor wells) and finds the deck floor nearest
     * {@link #SPAWN_DECK_FLOOR} that is open there. Returns {@code {x, y, z}}
     * with {@code y} being the block to stand ON's top (i.e. feet level).
     * Pure — unit-tested.
     */
    public static int[] arrivalPos(long seed, int wx, int wz) {
        // Corridor centre lane is local coordinate 1 (of the 0..2 corridor).
        int sx = cellOf(wx) * CELL + 1;
        int sz = cellOf(wz) * CELL + 1;
        // Step off a reactor well onto the next cell if we landed on one.
        int guard = 0;
        while (isReactorCell(seed, cellOf(sx), cellOf(sz)) && guard++ < 8) {
            sx += CELL;
        }
        int refDeck = deckOf(SPAWN_DECK_FLOOR);
        for (int d = 0; d < 8; d++) {
            for (int deck : new int[]{refDeck + d, refDeck - d}) {
                int floorY = deckFloorY(deck);
                if (floorY <= BOTTOM_Y || floorY >= TOP_Y - DECK) {
                    continue;
                }
                Kind atFeet = kindAt(seed, sx, floorY + 1, sz);
                if (atFeet == Kind.AIR && kindAt(seed, sx, floorY, sz) == Kind.FLOOR) {
                    return new int[]{sx, floorY + 1, sz};
                }
            }
        }
        return new int[]{sx, SPAWN_DECK_FLOOR + 1, sz};
    }

    /** Convenience: just the standing Y from {@link #arrivalPos}. */
    public static int safeFloorY(long seed, int wx, int wz) {
        return arrivalPos(seed, wx, wz)[1];
    }
}
