package com.tweeks.wildwest.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CannonFireGeometryTest {

    @Test
    void facingNorth_noTarget_spawnsOneBlockNorth_velocityNegativeZ() {
        // Cannon at (10, 64, 20), facing NORTH, no AI target → fire straight ahead.
        CannonFireGeometry.Result r = CannonFireGeometry.compute(
            10, 64, 20, CannonState.Facing.NORTH, 2.0,
            null, 0, 0, 0);
        assertEquals(10.5, r.spawnX(), 0.001); // block center X
        assertEquals(64.5, r.spawnY(), 0.001); // block center Y (slight above floor)
        assertEquals(19.0, r.spawnZ(), 0.001); // one block north of cannon (-Z)
        assertEquals(0.0, r.vx(), 0.001);
        assertEquals(0.0, r.vy(), 0.001);
        assertEquals(-2.0, r.vz(), 0.001);
    }

    @Test
    void facingEast_noTarget_velocityPositiveX() {
        CannonFireGeometry.Result r = CannonFireGeometry.compute(
            0, 64, 0, CannonState.Facing.EAST, 2.0,
            null, 0, 0, 0);
        assertEquals(1.0, r.spawnX(), 0.001);
        assertEquals(64.5, r.spawnY(), 0.001);
        assertEquals(0.5, r.spawnZ(), 0.001);
        assertEquals(2.0, r.vx(), 0.001);
        assertEquals(0.0, r.vz(), 0.001);
    }

    @Test
    void withTarget_velocityPointsAtTarget_withSpeedPreserved() {
        // Cannon at origin facing EAST, target far to the SE → velocity should aim SE.
        CannonFireGeometry.Result r = CannonFireGeometry.compute(
            0, 64, 0, CannonState.Facing.EAST, 2.0,
            new CannonFireGeometry.Vec3d(10, 64, 10), 0, 0, 0);

        // Direction unit-vector from spawn (1, 64.5, 0.5) toward target (10, 64, 10):
        // dx = 9, dy = -0.5, dz = 9.5  → length ~13.10 → ux ~0.687, uy ~ -0.038, uz ~0.725
        double speed = Math.sqrt(r.vx() * r.vx() + r.vy() * r.vy() + r.vz() * r.vz());
        assertEquals(2.0, speed, 0.001);
        assertEquals(true, r.vx() > 0, "should fire east");
        assertEquals(true, r.vz() > 0, "should fire south");
    }
}
