# Thief Mob & Multi-Module Restructure — Design

**Date:** 2026-04-29
**Status:** Draft (pending user review of this doc)
**Series:** Second mod in the `minecraft-mods` series. Restructures the repo into a multi-module "Security Pack."

## Goal

Two coupled goals:

1. **Restructure** the repo from a single-mod gradle project into a multi-module project so additional security-themed mods (Thief now, Traps and Barriers later) can ship as siblings of the Guard mod and share common code.
2. **Add a Thief mob** — a hostile that disguises as a villager, steals from chests, hides loot in a private hideout chest, and switches between a crossbow (ranged) and a blackjack (melee) when revealed. Guards attack revealed Thieves; Thieves secretly attack Guards when no player is watching.

## Platform

Java / NeoForge / MC 1.21.x. Same target as the existing Guard mod.

## Scope

### In scope (v1)

**Restructure:**
- Convert top-level `settings.gradle` to multi-project (`include 'securitycore', 'securityguard', 'thief'`)
- Hoist common build config to root `build.gradle`
- Extract reusable code from `securityguard/` into a new `securitycore/` library mod
- Refactor `securityguard/` to depend on `:securitycore`

**securitycore (new library mod):**
- `api.SecurityAlly` — marker interface for entities Guards protect (initially: villagers, players-by-default, the Guard itself)
- `api.SecurityHostile` — marker interface for entities Guards aggressively target (initially: revealed Thieves)
- `ai.StunningMeleeGoal` — extracted from `BatonStrikeGoal`; configurable damage, stun duration, hit cooldown
- `client.HeldItemLayer` — generalized version of `BatonHeldLayer`; renders a model in the entity's right hand

**Thief mob:**
- Entity `thief:thief`, base class `PathfinderMob` implementing `CrossbowAttackMob` and (when revealed) `SecurityHostile`
- Five-state behavioral machine: `DISGUISED`, `SUSPICIOUS`, `REVEALED_RANGED`, `REVEALED_MELEE`, `FLEEING`
- Layered disguise: subtle visual tell always visible (eye-mask stripe, darker robe) + dramatic reveal on state change
- Steal-from-chest goal targeting nearby chests within 24 blocks
- Hideout-chest mechanic: spawned chest at world-gen, persists in entity NBT, loot deposited on return
- Secret guard-attack goal that activates only when no player has line-of-sight on the Thief
- Crossbow + blackjack weapon switching based on aggressor distance (≤4 blocks → blackjack, else crossbow)
- Custom textures (`thief_disguised.png`, `thief_revealed.png`)
- Reveal animation (0.5s)
- Spawn egg
- Natural village spawn (rare, 1 per village) + periodic repopulation tick
- Loot table including the items the Thief was carrying when killed
- English localization
- Datagen for recipe (blackjack), loot table, language

**Blackjack item:**
- Melee weapon: 2 damage, applies Slowness II for 2s on hit
- Crafting recipe: 1 leather + 1 iron ingot, vertical pattern
- Drops from Thief at 25%

### Out of scope (future versions)

- Pickpocketing the player directly
- Stealing from villager workstations / killing villagers for drops
- Player-driven Thief recruitment (taming)
- Heist event (raid-style scheduled attack)
- Crossbow as a custom enchanted variant
- Multiple Thief variants (apprentice / master / etc.)
- Thief detection items for the player (alarms, motion sensors)
- Bedrock port
- Localizations beyond English

## Repo Restructure

### Target layout

```
minecraft-mods/
├── settings.gradle           # multi-project: include 'securitycore', 'securityguard', 'thief'
├── build.gradle              # shared neoforge plugin config + common deps
├── gradle.properties         # shared NeoForge / Minecraft / Java versions
├── securitycore/
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/tweeks/securitycore/
│       │   ├── SecurityCoreMod.java
│       │   ├── api/{SecurityAlly, SecurityHostile, Crime}.java
│       │   ├── ai/StunningMeleeGoal.java
│       │   └── client/HeldItemLayer.java
│       └── resources/META-INF/neoforge.mods.toml
├── securityguard/            # existing — refactored
│   ├── build.gradle          # depends on project(':securitycore')
│   └── src/...               # existing tree, with deletions noted below
└── thief/
    ├── build.gradle          # depends on project(':securitycore')
    └── src/main/
        ├── java/com/tweeks/thief/
        │   ├── ThiefMod.java
        │   ├── Registration.java
        │   ├── entity/ThiefEntity.java
        │   ├── entity/RevealState.java
        │   ├── entity/ai/{StealFromChestGoal, ReturnToHideoutGoal,
        │   │              FleeAndFireCrossbowGoal, BlackjackStrikeGoal,
        │   │              SecretGuardTargetGoal, WanderInVillageGoal}.java
        │   ├── item/BlackjackItem.java
        │   ├── client/model/{ThiefModel, BlackjackModel}.java
        │   ├── client/renderer/ThiefRenderer.java
        │   ├── data/{DataGenerators, ModRecipeProvider, ModLanguageProvider,
        │   │         ModEntityLootProvider}.java
        │   └── world/HideoutPlacer.java
        └── resources/...
```

### Deletions from `securityguard/`

- `entity/ai/BatonStrikeGoal.java` — replaced by `StunningMeleeGoal` from core
- `client/renderer/BatonHeldLayer.java` — replaced by `HeldItemLayer` from core
- `BatonModel` stays (it's a model, mod-specific)

### Predicate change in `securityguard`

`SecurityGuardEntity.GuardTargetHostilesGoal` predicate broadens to:

```java
target -> target instanceof SecurityHostile
       || (target instanceof Enemy && !(target instanceof Creeper))
```

This makes Guards attack revealed Thieves (which implement `SecurityHostile`) while still avoiding Creepers. Disguised Thieves don't implement `SecurityHostile` and don't implement `Enemy`, so they pass as villagers from the Guard's POV until reveal.

## Thief Mob Specification

### Identity
- Entity ID: `thief:thief`
- Display name: "Thief"
- Category: `MobCategory.MONSTER` (counts toward hostile cap, despawn-on-distance like other monsters; the disguise is behavioral, not registry-level)
- Size: 0.6 wide × 1.95 tall (matches villager-ish humanoid)

### Attributes
| Attribute | Value | Reasoning |
|---|---|---|
| Max health | 20 | Glass cannon — relies on stealth |
| Attack damage | 3 | Blackjack itself adds Slowness II; raw damage is low |
| Movement speed | 0.32 | Matches Guard pace; can keep up in a chase |
| Knockback resistance | 0.0 | No special resilience |
| Follow range | 24 | Tighter than Guard (32); thief picks fights it can win |

### Behavioral state machine

State stored as `EntityDataAccessor<Byte>` synced to client (renderer reads it).

| State | Visual | Weapon | Triggers / behavior |
|---|---|---|---|
| `DISGUISED` | villager texture + eye-mask stripe | none visible | Default. Wanders village, opens chests when no player line-of-sight, secretly attacks Guards when alone. |
| `SUSPICIOUS` | villager texture + mask half-down | none visible | 2s (40-tick) timer set when a player catches a chest-open between 8-16 blocks. During the window: if player closes to ≤8 blocks → escalate to REVEALED; if timer expires → return to DISGUISED. Thief breaks off the chest interaction immediately on entering SUSPICIOUS. |
| `REVEALED_RANGED` | thief texture, crossbow drawn | crossbow | Fires at nearest target ≥5 blocks; kites away if pressed. |
| `REVEALED_MELEE` | thief texture, blackjack drawn | blackjack | Strikes nearest target ≤4 blocks; uses `StunningMeleeGoal` from core. |
| `FLEEING` | thief texture | currently equipped | Carrying loot OR HP <30%. Pathfinds to hideout, deposits, then re-equips and re-engages from REVEALED_RANGED. |

#### Reveal triggers
1. Hit by anything → `REVEALED_*` (distance check below)
2. Player with line-of-sight within 16 blocks while opening chest → `SUSPICIOUS`, then escalates if player approaches within 8 blocks
3. Guard with line-of-sight within 8 blocks → `REVEALED_*` (Guards are trained to spot Thieves at closer range)
4. Carrying ≥1 stolen item AND player within 8 blocks with line-of-sight → `REVEALED_*` (the loot gives them away)

#### Distance check at reveal
On reveal trigger, find the nearest entity that the Thief considers a threat (player, Guard, anything that hit them):
- distance ≤4 blocks → `REVEALED_MELEE` (draw blackjack)
- distance >4 blocks → `REVEALED_RANGED` (draw crossbow)

Reveal state can swap between `REVEALED_RANGED` and `REVEALED_MELEE` based on closest threat, but with **20-tick (1s) hysteresis** to prevent goal thrashing — after a swap, the next swap is locked out for 20 ticks. No going back to `DISGUISED` or `SUSPICIOUS` once revealed (the disguise is blown for this Thief's lifetime).

### Goal selectors

Goal selector (priority order, lower = higher priority):
1. `FloatGoal`
2. `FleeAndFireCrossbowGoal` — when `REVEALED_RANGED`
3. `BlackjackStrikeGoal` — when `REVEALED_MELEE`; uses `StunningMeleeGoal(thief, 2.0, 40, 24)` (damage, stun ticks, cooldown ticks)
4. `ReturnToHideoutGoal` — when `FLEEING` OR (carrying ≥1 stolen item AND no nearby threat)
5. `StealFromChestGoal` — when `DISGUISED` AND inventory has space; finds nearest unclaimed chest within 24 blocks
6. `WanderInVillageGoal` — custom wrapper class. When `DISGUISED`, delegates to vanilla `MoveThroughVillageGoal` (movementSpeed 0.6, distance 16); otherwise no-op. Lets us gate village-pacing on disguise state.
7. `LookAtPlayerGoal`
8. `RandomLookAroundGoal`

Target selector:
1. `HurtByTargetGoal`
2. `SecretGuardTargetGoal` — `DISGUISED` only; targets nearest Guard within 12 blocks IF no player has `canSee(thief)` AND player is within 32 blocks (don't bother with the secrecy gate if no players are around — just attack)
3. `NearestAttackableTargetGoal<Player>` — REVEALED states only; targets the player who hit them or who's been chasing them

### Disguise visuals

Two textures, swapped at runtime by the renderer based on synced state:

- `thief_disguised.png` — base villager-style texture with two pixel-level changes:
  - 2px black horizontal stripe across the eyes (eye mask, partially lowered)
  - Robe color shifted ~10% darker than vanilla villager green
- `thief_revealed.png` — full thief look:
  - Mask snapped up over face (eyes barely visible)
  - Black/dark-grey robe
  - Visible weapon-drawing pose handled by `CrossbowAttackMob` model logic for crossbow; blackjack rendered via `HeldItemLayer`

Reveal transition is a 0.5s (10-tick) animation:
- Tick 0: state synced to client
- Ticks 0-10: client renderer interpolates a Y-axis head bob + texture fade between the two textures (simple alpha lerp on overlay layer)
- Tick 10: full reveal texture; weapon model appears in hand

### Hideout chest

#### Placement (called once at spawn)

`HideoutPlacer.place(ServerLevel, BlockPos spawnPos)`:

1. Sample up to 20 candidate positions in a 16-32 block radius from spawn (random offset, prefer further from spawn).
2. For each candidate, validate:
   - Block at candidate is replaceable (`Blocks.AIR` or `CAVE_AIR`)
   - Block above candidate is opaque (`isSolidRender`) — provides visual cover
   - Block below candidate is solid (chest needs support)
   - Candidate is NOT within 8 blocks of any village POI (`PoiManager`)
3. First valid candidate: place `Blocks.CHEST` with random horizontal facing.
4. Return `Optional<BlockPos>`.

If no valid candidate found in 20 attempts, return `Optional.empty()` — Thief spawn fails (entity discarded). Spawn-egg use prints a chat message: "No suitable hideout location nearby."

#### Persistence

`ThiefEntity` adds NBT field `hideout_pos` (`BlockPos` or absent). Saved/loaded with the entity.

If the chest at `hideout_pos` is broken by the player while the Thief is alive, the Thief loses its hideout (NBT cleared) and goes into a permanent `REVEALED_RANGED` state until killed — it has nothing to lose.

#### Deposit

`ReturnToHideoutGoal`:
1. Path to `hideout_pos`.
2. On arrival (within 1.5 blocks): open chest (1s animation), transfer all items from Thief inventory → chest inventory, close chest.
3. If chest is full, drop overflow on the ground at the chest's position.

Hideout chest **persists after the Thief dies.** The player can find and loot it. (Recovery mechanic + loot incentive for the player.)

### Items

#### Blackjack
- Class `BlackjackItem extends Item` (no `SwordItem` parent — we don't want vanilla sword behavior like blocking)
- Damage: 2 (set via `Item.Properties.attributes` with an `ItemAttributeModifiers` builder)
- On-hit effect: `MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)` (Slowness II, 2s) applied via `Item.hurtEnemy` override
- Stack size: 1
- Texture: `blackjack.png` (small dark-leather sap, ~6×3 pixels)
- Model: standard handheld item model

#### Recipe (datagen)
```
Slot pattern:
. L .
. I .

L = leather
I = iron ingot
Result: 1 blackjack
```

### Loot table

`thief:entities/thief`:
- 1× emerald, 50% chance, looting bonus +1 per level
- 0-2× arrow, 60% chance
- 1× blackjack, 25% chance
- **Plus everything in the Thief's inventory at death** (the items it stole and hadn't yet deposited). Implemented by overriding `dropCustomDeathLoot` to dump inventory contents.

### Natural spawn

`SpawnPlacements.register(THIEF, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ThiefEntity::checkSpawnRules)`:
- `checkSpawnRules`: must be on a village structure tag, light level any, surface only.
- Spawn weight: 5 (low), pack size: 1.
- Mob category: `MONSTER` so it counts toward and is gated by the monster cap.

**Repopulation tick** (registered via `ServerTickEvent` or a custom periodic check):
- Every 6000 ticks (5 in-game minutes):
  - For each loaded `ServerLevel`, iterate `ChunkPos` of village structure starts.
  - For each village: count Thieves within structure bounding box. If 0, roll 10% chance to spawn one at a random villager bed POI.

Spawn egg always works regardless of structure or rules.

## Multi-mod gradle wiring

### settings.gradle (root)
```gradle
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://maven.neoforged.net/releases' }
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
}

rootProject.name = 'security-pack'
include 'securitycore'
include 'securityguard'
include 'thief'
```

### build.gradle (root)
```gradle
allprojects {
    apply plugin: 'java'
    apply plugin: 'net.neoforged.gradle.userdev'

    java.toolchain.languageVersion = JavaLanguageVersion.of(21)

    repositories {
        mavenCentral()
        maven { url = 'https://maven.neoforged.net/releases' }
    }
}
```

### securitycore/build.gradle
- Standard NeoForge mod build.
- Produces a jar that other modules consume.
- Has its own `neoforge.mods.toml` declaring `modId = securitycore`.

### securityguard/build.gradle
```gradle
dependencies {
    implementation project(':securitycore')
}
```
Plus existing NeoForge config moved/inherited from root.

### thief/build.gradle
```gradle
dependencies {
    implementation project(':securitycore')
}
```

### Mod load-order
`thief` and `securityguard` both declare `securitycore` as a required mod dependency in their `neoforge.mods.toml`:
```toml
[[dependencies.thief]]
modId = "securitycore"
type = "required"
versionRange = "[0.1.0,)"
ordering = "AFTER"
side = "BOTH"
```

## Testing

### Unit (JUnit, in `thief/src/test/`)
- `RevealStateTest` — pure state-machine transitions (given current state + trigger → expected next state)
- `HideoutPlacerTest` — given a seeded fake `LevelReader`, returns expected position
- `SecretGuardTargetGoalTest` — `canUse()` returns false when mocked player has line-of-sight; true otherwise

### Manual in-game test plan
1. **Spawn-egg basics:** spawn a Thief in a flat village; confirm villager-like texture with eye-mask stripe; confirm wandering behavior; confirm a hideout chest gets placed nearby (use F3 + nearby chest scan).
2. **Chest theft:** place a chest with iron ingots; observe Thief approach, open, take, close, walk to hideout chest, deposit.
3. **Player observation:** stand close while Thief opens chest → confirm reveal triggers; flee further away during chest-open → confirm SUSPICIOUS state then return to DISGUISED.
4. **Distance-based weapon switch:** approach revealed Thief from 10 blocks → crossbow; close to 3 blocks → swap to blackjack.
5. **Stun on blackjack hit:** confirm Slowness II applied for 2s.
6. **Secret guard attack:** spawn Thief + Guard out of player line-of-sight (e.g. behind a wall) → observe Thief drawing crossbow on Guard (transition to REVEALED). Walk into view → confirm Thief stays REVEALED (the attack already broke cover) and continues to fight Guard. Then teleport away → Thief should NOT return to DISGUISED.
7. **Hideout recovery:** kill Thief carrying iron ingots → confirm items drop; locate hideout chest → confirm previously-deposited items present.
8. **Hideout destruction:** while Thief is alive, break its hideout chest → confirm Thief enters permanent revealed state.
9. **Natural spawn:** generate a fresh village; wait or `/locate` it; check for a Thief presence after a few in-game days.

### Build verification
- `./gradlew :securitycore:build :securityguard:build :thief:build` all succeed
- `./gradlew runData` for `:thief` produces expected JSON (recipe, loot table, language)
- Existing `:securityguard` tests still pass after refactor

## Open questions / risks

1. **Multi-module NeoForge in dev.** The `runClient` and `runData` tasks need to know to load all three mods together. May require `runs { client { modSources = [...] } }` to include all module source sets. Will validate during implementation.
2. **`MobCategory.MONSTER` despawn behavior.** Thieves count toward the hostile cap and despawn at distance like zombies. This may be wrong long-term — natural-spawn villager replacements should arguably persist. Acceptable for v1; revisit if testing shows villages emptying out.
3. **`canSee` performance.** `SecretGuardTargetGoal` does a `Player.canSee(thief)` check across all loaded players each tick. With many Thieves + many players, this is O(N×M). Acceptable for typical SP/small SMP. If profiling shows hot path, throttle to every 5 ticks.
4. **Hideout chest griefing/orphaning.** A killed Thief leaves its chest behind forever. World fills up over decades of play. Acceptable: it's a free chest for the player. Not addressing in v1.
