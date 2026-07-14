package com.tweeks.starwars.world.planet;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.faction.ModAttachments;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Stamps the visited-planets attachment as players hop dimensions. */
@EventBusSubscriber(modid = StarWarsMod.MOD_ID)
public final class PlanetVisits {
    private PlanetVisits() {
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        mark(player, Planet.byLevel(event.getTo()));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // Home counts as charted from the start; also covers logging in
        // while already standing on a planet.
        mark(player, Planet.HOME);
        mark(player, Planet.byLevel(player.level().dimension()));
    }

    public static VisitedPlanets get(ServerPlayer player) {
        VisitedPlanets visited = player.hasData(ModAttachments.VISITED_PLANETS)
            ? player.getData(ModAttachments.VISITED_PLANETS)
            : null;
        return visited == null ? VisitedPlanets.none() : visited;
    }

    private static void mark(ServerPlayer player, Planet planet) {
        if (planet == null) {
            return;
        }
        VisitedPlanets current = get(player);
        if (!current.has(planet)) {
            player.setData(ModAttachments.VISITED_PLANETS, current.with(planet));
        }
    }
}
