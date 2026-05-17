package com.tweeks.wildwest.item;

import com.tweeks.wildwest.entity.AgentEntity;
import com.tweeks.wildwest.entity.AgentSavedData;
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
 * Singleton-aware spawn egg for The Agent. Allowed in Overworld and End only.
 *
 * <p><b>Spawn branch:</b> if no 303 alive, delegates to vanilla {@code SpawnEggItem.useOn}.
 * The entity's {@code finalizeSpawn} (Task 10) claims the singleton flag.
 *
 * <p><b>Teleport branch:</b> if 303 is alive in the SAME dimension as the
 * player, teleports him to the click location. Egg not consumed.
 *
 * <p><b>Cross-dimension refusal:</b> if 303 is alive in a different dimension
 * than the player, refuse with a feedback message. Egg not consumed.
 *
 * <p><b>Wrong-dimension refusal:</b> if used outside Overworld/End (e.g.,
 * Nether), refuse with a different feedback message. Egg not consumed.
 */
public class AgentSpawnEggItem extends SpawnEggItem {

    public AgentSpawnEggItem(Properties properties) {
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

        // Wrong-dimension gate.
        if (level.dimension() != Level.OVERWORLD && level.dimension() != Level.END) {
            if (player != null) {
                player.sendOverlayMessage(
                    Component.translatable("item.wildwest.the_agent_spawn_egg.wrong_dimension"));
            }
            return InteractionResult.FAIL;
        }

        AgentSavedData saved = AgentSavedData.get(server);

        if (!saved.isAlive()) {
            // Spawn branch — delegate to vanilla.
            return super.useOn(context);
        }

        // Teleport branch — but only same-dimension.
        var savedDim = saved.getDimension();
        if (savedDim == null) {
            return refuseAway(player);
        }
        if (!savedDim.equals(level.dimension())) {
            if (player != null) {
                player.sendOverlayMessage(
                    Component.translatable("item.wildwest.the_agent_spawn_egg.different_dimension"));
            }
            return InteractionResult.FAIL;
        }

        ServerLevel target = server.getLevel(savedDim);
        if (target == null) return refuseAway(player);

        Entity existing = target.getEntity(saved.getCurrentId());
        if (!(existing instanceof AgentEntity agent)) {
            return refuseAway(player);
        }

        double tx = context.getClickedPos().getX() + 0.5;
        double ty = context.getClickedPos().getY() + 1;
        double tz = context.getClickedPos().getZ() + 0.5;

        sl.sendParticles(ParticleTypes.SMOKE,
            agent.getX(), agent.getY() + 1.0, agent.getZ(), 16, 0.5, 1.0, 0.5, 0.0);
        sl.playSound(null, agent.getX(), agent.getY(), agent.getZ(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.6f, 1.2f);

        agent.teleportTo(tx, ty, tz);

        sl.sendParticles(ParticleTypes.SMOKE, tx, ty + 1.0, tz, 16, 0.5, 1.0, 0.5, 0.0);
        sl.playSound(null, tx, ty, tz, SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.HOSTILE, 0.6f, 1.2f);

        return InteractionResult.SUCCESS;
    }

    private static InteractionResult refuseAway(Player player) {
        if (player != null) {
            player.sendOverlayMessage(
                Component.translatable("item.wildwest.the_agent_spawn_egg.away"));
        }
        return InteractionResult.FAIL;
    }
}
