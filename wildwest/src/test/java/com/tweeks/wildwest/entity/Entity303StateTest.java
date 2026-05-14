package com.tweeks.wildwest.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Entity303StateTest {

    @Test
    void defaultState_isCleared() {
        Entity303State s = new Entity303State();
        assertFalse(s.isAlive());
        assertNull(s.getCurrentId());
        assertNull(s.getDimensionId());
    }

    @Test
    void setAlive_storesAllFields() {
        Entity303State s = new Entity303State();
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        s.setAlive(id, "minecraft:the_end");

        assertTrue(s.isAlive());
        assertEquals(id, s.getCurrentId());
        assertEquals("minecraft:the_end", s.getDimensionId());
    }

    @Test
    void clear_resetsToDefault() {
        Entity303State s = new Entity303State();
        s.setAlive(UUID.randomUUID(), "minecraft:overworld");
        s.clear();

        assertFalse(s.isAlive());
        assertNull(s.getCurrentId());
        assertNull(s.getDimensionId());
    }

    @Test
    void copyOf_returnsEqualSnapshot() {
        Entity303State s = new Entity303State();
        UUID id = UUID.randomUUID();
        s.setAlive(id, "minecraft:the_end");

        Entity303State copy = Entity303State.copyOf(s);
        assertTrue(copy.isAlive());
        assertEquals(id, copy.getCurrentId());
        assertEquals("minecraft:the_end", copy.getDimensionId());
    }

    @Test
    void copyOf_isIndependent() {
        Entity303State s = new Entity303State();
        s.setAlive(UUID.randomUUID(), "minecraft:overworld");

        Entity303State copy = Entity303State.copyOf(s);
        s.clear();

        assertTrue(copy.isAlive());
        assertEquals("minecraft:overworld", copy.getDimensionId());
    }
}
