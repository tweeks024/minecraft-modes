package com.tweeks.starwars.world.planet;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/** Biome keys for the planet dimensions. Instances are built in datagen. */
public final class PlanetBiomes {
    /** Tatooine: endless rolling sand dunes. */
    public static final ResourceKey<Biome> DUNE_SEA = key("dune_sea");
    /** Tatooine: rocky canyon country, red sand and terracotta. */
    public static final ResourceKey<Biome> JUNDLAND_WASTES = key("jundland_wastes");
    /** Andor: cool green highlands with spruce groves and stone outcrops. */
    public static final ResourceKey<Biome> ALDHANI_HIGHLANDS = key("aldhani_highlands");
    /** Coruscant: the endless city. */
    public static final ResourceKey<Biome> CORUSCANT_CITY = key("coruscant_city");
    /** Dagobah: fog-bound mangrove marsh. */
    public static final ResourceKey<Biome> DAGOBAH_SWAMP = key("dagobah_swamp");
    /** Hoth: barren snowfields and glacier crags. */
    public static final ResourceKey<Biome> HOTH_PLAINS = key("hoth_plains");
    /** Death Star: the interior of the battle station. */
    public static final ResourceKey<Biome> DEATH_STAR_INTERIOR = key("death_star_interior");

    private PlanetBiomes() {
    }

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("starwars", name));
    }
}
