package com.tweeks.starwars.entity.ai;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class QuickdrawStateTest {

    private static final UUID TARGET_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID TARGET_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    public void freshTargetCanBeAmbushed() {
        QuickdrawState state = new QuickdrawState();
        assertTrue(state.canAmbush(TARGET_A));
    }

    @Test
    public void nullTargetCannotBeAmbushed() {
        QuickdrawState state = new QuickdrawState();
        assertFalse(state.canAmbush(null));
    }

    @Test
    public void ambushedTargetCannotBeAmbushedAgain() {
        QuickdrawState state = new QuickdrawState();
        state.markAmbushed(TARGET_A);
        assertFalse(state.canAmbush(TARGET_A));
    }

    @Test
    public void switchingTargetsReArmsQuickdraw() {
        QuickdrawState state = new QuickdrawState();
        state.markAmbushed(TARGET_A);
        assertTrue(state.canAmbush(TARGET_B));
        // Single-field memory: switching back re-arms against A (accepted in spec §3.3).
        state.markAmbushed(TARGET_B);
        assertTrue(state.canAmbush(TARGET_A));
    }

    @Test
    public void windupFiresExactlyOnEighthTick() {
        QuickdrawState state = new QuickdrawState();
        state.startWindup();
        assertTrue(state.isWindingUp());
        for (int i = 0; i < QuickdrawState.QUICKDRAW_WINDUP_TICKS - 1; i++) {
            assertFalse(state.tickWindup(), "tick " + i + " must not fire");
        }
        assertTrue(state.tickWindup(), "8th tick must fire");
        assertFalse(state.isWindingUp());
        assertFalse(state.tickWindup(), "no fire after expiry");
    }

    @Test
    public void cancelStopsWindupWithoutMarking() {
        QuickdrawState state = new QuickdrawState();
        state.startWindup();
        state.cancel();
        assertFalse(state.isWindingUp());
        assertFalse(state.tickWindup());
        assertTrue(state.canAmbush(TARGET_A), "cancel must not consume the ambush");
    }

    @Test
    public void markAmbushedClearsWindup() {
        QuickdrawState state = new QuickdrawState();
        state.startWindup();
        state.markAmbushed(TARGET_A);
        assertFalse(state.isWindingUp());
    }
}
