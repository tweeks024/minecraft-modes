# Wild-West Mod — Steve Stacker Boss — Design

**Date:** 2026-05-08
**Status:** Draft (pending user review of this doc)
**Series:** Stand-alone feature on top of merged Wild-West phases 1–3 + zombie-virus. No phase number.

## Goal

Add a new boss mob to the `:wildwest` mod called **Steve Stacker**: three vanilla-looking Steves stacked vertically into one tall entity. As the boss takes damage, the top Steve "falls off" at health thresholds, shrinking the visible stack from 3 → 2 → 1 and ramping the boss's speed and attack damage with each phase. Combat is melee only (no items, no ranged). The boss has a vanilla-style boss bar, drops diamonds on death, and can be encountered via spawn egg or as a very rare natural night-time monster spawn in plains/savanna.

## Platform

Java 25 / NeoForge (the version is read from `gradle.properties` `neo_version`; matches the existing `:wildwest` module). Minecraft version is whatever NeoForge ships with this `neo_version`; the design avoids hard-coded version assumptions and only uses APIs that are already in use elsewhere in the module (e.g., `Identifier.fromNamespaceAndPath`, `HumanoidRenderState`, `HumanoidMobRenderer`, `DeferredRegister`).

## In scope

### Entity

- New entity id: `wildwest:steve_stacker`.
- New class `com.tweeks.wildwest.entity.SteveStackerEntity` extends `net.minecraft.world.entity.monster.Monster` (mirrors `WalkerEntity` pattern; not a `WildWestMob` subclass because it carries no gun/knife and is not an `Outlaw`).
- `MobCategory.MONSTER`.
- Hitbox: width `0.6f`, height `5.85f` (three vanilla humanoid heights, 3 × 1.95). The hitbox stays fixed across phases — only the **render** shrinks. Rationale: shrinking the hitbox mid-fight would let the player walk through the head of the stack mid-phase-transition or get stuck inside a previously occupied volume. Fixed bbox keeps collisions predictable.
- `clientTrackingRange`: 10. `updateInterval`: default.
- Knockback resistance: `0.6` (boss should not be punted around like a normal mob, but is not immune).
- Follow range: `40.0`.
- Total HP: `90.0`.
- Attack damage attribute: `4.0` (overridden per phase, see below).
- Movement speed attribute: `0.25` (overridden per phase, see below).
- Fire-damage handling: default (boss can burn).
- Despawn: `setPersistenceRequired()` is called in the constructor so the boss does not despawn when the player walks away (consistent with bosses; matches the pattern players expect from named boss bars).

### Phases

Synced via `EntityDataAccessor<Byte>` named `STACK_HEIGHT` (values `3`, `2`, `1`). Stored on `SynchedEntityData` so the client renderer can read it without server round-trip per frame. Saved to NBT as `StackHeight` so phase persists across world reload.

Phase thresholds, evaluated in `aiStep()` server-side after `super.aiStep()`:

| Phase | Stack height | Triggers when health ≤ | Speed | Attack damage |
|------:|-------------:|-----------------------:|------:|--------------:|
| 1     | 3            | (initial)              | 0.25  | 4.0           |
| 2     | 2            | 60.0                   | 0.30  | 6.0           |
| 3     | 1            | 30.0                   | 0.38  | 8.0           |

Transition logic:

- The check fires **once per threshold**: compare the synced `STACK_HEIGHT` value against the band the current health falls into. If the new band is lower than the stored value, run the transition.
- Transition steps (server-side):
  1. Update `STACK_HEIGHT` data accessor.
  2. Set `Attributes.MOVEMENT_SPEED` base value to the new phase's speed.
  3. Set `Attributes.ATTACK_DAMAGE` base value to the new phase's attack damage.
  4. Spawn a `ParticleTypes.POOF` burst at the position of the Steve that just fell off (top of the current visible stack — y offset = phase-dependent).
  5. Play `SoundEvents.GENERIC_EXPLODE` at low volume (`0.6f`) and slightly high pitch (`1.2f`) at the boss's position.
  6. No new entity is spawned for the "fallen" Steve. The visual disappearance plus particle/sound is enough; spawning a corpse-Steve was considered and rejected (extra entity churn, no mechanical value).

Phase transitions are **monotonic**: if the boss is healed (e.g., regeneration potion via console), the stack does not grow back. Once a Steve has fallen off, it stays off. This avoids needing to re-trigger reverse transitions or worry about regeneration loopholes.

### HP & damage

- Single HP pool. Standard `hurt()` flow — no overrides for damage routing.
- No "burst damage skips a phase" mitigation: a single 70-damage hit takes the boss from full HP to phase 3 in one frame. That's accepted; the design picks Wither-style threshold simplicity over multi-bucket complexity (per the brainstorm).

### Combat / AI

`registerGoals()` registers, in priority order:

1. `FloatGoal(this)` — priority 0. Boss does not drown.
2. `MeleeAttackGoal(this, /*speedModifier=*/1.0, /*followingTargetEvenIfNotSeen=*/true)` — priority 1.
3. `WaterAvoidingRandomStrollGoal(this, /*speed=*/0.6)` — priority 7.
4. `LookAtPlayerGoal(this, Player.class, /*range=*/16.0f)` — priority 8.
5. `RandomLookAroundGoal(this)` — priority 8.

Target selection:

1. `HurtByTargetGoal(this)` — priority 1.
2. `NearestAttackableTargetGoal<>(this, Player.class, true)` — priority 2.

No ranged goal, no item-in-hand check. Punch attack uses vanilla `doHurtTarget(...)` from `Mob`.

### Boss bar

- Server side: `ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossBarColor.PURPLE, BossBarOverlay.PROGRESS);`
- `setCustomName(Component.translatable("entity.wildwest.steve_stacker"))` is called in the constructor; the `getDisplayName()` will surface it.
- `startSeenByPlayer` / `stopSeenByPlayer` are overridden to add/remove the player from the bar.
- `aiStep` updates `bossBar.setProgress(getHealth() / getMaxHealth())` once per server tick.
- `bossBar.setName(getDisplayName())` is called once in the constructor (after `setCustomName`); we do not refresh the name each tick.

### Visuals

- **Texture:** uses the vanilla default Steve skin. To avoid depending on vanilla's exact texture path (which has shifted across versions — `entity/steve.png` vs `entity/player/wide/steve.png` etc.), we ship a copy of the default Steve skin at `assets/wildwest/textures/entity/steve_stacker.png`. This is a single 64×64 PNG that is byte-equivalent to the default Steve skin Mojang ships. Rationale: vendoring this 1 KB texture is cheaper than handling version-specific path resolution and guarantees the boss looks the same on every Minecraft build the mod runs on.
- **Model:** new class `com.tweeks.wildwest.client.model.SteveStackerModel` extends `net.minecraft.client.model.EntityModel<SteveStackerRenderState>` (a custom render state — see below). The mesh is built by composing **three separate humanoid skeletons** (head + body + arms + legs) stacked vertically: the bottom Steve's feet at the entity render origin (Y = 0), the middle Steve sitting on top of the bottom Steve's head, and the top Steve sitting on top of the middle's head. The exact pixel offsets are implementation detail; the constraint is that each sub-Steve is one vanilla humanoid (1.95 blocks, ~31 pixels) and they stack contiguously.
  - `createBodyLayer()` builds a `MeshDefinition` whose root has three sub-roots named `steve_top`, `steve_mid`, `steve_bot`. Each sub-root contains the same humanoid layout (head with hat overlay disabled, body, two arms, two legs) using vanilla cube coordinates copied from `HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f)`. Texture is the standard 64×64 Steve UV layout, repeated for all three.
  - **Texture choice:** all three sub-models sample the same 64×64 sheet. We accept that the three Steves are visually identical; per-Steve variation is explicitly out of scope.
  - The model's `setupAnim(...)` advances each sub-model's limbs based on `state.walkAnimationPos` / `state.attackTime`. The top stack's limbs lead the animation by a small phase offset (`+0.3` radians) and the bottom by `-0.3` to give the stack a wobble effect. Sub-Steves do not animate independently of the parent (e.g., they don't punch on their own — only the visible top of the stack appears to swing on attack).
  - On render, the model **always shrinks from the top** (sub-root positions are fixed; only visibility flags change):
    - `stackHeight == 3`: `steve_top`, `steve_mid`, `steve_bot` all visible.
    - `stackHeight == 2`: `steve_top.visible = false`; `steve_mid` and `steve_bot` remain visible at their original (lower) Y positions. The visible stack is now the bottom two Steves.
    - `stackHeight == 1`: `steve_top.visible = false` and `steve_mid.visible = false`; only `steve_bot` is visible at its original ground-level Y position.
  - Visibility is set in `setupAnim`, not in the mesh definition. Hiding parts via `ModelPart.visible = false` is a hot-path-safe pattern used by vanilla.
- **Render state:** new `SteveStackerRenderState` extends `HumanoidRenderState` and adds a `public byte stackHeight;` field. The renderer's `extractRenderState` overrides write `state.stackHeight = entity.getStackHeight();`.
- **Renderer:** new `com.tweeks.wildwest.client.SteveStackerRenderer` extends `MobRenderer<SteveStackerEntity, SteveStackerRenderState, SteveStackerModel>` (using `MobRenderer` rather than `HumanoidMobRenderer` because we are not a vanilla humanoid model — we have three humanoid sub-trees). Shadow size `0.5f`. `getTextureLocation` returns the steve_stacker texture identifier.
- **Layer registration:** `SteveStackerModel.LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath(MOD_ID, "steve_stacker"), "main")`. Registered in `ClientSetup.registerLayerDefinitions` and `ClientSetup.registerRenderers`.

### Spawning

Two paths, both registered:

**A. Spawn egg.** New item `wildwest:steve_stacker_spawn_egg` registered in `Registration.java` and added to the creative tab `WILDWEST_TAB` after `WALKER_SPAWN_EGG`. Standard `SpawnEggItem` constructor.

**B. Natural rare spawn.** Data-driven via NeoForge biome modifier JSON, mirroring the walker pattern.

- File: `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/steve_stacker_spawns.json`.
- Biomes: `minecraft:plains`, `minecraft:savanna`, `minecraft:savanna_plateau`. (No desert — design choice; desert already has walker pressure, and the boss would be hard to see at night through the haze.)
- Spawner: `{ "type": "wildwest:steve_stacker", "weight": 1, "minCount": 1, "maxCount": 1 }`. Pack size is locked at 1; spawning multiple stackers in one chunk would be a nightmare.
- Spawn placement: registered in `WildWestMod.registerSpawnPlacementsEvent` block alongside the existing entries:
  - `SpawnPlacementTypes.ON_GROUND`
  - `Heightmap.Types.MOTION_BLOCKING_NO_LEAVES`
  - Predicate: `Monster::checkMonsterSpawnRules` (vanilla — requires light level ≤ 7, valid spawn block, etc.)

The boss bar attached to natural spawns can theoretically appear unannounced when a player wanders near one. That's accepted; the rarity (weight 1) makes this very infrequent, and the alternative (some kind of "approaching boss" warning) is out of scope.

### Loot

- New loot table: `wildwest/src/main/resources/data/wildwest/loot_table/entities/steve_stacker.json`.
- Drops on death:
  - `minecraft:diamond` × 3 (fixed; one per Steve in lore).
  - XP via `setExperienceReward(50)` in entity (not the loot table — XP is set directly on the entity in `getBaseExperienceReward()` override returning `50`).
- No conditional drops (looting bonus, only-killed-by-player, etc.) — keep it simple.

### Sounds

Reuse vanilla sounds; no new sound assets:

- Hurt: `SoundEvents.PLAYER_HURT` (it's a stack of Steves — the existing `getHurtSound` defaults to a generic sound; we override to use `PLAYER_HURT` for theme).
- Death: `SoundEvents.PLAYER_DEATH`.
- Ambient: `SoundEvents.PLAYER_BREATH` (low volume `0.4f`; called via the default ambient sound interval).
- Attack hit: vanilla `Mob.doHurtTarget` plays its own sound — no override needed.

### Lang strings

Add to `wildwest/src/main/resources/assets/wildwest/lang/en_us.json`:

- `entity.wildwest.steve_stacker`: `"Steve Stacker"`
- `item.wildwest.steve_stacker_spawn_egg`: `"Steve Stacker Spawn Egg"`

### Out of scope

The following are explicitly **not** in this spec:

- Custom skin variants per Steve (cowboy hats, vests, etc.). All three Steves are visually identical default Steves.
- AOE attacks, stomp shockwaves, knockback specials.
- Per-phase attack pattern changes (only attribute scaling changes per phase).
- Custom death cinematic, custom music, custom particles beyond the per-phase poof.
- Bosses-can-be-infected-by-zombie-virus interaction (already shipped in `155e420`; this design does not need to do anything special — `SteveStackerEntity` will be infectable by virtue of being a `LivingEntity` with no `InfectionImmunity` membership).
- Crafting recipe to summon (spawn egg + natural spawn cover both flavors).
- Boss-bar animation effects (color shifts on phase transition, etc.).

## File-level changes

**New files:**

- `wildwest/src/main/java/com/tweeks/wildwest/entity/SteveStackerEntity.java`
- `wildwest/src/main/java/com/tweeks/wildwest/client/model/SteveStackerModel.java`
- `wildwest/src/main/java/com/tweeks/wildwest/client/SteveStackerRenderer.java`
- `wildwest/src/main/java/com/tweeks/wildwest/client/SteveStackerRenderState.java`
- `wildwest/src/main/resources/assets/wildwest/textures/entity/steve_stacker.png` (vendored default Steve skin, 64×64)
- `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/steve_stacker_spawns.json`
- `wildwest/src/main/resources/data/wildwest/loot_table/entities/steve_stacker.json`

**Modified files:**

- `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java` — register `STEVE_STACKER` entity type with sized(0.6f, 5.85f).
- `wildwest/src/main/java/com/tweeks/wildwest/Registration.java` — register `STEVE_STACKER_SPAWN_EGG` item; add to creative tab.
- `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java` — `registerEntityAttributes` adds `SteveStackerEntity.createAttributes().build()`; `RegisterSpawnPlacementsEvent` listener adds entry for `STEVE_STACKER`.
- `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java` — register renderer + layer definition.
- `wildwest/src/main/resources/assets/wildwest/lang/en_us.json` — add two lang keys.

## Testing strategy

The repo's existing testing pattern (visible from `FactionPredicateTest.java` and the zombie-virus tests) is JUnit-style unit tests for pure logic. Tests for this feature:

1. **`SteveStackerPhaseLogicTest`** — pure unit test for the phase transition function. Extract a static helper `static byte computeStackHeight(float health, float maxHealth)` on `SteveStackerEntity` so it can be tested without instantiating the entity. Cases:
   - Full HP → `3`.
   - HP just above 60.0 → `3`.
   - HP at 60.0 → `2`.
   - HP at 30.01 → `2`.
   - HP at 30.0 → `1`.
   - HP at 0 → `1`.
   - Negative HP (overflow death) → `1`.
2. **Manual / integration testing** (documented in plan, not automated):
   - Spawn via egg in creative, observe stack of 3 Steves.
   - Damage with creative debug stick / commands until phase 2 transition; verify particle, sound, speed/damage attribute change, model shrink.
   - Continue to phase 3, then kill — verify diamond drop and XP reward.
   - Verify boss bar appears for nearby player and disappears on death.
   - Verify saved/loaded world preserves `StackHeight` NBT.
   - Verify natural spawning by setting a debug command spawn rate (or adjusting weight temporarily) in plains biome at night.

No mocking-of-Minecraft tests for the AI goals or render pipeline — those are integration concerns and the manual checklist covers them.

## Risks / open questions

- **Tall hitbox + indoor spawning.** A 5.85-block-tall mob spawning under 2-block ceilings could glitch into terrain. Mitigation: `Monster::checkMonsterSpawnRules` already requires the spawn block + 2 above to be passable; we additionally rely on the biome modifier targeting open biomes (plains/savanna). If players still report clipping, future work could add a custom spawn predicate that checks 6 blocks of vertical clearance — flagged but not implemented here.
- **Boss bar on natural spawn.** If a player wanders into a stacker's spawn radius unaware, the bar will pop in suddenly. Accepted; rarity makes this rare.
- **Vendored Steve texture.** If Mojang updates the default Steve skin in a future MC version, the boss will look "old." Accepted as the cost of version-independence.
