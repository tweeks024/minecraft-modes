/*
 * Original Java source: com.tweeks.wildwest.entity.CrabEntity (registerGoals + handleEntityEvent)
 *
 * Override of MeleeAttackGoal.checkAndPerformAttack broadcasts pinch event:
 *   CrabEntity.this.level().broadcastEntityEvent(CrabEntity.this, EVENT_ID_PINCH);
 * where EVENT_ID_PINCH = (byte) 60.
 *
 * handleEntityEvent on client:
 *   if (id == EVENT_ID_PINCH) pinchState.start(tickCount);
 *
 * Plays animation.crab.pinch on each successful melee swing.
 */

import { world, EntityHitEntityAfterEvent } from "@minecraft/server";

world.afterEvents.entityHitEntity.subscribe((ev: EntityHitEntityAfterEvent) => {
    if (ev.damagingEntity.typeId !== "wildwest:crab") return;
    try {
        ev.damagingEntity.playAnimation("animation.crab.pinch");
    } catch {
        // Animation not registered yet or entity unloaded — non-fatal.
    }
});
