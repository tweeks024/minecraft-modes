package com.tweeks.wildwest.effect;

import com.mojang.logging.LogUtils;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;

/**
 * Server tick handler driving Mind-charm and Reality-bubble timers.
 *
 * <p><b>Mind charm:</b> each tick, every loaded mob with a {@code MIND_CHARM}
 * attachment has its target re-pointed at the nearest hostile within 16
 * blocks. If no hostile is in range, the target is explicitly cleared
 * ({@code setTarget(null)}) so the vanilla AI doesn't fall back to
 * attacking the player who cast the charm. On expiry the attachment is
 * removed and the mob's behavior reverts to its goal selector.
 *
 * <p><b>Reality bubble:</b> a bat with a {@code REALITY_BUBBLE} attachment
 * has its expiry checked; on expiry we attempt to restore the original
 * entity type at the bat's position. Failures (corrupted attachment, mod
 * removed, registry mismatch) are logged and the bat is kept alive with
 * the expiry extended by 5 s so an operator can intervene without
 * silently losing the original entity.
 */
@EventBusSubscriber(modid = WildWestMod.MOD_ID)
public final class EffectTickHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int BUBBLE_RETRY_TICKS = 100; // 5 s

    private EffectTickHandler() {}

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        // Snapshot the entity list before iterating. tickRealityBubble can
        // discard the bat (and add the restored entity), which would mutate
        // the underlying entity collection mid-iteration. Copying to a list
        // up-front avoids ConcurrentModificationException.
        java.util.List<Mob> mobs = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.Entity e : level.getAllEntities()) {
            if (e instanceof Mob mob) mobs.add(mob);
        }
        for (Mob mob : mobs) {
            if (!mob.isAlive()) continue;
            try {
                tickMindCharm(level, mob, now);
            } catch (Exception ex) {
                LOGGER.error("Mind-charm tick failed for {}", mob, ex);
            }
            try {
                tickRealityBubble(level, mob, now);
            } catch (Exception ex) {
                LOGGER.error("Reality-bubble tick failed for {}", mob, ex);
            }
        }
    }

    private static void tickMindCharm(ServerLevel level, Mob mob, long now) {
        MindCharmAttachment charm = mob.getData(ModAttachments.MIND_CHARM.get());
        if (charm == null) return;
        if (now >= charm.expiresAtTick()) {
            mob.removeData(ModAttachments.MIND_CHARM.get());
            mob.setTarget(null);
            return;
        }
        AABB search = mob.getBoundingBox().inflate(16.0);
        net.minecraft.world.entity.LivingEntity nearest = null;
        double nearestSq = Double.MAX_VALUE;
        for (net.minecraft.world.entity.LivingEntity candidate :
                level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, search)) {
            if (candidate == mob) continue;
            if (!(candidate instanceof Enemy)) continue;
            double dsq = candidate.distanceToSqr(mob);
            if (dsq < nearestSq) {
                nearestSq = dsq;
                nearest = candidate;
            }
        }
        // Always clear targeting first, then point at nearest hostile if any.
        // The null-target path is critical: it stops vanilla AI from falling
        // back to the caster (whose UUID the mob remembers in charm.casterUuid()).
        mob.setTarget(nearest);
    }

    private static void tickRealityBubble(ServerLevel level, Mob bat, long now) {
        RealityBubbleAttachment bubble = bat.getData(ModAttachments.REALITY_BUBBLE.get());
        if (bubble == null) return;
        if (now < bubble.expiresAtTick()) return;
        restoreFromBubble(level, bat, bubble);
    }

    private static void restoreFromBubble(ServerLevel level, Mob bat, RealityBubbleAttachment bubble) {
        net.minecraft.resources.Identifier typeId;
        try {
            typeId = net.minecraft.resources.Identifier.parse(bubble.originalTypeId());
        } catch (Exception ex) {
            LOGGER.error(
                "Reality-bubble has corrupted entity-type id {} on bat {}; keeping bat alive for retry",
                bubble.originalTypeId(), bat, ex);
            extendBubble(bat, bubble);
            return;
        }

        net.minecraft.world.entity.EntityType<?> type =
            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(typeId);
        if (type == null) {
            LOGGER.warn(
                "Reality-bubble references unknown entity type {}; keeping bat alive for retry",
                typeId);
            extendBubble(bat, bubble);
            return;
        }

        net.minecraft.world.entity.Entity restored = type.create(level,
            net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (restored == null) {
            LOGGER.warn(
                "Reality-bubble could not create entity of type {}; keeping bat alive for retry",
                typeId);
            extendBubble(bat, bubble);
            return;
        }

        restored.snapTo(bat.getX(), bat.getY(), bat.getZ(), bat.getYRot(), 0.0f);
        level.addFreshEntity(restored);
        bat.discard();
    }

    private static void extendBubble(Mob bat, RealityBubbleAttachment bubble) {
        bat.setData(ModAttachments.REALITY_BUBBLE.get(),
            new RealityBubbleAttachment(bubble.originalTypeId(),
                bubble.expiresAtTick() + BUBBLE_RETRY_TICKS));
    }
}
