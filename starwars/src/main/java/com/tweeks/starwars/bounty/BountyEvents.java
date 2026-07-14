package com.tweeks.starwars.bounty;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.faction.ModAttachments;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import org.jspecify.annotations.Nullable;

/**
 * Tracks bounty progress: when a player kills a mob that matches their active
 * contract, the remaining count drops; the last kill announces completion.
 * The attachment accessors here are the single read/write point for a
 * player's {@link BountyState}.
 */
@EventBusSubscriber(modid = StarWarsMod.MOD_ID)
public final class BountyEvents {
    private BountyEvents() {
    }

    public static @Nullable BountyState get(ServerPlayer player) {
        return player.hasData(ModAttachments.BOUNTY) ? player.getData(ModAttachments.BOUNTY) : null;
    }

    public static void set(ServerPlayer player, @Nullable BountyState state) {
        player.setData(ModAttachments.BOUNTY, state);
    }

    public static void clear(ServerPlayer player) {
        player.removeData(ModAttachments.BOUNTY);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BountyState state = get(player);
        if (state == null || state.complete()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        String victimId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();
        BountyState next = BountyContract.onKill(state, victimId);
        if (next == state) {
            return; // not a bounty target
        }
        set(player, next);
        if (next.complete()) {
            player.sendSystemMessage(Component.translatable("starwars.bounty.done"), true);
        } else {
            player.sendSystemMessage(Component.translatable("starwars.bounty.progress",
                next.killed(), next.total()), true);
        }
    }
}
