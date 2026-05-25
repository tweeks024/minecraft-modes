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

// Per-entity timer: how long this Anomaly has been "settled" while revealed.
const timers = new Map<string, number>();

const ANOMALY_ID = "wildwest:anomaly";
const RE_DISGUISE_TICKS = 200; // 10 s @ 20 tps — mirror AnomalyEntityConstants.RE_DISGUISE_TICKS
const CHECK_EVERY = 20;        // 1 s polling interval

/**
 * "Revealed" is encoded in the entity JSON's component-group `wildwest:revealed`,
 * which adds the `monster` family. The `wildwest:disguised` group adds `villager`
 * instead. We discriminate via family membership rather than tracking the active
 * group, which the script API doesn't expose directly.
 */
function isRevealed(entity: Entity): boolean {
    return entity.matches({ families: ["monster"] });
}

// PERF: pooled 20-tick interval. The Java tick is per-tick (1 Hz vs 20 Hz is a
// 20x reduction in script overhead), and since we increment the timer by
// CHECK_EVERY ticks each fire, total time-to-re-disguise stays at
// RE_DISGUISE_TICKS / 20 = 10 s. See UNTRANSLATABLE.md for the in-combat
// approximation.
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
            // Bedrock script API doesn't expose entity.getTarget(), so we tick up
            // unconditionally while revealed. An anomaly in active combat may
            // briefly fire the wildwest:re_disguise event and then re-aggro on
            // the next damage tick. See UNTRANSLATABLE.md for the divergence.
            const next = (timers.get(key) ?? 0) + CHECK_EVERY;
            if (next >= RE_DISGUISE_TICKS) {
                entity.triggerEvent("wildwest:re_disguise");
                timers.delete(key);
            } else {
                timers.set(key, next);
            }
        }
    }
}, CHECK_EVERY);
