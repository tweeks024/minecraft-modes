# Wild-West Mod — Entity 303 Boss — Design

**Date:** 2026-05-14
**Status:** Draft (pending user review of this doc)
**Series:** Stand-alone feature on top of merged Wild-West phases 1–3 + zombie-virus + Steve-Stacker + Herobrine. No phase number.

## Goal

Add a new apex boss mob to the `:wildwest` mod called **Entity 303**: a singleton, world-wide legendary monster — peer of Herobrine — that spawns rarely at night on the overworld surface OR at any time in The End. He is an evasive, range-focused duelist: fires an enchanted bow from medium range, swaps to an iron sword in melee, and uses a "phantom swap" defensive mechanic — on receiving damage he occasionally spawns a 1-HP visual decoy at his current position and teleports 8–12 blocks behind the attacker. On death he drops a **Cursed Tome** (right-click ranged tool-wrecker), a diamond block, and (with low chance) his enchanted bow and/or iron sword.

303 is mechanically distinct from Herobrine: where Herobrine zone-controls with meteors and lightning, 303 plays keep-away with a bow and punishes commitment with the swap. Same singleton storage pattern, parallel apex tier.

## Platform

Java 25 / NeoForge (`neo_version` from `gradle.properties`). Matches the existing `:wildwest` module and the Herobrine implementation. Avoid hard-coded MC version assumptions; use only APIs already in use elsewhere in the module (`Identifier.fromNamespaceAndPath`, `HumanoidMobRenderer`, `DeferredRegister`, `SavedData`, biome-modifier JSON, etc.).

## In scope

### Entity

- New entity id: `wildwest:entity_303`.
- New class `com.tweeks.wildwest.entity.Entity303Entity extends net.minecraft.world.entity.monster.Monster` (mirrors `HerobrineEntity` pattern; not a `WildWestMob` subclass — 303 is its own thing).
- `MobCategory.MONSTER`.
- Hitbox: `0.6 × 1.95` (vanilla humanoid). Static — no dynamic dimensions.
- `clientTrackingRange`: 10. `updateInterval`: default.
- Knockback resistance: `0.6`.
- Follow range: `64.0`.
- Total HP: `160.0`.
- Attack damage attribute: `8.0` (iron sword adds enchant damage on top via Sharpness IV).
- Movement speed attribute: `0.45` (notably faster than Herobrine's 0.35 — fits the evasive-duelist theme).
- Fire damage handling: NOT immune. 303 has no fire-typed attacks of his own (bow w/ Flame I lights the *target* on fire, not the area around him); he should take normal fire damage.
- `setPersistenceRequired()` in constructor (boss must not despawn).

### Singleton mechanic (world-wide, persistent)

The "only one Entity 303 in the world" rule is enforced via a per-server `SavedData` record anchored to the overworld dimension's data storage, so reads/writes from any dimension consult the same record. **Note: this is a separate file from `HerobrineSavedData`** — both bosses can be alive simultaneously, but each is independently singleton.

- New class `com.tweeks.wildwest.entity.Entity303SavedData extends net.minecraft.world.level.saveddata.SavedData`.
- Stored at `server.overworld().getDataStorage()` under key `"wildwest_entity_303"`.
- Wraps a pure POJO `com.tweeks.wildwest.entity.Entity303State` (same pattern as `HerobrineState`/`HerobrineSavedData` — POJO is unit-tested, SavedData wraps it).
- Fields on the state:
  - `boolean alive`
  - `UUID currentId` (the alive entity's UUID; meaningful only when `alive == true`)
  - `String dimensionId` (the alive entity's dimension ID; meaningful only when `alive == true`; lookup translates to `ResourceKey<Level>` on the wrapper)
- Static accessor on the SavedData: `static Entity303SavedData get(MinecraftServer server)`.
- Writes go through `setDirty()`.
- NBT keys: `Alive`, `CurrentId`, `Dimension`.

> **Why a separate class instead of generic `BossSingletonState<T>`?** Refactoring into a shared generic boss-singleton class is tempting but out of scope for this spec. The Herobrine and 303 SavedData classes are short, parallel, and the duplication is honest and easy to inline-read. A unification pass can land later once a third singleton is on the table.

Lifecycle (mirrors Herobrine):

1. **On spawn (entity construct, server-side, in `finalizeSpawn`):** if `alive == true` and the existing entity is not this one, discard self via `discard()`. If `alive == false` or already-this-entity, set `alive = true`, `currentId = this.getUUID()`, `dimension = level().dimension()`.
2. **On death:** override `die(DamageSource)` to clear `alive = false` (and call `super.die(...)`).
3. **On removal for any reason** (chunk unload counts as `RemovalReason.UNLOADED_TO_CHUNK` and must NOT clear the flag; `KILLED` and `DISCARDED` should). Override `remove(RemovalReason reason)`:
   - If `reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED`, clear the singleton flag. For `UNLOADED_*`, leave it alone.
   - **Always (regardless of reason)** call `bossBar.removeAllPlayers()` before delegating to `super.remove(reason)`.
4. **Recovery** (flag stuck `true` across crash where entity was never properly removed): accept the rare bug. A `/kill @e[type=wildwest:entity_303]` will resolve via the `KILLED` removal reason. Documented as a known edge case.

### Spawning

Two paths, both registered:

**A. Natural rare spawn — two biome modifiers.**

Both modifiers reference the same entity type and the same `Entity303SpawnRules::checkSpawnRules` predicate. Singleton-gating means only one can fire at a time across both dimensions.

- File 1: `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/entity_303_overworld_spawns.json`
  - Biome tag: `#minecraft:is_overworld`.
  - Spawner: `{ "type": "wildwest:entity_303", "weight": 1, "minCount": 1, "maxCount": 1 }`. Pack size locked at 1 (singleton).
- File 2: `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/entity_303_end_spawns.json`
  - Biome tag: `#minecraft:is_end`.
  - Spawner: `{ "type": "wildwest:entity_303", "weight": 2, "minCount": 1, "maxCount": 1 }`. Higher weight to compensate for The End's sparser spawn surface area.

Spawn placement (registered once in `WildWestMod.registerSpawnPlacementsEvent` — same predicate covers both dimensions):

- `SpawnPlacementTypes.ON_GROUND`
- `Heightmap.Types.MOTION_BLOCKING_NO_LEAVES`
- Predicate: `Entity303SpawnRules::checkSpawnRules` — composes:
  - `Monster::checkMonsterSpawnRules` (light ≤ 7, valid block, etc.).
  - **Overworld branch:** require `level.canSeeSky(pos)` (mythic "night sky" flavor — overworld only).
  - **End branch:** no sky requirement. The End has no day/night cycle and partial sky; the light-level gate from `checkMonsterSpawnRules` is enough.
  - Branch detection: `level.getLevel().dimension() == Level.END`. If neither overworld nor end, reject (defensive — the biome modifiers shouldn't fire elsewhere, but defense-in-depth costs nothing).
  - `Entity303SavedData.get(server).isAlive() == false` (singleton gate).

**B. Spawn egg with singleton-aware behavior.** New item `wildwest:entity_303_spawn_egg`.

- Class `Entity303SpawnEggItem extends SpawnEggItem`.
- Egg colors: primary `0x000000` (black), secondary `0xCC0000` (red — the eyes).
- Override `useOn(UseOnContext context)`:
  - Server-side branch only (client returns `InteractionResult.PASS` so server runs the actual logic via the standard interaction flow).
  - **Dimension gate (applies to BOTH spawn and teleport branches):** if `level.dimension() != Level.OVERWORLD && level.dimension() != Level.END`, refuse with feedback `Component.translatable("item.wildwest.entity_303_spawn_egg.wrong_dimension")` ("Entity 303 does not walk here…") via `player.displayClientMessage(message, true)`. Egg not consumed. Return `InteractionResult.FAIL`. This blocks Nether use; natural spawn restricts to overworld + end, so the egg matches.
  - Read `Entity303SavedData.get(server)`.
  - **If `alive == false`:** delegate to `super.useOn(context)` (vanilla spawn flow). The entity's `finalizeSpawn` sets the singleton flag.
  - **If `alive == true`:** resolve existing entity via `server.getLevel(savedData.getDimension()).getEntity(savedData.getCurrentId())`.
    - **Entity loaded AND in same dimension as player** (cross-dim teleport is NOT allowed — egg can pull 303 from overworld to overworld or end to end, but not across the boundary): call `e303.teleportTo(clickX + 0.5, clickY + 1, clickZ + 0.5)`. Spawn `ParticleTypes.PORTAL` burst at both source and destination. Play `SoundEvents.ENDERMAN_TELEPORT` at the destination. **Egg is not consumed.** Return `InteractionResult.SUCCESS`.
    - **Entity unloaded** (`getEntity` returns null): refuse with feedback message `Component.translatable("item.wildwest.entity_303_spawn_egg.away")` via `player.displayClientMessage(message, true)`. Egg not consumed. Return `InteractionResult.FAIL`.
    - **Entity in a different dimension than the player** (e.g., 303 is alive in The End and player tries the egg in the overworld): refuse with `Component.translatable("item.wildwest.entity_303_spawn_egg.different_dimension")` ("Entity 303 walks elsewhere…"). Egg not consumed. Return `InteractionResult.FAIL`.
- Creative tab: `WILDWEST_TAB`, after `HEROBRINE_SPAWN_EGG`.

### Combat / AI

`registerGoals()` registers, in priority order:

1. `FloatGoal(this)` — priority 0.
2. `Entity303MeleeGoal` (custom; below) — priority 1.
3. `Entity303BowGoal` (custom; below) — priority 2.
4. `Entity303TeleportGoal` (custom; below) — priority 3.
5. `LookAtPlayerGoal(this, Player.class, 16.0f)` — priority 8.
6. `RandomLookAroundGoal(this)` — priority 8.

Target selection:

1. `HurtByTargetGoal(this)` — priority 1.
2. `NearestAttackableTargetGoal<>(this, Player.class, true)` — priority 2.

#### Entity303MeleeGoal

Engages when target distance ≤ 3 blocks (tighter than Herobrine's 4 — 303 is a ranged-first fighter, only goes melee when forced). Thin wrapper around `MeleeAttackGoal` semantics; swings iron sword on cooldown via `doHurtTarget`. Disables at distance > 4 to prevent thrashing across the threshold. Mainhand needs to be the sword for this goal — see **Equipment switching** below.

#### Entity303BowGoal

The primary attack. Custom goal (rather than vanilla `RangedBowAttackGoal` directly) so it owns the equipment-switching logic and the slightly customized cadence.

- Cooldown: 50 ticks (2.5 s). Tracked as `int bowCooldown`.
- Range: target distance in `[5.0, 20.0]` blocks (inclusive). Below 5 → melee takes over; above 20 → wait for teleport to close.
- `canUse()`: target alive, target distance ∈ `[5.0, 20.0]`, `bowCooldown == 0`, server-side line-of-sight to target.
- `start()`:
  - Ensure mainhand is the bow (swap via `setItemSlot(MAINHAND, bowStack)` if currently holding sword). See **Equipment switching**.
  - Spawn vanilla `Arrow` via `new Arrow(level, this, bowStack, null)`. Apply the enchantment effects (`EnchantmentHelper.processProjectileSpawn(...)` or whichever helper the active NeoForge version provides — mirrors how vanilla `Skeleton.performRangedAttack` shoots).
  - Compute trajectory: aim at target's position + small downward inheritance compensation, fixed `velocity = 1.6f`, `inaccuracy = 6.0f` (mirrors vanilla `Skeleton` hard difficulty inaccuracy).
  - Add to world via `level.addFreshEntity(arrow)`.
  - Play `SoundEvents.SKELETON_SHOOT` at vol 1.0, pitch random `0.8 + (random.nextFloat() * 0.4f)`.
  - Reset cooldown to 50.
- `tick()`: decrements cooldown via the entity's `aiStep` (shared counter, see **AI tick budget**).
- `canContinueToUse()`: false (one-shot).

The bow has Power V + Flame I (see Equipment) — vanilla's `EnchantmentHelper.processProjectileSpawn` applies both. Power V adds ~`0.5 × (V+1) × baseDamage` (vanilla scaling) and Flame I lights the target on fire for 5s.

#### Entity303TeleportGoal

Faster cadence than Herobrine's (3s vs 4s), simpler logic — 303 teleports to reposition, not to close gaps aggressively. Reuses `HerobrineTeleportTarget` pure helper directly (same close-gap / open-gap / random-reposition logic; the math is generic).

- Cooldown: 60 ticks (3 s). Tracked as `int teleportCooldown`.
- `canUse()`: target alive, `teleportCooldown == 0`.
- `start()`:
  - Pick destination via `HerobrineTeleportTarget.pick(selfX, selfZ, targetX, targetZ, rng)`. Reuse — the pure math is unchanged.
  - Validate destination: snap Y to ground via `level.getHeightmapPos(MOTION_BLOCKING_NO_LEAVES, dest)`; verify 2 blocks of vertical air clearance; if invalid, retry up to 5 times then skip the teleport (cooldown still resets to avoid stuck loop).
  - Particle burst at source: 16× `ParticleTypes.SMOKE` (NOT `PORTAL` — distinguishes 303's teleport from Herobrine's visually).
  - Move via `teleportTo(...)`; set `fallDistance = 0` (or version-equivalent).
  - Particle burst at destination: 16× `ParticleTypes.SMOKE`.
  - Play `SoundEvents.ENDERMAN_TELEPORT` at both endpoints (vol 0.6, pitch 1.2 — quieter and higher than Herobrine for distinguishable audio).
  - Reset cooldown to 60.
- `canContinueToUse()`: false.

> **Note on `HerobrineTeleportTarget` reuse:** the helper class lives under `com.tweeks.wildwest.entity.ai`. Either reuse it as-is (acknowledge in a Javadoc comment), or rename it during this work to a neutral name like `BossTeleportTarget` and import from both bosses. The spec implementation should reuse as-is; renaming is out of scope unless it becomes painful.

#### Phantom swap (not a Goal — a hook in `hurt()`)

The swap mechanic is NOT a `Goal`. Goals fire on cadence; the swap is reactive to damage. Implement via overriding `Entity303Entity.hurt(ServerLevel, DamageSource, float)` (or whichever damage-entry method the active NeoForge version exposes — match the override site used by `HerobrineEntity.isInvulnerableTo`'s sibling damage hook, e.g., `hurtServer`).

- Field on the entity: `int swapCooldown` (initial 0; decremented in `aiStep`).
- On incoming non-zero damage with `swapCooldown == 0`:
  - Roll `random.nextFloat() < 0.30f`. If false, no swap; allow vanilla damage to apply normally.
  - If true: prevent damage application this tick (the swap *replaces* the would-be hit — see "damage handling" below). Spawn an `Entity303CloneEntity` at the current position (see below). Compute swap destination 8–12 blocks behind the attacker (using attacker's `getLookAngle()`; if no attacker available, pick a random horizontal direction at distance 10). Validate clearance via the same retry loop as the teleport goal; if no valid spot, fall through and let damage apply (no swap). Move via `teleportTo(...)`; particle burst at both endpoints (16× `ParticleTypes.SMOKE`); play `SoundEvents.ENDERMAN_TELEPORT` (vol 0.6, pitch 1.2).
  - Reset `swapCooldown = 80` (4 s).
- Damage handling on successful swap: return early from `hurt`/`hurtServer` without applying damage to self. The swap *is* the defensive response. (If returning early is tricky in this NeoForge version, instead allow damage to apply but heal it back via `setHealth(getHealth() + amount)` — the visible effect is identical.)

#### Entity303CloneEntity (visual decoy spawned by phantom swap)

A minimal entity class for the 1-HP visual decoy. Lives only long enough to be hit, then dies. No AI goals, no targeting — it just stands there looking like 303 and bleeds particles.

- New class `com.tweeks.wildwest.entity.Entity303CloneEntity extends net.minecraft.world.entity.monster.Monster`. `Monster` (not a plain `Mob`) so it inherits hostile rendering layer / sword animation / etc. for visual mirroring; no `Goal` registrations.
- `registerGoals()`: empty (override to call nothing — clone does not move on its own).
- Hitbox: same as 303 (`0.6 × 1.95`).
- `MobCategory.MONSTER`. `clientTrackingRange`: 10.
- Stats: HP `1.0`, attack damage `0.0`, movement speed `0.0`, knockback resistance `1.0` (immovable). FollowRange `0.0`.
- `setPersistenceRequired()` so it doesn't despawn before the timer.
- Field: `int lifetimeTicks` (starts at 120 = 6 s). Decrements in `aiStep`; at 0 → `discard()`.
- `hurt`/`hurtServer`: any damage → `discard()` (no swap mechanic on the clone itself; one-hit-kill is the design).
- No loot table (loot table file `wildwest/src/main/resources/data/wildwest/loot_table/entities/entity_303_clone.json` exists and is **empty pools** — `{ "type": "minecraft:entity", "pools": [] }` — so vanilla doesn't fall back to "missing loot table" log spam).
- XP reward: 0 (`getBaseExperienceReward` returns 0).
- Renderer: same `Entity303Renderer` class can render either — the renderer is bound to a base interface or to both entity types via two `registerEntityRenderer` calls. Implementation detail: easiest is a separate trivial `Entity303CloneRenderer` that subclasses or duplicates `Entity303Renderer`; either is fine. The clone renders identically to the real 303 (same texture, same eyes overlay).
- Singleton flag: NOT touched by the clone. It is not the real 303 and does not claim or release the SavedData record.

> **Why a separate entity type rather than spawning a "frozen" 303?** A 1-HP `Entity303Entity` would still try to acquire the singleton flag in `finalizeSpawn`, get rejected as a duplicate (the real one is alive), and be discarded immediately. Modeling the clone as its own entity sidesteps the singleton logic cleanly.

#### AI tick budget

All goal cooldowns (`teleportCooldown`, `bowCooldown`, `swapCooldown`) and the clone's `lifetimeTicks` decrement once per tick in their respective entity's `aiStep` override. The Herobrine plan put them in each goal's `tick()`; either works. Implementation detail.

### Equipment

303 carries TWO weapons and switches between them:

- Bow: `ItemStack` of `Items.BOW` with Power V + Flame I enchantments applied via whichever enchantment API the active NeoForge version exposes (mirror `HerobrineEntity.populateDefaultEquipmentSlots`).
- Iron sword: `ItemStack` of `Items.IRON_SWORD` with Sharpness IV enchantment.

`populateDefaultEquipmentSlots(RandomSource, DifficultyInstance)`: main hand = bow (his default stance). Off-hand = iron sword (parked there so the player visually sees both weapons; the sword icon faces forward in offhand which is a clean visual tell).

**Equipment switching:** when `Entity303MeleeGoal.canUse()` returns true (target close), the goal's `start()` swaps the bow to offhand and the sword to mainhand. When `Entity303BowGoal.canUse()` becomes true again (target distance back in `[5, 20]`), it swaps back. Concretely, both goals check `getMainHandItem().is(Items.IRON_SWORD)` (for melee) vs `getMainHandItem().is(Items.BOW)` (for bow) before acting and call `setItemSlot(MAINHAND, ...)` / `setItemSlot(OFFHAND, ...)` if a swap is needed.

Drop chances:

- Mainhand drop chance: `0.10` (10%) via `setDropChance(EquipmentSlot.MAINHAND, 0.10f)`.
- Offhand drop chance: `0.10` (10%) via `setDropChance(EquipmentSlot.OFFHAND, 0.10f)`.

Whichever weapon is in the corresponding slot at death drops with that 10% chance. Because 303 switches slots based on combat distance, the dropped weapon is essentially a function of how he died (sword if killed in melee, bow if killed at range). Both drop with full enchantments intact.

### Visuals

- **Texture:** dark-cloaked Steve-skin variant with glowing red eyes. Vendored 64×64 PNG at `assets/wildwest/textures/entity/entity_303.png`. Body parts (legs, torso, arms, head, hat overlay) painted in dark colors — primary near-black `#0E0E14` for the cloak, midnight blue `#1A1A2E` for skin showing, hood implied by shading on the head. Eye pixels are painted full red `#FF0000` on the base texture (so even without the emissive layer 303 still has red eyes; the emissive layer just makes them glow at night).
- **Eyes overlay:** second 64×64 PNG at `assets/wildwest/textures/entity/entity_303_eyes.png`. Eye pixels red `#FF0000` (full opacity), rest fully transparent. Used by an emissive overlay layer so eyes glow at full brightness regardless of ambient light. **This is intentional even though the Herobrine emissive layer was dropped (commit `cea3dbd`):** Herobrine's white eyes were nearly invisible against the bright Steve face in daylight, making the emissive layer overkill. 303's red eyes against the near-black hood are the iconic creepypasta silhouette; the glow is the defining visual feature and worth the layer.
- **bbmodel source:** `wildwest/tools/entity_303.bbmodel` for future texture editing in Blockbench. Mirrors the herobrine.bbmodel workflow.
- **Model:** reuse vanilla `HumanoidModel<HumanoidRenderState>`. No new model class needed — 303 is a single humanoid like Herobrine.
- **Renderer:** new `com.tweeks.wildwest.client.Entity303Renderer extends HumanoidMobRenderer<Entity303Entity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>>`. Shadow size `0.5f`. `getTextureLocation` returns the 303 texture identifier.
- **Eyes layer:** new `com.tweeks.wildwest.client.Entity303EyesLayer extends RenderLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>>`. Renders the eyes texture using `RenderType.eyes(...)` (full-bright). Added to `Entity303Renderer` constructor via `addLayer(new Entity303EyesLayer(this))`.
- **Render state:** reuses vanilla `HumanoidRenderState` — no custom state needed.
- **Layer registration:** none required for the entity model (reuse vanilla `ModelLayers.PLAYER` via `context.bakeLayer(ModelLayers.PLAYER)`).
- **Clone rendering:** `Entity303CloneEntity` registers the same renderer (or a trivial subclass `Entity303CloneRenderer` that reuses `Entity303Renderer`'s texture + layer). The clone renders identically to the real 303 — same texture, same eyes overlay, same model.

### Boss bar

- Server-side: `ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossBarColor.PURPLE, BossBarOverlay.PROGRESS);`
- Color is `PURPLE` (vs Herobrine's `RED`) — visually distinct when both bosses are alive simultaneously.
- `setCustomName(Component.translatable("entity.wildwest.entity_303"))` is called in the constructor; `getDisplayName()` surfaces it.
- `startSeenByPlayer` / `stopSeenByPlayer` are overridden to add/remove the player from the bar.
- `aiStep` updates `bossBar.setProgress(getHealth() / getMaxHealth())` once per server tick.
- `bossBar.setName(getDisplayName())` called once in constructor; not refreshed each tick.
- Clone has NO boss bar (only the real 303 does).

### Loot

- New loot table: `wildwest/src/main/resources/data/wildwest/loot_table/entities/entity_303.json`.
- Drops on death:
  - `minecraft:diamond_block` × 1 (always).
  - `wildwest:cursed_tome` × 1 (always).
- Bow and iron sword drop via the equipment-slot drop chance (above), NOT via the loot table — same pattern as vanilla mob equipment / Herobrine's netherite sword.
- XP: override `getBaseExperienceReward()` to return `80`. Set directly on the entity, not the loot table.
- Clone loot table: empty pools (see Clone section).

### Cursed Tome item

- New item: `wildwest:cursed_tome`. Class `com.tweeks.wildwest.item.CursedTomeItem extends Item`.
- Properties: `stackSize(1)`, durability set via `Properties.durability(16)` — 16 uses total before crumbling. Set `rarity(Rarity.EPIC)` for the purple name.
- Override `use(Level, Player, InteractionHand)`:
  - Server-side only: raycast forward from `player.getEyePosition()` along `player.getLookAngle()` for up to 8 blocks via `level.clip(...)` or `ProjectileUtil.getEntityHitResult(...)` — match whatever idiom is already used in the codebase (e.g., `BulletEntity` may have a precedent).
  - Resolve hit target:
    - **Entity hit (any `LivingEntity` other than the user):** pick a random non-empty equipment slot from the target (mainhand, offhand, head, chest, legs, feet — uniform distribution over slots that contain a non-empty stack). If the slot's stack has durability (`isDamageableItem()`), damage it by 50 via `stack.hurtAndBreak(50, target, slot)` (use whichever overload the version exposes; mirror existing damage-item calls in the codebase). If the stack has no durability (e.g., a totem or unbreakable item), no-op for that use — the cursed tome still consumes a use (skill miss).
    - **No entity hit (block or air):** no-op, do NOT consume a use, do NOT trigger cooldown. Player gets immediate feedback that nothing happened.
  - Apply cooldown via `player.getCooldowns().addCooldown(this, 100)` (5 s) only on successful entity hit.
  - On successful hit: spawn `ParticleTypes.SOUL` around the target's bounding box (24 particles, 0.5 spread) for visual feedback; play `SoundEvents.ENCHANTMENT_TABLE_USE` at vol 0.6 pitch 0.7 (creepy low chime).
  - Damage the held tome by 1 durability via `stack.hurtAndBreak(1, player, hand)` — when the tome reaches 0 durability it breaks (vanilla item-break behavior; particle + sound + slot empties).
  - Return `InteractionResultHolder.consume(stack)` to play swing animation.
- Texture: new 16×16 item PNG at `assets/wildwest/textures/item/cursed_tome.png`. Art is implementation detail; broadly a dark book with a red glyph on the cover.
- Item model JSON at `assets/wildwest/models/item/cursed_tome.json` — standard `item/generated` parent (the tome is held like a book, not a sword).
- Creative tab: `WILDWEST_TAB`, after `ENTITY_303_SPAWN_EGG`.

### Damage type

No new damage type needed. 303's bow does vanilla arrow damage with vanilla Flame I behavior; sword does vanilla `Mob.doHurtTarget` damage. Cursed Tome damage is applied to *items* (via `stack.hurtAndBreak`), not entities, so no entity-damage-source is involved.

### Sounds

Reuse vanilla sounds; no new sound assets:

- Hurt: `SoundEvents.PLAYER_HURT` (player-shaped entity).
- Death: `SoundEvents.WITHER_DEATH` (matches Herobrine — mythic resonance).
- Ambient: `SoundEvents.ELDER_GUARDIAN_AMBIENT_LAND` at vol 0.4 (low, ominous — distinguishes from Herobrine's `AMBIENT_CAVE`). Called via the default ambient sound interval.
- Teleport (both goal and swap): `SoundEvents.ENDERMAN_TELEPORT` at vol 0.6, pitch 1.2 (quieter, higher-pitched than Herobrine's at 0.8 / 1.0 — distinguishes audio when both bosses are nearby).
- Bow shot: `SoundEvents.SKELETON_SHOOT` (vanilla bow sound).
- Cursed Tome use: `SoundEvents.ENCHANTMENT_TABLE_USE` (low pitch, see Cursed Tome).
- Attack hit (melee): vanilla `Mob.doHurtTarget` — no override.

### Lang strings

Add to `wildwest/src/main/resources/assets/wildwest/lang/en_us.json`:

- `entity.wildwest.entity_303`: `"Entity 303"`
- `entity.wildwest.entity_303_clone`: `"Phantom"`
- `item.wildwest.entity_303_spawn_egg`: `"Entity 303 Spawn Egg"`
- `item.wildwest.entity_303_spawn_egg.away`: `"Entity 303 is far away…"`
- `item.wildwest.entity_303_spawn_egg.wrong_dimension`: `"Entity 303 does not walk here…"`
- `item.wildwest.entity_303_spawn_egg.different_dimension`: `"Entity 303 walks elsewhere…"`
- `item.wildwest.cursed_tome`: `"Cursed Tome"`

### Out of scope

The following are explicitly **not** in this spec:

- Cross-dimensional teleport via spawn egg (refused with feedback message).
- Recovery mechanism for stuck `alive == true` flag. Documented as known edge case; `/kill @e[type=wildwest:entity_303]` resolves.
- Looting enchantment effect on drops (drops are fixed quantities per the design).
- Spawn egg dispenser behavior (creative-only via tab; dispensers use the default `SpawnEggItem` dispense behavior — singleton check fires via `finalizeSpawn`).
- Multiple simultaneous clones. Phantom swap creates ONE clone per swap; previous clones are not de-spawned but expire on their own 6s timer or first hit.
- Clone behaving as a meaningful target (it does not attack, does not retain XP, does not appear on the boss bar — strictly visual).
- Phase transitions or HP-threshold attribute changes.
- Boss-bar color or animation changes on damage milestones.
- 303 vs Herobrine interaction (they ignore each other if both alive; no special "rival" AI — out of scope for this spec, possible follow-up).
- Custom death cinematic or custom music.
- Entity 303-themed structures, signs, or world-gen.
- Refactoring `HerobrineSavedData` + `Entity303SavedData` into a generic `BossSingletonState<T>` — left for a future cleanup pass once a third singleton boss is on the table.
- Zombie-virus interaction beyond default — 303 is a `LivingEntity` and would be infectable, but per existing-design philosophy of "infectable unless explicitly immune," he stays infectable.
- Cursed Tome targeting blocks (e.g., breaking block durability). Block-hit branch is no-op by design.
- Cursed Tome enchantability (e.g., applying Mending). Out of scope; default `isEnchantable` behavior is fine.

## File-level changes

**New files:**

- `wildwest/src/main/java/com/tweeks/wildwest/entity/Entity303Entity.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/Entity303CloneEntity.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/Entity303SavedData.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/Entity303State.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/Entity303MeleeGoal.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/Entity303BowGoal.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/Entity303TeleportGoal.java`
- `wildwest/src/main/java/com/tweeks/wildwest/item/CursedTomeItem.java`
- `wildwest/src/main/java/com/tweeks/wildwest/item/Entity303SpawnEggItem.java`
- `wildwest/src/main/java/com/tweeks/wildwest/spawning/Entity303SpawnRules.java`
- `wildwest/src/main/java/com/tweeks/wildwest/client/Entity303Renderer.java`
- `wildwest/src/main/java/com/tweeks/wildwest/client/Entity303EyesLayer.java`
- `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/entity_303_overworld_spawns.json`
- `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/entity_303_end_spawns.json`
- `wildwest/src/main/resources/data/wildwest/loot_table/entities/entity_303.json`
- `wildwest/src/main/resources/data/wildwest/loot_table/entities/entity_303_clone.json`
- `wildwest/src/main/resources/assets/wildwest/textures/entity/entity_303.png`
- `wildwest/src/main/resources/assets/wildwest/textures/entity/entity_303_eyes.png`
- `wildwest/src/main/resources/assets/wildwest/textures/item/cursed_tome.png`
- `wildwest/src/main/resources/assets/wildwest/models/item/cursed_tome.json`
- `wildwest/tools/entity_303.bbmodel`

**Modified files:**

- `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java` — add `ENTITY_303` + `ENTITY_303_CLONE` `DeferredHolder`s.
- `wildwest/src/main/java/com/tweeks/wildwest/Registration.java` — add `ENTITY_303_SPAWN_EGG` + `CURSED_TOME` `DeferredItem`s; add both to `WILDWEST_TAB.displayItems`.
- `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java` — register attributes for `ENTITY_303` and `ENTITY_303_CLONE`; register spawn placement for `ENTITY_303` via `Entity303SpawnRules`.
- `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java` — register `Entity303Renderer` for both `ENTITY_303` and `ENTITY_303_CLONE` (or `Entity303CloneRenderer` if a separate subclass is used).
- `wildwest/src/main/resources/assets/wildwest/lang/en_us.json` — add the 7 lang strings listed above.

**Reused files (no modification needed):**

- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/HerobrineTeleportTarget.java` — reused as-is by `Entity303TeleportGoal`.

## Testing strategy

### Unit tests (JUnit 5 / Jupiter)

Three new test classes, mirroring the Herobrine plan's split:

1. **`Entity303StateTest`** — five tests covering default state, setAlive, clear, copyOf equality, copyOf independence. Same shape as `HerobrineStateTest`.
2. **`Entity303TeleportGoalTest`** — NOT a unit test (would need Minecraft boot). Skip. The teleport math is already covered by the reused `HerobrineTeleportTargetTest`.
3. **`CursedTomeTargetSelectionTest`** — pure helper tests for "pick a random non-empty equipment slot." Factor the slot-selection out of `CursedTomeItem.use` into a `static pickDamagableSlot(EnumSet<EquipmentSlot> populatedSlots, RandomSource rng)` helper. Tests:
   - All slots populated → returns one of the six with uniform probability (use a fixed-seed RNG, count over 600 calls, check distribution roughly uniform within ±20%).
   - Only mainhand populated → always returns mainhand.
   - No slots populated → returns null (caller no-ops).
   - Single slot populated → always returns that slot.

### Manual checklist (creative client, end-to-end)

Same shape as the Herobrine plan's Task 25 manual checklist. Specifics for 303:

- **Spawn egg + singleton** — verify spawn, then second egg use teleports existing 303 instead of spawning. Boss bar appears (purple, "Entity 303").
- **Dimension gate** — egg refused in Nether (`"Entity 303 does not walk here…"`).
- **Overworld + End spawn parity** — natural spawn works in both dims (test by editing biome modifier weights temporarily); singleton prevents simultaneous spawns across dims (kill 303 in overworld, verify a fresh 303 can spawn in End).
- **Cross-dim refusal** — with 303 alive in overworld, go to End and try the egg; expect `"Entity 303 walks elsewhere…"` message.
- **Bow + melee switching** — observe 303 holds bow at range, swaps to iron sword when player gets within 3 blocks, swaps back at distance.
- **Phantom swap** — hit 303 repeatedly. ~30% of hits should spawn a decoy and teleport 303 8–12 blocks behind you. Decoys die in one hit and disappear after 6s. Only ONE swap per 4s (spam should not produce multiple swaps).
- **Teleport** — observe ~3s teleport cadence with smoke particles (not portal — distinguishes from Herobrine).
- **Death + drops** — diamond block + cursed tome always; bow or iron sword (whichever was mainhand on death) with ~10% chance; 80 XP.
- **Cursed Tome** — right-click at a mob → random equipped item takes 50 durability damage. Right-click at air → no-op, no cooldown, no use consumed. After 16 uses the tome breaks.
- **Concurrent with Herobrine** — spawn both. Verify both boss bars (red Herobrine + purple 303) visible. Verify singletons don't interfere (kill 303, Herobrine unaffected; kill Herobrine, 303 unaffected).
- **Save / load** — with 303 alive, save and reload; boss reloads with same HP, singleton flag intact, boss bar reattaches.

## Risks / open questions

1. **NeoForge enchantment API signature drift** — same risk as Herobrine. `ItemStack.enchant(Holder<Enchantment>, int)` may or may not be the active overload. Mitigation: mirror whatever the Herobrine implementation actually used (now committed and working).
2. **`EntitySpawnReason` vs `MobSpawnType`** — same renamed-type risk. Mirror `HerobrineEntity.finalizeSpawn`'s signature exactly.
3. **`SoundEvents.X` Holder vs direct** — same `.value()` risk. Mirror existing usage in the Herobrine code path.
4. **Damage hook signature** — `hurt(DamageSource, float)` vs `hurtServer(ServerLevel, DamageSource, float)` varies. Mirror `HerobrineEntity`'s damage-side override.
5. **Arrow construction** — `new Arrow(Level, LivingEntity, ItemStack, ItemStack)` is the typical 26.x constructor (third arg = bow stack for enchantment context, fourth = weapon stack — often the same). Verify against the Herobrine code path or vanilla `Skeleton.performRangedAttack` at implementation time.
6. **`stack.hurtAndBreak` signature** — `(int amount, LivingEntity entity, EquipmentSlot slot)` is the modern shape but versions vary. Verify against any existing item-damage call site in the wildwest module before committing.
7. **End spawn surface** — The End is dominated by void and obsidian pillars; the central island and outer islands are the only natural spawn surfaces. Weight 2 should give reasonable spawn rate; if testing reveals 303 never spawns naturally in End, consider raising to 5 or 10. Out of scope for the spec; tune during manual testing.
8. **Phantom swap during projectile damage** — a swap triggered by an arrow hit means the swap fires AFTER the arrow has already entered the entity's tracking. In practice this is fine (the swap teleports 303 elsewhere, the arrow continues on its path or has already despawned). No special handling needed.

## Self-review

This spec covers:

| Concern | Section |
|---|---|
| Entity stats, hitbox, immunity | Entity |
| Singleton storage + lifecycle | Singleton mechanic |
| Natural spawn (two dims) | Spawning |
| Spawn egg (singleton-aware, cross-dim aware) | Spawning |
| Combat AI (melee, bow, teleport) | Combat |
| Phantom swap mechanic | Combat |
| Clone entity (visual decoy) | Combat |
| Equipment + switching | Equipment |
| Visuals (texture, renderer, eyes layer, bbmodel) | Visuals |
| Boss bar | Boss bar |
| Drops + XP | Loot |
| Cursed Tome item | Cursed Tome item |
| Damage type | (none needed — explained) |
| Sounds | Sounds |
| Lang strings | Lang strings |
| Out of scope | Out of scope |
| All new + modified files | File-level changes |
| Test plan (unit + manual) | Testing strategy |
| Known risks | Risks / open questions |

Mechanically distinct from Herobrine on every axis where it matters: kit (bow+sword vs sword+meteor+lightning), stats (evasive duelist vs apex bruiser), spawn (two dims vs one), defensive mechanic (phantom swap vs none), boss bar color (purple vs red), teleport particles (smoke vs portal), ambient sound (elder guardian vs cave), drop (Cursed Tome vs Meteor Staff). Singleton mechanic is parallel — both bosses can be alive simultaneously, each enforcing its own uniqueness.

No placeholders. No `[TBD]`. Math values (HP 160, speed 0.45, cooldowns 50/60/80 ticks, drop chance 0.10, swap probability 0.30) are explicit and consistent across sections.
