# Grim Reaper Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the fifth apex boss to the `:wildwest` mod — the Grim Reaper — a singleton overworld-night spawn that raises Sharpness-3 skeletons and lifts players for fall damage, dropping a dual-purpose Reaper Scythe that doubles as a melee weapon and a right-click skeleton-summoner for player-owned minions that fight hostiles and mine precious ores.

**Architecture:** Mirrors the four existing apex-boss patterns (Herobrine, Agent, Null, Steve Stacker). Sits on top of the existing `BossSingletonSavedData` base — no shared-infra changes needed. New entity (`GrimReaperEntity extends Monster`), new minion entity (`ScytheSkeletonEntity extends Skeleton`), new dual-purpose item (`ReaperScytheItem`), new spawn egg, new spawn-rules class, new biome modifier JSON, new loot table, new renderer (reusing `HumanoidMobRenderer`), four new AI goals (two for reaper, three for scythe-skeleton).

**Tech Stack:** Java 25 / NeoForge (see `gradle.properties` for `neo_version`). JUnit 5 Jupiter for unit tests. Gradle commands: `./gradlew :wildwest:test`, `./gradlew :wildwest:build`.

---

## File Structure

**New files (production code):**

```
wildwest/src/main/java/com/tweeks/wildwest/
├── entity/
│   ├── GrimReaperEntity.java            — boss mob, singleton, boss bar
│   ├── GrimReaperSavedData.java         — thin BossSingletonSavedData subclass
│   ├── ScytheSkeletonEntity.java        — player-summoned minion (extends Skeleton)
│   └── ai/
│       ├── GrimReaperRaiseDeadGoal.java   — summons Sharpness-3 skeletons
│       ├── GrimReaperSoulLiftGoal.java    — vertical-launch CC attack
│       ├── ScytheSkeletonFollowOwnerGoal.java — wolf-style follow + teleport-recall
│       ├── ScytheSkeletonMineOreGoal.java     — idle precious-ore mining
│       └── ScytheSkeletonTargetHostilesGoal.java — target Monster (not own-team)
├── item/
│   ├── GrimReaperSpawnEggItem.java     — singleton-aware spawn egg (overworld-only)
│   └── ReaperScytheItem.java           — melee + right-click summon, cooldown + cap
├── spawning/
│   └── GrimReaperSpawnRules.java        — predicate (sky + light + singleton)
└── client/
    └── GrimReaperRenderer.java          — humanoid renderer + texture binding
```

**New files (resources):**

```
wildwest/src/main/resources/
├── data/wildwest/neoforge/biome_modifier/grim_reaper_overworld_spawns.json
├── data/wildwest/loot_table/entities/grim_reaper.json
└── assets/wildwest/
    ├── textures/entity/grim_reaper.png
    ├── textures/item/reaper_scythe.png
    ├── textures/item/grim_reaper_spawn_egg.png
    ├── models/item/reaper_scythe.json
    └── models/item/grim_reaper_spawn_egg.json
```

**New files (tests):**

```
wildwest/src/test/java/com/tweeks/wildwest/
├── entity/
│   └── ScytheSkeletonOwnerScanTest.java  — countMinionsOwnedBy unit tests
└── item/
    └── ReaperScytheItemTest.java         — constants smoke test
```

**Modified files:**

```
wildwest/src/main/java/com/tweeks/wildwest/
├── ModEntities.java                       — register GRIM_REAPER + SCYTHE_SKELETON
├── Registration.java                      — register GRIM_REAPER_SPAWN_EGG + REAPER_SCYTHE + add to tab
├── WildWestMod.java                       — register attributes + spawn placement
└── client/ClientSetup.java                — register renderers for both entities

wildwest/src/main/resources/assets/wildwest/lang/en_us.json   — 11 new lang strings
```

---

## Task 1: Lang strings + entity type registration scaffolding

**Files:**
- Modify: `wildwest/src/main/resources/assets/wildwest/lang/en_us.json`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java`

This task adds the lang strings (so display names resolve later) and registers placeholder entity types so other tasks can reference them via `ModEntities.GRIM_REAPER` and `ModEntities.SCYTHE_SKELETON`. The entity classes themselves don't exist yet; we'll create stub classes in the next steps so the registration compiles.

- [ ] **Step 1.1: Add lang strings**

Add the 11 entries to `wildwest/src/main/resources/assets/wildwest/lang/en_us.json`. The file is JSON — insert these entries inside the top-level object, near existing wildwest entries (alphabetical insertion is fine, or grouped at the bottom; follow whatever style the file uses):

```json
"entity.wildwest.grim_reaper": "Grim Reaper",
"entity.wildwest.scythe_skeleton": "Bone Servant",
"item.wildwest.grim_reaper_spawn_egg": "Grim Reaper Spawn Egg",
"item.wildwest.grim_reaper_spawn_egg.away": "The Reaper drifts beyond reach…",
"item.wildwest.grim_reaper_spawn_egg.wrong_dimension": "The Reaper has no business here…",
"item.wildwest.grim_reaper_spawn_egg.different_dimension": "The Reaper walks elsewhere…",
"item.wildwest.reaper_scythe": "Reaper Scythe",
"item.wildwest.reaper_scythe.cap_reached": "Your bone servants are already three…",
"item.wildwest.reaper_scythe.tooltip.melee": "Melee: 3 hearts",
"item.wildwest.reaper_scythe.tooltip.summon": "Right-click: summon bone servant (cap 3, 5s cooldown)",
"item.wildwest.reaper_scythe.tooltip.servant": "Servants fight hostiles and mine precious ores nearby"
```

- [ ] **Step 1.2: Create stub `GrimReaperEntity` class**

Create `wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperEntity.java` as a minimal stub so `ModEntities` can reference it:

```java
package com.tweeks.wildwest.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Fifth apex boss for the :wildwest mod. Singleton, overworld-night spawn.
 * Glass-cannon minion master that raises Sharpness-3 skeletons and uses
 * Soul Lift to launch players for fall damage. Drops the Reaper Scythe.
 *
 * <p>Stub — full behavior added in later plan tasks.
 */
public class GrimReaperEntity extends Monster {
    public GrimReaperEntity(EntityType<? extends GrimReaperEntity> type, Level level) {
        super(type, level);
    }
}
```

- [ ] **Step 1.3: Create stub `ScytheSkeletonEntity` class**

Create `wildwest/src/main/java/com/tweeks/wildwest/entity/ScytheSkeletonEntity.java`:

```java
package com.tweeks.wildwest.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;

/**
 * Player-summoned minion summoned by the Reaper Scythe's right-click cast.
 * Persistent, owner-tagged via {@link java.util.UUID}, follows owner and
 * fights hostiles. Idle behavior: mines precious ores within 3 blocks.
 *
 * <p>Stub — full behavior added in later plan tasks.
 */
public class ScytheSkeletonEntity extends Skeleton {
    public ScytheSkeletonEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }
}
```

- [ ] **Step 1.4: Register entity types in `ModEntities`**

Open `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java`. Find the existing `NULL_RIFT` registration (around line 125). Add these registrations after it (or after the apex-boss block — place alongside HEROBRINE/AGENT/NULL for thematic grouping):

```java
public static final DeferredHolder<EntityType<?>, EntityType<GrimReaperEntity>> GRIM_REAPER =
    ENTITY_TYPES.register("grim_reaper", () -> EntityType.Builder.<GrimReaperEntity>of(
            GrimReaperEntity::new, MobCategory.MONSTER)
        .sized(0.6f, 2.2f)
        .clientTrackingRange(10)
        .build(ResourceKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "grim_reaper"))));

public static final DeferredHolder<EntityType<?>, EntityType<ScytheSkeletonEntity>> SCYTHE_SKELETON =
    ENTITY_TYPES.register("scythe_skeleton", () -> EntityType.Builder.<ScytheSkeletonEntity>of(
            ScytheSkeletonEntity::new, MobCategory.CREATURE)
        .sized(0.6f, 1.99f)
        .clientTrackingRange(8)
        .build(ResourceKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "scythe_skeleton"))));
```

The exact `EntityType.Builder` chain may need to adapt to whatever pattern is used by the existing `NULL` registration in this file — mirror that builder shape exactly. If the surrounding code imports `EntityType.Builder.of(...)` differently, follow suit. Also add any imports the file requires (`GrimReaperEntity`, `ScytheSkeletonEntity`).

- [ ] **Step 1.5: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL. If it fails, check that the entity stubs are syntactically correct and that the `ModEntities` imports include the new entity classes.

- [ ] **Step 1.6: Commit**

```bash
git add wildwest/src/main/resources/assets/wildwest/lang/en_us.json \
        wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperEntity.java \
        wildwest/src/main/java/com/tweeks/wildwest/entity/ScytheSkeletonEntity.java \
        wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java
git commit -m "feat(wildwest): Grim Reaper entity-type registration + lang strings"
```

---

## Task 2: `GrimReaperSavedData` singleton record

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperSavedData.java`

Mirrors `NullSavedData` exactly — thin subclass of `BossSingletonSavedData` with its own `FILE_ID`, codec, and `SavedDataType`. The shared base provides all behavior; this class just registers the identifier.

- [ ] **Step 2.1: Create the file**

Write `wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperSavedData.java`:

```java
package com.tweeks.wildwest.entity;

import com.mojang.serialization.Codec;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-server singleton record for the Grim Reaper. Anchored to the
 * overworld's data storage so reads/writes from any dimension consult the
 * same record. Independent from other apex bosses' SavedData — all five
 * can be alive simultaneously.
 */
public final class GrimReaperSavedData extends BossSingletonSavedData {

    private static final String FILE_ID = "wildwest_grim_reaper";

    public static final Codec<GrimReaperSavedData> CODEC = buildCodec(GrimReaperSavedData::new);

    public static final SavedDataType<GrimReaperSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, FILE_ID),
        GrimReaperSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    public GrimReaperSavedData() {}

    public static GrimReaperSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
```

- [ ] **Step 2.2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2.3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperSavedData.java
git commit -m "feat(wildwest): GrimReaperSavedData singleton record"
```

---

## Task 3: Spawn rules + biome modifier + spawn placement

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/spawning/GrimReaperSpawnRules.java`
- Create: `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/grim_reaper_overworld_spawns.json`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`

- [ ] **Step 3.1: Create spawn rules**

Write `wildwest/src/main/java/com/tweeks/wildwest/spawning/GrimReaperSpawnRules.java`:

```java
package com.tweeks.wildwest.spawning;

import com.tweeks.wildwest.entity.GrimReaperSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Spawn-rules predicate for the Grim Reaper. Overworld-only, night,
 * open sky, strict darkness (light &lt; 4), singleton-gated.
 */
public final class GrimReaperSpawnRules {
    private GrimReaperSpawnRules() {}

    public static boolean checkSpawnRules(EntityType<? extends Monster> type,
                                          ServerLevelAccessor level,
                                          EntitySpawnReason reason,
                                          BlockPos pos,
                                          RandomSource random) {
        if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
            return false;
        }

        if (level.getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }

        if (!level.canSeeSky(pos)) {
            return false;
        }

        if (level.getMaxLocalRawBrightness(pos) >= 4) {
            return false;
        }

        MinecraftServer server = level.getLevel().getServer();
        if (server == null) return false;
        if (GrimReaperSavedData.get(server).isAlive()) return false;

        return true;
    }
}
```

> **Note on `getMaxLocalRawBrightness`:** the exact method name on `LevelAccessor` / `LevelReader` varies between NeoForge versions (`getMaxLocalRawBrightness`, `getRawBrightness`, etc.). Mirror whatever the existing wildwest spawn rules use — `NullSpawnRules` uses `Monster::checkMonsterSpawnRules` which internally checks light, but Grim Reaper wants a stricter threshold. If `getMaxLocalRawBrightness` doesn't exist in the active version, drop the extra light check (the vanilla ≤7 from `checkMonsterSpawnRules` is acceptable as a fallback).

- [ ] **Step 3.2: Create biome modifier JSON**

Write `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/grim_reaper_overworld_spawns.json`:

```json
{
  "type": "neoforge:add_spawns",
  "biomes": "#minecraft:is_overworld",
  "spawners": {
    "type": "wildwest:grim_reaper",
    "maxCount": 1,
    "minCount": 1,
    "weight": 1
  }
}
```

- [ ] **Step 3.3: Register spawn placement in `WildWestMod`**

Open `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`. Find the existing `registerSpawnPlacementsEvent` method (search for the `NullSpawnRules::checkSpawnRules` line). Add this block right after the Null registration:

```java
event.register(ModEntities.GRIM_REAPER.get(),
    SpawnPlacementTypes.ON_GROUND,
    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
    com.tweeks.wildwest.spawning.GrimReaperSpawnRules::checkSpawnRules,
    RegisterSpawnPlacementsEvent.Operation.REPLACE);
```

Mirror the exact signature of the Null registration (operation argument may be `OR` instead of `REPLACE` — match the existing call). If `SpawnPlacementTypes` and `Heightmap.Types` aren't already imported, add the imports.

- [ ] **Step 3.4: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3.5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/spawning/GrimReaperSpawnRules.java \
        wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/grim_reaper_overworld_spawns.json \
        wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java
git commit -m "feat(wildwest): Grim Reaper spawn rules + biome modifier + placement"
```

---

## Task 4: `GrimReaperEntity` core (attributes, singleton lifecycle, boss bar)

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperEntity.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`

Build out the entity with HP/move attributes, the singleton claim/release lifecycle, the boss bar, and sounds. AI goals come in later tasks.

- [ ] **Step 4.1: Replace stub with full entity class**

Replace `wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperEntity.java` with:

```java
package com.tweeks.wildwest.entity;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Fifth apex boss for the :wildwest mod. Singleton, overworld-night spawn.
 * Glass-cannon (100 HP) minion master that raises Sharpness-3 skeletons
 * via {@link com.tweeks.wildwest.entity.ai.GrimReaperRaiseDeadGoal} and
 * lifts players for fall damage via
 * {@link com.tweeks.wildwest.entity.ai.GrimReaperSoulLiftGoal}.
 * Drops the Reaper Scythe.
 */
public class GrimReaperEntity extends Monster {

    private final ServerBossEvent bossBar;

    public GrimReaperEntity(EntityType<? extends GrimReaperEntity> type, Level level) {
        super(type, level);
        this.bossBar = new ServerBossEvent(
            Mth.createInsecureUUID(this.random),
            Component.translatable("entity.wildwest.grim_reaper"),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_10);
        this.setPersistenceRequired();
        this.setCustomName(Component.translatable("entity.wildwest.grim_reaper"));
        this.setCustomNameVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) return true;
        return super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
                                        DifficultyInstance difficulty,
                                        EntitySpawnReason reason,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);
        MinecraftServer server = level.getLevel().getServer();
        if (server != null) {
            GrimReaperSavedData saved = GrimReaperSavedData.get(server);
            if (saved.isAlive() && !this.getUUID().equals(saved.getCurrentId())) {
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
            MinecraftServer server = sl.getServer();
            if (server != null) {
                GrimReaperSavedData saved = GrimReaperSavedData.get(server);
                if (this.getUUID().equals(saved.getCurrentId())) {
                    saved.clear();
                }
            }
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        this.bossBar.removeAllPlayers();
        if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
            if (this.level() instanceof ServerLevel sl) {
                MinecraftServer server = sl.getServer();
                if (server != null) {
                    GrimReaperSavedData saved = GrimReaperSavedData.get(server);
                    if (this.getUUID().equals(saved.getCurrentId())) {
                        saved.clear();
                    }
                }
            }
        }
        super.remove(reason);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5f;
    }

    @Override
    public int getBaseExperienceReward(ServerLevel level) {
        return 80;
    }

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
}
```

> **Note on `isInvulnerableTo` signature:** NeoForge 26.x may use `isInvulnerableTo(DamageSource)` (no `ServerLevel` arg) — mirror whatever shape `HerobrineEntity` uses for its fire immunity. The 2-arg version is the current vanilla shape; if `HerobrineEntity` uses 1-arg, adapt accordingly.

> **Note on `causeFallDamage` signature:** signature may be `(float, float, DamageSource)` instead of `(double, float, DamageSource)` — mirror `NullEntity`'s override exactly.

- [ ] **Step 4.2: Register attributes in `WildWestMod`**

Open `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`. Find the existing `registerAttributes` event handler (search for `NullEntity.createAttributes().build()`). Add this line after the Null line:

```java
event.put(ModEntities.GRIM_REAPER.get(), GrimReaperEntity.createAttributes().build());
```

Add the `GrimReaperEntity` import if not already present.

- [ ] **Step 4.3: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL. If `isInvulnerableTo` or `causeFallDamage` signatures don't match, adjust the override to match the actual vanilla signature in the active NeoForge version (look at how `HerobrineEntity` / `NullEntity` handle these).

- [ ] **Step 4.4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperEntity.java \
        wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java
git commit -m "feat(wildwest): GrimReaperEntity core (attributes, singleton, boss bar)"
```

---

## Task 5: Loot table

**Files:**
- Create: `wildwest/src/main/resources/data/wildwest/loot_table/entities/grim_reaper.json`

The scythe item registration comes in Task 8; for now we use the eventual item ID `wildwest:reaper_scythe` in the loot table. Datapacks load lazily, so a forward reference is fine — the loot table won't be invoked until the entity dies after the scythe registration is wired up.

- [ ] **Step 5.1: Write loot table JSON**

```json
{
  "type": "minecraft:entity",
  "pools": [
    {
      "rolls": 1.0,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "wildwest:reaper_scythe"
        }
      ]
    },
    {
      "rolls": 1.0,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:bone",
          "functions": [
            {
              "function": "minecraft:set_count",
              "count": { "type": "minecraft:uniform", "min": 3.0, "max": 5.0 }
            }
          ]
        }
      ]
    },
    {
      "rolls": 1.0,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:soul_sand"
        }
      ]
    }
  ]
}
```

- [ ] **Step 5.2: Commit**

```bash
git add wildwest/src/main/resources/data/wildwest/loot_table/entities/grim_reaper.json
git commit -m "feat(wildwest): Grim Reaper loot table"
```

---

## Task 6: `ReaperScytheItem` skeleton (melee attributes + cooldown constant, summon stub)

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/item/ReaperScytheItem.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`

The item needs melee damage via `ItemAttributeModifiers` (so left-click works) and a `use(...)` override hooked for right-click. The actual summon-skeleton logic is left as a TODO marker we'll fill in Task 14 once `ScytheSkeletonEntity` is fully functional — for now `use(...)` just consumes the cooldown so the cap-check and cooldown plumbing can be verified end-to-end.

- [ ] **Step 6.1: Write the item class with melee + cooldown stub**

Create `wildwest/src/main/java/com/tweeks/wildwest/item/ReaperScytheItem.java`:

```java
package com.tweeks.wildwest.item;

import com.tweeks.wildwest.WildWestMod;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipDisplay;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

/**
 * Reaper Scythe — Grim Reaper's signature drop. Dual-purpose:
 *
 * <ul>
 *   <li>Left-click melee: 6 attack damage (3 hearts), ~1.0 attack speed.</li>
 *   <li>Right-click cast: summons one bone servant (cap 3 per owner,
 *       {@value #COOLDOWN_TICKS}t cooldown).</li>
 * </ul>
 *
 * <p>Stack 1, unbreakable, EPIC rarity. Right-click summon implementation
 * lives in {@link #use(Level, Player, InteractionHand)} — fully populated
 * once {@link com.tweeks.wildwest.entity.ScytheSkeletonEntity} is wired up.
 */
public class ReaperScytheItem extends Item {

    public static final int COOLDOWN_TICKS = 100; // 5 s
    public static final int MAX_MINIONS = 3;
    public static final double SUMMON_RANGE = 4.0;
    public static final double ATTACK_DAMAGE = 6.0;
    public static final double ATTACK_SPEED = -2.4; // ~1.0 atk/s

    private static final Identifier ATTACK_DAMAGE_ID =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "reaper_scythe_damage");
    private static final Identifier ATTACK_SPEED_ID =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "reaper_scythe_speed");

    public ReaperScytheItem(Properties properties) {
        super(properties
            .stacksTo(1)
            .rarity(net.minecraft.world.item.Rarity.EPIC)
            .component(DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ATTACK_DAMAGE_ID, ATTACK_DAMAGE,
                            AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(ATTACK_SPEED_ID, ATTACK_SPEED,
                            AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                    .build()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            // Client returns SUCCESS so the swing/animation fires; server
            // does the real work below.
            return InteractionResult.SUCCESS;
        }

        // Full summon implementation arrives in Task 14 once
        // ScytheSkeletonEntity supports owner UUID + spawn helper.
        // For now: stamp the cooldown so we can verify plumbing.
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                TooltipDisplay display, java.util.function.Consumer<Component> adder,
                                net.minecraft.world.item.TooltipFlag flag) {
        adder.accept(Component.translatable("item.wildwest.reaper_scythe.tooltip.melee")
            .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.translatable("item.wildwest.reaper_scythe.tooltip.summon")
            .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.translatable("item.wildwest.reaper_scythe.tooltip.servant")
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
```

> **Note on tooltip signature:** NeoForge 26.x has shuffled the `appendHoverText` signature several times — current shape uses `Item.TooltipContext`, `TooltipDisplay`, and `Consumer<Component>`. Mirror whatever existing items in the wildwest module use (`MeteorStaffItem`, `VoidMarkItem`). If the signature is different, adapt — the body is just three `.accept(Component.translatable(...))` calls.

- [ ] **Step 6.2: Register the item in `Registration`**

Open `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`. Find the existing `METEOR_STAFF` registration. Add this after it:

```java
public static final DeferredItem<ReaperScytheItem> REAPER_SCYTHE = ITEMS.registerItem(
    "reaper_scythe", ReaperScytheItem::new);
```

Then find the `WILDWEST_TAB` `displayItems` block. Add a line that appends `REAPER_SCYTHE.get()` next to other apex-boss-drop items like `METEOR_STAFF.get()`:

```java
output.accept(REAPER_SCYTHE.get());
```

Add the `ReaperScytheItem` import.

- [ ] **Step 6.3: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6.4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/ReaperScytheItem.java \
        wildwest/src/main/java/com/tweeks/wildwest/Registration.java
git commit -m "feat(wildwest): ReaperScytheItem melee + cooldown stub"
```

---

## Task 7: `GrimReaperSpawnEggItem`

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/item/GrimReaperSpawnEggItem.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`

Mirrors `NullSpawnEggItem` exactly but overworld-only. Singleton-aware: spawns if no Reaper alive, teleports the existing Reaper if alive in same dim, refuses with actionbar message in other cases.

- [ ] **Step 7.1: Write the spawn egg item**

Create `wildwest/src/main/java/com/tweeks/wildwest/item/GrimReaperSpawnEggItem.java`:

```java
package com.tweeks.wildwest.item;

import com.tweeks.wildwest.entity.GrimReaperEntity;
import com.tweeks.wildwest.entity.GrimReaperSavedData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Singleton-aware spawn egg for the Grim Reaper. Overworld-only.
 * Visual identity: {@link ParticleTypes#SOUL} (vs Null's ENCHANT).
 * Audio identity: {@link SoundEvents#SOUL_ESCAPE} on teleport.
 */
public class GrimReaperSpawnEggItem extends SpawnEggItem {

    public GrimReaperSpawnEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;

        Player player = context.getPlayer();
        MinecraftServer server = sl.getServer();
        if (server == null) return InteractionResult.PASS;

        if (level.dimension() != Level.OVERWORLD) {
            if (player != null) {
                player.sendOverlayMessage(
                    Component.translatable("item.wildwest.grim_reaper_spawn_egg.wrong_dimension"));
            }
            return InteractionResult.FAIL;
        }

        GrimReaperSavedData saved = GrimReaperSavedData.get(server);

        if (!saved.isAlive()) {
            return super.useOn(context);
        }

        var savedDim = saved.getDimension();
        if (savedDim == null) {
            return refuseAway(player);
        }
        if (!savedDim.equals(level.dimension())) {
            if (player != null) {
                player.sendOverlayMessage(
                    Component.translatable("item.wildwest.grim_reaper_spawn_egg.different_dimension"));
            }
            return InteractionResult.FAIL;
        }

        ServerLevel target = server.getLevel(savedDim);
        if (target == null) return refuseAway(player);

        Entity existing = target.getEntity(saved.getCurrentId());
        if (!(existing instanceof GrimReaperEntity reaper)) {
            return refuseAway(player);
        }

        double tx = context.getClickedPos().getX() + 0.5;
        double ty = context.getClickedPos().getY() + 1;
        double tz = context.getClickedPos().getZ() + 0.5;

        sl.sendParticles(ParticleTypes.SOUL,
            reaper.getX(), reaper.getY() + 1.0, reaper.getZ(),
            24, 0.5, 1.0, 0.5, 0.05);
        sl.playSound(null, reaper.getX(), reaper.getY(), reaper.getZ(),
            SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE, 1.0f, 0.7f);

        reaper.teleportTo(tx, ty, tz);

        sl.sendParticles(ParticleTypes.SOUL, tx, ty + 1.0, tz, 24, 0.5, 1.0, 0.5, 0.05);
        sl.playSound(null, tx, ty, tz, SoundEvents.SOUL_ESCAPE,
            SoundSource.HOSTILE, 1.0f, 0.7f);

        return InteractionResult.SUCCESS;
    }

    private static InteractionResult refuseAway(Player player) {
        if (player != null) {
            player.sendOverlayMessage(
                Component.translatable("item.wildwest.grim_reaper_spawn_egg.away"));
        }
        return InteractionResult.FAIL;
    }
}
```

- [ ] **Step 7.2: Register the spawn egg in `Registration`**

Open `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`. Find the `NULL_SPAWN_EGG` registration. Add right after it:

```java
public static final DeferredItem<GrimReaperSpawnEggItem> GRIM_REAPER_SPAWN_EGG = ITEMS.registerItem(
    "grim_reaper_spawn_egg",
    GrimReaperSpawnEggItem::new,
    p -> p.spawnEgg(ModEntities.GRIM_REAPER.get()));
```

Then in `WILDWEST_TAB` `displayItems`:

```java
output.accept(GRIM_REAPER_SPAWN_EGG.get());
```

Add imports.

- [ ] **Step 7.3: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7.4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/GrimReaperSpawnEggItem.java \
        wildwest/src/main/java/com/tweeks/wildwest/Registration.java
git commit -m "feat(wildwest): GrimReaperSpawnEggItem (overworld-only, singleton-aware)"
```

---

## Task 8: Renderer + texture + item models

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/GrimReaperRenderer.java`
- Create: `wildwest/src/main/resources/assets/wildwest/textures/entity/grim_reaper.png` (placeholder OK)
- Create: `wildwest/src/main/resources/assets/wildwest/textures/item/reaper_scythe.png` (placeholder)
- Create: `wildwest/src/main/resources/assets/wildwest/textures/item/grim_reaper_spawn_egg.png` (placeholder)
- Create: `wildwest/src/main/resources/assets/wildwest/models/item/reaper_scythe.json`
- Create: `wildwest/src/main/resources/assets/wildwest/models/item/grim_reaper_spawn_egg.json`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java`

For v1 the renderer reuses `HumanoidMobRenderer` with the standard humanoid model. The texture file can be a temporary 64×64 black placeholder — final art is a polish pass.

- [ ] **Step 8.1: Write the renderer**

Create `wildwest/src/main/java/com/tweeks/wildwest/client/GrimReaperRenderer.java`. Mirror the `HumanoidMobRenderer` generic chain used by `HerobrineRenderer` exactly — paste from there and change only the class name, the texture path, and the entity type. The skeleton:

```java
package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.entity.GrimReaperEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for the Grim Reaper. Reuses the vanilla humanoid model with a
 * custom texture. Future polish: bbmodel cloak overlay layer.
 */
public class GrimReaperRenderer
    extends HumanoidMobRenderer<GrimReaperEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/grim_reaper.png");

    public GrimReaperRenderer(EntityRendererProvider.Context context) {
        super(context,
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
            0.5f);
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }
}
```

> **Note:** the exact generic parameter triple and `createRenderState` requirements are version-sensitive. Open `HerobrineRenderer.java` and copy its class header / methods verbatim, swapping `Herobrine` for `GrimReaper` and the texture path. If `HerobrineRenderer` extends a different class (e.g. doesn't include the third `HumanoidModel<HumanoidRenderState>` type parameter), mirror that.

- [ ] **Step 8.2: Create placeholder textures**

For each of `grim_reaper.png`, `reaper_scythe.png`, `grim_reaper_spawn_egg.png`: use a 64×64 (entity) or 16×16 (items) PNG with any content. The simplest path:

```bash
# Use an existing placeholder PNG from the repo
cp wildwest/src/main/resources/assets/wildwest/textures/entity/herobrine.png \
   wildwest/src/main/resources/assets/wildwest/textures/entity/grim_reaper.png
cp wildwest/src/main/resources/assets/wildwest/textures/item/void_mark.png \
   wildwest/src/main/resources/assets/wildwest/textures/item/reaper_scythe.png
cp wildwest/src/main/resources/assets/wildwest/textures/item/null_spawn_egg.png \
   wildwest/src/main/resources/assets/wildwest/textures/item/grim_reaper_spawn_egg.png
```

Verify each source file exists first; if any are absent, copy from any other PNG of the right size in `assets/wildwest/textures/`.

- [ ] **Step 8.3: Create item models**

Write `wildwest/src/main/resources/assets/wildwest/models/item/reaper_scythe.json`:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "wildwest:item/reaper_scythe"
  }
}
```

Write `wildwest/src/main/resources/assets/wildwest/models/item/grim_reaper_spawn_egg.json`:

```json
{
  "parent": "minecraft:item/template_spawn_egg"
}
```

> If `null_spawn_egg.json` exists in the repo and uses a different template, mirror its `parent`.

- [ ] **Step 8.4: Register renderers in `ClientSetup`**

Open `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java`. Find the existing renderer registrations (search for `NullRenderer::new` or similar). Add these two lines:

```java
event.registerEntityRenderer(ModEntities.GRIM_REAPER.get(), GrimReaperRenderer::new);
event.registerEntityRenderer(ModEntities.SCYTHE_SKELETON.get(),
    net.minecraft.client.renderer.entity.SkeletonRenderer::new);
```

The `SkeletonRenderer` is vanilla and works directly — no custom renderer needed for the scythe-skeleton minion.

> Mirror the exact form of the existing `event.registerEntityRenderer` calls — the event class name and method name may differ in NeoForge 26.x.

- [ ] **Step 8.5: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL. If the renderer generic shape doesn't match, look at `HerobrineRenderer` or `AgentRenderer` and copy their class header verbatim.

- [ ] **Step 8.6: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/client/GrimReaperRenderer.java \
        wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java \
        wildwest/src/main/resources/assets/wildwest/textures/entity/grim_reaper.png \
        wildwest/src/main/resources/assets/wildwest/textures/item/reaper_scythe.png \
        wildwest/src/main/resources/assets/wildwest/textures/item/grim_reaper_spawn_egg.png \
        wildwest/src/main/resources/assets/wildwest/models/item/reaper_scythe.json \
        wildwest/src/main/resources/assets/wildwest/models/item/grim_reaper_spawn_egg.json
git commit -m "feat(wildwest): Grim Reaper renderer + placeholder textures + item models"
```

---

## Task 9: `GrimReaperRaiseDeadGoal`

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/GrimReaperRaiseDeadGoal.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperEntity.java`

Custom goal: every 10s, picks 2–3 ground positions near the target, plays a 1s emerge animation with dirt particles, then spawns vanilla skeletons holding Sharpness-3 iron swords with an NBT marker so they can be cleaned up on reaper death.

- [ ] **Step 9.1: Write the goal class**

Create `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/GrimReaperRaiseDeadGoal.java`:

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.GrimReaperEntity;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Heightmap;
import net.minecraft.world.level.block.Blocks;

/**
 * Grim Reaper signature ability. On a 10s cooldown, picks 2–3 ground
 * positions within 8 blocks of the current target, plays a 1s emerge
 * animation (dirt particles + sound), then spawns vanilla
 * {@link net.minecraft.world.entity.monster.Skeleton} entities holding
 * iron swords enchanted with {@link Enchantments#SHARPNESS} level 3.
 * Spawned skeletons carry an NBT marker ({@code wildwest:grim_reaper_minion})
 * so {@link GrimReaperEntity#die} can clean them up.
 */
public class GrimReaperRaiseDeadGoal extends Goal {

    public static final String MINION_NBT_KEY = "wildwest:grim_reaper_minion";

    private static final int COOLDOWN_TICKS = 200; // 10 s
    private static final int EMERGE_DELAY_TICKS = 20; // 1 s
    private static final int MAX_PLACEMENT_ATTEMPTS = 10;
    private static final double SUMMON_RADIUS = 8.0;
    private static final int MIN_SKELETONS = 2;
    private static final int MAX_SKELETONS = 3;

    private final GrimReaperEntity reaper;
    private int cooldown = 0;
    private int emergeTimer = 0;
    private final List<BlockPos> pendingSpawns = new ArrayList<>();

    public GrimReaperRaiseDeadGoal(GrimReaperEntity reaper) {
        this.reaper = reaper;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.reaper.getTarget();
        return target != null
            && target.isAlive()
            && this.reaper.distanceToSqr(target) <= 24.0 * 24.0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.emergeTimer > 0 && !this.pendingSpawns.isEmpty();
    }

    @Override
    public void start() {
        LivingEntity target = this.reaper.getTarget();
        if (target == null || !(this.reaper.level() instanceof ServerLevel sl)) {
            return;
        }

        this.pendingSpawns.clear();
        RandomSource rng = this.reaper.getRandom();
        int wantCount = MIN_SKELETONS + rng.nextInt(MAX_SKELETONS - MIN_SKELETONS + 1);

        for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS && this.pendingSpawns.size() < wantCount; attempt++) {
            double dx = (rng.nextDouble() * 2.0 - 1.0) * SUMMON_RADIUS;
            double dz = (rng.nextDouble() * 2.0 - 1.0) * SUMMON_RADIUS;
            BlockPos candidate = BlockPos.containing(
                target.getX() + dx,
                target.getY(),
                target.getZ() + dz);

            BlockPos surface = sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate);
            if (sl.getFluidState(surface).isEmpty() && sl.getBlockState(surface).isAir()) {
                this.pendingSpawns.add(surface);
            }
        }

        if (this.pendingSpawns.size() < MIN_SKELETONS) {
            this.pendingSpawns.clear();
            this.cooldown = COOLDOWN_TICKS / 2;
            return;
        }

        for (BlockPos pos : this.pendingSpawns) {
            sl.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                20, 0.3, 0.2, 0.3, 0.05);
            sl.playSound(null, pos, SoundEvents.ZOMBIE_VILLAGER_CONVERTED,
                SoundSource.HOSTILE, 0.6f, 0.6f);
        }

        this.emergeTimer = EMERGE_DELAY_TICKS;
    }

    @Override
    public void tick() {
        if (this.emergeTimer <= 0) return;
        if (!(this.reaper.level() instanceof ServerLevel sl)) return;

        // Continuous dirt emission during emerge phase
        if (this.emergeTimer % 4 == 0) {
            for (BlockPos pos : this.pendingSpawns) {
                sl.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                    pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5,
                    4, 0.2, 0.1, 0.2, 0.02);
            }
        }

        this.emergeTimer--;
        if (this.emergeTimer > 0) return;

        for (BlockPos pos : this.pendingSpawns) {
            Skeleton skeleton = net.minecraft.world.entity.EntityType.SKELETON.create(sl,
                EntitySpawnReason.MOB_SUMMONED);
            if (skeleton == null) continue;

            skeleton.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            applySharpness3(skeleton.registryAccess(), sword);
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, sword);
            skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.0f);

            skeleton.getPersistentData().putBoolean(MINION_NBT_KEY, true);

            LivingEntity target = this.reaper.getTarget();
            if (target != null) skeleton.setTarget(target);

            sl.addFreshEntity(skeleton);
        }

        this.pendingSpawns.clear();
        this.cooldown = COOLDOWN_TICKS;
    }

    @Override
    public void stop() {
        this.pendingSpawns.clear();
        if (this.emergeTimer > 0) {
            // Aborted mid-emerge — full cooldown anyway, don't allow infinite re-trigger.
            this.cooldown = COOLDOWN_TICKS;
            this.emergeTimer = 0;
        }
    }

    private static void applySharpness3(net.minecraft.core.HolderLookup.Provider registries, ItemStack stack) {
        var enchantRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        var sharpness = enchantRegistry.getOrThrow(Enchantments.SHARPNESS);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(sharpness, 3);
        stack.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }
}
```

> **Note on enchantment application:** `EnchantmentHelper.setEnchantments(...)` was renamed several times across NeoForge versions; the canonical 26.x form uses `ItemEnchantments.Mutable` as shown. If that class doesn't exist, fall back to `EnchantmentHelper.setEnchantments(map, stack)` with a `Map<Holder<Enchantment>, Integer>`. Mirror any existing enchantment-application code in the module before improvising.

> **Note on `EntitySpawnReason`:** if NeoForge 26.x uses `MobSpawnType.MOB_SUMMONED` instead, swap that. Mirror `NullEntity.finalizeSpawn`'s `EntitySpawnReason` reference.

- [ ] **Step 9.2: Wire up minion cleanup in `GrimReaperEntity.die`**

Open `wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperEntity.java`. Add a static import or fully-qualified reference for `GrimReaperRaiseDeadGoal.MINION_NBT_KEY`. Update the `die(DamageSource)` override to scan and discard minion skeletons. Replace the existing `die` method with:

```java
@Override
public void die(DamageSource damageSource) {
    super.die(damageSource);
    if (this.level() instanceof ServerLevel sl) {
        MinecraftServer server = sl.getServer();
        if (server != null) {
            GrimReaperSavedData saved = GrimReaperSavedData.get(server);
            if (this.getUUID().equals(saved.getCurrentId())) {
                saved.clear();
            }
        }
        // Cleanup raised minions
        sl.getEntitiesOfClass(net.minecraft.world.entity.monster.Skeleton.class,
            this.getBoundingBox().inflate(64.0))
            .stream()
            .filter(s -> s.getPersistentData().getBooleanOr(
                com.tweeks.wildwest.entity.ai.GrimReaperRaiseDeadGoal.MINION_NBT_KEY, false))
            .forEach(s -> s.discard());
    }
}
```

> **Note on `getBooleanOr` vs `getBoolean`:** older NeoForge had `getBoolean(String)` returning `false` for missing keys; some 26.x versions changed to `getBooleanOr(String, boolean)`. Try the modern form first; fall back to `getBoolean(MINION_NBT_KEY)` if the compiler complains. The cleanup radius of 64 blocks is generous to catch minions that ran far chasing the player.

- [ ] **Step 9.3: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9.4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ai/GrimReaperRaiseDeadGoal.java \
        wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperEntity.java
git commit -m "feat(wildwest): GrimReaperRaiseDeadGoal + minion cleanup on death"
```

---

## Task 10: `GrimReaperSoulLiftGoal`

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/GrimReaperSoulLiftGoal.java`

10t telegraph with soul-fire particles, then launch via `deltaMovement.y = 1.4`. Player-only target. 6s cooldown.

- [ ] **Step 10.1: Write the goal class**

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.GrimReaperEntity;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Soul Lift — Grim Reaper's CC attack. Telegraphs for 0.5s on the player's
 * feet (vertical soul-fire column + sound), then launches them straight up
 * to ~6.5 blocks above their start position. Player target only — non-player
 * targets are skipped so skeleton-minion combat isn't disrupted.
 *
 * <p>Cooldown 6s. Launch is implemented via {@code setDeltaMovement} +
 * {@code ServerPlayer.hurtMarked} to force the velocity packet.
 */
public class GrimReaperSoulLiftGoal extends Goal {

    private static final int COOLDOWN_TICKS = 120; // 6 s
    private static final int TELEGRAPH_TICKS = 10; // 0.5 s
    private static final double LAUNCH_RANGE = 12.0;
    private static final double LAUNCH_DELTA_Y = 1.4;

    private final GrimReaperEntity reaper;
    private int cooldown = 0;
    private int telegraphTimer = 0;
    private LivingEntity captured;

    public GrimReaperSoulLiftGoal(GrimReaperEntity reaper) {
        this.reaper = reaper;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.reaper.getTarget();
        if (!(target instanceof Player)) return false;
        if (!target.isAlive()) return false;
        if (!target.onGround()) return false;
        return this.reaper.distanceToSqr(target) <= LAUNCH_RANGE * LAUNCH_RANGE;
    }

    @Override
    public boolean canContinueToUse() {
        return this.telegraphTimer > 0 && this.captured != null && this.captured.isAlive();
    }

    @Override
    public void start() {
        this.captured = this.reaper.getTarget();
        if (this.captured == null) return;
        this.telegraphTimer = TELEGRAPH_TICKS;

        if (this.reaper.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.captured.getX(), this.captured.getY(), this.captured.getZ(),
                SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE, 1.0f, 0.6f);
        }
        this.reaper.swing(InteractionHand.MAIN_HAND);
    }

    @Override
    public void tick() {
        if (this.telegraphTimer <= 0 || this.captured == null) return;
        if (!(this.reaper.level() instanceof ServerLevel sl)) return;

        // Soul-fire column on target's feet
        for (int i = 0; i < 3; i++) {
            double yOff = this.reaper.getRandom().nextDouble() * 2.0;
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                this.captured.getX(), this.captured.getY() + yOff, this.captured.getZ(),
                1, 0.1, 0.1, 0.1, 0.02);
        }

        this.telegraphTimer--;
        if (this.telegraphTimer > 0) return;

        // Re-verify and launch
        if (!this.captured.isAlive()
            || !this.captured.onGround()
            || this.reaper.distanceToSqr(this.captured) > LAUNCH_RANGE * LAUNCH_RANGE) {
            this.cooldown = COOLDOWN_TICKS;
            return;
        }

        Vec3 v = this.captured.getDeltaMovement();
        this.captured.setDeltaMovement(v.x, LAUNCH_DELTA_Y, v.z);
        if (this.captured instanceof ServerPlayer sp) {
            sp.hurtMarked = true;
        }
        sl.playSound(null, this.captured.getX(), this.captured.getY(), this.captured.getZ(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 0.8f);

        this.cooldown = COOLDOWN_TICKS;
    }

    @Override
    public void stop() {
        this.captured = null;
        this.telegraphTimer = 0;
    }
}
```

> **Note on `hurtMarked`:** if the field is private in NeoForge 26.x, replace the assignment with:
> ```java
> sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
> ```
> after setting the delta. Try `hurtMarked = true` first.

- [ ] **Step 10.2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 10.3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ai/GrimReaperSoulLiftGoal.java
git commit -m "feat(wildwest): GrimReaperSoulLiftGoal (vertical launch CC)"
```

---

## Task 11: Wire goals + equipment into `GrimReaperEntity`

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperEntity.java`

Register all goals in priority order, equip the scythe in the main hand for the renderer.

- [ ] **Step 11.1: Add `registerGoals` override**

Open `wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperEntity.java`. Add this method (above `aiStep`):

```java
@Override
protected void registerGoals() {
    this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
    this.goalSelector.addGoal(1, new com.tweeks.wildwest.entity.ai.GrimReaperSoulLiftGoal(this));
    this.goalSelector.addGoal(2, new com.tweeks.wildwest.entity.ai.GrimReaperRaiseDeadGoal(this));
    this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.0D, false));
    this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.8D));
    this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(
        this, net.minecraft.world.entity.player.Player.class, 16.0F));
    this.goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));

    this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
    this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
        this, net.minecraft.world.entity.player.Player.class, true));
}
```

- [ ] **Step 11.2: Equip scythe in constructor**

In the same file, add at the end of the constructor (after `setCustomNameVisible(false)`):

```java
this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
    new net.minecraft.world.item.ItemStack(
        com.tweeks.wildwest.Registration.REAPER_SCYTHE.get()));
this.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0f);
```

(The scythe drop comes from the loot table; we don't want a duplicate drop from the equipment slot.)

- [ ] **Step 11.3: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 11.4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperEntity.java
git commit -m "feat(wildwest): wire GrimReaper goals + scythe equipment"
```

---

## Task 12: `ScytheSkeletonEntity` full implementation (sans goals)

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/ScytheSkeletonEntity.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java` (register attributes)

Replace the stub with a full class: owner UUID via synced `EntityDataAccessor`, NBT save/load, fire-clear in `aiStep`, attributes. Goals come in tasks 13–15.

- [ ] **Step 12.1: Replace the stub**

```java
package com.tweeks.wildwest.entity;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Player-summoned minion. Persistent, owner-tagged. Sun-immune (clears fire
 * every tick in {@link #aiStep()}). Iron sword + iron helmet on spawn.
 *
 * <p>AI goals are registered via {@link #registerGoals()} in a later task.
 */
public class ScytheSkeletonEntity extends Skeleton {

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
        SynchedEntityData.defineId(ScytheSkeletonEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public ScytheSkeletonEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, Optional.empty());
    }

    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(DATA_OWNER);
    }

    public void setOwnerUUID(@Nullable UUID owner) {
        this.entityData.set(DATA_OWNER, Optional.ofNullable(owner));
    }

    @Nullable
    public Player getOwnerPlayer() {
        Optional<UUID> id = this.getOwnerUUID();
        if (id.isEmpty()) return null;
        if (!(this.level() instanceof ServerLevel sl)) {
            return this.level().getPlayerByUUID(id.get());
        }
        var entity = sl.getEntity(id.get());
        return entity instanceof Player p ? p : null;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractSkeleton.createAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.27)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random,
                                                 net.minecraft.world.DifficultyInstance difficulty) {
        // Override vanilla skeleton (which gives a bow). Custom loadout: iron sword + helmet.
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        this.setDropChance(EquipmentSlot.HEAD, 0.0f);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Sun-immune: clear any fire applied by the daylight burn check.
        if (this.getRemainingFireTicks() > 0) {
            this.setRemainingFireTicks(0);
        }
    }

    @Override
    public boolean isSensitiveToWater() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.getOwnerUUID().ifPresent(id -> tag.putUUID("OwnerUUID", id));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) {
            this.setOwnerUUID(tag.getUUID("OwnerUUID"));
        }
    }

    /**
     * Count alive scythe-skeleton minions in the given level whose owner UUID
     * matches the given player. Used by {@code ReaperScytheItem} for cap-check
     * before spawning a new minion.
     */
    public static int countMinionsOwnedBy(ServerLevel level, UUID owner) {
        int count = 0;
        for (var entity : level.getAllEntities()) {
            if (entity instanceof ScytheSkeletonEntity minion
                && minion.isAlive()
                && minion.getOwnerUUID().filter(owner::equals).isPresent()) {
                count++;
            }
        }
        return count;
    }
}
```

> **Note on `getRemainingFireTicks` / `setRemainingFireTicks`:** the method names changed at some point (older versions: `getRemainingFireTicks` and `setRemainingFireTicks`; some intermediate versions: `getSecondsOnFire` / `setSecondsOnFire`). Mirror whatever existing fire-handling code in the wildwest module uses (`HerobrineEntity` is fire-immune — check how it handles this).

> **Note on `hasUUID` / `getUUID` in `CompoundTag`:** the API for storing UUIDs in NBT was simplified in newer versions to `tag.store("OwnerUUID", UUIDUtil.CODEC, uuid)` and `tag.read("OwnerUUID", UUIDUtil.CODEC)`. If `hasUUID` / `getUUID` don't exist, use the codec form. Mirror whatever vanilla `TamableAnimal.addAdditionalSaveData` uses in the active version.

> **Note on `level.getAllEntities()`:** if absent, use `level.getEntities(EntityTypeTest.forClass(ScytheSkeletonEntity.class), e -> true)` or `level.getEntities(...)` with an AABB the size of the whole world (huge inflated bounding box around (0,0,0)). Mirror an existing in-module entity scan if one exists; otherwise the simplest correct form is `level.getEntities(EntityTypeTest.forClass(ScytheSkeletonEntity.class), entity -> true)`.

- [ ] **Step 12.2: Register attributes in `WildWestMod`**

Open `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`. After the `GRIM_REAPER` attribute registration, add:

```java
event.put(ModEntities.SCYTHE_SKELETON.get(), ScytheSkeletonEntity.createAttributes().build());
```

Add the import.

- [ ] **Step 12.3: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 12.4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ScytheSkeletonEntity.java \
        wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java
git commit -m "feat(wildwest): ScytheSkeletonEntity core (owner UUID, NBT, attributes)"
```

---

## Task 13: TDD — `countMinionsOwnedBy` unit test

**Files:**
- Create: `wildwest/src/test/java/com/tweeks/wildwest/entity/ScytheSkeletonOwnerScanTest.java`

The helper `countMinionsOwnedBy` requires a `ServerLevel` to iterate, which is painful to instantiate in a unit test. Instead, extract a pure-Java overload that takes an `Iterable<? extends Entity>` of candidates and an `owner` UUID — the production version delegates to this pure variant.

- [ ] **Step 13.1: Write the failing test first**

Create `wildwest/src/test/java/com/tweeks/wildwest/entity/ScytheSkeletonOwnerScanTest.java`:

```java
package com.tweeks.wildwest.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ScytheSkeletonEntity#countMatching}.
 * Uses lightweight test doubles instead of constructing a real
 * {@link net.minecraft.server.level.ServerLevel}.
 */
class ScytheSkeletonOwnerScanTest {

    private static final UUID OWNER_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    /** Lightweight stand-in matching the contract of `countMatching`. */
    private record FakeMinion(boolean alive, Optional<UUID> ownerUUID) {}

    @Test
    void emptyList_returnsZero() {
        int n = ScytheSkeletonEntity.countMatching(
            List.<FakeMinion>of(),
            OWNER_A,
            FakeMinion::alive,
            FakeMinion::ownerUUID);
        assertEquals(0, n);
    }

    @Test
    void noMatchingOwner_returnsZero() {
        int n = ScytheSkeletonEntity.countMatching(
            List.of(new FakeMinion(true, Optional.of(OWNER_B))),
            OWNER_A,
            FakeMinion::alive,
            FakeMinion::ownerUUID);
        assertEquals(0, n);
    }

    @Test
    void singleMatch_returnsOne() {
        int n = ScytheSkeletonEntity.countMatching(
            List.of(new FakeMinion(true, Optional.of(OWNER_A))),
            OWNER_A,
            FakeMinion::alive,
            FakeMinion::ownerUUID);
        assertEquals(1, n);
    }

    @Test
    void twoMatchesOneOther_returnsTwoForA_oneForB() {
        var list = List.of(
            new FakeMinion(true, Optional.of(OWNER_A)),
            new FakeMinion(true, Optional.of(OWNER_A)),
            new FakeMinion(true, Optional.of(OWNER_B)));
        assertEquals(2, ScytheSkeletonEntity.countMatching(
            list, OWNER_A, FakeMinion::alive, FakeMinion::ownerUUID));
        assertEquals(1, ScytheSkeletonEntity.countMatching(
            list, OWNER_B, FakeMinion::alive, FakeMinion::ownerUUID));
    }

    @Test
    void deadMinion_isNotCounted() {
        int n = ScytheSkeletonEntity.countMatching(
            List.of(new FakeMinion(false, Optional.of(OWNER_A))),
            OWNER_A,
            FakeMinion::alive,
            FakeMinion::ownerUUID);
        assertEquals(0, n);
    }

    @Test
    void minionWithEmptyOwner_isNotCounted() {
        int n = ScytheSkeletonEntity.countMatching(
            List.of(new FakeMinion(true, Optional.empty())),
            OWNER_A,
            FakeMinion::alive,
            FakeMinion::ownerUUID);
        assertEquals(0, n);
    }
}
```

- [ ] **Step 13.2: Run the test to confirm it fails**

Run: `./gradlew :wildwest:test --tests com.tweeks.wildwest.entity.ScytheSkeletonOwnerScanTest`
Expected: COMPILATION FAILURE (`countMatching` doesn't exist) or test failure. Either is acceptable as a "failing" state.

- [ ] **Step 13.3: Add the pure helper to `ScytheSkeletonEntity`**

Open `wildwest/src/main/java/com/tweeks/wildwest/entity/ScytheSkeletonEntity.java`. Replace the existing `countMinionsOwnedBy` method with this pair:

```java
/**
 * Generic counter helper, testable without a {@link ServerLevel}.
 * Counts items in {@code candidates} that are alive (per {@code aliveProbe})
 * and have an owner UUID matching {@code owner}.
 */
public static <T> int countMatching(Iterable<T> candidates,
                                    UUID owner,
                                    java.util.function.Predicate<T> aliveProbe,
                                    java.util.function.Function<T, Optional<UUID>> ownerProbe) {
    int count = 0;
    for (T candidate : candidates) {
        if (!aliveProbe.test(candidate)) continue;
        if (ownerProbe.apply(candidate).filter(owner::equals).isPresent()) {
            count++;
        }
    }
    return count;
}

/**
 * Count alive scythe-skeleton minions in the given level whose owner UUID
 * matches the given player. Used by {@code ReaperScytheItem} for cap-check.
 */
public static int countMinionsOwnedBy(ServerLevel level, UUID owner) {
    Iterable<ScytheSkeletonEntity> minions =
        level.getEntities(
            net.minecraft.world.entity.EntityTypeTest.forClass(ScytheSkeletonEntity.class),
            e -> true);
    return countMatching(minions, owner,
        net.minecraft.world.entity.LivingEntity::isAlive,
        ScytheSkeletonEntity::getOwnerUUID);
}
```

> **Note:** if `ServerLevel.getEntities(EntityTypeTest, Predicate)` returns `List<T>` rather than `Iterable<T>`, that's still an `Iterable` — no change needed. If the method doesn't exist by that signature, try `level.getEntitiesOfClass(ScytheSkeletonEntity.class, hugeBoundingBox)`.

- [ ] **Step 13.4: Run the test to confirm it passes**

Run: `./gradlew :wildwest:test --tests com.tweeks.wildwest.entity.ScytheSkeletonOwnerScanTest`
Expected: 6 tests pass, BUILD SUCCESSFUL.

- [ ] **Step 13.5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ScytheSkeletonEntity.java \
        wildwest/src/test/java/com/tweeks/wildwest/entity/ScytheSkeletonOwnerScanTest.java
git commit -m "test(wildwest): countMatching helper + 6 unit tests for owner scan"
```

---

## Task 14: `ScytheSkeletonTargetHostilesGoal`

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/ScytheSkeletonTargetHostilesGoal.java`

Targets nearest `Monster` within 16 blocks. Excludes other scythe-skeletons with the same owner. Grim Reaper himself is a valid target.

- [ ] **Step 14.1: Write the goal**

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.ScytheSkeletonEntity;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;

/**
 * Custom target goal for bone servants. Targets the nearest hostile
 * {@link Monster} within 16 blocks, EXCLUDING other bone servants owned by
 * the same player. The Grim Reaper himself IS a valid target — the staff's
 * point is "fight enemies for you" and the boss is the headline enemy.
 */
public class ScytheSkeletonTargetHostilesGoal extends NearestAttackableTargetGoal<Monster> {

    public ScytheSkeletonTargetHostilesGoal(ScytheSkeletonEntity skeleton) {
        super(skeleton, Monster.class, /* randomInterval */ 10, /* mustSee */ true,
            /* mustReach */ false, ScytheSkeletonTargetHostilesGoal::sameOwnerExclusion(skeleton));
    }

    private static java.util.function.Predicate<LivingEntity> sameOwnerExclusion(
        ScytheSkeletonEntity self) {
        Optional<UUID> selfOwner = self.getOwnerUUID();
        return candidate -> {
            if (candidate instanceof ScytheSkeletonEntity other) {
                Optional<UUID> otherOwner = other.getOwnerUUID();
                if (selfOwner.isPresent()
                    && otherOwner.isPresent()
                    && selfOwner.get().equals(otherOwner.get())) {
                    return false;
                }
            }
            return true;
        };
    }
}
```

> **Note on `NearestAttackableTargetGoal` constructor:** the constructor signature has changed across versions. Common shapes:
> - `(mob, targetClass, mustSee)`
> - `(mob, targetClass, randomInterval, mustSee, mustReach, targetPredicate)`
> - `(mob, targetClass, randomInterval, mustSee, mustReach, TargetingConditions)`
>
> Mirror whatever existing `NearestAttackableTargetGoal<...>` calls in the wildwest module use (e.g. `NullEntity.registerGoals`). If the 6-arg form isn't available, fall back to the 3-arg form plus an override of `findTarget` / `canAttackTarget` for the exclusion logic.

- [ ] **Step 14.2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL. If the constructor doesn't match, adapt by extending and overriding `canAttack(LivingEntity)` to apply the exclusion predicate.

- [ ] **Step 14.3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ai/ScytheSkeletonTargetHostilesGoal.java
git commit -m "feat(wildwest): ScytheSkeletonTargetHostilesGoal"
```

---

## Task 15: `ScytheSkeletonFollowOwnerGoal`

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/ScytheSkeletonFollowOwnerGoal.java`

Wolf-style follow goal, ported because `ScytheSkeletonEntity` doesn't extend `TamableAnimal`. Each tick: if owner farther than min, navigate to owner. If farther than teleport-recall threshold, snap to a position behind owner.

- [ ] **Step 15.1: Write the goal**

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.ScytheSkeletonEntity;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;

/**
 * Wolf-style follow-owner for bone servants. Vanilla {@code FollowOwnerGoal}
 * is hardcoded to {@code TamableAnimal}, which we are not — so we own a
 * minimal port: stay within {@code stopDistance} of owner, teleport behind
 * owner if distance exceeds {@code teleportDistance}.
 */
public class ScytheSkeletonFollowOwnerGoal extends Goal {

    private final ScytheSkeletonEntity skeleton;
    private final double speed;
    private final float startDistance;
    private final float teleportDistance;
    private final PathNavigation navigation;
    private LivingEntity owner;
    private int timeToRecalcPath;

    public ScytheSkeletonFollowOwnerGoal(ScytheSkeletonEntity skeleton,
                                         double speed,
                                         float startDistance,
                                         float teleportDistance) {
        this.skeleton = skeleton;
        this.speed = speed;
        this.startDistance = startDistance;
        this.teleportDistance = teleportDistance;
        this.navigation = skeleton.getNavigation();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Player owner = this.skeleton.getOwnerPlayer();
        if (owner == null || owner.isSpectator()) return false;
        if (this.skeleton.distanceToSqr(owner) < this.startDistance * this.startDistance) return false;
        this.owner = owner;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.navigation.isDone()) return false;
        if (this.owner == null) return false;
        return this.skeleton.distanceToSqr(this.owner) > (this.startDistance * 0.5f) * (this.startDistance * 0.5f);
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.navigation.stop();
    }

    @Override
    public void tick() {
        if (this.owner == null) return;
        this.skeleton.getLookControl().setLookAt(this.owner, 10.0F, this.skeleton.getMaxHeadXRot());
        if (--this.timeToRecalcPath > 0) return;
        this.timeToRecalcPath = 10;

        if (this.skeleton.distanceToSqr(this.owner) >= this.teleportDistance * this.teleportDistance) {
            teleportBehindOwner();
        } else {
            this.navigation.moveTo(this.owner, this.speed);
        }
    }

    private void teleportBehindOwner() {
        BlockPos ownerPos = this.owner.blockPosition();
        var rng = this.skeleton.getRandom();
        for (int i = 0; i < 10; i++) {
            int dx = rng.nextInt(5) - 2;
            int dz = rng.nextInt(5) - 2;
            BlockPos candidate = ownerPos.offset(dx, 0, dz);
            if (isTeleportable(candidate)) {
                this.skeleton.snapTo(
                    candidate.getX() + 0.5,
                    candidate.getY(),
                    candidate.getZ() + 0.5,
                    this.skeleton.getYRot(),
                    this.skeleton.getXRot());
                this.navigation.stop();
                return;
            }
        }
    }

    private boolean isTeleportable(BlockPos pos) {
        LevelReader level = this.skeleton.level();
        var blockBelow = level.getBlockState(pos.below());
        var blockAt = level.getBlockState(pos);
        var blockAbove = level.getBlockState(pos.above());
        if (!blockBelow.isCollisionShapeFullBlock(level, pos.below())) return false;
        if (blockBelow.is(BlockTags.LEAVES)) return false;
        if (!blockAt.getCollisionShape(level, pos).isEmpty()) return false;
        if (!blockAbove.getCollisionShape(level, pos.above()).isEmpty()) return false;
        return true;
    }
}
```

> **Note on `snapTo` vs `teleportTo`:** in newer NeoForge `Entity.snapTo(x, y, z, yaw, pitch)` replaced `moveTo(x, y, z, yaw, pitch)` for the canonical "instant teleport" call. If `snapTo` doesn't exist, use `moveTo`. If neither exists, use `teleportTo(x, y, z)` plus `setYRot(...)` and `setXRot(...)`.

> **Note on `getMaxHeadXRot`:** if absent, replace with the literal `30.0F`.

> **Note on `isCollisionShapeFullBlock`:** API churn target. If it doesn't exist, use `blockBelow.isFaceSturdy(level, pos.below(), Direction.UP)` or `Block.isShapeFullBlock(blockBelow.getCollisionShape(...))`. The goal is "can the skeleton stand on this block."

- [ ] **Step 15.2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL. Adapt API churn points based on compiler errors.

- [ ] **Step 15.3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ai/ScytheSkeletonFollowOwnerGoal.java
git commit -m "feat(wildwest): ScytheSkeletonFollowOwnerGoal (wolf-style follow + teleport-recall)"
```

---

## Task 16: `ScytheSkeletonMineOreGoal`

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/ScytheSkeletonMineOreGoal.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/ScytheSkeletonEntity.java` (idle counter)

This goal owns an `idleTicks` counter (private to `ScytheSkeletonEntity`) updated each `aiStep`. When it crosses 60, this goal becomes eligible. On start, scan a 3×3×3 cube for precious ore, path to it, mine over 20 ticks, drop into owner inventory or feet.

- [ ] **Step 16.1: Add idle counter to `ScytheSkeletonEntity`**

Open `wildwest/src/main/java/com/tweeks/wildwest/entity/ScytheSkeletonEntity.java`. Add a private field near the top of the class:

```java
private int idleTicks = 0;
```

Add public accessors:

```java
public int getIdleTicks() {
    return this.idleTicks;
}

public void resetIdleTicks() {
    this.idleTicks = 0;
}
```

Modify the existing `aiStep` override to bump the counter:

```java
@Override
public void aiStep() {
    super.aiStep();
    if (this.getRemainingFireTicks() > 0) {
        this.setRemainingFireTicks(0);
    }
    if (this.getTarget() != null
        || this.getNavigation().isInProgress()
        || (this.getOwnerPlayer() != null
            && this.distanceToSqr(this.getOwnerPlayer()) < 5.0 * 5.0)) {
        this.idleTicks = 0;
    } else {
        this.idleTicks++;
    }
}
```

- [ ] **Step 16.2: Write the mining goal**

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.ScytheSkeletonEntity;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Idle behavior: scan 3×3×3 cube around the skeleton, find the nearest
 * precious-metal ore (iron / gold / diamond / emerald / ancient debris,
 * including deepslate variants), path to it, mine it over 20 ticks. Drops
 * route into owner inventory, overflow falls at owner's feet.
 *
 * <p>Eligible only when the skeleton's {@code idleTicks} counter exceeds
 * 60 (3 seconds of no target / no path / not adjacent to owner).
 */
public class ScytheSkeletonMineOreGoal extends Goal {

    private static final int IDLE_THRESHOLD = 60;
    private static final int SCAN_RADIUS = 3;
    private static final int Y_BOUND = 8;
    private static final int MINE_TIME_TICKS = 20;

    private static final Set<Block> PRECIOUS_ORES = Set.of(
        Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
        Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
        Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
        Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.ANCIENT_DEBRIS);

    private final ScytheSkeletonEntity skeleton;
    private BlockPos target;
    private int mineTimer = 0;

    public ScytheSkeletonMineOreGoal(ScytheSkeletonEntity skeleton) {
        this.skeleton = skeleton;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.skeleton.getTarget() != null) return false;
        if (this.skeleton.getIdleTicks() < IDLE_THRESHOLD) return false;
        Player owner = this.skeleton.getOwnerPlayer();
        if (owner == null) return false;
        if (this.skeleton.distanceToSqr(owner) > 32.0 * 32.0) return false;

        BlockPos found = findNearestOre();
        if (found == null) return false;

        Player ownerCheckY = owner;
        if (Math.abs(found.getY() - ownerCheckY.getBlockY()) > Y_BOUND) return false;

        this.target = found;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.target == null) return false;
        BlockState state = this.skeleton.level().getBlockState(this.target);
        return PRECIOUS_ORES.contains(state.getBlock());
    }

    @Override
    public void start() {
        if (this.target != null) {
            this.skeleton.getNavigation().moveTo(
                this.target.getX() + 0.5,
                this.target.getY(),
                this.target.getZ() + 0.5,
                1.0);
        }
        this.mineTimer = 0;
    }

    @Override
    public void tick() {
        if (this.target == null) return;
        if (!(this.skeleton.level() instanceof ServerLevel sl)) return;

        double distSq = this.skeleton.distanceToSqr(
            this.target.getX() + 0.5, this.target.getY() + 0.5, this.target.getZ() + 0.5);
        if (distSq > 1.5 * 1.5) {
            // Still pathing
            this.mineTimer = 0;
            return;
        }

        sl.sendParticles(ParticleTypes.CRIT,
            this.target.getX() + 0.5, this.target.getY() + 0.5, this.target.getZ() + 0.5,
            3, 0.2, 0.2, 0.2, 0.1);

        this.mineTimer++;
        if (this.mineTimer < MINE_TIME_TICKS) return;

        // Compute drops BEFORE destroying the block.
        BlockState state = sl.getBlockState(this.target);
        if (!PRECIOUS_ORES.contains(state.getBlock())) {
            this.target = null;
            return;
        }
        var drops = Block.getDrops(state, sl, this.target, sl.getBlockEntity(this.target));

        sl.destroyBlock(this.target, false, this.skeleton);

        Player owner = this.skeleton.getOwnerPlayer();
        for (ItemStack drop : drops) {
            if (owner == null) {
                sl.addFreshEntity(new ItemEntity(sl,
                    this.target.getX() + 0.5, this.target.getY() + 0.5, this.target.getZ() + 0.5,
                    drop));
                continue;
            }
            ItemStack toGive = drop.copy();
            if (!owner.getInventory().add(toGive)) {
                sl.addFreshEntity(new ItemEntity(sl,
                    owner.getX(), owner.getY(), owner.getZ(), toGive));
            }
        }

        this.target = null;
        this.skeleton.resetIdleTicks();
    }

    @Override
    public void stop() {
        this.target = null;
        this.mineTimer = 0;
    }

    private BlockPos findNearestOre() {
        BlockPos origin = this.skeleton.blockPosition();
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dy = -SCAN_RADIUS; dy <= SCAN_RADIUS; dy++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (!PRECIOUS_ORES.contains(this.skeleton.level().getBlockState(p).getBlock())) continue;
                    double dSq = origin.distSqr(p);
                    if (dSq < bestDistSq) {
                        bestDistSq = dSq;
                        best = p;
                    }
                }
            }
        }
        return best;
    }
}
```

> **Note on `Block.getDrops`:** signature in 26.x is typically `Block.getDrops(BlockState, ServerLevel, BlockPos, @Nullable BlockEntity)` returning `List<ItemStack>`. If a fifth `Entity tool` parameter is required, pass `this.skeleton`.

- [ ] **Step 16.3: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 16.4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ai/ScytheSkeletonMineOreGoal.java \
        wildwest/src/main/java/com/tweeks/wildwest/entity/ScytheSkeletonEntity.java
git commit -m "feat(wildwest): ScytheSkeletonMineOreGoal + idle counter"
```

---

## Task 17: Wire goals into `ScytheSkeletonEntity`

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/ScytheSkeletonEntity.java`

Override `registerGoals` to wire the three custom goals plus vanilla helpers.

- [ ] **Step 17.1: Add `registerGoals` override**

In `ScytheSkeletonEntity`, override `registerGoals` (vanilla `Skeleton.registerGoals` adds bow-based goals; we replace it entirely):

```java
@Override
protected void registerGoals() {
    this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
    this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.1D, true));
    this.goalSelector.addGoal(3, new com.tweeks.wildwest.entity.ai.ScytheSkeletonMineOreGoal(this));
    this.goalSelector.addGoal(4, new com.tweeks.wildwest.entity.ai.ScytheSkeletonFollowOwnerGoal(
        this, 1.0D, 5.0F, 10.0F));
    this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.8D));
    this.goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(
        this, net.minecraft.world.entity.player.Player.class, 8.0F));

    this.targetSelector.addGoal(1, new com.tweeks.wildwest.entity.ai.ScytheSkeletonTargetHostilesGoal(this));
}
```

> Note: do NOT call `super.registerGoals()`. Vanilla `Skeleton` adds bow goals + sun-avoidance — we want neither.

- [ ] **Step 17.2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 17.3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ScytheSkeletonEntity.java
git commit -m "feat(wildwest): wire ScytheSkeleton goals (melee, mine, follow-owner, target-hostiles)"
```

---

## Task 18: Complete `ReaperScytheItem` right-click summon flow

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/item/ReaperScytheItem.java`

Replace the cooldown stub with the full summon flow: cap check, spawn position projection, entity creation, owner UUID, particles, cooldown stamp.

- [ ] **Step 18.1: Replace the `use` method body**

Open `wildwest/src/main/java/com/tweeks/wildwest/item/ReaperScytheItem.java`. Replace the existing `use(...)` method with:

```java
@Override
public InteractionResult use(Level level, Player player, InteractionHand hand) {
    if (hand != InteractionHand.MAIN_HAND) {
        return InteractionResult.PASS;
    }

    ItemStack stack = player.getItemInHand(hand);
    if (player.getCooldowns().isOnCooldown(stack)) {
        return InteractionResult.FAIL;
    }

    if (level.isClientSide()) {
        return InteractionResult.SUCCESS;
    }

    if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) {
        return InteractionResult.PASS;
    }

    int alive = com.tweeks.wildwest.entity.ScytheSkeletonEntity.countMinionsOwnedBy(sl, player.getUUID());
    if (alive >= MAX_MINIONS) {
        player.sendOverlayMessage(
            net.minecraft.network.chat.Component.translatable("item.wildwest.reaper_scythe.cap_reached"));
        return InteractionResult.FAIL;
    }

    var eye = player.getEyePosition();
    var look = player.getLookAngle();
    double sx = eye.x + look.x * SUMMON_RANGE;
    double sy = eye.y + look.y * SUMMON_RANGE;
    double sz = eye.z + look.z * SUMMON_RANGE;
    net.minecraft.core.BlockPos.MutableBlockPos cursor =
        new net.minecraft.core.BlockPos.MutableBlockPos(
            (int) Math.floor(sx), (int) Math.floor(sy), (int) Math.floor(sz));
    for (int dy = 0; dy < 4; dy++) {
        net.minecraft.core.BlockPos below = cursor.below();
        if (!sl.getBlockState(below).isAir()) {
            break;
        }
        cursor.setY(below.getY());
    }
    double spawnY = cursor.getY();

    com.tweeks.wildwest.entity.ScytheSkeletonEntity minion =
        com.tweeks.wildwest.ModEntities.SCYTHE_SKELETON.get()
            .create(sl, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
    if (minion == null) {
        return InteractionResult.FAIL;
    }
    minion.setPos(sx, spawnY, sz);
    minion.setOwnerUUID(player.getUUID());
    minion.finalizeSpawn(sl, sl.getCurrentDifficultyAt(minion.blockPosition()),
        net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
    sl.addFreshEntity(minion);

    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
        sx, spawnY + 0.5, sz, 15, 0.3, 0.5, 0.3, 0.05);
    sl.playSound(null, sx, spawnY, sz,
        net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CONVERTED,
        net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.2f);

    player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
    player.swing(hand);
    return InteractionResult.CONSUME;
}
```

> **Note on cursor projection:** the goal is "drop the spawn point to the first solid ground within 4 blocks below the look-direction endpoint." The implementation above iterates downward through air blocks. If the target endpoint is inside a solid block (rare — player aimed at a wall), `cursor.below().isAir()` returns false immediately and we use the original Y. Acceptable failure mode.

> **Note on `getCurrentDifficultyAt`:** if absent, pass `null` to `finalizeSpawn` and rely on its handling of null difficulty (vanilla `Mob.finalizeSpawn` accepts null).

- [ ] **Step 18.2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 18.3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/ReaperScytheItem.java
git commit -m "feat(wildwest): ReaperScytheItem right-click summons bone servant (cap 3, 5s cd)"
```

---

## Task 19: Smoke test — `ReaperScytheItem` constants

**Files:**
- Create: `wildwest/src/test/java/com/tweeks/wildwest/item/ReaperScytheItemTest.java`

Single test verifies the constants haven't drifted. The full summon flow requires a live server, deferred to manual testing.

- [ ] **Step 19.1: Write the test**

```java
package com.tweeks.wildwest.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Constants smoke test. Catches accidental drift of cooldown / cap /
 * range / damage values that the spec promises. Full summon flow is
 * exercised manually in dev-client.
 */
class ReaperScytheItemTest {

    @Test
    void constants_matchSpec() {
        assertEquals(100, ReaperScytheItem.COOLDOWN_TICKS);  // 5 s
        assertEquals(3, ReaperScytheItem.MAX_MINIONS);
        assertEquals(4.0, ReaperScytheItem.SUMMON_RANGE);
        assertEquals(6.0, ReaperScytheItem.ATTACK_DAMAGE);
    }
}
```

- [ ] **Step 19.2: Run the test**

Run: `./gradlew :wildwest:test --tests com.tweeks.wildwest.item.ReaperScytheItemTest`
Expected: 1 test passes, BUILD SUCCESSFUL.

- [ ] **Step 19.3: Commit**

```bash
git add wildwest/src/test/java/com/tweeks/wildwest/item/ReaperScytheItemTest.java
git commit -m "test(wildwest): ReaperScytheItem constants smoke test"
```

---

## Task 20: Full test suite + build

Run the entire wildwest test suite and a clean build to catch any regressions from the new code.

- [ ] **Step 20.1: Run full test suite**

Run: `./gradlew :wildwest:test`
Expected: BUILD SUCCESSFUL with all tests passing (`BossSingletonStateTest`, `SteveStackerPhaseLogicTest`, `WeaponModeTest`, `ScytheSkeletonOwnerScanTest`, `ReaperScytheItemTest`, plus any others).

If any pre-existing test fails, do NOT mark this task complete. Investigate — likely a refactor in a previous task disturbed shared state. Likely culprit: changes to `WildWestMod.java` or `ModEntities.java` broke entity-attribute or spawn-placement registration.

- [ ] **Step 20.2: Full module build**

Run: `./gradlew :wildwest:build`
Expected: BUILD SUCCESSFUL. Catches issues that `compileJava` alone misses (datapack validation, resource pack JSON schema, etc.).

- [ ] **Step 20.3: Commit (only if build produced any auto-generated files)**

```bash
git status
# If there are auto-generated cache files under wildwest/src/generated/, stage them:
git add wildwest/src/generated
git commit -m "build(wildwest): regenerated server data after Grim Reaper additions" || true
```

If `git status` is clean, no commit needed.

---

## Task 21: Manual smoke test plan (creative dev-client)

This is a checklist for the human running the dev client, NOT automation. Boot `./gradlew :wildwest:runClient` and step through these.

- [ ] **Step 21.1: Spawn egg singleton flow**

- Open creative inventory → Wild West tab → confirm Grim Reaper Spawn Egg is visible.
- Right-click egg on grass at night → Grim Reaper spawns, yellow boss bar with 10 segments appears.
- Right-click egg again on grass → existing Grim Reaper teleports to new location (soul particles, soul sound). No second Reaper spawns.
- Confirm spawn egg refused in nether/end with overlay message.

- [ ] **Step 21.2: Raise Dead cycle**

- Engage the Reaper (let him target you).
- Within 10s, observe dirt-particle bursts at 2–3 ground positions within 8 blocks of you.
- After 1s emerge delay, vanilla skeletons appear holding iron swords.
- `/data get entity @e[type=skeleton,limit=1,sort=nearest]` to inspect the equipped sword's enchantments — confirm Sharpness 3.
- Wait ~10s, observe another wave.

- [ ] **Step 21.3: Reaper death cleanup**

- Spawn the Reaper, let him summon 3+ skeletons.
- Kill the Reaper (creative `/kill @e[type=wildwest:grim_reaper]`).
- Confirm all minion skeletons (the ones from Raise Dead) are discarded immediately. Other skeletons (natural spawns, or skeletons summoned by other means) survive.

- [ ] **Step 21.4: Soul Lift**

- Stand on flat ground within 12 blocks of the Reaper.
- Observe 0.5s soul-fire particle column at your feet, then upward launch.
- Confirm peak height ~6–7 blocks, fall damage ~3–4 hearts.
- Confirm 6s cooldown — Reaper can't Soul Lift you twice in 6s.

- [ ] **Step 21.5: Scythe melee**

- Kill the Reaper, pick up the Reaper Scythe drop.
- Equip it. Confirm tooltip shows the three lines from the lang strings.
- Hit a passive mob (cow / pig). Confirm 6 damage (3 hearts) per swing.

- [ ] **Step 21.6: Scythe summon + cap**

- Right-click scythe at empty space → bone servant appears 4 blocks ahead at ground level. Soul particles + zombie-villager-converted sound.
- Right-click again immediately → cooldown FAIL (no spawn).
- Wait 5s, right-click → second servant.
- Wait, right-click → third servant.
- Wait, right-click → overlay actionbar "Your bone servants are already three…". No 4th servant spawned, no cooldown applied.

- [ ] **Step 21.7: Bone servant follow + attack**

- Walk 20 blocks away from a servant → it pathfinds to follow; if distance exceeds 10 blocks, it teleports behind you.
- Spawn a zombie nearby (`/summon minecraft:zombie ~ ~ ~`) → servant engages and kills it.
- Attack a servant of yours yourself — confirm it does NOT retaliate (you are the owner, not a `Monster`).

- [ ] **Step 21.8: Bone servant mining**

- Place an iron ore block 2 blocks from an idle servant.
- Wait 3+ seconds. Confirm:
  - Servant paths to the ore.
  - 20 ticks of CRIT particles play.
  - Ore breaks, raw iron lands in your inventory (or at your feet if inventory full).
- Place a coal ore next to a servant → confirm servant does NOT mine it (coal is excluded).
- Repeat for: copper ore (excluded), iron deepslate ore (mined), gold ore (mined), redstone (excluded), diamond (mined), emerald (mined), ancient debris (mined).

- [ ] **Step 21.9: Save/load persistence**

- Spawn Reaper + 2 servants. Save world (ESC → Save and Quit to Title).
- Reload world. Confirm:
  - Reaper reappears with full HP, yellow boss bar.
  - Singleton SavedData survives (try the egg — should teleport-not-spawn).
  - Bone servants still alive, still follow you (owner UUID survives).

- [ ] **Step 21.10: Concurrent apex bosses**

- Spawn all five apex bosses simultaneously (use spawn eggs for each).
- Confirm five boss bars stacked (Herobrine red, Agent purple, Null white-notched-6, Steve Stacker purple, Grim Reaper yellow-notched-10).
- Confirm singletons don't interfere — each can be killed and respawned independently.

If all 10 manual sub-steps pass, the implementation is complete. Mark this task done.

- [ ] **Step 21.11: Commit any final tweaks**

If manual testing surfaces issues (e.g. minor balance, particle counts, sound volumes), apply fixes inline and commit them as `fix(wildwest): manual-test adjustments`. If nothing surfaces, no commit needed.

---

## Self-Review

**Spec coverage check:**

| Spec section | Task(s) |
|---|---|
| Entity stats, hitbox, attributes, fire/fall immunity | Task 4 |
| Singleton storage (`BossSingletonSavedData` subclass) | Task 2 |
| Spawn rules + biome modifier + placement | Task 3 |
| Spawn egg (overworld-only, singleton-aware) | Task 7 |
| AI goal priority + targeting | Task 11 |
| Scythe melee (attribute-driven damage on entity) | Task 4 (attribute), Task 11 (equipment) |
| Raise Dead (2–3 skeletons, Sharpness 3, dirt particles, cleanup) | Task 9 |
| Soul Lift (telegraph, launch, fall damage) | Task 10 |
| Equipment (scythe held) | Task 11 |
| Visuals (texture, renderer) | Task 8 |
| Boss bar (YELLOW NOTCHED_10) | Task 4 |
| Loot + XP | Task 4 (XP), Task 5 (loot table) |
| Reaper Scythe item (dual-purpose) | Task 6 (melee + stub), Task 18 (summon) |
| Scythe Skeleton entity (owner UUID, persistent, sun-immune) | Task 12 |
| Scythe Skeleton mining goal | Task 16 |
| Scythe Skeleton target/follow goals | Task 14, Task 15 |
| Scythe Skeleton renderer reuse | Task 8 |
| Sounds (entity, raise dead, soul lift) | Task 4 (entity), Task 9 (raise), Task 10 (lift) |
| Lang strings | Task 1 |
| Tests (`countMatching`, scythe constants) | Task 13, Task 19 |
| Manual smoke test | Task 21 |

All spec sections covered.

**Placeholder scan:** searched for "TBD", "TODO", "FIXME". One `TODO` marker exists in Task 6 (`ReaperScytheItem`'s `use(...)` body), explicitly flagged in the task description and resolved in Task 18 — acceptable as a deliberate cross-task hand-off rather than a plan placeholder.

**Type consistency:**

- `GrimReaperSavedData` static accessor: `get(MinecraftServer)` returns `GrimReaperSavedData` — used consistently in Task 4 (`finalizeSpawn`, `die`, `remove`), Task 7 (`useOn`), Task 3 (`checkSpawnRules`).
- `GrimReaperRaiseDeadGoal.MINION_NBT_KEY` defined as `String` constant in Task 9, referenced in same task's `die` modification — same file, no drift.
- `ScytheSkeletonEntity.getOwnerUUID()` returns `Optional<UUID>` — referenced consistently in Tasks 13 (test), 14 (target goal), 15 (follow goal), 16 (mining goal), 18 (item use).
- `ScytheSkeletonEntity.countMinionsOwnedBy(ServerLevel, UUID)` signature locked in Task 12, called from Task 18 (`ReaperScytheItem.use`) with matching args.
- `ScytheSkeletonEntity.countMatching(...)` (4-arg generic version) introduced in Task 13, used by `countMinionsOwnedBy` overload — same task.
- `ScytheSkeletonEntity.getIdleTicks()` / `resetIdleTicks()` added in Task 16, referenced in same task's mining goal — same file boundary.
- `ReaperScytheItem` constants `COOLDOWN_TICKS`, `MAX_MINIONS`, `SUMMON_RANGE`, `ATTACK_DAMAGE` defined Task 6, asserted Task 19, used Task 18.

No type drift detected.

**Execution Handoff:**

Plan complete and saved to `docs/superpowers/plans/2026-05-20-grim-reaper.md`. Two execution options:

1. **Subagent-Driven (recommended)** — dispatch a fresh subagent per task, review between tasks, fast iteration
2. **Inline Execution** — execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
