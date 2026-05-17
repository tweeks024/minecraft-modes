# Wild-West Mod — Null Boss — Design

**Date:** 2026-05-17
**Status:** Draft (pending user review of this doc)
**Series:** Stand-alone feature on top of merged Wild-West phases 1–3 + zombie-virus + Steve-Stacker + Herobrine + The Agent (Entity 303) + Pirates. No phase number.

## Goal

Add a third apex boss to the `:wildwest` mod called **Null**: a singleton, world-wide legendary "deletion entity" — peer of Herobrine and The Agent — modelled on the classic Minecraft creepypasta of the same name. Pure-black humanoid silhouette with glowing white eyes. He fights entirely through environmental hazard (no melee), drifts silently above terrain ignoring walls, and telegraphed "void rifts" at the player's feet do all his damage.

Null is mechanically distinct from the existing apex tier on every axis:

| Axis              | Herobrine                 | The Agent                  | Null                                 |
|-------------------|---------------------------|----------------------------|--------------------------------------|
| Role              | Zone-control bruiser      | Bow/sword keep-away duelist | Environmental-hazard god             |
| Engagement        | Closes & melees           | Mid-range, repositions     | Drifts inevitably toward player      |
| Movement          | Walks (0.35) + teleport   | Walks (0.45) + teleport    | Floats (0.20), ignores terrain       |
| Defensive         | None (pure HP)            | Phantom swap (decoy)       | None — wears player down with rifts  |
| Boss bar          | RED, PROGRESS             | PURPLE, PROGRESS           | WHITE, NOTCHED_6                     |
| Teleport particle | PORTAL                    | SMOKE                      | ENCHANT                              |
| Ambient sound     | AMBIENT_CAVE              | ELDER_GUARDIAN_AMBIENT_LAND| AMBIENT_UNDERWATER_LOOP              |
| Spawn dims        | Overworld (night)         | Overworld (night) + End    | Overworld (night) + Nether + End     |
| Signature drop    | Meteor Staff (active)     | Cursed Tome (active)       | Void Mark (passive death-save)       |
| HP                | 200                       | 160                        | 240                                  |

## Platform

Java 25 / NeoForge (`neo_version` from `gradle.properties`). Matches the existing `:wildwest` module. Avoid hard-coded MC version assumptions; mirror APIs already used by `HerobrineEntity` / `AgentEntity` (`Identifier.fromNamespaceAndPath`, `HumanoidMobRenderer`, `DeferredRegister`, `SavedData` + `SavedDataType` + `Codec`, biome-modifier JSON, etc.).

## In scope

### Singleton refactor (prerequisite — extract shared base)

Per the explicit out-of-scope note in the Entity 303 spec (`2026-05-14-entity303-design.md` line 302), the third singleton boss is the trigger to extract the shared abstraction. Read of `HerobrineState.java` and `AgentState.java` confirms they are byte-for-byte identical except class name. SavedData wrappers differ only in `FILE_ID` constant, class name, and which State POJO they hold.

**New shared classes:**

- `com.tweeks.wildwest.entity.BossSingletonState` (pure POJO, replaces `HerobrineState` and `AgentState`). Identical shape to the existing State classes: `boolean alive`, `UUID currentId`, `String dimensionId`, `setAlive(UUID, String)`, `clear()`, `copyOf(BossSingletonState)`.
- `com.tweeks.wildwest.entity.BossSingletonSavedData extends SavedData` — base class holding a single `BossSingletonState`. Provides shared instance methods (`isAlive`, `getCurrentId`, `getDimension`, `setAlive(UUID, ResourceKey<Level>)`, `clear`) and the shared DFU codec construction logic. Does NOT declare a static `TYPE` (each subclass declares its own with its unique `Identifier`).
- Each boss SavedData subclass becomes a thin wrapper that declares the unique `SavedDataType<Self>` with its own `FILE_ID` and exposes a static `get(MinecraftServer server)` accessor. ~25 lines each, no duplicated logic.

**Migration:**

1. Create `BossSingletonState` (verbatim copy of existing State POJOs, generic name).
2. Create `BossSingletonSavedData` with the shared codec construction (the codec factory takes `Function<Boolean, Function<Optional<UUID>, Function<Optional<String>, Self>>>` or equivalent — see implementation note below).
3. Rewrite `HerobrineSavedData` to extend `BossSingletonSavedData`. `FILE_ID = "wildwest_herobrine"`, static `TYPE`, static `get(server)`. Remove `HerobrineState.java`.
4. Rewrite `AgentSavedData` to extend `BossSingletonSavedData`. `FILE_ID = "wildwest_agent"`, static `TYPE`, static `get(server)`. Remove `AgentState.java`.
5. Update callers in `HerobrineEntity`, `AgentEntity`, `HerobrineEntityTest`, `AgentEntityTest`, etc. to import `BossSingletonState` and call shared accessors.
6. Update `HerobrineStateTest` → `BossSingletonStateTest`; delete `AgentStateTest` (covered by `BossSingletonStateTest`).
7. Verify NBT compatibility: existing `wildwest_herobrine.dat` and `wildwest_agent.dat` files on disk must continue to load. The codec field names (`Alive`, `CurrentId`, `Dimension`) and `SavedDataType` identifiers stay identical — only the in-memory class hierarchy changes. No data migration required.

> **Implementation note on the shared codec:** the existing codec uses `RecordCodecBuilder.create(...).apply(instance, HerobrineSavedData::fromCodec)`. To share construction across subclasses, factor a protected static helper on `BossSingletonSavedData` that takes a no-arg `Supplier<Self>` and applies the three fields. Each subclass calls `BossSingletonSavedData.<HerobrineSavedData>buildCodec(HerobrineSavedData::new)` to get its codec instance. Alternative: keep three explicit codec definitions and only share the state POJO + accessor methods. Pick whichever is cleanest at implementation time; the refactor goal is "no two identical State POJOs," not "literally one codec." If the codec-sharing turns ugly, leave codecs per-subclass.

> **Why now, why not later again?** The Entity 303 spec deferred this with: "Refactoring `HerobrineSavedData` + `Entity303SavedData` into a generic `BossSingletonState<T>` — left for a future cleanup pass once a third singleton boss is on the table." The third boss is now on the table. Putting it off again means writing a third identical POJO and three near-identical SavedData wrappers; future-us will thank current-us for paying the tax once.

### Entity

- New entity id: `wildwest:null`. (Display name "Null"; java identifier `NullEntity` — note the slight Java reserved-word friction; `Null` is NOT reserved, only `null` lowercase is, so `class NullEntity` is fine.)
- New class `com.tweeks.wildwest.entity.NullEntity extends net.minecraft.world.entity.monster.Monster` (mirrors `HerobrineEntity` / `AgentEntity` pattern; not a `WildWestMob` subclass — Null is its own apex thing).
- `MobCategory.MONSTER`.
- Hitbox: `0.6 × 1.95` (vanilla humanoid).
- `clientTrackingRange`: 10. `updateInterval`: default.
- Knockback resistance: `1.0` (immovable — drifter persona, no physics knockback).
- Follow range: `64.0`.
- Total HP: `240.0` (highest of the three apex bosses — fits "near-unkillable god" feel; no defensive mechanic, so HP IS the survivability).
- Attack damage attribute: `0.0` (no melee — all damage comes from rifts).
- Movement speed attribute: `0.20` (slowest of the three — drifter pace).
- Fire damage handling: NOT immune (consistent with Herobrine and Agent).
- Fall damage: immune. Override `causeFallDamage(...)` to return false (or set `fallDistance = 0` each tick) — he never touches the ground.
- `setPersistenceRequired()` in constructor.
- **Flight:** `setNoGravity(true)` in constructor. He floats. No vanilla flight-AI is needed because he uses a custom drift goal (see Combat) that moves him by setting `setDeltaMovement(...)` directly.

### Singleton mechanic

Anchored to the post-refactor `BossSingletonSavedData` infrastructure.

- New class `com.tweeks.wildwest.entity.NullSavedData extends BossSingletonSavedData`.
  - `FILE_ID = "wildwest_null"`.
  - Static `SavedDataType<NullSavedData> TYPE`.
  - Static `get(MinecraftServer server)` accessor.
- Stored at `server.overworld().getDataStorage()` (anchored to overworld for cross-dimension consistency, same as Herobrine and Agent).
- Lifecycle (mirrors Herobrine / Agent):
  1. **On spawn** (`finalizeSpawn`, server-side): if `alive == true` and the existing entity is not this one, `discard()`. Otherwise, claim the singleton by setting `alive = true`, `currentId = this.getUUID()`, `dimension = level().dimension()`.
  2. **On death:** override `die(DamageSource)` to clear `alive = false` then `super.die(...)`.
  3. **On removal** (`remove(RemovalReason)`): if `KILLED` or `DISCARDED`, clear singleton; if `UNLOADED_*`, leave the flag alone. Always call `bossBar.removeAllPlayers()` before delegating to `super.remove(reason)`.
  4. **Recovery for stuck flag:** documented as known edge case; `/kill @e[type=wildwest:null]` resolves.

### Spawning

Three biome modifiers (one per dimension), all referencing the same entity type and the same `NullSpawnRules::checkSpawnRules` predicate. Singleton-gating means only ONE Null is alive across all three dims at any time.

- `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/null_overworld_spawns.json`
  - Biome tag: `#minecraft:is_overworld`.
  - Spawner: `{ "type": "wildwest:null", "weight": 1, "minCount": 1, "maxCount": 1 }`.
- `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/null_nether_spawns.json`
  - Biome tag: `#minecraft:is_nether`.
  - Spawner: `{ "type": "wildwest:null", "weight": 2, "minCount": 1, "maxCount": 1 }` (weight 2 — Nether has dense hostile spawns; need to compete).
- `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/null_end_spawns.json`
  - Biome tag: `#minecraft:is_end`.
  - Spawner: `{ "type": "wildwest:null", "weight": 2, "minCount": 1, "maxCount": 1 }`.

**Spawn placement** (registered once in `WildWestMod.registerSpawnPlacementsEvent`):

- `SpawnPlacementTypes.ON_GROUND` (he floats but spawn-placement-wise we want him to land in a valid block — he lifts off after first tick).
- `Heightmap.Types.MOTION_BLOCKING_NO_LEAVES`.
- Predicate: `NullSpawnRules::checkSpawnRules`:
  - `Monster::checkMonsterSpawnRules` (light ≤ 7, valid block, etc.) for ALL three dims.
  - **Overworld branch:** require `level.canSeeSky(pos)` (night sky flavor — matches Herobrine and Agent).
  - **Nether branch:** no sky requirement (Nether has no sky); light gate is enough. Avoid bastion + fortress structures via standard structure-check (`level.getLevel().structureManager().getAllStructuresAt(pos).isEmpty()` or whichever idiom is already in use — defer to the implementation pass if no precedent exists, drop the structure check).
  - **End branch:** no sky requirement (same logic as the Agent's End branch).
  - Branch detection: switch on `level.getLevel().dimension()` against `Level.OVERWORLD`, `Level.NETHER`, `Level.END`. If none match, reject.
  - `NullSavedData.get(server).isAlive() == false` (singleton gate).

**Spawn egg** with singleton-aware behavior. New item `wildwest:null_spawn_egg`.

- Class `NullSpawnEggItem extends SpawnEggItem`.
- Egg colors: primary `0x000000` (pure black), secondary `0xFFFFFF` (white — the eyes).
- Override `useOn(UseOnContext)`:
  - Server-side only.
  - **Dimension gate:** allow in overworld, Nether, or End. If anywhere else (modded dim), refuse with `Component.translatable("item.wildwest.null_spawn_egg.wrong_dimension")` ("Null does not walk here…"). Egg not consumed. Return `InteractionResult.FAIL`.
  - Read `NullSavedData.get(server)`.
  - **If `alive == false`:** delegate to `super.useOn(context)` (vanilla spawn flow). `finalizeSpawn` sets the singleton flag.
  - **If `alive == true`:** resolve existing entity via `server.getLevel(savedData.getDimension()).getEntity(savedData.getCurrentId())`.
    - **Entity loaded AND in same dimension as player:** teleport Null to the clicked location. Spawn `ParticleTypes.ENCHANT` burst at both source and destination. Play `SoundEvents.ENDERMAN_TELEPORT`. Egg not consumed. Return `InteractionResult.SUCCESS`.
    - **Entity unloaded:** refuse with `Component.translatable("item.wildwest.null_spawn_egg.away")` ("Null is far away…"). Egg not consumed. Return `InteractionResult.FAIL`.
    - **Entity in a different dimension than the player:** refuse with `Component.translatable("item.wildwest.null_spawn_egg.different_dimension")` ("Null walks elsewhere…"). Egg not consumed. Return `InteractionResult.FAIL`. Cross-dim teleport via egg is NOT allowed (consistent with Agent).
- Creative tab: `WILDWEST_TAB`, after `AGENT_SPAWN_EGG` (registry path `"the_agent_spawn_egg"` — verified against `Registration.java`).

### Combat / AI

`registerGoals()` registers, in priority order:

1. `NullDriftGoal` (custom) — priority 1.
2. `NullRiftGoal` (custom) — priority 2.
3. `LookAtPlayerGoal(this, Player.class, 32.0f)` — priority 8.
4. `RandomLookAroundGoal(this)` — priority 8.

Note the absence of `FloatGoal` — Null doesn't path on ground/in-water, he floats independently. Also no melee goal, no `HurtByTargetGoal` (he doesn't need a re-target — anyone who hits him is already a `NearestAttackableTargetGoal` candidate). No `MeleeAttackGoal` (no attack damage attribute).

Target selection:

1. `NearestAttackableTargetGoal<>(this, Player.class, true)` — priority 1.

#### NullDriftGoal

Persistent goal — runs continuously while a target exists. Moves Null toward target horizontally; holds a fixed Y-offset above the terrain ceiling so he floats above obstacles instead of going around them.

- `canUse()`: target alive and within follow range. `canContinueToUse()`: same.
- `tick()`:
  - Read target horizontal position.
  - Compute desired direction = unit vector from self to target on the XZ plane.
  - Compute desired Y: `level.getHeight(MOTION_BLOCKING_NO_LEAVES, selfX, selfZ) + 3.0` (always 3 blocks above the local terrain ceiling, so he floats above buildings, trees, hills).
  - Apply velocity: `setDeltaMovement(dir.x * 0.20, (desiredY - selfY) * 0.10, dir.z * 0.20)`. The Y term is a soft spring toward the target altitude; clamp the Y velocity component to `[-0.4, 0.4]` to avoid yo-yoing over tall structures.
  - Face the target (`getLookControl().setLookAt(target, 30f, 30f)`).
- **Stopping condition:** Null never stops drifting — even when he's "on top of" the target he drifts. The Y-offset above-terrain rule means he sits at altitude relative to the local heightmap, which naturally puts him slightly above the player most of the time, looking down. This is intentional flavor (he hovers over you, watching).
- No path-finding (`Navigation` is unused for this entity — `setNoGravity(true)` plus custom velocity is enough).

#### NullRiftGoal

The primary attack — spawns telegraphed void rifts at the target's feet.

- Cooldown: 100 ticks (5 s). Tracked as `int riftCooldown` on the entity (decremented in `aiStep`, consistent with how Herobrine and Agent track theirs).
- `canUse()`: target alive, target within 32 blocks (horizontal), `riftCooldown == 0`, server-side line-of-sight (`canSee(target)`) — Null cannot phase rifts through walls if he can't see the player; this gives walls some defensive value.
- `start()`:
  - Resolve rift center: snap target's current XZ to ground via `level.getHeight(MOTION_BLOCKING_NO_LEAVES, targetX, targetZ)`. The rift is anchored to where the player IS at this moment, not where they will be.
  - Spawn a new `NullRiftEntity` at that position (see below). The rift entity owns its own 2s telegraph + 4s active timer + damage logic, so the goal is fire-and-forget.
  - Play `SoundEvents.PORTAL_TRIGGER` at vol 0.6, pitch 0.4 (low, ominous) at the rift origin.
  - Reset cooldown to 100.
- `canContinueToUse()`: false (one-shot, fire-and-forget).

#### NullRiftEntity (the hazard zone)

The rift is its own entity (not a particle effect on the level) so it ticks itself, manages its own lifecycle, and renders consistently on all clients.

- New class `com.tweeks.wildwest.entity.NullRiftEntity extends net.minecraft.world.entity.Entity` (NOT a `Mob` / `Monster` — it's a passive hazard, not a creature; no AI goals).
- `MobCategory` is irrelevant (`Entity` not `Mob`).
- Hitbox: `3.0 × 3.0` (the hazard volume — entities are queried via `getBoundingBox()` without further inflation, so the box IS the hazard area; visual is a flat ground rift, but the hazard extends 3 blocks up to catch jumpers).
- `clientTrackingRange`: 8.
- Spawning: created server-side by `NullRiftGoal` via `level.addFreshEntity(new NullRiftEntity(level, x, y, z))`.
- All logic runs server-side only (early-return on `level.isClientSide()`). Particles are dispatched to clients via `((ServerLevel) level).sendParticles(...)` — no need for per-side tick or `SynchedEntityData` for `ageTicks`. Client renderer is a no-op (see Visuals).
- Two-phase lifecycle (tracked via `int ageTicks`, incremented each server `tick()`):
  - **Telegraph phase** (`ageTicks ∈ [0, 40)`, i.e. 2 s):
    - Each tick, server sends `ParticleTypes.PORTAL` in a growing ring around the center on the XZ plane. Ring radius = `1.5 * (ageTicks / 40f)`. ~12 particles per tick, evenly distributed around the ring.
    - No damage. No pull.
  - **Active phase** (`ageTicks ∈ [40, 120)`, i.e. 4 s):
    - Each server tick:
      - Server sends dense `ParticleTypes.PORTAL` + `ParticleTypes.SMOKE` inside the 3×3×3 hazard volume (the rift is "open"). ~20 portal + 8 smoke per tick.
      - Query entities within hitbox: `level.getEntitiesOfClass(LivingEntity.class, getBoundingBox())` (no inflation — the 3×3×3 hitbox IS the hazard volume).
      - For each `LivingEntity` (including the player, excluding Null himself — `entity.getType() != ModEntities.NULL.get()`):
        - **Damage cadence:** apply `entity.hurt(level.damageSources().magic(), 2.0f)` every 10 ticks (i.e. when `ageTicks % 10 == 0` within the active phase). Net: 4 dmg / second sustained while standing in the rift. Total max dwell damage = 16 (4s × 4 dps). Integer per-hit value avoids the fractional-damage-floored-by-vanilla footgun.
        - Apply pull velocity every tick (smooth, not gated by the damage cadence): vector from entity to rift center on XZ, magnitude `0.15`. Add to entity's `deltaMovement`: `entity.setDeltaMovement(entity.getDeltaMovement().add(pullX, 0, pullZ))`. NOT applied to Null himself.
      - Query item entities (`level.getEntitiesOfClass(ItemEntity.class, getBoundingBox())`). For each item: `item.discard()` — `ItemEntity` instances (loot drops, Q-key throws) are erased on contact. `ExperienceOrb` is a separate class and is NOT affected. **Note:** dropped player inventory items are also erased — this is intentional flavor (Null deletes) but unkind if the player dies on the rift. Document in the manual checklist for testing; we'll see if it feels too harsh.
    - At `ageTicks == 120`: `discard()` (cleanup).
- Damage to the rift: `isInvulnerableTo(DamageSource)` returns true for everything. Rifts cannot be destroyed; they tick out on the timer.
- No collision with other entities (`canBeCollidedWith` returns false). Players walk through it (and take damage).
- No collision with blocks (it's a ghost entity). `noPhysics = true`.
- Sync: gameplay logic is server-only (see above); clients only need to know the rift exists (handled by the standard `addFreshEntity` spawn packet) and the rest is server-pushed particles. `ageTicks` lives only on the server.
- No loot table. Override `getLootTable()` to return null/empty (or provide an empty pools file as a defensive measure — same as the Agent clone's empty loot table).
- No XP reward (`getBaseExperienceReward` returns 0). Actually `Entity` base class doesn't have this method — only `LivingEntity` does. N/A.

> **Why an entity, not a `BlockEntity` or particle batch?** A `BlockEntity` would require placing a block at the rift origin, which is destructive to the world; we'd need to restore the block on cleanup, and that interacts badly with player block edits. Particle batches don't tick on their own and can't easily apply damage. A custom `Entity` is the cleanest fit — it owns its lifecycle and can spawn its own particles, apply damage, and clean itself up.

### Equipment

None. Null is unarmed. `populateDefaultEquipmentSlots` does nothing (don't override) — the default empty-hand result is what we want.

No drop chances need configuring (no equipment).

### Visuals

- **Texture:** pure-black 64×64 PNG at `assets/wildwest/textures/entity/null.png`. Every pixel of every body part (legs, torso, arms, head, hat overlay) painted full black `#000000`. The two eye pixel positions (standard Steve face: roughly `(9,12)` and `(13,12)` on the head face, but verify against the vanilla player texture grid in implementation) painted full white `#FFFFFF`. The look matches the user-provided reference image: solid silhouette, glowing eyes.
- **Eyes overlay:** second 64×64 PNG at `assets/wildwest/textures/entity/null_eyes.png`. Eye pixels white `#FFFFFF` (full opacity), rest fully transparent. Used by an emissive overlay layer (same pattern as `Entity303EyesLayer`) so eyes glow at full brightness regardless of ambient light. Justified vs Herobrine's dropped emissive layer (see Entity 303 spec): white eyes on a pitch-black silhouette in a dark cave or Nether are the iconic creepypasta image; the emissive layer is the defining visual feature.
- **bbmodel source:** `wildwest/tools/null.bbmodel` for future texture editing.
- **Model:** reuse vanilla `HumanoidModel<HumanoidRenderState>`. No custom model — Null is a single humanoid.
- **Renderer:** new `com.tweeks.wildwest.client.NullRenderer extends HumanoidMobRenderer<NullEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>>`. Shadow size `0.5f`. `getTextureLocation` returns the Null texture identifier.
- **Eyes layer:** new `com.tweeks.wildwest.client.NullEyesLayer extends RenderLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>>`. Renders the eyes texture using `RenderType.eyes(...)`. Added in `NullRenderer` constructor.
- **Render state:** reuse vanilla `HumanoidRenderState`.
- **Layer registration:** none required — reuse `ModelLayers.PLAYER` via `context.bakeLayer(ModelLayers.PLAYER)`.
- **Rift renderer:** new `com.tweeks.wildwest.client.NullRiftRenderer extends EntityRenderer<NullRiftEntity>`. The `render(...)` method does NOTHING — the rift has no model. All visuals come from server-spawned particles. The renderer exists only to satisfy the entity-type renderer-registration requirement; without it, NeoForge logs a missing-renderer warning. (Alternative: register `EntityRendererProvider` that returns a no-op `EntityRenderer` subclass. Same outcome.)

### Boss bar

- Server-side: `ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossBarColor.WHITE, BossBarOverlay.NOTCHED_6);`
- Color is `WHITE` (vs Herobrine RED, Agent PURPLE) — visually distinct when multiple bosses are alive.
- Overlay is `NOTCHED_6` (only boss with notches) — Null's bar visually breaks into 6 segments. Pure aesthetic differentiation; no mechanical effect tied to the notches.
- `setCustomName(Component.translatable("entity.wildwest.null"))` in constructor; `getDisplayName()` surfaces it.
- `startSeenByPlayer` / `stopSeenByPlayer` overridden to add/remove the player from the bar.
- `aiStep` updates `bossBar.setProgress(getHealth() / getMaxHealth())` once per server tick.
- `bossBar.setName(getDisplayName())` once in constructor.
- Rift entities have NO boss bar (only Null himself does).

### Loot

- New loot table: `wildwest/src/main/resources/data/wildwest/loot_table/entities/null.json`.
- Drops on death:
  - `minecraft:netherite_block` × 1 (always — peer-tier reward; Herobrine drops a netherite ingot-equivalent, Agent drops a diamond block, Null drops netherite block as the deepest-dim apex).
  - `wildwest:void_mark` × 1 (always).
- No equipment drops (no equipment).
- XP: override `getBaseExperienceReward()` to return `100` (vs Herobrine's 100 and Agent's 80 — Null is the hardest to kill so equal-or-better XP).
- Rift entities have no loot table (handled by the rift entity itself).

### Void Mark item

- New item: `wildwest:void_mark`. Class `com.tweeks.wildwest.item.VoidMarkItem extends Item`.
- Properties: `stackSize(16)` (stackable — players can stockpile them; each consumed individually on lethal damage), `rarity(Rarity.EPIC)`.
- No active use. Override `use(Level, Player, InteractionHand)` to return `InteractionResultHolder.pass(stack)` (right-click does nothing — purely passive trigger).
- **Trigger:** register a `LivingDamageEvent` handler on the NeoForge event bus (mod-bus or game-bus per NeoForge 26.x convention — mirror whatever event-handler style is already used in the wildwest module; see `WildWestMod.java` for precedent).
  - Handler signature: `@SubscribeEvent public static void onLivingDamage(LivingDamageEvent.Pre event)` (or `.Post`, depending on what the version exposes — the goal is "before death is finalised").
  - Filter: `event.getEntity() instanceof Player player` (server-side check via `!player.level().isClientSide()`).
  - **Lethal-check:** `if (player.getHealth() - event.getNewDamage() > 0) return;` (the incoming damage isn't lethal — let it apply normally).
  - **Inventory scan:** iterate `player.getInventory().items` (top-level main inventory + hotbar, NOT armor, NOT offhand, NOT shulker-box contents). Find the first non-empty `ItemStack` matching `stack.is(Registration.VOID_MARK.get())`. If none found, return (player dies normally).
  - **Activation:** `event.setNewDamage(0.0f)` (cancel the damage). `stack.shrink(1)` (consume one Void Mark). `player.setHealth(1.0f)` (set HP to 1).
  - **Teleport:** read `player.getRespawnPosition()` if available (NeoForge / vanilla API for spawn point — falls back to world spawn). Teleport via `player.teleportTo(ServerLevel, x, y, z, ...)` if cross-dim teleport is needed (player in Nether → respawn is in Overworld). Use the same cross-dim-teleport API the vanilla respawn flow uses.
  - **Effects:** spawn `ParticleTypes.PORTAL` burst (24 particles) at both old and new positions. Play `SoundEvents.TOTEM_USE` at the old position (vanilla totem-pop SFX — reusing the player's existing audio expectation for "you just got death-saved"). Play `SoundEvents.PORTAL_TRAVEL` at the new position.
  - **Feedback:** `player.displayClientMessage(Component.translatable("item.wildwest.void_mark.triggered"), true)` — actionbar message "The void claimed you back…".
- Texture: 16×16 item PNG at `assets/wildwest/textures/item/void_mark.png`. Art is implementation detail; broadly a black-bordered tag with a white sigil.
- Item model JSON at `assets/wildwest/models/item/void_mark.json` — standard `item/generated` parent.
- Creative tab: `WILDWEST_TAB`, after `NULL_SPAWN_EGG`.

> **Why an event handler, not an item-tick?** Items in inventory don't get `inventoryTick` called for death-detection use; even if they did, the death-check would need to read the player's HP every tick, which is wasteful. `LivingDamageEvent` fires exactly once per damage event with the resolved damage value in hand — perfect fit.

> **Edge case: player dies in the same tick as picking up a Void Mark.** Pickup happens via `EntityItemPickupEvent` which fires before `LivingDamageEvent`. The freshly-picked-up mark is in inventory by the time the damage event runs and would trigger. This is fine flavor — counts as "the mark protected you the instant it touched your hand."

> **Edge case: player dies in a void (Y < world floor).** Void damage in vanilla is `DamageTypes.OUT_OF_WORLD`, which bypasses some damage hooks. Verify `LivingDamageEvent` still fires for void damage — if not, the Void Mark cannot save you from the void. Documented as a known limitation; do not work around it.

> **Edge case: player dies in creative mode.** Skip activation if `player.getAbilities().instabuild` (creative-mode kill or `/kill @s` shouldn't consume marks).

### Damage type

No new damage type. Rift damage uses `level.damageSources().magic()` — vanilla magic damage source (purple text in chat). Mark trigger uses no damage source (purely defensive).

### Sounds

Reuse vanilla sounds; no new sound assets:

- **Hurt:** `SoundEvents.PLAYER_HURT` (humanoid).
- **Death:** `SoundEvents.WITHER_DEATH` (matches Herobrine and Agent — mythic resonance).
- **Ambient:** `SoundEvents.AMBIENT_UNDERWATER_LOOP` at vol 0.3 (deep, dread-laden — distinct from Herobrine's cave and Agent's elder-guardian). Default ambient sound interval.
- **Teleport (spawn-egg teleport):** `SoundEvents.ENDERMAN_TELEPORT` at vol 0.6, pitch 0.6 (lower than Agent's, distinguishes audio).
- **Rift open (when Null spawns one):** `SoundEvents.PORTAL_TRIGGER` at vol 0.6, pitch 0.4.
- **Void Mark trigger:** `SoundEvents.TOTEM_USE` at old position, `SoundEvents.PORTAL_TRAVEL` at new position.

### Lang strings

Add to `wildwest/src/main/resources/assets/wildwest/lang/en_us.json`:

- `entity.wildwest.null`: `"Null"`
- `entity.wildwest.null_rift`: `"Void Rift"`
- `item.wildwest.null_spawn_egg`: `"Null Spawn Egg"`
- `item.wildwest.null_spawn_egg.away`: `"Null is far away…"`
- `item.wildwest.null_spawn_egg.wrong_dimension`: `"Null does not walk here…"`
- `item.wildwest.null_spawn_egg.different_dimension`: `"Null walks elsewhere…"`
- `item.wildwest.void_mark`: `"Void Mark"`
- `item.wildwest.void_mark.triggered`: `"The void claimed you back…"`

### Out of scope

- Cross-dimensional spawn-egg teleport (refused with feedback).
- Void Mark in offhand or armor slots — top-level inventory only.
- Void Mark protecting against creative-mode `/kill`.
- Void Mark protecting against void damage IF `LivingDamageEvent` doesn't fire for `OUT_OF_WORLD` (documented limitation).
- Rifts targeting non-player `LivingEntity` (rifts spawn at player target; if Null retargets a non-player via `HurtByTargetGoal` — wait, no, that goal isn't registered. Only `NearestAttackableTargetGoal<Player>`. So this is enforced by the goal list; non-player damage to Null doesn't change his target.).
- Recovery mechanism for stuck singleton flag — same documented edge case as Herobrine / Agent.
- Looting enchantment effect on drops (drops are fixed).
- Phase transitions or HP-threshold attribute changes.
- Boss-bar color or animation changes on damage milestones.
- Null vs Herobrine vs Agent interaction (they ignore each other; no "rivals" AI).
- Custom death cinematic, music, or screen-shake effects.
- Null-themed structures, signs, blocks, or world-gen.
- Zombie-virus interaction beyond default (Null is `LivingEntity`, infectable per existing philosophy).
- Decay/permanent block damage from rifts — rifts are purely temporary hazards; they do NOT modify blocks.
- Rift dispelling tools (e.g., player-side counter to clear a rift early).
- Multiple Null Spawn Egg dispenser behavior (creative-only via tab).
- Achievement / advancement for killing Null.

## File-level changes

**New files:**

- `wildwest/src/main/java/com/tweeks/wildwest/entity/BossSingletonState.java` (shared POJO; replaces `HerobrineState` and `AgentState`)
- `wildwest/src/main/java/com/tweeks/wildwest/entity/BossSingletonSavedData.java` (shared SavedData base)
- `wildwest/src/main/java/com/tweeks/wildwest/entity/NullEntity.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/NullRiftEntity.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/NullSavedData.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/NullDriftGoal.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/NullRiftGoal.java`
- `wildwest/src/main/java/com/tweeks/wildwest/item/VoidMarkItem.java`
- `wildwest/src/main/java/com/tweeks/wildwest/item/NullSpawnEggItem.java`
- `wildwest/src/main/java/com/tweeks/wildwest/spawning/NullSpawnRules.java`
- `wildwest/src/main/java/com/tweeks/wildwest/client/NullRenderer.java`
- `wildwest/src/main/java/com/tweeks/wildwest/client/NullEyesLayer.java`
- `wildwest/src/main/java/com/tweeks/wildwest/client/NullRiftRenderer.java`
- `wildwest/src/main/java/com/tweeks/wildwest/event/VoidMarkHandler.java` (the `LivingDamageEvent` subscriber)
- `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/null_overworld_spawns.json`
- `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/null_nether_spawns.json`
- `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/null_end_spawns.json`
- `wildwest/src/main/resources/data/wildwest/loot_table/entities/null.json`
- `wildwest/src/main/resources/assets/wildwest/textures/entity/null.png`
- `wildwest/src/main/resources/assets/wildwest/textures/entity/null_eyes.png`
- `wildwest/src/main/resources/assets/wildwest/textures/item/void_mark.png`
- `wildwest/src/main/resources/assets/wildwest/textures/item/null_spawn_egg.png` (if the standard spawn-egg template isn't used — verify against existing spawn-egg precedent in the module)
- `wildwest/src/main/resources/assets/wildwest/models/item/void_mark.json`
- `wildwest/src/main/resources/assets/wildwest/models/item/null_spawn_egg.json` (if needed; spawn-egg JSON often inherits from `item/template_spawn_egg`)
- `wildwest/tools/null.bbmodel`

**Modified files:**

- `wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineSavedData.java` — rewrite to extend `BossSingletonSavedData`.
- `wildwest/src/main/java/com/tweeks/wildwest/entity/AgentSavedData.java` — rewrite to extend `BossSingletonSavedData`.
- `wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineEntity.java` — update calls from `HerobrineState` → `BossSingletonState` (mostly type renames).
- `wildwest/src/main/java/com/tweeks/wildwest/entity/AgentEntity.java` — same rename pass.
- `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java` — add `NULL` + `NULL_RIFT` `DeferredHolder`s.
- `wildwest/src/main/java/com/tweeks/wildwest/Registration.java` — add `NULL_SPAWN_EGG` + `VOID_MARK` `DeferredItem`s; add both to `WILDWEST_TAB.displayItems`.
- `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java` — register attributes for `NULL` (do NOT register attributes for `NULL_RIFT` — it's an `Entity`, not a `Mob`); register spawn placement for `NULL` via `NullSpawnRules`; register `VoidMarkHandler` on the appropriate event bus.
- `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java` — register `NullRenderer` for `NULL` and `NullRiftRenderer` for `NULL_RIFT`.
- `wildwest/src/main/resources/assets/wildwest/lang/en_us.json` — add the 8 lang strings.

**Deleted files:**

- `wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineState.java` — replaced by `BossSingletonState`.
- `wildwest/src/main/java/com/tweeks/wildwest/entity/AgentState.java` — replaced by `BossSingletonState`.
- `wildwest/src/test/java/com/tweeks/wildwest/entity/HerobrineStateTest.java` — renamed/migrated to `BossSingletonStateTest`.
- `wildwest/src/test/java/com/tweeks/wildwest/entity/AgentStateTest.java` — covered by `BossSingletonStateTest`.

## Testing strategy

### Unit tests (JUnit 5 / Jupiter)

1. **`BossSingletonStateTest`** — five tests covering default state, setAlive, clear, copyOf equality, copyOf independence. Replaces `HerobrineStateTest` and `AgentStateTest`.
2. **`NullRiftLifecycleTest`** — NOT a pure unit test if it needs `Entity`. Skip; covered by manual testing.
3. **`VoidMarkInventoryScanTest`** — extract a static helper `VoidMarkHandler.findFirstVoidMarkSlot(NonNullList<ItemStack> items, Item voidMarkItem) -> int` (returns index or -1). Tests:
   - Empty inventory → -1.
   - Inventory with no Void Marks → -1.
   - Void Mark in slot 5 → 5.
   - Multiple Void Marks (slot 2 and slot 7) → 2 (first wins).
   - Void Mark stack of size 1 vs 16 → returns same slot regardless of count.

### Manual checklist (creative client, end-to-end)

- **Refactor smoke test (run FIRST):** load an existing world that has Herobrine or Agent SavedData. Verify the boss state loads correctly — alive flag, current UUID, dimension all preserved across the refactor.
- **Null spawn egg + singleton:** verify spawn, then second egg use teleports existing Null instead of spawning. White boss bar appears.
- **Dimension gate:** egg refused in any non-vanilla dim (only testable if a modded dim is loaded; otherwise skip).
- **Tri-dim spawn parity:** force natural spawn in each of overworld (night), Nether, End by editing biome modifier weights temporarily. Singleton prevents simultaneous spawns across dims.
- **Cross-dim refusal:** with Null alive in End, go to overworld and try the egg; expect "Null walks elsewhere…".
- **Drift behavior:** Null floats above terrain, follows player over hills/buildings, never lands. Sets Y to terrain heightmap + 3.
- **Rifts:** rift spawns at player position. 2s telegraph (growing portal-particle ring). 4s active phase (dense particles, damage on dwell, pull toward center, item entities erased). Cadence ~5s.
- **Rift LoS:** stand behind a wall — Null cannot spawn rifts on you. Step out — rift spawns immediately.
- **Rift no-melee:** approach Null in melee. He floats above you, no damage from contact, continues to spawn rifts at your feet.
- **Death + drops:** netherite block + void mark always; 100 XP.
- **Void Mark trigger:** stockpile marks in inventory, dive off a cliff into a deep pit, take fatal fall damage. Mark consumed; HP set to 1; teleport to spawn; totem-pop sound + portal particles + actionbar message.
- **Void Mark in offhand:** verify it does NOT trigger (top-level inventory only).
- **Void Mark in creative:** verify no trigger when `/kill @s` in creative.
- **Concurrent with Herobrine + Agent:** spawn all three. Verify three boss bars (red Herobrine, purple Agent, white-notched Null) visible simultaneously. Verify singletons don't interfere across bosses.
- **Save / load:** with Null alive, save and reload; boss reloads with same HP, singleton flag intact, boss bar reattaches. Rifts in flight at save time do NOT need to persist (acceptable for them to disappear on reload — they're 6s-max ephemera).

## Risks / open questions

1. **`LivingDamageEvent` shape in NeoForge 26.x** — `LivingDamageEvent.Pre` vs `LivingDamageEvent` vs `LivingHurtEvent` vs `LivingIncomingDamageEvent`. NeoForge has shuffled this naming. Mirror whatever shape is already imported in the wildwest module (likely none yet — first event handler); fall back to the canonical "fires server-side, exposes `getNewDamage`, settable" event. Verify against vanilla totem-of-undying implementation in the active version.
2. **Cross-dim teleport API for Void Mark** — vanilla `ServerPlayer` has had `teleportTo(ServerLevel, x, y, z, yaw, pitch)` for many versions; in 26.x there's also `changeDimension(DimensionTransition)`. The respawn-point lookup (`getRespawnPosition`) and cross-dim teleport both need version-correct calls. Mirror vanilla `EndPortalBlock` / `BedBlock.findStandUpPosition` for canonical idioms.
3. **`OUT_OF_WORLD` damage bypass** — `LivingDamageEvent` may not fire for void damage. If so, Void Mark cannot save from void. Out of scope to work around; documented limitation.
4. **`HumanoidMobRenderer` generic signature in 26.x** — same risk as Herobrine and Agent. Mirror existing usage exactly.
5. **`EntitySpawnReason` / `MobSpawnType`** — same renamed-type risk. Mirror `AgentEntity.finalizeSpawn` signature.
6. **`SoundEvents.X` Holder vs direct** — same `.value()` risk. Mirror existing usage.
7. **`level.damageSources().magic()` availability** — `DamageSources` class is the modern shape; verify against existing damage-source calls in the module (probably present in bullet/projectile entities).
8. **NeoForge biome modifier schema for Nether** — `#minecraft:is_nether` tag should exist; verify against vanilla biome tag list at implementation time.
9. **Refactor breakage in HerobrineEntity / AgentEntity** — the rename `HerobrineState` → `BossSingletonState` will touch the boss entity classes. Verify all existing tests still pass after the refactor (the test changes are mechanical renames; the production code changes are mechanical renames). Run the full `:wildwest:test` suite after the refactor phase before starting Null-specific work.
10. **`Entity` lacks `getBaseExperienceReward`** — only `LivingEntity` and below have it. `NullRiftEntity extends Entity` correctly omits this. The rift drops nothing because it's not a creature.
11. **Boss bar `NOTCHED_6` vs other overlays** — vanilla supports `PROGRESS`, `NOTCHED_6`, `NOTCHED_10`, `NOTCHED_12`, `NOTCHED_20`. `NOTCHED_6` displays 6 segments visually, no mechanical tie. Confirmed in vanilla `BossEvent.BossBarOverlay` enum.

## Self-review

| Concern | Section |
|---|---|
| Refactor of existing singleton infra | Singleton refactor |
| Entity stats, hitbox, flight | Entity |
| Singleton storage + lifecycle | Singleton mechanic |
| Natural spawn (three dims) | Spawning |
| Spawn egg (singleton-aware, cross-dim aware) | Spawning |
| Combat AI (drift, rift goal) | Combat |
| Rift entity (telegraph, hazard, pull, item-erase) | Combat / NullRiftEntity |
| No equipment | Equipment |
| Visuals (texture, renderer, eyes layer, rift renderer, bbmodel) | Visuals |
| Boss bar (white, notched_6) | Boss bar |
| Drops + XP | Loot |
| Void Mark item (passive, event-driven) | Void Mark item |
| Damage type | (none needed — explained) |
| Sounds | Sounds |
| Lang strings | Lang strings |
| Out of scope (incl. edge cases) | Out of scope |
| All new + modified + deleted files | File-level changes |
| Test plan (unit + manual, refactor smoke test first) | Testing strategy |
| Known risks + version-drift mitigations | Risks / open questions |

Mechanically distinct from Herobrine and Agent on every axis: kit (rifts vs sword+meteor vs bow+swap), movement (drift vs walk vs walk+teleport), defensive (none — HP carries vs none vs phantom-swap), spawn (3 dims vs 1 vs 2), boss bar (WHITE NOTCHED_6 vs RED PROGRESS vs PURPLE PROGRESS), teleport particles (ENCHANT vs PORTAL vs SMOKE), ambient (UNDERWATER vs CAVE vs ELDER_GUARDIAN), drop (Void Mark passive vs Meteor Staff active vs Cursed Tome active). Singleton is parallel and now sits on shared refactored infra.

No placeholders. No `[TBD]`. Math values (HP 240, speed 0.20, rift cooldown 100t, rift telegraph 40t, rift active 80t, rift damage 2-per-10-ticks → 4 dps, pull magnitude 0.15, Void Mark cap 16 stack) are explicit and consistent across sections.
