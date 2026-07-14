package com.tweeks.starwars.entity.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroguForceNapTest {

    @Test
    void startsReadyAndIdlesWithoutHostiles() {
        GroguForceNap nap = new GroguForceNap();
        assertTrue(nap.isReady());
        assertEquals(GroguForceNap.Result.IDLE, nap.tick(false));
        assertEquals(GroguForceNap.Result.IDLE, nap.tick(false));
        assertTrue(nap.isReady());
        assertEquals(0, nap.cooldown());
    }

    @Test
    void hostileWhileReadyNapsOnceAndLocksOut() {
        GroguForceNap nap = new GroguForceNap();
        assertEquals(GroguForceNap.Result.NAP, nap.tick(true));
        assertFalse(nap.isReady());
        assertEquals(GroguForceNap.COOLDOWN_TICKS, nap.cooldown());
    }

    @Test
    void doesNotNapAgainWhileHostilePersistsDuringCooldown() {
        GroguForceNap nap = new GroguForceNap();
        assertEquals(GroguForceNap.Result.NAP, nap.tick(true));
        // Even with a hostile still adjacent, the whole cooldown is IDLE.
        for (int i = 0; i < GroguForceNap.COOLDOWN_TICKS; i++) {
            assertEquals(GroguForceNap.Result.IDLE, nap.tick(true),
                "should stay idle during lockout tick " + i);
        }
        // Exactly COOLDOWN_TICKS idle ticks later it is ready again.
        assertTrue(nap.isReady());
    }

    @Test
    void reArmsAfterCooldownAndNapsAgain() {
        GroguForceNap nap = new GroguForceNap();
        assertEquals(GroguForceNap.Result.NAP, nap.tick(true));
        for (int i = 0; i < GroguForceNap.COOLDOWN_TICKS; i++) {
            nap.tick(false);
        }
        assertTrue(nap.isReady());
        assertEquals(GroguForceNap.Result.NAP, nap.tick(true));
        assertEquals(GroguForceNap.COOLDOWN_TICKS, nap.cooldown());
    }

    @Test
    void cooldownBleedsDownByOneEachTick() {
        GroguForceNap nap = new GroguForceNap();
        nap.tick(true); // NAP -> cooldown = COOLDOWN_TICKS
        assertEquals(GroguForceNap.COOLDOWN_TICKS, nap.cooldown());
        nap.tick(false);
        assertEquals(GroguForceNap.COOLDOWN_TICKS - 1, nap.cooldown());
        nap.tick(false);
        assertEquals(GroguForceNap.COOLDOWN_TICKS - 2, nap.cooldown());
    }

    @Test
    void napFiresOncePerReadyWindow() {
        GroguForceNap nap = new GroguForceNap();
        int naps = 0;
        // Hostile present continuously. Nap at t=0, then again when it
        // re-arms at t=COOLDOWN_TICKS+1. Stop at t=2*COOLDOWN_TICKS, one tick
        // before it could re-arm for a third nap (which would be at
        // t=2*COOLDOWN_TICKS+1).
        for (int t = 0; t <= 2 * GroguForceNap.COOLDOWN_TICKS; t++) {
            if (nap.tick(true) == GroguForceNap.Result.NAP) {
                naps++;
            }
        }
        assertEquals(2, naps);
    }
}
