package com.tweeks.starwars.world.gate;

import com.tweeks.starwars.world.planet.CityLayout;
import com.tweeks.starwars.world.planet.Planet;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalLinkTest {

    @Test
    void snapToLaneFindsNearestStreetLane() {
        // Lanes sit at local coordinate 2 of every 24-block cell.
        assertEquals(2, PortalLink.snapToLane(0));
        assertEquals(2, PortalLink.snapToLane(2));
        assertEquals(2, PortalLink.snapToLane(13));
        assertEquals(26, PortalLink.snapToLane(15));
        assertEquals(26, PortalLink.snapToLane(26));
        assertEquals(2, PortalLink.snapToLane(-1));
        assertEquals(-22, PortalLink.snapToLane(-11));
    }

    @Test
    void snappedLaneIsAlwaysAStreetColumn() {
        for (int coord = -100; coord <= 100; coord += 7) {
            int lane = PortalLink.snapToLane(coord);
            assertEquals(2, Math.floorMod(lane, CityLayout.CELL), "lane " + lane + " from " + coord);
        }
    }

    @Test
    void snapToStreetMovesOnlyTheCloserAxis() {
        // x=5 is 3 from its lane; z=11 is 9 from its lane → snap x, keep z.
        long snapped = PortalLink.snapToStreet(5, 11);
        assertEquals(2, PortalLink.unpackX(snapped));
        assertEquals(11, PortalLink.unpackZ(snapped));
        // Mirror case.
        long snapped2 = PortalLink.snapToStreet(11, 5);
        assertEquals(11, PortalLink.unpackX(snapped2));
        assertEquals(2, PortalLink.unpackZ(snapped2));
    }

    @Test
    void packedCoordinatesSurviveNegatives() {
        int x = -1000;
        int z = -500;
        long packed = PortalLink.snapToStreet(x, z);
        int outX = PortalLink.unpackX(packed);
        int outZ = PortalLink.unpackZ(packed);
        // Exactly one axis moved, and the moved one landed on a street lane.
        boolean xMoved = outX != x;
        boolean zMoved = outZ != z;
        assertTrue(xMoved ^ zMoved, "exactly one axis should snap: " + outX + "," + outZ);
        int moved = xMoved ? outX : outZ;
        assertEquals(2, Math.floorMod(moved, CityLayout.CELL));
        // Values survive the long round-trip with sign intact.
        assertTrue(outX < 0 && outZ < 0);
    }

    @Test
    void nearestRecordHonoursRadiusAndDistance() {
        PortalRecords.GateRecord near = new PortalRecords.GateRecord(new BlockPos(10, 64, 0), true, Planet.TATOOINE);
        PortalRecords.GateRecord far = new PortalRecords.GateRecord(new BlockPos(80, 64, 0), true, Planet.HOME);
        PortalRecords.GateRecord tooFar = new PortalRecords.GateRecord(new BlockPos(500, 64, 0), true, Planet.ANDOR);
        List<PortalRecords.GateRecord> records = List.of(far, tooFar, near);

        Optional<PortalRecords.GateRecord> best =
            PortalRecords.nearest(records, new BlockPos(0, 100, 0), PortalLink.REUSE_RADIUS);
        assertTrue(best.isPresent());
        assertEquals(near, best.get());

        // Height difference is ignored — matching is horizontal.
        Optional<PortalRecords.GateRecord> sameColumn =
            PortalRecords.nearest(List.of(tooFar), new BlockPos(500, 0, 0), PortalLink.REUSE_RADIUS);
        assertEquals(tooFar, sameColumn.orElseThrow());

        // Outside the radius: nothing.
        assertTrue(PortalRecords.nearest(List.of(tooFar), new BlockPos(0, 64, 0), PortalLink.REUSE_RADIUS).isEmpty());
    }

    @Test
    void dropPointCentersOnTheFilm() {
        BlockPos origin = new BlockPos(100, 70, -40);
        assertEquals(101.0, PortalLink.dropPoint(origin, true).x());
        assertEquals(-39.5, PortalLink.dropPoint(origin, true).z());
        assertEquals(100.5, PortalLink.dropPoint(origin, false).x());
        assertEquals(-39.0, PortalLink.dropPoint(origin, false).z());
        assertEquals(70.0, PortalLink.dropPoint(origin, true).y());
    }
}
