package com.tweeks.wildwest.spawning;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.entity.BanditLeaderEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Listens to {@link FinalizeSpawnEvent} — fired at the end of every mob-spawn
 * pipeline including biome-modifier-driven natural spawns. When a
 * {@link BanditLeaderEntity} finishes spawning naturally, attach the
 * entourage (black horse + 2 bandits).
 *
 * <p>Filters strictly on natural spawns and on entity class to avoid recursive
 * spawning when our own follower spawns happen to also fire the event.
 *
 * <p>API note (MC 26.1.2 / NeoForge): the event class is
 * {@code FinalizeSpawnEvent} (not {@code FinalizeMobSpawnEvent} / not
 * nested under {@code MobSpawnEvent}); the spawn-reason getter is
 * {@code getSpawnType()}.
 */
@EventBusSubscriber(modid = WildWestMod.MOD_ID)
public final class BanditLeaderPackSpawner {
    private BanditLeaderPackSpawner() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getSpawnType() != EntitySpawnReason.NATURAL) return;
        if (!(event.getEntity() instanceof BanditLeaderEntity leader)) return;
        if (!(leader.level() instanceof ServerLevel sl)) return;

        // Pre-flight: skip if leader is already mounted (re-spawn / re-load case).
        if (leader.getVehicle() != null) return;

        // FinalizeSpawnEvent fires BEFORE the spawn pipeline calls
        // tryAddFreshEntityWithPassengers(leader). If we attach the entourage
        // here, the leader hasn't been added to the level's entity tracker
        // yet, and clients may transiently see it unmounted. Defer to the
        // server thread's next polling cycle, by which time the leader is
        // fully present. Re-check alive + no-vehicle in case state changed.
        sl.getServer().execute(() -> {
            if (!leader.isAlive()) return;
            if (leader.getVehicle() != null) return;
            LeaderEntourageSpawner.spawnEntourage(
                sl, leader, ModEntities.BANDIT.get(),
                Variant.BLACK, Markings.NONE);
        });
    }
}
