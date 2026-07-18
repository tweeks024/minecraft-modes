package com.tweeks.starwars.world.planet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PalaceLayoutTest {

    private static int stairTop(int lz) {
        for (int y = PalaceLayout.THRONE_FLOOR_Y + 10; y >= PalaceLayout.STREET_Y; y--) {
            if (PalaceLayout.kindAt(PalaceLayout.CENTER_X, y, lz) == PalaceLayout.Kind.STAIR) return y;
        }
        return -1;
    }

    @Test
    void deterministic() {
        for (int lz = 0; lz < PalaceLayout.SIZE_Z; lz += 3) {
            for (int lx = 0; lx < PalaceLayout.SIZE_X; lx += 3) {
                assertEquals(PalaceLayout.kindAt(lx, PalaceLayout.THRONE_FLOOR_Y, lz),
                    PalaceLayout.kindAt(lx, PalaceLayout.THRONE_FLOOR_Y, lz));
            }
        }
    }

    @Test
    void stairwayClimbsUnbrokenToTheThroneFloor() {
        int prev = -1, steps = 0;
        for (int lz = 0; lz < PalaceLayout.SIZE_Z; lz++) {
            int top = stairTop(lz);
            if (top < 0) continue;
            if (prev >= 0) {
                assertEquals(prev + 1, top, "stairway must rise exactly one block per row at lz=" + lz);
            }
            prev = top;
            steps++;
        }
        assertTrue(steps >= 20, "expected a long grand stairway, got " + steps + " steps");
        assertEquals(PalaceLayout.THRONE_FLOOR_Y - 1, prev,
            "the stairway should top out just below the throne floor (one step into the pavilion)");
    }

    @Test
    void throneSeatIsTheMarkerCellOnASolidDais() {
        int lx = PalaceLayout.throneX() - PalaceLayout.ORIGIN_X;
        int lz = PalaceLayout.throneZ() - PalaceLayout.ORIGIN_Z;
        int y = PalaceLayout.throneY();
        assertEquals(PalaceLayout.Kind.THRONE_MARKER, PalaceLayout.kindAt(lx, y, lz),
            "the spawner seat must land on the throne marker");
        assertEquals(PalaceLayout.Kind.DAIS, PalaceLayout.kindAt(lx, y - 1, lz),
            "the Emperor must stand on a solid dais");
    }

    @Test
    void exactlyOneThroneMarker() {
        int count = 0;
        for (int lx = 0; lx < PalaceLayout.SIZE_X; lx++) {
            for (int lz = 0; lz < PalaceLayout.SIZE_Z; lz++) {
                for (int y = PalaceLayout.STREET_Y; y <= PalaceLayout.THRONE_FLOOR_Y + 12; y++) {
                    if (PalaceLayout.kindAt(lx, y, lz) == PalaceLayout.Kind.THRONE_MARKER) count++;
                }
            }
        }
        assertEquals(1, count);
    }

    @Test
    void pavilionIsHollowWithFloorThroneAndWindows() {
        boolean air = false, floor = false, throne = false, window = false;
        for (int lx = 0; lx < PalaceLayout.SIZE_X; lx++) {
            for (int lz = 0; lz < PalaceLayout.SIZE_Z; lz++) {
                for (int y = PalaceLayout.THRONE_FLOOR_Y; y <= PalaceLayout.THRONE_FLOOR_Y + 8; y++) {
                    switch (PalaceLayout.kindAt(lx, y, lz)) {
                        case AIR -> air = true;
                        case FLOOR -> floor = true;
                        case THRONE -> throne = true;
                        case WINDOW -> window = true;
                        default -> { }
                    }
                }
            }
        }
        assertTrue(air, "pavilion needs hollow space");
        assertTrue(floor, "pavilion needs a floor");
        assertTrue(throne, "pavilion needs a throne");
        assertTrue(window, "pavilion needs windows");
    }

    @Test
    void aSpireCapsTheThrone() {
        int lz = PalaceLayout.throneZ() - PalaceLayout.ORIGIN_Z;
        boolean spire = false;
        for (int y = PalaceLayout.THRONE_FLOOR_Y + 8; y <= PalaceLayout.THRONE_FLOOR_Y + 20; y++) {
            if (PalaceLayout.kindAt(PalaceLayout.CENTER_X, y, lz) == PalaceLayout.Kind.SPIRE) spire = true;
        }
        assertTrue(spire, "a spire should rise over the throne");
    }

    @Test
    void footprintContainsAndExcludes() {
        assertTrue(PalaceLayout.contains(PalaceLayout.throneX(), PalaceLayout.throneZ()));
        assertTrue(PalaceLayout.contains(PalaceLayout.ORIGIN_X, PalaceLayout.ORIGIN_Z));
        assertFalse(PalaceLayout.contains(PalaceLayout.ORIGIN_X - 1, PalaceLayout.ORIGIN_Z));
        assertFalse(PalaceLayout.contains(PalaceLayout.ORIGIN_X + PalaceLayout.SIZE_X, PalaceLayout.ORIGIN_Z));
        assertFalse(PalaceLayout.contains(PalaceLayout.ORIGIN_X, PalaceLayout.ORIGIN_Z + PalaceLayout.SIZE_Z));
    }

    @Test
    void topYNeverBelowStreet() {
        for (int lx = 0; lx < PalaceLayout.SIZE_X; lx++) {
            for (int lz = 0; lz < PalaceLayout.SIZE_Z; lz++) {
                assertTrue(PalaceLayout.topY(lx, lz) >= PalaceLayout.STREET_Y,
                    "topY below the street at (" + lx + "," + lz + ")");
            }
        }
    }
}
