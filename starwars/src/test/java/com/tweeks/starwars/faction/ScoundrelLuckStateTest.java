package com.tweeks.starwars.faction;

import com.tweeks.starwars.entity.ai.QuickdrawState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ScoundrelLuckStateTest {

    private static final UUID PLAYER_A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID PLAYER_B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID TARGET = UUID.fromString("00000000-0000-0000-0000-0000000000c3");

    @AfterEach
    public void cleanup() {
        ScoundrelLuck.clear(PLAYER_A);
        ScoundrelLuck.clear(PLAYER_B);
    }

    @Test
    public void stateForReturnsSameInstancePerPlayer() {
        assertSame(ScoundrelLuck.stateFor(PLAYER_A), ScoundrelLuck.stateFor(PLAYER_A));
    }

    @Test
    public void statesAreIndependentAcrossPlayers() {
        ScoundrelLuck.stateFor(PLAYER_A).markAmbushed(TARGET);
        assertFalse(ScoundrelLuck.stateFor(PLAYER_A).canAmbush(TARGET));
        assertTrue(ScoundrelLuck.stateFor(PLAYER_B).canAmbush(TARGET));
    }

    @Test
    public void clearDropsTheEntry() {
        QuickdrawState before = ScoundrelLuck.stateFor(PLAYER_A);
        before.markAmbushed(TARGET);
        ScoundrelLuck.clear(PLAYER_A);
        assertNotSame(before, ScoundrelLuck.stateFor(PLAYER_A));
        assertTrue(ScoundrelLuck.stateFor(PLAYER_A).canAmbush(TARGET));
    }
}
