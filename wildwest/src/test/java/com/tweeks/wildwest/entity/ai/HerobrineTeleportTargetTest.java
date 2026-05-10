package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.ai.HerobrineTeleportTarget.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HerobrineTeleportTargetTest {

    /** Deterministic seed-driven RNG stand-in for the helper. */
    static class FixedRng implements HerobrineTeleportTarget.Rng {
        private final double[] values;
        private int idx = 0;
        FixedRng(double... values) { this.values = values; }
        @Override public double nextDouble() { return values[idx++ % values.length]; }
    }

    @Test
    void farTarget_closesGap() {
        // self at (0,0), target at (20, 0); distance > 12 → close gap, dest 6–10 from target.
        Result r = HerobrineTeleportTarget.pick(0, 0, 20, 0, new FixedRng(0.5));
        // dx = 20, distance = 20. Closing means dest = target - unit*(6 + 0.5*4) = (20,0) - (1,0)*8 = (12, 0).
        assertEquals(12.0, r.x(), 0.001);
        assertEquals(0.0, r.z(), 0.001);
        assertEquals(Result.Mode.CLOSE_GAP, r.mode());
    }

    @Test
    void closeTarget_opensGap() {
        // self at (0,0), target at (3, 0); distance < 5 → open gap, dest 8–12 from target.
        Result r = HerobrineTeleportTarget.pick(0, 0, 3, 0, new FixedRng(0.0));
        // Reverse direction unit = (-1, 0). 8 + 0.0*4 = 8 blocks from target along reverse.
        // dest = (3,0) + (-1,0)*8 = (-5, 0).
        assertEquals(-5.0, r.x(), 0.001);
        assertEquals(0.0, r.z(), 0.001);
        assertEquals(Result.Mode.OPEN_GAP, r.mode());
    }

    @Test
    void midTarget_randomReposition() {
        // self at (0,0), target at (8, 0); distance == 8, in [5,12] → random direction.
        // FixedRng returns angleFraction = 0.0 → angle = 0 → unit (1, 0).
        // distance = 8 + 0.5*8 = 12 (second nextDouble call).
        Result r = HerobrineTeleportTarget.pick(0, 0, 8, 0, new FixedRng(0.0, 0.5));
        // dest = self + (1,0)*12 = (12, 0).
        assertEquals(12.0, r.x(), 0.001);
        assertEquals(0.0, r.z(), 0.001);
        assertEquals(Result.Mode.RANDOM_REPOSITION, r.mode());
    }

    @Test
    void targetCoincidentWithSelf_doesNotDivideByZero() {
        // Edge case: self == target. Should fall through to random reposition (mid range).
        Result r = HerobrineTeleportTarget.pick(0, 0, 0, 0, new FixedRng(0.25, 0.5));
        // No NaN; finite result.
        assertTrue(Double.isFinite(r.x()));
        assertTrue(Double.isFinite(r.z()));
        assertEquals(Result.Mode.RANDOM_REPOSITION, r.mode());
    }
}
