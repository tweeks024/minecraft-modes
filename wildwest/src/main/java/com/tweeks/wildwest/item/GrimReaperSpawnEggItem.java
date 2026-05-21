package com.tweeks.wildwest.item;

import com.tweeks.wildwest.entity.GrimReaperEntity;
import com.tweeks.wildwest.entity.GrimReaperSavedData;
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
 * Singleton-aware spawn egg for the Grim Reaper. Overworld-only.
 * Visual identity: {@link ParticleTypes#SOUL} (vs Null's ENCHANT).
 * Audio identity: {@link SoundEvents#SOUL_ESCAPE} on teleport.
 */
public class GrimReaperSpawnEggItem extends SpawnEggItem {

    public GrimReaperSpawnEggItem(Properties properties) {
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

        if (level.dimension() != Level.OVERWORLD) {
            if (player != null) {
                player.sendOverlayMessage(
                    Component.translatable("item.wildwest.grim_reaper_spawn_egg.wrong_dimension"));
            }
            return InteractionResult.FAIL;
        }

        GrimReaperSavedData saved = GrimReaperSavedData.get(server);

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
                    Component.translatable("item.wildwest.grim_reaper_spawn_egg.different_dimension"));
            }
            return InteractionResult.FAIL;
        }

        ServerLevel target = server.getLevel(savedDim);
        if (target == null) return refuseAway(player);

        var savedId = saved.getCurrentId();
        if (savedId == null) return refuseAway(player);

        Entity existing = target.getEntity(savedId);
        if (!(existing instanceof GrimReaperEntity reaper)) {
            return refuseAway(player);
        }

        double tx = context.getClickedPos().getX() + 0.5;
        double ty = context.getClickedPos().getY() + 1;
        double tz = context.getClickedPos().getZ() + 0.5;

        sl.sendParticles(ParticleTypes.SOUL,
            reaper.getX(), reaper.getY() + 1.0, reaper.getZ(),
            24, 0.5, 1.0, 0.5, 0.05);
        sl.playSound(null, reaper.getX(), reaper.getY(), reaper.getZ(),
            SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE, 1.0f, 0.7f);

        reaper.teleportTo(tx, ty, tz);

        sl.sendParticles(ParticleTypes.SOUL, tx, ty + 1.0, tz, 24, 0.5, 1.0, 0.5, 0.05);
        sl.playSound(null, tx, ty, tz, SoundEvents.SOUL_ESCAPE,
            SoundSource.HOSTILE, 1.0f, 0.7f);

        return InteractionResult.SUCCESS;
    }

    private static InteractionResult refuseAway(Player player) {
        if (player != null) {
            player.sendOverlayMessage(
                Component.translatable("item.wildwest.grim_reaper_spawn_egg.away"));
        }
        return InteractionResult.FAIL;
    }
}
