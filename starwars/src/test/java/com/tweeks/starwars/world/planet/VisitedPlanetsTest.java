package com.tweeks.starwars.world.planet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisitedPlanetsTest {

    @Test
    void startsEmptyAndAccumulates() {
        VisitedPlanets none = VisitedPlanets.none();
        assertFalse(none.has(Planet.TATOOINE));
        assertEquals(0, none.toMask());

        VisitedPlanets some = none.with(Planet.TATOOINE).with(Planet.HOTH);
        assertTrue(some.has(Planet.TATOOINE));
        assertTrue(some.has(Planet.HOTH));
        assertFalse(some.has(Planet.CORUSCANT));
        // Original untouched (immutable value).
        assertFalse(none.has(Planet.TATOOINE));
    }

    @Test
    void maskBitsFollowOrdinals() {
        VisitedPlanets visited = VisitedPlanets.none().with(Planet.HOME).with(Planet.DAGOBAH);
        int mask = visited.toMask();
        assertTrue((mask & (1 << Planet.HOME.ordinal())) != 0);
        assertTrue((mask & (1 << Planet.DAGOBAH.ordinal())) != 0);
        assertEquals(0, mask & (1 << Planet.TATOOINE.ordinal()));
    }

    @Test
    void addingTwiceIsIdempotent() {
        VisitedPlanets visited = VisitedPlanets.none().with(Planet.ANDOR).with(Planet.ANDOR);
        assertEquals(1, visited.ids().size());
    }
}
