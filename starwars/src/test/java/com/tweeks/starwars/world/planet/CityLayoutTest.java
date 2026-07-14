package com.tweeks.starwars.world.planet;

import com.tweeks.starwars.world.planet.CityLayout.CellSpec;
import com.tweeks.starwars.world.planet.CityLayout.CellType;
import com.tweeks.starwars.world.planet.CityLayout.Kind;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CityLayoutTest {
    private static final long SEED = 0xC0FFEE_5EEDL;

    private static final Set<Kind> STREET_SURFACE =
        EnumSet.of(Kind.STREET, Kind.STREET_LINE, Kind.SIDEWALK);

    @Test
    void deterministicForSameSeed() {
        for (int i = 0; i < 50; i++) {
            int wx = i * 37 - 500;
            int wz = i * 91 - 700;
            assertEquals(CityLayout.topY(SEED, wx, wz), CityLayout.topY(SEED, wx, wz));
            for (int y = 0; y <= 130; y += 13) {
                assertEquals(CityLayout.kindAt(SEED, wx, y, wz), CityLayout.kindAt(SEED, wx, y, wz));
            }
        }
        assertEquals(CityLayout.cellSpec(SEED, 3, -7), CityLayout.cellSpec(SEED, 3, -7));
    }

    @Test
    void differentSeedsChangeTheCity() {
        int differences = 0;
        for (int cell = 0; cell < 100; cell++) {
            if (!CityLayout.cellSpec(SEED, cell, 0).equals(CityLayout.cellSpec(SEED + 1, cell, 0))) {
                differences++;
            }
        }
        assertTrue(differences > 10, "expected seed to reshape the city, got " + differences + " differing cells");
    }

    @Test
    void streetLatticeIsContinuousAcrossCells() {
        // Walk 300 blocks straight down a north-south road lane (lx == 2),
        // crossing many cell borders — every surface block stays street-kind.
        for (int wz = -150; wz < 150; wz++) {
            Kind kind = CityLayout.kindAt(SEED, 2, CityLayout.STREET_Y, wz);
            assertTrue(STREET_SURFACE.contains(kind), "road broke at wz=" + wz + ": " + kind);
        }
        // And an east-west road lane.
        for (int wx = -150; wx < 150; wx++) {
            Kind kind = CityLayout.kindAt(SEED, wx, CityLayout.STREET_Y, 2);
            assertTrue(STREET_SURFACE.contains(kind), "road broke at wx=" + wx + ": " + kind);
        }
    }

    @Test
    void specsStayInBounds() {
        for (int i = 0; i < 400; i++) {
            CellSpec spec = CityLayout.cellSpec(SEED, i * 13 - 2600, i * 7 - 1400);
            assertTrue(spec.height() >= CityLayout.MIN_TOWER && spec.height() <= CityLayout.MAX_TOWER,
                "height out of range: " + spec.height());
            assertTrue(spec.palette() >= 0 && spec.palette() < 3);
            assertTrue(spec.margin() >= 1 && spec.margin() <= 2);
            if (spec.spire() != 0) {
                assertTrue(spec.height() >= 90, "spire on a short tower");
                assertTrue(spec.spire() >= 3 && spec.spire() <= 6);
            }
        }
    }

    @Test
    void cellTypeDistributionIsRoughlyAsDesigned() {
        int plazas = 0;
        int pads = 0;
        int total = 5000;
        for (int i = 0; i < total; i++) {
            CellType type = CityLayout.cellSpec(SEED, i % 71, i / 71).type();
            if (type == CellType.PLAZA) {
                plazas++;
            } else if (type == CellType.PAD) {
                pads++;
            }
        }
        double plazaShare = plazas / (double) total;   // designed ≈ 17/221 ≈ 0.077
        double padShare = pads / (double) total;       // designed ≈ 13/221 ≈ 0.059
        assertTrue(plazaShare > 0.04 && plazaShare < 0.12, "plaza share " + plazaShare);
        assertTrue(padShare > 0.03 && padShare < 0.10, "pad share " + padShare);
    }

    @Test
    void topYAlwaysSitsOnTheActualSurface() {
        for (int i = 0; i < 500; i++) {
            int wx = i * 53 - 13000;
            int wz = i * 29 - 7000;
            int top = CityLayout.topY(SEED, wx, wz);
            assertTrue(top >= CityLayout.STREET_Y, "top below street at " + wx + "," + wz);
            assertNotEquals(Kind.AIR, CityLayout.kindAt(SEED, wx, top, wz),
                "air at reported top " + wx + "," + top + "," + wz);
            assertEquals(Kind.AIR, CityLayout.kindAt(SEED, wx, top + 1, wz),
                "solid above reported top " + wx + "," + top + "," + wz);
        }
    }

    @Test
    void foundationAndBedrockUnderEverything() {
        for (int i = 0; i < 100; i++) {
            int wx = i * 97 - 5000;
            int wz = i * 41 - 2000;
            assertEquals(Kind.BEDROCK, CityLayout.kindAt(SEED, wx, CityLayout.BOTTOM_Y, wz));
            assertEquals(Kind.FOUNDATION, CityLayout.kindAt(SEED, wx, CityLayout.STREET_Y - 10, wz));
        }
    }

    private int findCell(CellType wanted) {
        for (int cell = 0; cell < 4000; cell++) {
            if (CityLayout.cellSpec(SEED, cell, cell * 3 + 1).type() == wanted) {
                return cell;
            }
        }
        throw new AssertionError("no " + wanted + " cell found in probe range");
    }

    @Test
    void towersHaveWallsWindowsAndRoofs() {
        int cell = findCell(CellType.TOWER);
        CellSpec spec = CityLayout.cellSpec(SEED, cell, cell * 3 + 1);
        int baseX = cell * CityLayout.CELL + CityLayout.ROAD_W;
        int baseZ = (cell * 3 + 1) * CityLayout.CELL + CityLayout.ROAD_W;
        int m = spec.margin();
        int roofY = CityLayout.STREET_Y + spec.height();

        // Corner pillar is solid wall at mid height.
        int midY = CityLayout.STREET_Y + spec.height() / 2;
        assertEquals(Kind.WALL, CityLayout.kindAt(SEED, baseX + m, midY, baseZ + m));

        // Roof across the footprint centre.
        assertEquals(Kind.ROOF, CityLayout.kindAt(SEED, baseX + CityLayout.PLOT / 2, roofY, baseZ + CityLayout.PLOT / 2));

        // The facade contains windows somewhere between floors.
        boolean sawWindow = false;
        for (int t = m; t <= CityLayout.PLOT - 1 - m && !sawWindow; t++) {
            for (int y = CityLayout.STREET_Y + 1; y < roofY; y++) {
                Kind kind = CityLayout.kindAt(SEED, baseX + t, y, baseZ + m);
                if (kind == Kind.WINDOW || kind == Kind.WINDOW_LIT) {
                    sawWindow = true;
                    break;
                }
            }
        }
        assertTrue(sawWindow, "tower facade has no windows");

        // Setback strip outside the footprint is walkable sidewalk.
        assertEquals(Kind.SIDEWALK, CityLayout.kindAt(SEED, baseX, CityLayout.STREET_Y, baseZ));
        assertEquals(Kind.AIR, CityLayout.kindAt(SEED, baseX, CityLayout.STREET_Y + 1, baseZ));
    }

    @Test
    void someWindowsGlow() {
        // Across many towers, lit windows must show up (≈30% of panes).
        int lit = 0;
        int dark = 0;
        for (int cell = 0; cell < 60; cell++) {
            CellSpec spec = CityLayout.cellSpec(SEED, cell, 0);
            if (spec.type() != CellType.TOWER) {
                continue;
            }
            int baseX = cell * CityLayout.CELL + CityLayout.ROAD_W;
            int baseZ = CityLayout.ROAD_W;
            int m = spec.margin();
            for (int t = m; t <= CityLayout.PLOT - 1 - m; t++) {
                for (int y = CityLayout.STREET_Y + 1; y < CityLayout.STREET_Y + spec.height(); y++) {
                    Kind kind = CityLayout.kindAt(SEED, baseX + t, y, baseZ + m);
                    if (kind == Kind.WINDOW_LIT) {
                        lit++;
                    } else if (kind == Kind.WINDOW) {
                        dark++;
                    }
                }
            }
        }
        assertTrue(lit > 0, "no lit windows in 60 cells of skyline");
        assertTrue(dark > lit, "lit windows should be the minority");
    }

    @Test
    void plazaHasMonumentAndLamps() {
        int cell = findCell(CellType.PLAZA);
        int baseX = cell * CityLayout.CELL + CityLayout.ROAD_W;
        int baseZ = (cell * 3 + 1) * CityLayout.CELL + CityLayout.ROAD_W;
        int c = CityLayout.PLOT / 2;
        assertEquals(Kind.MONUMENT, CityLayout.kindAt(SEED, baseX + c, CityLayout.STREET_Y + 1, baseZ + c));
        assertEquals(Kind.LAMP_LIGHT, CityLayout.kindAt(SEED, baseX + c, CityLayout.STREET_Y + 5, baseZ + c));
        assertEquals(Kind.LAMP_LIGHT, CityLayout.kindAt(SEED, baseX + 4, CityLayout.STREET_Y + 3, baseZ + 4));
        Kind floor = CityLayout.kindAt(SEED, baseX + 2, CityLayout.STREET_Y, baseZ + 5);
        assertTrue(floor == Kind.PLAZA_FLOOR || floor == Kind.PLAZA_ACCENT);
    }

    @Test
    void landingPadHasElevatedDeckWithMarkings() {
        int cell = findCell(CellType.PAD);
        int baseX = cell * CityLayout.CELL + CityLayout.ROAD_W;
        int baseZ = (cell * 3 + 1) * CityLayout.CELL + CityLayout.ROAD_W;
        int c = CityLayout.PLOT / 2;
        int deckY = CityLayout.STREET_Y + CityLayout.PAD_RISE;
        assertEquals(Kind.PAD_MARK, CityLayout.kindAt(SEED, baseX + c, deckY, baseZ + c));
        assertEquals(Kind.PAD_DECK, CityLayout.kindAt(SEED, baseX + c + 2, deckY, baseZ + c));
        assertEquals(Kind.PAD_MARK, CityLayout.kindAt(SEED, baseX + c + 5, deckY, baseZ + c));
        // Under the deck: open air except the pylon.
        assertEquals(Kind.AIR, CityLayout.kindAt(SEED, baseX + 2, CityLayout.STREET_Y + 4, baseZ + 2));
        assertEquals(Kind.PYLON, CityLayout.kindAt(SEED, baseX + c, CityLayout.STREET_Y + 4, baseZ + c));
    }
}
