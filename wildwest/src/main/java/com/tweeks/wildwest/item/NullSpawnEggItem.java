package com.tweeks.wildwest.item;

import com.tweeks.wildwest.entity.NullEntity;
import com.tweeks.wildwest.entity.NullSavedData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Singleton-aware spawn egg for Null. Allowed in Overworld, Nether, and End.
 *
 * <p>Spawn / teleport / cross-dim refusal mirrors {@link AgentSpawnEggItem}.
 * Visual identity: {@link ParticleTypes#ENCHANT} (vs Agent's SMOKE) and a
 * lower teleport pitch (0.6 vs 1.2) to give Null his own audio tell.
 */
public class NullSpawnEggItem extends SpawnEggItem {

    public NullSpawnEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;

        Player player = context.getPlayer();
        MinecraftServer server = sl.getServer();
        if (server == null) return InteractionResult.PASS;

        if (level.dimension() != Level.OVERWORLD
            && level.dimension() != Level.NETHER
            && level.dimension() != Level.END) {
            if (player != null) {
                player.sendOverlayMessage(
                    Component.translatable("item.wildwest.null_spawn_egg.wrong_dimension"));
            }
            return InteractionResult.FAIL;
        }

        NullSavedData saved = NullSavedData.get(server);

        if (!saved.isAlive()) {
            return super.useOn(context);
        }

        var savedDim = saved.getDimension();
        if (savedDim == null) {
            return refuseAway(player);
        }
        if (!savedDim.equals(level.dimension())) {
            if (player != null) {
                player.sendOverlayMessage(
                    Component.translatable("item.wildwest.null_spawn_egg.different_dimension"));
            }
            return InteractionResult.FAIL;
        }

        ServerLevel target = server.getLevel(savedDim);
        if (target == null) return refuseAway(player);

        Entity existing = target.getEntity(saved.getCurrentId());
        if (!(existing instanceof NullEntity nullBoss)) {
            return refuseAway(player);
        }

        double tx = context.getClickedPos().getX() + 0.5;
        double ty = context.getClickedPos().getY() + 1;
        double tz = context.getClickedPos().getZ() + 0.5;

        sl.sendParticles(ParticleTypes.ENCHANT,
            nullBoss.getX(), nullBoss.getY() + 1.0, nullBoss.getZ(),
            24, 0.5, 1.0, 0.5, 0.5);
        sl.playSound(null, nullBoss.getX(), nullBoss.getY(), nullBoss.getZ(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.6f, 0.6f);

        nullBoss.teleportTo(tx, ty, tz);

        sl.sendParticles(ParticleTypes.ENCHANT, tx, ty + 1.0, tz, 24, 0.5, 1.0, 0.5, 0.5);
        sl.playSound(null, tx, ty, tz, SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.HOSTILE, 0.6f, 0.6f);

        return InteractionResult.SUCCESS;
    }

    private static InteractionResult refuseAway(Player player) {
        if (player != null) {
            player.sendOverlayMessage(
                Component.translatable("item.wildwest.null_spawn_egg.away"));
        }
        return InteractionResult.FAIL;
    }
}
