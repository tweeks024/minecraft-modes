# Pirates — Design Spec

**Date:** 2026-05-16
**Module:** `:wildwest`
**Status:** Approved for planning

## Summary

Add three pirate-themed hostile mobs (Pirate, Skeleton Pirate, Pirate Captain), one new melee weapon (Rapier), one ranged weapon (Flintlock Pistol), one block (Cannon) with its projectile (Cannonball), and two world-generated structures (Pirate Galleon, Captain's Flagship). The Captain is a flagship-anchored mini-boss (one per flagship, no global singleton). All combat mobs can operate adjacent loaded cannons as well as use their personal weapons.

## Goals

- Give the `:wildwest` mod ocean/beach content to complement its existing land-based bandit gameplay.
- Reuse existing mod patterns (boss bar from Herobrine, projectile + AoE from MeteorEntity, AI goal stacks from BanditEntity) rather than inventing new abstractions.
- Keep singleton/SavedData complexity out — the structure rarity is the gating mechanism.

## Non-Goals

- Rideable or controllable ships. Structures are static.
- Ship-vs-ship naval combat.
- Pirate villages, pirate trading, parrot pets.
- Treasure map "X marks the spot" buried-treasure dungeon.
- Server-wide singleton enforcement for the Captain.

---

## 1. Mobs

All three extend `Monster`. All three are placed by structure generation; **no biome `add_spawns` modifier is registered**. Mobs are flagged `setPersistenceRequired()` on structure-spawn so they survive chunk unload.

### 1.1 Pirate (`wildwest:pirate`)

| Attribute | Value |
|---|---|
| Max HP | 20 (10 hearts) |
| Movement speed | 0.30 |
| Attack damage | 5 (via rapier) |
| Knockback resistance | 0.0 |
| Follow range | 32 |

**Equipment (always):** Rapier in mainhand. 10% drop chance.
**Behavior:** `FloatGoal`, `CannonOperateGoal` (priority 1), `HerobrineMeleeGoal`-style melee at ≤4 blocks (priority 2), `WaterAvoidingRandomStrollGoal`, `LookAtPlayerGoal`, `RandomLookAroundGoal`. Targets: hurt-by + nearest player.
**Visual:** Humanoid model, vendored 64×64 skin (Steve base + tricorn hat + striped shirt + eye patch painted in).

### 1.2 Skeleton Pirate (`wildwest:skeleton_pirate`)

Undead variant. Burns in sunlight (vanilla skeleton behavior copied), ignores drowning damage (`canBreatheUnderwater` returns true).

| Attribute | Value |
|---|---|
| Max HP | 20 (10 hearts) |
| Movement speed | 0.30 |
| Attack damage | 4 (rapier base) |
| Knockback resistance | 0.0 |
| Follow range | 40 |

**Equipment (always):** Rapier mainhand (5% drop), Flintlock Pistol offhand (3% drop).
**Behavior:** Adds `SkeletonPirateRangedGoal` (priority 1, ahead of melee): if target distance 6–15 blocks AND line-of-sight, fires pistol shot every 1.5 s. Melee at ≤5 blocks via standard goal. Otherwise same goals as Pirate. Targets: hurt-by + nearest player + nearest villager.
**Visual:** Vanilla `Skeleton` model + custom 64×32 texture (skeleton bones with tricorn hat painted on, tattered shirt).

### 1.3 Pirate Captain (`wildwest:pirate_captain`) — flagship boss

| Attribute | Value |
|---|---|
| Max HP | 120 (60 hearts) |
| Movement speed | 0.32 |
| Attack damage | 8 (rapier base) |
| Knockback resistance | 0.6 |
| Follow range | 48 |

**Equipment (always):** Rapier mainhand (enchanted Sharpness II + Fire Aspect I, 100% drop), Captain's Pistol offhand (100% drop). Uses `populateDefaultEquipmentSlots` with enchant API mirroring `HerobrineEntity:populateDefaultEquipmentSlots`.
**Boss bar:** `ServerBossEvent`, color `PURPLE`, overlay `PROGRESS`, name from translation key. Wired via `aiStep` / `startSeenByPlayer` / `stopSeenByPlayer` exactly like Herobrine.
**Behavior:** Same goal stack as Skeleton Pirate (ranged 6–15 blocks, melee ≤5) plus `CannonOperateGoal` at priority 1. Cannon-fires take precedence over personal-pistol fires. Targets: hurt-by + nearest player + nearest villager.
**Lifecycle:** No SavedData. No singleton enforcement. Killed = gone from this flagship only. Boss bar tracking removed in `remove(RemovalReason)` regardless of cause.
**XP reward:** 50.
**Visual:** Humanoid model, vendored 64×64 skin (red coat, large feathered tricorn, beard, gold trim).

---

## 2. Weapons

### 2.1 Rapier (`wildwest:rapier`)

Iron-tier `SwordItem` subclass, but with **attack speed 2.4** (vs vanilla iron 1.6) and damage 5. Faster jabs, slightly lower per-hit. Standard sword interactions — no bespoke right-click ability.

- **Tier:** Iron equivalent (durability 250, mining level 2).
- **Drop from:** Pirate mobs (loot table) and Pirate (rare loot from galleon chest).
- **Crafting:** Not craftable in v1 — pirate-loot only.

### 2.2 Flintlock Pistol (`wildwest:flintlock_pistol`) — player-fired hitscan

The existing `:wildwest` codebase has a `PistolItem` that uses **hitscan** (via the `Hitscan` helper + `S2CTracerPacket`). The Flintlock Pistol is a `PistolItem` subclass (or constructor-parameterized variant) reusing the hitscan pipeline with pirate-themed values:

- **Damage:** 12 per shot.
- **Mechanic:** Hitscan (same as `PistolItem`). No projectile entity for player-fired shots.
- **Reload / mag size:** Match existing `PistolItem` semantics (do not invent new — the implementer reads `PistolItem.java` and mirrors its `use`/cooldown/reload logic, swapping the damage value and tracer color).
- **Drop from:** Skeleton Pirate (3% chance) and Captain (100% chance — Captain variant has gold-trim model only).

### 2.2a Captain's Pistol (`wildwest:captain_pistol`)

Cosmetic + stat variant of Flintlock Pistol:
- **Damage:** 16 per shot.
- **Model only:** gold trim texture.
- Functionally a separate `Item` registration so the gold-trim model and stat bump are independent of the loot-dropped Flintlock.

### 2.2b Musket Ball (`MusketBallEntity`) — mob-fired projectile

Player pistols are hitscan, but **mob-fired** pistol shots need a visible, dodgeable projectile so the player can play around them (and the AI can have a believable wind-up). Implement `MusketBallEntity`:

- `ThrowableItemProjectile` subclass.
- Default item: `Items.IRON_NUGGET` (renders as small gray ball via `ThrownItemRenderer`).
- Direct hit: 6 damage via `wildwest:cannonball` damage source (reuse the same damage type — both are gunpowder weapons). No AoE.
- Gravity: default `ThrowableItemProjectile` (slight arc visible across ~15-block range).
- Used by `SkeletonPirateRangedGoal` (§3.2) — never spawned by the player's pistol items.

### 2.3 Cannon (block) (`wildwest:cannon`)

Standalone block with `Direction` facing (horizontal) and boolean `loaded` blockstate. Hardness 3.5, mining tool axe-or-pickaxe, drops itself when broken.

- **States:** `facing=north|south|east|west`, `loaded=true|false`.
- **Place:** Facing direction = opposite of player look (so it faces away from the player after placing — like a furnace).
- **Reload:** Right-click empty cannon with main-hand holding gunpowder + offhand or inventory holding iron nugget. Consumes 1 of each, after 3 s of progress (visible item-use tick) sets `loaded=true`. Sound: `FIRECHARGE_USE`.
- **Fire (player):** Right-click loaded cannon → spawns `CannonballEntity` at cannon's front face, velocity = facing vector × 2.0. Sets `loaded=false`. Sound: `GENERIC_EXPLODE` at 1.0 volume + smoke particles.
- **Fire (AI):** See `CannonOperateGoal` in §3.
- **Drop chance from galleon chests:** 25% one cannon per galleon. Drops from flagship guaranteed (1 spare cannon).

### 2.4 Cannonball (`CannonballEntity`)

`ThrowableItemProjectile` subclass mirroring `MeteorEntity` but simpler (no magma replacement, no block side effects). Uses `Items.IRON_NUGGET` as its default item for `ThrownItemRenderer`.

- **Direct hit:** 15 damage via `wildwest:cannonball` damage source, owner attribution preserved.
- **AoE:** 4-block radius, 6 damage to living entities except the directly-hit entity (mirror MeteorEntity exclusion pattern — do NOT rely on `invulnerableTime`).
- **Block effect:** None. No magma. No fire. No block destruction. Pure entity damage + particles.
- **Particles + sound on impact:** `EXPLOSION_EMITTER` (1×), `LARGE_SMOKE` (12×), `GENERIC_EXPLODE` sound.
- **Gravity:** Default `ThrowableItemProjectile` gravity (0.03) — gives a noticeable arc over the ~30-block useful range.

### 2.5 Damage type

`wildwest:cannonball` registered via `WildWestDamageTypes` and `ModDamageTypeProvider`:

- `message_id`: `"wildwest.cannonball"`
- `exhaustion`: `0.1`
- Tagged in `IS_EXPLOSION` (so vanilla explosion-resistance enchants apply).
- NOT tagged in `IS_FIRE`.

---

## 3. AI goals

### 3.1 `CannonOperateGoal` (shared by all three pirate mobs)

| Aspect | Value |
|---|---|
| Cooldown | 80 ticks (4 s) |
| Trigger | Mob is standing within 1 block (8-neighborhood inclusive of diagonals) of a loaded `wildwest:cannon` block AND has a living target AND target distance > 5 blocks AND line-of-sight to target. |
| Action | Sets cannon `loaded=false`, spawns `CannonballEntity` at cannon front face, velocity = direction toward target (unit vector × 2.0). Owner = the mob (for damage attribution). |
| Priority | 1 (highest non-Float) |

The "AI fires" branch shares the spawn logic with player-fired cannons. Extracted to `CannonFireLogic.fire(Level, BlockPos, Vec3 targetPos, Entity owner)` for testability — see Testing.

### 3.2 `SkeletonPirateRangedGoal` (Skeleton Pirate + Captain)

| Aspect | Value |
|---|---|
| Cooldown | 30 ticks (1.5 s) |
| Trigger | Target distance in [6.0, 15.0] AND line-of-sight AND mob NOT already in cannon-fire range. |
| Action | Spawn `MusketBallEntity` (§2.2b) from the mob's eye position with velocity toward target (slight upward correction for ballistic arc — use `shootFromRotation` with `velocity=1.6`, `inaccuracy=1.0` like vanilla skeleton arrows). Spawn-time sound: `SoundEvents.FIRECHARGE_USE`. |

### 3.3 Melee (all three)

Use a thin `RapierMeleeGoal` extending vanilla `MeleeAttackGoal` with engage range ≤4 blocks and disengage at >5 — same pattern as `HerobrineMeleeGoal`. No separate per-mob class needed; instantiate one per mob with mob-specific damage from attributes.

### 3.4 Target selectors (all three)

- `HurtByTargetGoal(this)` priority 1
- `NearestAttackableTargetGoal(this, Player.class, true)` priority 2
- `NearestAttackableTargetGoal(this, Villager.class, true)` priority 3 (Skeleton Pirate + Captain only)

Pirates do not target Walkers, Bandits, Steve Stackers, Herobrine, or Entity 303 — they're rival hostiles. Confirmed via the standard `NearestAttackableTargetGoal(this, Player.class, ...)` only listing the specific classes above.

---

## 4. Structures

Both structures are vanilla NBT templates (`.nbt`) placed via NeoForge's structure system. No bespoke renderer or chunk-generator hook.

### 4.1 Pirate Galleon (`wildwest:galleon`)

| Aspect | Value |
|---|---|
| Bounding box | 18 × 8 × 24 (W × H × L) |
| Biome | `#minecraft:has_structure/shipwreck_beached` (re-use vanilla's beached-ship biome tag) and `#minecraft:is_ocean` |
| Placement spacing | `RandomSpreadStructurePlacement` spacing 24, separation 8 (similar to shipwreck) |
| Floats on water | Yes — bottom 2 layers below sea level, deck above sea level |
| Mob spawns on generate | 2–4 Pirate (random count), 1 Skeleton Pirate, all placed on deck via structure piece processor |
| Cannons | 4 (2 per side), already loaded on world-gen |
| Loot chest | 1 below-deck chest, loot table `wildwest:chests/galleon` |

NBT template path: `data/wildwest/structure/galleon.nbt`. Structure JSON: `data/wildwest/worldgen/structure/galleon.json`. Structure set JSON: `data/wildwest/worldgen/structure_set/galleon.json`.

### 4.2 Captain's Flagship (`wildwest:flagship`)

| Aspect | Value |
|---|---|
| Bounding box | 28 × 12 × 40 |
| Biome | `#minecraft:is_deep_ocean` |
| Placement spacing | `RandomSpreadStructurePlacement` spacing 96, separation 32 (similar to ocean monument) |
| Floats on water | Yes |
| Mob spawns on generate | 1 Captain (captain's quarters), 4–6 Pirate (deck), 2 Skeleton Pirate (rigging) |
| Cannons | 8 (4 per side), already loaded on world-gen |
| Loot chest | 1 captain's-quarters chest, loot table `wildwest:chests/flagship` |

NBT template path: `data/wildwest/structure/flagship.nbt`. Structure JSON: `data/wildwest/worldgen/structure/flagship.json`. Structure set JSON: `data/wildwest/worldgen/structure_set/flagship.json`.

### 4.3 NBT authoring

Structure NBTs are authored manually in a dev world using structure blocks, then exported to the resource path. The plan phase will include a step that documents the in-game build steps OR includes the pre-authored `.nbt` blobs in the patch. **Recommendation for the plan:** include the dev-world authoring procedure as a documented step rather than committing pre-baked NBTs blind; the author can run it once and commit the binary.

---

## 5. Loot tables

All paths under `wildwest/src/main/resources/data/wildwest/loot_table/`.

### 5.1 Mob loot

`entities/pirate.json`:
- Pool 1, roll 1: 0–2 emeralds (vanilla `set_count` 0–2)
- Pool 2, roll 1: 5% chance Rapier
- Pool 3, roll 1: 1% chance vanilla `LEATHER_HELMET` named "Pirate Hat" (uses `set_custom_data` for name component)

`entities/skeleton_pirate.json`:
- Pool 1, roll 1: 0–2 bones (vanilla skeleton pattern)
- Pool 2, roll 1: 0–2 gunpowder
- Pool 3, roll 1: 5% rapier
- Pool 4, roll 1: 3% flintlock pistol

`entities/pirate_captain.json`:
- Pool 1, roll 1: 1× gold block
- Pool 2, roll 1: 1× captain's pistol (always)
- Pool 3, roll 1: 1× rapier (always — the enchanted captain rapier is dropped via equipment slot at 100% chance, NOT via loot table, to preserve enchantments. This pool drops an *extra* plain rapier.)
- Pool 4, roll 1: 0–3 emeralds
- Pool 5, roll 1: 1× vanilla buried-treasure map (use vanilla item, no custom)

The captain's rapier (enchanted) drops via `setDropChance(MAINHAND, 1.0f)` like the equipment-slot pattern in `HerobrineEntity`. The captain's pistol drops via `setDropChance(OFFHAND, 1.0f)`. The loot table only adds the bonuses.

### 5.2 Chest loot

`chests/galleon.json`:
- 1 pool, 3 rolls, entries: emeralds (weight 10, 1–4), gold ingots (weight 8, 1–3), gunpowder (weight 8, 2–5), strings (weight 10, 2–6), 5% rapier (weight 1)

`chests/flagship.json`:
- 1 pool, 4 rolls, entries: diamond block (weight 1, 1), gold blocks (weight 4, 1–2), gunpowder (weight 8, 4–8), 1% enchanted golden apple (weight 1)

---

## 6. Visuals

### 6.1 Textures

Path: `wildwest/src/main/resources/assets/wildwest/textures/`

| File | Dimensions | Notes |
|---|---|---|
| `entity/pirate.png` | 64×64 | Steve base + tricorn + eye patch + striped shirt |
| `entity/skeleton_pirate.png` | 64×32 | Vanilla skeleton UV + tricorn hat + tattered shirt |
| `entity/pirate_captain.png` | 64×64 | Steve base + large feathered tricorn + red coat + beard |
| `item/rapier.png` | 16×16 | Thin sword, cup guard |
| `item/flintlock_pistol.png` | 16×16 | Brown wooden grip, iron barrel |
| `item/captain_pistol.png` | 16×16 | Same shape, gold trim |
| `block/cannon_side.png` | 16×16 | Wooden carriage side |
| `block/cannon_top.png` | 16×16 | Top of carriage with iron strap |
| `block/cannon_front.png` | 16×16 | Iron tube end (animated frame variant for `loaded=true` showing fuse glow) |
| `block/cannon_back.png` | 16×16 | Iron tube butt |
| `entity/cannonball.png` | 16×16 | Iron sphere (optional — falls back to iron nugget icon if omitted) |

### 6.2 Models

- **Cannon block model:** `models/block/cannon.json` (untextured base) + `cannon_loaded.json` (variant). Block-state JSON `blockstates/cannon.json` maps `facing` + `loaded` to model variants. Use `minecraft:block/orientable` parent for facing math.
- **Block item model:** `models/item/cannon.json` parents `block/cannon`.
- **Rapier item model:** `models/item/rapier.json` parents `item/handheld`, layer0 = `wildwest:item/rapier`.
- **Flintlock + Captain pistol models:** both parent `item/handheld`, layer0 their respective texture.
- **Pirate Captain emissive eyes:** none. The captain is human, not undead.

### 6.3 Renderers

Client side (`com.tweeks.wildwest.client`):

- `PirateRenderer` — `HumanoidMobRenderer` reusing `ModelLayers.PLAYER`, points at `pirate.png`. Mirror `HerobrineRenderer` pattern.
- `SkeletonPirateRenderer` — `SkeletonRenderer` subclass overriding `getTextureLocation`. Reuses vanilla `ModelLayers.SKELETON`.
- `PirateCaptainRenderer` — same shape as `PirateRenderer`, different texture.
- `CannonballRenderer` — `ThrownItemRenderer` reuse, no new class.

Register all in `ClientSetup.registerRenderers` alongside existing entries.

---

## 7. Persistence

None beyond standard entity NBT (vanilla `Mob` serialization). No `SavedData` files. Captain HP / boss-bar progress persist through the mob's own NBT save.

---

## 8. Lang strings

`wildwest/src/main/resources/assets/wildwest/lang/en_us.json` adds (alphabetically sorted in the existing file):

```
"block.wildwest.cannon": "Cannon",
"entity.wildwest.pirate": "Pirate",
"entity.wildwest.pirate_captain": "Pirate Captain",
"entity.wildwest.skeleton_pirate": "Skeleton Pirate",
"item.wildwest.cannon": "Cannon",
"item.wildwest.captain_pistol": "Captain's Pistol",
"item.wildwest.flintlock_pistol": "Flintlock Pistol",
"item.wildwest.pirate_spawn_egg": "Pirate Spawn Egg",
"item.wildwest.pirate_captain_spawn_egg": "Pirate Captain Spawn Egg",
"item.wildwest.rapier": "Rapier",
"item.wildwest.skeleton_pirate_spawn_egg": "Skeleton Pirate Spawn Egg",
"subtitles.wildwest.cannon_fire": "Cannon fires",
"subtitles.wildwest.cannon_reload": "Cannon reloads",
```

---

## 9. Sounds

Reuse vanilla:
- Cannon fire = `SoundEvents.GENERIC_EXPLODE`
- Cannon reload tick = `SoundEvents.FIRECHARGE_USE`
- Pirate hurt = `SoundEvents.PLAYER_HURT`
- Pirate death = `SoundEvents.PLAYER_DEATH`
- Mob-fired musket ball spawn = `SoundEvents.FIRECHARGE_USE`
- Musket ball impact = `SoundEvents.GENERIC_HURT` (entity hit), no sound on block hit
- Skeleton Pirate sounds = vanilla skeleton ambient/hurt/death
- Captain ambient = `SoundEvents.PILLAGER_AMBIENT` (closest "human-leader" feel without a bespoke sound)

No custom `.ogg` files in v1.

---

## 10. Testing strategy

### 10.1 Pure-helper unit tests (JUnit 5)

Mirroring the pattern in `HerobrineStateTest`, `MeteorImpactLogicTest`, `HerobrineTeleportTargetTest`:

- **`CannonStateTest`** — pure record for `(facing, loaded)` state, equality + serialization round-trip (NBT-free, just the data class).
- **`CannonballImpactLogicTest`** — given a list of entities + radius + direct-hit entity, returns the AoE victim list. Pure list/predicate logic, no Minecraft API.
- **`CannonFireGeometryTest`** — given a cannon `BlockPos` + facing + target position, returns `(spawnPos, velocityVector)`. Pure 3D math.
- **`RapierAttributesTest`** — sanity check that the rapier's attribute modifier map has speed 2.4 and damage 5 (instantiate the item via reflection or a test fixture; if too entangled with NeoForge bootstrap, skip and rely on manual test instead).

### 10.2 Manual integration checklist (in-client)

Following the `2026-05-10-herobrine.md` precedent — a single Task at the end of the implementation plan with the checklist:

- [ ] Galleon generates at beach/ocean boundary in a fresh creative world (use `/locate structure wildwest:galleon` to find one).
- [ ] Flagship generates in deep ocean (use `/locate structure wildwest:flagship`).
- [ ] Mobs are present on generated structure (not just empty ships).
- [ ] Captain boss bar appears when within 32 blocks.
- [ ] Player can break + place + reload + fire a cannon.
- [ ] AI fires cannon when player approaches a galleon.
- [ ] Skeleton Pirate fires pistol at range, switches to melee in close.
- [ ] Captain drops gold block + pistol (always) + rapier (always) + map.
- [ ] Loot chests contain expected items.
- [ ] Captain death clears boss bar, no SavedData orphans.
- [ ] World save/reload preserves living captain HP + position.

---

## 11. File-level changes

### New Java files

- `entity/PirateEntity.java`
- `entity/SkeletonPirateEntity.java`
- `entity/PirateCaptainEntity.java`
- `entity/projectile/CannonballEntity.java`
- `entity/projectile/MusketBallEntity.java`
- `entity/ai/RapierMeleeGoal.java`
- `entity/ai/CannonOperateGoal.java`
- `entity/ai/SkeletonPirateRangedGoal.java`
- `entity/projectile/CannonballImpactLogic.java`
- `entity/projectile/CannonFireGeometry.java`
- `block/CannonBlock.java`
- `block/CannonState.java` (pure record)
- `item/RapierItem.java`
- `item/FlintlockPistolItem.java` (subclass of existing `PistolItem`)
- `item/CaptainPistolItem.java` (subclass of `FlintlockPistolItem` with higher damage)
- `client/PirateRenderer.java`
- `client/SkeletonPirateRenderer.java`
- `client/PirateCaptainRenderer.java`
- `spawning/PirateStructurePieces.java` (if structure post-processing requires it for mob placement)

### New test files

- `test/java/.../entity/projectile/CannonballImpactLogicTest.java`
- `test/java/.../entity/projectile/CannonFireGeometryTest.java`
- `test/java/.../block/CannonStateTest.java`
- `test/java/.../item/RapierAttributesTest.java` (best-effort)

### Modified Java files

- `ModEntities.java` (3 mob entity types + cannonball type + musket-ball type)
- `Registration.java` (rapier, flintlock pistol, captain pistol, cannon block + block-item, optional cannonball item)
- `WildWestMod.java` (entity attribute registration for 3 new entities + spawn-placement entries for capture-during-structure-gen if needed)
- `WildWestDamageTypes.java` (cannonball damage type)
- `data/ModDamageTypeProvider.java` + `data/ModDamageTypeTagsProvider.java` (register + tag the cannonball type)
- `client/ClientSetup.java` (5 renderer registrations — 3 mobs + cannonball + musket ball)
- `Registration.java`'s creative-tab `displayItems` block (add new items)

### New resource files

- `assets/wildwest/lang/en_us.json` (modified — strings above)
- `assets/wildwest/textures/entity/pirate.png` (new)
- `assets/wildwest/textures/entity/skeleton_pirate.png` (new)
- `assets/wildwest/textures/entity/pirate_captain.png` (new)
- `assets/wildwest/textures/item/rapier.png` (new)
- `assets/wildwest/textures/item/flintlock_pistol.png` (new)
- `assets/wildwest/textures/item/captain_pistol.png` (new)
- `assets/wildwest/textures/block/cannon_*.png` (4 new files)
- `assets/wildwest/models/item/rapier.json` (new)
- `assets/wildwest/models/item/flintlock_pistol.json` (new)
- `assets/wildwest/models/item/captain_pistol.json` (new)
- `assets/wildwest/models/item/cannon.json` (new, blockitem)
- `assets/wildwest/models/block/cannon.json` (new)
- `assets/wildwest/models/block/cannon_loaded.json` (new)
- `assets/wildwest/blockstates/cannon.json` (new)
- `data/wildwest/loot_table/entities/pirate.json` (new)
- `data/wildwest/loot_table/entities/skeleton_pirate.json` (new)
- `data/wildwest/loot_table/entities/pirate_captain.json` (new)
- `data/wildwest/loot_table/chests/galleon.json` (new)
- `data/wildwest/loot_table/chests/flagship.json` (new)
- `data/wildwest/structure/galleon.nbt` (new — authored in dev world)
- `data/wildwest/structure/flagship.nbt` (new — authored in dev world)
- `data/wildwest/worldgen/structure/galleon.json` (new)
- `data/wildwest/worldgen/structure/flagship.json` (new)
- `data/wildwest/worldgen/structure_set/galleon.json` (new)
- `data/wildwest/worldgen/structure_set/flagship.json` (new)

### Generated outputs (from `./gradlew :wildwest:runData`)

- `generated/serverData/data/wildwest/damage_type/cannonball.json`
- `generated/serverData/data/minecraft/tags/damage_type/is_explosion.json` (updated to include `wildwest:cannonball`)

---

## 12. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Structure floats on water but generates with deck partly submerged on uneven seafloor | Author NBT with explicit "water-line" bottom slice; use `Heightmap.Types.OCEAN_FLOOR_WG` projection in structure JSON to anchor base. |
| AI fires cannon when player is in line of friendly fire from another pirate | Acceptable in v1 — pirates can hit each other. Don't add team-damage suppression. |
| Cannon block can be exploited (place cannon, reload infinitely, AFK farm pirate-aggro) | Cannons require gunpowder + iron nugget per shot, which gates the loop. Accept this. |
| Captain pistol obtained early via locator command + creative-flight expedition | Intended — flagship is supposed to be a reward destination. Accept. |
| Adding pirates spawning via biome modifier would flood beaches with hostile pirates | Deliberately not registering an `add_spawns` biome modifier. Structure-only. |
| `EntitySpawnReason` vs `MobSpawnType` API name drift between NeoForge versions | Plan must mirror whatever `Mob.finalizeSpawn` expects — see existing entity files for the canonical signature in this codebase. |

---

## 13. Out of scope (explicit non-goals)

- Rideable / controllable ships and naval combat
- Pirate villages and trading
- Parrots as pets
- Treasure-map "X marks the spot" buried dungeon
- Server-wide Captain singleton + SavedData
- Custom .ogg sound files (reuse vanilla)
- Cannon as entity (block-only in v1)
- Crafting recipes for rapier, pistols, cannon (loot-only in v1)
