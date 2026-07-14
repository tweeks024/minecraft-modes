package com.tweeks.starwars.client;

import com.tweeks.starwars.world.planet.Planet;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Shared little planet-globe icons for the hyperspace picker and the galaxy
 * map. One 32x32 texture per world under {@code textures/gui/planet/}, drawn
 * via a raw {@link GuiGraphicsExtractor#blit} (no sprite-atlas registration).
 */
public final class PlanetIcons {
    private static final Identifier[] BY_ORDINAL = new Identifier[Planet.values().length];

    static {
        for (Planet planet : Planet.values()) {
            BY_ORDINAL[planet.ordinal()] = Identifier.fromNamespaceAndPath(
                "starwars", "textures/gui/planet/" + planet.id() + ".png");
        }
    }

    private PlanetIcons() {
    }

    public static Identifier of(Planet planet) {
        return BY_ORDINAL[planet.ordinal()];
    }

    /** Draws a planet's globe centred on (cx, cy) at the given half-size. */
    public static void draw(GuiGraphicsExtractor graphics, Planet planet, int cx, int cy, int half) {
        graphics.blit(of(planet), cx - half, cy - half, cx + half, cy + half, 0.0F, 1.0F, 0.0F, 1.0F);
    }
}
