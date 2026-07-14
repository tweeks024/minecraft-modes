package com.tweeks.starwars.world.planet;

import com.tweeks.starwars.world.planet.StationLayout.Kind;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationLayoutTest {
    private static final long SEED = 0xDEA757A9L;

    @Test
    void deterministic() {
        for (int i = 0; i < 60; i++) {
            int wx = i * 41 - 400;
            int wz = i * 71 - 600;
            for (int y = 0; y <= StationLayout.TOP_Y; y += 11) {
                assertEquals(StationLayout.kindAt(SEED, wx, y, wz),
                    StationLayout.kindAt(SEED, wx, y, wz));
            }
        }
        assertEquals(StationLayout.roomType(SEED, 2, 3, 4), StationLayout.roomType(SEED, 2, 3, 4));
    }

    @Test
    void seedChangesTheStation() {
        int diff = 0;
        for (int cell = 0; cell < 120; cell++) {
            if (StationLayout.roomType(SEED, cell, 0, 12) != StationLayout.roomType(SEED + 1, cell, 0, 12)) {
                diff++;
            }
        }
        assertTrue(diff > 20, "seed should reshuffle rooms, got " + diff);
    }

    @Test
    void bottomAndTopAreSolidHull() {
        for (int i = 0; i < 40; i++) {
            int wx = i * 53 - 700;
            int wz = i * 37 - 300;
            assertEquals(Kind.BULKHEAD, StationLayout.kindAt(SEED, wx, StationLayout.BOTTOM_Y, wz));
            assertEquals(Kind.HULL, StationLayout.kindAt(SEED, wx, StationLayout.TOP_Y, wz));
        }
    }

    @Test
    void decksHaveFloorsAndHeadroom() {
        // Find a corridor column that is not a reactor cell, then check a deck.
        int wx = 1;   // local x = 1 → corridor
        int wz = 1;   // local z = 1 → corridor
        // Ensure not a reactor/lift cell for a clean read.
        int cell = 0;
        int probe = 0;
        while ((StationLayout.isReactorCell(SEED, cell, cell)) && probe < 200) {
            cell++;
            probe++;
        }
        wx = cell * StationLayout.CELL + 1;
        wz = cell * StationLayout.CELL + 1;

        int floorY = 96 - (96 % StationLayout.DECK); // a deck floor near spawn
        // Floor solid, headroom air above it, ceiling solid at top of deck.
        assertEquals(Kind.FLOOR, StationLayout.kindAt(SEED, wx, floorY, wz));
        boolean sawAir = false;
        for (int dy = 1; dy <= StationLayout.HEADROOM; dy++) {
            Kind k = StationLayout.kindAt(SEED, wx, floorY + dy, wz);
            if (k == Kind.AIR || k == Kind.CORRIDOR_LIGHT || k == Kind.LADDER) {
                sawAir = true;
            }
        }
        assertTrue(sawAir, "corridor deck should have open headroom");
        assertEquals(Kind.CEILING, StationLayout.kindAt(SEED, wx, floorY + StationLayout.DECK - 1, wz));
    }

    @Test
    void corridorsRunContinuouslyAcrossCells() {
        // Walk a corridor lane (local x==1) across many cells at head height on
        // a deck; every column is open (air/light/ladder), never solid wall.
        int floorY = 96 - (96 % StationLayout.DECK);
        int y = floorY + 2;
        int wx = 1;
        Set<Kind> open = EnumSet.of(Kind.AIR, Kind.CORRIDOR_LIGHT, Kind.LADDER, Kind.DOORWAY,
            Kind.REACTOR_CORE, Kind.REACTOR_CASING); // reactor wells legitimately interrupt
        int solidHits = 0;
        for (int wz = -120; wz < 120; wz++) {
            Kind k = StationLayout.kindAt(SEED, wx, y, wz);
            if (!open.contains(k)) {
                solidHits++;
            }
        }
        // Corridors are mostly clear; allow a little for reactor edges.
        assertTrue(solidHits < 24, "corridor lane too obstructed: " + solidHits + " solid columns");
    }

    @Test
    void everyRoomTypeAppears() {
        Set<StationLayout.RoomType> seen = EnumSet.noneOf(StationLayout.RoomType.class);
        for (int cx = 0; cx < 40; cx++) {
            for (int cz = 0; cz < 40; cz++) {
                seen.add(StationLayout.roomType(SEED, cx, cz, 12));
            }
        }
        assertEquals(EnumSet.allOf(StationLayout.RoomType.class), seen);
    }

    @Test
    void supplyRoomsYieldValuables() {
        // Scan many supply rooms; confirm mineable blocks (incl. some diamond)
        // actually appear in the racks.
        boolean sawDiamond = false;
        int valuables = 0;
        int floorY = 96 - (96 % StationLayout.DECK);
        for (int cx = 0; cx < 60 && !sawDiamond; cx++) {
            for (int cz = 0; cz < 60; cz++) {
                if (StationLayout.roomType(SEED, cx, cz, 12) != StationLayout.RoomType.SUPPLY) {
                    continue;
                }
                int baseX = cx * StationLayout.CELL;
                int baseZ = cz * StationLayout.CELL;
                for (int lx = 5; lx <= 9; lx++) {
                    for (int lz = 5; lz <= 9; lz++) {
                        for (int dy = 1; dy <= 3; dy++) {
                            Kind k = StationLayout.kindAt(SEED, baseX + lx, floorY + dy, baseZ + lz);
                            if (k == Kind.SUPPLY_DIAMOND) {
                                sawDiamond = true;
                            }
                            if (k == Kind.SUPPLY_IRON || k == Kind.SUPPLY_GOLD
                                || k == Kind.SUPPLY_DIAMOND || k == Kind.SUPPLY_REDSTONE) {
                                valuables++;
                            }
                        }
                    }
                }
            }
        }
        assertTrue(valuables > 0, "supply rooms produced no valuables");
        assertTrue(sawDiamond, "no diamond block found across many supply rooms");
    }

    @Test
    void detentionRoomsHaveBars() {
        int floorY = 96 - (96 % StationLayout.DECK);
        boolean sawBars = false;
        for (int cx = 0; cx < 80 && !sawBars; cx++) {
            for (int cz = 0; cz < 80; cz++) {
                if (StationLayout.roomType(SEED, cx, cz, 12) != StationLayout.RoomType.DETENTION) {
                    continue;
                }
                int baseX = cx * StationLayout.CELL;
                int baseZ = cz * StationLayout.CELL;
                for (int lx = 4; lx <= 10 && !sawBars; lx++) {
                    for (int lz = 4; lz <= 10; lz++) {
                        if (StationLayout.kindAt(SEED, baseX + lx, floorY + 2, baseZ + lz) == Kind.BARS) {
                            sawBars = true;
                            break;
                        }
                    }
                }
            }
        }
        assertTrue(sawBars, "detention rooms had no iron bars");
    }

    @Test
    void arrivalLandsOnAnOpenCorridorFloor() {
        for (int i = 0; i < 300; i++) {
            int wx = i * 29 - 3000;
            int wz = i * 47 - 2000;
            int[] spot = StationLayout.arrivalPos(SEED, wx, wz);
            int sx = spot[0];
            int y = spot[1];
            int sz = spot[2];
            assertTrue(y > StationLayout.BOTTOM_Y && y < StationLayout.TOP_Y,
                "arrival out of station bounds at " + wx + "," + wz + ": " + y);
            // Feet in open air, a solid floor directly beneath, headroom above.
            assertEquals(Kind.AIR, StationLayout.kindAt(SEED, sx, y, sz),
                "arrival not in open air at " + sx + "," + y + "," + sz);
            assertEquals(Kind.FLOOR, StationLayout.kindAt(SEED, sx, y - 1, sz),
                "no floor under arrival at " + sx + "," + (y - 1) + "," + sz);
            Kind head = StationLayout.kindAt(SEED, sx, y + 1, sz);
            assertNotEquals(Kind.HULL, head, "no headroom at arrival " + sx + "," + y + "," + sz);
        }
    }

    @Test
    void reactorWellsHaveGlowingCores() {
        int probe = 0;
        int cell = 0;
        while (!StationLayout.isReactorCell(SEED, cell, cell) && probe < 4000) {
            cell++;
            probe++;
        }
        assertTrue(probe < 4000, "no reactor cell found in probe range");
        int baseX = cell * StationLayout.CELL + StationLayout.CELL / 2;
        int baseZ = cell * StationLayout.CELL + StationLayout.CELL / 2;
        assertEquals(Kind.REACTOR_CORE, StationLayout.kindAt(SEED, baseX, 100, baseZ));
    }
}
