/*
 * Original Java source: com.tweeks.wildwest.entity.AnomalyEntity (re-disguise tick path)
 *
 * private static final int RE_DISGUISE_TICKS = 200;        // 10 s
 * private static final int DAMAGE_GRACE_TICKS = 40;        // 2 s
 *
 * // In the entity tick, while REVEALED:
 * boolean inCombat = getTarget() != null
 *         || (tickCount - lastHurtByMobTimestamp) < DAMAGE_GRACE_TICKS;
 * if (!inCombat) {
 *     reDisguiseTicks++;
 *     if (reDisguiseTicks >= RE_DISGUISE_TICKS) {
 *         setRevealState(DISGUISED);
 *         reDisguiseTicks = 0;
 *     }
 * } else {
 *     reDisguiseTicks = 0;
 * }
 */

import { world, system, Entity } from "@minecraft/server";

// Per-entity re-disguise timer: ticks accumulated since the anomaly was
// last "engaged" (took damage or — best-effort — had a target).
const timers = new Map<string, number>();

// Last tick on which each anomaly was hurt. Mirrors Java's
// lastHurtByMobTimestamp + DAMAGE_GRACE_TICKS check: damage within the
// grace window resets the re-disguise countdown so the anomaly stays
// hostile through active combat.
const lastHurtTick = new Map<string, number>();

const ANOMALY_ID = "wildwest:anomaly";
const RE_DISGUISE_TICKS = 200;       // 10 s @ 20 tps — mirror AnomalyEntityConstants.RE_DISGUISE_TICKS
const DAMAGE_GRACE_TICKS = 40;       // 2 s — mirror AnomalyEntityConstants.DAMAGE_GRACE_TICKS
const CHECK_EVERY = 20;              // 1 s polling interval

/**
 * "Revealed" is encoded in the entity JSON's component-group `wildwest:revealed`,
 * which adds the `monster` family. The `wildwest:disguised` group adds `villager`
 * instead. We discriminate via family membership rather than tracking the active
 * group, which the script API doesn't expose directly.
 */
function isRevealed(entity: Entity): boolean {
    return entity.matches({ families: ["monster"] });
}

// Record damage hits on Anomalies so the re-disguise timer can be reset.
// Without this, a revealed Anomaly in active combat would always re-disguise
// after exactly RE_DISGUISE_TICKS regardless of whether the fight is ongoing,
// leaving it briefly defenseless and unable to retaliate (the disguised
// component group has no aggro behaviors).
world.afterEvents.entityHurt.subscribe(ev => {
    const e = ev.hurtEntity;
    if (e && e.typeId === ANOMALY_ID) {
        lastHurtTick.set(e.id, system.currentTick);
    }
});

// PERF: pooled 20-tick interval. The Java tick path runs per-tick (1 Hz vs
// 20 Hz is a 20x script-overhead reduction); incrementing the timer by
// CHECK_EVERY each fire keeps total time-to-re-disguise at
// RE_DISGUISE_TICKS / 20 = 10 s. See UNTRANSLATABLE.md for the remaining
// in-combat divergence (no Bedrock getTarget()).
system.runInterval(() => {
    for (const dimId of ["overworld", "nether", "the_end"]) {
        let anomalies: Entity[];
        try {
            anomalies = world.getDimension(dimId).getEntities({ type: ANOMALY_ID });
        } catch {
            continue;
        }
        for (const entity of anomalies) {
            const key = entity.id;
            if (!isRevealed(entity)) {
                timers.delete(key);
                continue;
            }
            // Reset countdown if hurt within the damage grace window
            // (active combat keeps the anomaly hostile).
            const sinceHurt = system.currentTick - (lastHurtTick.get(key) ?? -RE_DISGUISE_TICKS);
            if (sinceHurt < DAMAGE_GRACE_TICKS) {
                timers.set(key, 0);
                continue;
            }
            const next = (timers.get(key) ?? 0) + CHECK_EVERY;
            if (next >= RE_DISGUISE_TICKS) {
                entity.triggerEvent("wildwest:re_disguise");
                timers.delete(key);
                lastHurtTick.delete(key);
            } else {
                timers.set(key, next);
            }
        }
    }
}, CHECK_EVERY);
