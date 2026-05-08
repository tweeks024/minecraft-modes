# Wild-West Mod — Steve Stacker Boss — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Steve Stacker boss mob: three vanilla Steves stacked vertically, single 90 HP pool with phase thresholds at 60 / 30 HP. Each phase shrinks the visible stack and the bbox (5.85 → 3.90 → 1.95) and ramps speed/damage. Melee-only. Spawn egg + rare night spawn in plains/savanna. Vendors a copy of the default Steve skin to insulate against vanilla path changes.

**Architecture:** Single `SteveStackerEntity extends Monster` (mirrors `WalkerEntity`). Phase state lives in `SynchedEntityData<Byte> STACK_HEIGHT` (3/2/1) with NBT persistence. Phase logic extracted as a static pure function `computeStackHeight(health, maxHealth)` so it is unit-testable without booting Minecraft. Hitbox is dynamic: `getDimensions(Pose)` returns `EntityDimensions.scalable(0.6f, 1.95f * stackHeight)`, and the transition path calls `refreshDimensions()` after mutating the data accessor. Three identical humanoid sub-skeletons in a custom `EntityModel<SteveStackerRenderState>`; visibility flags shrink the visible stack from the top while the bottom Steve stays at ground level. Boss bar via vanilla `ServerBossEvent`.

**Tech Stack:** Java 25, NeoForge (version from `gradle.properties`'s `neo_version`), Minecraft 26.1.2 (the version that ships with that NeoForge), JUnit 5.

**Spec:** [docs/superpowers/specs/2026-05-08-steve-stacker-boss-design.md](../specs/2026-05-08-steve-stacker-boss-design.md)

**API lessons carried forward (phases 1–3 + zombie-virus):**
- `Identifier` not `ResourceLocation`.
- `Identifier.fromNamespaceAndPath(MOD_ID, "...")`.
- `level.isClientSide()` is a method.
- Inside entity classes, `Registration` is name-shadowed by an inherited symbol — use FQN `com.tweeks.wildwest.Registration.X` if needed (this plan does not need `Registration` from inside the entity, but the rule still applies).
- Biome modifiers live at `src/main/resources/data/wildwest/neoforge/biome_modifier/<name>.json`.
- Spawn placements are registered in `WildWestMod.java`'s `RegisterSpawnPlacementsEvent` listener (already wired for other mobs).
- Loot tables live at `src/main/resources/data/wildwest/loot_table/entities/<name>.json`.
- Render state pattern uses `HumanoidRenderState` (or a subtype) and `extractRenderState` writes per-frame fields onto it.
- Entity dimensions: `EntityDimensions.scalable(width, height)` for resizable mobs; `Entity#refreshDimensions()` recomputes after `getDimensions(Pose)` would return something new.

**API risks to validate during implementation:**
- `Mob#getDimensions(Pose pose)` and `Entity#refreshDimensions()` — verify both compile in 26.x before Task 5 (extracted strings from `Entity.class` confirm both names exist; signature shape may need a tweak).
- `ServerBossEvent(Component, BossBarColor, BossBarOverlay)` constructor in 26.x — verify before Task 6.
- `MobRenderer<E, S, M>` generic signature in 26.x and the `extractRenderState(entity, state, partialTick)` override shape — verify before Task 11.
- `EntityModel<S>` constructor (root part) and `setupAnim(state)` override shape in 26.x — verify before Task 11.

---

## File Structure

```
wildwest/src/main/java/com/tweeks/wildwest/
  entity/
    SteveStackerEntity.java                 NEW   Monster subclass; phases, dynamic bbox, boss bar, sounds, XP
  client/
    SteveStackerRenderer.java               NEW   MobRenderer<SteveStackerEntity, SteveStackerRenderState, SteveStackerModel>
    SteveStackerRenderState.java            NEW   extends HumanoidRenderState; adds `byte stackHeight`
    model/
      SteveStackerModel.java                NEW   EntityModel<SteveStackerRenderState>; 3 humanoid sub-roots
    ClientSetup.java                        MOD   register renderer + layer
  ModEntities.java                          MOD   register STEVE_STACKER entity type
  Registration.java                         MOD   register STEVE_STACKER_SPAWN_EGG, add to creative tab
  WildWestMod.java                          MOD   attributes + spawn placement listener entry

wildwest/src/main/resources/
  assets/wildwest/
    lang/en_us.json                         MOD   add 2 lang keys
    textures/entity/steve_stacker.png       NEW   vendored 64×64 default Steve skin
  data/wildwest/
    loot_table/entities/steve_stacker.json  NEW   3× diamond
    neoforge/biome_modifier/
      steve_stacker_spawns.json             NEW   plains/savanna/savanna_plateau, weight 1

wildwest/src/test/java/com/tweeks/wildwest/
  entity/
    SteveStackerPhaseLogicTest.java         NEW   JUnit 5 tests for computeStackHeight
```

---

## Task 1: Pre-flight — register `STEVE_STACKER` entity skeleton + smoke build

**Why first:** the entity registration plumbing (DeferredHolder, EntityType.Builder, attribute registration, spawn placement) has the most version-sensitive surface. Stand up a no-op `Monster` subclass first so we know the skeleton compiles before adding phase logic, boss bars, render state, etc.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/SteveStackerEntity.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`

- [ ] **Step 1: Create `SteveStackerEntity.java` skeleton (no phase logic yet)**

```java
package com.tweeks.wildwest.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class SteveStackerEntity extends Monster {

    public SteveStackerEntity(EntityType<? extends SteveStackerEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 90.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
}
```

- [ ] **Step 2: Register `STEVE_STACKER` in `ModEntities.java`**

In `ModEntities.java`, add the import and a `DeferredHolder` after `WALKER`:

```java
import com.tweeks.wildwest.entity.SteveStackerEntity;

public static final DeferredHolder<EntityType<?>, EntityType<SteveStackerEntity>> STEVE_STACKER =
    ENTITY_TYPES.register("steve_stacker", () -> EntityType.Builder.<SteveStackerEntity>of(
            SteveStackerEntity::new, MobCategory.MONSTER)
        .sized(0.6f, 5.85f)
        .clientTrackingRange(10)
        .build(ResourceKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "steve_stacker"))));
```

- [ ] **Step 3: Wire attribute creation + spawn placement in `WildWestMod.java`**

In `registerEntityAttributes`, append:

```java
event.put(ModEntities.STEVE_STACKER.get(), SteveStackerEntity.createAttributes().build());
```

(Add the import for `com.tweeks.wildwest.entity.SteveStackerEntity` at the top of the file.)

In the `RegisterSpawnPlacementsEvent` listener block, append a new `event.register(...)` call after the WALKER entry:

```java
event.register(ModEntities.STEVE_STACKER.get(),
    net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
    net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
    net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
```

- [ ] **Step 4: Build and verify**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

If a goal class import doesn't resolve, double-check the `ai.goal` / `ai.goal.target` package paths — the imports above match what `WalkerEntity` uses for similar goals.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/SteveStackerEntity.java \
        wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java \
        wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java
git commit -m "feat(wildwest): SteveStackerEntity skeleton + registration"
```

---

## Task 2: `computeStackHeight` static helper + JUnit tests

**Why a pure helper:** the phase-band logic (`health > 60 → 3`, `30 < health ≤ 60 → 2`, `health ≤ 30 → 1`) is the one piece of behaviour we can unit-test without spinning up a Minecraft world. Extracting it as a `static` function means we never have to mock `LivingEntity#getHealth()`. The same pattern is used by `InfectionImmunity` and `WeaponMode` in this codebase.

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/SteveStackerEntity.java`
- Create: `wildwest/src/test/java/com/tweeks/wildwest/entity/SteveStackerPhaseLogicTest.java`

- [ ] **Step 1: Write failing tests**

`wildwest/src/test/java/com/tweeks/wildwest/entity/SteveStackerPhaseLogicTest.java`:

```java
package com.tweeks.wildwest.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SteveStackerPhaseLogicTest {

    private static final float MAX = 90.0f;

    @Test
    void fullHp_returnsThree() {
        assertEquals((byte) 3, SteveStackerEntity.computeStackHeight(MAX, MAX));
    }

    @Test
    void justAbovePhase2_returnsThree() {
        assertEquals((byte) 3, SteveStackerEntity.computeStackHeight(60.01f, MAX));
    }

    @Test
    void atPhase2Threshold_returnsTwo() {
        assertEquals((byte) 2, SteveStackerEntity.computeStackHeight(60.0f, MAX));
    }

    @Test
    void justAbovePhase3_returnsTwo() {
        assertEquals((byte) 2, SteveStackerEntity.computeStackHeight(30.01f, MAX));
    }

    @Test
    void atPhase3Threshold_returnsOne() {
        assertEquals((byte) 1, SteveStackerEntity.computeStackHeight(30.0f, MAX));
    }

    @Test
    void atZero_returnsOne() {
        assertEquals((byte) 1, SteveStackerEntity.computeStackHeight(0.0f, MAX));
    }

    @Test
    void negativeOverflow_returnsOne() {
        assertEquals((byte) 1, SteveStackerEntity.computeStackHeight(-10.0f, MAX));
    }
}
```

- [ ] **Step 2: Verify tests fail**

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.entity.SteveStackerPhaseLogicTest"`
Expected: FAIL — `computeStackHeight` is not defined.

- [ ] **Step 3: Add `computeStackHeight` to `SteveStackerEntity`**

Add this static method to `SteveStackerEntity` (anywhere in the class body):

```java
/**
 * Pure phase-band logic. Extracted as static so the band thresholds can be unit-tested
 * without instantiating a live entity.
 *
 * @param health current HP
 * @param maxHealth max HP (typically 90)
 * @return stack height (3 = full stack, 2 = mid phase, 1 = final phase)
 */
public static byte computeStackHeight(float health, float maxHealth) {
    if (health > 60.0f) return 3;
    if (health > 30.0f) return 2;
    return 1;
}
```

Note: thresholds are hard-coded (not derived from `maxHealth`) because the spec fixes max HP at 90 and the bands at 60/30. If max HP ever changes, the test cases AND this function need to be updated together. The `maxHealth` parameter is kept in the signature so future scaling is a one-call-site change.

- [ ] **Step 4: Verify tests pass**

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.entity.SteveStackerPhaseLogicTest"`
Expected: PASS — all 7 tests.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/SteveStackerEntity.java \
        wildwest/src/test/java/com/tweeks/wildwest/entity/SteveStackerPhaseLogicTest.java
git commit -m "feat(wildwest): SteveStacker pure phase-height function + tests"
```

---

## Task 3: Synced `STACK_HEIGHT` data accessor + NBT persistence

**Why next:** the stack-height byte is read by client (renderer + future model) and server (transition logic), and persists across world reload. Wire it in isolation before the transition logic that mutates it, so we can verify NBT round-trip with no other moving parts.

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/SteveStackerEntity.java`

- [ ] **Step 1: Add the data accessor + getter/setter + NBT save/load**

In `SteveStackerEntity`, add the imports:

```java
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
```

Inside the class body:

```java
private static final EntityDataAccessor<Byte> STACK_HEIGHT =
    SynchedEntityData.defineId(SteveStackerEntity.class, EntityDataSerializers.BYTE);

@Override
protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(STACK_HEIGHT, (byte) 3);
}

public byte getStackHeight() {
    return this.entityData.get(STACK_HEIGHT);
}

private void setStackHeight(byte value) {
    this.entityData.set(STACK_HEIGHT, value);
}

@Override
public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
    super.addAdditionalSaveData(output);
    output.putByte("StackHeight", this.getStackHeight());
}

@Override
public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
    super.readAdditionalSaveData(input);
    this.setStackHeight(input.getByteOr("StackHeight", (byte) 3));
}
```

**API note:** wildwest already uses the `ValueOutput`/`ValueInput` save/load shape (see commit `cfabf47` and `WildWestMob` in this module). If those methods don't compile or the helper names differ (`getByteOr` vs `getByte`), check `WildWestMob.java` in this module for the exact form and copy it. Do not switch back to `CompoundTag`-based save/load — that path is deprecated in 26.x.

- [ ] **Step 2: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

If the `ValueOutput`/`ValueInput` import paths are wrong, search for `addAdditionalSaveData` in this module's existing entity classes and match those imports.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/SteveStackerEntity.java
git commit -m "feat(wildwest): SteveStacker synced STACK_HEIGHT data accessor + NBT"
```

---

## Task 4: Dynamic hitbox via `getDimensions(Pose)`

**Why now:** before the transition logic mutates `STACK_HEIGHT`, hook the dimensions function so a phase change (whatever causes it later) automatically narrows the hitbox. Doing this independently means the boss in phase 1 looks and behaves correctly even before transitions exist.

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/SteveStackerEntity.java`

- [ ] **Step 1: Override `getDimensions(Pose)`**

Add the imports:

```java
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
```

Override the method:

```java
@Override
public EntityDimensions getDimensions(Pose pose) {
    byte stack = this.getStackHeight();
    if (stack < 1) stack = 1;
    if (stack > 3) stack = 3;
    return EntityDimensions.scalable(0.6f, 1.95f * stack);
}
```

The clamp guards against malformed NBT or stale data — the hitbox should never be negative-height and never larger than the registered max (5.85).

- [ ] **Step 2: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

If `EntityDimensions.scalable(...)` doesn't resolve, the alternative in 26.x is `EntityDimensions.fixed(width, height)` — the difference is whether the hitbox auto-scales with mounted entities. We want a simple non-scaled hitbox; `fixed` is the right fallback. Update both spec and code if forced to change.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/SteveStackerEntity.java
git commit -m "feat(wildwest): SteveStacker dynamic hitbox per stack height"
```

---

## Task 5: Phase transition logic in `aiStep`

**Why now:** the synced data accessor + dynamic dimensions are in place. The transition logic is the bridge — it uses `computeStackHeight` (Task 2), mutates `STACK_HEIGHT` (Task 3), and calls `refreshDimensions()` (Task 4) so the hitbox shrinks. Adding it last means each part has been wired in isolation.

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/SteveStackerEntity.java`

- [ ] **Step 1: Implement `aiStep` override**

Add the imports:

```java
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
```

Inside `SteveStackerEntity`:

```java
@Override
public void aiStep() {
    super.aiStep();
    if (this.level().isClientSide()) return;

    byte previous = this.getStackHeight();
    byte target = computeStackHeight(this.getHealth(), this.getMaxHealth());
    if (target < previous) {
        // Capture the y-position of the Steve that just fell off BEFORE we shrink.
        // Each Steve occupies 1.95 blocks of vertical bbox; the "top" of the pre-shrink
        // stack is at this.getY() + (previous * 1.95).
        double poofY = this.getY() + (previous * 1.95) - 0.95;
        double poofX = this.getX();
        double poofZ = this.getZ();

        this.entityData.set(STACK_HEIGHT, target);
        this.refreshDimensions();
        applyPhaseAttributes(target);

        ServerLevel server = (ServerLevel) this.level();
        for (int i = 0; i < 24; i++) {
            server.sendParticles(ParticleTypes.POOF,
                poofX + (this.random.nextDouble() - 0.5) * 0.6,
                poofY + (this.random.nextDouble() - 0.5) * 0.4,
                poofZ + (this.random.nextDouble() - 0.5) * 0.6,
                1, 0.0, 0.0, 0.0, 0.02);
        }
        server.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.6f, 1.2f);
    }
}

private void applyPhaseAttributes(byte stackHeight) {
    double speed;
    double damage;
    switch (stackHeight) {
        case 3 -> { speed = 0.25; damage = 4.0; }
        case 2 -> { speed = 0.30; damage = 6.0; }
        default -> { speed = 0.38; damage = 8.0; }  // stackHeight == 1 (or clamped)
    }
    var speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
    if (speedAttr != null) speedAttr.setBaseValue(speed);
    var damageAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
    if (damageAttr != null) damageAttr.setBaseValue(damage);
}
```

**Important:** the transition only fires when `target < previous`. This is the monotonic-phase guarantee — heal effects cannot regrow the stack, and the body skips work on every tick where the stack is unchanged.

- [ ] **Step 2: Apply phase attributes on load + spawn (so a saved boss restores its phase-3 speed/damage)**

Add this method:

```java
@Override
public void onAddedToLevel() {
    super.onAddedToLevel();
    if (!this.level().isClientSide()) {
        applyPhaseAttributes(this.getStackHeight());
    }
}
```

`onAddedToLevel` fires on both first spawn (where stack=3 and applies the phase-1 numbers — equivalent to the createAttributes defaults, harmless) and on world load (where a saved phase-3 boss correctly gets the phase-3 numbers without waiting for an HP threshold trigger).

If `onAddedToLevel` does not exist in this module's vanilla MC version, fall back to overriding `tick()` once with a `boolean appliedInitialPhase` guard — but try `onAddedToLevel` first; it has been on `Entity` for many versions.

- [ ] **Step 3: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

Common failure: `SoundEvents.GENERIC_EXPLODE` may be a `Holder<SoundEvent>` rather than `SoundEvent` directly — that's why we call `.value()` on it. If that's still wrong, look at how `WildWestMob` plays sounds (it does this same pattern, e.g., in gun fire).

- [ ] **Step 4: Run all tests (regression check on phase logic)**

Run: `./gradlew :wildwest:test`
Expected: PASS — `SteveStackerPhaseLogicTest` still green.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/SteveStackerEntity.java
git commit -m "feat(wildwest): SteveStacker phase transitions in aiStep"
```

---

## Task 6: Boss bar via `ServerBossEvent`

**Why now:** the entity is fully functional combat-wise. Adding the boss bar is independent of phase logic and is the last server-side gameplay feature.

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/SteveStackerEntity.java`

- [ ] **Step 1: Add boss bar field + lifecycle hooks**

Add the imports:

```java
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.scores.PlayerTeam;  // (only if your version surfaces team-coloured bars; otherwise skip)
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.server.level.ServerBossEvent;
```

(If `ServerBossEvent` lives in a different package in 26.x — most commonly `net.minecraft.server.level` or `net.minecraft.world.bossevent` — adjust the import. The vanilla Wither uses it; grep the vendored MC class jar via `unzip -l` if needed.)

Inside `SteveStackerEntity`:

```java
private final ServerBossEvent bossBar = new ServerBossEvent(
    Component.translatable("entity.wildwest.steve_stacker"),
    BossBarColor.PURPLE,
    BossBarOverlay.PROGRESS);
```

Wire the bar to the boss's HP each tick. Inside the `aiStep()` method written in Task 5, add this line **after** the `if (this.level().isClientSide()) return;` guard (the boss bar is a server-side object — calling `setProgress` on the client would have no effect at best and could touch null state at worst):

```java
this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
```

Place it before the `byte previous = this.getStackHeight();` line so the bar always reflects the latest health, even on the rare tick where a phase transition runs.

Override `startSeenByPlayer` and `stopSeenByPlayer` to attach/detach players:

```java
@Override
public void startSeenByPlayer(ServerPlayer player) {
    super.startSeenByPlayer(player);
    this.bossBar.addPlayer(player);
}

@Override
public void stopSeenByPlayer(ServerPlayer player) {
    super.stopSeenByPlayer(player);
    this.bossBar.removePlayer(player);
}
```

Set the custom name in the constructor (so the bar's name is correct) by adding to the constructor body after `setPersistenceRequired()`:

```java
this.setCustomName(Component.translatable("entity.wildwest.steve_stacker"));
this.setCustomNameVisible(false);  // no nameplate floating above the head; the boss bar is the name surface
```

- [ ] **Step 2: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

If `ServerBossEvent` is not found, drop the `server.level` package guess and try `import net.minecraft.world.BossEvent;` plus the vanilla-internal helper. Vanilla Wither (`WitherBoss.java`) is the canonical reference — the import that file uses is what we should use here.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/SteveStackerEntity.java
git commit -m "feat(wildwest): SteveStacker boss bar"
```

---

## Task 7: Sound overrides + XP reward

**Why now:** small isolated polish. Self-contained.

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/SteveStackerEntity.java`

- [ ] **Step 1: Override sound methods + XP**

Add the imports:

```java
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
```

Inside `SteveStackerEntity`:

```java
@Override
protected SoundEvent getAmbientSound() {
    return SoundEvents.PLAYER_BREATH;
}

@Override
protected SoundEvent getHurtSound(DamageSource damageSource) {
    return SoundEvents.PLAYER_HURT;
}

@Override
protected SoundEvent getDeathSound() {
    return SoundEvents.PLAYER_DEATH;
}

@Override
public int getBaseExperienceReward(ServerLevel level) {
    return 50;
}
```

If `SoundEvents.PLAYER_BREATH` is reported missing at compile time, double-check the import (`net.minecraft.sounds.SoundEvents`). The constant exists in MC 26.1.2 (verified by string-extracting the class jar) — a missing-symbol error means the import is wrong, not the API.

If `getBaseExperienceReward(ServerLevel)` does not exist or has a different signature, search this module for any existing override (none currently — most mobs use the loot table for XP) and check the parent class. Possible alternate signature: `protected int getBaseExperienceReward()` no-arg. Try the no-arg version if the ServerLevel form fails.

- [ ] **Step 2: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/SteveStackerEntity.java
git commit -m "feat(wildwest): SteveStacker player-skin sounds + 50 XP drop"
```

---

## Task 8: Loot table — 3 diamonds on death

**Why now:** server-side data only; independent of any code we just wrote.

**Files:**
- Create: `wildwest/src/main/resources/data/wildwest/loot_table/entities/steve_stacker.json`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/SteveStackerEntity.java`

- [ ] **Step 1: Create the loot table**

`wildwest/src/main/resources/data/wildwest/loot_table/entities/steve_stacker.json`:

```json
{
  "type": "minecraft:entity",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:diamond",
          "functions": [
            {
              "function": "minecraft:set_count",
              "count": 3
            }
          ]
        }
      ]
    }
  ]
}
```

- [ ] **Step 2: Verify the entity references the correct loot table**

By default `Mob#getDefaultLootTable()` resolves to `data/<modid>/loot_table/entities/<entity_id>.json` for the entity's registered id. With our id `wildwest:steve_stacker`, the JSON above is automatically found — no code change needed.

- [ ] **Step 3: Build**

Run: `./gradlew :wildwest:build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/resources/data/wildwest/loot_table/entities/steve_stacker.json
git commit -m "feat(wildwest): SteveStacker drops 3 diamonds on death"
```

---

## Task 9: Vendored Steve skin texture

**Why now:** texture is needed before the renderer is registered. Ship a copy of the default Steve skin to insulate against vanilla path changes.

**Files:**
- Create: `wildwest/src/main/resources/assets/wildwest/textures/entity/steve_stacker.png`

- [ ] **Step 1: Extract the default Steve skin from the vanilla jar**

Run:

```bash
# Locate the MC client jar
MC_JAR=$(find ~/.gradle/caches/neoformruntime/artifacts -name "minecraft_*_client.jar" | head -1)
echo "Using MC jar: $MC_JAR"

# Try the most common path; fall back if needed
mkdir -p wildwest/src/main/resources/assets/wildwest/textures/entity
unzip -p "$MC_JAR" assets/minecraft/textures/entity/player/wide/steve.png \
  > wildwest/src/main/resources/assets/wildwest/textures/entity/steve_stacker.png 2>/dev/null \
  || unzip -p "$MC_JAR" assets/minecraft/textures/entity/steve.png \
  > wildwest/src/main/resources/assets/wildwest/textures/entity/steve_stacker.png

# Verify it's a real 64x64 PNG (>1 KB and starts with PNG magic)
file wildwest/src/main/resources/assets/wildwest/textures/entity/steve_stacker.png
```

Expected: file output reports `PNG image data, 64 x 64`.

If both paths fail, list the jar's player-skin contents:

```bash
unzip -l "$MC_JAR" | grep -i "steve\.png\|player.*\.png" | head
```

…and copy whichever path exists.

- [ ] **Step 2: Sanity-check size**

Run: `wc -c wildwest/src/main/resources/assets/wildwest/textures/entity/steve_stacker.png`
Expected: somewhere between 800 and 4000 bytes (default Steve skin is ~1 KB).

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/resources/assets/wildwest/textures/entity/steve_stacker.png
git commit -m "chore(wildwest): vendor default Steve skin for SteveStacker"
```

---

## Task 10: `SteveStackerRenderState` (custom render state)

**Why now:** the model and renderer both depend on this class. Keep it in its own task because it's a 5-line type whose only job is to add a `byte stackHeight` field.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/SteveStackerRenderState.java`

- [ ] **Step 1: Create the render state**

```java
package com.tweeks.wildwest.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class SteveStackerRenderState extends HumanoidRenderState {
    public byte stackHeight = 3;
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/client/SteveStackerRenderState.java
git commit -m "feat(wildwest): SteveStackerRenderState"
```

---

## Task 11: `SteveStackerModel` (3 humanoid sub-roots, visibility-based shrink)

**Why now:** with the render state defined, build the model. This is the most version-sensitive piece (model API has shifted across MC releases). Compile + visual verification will follow in the next task.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/model/SteveStackerModel.java`

- [ ] **Step 1: Create the model**

```java
package com.tweeks.wildwest.client.model;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.SteveStackerRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * Three humanoid skeletons stacked vertically. Each sub-root contains the
 * same vanilla humanoid layout (head/body/arms/legs). Visibility flags hide
 * the upper sub-roots as the boss takes damage; the bottom Steve is always
 * the one left standing in phase 3.
 */
public class SteveStackerModel extends EntityModel<SteveStackerRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "steve_stacker"),
        "main");

    /** Vanilla humanoid model height in pixels (head 8 + body 12 + legs 12 = 32, but we offset
     *  using the conventional 24-pixel "feet to crown of head" measurement that matches a
     *  1.95-block tall vanilla mob. */
    private static final float STEVE_PIXEL_HEIGHT = 32.0f;

    private final ModelPart steveTop;
    private final ModelPart steveMid;
    private final ModelPart steveBot;

    public SteveStackerModel(ModelPart root) {
        super(root);
        this.steveTop = root.getChild("steve_top");
        this.steveMid = root.getChild("steve_mid");
        this.steveBot = root.getChild("steve_bot");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Build three vanilla humanoid skeletons. Each one is the standard
        // HumanoidModel.createMesh layout, copied as a sub-tree under our root,
        // with the bottom Steve at y=0 (feet on the ground), middle Steve
        // STEVE_PIXEL_HEIGHT pixels above that, and top Steve another
        // STEVE_PIXEL_HEIGHT pixels above THAT.
        //
        // Negative Y in model space is up (Minecraft model convention).
        attachHumanoidAt(root, "steve_bot", 0.0f);
        attachHumanoidAt(root, "steve_mid", -STEVE_PIXEL_HEIGHT);
        attachHumanoidAt(root, "steve_top", -STEVE_PIXEL_HEIGHT * 2);

        // 64x64 texture, standard Steve UV layout
        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * Attaches a complete vanilla humanoid skeleton (head/body/arms/legs) under
     * `parent` at the given Y offset, rooted via a single empty `PartPose`.
     */
    private static void attachHumanoidAt(PartDefinition parent, String name, float yOffset) {
        // Build a fresh humanoid mesh and copy its part definitions into our subtree.
        // We use HumanoidModel.createMesh as the source of truth so we get the
        // exact vanilla cube layout (matters for skin texture alignment).
        MeshDefinition source = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition srcRoot = source.getRoot();

        PartDefinition holder = parent.addOrReplaceChild(name,
            net.minecraft.client.model.geom.builders.CubeListBuilder.create(),
            PartPose.offset(0.0f, yOffset, 0.0f));

        // Copy each child of the humanoid root into our holder. The standard
        // children are: head, body, right_arm, left_arm, right_leg, left_leg.
        // We rely on srcRoot exposing children via reflection-free traversal:
        // PartDefinition's API in 26.x exposes a children-iteration helper —
        // if not available, fall back to manually re-defining each part
        // (head/body/arms/legs) with the cube coords from HumanoidModel.
        for (String childName : new String[]{"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
            PartDefinition child = srcRoot.getChild(childName);
            holder.addOrReplaceChild(childName, child);
        }
    }

    @Override
    public void setupAnim(SteveStackerRenderState state) {
        super.setupAnim(state);

        byte stack = state.stackHeight;
        steveTop.visible = stack >= 3;
        steveMid.visible = stack >= 2;
        steveBot.visible = stack >= 1;

        // Walking-animation pass-through. The wobble offset (top leads, bottom lags)
        // gives the stack a swaying feel without per-Steve walk anim.
        animateLimbs(steveTop, state, +0.3f);
        animateLimbs(steveMid, state, 0.0f);
        animateLimbs(steveBot, state, -0.3f);
    }

    private static void animateLimbs(ModelPart steve, SteveStackerRenderState state, float phaseOffset) {
        ModelPart leftArm = steve.getChild("left_arm");
        ModelPart rightArm = steve.getChild("right_arm");
        ModelPart leftLeg = steve.getChild("left_leg");
        ModelPart rightLeg = steve.getChild("right_leg");

        float walk = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.0f);
        float swing = (float) Math.cos(walk * 0.6662f + phaseOffset) * 1.4f * speed;

        rightArm.xRot = -swing;
        leftArm.xRot = swing;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
    }
}
```

**API risk note for this task:** the `addOrReplaceChild(String, PartDefinition)` shape used in `attachHumanoidAt` may not exist in 26.x — `PartDefinition`'s public surface usually requires `addOrReplaceChild(String, CubeListBuilder, PartPose)`. If the `(String, PartDefinition)` form is missing, the fallback is to **stop reusing `HumanoidModel.createMesh`** and re-define each part manually with vanilla cube coords:

- `head`: `CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8)` at `PartPose.offset(0, -24, 0)` (relative to the holder).
- `body`: `texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4)` at `PartPose.offset(0, -24, 0)`.
- `right_arm`: `texOffs(40, 16).addBox(-3, -2, -2, 4, 12, 4)` at `PartPose.offset(-5, -22, 0)`.
- `left_arm`: `texOffs(32, 48).mirror().addBox(-1, -2, -2, 4, 12, 4)` at `PartPose.offset(5, -22, 0)`.
- `right_leg`: `texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4)` at `PartPose.offset(-1.9, -12, 0)`.
- `left_leg`: `texOffs(16, 48).mirror().addBox(-2, 0, -2, 4, 12, 4)` at `PartPose.offset(1.9, -12, 0)`.

These coords match the vanilla 64×64 Steve UV layout. If you go this route, also note the holder offset (the `yOffset` arg) needs to add 24 to compensate for the in-skeleton translation (the head's y=-24 above the body's origin). Concretely: pass `yOffset = -STEVE_PIXEL_HEIGHT - 24` rather than `-STEVE_PIXEL_HEIGHT` for the manual route.

- [ ] **Step 2: Build**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

If the `addOrReplaceChild(String, PartDefinition)` line fails, switch to the manual cube-redefinition fallback documented above. Fix the spec if you change the structure.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/client/model/SteveStackerModel.java
git commit -m "feat(wildwest): SteveStackerModel — 3 humanoid sub-roots"
```

---

## Task 12: `SteveStackerRenderer` + ClientSetup wiring

**Why now:** model + render state are defined; this task glues them to the entity type and registers the layer.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/SteveStackerRenderer.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java`

- [ ] **Step 1: Create the renderer**

```java
package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.model.SteveStackerModel;
import com.tweeks.wildwest.entity.SteveStackerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class SteveStackerRenderer
        extends MobRenderer<SteveStackerEntity, SteveStackerRenderState, SteveStackerModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/steve_stacker.png");

    public SteveStackerRenderer(EntityRendererProvider.Context context) {
        super(context, new SteveStackerModel(context.bakeLayer(SteveStackerModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public SteveStackerRenderState createRenderState() {
        return new SteveStackerRenderState();
    }

    @Override
    public void extractRenderState(SteveStackerEntity entity, SteveStackerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.stackHeight = entity.getStackHeight();
    }

    @Override
    public Identifier getTextureLocation(SteveStackerRenderState state) {
        return TEXTURE;
    }
}
```

- [ ] **Step 2: Register the renderer + layer in `ClientSetup.java`**

In `registerRenderers`, append:

```java
event.registerEntityRenderer(ModEntities.STEVE_STACKER.get(), SteveStackerRenderer::new);
```

In `registerLayerDefinitions`, append:

```java
event.registerLayerDefinition(SteveStackerModel.LAYER_LOCATION, SteveStackerModel::createBodyLayer);
```

Add the imports for `SteveStackerRenderer` and `SteveStackerModel` at the top of `ClientSetup.java` (mirror the existing `WalkerModel` / `WalkerRenderer` imports).

- [ ] **Step 3: Build**

Run: `./gradlew :wildwest:build`
Expected: BUILD SUCCESSFUL.

If `extractRenderState`'s signature is different in 26.x (e.g., no `partialTick` arg), match the override the existing `WalkerRenderer` uses. WalkerRenderer relies on the parent class's default `extractRenderState`, but if it overrides one, copy its arity.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/client/SteveStackerRenderer.java \
        wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java
git commit -m "feat(wildwest): register SteveStacker renderer + layer"
```

---

## Task 13: Spawn egg item + creative tab + lang strings

**Why now:** the entity is fully renderable; now make it summonable.

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`
- Modify: `wildwest/src/main/resources/assets/wildwest/lang/en_us.json`

- [ ] **Step 1: Register the spawn egg in `Registration.java`**

After the existing `WALKER_SPAWN_EGG` declaration, add:

```java
public static final DeferredItem<SpawnEggItem> STEVE_STACKER_SPAWN_EGG = ITEMS.registerItem(
    "steve_stacker_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.STEVE_STACKER.get()));
```

In the `WILDWEST_TAB` `displayItems` block, append:

```java
output.accept(STEVE_STACKER_SPAWN_EGG.get());
```

- [ ] **Step 2: Add lang strings**

Open `wildwest/src/main/resources/assets/wildwest/lang/en_us.json` and add the two keys (alongside the existing entries; pick a stable spot in the file):

```json
"entity.wildwest.steve_stacker": "Steve Stacker",
"item.wildwest.steve_stacker_spawn_egg": "Steve Stacker Spawn Egg"
```

(Comma placement: if these are appended at the end of the JSON object, ensure the previous-last entry has a trailing comma and these two are the new last entries.)

- [ ] **Step 3: Build**

Run: `./gradlew :wildwest:build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/Registration.java \
        wildwest/src/main/resources/assets/wildwest/lang/en_us.json
git commit -m "feat(wildwest): SteveStacker spawn egg + lang strings"
```

---

## Task 14: Natural rare spawn — biome modifier JSON

**Why now:** spawn placement is already wired via Task 1's `RegisterSpawnPlacementsEvent` listener entry; this task adds the biome-side data file that opts the boss into night spawns.

**Files:**
- Create: `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/steve_stacker_spawns.json`

- [ ] **Step 1: Create the biome modifier**

```json
{
  "type": "neoforge:add_spawns",
  "biomes": [
    "minecraft:plains",
    "minecraft:savanna",
    "minecraft:savanna_plateau"
  ],
  "spawners": {
    "type": "wildwest:steve_stacker",
    "maxCount": 1,
    "minCount": 1,
    "weight": 1
  }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :wildwest:build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/steve_stacker_spawns.json
git commit -m "feat(wildwest): SteveStacker rare night spawn in plains/savanna"
```

---

## Task 15: Manual integration testing

**Why a checkpoint:** unit tests cover phase-band logic but render, AI goals, hitbox, and boss bar are integration concerns. Verify in a live client before declaring done.

**Files:** none (test plan only).

- [ ] **Step 1: Launch the dev client**

Run: `./gradlew :wildwest:runClient`
Expected: client launches without crashing; the wildwest creative tab is visible.

- [ ] **Step 2: Spawn via egg**

In a creative world: open the wildwest tab, take the Steve Stacker spawn egg, place a stacker in an open area.

Verify:
- [x] The boss renders as **three identical Steves stacked**, total height ≈ 6 blocks.
- [x] The boss bar appears at the top of the screen, name "Steve Stacker", purple, full HP.
- [x] No console errors.

- [ ] **Step 3: Damage to phase 2**

Switch to survival; deal damage until HP ≤ 60.

Verify:
- [x] Top Steve disappears.
- [x] A poof particle burst appears at the top of the previous stack height.
- [x] An explosion-like sound plays.
- [x] The boss is now visibly 2 Steves tall.
- [x] The hitbox shrinks (verify by trying to swing a sword 4 blocks above the visible top — should miss).
- [x] The boss is noticeably faster and hits harder.

- [ ] **Step 4: Damage to phase 3**

Continue until HP ≤ 30.

Verify:
- [x] Middle Steve disappears.
- [x] Another poof + sound triggers.
- [x] Only the bottom Steve is visible, on the ground.
- [x] Boss is even faster / harder-hitting.

- [ ] **Step 5: Kill**

Finish the boss off.

Verify:
- [x] 3 diamonds drop.
- [x] ~50 XP orbs drop.
- [x] Boss bar disappears.
- [x] No console errors.

- [ ] **Step 6: Save/load round-trip**

Spawn a new stacker, damage it to phase 2, then save and reload the world (Esc → Save and Quit → reopen).

Verify:
- [x] The boss is still 2 Steves tall after reload.
- [x] Its speed and damage are still phase-2 values (try to confirm by combat feel; or use `/data get entity @e[type=wildwest:steve_stacker,limit=1]` to read attribute base values).

- [ ] **Step 7: Natural-spawn smoke check (optional, time permitting)**

Edit the biome modifier weight from `1` to `200` temporarily (do not commit), launch a fresh world, fly around plains/savanna at night for a couple of minutes.

Verify:
- [x] Steve Stackers eventually spawn in the wild.
- [x] Their AI works (they aggro nearby players, melee them).

Revert the weight before committing anything else.

- [ ] **Step 8: Run the full test suite once more**

Run: `./gradlew :wildwest:test`
Expected: PASS — `SteveStackerPhaseLogicTest` and all pre-existing tests.

- [ ] **Step 9: Commit any test-pass-driven fixes**

If anything in steps 1–8 surfaced a regression, fix it task-by-task with TDD where possible. Commit each fix as its own commit; do not roll multiple fixes into one.

---

## Self-review summary

**Spec coverage check:**

| Spec section | Implementing task |
|--------------|-------------------|
| Entity skeleton (Monster subclass, attributes, goals) | Task 1 |
| Phase logic (`computeStackHeight`) | Task 2 |
| Synced `STACK_HEIGHT` + NBT persistence | Task 3 |
| Dynamic hitbox via `getDimensions(Pose)` | Task 4 |
| Phase transitions in `aiStep` (data set + refreshDimensions + attribute swap + particles + sound) | Task 5 |
| Boss bar (ServerBossEvent, startSeenByPlayer, stopSeenByPlayer) | Task 6 |
| Sounds + XP override | Task 7 |
| Loot table (3 diamonds) | Task 8 |
| Vendored Steve texture | Task 9 |
| Render state | Task 10 |
| Model (3 humanoid sub-roots, visibility shrink) | Task 11 |
| Renderer + ClientSetup wiring | Task 12 |
| Spawn egg + lang strings | Task 13 |
| Natural rare spawn (biome modifier) | Task 14 |
| Manual integration verification | Task 15 |
| `setPersistenceRequired` (boss does not despawn) | Task 1 |
| Knockback resistance attribute | Task 1 |

All scope items from the spec map to a task. Items called out as "Out of scope" in the spec (custom skins per Steve, AOE moves, per-phase attack-pattern changes, custom death cinematic, music, recipes, boss-bar animation effects) are intentionally absent from this plan.

**Type-consistency check:**
- `STACK_HEIGHT` is `EntityDataAccessor<Byte>` (Task 3) and is read as `byte` in Tasks 4, 5, 11, 12 — consistent.
- `computeStackHeight(float, float)` returns `byte` (Task 2) and is consumed as `byte` in Task 5 — consistent.
- `getStackHeight()` returns `byte` (Task 3); used in Task 4, Task 12 — consistent.
- `EntityDimensions.scalable(width, height)` (Task 4) — fallback to `EntityDimensions.fixed(...)` documented if missing.
- `ServerBossEvent` import path (Task 6) — guidance to grep the vendored MC client jar if the default import fails.
- Model layer location id `wildwest:steve_stacker` (Task 11) — matches the entity id (Task 1) and texture path (Task 9), so resource discovery is consistent.
- All file paths in the file-structure section reappear verbatim in the tasks that create or modify them.

**Placeholder scan:** No "TBD", no "implement later", no "similar to Task N" without showing the code. Each step contains the exact content a fresh engineer needs.
