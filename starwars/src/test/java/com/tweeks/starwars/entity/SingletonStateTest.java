package com.tweeks.starwars.entity;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SingletonStateTest {

    @Test
    void freshState_isDead() {
        SingletonState s = new SingletonState();
        assertFalse(s.isAlive());
        assertNull(s.getCurrentId());
        assertNull(s.getDimensionId());
    }

    @Test
    void setAlive_storesIdAndDimension() {
        SingletonState s = new SingletonState();
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000042");
        s.setAlive(id, "minecraft:overworld");
        assertTrue(s.isAlive());
        assertEquals(id, s.getCurrentId());
        assertEquals("minecraft:overworld", s.getDimensionId());
    }

    @Test
    void clear_resetsEverything() {
        SingletonState s = new SingletonState();
        s.setAlive(UUID.randomUUID(), "minecraft:the_nether");
        s.clear();
        assertFalse(s.isAlive());
        assertNull(s.getCurrentId());
        assertNull(s.getDimensionId());
    }

    @Test
    void copyOf_copiesLiveState_andDeadState() {
        SingletonState live = new SingletonState();
        UUID id = UUID.randomUUID();
        live.setAlive(id, "minecraft:overworld");
        SingletonState copy = SingletonState.copyOf(live);
        assertTrue(copy.isAlive());
        assertEquals(id, copy.getCurrentId());

        SingletonState deadCopy = SingletonState.copyOf(new SingletonState());
        assertFalse(deadCopy.isAlive());
    }
}
