# Han Solo / Princess Leia / Landspeeder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add two Light-faction named singleton characters (Han Solo with a quickdraw ambush, Princess Leia with a rally aura) and a craftable, drivable, hover-physics landspeeder vehicle to the existing `starwars` module, with Bedrock output via a new translator vehicle path.

**Architecture:** Han/Leia reuse the proven `SwMob` + `NamedCharacterSavedData` singleton machinery verbatim (per-character SavedData subclass, claiming `finalizeSpawn`, UUID-guarded clears). The landspeeder is a non-Mob `VehicleEntity` subclass with boat-style client-simulated driving and pure-math hover physics (`HoverPhysics`, unit-tested). The translator gains a vehicle emission path that *replaces* the walking-mob defaults, plus singleton-honesty entries for all six named characters.

**Tech Stack:** Java (NeoForge, `starwars` module), Kotlin (`translator` module), Python (art tools), JUnit.

## Global Constraints

Copied from the spec (`docs/superpowers/specs/2026-07-11-starwars-han-leia-landspeeder-design.md`); every task's requirements implicitly include these:

- **NO placeholder art.** Every texture ships as finished pixel art: 3+ tones per material region, shading, recognizable silhouette. bbmodel sources committed and editable.
- **Work on `main` directly. NO branch operations** (no checkout, no new branches). Stage ONLY the files your task names — the repo has pre-existing uncommitted user WIP in `thief/`, `securityguard/`, `wildwest/`, `craftee/`, root `gradle.properties`, and their `bedrock-out/` trees that must NEVER be staged, modified, or reverted.
- **Never assume engine API names.** This repo's MC version (26.1.2) renamed many classes (`Identifier` not ResourceLocation, `ValueInput`/`ValueOutput` NBT, `EntitySpawnReason`, `hurtServer(ServerLevel, DamageSource, float)`, `isLocalInstanceAuthoritative()` at `Entity.java:3508`). When a step says "verify against decompiled sources," locate the decompiled vanilla source (`find ~/.gradle -name "AbstractBoat.java" 2>/dev/null | head`, same tree earlier tasks used) and lift exact signatures from it or from named sibling files in this repo.
- **Bedrock honesty:** nothing silently dropped; every untranslatable behavior gets an `UNTRANSLATABLE.md` entry via the `Untranslatable` recorder.
- `Entity` implements `DebugValueSource` whose nested `Registration` interface SHADOWS the imported `com.tweeks.starwars.Registration` class inside entity subclasses — fully-qualify `com.tweeks.starwars.Registration` in entity class bodies.
- Commits: `feat(starwars): ...` / `feat(translator): ...` / `test(...)` style, ending with `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`.
- Tests: `./gradlew :starwars:test` currently has 48 passing tests; `./gradlew :translator:test` has 158+. Never leave either red.
- Datagen must stay byte-deterministic: running it twice produces zero git diff on the second run.

---

### Task 1: QuickdrawState (pure logic)

**Files:**
- Create: `starwars/src/main/java/com/tweeks/starwars/entity/ai/QuickdrawState.java`
- Test: `starwars/src/test/java/com/tweeks/starwars/entity/ai/QuickdrawStateTest.java`

**Interfaces:**
- Consumes: nothing (pure Java, no engine imports).
- Produces: `QuickdrawState` with `QUICKDRAW_WINDUP_TICKS = 8`, `boolean canAmbush(UUID)`, `void startWindup()`, `boolean tickWindup()` (true exactly on the tick the windup expires), `boolean isWindingUp()`, `void markAmbushed(UUID)`, `void cancel()`. Task 3's `HanQuickdrawGoal` calls all of these.

- [ ] **Step 1: Write the failing test**

```java
package com.tweeks.starwars.entity.ai;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class QuickdrawStateTest {

    private static final UUID TARGET_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID TARGET_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    public void freshTargetCanBeAmbushed() {
        QuickdrawState state = new QuickdrawState();
        assertTrue(state.canAmbush(TARGET_A));
    }

    @Test
    public void nullTargetCannotBeAmbushed() {
        QuickdrawState state = new QuickdrawState();
        assertFalse(state.canAmbush(null));
    }

    @Test
    public void ambushedTargetCannotBeAmbushedAgain() {
        QuickdrawState state = new QuickdrawState();
        state.markAmbushed(TARGET_A);
        assertFalse(state.canAmbush(TARGET_A));
    }

    @Test
    public void switchingTargetsReArmsQuickdraw() {
        QuickdrawState state = new QuickdrawState();
        state.markAmbushed(TARGET_A);
        assertTrue(state.canAmbush(TARGET_B));
        // Single-field memory: switching back re-arms against A (accepted in spec §3.3).
        state.markAmbushed(TARGET_B);
        assertTrue(state.canAmbush(TARGET_A));
    }

    @Test
    public void windupFiresExactlyOnEighthTick() {
        QuickdrawState state = new QuickdrawState();
        state.startWindup();
        assertTrue(state.isWindingUp());
        for (int i = 0; i < QuickdrawState.QUICKDRAW_WINDUP_TICKS - 1; i++) {
            assertFalse(state.tickWindup(), "tick " + i + " must not fire");
        }
        assertTrue(state.tickWindup(), "8th tick must fire");
        assertFalse(state.isWindingUp());
        assertFalse(state.tickWindup(), "no fire after expiry");
    }

    @Test
    public void cancelStopsWindupWithoutMarking() {
        QuickdrawState state = new QuickdrawState();
        state.startWindup();
        state.cancel();
        assertFalse(state.isWindingUp());
        assertFalse(state.tickWindup());
        assertTrue(state.canAmbush(TARGET_A), "cancel must not consume the ambush");
    }

    @Test
    public void markAmbushedClearsWindup() {
        QuickdrawState state = new QuickdrawState();
        state.startWindup();
        state.markAmbushed(TARGET_A);
        assertFalse(state.isWindingUp());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :starwars:test --tests "com.tweeks.starwars.entity.ai.QuickdrawStateTest"`
Expected: FAIL — compilation error, `QuickdrawState` does not exist.

- [ ] **Step 3: Write the implementation**

```java
package com.tweeks.starwars.entity.ai;

import java.util.UUID;

/**
 * Pure state machine for Han Solo's "Shoots First" quickdraw: the first shot
 * against each newly acquired target fires after a short windup with bonus
 * damage; the target is then remembered so subsequent engagement falls
 * through to the normal {@link BlasterAttackGoal} pacing.
 *
 * <p>Single-field memory (last ambushed target only): switching to a
 * different target re-arms the quickdraw, including switching back to a
 * previously ambushed one. Accepted in the spec — keeps state trivially
 * small and unit-testable.
 *
 * <p>No engine imports — lives outside MC classes for unit testing, same
 * pattern as {@link com.tweeks.starwars.faction.Alignment}.
 */
public final class QuickdrawState {

    public static final int QUICKDRAW_WINDUP_TICKS = 8;

    private UUID lastAmbushedTargetId = null;
    private int windupRemaining = 0;

    /** True when a quickdraw may begin against this target. */
    public boolean canAmbush(UUID targetId) {
        return targetId != null && !targetId.equals(this.lastAmbushedTargetId);
    }

    public void startWindup() {
        this.windupRemaining = QUICKDRAW_WINDUP_TICKS;
    }

    /** Advance one tick; returns true exactly on the tick the windup expires. */
    public boolean tickWindup() {
        if (this.windupRemaining <= 0) return false;
        this.windupRemaining--;
        return this.windupRemaining == 0;
    }

    public boolean isWindingUp() {
        return this.windupRemaining > 0;
    }

    /** Record a completed ambush; the same target can't be ambushed again. */
    public void markAmbushed(UUID targetId) {
        this.lastAmbushedTargetId = targetId;
        this.windupRemaining = 0;
    }

    /** Abort the windup (target died/lost) without consuming the ambush. */
    public void cancel() {
        this.windupRemaining = 0;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :starwars:test --tests "com.tweeks.starwars.entity.ai.QuickdrawStateTest"`
Expected: PASS, 7 tests.

- [ ] **Step 5: Commit**

```bash
git add starwars/src/main/java/com/tweeks/starwars/entity/ai/QuickdrawState.java starwars/src/test/java/com/tweeks/starwars/entity/ai/QuickdrawStateTest.java
git commit -m "feat(starwars): QuickdrawState pure logic for Han's ambush shot

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: Han Solo entity, SavedData, and registrations

**Files:**
- Create: `starwars/src/main/java/com/tweeks/starwars/entity/HanSavedData.java`
- Create: `starwars/src/main/java/com/tweeks/starwars/entity/HanSoloEntity.java`
- Modify: `starwars/src/main/java/com/tweeks/starwars/ModEntities.java` (add HAN_SOLO)
- Modify: `starwars/src/main/java/com/tweeks/starwars/StarWarsMod.java` (attributes; NO spawn placement — named characters have none, mirroring Luke)
- Modify: `starwars/src/main/java/com/tweeks/starwars/Registration.java` (spawn egg + creative tab)
- Modify: `starwars/src/main/java/com/tweeks/starwars/data/ModLanguageProvider.java`
- Modify: `starwars/src/main/java/com/tweeks/starwars/data/ModEntityLootProvider.java`

**Interfaces:**
- Consumes: `SwMob`, `NamedCharacterSavedData.buildCodec(Supplier<T>)`, `LukeSavedData`/`LukeSkywalkerEntity` as the verbatim pattern donors.
- Produces: `ModEntities.HAN_SOLO` (`EntityType<HanSoloEntity>`), `HanSavedData.get(MinecraftServer)`, `Registration.HAN_SOLO_SPAWN_EGG`. Task 3 adds the goal; Task 6 adds the spawner roster line.

- [ ] **Step 1: Write `HanSavedData`**

Mirror `LukeSavedData.java` exactly, renaming Luke→Han. The three literals that change: class name, javadoc, `FILE_ID`.

```java
package com.tweeks.starwars.entity;

import com.mojang.serialization.Codec;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-server singleton record for Han Solo. Anchored to the overworld's
 * data storage so reads/writes from any dimension consult the same data.
 * Same five-part shape as {@link LukeSavedData}.
 */
public final class HanSavedData extends NamedCharacterSavedData {

    private static final String FILE_ID = "starwars_han";

    public static final Codec<HanSavedData> CODEC = buildCodec(HanSavedData::new);

    public static final SavedDataType<HanSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, FILE_ID),
        HanSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    public HanSavedData() {}

    public static HanSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
```

- [ ] **Step 2: Write `HanSoloEntity`**

Mirror `LukeSkywalkerEntity.java`'s singleton lifecycle **verbatim** (finalizeSpawn claim with UUID-guarded duplicate discard; UUID-guarded clears in both `die()` and `remove()` gated on `KILLED`/`DISCARDED`), swapping `LukeSavedData`→`HanSavedData`. Differences from Luke: blaster user, different attribute values. Before writing `getWeaponStack()`, open `StormtrooperEntity.java` and copy its `getWeaponStack()` body's exact idiom for producing a blaster-pistol stack (fully-qualified `com.tweeks.starwars.Registration` per Global Constraints).

```java
package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.ai.HanQuickdrawGoal;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Named hero: server-wide singleton (see {@link HanSavedData}). Fights with
 * the blaster pistol; opens each engagement with {@link HanQuickdrawGoal}'s
 * "Shoots First" ambush. No natural spawn placement — only the
 * NamedCharacterSpawner roster and spawn eggs / {@code /summon} bring him
 * into a world.
 */
public class HanSoloEntity extends SwMob {

    public static final double MAX_HEALTH = 80.0;
    public static final double ATTACK_DAMAGE = 8.0;
    public static final double MOVEMENT_SPEED = 0.32;

    public HanSoloEntity(EntityType<? extends HanSoloEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public SwFaction getFaction() { return SwFaction.LIGHT; }

    @Override
    public boolean usesBlaster() { return true; }

    @Override
    public boolean usesRifleBlaster() { return false; }

    @Override
    protected ItemStack getWeaponStack() {
        // Copy the exact idiom StormtrooperEntity.getWeaponStack() uses for
        // a blaster-pistol stack (fully-qualified Registration).
        return com.tweeks.starwars.Registration.BLASTER_PISTOL.get().getDefaultInstance();
    }

    // registerGoals: Task 3 adds HanQuickdrawGoal at priority 1. In THIS
    // task, do not override registerGoals at all — SwMob's goal set suffices.

    /**
     * Singleton lifecycle — mirrors {@code LukeSkywalkerEntity} exactly.
     */
    @Override
    @org.jetbrains.annotations.Nullable
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.EntitySpawnReason reason,
            @org.jetbrains.annotations.Nullable net.minecraft.world.entity.SpawnGroupData spawnData) {
        var result = super.finalizeSpawn(level, difficulty, reason, spawnData);
        var server = level.getLevel().getServer();
        if (server != null) {
            HanSavedData saved = HanSavedData.get(server);
            if (saved.isAlive() && !this.getUUID().equals(saved.getCurrentId())) {
                this.discard();
                return result;
            }
            saved.setAlive(this.getUUID(), level.getLevel().dimension());
        }
        return result;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel sl) {
            var server = sl.getServer();
            if (server != null) {
                HanSavedData saved = HanSavedData.get(server);
                if (this.getUUID().equals(saved.getCurrentId())) {
                    saved.clear();
                }
            }
        }
    }

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        if (reason == net.minecraft.world.entity.Entity.RemovalReason.KILLED
                || reason == net.minecraft.world.entity.Entity.RemovalReason.DISCARDED) {
            if (this.level() instanceof ServerLevel sl) {
                var server = sl.getServer();
                if (server != null) {
                    HanSavedData saved = HanSavedData.get(server);
                    if (this.getUUID().equals(saved.getCurrentId())) {
                        saved.clear();
                    }
                }
            }
        }
        super.remove(reason);
    }
}
```

Note: the `HanQuickdrawGoal` import will not resolve until Task 3 — **remove the import and the javadoc `@link` for this task's commit** (plain-text mention instead), so the module compiles green. Task 3 restores them.

- [ ] **Step 3: Register in `ModEntities`**

Add after `ASTROMECH`, mirroring the `LUKE_SKYWALKER` registration shape exactly:

```java
    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.HanSoloEntity>> HAN_SOLO =
        ENTITY_TYPES.register("han_solo", () -> EntityType.Builder.<com.tweeks.starwars.entity.HanSoloEntity>of(
                com.tweeks.starwars.entity.HanSoloEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "han_solo"))));
```

- [ ] **Step 4: Wire attributes in `StarWarsMod.registerEntityAttributes`**

```java
        event.put(ModEntities.HAN_SOLO.get(),
            com.tweeks.starwars.entity.HanSoloEntity.createAttributes().build());
```

- [ ] **Step 5: Spawn egg + creative tab in `Registration`**

```java
    public static final DeferredItem<SpawnEggItem> HAN_SOLO_SPAWN_EGG = ITEMS.registerItem(
        "han_solo_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.HAN_SOLO.get()));
```

And in the `STARWARS_TAB` `displayItems` lambda, after `output.accept(BOBA_FETT_SPAWN_EGG.get());`:

```java
                    output.accept(HAN_SOLO_SPAWN_EGG.get());
```

- [ ] **Step 6: Lang entries in `ModLanguageProvider.addTranslations`**

```java
        add(com.tweeks.starwars.Registration.HAN_SOLO_SPAWN_EGG.get(), "Han Solo Spawn Egg");
        add(com.tweeks.starwars.ModEntities.HAN_SOLO.get(), "Han Solo");
```

- [ ] **Step 7: Loot table in `ModEntityLootProvider`**

Mirror the Luke table (smuggler's payout: gold): add inside `generate()` after the Obi-Wan block, and add `ModEntities.HAN_SOLO.get()` to the `Set.of(...)` in `getKnownEntityTypes()`.

```java
        // Han Solo: 1-2 gold_ingot @60% + 25% holocron — hero-tier haul
        // matching Luke. Same no-component-setting-loot-function caveat as
        // the other named heroes: no signature-weapon drop.
        this.add(ModEntities.HAN_SOLO.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(60)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(40)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.25f))
                    .add(LootItem.lootTableItem(Registration.HOLOCRON.get()))));
```

- [ ] **Step 8: Build, run datagen twice, verify determinism**

```bash
./gradlew :starwars:build
./gradlew :starwars:runData   # (use the exact datagen task earlier starwars tasks used — check `./gradlew :starwars:tasks | grep -i data` if runData is not it)
git status --porcelain starwars/   # note generated changes
./gradlew :starwars:runData
git status --porcelain starwars/   # second run must produce NO additional diff
```

Expected: build green (48 existing + 7 Task-1 tests pass), generated lang/loot JSON updated once, byte-stable on rerun.

- [ ] **Step 9: Commit**

```bash
git add starwars/src/main/java/com/tweeks/starwars/entity/HanSavedData.java starwars/src/main/java/com/tweeks/starwars/entity/HanSoloEntity.java starwars/src/main/java/com/tweeks/starwars/ModEntities.java starwars/src/main/java/com/tweeks/starwars/StarWarsMod.java starwars/src/main/java/com/tweeks/starwars/Registration.java starwars/src/main/java/com/tweeks/starwars/data/ModLanguageProvider.java starwars/src/main/java/com/tweeks/starwars/data/ModEntityLootProvider.java starwars/src/generated
git commit -m "feat(starwars): Han Solo entity — Light singleton, blaster pistol, gold loot

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

(If generated resources live somewhere other than `starwars/src/generated`, stage the actual datagen output path — check `git status` — but stage nothing outside the `starwars/` module.)

---

### Task 3: HanQuickdrawGoal

**Files:**
- Create: `starwars/src/main/java/com/tweeks/starwars/entity/ai/HanQuickdrawGoal.java`
- Modify: `starwars/src/main/java/com/tweeks/starwars/entity/HanSoloEntity.java` (registerGoals override + restore javadoc link)

**Interfaces:**
- Consumes: `QuickdrawState` (Task 1), `BlasterPistolItem.fireFromMob(LivingEntity, LivingEntity, float, int)`, `BlasterPistolItem.DAMAGE = 5.0F`, `SwMob.getTracerColor()`, `SwMob.usesBlaster()`.
- Produces: `HanQuickdrawGoal(SwMob)` registered at goal priority 1 (above SwMob's priority-2 `BlasterAttackGoal`).

- [ ] **Step 1: Write the goal**

Spec §3.3 requirements baked in: `Flag.LOOK` (cleanly suspends the priority-2 `BlasterAttackGoal`, whose `start()` resets its cooldown to 30 ticks — no double-tap on handoff), 8-tick windup, `2 * BlasterPistolItem.DAMAGE`, one ambush per newly acquired target.

```java
package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.SwMob;
import com.tweeks.starwars.item.BlasterPistolItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Han's "Shoots First" ambush: the first shot against each newly acquired
 * target fires after a short windup ({@link QuickdrawState#QUICKDRAW_WINDUP_TICKS})
 * at double pistol damage, then this goal yields to the normal
 * {@link BlasterAttackGoal} pacing. Registered at priority 1 with
 * {@code Flag.LOOK} — the same flag set as BlasterAttackGoal — so the
 * blaster goal is suspended during the windup and cannot fire mid-ambush.
 */
public class HanQuickdrawGoal extends Goal {

    private final SwMob mob;
    private final QuickdrawState state = new QuickdrawState();

    public HanQuickdrawGoal(SwMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return mob.usesBlaster()
            && target != null
            && target.isAlive()
            && mob.getSensing().hasLineOfSight(target)
            && state.canAmbush(target.getUUID());
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        return state.isWindingUp()
            && target != null
            && target.isAlive()
            && mob.getSensing().hasLineOfSight(target);
    }

    @Override
    public void start() {
        state.startWindup();
    }

    @Override
    public void stop() {
        state.cancel();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        // Default goal ticking is every-other-tick; the 8-tick windup needs
        // per-tick accuracy.
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        mob.getLookControl().setLookAt(target, 30, 30);
        if (state.tickWindup()) {
            BlasterPistolItem.fireFromMob(mob, target,
                2 * BlasterPistolItem.DAMAGE, mob.getTracerColor());
            state.markAmbushed(target.getUUID());
        }
    }
}
```

Verify `requiresUpdateEveryTick` is the correct override name by grepping an existing per-tick goal (`grep -rn "requiresUpdateEveryTick" */src/main/java` — wildwest/securitycore goals use it) or the decompiled `Goal.java`.

- [ ] **Step 2: Wire into `HanSoloEntity`**

Add the `registerGoals` override (and restore the `HanQuickdrawGoal` import + javadoc link removed in Task 2):

```java
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new HanQuickdrawGoal(this));
    }
```

- [ ] **Step 3: Build + full module tests**

Run: `./gradlew :starwars:build`
Expected: green; no test count change (goal is engine-coupled; its decision logic is the Task-1-tested `QuickdrawState`).

- [ ] **Step 4: Commit**

```bash
git add starwars/src/main/java/com/tweeks/starwars/entity/ai/HanQuickdrawGoal.java starwars/src/main/java/com/tweeks/starwars/entity/HanSoloEntity.java
git commit -m "feat(starwars): HanQuickdrawGoal — Shoots First ambush at priority 1

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: RallyMath (pure logic)

**Files:**
- Create: `starwars/src/main/java/com/tweeks/starwars/entity/ai/RallyMath.java`
- Test: `starwars/src/test/java/com/tweeks/starwars/entity/ai/RallyMathTest.java`

**Interfaces:**
- Consumes: `SwFaction` (existing enum: `EMPIRE`, `LIGHT`, `NEUTRAL`).
- Produces: constants `RALLY_INTERVAL_TICKS = 240`, `RALLY_DURATION_TICKS = 160`, `RALLY_RADIUS = 12.0`; predicates `isEligibleFaction(SwFaction)`, `isEligibleScore(int)`, `isWithinRadius(double distSq)`, `isReady(long now, long lastPulseGameTime)`. Task 6's `LeiaRallyGoal` consumes all of these.

- [ ] **Step 1: Write the failing test**

```java
package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.faction.SwFaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RallyMathTest {

    @Test
    public void onlyLightFactionIsEligible() {
        assertTrue(RallyMath.isEligibleFaction(SwFaction.LIGHT));
        assertFalse(RallyMath.isEligibleFaction(SwFaction.EMPIRE));
        assertFalse(RallyMath.isEligibleFaction(SwFaction.NEUTRAL));
    }

    @Test
    public void playerScoreMustBeStrictlyPositive() {
        assertTrue(RallyMath.isEligibleScore(1));
        assertFalse(RallyMath.isEligibleScore(0));
        assertFalse(RallyMath.isEligibleScore(-1));
    }

    @Test
    public void radiusBoundaryIsInclusive() {
        double r = RallyMath.RALLY_RADIUS;
        assertTrue(RallyMath.isWithinRadius(r * r));
        assertTrue(RallyMath.isWithinRadius(0.0));
        assertFalse(RallyMath.isWithinRadius(r * r + 0.001));
    }

    @Test
    public void cooldownGatesOnGameTimeDelta() {
        assertTrue(RallyMath.isReady(240, 0));
        assertTrue(RallyMath.isReady(1000, 760));
        assertFalse(RallyMath.isReady(239, 0));
        // First-ever pulse: lastPulseGameTime 0 at world start still gates
        // until tick 240 — acceptable (worst case one idle interval).
        assertTrue(RallyMath.isReady(0, -RallyMath.RALLY_INTERVAL_TICKS));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :starwars:test --tests "com.tweeks.starwars.entity.ai.RallyMathTest"`
Expected: FAIL — `RallyMath` does not exist.

- [ ] **Step 3: Write the implementation**

```java
package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.faction.SwFaction;

/**
 * Pure eligibility/cadence math for Leia's "Rebel Rally" aura. Lives outside
 * MC classes for unit testing (same pattern as
 * {@link com.tweeks.starwars.faction.Alignment}).
 *
 * <p>Effect choice (Resistance, not Strength) is deliberate: blaster damage
 * is a flat constant passed straight to {@code hurtServer(...)} and never
 * reads the ATTACK_DAMAGE attribute Strength modifies — Strength would be a
 * silent no-op for every blaster-wielding ally. Resistance applies on the
 * victim side of the damage pipeline and benefits everyone. (Spec §4.3.)
 */
public final class RallyMath {
    private RallyMath() {}

    /** One pulse at most every 12 seconds. */
    public static final int RALLY_INTERVAL_TICKS = 240;
    /** Resistance I + Regeneration I for 8 seconds. */
    public static final int RALLY_DURATION_TICKS = 160;
    public static final double RALLY_RADIUS = 12.0;

    public static boolean isEligibleFaction(SwFaction faction) {
        return faction == SwFaction.LIGHT;
    }

    /** Players rally only when strictly Light-aligned (score > 0). */
    public static boolean isEligibleScore(int alignmentScore) {
        return alignmentScore > 0;
    }

    /** Squared-distance radius check, inclusive at the boundary. */
    public static boolean isWithinRadius(double distSq) {
        return distSq <= RALLY_RADIUS * RALLY_RADIUS;
    }

    /** Cadence gate on game time (survives goal stop/start; no per-tick counter). */
    public static boolean isReady(long now, long lastPulseGameTime) {
        return now - lastPulseGameTime >= RALLY_INTERVAL_TICKS;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :starwars:test --tests "com.tweeks.starwars.entity.ai.RallyMathTest"`
Expected: PASS, 4 tests.

- [ ] **Step 5: Commit**

```bash
git add starwars/src/main/java/com/tweeks/starwars/entity/ai/RallyMath.java starwars/src/test/java/com/tweeks/starwars/entity/ai/RallyMathTest.java
git commit -m "feat(starwars): RallyMath pure eligibility/cadence logic for Leia's rally

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 5: Princess Leia entity, SavedData, and registrations

**Files:**
- Create: `starwars/src/main/java/com/tweeks/starwars/entity/LeiaSavedData.java`
- Create: `starwars/src/main/java/com/tweeks/starwars/entity/PrincessLeiaEntity.java`
- Modify: `starwars/src/main/java/com/tweeks/starwars/ModEntities.java`
- Modify: `starwars/src/main/java/com/tweeks/starwars/StarWarsMod.java`
- Modify: `starwars/src/main/java/com/tweeks/starwars/Registration.java`
- Modify: `starwars/src/main/java/com/tweeks/starwars/data/ModLanguageProvider.java`
- Modify: `starwars/src/main/java/com/tweeks/starwars/data/ModEntityLootProvider.java`

**Interfaces:**
- Consumes: same donors as Task 2 (`LukeSavedData` / `HanSavedData` pattern; `HanSoloEntity` from Task 2 is the closest sibling — blaster-wielding Light singleton).
- Produces: `ModEntities.PRINCESS_LEIA` (`EntityType<PrincessLeiaEntity>`), `LeiaSavedData.get(MinecraftServer)`, `Registration.PRINCESS_LEIA_SPAWN_EGG`. Task 6 adds the rally goal + spawner lines.

This task is Task 2 with Leia's values. Repeat the full Task-2 recipe with these substitutions (all other code identical in shape):

| Item | Value |
|---|---|
| SavedData class / `FILE_ID` | `LeiaSavedData` / `"starwars_leia"` |
| Entity class | `PrincessLeiaEntity` |
| Attributes | `MAX_HEALTH = 70.0`, `ATTACK_DAMAGE = 7.0`, `MOVEMENT_SPEED = 0.32`, `FOLLOW_RANGE 32.0` |
| `usesBlaster()` / `usesRifleBlaster()` | `true` / `false` |
| `getWeaponStack()` | blaster pistol, same idiom as Han |
| Registry id / size | `"princess_leia"`, `MobCategory.CREATURE`, `.sized(0.6f, 1.9f)`, `.clientTrackingRange(10)` |
| Egg registration | `PRINCESS_LEIA_SPAWN_EGG`, id `"princess_leia_spawn_egg"`, tab line after Han's |
| Lang | `"Princess Leia Spawn Egg"`, `"Princess Leia"` |
| `registerGoals` | none in this task (Task 6 adds `LeiaRallyGoal`) |

- [ ] **Step 1: Write `LeiaSavedData`** (Task 2 Step 1 with the table above)
- [ ] **Step 2: Write `PrincessLeiaEntity`** (Task 2 Step 2 with the table above — full singleton lifecycle verbatim, `LeiaSavedData` in all three lifecycle methods)
- [ ] **Step 3: Register in `ModEntities`** (Task 2 Step 3 shape, `"princess_leia"`, `.sized(0.6f, 1.9f)`)
- [ ] **Step 4: Attributes in `StarWarsMod`** (Task 2 Step 4 shape)
- [ ] **Step 5: Egg + tab in `Registration`** (Task 2 Step 5 shape)
- [ ] **Step 6: Lang entries** (Task 2 Step 6 shape)
- [ ] **Step 7: Loot table** — mirror Obi-Wan's (diplomat's haul: emeralds), plus `getKnownEntityTypes` addition:

```java
        // Princess Leia: 1-2 emerald @60% + 25% holocron — hero-tier haul
        // matching Obi-Wan. Same no-component-setting-loot-function caveat.
        this.add(ModEntities.PRINCESS_LEIA.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.EMERALD).setWeight(60)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(40)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.25f))
                    .add(LootItem.lootTableItem(Registration.HOLOCRON.get()))));
```

- [ ] **Step 8: Build + datagen twice + determinism check** (Task 2 Step 8 commands)
- [ ] **Step 9: Commit**

```bash
git add starwars/src/main/java/com/tweeks/starwars/entity/LeiaSavedData.java starwars/src/main/java/com/tweeks/starwars/entity/PrincessLeiaEntity.java starwars/src/main/java/com/tweeks/starwars/ModEntities.java starwars/src/main/java/com/tweeks/starwars/StarWarsMod.java starwars/src/main/java/com/tweeks/starwars/Registration.java starwars/src/main/java/com/tweeks/starwars/data/ModLanguageProvider.java starwars/src/main/java/com/tweeks/starwars/data/ModEntityLootProvider.java starwars/src/generated
git commit -m "feat(starwars): Princess Leia entity — Light singleton, blaster pistol, emerald loot

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 6: LeiaRallyGoal + spawner roster for both characters

**Files:**
- Create: `starwars/src/main/java/com/tweeks/starwars/entity/ai/LeiaRallyGoal.java`
- Modify: `starwars/src/main/java/com/tweeks/starwars/entity/PrincessLeiaEntity.java` (registerGoals)
- Modify: `starwars/src/main/java/com/tweeks/starwars/spawning/NamedCharacterSpawner.java` (two roster lines)

**Interfaces:**
- Consumes: `RallyMath` (Task 4), `AlignmentEvents.getScore(Player)`, `SwCombatant.getFaction()`, `HanSavedData`/`LeiaSavedData` (Tasks 2/5), `NamedCharacterSpawner.tryRollCharacter(...)` (existing private method — the roster lines call it the same way Luke's does).
- Produces: `LeiaRallyGoal(SwMob)`; Han and Leia spawning via the 1200-tick/15% roller with `JEDI_BIOMES` + `JEDI_STRUCTURES`, no escort.

- [ ] **Step 1: Write the rally goal**

Effect names: verify `MobEffects.RESISTANCE` and `MobEffects.REGENERATION` are the field names in this version (`grep -n "RESISTANCE\|REGENERATION" <decompiled>/MobEffects.java`); older versions call it `DAMAGE_RESISTANCE`. Also verify `addEffect(new MobEffectInstance(holder, duration, amplifier))` — grep a sibling module for `MobEffectInstance` usage (wildwest uses effects) and copy the constructor shape.

```java
package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.SwMob;
import com.tweeks.starwars.faction.AlignmentEvents;
import com.tweeks.starwars.faction.SwCombatant;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;

/**
 * Leia's "Rebel Rally": when combat is happening nearby and the 12s cadence
 * has elapsed, pulse Resistance I + Regeneration I (8s) to every eligible
 * ally within 12 blocks — Light-faction mobs (Leia included) and strictly
 * Light-aligned players (score > 0). Eligibility/cadence math lives in
 * {@link RallyMath}. No goal flags: the rally is an aura and must not
 * suppress Leia's own blaster goal.
 */
public class LeiaRallyGoal extends Goal {

    private final SwMob mob;
    private long lastPulseGameTime = -RallyMath.RALLY_INTERVAL_TICKS;

    public LeiaRallyGoal(SwMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!(mob.level() instanceof ServerLevel sl)) return false;
        if (!RallyMath.isReady(sl.getGameTime(), lastPulseGameTime)) return false;
        return combatNearby(sl);
    }

    @Override
    public boolean canContinueToUse() {
        return false; // one-shot pulse; start() does all the work
    }

    @Override
    public void start() {
        ServerLevel sl = (ServerLevel) mob.level();
        for (LivingEntity ally : findEligibleAllies(sl)) {
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                RallyMath.RALLY_DURATION_TICKS, 0));
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                RallyMath.RALLY_DURATION_TICKS, 0));
        }
        // Particle ring at the rally radius edge, 24 points, Y + 1.
        for (int i = 0; i < 24; i++) {
            double angle = (i / 24.0) * Math.PI * 2.0;
            sl.sendParticles(ParticleTypes.END_ROD,
                mob.getX() + Math.cos(angle) * RallyMath.RALLY_RADIUS,
                mob.getY() + 1.0,
                mob.getZ() + Math.sin(angle) * RallyMath.RALLY_RADIUS,
                1, 0.0, 0.0, 0.0, 0.0);
        }
        sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
            SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.8f, 1.4f);
        this.lastPulseGameTime = sl.getGameTime();
    }

    /** Combat nearby = Leia has a target, or an eligible ally mob does. */
    private boolean combatNearby(ServerLevel sl) {
        if (mob.getTarget() != null) return true;
        for (LivingEntity ally : findEligibleAllies(sl)) {
            if (ally instanceof Mob m && m.getTarget() != null) return true;
        }
        return false;
    }

    private List<LivingEntity> findEligibleAllies(ServerLevel sl) {
        return sl.getEntitiesOfClass(LivingEntity.class,
            mob.getBoundingBox().inflate(RallyMath.RALLY_RADIUS),
            e -> RallyMath.isWithinRadius(e.distanceToSqr(mob)) && isEligible(e));
    }

    private static boolean isEligible(LivingEntity e) {
        if (e instanceof SwCombatant c) return RallyMath.isEligibleFaction(c.getFaction());
        if (e instanceof Player p) return RallyMath.isEligibleScore(AlignmentEvents.getScore(p));
        return false;
    }
}
```

Note the AABB `inflate` is a box; the `isWithinRadius(distanceToSqr)` predicate trims it to the true sphere with the inclusive boundary the test pins.

- [ ] **Step 2: Wire into `PrincessLeiaEntity`**

```java
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new LeiaRallyGoal(this));
    }
```

- [ ] **Step 3: Add both roster lines to `NamedCharacterSpawner.onServerLevelTick`**

After the Boba Fett `tryRollCharacter` call (heroes use `JEDI_BIOMES` + `JEDI_STRUCTURES`, no escort — same as Luke/Obi-Wan), plus the two imports:

```java
        tryRollCharacter(sl, com.tweeks.starwars.entity.HanSavedData.get(sl.getServer()),
            ModEntities.HAN_SOLO.get(), JEDI_BIOMES, JEDI_STRUCTURES, false);
        tryRollCharacter(sl, com.tweeks.starwars.entity.LeiaSavedData.get(sl.getServer()),
            ModEntities.PRINCESS_LEIA.get(), JEDI_BIOMES, JEDI_STRUCTURES, false);
```

(Match the file's existing import style — the other four SavedData classes are imported at the top; do the same rather than fully-qualifying, and update the class javadoc's character list.)

- [ ] **Step 4: Build + tests**

Run: `./gradlew :starwars:build`
Expected: green.

- [ ] **Step 5: Commit**

```bash
git add starwars/src/main/java/com/tweeks/starwars/entity/ai/LeiaRallyGoal.java starwars/src/main/java/com/tweeks/starwars/entity/PrincessLeiaEntity.java starwars/src/main/java/com/tweeks/starwars/spawning/NamedCharacterSpawner.java
git commit -m "feat(starwars): LeiaRallyGoal aura + Han/Leia join the named-character spawner

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 7: Han/Leia art — bbmodels, entity textures, egg icons (ART GATE)

**Files:**
- Modify: `starwars/tools/gen_bbmodels.py` (two accessory tables + two `MOBS` entries)
- Modify: `starwars/tools/gen_textures.py` (two paint functions + registry entry, matching the file's existing dispatch pattern)
- Modify: `starwars/tools/gen_spawn_eggs.py` (two `EGGS` entries)
- Create (generated): `starwars/tools/han_solo.bbmodel`, `starwars/tools/princess_leia.bbmodel`
- Create (generated): `starwars/src/main/resources/assets/starwars/textures/entity/han_solo.png`, `.../princess_leia.png`
- Create (generated): `starwars/src/main/resources/assets/starwars/textures/item/han_solo_spawn_egg.png`, `.../princess_leia_spawn_egg.png`

**Interfaces:**
- Consumes: `HUMANOID_CUBES`, `HEAD_BONE`/`BODY_BONE`, `write_bbmodel`, `paint_humanoid_base`, `write_png` — all existing in the tools.
- Produces: geometry tables that Task 8's Java model classes MUST mirror cube-for-cube (same coords, same UVs, same inflate).

- [ ] **Step 1: Add geometry to `gen_bbmodels.py`**

After `BOBA_FETT_ACCESSORIES`:

```python
# Han Solo: black vest as an inflated layer over the torso's upper half.
# The vest is the silhouette feature (spec §3.5).
HAN_SOLO_ACCESSORIES = [
    ('vest', BODY_BONE, (-4.0, 0.0, -2.0, 8, 8, 4), (32, 32), 0.25),
]

# Princess Leia: side hair buns modeled as geometry (mandatory silhouette
# feature, spec §4.4) + the Jedi-style robe skirt for the senatorial gown.
PRINCESS_LEIA_ACCESSORIES = [
    ('bun_right',  HEAD_BONE, (-5.5, -5.0, -1.5, 2, 3, 3), (56, 0), 0.0),
    ('bun_left',   HEAD_BONE, ( 3.5, -5.0, -1.5, 2, 3, 3), (56, 6), 0.0),
    ('robe_skirt', BODY_BONE, (-4.5, 12.0, -2.5, 9, 7, 5), (32, 32), 0.0),
]
```

And in `MOBS`:

```python
    # Han Solo: humanoid + black vest layer over the white shirt.
    'han_solo': HUMANOID_CUBES + HAN_SOLO_ACCESSORIES,
    # Princess Leia: humanoid + side buns (geometry silhouette) + robe skirt.
    'princess_leia': HUMANOID_CUBES + PRINCESS_LEIA_ACCESSORIES,
```

Check the chosen UV offsets against the existing entries for collisions *within the same mob's texture* only (each mob has its own 64×64 sheet; Leia's `robe_skirt` at (32,32) is fine because it reuses the Jedi robe UV block, and her buns sit in the (56,0..) strip that the humanoid layout leaves free — confirm by reading the UV map comment at the top of `gen_textures.py` if one exists, else by inspecting `paint_*` coordinates).

- [ ] **Step 2: Add paint functions to `gen_textures.py`**

Read the file's existing structure first: `paint_humanoid_base(rgba, shirt)` paints the standard humanoid UV; each mob has a `paint_<name>` function and a bottom-of-file dispatch that writes `<name>.png`. Follow that pattern exactly. Paint requirements (3+ tones per region — the art gate checks this):

```python
# Han Solo palette
HAN_SHIRT      = (0xE8, 0xE0, 0xD0, 0xFF)   # off-white shirt
HAN_SHIRT_DIM  = (0xC9, 0xC0, 0xAE, 0xFF)   # shirt shadow
HAN_VEST       = (0x2B, 0x2B, 0x2B, 0xFF)   # black vest
HAN_VEST_HI    = (0x45, 0x45, 0x48, 0xFF)   # vest highlight
HAN_VEST_DK    = (0x17, 0x17, 0x19, 0xFF)   # vest deep shadow
HAN_TROUSER    = (0x2E, 0x3A, 0x52, 0xFF)   # navy trousers
HAN_TROUSER_DK = (0x1F, 0x28, 0x3A, 0xFF)
HAN_STRIPE     = (0xB0, 0x30, 0x30, 0xFF)   # Corellian bloodstripe (trouser seam)
HAN_SKIN       = (0xC8, 0x9E, 0x7A, 0xFF)
HAN_SKIN_DK    = (0xA8, 0x80, 0x60, 0xFF)
HAN_HAIR       = (0x4A, 0x35, 0x22, 0xFF)   # dark brown hair
HAN_BELT       = (0x5A, 0x40, 0x28, 0xFF)   # holster belt, right hip detail
HAN_BUCKLE     = (0x9A, 0x8A, 0x60, 0xFF)

def paint_han_solo(rgba):
    paint_humanoid_base(rgba, HAN_SHIRT)
    # Face: skin with hair top/sides, eyes, shading — mirror the structure of
    # paint_luke's head block, substituting the palette above.
    # Torso front (UV 20,20..27,31): shirt with HAN_SHIRT_DIM fold lines,
    # HAN_BELT row at the waist (y=30) with HAN_BUCKLE at center.
    # Right hip: 2px HAN_BELT holster drop with HAN_VEST_DK gun grip.
    # Legs: HAN_TROUSER with HAN_TROUSER_DK inner shadow and a 1px
    # (0xB0,0x30,0x30) bloodstripe down the outer seam.
    # Vest cube UV block (32,32..): HAN_VEST body, HAN_VEST_HI on upper
    # edges/lapels, HAN_VEST_DK under the arms; leave the front-center
    # column open (shirt shows through the open vest).
    #
    # BODY: replicate paint_luke's rect() calls region-for-region (same
    # humanoid UV rectangles), substituting this palette. Every constant
    # above must appear in the output — the art gate checks tone counts.

# Princess Leia palette
LEIA_ROBE     = (0xF2, 0xEE, 0xE6, 0xFF)    # white senatorial robe
LEIA_ROBE_DIM = (0xD8, 0xD2, 0xC4, 0xFF)    # robe fold shadow
LEIA_ROBE_DK  = (0xB8, 0xB0, 0xA0, 0xFF)    # deep fold
LEIA_BELT     = (0x8A, 0x86, 0x7A, 0xFF)    # silver-grey belt
LEIA_SKIN     = (0xD8, 0xB0, 0x8E, 0xFF)
LEIA_SKIN_DK  = (0xB6, 0x92, 0x74, 0xFF)
LEIA_HAIR     = (0x5A, 0x40, 0x30, 0xFF)    # brown hair
LEIA_HAIR_DK  = (0x42, 0x2E, 0x22, 0xFF)    # bun shadow / parting

def paint_princess_leia(rgba):
    paint_humanoid_base(rgba, LEIA_ROBE)
    # Head: LEIA_SKIN face with LEIA_HAIR crown/parting and LEIA_HAIR_DK
    # center part; eyes as in the sibling heads.
    # Bun cubes (UV 56,0 and 56,6): LEIA_HAIR with LEIA_HAIR_DK spiral hint
    # (1-2 darker pixels per face).
    # Torso/arms/legs: LEIA_ROBE with LEIA_ROBE_DIM vertical folds and
    # LEIA_ROBE_DK under-arm shadow; LEIA_BELT waist row.
    # Robe skirt UV block (32,32..): LEIA_ROBE with alternating
    # LEIA_ROBE_DIM/LEIA_ROBE_DK fold columns.
    #
    # BODY: replicate paint_luke's rect() calls region-for-region (same
    # humanoid UV rectangles) with this palette; buns use the UV blocks
    # chosen in Step 1. Every constant above must appear in the output.
```

The `...` bodies above are **direction, not literal code** — the actual `rect(...)` calls must target the exact UV rectangles this file's other paint functions use for each humanoid part (head 0,0..31,15; body 16,16..; arms/legs per `HUMANOID_CUBES` offsets; accessory blocks per Step 1's UV choices). Write them by copying the rectangle coordinates from `paint_luke` / `paint_jedi_knight` (same skeleton) and substituting palettes. Every named palette color must appear in the output — that is what makes the 3-tone art gate objective.

- [ ] **Step 3: Add egg colors to `gen_spawn_eggs.py`**

```python
    'han_solo_spawn_egg': ((0xE8, 0xE0, 0xD0), (0x2B, 0x2B, 0x2B)),
    'princess_leia_spawn_egg': ((0xF2, 0xEE, 0xE6), (0x5A, 0x40, 0x30)),
```

- [ ] **Step 4: Run all three tools**

Each tool's `__main__` block documents its output-dir convention (e.g. `gen_spawn_eggs.py <out_dir>`). Run them the way the previous milestone did — check `git log --oneline --follow starwars/tools/gen_textures.py` and the tool headers; typical invocations:

```bash
python3 starwars/tools/gen_bbmodels.py starwars/tools
python3 starwars/tools/gen_textures.py starwars/src/main/resources/assets/starwars/textures/entity
python3 starwars/tools/gen_spawn_eggs.py starwars/src/main/resources/assets/starwars/textures/item
```

Expected new files: `han_solo.bbmodel`, `princess_leia.bbmodel`, `han_solo.png`, `princess_leia.png`, both egg PNGs. **Existing sibling files must be byte-identical after the run** (`git status` shows only the new files) — the generators are deterministic; a diff in an existing file means you changed shared code and must fix it.

- [ ] **Step 5: ART GATE — visually verify**

Read both entity PNGs (as images) and both egg PNGs. Verify per the Global Constraints: 3+ tones per material region, actual shading (folds/shadows painted), silhouette features present (Han's vest block, Leia's buns), nothing flat-filled. If any region is a flat rectangle of one color, fix the paint function and re-run before committing.

- [ ] **Step 6: Commit**

```bash
git add starwars/tools/gen_bbmodels.py starwars/tools/gen_textures.py starwars/tools/gen_spawn_eggs.py starwars/tools/han_solo.bbmodel starwars/tools/princess_leia.bbmodel starwars/src/main/resources/assets/starwars/textures/entity/han_solo.png starwars/src/main/resources/assets/starwars/textures/entity/princess_leia.png starwars/src/main/resources/assets/starwars/textures/item/han_solo_spawn_egg.png starwars/src/main/resources/assets/starwars/textures/item/princess_leia_spawn_egg.png
git commit -m "feat(starwars): Han/Leia bbmodels, entity textures, and egg icons

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 8: Han/Leia client models + renderers

**Files:**
- Create: `starwars/src/main/java/com/tweeks/starwars/client/model/HanSoloModel.java`
- Create: `starwars/src/main/java/com/tweeks/starwars/client/model/PrincessLeiaModel.java`
- Create: `starwars/src/main/java/com/tweeks/starwars/client/HanSoloRenderer.java`
- Create: `starwars/src/main/java/com/tweeks/starwars/client/PrincessLeiaRenderer.java`
- Modify: `starwars/src/main/java/com/tweeks/starwars/client/ClientSetup.java`

**Interfaces:**
- Consumes: `HumanoidModel`/`HumanoidMobRenderer`/`HumanoidRenderState` (as `LukeModel`/`LukeRenderer` use them), Task 7's geometry tables (cube coords/UVs/inflates MUST match the bbmodels exactly), textures `textures/entity/han_solo.png` / `princess_leia.png`.
- Produces: renderers registered for `HAN_SOLO` and `PRINCESS_LEIA`.

- [ ] **Step 1: Write `HanSoloModel`**

`LukeModel` shape plus the vest cube. The accessory-cube technique (child part on `body` with `CubeDeformation` inflate) — copy the exact builder idiom from `JediKnightModel.createBodyLayer()`'s `robe_skirt` (read that file first; it is the in-repo donor for body-attached accessory cubes).

```java
package com.tweeks.starwars.client.model;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Han Solo: humanoid skeleton + black-vest layer cube over the torso's
 * upper half (matches han_solo.bbmodel: (-4,0,-2) 8x8x4 @ UV(32,32),
 * inflate 0.25).
 */
public class HanSoloModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "han_solo"),
        "main");

    public HanSoloModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition body = mesh.getRoot().getChild("body");
        body.addOrReplaceChild("vest",
            CubeListBuilder.create()
                .texOffs(32, 32)
                .addBox(-4.0f, 0.0f, -2.0f, 8.0f, 8.0f, 4.0f, new CubeDeformation(0.25f)),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
```

(If `JediKnightModel` attaches accessories differently — e.g. via `mesh.getRoot().addOrReplaceChild` with an offset `PartPose` — copy that idiom instead; the bbmodel/Java-model pair must visually agree, and the Jedi model is the proven pairing.)

- [ ] **Step 2: Write `PrincessLeiaModel`**

Same pattern: buns on the `head` part, robe skirt on `body` (copy `JediKnightModel`'s `robe_skirt` child verbatim — same coords `(-4.5, 12.0, -2.5) 9x7x5 @ UV(32,32)`):

```java
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("bun_right",
            CubeListBuilder.create().texOffs(56, 0)
                .addBox(-5.5f, -5.0f, -1.5f, 2.0f, 3.0f, 3.0f),
            PartPose.ZERO);
        head.addOrReplaceChild("bun_left",
            CubeListBuilder.create().texOffs(56, 6)
                .addBox(3.5f, -5.0f, -1.5f, 2.0f, 3.0f, 3.0f),
            PartPose.ZERO);
        PartDefinition body = mesh.getRoot().getChild("body");
        body.addOrReplaceChild("robe_skirt",
            CubeListBuilder.create().texOffs(32, 32)
                .addBox(-4.5f, 12.0f, -2.5f, 9.0f, 7.0f, 5.0f),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
```

- [ ] **Step 3: Write both renderers** — `LukeRenderer` verbatim with names/texture/model swapped (`HanSoloRenderer` → `textures/entity/han_solo.png`, `PrincessLeiaRenderer` → `textures/entity/princess_leia.png`).

- [ ] **Step 4: Register in `ClientSetup`**

```java
        event.registerEntityRenderer(ModEntities.HAN_SOLO.get(), HanSoloRenderer::new);
        event.registerEntityRenderer(ModEntities.PRINCESS_LEIA.get(), PrincessLeiaRenderer::new);
```
```java
        event.registerLayerDefinition(HanSoloModel.LAYER_LOCATION, HanSoloModel::createBodyLayer);
        event.registerLayerDefinition(PrincessLeiaModel.LAYER_LOCATION, PrincessLeiaModel::createBodyLayer);
```

- [ ] **Step 5: Build**

Run: `./gradlew :starwars:build`
Expected: green.

- [ ] **Step 6: Commit**

```bash
git add starwars/src/main/java/com/tweeks/starwars/client/model/HanSoloModel.java starwars/src/main/java/com/tweeks/starwars/client/model/PrincessLeiaModel.java starwars/src/main/java/com/tweeks/starwars/client/HanSoloRenderer.java starwars/src/main/java/com/tweeks/starwars/client/PrincessLeiaRenderer.java starwars/src/main/java/com/tweeks/starwars/client/ClientSetup.java
git commit -m "feat(starwars): Han/Leia client models and renderers

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 9: HoverPhysics (pure math)

**Files:**
- Create: `starwars/src/main/java/com/tweeks/starwars/entity/vehicle/HoverPhysics.java`
- Test: `starwars/src/test/java/com/tweeks/starwars/entity/vehicle/HoverPhysicsTest.java`

**Interfaces:**
- Consumes: nothing (pure math, NO engine imports — that is the point).
- Produces: all landspeeder tuning constants + `verticalAccel(double distToGround, double verticalVel)` (NaN dist = no ground in scan), `nextForwardSpeed(double current, int forwardInput)`, `nextYaw(float yaw, int turnInput)`. Task 10 consumes every one of these.

- [ ] **Step 1: Write the failing test**

```java
package com.tweeks.starwars.entity.vehicle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HoverPhysicsTest {

    /** Simulate the 1-D vertical system: dist' = dist + vel, vel' = vel + accel. */
    private static double[] simulate(double dist, double vel, int ticks) {
        for (int i = 0; i < ticks; i++) {
            vel += HoverPhysics.verticalAccel(dist, vel);
            dist += vel;
        }
        return new double[] {dist, vel};
    }

    @Test
    public void settlesToHoverHeightFromAbove() {
        double[] end = simulate(3.0, 0.0, 200);
        assertEquals(HoverPhysics.HOVER_HEIGHT, end[0], 0.05);
        assertEquals(0.0, end[1], 0.02);
    }

    @Test
    public void settlesToHoverHeightFromBelow() {
        double[] end = simulate(0.1, 0.0, 200);
        assertEquals(HoverPhysics.HOVER_HEIGHT, end[0], 0.05);
    }

    @Test
    public void terminalFallIntoScanRangeNeverClipsGround() {
        // Worst case: entering the 3-block scan at terminal velocity.
        double dist = 3.0, vel = HoverPhysics.TERMINAL_FALL, min = dist;
        for (int i = 0; i < 200; i++) {
            vel += HoverPhysics.verticalAccel(dist, vel);
            dist += vel;
            min = Math.min(min, dist);
        }
        assertTrue(min > 0.0, "hull must never reach the ground (min=" + min + ")");
        assertEquals(HoverPhysics.HOVER_HEIGHT, dist, 0.05);
    }

    @Test
    public void verticalAccelIsClamped() {
        assertTrue(Math.abs(HoverPhysics.verticalAccel(0.0, 0.0)) <= HoverPhysics.MAX_VERTICAL_ACCEL + 1e-9);
        assertTrue(Math.abs(HoverPhysics.verticalAccel(10.0, 0.0)) <= HoverPhysics.MAX_VERTICAL_ACCEL + 1e-9);
    }

    @Test
    public void noGroundMeansGravityTowardTerminal() {
        double vel = 0.0;
        for (int i = 0; i < 100; i++) {
            vel += HoverPhysics.verticalAccel(Double.NaN, vel);
        }
        assertEquals(HoverPhysics.TERMINAL_FALL, vel, 0.01);
        // Never accelerates below terminal.
        assertTrue(vel >= HoverPhysics.TERMINAL_FALL - 1e-9);
    }

    @Test
    public void forwardSpeedCapsAtMaxSpeed() {
        double s = 0.0;
        for (int i = 0; i < 100; i++) s = HoverPhysics.nextForwardSpeed(s, 1);
        assertEquals(HoverPhysics.MAX_SPEED, s, 1e-9);
    }

    @Test
    public void reverseSpeedCapsAtReverseMax() {
        double s = 0.0;
        for (int i = 0; i < 100; i++) s = HoverPhysics.nextForwardSpeed(s, -1);
        assertEquals(-HoverPhysics.MAX_REVERSE_SPEED, s, 1e-9);
    }

    @Test
    public void noInputCoastsDownByFriction() {
        double s = HoverPhysics.MAX_SPEED;
        s = HoverPhysics.nextForwardSpeed(s, 0);
        assertEquals(HoverPhysics.MAX_SPEED * HoverPhysics.FRICTION, s, 1e-9);
    }

    @Test
    public void yawTurnsAtTurnRate() {
        assertEquals(10.0f + HoverPhysics.TURN_RATE_DEG, HoverPhysics.nextYaw(10.0f, 1), 1e-6);
        assertEquals(10.0f - HoverPhysics.TURN_RATE_DEG, HoverPhysics.nextYaw(10.0f, -1), 1e-6);
        assertEquals(10.0f, HoverPhysics.nextYaw(10.0f, 0), 1e-6);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :starwars:test --tests "com.tweeks.starwars.entity.vehicle.HoverPhysicsTest"`
Expected: FAIL — class does not exist.

- [ ] **Step 3: Write the implementation**

```java
package com.tweeks.starwars.entity.vehicle;

/**
 * Pure hover/drive math for the landspeeder. No engine imports — lives
 * outside MC classes for unit testing (spec §5.3; same pattern as
 * {@link com.tweeks.starwars.faction.Alignment}).
 *
 * <p>Spring-damper hover: accelerate toward {@link #HOVER_HEIGHT} above
 * sensed ground, damped against vertical velocity, clamped to
 * {@link #MAX_VERTICAL_ACCEL} per tick. Numerically verified to settle
 * without ground contact even entering the scan window at terminal
 * velocity (see HoverPhysicsTest).
 */
public final class HoverPhysics {
    private HoverPhysics() {}

    public static final double HOVER_HEIGHT = 0.5;
    public static final double HOVER_SCAN_DEPTH = 3.0;
    public static final double SPRING_STIFFNESS = 0.10;
    public static final double SPRING_DAMPING = 0.40;
    public static final double MAX_VERTICAL_ACCEL = 0.15;
    public static final double GRAVITY = -0.04;
    public static final double TERMINAL_FALL = -0.6;
    public static final double FORWARD_ACCEL = 0.06;
    public static final double REVERSE_ACCEL = FORWARD_ACCEL * 0.4;
    /** ~1.7x vanilla boat top speed (blocks/tick). */
    public static final double MAX_SPEED = 0.7;
    public static final double MAX_REVERSE_SPEED = 0.2;
    public static final double FRICTION = 0.95;
    public static final float TURN_RATE_DEG = 3.5f;
    /** Per-tick blend of horizontal velocity toward the facing (drift feel). */
    public static final double VELOCITY_BLEND = 0.20;

    /**
     * Vertical acceleration for this tick.
     *
     * @param distToGround hull-bottom to sensed-ground distance, or
     *                     {@code Double.NaN} when no ground within
     *                     {@link #HOVER_SCAN_DEPTH}
     * @param verticalVel  current vertical velocity (blocks/tick)
     */
    public static double verticalAccel(double distToGround, double verticalVel) {
        if (Double.isNaN(distToGround)) {
            // Free fall: gravity, but never accelerate past terminal velocity
            // (and gently decelerate back to terminal if somehow beyond it).
            return Math.max(GRAVITY, TERMINAL_FALL - verticalVel);
        }
        double accel = SPRING_STIFFNESS * (HOVER_HEIGHT - distToGround)
            - SPRING_DAMPING * verticalVel;
        return Math.max(-MAX_VERTICAL_ACCEL, Math.min(MAX_VERTICAL_ACCEL, accel));
    }

    /** @param forwardInput -1 (reverse), 0 (coast), or 1 (forward) */
    public static double nextForwardSpeed(double current, int forwardInput) {
        if (forwardInput > 0) return Math.min(MAX_SPEED, current + FORWARD_ACCEL);
        if (forwardInput < 0) return Math.max(-MAX_REVERSE_SPEED, current - REVERSE_ACCEL);
        return current * FRICTION;
    }

    /** @param turnInput -1 (right), 0, or 1 (left) — sign matched to yaw convention in LandspeederEntity */
    public static float nextYaw(float yawDegrees, int turnInput) {
        return yawDegrees + turnInput * TURN_RATE_DEG;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :starwars:test --tests "com.tweeks.starwars.entity.vehicle.HoverPhysicsTest"`
Expected: PASS, 9 tests.

- [ ] **Step 5: Commit**

```bash
git add starwars/src/main/java/com/tweeks/starwars/entity/vehicle/HoverPhysics.java starwars/src/test/java/com/tweeks/starwars/entity/vehicle/HoverPhysicsTest.java
git commit -m "feat(starwars): HoverPhysics — pure spring-damper hover and drive math

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 10: LandspeederEntity

**Files:**
- Create: `starwars/src/main/java/com/tweeks/starwars/entity/LandspeederEntity.java`
- Modify: `starwars/src/main/java/com/tweeks/starwars/ModEntities.java` (add LANDSPEEDER)
- Modify: `starwars/src/main/java/com/tweeks/starwars/data/ModLanguageProvider.java` (entity name)

**Interfaces:**
- Consumes: `HoverPhysics` (Task 9), vanilla `VehicleEntity`/`AbstractBoat` (decompiled donors), `ValueInput`/`ValueOutput` save hooks (donor: `wildwest/.../NullRiftEntity.java` lines 53-61).
- Produces: `ModEntities.LANDSPEEDER` (`EntityType<LandspeederEntity>`, `MobCategory.MISC`, `.sized(2.0f, 0.8f)`); `MAX_HULL_HEALTH = 40.0f` and `getHullHealth()` (Task 14's translator constant-folding reads `MAX_HULL_HEALTH` and `HoverPhysics.MAX_SPEED` by name); destruction drops `Registration.LANDSPEEDER` (registered in Task 11 — see the note in Step 3 about the one-task forward reference).

**This is the engine-heaviest task in the plan. Step 1 is mandatory reading, not optional.**

- [ ] **Step 1: Read the decompiled donors**

```bash
find ~/.gradle -name "AbstractBoat.java" 2>/dev/null | head -3
find ~/.gradle -name "VehicleEntity.java" 2>/dev/null | head -3
```

Read `VehicleEntity.java` fully (it is short) and these members of `AbstractBoat.java` / `Entity.java`: `getControllingPassenger`, `isLocalInstanceAuthoritative` (`Entity.java:3508`), `tick` (the client-authoritative physics branch), how the boat reads its controlling player's input (in recent versions the controlling player's input reaches the vehicle via `player.getLastClientInput()` or equivalent — find the exact accessor the boat uses), `positionRider`/`MoveFunction`, `interact`, `canAddPassenger`, `getDismountLocationForPassenger` (if boats override it), `hurtServer`/damage-wobble fields, `getDropItem`, and `spawnAtLocation`'s current signature. The reference implementation below is the **expected shape**; adjust every marked `// VERIFY` line to the decompiled reality and note deviations in your report.

- [ ] **Step 2: Register the entity type + lang**

In `ModEntities` (after PRINCESS_LEIA):

```java
    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.LandspeederEntity>> LANDSPEEDER =
        ENTITY_TYPES.register("landspeeder", () -> EntityType.Builder.<com.tweeks.starwars.entity.LandspeederEntity>of(
                com.tweeks.starwars.entity.LandspeederEntity::new, MobCategory.MISC)
            .sized(2.0f, 0.8f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "landspeeder"))));
```

No attributes registration (not a LivingEntity). No spawn placement. Lang: `add(com.tweeks.starwars.ModEntities.LANDSPEEDER.get(), "Landspeeder");`

- [ ] **Step 3: Write `LandspeederEntity`**

```java
package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.vehicle.HoverPhysics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The X-34 landspeeder: a two-seat hover vehicle. Not a Mob — no AI, no
 * attributes; extends {@link VehicleEntity} for the boat-family hurt/wobble
 * plumbing and uses boat-style client-simulated driving (the controlling
 * player's client runs the physics; vanilla vehicle-move packets sync it).
 * All tuning math lives in {@link HoverPhysics} (unit-tested).
 *
 * <p>Hull health (40) is synched and PERSISTED — a chunk reload must not
 * heal the speeder (spec §5.4). No regeneration.
 */
public class LandspeederEntity extends VehicleEntity {

    public static final float MAX_HULL_HEALTH = 40.0f;

    private static final EntityDataAccessor<Float> DATA_HULL_HEALTH =
        SynchedEntityData.defineId(LandspeederEntity.class, EntityDataSerializers.FLOAT);

    /** Signed forward speed (blocks/tick), driver's local frame. Client-side driving state. */
    private double forwardSpeed = 0.0;

    public LandspeederEntity(EntityType<? extends LandspeederEntity> type, Level level) {
        super(type, level);
    }

    // ---------- synched data + persistence ----------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); // VehicleEntity defines hurt/wobble fields — VERIFY super call requirement
        builder.define(DATA_HULL_HEALTH, MAX_HULL_HEALTH);
    }

    public float getHullHealth() {
        return this.entityData.get(DATA_HULL_HEALTH);
    }

    private void setHullHealth(float value) {
        this.entityData.set(DATA_HULL_HEALTH, value);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setHullHealth(input.getFloatOr("HullHealth", MAX_HULL_HEALTH)); // VERIFY getFloatOr exists (getIntOr does — NullRiftEntity:55)
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("HullHealth", this.getHullHealth());
    }

    // ---------- riding ----------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!this.level().isClientSide()) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < 2;
    }

    @Override
    @org.jetbrains.annotations.Nullable
    public LivingEntity getControllingPassenger() {
        // Boat semantics: first passenger drives when it's a player. VERIFY
        // the return type/override shape against AbstractBoat (some versions
        // return Player, some LivingEntity).
        return this.getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) { // VERIFY MoveFunction nesting + signature
        if (!this.hasPassenger(passenger)) return;
        int index = this.getPassengers().indexOf(passenger);
        // Side-by-side X-34 cockpit: driver left, passenger right.
        double lateral = index == 0 ? -0.45 : 0.45;
        double yawRad = Math.toRadians(this.getYRot());
        // Local +X (right of facing) rotated into world space. VERIFY sign
        // convention against AbstractBoat.positionRider.
        double ox = Math.cos(yawRad) * lateral;
        double oz = Math.sin(yawRad) * lateral;
        move.accept(passenger, this.getX() + ox, this.getY() + 0.35, this.getZ() + oz);
        passenger.setYRot(passenger.getYRot()); // keep rider yaw free (boat clamps; speeder doesn't)
    }

    // ---------- tick / physics ----------

    @Override
    public void tick() {
        super.tick();
        if (this.isLocalInstanceAuthoritative()) {
            this.tickDriven();
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
        // Speeders never cause fall damage — per-tick assignment, one-shot
        // fails (see LukeLeapGoal's comment on fallDistance).
        this.fallDistance = 0;
        for (Entity p : this.getPassengers()) {
            p.fallDistance = 0;
        }
    }

    private void tickDriven() {
        Vec3 vel = this.getDeltaMovement();
        double vy = vel.y + HoverPhysics.verticalAccel(this.sampleGroundDistance(), vel.y);

        int fwd = 0;
        int turn = 0;
        if (this.getControllingPassenger() instanceof Player driver) {
            // VERIFY: read the controlling player's movement input the way
            // AbstractBoat does in the decompiled source (getLastClientInput()
            // / zza+xxa impulse fields / Input record). Map: forward > 0 ->
            // fwd=1, backward -> fwd=-1; strafe-left -> turn left.
            fwd = driver.zza > 0 ? 1 : (driver.zza < 0 ? -1 : 0);
            turn = driver.xxa > 0 ? 1 : (driver.xxa < 0 ? -1 : 0);
            if (turn != 0) {
                // MC yaw decreases turning left; flip if the in-game turn
                // direction is inverted during smoke testing. VERIFY against
                // AbstractBoat's deltaRotation handling.
                this.setYRot(HoverPhysics.nextYaw(this.getYRot(), -turn));
            }
        }
        this.forwardSpeed = HoverPhysics.nextForwardSpeed(this.forwardSpeed, fwd);

        double yawRad = Math.toRadians(this.getYRot());
        Vec3 facing = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
        Vec3 desired = facing.scale(this.forwardSpeed);
        // Drift: blend horizontal velocity toward the facing direction.
        double bx = vel.x + (desired.x - vel.x) * HoverPhysics.VELOCITY_BLEND;
        double bz = vel.z + (desired.z - vel.z) * HoverPhysics.VELOCITY_BLEND;
        this.setDeltaMovement(bx, vy, bz);
    }

    /**
     * Distance from hull origin straight down to ground, fluid surfaces
     * included (the speeder skims water — spec §5.3). NaN = nothing within
     * scan depth.
     */
    private double sampleGroundDistance() {
        Vec3 from = this.position();
        Vec3 to = from.add(0.0, -HoverPhysics.HOVER_SCAN_DEPTH, 0.0);
        BlockHitResult hit = this.level().clip(new ClipContext(
            from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        if (hit.getType() == HitResult.Type.MISS) return Double.NaN;
        return from.y - hit.getLocation().y;
    }

    // ---------- damage / destruction ----------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isRemoved()) return false;
        if (this.isInvulnerableTo(level, source)) return false;
        if (source.getEntity() instanceof Player p && p.getAbilities().instabuild) {
            this.destroySpeeder(level, false);
            return true;
        }
        this.setHullHealth(this.getHullHealth() - amount);
        // Reuse VehicleEntity's hurt-wobble sync so the renderer's shake
        // works. VERIFY field/setter names in decompiled VehicleEntity
        // (setHurtTime / setHurtDir / setDamage family) and call them here.
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.IRON_GOLEM_HURT, SoundSource.NEUTRAL, 1.0f, 1.2f);
        if (this.getHullHealth() <= 0.0f) {
            this.destroySpeeder(level, true);
        }
        return true;
    }

    private void destroySpeeder(ServerLevel level, boolean dropItem) {
        this.ejectPassengers();
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
            this.getX(), this.getY() + 0.5, this.getZ(), 20, 0.8, 0.4, 0.8, 0.02);
        level.sendParticles(ParticleTypes.CRIT,
            this.getX(), this.getY() + 0.5, this.getZ(), 12, 0.8, 0.4, 0.8, 0.1);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.IRON_GOLEM_DEATH, SoundSource.NEUTRAL, 1.0f, 1.3f);
        if (dropItem) {
            this.spawnAtLocation(level, this.getDropItem()); // VERIFY spawnAtLocation signature
        }
        this.discard();
    }

    @Override
    protected Item getDropItem() { // VERIFY: VehicleEntity declares this (boats implement it)
        return com.tweeks.starwars.Registration.LANDSPEEDER.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(com.tweeks.starwars.Registration.LANDSPEEDER.get());
    }

    // ---------- misc ----------

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() { // VERIFY signature (some versions take an Entity param)
        return true;
    }
}
```

**Forward reference note:** `Registration.LANDSPEEDER` is created in Task 11. To keep THIS task compiling, add the item registration line from Task 11 Step 1 **in this task** (just the `DeferredItem` + a bare `LandspeederItem` class is not needed — register it as a plain `Item::new` here and Task 11 upgrades it to `LandspeederItem`):

```java
    public static final DeferredItem<Item> LANDSPEEDER = ITEMS.registerItem(
        "landspeeder", Item::new, p -> p.stacksTo(1));
```

- [ ] **Step 4: Build + full tests**

Run: `./gradlew :starwars:build`
Expected: green (57 module tests + compile). If `VehicleEntity` has abstract members not covered above (e.g. a `getDamageSources`-style hook), implement them per the decompiled boat and record it.

- [ ] **Step 5: Commit**

```bash
git add starwars/src/main/java/com/tweeks/starwars/entity/LandspeederEntity.java starwars/src/main/java/com/tweeks/starwars/ModEntities.java starwars/src/main/java/com/tweeks/starwars/Registration.java starwars/src/main/java/com/tweeks/starwars/data/ModLanguageProvider.java
git commit -m "feat(starwars): LandspeederEntity — two-seat hover vehicle with persistent hull

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 11: LandspeederItem, recipe, tab, lang

**Files:**
- Create: `starwars/src/main/java/com/tweeks/starwars/item/LandspeederItem.java`
- Modify: `starwars/src/main/java/com/tweeks/starwars/Registration.java` (upgrade the Task-10 stub registration to `LandspeederItem::new`; add tab line)
- Modify: `starwars/src/main/java/com/tweeks/starwars/data/ModRecipeProvider.java`
- Modify: `starwars/src/main/java/com/tweeks/starwars/data/ModLanguageProvider.java`

**Interfaces:**
- Consumes: `ModEntities.LANDSPEEDER` (Task 10). Donor for placement idiom: decompiled `BoatItem.java` (raycast + spawn + consume + game event).
- Produces: `Registration.LANDSPEEDER` as `DeferredItem<LandspeederItem>`; shaped recipe `IRI / III` (5 iron ingots + 1 redstone block).

- [ ] **Step 1: Write `LandspeederItem`**

Read decompiled `BoatItem.java` first and mirror its use() flow with current names. Expected shape:

```java
package com.tweeks.starwars.item;

import com.tweeks.starwars.ModEntities;
import com.tweeks.starwars.entity.LandspeederEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Boat-style placement: raycast at the ground, spawn a {@link LandspeederEntity}
 * facing the player's yaw, consume the item (not in creative). Placement
 * flow mirrors decompiled BoatItem — VERIFY each engine call against it.
 */
public class LandspeederItem extends Item {

    public LandspeederItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE); // VERIFY helper name on Item
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel sl)) {
            return InteractionResult.SUCCESS;
        }
        LandspeederEntity speeder = ModEntities.LANDSPEEDER.get()
            .create(sl, EntitySpawnReason.SPAWN_ITEM_USE); // VERIFY enum constant (BoatItem uses it)
        if (speeder == null) {
            return InteractionResult.FAIL;
        }
        Vec3 pos = hit.getLocation();
        speeder.snapTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0f);
        if (!sl.noCollision(speeder, speeder.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        sl.addFreshEntity(speeder);
        sl.gameEvent(player, GameEvent.ENTITY_PLACE, BlockPos.containing(pos));
        stack.consume(1, player); // VERIFY consume(int, LivingEntity) exists; else shrink-unless-creative like BoatItem
        return InteractionResult.SUCCESS;
    }
}
```

- [ ] **Step 2: Upgrade the registration + tab**

Replace Task 10's stub in `Registration.java`:

```java
    public static final DeferredItem<com.tweeks.starwars.item.LandspeederItem> LANDSPEEDER =
        ITEMS.registerItem("landspeeder", com.tweeks.starwars.item.LandspeederItem::new, p -> p);
```

(`stacksTo(1)` lives in the constructor, matching `BlasterPistolItem`'s pattern.) Tab line after the Leia egg: `output.accept(LANDSPEEDER.get());`

- [ ] **Step 3: Recipe**

In `ModRecipeProvider.buildRecipes()` after the rifle recipe (5 iron + 1 redstone block, spec §5.5):

```java
        // Landspeeder: hull row + engine (5 iron, 1 redstone block).
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.TRANSPORTATION, Registration.LANDSPEEDER.get())
            .pattern("IRI")
            .pattern("III")
            .define('I', Items.IRON_INGOT)
            .define('R', Items.REDSTONE_BLOCK)
            .unlockedBy("has_iron", this.has(Items.IRON_INGOT))
            .save(this.output);
```

- [ ] **Step 4: Lang** — `add(com.tweeks.starwars.Registration.LANDSPEEDER.get(), "Landspeeder");`

- [ ] **Step 5: Build + datagen twice + determinism check** (same commands as Task 2 Step 8)

- [ ] **Step 6: Commit**

```bash
git add starwars/src/main/java/com/tweeks/starwars/item/LandspeederItem.java starwars/src/main/java/com/tweeks/starwars/Registration.java starwars/src/main/java/com/tweeks/starwars/data/ModRecipeProvider.java starwars/src/main/java/com/tweeks/starwars/data/ModLanguageProvider.java starwars/src/generated
git commit -m "feat(starwars): landspeeder item — boat-style placement, IRI/III recipe

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 12: Landspeeder art — bbmodel, entity texture, item sprite, item model JSON (ART GATE)

**Files:**
- Modify: `starwars/tools/gen_bbmodels.py` (landspeeder bone/cube tables + `MOB_BONE_DEFS` + `MOBS` entries)
- Modify: `starwars/tools/gen_textures.py` (`paint_landspeeder`)
- Modify: `starwars/tools/gen_item_textures.py` (landspeeder side-profile sprite)
- Create (generated): `starwars/tools/landspeeder.bbmodel`, `starwars/src/main/resources/assets/starwars/textures/entity/landspeeder.png`, `starwars/src/main/resources/assets/starwars/textures/item/landspeeder.png`
- Create: `starwars/src/main/resources/assets/starwars/items/landspeeder.json`
- Create: `starwars/src/main/resources/assets/starwars/models/item/landspeeder.json`

**Interfaces:**
- Consumes: astromech's custom-skeleton precedent (`ASTROMECH_BONE_DEFS` / `MOB_BONE_DEFS` — the landspeeder is also non-humanoid).
- Produces: the geometry table Task 13's `LandspeederModel` MUST mirror cube-for-cube.

- [ ] **Step 1: Geometry in `gen_bbmodels.py`**

X-34 silhouette per spec §5.6: long low hull, nose, open two-seat cockpit, curved windshield (one angled slab cube), three rear turbine pods. Single static bone (no animation skeleton needed):

```python
# Landspeeder: single-bone static vehicle (no limb animation). Origin at
# entity center, Java model-space coords (y-down from the 24-block plane,
# same convention java_to_bbmodel converts for the mobs).
SPEEDER_BONE = (0, 24, 0)

LANDSPEEDER_BONE_DEFS = [
    ('body', SPEEDER_BONE),
]

LANDSPEEDER_CUBES = [
    # (name, bone, (jx, jy, jz, jw, jh, jd), uv[, inflate])
    ('hull',        SPEEDER_BONE, (-8.0, 17.0, -13.0, 16, 5, 26), (0, 0)),
    ('nose',        SPEEDER_BONE, (-6.0, 18.0, -19.0, 12, 4, 6),  (0, 31)),
    ('windshield',  SPEEDER_BONE, (-7.0, 13.0,  -7.0, 14, 4, 1),  (42, 0)),
    ('seat_left',   SPEEDER_BONE, (-7.0, 19.0,  -4.0, 6, 2, 6),   (42, 5)),
    ('seat_right',  SPEEDER_BONE, ( 1.0, 19.0,  -4.0, 6, 2, 6),   (42, 13)),
    ('turbine_c',   SPEEDER_BONE, (-3.0, 15.0,  11.0, 6, 6, 8),   (0, 41)),
    ('turbine_l',   SPEEDER_BONE, (-10.0, 16.0, 10.0, 5, 5, 9),   (28, 41)),
    ('turbine_r',   SPEEDER_BONE, ( 5.0, 16.0,  10.0, 5, 5, 9),   (28, 55)),
]
```

Wire in: `MOB_BONE_DEFS['landspeeder'] = LANDSPEEDER_BONE_DEFS` and `MOBS['landspeeder'] = LANDSPEEDER_CUBES`. Before finalizing, verify the y-coordinate convention against `ASTROMECH_CUBES` (its body sits at java-y `-14..-4` under a bone at y 24) — the hull must render with its underside ~0.1 blocks above the entity origin so the hover height reads correctly; adjust y values once you've confirmed how `java_to_bbmodel` maps them, and keep Task 13's model class in exact agreement.

- [ ] **Step 2: Entity texture in `gen_textures.py`**

`paint_landspeeder(rgba)` on a 64×64 sheet covering the UV blocks above. Palette (all must appear — art gate):

```python
SPEEDER_BODY    = (0xC8, 0x86, 0x4A, 0xFF)  # sand-orange hull
SPEEDER_BODY_HI = (0xE0, 0xA6, 0x6A, 0xFF)  # sun-lit top
SPEEDER_BODY_DK = (0x9A, 0x64, 0x36, 0xFF)  # underside shadow
SPEEDER_RUST    = (0x7A, 0x52, 0x30, 0xFF)  # weathering blotches
SPEEDER_COCKPIT = (0x2A, 0x2A, 0x30, 0xFF)  # dark cockpit interior / seats
SPEEDER_SEAT_HI = (0x44, 0x44, 0x4C, 0xFF)
SPEEDER_GLASS   = (0xA8, 0xC8, 0xD8, 0xFF)  # windshield
SPEEDER_GLASS_HI= (0xD0, 0xE8, 0xF0, 0xFF)
SPEEDER_METAL   = (0x8A, 0x8A, 0x92, 0xFF)  # turbine housings
SPEEDER_METAL_DK= (0x5A, 0x5A, 0x62, 0xFF)  # turbine intake dark
SPEEDER_METAL_HI= (0xB4, 0xB4, 0xBC, 0xFF)
```

Hull top gets `SPEEDER_BODY_HI` with `SPEEDER_RUST` weathering blotches (4-6 irregular 1-2px clusters); sides `SPEEDER_BODY` with `SPEEDER_BODY_DK` lower third; turbine front faces get a `SPEEDER_METAL_DK` circular intake with `SPEEDER_METAL_HI` rim.

- [ ] **Step 3: Item sprite in `gen_item_textures.py`**

16×16 side-profile: sand-orange hull wedge (rows 7-10), dark cockpit notch, three grey turbine circles at the rear, using the same palette constants (redeclare locally if the tools don't share modules). Follow the file's existing per-item function + dispatch pattern.

- [ ] **Step 4: Item model JSON (hand-authored, flat sprite)**

`starwars/src/main/resources/assets/starwars/items/landspeeder.json`:

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "starwars:item/landspeeder"
  }
}
```

`starwars/src/main/resources/assets/starwars/models/item/landspeeder.json`:

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "starwars:item/landspeeder"
  }
}
```

- [ ] **Step 5: Run the tools** (same invocations as Task 7 Step 4, plus `gen_item_textures.py`'s documented invocation). Verify only the new files changed.

- [ ] **Step 6: ART GATE** — view `landspeeder.png` (entity) and `landspeeder.png` (item sprite): 3+ tones per region, weathering visible, turbine intakes read as circles, no flat rectangles.

- [ ] **Step 7: Commit**

```bash
git add starwars/tools/gen_bbmodels.py starwars/tools/gen_textures.py starwars/tools/gen_item_textures.py starwars/tools/landspeeder.bbmodel starwars/src/main/resources/assets/starwars/textures/entity/landspeeder.png starwars/src/main/resources/assets/starwars/textures/item/landspeeder.png starwars/src/main/resources/assets/starwars/items/landspeeder.json starwars/src/main/resources/assets/starwars/models/item/landspeeder.json
git commit -m "feat(starwars): landspeeder bbmodel, entity texture, item sprite

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 13: Landspeeder client model + renderer

**Files:**
- Create: `starwars/src/main/java/com/tweeks/starwars/client/model/LandspeederModel.java`
- Create: `starwars/src/main/java/com/tweeks/starwars/client/LandspeederRenderer.java`
- Modify: `starwars/src/main/java/com/tweeks/starwars/client/ClientSetup.java`

**Interfaces:**
- Consumes: Task 12's geometry table (cube-for-cube), Task 10's entity (`getHullHealth`, tickCount, yRot/yRotO), decompiled `BoatRenderer`/`AbstractBoatRenderer` + its render state as the donor for a non-living entity renderer in this version.
- Produces: registered renderer with banking + hover bob (spec §5.6: bank up to ±12° proportional to yaw rate; ±0.03-block sinusoidal bob, ~2 s period).

- [ ] **Step 1: Read decompiled `BoatRenderer` (or `AbstractBoatRenderer`) and its render-state class.** Non-living renderers in this version follow the render-state pattern (`createRenderState` / `extractRenderState` / `render(state, ...)`) — lift the exact override names, the model-layer bake idiom, and how the boat applies its yaw + hurt wobble to the PoseStack.

- [ ] **Step 2: Write the model** — plain `Model` subclass (or whatever `BoatModel`'s current base is) with `createBodyLayer()` mirroring Task 12's cubes exactly:

```java
package com.tweeks.starwars.client.model;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * X-34 landspeeder geometry — MUST stay cube-for-cube identical to
 * starwars/tools/gen_bbmodels.py's LANDSPEEDER_CUBES (the editable source).
 * Base class per decompiled BoatModel — VERIFY and match.
 */
public class LandspeederModel /* extends the base BoatModel uses — VERIFY */ {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "landspeeder"),
        "main");

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8.0f, 17.0f, -13.0f, 16.0f, 5.0f, 26.0f)
                .texOffs(0, 31).addBox(-6.0f, 18.0f, -19.0f, 12.0f, 4.0f, 6.0f)
                .texOffs(42, 0).addBox(-7.0f, 13.0f, -7.0f, 14.0f, 4.0f, 1.0f)
                .texOffs(42, 5).addBox(-7.0f, 19.0f, -4.0f, 6.0f, 2.0f, 6.0f)
                .texOffs(42, 13).addBox(1.0f, 19.0f, -4.0f, 6.0f, 2.0f, 6.0f)
                .texOffs(0, 41).addBox(-3.0f, 15.0f, 11.0f, 6.0f, 6.0f, 8.0f)
                .texOffs(28, 41).addBox(-10.0f, 16.0f, 10.0f, 5.0f, 5.0f, 9.0f)
                .texOffs(28, 55).addBox(5.0f, 16.0f, 10.0f, 5.0f, 5.0f, 9.0f),
            PartPose.offset(0.0f, 24.0f, 0.0f)); // VERIFY offset convention vs BoatModel
        return LayerDefinition.create(mesh, 64, 64);
    }
    // constructor + renderToBuffer per the base class — mirror BoatModel.
}
```

- [ ] **Step 3: Write the renderer** — donor is `BoatRenderer`, adapted:

Render-state fields to extract per frame: interpolated yaw, hurt-wobble values (VehicleEntity's synced hurt fields), `bankRoll = clamp((yRot - yRotO) * 3.0f, -12f, +12f)` (degrees; yaw-rate-proportional), `bobOffset = sin((tickCount + partialTick) * (2π/40)) * 0.03`. Render: translate by bob, rotate -yaw, roll bank around the forward axis, apply hurt wobble like the boat does, render model with `textures/entity/landspeeder.png`.

- [ ] **Step 4: Register in `ClientSetup`** (renderer + layer definition lines, same shape as Task 8 Step 4).

- [ ] **Step 5: Build** — `./gradlew :starwars:build`, green.

- [ ] **Step 6: Commit**

```bash
git add starwars/src/main/java/com/tweeks/starwars/client/model/LandspeederModel.java starwars/src/main/java/com/tweeks/starwars/client/LandspeederRenderer.java starwars/src/main/java/com/tweeks/starwars/client/ClientSetup.java
git commit -m "feat(starwars): landspeeder renderer with banking and hover bob

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 14: Translator — vehicle path + named-character singleton honesty

**Files:**
- Modify: `translator/src/main/kotlin/com/tweeks/translator/java/EntityAnalyzer.kt`
- Modify: `translator/src/main/kotlin/com/tweeks/translator/emit/Untranslatable.kt`
- Test: `translator/src/test/kotlin/com/tweeks/translator/java/EntityAnalyzerTest.kt` (additions)
- Test: `translator/src/test/kotlin/com/tweeks/translator/emit/UntranslatableTest.kt` (additions)

**Interfaces:**
- Consumes: `EntityAnalyzer.analyzeOne` (the projectile-skip at its top is the pattern donor for the vehicle branch), `resolveConstantField`/`resolveNumericValue` (existing constant folding), `Untranslatable`'s `record*` + `writeFor` section machinery.
- Produces: Bedrock vehicle JSON for `VehicleEntity` subclasses (rideable, 2 seats, input-ground-controlled, health/movement from folded constants, NO walking-mob defaults); `UNTRANSLATABLE.md` sections for vehicle approximation and named-character singletons.

- [ ] **Step 1: Write the failing tests first.** Read `EntityAnalyzerTest.kt` to learn its fixture harness (how it feeds Java source strings/files and asserts on emitted JSON), then add, in that harness's exact style:

1. `vehicleEntityEmitsRideableNotWalkingMob` — fixture: a `Landspeeder`-like class `extends VehicleEntity` with `public static final float MAX_HULL_HEALTH = 40.0f;`, a sibling `HoverPhysics` class with `public static final double MAX_SPEED = 0.7;`, registered `("landspeeder", MobCategory.MISC).sized(2.0f, 0.8f)`. Assert emitted behavior JSON: has `minecraft:rideable` with `seat_count == 2`, has `minecraft:input_ground_controlled`, `minecraft:health.value == 40`, `minecraft:movement.value == 0.35`, has `minecraft:collision_box` and `minecraft:physics`, and does **NOT** contain `minecraft:navigation.walk`, `minecraft:jump.static`, or `minecraft:behavior.*` keys.
2. `vehicleWithoutResolvableConstantsRecordsUntranslatable` — same fixture minus the constants; assert health/movement components absent and an `Untranslatable` vehicle entry recorded.
3. `savedDataEntityRecordsSingletonEntry` — fixture: a mob whose class body references `FooSavedData.get(server)`; assert `UNTRANSLATABLE.md` output (via `writeFor` to a temp dir, or however `UntranslatableTest` asserts sections) contains a "singleton" section naming the entity.

Run: `./gradlew :translator:test --tests "*EntityAnalyzerTest*"` → new tests FAIL.

- [ ] **Step 2: Add the two recorders to `Untranslatable.kt`** (follow the existing `recordX(modId, ...)` + section-list + `writeFor` pattern exactly — read two existing recorders and their section emission first):

```kotlin
    /** Vehicle emitted as a ground-driven Bedrock approximation (no hover). */
    fun recordVehicleApproximated(modId: String, entityId: String, summary: String) {
        // same list-append + section shape as recordDatapackWorldgenStructure
    }

    /** Java named-character singleton (SavedData-backed) has no Bedrock equivalent. */
    fun recordNamedCharacterSingleton(modId: String, entityId: String, summary: String) {
        // same shape
    }
```

Each gets its own `writeFor` section ("## Vehicles (approximated)", "## Named-character singletons (not enforced on Bedrock)") emitted in the same deterministic order style as the existing sections.

- [ ] **Step 3: Add the vehicle branch to `EntityAnalyzer.analyzeOne`**, immediately after the projectile skip (same early-return pattern):

```kotlin
        // Vehicles (non-Mob VehicleEntity subclasses, e.g. starwars'
        // landspeeder) must NOT flow through the mob pipeline — it would
        // emit a walking mob with no seats. Emit a rideable,
        // player-input-driven approximation instead and return.
        if (isVehicleClass(entityClass)) {
            emitVehicle(mod, reg, entityClass, classLookup, outputRoot)
            return
        }
```

with:

```kotlin
    /** Direct or same-module-transitive `extends VehicleEntity`. */
    private fun isVehicleClass(entity: ClassOrInterfaceDeclaration): Boolean =
        entity.extendedTypes.any { it.nameAsString == "VehicleEntity" }

    private fun emitVehicle(
        mod: ModDiscovery.DiscoveredMod,
        reg: EntityRegistration,
        entityClass: ClassOrInterfaceDeclaration,
        classLookup: (String) -> ClassOrInterfaceDeclaration?,
        outputRoot: Path,
    ) {
        val sorted = sortedMapOf<String, JsonElement>()

        sorted["minecraft:type_family"] = buildJsonObject {
            put("family", buildJsonArray {
                add(JsonPrimitive(reg.entityId))
                add(JsonPrimitive("vehicle"))
            })
        }
        sorted["minecraft:collision_box"] = buildJsonObject {
            put("width", num(roundTo4(reg.width.toDouble())))
            put("height", num(roundTo4(reg.height.toDouble())))
        }
        sorted["minecraft:physics"] = buildJsonObject { }
        sorted["minecraft:pushable"] = buildJsonObject {
            put("is_pushable", JsonPrimitive(false))
            put("is_pushable_by_piston", JsonPrimitive(true))
        }

        // Hull health from the entity's own MAX_HULL_HEALTH constant (the
        // vehicle has no attributes — the usual attrs path can't fire).
        val hull = resolveConstantField(entityClass, "MAX_HULL_HEALTH", classLookup, mutableSetOf())
        if (hull != null) {
            sorted["minecraft:health"] = buildJsonObject {
                put("value", num(hull))
                put("max", num(hull))
            }
        }
        // Bedrock movement.value is a walk-speed-like scalar, not
        // blocks/tick — halve the Java top speed as the documented mapping.
        val maxSpeed = resolveConstantField(
            classLookup("HoverPhysics") ?: entityClass, "MAX_SPEED", classLookup, mutableSetOf())
        if (maxSpeed != null) {
            sorted["minecraft:movement"] = buildJsonObject {
                put("value", num(roundTo4(maxSpeed / 2.0)))
            }
        }
        if (hull == null || maxSpeed == null) {
            unt.recordEntityAttributeUnresolved(
                mod.modId, entityClass.nameAsString,
                "vehicle constants MAX_HULL_HEALTH/MAX_SPEED not statically resolvable",
            )
        }

        sorted["minecraft:rideable"] = buildJsonObject {
            put("seat_count", JsonPrimitive(2))
            put("family_types", buildJsonArray { add(JsonPrimitive("player")) })
            put("interact_text", JsonPrimitive("action.interact.ride"))
            put("seats", buildJsonArray {
                add(buildJsonObject { put("position", buildJsonArray {
                    add(num(-0.45)); add(num(0.35)); add(num(0.0)) }) })
                add(buildJsonObject { put("position", buildJsonArray {
                    add(num(0.45)); add(num(0.35)); add(num(0.0)) }) })
            })
        }
        sorted["minecraft:input_ground_controlled"] = buildJsonObject { }
        sorted["minecraft:movement.basic"] = buildJsonObject { }

        unt.recordVehicleApproximated(
            mod.modId, reg.entityId,
            "Java hover physics (spring to 0.5 blocks, water-skimming) has no " +
                "Bedrock equivalent — emitted as a ground-driven rideable " +
                "(input_ground_controlled); banking/bob visuals dropped.",
        )

        // Behavior + client entity JSON writing: same two write blocks as
        // analyzeOne's tail (identifier/is_spawnable/description + geometry/
        // texture pick). Extract those ~60 lines into a shared private helper
        // `writeEntityFiles(mod, reg, components, outputRoot)` and call it
        // from BOTH analyzeOne and emitVehicle rather than duplicating.
    }
```

- [ ] **Step 4: Add singleton detection** in `analyzeOne` (mob path, after `securityFamilies`):

```kotlin
        // Named-character singletons: a mob whose class references a
        // *SavedData type is (in this repo) a one-per-server character.
        // Bedrock has no SavedData equivalent — record honestly.
        val referencesSavedData = entityClass
            .findAll(com.github.javaparser.ast.type.ClassOrInterfaceType::class.java)
            .any { it.nameAsString.endsWith("SavedData") } ||
            entityClass.findAll(MethodCallExpr::class.java).any { call ->
                (call.scope.orElse(null) as? com.github.javaparser.ast.expr.NameExpr)
                    ?.nameAsString?.endsWith("SavedData") == true
            }
        if (referencesSavedData) {
            unt.recordNamedCharacterSingleton(
                mod.modId, reg.entityId,
                "Java enforces one living instance per server via SavedData " +
                    "(finalizeSpawn claim + die/remove clear); Bedrock output has " +
                    "no equivalent — duplicates are possible.",
            )
        }
```

This fires for all six named characters (Vader, Luke, Obi-Wan, Boba Fett, Han, Leia) — closing the pre-existing honesty gap the spec review found (spec §6).

- [ ] **Step 5: Run the new tests, then the full translator suite**

```bash
./gradlew :translator:test --tests "*EntityAnalyzerTest*" --tests "*UntranslatableTest*"
./gradlew :translator:test
```

Expected: new tests pass; full suite green. If any existing EntityAnalyzer test asserted on the exact shape of `analyzeOne`'s tail (now extracted into `writeEntityFiles`), fix the refactor, not the test.

- [ ] **Step 6: Commit**

```bash
git add translator/src/main/kotlin/com/tweeks/translator/java/EntityAnalyzer.kt translator/src/main/kotlin/com/tweeks/translator/emit/Untranslatable.kt translator/src/test/kotlin/com/tweeks/translator/java/EntityAnalyzerTest.kt translator/src/test/kotlin/com/tweeks/translator/emit/UntranslatableTest.kt
git commit -m "feat(translator): vehicle emission path + named-character singleton honesty entries

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 15: Regenerate bedrock-out/starwars

**Files:**
- Modify (generated): `bedrock-out/starwars/**` ONLY. Global Constraints: never touch any other module's `bedrock-out/` tree.

**Interfaces:**
- Consumes: Tasks 2-14 (all Java content + translator changes).
- Produces: committed Bedrock addon output with Han/Leia entities, landspeeder vehicle entity, landspeeder recipe, updated UNTRANSLATABLE.md.

- [ ] **Step 1: Find the translate invocation the previous milestones used**

```bash
git log --oneline -- bedrock-out/starwars | head -5
grep -rn "translate" translator/build.gradle.kts | head
```

(The Cli supports per-mod scoping; earlier commits regenerated `bedrock-out/starwars` without touching siblings — reuse that exact command, WITHOUT `--with-llm` per spec §6: the two new goals emit as cache-miss TODO stubs like the existing six.)

- [ ] **Step 2: Run it; inspect the diff**

```bash
git status --porcelain bedrock-out/
```

MUST show changes ONLY under `bedrock-out/starwars/`. Verify the content:

```bash
ls bedrock-out/starwars/behavior_pack/entities/ | grep -E "han_solo|princess_leia|landspeeder"
ls bedrock-out/starwars/behavior_pack/recipes/ | grep landspeeder
ls bedrock-out/starwars/behavior_pack/scripts/goals/ | grep -E "HanQuickdraw|LeiaRally"
grep -c "singleton" bedrock-out/starwars/UNTRANSLATABLE.md   # expect >= 6 entries' worth
grep -n "landspeeder" bedrock-out/starwars/UNTRANSLATABLE.md  # vehicle-approximation entry
python3 -c "import json,sys; d=json.load(open('bedrock-out/starwars/behavior_pack/entities/landspeeder.json')); c=d['minecraft:entity']['components']; assert 'minecraft:rideable' in c and 'minecraft:input_ground_controlled' in c and 'minecraft:navigation.walk' not in c; print('vehicle OK')"
```

- [ ] **Step 3: Commit**

```bash
git add bedrock-out/starwars
git commit -m "feat(starwars): regenerate Bedrock addon — Han, Leia, landspeeder vehicle, singleton honesty entries

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 16: Final sweep

**Files:** none new (fixes only if something is red).

- [ ] **Step 1: Full test + build sweep**

```bash
./gradlew :starwars:test :translator:test :starwars:build
```

Expected: everything green (starwars: 48 pre-existing + ~20 new tests; translator: 158+ plus Task 14's additions). Note: repo-wide `./gradlew build` fails on `:translator:driftCheck` because of the user's pre-existing WIP in sibling modules — that failure is PRE-EXISTING and out of scope; do not "fix" it.

- [ ] **Step 2: Datagen determinism final check** — run the datagen task once more; `git status` must be clean.

- [ ] **Step 3: Verify no WIP contamination**

```bash
git status --porcelain | grep -v "^??" | grep -vE "starwars|bedrock-out/starwars|translator|docs/superpowers|\.superpowers"
```

Expected: only the user's pre-existing WIP lines (thief/securityguard/wildwest/craftee/gradle files), unchanged from session start. If anything else appears, investigate before committing anything.

- [ ] **Step 4: Update the progress ledger** (`.superpowers/sdd/progress.md`) with the expansion's task completions, and record the dev-client smoke checklist for the user: drive the speeder (feel, banking, water skim, 3-block ledge fall), two-seat ride, break it (item drop), craft it, spawn Han (watch a quickdraw open a fight), spawn Leia near a fight (rally ring + Resistance/Regen), `/kill` + re-egg a singleton (duplicate discard).

---

## Self-review notes (already applied)

- Spec §6 recipe firmness → Task 11/15 emit and verify the Bedrock recipe.
- Spec §6 vehicle path "replace, not layer" → Task 14 builds a fresh component map (no walking-mob defaults) and the test asserts `navigation.walk` absent.
- Spec §5.4 hull persistence → Task 10 `ValueInput`/`ValueOutput` (`getFloatOr` flagged VERIFY with `getIntOr` precedent).
- Spec §4.3 Resistance rationale → embedded in `RallyMath` javadoc and RallyMathTest.
- Type-consistency pass: `QuickdrawState`/`RallyMath`/`HoverPhysics` names and signatures match between defining tasks (1/4/9) and consuming tasks (3/6/10/14); `MAX_HULL_HEALTH`/`MAX_SPEED` names match between Task 10 code and Task 14's constant folding; `HAN_SOLO`/`PRINCESS_LEIA`/`LANDSPEEDER` registry ids consistent across Tasks 2/5/7/8/10-13.
