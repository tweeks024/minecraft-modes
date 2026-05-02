# Wild-West Mod — Zombie Virus — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a zombie virus to the Wild-West mod that can infect any `LivingEntity` via two `MobEffect`s (`festering_wound` 60s → `zombified` infinite). Zombified entities get a +30 % speed buff, a green render tint, ambient particles, and a hostile-melee AI overlay. Direct-hit melee from a zombified entity rolls a 60 s `festering_wound` on the target plus a 10 % `WITHER` chance. Cured by milk (player) or golden-apple right-click (mob, with a 30 s `curing_shake` for the `zombified` stage). New carrier mob (Walker) and throwable item (tainted vial) seed and weaponize the virus.

**Architecture:** State lives entirely in three vanilla `MobEffect`s (no custom NBT for infection state itself). Universal AI is achieved by adding two effect-gated goals to every `Mob` on `EntityJoinLevelEvent`. AI-conflict avoidance for ranged mobs is handled by snapshotting+clearing held items in `MobEffectEvent.Added`/restoring on `MobEffectEvent.Remove`. Projectile spread is blocked by a `source.getDirectEntity() == source.getEntity()` guard. Cure interruption is restricted to player-inflicted damage. Client tint composes via `RenderSystem.getShaderColor()` snapshot/multiply/restore (no clobbering).

**Tech Stack:** Java 25, NeoForge 26.1.2.30-beta, Minecraft 26.1.2, JUnit 5.

**Spec:** [docs/superpowers/specs/2026-05-02-wildwest-zombie-virus-design.md](../specs/2026-05-02-wildwest-zombie-virus-design.md)

**API lessons carried forward (phases 1–3):**
- `Identifier` not `ResourceLocation`.
- `LivingEntity.hurtServer((ServerLevel) level, source, dmg)` — `hurt` is deprecated.
- `MobEffects.SLOWNESS` not `MOVEMENT_SLOWDOWN`.
- `Item.inventoryTick(ItemStack, ServerLevel, Entity, EquipmentSlot)`.
- `level.isClientSide()` is a method.
- `WildWestMob` save/load uses `ValueOutput`/`ValueInput`, not `CompoundTag` directly.
- `MobSpawnSettings.SpawnerData` no longer carries weight; wrap in `Weighted<>`.
- Inside entity classes, `Registration` is name-shadowed by an inherited symbol — use FQN `com.tweeks.wildwest.Registration.X`.
- NeoForge spawn-finalize event class is `FinalizeSpawnEvent` (not `FinalizeMobSpawnEvent`); spawn-reason getter is `getSpawnType()`.
- Biome modifiers live at `src/main/resources/data/wildwest/neoforge/biome_modifier/<name>.json`.
- Spawn registrations live in `WildWestMod.java`'s `RegisterSpawnPlacementsEvent` listener.

**API risks to validate during implementation:**
- `MobEffectEvent.Added` / `MobEffectEvent.Remove` / `MobEffectEvent.Expired` exact class names in NeoForge 26.x — verify before Task 5.
- `RenderLivingEvent.Pre/Post` generic signature in 26.x — verify before Task 13.
- `Player.isCreative()` and `Player.isSpectator()` availability — both exist in 26.x but confirm.

---

## File Structure

```
wildwest/src/main/java/com/tweeks/wildwest/
  effect/
    ModEffects.java                       NEW   DeferredRegister<MobEffect>; FESTERING_WOUND, ZOMBIFIED, CURING_SHAKE
    FesteringWoundEffect.java             NEW   60s; on expire applies ZOMBIFIED
    ZombifiedEffect.java                  NEW   infinite; speed attribute modifier; ambient particle tick
    CuringShakeEffect.java                NEW   30s; on expire removes ZOMBIFIED
  entity/
    WalkerEntity.java                     NEW   Monster subclass; permanent ZOMBIFIED carrier
    ai/zombified/
      InfectionImmunity.java              NEW   pure predicate: can a target be infected
      ZombifiedHostileTargetGoal.java     NEW   target nearest non-zombified LivingEntity
      ZombifiedMeleeAttackGoal.java       NEW   melee approach + attack
    projectile/
      TaintedVialEntity.java              NEW   ThrowableProjectile; impact applies festering in 3-blk radius
  item/
    TaintedVialItem.java                  NEW   useDuration=0; releaseUsing throws TaintedVialEntity
  client/
    ZombifiedRenderHandler.java           NEW   RenderLivingEvent.Pre/Post green tint w/ save+restore
    WalkerRenderer.java                   NEW   uses WalkerModel (flat client/, mirrors DeputyRenderer)
    model/WalkerModel.java                NEW   humanoid layer
    ClientSetup.java                      MOD   add Walker renderer + layer entries
  ZombieVirusHandler.java                 NEW   server event listeners (LivingDamageEvent, MobEffectEvent.Added/Remove,
                                                EntityJoinLevelEvent, PlayerInteractEvent.EntityInteract)
  Registration.java                       MOD   register tainted_vial, walker_spawn_egg; creative-tab entries
  ModEntities.java                        MOD   register WALKER, TAINTED_VIAL_PROJECTILE
  WildWestMod.java                        MOD   register ModEffects; entity attributes for Walker; spawn placement;
                                                client renderer registration; ZombieVirusHandler bus subscription

wildwest/src/main/resources/
  assets/wildwest/
    lang/en_us.json                       MOD   add new strings
    items/tainted_vial.json               NEW
    items/walker_spawn_egg.json           NEW
    models/item/tainted_vial.json         NEW
    models/item/walker_spawn_egg.json     NEW
    textures/item/tainted_vial.png        NEW   16x16
    textures/item/walker_spawn_egg.png    NEW   16x16 flat icon
    textures/entity/walker.png            NEW   64x64 player-skin layout
    textures/mob_effect/festering_wound.png  NEW  18x18
    textures/mob_effect/zombified.png        NEW  18x18
    textures/mob_effect/curing_shake.png     NEW  18x18
  data/wildwest/
    recipe/tainted_vial.json              NEW
    loot_table/entities/walker.json       NEW
    neoforge/biome_modifier/add_walkers.json  NEW

wildwest/src/test/java/com/tweeks/wildwest/
  entity/ai/zombified/
    InfectionImmunityTest.java            NEW   JUnit 5

wildwest/tools/
  gen_zombie_virus_textures.py            NEW   16x16 vial + 18x18 effect icons + 64x64 walker placeholder
```

---

## Task 1: Pre-flight — register `MobEffect` skeleton + smoke build

**Why first:** the NeoForge 26.x `MobEffect` API is the single biggest unknown. We register a no-op effect first to validate the API surface before building real effect classes. This is a 5-minute task that de-risks everything that follows.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/effect/ModEffects.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`

- [ ] **Step 1: Create `ModEffects.java` with a single placeholder effect**

```java
package com.tweeks.wildwest.effect;

import com.tweeks.wildwest.WildWestMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    private ModEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, WildWestMod.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> FESTERING_WOUND =
        EFFECTS.register("festering_wound",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x6B8E23) {});

    public static final DeferredHolder<MobEffect, MobEffect> ZOMBIFIED =
        EFFECTS.register("zombified",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x4A7C2E) {});

    public static final DeferredHolder<MobEffect, MobEffect> CURING_SHAKE =
        EFFECTS.register("curing_shake",
            () -> new MobEffect(MobEffectCategory.NEUTRAL, 0xFFD700) {});

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}
```

- [ ] **Step 2: Wire ModEffects.register in WildWestMod.java**

In `WildWestMod` constructor after `Registration.register(modEventBus);`:

```java
ModEffects.register(modEventBus);
```

(Add the import for `com.tweeks.wildwest.effect.ModEffects`.)

- [ ] **Step 3: Build and verify**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

If the constructor signature `MobEffect(MobEffectCategory, int)` doesn't compile, check NeoForge 26.x docs for the current ctor signature and adjust. (As of MC 26.1.2 it should match this.)

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/effect/ModEffects.java \
        wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java
git commit -m "feat(wildwest): register zombie-virus mob effects (skeleton)"
```

---

## Task 2: `InfectionImmunity` pure predicate + JUnit tests

**Why a pure helper:** the immune-set logic (creative players, undead-tag, bosses, Walker, non-Mob/non-Player) is complex enough to warrant a unit test, and the predicate has zero Minecraft state — just type checks and tag membership. Extracting it lets us run the whole test suite without booting a server.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/zombified/InfectionImmunity.java`
- Create: `wildwest/src/test/java/com/tweeks/wildwest/entity/ai/zombified/InfectionImmunityTest.java`

- [ ] **Step 1: Write failing tests**

`wildwest/src/test/java/com/tweeks/wildwest/entity/ai/zombified/InfectionImmunityTest.java`:

```java
package com.tweeks.wildwest.entity.ai.zombified;

import com.tweeks.wildwest.entity.ai.zombified.InfectionImmunity.Subject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfectionImmunityTest {

    static class TestSubject implements Subject {
        boolean undead, boss, walker, creative, spectator, mobOrPlayer = true;
        @Override public boolean isUndead() { return undead; }
        @Override public boolean isBoss() { return boss; }
        @Override public boolean isWalker() { return walker; }
        @Override public boolean isCreativeOrSpectatorPlayer() { return creative || spectator; }
        @Override public boolean isMobOrPlayer() { return mobOrPlayer; }
    }

    @Test
    void plainCow_isNotImmune() {
        assertFalse(InfectionImmunity.isImmune(new TestSubject()));
    }

    @Test
    void undeadEntity_isImmune() {
        TestSubject s = new TestSubject(); s.undead = true;
        assertTrue(InfectionImmunity.isImmune(s));
    }

    @Test
    void boss_isImmune() {
        TestSubject s = new TestSubject(); s.boss = true;
        assertTrue(InfectionImmunity.isImmune(s));
    }

    @Test
    void walker_isImmune() {
        TestSubject s = new TestSubject(); s.walker = true;
        assertTrue(InfectionImmunity.isImmune(s));
    }

    @Test
    void creativePlayer_isImmune() {
        TestSubject s = new TestSubject(); s.creative = true;
        assertTrue(InfectionImmunity.isImmune(s));
    }

    @Test
    void spectatorPlayer_isImmune() {
        TestSubject s = new TestSubject(); s.spectator = true;
        assertTrue(InfectionImmunity.isImmune(s));
    }

    @Test
    void armorStandLikeNonLiving_isImmune() {
        TestSubject s = new TestSubject(); s.mobOrPlayer = false;
        assertTrue(InfectionImmunity.isImmune(s));
    }
}
```

- [ ] **Step 2: Verify tests fail**

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.entity.ai.zombified.InfectionImmunityTest"`
Expected: FAIL — `InfectionImmunity` class doesn't exist.

- [ ] **Step 3: Implement `InfectionImmunity`**

```java
package com.tweeks.wildwest.entity.ai.zombified;

import com.tweeks.wildwest.effect.ModEffects;
import com.tweeks.wildwest.entity.WalkerEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;

public final class InfectionImmunity {
    private InfectionImmunity() {}

    /** Test seam: a `Subject` is the bits-of-state we care about, no Minecraft mocks needed. */
    public interface Subject {
        boolean isUndead();
        boolean isBoss();
        boolean isWalker();
        boolean isCreativeOrSpectatorPlayer();
        boolean isMobOrPlayer();
    }

    public static boolean isImmune(Subject s) {
        if (!s.isMobOrPlayer()) return true;
        if (s.isUndead()) return true;
        if (s.isBoss()) return true;
        if (s.isWalker()) return true;
        if (s.isCreativeOrSpectatorPlayer()) return true;
        return false;
    }

    /** Adapter for live LivingEntity → Subject. */
    public static boolean isImmune(LivingEntity e) {
        return isImmune(new Subject() {
            @Override public boolean isUndead() {
                return e.getType().is(EntityTypeTags.UNDEAD);
            }
            @Override public boolean isBoss() {
                return e instanceof EnderDragon || e instanceof WitherBoss;
            }
            @Override public boolean isWalker() {
                return e instanceof WalkerEntity;
            }
            @Override public boolean isCreativeOrSpectatorPlayer() {
                return e instanceof Player p && (p.isCreative() || p.isSpectator());
            }
            @Override public boolean isMobOrPlayer() {
                return e instanceof Mob || e instanceof Player;
            }
        });
    }
}
```

Note: `WalkerEntity` does not yet exist (Task 9 creates it). To allow this file to compile now, the `WalkerEntity` import will fail — fix by deferring the `e instanceof WalkerEntity` check via a forward-reference helper. Replace the adapter's `isWalker()` body with a class-name string check:

```java
@Override public boolean isWalker() {
    // String compare to avoid forward dependency on WalkerEntity (created in Task 9).
    return e.getClass().getSimpleName().equals("WalkerEntity");
}
```

(Remove the `import com.tweeks.wildwest.entity.WalkerEntity;` line since it's no longer used.)

- [ ] **Step 4: Verify tests pass**

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.entity.ai.zombified.InfectionImmunityTest"`
Expected: 7 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ai/zombified/InfectionImmunity.java \
        wildwest/src/test/java/com/tweeks/wildwest/entity/ai/zombified/InfectionImmunityTest.java
git commit -m "feat(wildwest): InfectionImmunity pure predicate + tests"
```

---

## Task 3: Implement the three `MobEffect` subclasses

Replace the placeholder anonymous-class effects from Task 1 with real effect classes that handle the state-machine transitions: `FesteringWound` → `Zombified` on expiry, `CuringShake` removes `Zombified` on expiry.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/effect/FesteringWoundEffect.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/effect/ZombifiedEffect.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/effect/CuringShakeEffect.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/effect/ModEffects.java`

- [ ] **Step 1: Implement `FesteringWoundEffect`**

```java
package com.tweeks.wildwest.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class FesteringWoundEffect extends MobEffect {
    public FesteringWoundEffect() {
        super(MobEffectCategory.HARMFUL, 0x6B8E23);
    }

    /** Called every tick the effect is active. Returning false drops the effect. */
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Cosmetic only — actual state transition happens on expiry, in onEffectTick or via
        // a tracked tick counter. We rely on Minecraft removing the effect when its duration
        // hits zero, then a parallel server tick listener will detect the removal and apply
        // ZOMBIFIED. The clean way: hook MobEffectEvent.Expired in ZombieVirusHandler (Task 5).
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;  // tick every tick (light cost)
    }
}
```

- [ ] **Step 2: Implement `ZombifiedEffect`**

```java
package com.tweeks.wildwest.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class ZombifiedEffect extends MobEffect {
    public static final Identifier SPEED_MOD_ID =
        Identifier.fromNamespaceAndPath("wildwest", "zombified_speed");

    public ZombifiedEffect() {
        super(MobEffectCategory.HARMFUL, 0x4A7C2E);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MOD_ID, 0.30,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel sl && entity.tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
                2, 0.3, 0.3, 0.3, 0.0);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
```

`addAttributeModifier` registered via the `MobEffect` constructor lets vanilla apply/remove the attribute modifier automatically when the effect is added/removed. No manual on-add/on-remove required for the speed buff.

- [ ] **Step 3: Implement `CuringShakeEffect`**

```java
package com.tweeks.wildwest.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class CuringShakeEffect extends MobEffect {
    public CuringShakeEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFD700);
    }
    // Expiry triggers ZOMBIFIED removal — handled in ZombieVirusHandler (Task 5).
    // No tick logic needed.
}
```

- [ ] **Step 4: Update `ModEffects.java` to use the real classes**

Replace the three lambda registrations:

```java
public static final DeferredHolder<MobEffect, MobEffect> FESTERING_WOUND =
    EFFECTS.register("festering_wound", FesteringWoundEffect::new);

public static final DeferredHolder<MobEffect, MobEffect> ZOMBIFIED =
    EFFECTS.register("zombified", ZombifiedEffect::new);

public static final DeferredHolder<MobEffect, MobEffect> CURING_SHAKE =
    EFFECTS.register("curing_shake", CuringShakeEffect::new);
```

- [ ] **Step 5: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

If `addAttributeModifier(Attributes.MOVEMENT_SPEED, Identifier, double, AttributeModifier.Operation)` doesn't compile, the API may take an older `UUID` form or a `Holder<Attribute>`. Adjust based on the compiler error.

- [ ] **Step 6: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/effect/
git commit -m "feat(wildwest): festering/zombified/curing-shake effect classes"
```

---

## Task 4: `HandSnapshot` helper for disarm/restore

Snapshots and restores `MAINHAND` + `OFFHAND` `ItemStack`s on a `Mob` via persistent NBT keys. Used by Task 5 to neutralize ranged-AI conflicts during zombification.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/effect/HandSnapshot.java`

- [ ] **Step 1: Implement `HandSnapshot`**

```java
package com.tweeks.wildwest.effect;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * Saves the held items of a Mob into its persistent data, then clears them.
 * Restore reads the saved snapshot and writes it back to the equipment slots.
 *
 * <p>Used to disable native ranged-combat goals (which gate on `mob.isHolding(BOW)` etc.)
 * for the duration of the zombified effect, without surgically removing the goals.
 */
public final class HandSnapshot {
    private HandSnapshot() {}

    private static final String KEY_MAIN = "wildwest:pre_zombified_mainhand";
    private static final String KEY_OFF  = "wildwest:pre_zombified_offhand";

    /** Snapshot held items into persistent data and clear the slots. No-op if already snapshotted. */
    public static void snapshotAndClear(Mob mob) {
        var pd = mob.getPersistentData();
        if (pd.contains(KEY_MAIN) || pd.contains(KEY_OFF)) return;

        ItemStack main = mob.getItemBySlot(EquipmentSlot.MAINHAND);
        ItemStack off  = mob.getItemBySlot(EquipmentSlot.OFFHAND);

        if (!main.isEmpty()) pd.put(KEY_MAIN, main.save(mob.registryAccess()));
        if (!off.isEmpty())  pd.put(KEY_OFF,  off.save(mob.registryAccess()));

        mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        mob.setItemSlot(EquipmentSlot.OFFHAND,  ItemStack.EMPTY);
    }

    /** Restore previously snapshotted held items. Clears the snapshot keys after restoring. */
    public static void restore(Mob mob) {
        var pd = mob.getPersistentData();

        if (pd.contains(KEY_MAIN)) {
            var stack = ItemStack.parse(mob.registryAccess(), pd.get(KEY_MAIN));
            stack.ifPresent(s -> mob.setItemSlot(EquipmentSlot.MAINHAND, s));
            pd.remove(KEY_MAIN);
        }
        if (pd.contains(KEY_OFF)) {
            var stack = ItemStack.parse(mob.registryAccess(), pd.get(KEY_OFF));
            stack.ifPresent(s -> mob.setItemSlot(EquipmentSlot.OFFHAND, s));
            pd.remove(KEY_OFF);
        }
    }
}
```

API note: `Entity.getPersistentData()` returns a `CompoundTag` that vanilla saves/loads automatically. `ItemStack.save(RegistryAccess)` / `ItemStack.parse(RegistryAccess, Tag)` is the 26.x serialization shape; if the compiler reports a different signature (e.g., requires `HolderLookup.Provider`), substitute accordingly.

- [ ] **Step 2: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/effect/HandSnapshot.java
git commit -m "feat(wildwest): HandSnapshot helper for zombified disarm/restore"
```

---

## Task 5: `ZombieVirusHandler` — bite spread + effect transitions + cure interaction

Server-side event handler that wires every runtime behavior together. Most logic lives here so that other classes stay focused.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/ZombieVirusHandler.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`

- [ ] **Step 1: Create the handler skeleton**

```java
package com.tweeks.wildwest;

import com.tweeks.wildwest.effect.HandSnapshot;
import com.tweeks.wildwest.effect.ModEffects;
import com.tweeks.wildwest.entity.ai.zombified.InfectionImmunity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = WildWestMod.MOD_ID)
public final class ZombieVirusHandler {
    private ZombieVirusHandler() {}

    private static final int FESTERING_DURATION_TICKS = 60 * 20;
    private static final int WITHER_DURATION_TICKS = 5 * 20;
    private static final int CURING_SHAKE_DURATION_TICKS = 30 * 20;
    private static final double WITHER_CHANCE = 0.10;

    // Bite spread: see Step 2.
    // MobEffectEvent.Added/Remove: see Step 3.
    // EntityJoinLevelEvent: see Task 7 (AI goal injection).
    // PlayerInteractEvent.EntityInteract: see Step 4.
}
```

- [ ] **Step 2: Add the bite-spread handler**

Inside `ZombieVirusHandler`:

```java
@SubscribeEvent
public static void onLivingDamage(LivingDamageEvent.Pre event) {
    LivingEntity target = event.getEntity();
    var source = event.getSource();
    if (!(source.getEntity() instanceof LivingEntity attacker)) return;

    // Direct-hit guard: melee only, not arrows/llama-spit/etc.
    if (source.getDirectEntity() != source.getEntity()) return;

    if (!attacker.hasEffect(ModEffects.ZOMBIFIED)) return;
    if (target.level().isClientSide()) return;
    if (InfectionImmunity.isImmune(target)) return;

    // Festering apply / refresh
    if (target.hasEffect(ModEffects.ZOMBIFIED)) {
        // already turned — skip festering
    } else {
        target.addEffect(new MobEffectInstance(ModEffects.FESTERING_WOUND,
            FESTERING_DURATION_TICKS, 0, false, true));
    }

    // 10 % wither roll, independent
    if (target.getRandom().nextDouble() < WITHER_CHANCE) {
        target.addEffect(new MobEffectInstance(MobEffects.WITHER,
            WITHER_DURATION_TICKS, 0, false, true));
    }
}
```

Class signature note: `LivingDamageEvent` in NeoForge 26.x is split into `LivingDamageEvent.Pre` and `LivingDamageEvent.Post`. We use `Pre` because we want the bite logic regardless of whether damage is canceled by armor/effects. If the import doesn't resolve, try `LivingDamageEvent` directly (single class) and remove `.Pre`.

- [ ] **Step 3: Add the effect-add/remove handlers (disarm + curing-shake clear)**

Inside `ZombieVirusHandler`:

```java
@SubscribeEvent
public static void onEffectAdded(MobEffectEvent.Added event) {
    if (!event.getEffectInstance().is(ModEffects.ZOMBIFIED)) return;
    LivingEntity entity = event.getEntity();
    if (entity instanceof Mob mob && !(entity instanceof Player)) {
        HandSnapshot.snapshotAndClear(mob);
    }
}

@SubscribeEvent
public static void onEffectRemoved(MobEffectEvent.Remove event) {
    if (!event.getEffect().is(ModEffects.ZOMBIFIED)) return;
    LivingEntity entity = event.getEntity();
    if (entity instanceof Mob mob && !(entity instanceof Player)) {
        HandSnapshot.restore(mob);
    }
}

@SubscribeEvent
public static void onEffectExpired(MobEffectEvent.Expired event) {
    LivingEntity entity = event.getEntity();
    var inst = event.getEffectInstance();
    if (inst == null) return;

    if (inst.is(ModEffects.FESTERING_WOUND)) {
        // Festering naturally expired — convert to zombified.
        entity.addEffect(new MobEffectInstance(ModEffects.ZOMBIFIED,
            -1 /* infinite */, 0, false, true));
    } else if (inst.is(ModEffects.CURING_SHAKE)) {
        // Curing shake completed without interruption — remove zombified.
        entity.removeEffect(ModEffects.ZOMBIFIED);
    } else if (inst.is(ModEffects.ZOMBIFIED)) {
        // Zombified being removed (via cure or otherwise): handled by onEffectRemoved.
    }
}

/** If a curing entity is hit by a player, cancel the cure attempt. */
@SubscribeEvent
public static void onCureInterrupt(LivingDamageEvent.Pre event) {
    LivingEntity target = event.getEntity();
    if (!target.hasEffect(ModEffects.CURING_SHAKE)) return;
    if (event.getSource().getEntity() instanceof Player) {
        target.removeEffect(ModEffects.CURING_SHAKE);
    }
}
```

API note: in NeoForge 26.x, `MobEffectEvent.Added`, `MobEffectEvent.Remove`, `MobEffectEvent.Expired` are nested events. If absent, fall back to: `MobEffectEvent$Added`, `MobEffectEvent$Removed`, `MobEffectEvent$Expired`. The class compile-error message will tell you the correct names.

The `LivingDamageEvent.Pre` listener for cure-interrupt is a separate `@SubscribeEvent` from the bite handler — the event bus calls both.

- [ ] **Step 4: Add the golden-apple cure handler**

```java
@SubscribeEvent
public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
    if (event.getLevel().isClientSide()) return;
    if (event.getItemStack().getItem() != Items.GOLDEN_APPLE) return;
    if (!(event.getTarget() instanceof LivingEntity target)) return;

    if (target.hasEffect(ModEffects.FESTERING_WOUND)) {
        target.removeEffect(ModEffects.FESTERING_WOUND);
        if (!event.getEntity().getAbilities().instabuild) event.getItemStack().shrink(1);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    } else if (target.hasEffect(ModEffects.ZOMBIFIED)
            && !target.hasEffect(ModEffects.CURING_SHAKE)) {
        target.addEffect(new MobEffectInstance(ModEffects.CURING_SHAKE,
            CURING_SHAKE_DURATION_TICKS, 0, false, false));
        if (!event.getEntity().getAbilities().instabuild) event.getItemStack().shrink(1);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
```

- [ ] **Step 5: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

If `LivingDamageEvent.Pre` / `MobEffectEvent.Added` resolve to wrong names, look at the existing `BanditLeaderPackSpawner` for an event-class lookup pattern, then check the imported NeoForge classes.

- [ ] **Step 6: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/ZombieVirusHandler.java
git commit -m "feat(wildwest): bite spread, effect transitions, golden-apple cure"
```

---

## Task 6: Zombified AI goals

Two `Goal`s gated on `hasEffect(ZOMBIFIED)` that any `Mob` gets injected with on entity-join.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/zombified/ZombifiedHostileTargetGoal.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/zombified/ZombifiedMeleeAttackGoal.java`

- [ ] **Step 1: Implement `ZombifiedHostileTargetGoal`**

```java
package com.tweeks.wildwest.entity.ai.zombified;

import com.tweeks.wildwest.effect.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.EnumSet;

public class ZombifiedHostileTargetGoal extends Goal {
    private static final double SCAN_RANGE = 16.0;

    private final Mob self;
    private LivingEntity chosen;

    public ZombifiedHostileTargetGoal(Mob self) {
        this.self = self;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!self.hasEffect(ModEffects.ZOMBIFIED)) return false;
        if (self.hasEffect(ModEffects.CURING_SHAKE)) return false;  // suppress while curing

        AABB area = self.getBoundingBox().inflate(SCAN_RANGE);
        chosen = self.level().getEntitiesOfClass(LivingEntity.class, area, e ->
                e != self
                && e.isAlive()
                && !e.hasEffect(ModEffects.ZOMBIFIED)
                && !InfectionImmunity.isImmune(e)
                && self.hasLineOfSight(e))
            .stream()
            .min(Comparator.comparingDouble(self::distanceToSqr))
            .orElse(null);
        return chosen != null;
    }

    @Override
    public void start() {
        self.setTarget(chosen);
    }

    @Override
    public boolean canContinueToUse() {
        return self.hasEffect(ModEffects.ZOMBIFIED)
            && !self.hasEffect(ModEffects.CURING_SHAKE)
            && chosen != null
            && chosen.isAlive()
            && self.distanceToSqr(chosen) <= (SCAN_RANGE * 1.5) * (SCAN_RANGE * 1.5);
    }

    @Override
    public void stop() {
        self.setTarget(null);
        chosen = null;
    }
}
```

- [ ] **Step 2: Implement `ZombifiedMeleeAttackGoal`**

We extend vanilla's `MeleeAttackGoal` and add an effect gate to `canUse`/`canContinueToUse`. This piggybacks on its tested approach/swing/cooldown logic.

```java
package com.tweeks.wildwest.entity.ai.zombified;

import com.tweeks.wildwest.effect.ModEffects;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class ZombifiedMeleeAttackGoal extends MeleeAttackGoal {
    private final PathfinderMob self;

    public ZombifiedMeleeAttackGoal(PathfinderMob self) {
        super(self, 1.0, /* mustReachTarget */ true);
        this.self = self;
    }

    @Override
    public boolean canUse() {
        if (!self.hasEffect(ModEffects.ZOMBIFIED)) return false;
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (!self.hasEffect(ModEffects.ZOMBIFIED)) return false;
        return super.canContinueToUse();
    }
}
```

`MeleeAttackGoal` requires `PathfinderMob` (not just `Mob`). Most living mobs are pathfinders. For non-pathfinder mobs (e.g., `FlyingMob`), we'll skip the melee-attack goal injection in Task 7.

- [ ] **Step 3: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ai/zombified/
git commit -m "feat(wildwest): zombified hostile-target + melee-attack goals"
```

---

## Task 7: Inject zombified AI goals on `EntityJoinLevelEvent`

Wire the goals into every `Mob` joining the level.

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/ZombieVirusHandler.java`

- [ ] **Step 1: Add the join handler**

Append to `ZombieVirusHandler`:

```java
@SubscribeEvent
public static void onEntityJoin(EntityJoinLevelEvent event) {
    if (event.getLevel().isClientSide()) return;
    if (!(event.getEntity() instanceof Mob mob)) return;
    if (mob instanceof Player) return;  // defensive (Player is not a Mob anyway)

    // Always-on target goal at high priority — gates on ZOMBIFIED at runtime.
    mob.targetSelector.addGoal(0, new ZombifiedHostileTargetGoal(mob));

    // Melee attack goal — only inject for PathfinderMob (which is most things).
    if (mob instanceof net.minecraft.world.entity.PathfinderMob pm) {
        mob.goalSelector.addGoal(1, new ZombifiedMeleeAttackGoal(pm));
    }
}
```

Add imports:
```java
import com.tweeks.wildwest.entity.ai.zombified.ZombifiedHostileTargetGoal;
import com.tweeks.wildwest.entity.ai.zombified.ZombifiedMeleeAttackGoal;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
```

- [ ] **Step 2: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/ZombieVirusHandler.java
git commit -m "feat(wildwest): inject zombified goals on entity join"
```

---

## Task 8: `TaintedVialEntity` projectile

Throwable that applies festering to all `LivingEntity` in a 3-block sphere on impact.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/projectile/TaintedVialEntity.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java`

- [ ] **Step 1: Implement `TaintedVialEntity`**

```java
package com.tweeks.wildwest.entity.projectile;

import com.tweeks.wildwest.effect.ModEffects;
import com.tweeks.wildwest.entity.ai.zombified.InfectionImmunity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class TaintedVialEntity extends ThrowableItemProjectile {
    private static final double SPLASH_RADIUS = 3.0;
    private static final int FESTERING_DURATION_TICKS = 60 * 20;

    public TaintedVialEntity(EntityType<? extends TaintedVialEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDefaultItem() {
        return com.tweeks.wildwest.Registration.TAINTED_VIAL.get();
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(this.level() instanceof ServerLevel sl)) return;

        Vec3 center = hit.getLocation();

        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
            center.x, center.y, center.z, 30, SPLASH_RADIUS, 0.5, SPLASH_RADIUS, 0.0);
        sl.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK,
            SoundSource.NEUTRAL, 1.0F, 1.0F);

        AABB box = AABB.ofSize(center, SPLASH_RADIUS * 2, SPLASH_RADIUS * 2, SPLASH_RADIUS * 2);
        sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.position().distanceTo(center) <= SPLASH_RADIUS)
            .forEach(e -> {
                if (InfectionImmunity.isImmune(e)) return;
                if (e.hasEffect(ModEffects.ZOMBIFIED)) return;
                e.addEffect(new MobEffectInstance(ModEffects.FESTERING_WOUND,
                    FESTERING_DURATION_TICKS, 0, false, true));
            });

        this.discard();
    }
}
```

- [ ] **Step 2: Register the entity type in `ModEntities.java`**

Add after the `BANDIT_LEADER` registration:

```java
public static final DeferredHolder<EntityType<?>, EntityType<TaintedVialEntity>> TAINTED_VIAL_PROJECTILE =
    ENTITY_TYPES.register("tainted_vial_projectile", () -> EntityType.Builder.<TaintedVialEntity>of(
            TaintedVialEntity::new, MobCategory.MISC)
        .sized(0.25f, 0.25f)
        .clientTrackingRange(4)
        .updateInterval(10)
        .build(ResourceKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "tainted_vial_projectile"))));
```

Add the import: `import com.tweeks.wildwest.entity.projectile.TaintedVialEntity;`.

- [ ] **Step 3: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD FAILS — `Registration.TAINTED_VIAL` doesn't exist yet (Task 9 creates it).

- [ ] **Step 4: Continue to Task 9 — commit deferred until vial item exists**

Don't commit yet; the next task closes the loop.

---

## Task 9: `TaintedVialItem` + recipe

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/item/TaintedVialItem.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`
- Create: `wildwest/src/main/resources/data/wildwest/recipe/tainted_vial.json`

- [ ] **Step 1: Implement `TaintedVialItem`**

```java
package com.tweeks.wildwest.item;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.entity.projectile.TaintedVialEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TaintedVialItem extends Item {
    public TaintedVialItem(Properties props) {
        super(props.stacksTo(16));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.4F);

        if (level instanceof ServerLevel sl) {
            TaintedVialEntity proj = new TaintedVialEntity(
                ModEntities.TAINTED_VIAL_PROJECTILE.get(), level);
            proj.setOwner(player);
            proj.setItem(stack.copyWithCount(1));
            proj.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            proj.shootFromRotation(player, player.getXRot(), player.getYRot(),
                -20.0F, 0.5F, 1.0F);
            sl.addFreshEntity(proj);
        }

        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }
}
```

- [ ] **Step 2: Register the item in `Registration.java`**

Add after `BANDIT_LEADER_SPAWN_EGG`:

```java
public static final DeferredItem<TaintedVialItem> TAINTED_VIAL = ITEMS.registerItem(
    "tainted_vial", TaintedVialItem::new, p -> p);
```

Add the import: `import com.tweeks.wildwest.item.TaintedVialItem;`.

In the creative-tab `displayItems` lambda, add:

```java
output.accept(TAINTED_VIAL.get());
```

- [ ] **Step 3: Create the recipe**

`wildwest/src/main/resources/data/wildwest/recipe/tainted_vial.json`:

```json
{
  "type": "minecraft:crafting_shapeless",
  "category": "misc",
  "ingredients": [
    { "item": "minecraft:rotten_flesh" },
    { "item": "minecraft:glass_bottle" },
    { "item": "minecraft:gunpowder" }
  ],
  "result": {
    "id": "wildwest:tainted_vial",
    "count": 1
  }
}
```

- [ ] **Step 4: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL (Task 8's compile error now resolves).

- [ ] **Step 5: Commit Tasks 8 + 9 together**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/projectile/ \
        wildwest/src/main/java/com/tweeks/wildwest/item/TaintedVialItem.java \
        wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java \
        wildwest/src/main/java/com/tweeks/wildwest/Registration.java \
        wildwest/src/main/resources/data/wildwest/recipe/tainted_vial.json
git commit -m "feat(wildwest): tainted vial item + projectile + recipe"
```

---

## Task 10: `WalkerEntity` carrier mob + entity registration

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/WalkerEntity.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`

- [ ] **Step 1: Implement `WalkerEntity`**

```java
package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.effect.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AttributeSupplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class WalkerEntity extends Monster {

    public WalkerEntity(EntityType<? extends WalkerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.23)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Zombified target + melee goals are added universally by ZombieVirusHandler.onEntityJoin.
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && !this.hasEffect(ModEffects.ZOMBIFIED)) {
            this.addEffect(new MobEffectInstance(ModEffects.ZOMBIFIED,
                -1 /* infinite */, 0, /* ambient */ true, /* visible */ false));
        }
    }
}
```

The Walker is a permanent carrier — re-applying `ZOMBIFIED` on tick handles save/load reflows where the effect might briefly miss.

- [ ] **Step 2: Register the entity type in `ModEntities.java`**

After `BANDIT_LEADER`:

```java
public static final DeferredHolder<EntityType<?>, EntityType<WalkerEntity>> WALKER =
    ENTITY_TYPES.register("walker", () -> EntityType.Builder.<WalkerEntity>of(
            WalkerEntity::new, MobCategory.MONSTER)
        .sized(0.6f, 1.95f)
        .clientTrackingRange(8)
        .build(ResourceKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "walker"))));
```

Add the import: `import com.tweeks.wildwest.entity.WalkerEntity;`.

- [ ] **Step 3: Register Walker attributes + spawn placement in `WildWestMod.java`**

In `registerEntityAttributes`:

```java
event.put(ModEntities.WALKER.get(), WalkerEntity.createAttributes().build());
```

In the `RegisterSpawnPlacementsEvent` listener (after the existing bandit registrations):

```java
event.register(ModEntities.WALKER.get(),
    net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
    net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
    net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
```

Add the import: `import com.tweeks.wildwest.entity.WalkerEntity;`.

- [ ] **Step 4: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/WalkerEntity.java \
        wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java \
        wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java
git commit -m "feat(wildwest): WalkerEntity carrier mob + registration"
```

---

## Task 11: Walker biome modifier + spawn egg

**Files:**
- Create: `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/add_walkers.json`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`

- [ ] **Step 1: Create the biome modifier**

`wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/add_walkers.json`:

```json
{
  "type": "neoforge:add_spawns",
  "biomes": [
    "minecraft:plains",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:desert"
  ],
  "spawners": {
    "type": "wildwest:walker",
    "maxCount": 2,
    "minCount": 1,
    "weight": 1
  }
}
```

- [ ] **Step 2: Register the spawn-egg item**

In `Registration.java`, after `BANDIT_LEADER_SPAWN_EGG`:

```java
public static final DeferredItem<SpawnEggItem> WALKER_SPAWN_EGG = ITEMS.registerItem(
    "walker_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.WALKER.get()));
```

In the creative-tab lambda add:
```java
output.accept(WALKER_SPAWN_EGG.get());
```

- [ ] **Step 3: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/Registration.java \
        wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/add_walkers.json
git commit -m "feat(wildwest): walker spawn egg + biome modifier"
```

---

## Task 12: Walker model + renderer (client)

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/model/WalkerModel.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/WalkerRenderer.java` (flat `client/`, mirroring `DeputyRenderer.java`)
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java` (existing — adds Walker entries to both methods)

- [ ] **Step 1: Add Walker entries to existing `ClientSetup.java`**

The existing file has methods `registerRenderers(EntityRenderersEvent.RegisterRenderers)` and `registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions)`. Add a line in each:

In `registerRenderers`, after the `BANDIT_LEADER` line:
```java
event.registerEntityRenderer(ModEntities.WALKER.get(), WalkerRenderer::new);
```

In `registerLayerDefinitions`, after the `BanditLeaderModel` line:
```java
event.registerLayerDefinition(WalkerModel.LAYER_LOCATION, WalkerModel::createBodyLayer);
```

Add the imports:
```java
import com.tweeks.wildwest.client.model.WalkerModel;
```
(`WalkerRenderer` is in the same package, no import needed.)

- [ ] **Step 2: Implement `WalkerModel`** — plain humanoid (no hat / no decorations)

```java
package com.tweeks.wildwest.client.model;

import com.tweeks.wildwest.WildWestMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Walker: vanilla humanoid mesh, no hat overlay. Tint comes from ZombifiedRenderHandler. */
public class WalkerModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "walker"),
        "main");

    public WalkerModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
```

(Layer registration was already added to `ClientSetup.registerLayerDefinitions` in Step 1.)

- [ ] **Step 3: Implement `WalkerRenderer`** — mirrors `DeputyRenderer.java` exactly

```java
package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.model.WalkerModel;
import com.tweeks.wildwest.entity.WalkerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class WalkerRenderer
        extends HumanoidMobRenderer<WalkerEntity, HumanoidRenderState, WalkerModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/walker.png");

    public WalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new WalkerModel(context.bakeLayer(WalkerModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
```

This mirrors [DeputyRenderer.java](wildwest/src/main/java/com/tweeks/wildwest/client/DeputyRenderer.java) exactly — same generic-param ordering and method overrides.

- [ ] **Step 4: Build (client)**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/client/
git commit -m "feat(wildwest): walker model + renderer"
```

---

## Task 13: Client green-tint render handler

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/ZombifiedRenderHandler.java`

- [ ] **Step 1: Implement the handler**

```java
package com.tweeks.wildwest.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.effect.ModEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

@EventBusSubscriber(modid = WildWestMod.MOD_ID, value = Dist.CLIENT)
public final class ZombifiedRenderHandler {
    private ZombifiedRenderHandler() {}

    private static final ThreadLocal<float[]> SAVED = new ThreadLocal<>();

    @SubscribeEvent
    public static void pre(RenderLivingEvent.Pre<?, ?, ?> event) {
        if (!event.getEntity().hasEffect(ModEffects.ZOMBIFIED)) return;
        float[] cur = RenderSystem.getShaderColor();
        // Clone — RenderSystem.getShaderColor() may return a shared reference.
        SAVED.set(new float[] { cur[0], cur[1], cur[2], cur[3] });
        RenderSystem.setShaderColor(cur[0] * 0.4f, cur[1] * 1.0f, cur[2] * 0.4f, cur[3]);
    }

    @SubscribeEvent
    public static void post(RenderLivingEvent.Post<?, ?, ?> event) {
        float[] s = SAVED.get();
        if (s == null) return;
        RenderSystem.setShaderColor(s[0], s[1], s[2], s[3]);
        SAVED.remove();
    }
}
```

The `RenderLivingEvent.Pre/Post` generic parameter count varies by NeoForge version — adjust wildcards (`<?>` vs `<?, ?, ?>`) to satisfy the compiler.

- [ ] **Step 2: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/client/ZombifiedRenderHandler.java
git commit -m "feat(wildwest): client green-tint render handler for zombified mobs"
```

---

## Task 14: Generate textures + effect icons + lang strings

**Files:**
- Create: `wildwest/tools/gen_zombie_virus_textures.py`
- Generated: `wildwest/src/main/resources/assets/wildwest/textures/item/tainted_vial.png`
- Generated: `wildwest/src/main/resources/assets/wildwest/textures/item/walker_spawn_egg.png`
- Generated: `wildwest/src/main/resources/assets/wildwest/textures/entity/walker.png`
- Generated: `wildwest/src/main/resources/assets/wildwest/textures/mob_effect/festering_wound.png`
- Generated: `wildwest/src/main/resources/assets/wildwest/textures/mob_effect/zombified.png`
- Generated: `wildwest/src/main/resources/assets/wildwest/textures/mob_effect/curing_shake.png`
- Modify: `wildwest/src/main/resources/assets/wildwest/lang/en_us.json`

- [ ] **Step 1: Write the texture generator**

`wildwest/tools/gen_zombie_virus_textures.py`:

```python
#!/usr/bin/env python3
"""Generate placeholder PNGs for the zombie-virus feature.

Outputs:
- 16x16 tainted_vial item icon (vial silhouette w/ green liquid)
- 16x16 walker_spawn_egg flat icon
- 64x64 walker entity texture (humanoid skin, dark cowboy palette)
- 18x18 effect icons (festering_wound, zombified, curing_shake)
"""
import struct, zlib, os, sys

ROOT = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.abspath(os.path.join(ROOT, '..', 'src', 'main', 'resources', 'assets', 'wildwest'))

def write_png(path, w, h, rgba):
    sig = b'\x89PNG\r\n\x1a\n'
    def chunk(tag, data):
        crc = zlib.crc32(tag + data) & 0xffffffff
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', crc)
    ihdr = struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        raw.extend(rgba[4*w*y : 4*w*(y+1)])
    idat = zlib.compress(bytes(raw))
    out = sig + chunk(b'IHDR', ihdr) + chunk(b'IDAT', idat) + chunk(b'IEND', b'')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(out)

def solid_rect(rgba, w, x0, y0, x1, y1, color):
    r, g, b, a = color
    for y in range(y0, y1):
        for x in range(x0, x1):
            i = 4 * (y * w + x)
            rgba[i+0] = r; rgba[i+1] = g; rgba[i+2] = b; rgba[i+3] = a

def make_vial():
    w = h = 16
    rgba = bytearray(w * h * 4)
    GLASS = (200, 220, 220, 0xFF)
    LIQUID = (90, 140, 60, 0xFF)
    DARK = (40, 30, 30, 0xFF)
    # Bottle outline
    solid_rect(rgba, w, 6, 2, 10, 4, DARK)         # neck
    solid_rect(rgba, w, 5, 4, 11, 14, GLASS)       # body
    solid_rect(rgba, w, 4, 13, 12, 14, DARK)       # base
    # Liquid inside
    solid_rect(rgba, w, 6, 8, 10, 13, LIQUID)
    return w, h, rgba

def make_spawn_egg():
    w = h = 16
    rgba = bytearray(w * h * 4)
    PRIMARY = (0x1A, 0x3A, 0x1A)
    SECONDARY = (0x6B, 0x8E, 0x23)
    EGG = [
        "................","................","......####......",".....######.....",
        ".....######.....","....########....","....########....","...##########...",
        "...##########...","..############..","..############..","..############..",
        "...##########...","....########....",".....######.....","................",
    ]
    SPECKLES = [
        "................","................","................","......#.........",
        "....#.....#.....",".......#........","..#..........#..",".....#.....#....",
        "...#............","........#.......",".#...........#..","....#....#......",
        ".....#..........","................","................","................",
    ]
    for y in range(h):
        for x in range(w):
            if EGG[y][x] != '#': continue
            i = 4 * (y * w + x)
            r, g, b = SECONDARY if SPECKLES[y][x] == '#' else PRIMARY
            rgba[i+0] = r; rgba[i+1] = g; rgba[i+2] = b; rgba[i+3] = 0xFF
    return w, h, rgba

def make_walker_entity():
    """64x64 placeholder humanoid skin layout. Dark cowboy palette."""
    w = h = 64
    rgba = bytearray(w * h * 4)
    SKIN = (0x6B, 0x8E, 0x23, 0xFF)        # sickly green-tan
    SHIRT = (0x3A, 0x2A, 0x1A, 0xFF)       # dark brown
    PANTS = (0x2A, 0x1A, 0x10, 0xFF)
    HAT = (0x10, 0x10, 0x10, 0xFF)         # near-black hat
    # Head (face front 8..16, 8..16)
    solid_rect(rgba, w, 8, 8, 16, 16, SKIN)
    # Hat band
    solid_rect(rgba, w, 8, 8, 16, 10, HAT)
    # Body (16..24, 16..32)
    solid_rect(rgba, w, 16, 16, 24, 32, SHIRT)
    # Right arm (40..48, 16..32)
    solid_rect(rgba, w, 40, 16, 48, 32, SHIRT)
    # Right leg (0..8, 16..32)
    solid_rect(rgba, w, 0, 16, 8, 32, PANTS)
    # Left leg / arm via 1.8+ skin layout
    solid_rect(rgba, w, 16, 48, 24, 64, PANTS)   # left leg
    solid_rect(rgba, w, 32, 48, 40, 64, SHIRT)   # left arm
    return w, h, rgba

def make_effect_icon(primary, accent):
    """18x18 effect icon: filled circle of primary, accent dot in center."""
    w = h = 18
    rgba = bytearray(w * h * 4)
    cx = cy = 8
    for y in range(h):
        for x in range(w):
            d2 = (x-cx)*(x-cx) + (y-cy)*(y-cy)
            if d2 <= 64:
                i = 4 * (y * w + x)
                if d2 <= 4:
                    rgba[i+0], rgba[i+1], rgba[i+2] = accent
                else:
                    rgba[i+0], rgba[i+1], rgba[i+2] = primary
                rgba[i+3] = 0xFF
    return w, h, rgba

if __name__ == '__main__':
    w, h, rgba = make_vial()
    write_png(os.path.join(ASSETS, 'textures', 'item', 'tainted_vial.png'), w, h, rgba)
    print('tainted_vial.png')

    w, h, rgba = make_spawn_egg()
    write_png(os.path.join(ASSETS, 'textures', 'item', 'walker_spawn_egg.png'), w, h, rgba)
    print('walker_spawn_egg.png')

    w, h, rgba = make_walker_entity()
    write_png(os.path.join(ASSETS, 'textures', 'entity', 'walker.png'), w, h, rgba)
    print('walker.png')

    for name, primary, accent in [
        ('festering_wound', (0x6B, 0x8E, 0x23), (0x4A, 0x3A, 0x10)),
        ('zombified',       (0x4A, 0x7C, 0x2E), (0x10, 0x30, 0x10)),
        ('curing_shake',    (0xFF, 0xD7, 0x00), (0xFF, 0xFF, 0xFF)),
    ]:
        w, h, rgba = make_effect_icon(primary, accent)
        write_png(os.path.join(ASSETS, 'textures', 'mob_effect', f'{name}.png'), w, h, rgba)
        print(f'{name}.png')
```

- [ ] **Step 2: Run the generator**

Run: `python3 wildwest/tools/gen_zombie_virus_textures.py`
Expected output: 6 PNG file names printed.

- [ ] **Step 3: Create item-model + items JSONs (vial + walker spawn egg)**

`wildwest/src/main/resources/assets/wildwest/items/tainted_vial.json`:

```json
{ "model": { "type": "minecraft:model", "model": "wildwest:item/tainted_vial" } }
```

`wildwest/src/main/resources/assets/wildwest/models/item/tainted_vial.json`:

```json
{ "parent": "minecraft:item/generated", "textures": { "layer0": "wildwest:item/tainted_vial" } }
```

`wildwest/src/main/resources/assets/wildwest/items/walker_spawn_egg.json`:

```json
{ "model": { "type": "minecraft:model", "model": "wildwest:item/walker_spawn_egg" } }
```

`wildwest/src/main/resources/assets/wildwest/models/item/walker_spawn_egg.json`:

```json
{ "parent": "minecraft:item/generated", "textures": { "layer0": "wildwest:item/walker_spawn_egg" } }
```

- [ ] **Step 4: Update `lang/en_us.json`**

Read the current file first to preserve existing keys. Add:

```json
"item.wildwest.tainted_vial": "Tainted Vial",
"item.wildwest.walker_spawn_egg": "Walker Spawn Egg",
"entity.wildwest.walker": "Walker",
"effect.wildwest.festering_wound": "Festering Wound",
"effect.wildwest.zombified": "Zombified",
"effect.wildwest.curing_shake": "Curing"
```

- [ ] **Step 5: Build + verify resources**

Run: `./gradlew :wildwest:processResources`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add wildwest/tools/gen_zombie_virus_textures.py \
        wildwest/src/main/resources/assets/wildwest/
git commit -m "feat(wildwest): zombie-virus textures, item models, lang strings"
```

---

## Task 15: Walker loot table

**Files:**
- Create: `wildwest/src/main/resources/data/wildwest/loot_table/entities/walker.json`

- [ ] **Step 1: Create the loot table**

```json
{
  "type": "minecraft:entity",
  "pools": [
    {
      "rolls": 1.0,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:rotten_flesh",
          "functions": [
            { "function": "minecraft:set_count", "count": { "type": "minecraft:uniform", "min": 0, "max": 2 } },
            { "function": "minecraft:looting_enchant", "count": { "type": "minecraft:uniform", "min": 0, "max": 1 } }
          ]
        }
      ]
    },
    {
      "rolls": 1.0,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "wildwest:tainted_vial",
          "functions": [
            { "function": "minecraft:set_count", "count": { "type": "minecraft:uniform", "min": 0, "max": 1 } }
          ],
          "conditions": [
            { "condition": "minecraft:random_chance", "chance": 0.25 }
          ]
        }
      ]
    }
  ]
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :wildwest:processResources`
Expected: BUILD SUCCESSFUL.

If `looting_enchant` is unavailable in 26.x, drop the line — looting amplification is optional polish.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/resources/data/wildwest/loot_table/entities/walker.json
git commit -m "feat(wildwest): walker loot table"
```

---

## Task 16: End-to-end smoke test (manual)

The test path must be exercised in-game to verify the integration. There is no automated coverage for AI / rendering / spawning.

**Files:** none — runtime verification only.

- [ ] **Step 1: Launch the game**

Run: `./gradlew :wildwest:runClient` (background OK).

Wait for the world / main menu.

- [ ] **Step 2: Create a creative test world + record results**

Use `/give @s wildwest:walker_spawn_egg`. Spawn one. Verify:
- Walker has visible green tint (zombified shader).
- Ambient green particles emitted around it.
- Walker pursues the player.

Spawn a `minecraft:cow`. Let the Walker bite it. Verify:
- Cow gets a `festering_wound` effect (visible if you mount-spectate or `/effect query @e[type=cow,limit=1] wildwest:festering_wound`).

Wait 60 seconds (or `/time add 1200` if game time scales effects — usually it doesn't, so wait real-time). Verify:
- Cow becomes zombified — green tint, particles, and now hostile (it'll come at you).
- A second cow nearby — try right-click on the zombified cow with a golden apple. `curing_shake` effect appears. Wait 30 seconds. Cow becomes a normal cow again (no tint, no particles, no hostility).

Throw a tainted vial (right-click `wildwest:tainted_vial`) into a group of villagers. Verify:
- All in radius receive `festering_wound`.
- After 60 s, all zombify.

Drink milk while festering: effect clears.

Test direct-hit guard: get a zombified skeleton (e.g., spawn skeleton, give it `/effect give @e[type=skeleton,limit=1] wildwest:zombified infinite 0`). It will have its bow stripped. Watch it punch the player. Verify festering applies. Then equip skeleton again with bow via `/item replace entity @e[type=skeleton,limit=1] weapon.mainhand with bow` — it should resist (because we strip again on next zombify cycle, but if it's mid-zombify it might not). This is an edge case; primary test is the bare-handed skeleton biting.

Cure-interrupt test: zombify a sheep, golden-apple it, then hit it with a sword. Curing should cancel.

- [ ] **Step 3: Document results**

If everything works, commit a checklist marker:

```bash
git commit --allow-empty -m "chore(wildwest): zombie-virus smoke test passed"
```

If anything fails, file a follow-up issue with the specific symptom and continue.

---

## Spec coverage check

| Spec requirement | Task |
|---|---|
| `festering_wound` effect (60 s, on expiry → zombified) | 3, 5 |
| `zombified` effect (infinite, +30 % speed, particles) | 3 |
| `curing_shake` effect (30 s, on expiry → remove zombified) | 3, 5 |
| Bite spread on direct-hit only | 5 |
| Wither 10 % roll | 5 |
| Immune set (undead, bosses, walker, creative/spectator, non-Mob/non-Player) | 2, 5 |
| Disarm-on-zombify (Mobs, not Players) | 4, 5 |
| Universal AI via EntityJoinLevelEvent | 6, 7 |
| Cure: milk (vanilla — free) | (no task — vanilla milk handles) |
| Cure: golden apple → festering instant | 5 |
| Cure: golden apple → zombified shake → remove | 5 |
| Cure interrupt only on player damage | 5 |
| Tainted vial item + projectile + AABB-pre-filter sphere | 8, 9 |
| Walker carrier mob | 10 |
| Walker biome modifier (Frontier preset) | 11 |
| Walker spawn egg | 11 |
| Walker model + renderer | 12 |
| Client green tint w/ save/restore | 13 |
| Server particle broadcast tick | 3 (in `ZombifiedEffect.applyEffectTick`) |
| Recipes, loot, lang | 9, 14, 15 |
| Smoke test | 16 |

All spec lines accounted for.
