package com.tweeks.starwars.world.gate;

import com.tweeks.starwars.world.planet.PalaceLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression guard for the Coruscant arrival fix: a hyperspace gate must never
 * be stamped inside the solid Imperial Palace, no matter where the traveler
 * jumps from.
 */
class PortalLinkArrivalTest {

    @Test
    void coruscantArrivalNeverLandsInsideThePalace() {
        // Sweep approximate arrival points densely across and around the whole
        // palace footprint — every resolved lane must be outside it.
        for (int ax = PalaceLayout.ORIGIN_X - 12; ax <= PalaceLayout.ORIGIN_X + PalaceLayout.SIZE_X + 12; ax += 2) {
            for (int az = PalaceLayout.ORIGIN_Z - 12; az <= PalaceLayout.ORIGIN_Z + PalaceLayout.SIZE_Z + 12; az += 2) {
                long lane = PortalLink.coruscantArrivalLane(ax, az);
                int x = PortalLink.unpackX(lane);
                int z = PortalLink.unpackZ(lane);
                assertFalse(PalaceLayout.contains(x, z),
                    "arrival (" + x + "," + z + ") from approx (" + ax + "," + az + ") is inside the palace");
            }
        }
    }

    @Test
    void arrivalAlwaysLandsOnAStreetLane() {
        // The gate needs a road to stand on: one of the two coordinates must be
        // on the street lattice, even after being pushed clear of the palace.
        for (int ax = PalaceLayout.ORIGIN_X - 5; ax <= PalaceLayout.ORIGIN_X + PalaceLayout.SIZE_X + 5; ax += 3) {
            for (int az = PalaceLayout.ORIGIN_Z - 5; az <= PalaceLayout.ORIGIN_Z + PalaceLayout.SIZE_Z + 5; az += 3) {
                long lane = PortalLink.coruscantArrivalLane(ax, az);
                int x = PortalLink.unpackX(lane);
                int z = PortalLink.unpackZ(lane);
                assertTrue(PortalLink.snapToLane(x) == x || PortalLink.snapToLane(z) == z,
                    "arrival (" + x + "," + z + ") is not on a street lane");
            }
        }
    }

    @Test
    void arrivalsOutsideThePalaceAreLeftAlone() {
        // A normal street jump well south of the palace should snap normally
        // and not be relocated.
        long lane = PortalLink.coruscantArrivalLane(0, PalaceLayout.ORIGIN_Z - 40);
        int z = PortalLink.unpackZ(lane);
        assertTrue(z < PalaceLayout.ORIGIN_Z, "a southern arrival should stay south");
        assertFalse(PalaceLayout.contains(PortalLink.unpackX(lane), z));
    }
}
