package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.CrabEntity;
import com.tweeks.wildwest.entity.CrabEntityConstants;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class CrabAlertSwarmHelper {

    private CrabAlertSwarmHelper() {}

    /**
     * Pure decision: should the candidate crab be alerted?
     * - distance: actual distance from the hurt crab (blocks)
     * - isBaby: candidate is a baby crab
     * - hasTarget: candidate already targets something
     */
    public static boolean shouldAlert(double distance, boolean isBaby, boolean hasTarget) {
        // Inclusive: a crab sitting exactly 8 blocks away counts as "within 8 blocks".
        if (distance > CrabEntityConstants.SWARM_RADIUS) return false;
        if (isBaby) return false;
        if (hasTarget) return false;
        return true;
    }

    /**
     * Broadcast: for every other adult crab within SWARM_RADIUS of {@code src} that
     * has no existing target, set {@code attacker} as target and start anger timer.
     * Server-side only — caller must guard {@code !level().isClientSide}.
     *
     * <p>Note: {@code setPersistentAngerTarget} is called with {@link EntityReference#of(Object)}
     * which accepts any {@code UniquelyIdentifyable} (LivingEntity qualifies). This is the
     * NeoForge 26.1.2 pattern — the UUID-based overload present in older API versions is gone.
     */
    public static void alertNearby(CrabEntity src, LivingEntity attacker) {
        AABB box = src.getBoundingBox().inflate(CrabEntityConstants.SWARM_RADIUS);
        for (CrabEntity other : src.level().getEntitiesOfClass(CrabEntity.class, box)) {
            if (other == src) continue;
            double dist = Math.sqrt(other.distanceToSqr(src));
            if (!shouldAlert(dist, other.isBaby(), other.getTarget() != null)) continue;
            other.setTarget(attacker);
            other.setLastHurtByMob(attacker);
            other.startPersistentAngerTimer();
            other.setPersistentAngerTarget(EntityReference.of(attacker));
        }
    }
}
