# Wild-West Mod Phase 2 — Mobs + Hand Weapons — Design

**Date:** 2026-04-30
**Status:** Draft (pending user review of this doc)
**Series:** Phase 2 of 3 for the Wild-West mod. Phase 1 (`:wildwest` module + pistol + rifle) is merged at `698f099`.

## Goal

Ship the four wild-west mobs — **deputy**, **sherrif**, **bandit**, **bandit-leader** — with their hand weapons (`billy_club`, `bandit_knife`), faction marker interfaces, weapon-switching AI, natural spawning, and lawman-vs-outlaw hostility. No horses, no leader-follower AI (those defer to phase 3).

## Platform

Java 25 / NeoForge 26.1.2.30-beta / Minecraft 26.1.2. Same target as phase 1.

## Phasing recap

| Phase | Scope | Status |
|---|---|---|
| 1 | `:wildwest` module + pistol + rifle | Merged @ `698f099` |
| **2 (this doc)** | 4 mobs + hand weapons + faction markers + basic AI | Draft |
| 3 | Horse mounts + leader-follower AI polish | Future |

Phase 2 produces no horse/lead-follower behavior; sherrif and bandit-leader differ from deputy/bandit only by weapon (rifle vs pistol) and stat tier in this phase. Phase 3 layers their unique flair on top.

## Scope

### In scope (phase 2)

**Faction API (`wildwest.api`)**
- `Lawman` — empty marker interface; implemented by `SherrifEntity`, `DeputyEntity`.
- `Outlaw` — empty marker interface; implemented by `BanditEntity`, `BanditLeaderEntity`.
- No supertypes from `securitycore`. Used in target-selector predicates only.

**Mob entities (`wildwest.entity`)**
- Abstract base `WildWestMob extends PathfinderMob` — shared weapon-switching state, AI goal stack, attribute hooks.
- `DeputyEntity extends WildWestMob implements Lawman` — pistol + billy_club, 20 HP / 0.30 spd / 2 atk / 24 follow.
- `SherrifEntity extends WildWestMob implements Lawman` — rifle + billy_club, 28 HP / 0.28 spd / 3 atk / 32 follow.
- `BanditEntity extends WildWestMob implements Outlaw` — pistol + bandit_knife, 20 / 0.30 / 2 / 24.
- `BanditLeaderEntity extends WildWestMob implements Outlaw` — rifle + bandit_knife, 28 / 0.28 / 3 / 32.
- All four register `EntityType` via existing `ModEntities` from phase 1.
- Vanilla `Enemy` interface on `BanditEntity`/`BanditLeaderEntity` only (zombie-style aggression to player). Lawmen do not implement `Enemy`.

**AI goals (`wildwest.entity.ai`)**
- `WildWestRangedAttackGoal` — when `weaponMode == RANGED`. Calls `PistolItem.fireFromMob` or `RifleItem.fireFromMob` (statics added to phase-1 items, see "Phase-1 add-backs"). Cooldown = gun's player cooldown × 1.5.
- `WildWestMeleeAttackGoal extends MeleeAttackGoal` — when `weaponMode == MELEE`. The hand weapon's own `hurtEnemy` override applies its on-hit effect.
- `LawmanTargetGoal extends NearestAttackableTargetGoal<LivingEntity>` — predicate `e -> e instanceof Outlaw && e.isAlive()`.
- `OutlawTargetGoal extends NearestAttackableTargetGoal<LivingEntity>` — predicate `e -> (e instanceof Lawman || e instanceof Player) && e.isAlive()`.
- Reused vanilla goals: `FloatGoal`, `LookAtPlayerGoal`, `RandomLookAroundGoal`, `WaterAvoidingRandomStrollGoal`, `HurtByTargetGoal`.

**Weapon-switching AI (lives on `WildWestMob`)**
- `EntityDataAccessor<Byte> WEAPON_MODE` synced to client (renderer reads it).
- Every 5 ticks, `WildWestMob.tick()`:
  - If `target == null` → set `weaponMode = RANGED`, equip gun in MAINHAND, return.
  - Else compute `dist = this.distanceTo(target)`.
  - If `dist <= 4.0` → desired = MELEE; else → RANGED.
  - If `desired != weaponMode` AND `hysteresisLockTicks <= 0` → swap MAINHAND item (`setItemSlot(EquipmentSlot.MAINHAND, ...)`), set `weaponMode = desired`, set `hysteresisLockTicks = 20`.
  - Decrement `hysteresisLockTicks` each tick.
- The 20-tick hysteresis prevents back-and-forth thrashing at range = 4.0 ± epsilon.

**Hand-weapon items (`wildwest.item`)**
- `BillyClubItem extends Item` — 4 attack damage via `ItemAttributeModifiers` (similar to thief's `BlackjackItem`). Override `hurtEnemy(stack, target, attacker)` to apply `MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)` (Slowness II, 2s).
- `BanditKnifeItem extends Item` — 5 attack damage. Override `hurtEnemy` to apply `MobEffectInstance(MobEffects.WITHER, 40, 0)` (Wither I, 2s).
- Stack size 1, durability 200. Repair tag: `ItemTags.WOODEN_TOOL_MATERIALS` for the club; `ItemTags.IRON_TOOL_MATERIALS` for the knife.
- Crafting recipes (datagen): club = wooden vertical L (`. W .` / `. W .` with iron at top); knife = single iron + leather wrap. Concrete patterns in §"Recipes" below.

**Phase-1 add-backs**
- `PistolItem.fireFromMob(LivingEntity shooter, LivingEntity target)` — server-side raycast same as `use()`, but origin = shooter's eye, direction = target's torso minus origin, normalized + Gaussian random offset (~0.05 rad). Tracer packet: `PacketDistributor.sendToPlayersTrackingEntity(shooter, ...)` (NOT `...AndSelf` — phase-1 spec called this out).
- `RifleItem.fireFromMob(...)` — spawn `BulletEntity` from shooter, velocity vector toward target with same Gaussian.
- These edits stay localized: ~30-40 lines added to each item file. No new classes, no architecture change.

**Spawning**
- **Outlaws** = `MobCategory.MONSTER`.
  - `BanditEntity::checkSpawnRules`: must be in plains/savanna/desert biome tag, light level ≤7, surface only.
  - `BanditLeaderEntity::checkSpawnRules`: same biomes + light, weight 1.
  - Spawn weights via `SpawnPlacements.register(...)` + `BiomeModifier` JSON: bandit weight 5 (pack 1-3); leader weight 1 (pack 1).
- **Lawmen** = `MobCategory.CREATURE`.
  - No biome-based spawn placement. Instead, a `LevelTickEvent.Post` periodic check (every 6000 ticks ≈ 5 in-game minutes):
    - For each `ServerLevel`, iterate village structure starts (existing thief-mod precedent in `ThiefMod.GameEvents`).
    - Per village: count Deputy/Sherrif within structure bounds.
    - If Deputy count <2 → 10% chance to spawn a Deputy at a random villager bed POI.
    - If Sherrif count <1 → 5% chance to spawn a Sherrif at a random villager bed POI.
- Spawn eggs for all four registered in phase-1 `WILDWEST_TAB` creative tab.

**Loot tables (datagen)**

| Mob | Drops |
|---|---|
| Deputy | 0-1 `billy_club` @ 25% (1× looting bonus); 0-2 emerald @ 60% |
| Sherrif | 0-1 `billy_club` @ 25%; 1-3 emerald @ 80%; 0-1 iron_ingot @ 30% |
| Bandit | 0-1 `bandit_knife` @ 25%; 0-1 gold_ingot @ 30%; 0-2 string @ 50% |
| BanditLeader | 0-1 `bandit_knife` @ 30%; 1-3 gold_ingot @ 60%; 0-1 emerald @ 30% |

Plus the gun the mob was carrying (50% drop chance, 30%+ remaining durability via `LootItemRandomChanceCondition` + `SetItemDamageFunction`).

**Hostility / retaliation**
- Bandits/leaders implement `Enemy` → vanilla mob-cap + `Enemy` makes them hostile to player by default. `OutlawTargetGoal` extends to also target `Lawman`.
- Lawmen do NOT implement `Enemy`. They start neutral to player. `LawmanTargetGoal` targets `Outlaw` only. `HurtByTargetGoal` retaliates against whoever hit them — including player. Iron-golem-style: after retaliation, the lawman stays hostile to that specific player (`HurtByTargetGoal` itself handles this via the "if hurt by, target the hurter" loop; sheriffs lose interest only if the hurter dies or leaves the world).
- Sheriffs do NOT propagate aggression: hurting one sheriff doesn't make other sheriffs hostile.

**Renderers + bbmodels**
- 4 entity renderers in `wildwest.client`: `DeputyRenderer`, `SherrifRenderer`, `BanditRenderer`, `BanditLeaderRenderer`. Each extends `MobRenderer<T, HumanoidRenderState, HumanoidModel<HumanoidRenderState>>` (or whatever the modern signature in MC 26.1.2 is — phase-1 BulletRenderer hit a similar signature drift; subclass implementer adapts).
- All four reuse vanilla `HumanoidModel` (player-shaped). Add a `HeldItemLayer`-style render layer that draws the synced MAINHAND item at the right hand.
- bbmodel sources in `wildwest/tools/`: `deputy.bbmodel`, `sherrif.bbmodel`, `bandit.bbmodel`, `bandit_leader.bbmodel`. Phase 2 ships hand-written cube JSON exports + placeholder textures (matching phase-1 strategy); user iterates in Blockbench later.
- Hand-weapon item models: 3D cube JSONs at `assets/wildwest/models/item/{billy_club,bandit_knife}.json`. Two more textures.

**Recipes (datagen)**
- `billy_club` (wooden version of a baton):
  ```
  . I .
  . W .
  . W .
  ```
  I = iron ingot (the cap); W = oak plank (the shaft). Yields 1 club.
- `bandit_knife`:
  ```
  . I .
  L . .
  ```
  I = iron ingot (blade); L = leather (handle wrap). Yields 1 knife.

**Localization (en_us, datagen)**
- Entity names: `Deputy`, `Sheriff`, `Bandit`, `Bandit Leader`.
- Spawn-egg names: same with " Spawn Egg" appended.
- Item names: `Billy Club`, `Bandit Knife`.
- Custom death messages (optional in scope; ships in phase 2):
  - `death.attack.wildwest.club` → `"%1$s was clubbed by %2$s"` (when killed by `billy_club`)
  - `death.attack.wildwest.knife` → `"%1$s was knifed by %2$s"`
  - Implemented as new damage types `wildwest:club` and `wildwest:knife` registered alongside the existing `wildwest:gunshot`.

### Out of scope (phase 3)

- Horse mounts for sherrif + bandit-leader.
- Leader-follower AI (sherrif → leads deputies, bandit-leader → leads bandits).
- Group / patrol / pack-spawn logic.
- Real `.ogg` sounds (still using vanilla redirects).
- Polished Blockbench-authored bbmodels (phase 2 ships hand-written cube JSONs).
- Localizations beyond English.
- Bedrock port.

## Design decisions

| # | Decision | Chosen | Rejected |
|---|---|---|---|
| Q1 | Faction marker design | Wildwest-only `Lawman`+`Outlaw` interfaces, no `securitycore` coupling | Extend `SecurityAlly`/`SecurityHostile`; reuse existing markers directly |
| Q2 | Hand weapon scope | 2 items (billy_club + bandit_knife) faction-themed | 4 unique per mob; 1 generic; reuse baton/blackjack |
| Q3 | Spawning + hostility | Frontier preset (natural spawn, neutral sheriffs, hostile bandits) | Spawn-eggs only; aggressive-everywhere outlaws |
| Q4 | Stat balance | Standard preset | Tame; Lethal |

## Module layout (new + modified files)

```
wildwest/
├── src/main/java/com/tweeks/wildwest/
│   ├── api/
│   │   ├── Lawman.java                           NEW
│   │   └── Outlaw.java                           NEW
│   ├── entity/
│   │   ├── WildWestMob.java                      NEW (abstract base)
│   │   ├── WeaponMode.java                       NEW (enum: RANGED, MELEE)
│   │   ├── DeputyEntity.java                     NEW
│   │   ├── SherrifEntity.java                    NEW
│   │   ├── BanditEntity.java                     NEW
│   │   ├── BanditLeaderEntity.java               NEW
│   │   └── ai/
│   │       ├── WildWestRangedAttackGoal.java     NEW
│   │       ├── WildWestMeleeAttackGoal.java      NEW
│   │       ├── LawmanTargetGoal.java             NEW
│   │       └── OutlawTargetGoal.java             NEW
│   ├── item/
│   │   ├── PistolItem.java                       MOD (add fireFromMob)
│   │   ├── RifleItem.java                        MOD (add fireFromMob)
│   │   ├── BillyClubItem.java                    NEW
│   │   └── BanditKnifeItem.java                  NEW
│   ├── client/
│   │   ├── DeputyRenderer.java                   NEW
│   │   ├── SherrifRenderer.java                  NEW
│   │   ├── BanditRenderer.java                   NEW
│   │   ├── BanditLeaderRenderer.java             NEW
│   │   ├── WildWestHeldItemLayer.java            NEW (renders synced MAINHAND)
│   │   └── ClientSetup.java                      MOD (register 4 renderers)
│   ├── data/
│   │   ├── DataGenerators.java                   MOD (wire 3 new providers)
│   │   ├── ModRecipeProvider.java                MOD (add 2 recipes)
│   │   ├── ModLanguageProvider.java              MOD (add new keys)
│   │   ├── ModDamageTypeProvider.java            MOD (add club + knife types)
│   │   ├── ModEntityLootProvider.java            NEW
│   │   ├── ModBiomeModifierProvider.java         NEW (bandit spawn weight)
│   │   └── ModSpawnPlacementHandler.java         NEW (registers SpawnPlacements via @SubscribeEvent on RegisterSpawnPlacementsEvent)
│   ├── ModEntities.java                          MOD (register 4 EntityTypes)
│   ├── Registration.java                         MOD (register 2 items + 4 spawn eggs + add to creative tab)
│   ├── WildWestDamageTypes.java                  MOD (add club + knife ResourceKeys)
│   └── WildWestMod.java                          MOD (registerEntityAttributes for 4 mobs; LevelTickEvent for lawman-village spawn)
├── src/main/resources/assets/wildwest/
│   ├── textures/entity/{deputy,sherrif,bandit,bandit_leader}.png   NEW (placeholders)
│   ├── textures/item/{billy_club,bandit_knife}.png                 NEW (placeholders)
│   ├── models/entity/{deputy,sherrif,bandit,bandit_leader}.json    NEW (cube JSONs)
│   ├── models/item/{billy_club,bandit_knife}.json                  NEW
│   ├── items/{billy_club,bandit_knife,deputy_spawn_egg,sherrif_spawn_egg,bandit_spawn_egg,bandit_leader_spawn_egg}.json  NEW
│   └── (no new sounds — reuses phase-1 + vanilla)
├── src/test/java/com/tweeks/wildwest/
│   ├── FactionPredicateTest.java                 NEW
│   └── WeaponModeTest.java                       NEW
└── tools/
    ├── deputy.bbmodel                            (placeholder; user iterates)
    ├── sherrif.bbmodel                           (placeholder)
    ├── bandit.bbmodel                            (placeholder)
    └── bandit_leader.bbmodel                     (placeholder)
```

## Behavior detail

### Weapon-mode swap

Pure-logic helper extracted to `WeaponMode.choose(double distance, WeaponMode current, int hysteresisRemaining) → WeaponMode` so it's unit-testable without booting Minecraft (matches phase-1 `Hitscan` pattern). Called from `WildWestMob.tick()`:

```
Decision rule:
  desired = (distance <= 4.0) ? MELEE : RANGED
  if desired == current → return current (no swap)
  if hysteresisRemaining > 0 → return current (locked)
  return desired
```

When the helper returns a different mode, the entity:
1. Sets `WEAPON_MODE` synced data to new mode.
2. Calls `setItemSlot(EquipmentSlot.MAINHAND, ...)` with the appropriate gun or hand-weapon `ItemStack`.
3. Sets `hysteresisLockTicks = 20`.

### Lawman-village spawn periodic check

In `WildWestMod.GameEvents` (mirroring the existing `ThiefMod.GameEvents` pattern):

```
@SubscribeEvent
public static void onServerLevelTick(LevelTickEvent.Post event) {
    if (!(event.getLevel() instanceof ServerLevel sl)) return;
    if (++tickCounter < 6000) return;
    tickCounter = 0;

    for each plains-village structure start in sl:
        countDep = entitiesOfClass(DeputyEntity, structureBounds)
        countShf = entitiesOfClass(SherrifEntity, structureBounds)
        if countDep < 2 and rand.nextFloat() < 0.10:
            spawn a Deputy at a random villager bed POI
        if countShf < 1 and rand.nextFloat() < 0.05:
            spawn a Sherrif at a random villager bed POI
}
```

Lawmen spawned this way are persistent (`MobCategory.CREATURE` doesn't despawn at distance) — once a village has its lawmen, they stay.

### Mob `fireFromMob` helpers

`PistolItem.fireFromMob(LivingEntity shooter, LivingEntity target)`:

```
start = shooter.getEyePosition()
look = (target.getEyePosition() − start).normalize()
look = look.add(gaussianOffset(±0.05 rad))
end = start + look × MAX_RANGE

(rest is identical to the use() raycast: ClipContext block hit, entity AABB walk via Hitscan helper, hurtServer on first-hit entity, S2C tracer packet via sendToPlayersTrackingEntity)

No cooldown / durability tracking on the mob — guns are infinite-ammo for mobs.
```

`RifleItem.fireFromMob(LivingEntity shooter, LivingEntity target)`:

```
bullet = new BulletEntity(ModEntities.BULLET.get(), level, shooter)
start = shooter.getEyePosition()
look = (target.getEyePosition() − start).normalize() + gaussianOffset
bullet.setPos(start)
bullet.shoot(look.x, look.y, look.z, BULLET_VELOCITY, 0.0F)
level.addFreshEntity(bullet)
```

## Damage types (additions to phase-1 `wildwest:gunshot`)

- `wildwest:club` — `message_id = "wildwest.club"`, exhaustion 0.1, no special tags. Death message `death.attack.wildwest.club` → `"%1$s was clubbed by %2$s"`. Used by `BillyClubItem.hurtEnemy` via a custom `DamageSource`.
- `wildwest:knife` — `message_id = "wildwest.knife"`, exhaustion 0.1, tag `bypasses_armor=false`. Death message `death.attack.wildwest.knife` → `"%1$s was knifed by %2$s"`.

Tags via `ModDamageTypeTagsProvider`: neither is `is_projectile`. Neither bypasses armor.

## Testing

### Unit tests (`wildwest/src/test/`)

- `FactionPredicateTest`
  - `Lawman.class.isAssignableFrom(DeputyEntity.class)` true, false for BanditEntity
  - `Outlaw.class.isAssignableFrom(BanditEntity.class)` true, false for SherrifEntity
- `WeaponModeTest` — pure logic, no Minecraft mocking
  - `choose(2.0, RANGED, 0) == MELEE` (close, swap allowed)
  - `choose(2.0, RANGED, 5) == RANGED` (close, locked)
  - `choose(8.0, MELEE, 0) == RANGED` (far, swap allowed)
  - `choose(4.0, MELEE, 0) == MELEE` (boundary — exactly 4 stays melee)
  - `choose(4.001, MELEE, 0) == RANGED` (just past boundary)

(No mob-AI tests — those would require booting Minecraft. Manual smoke testing covers AI behavior.)

### Manual smoke test

1. **Spawn-egg basics:** in creative, spawn each of the four mobs. Confirm:
   - Each appears with the right texture (placeholder is fine — just confirm mob is visible and not the missing-model magenta).
   - Each holds the correct mainhand item (deputy → pistol, sherrif → rifle, bandit → pistol, bandit_leader → rifle) when no target is in range.
2. **Bandit vs player:** a bandit pursues the player, fires its pistol at range, swaps to bandit_knife within 4 blocks, melees with Wither I applied. Player takes damage at both ranges.
3. **Sheriff neutrality:** spawn a sheriff next to the player → sheriff ignores. Player attacks sheriff once → sheriff retaliates (returns fire, melees up close). Other sheriffs in the area stay neutral.
4. **Lawman vs outlaw:** spawn a deputy + a bandit 10 blocks apart → they target each other, exchange shots, eventually one dies. Loot drops match the table.
5. **Weapon swap with hysteresis:** create a sheriff → bandit_leader pair at 5 blk distance, watch them swap to melee, then push them apart slightly → they DON'T swap back to ranged immediately (within the 20-tick window).
6. **Sheriff retaliation persistence:** hit a sheriff, walk 50 blocks away, come back → sheriff still hostile to that player. Kill the player (creative respawn) → sheriff stays hostile. Wait for sheriff to die or unload chunks → no longer relevant.
7. **Natural spawn:**
   - Plains/savanna at night (`/time set night`) → bandits spawn. Bandit-leaders rare but appear.
   - Generate a fresh plains village → wait or `/time` advance ~5 game minutes → at least one deputy appears in the village. After 30+ minutes, occasionally a sheriff.
8. **Loot:** kill each mob type, confirm drops within the configured probability range. Run multiple trials to verify the gun drops at ~50%.
9. **Death messages:** kill the player with each weapon (`/give @s wildwest:billy_club` then attack player; same for knife). Chat shows the appropriate killing-blow message.
10. **Build verification:** `./gradlew :wildwest:build` and `./gradlew :wildwest:runServerData :wildwest:runClientData` all succeed; new generated files committed.

## Open questions / risks

1. **`MobRenderer` signature drift.** Phase 1 found that `EntityRenderer<T, S extends EntityRenderState>` redesign requires extending `ArrowRenderer` for the bullet. The mob renderer surface in MC 26.1.2 may similarly require extending a vanilla `HumanoidMobRenderer<T, ?>` or providing a `RenderState` factory. Implementer adapts. Functionality unchanged — vanilla Humanoid model is the visual.
2. **`HeldItemLayer` API.** `securitycore.client.HeldItemLayer` exists but is themed for security mobs and is part of `securitycore` which we're explicitly not coupling to. Implement a fresh `WildWestHeldItemLayer` in wildwest/client; if it ends up identical to `HeldItemLayer`, that's a future refactor (move both to a shared utility). For phase 2: duplicate is fine.
3. **Periodic village-tick performance.** The lawman-village spawn check iterates village structure starts every 5 minutes per ServerLevel. For typical worlds this is <100 villages; the iteration is O(N) and bounded. If profiling shows it as a hot path, throttle to 12000 ticks (10 minutes). Not addressing in v1.
4. **Sheriff retaliation propagation.** Spec says "other sheriffs stay neutral." This relies on `HurtByTargetGoal` NOT calling its sibling's `setTarget` — which is the vanilla default (Iron Golems work this way). If we discover sheriffs ARE propagating, add `alertOthers = false` flag or override `HurtByTargetGoal`. Verify in smoke test #3.
5. **Entity inventory `getItemBySlot(MAINHAND)` while ranged-attacking.** When `weaponMode == RANGED`, the gun is in MAINHAND for animation purposes, but the actual firing call (`PistolItem.fireFromMob`) bypasses the inventory and just runs the static helper. The mainhand ItemStack is purely cosmetic in ranged mode — it has no durability tracking and never gets damaged. Acceptable for v1; revisit if mob disarming is ever a feature.
6. **Bandit-leader rarity.** Spawn weight 1 in plains/savanna/desert at light ≤7 night. This may make leaders extremely rare in practice (vanilla zombies have weight 95). Acceptable starting point — bandit-leader is intended to feel like a mini-boss. If too rare in testing, bump to weight 2-3.
7. **Death-message custom damage types are scope creep.** Spec includes them ("optional in scope; ships in phase 2"). Two additional damage type registrations + lang keys + datagen tag entries. ~30 lines of code total. Worth it for flavor.
