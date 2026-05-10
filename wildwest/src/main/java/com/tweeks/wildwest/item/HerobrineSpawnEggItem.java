package com.tweeks.wildwest.item;

import com.tweeks.wildwest.entity.HerobrineEntity;
import com.tweeks.wildwest.entity.HerobrineSavedData;
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
 * Singleton-aware spawn egg for Herobrine.
 *
 * <p><b>Spawn branch:</b> if no Herobrine is alive, delegates to vanilla
 * {@link SpawnEggItem#useOn(UseOnContext)} which spawns the entity normally.
 * The entity's {@code finalizeSpawn} (Task 13) sets the singleton flag.
 *
 * <p><b>Teleport branch:</b> if a Herobrine is already alive, teleports the
 * existing entity to the click location instead of spawning a duplicate. Egg
 * is not consumed in this branch.
 *
 * <p><b>Dimension gate:</b> rejects use outside Overworld (vanilla
 * {@code SpawnEggItem.useOn} doesn't check dimension; without this gate the
 * egg would spawn Herobrine in the Nether/End on first use).
 */
public class HerobrineSpawnEggItem extends SpawnEggItem {

    public HerobrineSpawnEggItem(Properties properties) {
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

        // Dimension gate.
        if (level.dimension() != Level.OVERWORLD) {
            if (player != null) {
                player.sendOverlayMessage(
                    Component.translatable("item.wildwest.herobrine_spawn_egg.overworld_only"));
            }
            return InteractionResult.FAIL;
        }

        HerobrineSavedData saved = HerobrineSavedData.get(server);

        if (!saved.isAlive()) {
            // Spawn branch — delegate to vanilla. The entity's finalizeSpawn
            // sets the singleton flag.
            return super.useOn(context);
        }

        // Teleport branch.
        var dimension = saved.getDimension();
        if (dimension == null || dimension != Level.OVERWORLD) {
            // Stored dimension is invalid or non-overworld — treat as unloaded.
            return refuseAway(player);
        }
        ServerLevel target = server.getLevel(dimension);
        if (target == null) return refuseAway(player);

        Entity existing = target.getEntity(saved.getCurrentId());
        if (!(existing instanceof HerobrineEntity hb)) {
            return refuseAway(player);
        }

        double tx = context.getClickedPos().getX() + 0.5;
        double ty = context.getClickedPos().getY() + 1;
        double tz = context.getClickedPos().getZ() + 0.5;

        // Source-side particle burst.
        sl.sendParticles(ParticleTypes.PORTAL,
            hb.getX(), hb.getY() + 1.0, hb.getZ(), 16, 0.5, 1.0, 0.5, 0.0);
        sl.playSound(null, hb.getX(), hb.getY(), hb.getZ(), SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.HOSTILE, 0.8f, 1.0f);

        hb.teleportTo(tx, ty, tz);

        // Destination-side particle burst + sound.
        sl.sendParticles(ParticleTypes.PORTAL, tx, ty + 1.0, tz, 16, 0.5, 1.0, 0.5, 0.0);
        sl.playSound(null, tx, ty, tz, SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.HOSTILE, 0.8f, 1.0f);

        return InteractionResult.SUCCESS;
    }

    private static InteractionResult refuseAway(Player player) {
        if (player != null) {
            player.sendOverlayMessage(
                Component.translatable("item.wildwest.herobrine_spawn_egg.away"));
        }
        return InteractionResult.FAIL;
    }
}
