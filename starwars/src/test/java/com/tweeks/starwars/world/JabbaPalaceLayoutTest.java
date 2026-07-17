package com.tweeks.starwars.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JabbaPalaceLayoutTest {

    private static final List<JabbaPalaceLayout.Placement> P = JabbaPalaceLayout.placements();

    private static long count(JabbaPalaceLayout.Kind k) {
        return P.stream().filter(p -> p.kind() == k).count();
    }

    private static JabbaPalaceLayout.Placement one(JabbaPalaceLayout.Kind k) {
        var l = P.stream().filter(p -> p.kind() == k).toList();
        assertEquals(1, l.size(), "expected exactly one " + k);
        return l.getFirst();
    }

    /** The single placement at this cell (the layout dedupes to one per cell). */
    private static JabbaPalaceLayout.Kind at(int x, int y, int z) {
        return P.stream().filter(p -> p.dx() == x && p.dy() == y && p.dz() == z)
            .map(JabbaPalaceLayout.Placement::kind).findFirst().orElse(null);
    }

    private static boolean open(JabbaPalaceLayout.Kind k) {
        return k == JabbaPalaceLayout.Kind.AIR || k == JabbaPalaceLayout.Kind.GATE_AIR || k == null;
    }

    @Test
    void allPlacementsInsideDeclaredBounds() {
        for (var p : P) {
            assertTrue(p.dx() >= 0 && p.dx() < JabbaPalaceLayout.SIZE_X, p.toString());
            assertTrue(p.dy() >= 0 && p.dy() < JabbaPalaceLayout.SIZE_Y, p.toString());
            assertTrue(p.dz() >= 0 && p.dz() < JabbaPalaceLayout.SIZE_Z, p.toString());
        }
    }

    @Test
    void placementsAreDeterministic() {
        assertEquals(P, JabbaPalaceLayout.placements());
    }

    @Test
    void exactlyOneRancorDownInThePit() {
        var r = one(JabbaPalaceLayout.Kind.RANCOR);
        assertTrue(r.dz() > JabbaPalaceLayout.SIZE_Z / 2, "rancor belongs deep in the pit: " + r);
        assertTrue(r.dy() <= 3, "rancor stands on the pit floor: " + r);
    }

    @Test
    void exactlyOneJabbaOnTheDais() {
        var j = one(JabbaPalaceLayout.Kind.JABBA);
        assertTrue(j.dy() >= 8, "Jabba holds court on the upper level: " + j);
        assertTrue(j.dz() > JabbaPalaceLayout.SIZE_Z / 2, "the dais is at the back: " + j);
    }

    @Test
    void rancorAndTreasureSitBeneathJabba() {
        var r = one(JabbaPalaceLayout.Kind.RANCOR);
        var j = one(JabbaPalaceLayout.Kind.JABBA);
        var c = one(JabbaPalaceLayout.Kind.CHEST);
        assertTrue(j.dy() > r.dy(), "Jabba is above the pit");
        assertTrue(c.dy() < j.dy(), "the treasure lies below Jabba");
        assertEquals(JabbaPalaceLayout.CENTER_X, c.dx(), "treasure on the central axis");
    }

    @Test
    void rancorHasHeadroomToTheGrate() {
        var r = one(JabbaPalaceLayout.Kind.RANCOR);
        for (int dy = 1; dy <= 4; dy++) {
            assertTrue(open(at(r.dx(), r.dy() + dy, r.dz())), "no headroom at +" + dy);
        }
    }

    @Test
    void grateFloorsTheThroneRoomOverThePit() {
        assertTrue(count(JabbaPalaceLayout.Kind.GRATE) >= 20,
            "expected a sizable throne-floor grate");
        var floors = P.stream().filter(p -> p.kind() == JabbaPalaceLayout.Kind.GRATE)
            .map(JabbaPalaceLayout.Placement::dy).distinct().toList();
        assertEquals(1, floors.size(), "the grate is a single floor plane");
        assertTrue(floors.getFirst() > one(JabbaPalaceLayout.Kind.RANCOR).dy(),
            "the grate looks down onto the rancor");
    }

    @Test
    void twoOneWideDoorwaysCageTheRancor() {
        // Side doorways through the inner wall pass the player but not the
        // 2.4-wide rancor: each opening must be strictly one block wide.
        var doorways = P.stream().filter(p -> p.kind() == JabbaPalaceLayout.Kind.GATE_AIR
            && p.dz() > 0 && (p.dx() == 2 || p.dx() == JabbaPalaceLayout.SIZE_X - 3)).toList();
        assertFalse(doorways.isEmpty(), "expected side doorways into the pit");
        int wallZ = doorways.getFirst().dz();
        for (int dx : new int[]{2, JabbaPalaceLayout.SIZE_X - 3}) {
            assertTrue(P.stream().anyMatch(p -> p.kind() == JabbaPalaceLayout.Kind.GATE_AIR
                && p.dx() == dx && p.dz() == wallZ), "doorway open at x=" + dx);
            for (int nb : new int[]{dx - 1, dx + 1}) {
                for (int y = 2; y <= 4; y++) {
                    assertFalse(open(at(nb, y, wallZ)), "doorway wider than one block at x=" + nb);
                }
            }
        }
    }

    @Test
    void theFrontGateOpens() {
        assertTrue(P.stream().anyMatch(p -> p.kind() == JabbaPalaceLayout.Kind.GATE_AIR && p.dz() == 0),
            "a gateway should pierce the front wall");
    }

    @Test
    void aDomeCapsTheStructure() {
        assertTrue(count(JabbaPalaceLayout.Kind.CUT) > 50, "expected a cut-sandstone dome");
        int maxY = P.stream().mapToInt(JabbaPalaceLayout.Placement::dy).max().orElseThrow();
        assertTrue(P.stream().anyMatch(p -> p.kind() == JabbaPalaceLayout.Kind.CUT && p.dy() == maxY),
            "the dome reaches the top of the box");
    }

    @Test
    void bonesLitterThePitFloor() {
        assertTrue(count(JabbaPalaceLayout.Kind.BONE) >= 3);
    }
}
