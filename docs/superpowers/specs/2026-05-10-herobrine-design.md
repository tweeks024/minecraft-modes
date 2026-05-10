# Wild-West Mod — Herobrine Boss — Design

**Date:** 2026-05-10
**Status:** Draft (pending user review of this doc)
**Series:** Stand-alone feature on top of merged Wild-West phases 1–3 + zombie-virus + Steve-Stacker. No phase number.

## Goal

Add a new apex boss mob to the `:wildwest` mod called **Herobrine**: a singleton, world-wide legendary monster that spawns rarely at night under open sky. He teleports frequently, swings a netherite sword in melee, summons lightning at distant targets, and rains meteors that create magma-and-fire hazard zones around him. On death he drops a `Meteor Staff` weapon (player-fired meteors, 10 hearts direct-hit damage), a diamond block, and (with low chance) his enchanted netherite sword.

## Platform

Java 25 / NeoForge (`neo_version` from `gradle.properties`). Matches the existing `:wildwest` module. Avoid hard-coded MC version assumptions; use only APIs already in use elsewhere in the module (`Identifier.fromNamespaceAndPath`, `HumanoidMobRenderer`, `DeferredRegister`, `SavedData`, biome-modifier JSON, etc.).

## In scope

### Entity

- New entity id: `wildwest:herobrine`.
- New class `com.tweeks.wildwest.entity.HerobrineEntity extends net.minecraft.world.entity.monster.Monster` (mirrors `SteveStackerEntity` pattern; not a `WildWestMob` subclass — Herobrine is its own thing).
- `MobCategory.MONSTER`.
- Hitbox: `0.6 × 1.95` (vanilla humanoid). Static — no dynamic dimensions.
- `clientTrackingRange`: 10. `updateInterval`: default.
- Knockback resistance: `0.8`.
- Follow range: `64.0`.
- Total HP: `200.0`.
- Attack damage attribute: `10.0` (sword adds enchant damage on top via Sharpness V).
- Movement speed attribute: `0.35`.
- Fire damage handling: **immune** (`isInvulnerableTo` returns true for `DamageTypeTags.IS_FIRE`). He summons fire and rains meteors; he should not damage himself with his own hazards.
- `setPersistenceRequired()` in constructor (boss must not despawn).

### Singleton mechanic (world-wide, persistent)

The "only one Herobrine in the world" rule is enforced via a per-server `SavedData` record anchored to the overworld dimension's data storage, so reads/writes from any dimension consult the same record.

- New class `com.tweeks.wildwest.entity.HerobrineSavedData extends net.minecraft.world.level.saveddata.SavedData`.
- Stored at `server.overworld().getDataStorage()` under key `"wildwest_herobrine"`.
- Fields:
  - `boolean alive`
  - `UUID currentId` (the alive entity's UUID; meaningful only when `alive == true`)
  - `ResourceKey<Level> dimension` (where the alive entity lives; meaningful only when `alive == true`)
- Static accessor: `static HerobrineSavedData get(MinecraftServer server)`.
- Writes go through `setDirty()`.
- NBT keys: `Alive`, `CurrentId`, `Dimension`.

Lifecycle:

1. **On spawn (entity construct, server-side, in `finalizeSpawn`):** if `alive == true` and the existing entity is not this one, discard self via `discard()`. If `alive == false` or already-this-entity, set `alive = true`, `currentId = this.getUUID()`, `dimension = level().dimension()`.
2. **On death:** override `die(DamageSource)` to clear `alive = false` (and call `super.die(...)`).
3. **On removal for any reason** (chunk unload counts as `RemovalReason.UNLOADED_TO_CHUNK` and must NOT clear the flag; `KILLED` and `DISCARDED` should). Override `remove(RemovalReason reason)`: if `reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED`, clear the flag. For `UNLOADED_*`, leave it alone.
4. **Recovery** (flag stuck `true` across crash where entity was never properly removed): accept the rare bug. A `/kill @e[type=wildwest:herobrine]` will resolve via the `KILLED` removal reason. Documented as a known edge case.

### Spawning

Two paths, both registered:

**A. Natural rare spawn.** Data-driven via NeoForge biome modifier JSON.

- File: `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/herobrine_spawns.json`.
- Biome tag: `#minecraft:is_overworld` (all overworld biomes).
- Spawner: `{ "type": "wildwest:herobrine", "weight": 1, "minCount": 1, "maxCount": 1 }`. Pack size locked at 1 (singleton).
- Spawn placement (registered in `WildWestMod.registerSpawnPlacementsEvent`):
  - `SpawnPlacementTypes.ON_GROUND`
  - `Heightmap.Types.MOTION_BLOCKING_NO_LEAVES`
  - Predicate: `HerobrineEntity::checkHerobrineSpawnRules` — composes:
    - `Monster::checkMonsterSpawnRules` (light ≤ 7, valid block, etc.)
    - `level.canSeeSky(pos)` (open sky required — mythic, sky-watcher flavor)
    - `HerobrineSavedData.get(server).alive == false` (singleton gate)

**B. Spawn egg with singleton-aware behavior.** New item `wildwest:herobrine_spawn_egg`.

- Class `HerobrineSpawnEggItem extends SpawnEggItem`.
- Egg colors: primary `0x3F0000` (dark red), secondary `0xFFFFFF` (white).
- Override `useOn(UseOnContext context)`:
  - Server-side branch only (client returns `InteractionResult.PASS` so server runs the actual logic via the standard interaction flow).
  - Read `HerobrineSavedData.get(server)`.
  - **If `alive == false`:** delegate to `super.useOn(context)` (vanilla spawn flow). The entity's construct logic sets the singleton flag during `finalizeSpawn`.
  - **If `alive == true`:** resolve existing entity via `server.getLevel(savedData.dimension).getEntity(savedData.currentId)`.
    - **Entity loaded AND in same dimension as player:** call `herobrine.teleportTo(clickX + 0.5, clickY + 1, clickZ + 0.5)`. Spawn `ParticleTypes.PORTAL` burst at both source and destination. Play `SoundEvents.ENDERMAN_TELEPORT` at the destination. **Egg is not consumed.** Return `InteractionResult.SUCCESS`.
    - **Entity unloaded (`getEntity` returns null) OR cross-dimension:** refuse with feedback message `Component.translatable("item.wildwest.herobrine_spawn_egg.away")` shown via `player.displayClientMessage(message, true)` (above hotbar). Egg is not consumed. Return `InteractionResult.FAIL`.
- Creative tab: `WILDWEST_TAB`, after `STEVE_STACKER_SPAWN_EGG`.

### Combat / AI

`registerGoals()` registers, in priority order:

1. `FloatGoal(this)` — priority 0.
2. `HerobrineMeleeGoal` (custom; below) — priority 1.
3. `HerobrineLightningGoal` (custom; below) — priority 2.
4. `HerobrineMeteorGoal` (custom; below) — priority 2.
5. `HerobrineTeleportGoal` (custom; below) — priority 3.
6. `LookAtPlayerGoal(this, Player.class, 16.0f)` — priority 8.
7. `RandomLookAroundGoal(this)` — priority 8.

Target selection:

1. `HurtByTargetGoal(this)` — priority 1.
2. `NearestAttackableTargetGoal<>(this, Player.class, true)` — priority 2.

#### HerobrineMeleeGoal

Engages when target distance ≤ 4 blocks. Internally a thin wrapper around `MeleeAttackGoal` semantics: paths to target, swings sword on cooldown via `doHurtTarget`. Disable at distance > 5 to prevent thrashing across the threshold.

#### HerobrineLightningGoal

- Cooldown: 100 ticks (5 s). Tracked as `int lightningCooldown`.
- `canUse()`: target alive, target distance > 8 blocks, `lightningCooldown == 0`, server-side has line-of-sight to target.
- `start()`: spawn vanilla `LightningBolt` at target's `position()` via `EntityType.LIGHTNING_BOLT.create(level)` + `level.addFreshEntity(...)`. Do NOT set `setVisualOnly(true)` — we want full lightning damage and fire. Reset cooldown to 100.
- `tick()`: no per-tick work; one-shot goal that ends after one fire.
- `canContinueToUse()`: returns false (one-shot).

Vanilla `LightningBolt` damage: 5 dmg + lights fire on flammable adjacent blocks. Acceptable as-is.

#### HerobrineMeteorGoal

- Cooldown: 160 ticks (8 s). Independent of lightning. Tracked as `int meteorCooldown`.
- `canUse()`: target alive, `meteorCooldown == 0`. (No range gate — meteors fall around Herobrine, not around the target.)
- `start()`: spawn one `MeteorEntity` at a random point in a horizontal ring **6–14 blocks** around Herobrine's `position()`, **30 blocks** above the highest non-air block at that XZ. Random angle uniform in `[0, 2π)`, random radius uniform in `[6, 14]`. Reset cooldown to 160.
- `canContinueToUse()`: false.

#### HerobrineTeleportGoal

- Cooldown: 80 ticks (4 s). Tracked as `int teleportCooldown`.
- `canUse()`: target alive, `teleportCooldown == 0`.
- `start()`:
  - Compute target distance.
  - Pick destination:
    - If distance > 12: pick point 6–10 blocks from target along a vector toward the target (close the gap).
    - Else if distance < 5: pick point 8–12 blocks away from target along the inverse vector (open the gap).
    - Else: pick random point 8–16 blocks from current position in a random horizontal direction.
  - Validate destination: snap Y to ground via `level.getHeightmapPos(MOTION_BLOCKING_NO_LEAVES, dest)`; verify 2 blocks of vertical air clearance; if invalid, retry up to 5 times then skip the teleport (cooldown still resets to avoid stuck loop).
  - Particle burst at source: 16× `ParticleTypes.PORTAL` in a 1-block sphere.
  - Move via `setPos(...)`; set `fallDistance = 0`.
  - Particle burst at destination: 16× `ParticleTypes.PORTAL`.
  - Play `SoundEvents.ENDERMAN_TELEPORT` at both endpoints (vol 0.8, pitch 1.0).
  - Reset cooldown to 80.
- `canContinueToUse()`: false.

All goal cooldowns decrement once per tick in their respective `tick()` callbacks (or via the entity's `aiStep` if simpler — implementation detail).

### MeteorEntity

- New class `com.tweeks.wildwest.entity.projectile.MeteorEntity extends net.minecraft.world.entity.projectile.Projectile`.
- Field: `int directHitDamage` (default `6`; setter for spawner override).
- Constructor: standard `(EntityType<? extends MeteorEntity> type, Level level)`. Plus a static factory or builder for setting velocity + damage at spawn.
- Spawned with explicit initial velocity supplied by the spawner (Herobrine summons → straight down 0 horizontal; staff → player look × 1.5).
- Gravity: enabled (`isNoGravity()` returns false).
- Visual: reuses vanilla `EntityType.FIREBALL` texture via a custom renderer that delegates to `ThrownItemRenderer` patterns. Implementation detail; the constraint is "looks like a fireball, ~0.5–1 block visual size."
- Collision behavior (`onHit(HitResult)`):
  - **`onHitBlock(BlockHitResult)`:**
    - Replace impact block with `Blocks.MAGMA_BLOCK.defaultBlockState()` *if* the impact block is not in `BlockTags.DRAGON_IMMUNE` (protect bedrock/end portal frame/etc.).
    - For each of the 4 horizontal neighbors of the impact block: if the neighbor is air and the block below is solid, set neighbor to `Blocks.FIRE.defaultBlockState()` via `BaseFireBlock.canBePlacedAt`-aware path (use `level.setBlockAndUpdate` only when `BaseFireBlock.canBePlacedAt(level, pos, Direction.UP)` is true).
    - AoE: `level.getEntities(this, AABB.ofSize(impactCenter, 4, 4, 4))` → for each `LivingEntity`, deal **6 damage** via custom damage source `WildWestDamageTypes.METEOR` (fire-typed). Note: AoE damage uses the fixed AoE value (6), not `directHitDamage` — the damage knob is for direct hits only.
    - Particle burst: 24× `ParticleTypes.LAVA` + 16× `ParticleTypes.LARGE_SMOKE` at impact center.
    - Sound: `SoundEvents.GENERIC_EXPLODE` at vol 1.0, pitch 0.8.
    - **NO actual `level.explode(...)` call.** No block destruction beyond the single impact block.
    - `discard()` self.
  - **`onHitEntity(EntityHitResult)`:**
    - Deal `directHitDamage` to the hit entity via `WildWestDamageTypes.METEOR`.
    - Then trigger the same impact effects as `onHitBlock` (magma at the entity's block-position-below, adjacent fire, AoE damage to OTHER entities — exclude the directly-hit entity from AoE to avoid double-damage).
    - `discard()` self.
- No tick-time entity-collision sweep; rely on engine's projectile movement + `onHit` dispatch.

### Equipment

- `populateDefaultEquipmentSlots(RandomSource, DifficultyInstance)`: main hand = `ItemStack` of `Items.NETHERITE_SWORD` with stored enchantments **Fire Aspect II + Sharpness V** applied via whichever enchantment API the active NeoForge version exposes (e.g., `ItemStack.enchant(Holder<Enchantment>, int)` or `EnchantmentHelper`-style; matches whatever the existing `:wildwest` codebase already uses for enchanted items if any). Off-hand: empty.
- Drop chance for the sword: **0.10** (10%) via `setDropChance(EquipmentSlot.MAINHAND, 0.10f)`. When it drops, the enchanted sword drops intact.

### Visuals

- **Texture:** Steve-skin variant with white glowing eyes. Vendored 64×64 PNG at `assets/wildwest/textures/entity/herobrine.png`. Created by hand (or tooled via the existing `tools/` workflow if there is one for Steve-skin variants — implementation detail). Same humanoid layout as default Steve, eye pixels overwritten with full white.
- **Eyes overlay:** second 64×64 PNG at `assets/wildwest/textures/entity/herobrine_eyes.png`. Eye pixels white, rest fully transparent. Used by an emissive overlay layer so eyes glow at full brightness regardless of ambient light.
- **Model:** reuse vanilla `HumanoidModel<HumanoidRenderState>`. No new model class needed — Herobrine is a single humanoid, not a stack.
- **Renderer:** new `com.tweeks.wildwest.client.HerobrineRenderer extends HumanoidMobRenderer<HerobrineEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>>`. Shadow size `0.5f`. `getTextureLocation` returns the herobrine texture identifier.
- **Eyes layer:** new `com.tweeks.wildwest.client.HerobrineEyesLayer extends RenderLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>>`. Renders the eyes texture using `RenderType.eyes(...)` (full-bright). Added to `HerobrineRenderer` constructor via `addLayer(new HerobrineEyesLayer(this))`.
- **Render state:** reuses vanilla `HumanoidRenderState` — no custom state needed (no per-entity client-only data).
- **Layer registration:** none required for the entity model (it's vanilla `HumanoidModel.LAYER_LOCATION_PLAYER` or the standard player layer; reuse via `context.bakeLayer(ModelLayers.PLAYER)`).

### MeteorEntity rendering

- Renderer: new `com.tweeks.wildwest.client.MeteorRenderer extends EntityRenderer<MeteorEntity, ProjectileRenderState>`. Render as an item-icon billboard using vanilla `Items.FIRE_CHARGE` icon, scaled to ~1 block. Shadow size `0.0f`.
- Render state: `ProjectileRenderState` from vanilla — sufficient (position + rotation).

### Boss bar

- Server-side: `ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossBarColor.RED, BossBarOverlay.PROGRESS);`
- `setCustomName(Component.translatable("entity.wildwest.herobrine"))` is called in the constructor; `getDisplayName()` surfaces it.
- `startSeenByPlayer` / `stopSeenByPlayer` are overridden to add/remove the player from the bar.
- `aiStep` updates `bossBar.setProgress(getHealth() / getMaxHealth())` once per server tick.
- `bossBar.setName(getDisplayName())` called once in constructor (after `setCustomName`); not refreshed each tick.

### Loot

- New loot table: `wildwest/src/main/resources/data/wildwest/loot_table/entities/herobrine.json`.
- Drops on death:
  - `minecraft:diamond_block` × 1 (always).
  - `wildwest:meteor_staff` × 1 (always).
- Sword drops via the equipment-slot drop chance (above), NOT via the loot table — same pattern as vanilla mob equipment.
- XP: override `getBaseExperienceReward()` to return `100`. Set directly on the entity, not the loot table.

### Meteor Staff item

- New item: `wildwest:meteor_staff`. Class `com.tweeks.wildwest.item.MeteorStaffItem extends Item`.
- Properties: `stackSize(1)`, no durability set → defaults to no durability (effectively unbreakable). Stack size enforced. Set `rarity(Rarity.EPIC)` for the purple name.
- Override `use(Level, Player, InteractionHand)`:
  - Server-side: spawn `MeteorEntity` at `player.getEyePosition()` with velocity `player.getLookAngle().scale(1.5)`. Set `directHitDamage = 20` on the entity. Set `setOwner(player)`.
  - Apply cooldown via `player.getCooldowns().addCooldown(this, 60)` (3 s).
  - Both sides: return `InteractionResultHolder.consume(stack)` to play swing animation.
- Texture: new 16×16 item PNG at `assets/wildwest/textures/item/meteor_staff.png`. Art is implementation detail; broadly orange/red staff with glowing tip.
- Item model JSON at `assets/wildwest/models/item/meteor_staff.json` — standard `item/handheld` parent for held-in-hand orientation.
- Creative tab: `WILDWEST_TAB`, after `HEROBRINE_SPAWN_EGG`.

### Damage type

- New damage type `wildwest:meteor` registered in `WildWestDamageTypes`.
- Tagged with `#minecraft:is_fire` (so fire-resistance protects against it; consistent with the impact's fire flavor).
- Tagged with `#minecraft:bypasses_armor`? **No** — armor should reduce meteor damage; this is a "molten rock falling on you" effect, not magic.
- Used by `MeteorEntity` for both direct-hit and AoE damage.

### Sounds

Reuse vanilla sounds; no new sound assets:

- Hurt: `SoundEvents.PLAYER_HURT` (he is a player-shaped entity).
- Death: `SoundEvents.WITHER_DEATH` (mythic resonance).
- Ambient: `SoundEvents.AMBIENT_CAVE` at vol 0.6, called via the default ambient sound interval.
- Teleport: `SoundEvents.ENDERMAN_TELEPORT` (in `HerobrineTeleportGoal`).
- Meteor impact: `SoundEvents.GENERIC_EXPLODE` (in `MeteorEntity.onHit`).
- Attack hit: vanilla `Mob.doHurtTarget` — no override.

### Lang strings

Add to `wildwest/src/main/resources/assets/wildwest/lang/en_us.json`:

- `entity.wildwest.herobrine`: `"Herobrine"`
- `item.wildwest.herobrine_spawn_egg`: `"Herobrine Spawn Egg"`
- `item.wildwest.herobrine_spawn_egg.away`: `"Herobrine is far away…"`
- `item.wildwest.meteor_staff`: `"Meteor Staff"`

### Out of scope

The following are explicitly **not** in this spec:

- Build-tracking, player-structure detection, targeted attacks on player builds. (Earlier draft considered this; the simplification "meteors fall around him randomly" replaces it.)
- Ritual / multi-block summoning structure.
- Custom death cinematic or custom music.
- Phase transitions or HP-threshold attribute changes (single HP pool; AI is distance-based, not HP-banded).
- Boss-bar animation on damage milestones (color shift, etc.).
- Cross-dimensional teleport from spawn egg (refused with feedback message).
- Recovery mechanism for stuck `alive == true` flag if the entity disappears via crash. Documented as known edge case; `/kill @e[type=wildwest:herobrine]` resolves.
- Looting enchantment effect on drops (drops are fixed quantities per the design).
- Spawn egg dispenser behavior (creative-only via tab; dispensers can use the default `SpawnEggItem` dispense behavior — singleton check fires via the entity's `finalizeSpawn`).
- Herobrine-themed structures, signs, or world-gen.
- Zombie-virus interaction beyond default — Herobrine is a `LivingEntity` and would be infectable, but per existing-design philosophy of "infectable unless explicitly immune," he stays infectable. (Visual oddness if it ever happens is accepted; the singleton + boss tier makes it vanishingly rare.)

## File-level changes

**New files:**

- `wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineEntity.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineSavedData.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineMeleeGoal.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineLightningGoal.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineMeteorGoal.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineTeleportGoal.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/projectile/MeteorEntity.java`
- `wildwest/src/main/java/com/tweeks/wildwest/item/MeteorStaffItem.java`
- `wildwest/src/main/java/com/tweeks/wildwest/item/HerobrineSpawnEggItem.java`
- `wildwest/src/main/java/com/tweeks/wildwest/client/HerobrineRenderer.java`
- `wildwest/src/main/java/com/tweeks/wildwest/client/HerobrineEyesLayer.java`
- `wildwest/src/main/java/com/tweeks/wildwest/client/MeteorRenderer.java`
- `wildwest/src/main/resources/assets/wildwest/textures/entity/herobrine.png` (vendored 64×64, white-eye Steve variant)
- `wildwest/src/main/resources/assets/wildwest/textures/entity/herobrine_eyes.png` (64×64, white eyes on transparent)
- `wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff.png` (16×16)
- `wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff.json`
- `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/herobrine_spawns.json`
- `wildwest/src/main/resources/data/wildwest/loot_table/entities/herobrine.json`
- `wildwest/src/main/resources/data/wildwest/damage_type/meteor.json`
- `wildwest/src/main/resources/data/minecraft/tags/damage_type/is_fire.json` (additive: `{"replace": false, "values": ["wildwest:meteor"]}` — extends vanilla `#minecraft:is_fire`)

**Modified files:**

- `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java` — register `HEROBRINE` entity type with `sized(0.6f, 1.95f)` and `clientTrackingRange(10)`; register `METEOR` entity type with `sized(0.5f, 0.5f)` and `clientTrackingRange(64)` (must be visible during 30-block fall, plus margin for late-loaded clients).
- `wildwest/src/main/java/com/tweeks/wildwest/Registration.java` — register `HEROBRINE_SPAWN_EGG` (custom subclass) and `METEOR_STAFF` items; add to creative tab `WILDWEST_TAB`.
- `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java` — `registerEntityAttributes` adds `HerobrineEntity.createAttributes().build()`; `RegisterSpawnPlacementsEvent` listener adds entry for `HEROBRINE`.
- `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java` — register `HerobrineRenderer` and `MeteorRenderer`.
- `wildwest/src/main/java/com/tweeks/wildwest/WildWestDamageTypes.java` — add `METEOR` resource key + registration.
- `wildwest/src/main/resources/assets/wildwest/lang/en_us.json` — add four lang keys (entity name, spawn egg name, spawn egg "away" message, meteor staff name).

## Testing strategy

The repo's existing testing pattern is JUnit-style unit tests for pure logic. Tests for this feature:

1. **`HerobrineSavedDataTest`** — pure unit test for the SavedData NBT round-trip and lifecycle helpers. Cases:
   - Default state: `alive == false`, `currentId == null`, `dimension == null`.
   - After `setAlive(uuid, dimensionKey)`: state reflects values; `setDirty` triggered (verify via mock or via the dirty flag if exposed).
   - After `clear()`: back to default.
   - NBT save → load round-trip preserves all three fields.
   - Loading from NBT with `Alive: false` ignores stale `CurrentId` / `Dimension` fields.
2. **`MeteorImpactLogicTest`** — extract a static helper that, given a `BlockState` and `BlockPos`, returns whether the impact block should be replaced with magma (i.e., not in `DRAGON_IMMUNE`). Tested with a mock registry-tag context. Cases:
   - Stone → replaceable.
   - Bedrock → not replaceable.
   - End portal frame → not replaceable.
   - Air → not replaceable (no impact in air).
3. **`HerobrineTeleportTargetTest`** — extract the destination-picking math into a pure helper `static Vec3 pickTeleportTarget(Vec3 selfPos, Vec3 targetPos, RandomSource rng)`. Cases:
   - Distance > 12 → result is between 6 and 10 blocks from target along self→target vector.
   - Distance < 5 → result is between 8 and 12 blocks from target along reverse vector.
   - Distance in [5, 12] → result is between 8 and 16 blocks from selfPos in some direction.
4. **Manual / integration testing** (documented in plan, not automated):
   - Spawn via egg in creative, observe singleton flag enforcement: try egg again, expect Herobrine teleports to click position.
   - Try spawn egg cross-dimension (spawn in overworld, switch to nether, use egg) — expect "far away" message.
   - Damage Herobrine in melee; observe sword swings, Fire Aspect ignites player.
   - Run away to range > 8; expect lightning strikes at player position within ~5 s.
   - Stay near Herobrine; observe meteors falling in 6–14 block ring around him; verify magma block spawns at impacts and adjacent blocks catch fire.
   - Kill Herobrine; verify singleton flag clears (try egg → spawns new one). Verify drops: meteor staff, diamond block, sometimes the netherite sword.
   - Use meteor staff: right-click in air, observe meteor entity flying forward in look direction; observe 3-second cooldown.
   - Hit a mob directly with meteor staff: verify 20 dmg to the hit entity plus magma/fire impact.
   - Save / load world while Herobrine is alive: verify SavedData persists, entity reloads correctly.
   - Verify natural spawning by adjusting weight temporarily and waiting a few in-game nights in plains.

No mocking-of-Minecraft tests for goals or render pipeline — those are integration concerns covered by the manual checklist.

## Risks / open questions

- **Singleton flag stuck `true`** if entity is removed by an unhandled path (server crash mid-tick where neither `die` nor `remove(KILLED)` ran, or a mod removing the entity bypassing both). Mitigation: `/kill @e[type=wildwest:herobrine]` triggers `KILLED`. If the issue becomes a real complaint, a future tweak could add a "no Herobrine entity loaded anywhere AND no chunk loaded that contains the saved position for N minutes → clear flag" recovery — explicitly out of scope here.
- **Meteor + village collateral damage.** Meteors falling in a 6–14 block ring around Herobrine WILL set fire to villages, woods, and player builds if Herobrine wanders into them. Accepted; matches the legend's "burns the world" flavor.
- **Lightning + raids / villager death cascades.** Vanilla lightning spawning near villagers can convert them to witches. Rare and on-flavor; accepted.
- **Open-sky spawn predicate** prevents underground spawns. Intentional. Combined with weight 1, expected spawn rate is "rare across many in-game nights of play."
- **Cross-dimension spawn egg refusal.** A player who has the egg in nether/end can't summon Herobrine there even if no Herobrine exists yet. Accepted limitation; the egg is a singleton-controller, not a free spawn.
- **Vendored Herobrine texture.** If Mojang updates the default Steve skin in a future MC version, Herobrine's base layer will look "old" relative to the default Steve skin. Same trade-off as Steve-Stacker; accepted.
- **Meteor Staff in PvP.** 20-damage direct hits + AoE fire is brutal in PvP. The staff is intended as a PvE trophy. Accepted; PvP balance is not a goal of this mod.
- **Meteor entity trajectory + far-fall.** A Herobrine-summoned meteor falls 30 blocks; if a player walks under it, the player can be directly hit even though it was meant to be ambient hazard. The 6-radius minimum offset from Herobrine reduces the chance the player is also there but does not eliminate it. Accepted; hazard zones are dangerous.
