package com.tweeks.starwars.world.planet;

import java.util.Set;
import java.util.TreeSet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

/**
 * Per-player record of planets set foot on (by {@link Planet#id()}), stored
 * as an attachment and consulted by the galaxy map. Immutable value; sorted
 * set keeps serialization deterministic.
 */
public record VisitedPlanets(Set<String> ids) {
    public static final MapCodec<VisitedPlanets> CODEC = Codec.STRING.listOf()
        .xmap(list -> new VisitedPlanets(new TreeSet<>(list)),
              visited -> visited.ids().stream().sorted().toList())
        .fieldOf("ids");

    public static VisitedPlanets none() {
        return new VisitedPlanets(new TreeSet<>());
    }

    public boolean has(Planet planet) {
        return ids.contains(planet.id());
    }

    public VisitedPlanets with(Planet planet) {
        TreeSet<String> next = new TreeSet<>(ids);
        next.add(planet.id());
        return new VisitedPlanets(next);
    }

    /** Bitmask over Planet.values() ordinals, for the wire. */
    public int toMask() {
        int mask = 0;
        for (Planet planet : Planet.values()) {
            if (has(planet)) {
                mask |= 1 << planet.ordinal();
            }
        }
        return mask;
    }
}
