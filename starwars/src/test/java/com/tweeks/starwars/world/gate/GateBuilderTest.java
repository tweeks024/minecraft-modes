package com.tweeks.starwars.world.gate;

import com.tweeks.starwars.world.gate.GateBuilder.Kind;
import com.tweeks.starwars.world.gate.GateBuilder.Placement;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GateBuilderTest {

    private static Set<Long> positionsOf(List<Placement> placements, Kind kind) {
        Set<Long> set = new HashSet<>();
        for (Placement p : placements) {
            if (p.kind() == kind) {
                set.add(pack(p.dx(), p.dy(), p.dz()));
            }
        }
        return set;
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x + 512) << 40) | ((long) (y + 512) << 20) | (z + 512);
    }

    @Test
    void componentCounts() {
        List<Placement> placements = GateBuilder.arrivalGate(true);
        assertEquals(6, positionsOf(placements, Kind.PORTAL).size(), "2x3 film");
        assertEquals(14, positionsOf(placements, Kind.FRAME).size(), "4x5 ring");
        assertEquals(14, positionsOf(placements, Kind.PLATFORM).size(), "landing platform");
        assertTrue(positionsOf(placements, Kind.CLEAR).size() > 20, "breathing room");
    }

    @Test
    void kindsNeverOverlap() {
        List<Placement> placements = GateBuilder.arrivalGate(true);
        Set<Long> all = new HashSet<>();
        int solidCount = 0;
        for (Placement p : placements) {
            long key = pack(p.dx(), p.dy(), p.dz());
            if (p.kind() != Kind.CLEAR) {
                solidCount++;
                assertTrue(all.add(key), "duplicate placement at " + p);
            }
        }
        assertEquals(6 + 14 + 14, solidCount);
    }

    @Test
    void stampOrderIsClearPlatformFrameThenFilm() {
        List<Placement> placements = GateBuilder.arrivalGate(false);
        int lastClear = -1;
        int firstPlatform = Integer.MAX_VALUE;
        int lastPlatform = -1;
        int firstFrame = Integer.MAX_VALUE;
        int lastFrame = -1;
        int firstPortal = Integer.MAX_VALUE;
        for (int i = 0; i < placements.size(); i++) {
            switch (placements.get(i).kind()) {
                case CLEAR -> lastClear = i;
                case PLATFORM -> {
                    firstPlatform = Math.min(firstPlatform, i);
                    lastPlatform = i;
                }
                case FRAME -> {
                    firstFrame = Math.min(firstFrame, i);
                    lastFrame = i;
                }
                case PORTAL -> firstPortal = Math.min(firstPortal, i);
            }
        }
        assertTrue(lastClear < firstPlatform, "clears before platform");
        assertTrue(lastPlatform < firstFrame, "platform before frame");
        assertTrue(lastFrame < firstPortal, "frame ring complete before film");
    }

    @Test
    void everyGroundLevelCellIsSupported() {
        List<Placement> placements = GateBuilder.arrivalGate(true);
        Set<Long> support = new HashSet<>();
        support.addAll(positionsOf(placements, Kind.PLATFORM));
        support.addAll(positionsOf(placements, Kind.FRAME));
        for (Placement p : placements) {
            if (p.dy() == 0 && (p.kind() == Kind.PORTAL || p.kind() == Kind.CLEAR)) {
                assertTrue(support.contains(pack(p.dx(), -1, p.dz())),
                    "unsupported walkway cell at " + p.dx() + "," + p.dz());
            }
        }
    }

    @Test
    void filmSitsInsideTheRing() {
        List<Placement> placements = GateBuilder.arrivalGate(true);
        Set<Long> frame = positionsOf(placements, Kind.FRAME);
        for (Placement p : placements) {
            if (p.kind() != Kind.PORTAL) {
                continue;
            }
            assertEquals(0, p.dz(), "film out of plane");
            // Each film cell reaches frame (or more film) in all 4 in-plane
            // directions eventually; check the immediate ring bounds instead:
            assertTrue(p.dx() >= 0 && p.dx() < GateBuilder.WIDTH);
            assertTrue(p.dy() >= 0 && p.dy() < GateBuilder.HEIGHT);
        }
        // Ring closes around the film on all four sides.
        for (int a = 0; a < GateBuilder.WIDTH; a++) {
            assertTrue(frame.contains(pack(a, -1, 0)), "no sill under film column " + a);
            assertTrue(frame.contains(pack(a, GateBuilder.HEIGHT, 0)), "no lintel over film column " + a);
        }
        for (int dy = 0; dy < GateBuilder.HEIGHT; dy++) {
            assertTrue(frame.contains(pack(-1, dy, 0)), "no left jamb at row " + dy);
            assertTrue(frame.contains(pack(GateBuilder.WIDTH, dy, 0)), "no right jamb at row " + dy);
        }
    }

    @Test
    void axisSwapMirrorsCoordinates() {
        List<Placement> alongX = GateBuilder.arrivalGate(true);
        List<Placement> alongZ = GateBuilder.arrivalGate(false);
        assertEquals(alongX.size(), alongZ.size());
        for (int i = 0; i < alongX.size(); i++) {
            Placement x = alongX.get(i);
            Placement z = alongZ.get(i);
            assertEquals(x.kind(), z.kind());
            assertEquals(x.dx(), z.dz(), "swap dx→dz at " + i);
            assertEquals(x.dz(), z.dx(), "swap dz→dx at " + i);
            assertEquals(x.dy(), z.dy());
        }
    }
}
