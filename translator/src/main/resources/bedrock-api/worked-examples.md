# Bedrock @minecraft/server worked examples

These are the canonical idioms the translator's LLM stage should pattern-match
against when translating Java NeoForge AI goal classes to Bedrock JavaScript /
TypeScript event handlers. Each example shows the Java source the pattern
applies to, then the Bedrock equivalent.

## Performance rules (apply verbatim)

1. Translate **discrete-event** Java logic — entity hurt, entity died, item
   used, block placed — to `world.afterEvents.*` / `world.beforeEvents.*`
   subscriptions. **Never poll** for these.
2. For **periodic state checks** (cooldown countdown, AI re-targeting, distance
   check), use `system.runInterval(fn, ticks)` with the **largest** interval
   that preserves correctness. Default to **20** (1 second) unless the source
   explicitly uses a tighter tick rate.
3. **Pool intervals**: share a single `runInterval` for multiple pieces of
   logic that want the same cadence rather than registering one interval per
   piece.
4. **Never use `system.run()` for periodic logic** — it is one-shot. Use
   `runInterval` instead.
5. If the source's tick semantics genuinely cannot be loosened, output
   `runInterval(fn, 1)` with a `// PERF:` comment explaining why. The
   `// PERF:` markers are searchable audit points.

## Example 1: discrete-event handler

**Java pattern:** `extends Goal` with `tick()` calling `mob.hurt(...)` only
when an event has happened (e.g. on contact).

**Java source:**
```java
public class StunOnHitGoal extends Goal {
    private final Mob mob;
    public boolean canUse() { return mob.getTarget() != null; }
    public void tick() {
        LivingEntity t = mob.getTarget();
        if (t != null && mob.distanceTo(t) < 1.5) {
            t.hurt(t.damageSources().mobAttack(mob), 4);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60));
        }
    }
}
```

**Bedrock equivalent:**
```ts
import { world, system } from "@minecraft/server";

// The hit moment is observable as a discrete event — no need to poll.
world.afterEvents.entityHitEntity.subscribe((ev) => {
    if (ev.damagingEntity.typeId !== "securityguard:guard") return;
    ev.hitEntity.applyDamage(4, {
        cause: "entityAttack",
        damagingEntity: ev.damagingEntity,
    });
    // 60 ticks of slowness, amplifier 0 = level I.
    ev.hitEntity.addEffect("slowness", 60, { amplifier: 0, showParticles: true });
});
```

## Example 2: periodic check, sensible cadence

**Java pattern:** `tick()` decrementing a cooldown counter every server tick.

**Java source:**
```java
public class CooldownGoal extends Goal {
    private int cooldown = 0;
    public boolean canUse() { return cooldown > 0; }
    public void tick() {
        cooldown--;
        if (cooldown <= 0) finish();
    }
}
```

**Bedrock equivalent:**
```ts
import { world, system } from "@minecraft/server";

// 20 ticks = 1 second. The Java code decrements once per tick (50ms), but the
// observable behavior — "cooldown expires after N seconds" — is preserved at
// the coarser 1-second cadence. Default to 20 unless the source needs tighter.
system.runInterval(() => {
    for (const entity of world.getDimension("overworld").getEntities({ type: "securityguard:guard" })) {
        const expiresAt = entity.getDynamicProperty("cooldownExpiresAt") as number | undefined;
        if (expiresAt === undefined) continue;
        if (system.currentTick >= expiresAt) {
            entity.setDynamicProperty("cooldownExpiresAt", undefined);
        }
    }
}, 20);
```

## Example 3: dynamic-property state machine

**Java pattern:** an entity field tracking a small enum / counter persisted
across ticks (e.g. `RevealState revealState = HIDDEN;`).

**Java source:**
```java
public enum RevealState { HIDDEN, REVEALING, REVEALED }
private RevealState revealState = RevealState.HIDDEN;

public void postHurtEnemy(LivingEntity enemy, DamageSource src) {
    if (revealState == RevealState.HIDDEN) {
        revealState = RevealState.REVEALING;
        // ... visual transition
    }
}
```

**Bedrock equivalent:**
```ts
import { world } from "@minecraft/server";

world.afterEvents.entityHurt.subscribe((ev) => {
    const attacker = ev.damageSource.damagingEntity;
    if (attacker?.typeId !== "thief:disguised") return;

    const state = (attacker.getDynamicProperty("revealState") as string | undefined) ?? "hidden";
    if (state === "hidden") {
        attacker.setDynamicProperty("revealState", "revealing");
        // PERF: visual transition deferred to a one-shot system.runTimeout
        // since revealing is a single 0.5s anim, not a periodic update.
    }
});
```

## Example 4: pooled interval for multiple cooldowns

**Java pattern:** several goals each decrementing their own `int cooldown`
field every tick.

**Java source:** (paraphrased — three goals each owning their own cooldown)
```java
class GoalA { int cooldown; void tick() { cooldown--; } }
class GoalB { int cooldown; void tick() { cooldown--; } }
class GoalC { int cooldown; void tick() { cooldown--; } }
```

**Bedrock equivalent:**
```ts
import { world, system } from "@minecraft/server";

// PERF: pool. One runInterval drives every cooldown timer, indexed by
// dynamic-property name. Cheaper than three separate runInterval calls.
const COOLDOWN_KEYS = ["cooldownA", "cooldownB", "cooldownC"];

system.runInterval(() => {
    for (const entity of world.getDimension("overworld").getEntities({ type: "securityguard:guard" })) {
        for (const key of COOLDOWN_KEYS) {
            const remaining = (entity.getDynamicProperty(key) as number | undefined) ?? 0;
            if (remaining > 0) entity.setDynamicProperty(key, remaining - 1);
        }
    }
}, 20);
```

## Output contract

When responding to the translator, produce a single fenced-JSON object:

```json
{
  "confidence": 0.85,
  "js": "import { world } from \"@minecraft/server\";\n// ..."
}
```

`confidence` ∈ [0.0, 1.0]: your self-rated confidence that the emitted JS
preserves the source Java's observable behavior in Bedrock 1.21.x. The
translator's confidence gate refuses outputs with confidence < 0.8 and
demotes them to a TODO stub for manual review.
