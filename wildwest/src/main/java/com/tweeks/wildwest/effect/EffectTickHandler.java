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
            MindCharmAttachment charm = mob.getData(ModAttachments.MIND_CHARM.get());
            if (charm == null) continue;
            if (now >= charm.expiresAtTick()) {
                mob.removeData(ModAttachments.MIND_CHARM.get());
                continue;
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
    }
}
