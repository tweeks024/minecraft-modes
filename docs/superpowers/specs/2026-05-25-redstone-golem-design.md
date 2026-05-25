# Wild-West Mod — Redstone Golem — Design

**Date:** 2026-05-25
**Status:** Draft (pending user review of this doc)
**Series:** Stand-alone feature on top of merged Wild-West phases 1–3 + zombie-virus + Steve-Stacker + Herobrine + The Agent + Pirates + Null + Grim Reaper. Sixth apex-tier boss. **Breaks two established patterns:** no natural spawn (player-summoned only) and no global singleton (multiple instances allowed).

## Goal

Add a sixth apex-tier mob to the `:wildwest` mod called **Redstone Golem**: a slow, immovable, tank-archetype boss that the player constructs deliberately (Wither-style summon). Hostile to nearest player on spawn. Drops the **Piston Gauntlet**, an active utility item with dual punch-or-rocket-jump behavior. Combat kit: melee swing, ground-slam shockwave, ranged redstone bombs.

Mechanically he is the "wall-of-meat berserker" archetype, distinct from the prior apex bosses on engagement profile and how he is encountered:

| Axis              | Herobrine                 | The Agent                  | Null                                 | Grim Reaper                          | Redstone Golem                       |
|-------------------|---------------------------|----------------------------|--------------------------------------|--------------------------------------|--------------------------------------|
| Role              | Zone-control bruiser      | Bow/sword keep-away duelist | Environmental-hazard god             | Minion master + CC                   | Tank / berserker                     |
| Encounter         | Natural night spawn       | Natural night spawn        | Natural night spawn                  | Natural night spawn                  | Player-built construction            |
| Singleton         | Yes (global)              | Yes (global)               | Yes (global)                         | Yes (global)                         | **No — multi-instance allowed**      |
| Engagement        | Closes & melees           | Mid-range, repositions     | Drifts inevitably toward player      | Walks in, raises adds                | Walks (slow), bombs at range, slams up close |
| Movement          | Walks (0.35) + teleport   | Walks (0.45) + teleport    | Floats (0.20)                        | Walks (0.28)                         | Walks (0.22), no teleport            |
| Defensive         | None (pure HP)            | Phantom swap (decoy)       | None                                 | Adds soak damage                     | Armor 14 + knockback resist 1.0      |
| HP                | 200                       | 160                        | 240                                  | 100                                  | 280 (highest of apex tier)           |
| Boss bar          | RED, PROGRESS             | PURPLE, PROGRESS           | WHITE, NOTCHED_6                     | YELLOW, NOTCHED_10                   | RED, NOTCHED_10                      |
| Signature drop    | Meteor Staff (active)     | Cursed Tome (active)       | Void Mark (passive death-save)       | Reaper Scythe (melee + summon)       | Piston Gauntlet (active punch + rocket-jump) |

## Platform

Java 25 / NeoForge (`neo_version` from `gradle.properties`). Matches the existing `:wildwest` module. Avoid hard-coded MC version assumptions; mirror APIs already used by `HerobrineEntity` / `AgentEntity` / `NullEntity` / `GrimReaperEntity` (`Identifier.fromNamespaceAndPath`, `DeferredRegister`, etc.). For the construction listener, use `net.neoforged.neoforge.event.entity.EntityJoinLevelEvent`.

## In scope

### Entity

- New entity id: `wildwest:redstone_golem`. Java class `com.tweeks.wildwest.entity.RedstoneGolemEntity extends net.minecraft.world.entity.monster.Monster`. Not a `WildWestMob` subclass — apex tier sits parallel. Not an `IronGolem` subclass (village-AI baggage we don't want; consistent with how all other apex bosses extend `Monster` directly).
- `MobCategory.MONSTER`.
- Hitbox: `1.4 × 2.7` (iron-golem proportions — bigger silhouette than the humanoid bosses).
- `clientTrackingRange`: 10. `updateInterval`: default.
- Knockback resistance attribute: `1.0` (immovable — defining tank trait).
- Follow range: `48.0`.
- Total HP: `280.0` (highest of the six apex bosses; beats Null's 240 — this is the tank slot).
- Attack damage attribute: `10.0` (5 hearts via melee swing).
- Movement speed attribute: `0.22` (slow but inexorable).
- Armor attribute: `14.0` (heavy plate).
- Fire damage handling: **NOT immune** (thematic — TNT-head boss should be hurt by fire; gives players a counter-attack lane via flint & steel / lava / fire aspect).
- Fall damage: immune (override `causeFallDamage(...)` returning false — heavy construct doesn't take falling damage).
- `setPersistenceRequired()` in constructor (doesn't despawn).
- **No flight.** Walks. No `setNoGravity`. Standard gravity + pathfinding.

### No singleton

Unlike the prior five apex bosses, the Redstone Golem is **not** anchored to `BossSingletonSavedData`. There is no global "only one alive" enforcement. Multiple golems may exist concurrently. Rationale: the boss is summoned via deliberate player construction; restricting count would turn the build into a grief-vector ("rival player summoned theirs first so mine fizzles"). Each golem is fully independent.

No `RedstoneGolemSavedData` class is created.

### Spawning — Construction trigger only

**No biome modifier JSON. No `SpawnPlacementTypes` registration. No natural spawn rule.**

The only way the golem appears in the world is by player block placement matching the construction pattern.

#### Pattern

T-shape of redstone blocks, with TNT as the head. Footprint identical to vanilla iron golem (same as `JackOLanternPattern`), but with different block ingredients:

```
       [TNT]
   [R] [R] [R]
       [R]
```

Where `R` = `minecraft:redstone_block` and `TNT` = `minecraft:tnt`.

Match conditions:

- 3 redstone blocks in a horizontal line (the "shoulders"), oriented along the X or Z axis.
- 1 redstone block centered directly below the middle "shoulder" block (the "torso").
- 1 TNT block directly above the middle "shoulder" block (the "head").
- All 5 blocks placed at any time; the **TNT placement** is what triggers the check.

#### Listener

New class `com.tweeks.wildwest.event.RedstoneGolemConstructionHandler`.

**Why `EntityJoinLevelEvent` for `PrimedTnt`, not `BlockEvent.EntityPlaceEvent`:**

A `redstone_block` adjacent to TNT emits power level 15, which primes the TNT immediately via `TntBlock.onPlace` (vanilla behavior — `setOnFire = true`, spawns `PrimedTnt` entity, sets the block to AIR). `BlockEvent.EntityPlaceEvent` fires *after* `onPlace`, so by the time our handler runs the TNT block is already gone and a `PrimedTnt` is in the world. Detecting on TNT placement would race the auto-priming and lose.

Listening for `PrimedTnt` joining the level catches both paths (direct placement on powered redstone, indirect priming via dispenser, etc.) and is robust to the placement-ordering issue.

- Subscribes to `EntityJoinLevelEvent` on the NeoForge event bus. Registered via `@EventBusSubscriber(modid = WildWestMod.MOD_ID)` annotation or programmatic `NeoForge.EVENT_BUS.register(...)` in `WildWestMod` setup.
- On event fire:
  - Filter: `event.getEntity() instanceof PrimedTnt primedTnt` (early return otherwise — keeps the listener cheap).
  - Filter: server-side only (`!event.getLevel().isClientSide()`).
  - Filter: ignore if the `PrimedTnt` already has internal "consumed" flag set (use the persistent NBT tag `wildwest:golem_consumed = true` to mark TNT we've already handled, in case of edge-case re-emissions).
  - Resolve `Level level = (Level) event.getLevel()`. Compute the head position: `BlockPos headPos = primedTnt.blockPosition()` (the position where the original TNT block was — `PrimedTnt` spawns at the exact center of the consumed block, so `blockPosition()` gives us the block coords directly).
  - Check each of 2 axis orientations (X-axis shoulders, Z-axis shoulders). For each axis:
    - Verify `headPos.below()` is a `redstone_block` (the center shoulder).
    - Verify the two flanking shoulder positions are redstone blocks (`headPos.below().relative(axis, +1)` and `headPos.below().relative(axis, -1)`).
    - Verify the torso position (`headPos.below(2)`) is a redstone block.
    - On match:
      - Consume blocks: set the 4 redstone positions to `Blocks.AIR.defaultBlockState()` with flag `Block.UPDATE_ALL`.
      - Discard the `PrimedTnt`: `primedTnt.discard()` (prevents the explosion).
      - Spawn `RedstoneGolemEntity` at `Vec3.atBottomCenterOf(headPos.below(2))` (top of the torso block, golem stands on the torso position).
      - Effects: play `SoundEvents.IRON_GOLEM_REPAIR` + 20× `ParticleTypes.REDSTONE` (dust particle option with red color, scale 1.5) at the construction center.
    - Return early after a successful match (don't double-process).
- Pattern-matcher helper: a private static method `tryMatch(Level, BlockPos headPos, Direction.Axis axis)` returning `boolean` keeps the orientation check small and testable.

**Why a custom listener instead of vanilla `BlockPattern` / `JackOLanternPattern` infra:**

Vanilla iron-golem detection uses `BlockPattern` registered via `IronGolem.checkSpawnObstruction` + static patterns in `WitherBoss` / `IronGolem`. Reusing that machinery would require either:

1. Subclassing `IronGolem` and overriding the pattern (drags in village AI baggage).
2. Registering a new `BlockPattern` in the same global registry vanilla uses (fragile across NeoForge versions; involves reflection-style access).

A custom `EntityJoinLevelEvent` listener with a small pattern-matcher is the standard modding extension point for custom golem-style summons. Local, testable, robust to MC updates, and handles the TNT auto-prime gracefully.

**Trade-off (accepted):** A player who places a single TNT on top of a single redstone block (incomplete pattern) will see normal TNT priming behavior — the listener inspects the pattern and finds no match, so the `PrimedTnt` is left alone to explode normally. This is the intended fallback.

### Combat

#### AI goals (priority order)

In `RedstoneGolemEntity.registerGoals`:

1. `new FloatGoal(this)` — pathfind around water normally (the golem is not waterlogged, just buoyant).
2. `new RedstoneGolemGroundSlamGoal(this)` — see below. Higher priority than ranged so close-range becomes a kill zone.
3. `new RedstoneGolemThrowBombGoal(this)` — see below. Mid-priority — used at range 6–16 when slam not in range.
4. `new MeleeAttackGoal(this, 1.0D, true)` — standard pathfind-and-swing.
5. `new MoveTowardsTargetGoal(this, 0.9D, 32.0F)` — closes distance even outside attack ranges.
6. `new RandomLookAroundGoal(this)`.

Targeting:

1. `new HurtByTargetGoal(this)`.
2. `new NearestAttackableTargetGoal<>(this, Player.class, true)` — always hostile to players.

#### Melee swing

- Damage 10.0 via `Attributes.ATTACK_DAMAGE`. Vanilla `MeleeAttackGoal` cadence (default `attackInterval` ticks).
- Attack reach: vanilla default (~3 blocks).
- No held item — the model's fists are the weapon.

#### Ground Slam — `RedstoneGolemGroundSlamGoal`

Custom goal class `com.tweeks.wildwest.entity.ai.RedstoneGolemGroundSlamGoal extends Goal`.

- **Trigger condition (`canUse`):** has a `LivingEntity` target; cooldown timer ≤ 0; at least one living entity within 5 blocks of the golem (excludes the golem itself); golem not currently swinging.
- **Effect on start:**
  - Wind-up phase, 20 ticks (1.0 s). Play `SoundEvents.TNT_PRIMED` at golem position at wind-up start. Emit a small ring of `ParticleTypes.SMOKE` at the golem's feet every 4 ticks during wind-up.
  - Set a `DataParameter<Boolean> WINDING_UP` for client renderer to read (used by model to raise arms; if not implemented v1, the sound + smoke alone telegraph).
- **At wind-up end:**
  - Re-verify at least one target in range; if not, abort but still apply cooldown (golem is committed to the swing).
  - Damage: iterate all `LivingEntity` within AABB radius 4 of the golem's position (excluding the golem itself). For each, apply `4f`-damage via `golem.damageSources().mobAttack(golem)` (use mob-attack damage source, not a custom one — Section "Damage types" reasoning below).
  - Knockback: each hit entity receives knockback strength 2.5 — `target.knockback(2.5, golem.x - target.x, golem.z - target.z)` plus vertical lift via `target.setDeltaMovement(dx, max(dy, 0.6), dz)`.
  - Visual: spawn `ParticleTypes.EXPLOSION` (1 large particle) at golem's feet + 30× `ParticleTypes.BLOCK` with `Blocks.REDSTONE_BLOCK.defaultBlockState()` particle option scattered in a 4-block radius ring.
  - Sound: `SoundEvents.GENERIC_EXPLODE` at golem position, volume 0.6, no actual `Level.explode` call (no block damage, no entity damage from a real explosion — we control the radius via the AABB iteration above).
- **Cooldown after slam:** 160 ticks (8 s).

#### Bomb Throw — `RedstoneGolemThrowBombGoal`

Custom goal class `com.tweeks.wildwest.entity.ai.RedstoneGolemThrowBombGoal extends Goal`.

- **Trigger condition (`canUse`):** has a `LivingEntity` target; cooldown timer ≤ 0; target distance between 6.0 and 16.0 blocks (squared distance 36–256); golem has line-of-sight to target (`golem.getSensing().hasLineOfSight(target)`).
- **Effect on start:**
  - Wind-up phase, 12 ticks (0.6 s). Plays `SoundEvents.CREEPER_PRIMED` at golem position. Optional `DataParameter<Boolean> THROWING` for client renderer arm-cock animation; v1 can skip.
- **At wind-up end:**
  - Re-verify target still in range and line-of-sight. If not, abort but still apply cooldown.
  - Spawn `RedstoneBombEntity` at `golem.getEyePosition().add(0, -0.2, 0)`.
  - Set owner: `bomb.setOwner(golem)`.
  - Compute ballistic shot: `bomb.shoot(dx, dy + 0.2, dz, 1.4f, 2.0f)` where `(dx, dy, dz)` is the unit vector from golem to target. The `+0.2` adds slight upward arc. The `1.4f` velocity and `2.0f` inaccuracy mirror skeleton-arrow tuning (skeleton uses `1.6f` / `14f` — bombs are lobbed slower and more accurately).
  - Sound: `SoundEvents.CREEPER_PRIMED` at golem at release.
- **Cooldown after throw:** 100 ticks (5 s).

### Redstone Bomb projectile

New class `com.tweeks.wildwest.entity.projectile.RedstoneBombEntity extends ThrowableProjectile`.

- Extends `ThrowableProjectile` (not `ThrowableItemProjectile`, since the bomb is not crafted from an item — it only exists as a projectile, no item form).
- Constants:
  - `EXPLOSION_RADIUS = 3.0f`.
  - `FUSE_TICKS = 100` (5 s max airtime before fuse expires and detonates wherever it is).
- Constructor signatures:
  - `RedstoneBombEntity(EntityType<RedstoneBombEntity>, Level)` — required for registration.
  - `RedstoneBombEntity(Level, LivingEntity owner)` — convenience for `setPos(owner.eye)` + `setOwner(owner)`.
- Override `getGravity()` returning `0.04` (matches `ThrownPotion` arc — slow drop, ballistic feel).
- Override `tick()`: call `super.tick()`; increment internal fuse counter; if fuse ≥ `FUSE_TICKS` and still in flight, call `detonate()`.
- Override `onHit(HitResult)`: call `detonate()`. (Both `BlockHitResult` and `EntityHitResult` paths converge here — keeps the explosion logic in one place.)
- `detonate()` method:
  - Server-side only.
  - Iterate `level.getEntitiesOfClass(LivingEntity.class, new AABB(...).inflate(EXPLOSION_RADIUS))`.
  - For each, compute distance attenuation: `damage = 6.0 * max(0, 1.0 - (distance / EXPLOSION_RADIUS))`.
  - Skip if entity is the bomb's owner (`getOwner()` — prevents the golem from killing itself with its own bomb).
  - Apply damage: `target.hurt(level().damageSources().explosion(this, getOwner()), damage)` — uses vanilla explosion damage type (Section "Damage types" reasoning below).
  - Knockback: each hit entity receives radial knockback strength 1.2 from the bomb's position.
  - Visual: spawn `ParticleTypes.EXPLOSION_EMITTER` at bomb position + 8× `ParticleTypes.EXPLOSION` in a small cluster.
  - Sound: `SoundEvents.GENERIC_EXPLODE` at bomb position, volume 1.0, pitch 1.0.
  - **No block damage.** Do not call `Level.explode(...)`. The reasoning: the golem is player-summoned in player-controlled locations (often a player's base); block-breaking would turn the boss summon into a self-grief mechanic.
  - Call `discard()` on the bomb entity.
- Texture: `wildwest/src/main/resources/assets/wildwest/textures/entity/redstone_bomb.png` (16×16, redstone-block-red cube with a small fuse on top).
- Renderer: new `RedstoneBombRenderer extends EntityRenderer<RedstoneBombEntity>` that renders the texture as a billboard (mirror `BulletRenderer` or `CannonballEntity`'s renderer pattern — choose whichever is closer to a billboarded cube; if neither exists in a directly reusable form, build a small 6-face cube model in `client/model/`).
  - Verify at implementation time by reading `BulletRenderer.java` and `CannonballEntity`'s renderer; reuse the simpler of the two.
- Registration:
  - In `ModEntities`: `EntityType.Builder.<RedstoneBombEntity>of(RedstoneBombEntity::new, MobCategory.MISC).sized(0.4f, 0.4f).clientTrackingRange(4).updateInterval(10)`.
  - In `WildWestClient`: register `RedstoneBombRenderer`.

### Piston Gauntlet item

New file `wildwest/src/main/java/com/tweeks/wildwest/item/PistonGauntletItem.java`.

- `class PistonGauntletItem extends Item`. Properties:
  - `stacksTo(1)`.
  - `rarity(Rarity.RARE)`.
  - `durability(250)` (iron-tier).
- Constants:
  - `COOLDOWN_TICKS = 30` (1.5 s).
  - `RAY_DISTANCE = 4.0` (blocks ahead of player).
  - `HIT_DAMAGE = 4.0f`.
  - `HIT_KNOCKBACK = 2.0`.
  - `SELF_LAUNCH_VELOCITY = 1.5` (backward along look vector).
- Override `use(Level, Player, InteractionHand)`:
  - Server-side: if `player.getCooldowns().isOnCooldown(stack)` → return `InteractionResultHolder.fail(stack)`.
  - Cast forward ray: `Vec3 eye = player.getEyePosition()`, `Vec3 look = player.getLookAngle()`, `Vec3 end = eye.add(look.scale(RAY_DISTANCE))`. Use `level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player))` for block hit; in parallel, scan entities along the ray using `ProjectileUtil.getEntityHitResult(level, player, eye, end, new AABB(eye, end).inflate(0.5), e -> e != player && e.isAlive())`.
  - **Entity hit (closer than block hit):**
    - `target.hurt(WildWestDamageTypes.pistonPunch(level, player), HIT_DAMAGE)`.
    - `target.knockback(HIT_KNOCKBACK, -look.x, -look.z)` (knock entity away from player along look vector).
    - Sound: `SoundEvents.PISTON_EXTEND` at player position.
    - Particle: 8× `ParticleTypes.EXPLOSION` along the ray from eye to target.
  - **No entity hit (block hit or empty space):**
    - Rocket-jump self: `player.setDeltaMovement(player.getDeltaMovement().add(-look.x * SELF_LAUNCH_VELOCITY, -look.y * SELF_LAUNCH_VELOCITY, -look.z * SELF_LAUNCH_VELOCITY))`. Aim down → fly up; aim forward → fly backward.
    - For player targets, also call `((ServerPlayer) player).hurtMarked = true` to force the velocity packet to send (vanilla quirk — server velocity changes to players don't propagate without this flag; matches Grim Reaper Soul Lift treatment).
    - Sound: `SoundEvents.PISTON_EXTEND` at player position.
    - Particle: 8× `ParticleTypes.EXPLOSION` along the ray.
  - Both branches:
    - Apply cooldown: `player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS)`.
    - Damage item: `stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND)` (mirror existing usage in `ReaperScytheItem` / `MeteorStaffItem`).
    - Return `InteractionResultHolder.success(stack)`.
- Override `appendHoverText(...)`:
  - Line 1: translation key `item.wildwest.piston_gauntlet.tooltip.use` → "Press right-click to punch."
  - Line 2: translation key `item.wildwest.piston_gauntlet.tooltip.launch` → "Aim down to rocket-jump."
  - Use 5-arg `appendHoverText(stack, Item.TooltipContext, List<Component>, TooltipFlag)` signature — mirror current item files (NeoForge 26.1.2 API quirk noted in memory).
- Creative tab: added to `wildwest:combat` via the existing creative-tab event handler (mirror how `ReaperScytheItem` registers).

### Damage types

New damage type `wildwest:piston_punch` added to `WildWestDamageTypes`:

- JSON: `wildwest/src/main/resources/data/wildwest/damage_type/piston_punch.json` — `{ "message_id": "wildwest.piston_punch", "exhaustion": 0.1, "scaling": "never" }`.
- Tag membership: add to `data/minecraft/tags/damage_type/bypasses_armor.json`? **No** — shields and armor should both work; piston is kinetic but normal.
- Static accessor in `WildWestDamageTypes`: `public static DamageSource pistonPunch(Level level, Entity attacker)` returning `new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(PISTON_PUNCH_KEY), attacker)`.

The bomb's explosion damage uses vanilla `level.damageSources().explosion(bomb, owner)` — no new damage type needed. The melee swing and ground slam use vanilla `level.damageSources().mobAttack(golem)`.

### Equipment

- No held items. No armor slots. The renderer's textured body provides the visual.

### Visuals

- Texture: `wildwest/src/main/resources/assets/wildwest/textures/entity/redstone_golem.png`. Iron-golem UV layout (~256×128). Body recolored to redstone-block red with subtle dust-glow shading. Head replaced with TNT-block pattern: red base, white horizontal stripes, fuse pixel on top. Hand-pixeled.
- Model: `com.tweeks.wildwest.client.model.RedstoneGolemModel extends EntityModel<RedstoneGolemEntity>`. Mirror `IronGolemModel` part layout (head, body, 2 arms, 2 legs). Mesh definition built via `LayerDefinition.create(MeshDefinition, 256, 128)`. Register a layer location in `WildWestClient`.
- Renderer: `com.tweeks.wildwest.client.RedstoneGolemRenderer extends MobRenderer<RedstoneGolemEntity, RedstoneGolemRenderState, RedstoneGolemModel>`. Mirror the parameter shape used by `SteveStackerRenderer` (also non-humanoid) or by `IronGolemRenderer` for reference patterns. Shadow radius 0.7 (matches iron-golem proportions).
- Optional bbmodel: `wildwest/tools/redstone_golem.bbmodel` for hand-tuned proportions. Deferred to polish; v1 uses the iron-golem-mirroring model definition.

### Boss bar

- Color: `BossEvent.BossBarColor.RED`. Shared color with Herobrine — distinct because: Herobrine uses `PROGRESS` overlay (no notches), Redstone Golem uses `NOTCHED_10` (10 segments × 28 HP each). Visually distinguishable in the HUD.
- Overlay: `BossEvent.BossBarOverlay.NOTCHED_10`.
- Server-side `ServerBossEvent` constructed in entity constructor. Updated each tick from `getHealth() / getMaxHealth()`. Players within tracking distance added via `bossEvent.addPlayer(serverPlayer)` in `startSeenByPlayer` override; removed in `stopSeenByPlayer` override.
- Multiple concurrent golems → multiple boss bars. Vanilla HUD stacks them (player sees one bar per visible golem). Cap is the player's screen real estate, not enforced server-side.

### Loot

`wildwest/src/main/resources/data/wildwest/loot_table/entities/redstone_golem.json`:

- 1× `wildwest:piston_gauntlet` (guaranteed, rolls = 1, single item pool).
- 5–9 `minecraft:redstone` (rolls = 1, uniform 5–9 — partial refund of the 4×9 = 36 dust crafted into 4 redstone blocks).
- 0–2 `minecraft:tnt` (rolls = 1, uniform 0–2 — chance of head salvage).
- XP drop: `100` (via `getBaseExperienceReward` override in `RedstoneGolemEntity` — highest of apex tier, matches highest HP).

No conditional / looting-enhanced drops.

### Sounds

All vanilla — no new sound files for v1.

- Ambient: `SoundEvents.IRON_GOLEM_HURT` (pitch 0.7, every 80–160 ticks via `getAmbientSound` + `getAmbientSoundInterval` overrides).
- Hurt: `SoundEvents.IRON_GOLEM_HURT`.
- Death: `SoundEvents.IRON_GOLEM_DEATH`.
- Step: `SoundEvents.IRON_GOLEM_STEP` (set via `playStepSound` override).
- Ground-slam wind-up: `SoundEvents.TNT_PRIMED`.
- Ground-slam impact: `SoundEvents.GENERIC_EXPLODE` (volume 0.6).
- Bomb wind-up: `SoundEvents.CREEPER_PRIMED`.
- Bomb release: `SoundEvents.CREEPER_PRIMED` (lower pitch, 0.8).
- Bomb detonation: `SoundEvents.GENERIC_EXPLODE`.

### Registration

In `ModEntities`:
- `REDSTONE_GOLEM` — `EntityType.Builder.of(RedstoneGolemEntity::new, MobCategory.MONSTER).sized(1.4f, 2.7f).clientTrackingRange(10)`.
- `REDSTONE_BOMB` — `EntityType.Builder.<RedstoneBombEntity>of(RedstoneBombEntity::new, MobCategory.MISC).sized(0.4f, 0.4f).clientTrackingRange(4).updateInterval(10)`.

In `Registration` (or wherever item registration lives — check `WildWestMod` / `ModItems.java`):
- `PISTON_GAUNTLET` item registered, added to `wildwest:combat` creative tab.

In `Registration.registerEntityAttributes` (or equivalent — mirror Grim Reaper):
- Attribute supplier for `REDSTONE_GOLEM` setting MAX_HEALTH, ATTACK_DAMAGE, MOVEMENT_SPEED, ARMOR, KNOCKBACK_RESISTANCE, FOLLOW_RANGE per the stats table.

In `WildWestClient`:
- Register `RedstoneGolemRenderer` for `REDSTONE_GOLEM`.
- Register `RedstoneBombRenderer` for `REDSTONE_BOMB`.
- Register layer definition for `RedstoneGolemModel`.

In `WildWestMod`:
- Register `RedstoneGolemConstructionHandler` to the NeoForge event bus.

In `WildWestDamageTypes`:
- Register `PISTON_PUNCH` damage type key.

### Translations

Add to `wildwest/src/main/resources/assets/wildwest/lang/en_us.json`:

- `entity.wildwest.redstone_golem` → `"Redstone Golem"`.
- `entity.wildwest.redstone_bomb` → `"Redstone Bomb"`.
- `item.wildwest.piston_gauntlet` → `"Piston Gauntlet"`.
- `item.wildwest.piston_gauntlet.tooltip.use` → `"Press right-click to punch."`
- `item.wildwest.piston_gauntlet.tooltip.launch` → `"Aim down to rocket-jump."`
- `death.attack.wildwest.piston_punch` → `"%1$s was punched into next week by %2$s"`.
- `subtitles.entity.wildwest.redstone_golem.ambient` → `"Redstone Golem rumbles"`.
- `subtitles.entity.wildwest.redstone_golem.hurt` → `"Redstone Golem hurts"`.
- `subtitles.entity.wildwest.redstone_golem.death` → `"Redstone Golem breaks"`.

## Out of scope

- Natural spawn (no biome modifier, no spawn placement rules).
- Singleton enforcement / `BossSingletonSavedData` integration.
- Spawn egg item — apex bosses with eggs have natural spawns to recover from; this one is summoned, the recipe is the recovery.
- Block-breaking explosions (bombs and slam are damage-only).
- Block-pattern detection via vanilla `BlockPattern` infra (using event listener instead).
- Custom sounds — vanilla only.
- bbmodel hand-tuning — deferred to polish.
- Bedrock parity — note in `UNTRANSLATABLE.md` only.
- Looting bonuses on the drop pool.

## Testing strategy

Unit tests under `wildwest/src/test/java/com/tweeks/wildwest/`:

- `RedstoneGolemConstructionHandlerTest` — fake a Level with placed redstone blocks, fire an `EntityJoinLevelEvent` for a `PrimedTnt` at the head position, assert golem spawns + redstone blocks consumed + PrimedTnt discarded. Cover: X-axis match, Z-axis match, non-match (missing torso → PrimedTnt left alone, redstone blocks intact), non-match (PrimedTnt over a single redstone block → no spawn, normal explosion path).
- `RedstoneGolemEntityTest` — smoke test entity construction; verify default attributes match the stats table.
- `RedstoneBombEntityTest` — verify `detonate()` damage falloff (full damage at center, ~0 at radius edge); verify owner exempted from damage; verify no block-state changes after detonation.
- `PistonGauntletItemTest` — verify entity-hit branch (damage applied, knockback set, cooldown started) vs no-hit branch (player launched, cooldown started). Mock the ray-clip results to drive both branches.
- `RedstoneGolemGroundSlamGoalTest` — verify cooldown enforcement, target-in-range gating, AABB damage application.
- `RedstoneGolemThrowBombGoalTest` — verify range gating (6–16), line-of-sight gating, projectile spawn + velocity vector direction.

Manual dev-client smoke (deferred — note in spec, like prior apex bosses):

- Build T+TNT pattern → golem spawns, boss bar appears, blocks consumed.
- Engage golem → take melee, take slam at close range, take bomb at distance.
- Kill golem → loot drops, boss bar disappears.
- Equip Piston Gauntlet → right-click entity launches them; right-click empty aimed down launches self up.
- Build 2 golems in same world → both alive, both bars stack in HUD.

## Open questions / known edge cases

- **Slab/stair placement under T-shape:** the listener checks block types but does not verify block faces. Placing the T over a half-slab or stairs would still match. Acceptable — vanilla iron-golem detection has the same forgiveness.
- **Builder ownership:** since there's no singleton, no SavedData tracks who built what. The golem doesn't know its builder. Drop goes to whoever kills it. Acceptable for v1; could add owner-tracking later if PvP scenarios demand it.
- **Stack overflow risk on Piston Gauntlet self-launch:** the rocket-jump applies a velocity, then on the next tick the player's velocity-changed packet may re-trigger ground checks. Not a real concern (one-shot delta-movement add, no recursion) but worth noting if motion behaves oddly in playtest.
- **Pattern collision with vanilla iron golem:** the iron-golem pattern uses iron blocks + pumpkin/jack-o-lantern. The redstone-golem pattern uses redstone blocks + TNT. No overlap — listeners run independently.

## Build sequence (high-level — full plan in writing-plans output)

1. Entity stub: `RedstoneGolemEntity` extending `Monster`, attribute registration, basic vanilla AI goals (no custom goals yet).
2. Renderer + model + texture wiring so the entity is visible in dev-client.
3. Construction listener + pattern matcher.
4. Ground slam goal.
5. Bomb projectile entity + renderer.
6. Bomb throw goal.
7. Piston Gauntlet item + damage type.
8. Loot table + XP drop.
9. Boss bar wiring.
10. Translations + creative tab registration.
11. Tests for each non-trivial unit.
12. Spec self-review + integration smoke.

## References

- Sibling apex bosses: `GrimReaperEntity`, `NullEntity`, `HerobrineEntity`, `AgentEntity` — all in `wildwest/src/main/java/com/tweeks/wildwest/entity/`.
- Projectile pattern: `MeteorEntity`, `CannonballEntity` — `wildwest/src/main/java/com/tweeks/wildwest/entity/projectile/`.
- Item with cooldown + active right-click: `ReaperScytheItem`, `MeteorStaffItem`.
- Damage type registry: `WildWestDamageTypes`.
- Iron-golem AI / model: vanilla `net.minecraft.world.entity.animal.IronGolem` + `IronGolemModel` (read-only reference).
- NeoForge event API: `net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent`.
