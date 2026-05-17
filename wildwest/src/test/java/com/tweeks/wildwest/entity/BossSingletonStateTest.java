package com.tweeks.wildwest.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossSingletonStateTest {

    @Test
    void defaultState_isCleared() {
        BossSingletonState s = new BossSingletonState();
        assertFalse(s.isAlive());
        assertNull(s.getCurrentId());
        assertNull(s.getDimensionId());
    }

    @Test
    void setAlive_storesAllFields() {
        BossSingletonState s = new BossSingletonState();
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        s.setAlive(id, "minecraft:overworld");

        assertTrue(s.isAlive());
        assertEquals(id, s.getCurrentId());
        assertEquals("minecraft:overworld", s.getDimensionId());
    }

    @Test
    void clear_resetsToDefault() {
        BossSingletonState s = new BossSingletonState();
        s.setAlive(UUID.randomUUID(), "minecraft:overworld");
        s.clear();

        assertFalse(s.isAlive());
        assertNull(s.getCurrentId());
        assertNull(s.getDimensionId());
    }

    @Test
    void copyOf_returnsEqualSnapshot() {
        BossSingletonState s = new BossSingletonState();
        UUID id = UUID.randomUUID();
        s.setAlive(id, "minecraft:the_nether");

        BossSingletonState copy = BossSingletonState.copyOf(s);
        assertTrue(copy.isAlive());
        assertEquals(id, copy.getCurrentId());
        assertEquals("minecraft:the_nether", copy.getDimensionId());
    }

    @Test
    void copyOf_isIndependent() {
        BossSingletonState s = new BossSingletonState();
        s.setAlive(UUID.randomUUID(), "minecraft:overworld");

        BossSingletonState copy = BossSingletonState.copyOf(s);
        s.clear();

        // The original is cleared, but the copy still holds the original values.
        assertTrue(copy.isAlive());
        assertEquals("minecraft:overworld", copy.getDimensionId());
    }
}
