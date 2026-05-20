# Wild-West Mod — Grim Reaper — Design

**Date:** 2026-05-20
**Status:** Draft (pending user review of this doc)
**Series:** Stand-alone feature on top of merged Wild-West phases 1–3 + zombie-virus + Steve-Stacker + Herobrine + The Agent + Pirates + Null. Sits on the existing `BossSingletonSavedData` infra introduced by the Null spec. No phase number.

## Goal

Add a fifth apex boss to the `:wildwest` mod called **Grim Reaper**: a singleton, overworld-only night-spawn hooded humanoid that fights primarily through summoned skeleton minions and a vertical-launch crowd-control attack. He drops a single iconic item — the **Reaper Scythe** — a dual-purpose weapon that is both a melee scythe AND a right-click cast that summons player-owned skeleton helpers that fight hostiles and mine precious ores.

Mechanically he is the "minion master" archetype, distinct from the existing four apex bosses on kit, defensive profile, and tempo:

| Axis              | Herobrine                 | The Agent                  | Null                                 | Grim Reaper                          |
|-------------------|---------------------------|----------------------------|--------------------------------------|--------------------------------------|
| Role              | Zone-control bruiser      | Bow/sword keep-away duelist | Environmental-hazard god             | Minion master + CC                  |
| Engagement        | Closes & melees           | Mid-range, repositions     | Drifts inevitably toward player      | Walks in, raises adds, scythe melee  |
| Movement          | Walks (0.35) + teleport   | Walks (0.45) + teleport    | Floats (0.20), ignores terrain       | Walks (0.28), no teleport            |
| Defensive         | None (pure HP)            | Phantom swap (decoy)       | None — wears player down with rifts  | Adds soak damage; reaper himself frail |
| HP                | 200                       | 160                        | 240                                  | 100 (glass cannon)                   |
| Boss bar          | RED, PROGRESS             | PURPLE, PROGRESS           | WHITE, NOTCHED_6                     | YELLOW, NOTCHED_10                   |
| Ambient sound     | AMBIENT_CAVE              | ELDER_GUARDIAN_AMBIENT_LAND| AMBIENT_UNDERWATER_LOOP              | WITHER_AMBIENT (low pitch)           |
| Spawn dims        | Overworld (night)         | Overworld (night) + End    | Overworld (night) + Nether + End     | Overworld (night)                    |
| Signature drop    | Meteor Staff (active)     | Cursed Tome (active)       | Void Mark (passive death-save)       | Reaper Scythe (melee + active summon)|

## Platform

Java 25 / NeoForge (`neo_version` from `gradle.properties`). Matches the existing `:wildwest` module. Avoid hard-coded MC version assumptions; mirror APIs already used by `HerobrineEntity` / `AgentEntity` / `NullEntity` (`Identifier.fromNamespaceAndPath`, `HumanoidMobRenderer`, `DeferredRegister`, `SavedData` + `SavedDataType` + `Codec`, biome-modifier JSON, etc.).

## In scope

### Entity

- New entity id: `wildwest:grim_reaper`. Java class `com.tweeks.wildwest.entity.GrimReaperEntity extends net.minecraft.world.entity.monster.Monster`. Not a `WildWestMob` subclass — apex tier sits parallel.
- `MobCategory.MONSTER`.
- Hitbox: `0.6 × 2.2` (slightly taller than vanilla humanoid — reaper presence; matches a hooded silhouette).
- `clientTrackingRange`: 10. `updateInterval`: default.
- Knockback resistance: `0.5` (resistant but not immovable — he's frail, knockback should still feel impactful but not trivially chain).
- Follow range: `48.0`.
- Total HP: `100.0` (lowest of the five apex bosses by a wide margin — glass cannon by design; the minions are the threat).
- Attack damage attribute: `6.0` (3 hearts via scythe melee).
- Movement speed attribute: `0.28` (walks normally; no teleport mechanic).
- Armor: `4.0` (light plate-equivalent).
- Fire damage handling: **immune** (`isInvulnerableTo` returns true for `DamageTypeTags.IS_FIRE` — thematic, matches the supernatural-death feel; consistent with Herobrine's fire immunity).
- Fall damage: immune (override `causeFallDamage(...)` returning false — he won't take fall damage from his own Soul Lift kicking him in the air via knockback, and he can be lifted by other entities without taking damage).
- `setPersistenceRequired()` in constructor.
- **No flight.** He walks. No `setNoGravity`. Standard gravity + pathfinding.

### Singleton mechanic

Anchored to the existing `BossSingletonSavedData` infrastructure (introduced by the Null spec — `wildwest/src/main/java/com/tweeks/wildwest/entity/BossSingletonSavedData.java`).

- New class `com.tweeks.wildwest.entity.GrimReaperSavedData extends BossSingletonSavedData`.
  - `FILE_ID = "wildwest_grim_reaper"`.
  - Static `SavedDataType<GrimReaperSavedData> TYPE`.
  - Static `get(MinecraftServer server)` accessor.
- Stored at `server.overworld().getDataStorage()` (anchored to overworld; Grim Reaper only spawns in overworld so this is also the dim of record).
- Lifecycle (mirrors Herobrine / Agent / Null):
  1. **On spawn** (`finalizeSpawn`, server-side): if `alive == true` and the existing entity is not this one, `discard()`. Otherwise, claim the singleton by setting `alive = true`, `currentId = this.getUUID()`, `dimension = level().dimension()`.
  2. **On death:** override `die(DamageSource)` to clear `alive = false` then `super.die(...)`.
  3. **On removal** (`remove(RemovalReason)`): if `KILLED` or `DISCARDED`, clear singleton; if `UNLOADED_*`, leave the flag alone. Always call `bossBar.removeAllPlayers()` before delegating to `super.remove(reason)`.
  4. **Recovery for stuck flag:** documented as known edge case; `/kill @e[type=wildwest:grim_reaper]` resolves.

### Spawning

Single biome modifier (overworld only).

- `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/grim_reaper_overworld_spawns.json`
  - Biome tag: `#minecraft:is_overworld`.
  - Spawner: `{ "type": "wildwest:grim_reaper", "weight": 1, "minCount": 1, "maxCount": 1 }`.

**Spawn placement** (registered once in `WildWestMod.registerSpawnPlacementsEvent`):

- `SpawnPlacementTypes.ON_GROUND`.
- `Heightmap.Types.MOTION_BLOCKING_NO_LEAVES`.
- Predicate: `GrimReaperSpawnRules::checkSpawnRules`:
  - `Monster::checkMonsterSpawnRules` (light ≤ 7, valid block, etc.).
  - Require `level.canSeeSky(pos)` (night sky flavor — matches Herobrine, Agent, Null overworld branches).
  - Require `level.getMaxLocalRawBrightness(pos) < 4` (extra-dark — slightly stricter than the standard `≤ 7` to make the spawn flavor distinct from regular zombies/skeletons).
  - Singleton check: short-circuit-return false if `GrimReaperSavedData.get(server).isAlive()` (avoid wasting a natural spawn on a doomed `discard()`).

**Spawn egg** with singleton-aware behavior. New item `wildwest:grim_reaper_spawn_egg`.

- Class `GrimReaperSpawnEggItem extends SpawnEggItem`.
- Egg colors: primary `0x202020` (near-black robe), secondary `0xC8C8C8` (bone gray).
- Override `useOn(UseOnContext)`:
  - Server-side only does work; client returns `InteractionResult.SUCCESS`.
  - Resolve `GrimReaperSavedData savedData = GrimReaperSavedData.get(server)`.
  - **If `alive == false`:** delegate to `super.useOn(context)` (vanilla spawn flow). `finalizeSpawn` sets the singleton flag.
  - **If `alive == true`:** resolve existing entity via `server.getLevel(savedData.getDimension()).getEntity(savedData.getCurrentId())`.
    - **Entity loaded AND in same dimension as player:** teleport Grim Reaper to the clicked location. Spawn `ParticleTypes.SOUL` burst at both source and destination. Play `SoundEvents.SOUL_ESCAPE`. Egg not consumed. Return `InteractionResult.SUCCESS`.
    - **Entity loaded in different dimension:** show actionbar `item.wildwest.grim_reaper_spawn_egg.different_dimension`. Return `InteractionResult.FAIL`.
    - **Entity not loaded (unloaded chunk):** show actionbar `item.wildwest.grim_reaper_spawn_egg.away`. Return `InteractionResult.FAIL`.
- Dimension gate (in `useOn` before any singleton work): require `level.dimension() == Level.OVERWORLD`. Else show actionbar `item.wildwest.grim_reaper_spawn_egg.wrong_dimension`, return `InteractionResult.FAIL`.

### Combat

#### AI goals (priority order)

In `GrimReaperEntity.registerGoals`:

1. `new FloatGoal(this)` — pathfind around water normally.
2. `new GrimReaperSoulLiftGoal(this)` — see below. Higher priority than melee so it interrupts the swing chain when target is in launch range and cooldown is ready.
3. `new GrimReaperRaiseDeadGoal(this)` — see below. Mid priority — used between melee approaches when target out of melee range.
4. `new MeleeAttackGoal(this, 1.0D, false)` — standard pathfind-and-swing. The scythe is rendered in the main hand via equipment slot (see Equipment).
5. `new WaterAvoidingRandomStrollGoal(this, 0.8D)`.
6. `new LookAtPlayerGoal(this, Player.class, 16.0F)`.
7. `new RandomLookAroundGoal(this)`.

Targeting:

1. `new HurtByTargetGoal(this)`.
2. `new NearestAttackableTargetGoal<>(this, Player.class, true)`.

#### Scythe melee

- Damage 6.0 via `Attributes.ATTACK_DAMAGE` (set at attribute registration in `WildWestMod`). The held scythe item is **cosmetic** server-side from a damage-calculation perspective: vanilla mobs deal damage from their attribute, not the held item's tool stats. Reusing the same `Reaper Scythe` ItemStack as the equip is fine for renderer purposes.
- Attack speed: vanilla `MeleeAttackGoal` cadence (default `attackInterval = 20` ticks).

#### Raise Dead — `GrimReaperRaiseDeadGoal`

Custom goal class `com.tweeks.wildwest.entity.ai.GrimReaperRaiseDeadGoal extends Goal`.

- **Trigger condition (`canUse`):** has a `LivingEntity` target; cooldown timer ≤ 0; target within 24 blocks; reaper not currently swinging.
- **Effect on start:**
  - Pick 2–3 random ground positions within 8 blocks of the target. Use `level.getHeightmapPos(MOTION_BLOCKING_NO_LEAVES, ...)` to land on actual ground. Reject positions inside fluid or with no air gap above. If fewer than 2 valid positions found in 10 attempts, abort and reset cooldown to half so we try again sooner.
  - For each valid position: spawn dirt-burst particles (`new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState())`, 20 particles in a small cone) and play `SoundEvents.ZOMBIE_VILLAGER_CONVERTED` at quiet volume.
  - 20-tick (1s) "emerging" delay during which the goal is still active but no skeleton entity exists yet. Visual feedback during this period: keep emitting a small dirt particle every 4 ticks at each pending position.
- **At end of emerge delay:**
  - Spawn one vanilla `EntityType.SKELETON` at each pending position.
  - Equip each skeleton with `new ItemStack(Items.IRON_SWORD)` enchanted with `Enchantments.SHARPNESS` level 3. Use `EnchantmentHelper.setEnchantments(...)` (mirror existing enchant-application code in the module if present; otherwise use the canonical vanilla idiom).
  - Set `skeleton.setTarget(reaper.getTarget())` so they immediately engage the player.
  - Tag each spawned skeleton with a persistent NBT marker (`wildwest:grim_reaper_minion = true`) so we can find them on reaper death.
- **Cooldown after summon:** 200 ticks (10 s).
- **Cleanup on reaper death:** in `GrimReaperEntity.die(DamageSource)`, scan loaded skeletons in the level for the `wildwest:grim_reaper_minion` NBT tag and `discard()` them. Avoids the "swarm aftermath" feel where killing the boss leaves a battlefield of skeletons still active.

#### Soul Lift — `GrimReaperSoulLiftGoal`

Custom goal class `com.tweeks.wildwest.entity.ai.GrimReaperSoulLiftGoal extends Goal`.

- **Trigger condition (`canUse`):** has a `LivingEntity` target; cooldown timer ≤ 0; target within 12 blocks; target is on ground (`target.onGround() == true`); target is a `Player` (do NOT lift non-players to avoid breaking skeleton minion combat).
- **Effect on start:**
  - Telegraph phase, 10 ticks (0.5 s). During telegraph, emit a vertical column of `ParticleTypes.SOUL_FIRE_FLAME` particles centered on the target's feet (3 particles per tick at varying Y offsets 0–2). Play `SoundEvents.SOUL_ESCAPE` at the target's location at telegraph start.
  - Reaper raises its right arm (cosmetic only — no model articulation in scope, just the swing animation via `swing(InteractionHand.MAIN_HAND)`).
- **At telegraph end:**
  - Re-verify target is still in range and on ground. If not, abort.
  - Apply launch: `target.setDeltaMovement(target.getDeltaMovement().x, 1.4, target.getDeltaMovement().z)`. Peaks at ~6.5 blocks above start position (computed from `v²/2g` with g ≈ 0.08; verified against vanilla peak height for similar deltaY values).
  - For player targets, also call `((ServerPlayer) target).hurtMarked = true` to force the velocity packet to send immediately (vanilla quirk — server velocity changes to players don't propagate without this flag).
  - Fall damage is left to vanilla — player falls ~6.5 blocks → ~3.5 hearts of fall damage. Acceptable variance: 3 damage on a flat 6-block fall, 4 damage on a 7-block fall; if the player lands on a hill or block 1 below, can drop to 2.5 hearts; if landing in deeper terrain, can climb to 4.5. Target band of 3–4 hearts holds for level ground.
- **Cooldown after launch:** 120 ticks (6 s).

### Equipment

- Main hand: `new ItemStack(Registration.REAPER_SCYTHE.get())` set via `setItemSlot(EquipmentSlot.MAINHAND, ...)` in constructor. Renderer picks up the held item via standard `HumanoidMobRenderer` arm rendering. The scythe model JSON should have a `display.thirdperson_righthand` transform that orients it like a tall pole arm.
- No armor slots populated (the texture itself paints in the dark robe).

### Visuals

- Texture: `wildwest/src/main/resources/assets/wildwest/textures/entity/grim_reaper.png`. 64×64 humanoid layout. Dark robe (near-black with subtle purple shading), skeletal face visible inside the hood, bony hands. A wider hood/cape silhouette would benefit from a bbmodel override; for v1 the vanilla biped UV is the floor.
- Renderer: `com.tweeks.wildwest.client.GrimReaperRenderer extends HumanoidMobRenderer<GrimReaperEntity, ...>`. Mirror the generic parameter shape used by `HerobrineRenderer` / `AgentRenderer`.
- Optional bbmodel: `wildwest/tools/grim_reaper.bbmodel` for a cloak overlay layer. Deferred to polish if time-boxed; v1 ships with biped + texture only.

### Boss bar

- Color: `BossEvent.BossBarColor.YELLOW` (distinct from Herobrine RED, Agent PURPLE, Null WHITE, Steve Stacker whatever-it-uses — verify in `SteveStackerEntity` at implementation time).
- Overlay: `BossEvent.BossBarOverlay.NOTCHED_10` (10 segments — 100 HP makes each notch worth 10 HP, clean math; visually distinguishes from Null's NOTCHED_6).
- Server-side `ServerBossEvent` constructed in entity constructor, updated each tick from `getHealth() / getMaxHealth()`. Players within tracking distance added via `bossEvent.addPlayer(serverPlayer)` in `startSeenByPlayer` override; removed in `stopSeenByPlayer`.

### Loot

`wildwest/src/main/resources/data/wildwest/loot_table/entities/grim_reaper.json`:

- 1× `wildwest:reaper_scythe` (guaranteed, rolls = 1, single item pool).
- 3–5 `minecraft:bone` (rolls = 1, uniform 3–5).
- 1× `minecraft:soul_sand` (guaranteed).
- XP drop: `80` (via `getBaseExperienceReward` override in `GrimReaperEntity` — between vanilla bosses' high values and standard hostile mob drops; matches the apex tier feel).

No conditional / looting-enhanced drops.

### Reaper Scythe item

New file `wildwest/src/main/java/com/tweeks/wildwest/item/ReaperScytheItem.java`.

- `class ReaperScytheItem extends Item`. Properties: `stacksTo(1)`, `rarity(Rarity.EPIC)`, `durability(0)` (unbreakable, like Meteor Staff). Apply `attributeModifiers(...)` to give it left-click melee damage (`Attributes.ATTACK_DAMAGE` +6.0, `Attributes.ATTACK_SPEED` -2.4 → ~1.0 atk/s).
  - The exact API for attaching attribute modifiers in NeoForge 26.x is `Item.Properties.attributes(ItemAttributeModifiers)` or similar — mirror whatever `BillyClubItem` / `RapierItem` / `BanditKnifeItem` do (they all have melee profiles).
- Constants:
  - `COOLDOWN_TICKS = 100` (5 s)
  - `MAX_MINIONS = 3` (per-owner cap)
  - `SUMMON_RANGE = 4.0` (blocks ahead of player look position to spawn the minion)
- Override `use(Level, Player, InteractionHand)` (right-click cast):
  - Server-side: if `player.getCooldowns().isOnCooldown(stack)` → return `InteractionResult.FAIL`.
  - Count alive `ScytheSkeletonEntity` minions belonging to this player UUID across the player's current level. If `>= MAX_MINIONS`, show actionbar `item.wildwest.reaper_scythe.cap_reached` and return `InteractionResult.FAIL` (no cooldown applied — don't punish over-clicks).
  - Compute spawn position: `playerEye + look.scale(SUMMON_RANGE)`, projected down to first ground block. If no valid ground within 4 blocks down, fall back to `player.position()`.
  - Spawn `ScytheSkeletonEntity` at that position. Set owner UUID. Equip iron sword (no enchant — player-summoned minions are utility, weaker than reaper-summoned).
  - Play `SoundEvents.ZOMBIE_VILLAGER_CONVERTED` at the spawn position.
  - Particle burst: 15 `ParticleTypes.SOUL` at spawn.
  - `player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS)`.
  - Return `InteractionResult.CONSUME`.
- Override `appendHoverText(...)` to show tooltip: melee damage line, cooldown, summon description, cap.

#### Scythe Skeleton minion entity

New entity `wildwest:scythe_skeleton`. Java class `com.tweeks.wildwest.entity.ScytheSkeletonEntity extends Skeleton`.

- `MobCategory.CREATURE` (companion category, like tamed wolves — does NOT count toward monster spawn cap, does NOT despawn naturally when persistence is required).
- Hitbox: inherit from `Skeleton` (`0.6 × 1.99`).
- Persistent: yes. `setPersistenceRequired()` in constructor.
- Custom data:
  - `EntityDataAccessor<Optional<UUID>> DATA_OWNER` — synced for client-side rendering of owner indicator (a small soul-fire flame above their head when the player looks at them).
  - Stored in NBT as `OwnerUUID`.
- Constructor: equip iron sword main hand, iron helmet head slot (cosmetic differentiator from vanilla skeletons). No bow.
- Override `aiStep`: call `super.aiStep()`, then clear any fire applied this tick (`setRemainingFireTicks(0)` / equivalent — mirror `HerobrineEntity`'s fire-immunity handling). Bone servants are soul-bound, not classic undead — sun-immune by design.
- Goals (priority order, all custom or vanilla-reused):
  1. `new FloatGoal(this)`.
  2. `new MeleeAttackGoal(this, 1.1D, true)` — true = follow even when target out of sight, to keep chasing through walls/corners.
  3. `new ScytheSkeletonMineOreGoal(this)` — see below. Mid priority.
  4. `new ScytheSkeletonFollowOwnerGoal(this, 1.0D, 5.0F, 10.0F)` — vanilla wolf follow-owner shape, ported. Vanilla `FollowOwnerGoal` is hardcoded to `TamableAnimal`, which `ScytheSkeletonEntity` is not, so we own the implementation. Min distance 5 (don't crowd owner closer than that), teleport-recall distance 10 (if owner > 10 blocks away or out of LoS, teleport behind owner). Implementation outline: each tick check owner distance and pathfind toward owner if distance > min; if distance > teleport-recall, snap to a valid block position 1–3 blocks behind owner.
  5. `new WaterAvoidingRandomStrollGoal(this, 0.8D)`.
  6. `new LookAtPlayerGoal(this, Player.class, 8.0F)`.
- Targeting goals (custom):
  - `new ScytheSkeletonTargetHostilesGoal(this)` — picks nearest `Monster` within 16 blocks, EXCLUDING:
    - Other `ScytheSkeletonEntity` with the same owner UUID.
  - Grim Reaper himself is a valid target (he's a `Monster`); siccing minions on the boss is intended.
  - Target predicate: `entity instanceof Monster && !(entity instanceof ScytheSkeletonEntity sse && sse.getOwnerUUID().filter(o -> o.equals(this.getOwnerUUID().orElse(null))).isPresent())`.

#### Scythe Skeleton mining goal

Custom `com.tweeks.wildwest.entity.ai.ScytheSkeletonMineOreGoal extends Goal`.

- **Idle counter:** the goal owns a `int idleTicks` field. Each tick of `ScytheSkeletonEntity.aiStep` increments it; reset to `0` whenever (a) `getTarget()` is non-null, (b) the navigation has an active path (`getNavigation().isInProgress()`), or (c) the owner is < 5 blocks away (the follow-owner goal is the active behavior). When `idleTicks >= 60`, the goal is eligible to start.
- **Trigger condition (`canUse`):** no current attack target; `idleTicks >= 60`; owner is alive and within 32 blocks; no existing mining target in progress.
- **Effect on start:**
  - Scan a `3×3×3` cube centered on the skeleton's position for blocks matching the "precious ores" set.
  - Precious-ore set: `Blocks.IRON_ORE`, `Blocks.DEEPSLATE_IRON_ORE`, `Blocks.GOLD_ORE`, `Blocks.DEEPSLATE_GOLD_ORE`, `Blocks.NETHER_GOLD_ORE`, `Blocks.DIAMOND_ORE`, `Blocks.DEEPSLATE_DIAMOND_ORE`, `Blocks.EMERALD_ORE`, `Blocks.DEEPSLATE_EMERALD_ORE`, `Blocks.ANCIENT_DEBRIS`. Hardcoded `Set<Block>` — explicit list, not a tag, because the `c:ores` tag includes coal/copper/redstone/lapis/quartz which we want excluded.
  - Pick the closest matching block. If none, end goal.
- **Effect during tick:**
  - Path the skeleton to within `1.5` blocks of the target block.
  - Once adjacent, simulate mining: count up a 20-tick "mining" timer with particle effects (`ParticleTypes.CRIT` at the block position, 3 per tick). On reach 20 ticks: break the block via `level.destroyBlock(blockPos, true, this)` with `dropOnBreak = true`.
  - Capture the block-break drops by overriding behavior: instead of letting drops fall as items, compute `Block.getDrops(state, level, blockPos, blockEntity)` ahead of `destroyBlock`. Call `destroyBlock(..., false, ...)` (drop = false), iterate over the computed drops, attempt `owner.getInventory().add(stack)` for each. If owner inventory full, `level.addFreshEntity(new ItemEntity(level, owner.getX(), owner.getY(), owner.getZ(), stack))` (drop at owner's feet).
- **End goal:** when block broken OR owner moves > 32 blocks away OR new attack target acquired.
- **Cooldown after success:** none (next idle window triggers another scan).
- **Mining range guard:** never mine block at `blockPos.y > owner.getY() + 8` or `< owner.getY() - 8` to avoid skeletons tunneling indefinitely.

#### Scythe Skeleton renderer

- Reuse vanilla `SkeletonRenderer` directly (NO new class). In `ClientSetup`, register `EntityRenderers.register(ModEntities.SCYTHE_SKELETON.get(), SkeletonRenderer::new)`. The iron helmet renders on top of the skeleton skull via vanilla equipment rendering.
- If the iron helmet doesn't cosmetically distinguish enough from a vanilla skeleton, defer a custom texture/renderer to a polish pass — out of scope for v1.

### Sounds

- `setAmbientSound` → `SoundEvents.WITHER_AMBIENT` (low, slow growl — distinct from skeleton/zombie/wither-skeleton). Volume reduced to 0.5 (`getSoundVolume` override) for ambient flavor without overwhelming.
- `getHurtSound(DamageSource)` → `SoundEvents.WITHER_HURT`.
- `getDeathSound()` → `SoundEvents.WITHER_DEATH`.
- Raise Dead trigger → `SoundEvents.ZOMBIE_VILLAGER_CONVERTED`.
- Soul Lift telegraph → `SoundEvents.SOUL_ESCAPE`.
- Soul Lift launch → `SoundEvents.ENDERMAN_TELEPORT` (the "whoosh" of the player going up).
- Scythe melee swing → vanilla weapon swing (no override needed).

### Lang strings

`wildwest/src/main/resources/assets/wildwest/lang/en_us.json`:

- `entity.wildwest.grim_reaper`: `"Grim Reaper"`
- `entity.wildwest.scythe_skeleton`: `"Bone Servant"`
- `item.wildwest.grim_reaper_spawn_egg`: `"Grim Reaper Spawn Egg"`
- `item.wildwest.grim_reaper_spawn_egg.away`: `"The Reaper drifts beyond reach…"`
- `item.wildwest.grim_reaper_spawn_egg.wrong_dimension`: `"The Reaper has no business here…"`
- `item.wildwest.grim_reaper_spawn_egg.different_dimension`: `"The Reaper walks elsewhere…"`
- `item.wildwest.reaper_scythe`: `"Reaper Scythe"`
- `item.wildwest.reaper_scythe.cap_reached`: `"Your bone servants are already three…"`
- `item.wildwest.reaper_scythe.tooltip.melee`: `"Melee: 3 hearts"`
- `item.wildwest.reaper_scythe.tooltip.summon`: `"Right-click: summon bone servant (cap 3, 5s cooldown)"`
- `item.wildwest.reaper_scythe.tooltip.servant`: `"Servants fight hostiles and mine precious ores nearby"`

### Out of scope

- Cross-dimensional spawn-egg teleport (refused with feedback, same as other apex bosses).
- Reaper Scythe in offhand (right-click only fires when held in main hand — vanilla `use(...)` is called by both hands, but we gate on `hand == InteractionHand.MAIN_HAND`).
- Custom Reaper Scythe 3D model — v1 uses a simple item texture; bbmodel polish deferred.
- Phase transitions, HP-threshold ability swaps — flat behavior across all HP.
- Reaper teleport / chase mechanic — he walks. Out of melee, players can kite him.
- Boss bar color or animation changes on damage milestones.
- Grim Reaper vs Herobrine vs Agent vs Null vs Steve Stacker interaction (they ignore each other; no "rivals" AI).
- Custom death cinematic, music, or screen-shake effects.
- Grim Reaper-themed structures, signs, blocks, or world-gen.
- Zombie-virus interaction beyond default.
- Recovery mechanism for stuck singleton flag — same documented edge case as Herobrine / Agent / Null.
- Looting enchantment effect on drops (drops are fixed).
- Scythe Skeleton (player-summoned) own AI for: opening doors, climbing ladders, swimming long distances. Standard mob pathfinding only.
- Scythe Skeleton hunger / starvation / decay over time. They persist until killed or world unloaded.
- Cap on total Scythe Skeletons across multiple players in same world (per-player cap only).
- Mining blocks above bedrock layer constraints — skeleton can dig down to bedrock + 0 if owner stands there; trust the y-bounds guard (±8 from owner y).
- Scythe Skeleton picking up items / armor swap. They wear what they spawn with.
- Player tag system for "friendly mobs" — skeletons target any `Monster`, full stop.
- Achievement / advancement for killing Grim Reaper or owning N skeletons.
- Cross-server / multiplayer dedicated-server stress test (singleton is server-process-scoped, so functionally correct, but not load-tested).
- Cross-singleton interaction during spawn collision (e.g., Null and Grim Reaper try to spawn the same tick — both `finalizeSpawn` calls succeed because they check their own SavedData; this is fine and not a bug).

## File-level changes

**New files:**

- `wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperEntity.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperSavedData.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ScytheSkeletonEntity.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/GrimReaperRaiseDeadGoal.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/GrimReaperSoulLiftGoal.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/ScytheSkeletonFollowOwnerGoal.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/ScytheSkeletonMineOreGoal.java`
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/ScytheSkeletonTargetHostilesGoal.java`
- `wildwest/src/main/java/com/tweeks/wildwest/item/GrimReaperSpawnEggItem.java`
- `wildwest/src/main/java/com/tweeks/wildwest/item/ReaperScytheItem.java`
- `wildwest/src/main/java/com/tweeks/wildwest/spawning/GrimReaperSpawnRules.java`
- `wildwest/src/main/java/com/tweeks/wildwest/client/GrimReaperRenderer.java`
- `wildwest/src/main/resources/data/wildwest/neoforge/biome_modifier/grim_reaper_overworld_spawns.json`
- `wildwest/src/main/resources/data/wildwest/loot_table/entities/grim_reaper.json`
- `wildwest/src/main/resources/assets/wildwest/textures/entity/grim_reaper.png`
- `wildwest/src/main/resources/assets/wildwest/textures/item/reaper_scythe.png`
- `wildwest/src/main/resources/assets/wildwest/textures/item/grim_reaper_spawn_egg.png` (if standard spawn-egg template isn't used)
- `wildwest/src/main/resources/assets/wildwest/models/item/reaper_scythe.json`
- `wildwest/src/main/resources/assets/wildwest/models/item/grim_reaper_spawn_egg.json` (if needed)
- `wildwest/src/test/java/com/tweeks/wildwest/entity/ScytheSkeletonOwnerScanTest.java`
- `wildwest/src/test/java/com/tweeks/wildwest/entity/GrimReaperSpawnRulesTest.java` (if testable in isolation)
- `wildwest/src/test/java/com/tweeks/wildwest/item/ReaperScytheItemTest.java`

**Modified files:**

- `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java` — add `GRIM_REAPER` and `SCYTHE_SKELETON` `DeferredHolder`s.
- `wildwest/src/main/java/com/tweeks/wildwest/Registration.java` — add `GRIM_REAPER_SPAWN_EGG` + `REAPER_SCYTHE` `DeferredItem`s; add both to `WILDWEST_TAB.displayItems`.
- `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java` — register attributes for `GRIM_REAPER` and `SCYTHE_SKELETON`; register spawn placement for `GRIM_REAPER` via `GrimReaperSpawnRules`.
- `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java` — register `GrimReaperRenderer` for `GRIM_REAPER`; register vanilla `SkeletonRenderer::new` for `SCYTHE_SKELETON`.
- `wildwest/src/main/resources/assets/wildwest/lang/en_us.json` — add the 11 lang strings.

## Testing strategy

### Unit tests (JUnit 5 / Jupiter)

1. **`ScytheSkeletonOwnerScanTest`** — extract a static helper `ScytheSkeletonEntity.countMinionsOwnedBy(Level level, UUID owner) -> int`. Tests:
   - Empty level → 0.
   - 1 minion belonging to owner A → 1.
   - 2 minions belonging to A, 1 to B → query A returns 2, query B returns 1.
   - Dead minion (`isAlive() == false`) NOT counted.
2. **`ReaperScytheItemTest`** — verify constants only (cooldown ticks, max minions, summon range). Smoke check, since the actual `use(...)` flow requires a `Level` and `Player` instance that's painful to mock; defer behavior testing to manual.
3. **Skip `GrimReaperSpawnRulesTest`** if the predicate signature requires deep mocking — covered by manual force-spawn test.

(No `BossSingletonStateTest` change needed — that test exists from the Null spec and covers the shared infra. Grim Reaper rides on top without touching the shared base.)

### Manual checklist (creative client, end-to-end)

- **Spawn egg + singleton:** verify spawn, then second egg use teleports existing Grim Reaper instead of spawning. Yellow boss bar appears with 10 segments.
- **Dimension gate:** egg refused in nether/end with appropriate actionbar message.
- **Natural spawn:** force-spawn at night by editing biome modifier weight temporarily. Verify spawn in overworld under open sky, light ≤ 3, ground placement.
- **Cross-dim refusal:** with Grim Reaper alive in overworld, go to nether and try the egg; expect refusal.
- **Raise Dead cycle:** engage the reaper. Within 10 s of engagement, 2–3 skeletons emerge from the ground with iron swords. Verify Sharpness 3 by checking the sword tooltip via `/loot give` or NBT inspection of an emerged skeleton. Verify cadence: another wave 10 s later.
- **Reaper death cleanup:** with several minion skeletons alive, kill the reaper. Verify minion skeletons are discarded (despawn) on his death.
- **Soul Lift:** stand within 12 blocks on flat ground. Verify telegraph particle column (0.5 s) → vertical launch to ~6–7 blocks → fall back to ground for ~3–4 fall damage. Test on hills (damage should still hit 3–4 band).
- **Soul Lift cooldown:** can't be re-triggered within 6 s of last launch.
- **Soul Lift won't target non-players:** reaper engages a wolf (via /summon and force-aggro) — verify wolves are not lifted (Soul Lift goal targets `Player` only).
- **Scythe melee:** swing reaper at player → 3 hearts damage per hit.
- **Drops:** kill the reaper. Verify Reaper Scythe + 3–5 bones + 1 soul sand + 80 XP.
- **Scythe right-click summon:** right-click in air → bone servant appears 4 blocks ahead at ground level, with iron sword and iron helmet. Particle + sound. 5 s cooldown visible on item icon.
- **Scythe cap:** summon 3, try a 4th → actionbar message, no cooldown applied.
- **Bone servant follows owner:** walk 20 blocks away → servant teleports/paths to follow.
- **Bone servant attacks hostile:** spawn a zombie nearby → servant engages and kills it.
- **Bone servant ignores owner:** PVP-style: attack the servant or other players' servants — verify your own never targets you.
- **Bone servant mines precious ore:** stand still, place an iron ore block within 3 blocks of an idle servant → after 3 s idle, servant paths to the ore and mines it over 1 s; ore drop appears in owner inventory.
- **Bone servant ignores non-precious ore:** place coal/copper/redstone/lapis ore — servant should NOT mine these. Place iron next to coal — servant mines iron, ignores coal.
- **Bone servant inventory overflow:** fill owner inventory, place mineable ore near servant — drop falls at owner's feet instead of into inventory.
- **Bone servant Y-bounds guard:** stand at y=64, place iron ore at y=55 — servant should mine (within ±8); place at y=50 — servant should not (out of bounds).
- **Save / load:** with Grim Reaper alive and 2 bone servants summoned, save and reload. Verify reaper reloads with boss bar and singleton intact, servants persist with owner UUID, and the world's NBT marker on reaper-summoned minion skeletons survives the save.
- **Concurrent with other apex bosses:** spawn Herobrine + Agent + Null + Grim Reaper simultaneously. Verify four boss bars (red, purple, white-notched-6, yellow-notched-10) visible together. Confirm singletons don't interfere.

## Risks / open questions

1. **`EnchantmentHelper.setEnchantments(...)` API shape in NeoForge 26.x** — vanilla shuffles between `Map<Enchantment, Integer>`-style and `ItemEnchantments` builder shape across versions. Mirror existing usage in the wildwest module if any; otherwise mirror vanilla `MobSpawnerLogic` or villager-trade enchantment application code.
2. **`EntityType.SKELETON` direct spawn vs `finalizeSpawn` flow** — when spawning vanilla mobs server-side, the canonical idiom is `entityType.create(level, EntitySpawnReason.MOB_SUMMONED)` followed by `setPos` + `finalizeSpawn` + `addFreshEntity`. Verify against the existing `WildWestMod.summonMob` / similar helper if one exists; otherwise mirror `MobSpawnerLogic.spawnMob`.
3. **Persistent NBT on vanilla `Skeleton`** — applying a custom NBT tag (`wildwest:grim_reaper_minion`) to a vanilla skeleton instance is done via `skeleton.getPersistentData().putBoolean(...)`. Verify this survives save/load — `getPersistentData()` writes to the entity's NBT compound under a NeoForge-specific key. If it doesn't persist, fall back to spawning a custom `GrimReaperMinionSkeletonEntity extends Skeleton` (much heavier change; avoid unless needed).
4. **`Block.getDrops` signature and `BlockEntity` argument** — vanilla `Block.getDrops(BlockState, ServerLevel, BlockPos, BlockEntity)` returns `List<ItemStack>`. The `BlockEntity` can be null for non-block-entity blocks (all precious ores qualify). Verify the call site signature in 26.x.
5. **`FollowOwnerGoal` availability for non-`TamableAnimal` mobs** — vanilla `FollowOwnerGoal` is hardcoded to `TamableAnimal` in many versions. Since `ScytheSkeletonEntity extends Skeleton extends AbstractSkeleton extends Monster`, we cannot use vanilla `FollowOwnerGoal`. Port the body into `ScytheSkeletonFollowOwnerGoal`: scan owner position each tick, navigate if distance > min, teleport if distance > maxTeleport. ~50 lines of straightforward code; defer specifics to implementation.
6. **`HumanoidMobRenderer` generic signature in 26.x** — same risk as other apex bosses. Mirror existing usage exactly (`HerobrineRenderer`, `AgentRenderer`).
7. **`EntitySpawnReason` / `MobSpawnType`** — same renamed-type risk. Mirror `AgentEntity.finalizeSpawn` signature.
8. **`SoundEvents.X` Holder vs direct** — same `.value()` risk. Mirror existing usage.
9. **Soul Lift launch and server-to-client velocity sync** — `((ServerPlayer) target).hurtMarked = true` is the canonical idiom but the field may be `connection.send(new ClientboundSetEntityMotionPacket(...))` in 26.x. Verify against the active version. Alternatively, vanilla `Player.knockback` already handles the network sync; we could repurpose `target.knockback(strength, 0, 0)` with vertical adjustment — but that's not what we want here (knockback is horizontal-biased). Stick with direct deltaMovement + hurtMarked, falling back to explicit motion packet if needed.
10. **`ServerBossEvent` show-on-startSeenByPlayer pattern** — verify existing apex bosses use `startSeenByPlayer` / `stopSeenByPlayer` overrides, not a tick loop. (Null and Herobrine both do — confirmed in their entity classes.)
11. **Mining-goal block-break + drops capture race** — between `destroyBlock(...)` with `drop = false` and our manual `Block.getDrops` call, the block state has already been replaced with air. We must call `Block.getDrops(originalState, ...)` BEFORE `destroyBlock`. Capture state first, then break, then iterate drops.
12. **Sun damage on bone servants** — `Skeleton.aiStep` (inherited from `AbstractSkeleton` → `Monster` → `Mob`) includes the daylight-burn logic. Overriding `aiStep` to call `super.aiStep()` then `setRemainingFireTicks(0)` clears any fire applied this tick before it renders, so visually they appear sun-immune. The exact "clear fire" API name (`setSecondsOnFire`, `setRemainingFireTicks`, etc.) shuffles between MC versions — mirror whatever existing fire-immune handling exists in `HerobrineEntity` (he's also fire-immune).
13. **Block-mining goal interaction with vanilla "GameRules.RULE_MOBGRIEFING"** — if mobGriefing is false, our `level.destroyBlock(...)` call still works (we are calling level methods directly, not relying on the mob's griefing). Verify behavior matches expectation. Document: bone servants mine regardless of mobGriefing gamerule. If users want to disable, they can drop their scythe.
14. **`Rarity.EPIC` import / availability** — `Rarity` is in `net.minecraft.world.item`. Existing `MeteorStaffItem` uses `Rarity.EPIC` — confirmed in module.
15. **Cooldown packet timing for `Player.getCooldowns`** — vanilla cooldown applies to ALL stacks of the same `Item` instance. The scythe is stack-size 1 so this doesn't matter; documented for completeness.

## Self-review

| Concern | Section |
|---|---|
| Entity stats, hitbox, attributes | Entity |
| Singleton storage + lifecycle (uses existing infra) | Singleton mechanic |
| Natural spawn (overworld only) | Spawning |
| Spawn egg (singleton-aware, dimension-gated) | Spawning |
| Combat AI priority list | Combat / AI goals |
| Scythe melee (attribute-based damage) | Combat / Scythe melee |
| Raise Dead (2–3 skeletons, Sharpness 3, dirt particle burst, 10s cooldown, cleanup on death) | Combat / Raise Dead |
| Soul Lift (0.5s telegraph, dY=1.4 → ~6.5 block peak → 3–4 fall damage, 6s cooldown) | Combat / Soul Lift |
| Equipment (scythe held in main hand for renderer; no armor) | Equipment |
| Visuals (texture, renderer; bbmodel deferred) | Visuals |
| Boss bar (YELLOW NOTCHED_10) | Boss bar |
| Drops + XP | Loot |
| Reaper Scythe item (dual-purpose: melee attributes + right-click summon, 5s cooldown, cap 3) | Reaper Scythe item |
| Scythe Skeleton entity (custom subclass of vanilla Skeleton, owner UUID, persistent) | Scythe Skeleton minion entity |
| Scythe Skeleton mining goal (precious ores only, 3-block radius, owner inventory or feet) | Scythe Skeleton mining goal |
| Scythe Skeleton renderer (reuse vanilla) | Scythe Skeleton renderer |
| Sounds | Sounds |
| Lang strings (11 entries) | Lang strings |
| Out of scope (incl. edge cases) | Out of scope |
| All new + modified files | File-level changes |
| Test plan (unit + manual, save-load smoke test) | Testing strategy |
| Known risks + version-drift mitigations | Risks / open questions |

Mechanically distinct from the four prior apex bosses: kit (minion master + CC), HP curve (100, the lowest by far), tempo (slow walk + minion pressure rather than direct burst), defensive (lets adds soak damage rather than dodging/teleporting), boss bar (YELLOW NOTCHED_10), spawn dims (overworld only, like Herobrine). Singleton sits cleanly on top of the existing `BossSingletonSavedData` base from the Null refactor.

Item design: one signature drop, dual-purpose (melee + active cast). Avoids splitting power across two items (vs. e.g. Meteor Staff + Sword), keeping the kill reward iconic and the player's hotbar uncluttered. The active right-click summons player-owned minions that are utility-focused (mine precious ores) and combat-secondary (fight any hostile incl. the reaper himself, but with un-enchanted swords vs. reaper's Sharpness 3 — they're weaker than the boss's minions, by design).

No placeholders. No `[TBD]`. Math values (HP 100, speed 0.28, scythe damage 6, scythe right-click cooldown 100t, Raise Dead cooldown 200t, Soul Lift dY 1.4 / cooldown 120t, minion cap 3, mining radius 3, idle window 60t, mining time 20t, Y-bounds ±8) are explicit and consistent across sections.
