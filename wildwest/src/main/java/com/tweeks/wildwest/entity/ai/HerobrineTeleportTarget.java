package com.tweeks.wildwest.entity.ai;

/**
 * Pure horizontal-position picker for Herobrine's teleport goal. Three modes:
 *  <ul>
 *      <li>{@code CLOSE_GAP}  — distance > 12: blink 6–10 blocks toward target along self→target unit vector</li>
 *      <li>{@code OPEN_GAP}   — distance < 5: blink 8–12 blocks away along the inverse unit vector</li>
 *      <li>{@code RANDOM_REPOSITION} — distance in [5, 12]: pick random direction, distance 8–16 blocks from self</li>
 *  </ul>
 *
 * <p>Y is intentionally not handled here — the calling goal snaps to ground via
 * the level heightmap (Minecraft API).
 */
public final class HerobrineTeleportTarget {

    private HerobrineTeleportTarget() {}

    /** RNG abstraction so tests can drive deterministic sequences. */
    public interface Rng {
        double nextDouble();
    }

    public record Result(double x, double z, Mode mode) {
        public enum Mode { CLOSE_GAP, OPEN_GAP, RANDOM_REPOSITION }
    }

    public static Result pick(double selfX, double selfZ, double targetX, double targetZ, Rng rng) {
        double dx = targetX - selfX;
        double dz = targetZ - selfZ;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist > 12.0) {
            // Close the gap: dest = target - unit*(6 + r*4)
            double offset = 6.0 + rng.nextDouble() * 4.0;
            double ux = dx / dist;
            double uz = dz / dist;
            return new Result(targetX - ux * offset, targetZ - uz * offset, Result.Mode.CLOSE_GAP);
        }

        if (dist < 5.0 && dist > 1.0e-6) {
            // Open the gap: dest = target + (-unit)*(8 + r*4)
            double offset = 8.0 + rng.nextDouble() * 4.0;
            double ux = dx / dist;
            double uz = dz / dist;
            return new Result(targetX - ux * offset, targetZ - uz * offset, Result.Mode.OPEN_GAP);
        }

        // Random reposition (also covers the dist≈0 degenerate case).
        double angle = rng.nextDouble() * 2.0 * Math.PI;
        double dest = 8.0 + rng.nextDouble() * 8.0;
        return new Result(selfX + Math.cos(angle) * dest, selfZ + Math.sin(angle) * dest,
            Result.Mode.RANDOM_REPOSITION);
    }
}
