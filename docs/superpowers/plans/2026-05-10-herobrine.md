# Herobrine Boss Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a singleton, world-wide Herobrine boss mob to the `:wildwest` mod with rare night spawn, frequent teleport, distance-based AI mixing netherite-sword melee, lightning, and meteor barrage, plus a Meteor Staff drop weapon.

**Architecture:** Herobrine is a `Monster` subclass with custom AI goals (teleport, meteor, lightning, melee) keyed off target distance. Singleton enforcement is anchored to overworld `SavedData` so reads/writes from any dimension consult the same record. Meteors are a single `ThrowableItemProjectile`-based entity (`MeteorEntity`) reused for both Herobrine summons (vertical fall) and Meteor Staff fires (forward arc), with a settable `directHitDamage` field. On impact, meteors create a magma block + adjacent fire + 6-damage AoE; staff direct hits add 20 damage to the target.

**Tech Stack:** Java 25, NeoForge 26.1.2.30-beta, MC 26.1.2 (per `gradle.properties`). JUnit 5 / Jupiter for tests. Static JSON for biome-modifier / loot-table / lang. NeoForge data-gen providers for damage types + their tags. Renderer reuse (`ThrownItemRenderer` for meteors; `HumanoidMobRenderer` for Herobrine).

**Spec:** See `docs/superpowers/specs/2026-05-10-herobrine-design.md`.

---

## Pre-flight

- [ ] **Step 0: Confirm the working tree is clean and tests pass**

Run: `git status && ./gradlew :wildwest:test`
Expected: working tree clean (or only the pre-existing `walker.png` / `walker.bbmodel` modifications); all tests pass.

If the wildwest tests fail before any plan changes, stop and investigate before proceeding.

---

## Task 1: Register `wildwest:meteor` damage type

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestDamageTypes.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/data/ModDamageTypeProvider.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/data/ModDamageTypeTagsProvider.java`

- [ ] **Step 1: Add `METEOR` resource key + `meteor(...)` factory in `WildWestDamageTypes.java`**

Insert after the `KNIFE` resource key (around line 23):

```java
    public static final ResourceKey<DamageType> METEOR = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "meteor"));
```

Insert after the `knife(...)` factory (around line 47):

```java
    public static DamageSource meteor(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(METEOR),
            attacker);
    }

    public static DamageSource meteorAoe(Level level) {
        return new DamageSource(
            level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(METEOR));
    }
```

Add the `Level` import at the top of the file:
```java
import net.minecraft.world.level.Level;
```

The `meteorAoe` overload (no attacker) is for the AoE branch where the projectile may already be discarded; the entity-attributable variant is used for direct hits.

- [ ] **Step 2: Add bootstrap entry for `METEOR` in `ModDamageTypeProvider.java`**

Inside `bootstrap(BootstrapContext<DamageType> ctx)`, add after the `KNIFE` line:

```java
        ctx.register(WildWestDamageTypes.METEOR,
            new DamageType("wildwest.meteor", 0.1f));
```

- [ ] **Step 3: Tag `METEOR` as `IS_FIRE` in `ModDamageTypeTagsProvider.java`**

Inside `addTags(HolderLookup.Provider provider)`, append:

```java
        tag(DamageTypeTags.IS_FIRE).add(WildWestDamageTypes.METEOR);
```

- [ ] **Step 4: Regenerate datapack files**

Run: `./gradlew :wildwest:runData`
Expected: Successful task. New file `wildwest/src/generated/serverData/data/wildwest/damage_type/meteor.json` is created and `wildwest/src/generated/serverData/data/minecraft/tags/damage_type/is_fire.json` is updated to include `wildwest:meteor`.

- [ ] **Step 5: Verify generated outputs**

Run: `cat wildwest/src/generated/serverData/data/wildwest/damage_type/meteor.json`
Expected: JSON with `"message_id": "wildwest.meteor"`, `"exhaustion": 0.1`.

Run: `cat wildwest/src/generated/serverData/data/minecraft/tags/damage_type/is_fire.json`
Expected: JSON listing `wildwest:meteor` (with `"replace": false`).

- [ ] **Step 6: Compile + commit**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/WildWestDamageTypes.java \
        wildwest/src/main/java/com/tweeks/wildwest/data/ModDamageTypeProvider.java \
        wildwest/src/main/java/com/tweeks/wildwest/data/ModDamageTypeTagsProvider.java \
        wildwest/src/generated/serverData/data/wildwest/damage_type/meteor.json \
        wildwest/src/generated/serverData/data/minecraft/tags/damage_type/is_fire.json
git commit -m "feat(wildwest): register wildwest:meteor damage type (Herobrine prep)"
```

---

## Task 2: Add Herobrine lang strings

**Files:**
- Modify: `wildwest/src/main/resources/assets/wildwest/lang/en_us.json`

- [ ] **Step 1: Add four new keys in alphabetical position**

Open the file and add (preserving alphabetical key order):

```json
  "entity.wildwest.herobrine": "Herobrine",
  "item.wildwest.herobrine_spawn_egg": "Herobrine Spawn Egg",
  "item.wildwest.herobrine_spawn_egg.away": "Herobrine is far away…",
  "item.wildwest.herobrine_spawn_egg.overworld_only": "Herobrine belongs only to the surface…",
  "item.wildwest.meteor_staff": "Meteor Staff",
```

Place between the existing `"entity.wildwest.deputy"` and `"entity.wildwest.sherrif"` entries for the entity key, and similarly for the item keys (between `"item.wildwest.deputy_spawn_egg"` and `"item.wildwest.pistol"`). Maintain alphabetical ordering of the existing JSON.

- [ ] **Step 2: Verify JSON parses**

Run: `python3 -c "import json; json.load(open('wildwest/src/main/resources/assets/wildwest/lang/en_us.json'))"`
Expected: no output (silent success).

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/resources/assets/wildwest/lang/en_us.json
git commit -m "feat(wildwest): add Herobrine + Meteor Staff lang strings"
```

---

## Task 3: `HerobrineState` pure record + tests

This task extracts the SavedData's mutable state into a pure (non-Minecraft) record so it can be unit-tested. The actual `SavedData` (next task) wraps it.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineState.java`
- Create: `wildwest/src/test/java/com/tweeks/wildwest/entity/HerobrineStateTest.java`

- [ ] **Step 1: Write the failing tests**

Create `wildwest/src/test/java/com/tweeks/wildwest/entity/HerobrineStateTest.java`:

```java
package com.tweeks.wildwest.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HerobrineStateTest {

    @Test
    void defaultState_isCleared() {
        HerobrineState s = new HerobrineState();
        assertFalse(s.isAlive());
        assertNull(s.getCurrentId());
        assertNull(s.getDimensionId());
    }

    @Test
    void setAlive_storesAllFields() {
        HerobrineState s = new HerobrineState();
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        s.setAlive(id, "minecraft:overworld");

        assertTrue(s.isAlive());
        assertEquals(id, s.getCurrentId());
        assertEquals("minecraft:overworld", s.getDimensionId());
    }

    @Test
    void clear_resetsToDefault() {
        HerobrineState s = new HerobrineState();
        s.setAlive(UUID.randomUUID(), "minecraft:overworld");
        s.clear();

        assertFalse(s.isAlive());
        assertNull(s.getCurrentId());
        assertNull(s.getDimensionId());
    }

    @Test
    void copyOf_returnsEqualSnapshot() {
        HerobrineState s = new HerobrineState();
        UUID id = UUID.randomUUID();
        s.setAlive(id, "minecraft:the_nether");

        HerobrineState copy = HerobrineState.copyOf(s);
        assertTrue(copy.isAlive());
        assertEquals(id, copy.getCurrentId());
        assertEquals("minecraft:the_nether", copy.getDimensionId());
    }

    @Test
    void copyOf_isIndependent() {
        HerobrineState s = new HerobrineState();
        s.setAlive(UUID.randomUUID(), "minecraft:overworld");

        HerobrineState copy = HerobrineState.copyOf(s);
        s.clear();

        // The original is cleared, but the copy still holds the original values.
        assertTrue(copy.isAlive());
        assertEquals("minecraft:overworld", copy.getDimensionId());
    }
}
```

- [ ] **Step 2: Run tests, verify they fail with "cannot find symbol HerobrineState"**

Run: `./gradlew :wildwest:test --tests com.tweeks.wildwest.entity.HerobrineStateTest`
Expected: FAIL — compilation error referencing `HerobrineState`.

- [ ] **Step 3: Implement `HerobrineState`**

Create `wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineState.java`:

```java
package com.tweeks.wildwest.entity;

import java.util.UUID;

/**
 * Mutable snapshot of the Herobrine singleton's status. Lives outside any
 * Minecraft class so it can be unit-tested without booting the FML loader
 * (same constraint as {@link SteveStackerPhase}).
 *
 * <p>The persistent backing is {@link HerobrineSavedData}, which translates
 * between this state and Minecraft NBT.
 */
public final class HerobrineState {

    private boolean alive;
    private UUID currentId;
    private String dimensionId;

    public HerobrineState() {}

    public boolean isAlive() { return this.alive; }
    public UUID getCurrentId() { return this.currentId; }
    public String getDimensionId() { return this.dimensionId; }

    public void setAlive(UUID id, String dimensionId) {
        this.alive = true;
        this.currentId = id;
        this.dimensionId = dimensionId;
    }

    public void clear() {
        this.alive = false;
        this.currentId = null;
        this.dimensionId = null;
    }

    public static HerobrineState copyOf(HerobrineState other) {
        HerobrineState copy = new HerobrineState();
        if (other.alive) {
            copy.setAlive(other.currentId, other.dimensionId);
        }
        return copy;
    }
}
```

- [ ] **Step 4: Run tests, verify pass**

Run: `./gradlew :wildwest:test --tests com.tweeks.wildwest.entity.HerobrineStateTest`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineState.java \
        wildwest/src/test/java/com/tweeks/wildwest/entity/HerobrineStateTest.java
git commit -m "feat(wildwest): HerobrineState pure record for singleton tracking"
```

---

## Task 4: `HerobrineSavedData` (Minecraft-side wrapper)

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineSavedData.java`

- [ ] **Step 1: Implement the SavedData class**

Create `wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineSavedData.java`:

```java
package com.tweeks.wildwest.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

/**
 * Per-server singleton record for Herobrine. Anchored to the overworld's
 * data storage so reads/writes from any dimension consult the same data.
 *
 * <p>Wraps {@link HerobrineState} (pure POJO; unit-tested in
 * {@code HerobrineStateTest}). All Minecraft-API glue lives here.
 */
public final class HerobrineSavedData extends SavedData {

    public static final String FILE_ID = "wildwest_herobrine";

    private static final SavedData.Factory<HerobrineSavedData> FACTORY = new SavedData.Factory<>(
        HerobrineSavedData::new,
        HerobrineSavedData::load);

    private final HerobrineState state = new HerobrineState();

    public HerobrineSavedData() {}

    /**
     * Read/create the singleton record. Anchored to {@code server.overworld()}
     * so callers in other dimensions consult the same file.
     */
    public static HerobrineSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public boolean isAlive() { return this.state.isAlive(); }
    public UUID getCurrentId() { return this.state.getCurrentId(); }

    public ResourceKey<Level> getDimension() {
        String id = this.state.getDimensionId();
        if (id == null) return null;
        return ResourceKey.create(Registries.DIMENSION,
            Identifier.parse(id));
    }

    public void setAlive(UUID id, ResourceKey<Level> dimension) {
        this.state.setAlive(id, dimension.location().toString());
        this.setDirty();
    }

    public void clear() {
        this.state.clear();
        this.setDirty();
    }

    @Override
    public void save(ValueOutput output) {
        output.putBoolean("Alive", this.state.isAlive());
        if (this.state.isAlive()) {
            UUID id = this.state.getCurrentId();
            if (id != null) {
                output.putString("CurrentId", id.toString());
            }
            String dim = this.state.getDimensionId();
            if (dim != null) {
                output.putString("Dimension", dim);
            }
        }
    }

    public static HerobrineSavedData load(ValueInput input) {
        HerobrineSavedData sd = new HerobrineSavedData();
        if (input.getBooleanOr("Alive", false)) {
            String idStr = input.getStringOr("CurrentId", "");
            String dimStr = input.getStringOr("Dimension", "");
            if (!idStr.isEmpty() && !dimStr.isEmpty()) {
                try {
                    sd.state.setAlive(UUID.fromString(idStr), dimStr);
                } catch (IllegalArgumentException ignored) {
                    // Corrupt UUID — leave state cleared.
                }
            }
        }
        return sd;
    }
}
```

> **Note on the SavedData API:** the `save(ValueOutput)` / `load(ValueInput)` signatures match the codebase's existing `addAdditionalSaveData` / `readAdditionalSaveData` style (see `SteveStackerEntity:98-107`). If the active NeoForge version exposes `save(CompoundTag, HolderLookup.Provider)` instead, swap to that signature using the same field names. The unit tests in Task 3 cover the pure logic regardless.

- [ ] **Step 2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

If the build fails on `save`/`load` signatures, switch to whichever overload the parent class declares — search vanilla `SavedData` for the abstract method, mirror it.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineSavedData.java
git commit -m "feat(wildwest): HerobrineSavedData overworld-anchored singleton record"
```

---

## Task 5: `MeteorImpactLogic` pure helper + tests

Pure helper for the "should this block be replaced by magma?" decision. Tested without booting Minecraft via predicate injection.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/projectile/MeteorImpactLogic.java`
- Create: `wildwest/src/test/java/com/tweeks/wildwest/entity/projectile/MeteorImpactLogicTest.java`

- [ ] **Step 1: Write the failing tests**

Create `wildwest/src/test/java/com/tweeks/wildwest/entity/projectile/MeteorImpactLogicTest.java`:

```java
package com.tweeks.wildwest.entity.projectile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeteorImpactLogicTest {

    @Test
    void airBlock_isNotReplaceable() {
        assertFalse(MeteorImpactLogic.shouldReplaceWithMagma(true, false));
    }

    @Test
    void solidNonImmune_isReplaceable() {
        assertFalse(MeteorImpactLogic.shouldReplaceWithMagma(false, false) == false);
        // Equivalent positive assertion:
        assertTrue(MeteorImpactLogic.shouldReplaceWithMagma(false, false));
    }

    @Test
    void dragonImmune_isNotReplaceable() {
        // Bedrock, end portal frame, etc.
        assertFalse(MeteorImpactLogic.shouldReplaceWithMagma(false, true));
    }

    @Test
    void airAndImmune_returnsFalse() {
        // Pathological combination — air shouldn't be replaced regardless.
        assertFalse(MeteorImpactLogic.shouldReplaceWithMagma(true, true));
    }
}
```

- [ ] **Step 2: Run tests, verify they fail**

Run: `./gradlew :wildwest:test --tests com.tweeks.wildwest.entity.projectile.MeteorImpactLogicTest`
Expected: FAIL — compilation error referencing `MeteorImpactLogic`.

- [ ] **Step 3: Implement the helper**

Create `wildwest/src/main/java/com/tweeks/wildwest/entity/projectile/MeteorImpactLogic.java`:

```java
package com.tweeks.wildwest.entity.projectile;

/**
 * Pure decision logic for meteor block-impact rewriting. Extracted as a
 * standalone class so it can be unit-tested without booting Minecraft —
 * the test passes booleans directly instead of a {@code BlockState}.
 *
 * <p>The actual {@code BlockState}-aware call site in {@link MeteorEntity}
 * computes {@code isAir} via {@code BlockState.isAir()} and {@code isDragonImmune}
 * via {@code state.is(BlockTags.DRAGON_IMMUNE)} before delegating here.
 */
public final class MeteorImpactLogic {
    private MeteorImpactLogic() {}

    /**
     * @param isAir          whether the impact block's state is air
     * @param isDragonImmune whether the impact block is in {@code #minecraft:dragon_immune}
     * @return {@code true} if the impact block should be replaced with magma
     */
    public static boolean shouldReplaceWithMagma(boolean isAir, boolean isDragonImmune) {
        if (isAir) return false;
        if (isDragonImmune) return false;
        return true;
    }
}
```

- [ ] **Step 4: Run tests, verify pass**

Run: `./gradlew :wildwest:test --tests com.tweeks.wildwest.entity.projectile.MeteorImpactLogicTest`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/projectile/MeteorImpactLogic.java \
        wildwest/src/test/java/com/tweeks/wildwest/entity/projectile/MeteorImpactLogicTest.java
git commit -m "feat(wildwest): MeteorImpactLogic pure helper for magma replacement"
```

---

## Task 6: `MeteorEntity` projectile

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/projectile/MeteorEntity.java`

- [ ] **Step 1: Write the entity**

Create `wildwest/src/main/java/com/tweeks/wildwest/entity/projectile/MeteorEntity.java`:

```java
package com.tweeks.wildwest.entity.projectile;

import com.tweeks.wildwest.WildWestDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Meteor projectile. Used in two flavors:
 *  <ul>
 *      <li>Herobrine summons — spawned 30 blocks above ground, falls straight
 *          down via gravity. {@code directHitDamage} stays at default 6.</li>
 *      <li>Meteor Staff — spawned at player eye, velocity = look × 1.5,
 *          falls in an arc. {@code directHitDamage} set to 20 for 10-heart hits.</li>
 *  </ul>
 *
 * <p>On impact (block or entity) the meteor:
 *  <ol>
 *      <li>Replaces the impact block with magma (unless dragon-immune)</li>
 *      <li>Sets adjacent flammable air to fire</li>
 *      <li>Deals AoE damage to nearby entities (excluding any directly-hit entity)</li>
 *      <li>Plays explosion sound + lava/smoke particles</li>
 *  </ol>
 *
 * <p>NO actual {@code level.explode(...)} call — the design forbids block
 * destruction beyond the single impact tile.
 */
public class MeteorEntity extends ThrowableItemProjectile {

    public static final int DEFAULT_DIRECT_DAMAGE = 6;
    public static final int AOE_DAMAGE = 6;
    public static final double AOE_RADIUS = 2.0;

    private int directHitDamage = DEFAULT_DIRECT_DAMAGE;

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIRE_CHARGE;
    }

    public void setDirectHitDamage(int damage) {
        this.directHitDamage = damage;
    }

    public int getDirectHitDamage() {
        return this.directHitDamage;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(this.level() instanceof ServerLevel sl)) return;

        Entity directlyHit = result.getEntity();
        if (directlyHit instanceof LivingEntity living) {
            living.invulnerableTime = 0;
            living.hurtServer(sl,
                WildWestDamageTypes.meteor(this.getOwner() == null ? this : this.getOwner()),
                this.directHitDamage);
        }

        applyImpact(sl, result.getLocation(), directlyHit);
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(this.level() instanceof ServerLevel sl)) return;

        applyImpact(sl, result.getLocation(), null);
        this.discard();
    }

    /**
     * @param sl              the server level
     * @param impactLocation  precise impact point
     * @param directlyHitEntity entity to exclude from AoE (or {@code null} for
     *                          block hits)
     */
    private void applyImpact(ServerLevel sl, Vec3 impactLocation, Entity directlyHitEntity) {
        BlockPos impactPos = BlockPos.containing(impactLocation);
        BlockState impactState = sl.getBlockState(impactPos);

        boolean replace = MeteorImpactLogic.shouldReplaceWithMagma(
            impactState.isAir(),
            impactState.is(BlockTags.DRAGON_IMMUNE));

        if (replace) {
            sl.setBlockAndUpdate(impactPos, Blocks.MAGMA_BLOCK.defaultBlockState());
        }

        // Adjacent fire (4-neighborhood horizontal).
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = impactPos.relative(dir);
            if (sl.getBlockState(neighbor).isAir()
                && BaseFireBlock.canBePlacedAt(sl, neighbor, Direction.UP)) {
                sl.setBlockAndUpdate(neighbor, Blocks.FIRE.defaultBlockState());
            }
        }

        // AoE: damage other entities; explicitly exclude the directly-hit entity
        // (do NOT rely on invulnerableTime — fire-typed sources can bypass i-frames).
        AABB aoeBox = AABB.ofSize(impactLocation, AOE_RADIUS * 2, AOE_RADIUS * 2, AOE_RADIUS * 2);
        List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, aoeBox,
            e -> e != directlyHitEntity && e != this && e.position().distanceTo(impactLocation) <= AOE_RADIUS);
        for (LivingEntity victim : victims) {
            victim.hurtServer(sl, WildWestDamageTypes.meteorAoe(sl), AOE_DAMAGE);
        }

        // Particles + sound.
        sl.sendParticles(ParticleTypes.LAVA,
            impactLocation.x, impactLocation.y, impactLocation.z,
            24, 0.5, 0.3, 0.5, 0.0);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE,
            impactLocation.x, impactLocation.y, impactLocation.z,
            16, 0.5, 0.3, 0.5, 0.02);
        sl.playSound(null, impactPos, SoundEvents.GENERIC_EXPLODE.value(),
            SoundSource.HOSTILE, 1.0f, 0.8f);
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

If the compile fails on `hurtServer` (signature drift), check the `BulletEntity:54` call site for the correct shape and adjust.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/projectile/MeteorEntity.java
git commit -m "feat(wildwest): MeteorEntity projectile with magma+fire impact"
```

---

## Task 7: Register `METEOR` entity type + renderer

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java`

- [ ] **Step 1: Add the import + DeferredHolder in `ModEntities.java`**

Add the import at the top (alphabetical with the others):
```java
import com.tweeks.wildwest.entity.projectile.MeteorEntity;
```

Add the registration after `STEVE_STACKER` (around line 81), before `TAINTED_VIAL_PROJECTILE`:

```java
    public static final DeferredHolder<EntityType<?>, EntityType<MeteorEntity>> METEOR =
        ENTITY_TYPES.register("meteor", () -> EntityType.Builder.<MeteorEntity>of(
                MeteorEntity::new, MobCategory.MISC)
            .sized(0.5f, 0.5f)
            .clientTrackingRange(64)
            .updateInterval(2)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "meteor"))));
```

The `clientTrackingRange(64)` is required so clients see the meteor during its 30-block fall (spec risk note); `updateInterval(2)` keeps movement smooth without flooding the network.

- [ ] **Step 2: Register the renderer in `ClientSetup.java`**

In `registerRenderers`, add after the `TAINTED_VIAL_PROJECTILE` line:

```java
        event.registerEntityRenderer(ModEntities.METEOR.get(), ThrownItemRenderer::new);
```

`ThrownItemRenderer` is already imported at the top of the file (used by `TAINTED_VIAL_PROJECTILE`); reuse it. The meteor renders as a `Items.FIRE_CHARGE` icon billboard via `MeteorEntity.getDefaultItem()`.

- [ ] **Step 3: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java \
        wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java
git commit -m "feat(wildwest): register METEOR entity type + ThrownItemRenderer"
```

---

## Task 8: `HerobrineTeleportTarget` pure helper + tests

Pure math for the teleport-destination decision (close gap / open gap / random reposition). Tested with explicit positions.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineTeleportTarget.java`
- Create: `wildwest/src/test/java/com/tweeks/wildwest/entity/ai/HerobrineTeleportTargetTest.java`

- [ ] **Step 1: Write failing tests**

Create `wildwest/src/test/java/com/tweeks/wildwest/entity/ai/HerobrineTeleportTargetTest.java`:

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.ai.HerobrineTeleportTarget.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HerobrineTeleportTargetTest {

    /** Deterministic seed-driven RNG stand-in for the helper. */
    static class FixedRng implements HerobrineTeleportTarget.Rng {
        private final double[] values;
        private int idx = 0;
        FixedRng(double... values) { this.values = values; }
        @Override public double nextDouble() { return values[idx++ % values.length]; }
    }

    @Test
    void farTarget_closesGap() {
        // self at (0,0), target at (20, 0); distance > 12 → close gap, dest 6–10 from target.
        Result r = HerobrineTeleportTarget.pick(0, 0, 20, 0, new FixedRng(0.5));
        // dx = 20, distance = 20. Closing means dest = target - unit*(6 + 0.5*4) = (20,0) - (1,0)*8 = (12, 0).
        assertEquals(12.0, r.x(), 0.001);
        assertEquals(0.0, r.z(), 0.001);
        assertEquals(Result.Mode.CLOSE_GAP, r.mode());
    }

    @Test
    void closeTarget_opensGap() {
        // self at (0,0), target at (3, 0); distance < 5 → open gap, dest 8–12 from target.
        Result r = HerobrineTeleportTarget.pick(0, 0, 3, 0, new FixedRng(0.0));
        // Reverse direction unit = (-1, 0). 8 + 0.0*4 = 8 blocks from target along reverse.
        // dest = (3,0) + (-1,0)*8 = (-5, 0).
        assertEquals(-5.0, r.x(), 0.001);
        assertEquals(0.0, r.z(), 0.001);
        assertEquals(Result.Mode.OPEN_GAP, r.mode());
    }

    @Test
    void midTarget_randomReposition() {
        // self at (0,0), target at (8, 0); distance == 8, in [5,12] → random direction.
        // FixedRng returns angleFraction = 0.0 → angle = 0 → unit (1, 0).
        // distance = 8 + 0.5*8 = 12 (second nextDouble call).
        Result r = HerobrineTeleportTarget.pick(0, 0, 8, 0, new FixedRng(0.0, 0.5));
        // dest = self + (1,0)*12 = (12, 0).
        assertEquals(12.0, r.x(), 0.001);
        assertEquals(0.0, r.z(), 0.001);
        assertEquals(Result.Mode.RANDOM_REPOSITION, r.mode());
    }

    @Test
    void targetCoincidentWithSelf_doesNotDivideByZero() {
        // Edge case: self == target. Should fall through to random reposition (mid range).
        Result r = HerobrineTeleportTarget.pick(0, 0, 0, 0, new FixedRng(0.25, 0.5));
        // No NaN; finite result.
        assertTrue(Double.isFinite(r.x()));
        assertTrue(Double.isFinite(r.z()));
        assertEquals(Result.Mode.RANDOM_REPOSITION, r.mode());
    }
}
```

- [ ] **Step 2: Run tests, verify they fail**

Run: `./gradlew :wildwest:test --tests com.tweeks.wildwest.entity.ai.HerobrineTeleportTargetTest`
Expected: FAIL — compilation error referencing `HerobrineTeleportTarget`.

- [ ] **Step 3: Implement the helper**

Create `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineTeleportTarget.java`:

```java
package com.tweeks.wildwest.entity.ai;

/**
 * Pure horizontal-position picker for Herobrine's teleport goal. Three modes:
 *  <ul>
 *      <li>{@code CLOSE_GAP}  — distance > 12: blink 6–10 blocks toward target along self→target unit vector</li>
 *      <li>{@code OPEN_GAP}   — distance < 5: blink 8–12 blocks away along the inverse unit vector</li>
 *      <li>{@code RANDOM_REPOSITION} — distance in [5, 12]: pick random direction, distance 8–16 blocks from self</li>
 *  </ul>
 *
 * <p>Y is intentionally not handled here — the calling goal snaps to ground via
 * the level heightmap (Minecraft API).
 */
public final class HerobrineTeleportTarget {

    private HerobrineTeleportTarget() {}

    /** RNG abstraction so tests can drive deterministic sequences. */
    public interface Rng {
        double nextDouble();
    }

    public record Result(double x, double z, Mode mode) {
        public enum Mode { CLOSE_GAP, OPEN_GAP, RANDOM_REPOSITION }
    }

    public static Result pick(double selfX, double selfZ, double targetX, double targetZ, Rng rng) {
        double dx = targetX - selfX;
        double dz = targetZ - selfZ;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist > 12.0) {
            // Close the gap: dest = target - unit*(6 + r*4)
            double offset = 6.0 + rng.nextDouble() * 4.0;
            double ux = dx / dist;
            double uz = dz / dist;
            return new Result(targetX - ux * offset, targetZ - uz * offset, Result.Mode.CLOSE_GAP);
        }

        if (dist < 5.0 && dist > 1.0e-6) {
            // Open the gap: dest = target + (-unit)*(8 + r*4)
            double offset = 8.0 + rng.nextDouble() * 4.0;
            double ux = dx / dist;
            double uz = dz / dist;
            return new Result(targetX - ux * offset, targetZ - uz * offset, Result.Mode.OPEN_GAP);
        }

        // Random reposition (also covers the dist≈0 degenerate case).
        double angle = rng.nextDouble() * 2.0 * Math.PI;
        double dest = 8.0 + rng.nextDouble() * 8.0;
        return new Result(selfX + Math.cos(angle) * dest, selfZ + Math.sin(angle) * dest,
            Result.Mode.RANDOM_REPOSITION);
    }
}
```

- [ ] **Step 4: Run tests, verify pass**

Run: `./gradlew :wildwest:test --tests com.tweeks.wildwest.entity.ai.HerobrineTeleportTargetTest`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineTeleportTarget.java \
        wildwest/src/test/java/com/tweeks/wildwest/entity/ai/HerobrineTeleportTargetTest.java
git commit -m "feat(wildwest): HerobrineTeleportTarget pure destination math"
```

---

## Task 9: `HerobrineEntity` skeleton

Bare entity class — extends `Monster`, declares attributes, equipment, basic overrides. Goals + boss bar + lifecycle come in later tasks; everything that requires the entity to exist gets stubbed first so registration succeeds.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineEntity.java`

- [ ] **Step 1: Write the skeleton**

Create `wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineEntity.java`:

```java
package com.tweeks.wildwest.entity;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerBossEvent;

/**
 * Apex boss mob. Singleton across the whole server (see {@link HerobrineSavedData}).
 * Spawns rarely at night under open sky in overworld biomes; teleports often,
 * mixes melee netherite-sword swings, vanilla {@code LightningBolt} casts, and
 * a {@link com.tweeks.wildwest.entity.projectile.MeteorEntity} barrage that
 * creates magma + fire hazard zones around him.
 */
public class HerobrineEntity extends Monster {

    private final ServerBossEvent bossBar;

    public HerobrineEntity(EntityType<? extends HerobrineEntity> type, Level level) {
        super(type, level);
        this.bossBar = new ServerBossEvent(
            Mth.createInsecureUUID(this.random),
            Component.translatable("entity.wildwest.herobrine"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS);
        this.setPersistenceRequired();
        this.setCustomName(Component.translatable("entity.wildwest.herobrine"));
        this.setCustomNameVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 200.0)
            .add(Attributes.MOVEMENT_SPEED, 0.35)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) return true;
        return super.isInvulnerableTo(level, source);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);

        // Apply Sharpness V + Fire Aspect II using whatever enchant API the
        // active NeoForge version exposes via the level's registry access.
        var registries = this.level().registryAccess();
        var enchRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = enchRegistry.getOrThrow(Enchantments.SHARPNESS);
        Holder<Enchantment> fireAspect = enchRegistry.getOrThrow(Enchantments.FIRE_ASPECT);
        sword.enchant(sharpness, 5);
        sword.enchant(fireAspect, 2);

        this.setItemSlot(EquipmentSlot.MAINHAND, sword);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.10f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMBIENT_CAVE.value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        // Slightly muted ambient breath; matches the "creepy distant" feel.
        return 0.6f;
    }

    @Override
    public int getBaseExperienceReward(ServerLevel level) {
        return 100;
    }

    public ServerBossEvent getBossBar() {
        return this.bossBar;
    }
}
```

> **Note on enchant API:** if `ItemStack.enchant(Holder<Enchantment>, int)` is unavailable in this NeoForge build, fall back to `EnchantmentHelper.setEnchantments(...)` populated from a `Map<Holder<Enchantment>, Integer>`. The `Enchantments.SHARPNESS` / `Enchantments.FIRE_ASPECT` ResourceKeys are stable and can be looked up the same way.

> **Note on `getAmbientSound`:** vanilla returns `SoundEvent` directly; if `SoundEvents.AMBIENT_CAVE` is a `Holder<SoundEvent>` in this version, call `.value()` (as shown). If not, drop the `.value()`.

- [ ] **Step 2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL. If `isInvulnerableTo` signature differs (e.g., no `ServerLevel` param), match `Monster`'s declared method.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineEntity.java
git commit -m "feat(wildwest): HerobrineEntity skeleton — attributes + equipment"
```

---

## Task 10: Register `HEROBRINE` entity type + attributes

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`

- [ ] **Step 1: Add import + DeferredHolder in `ModEntities.java`**

Add the import (alphabetical with existing entity imports):
```java
import com.tweeks.wildwest.entity.HerobrineEntity;
```

Add the registration after `STEVE_STACKER` and before `METEOR` (so all "boss" entities cluster):

```java
    public static final DeferredHolder<EntityType<?>, EntityType<HerobrineEntity>> HEROBRINE =
        ENTITY_TYPES.register("herobrine", () -> EntityType.Builder.<HerobrineEntity>of(
                HerobrineEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "herobrine"))));
```

- [ ] **Step 2: Add attribute registration in `WildWestMod.java`**

Add the import:
```java
import com.tweeks.wildwest.entity.HerobrineEntity;
```

In `registerEntityAttributes`, append after the `STEVE_STACKER` line:

```java
        event.put(ModEntities.HEROBRINE.get(), HerobrineEntity.createAttributes().build());
```

- [ ] **Step 3: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java \
        wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java
git commit -m "feat(wildwest): register HEROBRINE entity type + attributes"
```

---

## Task 11: `HerobrineSpawnEggItem` + register + creative tab

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/item/HerobrineSpawnEggItem.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`

- [ ] **Step 1: Implement the spawn egg subclass**

Create `wildwest/src/main/java/com/tweeks/wildwest/item/HerobrineSpawnEggItem.java`:

```java
package com.tweeks.wildwest.item;

import com.tweeks.wildwest.entity.HerobrineEntity;
import com.tweeks.wildwest.entity.HerobrineSavedData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Singleton-aware spawn egg for Herobrine.
 *
 * <p><b>Spawn branch:</b> if no Herobrine is alive, delegates to vanilla
 * {@link SpawnEggItem#useOn(UseOnContext)} which spawns the entity normally.
 * The entity's {@code finalizeSpawn} (Task 13) sets the singleton flag.
 *
 * <p><b>Teleport branch:</b> if a Herobrine is already alive, teleports the
 * existing entity to the click location instead of spawning a duplicate. Egg
 * is not consumed in this branch.
 *
 * <p><b>Dimension gate:</b> rejects use outside Overworld (vanilla
 * {@code SpawnEggItem.useOn} doesn't check dimension; without this gate the
 * egg would spawn Herobrine in the Nether/End on first use).
 */
public class HerobrineSpawnEggItem extends SpawnEggItem {

    public HerobrineSpawnEggItem(EntityType<? extends Entity> type,
                                 int primaryColor, int secondaryColor,
                                 Properties properties) {
        super(type, primaryColor, secondaryColor, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;

        Player player = context.getPlayer();
        MinecraftServer server = sl.getServer();
        if (server == null) return InteractionResult.PASS;

        // Dimension gate.
        if (level.dimension() != Level.OVERWORLD) {
            if (player != null) {
                player.displayClientMessage(
                    Component.translatable("item.wildwest.herobrine_spawn_egg.overworld_only"),
                    true);
            }
            return InteractionResult.FAIL;
        }

        HerobrineSavedData saved = HerobrineSavedData.get(server);

        if (!saved.isAlive()) {
            // Spawn branch — delegate to vanilla. The entity's finalizeSpawn
            // sets the singleton flag.
            return super.useOn(context);
        }

        // Teleport branch.
        var dimension = saved.getDimension();
        if (dimension == null || dimension != Level.OVERWORLD) {
            // Stored dimension is invalid or non-overworld — treat as unloaded.
            return refuseAway(player);
        }
        ServerLevel target = server.getLevel(dimension);
        if (target == null) return refuseAway(player);

        Entity existing = target.getEntity(saved.getCurrentId());
        if (!(existing instanceof HerobrineEntity hb)) {
            return refuseAway(player);
        }

        double tx = context.getClickedPos().getX() + 0.5;
        double ty = context.getClickedPos().getY() + 1;
        double tz = context.getClickedPos().getZ() + 0.5;

        // Source-side particle burst.
        sl.sendParticles(ParticleTypes.PORTAL,
            hb.getX(), hb.getY() + 1.0, hb.getZ(), 16, 0.5, 1.0, 0.5, 0.0);

        hb.teleportTo(tx, ty, tz);

        // Destination-side particle burst + sound.
        sl.sendParticles(ParticleTypes.PORTAL, tx, ty + 1.0, tz, 16, 0.5, 1.0, 0.5, 0.0);
        sl.playSound(null, tx, ty, tz, SoundEvents.ENDERMAN_TELEPORT.value(),
            SoundSource.HOSTILE, 0.8f, 1.0f);

        return InteractionResult.SUCCESS;
    }

    private static InteractionResult refuseAway(Player player) {
        if (player != null) {
            player.displayClientMessage(
                Component.translatable("item.wildwest.herobrine_spawn_egg.away"),
                true);
        }
        return InteractionResult.FAIL;
    }
}
```

> **Note:** if `SoundEvents.ENDERMAN_TELEPORT` is a direct `SoundEvent` rather than a `Holder<SoundEvent>` in this version, drop the `.value()` call.

- [ ] **Step 2: Register the spawn egg in `Registration.java`**

Add the import:
```java
import com.tweeks.wildwest.item.HerobrineSpawnEggItem;
```

Add the registration after `STEVE_STACKER_SPAWN_EGG`:

```java
    public static final DeferredItem<HerobrineSpawnEggItem> HEROBRINE_SPAWN_EGG = ITEMS.registerItem(
        "herobrine_spawn_egg",
        properties -> new HerobrineSpawnEggItem(
            ModEntities.HEROBRINE.get(), 0x3F0000, 0xFFFFFF, properties),
        p -> p.spawnEgg(ModEntities.HEROBRINE.get()));
```

In the `WILDWEST_TAB` builder's `displayItems` block, add after `output.accept(STEVE_STACKER_SPAWN_EGG.get());`:

```java
                    output.accept(HEROBRINE_SPAWN_EGG.get());
```

- [ ] **Step 3: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/HerobrineSpawnEggItem.java \
        wildwest/src/main/java/com/tweeks/wildwest/Registration.java
git commit -m "feat(wildwest): Herobrine spawn egg — singleton-aware teleport behavior"
```

---

## Task 12: Spawn placement + biome modifier

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`
- Create: `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/herobrine_spawns.json`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/spawning/HerobrineSpawnRules.java`

- [ ] **Step 1: Implement the composite spawn predicate**

Create `wildwest/src/main/java/com/tweeks/wildwest/spawning/HerobrineSpawnRules.java`:

```java
package com.tweeks.wildwest.spawning;

import com.tweeks.wildwest.entity.HerobrineEntity;
import com.tweeks.wildwest.entity.HerobrineSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Spawn-rules predicate composing:
 *  <ol>
 *      <li>Vanilla {@link Monster#checkMonsterSpawnRules} (light ≤ 7 etc.)</li>
 *      <li>Open-sky requirement (mythic flavor)</li>
 *      <li>Singleton gate from {@link HerobrineSavedData}</li>
 *  </ol>
 */
public final class HerobrineSpawnRules {
    private HerobrineSpawnRules() {}

    public static boolean checkSpawnRules(EntityType<? extends Monster> type,
                                          ServerLevelAccessor level,
                                          MobSpawnType spawnType,
                                          BlockPos pos,
                                          RandomSource random) {
        if (!Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }
        if (!level.canSeeSky(pos)) {
            return false;
        }
        MinecraftServer server = level.getLevel().getServer();
        if (server == null) return false;
        if (HerobrineSavedData.get(server).isAlive()) return false;
        return true;
    }
}
```

> **Note:** if `MobSpawnType` was renamed to `EntitySpawnReason` (or similar) in this NeoForge build, mirror whatever signature `Monster::checkMonsterSpawnRules` declares — copy from the existing `WALKER` registration site at `WildWestMod:42-45`.

- [ ] **Step 2: Register the spawn placement in `WildWestMod.java`**

Add to the `RegisterSpawnPlacementsEvent` listener (after the `STEVE_STACKER` block):

```java
            event.register(ModEntities.HEROBRINE.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.wildwest.spawning.HerobrineSpawnRules::checkSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
```

- [ ] **Step 3: Create the biome modifier JSON**

Create `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/herobrine_spawns.json`:

```json
{
  "type": "neoforge:add_spawns",
  "biomes": "#minecraft:is_overworld",
  "spawners": {
    "type": "wildwest:herobrine",
    "maxCount": 1,
    "minCount": 1,
    "weight": 1
  }
}
```

The `biomes` field accepts a tag reference (`#minecraft:is_overworld`) directly; this matches NeoForge's biome-modifier schema.

- [ ] **Step 4: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java \
        wildwest/src/main/java/com/tweeks/wildwest/spawning/HerobrineSpawnRules.java \
        wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/herobrine_spawns.json
git commit -m "feat(wildwest): Herobrine rare night overworld spawn — open-sky + singleton gate"
```

---

## Task 13: Singleton lifecycle in `HerobrineEntity`

Wire up the entity to the SavedData: claim singleton on spawn, release on death/discard, always clear boss bar on remove.

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineEntity.java`

- [ ] **Step 1: Add singleton lifecycle overrides**

Add these imports near the top of `HerobrineEntity.java`:

```java
import javax.annotation.Nullable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
```

> If your NeoForge version still uses `MobSpawnType` instead of `EntitySpawnReason`, mirror the type used by `Mob.finalizeSpawn` in this build (check by looking at `ThiefEntity:150` or any other entity that overrides `finalizeSpawn` — copy the exact signature).

Add these methods to the class (anywhere, but conventionally after `populateDefaultEquipmentSlots`):

```java
    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
                                        DifficultyInstance difficulty,
                                        EntitySpawnReason reason,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);

        // Claim singleton — anchor on overworld SavedData regardless of caller dimension.
        var server = level.getLevel().getServer();
        if (server != null) {
            HerobrineSavedData saved = HerobrineSavedData.get(server);
            if (saved.isAlive() && !this.getUUID().equals(saved.getCurrentId())) {
                // Another Herobrine already alive — discard this duplicate.
                this.discard();
                return result;
            }
            saved.setAlive(this.getUUID(), level.getLevel().dimension());
        }
        return result;
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (this.level() instanceof ServerLevel sl) {
            var server = sl.getServer();
            if (server != null) {
                HerobrineSavedData saved = HerobrineSavedData.get(server);
                if (this.getUUID().equals(saved.getCurrentId())) {
                    saved.clear();
                }
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        // Always clear the boss bar — a stuck-flag scenario or forced discard
        // must not leave the bar visible to clients server-side.
        this.bossBar.removeAllPlayers();

        // Only clear the singleton flag for "real" removals. Chunk unload is
        // not a death.
        if (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED) {
            if (this.level() instanceof ServerLevel sl) {
                var server = sl.getServer();
                if (server != null) {
                    HerobrineSavedData saved = HerobrineSavedData.get(server);
                    if (this.getUUID().equals(saved.getCurrentId())) {
                        saved.clear();
                    }
                }
            }
        }
        super.remove(reason);
    }
```

> **Note on `EntitySpawnReason` vs `MobSpawnType`:** these renamed across the 1.21.x → 26.x transitions. Use whichever the `Mob.finalizeSpawn` override expects; signature mismatch is a hard-compile failure.

- [ ] **Step 2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineEntity.java
git commit -m "feat(wildwest): Herobrine singleton lifecycle (finalizeSpawn/die/remove)"
```

---

## Task 14: Boss bar wiring

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineEntity.java`

- [ ] **Step 1: Add the boss-bar overrides + aiStep**

Add the import:
```java
import net.minecraft.server.level.ServerPlayer;
```

Add to the class (after the lifecycle methods from Task 13):

```java
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;
        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
    }

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

- [ ] **Step 2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineEntity.java
git commit -m "feat(wildwest): Herobrine boss bar wiring (red, progress)"
```

---

## Task 15: `HerobrineTeleportGoal`

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineTeleportGoal.java`

- [ ] **Step 1: Implement the goal**

Create `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineTeleportGoal.java`:

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.HerobrineEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Teleports Herobrine every ~4 seconds. Distance-aware:
 *  <ul>
 *      <li>far → close gap toward target</li>
 *      <li>close → open gap away from target</li>
 *      <li>mid → random horizontal reposition</li>
 *  </ul>
 *
 * <p>Pure destination math is in {@link HerobrineTeleportTarget} (unit-tested).
 * This goal handles Y-snapping via the heightmap and clearance validation.
 */
public class HerobrineTeleportGoal extends Goal {

    private static final int COOLDOWN_TICKS = 80; // 4 s at 20 tps
    private static final int CLEARANCE_RETRIES = 5;

    private final HerobrineEntity boss;
    private int cooldown = 0;

    public HerobrineTeleportGoal(HerobrineEntity boss) {
        this.boss = boss;
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.boss.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return false; // one-shot
    }

    @Override
    public void start() {
        LivingEntity target = this.boss.getTarget();
        if (target == null) return;
        this.cooldown = COOLDOWN_TICKS;

        if (!(this.boss.level() instanceof ServerLevel sl)) return;

        // Pick destination via pure helper.
        HerobrineTeleportTarget.Rng rng = this.boss.getRandom()::nextDouble;
        HerobrineTeleportTarget.Result picked = HerobrineTeleportTarget.pick(
            this.boss.getX(), this.boss.getZ(),
            target.getX(), target.getZ(), rng);

        // Y-snap + clearance.
        double destX = picked.x();
        double destZ = picked.z();
        double destY = -1;
        for (int retry = 0; retry < CLEARANCE_RETRIES; retry++) {
            BlockPos topPos = sl.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(destX, this.boss.getY(), destZ));
            int candidateY = topPos.getY();

            // Need 2 blocks of vertical air clearance for the entity bbox.
            if (sl.getBlockState(topPos).isAir()
                && sl.getBlockState(topPos.above()).isAir()) {
                destY = candidateY;
                break;
            }
            // Retry with a small lateral perturbation.
            double angle = this.boss.getRandom().nextDouble() * 2.0 * Math.PI;
            destX += Math.cos(angle) * 1.5;
            destZ += Math.sin(angle) * 1.5;
        }
        if (destY < 0) {
            // Couldn't find a valid spot — skip this teleport, cooldown stays reset.
            return;
        }

        // Source-side particles.
        sl.sendParticles(ParticleTypes.PORTAL,
            this.boss.getX(), this.boss.getY() + 1.0, this.boss.getZ(),
            16, 0.5, 1.0, 0.5, 0.0);

        // Move + zero fall distance.
        this.boss.teleportTo(destX, destY, destZ);
        this.boss.fallDistance = 0;

        // Destination-side particles + sound.
        sl.sendParticles(ParticleTypes.PORTAL, destX, destY + 1.0, destZ,
            16, 0.5, 1.0, 0.5, 0.0);
        sl.playSound(null, destX, destY, destZ, SoundEvents.ENDERMAN_TELEPORT.value(),
            SoundSource.HOSTILE, 0.8f, 1.0f);
    }
}
```

> **Note:** if `boss.fallDistance` is no longer a public field in this version, swap to `boss.resetFallDistance()` if it exists, or call `setOnGround(true)` as a fallback.

- [ ] **Step 2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineTeleportGoal.java
git commit -m "feat(wildwest): HerobrineTeleportGoal — distance-aware blink"
```

---

## Task 16: `HerobrineMeteorGoal`

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineMeteorGoal.java`

- [ ] **Step 1: Implement the goal**

Create `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineMeteorGoal.java`:

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.entity.HerobrineEntity;
import com.tweeks.wildwest.entity.projectile.MeteorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Spawns one {@link MeteorEntity} every ~8 s, falling straight down from above
 * a random ring point 6–14 blocks around Herobrine. Ceiling-aware: in
 * dimensions with {@link DimensionType#hasCeiling()} (Nether-style), uses a
 * downward raycast to find an air pocket beneath solid blocks instead of
 * spawning above the bedrock ceiling.
 */
public class HerobrineMeteorGoal extends Goal {

    private static final int COOLDOWN_TICKS = 160; // 8 s
    private static final double RING_MIN = 6.0;
    private static final double RING_MAX = 14.0;
    private static final int FALL_HEIGHT = 30;
    private static final int CEILING_MIN_POCKET = 4;

    private final HerobrineEntity boss;
    private int cooldown = 0;

    public HerobrineMeteorGoal(HerobrineEntity boss) {
        this.boss = boss;
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.boss.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() { return false; }

    @Override
    public void start() {
        this.cooldown = COOLDOWN_TICKS;
        if (!(this.boss.level() instanceof ServerLevel sl)) return;

        // Pick a ring point (uniform angle, uniform radius in [RING_MIN, RING_MAX]).
        double angle = this.boss.getRandom().nextDouble() * 2.0 * Math.PI;
        double radius = RING_MIN + this.boss.getRandom().nextDouble() * (RING_MAX - RING_MIN);
        double xzX = this.boss.getX() + Math.cos(angle) * radius;
        double xzZ = this.boss.getZ() + Math.sin(angle) * radius;

        Vec3 spawn = pickSpawnY(sl, xzX, xzZ);
        if (spawn == null) return; // no valid pocket; cooldown still resets

        MeteorEntity meteor = ModEntities.METEOR.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (meteor == null) return;
        meteor.setPos(spawn.x, spawn.y, spawn.z);
        meteor.setDeltaMovement(0.0, -0.4, 0.0); // initial downward kick to avoid hover before gravity kicks in
        meteor.setOwner(this.boss);
        sl.addFreshEntity(meteor);
    }

    /**
     * Compute the spawn Y. Caps at {@code level.getMaxY() - 1}. In ceilinged
     * dimensions, finds the first air pocket of {@link #CEILING_MIN_POCKET}
     * vertical clearance via downward scan from {@code maxY}.
     */
    private static Vec3 pickSpawnY(ServerLevel sl, double xzX, double xzZ) {
        BlockPos seed = BlockPos.containing(xzX, sl.getMaxY(), xzZ);
        int maxY = sl.getMaxY() - 1;

        if (sl.dimensionType().hasCeiling()) {
            // Scan down for first run of CEILING_MIN_POCKET air blocks above a non-air block.
            int pocketRun = 0;
            for (int y = maxY; y > sl.getMinY() + 1; y--) {
                BlockPos p = new BlockPos(seed.getX(), y, seed.getZ());
                if (sl.getBlockState(p).isAir()) {
                    pocketRun++;
                    if (pocketRun >= CEILING_MIN_POCKET) {
                        return new Vec3(xzX, y, xzZ);
                    }
                } else {
                    pocketRun = 0;
                }
            }
            return null;
        }

        // Open-sky path: 30 blocks above the surface, capped at maxY.
        BlockPos top = sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, seed);
        int spawnY = Math.min(top.getY() + FALL_HEIGHT, maxY);
        return new Vec3(xzX, spawnY, xzZ);
    }
}
```

> **Note:** if `EntitySpawnReason` is named `MobSpawnType` in this NeoForge build, rename the import + the `.create(sl, ...)` call accordingly. The `EntityType.create(Level, EntitySpawnReason)` factory is the standard way to instantiate registered entities.

> **Note on `getMaxY()` / `getMinY()`:** in 26.1.2 these are the canonical level methods; if compile fails, try `level.getHeight()` / `level.getMinBuildHeight()` as version-mapped equivalents.

- [ ] **Step 2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineMeteorGoal.java
git commit -m "feat(wildwest): HerobrineMeteorGoal — ring-spawn meteors with ceiling-aware Y"
```

---

## Task 17: `HerobrineLightningGoal`

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineLightningGoal.java`

- [ ] **Step 1: Implement the goal**

Create `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineLightningGoal.java`:

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.HerobrineEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Casts a vanilla {@link LightningBolt} at the target every ~5 s, when the
 * target is &gt; 8 blocks away. Vanilla bolt damage (5) + auto-fire-on-flammable
 * is acceptable as-is.
 */
public class HerobrineLightningGoal extends Goal {

    private static final int COOLDOWN_TICKS = 100; // 5 s
    private static final double MIN_RANGE_SQ = 8.0 * 8.0;

    private final HerobrineEntity boss;
    private int cooldown = 0;

    public HerobrineLightningGoal(HerobrineEntity boss) {
        this.boss = boss;
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.boss.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (this.boss.distanceToSqr(target) <= MIN_RANGE_SQ) return false;
        return this.boss.hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() { return false; }

    @Override
    public void start() {
        LivingEntity target = this.boss.getTarget();
        if (target == null) return;
        this.cooldown = COOLDOWN_TICKS;

        if (!(this.boss.level() instanceof ServerLevel sl)) return;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl,
            net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (bolt == null) return;
        bolt.moveTo(target.getX(), target.getY(), target.getZ());
        // setVisualOnly(false) is the default — full damage + fire.
        sl.addFreshEntity(bolt);
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineLightningGoal.java
git commit -m "feat(wildwest): HerobrineLightningGoal — vanilla bolt at >8-block targets"
```

---

## Task 18: `HerobrineMeleeGoal`

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineMeleeGoal.java`

- [ ] **Step 1: Implement the goal**

Create `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineMeleeGoal.java`:

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.HerobrineEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Engages target in melee at distance ≤ 4 blocks. Disables at distance &gt; 5
 * to prevent thrashing across the threshold. Otherwise vanilla behavior.
 */
public class HerobrineMeleeGoal extends MeleeAttackGoal {

    private static final double ENGAGE_RANGE_SQ = 4.0 * 4.0;
    private static final double DISENGAGE_RANGE_SQ = 5.0 * 5.0;

    public HerobrineMeleeGoal(HerobrineEntity boss) {
        super(boss, 1.0, true);
    }

    @Override
    public boolean canUse() {
        if (!super.canUse()) return false;
        LivingEntity target = this.mob.getTarget();
        if (target == null) return false;
        return this.mob.distanceToSqr(target) <= ENGAGE_RANGE_SQ;
    }

    @Override
    public boolean canContinueToUse() {
        if (!super.canContinueToUse()) return false;
        LivingEntity target = this.mob.getTarget();
        if (target == null) return false;
        return this.mob.distanceToSqr(target) <= DISENGAGE_RANGE_SQ;
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineMeleeGoal.java
git commit -m "feat(wildwest): HerobrineMeleeGoal — engages at ≤4 blocks, disengages at >5"
```

---

## Task 19: Wire goals into `HerobrineEntity.registerGoals`

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineEntity.java`

- [ ] **Step 1: Add `registerGoals` override**

Add the imports:
```java
import com.tweeks.wildwest.entity.ai.HerobrineLightningGoal;
import com.tweeks.wildwest.entity.ai.HerobrineMeleeGoal;
import com.tweeks.wildwest.entity.ai.HerobrineMeteorGoal;
import com.tweeks.wildwest.entity.ai.HerobrineTeleportGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
```

Add the method (insert anywhere; conventionally after `createAttributes()`):

```java
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HerobrineMeleeGoal(this));
        this.goalSelector.addGoal(2, new HerobrineLightningGoal(this));
        this.goalSelector.addGoal(2, new HerobrineMeteorGoal(this));
        this.goalSelector.addGoal(3, new HerobrineTeleportGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
```

- [ ] **Step 2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run all tests**

Run: `./gradlew :wildwest:test`
Expected: BUILD SUCCESSFUL with all existing tests + 3 new tests (HerobrineState, MeteorImpactLogic, HerobrineTeleportTarget) passing.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineEntity.java
git commit -m "feat(wildwest): HerobrineEntity.registerGoals — wire AI goal stack"
```

---

## Task 20: Loot table

**Files:**
- Create: `wildwest/src/main/resources/data/wildwest/loot_table/entities/herobrine.json`

- [ ] **Step 1: Write the loot table JSON**

Create `wildwest/src/main/resources/data/wildwest/loot_table/entities/herobrine.json`:

```json
{
  "type": "minecraft:entity",
  "pools": [
    {
      "rolls": 1.0,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:diamond_block"
        }
      ]
    },
    {
      "rolls": 1.0,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "wildwest:meteor_staff"
        }
      ]
    }
  ]
}
```

The netherite sword is dropped via the equipment-slot `setDropChance(MAINHAND, 0.10f)` set in `populateDefaultEquipmentSlots`, NOT via the loot table — same pattern as vanilla mob equipment.

- [ ] **Step 2: Verify JSON parses**

Run: `python3 -c "import json; json.load(open('wildwest/src/main/resources/data/wildwest/loot_table/entities/herobrine.json'))"`
Expected: silent success.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/resources/data/wildwest/loot_table/entities/herobrine.json
git commit -m "feat(wildwest): Herobrine loot table — diamond block + Meteor Staff"
```

---

## Task 21: `MeteorStaffItem` + register

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/item/MeteorStaffItem.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`

- [ ] **Step 1: Implement the item**

Create `wildwest/src/main/java/com/tweeks/wildwest/item/MeteorStaffItem.java`:

```java
package com.tweeks.wildwest.item;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.entity.projectile.MeteorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Player-fired meteor weapon dropped by Herobrine. Right-click fires a
 * {@link MeteorEntity} along the player's look direction with a 3-second
 * cooldown and 10-heart direct-hit damage. Unbreakable, stack size 1, EPIC.
 */
public class MeteorStaffItem extends Item {

    public static final int COOLDOWN_TICKS = 60; // 3 s
    public static final int DIRECT_HIT_DAMAGE = 20; // 10 hearts
    public static final double FIRE_VELOCITY = 1.5;

    public MeteorStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level instanceof ServerLevel sl) {
            MeteorEntity meteor = ModEntities.METEOR.get().create(sl,
                net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (meteor != null) {
                Vec3 eye = player.getEyePosition();
                meteor.setPos(eye.x, eye.y, eye.z);
                Vec3 look = player.getLookAngle().scale(FIRE_VELOCITY);
                meteor.setDeltaMovement(look);
                meteor.setOwner(player);
                meteor.setDirectHitDamage(DIRECT_HIT_DAMAGE);
                sl.addFreshEntity(meteor);

                sl.playSound(null, eye.x, eye.y, eye.z,
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        player.swing(hand);

        return InteractionResultHolder.consume(stack);
    }
}
```

> **Note:** `EntitySpawnReason.TRIGGERED` (or `.SPAWN_EGG`, etc.) — pick whichever value the enum exposes; the meteor doesn't make spawn-reason decisions.

> **Note:** `SoundEvents.FIRECHARGE_USE` may need `.value()` if it's a `Holder<SoundEvent>`. Match how `SoundEvents.PLAYER_HURT` is consumed in `HerobrineEntity.getHurtSound`.

- [ ] **Step 2: Register the item**

In `Registration.java`, add the import:
```java
import com.tweeks.wildwest.item.MeteorStaffItem;
import net.minecraft.world.item.Rarity;
```

Register after `HEROBRINE_SPAWN_EGG`:

```java
    public static final DeferredItem<MeteorStaffItem> METEOR_STAFF = ITEMS.registerItem(
        "meteor_staff",
        MeteorStaffItem::new,
        p -> p.stacksTo(1).rarity(Rarity.EPIC));
```

In `WILDWEST_TAB.displayItems`, add after `output.accept(HEROBRINE_SPAWN_EGG.get());`:

```java
                    output.accept(METEOR_STAFF.get());
```

- [ ] **Step 3: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/MeteorStaffItem.java \
        wildwest/src/main/java/com/tweeks/wildwest/Registration.java
git commit -m "feat(wildwest): MeteorStaffItem — fires MeteorEntity, 10-heart direct hit"
```

---

## Task 22: Meteor Staff item model + texture

**Files:**
- Create: `wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff.png` (16×16)
- Create: `wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff.json`

- [ ] **Step 1: Create the item model JSON**

Create `wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff.json`:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "wildwest:item/meteor_staff"
  }
}
```

`item/handheld` is the standard parent for held-in-hand items (ensures correct rotation in first-person view). Same parent used by `pistol.json` / `rifle.json` if present.

- [ ] **Step 2: Create the texture PNG (16×16)**

The texture must be a 16×16 PNG depicting an orange/red staff with a glowing tip. If you have access to an image editor, draw it. Otherwise, copy and recolor the existing `bandit_knife.png` or `billy_club.png` as a placeholder; the visual specifics are implementation detail per spec.

For automated placement, copy the `fire_charge` icon from the vanilla resource pack (16×16, recognizable orange-on-black) as a temporary stand-in and commit, with a follow-up TODO to replace.

A minimal placeholder method using ImageMagick (if installed):

```bash
convert -size 16x16 xc:transparent \
  -fill orange -draw "rectangle 7,2 8,11" \
  -fill red    -draw "circle 7,1 8,2" \
  wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff.png
```

If ImageMagick is unavailable, hand-draw the 16×16 PNG in any pixel-art tool and save to the path. Verify it's a valid PNG:

Run: `file wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff.png`
Expected: `PNG image data, 16 x 16, ...`

- [ ] **Step 3: Verify JSON parses**

Run: `python3 -c "import json; json.load(open('wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff.json'))"`
Expected: silent success.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff.png \
        wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff.json
git commit -m "feat(wildwest): Meteor Staff item model + placeholder texture"
```

---

## Task 23: Vendored Herobrine textures

**Files:**
- Create: `wildwest/src/main/resources/assets/wildwest/textures/entity/herobrine.png` (64×64, Steve variant w/ white eyes)
- Create: `wildwest/src/main/resources/assets/wildwest/textures/entity/herobrine_eyes.png` (64×64, white eyes on transparent)

- [ ] **Step 1: Source the base Herobrine skin**

The simplest path: copy the vendored Steve skin already in the repo (`wildwest/src/main/resources/assets/wildwest/textures/entity/steve_stacker.png`), then overwrite the eye pixels with full-white. The eye pixels in the standard 64×64 Steve UV layout are at:
- Right eye: pixels (10, 12) and (11, 12) on the front-of-head face
- Left eye: pixels (12, 12) and (13, 12) on the front-of-head face

Concretely:

```bash
cp wildwest/src/main/resources/assets/wildwest/textures/entity/steve_stacker.png \
   wildwest/src/main/resources/assets/wildwest/textures/entity/herobrine.png
```

Then open the copy in any pixel-art editor and paint the four eye pixels solid white. Save.

If a pixel-art editor isn't available, this can be scripted via Python + Pillow:

```bash
python3 - <<'PY'
from PIL import Image
src = Image.open("wildwest/src/main/resources/assets/wildwest/textures/entity/steve_stacker.png").convert("RGBA")
for x in (10, 11, 12, 13):
    src.putpixel((x, 12), (255, 255, 255, 255))
src.save("wildwest/src/main/resources/assets/wildwest/textures/entity/herobrine.png")
PY
```

Verify:

Run: `file wildwest/src/main/resources/assets/wildwest/textures/entity/herobrine.png`
Expected: `PNG image data, 64 x 64, ...`

- [ ] **Step 2: Create the emissive eyes overlay**

Same dimensions, fully transparent except the eye pixels. Via Python + Pillow:

```bash
python3 - <<'PY'
from PIL import Image
img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
for x in (10, 11, 12, 13):
    img.putpixel((x, 12), (255, 255, 255, 255))
img.save("wildwest/src/main/resources/assets/wildwest/textures/entity/herobrine_eyes.png")
PY
```

Verify:

Run: `file wildwest/src/main/resources/assets/wildwest/textures/entity/herobrine_eyes.png`
Expected: `PNG image data, 64 x 64, ...`

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/resources/assets/wildwest/textures/entity/herobrine.png \
        wildwest/src/main/resources/assets/wildwest/textures/entity/herobrine_eyes.png
git commit -m "feat(wildwest): vendored Herobrine textures (white-eye Steve + emissive overlay)"
```

---

## Task 24: `HerobrineRenderer` + `HerobrineEyesLayer` + register

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/HerobrineRenderer.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/HerobrineEyesLayer.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java`

- [ ] **Step 1: Implement the renderer**

Create `wildwest/src/main/java/com/tweeks/wildwest/client/HerobrineRenderer.java`:

```java
package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.entity.HerobrineEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class HerobrineRenderer
        extends HumanoidMobRenderer<HerobrineEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/herobrine.png");

    public HerobrineRenderer(EntityRendererProvider.Context context) {
        super(context,
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
            0.5F);
        this.addLayer(new HerobrineEyesLayer(this));
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
```

> **Note:** `ModelLayers.PLAYER` is the standard player skeleton layer (vanilla bakes it). If `HumanoidMobRenderer` requires a `<HumanoidModel<HumanoidRenderState>>` of a different generic shape in this version, mirror what `WalkerRenderer` does (since `WalkerEntity` also uses a humanoid model).

- [ ] **Step 2: Implement the eyes layer**

Create `wildwest/src/main/java/com/tweeks/wildwest/client/HerobrineEyesLayer.java`:

```java
package com.tweeks.wildwest.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.Identifier;

/**
 * Emissive overlay drawing the Herobrine eyes texture at full brightness.
 * Same pattern as vanilla {@code EyesLayer} but bound to a static texture.
 */
public class HerobrineEyesLayer
        extends RenderLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final RenderType EYES = RenderType.eyes(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/herobrine_eyes.png"));

    public HerobrineEyesLayer(RenderLayerParent<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight,
                       HumanoidRenderState state, float yaw, float pitch) {
        var buffer = buffers.getBuffer(EYES);
        this.getParentModel().renderToBuffer(pose, buffer, 0xF000F0, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
    }
}
```

> **Note:** the `render(...)` signature and `renderToBuffer(...)` overload may differ between MC versions. If compile fails, mirror an existing `RenderLayer` subclass in the codebase or vanilla. The intent is "draw the parent model with the eyes texture at full brightness via `RenderType.eyes`."

- [ ] **Step 3: Register the renderer in `ClientSetup.java`**

In `registerRenderers`, add after the `STEVE_STACKER` line:

```java
        event.registerEntityRenderer(ModEntities.HEROBRINE.get(), HerobrineRenderer::new);
```

No layer-definition registration is required — Herobrine reuses vanilla `ModelLayers.PLAYER` rather than defining its own.

- [ ] **Step 4: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/client/HerobrineRenderer.java \
        wildwest/src/main/java/com/tweeks/wildwest/client/HerobrineEyesLayer.java \
        wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java
git commit -m "feat(wildwest): HerobrineRenderer + emissive eyes layer + register"
```

---

## Task 25: End-to-end build, tests, manual checklist

**Files:** none modified.

- [ ] **Step 1: Run the full wildwest build**

Run: `./gradlew :wildwest:build`
Expected: BUILD SUCCESSFUL. All tests pass (existing + 3 new test classes added during this plan).

- [ ] **Step 2: Run the data-gen task one more time**

Run: `./gradlew :wildwest:runData`
Expected: BUILD SUCCESSFUL. `git status` shows no changes (datagen output should already be committed in Task 1).

If new generated files appear, inspect them, commit if expected, investigate if not.

- [ ] **Step 3: Manual integration test — creative client**

Run: `./gradlew :wildwest:runClient`
Wait for the client to launch into a world.

Perform the manual checklist (one section per group; tick as completed):

  - [ ] **Spawn egg + singleton**
    - Switch to creative; open creative inventory; locate Herobrine Spawn Egg in the Wild West tab.
    - Right-click on a grass block in the overworld. Herobrine spawns. Boss bar appears (red, "Herobrine").
    - Use the egg again at a different spot. Existing Herobrine teleports to the new spot (egg not consumed).

  - [ ] **Cross-dimension refusal**
    - Build a nether portal or use `/execute in minecraft:the_nether ...`. With the egg, right-click in the Nether. Above-hotbar message: "Herobrine belongs only to the surface…". Egg not consumed. No spawn.

  - [ ] **Combat — melee**
    - Get within 4 blocks of Herobrine. He swings the netherite sword. Player ignites (Fire Aspect II) and takes damage.

  - [ ] **Combat — lightning**
    - Run more than 8 blocks away. Within ~5 s, lightning strikes the player position.

  - [ ] **Combat — meteors**
    - Stay within line-of-sight of Herobrine. Every ~8 s, a meteor falls from the sky in a 6–14 block ring around him. Impact creates a magma block; adjacent grass/wood catches fire. Standing near the impact takes 6 damage.

  - [ ] **Combat — teleport**
    - Observe Herobrine teleports every ~4 s. Particles + Enderman teleport sound at both endpoints.

  - [ ] **Death + drops**
    - Kill Herobrine (use `/effect give @s minecraft:strength 60 9` for help). On death:
      - 1 diamond block drops.
      - 1 Meteor Staff drops.
      - With ~10% chance, an enchanted netherite sword drops (Sharpness V + Fire Aspect II intact).
      - 100 XP awarded.
    - Boss bar disappears. Singleton flag clears (try the egg again — spawns a new one rather than teleporting).

  - [ ] **Meteor Staff**
    - Right-click the staff in mid-air. A meteor flies out in look direction with slight arc.
    - Aim at a mob; on direct hit, the mob takes 20 damage. Magma + fire impact spawns.
    - Cooldown bar visible in hotbar for 3 s after firing.

  - [ ] **Save / load persistence**
    - With Herobrine alive, save and reload the world. Boss reloads with same HP, boss bar reattaches, singleton flag remains true.

  - [ ] **Natural spawn (sanity-check)**
    - Open `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/herobrine_spawns.json` and temporarily change `"weight": 1` → `"weight": 1000`. Reload world. Run several in-game nights in a plains biome at light level ≤ 7 with open sky. Verify a Herobrine spawns naturally. **Revert the weight change before committing.**

- [ ] **Step 4: Tag completion**

If all manual checks pass, commit any final tweaks discovered during testing (e.g., texture refinements, sound tuning) and tag the work:

```bash
git status
# If clean, no commit needed.
git log --oneline -25
# Sanity-check the commit chain.
```

The branch is now ready for code review or merge.

---

## Self-review notes

This plan covers every section of the spec:

| Spec section | Tasks |
|---|---|
| Entity (HP, attributes, attack, equipment) | 9, 13 |
| Singleton mechanic (SavedData, lifecycle) | 3, 4, 13 |
| Spawning (biome modifier, predicate, egg) | 11, 12 |
| Combat AI (4 goals + targets) | 8, 15, 16, 17, 18, 19 |
| MeteorEntity (impact + AoE + ceiling-aware Y) | 5, 6, 7, 16 |
| Equipment (sword + drop chance) | 9 |
| Visuals (textures, renderer, eyes layer) | 23, 24 |
| Boss bar | 14 |
| Loot (diamond block + meteor staff) | 20 |
| Meteor Staff item | 21, 22 |
| Damage type | 1 |
| Sounds | 9 (in entity) |
| Lang | 2 |
| File-level changes (all listed paths) | distributed across tasks |
| Testing strategy (3 unit test classes + manual checklist) | 3, 5, 8, 25 |

No placeholders, no "implement appropriately" hand-waves. Every code block is complete and executable. Type names (`HerobrineState`, `HerobrineSavedData`, `HerobrineSpawnEggItem`, `MeteorEntity`, `MeteorImpactLogic`, `HerobrineTeleportTarget`, all four goal classes, `MeteorStaffItem`, `HerobrineRenderer`, `HerobrineEyesLayer`, `HerobrineSpawnRules`) are consistent across tasks. Method signatures (`isAlive()`, `getCurrentId()`, `getDimension()`, `setAlive(...)`, `clear()`, `pick(...)`, `shouldReplaceWithMagma(...)`, `setDirectHitDamage(...)`) match between task definitions and call sites.

Two areas where the active NeoForge version may force minor signature swaps (called out inline in the relevant tasks):

1. `EntitySpawnReason` vs `MobSpawnType` — used in `finalizeSpawn`, `EntityType.create(...)`. Mirror whatever `Mob.finalizeSpawn` declares in this build.
2. `SoundEvents.X` may be `Holder<SoundEvent>` or direct `SoundEvent` depending on version — adjust `.value()` calls accordingly. Use `getHurtSound`'s reference resolution as the in-build canonical.

These are mechanical compile-error fixes, not logic changes.
