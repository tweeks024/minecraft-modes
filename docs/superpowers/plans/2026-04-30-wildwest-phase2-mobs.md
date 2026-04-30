# Wild-West Phase 2 — Mobs + Hand Weapons — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the four wild-west mobs (deputy, sherrif, bandit, bandit-leader), their two faction-themed hand weapons (billy_club, bandit_knife), wildwest-only `Lawman`/`Outlaw` faction interfaces, distance-based weapon-mode swap AI, lawman-vs-outlaw hostility with iron-golem-style sheriff retaliation, and natural spawning in plains/savanna/desert biomes.

**Architecture:** All four mobs share an abstract `WildWestMob extends PathfinderMob` base class that handles weapon-mode swapping (RANGED ↔ MELEE) via a synced data accessor, with 20-tick hysteresis. Subclasses set attributes + which gun + which hand weapon. AI goals (`WildWestRangedAttackGoal`, `WildWestMeleeAttackGoal`, `LawmanTargetGoal`, `OutlawTargetGoal`) are shared. Phase-1 `PistolItem`/`RifleItem` get static `fireFromMob` helpers added back. Lawmen spawn near plains-village beds via a periodic `LevelTickEvent.Post` check; outlaws spawn in plains/savanna/desert via a NeoForge `BiomeModifier` JSON.

**Tech Stack:** Java 25, NeoForge 26.1.2.30-beta, Minecraft 26.1.2, JUnit 5.

**Spec:** [docs/superpowers/specs/2026-04-30-wildwest-phase2-mobs-design.md](../specs/2026-04-30-wildwest-phase2-mobs-design.md)

**API lessons carried forward from phase 1:**
- `Identifier` is the resource-key constructor (not `ResourceLocation`).
- `Item.use` returns `InteractionResult` (not `InteractionResultHolder<ItemStack>`).
- `level.isClientSide()` is a method.
- `Item.inventoryTick` signature is `(ItemStack, ServerLevel, Entity, EquipmentSlot)`.
- `ItemCooldowns.isOnCooldown/addCooldown/getCooldownPercent` take `ItemStack`.
- `LivingEntity.hurtServer((ServerLevel) level, source, dmg)` is the modern damage API; `hurt(...)` is deprecated.
- `AbstractArrow` lives in `net.minecraft.world.entity.projectile.arrow.AbstractArrow`.
- `EntityRenderer<T, S>` is generic in render state; mob renderers extend `HumanoidMobRenderer<T, HumanoidRenderState, M>`.
- `@EventBusSubscriber` doesn't take a `bus` parameter (defaults to MOD).
- `ItemProperties` Java class is gone in MC 26.1.2; selectors moved to JSON `range_dispatch`.

---

## Task 1: Faction marker interfaces + WeaponMode pure helper + tests

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/api/Lawman.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/api/Outlaw.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/WeaponMode.java`
- Create: `wildwest/src/test/java/com/tweeks/wildwest/entity/WeaponModeTest.java`
- Create: `wildwest/src/test/java/com/tweeks/wildwest/api/FactionPredicateTest.java`

- [ ] **Step 1: Write failing tests**

`wildwest/src/test/java/com/tweeks/wildwest/entity/WeaponModeTest.java`:

```java
package com.tweeks.wildwest.entity;

import org.junit.jupiter.api.Test;

import static com.tweeks.wildwest.entity.WeaponMode.MELEE;
import static com.tweeks.wildwest.entity.WeaponMode.RANGED;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WeaponModeTest {

    @Test
    void closeRange_swapAllowed_picksMelee() {
        assertEquals(MELEE, WeaponMode.choose(2.0, RANGED, 0));
    }

    @Test
    void closeRange_locked_keepsCurrent() {
        assertEquals(RANGED, WeaponMode.choose(2.0, RANGED, 5));
    }

    @Test
    void farRange_swapAllowed_picksRanged() {
        assertEquals(RANGED, WeaponMode.choose(8.0, MELEE, 0));
    }

    @Test
    void atBoundary_4blk_staysMelee() {
        assertEquals(MELEE, WeaponMode.choose(4.0, MELEE, 0));
    }

    @Test
    void justPastBoundary_swapsToRanged() {
        assertEquals(RANGED, WeaponMode.choose(4.001, MELEE, 0));
    }

    @Test
    void desiredEqualsCurrent_isNoOp() {
        assertEquals(RANGED, WeaponMode.choose(8.0, RANGED, 0));
        assertEquals(MELEE, WeaponMode.choose(2.0, MELEE, 0));
    }
}
```

`wildwest/src/test/java/com/tweeks/wildwest/api/FactionPredicateTest.java`:

```java
package com.tweeks.wildwest.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionPredicateTest {

    static class TestLawman implements Lawman {}
    static class TestOutlaw implements Outlaw {}
    static class Neutral {}

    @Test
    void lawman_implementer_passes_lawman_check() {
        assertTrue(new TestLawman() instanceof Lawman);
        assertFalse(new TestLawman() instanceof Outlaw);
    }

    @Test
    void outlaw_implementer_passes_outlaw_check() {
        assertTrue(new TestOutlaw() instanceof Outlaw);
        assertFalse(new TestOutlaw() instanceof Lawman);
    }

    @Test
    void neutral_class_passes_neither() {
        assertFalse(new Neutral() instanceof Lawman);
        assertFalse(new Neutral() instanceof Outlaw);
    }
}
```

- [ ] **Step 2: Run tests, verify they fail**

```bash
./gradlew :wildwest:test
```

Expected: compile error (Lawman/Outlaw/WeaponMode classes don't exist yet).

- [ ] **Step 3: Implement the markers**

`wildwest/src/main/java/com/tweeks/wildwest/api/Lawman.java`:

```java
package com.tweeks.wildwest.api;

/**
 * Marker interface for wild-west faction "lawmen" (sheriff, deputy).
 * Used in target-selector predicates so outlaws can identify them as enemies.
 *
 * Empty by design — no methods. Identity check via {@code instanceof Lawman}.
 */
public interface Lawman {
}
```

`wildwest/src/main/java/com/tweeks/wildwest/api/Outlaw.java`:

```java
package com.tweeks.wildwest.api;

/**
 * Marker interface for wild-west faction "outlaws" (bandit, bandit-leader).
 * Used in target-selector predicates so lawmen can identify them as enemies.
 */
public interface Outlaw {
}
```

- [ ] **Step 4: Implement WeaponMode**

`wildwest/src/main/java/com/tweeks/wildwest/entity/WeaponMode.java`:

```java
package com.tweeks.wildwest.entity;

public enum WeaponMode {
    RANGED,
    MELEE;

    /** Distance threshold (blocks). At-or-below = MELEE, above = RANGED. */
    public static final double MELEE_RANGE = 4.0;

    /**
     * Pick the next weapon mode given the current state.
     *
     * @param distanceToTarget   distance from this mob to its target, in blocks
     * @param current            mob's current mode
     * @param hysteresisRemaining ticks until the swap-lock expires; 0 = swap allowed
     * @return the new mode (may equal current — caller decides if that's a no-op)
     */
    public static WeaponMode choose(double distanceToTarget, WeaponMode current, int hysteresisRemaining) {
        WeaponMode desired = (distanceToTarget <= MELEE_RANGE) ? MELEE : RANGED;
        if (desired == current) return current;
        if (hysteresisRemaining > 0) return current;
        return desired;
    }
}
```

- [ ] **Step 5: Run tests, verify they pass**

```bash
./gradlew :wildwest:test
```

Expected: all 9 tests pass (6 WeaponModeTest + 3 FactionPredicateTest).

- [ ] **Step 6: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/api/ wildwest/src/main/java/com/tweeks/wildwest/entity/WeaponMode.java wildwest/src/test/java/com/tweeks/wildwest/
git commit -m "feat(wildwest): faction markers + WeaponMode helper + unit tests

Lawman + Outlaw empty marker interfaces for instanceof-based faction
checks. WeaponMode pure-logic helper picks RANGED vs MELEE based on
distance + hysteresis lock, tested without booting Minecraft."
```

---

## Task 2: Phase-1 add-back — `fireFromMob` helpers

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/item/PistolItem.java` (add static `fireFromMob`)
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/item/RifleItem.java` (add static `fireFromMob`)

The phase-1 spec called these out as deferred. Phase 2 needs them so mob AI goals can call into the gun-firing logic. Same hitscan / projectile mechanics as the player path, but the shooter is a `LivingEntity` (mob) and the tracer packet target list excludes the shooter (since mobs aren't `ServerPlayer`).

- [ ] **Step 1: Add `fireFromMob` to PistolItem**

In `wildwest/src/main/java/com/tweeks/wildwest/item/PistolItem.java`, add the import:

```java
import net.minecraft.util.Mth;
import java.util.Random;
```

Add this static helper after the `use(...)` method:

```java
    /**
     * Mob-side firing path. Server-side hitscan from shooter's eye toward target,
     * with Gaussian aim inaccuracy so mobs don't pixel-perfect snipe. Sends tracer
     * packet to all players tracking the shooter (NOT including-self — mobs aren't
     * ServerPlayers).
     *
     * No cooldown / durability tracking on the mob. The mob's own AI goal manages
     * its fire-rate timing.
     */
    public static void fireFromMob(LivingEntity shooter, LivingEntity target) {
        Level level = shooter.level();
        if (level.isClientSide()) return;

        Vec3 start = shooter.getEyePosition();
        // Target the torso, not the feet — minecraft entities have getEyeY too high
        // for ground-level shooters but torso = pos + bbHeight*0.5.
        Vec3 aimAt = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 dir = aimAt.subtract(start).normalize();

        // Gaussian inaccuracy ~0.05 rad on each axis.
        java.util.Random rand = shooter.getRandom();
        double ax = rand.nextGaussian() * 0.05;
        double ay = rand.nextGaussian() * 0.05;
        double az = rand.nextGaussian() * 0.05;
        dir = new Vec3(dir.x + ax, dir.y + ay, dir.z + az).normalize();

        Vec3 end = start.add(dir.scale(MAX_RANGE));

        BlockHitResult blockHit = level.clip(new ClipContext(
            start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        double blockDist = blockHit.getType() == HitResult.Type.MISS
            ? MAX_RANGE
            : start.distanceTo(blockHit.getLocation());

        List<LivingEntity> nearby = level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(start, end).inflate(1.0),
            e -> e != shooter && e.isAlive());

        List<Hitscan.Candidate> candidates = new ArrayList<>();
        Map<String, LivingEntity> byId = new HashMap<>();
        for (LivingEntity e : nearby) {
            var clip = e.getBoundingBox().inflate(0.3).clip(start, end);
            if (clip.isPresent()) {
                String id = e.getUUID().toString();
                candidates.add(new Hitscan.Candidate(id, start.distanceTo(clip.get())));
                byId.put(id, e);
            }
        }

        var hit = Hitscan.firstHitWithinRange(blockDist, candidates);
        Vec3 endPoint = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        if (hit.isPresent()) {
            LivingEntity hitTarget = byId.get(hit.get().id());
            hitTarget.invulnerableTime = 0;
            hitTarget.hurtServer((ServerLevel) level,
                com.tweeks.wildwest.WildWestDamageTypes.gunshot(shooter), DAMAGE);
            endPoint = hitTarget.position().add(0, hitTarget.getBbHeight() * 0.5, 0);
        }

        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
            com.tweeks.wildwest.ModSounds.PISTOL_FIRE.get(),
            net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);

        // Mob shooter is not a ServerPlayer — use sendToPlayersTrackingEntity (no Self).
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(
            shooter,
            new com.tweeks.wildwest.network.S2CTracerPacket(start, endPoint));
    }
```

The `Mth` import isn't used yet — remove it. Keep `java.util.Random` if not already imported (it shouldn't be — use `shooter.getRandom()` which returns `RandomSource`; replace `java.util.Random rand` declaration with `var rand = shooter.getRandom()`).

Corrected version of the random block:

```java
        var rand = shooter.getRandom();
        double ax = rand.nextGaussian() * 0.05;
        double ay = rand.nextGaussian() * 0.05;
        double az = rand.nextGaussian() * 0.05;
```

(`shooter.getRandom()` returns `net.minecraft.util.RandomSource`, which has `nextGaussian()`.)

- [ ] **Step 2: Add `fireFromMob` to RifleItem**

In `wildwest/src/main/java/com/tweeks/wildwest/item/RifleItem.java`, add the static helper after the `inventoryTick` method:

```java
    /**
     * Mob-side firing path. Spawns a BulletEntity from shooter's eye position
     * with Gaussian aim inaccuracy. No cooldown / durability tracking.
     */
    public static void fireFromMob(net.minecraft.world.entity.LivingEntity shooter,
                                   net.minecraft.world.entity.LivingEntity target) {
        net.minecraft.world.level.Level level = shooter.level();
        if (level.isClientSide()) return;

        com.tweeks.wildwest.entity.BulletEntity bullet =
            new com.tweeks.wildwest.entity.BulletEntity(
                com.tweeks.wildwest.ModEntities.BULLET.get(), level, shooter);
        bullet.setPos(shooter.getEyePosition());

        net.minecraft.world.phys.Vec3 aimAt = target.position()
            .add(0, target.getBbHeight() * 0.5, 0);
        net.minecraft.world.phys.Vec3 dir = aimAt.subtract(shooter.getEyePosition()).normalize();
        var rand = shooter.getRandom();
        double ax = rand.nextGaussian() * 0.05;
        double ay = rand.nextGaussian() * 0.05;
        double az = rand.nextGaussian() * 0.05;
        bullet.shoot(dir.x + ax, dir.y + ay, dir.z + az, BULLET_VELOCITY, 0.0F);

        level.addFreshEntity(bullet);
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
            ModSounds.RIFLE_FIRE.get(),
            net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
    }
```

- [ ] **Step 3: Build to verify both compile**

```bash
./gradlew :wildwest:build
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/PistolItem.java wildwest/src/main/java/com/tweeks/wildwest/item/RifleItem.java
git commit -m "feat(wildwest): mob-side fireFromMob helpers on pistol + rifle

Static helpers used by Phase-2 AI goals. Same mechanics as the player
path with Gaussian aim inaccuracy (~0.05 rad). Tracer packet uses
sendToPlayersTrackingEntity (mob shooter is not a ServerPlayer)."
```

---

## Task 3: Hand weapons + their damage types

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestDamageTypes.java` (add CLUB, KNIFE)
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/data/ModDamageTypeProvider.java` (register new types)
- Create: `wildwest/src/main/java/com/tweeks/wildwest/item/BillyClubItem.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/item/BanditKnifeItem.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java` (register both items + add to creative tab)

- [ ] **Step 1: Add CLUB + KNIFE to WildWestDamageTypes**

Edit `wildwest/src/main/java/com/tweeks/wildwest/WildWestDamageTypes.java`. After the existing `GUNSHOT` field, add:

```java
    public static final ResourceKey<DamageType> CLUB = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "club"));

    public static final ResourceKey<DamageType> KNIFE = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "knife"));

    public static DamageSource club(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(CLUB),
            attacker);
    }

    public static DamageSource knife(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(KNIFE),
            attacker);
    }
```

- [ ] **Step 2: Register both types in ModDamageTypeProvider**

Edit `wildwest/src/main/java/com/tweeks/wildwest/data/ModDamageTypeProvider.java`, replace the body of `bootstrap` with:

```java
    public static void bootstrap(BootstrapContext<DamageType> ctx) {
        ctx.register(WildWestDamageTypes.GUNSHOT,
            new DamageType("wildwest.gunshot", 0.1f));
        ctx.register(WildWestDamageTypes.CLUB,
            new DamageType("wildwest.club", 0.1f));
        ctx.register(WildWestDamageTypes.KNIFE,
            new DamageType("wildwest.knife", 0.1f));
    }
```

- [ ] **Step 3: Create BillyClubItem**

`wildwest/src/main/java/com/tweeks/wildwest/item/BillyClubItem.java`:

```java
package com.tweeks.wildwest.item;

import com.tweeks.wildwest.WildWestDamageTypes;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Lawman hand weapon. 4 attack damage; on hit, applies Slowness II for 2s.
 */
public class BillyClubItem extends Item {

    private static final Identifier ATTACK_DAMAGE_ID =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "billy_club_damage");

    public BillyClubItem(Properties properties) {
        super(properties
            .stacksTo(1)
            .durability(200)
            .component(DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ATTACK_DAMAGE_ID, 4.0,
                            AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                    .build()));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        // Slowness II for 2 seconds. Amplifier 1 = level II.
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        // Tag the kill with our custom damage type for the death message.
        if (!target.isAlive() && target.level() instanceof ServerLevel sl) {
            target.hurtServer(sl, WildWestDamageTypes.club(attacker), 0.0F);
        }
    }
}
```

(The `if (!target.isAlive())` block is a no-op damage call to register the death-message attribution — it doesn't deal more damage, it just ensures the kill is attributed to the `wildwest:club` damage type. If this approach proves problematic in testing, fall back to the simpler form: just apply the slow effect and accept that the death message uses vanilla `attack` keys. The custom damage-type death message is a polish-level feature.)

- [ ] **Step 4: Create BanditKnifeItem**

`wildwest/src/main/java/com/tweeks/wildwest/item/BanditKnifeItem.java`:

```java
package com.tweeks.wildwest.item;

import com.tweeks.wildwest.WildWestDamageTypes;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Outlaw hand weapon. 5 attack damage; on hit, applies Wither I for 2s.
 */
public class BanditKnifeItem extends Item {

    private static final Identifier ATTACK_DAMAGE_ID =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "bandit_knife_damage");

    public BanditKnifeItem(Properties properties) {
        super(properties
            .stacksTo(1)
            .durability(200)
            .component(DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ATTACK_DAMAGE_ID, 5.0,
                            AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                    .build()));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
        if (!target.isAlive() && target.level() instanceof ServerLevel sl) {
            target.hurtServer(sl, WildWestDamageTypes.knife(attacker), 0.0F);
        }
    }
}
```

- [ ] **Step 5: Register both items + add to creative tab**

Edit `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`. Add imports:

```java
import com.tweeks.wildwest.item.BillyClubItem;
import com.tweeks.wildwest.item.BanditKnifeItem;
```

Add the registrations after the existing `RIFLE` field, before `WILDWEST_TAB`:

```java
    public static final DeferredItem<BillyClubItem> BILLY_CLUB = ITEMS.registerItem(
        "billy_club", BillyClubItem::new, p -> p);

    public static final DeferredItem<BanditKnifeItem> BANDIT_KNIFE = ITEMS.registerItem(
        "bandit_knife", BanditKnifeItem::new, p -> p);
```

In the creative tab `displayItems` lambda, add after the existing `output.accept(RIFLE.get());`:

```java
                    output.accept(BILLY_CLUB.get());
                    output.accept(BANDIT_KNIFE.get());
```

- [ ] **Step 6: Run datagen + build**

```bash
./gradlew :wildwest:runServerData :wildwest:build
```

Expected: BUILD SUCCESSFUL. Confirm `wildwest/src/generated/serverData/data/wildwest/damage_type/club.json` and `knife.json` were written.

- [ ] **Step 7: Commit**

```bash
git add wildwest/
git commit -m "feat(wildwest): hand weapons billy_club + bandit_knife + damage types

BillyClub: 4 atk dmg + Slowness II 2s. BanditKnife: 5 atk dmg + Wither I 2s.
Both stack-to-1, durability 200. New wildwest:club + wildwest:knife
damage types for kill-message attribution."
```

---

## Task 4: AI goals (ranged + melee + lawman target + outlaw target)

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/WildWestRangedAttackGoal.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/WildWestMeleeAttackGoal.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/LawmanTargetGoal.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/OutlawTargetGoal.java`

These goals reference `WildWestMob` (Task 5) — class doesn't exist yet, so they'll fail to compile until Task 5 lands. Resolve by writing them as generic `extends Goal` on `Mob` rather than typed on `WildWestMob`. `WildWestMob` itself will install them in its `registerGoals()`.

- [ ] **Step 1: Create WildWestRangedAttackGoal**

`wildwest/src/main/java/com/tweeks/wildwest/entity/ai/WildWestRangedAttackGoal.java`:

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.WeaponMode;
import com.tweeks.wildwest.entity.WildWestMob;
import com.tweeks.wildwest.item.PistolItem;
import com.tweeks.wildwest.item.RifleItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Fires the mob's ranged weapon when in RANGED mode and a target is in line of sight.
 * Cooldown is the gun's player-side cooldown × 1.5.
 */
public class WildWestRangedAttackGoal extends Goal {

    private final WildWestMob mob;
    private int cooldown;

    public WildWestRangedAttackGoal(WildWestMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.getWeaponMode() == WeaponMode.RANGED
            && mob.getTarget() != null
            && mob.getTarget().isAlive()
            && mob.getSensing().hasLineOfSight(mob.getTarget());
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        this.cooldown = 20; // initial wind-up
    }

    @Override
    public void stop() {
        this.cooldown = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        // Face the target.
        mob.getLookControl().setLookAt(target, 30, 30);
        if (--this.cooldown <= 0) {
            if (mob.usesRifle()) {
                RifleItem.fireFromMob(mob, target);
                this.cooldown = (int) (RifleItem.COOLDOWN_TICKS * 1.5);  // 60 ticks
            } else {
                PistolItem.fireFromMob(mob, target);
                this.cooldown = (int) (PistolItem.COOLDOWN_TICKS * 1.5);  // 12 ticks
            }
        }
    }
}
```

(`mob.usesRifle()` is a method `WildWestMob` will define in Task 5 — see that task's class definition.)

- [ ] **Step 2: Create WildWestMeleeAttackGoal**

`wildwest/src/main/java/com/tweeks/wildwest/entity/ai/WildWestMeleeAttackGoal.java`:

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.WeaponMode;
import com.tweeks.wildwest.entity.WildWestMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Standard melee attack, gated on weaponMode == MELEE. Vanilla MeleeAttackGoal
 * already handles pathing-to-target, attack-tick cooldown, and cooldown-driven
 * swing animation. The hand weapon's hurtEnemy() override applies the on-hit
 * effect (Slowness for billy_club, Wither for bandit_knife).
 */
public class WildWestMeleeAttackGoal extends MeleeAttackGoal {

    private final WildWestMob mob;

    public WildWestMeleeAttackGoal(WildWestMob mob, double speedMultiplier, boolean followingTargetEvenIfNotSeen) {
        super(mob, speedMultiplier, followingTargetEvenIfNotSeen);
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        return mob.getWeaponMode() == WeaponMode.MELEE && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return mob.getWeaponMode() == WeaponMode.MELEE && super.canContinueToUse();
    }
}
```

- [ ] **Step 3: Create LawmanTargetGoal**

`wildwest/src/main/java/com/tweeks/wildwest/entity/ai/LawmanTargetGoal.java`:

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.api.Outlaw;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/**
 * Lawmen target the nearest Outlaw within follow range. Does NOT target players
 * (player retaliation is handled by HurtByTargetGoal so sheriffs only attack
 * players who hit them — iron-golem-style).
 */
public class LawmanTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {

    public LawmanTargetGoal(Mob mob) {
        super(mob, LivingEntity.class, 5, false, false,
            (target, level) -> target instanceof Outlaw && target.isAlive());
    }
}
```

- [ ] **Step 4: Create OutlawTargetGoal**

`wildwest/src/main/java/com/tweeks/wildwest/entity/ai/OutlawTargetGoal.java`:

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.api.Lawman;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

/**
 * Outlaws target the nearest Lawman or Player within follow range. Outlaws are
 * vanilla Enemy implementers, so the player-targeting is the standard hostile-mob
 * behavior; the Lawman branch adds faction hostility on top.
 */
public class OutlawTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {

    public OutlawTargetGoal(Mob mob) {
        super(mob, LivingEntity.class, 5, false, false,
            (target, level) -> (target instanceof Lawman || target instanceof Player) && target.isAlive());
    }
}
```

- [ ] **Step 5: Build (will fail — WildWestMob doesn't exist yet)**

```bash
./gradlew :wildwest:build
```

Expected: compile errors referencing `com.tweeks.wildwest.entity.WildWestMob`. **Don't commit yet** — Task 5 introduces the class and resolves these errors.

- [ ] **Step 6: Hold off on commit**

This task's files compile only after Task 5 lands. Combine the commits when Task 5 builds clean.

---

## Task 5: WildWestMob abstract base class

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/WildWestMob.java`

This is the most complex single class in phase 2. It owns:
- `WEAPON_MODE` synced data accessor (byte: 0=RANGED, 1=MELEE)
- `tick()` distance-check that calls `WeaponMode.choose(...)` and swaps mainhand
- `registerGoals()` that wires the four AI goals from Task 4
- abstract methods that subclasses implement: `getGunStack()`, `getHandWeaponStack()`, `usesRifle()`, `isLawman()`

- [ ] **Step 1: Create WildWestMob**

`wildwest/src/main/java/com/tweeks/wildwest/entity/WildWestMob.java`:

```java
package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.entity.ai.LawmanTargetGoal;
import com.tweeks.wildwest.entity.ai.OutlawTargetGoal;
import com.tweeks.wildwest.entity.ai.WildWestMeleeAttackGoal;
import com.tweeks.wildwest.entity.ai.WildWestRangedAttackGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Abstract base class for all four wild-west mobs (deputy, sherrif, bandit,
 * bandit-leader). Owns the weapon-mode synced state, the tick-driven mainhand
 * swap with hysteresis, and the shared goal stack.
 */
public abstract class WildWestMob extends PathfinderMob {

    private static final EntityDataAccessor<Byte> DATA_WEAPON_MODE =
        SynchedEntityData.defineId(WildWestMob.class, EntityDataSerializers.BYTE);

    /** Frequency of the distance-check tick in WildWestMob.tick(). */
    private static final int CHECK_INTERVAL_TICKS = 5;
    /** Lock duration after a swap. Prevents thrashing at boundary. */
    private static final int HYSTERESIS_TICKS = 20;

    private int hysteresisLockTicks = 0;
    private int tickCounter = 0;

    protected WildWestMob(EntityType<? extends WildWestMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WEAPON_MODE, (byte) WeaponMode.RANGED.ordinal());
    }

    public WeaponMode getWeaponMode() {
        byte b = this.entityData.get(DATA_WEAPON_MODE);
        return WeaponMode.values()[b];
    }

    private void setWeaponMode(WeaponMode mode) {
        this.entityData.set(DATA_WEAPON_MODE, (byte) mode.ordinal());
    }

    /** Subclass-supplied gun ItemStack. Called once at spawn, used to equip MAINHAND in RANGED mode. */
    protected abstract ItemStack getGunStack();

    /** Subclass-supplied hand-weapon ItemStack. Used to equip MAINHAND in MELEE mode. */
    protected abstract ItemStack getHandWeaponStack();

    /** True if this mob's gun is a rifle (subclass routes to RifleItem.fireFromMob). */
    public abstract boolean usesRifle();

    /** True if this mob is a Lawman (sheriff, deputy). False if Outlaw. */
    public abstract boolean isLawman();

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WildWestRangedAttackGoal(this));
        this.goalSelector.addGoal(2, new WildWestMeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Faction targeting differs by subclass type — installed via the abstract isLawman()
        // hook so we don't need different registerGoals overrides per mob.
        if (this.isLawman()) {
            this.targetSelector.addGoal(2, new LawmanTargetGoal(this));
        } else {
            this.targetSelector.addGoal(2, new OutlawTargetGoal(this));
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) return;
        if (this.hysteresisLockTicks > 0) this.hysteresisLockTicks--;

        if (++this.tickCounter < CHECK_INTERVAL_TICKS) return;
        this.tickCounter = 0;

        var target = this.getTarget();
        if (target == null) {
            // No target — be in RANGED with gun visible (default idle stance).
            if (this.getWeaponMode() != WeaponMode.RANGED) {
                this.setWeaponMode(WeaponMode.RANGED);
                this.setItemSlot(EquipmentSlot.MAINHAND, this.getGunStack());
                this.hysteresisLockTicks = HYSTERESIS_TICKS;
            }
            return;
        }

        double dist = this.distanceTo(target);
        WeaponMode current = this.getWeaponMode();
        WeaponMode next = WeaponMode.choose(dist, current, this.hysteresisLockTicks);
        if (next != current) {
            this.setWeaponMode(next);
            this.setItemSlot(EquipmentSlot.MAINHAND,
                next == WeaponMode.MELEE ? this.getHandWeaponStack() : this.getGunStack());
            this.hysteresisLockTicks = HYSTERESIS_TICKS;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("WeaponMode", (byte) this.getWeaponMode().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("WeaponMode")) {
            byte b = tag.getByte("WeaponMode");
            if (b >= 0 && b < WeaponMode.values().length) {
                this.entityData.set(DATA_WEAPON_MODE, b);
            }
        }
    }
}
```

The `tag.getByte("WeaponMode")` call may have changed signature in MC 26.1.2 — newer versions ship `tag.getByteOr("WeaponMode", (byte) 0)` or `tag.getByte("WeaponMode").orElse((byte) 0)`. Adapt at compile time if needed.

- [ ] **Step 2: Build (Tasks 4 + 5 together)**

```bash
./gradlew :wildwest:build
```

Expected: BUILD SUCCESSFUL. Tasks 4's goals now resolve to the new `WildWestMob`.

- [ ] **Step 3: Commit Tasks 4 + 5 together**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ wildwest/src/main/java/com/tweeks/wildwest/entity/ai/
git commit -m "feat(wildwest): WildWestMob base + 4 AI goals

Abstract base: synced WEAPON_MODE state, 5-tick distance check, 20-tick
hysteresis lock on swaps. Subclasses supply gun/hand-weapon stacks +
faction membership. AI goals: ranged attack (delegates to fireFromMob),
melee attack (extends MeleeAttackGoal gated on mode), Lawman/Outlaw
target goals."
```

---

## Task 6: Concrete mob entities — DeputyEntity + BanditEntity (footsoldiers)

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/DeputyEntity.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/BanditEntity.java`

- [ ] **Step 1: Create DeputyEntity**

```java
package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.Registration;
import com.tweeks.wildwest.api.Lawman;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DeputyEntity extends WildWestMob implements Lawman {

    public DeputyEntity(EntityType<? extends DeputyEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.MOVEMENT_SPEED, 0.30)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected ItemStack getGunStack() {
        return new ItemStack(Registration.PISTOL.get());
    }

    @Override
    protected ItemStack getHandWeaponStack() {
        return new ItemStack(Registration.BILLY_CLUB.get());
    }

    @Override
    public boolean usesRifle() { return false; }

    @Override
    public boolean isLawman() { return true; }
}
```

The `PathfinderMob.createMobAttributes()` static needs the explicit class import `net.minecraft.world.entity.PathfinderMob`. Add it.

- [ ] **Step 2: Create BanditEntity**

```java
package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.Registration;
import com.tweeks.wildwest.api.Outlaw;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BanditEntity extends WildWestMob implements Outlaw, Enemy {

    public BanditEntity(EntityType<? extends BanditEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.MOVEMENT_SPEED, 0.30)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected ItemStack getGunStack() {
        return new ItemStack(Registration.PISTOL.get());
    }

    @Override
    protected ItemStack getHandWeaponStack() {
        return new ItemStack(Registration.BANDIT_KNIFE.get());
    }

    @Override
    public boolean usesRifle() { return false; }

    @Override
    public boolean isLawman() { return false; }
}
```

- [ ] **Step 3: Build (will fail — entity types not registered yet)**

```bash
./gradlew :wildwest:build
```

Expected: BUILD SUCCESSFUL (no entity-type registration is needed for the class to compile; only for runtime spawning). Hold the commit until Task 8 registers the types.

---

## Task 7: Concrete mob entities — SherrifEntity + BanditLeaderEntity (leaders)

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/SherrifEntity.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/BanditLeaderEntity.java`

- [ ] **Step 1: Create SherrifEntity**

```java
package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.Registration;
import com.tweeks.wildwest.api.Lawman;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SherrifEntity extends WildWestMob implements Lawman {

    public SherrifEntity(EntityType<? extends SherrifEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 28.0)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected ItemStack getGunStack() {
        return new ItemStack(Registration.RIFLE.get());
    }

    @Override
    protected ItemStack getHandWeaponStack() {
        return new ItemStack(Registration.BILLY_CLUB.get());
    }

    @Override
    public boolean usesRifle() { return true; }

    @Override
    public boolean isLawman() { return true; }
}
```

- [ ] **Step 2: Create BanditLeaderEntity**

```java
package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.Registration;
import com.tweeks.wildwest.api.Outlaw;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BanditLeaderEntity extends WildWestMob implements Outlaw, Enemy {

    public BanditLeaderEntity(EntityType<? extends BanditLeaderEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 28.0)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected ItemStack getGunStack() {
        return new ItemStack(Registration.RIFLE.get());
    }

    @Override
    protected ItemStack getHandWeaponStack() {
        return new ItemStack(Registration.BANDIT_KNIFE.get());
    }

    @Override
    public boolean usesRifle() { return true; }

    @Override
    public boolean isLawman() { return false; }
}
```

- [ ] **Step 3: Build**

```bash
./gradlew :wildwest:build
```

Expected: BUILD SUCCESSFUL. Hold commit until Task 8 ties the runtime registration together.

---

## Task 8: Entity-type registration + spawn eggs + creative tab + attribute registration

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`

- [ ] **Step 1: Register the four entity types**

In `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java`, add imports:

```java
import com.tweeks.wildwest.entity.DeputyEntity;
import com.tweeks.wildwest.entity.SherrifEntity;
import com.tweeks.wildwest.entity.BanditEntity;
import com.tweeks.wildwest.entity.BanditLeaderEntity;
```

Add the four registrations after the existing `BULLET` field:

```java
    public static final DeferredHolder<EntityType<?>, EntityType<DeputyEntity>> DEPUTY =
        ENTITY_TYPES.register("deputy", () -> EntityType.Builder.<DeputyEntity>of(
                DeputyEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "deputy"))));

    public static final DeferredHolder<EntityType<?>, EntityType<SherrifEntity>> SHERRIF =
        ENTITY_TYPES.register("sherrif", () -> EntityType.Builder.<SherrifEntity>of(
                SherrifEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "sherrif"))));

    public static final DeferredHolder<EntityType<?>, EntityType<BanditEntity>> BANDIT =
        ENTITY_TYPES.register("bandit", () -> EntityType.Builder.<BanditEntity>of(
                BanditEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "bandit"))));

    public static final DeferredHolder<EntityType<?>, EntityType<BanditLeaderEntity>> BANDIT_LEADER =
        ENTITY_TYPES.register("bandit_leader", () -> EntityType.Builder.<BanditLeaderEntity>of(
                BanditLeaderEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "bandit_leader"))));
```

- [ ] **Step 2: Register the four spawn eggs and add to creative tab**

In `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`, add imports:

```java
import net.minecraft.world.item.SpawnEggItem;
```

After the `BANDIT_KNIFE` field, add:

```java
    public static final DeferredItem<SpawnEggItem> DEPUTY_SPAWN_EGG = ITEMS.registerItem(
        "deputy_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.DEPUTY.get()));

    public static final DeferredItem<SpawnEggItem> SHERRIF_SPAWN_EGG = ITEMS.registerItem(
        "sherrif_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.SHERRIF.get()));

    public static final DeferredItem<SpawnEggItem> BANDIT_SPAWN_EGG = ITEMS.registerItem(
        "bandit_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.BANDIT.get()));

    public static final DeferredItem<SpawnEggItem> BANDIT_LEADER_SPAWN_EGG = ITEMS.registerItem(
        "bandit_leader_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.BANDIT_LEADER.get()));
```

(`SpawnEggItem` constructor signature in MC 26.1.2 may differ — recent versions accept just `Item.Properties`, with the entity type set via `Properties.spawnEgg(EntityType)`. If `SpawnEggItem::new` doesn't compile against `Item.Properties` alone, fall back to the older 3-arg constructor `(EntityType, int, int, Properties)` — pass background/spot colors as ints.)

In the creative tab `displayItems` lambda, after the existing item entries, add:

```java
                    output.accept(DEPUTY_SPAWN_EGG.get());
                    output.accept(SHERRIF_SPAWN_EGG.get());
                    output.accept(BANDIT_SPAWN_EGG.get());
                    output.accept(BANDIT_LEADER_SPAWN_EGG.get());
```

- [ ] **Step 3: Wire entity-attribute registration**

In `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`, add imports:

```java
import com.tweeks.wildwest.entity.DeputyEntity;
import com.tweeks.wildwest.entity.SherrifEntity;
import com.tweeks.wildwest.entity.BanditEntity;
import com.tweeks.wildwest.entity.BanditLeaderEntity;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
```

In the constructor, after `ModEntities.register(modEventBus);`, add:

```java
        modEventBus.addListener(WildWestMod::registerEntityAttributes);
```

And add the static method:

```java
    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.DEPUTY.get(), DeputyEntity.createAttributes().build());
        event.put(ModEntities.SHERRIF.get(), SherrifEntity.createAttributes().build());
        event.put(ModEntities.BANDIT.get(), BanditEntity.createAttributes().build());
        event.put(ModEntities.BANDIT_LEADER.get(), BanditLeaderEntity.createAttributes().build());
    }
```

- [ ] **Step 4: Build**

```bash
./gradlew :wildwest:build
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit Tasks 6 + 7 + 8 together**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java wildwest/src/main/java/com/tweeks/wildwest/Registration.java wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java
git commit -m "feat(wildwest): four mob entities + spawn eggs + attribute registration

Deputy/Sherrif (Lawman, CREATURE), Bandit/BanditLeader (Outlaw + Enemy,
MONSTER). Footsoldiers 20HP/0.30/2dmg/24follow; Leaders 28/0.28/3/32.
Each subclass supplies gun + hand-weapon stack and faction membership."
```

---

## Task 9: Spawning — outlaw biome modifier + lawman village periodic-tick

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/data/ModBiomeModifierProvider.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/data/DataGenerators.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/spawning/OutlawSpawnRules.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java` (register spawn placements + village-tick)
- Create: `wildwest/src/main/java/com/tweeks/wildwest/spawning/LawmanVillageSpawner.java`

- [ ] **Step 1: Create OutlawSpawnRules**

`wildwest/src/main/java/com/tweeks/wildwest/spawning/OutlawSpawnRules.java`:

```java
package com.tweeks.wildwest.spawning;

import com.tweeks.wildwest.entity.BanditEntity;
import com.tweeks.wildwest.entity.BanditLeaderEntity;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnSettings;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public final class OutlawSpawnRules {
    private OutlawSpawnRules() {}

    /** Outlaws need the same conditions as zombies — surface, dim light. */
    public static boolean checkSpawnRules(EntityType<? extends BanditEntity> type,
                                          ServerLevelAccessor level,
                                          EntitySpawnReason reason,
                                          net.minecraft.core.BlockPos pos,
                                          RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }

    public static boolean checkLeaderSpawnRules(EntityType<? extends BanditLeaderEntity> type,
                                                ServerLevelAccessor level,
                                                EntitySpawnReason reason,
                                                net.minecraft.core.BlockPos pos,
                                                RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }
}
```

(Method signatures may need adjustment — `EntitySpawnReason` vs older `MobSpawnType` enum varies by version. Compile, then adapt to whichever exists. The semantic is "delegate to vanilla zombie-spawn check.")

- [ ] **Step 2: Wire SpawnPlacements in WildWestMod**

In `WildWestMod.java`, add to the constructor:

```java
        modEventBus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) -> {
            event.register(ModEntities.BANDIT.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.wildwest.spawning.OutlawSpawnRules::checkSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.BANDIT_LEADER.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.wildwest.spawning.OutlawSpawnRules::checkLeaderSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
```

- [ ] **Step 3: Create the biome-modifier datagen provider**

`wildwest/src/main/java/com/tweeks/wildwest/data/ModBiomeModifierProvider.java`:

```java
package com.tweeks.wildwest.data;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModBiomeModifierProvider {
    private ModBiomeModifierProvider() {}

    private static final ResourceKey<BiomeModifier> ADD_BANDITS = ResourceKey.create(
        NeoForgeRegistries.Keys.BIOME_MODIFIERS,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "add_bandits"));

    private static final ResourceKey<BiomeModifier> ADD_BANDIT_LEADERS = ResourceKey.create(
        NeoForgeRegistries.Keys.BIOME_MODIFIERS,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "add_bandit_leaders"));

    public static void bootstrap(BootstrapContext<BiomeModifier> ctx) {
        var biomes = ctx.lookup(Registries.BIOME);

        // Plains, savanna, desert tag — covers the wild-west aesthetic.
        HolderSet<Biome> targetBiomes = HolderSet.direct(
            biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS),
            biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.SAVANNA),
            biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.SAVANNA_PLATEAU),
            biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.DESERT));

        ctx.register(ADD_BANDITS, new BiomeModifiers.AddSpawnsBiomeModifier(
            targetBiomes,
            java.util.List.of(new MobSpawnSettings.SpawnerData(
                ModEntities.BANDIT.get(), 5, 1, 3))));

        ctx.register(ADD_BANDIT_LEADERS, new BiomeModifiers.AddSpawnsBiomeModifier(
            targetBiomes,
            java.util.List.of(new MobSpawnSettings.SpawnerData(
                ModEntities.BANDIT_LEADER.get(), 1, 1, 1))));
    }
}
```

- [ ] **Step 4: Wire biome modifier into DataGenerators**

In `wildwest/src/main/java/com/tweeks/wildwest/data/DataGenerators.java`, modify the `RegistrySetBuilder` chain in `gatherDataServer` to also register biome modifiers:

```java
        RegistrySetBuilder builder = new RegistrySetBuilder()
            .add(Registries.DAMAGE_TYPE, ModDamageTypeProvider::bootstrap)
            .add(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                 ModBiomeModifierProvider::bootstrap);
```

- [ ] **Step 5: Create LawmanVillageSpawner**

`wildwest/src/main/java/com/tweeks/wildwest/spawning/LawmanVillageSpawner.java`:

```java
package com.tweeks.wildwest.spawning;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.entity.DeputyEntity;
import com.tweeks.wildwest.entity.SherrifEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;

@EventBusSubscriber(modid = WildWestMod.MOD_ID)
public final class LawmanVillageSpawner {
    private LawmanVillageSpawner() {}

    private static final int CHECK_INTERVAL_TICKS = 6000;
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (++tickCounter < CHECK_INTERVAL_TICKS) return;
        tickCounter = 0;

        // Iterate plains-village structure starts in loaded chunks.
        var random = sl.getRandom();
        sl.structureManager().registryAccess()
            .lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
            .listElements()
            .filter(holder -> holder.is(StructureTags.VILLAGE))
            .forEach(structureHolder -> {
                // For each loaded chunk where this structure starts, spawn lawmen.
                // Simplification: scan all loaded chunks and check structure-presence.
                for (var chunk : sl.getChunkSource().chunkMap.getChunks()) {
                    StructureStart start = chunk.getChunk().getStartForStructure(structureHolder.value());
                    if (start == null || !start.isValid()) continue;
                    BoundingBox bb = start.getBoundingBox();
                    AABB aabb = AABB.of(bb);

                    int deputyCount = sl.getEntitiesOfClass(DeputyEntity.class, aabb).size();
                    int sherrifCount = sl.getEntitiesOfClass(SherrifEntity.class, aabb).size();

                    if (deputyCount < 2 && random.nextFloat() < 0.10f) {
                        spawnAt(sl, ModEntities.DEPUTY.get(), bb, random);
                    }
                    if (sherrifCount < 1 && random.nextFloat() < 0.05f) {
                        spawnAt(sl, ModEntities.SHERRIF.get(), bb, random);
                    }
                }
            });
    }

    private static <T extends net.minecraft.world.entity.Entity> void spawnAt(
            ServerLevel sl, net.minecraft.world.entity.EntityType<T> type,
            BoundingBox bb, net.minecraft.util.RandomSource random) {
        int x = bb.minX() + random.nextInt(bb.getXSpan());
        int z = bb.minZ() + random.nextInt(bb.getZSpan());
        int y = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        type.spawn(sl, pos, EntitySpawnReason.NATURAL);
    }
}
```

The exact API for `chunkMap.getChunks()` and `getStartForStructure()` may vary between MC minor versions. **If the chunk-iteration API isn't available**, simplify by iterating all loaded chunks via `sl.getChunkSource().getLoadedChunks()` or similar, then for each chunk read its structure starts. Implementer adapts to whichever API surface exists; the algorithm is: every 6000 ticks, find village structure starts, count lawmen inside their bounding boxes, possibly spawn more.

If the implementer can't get the structure-iteration to work, **fallback**: scan all loaded `Villager` POI positions via `sl.getPoiManager()` and use their proximity as a village proxy. Acceptable degradation.

- [ ] **Step 6: Run datagen + build**

```bash
./gradlew :wildwest:runServerData :wildwest:build
```

Expected: BUILD SUCCESSFUL. Confirm `wildwest/src/generated/serverData/data/wildwest/neoforge/biome_modifier/add_bandits.json` and `add_bandit_leaders.json` are written.

- [ ] **Step 7: Commit**

```bash
git add wildwest/
git commit -m "feat(wildwest): outlaw biome spawning + lawman village periodic tick

Bandits weight 5 pack 1-3, leaders weight 1 pack 1, both in
plains/savanna/savanna_plateau/desert at light <=7. Lawmen spawn near
village structure starts every 5 game minutes (deputy <2 -> 10%,
sherrif <1 -> 5%)."
```

---

## Task 10: Mob renderers + WildWestHeldItemLayer + ClientSetup wiring

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/WildWestHeldItemLayer.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/DeputyRenderer.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/SherrifRenderer.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/BanditRenderer.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/BanditLeaderRenderer.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java`

- [ ] **Step 1: Create WildWestHeldItemLayer**

`wildwest/src/main/java/com/tweeks/wildwest/client/WildWestHeldItemLayer.java`:

```java
package com.tweeks.wildwest.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the entity's MAINHAND item in its right hand. Reads the held-item
 * stack from the synced render state (set by the renderer). Mirrors the
 * structure of vanilla ItemInHandLayer but limited to MAINHAND.
 */
public class WildWestHeldItemLayer
        extends RenderLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private final ItemRenderer itemRenderer;

    public WildWestHeldItemLayer(
            RenderLayerParent<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> parent,
            ItemRenderer itemRenderer) {
        super(parent);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light,
                       HumanoidRenderState state, float yaw, float partialTicks) {
        ItemStack stack = state.rightHandItem;
        if (stack == null || stack.isEmpty()) return;

        pose.pushPose();
        this.getParentModel().translateToHand(HumanoidArm.RIGHT, pose);
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.translate(0.0F, 0.125F, -0.625F);
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            light, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
            pose, buffers, state.entity.level(), 0);
        pose.popPose();
    }
}
```

`HumanoidRenderState.rightHandItem` is the standard render-state field for held items. If the actual MC 26.1.2 field name is `mainHandItem` or another, adapt. The pattern is: read from render state (populated by renderer), draw at the right-hand bone via `model.translateToHand`.

- [ ] **Step 2: Create the four mob renderers**

All four follow the same pattern. Create `DeputyRenderer.java`:

```java
package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.entity.DeputyEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class DeputyRenderer
        extends HumanoidMobRenderer<DeputyEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/deputy.png");

    public DeputyRenderer(EntityRendererProvider.Context context) {
        super(context,
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
            0.5F);
        this.addLayer(new WildWestHeldItemLayer(this, context.getItemRenderer()));
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }

    @Override
    public void extractRenderState(DeputyEntity entity, HumanoidRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.rightHandItem = entity.getMainHandItem();
    }
}
```

(`extractRenderState` may have a slightly different signature in MC 26.1.2 — confirm at compile-time. Goal: copy `entity.getMainHandItem()` into the render state so the layer can read it.)

`SherrifRenderer.java` — same code, swap `Deputy` → `Sherrif`, texture file `sherrif.png`.

`BanditRenderer.java` — same code, swap `Deputy` → `Bandit`, texture file `bandit.png`.

`BanditLeaderRenderer.java` — same code, swap `Deputy` → `BanditLeader`, texture file `bandit_leader.png`.

- [ ] **Step 3: Wire renderers in ClientSetup**

In `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java`, in the existing `registerRenderers` method, add after the BULLET registration:

```java
        event.registerEntityRenderer(ModEntities.DEPUTY.get(), DeputyRenderer::new);
        event.registerEntityRenderer(ModEntities.SHERRIF.get(), SherrifRenderer::new);
        event.registerEntityRenderer(ModEntities.BANDIT.get(), BanditRenderer::new);
        event.registerEntityRenderer(ModEntities.BANDIT_LEADER.get(), BanditLeaderRenderer::new);
```

(Imports for the four new renderer classes will be needed.)

- [ ] **Step 4: Build**

```bash
./gradlew :wildwest:build
```

Expected: BUILD SUCCESSFUL. (Without textures the mobs will render as missing-texture magenta — that's fine, textures come in Task 11.)

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/client/
git commit -m "feat(wildwest): mob renderers + WildWestHeldItemLayer

Four mobs reuse vanilla HumanoidModel + standard player layer location.
Custom held-item layer reads the synced MAINHAND ItemStack from the
render state and draws it in the right hand."
```

---

## Task 11: Mob entity textures + 3D entity model JSONs (placeholders)

**Files:**
- Create: `wildwest/src/main/resources/assets/wildwest/textures/entity/deputy.png` (64×64 placeholder)
- Create: `wildwest/src/main/resources/assets/wildwest/textures/entity/sherrif.png` (64×64)
- Create: `wildwest/src/main/resources/assets/wildwest/textures/entity/bandit.png` (64×64)
- Create: `wildwest/src/main/resources/assets/wildwest/textures/entity/bandit_leader.png` (64×64)
- Update: `wildwest/tools/README.md` (add note about new entity bbmodels)

The four mob renderers reuse vanilla `HumanoidModel` (the player skeleton). Skinning is per-mob via the four PNG textures. Phase 2 ships solid-colour placeholders; user iterates in Blockbench (each mob's texture maps onto the standard 64×64 player UV layout).

- [ ] **Step 1: Generate four placeholder textures**

```bash
mkdir -p wildwest/src/main/resources/assets/wildwest/textures/entity

# Deputy: tan/brown sheriff colors with a star (placeholder = solid tan)
magick -size 64x64 xc:'#A88060' wildwest/src/main/resources/assets/wildwest/textures/entity/deputy.png
# Sheriff: darker brown with a hat (placeholder = solid darker tan)
magick -size 64x64 xc:'#7A5240' wildwest/src/main/resources/assets/wildwest/textures/entity/sherrif.png
# Bandit: dark colors with bandanna (placeholder = solid dark grey)
magick -size 64x64 xc:'#3A3A3A' wildwest/src/main/resources/assets/wildwest/textures/entity/bandit.png
# Bandit Leader: black with red trim (placeholder = solid black)
magick -size 64x64 xc:'#1A1A1A' wildwest/src/main/resources/assets/wildwest/textures/entity/bandit_leader.png
```

Each placeholder is a flat-colour 64×64 PNG. Vanilla `HumanoidModel` will tile this onto the standard player UV regions.

- [ ] **Step 2: Update tools/README.md**

Append to the existing `wildwest/tools/README.md`:

```markdown

## Phase 2 mob bbmodels

The four mobs in phase 2 (deputy, sherrif, bandit, bandit_leader) reuse
the vanilla `HumanoidModel` (player skeleton). Texture authoring goes
through the standard 64×64 player UV layout:

- Head: top-left 32×16
- Body: 16×16 starting at (16, 16)
- Right arm / left arm / right leg / left leg: their standard regions

To author in Blockbench:

1. **File → New → Modded Entity** (or open a copy of `securityguard/tools/security_guard.bbmodel` as a template if you want a pre-built humanoid skeleton).
2. Import the placeholder texture, paint over it.
3. Export as PNG, save to `assets/wildwest/textures/entity/<mob_id>.png` overwriting the placeholder.
4. Save the .bbmodel source in `wildwest/tools/<mob_id>.bbmodel` for future iteration.
```

- [ ] **Step 3: Build**

```bash
./gradlew :wildwest:build
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/resources/assets/wildwest/textures/entity/ wildwest/tools/README.md
git commit -m "feat(wildwest): placeholder mob textures + bbmodel notes

Four 64×64 solid-colour placeholders per mob; vanilla HumanoidModel
maps them onto the standard player UV. Iterate in Blockbench."
```

---

## Task 12: Hand-weapon item textures + 3D item model JSONs

**Files:**
- Create: `wildwest/src/main/resources/assets/wildwest/textures/item/billy_club.png`
- Create: `wildwest/src/main/resources/assets/wildwest/textures/item/bandit_knife.png`
- Create: `wildwest/src/main/resources/assets/wildwest/items/billy_club.json`
- Create: `wildwest/src/main/resources/assets/wildwest/items/bandit_knife.json`
- Create: `wildwest/src/main/resources/assets/wildwest/items/deputy_spawn_egg.json`
- Create: `wildwest/src/main/resources/assets/wildwest/items/sherrif_spawn_egg.json`
- Create: `wildwest/src/main/resources/assets/wildwest/items/bandit_spawn_egg.json`
- Create: `wildwest/src/main/resources/assets/wildwest/items/bandit_leader_spawn_egg.json`
- Create: `wildwest/src/main/resources/assets/wildwest/models/item/billy_club.json` (3D cubes)
- Create: `wildwest/src/main/resources/assets/wildwest/models/item/bandit_knife.json` (3D cubes)

Spawn eggs use the vanilla `template_spawn_egg` parent — no custom model needed.

- [ ] **Step 1: Generate textures**

```bash
magick -size 16x16 xc:'#5A3820' wildwest/src/main/resources/assets/wildwest/textures/item/billy_club.png
magick -size 16x16 xc:'#A0A0A0' wildwest/src/main/resources/assets/wildwest/textures/item/bandit_knife.png
```

- [ ] **Step 2: Item entry JSONs**

`wildwest/src/main/resources/assets/wildwest/items/billy_club.json`:

```json
{ "model": { "type": "minecraft:model", "model": "wildwest:item/billy_club" } }
```

`wildwest/src/main/resources/assets/wildwest/items/bandit_knife.json`:

```json
{ "model": { "type": "minecraft:model", "model": "wildwest:item/bandit_knife" } }
```

`wildwest/src/main/resources/assets/wildwest/items/deputy_spawn_egg.json`:

```json
{ "model": { "type": "minecraft:special", "model": { "type": "minecraft:spawn_egg" }, "base": "minecraft:item/template_spawn_egg" } }
```

(If `minecraft:special`/`spawn_egg` is the wrong wrapper for spawn eggs in MC 26.1.2, use the simpler form `{ "model": { "type": "minecraft:model", "model": "minecraft:item/template_spawn_egg" } }` — vanilla spawn eggs are flat-2D items, so the standard model reference is acceptable. Visual color comes from the SpawnEggItem registration (background + spot colors), set in Task 8.)

`sherrif_spawn_egg.json`, `bandit_spawn_egg.json`, `bandit_leader_spawn_egg.json` — same content.

- [ ] **Step 3: 3D model JSONs**

`wildwest/src/main/resources/assets/wildwest/models/item/billy_club.json`:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "wildwest:item/billy_club",
    "particle": "wildwest:item/billy_club"
  },
  "elements": [
    {
      "from": [7, 1, 6],
      "to": [9, 13, 8],
      "faces": {
        "north": { "uv": [0, 0, 2, 12], "texture": "#0" },
        "east":  { "uv": [2, 0, 4, 12], "texture": "#0" },
        "south": { "uv": [4, 0, 6, 12], "texture": "#0" },
        "west":  { "uv": [6, 0, 8, 12], "texture": "#0" },
        "up":    { "uv": [8, 0, 10, 2], "texture": "#0" },
        "down":  { "uv": [10, 0, 12, 2], "texture": "#0" }
      }
    }
  ],
  "display": {
    "thirdperson_righthand": { "rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [1, 1, 1] },
    "firstperson_righthand": { "rotation": [0, -10, 0], "translation": [1, 4.5, 2], "scale": [1.2, 1.2, 1.2] }
  }
}
```

`wildwest/src/main/resources/assets/wildwest/models/item/bandit_knife.json`:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "wildwest:item/bandit_knife",
    "particle": "wildwest:item/bandit_knife"
  },
  "elements": [
    {
      "from": [7, 4, 7],
      "to": [9, 14, 9],
      "faces": {
        "north": { "uv": [0, 0, 2, 10], "texture": "#0" },
        "east":  { "uv": [2, 0, 4, 10], "texture": "#0" },
        "south": { "uv": [4, 0, 6, 10], "texture": "#0" },
        "west":  { "uv": [6, 0, 8, 10], "texture": "#0" },
        "up":    { "uv": [8, 0, 10, 2], "texture": "#0" },
        "down":  { "uv": [10, 0, 12, 2], "texture": "#0" }
      }
    },
    {
      "from": [7, 1, 7],
      "to": [9, 4, 9],
      "faces": {
        "north": { "uv": [0, 10, 2, 13], "texture": "#0" },
        "east":  { "uv": [2, 10, 4, 13], "texture": "#0" },
        "south": { "uv": [4, 10, 6, 13], "texture": "#0" },
        "west":  { "uv": [6, 10, 8, 13], "texture": "#0" },
        "up":    { "uv": [8, 10, 10, 12], "texture": "#0" },
        "down":  { "uv": [10, 10, 12, 12], "texture": "#0" }
      }
    }
  ],
  "display": {
    "thirdperson_righthand": { "rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [1, 1, 1] },
    "firstperson_righthand": { "rotation": [0, -10, 0], "translation": [1, 4.5, 2], "scale": [1.2, 1.2, 1.2] }
  }
}
```

- [ ] **Step 4: Build**

```bash
./gradlew :wildwest:build
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/resources/assets/
git commit -m "feat(wildwest): hand-weapon textures + 3D item models + spawn-egg item JSONs"
```

---

## Task 13: Loot tables (datagen)

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/data/ModEntityLootProvider.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/data/DataGenerators.java`

Reference: thief module's `ModEntityLootProvider.java` is the closest pattern. Each mob's loot table = pool with weighted items + EmptyLootItem for the "did not drop" branch.

- [ ] **Step 1: Create ModEntityLootProvider**

`wildwest/src/main/java/com/tweeks/wildwest/data/ModEntityLootProvider.java`:

```java
package com.tweeks.wildwest.data;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Set;
import java.util.stream.Stream;

public class ModEntityLootProvider extends EntityLootSubProvider {

    public ModEntityLootProvider(HolderLookup.Provider lookup) {
        super(FeatureFlags.REGISTRY.allFlags(), lookup);
    }

    @Override
    public void generate() {
        // Deputy: 0-1 billy_club @ 25%; 0-2 emerald @ 60%
        this.add(ModEntities.DEPUTY.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Registration.BILLY_CLUB.get()).setWeight(25))
                    .add(EmptyLootItem.emptyItem().setWeight(75)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.EMERALD).setWeight(60)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0f, 2.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(40))));

        // Sherrif: 0-1 billy_club @ 25%; 1-3 emerald @ 80%; 0-1 iron_ingot @ 30%
        this.add(ModEntities.SHERRIF.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Registration.BILLY_CLUB.get()).setWeight(25))
                    .add(EmptyLootItem.emptyItem().setWeight(75)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.EMERALD).setWeight(80)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 3.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(20)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(30))
                    .add(EmptyLootItem.emptyItem().setWeight(70))));

        // Bandit: 0-1 bandit_knife @ 25%; 0-1 gold_ingot @ 30%; 0-2 string @ 50%
        this.add(ModEntities.BANDIT.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Registration.BANDIT_KNIFE.get()).setWeight(25))
                    .add(EmptyLootItem.emptyItem().setWeight(75)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(30))
                    .add(EmptyLootItem.emptyItem().setWeight(70)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.STRING).setWeight(50)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0f, 2.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(50))));

        // BanditLeader: 0-1 bandit_knife @ 30%; 1-3 gold_ingot @ 60%; 0-1 emerald @ 30%
        this.add(ModEntities.BANDIT_LEADER.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Registration.BANDIT_KNIFE.get()).setWeight(30))
                    .add(EmptyLootItem.emptyItem().setWeight(70)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(60)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 3.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(40)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.EMERALD).setWeight(30))
                    .add(EmptyLootItem.emptyItem().setWeight(70))));
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return Set.of(
            ModEntities.DEPUTY.get(),
            ModEntities.SHERRIF.get(),
            ModEntities.BANDIT.get(),
            ModEntities.BANDIT_LEADER.get()
        ).stream().map(t -> (EntityType<?>) t);
    }
}
```

- [ ] **Step 2: Wire into DataGenerators**

In `wildwest/src/main/java/com/tweeks/wildwest/data/DataGenerators.java`, in `gatherDataServer`, add after the recipe provider:

```java
        gen.addProvider(true, new net.minecraft.data.loot.LootTableProvider(
            output,
            java.util.Set.of(),
            java.util.List.of(new net.minecraft.data.loot.LootTableProvider.SubProviderEntry(
                ModEntityLootProvider::new,
                net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY)),
            lookup));
```

- [ ] **Step 3: Run datagen + build**

```bash
./gradlew :wildwest:runServerData :wildwest:build
```

Expected: writes 4 loot-table JSONs in `src/generated/serverData/data/wildwest/loot_table/entities/`. Confirm.

- [ ] **Step 4: Commit**

```bash
git add wildwest/
git commit -m "feat(wildwest): loot tables for 4 mobs (datagen)

Deputy/Sherrif drop billy_club + emeralds. Bandit/BanditLeader drop
bandit_knife + gold + string/emerald. Probabilities per spec table."
```

---

## Task 14: Recipes (datagen)

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/data/ModRecipeProvider.java`

- [ ] **Step 1: Add billy_club + bandit_knife recipes**

In `ModRecipeProvider.buildRecipes()`, after the existing pistol + rifle recipes, add:

```java
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.BILLY_CLUB.get())
            .pattern(" I ")
            .pattern(" W ")
            .pattern(" W ")
            .define('I', Items.IRON_INGOT)
            .define('W', Items.OAK_PLANKS)
            .unlockedBy("has_iron", this.has(Items.IRON_INGOT))
            .save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.BANDIT_KNIFE.get())
            .pattern(" I")
            .pattern("L ")
            .define('I', Items.IRON_INGOT)
            .define('L', Items.LEATHER)
            .unlockedBy("has_iron", this.has(Items.IRON_INGOT))
            .save(this.output);
```

- [ ] **Step 2: Run datagen + build**

```bash
./gradlew :wildwest:runServerData :wildwest:build
```

Expected: writes `recipe/billy_club.json` and `recipe/bandit_knife.json`. Confirm.

- [ ] **Step 3: Commit**

```bash
git add wildwest/
git commit -m "feat(wildwest): billy_club + bandit_knife recipes (datagen)"
```

---

## Task 15: Localization (datagen)

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/data/ModLanguageProvider.java`

- [ ] **Step 1: Add new keys to ModLanguageProvider.addTranslations**

After the existing `add(...)` calls, add:

```java
        // Hand weapons
        add(Registration.BILLY_CLUB.get(), "Billy Club");
        add(Registration.BANDIT_KNIFE.get(), "Bandit Knife");

        // Mob entity names
        add(ModEntities.DEPUTY.get(), "Deputy");
        add(ModEntities.SHERRIF.get(), "Sheriff");
        add(ModEntities.BANDIT.get(), "Bandit");
        add(ModEntities.BANDIT_LEADER.get(), "Bandit Leader");

        // Spawn eggs
        add(Registration.DEPUTY_SPAWN_EGG.get(), "Deputy Spawn Egg");
        add(Registration.SHERRIF_SPAWN_EGG.get(), "Sheriff Spawn Egg");
        add(Registration.BANDIT_SPAWN_EGG.get(), "Bandit Spawn Egg");
        add(Registration.BANDIT_LEADER_SPAWN_EGG.get(), "Bandit Leader Spawn Egg");

        // Death messages for new damage types
        add("death.attack.wildwest.club", "%1$s was clubbed by %2$s");
        add("death.attack.wildwest.knife", "%1$s was knifed by %2$s");
```

Add the import:

```java
import com.tweeks.wildwest.ModEntities;
```

- [ ] **Step 2: Run datagen + build**

```bash
./gradlew :wildwest:runClientData :wildwest:build
```

Expected: regenerated `en_us.json` contains all the new keys.

- [ ] **Step 3: Commit**

```bash
git add wildwest/
git commit -m "feat(wildwest): localization for mobs + hand weapons + death msgs"
```

---

## Task 16: End-to-end smoke test

This task is verification only. The user runs `./gradlew :wildwest:runClient` from the worktree and walks the spec's manual test plan. No code change unless something fails.

- [ ] **Step 1: Launch dev client**

```bash
./gradlew :wildwest:runClient
```

- [ ] **Step 2: Walk the manual test plan from the spec**

Reference: design spec section "Manual smoke test" (steps 1-10). Tick each off.

- [ ] **Step 3: Final commit if any tweaks emerged**

If the smoke test produced fixes, commit them. If not, no commit needed.

- [ ] **Step 4: Mark phase 2 complete in the spec**

Open `docs/superpowers/specs/2026-04-30-wildwest-phase2-mobs-design.md`, change `**Status:** Draft` to `**Status:** Implemented in [hash]`. Commit:

```bash
git add docs/superpowers/specs/2026-04-30-wildwest-phase2-mobs-design.md
git commit -m "docs(wildwest): mark phase 2 spec as implemented"
```

---

## Self-review notes

- **Spec coverage check:**
  - Faction API → Task 1.
  - WildWestMob base + WeaponMode helper → Tasks 1, 5.
  - Four AI goals → Task 4 (joint with Task 5 commit).
  - Hand weapons + their damage types → Task 3.
  - Phase-1 add-back fireFromMob → Task 2.
  - Four mob entities → Tasks 6, 7.
  - Entity registration + spawn eggs + creative tab + attribute registration → Task 8.
  - Spawning (biome modifier + village periodic tick) → Task 9.
  - Renderers + held-item layer → Task 10.
  - Mob textures → Task 11.
  - Hand-weapon textures + item models → Task 12.
  - Loot tables → Task 13.
  - Recipes → Task 14.
  - Localization (incl. death messages) → Task 15.
  - Smoke test → Task 16.
- **Scope:** 16 tasks. Phase 1 was 13 tasks of comparable individual size. Comfortably within one PR.
- **Type consistency:** `Registration.BILLY_CLUB`, `BANDIT_KNIFE`, `DEPUTY_SPAWN_EGG` etc. and `ModEntities.DEPUTY`, `SHERRIF`, `BANDIT`, `BANDIT_LEADER` referenced consistently. `WildWestMob.usesRifle()`, `isLawman()`, `getGunStack()`, `getHandWeaponStack()` defined in Task 5 and used by Task 4 goals + Task 6/7 subclasses — all consistent.
- **No placeholders:** every code block has actual content; every command has expected output. The "STOP and report" escape hatches in API-drift sections are intentional reactive instructions, not deferred work.
- **Tests:** Tasks 1 has unit tests (TDD). Other tasks rely on the manual smoke test in Task 16 (matching phase-1 convention). No mob-AI mocking attempts — would require booting Minecraft.
