package com.tweeks.wildwest.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CannonStateTest {

    @Test
    void defaultIsUnloadedNorth() {
        CannonState s = CannonState.unloaded(CannonState.Facing.NORTH);
        assertFalse(s.loaded());
        assertEquals(CannonState.Facing.NORTH, s.facing());
    }

    @Test
    void loaded_returnsNewLoadedInstance() {
        CannonState a = CannonState.unloaded(CannonState.Facing.EAST);
        CannonState b = a.loaded(true);
        assertTrue(b.loaded());
        assertEquals(CannonState.Facing.EAST, b.facing());
        assertNotSame(a, b);
        assertFalse(a.loaded(), "Original must be immutable");
    }

    @Test
    void unloaded_returnsNewUnloadedInstance() {
        CannonState a = new CannonState(CannonState.Facing.WEST, true);
        CannonState b = a.loaded(false);
        assertFalse(b.loaded());
        assertEquals(CannonState.Facing.WEST, b.facing());
    }

    @Test
    void facing_doesNotAffectLoadedStatus() {
        CannonState a = new CannonState(CannonState.Facing.NORTH, true);
        CannonState b = a.withFacing(CannonState.Facing.SOUTH);
        assertTrue(b.loaded());
        assertEquals(CannonState.Facing.SOUTH, b.facing());
        assertNotEquals(a, b);
    }

    @Test
    void recordEquality() {
        CannonState a = new CannonState(CannonState.Facing.NORTH, true);
        CannonState b = new CannonState(CannonState.Facing.NORTH, true);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
