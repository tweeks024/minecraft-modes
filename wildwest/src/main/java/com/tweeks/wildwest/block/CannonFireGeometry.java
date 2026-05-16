package com.tweeks.wildwest.block;

/**
 * Pure ballistics helper for the cannon block. Given the cannon's block
 * coordinates, its facing, a muzzle speed, and an optional target position
 * (for AI fires), returns the cannonball spawn position + initial velocity.
 *
 * <p>Spawn point = the center of the block face that the cannon is pointing
 * toward (one block beyond the cannon's own position, at half-block height).
 * Velocity = facing-vector × speed when no target; aim-at-target vector ×
 * speed when a target is supplied.
 */
public final class CannonFireGeometry {
    private CannonFireGeometry() {}

    public record Vec3d(double x, double y, double z) {}

    public record Result(double spawnX, double spawnY, double spawnZ,
                         double vx, double vy, double vz) {}

    public static Result compute(int cannonX, int cannonY, int cannonZ,
                                 CannonState.Facing facing, double speed,
                                 Vec3d target,
                                 double pad1, double pad2, double pad3) {
        // (pad1/pad2/pad3 are unused but kept so the call site can pass deterministic
        // RNG samples in future without changing the signature again.)

        double fdx = switch (facing) {
            case NORTH -> 0;
            case SOUTH -> 0;
            case EAST -> 1;
            case WEST -> -1;
        };
        double fdz = switch (facing) {
            case NORTH -> -1;
            case SOUTH -> 1;
            case EAST -> 0;
            case WEST -> 0;
        };

        double spawnX = fdx == 0 ? cannonX + 0.5 : cannonX + fdx;
        double spawnY = cannonY + 0.5;
        double spawnZ = fdz == 0 ? cannonZ + 0.5 : cannonZ + fdz;

        double vx, vy, vz;
        if (target == null) {
            vx = fdx * speed;
            vy = 0;
            vz = fdz * speed;
        } else {
            double dx = target.x - spawnX;
            double dy = target.y - spawnY;
            double dz = target.z - spawnZ;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0e-6) {
                vx = fdx * speed; vy = 0; vz = fdz * speed;
            } else {
                vx = (dx / len) * speed;
                vy = (dy / len) * speed;
                vz = (dz / len) * speed;
            }
        }

        return new Result(spawnX, spawnY, spawnZ, vx, vy, vz);
    }
}
