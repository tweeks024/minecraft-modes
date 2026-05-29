/*
 * Original Java source: com.tweeks.wildwest.entity.ai.CrabAlertSwarmHelper
 *
 * public static void alertNearby(CrabEntity src, LivingEntity attacker) {
 *     AABB box = src.getBoundingBox().inflate(8.0);
 *     for (CrabEntity other : src.level().getEntitiesOfClass(CrabEntity.class, box)) {
 *         if (other == src) continue;
 *         if (other.isBaby()) continue;
 *         if (other.getTarget() != null) continue;
 *         other.setTarget(attacker);
 *     }
 * }
 *
 * Triggered from CrabEntity#hurtServer() when source is a LivingEntity, server-side only.
 */

import { world, Entity, EntityHurtAfterEvent } from "@minecraft/server";

const SWARM_RADIUS = 8;

function isCrab(e: Entity): boolean {
    return e.typeId === "wildwest:crab";
}

function isAdultCrab(e: Entity): boolean {
    return isCrab(e) && !e.getComponent("minecraft:is_baby");
}

world.afterEvents.entityHurt.subscribe((ev: EntityHurtAfterEvent) => {
    const hurt = ev.hurtEntity;
    if (!isCrab(hurt)) return;
    const attacker = ev.damageSource.damagingEntity;
    if (!attacker) return;
    if (attacker.typeId === "wildwest:crab") return;

    const nearby = hurt.dimension.getEntities({
        location: hurt.location,
        maxDistance: SWARM_RADIUS,
        type: "wildwest:crab"
    });

    for (const other of nearby) {
        if (other.id === hurt.id) continue;
        if (!isAdultCrab(other)) continue;
        other.triggerEvent("wildwest:crab_hurt");
    }
});
