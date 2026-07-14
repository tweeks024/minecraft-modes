package com.tweeks.starwars.world.planet;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlanetTest {

    @Test
    void wedgeRoundTrip() {
        for (Planet planet : Planet.values()) {
            assertEquals(planet, Planet.byWedge(planet.ordinal()));
        }
        assertNull(Planet.byWedge(-1));
        assertNull(Planet.byWedge(Planet.COUNT));
    }

    @Test
    void homeIsTheOverworld() {
        // Note: cannot reference Level.OVERWORLD here (class-loading Level
        // needs FML); ResourceKeys are interned so this key IS that key.
        assertEquals("minecraft:overworld", Planet.HOME.levelKey().identifier().toString());
        assertEquals(Planet.HOME, Planet.byLevel(Planet.HOME.levelKey()));
    }

    @Test
    void planetLevelKeysAreNamespaced() {
        assertEquals("starwars:tatooine", Planet.TATOOINE.levelKey().identifier().toString());
        assertEquals("starwars:andor", Planet.ANDOR.levelKey().identifier().toString());
        assertEquals("starwars:coruscant", Planet.CORUSCANT.levelKey().identifier().toString());
        assertEquals("starwars:dagobah", Planet.DAGOBAH.levelKey().identifier().toString());
        assertEquals("starwars:hoth", Planet.HOTH.levelKey().identifier().toString());
        assertEquals(Planet.CORUSCANT, Planet.byLevel(Planet.CORUSCANT.levelKey()));
        assertEquals(Planet.HOTH, Planet.byLevel(Planet.HOTH.levelKey()));
        assertNull(Planet.byLevel(
            ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("the_nether"))));
    }

    @Test
    void radialHasNineWedgesWithHomeLast() {
        assertEquals(9, Planet.COUNT);
        assertEquals(Planet.HOME, Planet.byWedge(Planet.COUNT - 1));
        assertEquals("starwars:death_star", Planet.DEATH_STAR.levelKey().identifier().toString());
        assertEquals("starwars:endor", Planet.ENDOR.levelKey().identifier().toString());
        assertEquals("starwars:mustafar", Planet.MUSTAFAR.levelKey().identifier().toString());
        assertEquals(Planet.MUSTAFAR, Planet.byLevel(Planet.MUSTAFAR.levelKey()));
    }

    @Test
    void serializedNamesMatchIds() {
        for (Planet planet : Planet.values()) {
            assertEquals(planet.id(), planet.getSerializedName());
        }
        assertEquals("starwars.planet.coruscant", Planet.CORUSCANT.translationKey());
    }
}
