package com.tweeks.starwars.world.planet;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
// NOTE: Level is only used in signatures (ResourceKey<Level>), never
// class-loaded here — keep it that way for unit-test friendliness.

import org.jspecify.annotations.Nullable;

/**
 * The travel destinations reachable through a hyperspace gate. HOME is the
 * overworld; the other three are custom planet dimensions. Wedge order is the
 * clockwise order of the planet picker radial, starting at 12 o'clock, and
 * doubles as the stable ordinal used on the wire and in block state.
 */
public enum Planet implements StringRepresentable {
    TATOOINE("tatooine"),
    ANDOR("andor"),
    CORUSCANT("coruscant"),
    DAGOBAH("dagobah"),
    HOTH("hoth"),
    DEATH_STAR("death_star"),
    ENDOR("endor"),
    MUSTAFAR("mustafar"),
    HOME("home");

    /** Number of wedges on the planet picker radial. */
    public static final int COUNT = 9;

    public static final com.mojang.serialization.Codec<Planet> CODEC =
        StringRepresentable.fromEnum(Planet::values);

    private final String id;
    private final ResourceKey<Level> levelKey;

    Planet(String id) {
        this.id = id;
        // ResourceKeys are interned, so this is identical to Level.OVERWORLD —
        // but naming it directly keeps this class loadable in bare unit tests
        // (class-loading Level drags in FML-dependent statics).
        this.levelKey = "home".equals(id)
            ? ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("overworld"))
            : ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("starwars", id));
    }

    public String id() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** Lang key for the display name ("starwars.planet.tatooine" etc.). */
    public String translationKey() {
        return "starwars.planet." + id;
    }

    /** The runtime dimension this planet lives in (HOME → the overworld). */
    public ResourceKey<Level> levelKey() {
        return levelKey;
    }

    /** Dimension type key; only valid for the three custom planets. */
    public ResourceKey<DimensionType> dimensionTypeKey() {
        return ResourceKey.create(Registries.DIMENSION_TYPE, Identifier.fromNamespaceAndPath("starwars", id));
    }

    /** Level stem key; only valid for the three custom planets. */
    public ResourceKey<LevelStem> stemKey() {
        return ResourceKey.create(Registries.LEVEL_STEM, Identifier.fromNamespaceAndPath("starwars", id));
    }

    /** Wedge index → planet; null when out of range (bad packet data). */
    public static @Nullable Planet byWedge(int wedge) {
        return wedge >= 0 && wedge < values().length ? values()[wedge] : null;
    }

    /** The planet a level belongs to, or null for non-planet dimensions. */
    public static @Nullable Planet byLevel(ResourceKey<Level> key) {
        for (Planet planet : values()) {
            if (planet.levelKey.equals(key)) {
                return planet;
            }
        }
        return null;
    }
}
