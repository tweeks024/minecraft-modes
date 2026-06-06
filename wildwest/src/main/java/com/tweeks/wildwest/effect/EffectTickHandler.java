package com.tweeks.wildwest.effect;

import com.tweeks.wildwest.WildWestMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Server tick handler that drives Mind-charm + Reality-bubble timers.
 *
 * <p>Mind: each tick, every loaded mob with a {@code MIND_CHARM} attachment
 * gets its target re-set to the nearest hostile within 16 blocks (vanilla
 * AI would otherwise overwrite our target choice). On expiry the
 * attachment is removed.
 *
 * <p>Reality: see {@link #restoreFromBubble} — wired up in Task 12.
 */
@EventBusSubscriber(modid = WildWestMod.MOD_ID)
public final class EffectTickHandler {
    private EffectTickHandler() {}

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        for (net.minecraft.world.entity.Entity e : level.getAllEntities()) {
            if (!(e instanceof Mob mob)) continue;
            tickMindCharm(level, mob, now);
            tickRealityBubble(level, mob, now);
        }
    }

    private static void tickMindCharm(ServerLevel level, Mob mob, long now) {
        MindCharmAttachment charm = mob.getData(ModAttachments.MIND_CHARM.get());
        if (charm == null) return;
        if (now >= charm.expiresAtTick()) {
            mob.removeData(ModAttachments.MIND_CHARM.get());
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
        if (nearest != null) {
            mob.setTarget(nearest);
        }
    }

    private static void tickRealityBubble(ServerLevel level, Mob bat, long now) {
        RealityBubbleAttachment bubble = bat.getData(ModAttachments.REALITY_BUBBLE.get());
        if (bubble == null) return;
        if (now >= bubble.expiresAtTick()) {
            restoreFromBubble(level, bat, bubble);
        }
    }

    private static void restoreFromBubble(ServerLevel level, Mob bat, RealityBubbleAttachment bubble) {
        net.minecraft.resources.Identifier typeId =
            net.minecraft.resources.Identifier.parse(bubble.originalTypeId());
        net.minecraft.world.entity.EntityType<?> type =
            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(typeId);
        if (type != null) {
            net.minecraft.world.entity.Entity restored = type.create(level,
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (restored != null) {
                restored.snapTo(bat.getX(), bat.getY(), bat.getZ(), bat.getYRot(), 0.0f);
                level.addFreshEntity(restored);
            }
        }
        bat.discard();
    }
}
