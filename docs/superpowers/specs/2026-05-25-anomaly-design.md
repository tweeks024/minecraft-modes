# Wild-West Mod — Anomaly — Design

**Date:** 2026-05-25
**Status:** Draft (pending user review of this doc)
**Series:** Stand-alone mid-tier mob feature on top of merged Wild-West phases 1–3 + zombie-virus + Steve-Stacker + Herobrine + The Agent + Pirates + Null + Grim Reaper. Not an apex boss — no singleton, no boss bar.

## Goal

Add a mid-tier hostile mob to the `:wildwest` mod called **Anomaly**: a villager-shaped infiltrator that hides in villages and attacks when a player attempts to trade with it. Right-clicking the disguised entity triggers a reveal — its jaw hinges open exposing rows of jagged teeth — and the entity becomes a fast-rushing bite attacker that applies a brief bleed DoT. If it loses its target, it re-disguises and waits to ambush again.

Mechanically it occupies a "betrayal ambusher" niche distinct from the existing roster: it does not patrol or aggro on sight, it does not use guns, and it cannot be safely identified as hostile until the player has already triggered it.

| Axis              | Bandit (low)              | Walker (low)              | Sherrif (mid)              | Anomaly (mid)                          |
|-------------------|---------------------------|---------------------------|----------------------------|----------------------------------------|
| Role              | Frontier hostile          | Undead frontier hostile   | Lawman (passive→retaliate) | Disguised ambusher                     |
| Aggro             | On sight                  | On sight                  | On player attack on town   | On right-click "trade" attempt only    |
| Movement          | Walks (0.30)              | Walks                     | Walks                      | Walks (0.30 hostile, 0.20 disguised)   |
| HP                | 20                        | ~20                       | ~30                        | 40                                     |
| Melee dmg         | 2                         | 3                         | 6                          | 8 + bleed (1 dmg/sec × 4s)             |
| Disguise          | none                      | none                      | none                       | Villager-look + 25% damage resist      |
| Spawning          | OutlawSpawnRules          | OutlawSpawnRules          | LawmanVillageSpawner       | 5% chance per natural village spawn    |
| Drops             | Emeralds + knife          | Bones / rotten flesh      | Emeralds + badge           | 1–3 emeralds + 0–2 anomaly teeth       |

## Platform

Java 25 / NeoForge (`neo_version` from `gradle.properties`). Matches the existing `:wildwest` module. Avoid hard-coded MC version assumptions; mirror APIs already used by `BanditEntity` / `SherrifEntity` / `WalkerEntity` (`Identifier.fromNamespaceAndPath`, `HumanoidMobRenderer`, `DeferredRegister`, biome-modifier JSON, `Monster.checkMonsterSpawnRules`, etc.).

## In scope

### Entity

- New entity id: `wildwest:anomaly`. Java class `com.tweeks.wildwest.entity.AnomalyEntity extends net.minecraft.world.entity.monster.Monster`. Not a `WildWestMob` subclass (those carry gun/melee weapon slots that don't apply here).
- `MobCategory.MONSTER` — counts against monster cap, not creature cap. This is intentional so that during raid-busy nights the Anomaly's density naturally drops without displacing real villager spawns.
- Hitbox: `0.6 × 1.95` (matches vanilla villager and humanoid mobs in this mod).
- `clientTrackingRange`: 10. `updateInterval`: default.
- Knockback resistance: `0.0` (frail; bleed is its threat, not stickiness).
- Follow range: `24.0`.
- Total HP: `40.0`.
- Attack damage attribute: `8.0` (4 hearts per melee, plus bleed).
- Movement speed attribute: `0.30` while revealed; `0.20` while disguised (overridden via attribute modifier; see "Reveal lifecycle" below).
- Armor: `0.0`.
- Fire damage handling: normal (not immune).
- `setPersistenceRequired()` NOT called — natural-spawned Anomalies despawn normally; spawn-egg Anomalies persist (vanilla default).

### Reveal lifecycle (combat-only)

The entity has a synced data param `DATA_REVEALED: Boolean` (default `false`). State transitions:

1. **Disguised (default)** — `revealed == false`. Goals: `RandomLookAround`, `RandomStroll` (no attack goals). Mouth-closed renderer state. Movement speed = 0.20. Damage taken is multiplied by `0.75` (25% resist) via a `LivingIncomingDamageEvent` listener checking `event.getEntity() instanceof AnomalyEntity a && !a.isRevealed()` then `event.setAmount(event.getAmount() * 0.75f)`. (If the closest existing wildwest reference uses an `actuallyHurt` override instead, match that pattern for consistency.)
2. **Right-click reveal trigger** — Override `mobInteract(Player, InteractionHand)`. On server side, if `!revealed`:
   - Set `DATA_REVEALED = true`.
   - Play `entity.anomaly.reveal` screech sound at entity position.
   - Set `setTarget(player)`.
   - Reset re-disguise timer to 0.
   - Return `InteractionResult.SUCCESS` (consume the click; no GUI opens).
3. **Revealed (hostile)** — `revealed == true`. Goals: `MeleeAttackGoal` (speed 1.0, follow target), `NearestAttackableTargetGoal<Player>`. Mouth-open renderer state. Movement speed = 0.30. No damage resist. On successful melee hit, apply `MobEffectInstance(ModEffects.ANOMALY_BLEED, 80, 0)` (4 seconds = 80 ticks).
4. **Re-disguise trigger** — Tick-based timer (`reDisguiseTicks` int field on entity). Increments each server tick when `getTarget() == null` AND `tickCount - lastHurtByMobTimestamp > 200`. Resets to 0 whenever a target is acquired or damage is taken. When `reDisguiseTicks >= 200` (10 seconds), flip `DATA_REVEALED = false`, play closed-mouth chomp sound, restore disguised speed/resist.

The `addAdditionalSaveData` / `readAdditionalSaveData` overrides persist `DATA_REVEALED` only (re-disguise timer resets on load; intentional — chunk reload counts as "lost target").

### Bleed effect

- New class `com.tweeks.wildwest.effect.AnomalyBleedEffect extends MobEffect`. Category `HARMFUL`, color `0x8B0000` (dark red).
- `applyEffectTick(ServerLevel, LivingEntity, int amplifier)`: deal `1.0f` damage of `DamageTypes.GENERIC` (no special damage type — bleed is generic so armor and absorption work normally per Minecraft convention). Return `true`.
- `shouldApplyEffectTickThisTick(int duration, int amplifier)`: return `duration % 20 == 0` (1 dmg per second).
- Registered in `ModEffects.java` as `ANOMALY_BLEED`.

### Spawning — Village interloper

- New class `com.tweeks.wildwest.spawning.AnomalyVillageSpawner`. Static method `onFinalizeSpawn(FinalizeMobSpawnEvent)` (NeoForge 26.1.2 event; if signature differs, lift from `LawmanVillageSpawner` which already implements the village-spawn hook pattern).
- Trigger: when `event.getEntity() instanceof Villager` AND `event.getSpawnReason()` is `NATURAL` (NOT `BREEDING`, `STRUCTURE`, or `SPAWN_EGG` — we don't replace bred or quest-spawned villagers).
- Roll: `event.getLevel().getRandom().nextFloat() < 0.05f` (5%).
- On hit: cancel the villager spawn (`event.setSpawnCancelled(true)` or equivalent for the NeoForge version), then spawn an `AnomalyEntity` at the same position via `EntityType.spawn(...)` with reason `NATURAL`.
- Standard spawn-rules check: `Monster.checkMonsterSpawnRules(...)` registered via `RegisterSpawnPlacementsEvent` as a defensive guard on any non-village spawn paths (spawn egg, command, future hooks). The Anomaly is NOT added to any biome spawn list — village-villager-replacement is the only natural spawn path.

### Items

- **`AnomalyToothItem`** — trophy item. Plain `Item` with no recipe and no use beyond inventory display. Tooltip text in lang file. Registered in `Registration.java` (or wherever items live — match pattern of `BanditKnifeItem`).
- **`AnomalySpawnEggItem`** — spawn egg (matches existing `HerobrineSpawnEggItem` / `GrimReaperSpawnEggItem` pattern).

### Model & rendering

- Source model: new `wildwest/tools/anomaly.bbmodel` (Blockbench). Body matches villager silhouette (robed humanoid, long nose). Head has a hinged lower jaw with three rows of jagged teeth on both upper and lower jaw.
- Exported texture: `wildwest/src/main/resources/assets/wildwest/textures/entity/anomaly.png`.
- Renderer: `com.tweeks.wildwest.client.AnomalyRenderer extends MobRenderer<AnomalyEntity, ...>` — match the pattern used by `WalkerEntity` / `HerobrineEntity` (whichever uses a custom bbmodel-exported model class).
- Model has two animation states keyed off `entity.getEntityData().get(DATA_REVEALED)`:
  - `false` → jaw closed (rotation matches villager rest pose, mouth flat)
  - `true` → jaw rotated open ~70° downward; rows of teeth become visible
- Transition is instant (single-tick snap) — intentional for jump-scare effect.
- Registered in client setup event (`FMLClientSetupEvent` handler or `EntityRenderersEvent.RegisterRenderers`, whichever pattern is in current use — see `WildWestMod` client side).

### Sounds

- New entries in `assets/wildwest/sounds.json`:
  - `entity.anomaly.ambient` → reuses vanilla `entity.villager.ambient` resource path (sound event aliases vanilla so it sounds like a villager when disguised)
  - `entity.anomaly.reveal` → new OGG `assets/wildwest/sounds/entity/anomaly/reveal.ogg` (screech)
  - `entity.anomaly.hurt` → new OGG (distorted villager hurt)
  - `entity.anomaly.death` → new OGG (gurgled villager death)
  - `entity.anomaly.bite` → new OGG (wet crunch)
- Registered in `ModSounds.java` matching existing pattern.
- Entity overrides `getAmbientSound()`, `getHurtSound(...)`, `getDeathSound()`. Ambient sound used in both disguised and revealed states (the "villager mumble" persists into combat for unsettling effect).
- **Subtitles** for all five sounds added to `assets/wildwest/lang/en_us.json`.

### Loot table

- `wildwest/src/main/resources/data/wildwest/loot_tables/entities/anomaly.json`:
  - Pool 1: `minecraft:emerald`, count `UniformGenerator { min: 1, max: 3 }`, rolls 1
  - Pool 2: `wildwest:anomaly_tooth`, count `UniformGenerator { min: 0, max: 2 }`, rolls 1
- Standard XP drop (override `getExperienceReward` → return 5; matches mid-tier).

### Registrations

- `ModEntities.java` — add `ANOMALY` `DeferredHolder<EntityType<?>, EntityType<AnomalyEntity>>` registration with hitbox `0.6 × 1.95`, `MobCategory.MONSTER`, `clientTrackingRange(10)`.
- Attribute registration — add `AnomalyEntity::createAttributes` to the `EntityAttributeCreationEvent` handler (same place `BanditEntity`, `SherrifEntity`, etc. are registered).
- Spawn placement registration — register `Monster::checkMonsterSpawnRules` via `RegisterSpawnPlacementsEvent` (match how `BanditEntity` does it).
- `Registration.java` — add `ANOMALY_TOOTH` item, `ANOMALY_SPAWN_EGG` item.
- `ModEffects.java` — add `ANOMALY_BLEED`.
- `ModSounds.java` — add the five new sound events.
- Event bus subscription — `AnomalyVillageSpawner` registered as event handler on the appropriate bus.

### Lang

- `assets/wildwest/lang/en_us.json` additions:
  - `entity.wildwest.anomaly` → "Anomaly"
  - `item.wildwest.anomaly_tooth` → "Anomaly Tooth"
  - `item.wildwest.anomaly_spawn_egg` → "Anomaly Spawn Egg"
  - `effect.wildwest.anomaly_bleed` → "Bleeding"
  - 5× subtitle entries for sounds

### Bedrock parity

Matches the established multi-mod parity pattern. Files:

- `bedrock-out/wildwest/behavior_pack/entities/anomaly.json` — entity definition with component groups for `disguised` and `revealed` states, transitioned via events. Right-click trigger = `minecraft:interact` component on disguised group whose `on_interact` event transitions to revealed group.
- `bedrock-out/wildwest/resource_pack/entity/anomaly.entity.json` — client entity, references geometry + texture + sounds, animation controller for jaw open/close keyed off entity flag.
- `bedrock-out/wildwest/resource_pack/textures/entity/anomaly.png` — exported from same bbmodel as Java side.
- `bedrock-out/wildwest/resource_pack/models/entity/anomaly.geo.json` — exported geometry.
- `bedrock-out/wildwest/resource_pack/animations/anomaly.animation.json` — jaw open/close, walk, idle.
- `bedrock-out/wildwest/behavior_pack/loot_tables/entities/anomaly.json` — emerald + tooth drops (mirrors Java).
- `bedrock-out/wildwest/behavior_pack/scripts/goals/AnomalyRevealGoal.ts` — script-side reveal/re-disguise lifecycle, paralleling the Java behavior. Follow the convention in `bedrock-out/thief/behavior_pack/scripts/goals/*` (e.g. `BlackjackStrikeGoal.ts`).
- `bedrock-out/wildwest/UNTRANSLATABLE.md` — note any Java-only mechanics that don't have a clean Bedrock equivalent (likely: synced data params translate cleanly via component groups; no UNTRANSLATABLE additions expected, but verify during impl).
- Sound entries in `bedrock-out/wildwest/resource_pack/sounds/sound_definitions.json` mirroring the Java sound IDs.
- Text entries in `bedrock-out/wildwest/resource_pack/texts/en_US.lang`.

### Translator

- Check `translator/src/main/kotlin/com/tweeks/translator/java/goals/VanillaGoalCatalog.kt` — if any new vanilla goal references are used (none expected; `MeleeAttackGoal` + `NearestAttackableTargetGoal` are already cataloged), no change needed. Verify during impl.

## Out of scope

- No crafting recipe for `anomaly_tooth` (trophy only this iteration).
- No latch/grab mechanic (rejected in brainstorming in favor of simple bite rusher).
- No fake merchant GUI (rejected — right-click triggers attack directly).
- No proximity-bait auto-reveal (rejected — right-click is the only reveal trigger).
- No multi-Anomaly group spawns (single replacement per villager spawn roll).
- No structure spawning (no "Anomaly nests" — village interloper only).
- No advancement / achievement trigger for the first encounter (future iteration).
- No special interaction with `wildwest:zombie_virus` or other existing effects.

## Testing

- **Unit smoke test** — `AnomalyEntityConstantsTest` in `wildwest/src/test/...` mirroring the recent `ReaperScytheItemConstantsTest` pattern: assert HP = 40, attack damage = 8, bleed duration = 80 ticks, disguised speed = 0.20, revealed speed = 0.30, re-disguise window = 200 ticks, village-spawn chance = 0.05.
- **Effect smoke test** — `AnomalyBleedEffectTest` confirming `shouldApplyEffectTickThisTick` returns true on tick boundaries `duration % 20 == 0` and false otherwise.
- **Manual dev-client smoke** — deferred per project convention (see prior boss memories). Document in commit message that manual verification is pending.

## Risks & open questions

- **NeoForge 26.1.2 spawn-event API** — `FinalizeMobSpawnEvent` may have moved or been renamed; verify by reading `LawmanVillageSpawner` and any existing event handlers before implementing. If the cancel-and-replace pattern isn't directly supported, fall back to `MobSpawnEvent.PositionCheck` or post-spawn replacement.
- **Synced data param flicker on chunk reload** — confirmed acceptable: `DATA_REVEALED` persists via NBT, re-disguise timer resets. Worst case the entity appears revealed-but-passive briefly until a new target is acquired, which is fine.
- **Damage resist scaling** — `LivingIncomingDamageEvent` vs overriding `actuallyHurt`: pick whichever matches the closest existing reference in this codebase. Both work; consistency wins.
- **Bedrock right-click → reveal** — Bedrock's `minecraft:interact` component supports `on_interact` events; this should map cleanly. The animation controller must respond to a Molang query on a component-group flag rather than a synced data param.
