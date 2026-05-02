# Wild-West Mod — Zombie Virus — Design

**Date:** 2026-05-02
**Status:** Draft (pending user review of this doc)
**Series:** Stand-alone feature on top of the merged Wild-West phases 1–3. No phase number; this is a single-spec feature.

## Goal

Add a zombie virus to the Wild-West world that can infect **any** `LivingEntity` (vanilla mobs, modded mobs, wildwest mobs, the player). Infected entities pass through a delayed-onset stage, then become "zombified" — same entity type and model as before, but tinted green, surrounded by ambient particles, faster, and hostile to anything not infected. On melee hit, zombified entities have a 10% chance to apply vanilla `Wither I (5s)` to their target in addition to the bite.

Crucially, zombified mobs **retain their original entity type** — a zombified cow is still a cow, not a zombie. We achieve this by encoding the infection state entirely in two vanilla `MobEffect` instances rather than swapping or subclassing entities.

## Platform

Java 25 / NeoForge 26.1.2.30-beta / Minecraft 26.1.2. Same module (`:wildwest`) as phases 1–3.

## In scope

**Effects (vanilla `MobEffect`, registered in `:wildwest`)**

1. **`festering_wound`** — applied on bite or vial impact. Duration 60 s. Visible in player's status HUD with brown/sickly-green icon. Emits brown drip particles. Curable on any `LivingEntity` by:
   - Player drinks milk (vanilla — clears all effects, free).
   - Throw / use golden apple on infected mob (instant clear). Vanilla zombie-villager pattern.
   - Otherwise: when 60 s expires, the effect removes itself and applies `zombified` in its place.
2. **`zombified`** — infinite duration. While present:
   - Movement-speed attribute modifier `+0.30` (multiplicative). Cleared when effect is removed.
   - Server tick: every 10 ticks, broadcast a green ambient particle around the entity.
   - Client render: green color multiplier (R 0.4, G 1.0, B 0.4) applied to the entity model.
   - AI override (see "AI" below) makes the entity hostile to nearest non-zombified `LivingEntity` within 16 blocks.
   - Cure: golden apple held by a player and right-clicked on the infected mob starts a 30 s "shaking" timer, tracked via a third `MobEffect` named `curing_shake` (duration 600 ticks, hidden particles). When `curing_shake` expires while the entity has `zombified`, both effects are removed (and the speed modifier is cleared). If the entity takes damage during the shake, `curing_shake` is cleared and the cure attempt fails — player must use another golden apple.

**Bite / spread mechanic**

- `LivingDamageEvent` listener: when `event.getSource().getEntity()` has the `zombified` effect AND the target is a non-immune `LivingEntity`:
  - If target has neither `festering_wound` nor `zombified`: apply `festering_wound` (60 s).
  - If target already has `festering_wound`: refresh its duration to a full 60 s (extending exposure).
  - If target already has `zombified`: skip the festering apply (already turned).
  - 10 % roll, independent of festering state: also apply vanilla `MobEffects.WITHER` (level I, 5 s) to the target. Wither stacks freshly on each successful roll.
- Immune set (skip ALL bite logic — no festering, no wither):
  - Tagged `minecraft:undead` (zombies, skeletons, etc.).
  - Bosses (`tag minecraft:bosses` / `EnderDragon` / `WitherBoss`).
  - Players in creative or spectator mode.
  - The Walker carrier mob (already a permanent carrier — bites passing through it would be a no-op but skip cleanly).
  - Any entity that is not a `Mob` and not a `Player` (e.g., `ArmorStand`).

**AI override (universal)**

The challenge: any `LivingEntity` can be zombified, but only `Mob` subclasses have goal selectors. We attach AI conditionally:

- `EntityJoinLevelEvent` listener (server side): for any `Mob` joining the level, register two extra goals at low priority that gate on `hasEffect(ZOMBIFIED)`:
  - `ZombifiedHostileTargetGoal` (priority 0 in `targetSelector`) — finds nearest non-zombified, non-immune `LivingEntity` within 16 blocks; sets it as target. Only active when zombified.
  - `ZombifiedMeleeAttackGoal` (priority 1 in `goalSelector`) — basic 1.0 speed melee approach + attack. Only active when zombified.
- For `LivingEntity` that are NOT `Mob` (e.g., `ArmorStand`): they cannot be zombified — they are not living in the gameplay sense. Add to immune set.
- For the player: zombified player gets no AI changes; the player retains full control. The visual tint, particles, speed buff, and bite-with-wither still apply when the player melee-hits things.

**Carrier mob — `WalkerEntity`**

A new mob type in `:wildwest`, the natural-spawning vector for the virus.

- Class: `WalkerEntity extends Monster` (NOT `WildWestMob` — we don't want the weapon-mode / faction AI from `WildWestMob`; the Walker is a generic shambler that uses the `ZombifiedHostileTargetGoal` + `ZombifiedMeleeAttackGoal` like any other zombified entity).
- Goals: standard `FloatGoal`, `LookAtPlayerGoal`, `RandomLookAroundGoal`, `WaterAvoidingRandomStrollGoal` — the zombified hostile + melee goals are added universally by the `EntityJoinLevelEvent` listener and become active because the Walker has the `zombified` effect from spawn.
- Constructor: applies `zombified` (infinite duration, ambient = true so no HUD spam) once on first tick.
- Visual: cowboy texture with green tint baked in (the same shader as zombified gives a slightly stronger tint when the entity is a Walker — single-channel hint, optional polish; default behavior is just the standard zombified tint).
- Spawn: registered in the **Frontier preset** (extending the existing wildwest spawn registration) with `MobCategory.MONSTER`, spawn weight 1 (rare), min-group-size 1, max-group-size 2. Same biome filter as bandits (plains / desert / savanna).
- Loot: drops 0–1 `tainted_vial` (uncrafted), 0–2 rotten flesh.
- Spawn egg: `walker_spawn_egg` following the flat-PNG icon pattern established for the other mobs.

**Item — `tainted_vial`**

A throwable splash-style item, the player-controlled vector.

- Registration: `Item` with `useDuration` of 0; `releaseUsing` throws a custom `TaintedVialEntity` (extending `ThrowableProjectile`) in the look direction with similar physics to a splash potion.
- On entity impact OR block impact:
  - Spawn glass-break particles + sound.
  - For each `LivingEntity` within 3.0 block radius (sphere), apply `festering_wound` (60 s). Skips entities in the immune set above.
- Stack size: 16. Recipe (shapeless): 1 rotten flesh + 1 glass bottle + 1 gunpowder → 1 tainted vial.
- Creative tab: same wildwest tab as the other items.

**Visual — client rendering**

- `RenderLivingEvent.Pre<LivingEntity, EntityModel<?>>` listener (client only): if the entity has `zombified`, push a `Matrix4f` color modifier on the pose stack — actually NeoForge's correct mechanism is to set a tint via the `RenderType` or to override `getRenderType` is not feasible cross-mod. Instead, use `LivingEntityRenderer`'s `setColorModifier` analogue: NeoForge exposes `RenderLivingEvent` which lets us set a tint via `poseStack` color in the pre-event and reset in the post-event. Concrete: set `RenderSystem.setShaderColor(0.4f, 1.0f, 0.4f, 1.0f)` in pre, restore in post.
- Server-side particles: in `ZombifiedEffect.applyEffectTick(LivingEntity, int amplifier)` (called every tick by the engine), check `entity.tickCount % 10 == 0` — if true, call `ServerLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y+0.5, z, 1, 0.3, 0.3, 0.3, 0.0)`. (Reusing vanilla `HAPPY_VILLAGER` particles re-tinted via shader is non-trivial; for v1 we ship `HAPPY_VILLAGER` as-is — they're green sparkles, which reads as "infected". A custom particle type can replace it later if needed.)

**Save / load**

No additional persistence code required — `MobEffect` instances are written by vanilla into the entity's NBT automatically. The infection state survives world reload, chunk unload, and dimension changes.

## Out of scope

- Custom particle type (use vanilla `HAPPY_VILLAGER` for v1 — green sparkles read as "infected" well enough).
- HUD / overlay for "infection level" beyond the vanilla effect-icon in the status bar.
- Custom shambling walk animation. Zombified mobs use their original animation set + the green tint.
- Cure altar / structure / ritual. Milk + golden apple are the cures.
- Night-only or time-gated spread. Spread runs whenever bites land.
- Curing bosses, the Walker, or other immune entities.
- Outbreak / wave system that ramps up infection over time.

## Architecture

### Module layout

All in `:wildwest`. New files:

```
wildwest/src/main/java/com/tweeks/wildwest/
  effect/
    ModEffects.java                     — DeferredRegister for MobEffect (FESTERING_WOUND, ZOMBIFIED, CURING_SHAKE)
    FesteringWoundEffect.java           — 60s, on expiry applies ZOMBIFIED
    ZombifiedEffect.java                — infinite, applies speed modifier on add, removes on remove
    CuringShakeEffect.java              — 30s, on expiry removes ZOMBIFIED if present and clears self
  entity/
    WalkerEntity.java                   — carrier mob extending WildWestMob
    ai/zombified/
      ZombifiedHostileTargetGoal.java   — gated on hasEffect(ZOMBIFIED)
      ZombifiedMeleeAttackGoal.java     — gated on hasEffect(ZOMBIFIED)
    projectile/
      TaintedVialEntity.java            — ThrowableProjectile
  item/
    TaintedVialItem.java                — uses TaintedVialEntity
  client/
    ZombifiedRenderHandler.java         — RenderLivingEvent pre/post for green tint
    model/WalkerModel.java              — humanoid variant for the Walker
    renderer/WalkerRenderer.java        — uses WalkerModel
  ZombieVirusHandler.java               — server-side event listener:
                                            - LivingDamageEvent: bite spread + 10 % wither
                                            - EntityJoinLevelEvent: attach gated zombified goals to all Mob entities
                                            - PlayerInteractEvent.EntityInteract: golden apple → festering cure (instant) OR zombified cure (start curing_shake)
```

Modified files:

```
wildwest/src/main/java/com/tweeks/wildwest/
  Registration.java                     — register tainted_vial item, walker_spawn_egg item
  ModEntities.java                      — register WALKER entity type + attributes + spawn placement
  WildWestMod.java                      — register ModEffects, ZombieVirusHandler event bus subscriptions
  spawning/                             — add Walker spawn entry to Frontier preset

wildwest/src/main/resources/
  assets/wildwest/
    lang/en_us.json                     — names for new effects, item, mob
    items/tainted_vial.json
    items/walker_spawn_egg.json
    models/item/tainted_vial.json
    models/item/walker_spawn_egg.json
    textures/item/tainted_vial.png      — 16x16 vial w/ greenish liquid
    textures/item/walker_spawn_egg.png  — 16x16 flat egg icon (faction palette: dark green + black)
    textures/entity/walker.png
    textures/mob_effect/festering_wound.png  — 18x18 effect icon
    textures/mob_effect/zombified.png        — 18x18 effect icon
    textures/mob_effect/curing_shake.png     — 18x18 effect icon (hidden in HUD via flag, but Minecraft requires the asset)
  data/wildwest/
    recipe/tainted_vial.json            — shapeless recipe
    loot_table/entities/walker.json     — 0-1 tainted vial, 0-2 rotten flesh
```

### Data flow

```
Bite from carrier OR tainted_vial impact
            │
            ▼
   FESTERING_WOUND applied (60 s)
            │
       ┌────┴────┐
       │         │
   60 s expires  Player drinks milk OR golden apple thrown at mob
       │         │
       ▼         ▼
   ZOMBIFIED   FESTERING_WOUND cleared, no further state change
   applied
       │
       ▼
   Speed +30 %, green tint, ambient particles, hostile AI
       │
       ▼
   Mob hits LivingEntity ──▶ FESTERING_WOUND on target (10 % WITHER too)
       │
       ▼
   Death OR golden-apple shake (30 s) ─▶ ZOMBIFIED removed, speed restored
```

### Key design decisions

- **Effect-based state.** `MobEffect` is the single source of truth. AI, rendering, attack hooks, and persistence all read from it. No parallel data attachment, no custom NBT.
- **Universal AI via `EntityJoinLevelEvent`.** All `Mob` entities get the gated goals on join; the goals are no-ops when the effect is absent. This works for vanilla, modded, and our wildwest mobs uniformly.
- **No entity replacement.** A zombified cow is still a `Cow` with `cow.getType() == EntityType.COW`. Tools, loot tables, and `selector @e[type=cow]` continue to match — important for compatibility with other mods and datapacks.
- **Player-zombified is intentional.** A zombified player is still controlled by the player but gets the speed buff, the tint, and bites-cause-festering. Milk cures. This is consistent with the LivingEntity scope and the user's spec.

## Testing

- **Unit (JUnit / GameTest where applicable):**
  - `FesteringWoundEffect` expires → `ZOMBIFIED` is applied to the host entity.
  - `ZombifiedEffect.onAdd` adds the speed attribute modifier; `onRemove` removes it. Verify modifier UUID is unique and not duplicated on re-apply.
  - Bite hook: zombified attacker damages cow → cow has `FESTERING_WOUND`. Run 200 ticks of attacks → wither applied at roughly 10 % rate (chi-square loose bound).
  - Immune set respected (undead skip, bosses skip, Walker skips).

- **Integration / manual:**
  - Spawn Walker via spawn egg, let it bite a sheep, fast-forward 60 s, verify sheep is zombified (effect, speed, hostility, tint).
  - Throw tainted vial at a group of villagers — all in radius get `FESTERING_WOUND`.
  - Drink milk during festering — effect clears.
  - Throw golden apple at zombified pig — 30 s shake, then cured.
  - Player gets infected, zombifies, hits a cow with bare fists — cow gets festering wound. Drink milk, all back to normal.
  - Save and reload world during festering and during zombified — state persists.

## Open questions / risks

- **NeoForge 26.x API for `RenderLivingEvent` color tinting.** The exact pre/post handler signature in NeoForge 26 needs to be verified during implementation; if `setShaderColor` is the wrong vector, fall back to wrapping the entity model render or using a `RenderType` override.
- **Vanilla `MobEffect` SPI changes between MC versions.** This repo isn't strict 1.x — `effect/MobEffect` API in 26.x must be checked at implementation time. Plan should include a smoke task that registers a no-op effect first to validate the API surface before building both effects.
- **Particle visibility for non-tracked players.** Server-broadcast particles must use `ServerLevel.sendParticles` (broadcasts to all tracking players), not `Level.addParticle` (client-side only). Verify in implementation.
