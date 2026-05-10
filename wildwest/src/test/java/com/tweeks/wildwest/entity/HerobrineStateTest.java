package com.tweeks.wildwest.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HerobrineStateTest {

    @Test
    void defaultState_isCleared() {
        HerobrineState s = new HerobrineState();
        assertFalse(s.isAlive());
        assertNull(s.getCurrentId());
        assertNull(s.getDimensionId());
    }

    @Test
    void setAlive_storesAllFields() {
        HerobrineState s = new HerobrineState();
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        s.setAlive(id, "minecraft:overworld");

        assertTrue(s.isAlive());
        assertEquals(id, s.getCurrentId());
        assertEquals("minecraft:overworld", s.getDimensionId());
    }

    @Test
    void clear_resetsToDefault() {
        HerobrineState s = new HerobrineState();
        s.setAlive(UUID.randomUUID(), "minecraft:overworld");
        s.clear();

        assertFalse(s.isAlive());
        assertNull(s.getCurrentId());
        assertNull(s.getDimensionId());
    }

    @Test
    void copyOf_returnsEqualSnapshot() {
        HerobrineState s = new HerobrineState();
        UUID id = UUID.randomUUID();
        s.setAlive(id, "minecraft:the_nether");

        HerobrineState copy = HerobrineState.copyOf(s);
        assertTrue(copy.isAlive());
        assertEquals(id, copy.getCurrentId());
        assertEquals("minecraft:the_nether", copy.getDimensionId());
    }

    @Test
    void copyOf_isIndependent() {
        HerobrineState s = new HerobrineState();
        s.setAlive(UUID.randomUUID(), "minecraft:overworld");

        HerobrineState copy = HerobrineState.copyOf(s);
        s.clear();

        // The original is cleared, but the copy still holds the original values.
        assertTrue(copy.isAlive());
        assertEquals("minecraft:overworld", copy.getDimensionId());
    }
}
