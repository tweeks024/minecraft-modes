# Star Wars Expansion: Han Solo, Princess Leia, and the Landspeeder — Design Spec

**Date:** 2026-07-11
**Module:** `starwars` (existing; this is an expansion, not a new module)
**Approach:** Approach A — characters via the existing named-singleton pattern; landspeeder as a custom `VehicleEntity` subclass with boat-style client-simulated driving and bespoke hover physics.

## 1. Goals

1. Two new Light-faction named characters — **Han Solo** and **Princess Leia** — with the same singleton treatment as Luke/Obi-Wan/Vader/Boba Fett, each with a signature ability, editable bbmodels, and finished textures.
2. A **drivable landspeeder** vehicle: craftable boat-style item, hover-cruiser drive feel, driver + 1 passenger, destructible (drops its item).
3. **Bedrock output** through the translator: Han/Leia via the existing character pipeline; the landspeeder as a genuinely drivable Bedrock entity (ground-driven, hover documented as a deviation); all gaps recorded honestly in `bedrock-out/starwars/UNTRANSLATABLE.md`.

**Non-goals (out of scope):** other vehicles (no X-wing, no Falcon), speeder-mounted weapons, Chewbacca, character dialogue, speeder variants/colors, Bedrock hover parity.

## 2. Global constraints (inherited from the main project)

- NO placeholder art: every texture and model ships as finished pixel art (3+ tones, shading, recognizable silhouette). bbmodel sources are committed and editable.
- Work happens on `main` directly; never stage or modify the user's pre-existing WIP in other modules (`thief`, `securityguard`, `wildwest`, `craftee`, root gradle files) or their `bedrock-out/` trees.
- Version facts come from `gradle.properties` / decompiled sources, never assumed. Exact engine signatures (especially the vehicle ride plumbing) are lifted from decompiled vanilla sources at implementation time — this repo's MC version (26.1.2) has renamed/reshaped APIs (`Identifier`, `ValueInput/ValueOutput`, `EntitySpawnReason`, etc.).
- Bedrock honesty: nothing is silently dropped; every untranslatable behavior gets an `UNTRANSLATABLE.md` entry.
- Unit tests for all pure logic; interactive dev-client verification is deferred to the user's smoke sessions.
- Commits: `feat(starwars): ...` / `feat(translator): ...` style with the Claude Fable co-author trailer.

## 3. Han Solo

### 3.1 Entity

`HanSoloEntity extends SwMob` in `com.tweeks.starwars.entity`, registered in `ModEntities` as `"han_solo"`, `MobCategory.CREATURE`, `.sized(0.6f, 1.95f)`, `.clientTrackingRange(10)` — identical shape to the `LUKE_SKYWALKER` registration.

- `getFaction()` → `SwFaction.LIGHT`
- `usesBlaster()` → `true`; `usesRifleBlaster()` → `false`
- `getWeaponStack()` → the existing blaster pistol item (Han's DL-44 is represented by the existing pistol; no new weapon item)
- Attributes: `MAX_HEALTH = 80.0`, `ATTACK_DAMAGE = 8.0`, `MOVEMENT_SPEED = 0.32`, `FOLLOW_RANGE = 32.0` (same builder shape as `LukeSkywalkerEntity.createAttributes()`)
- Constructor calls `setPersistenceRequired()` (singleton pattern).

### 3.2 Singleton lifecycle (preserve this pattern exactly)

Mirrors `LukeSkywalkerEntity` verbatim:

- `HanSavedData extends NamedCharacterSavedData` with `FILE_ID = "starwars_han"`, `CODEC = buildCodec(HanSavedData::new)`, `SavedDataType` using `DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS`, and `get(MinecraftServer)` via `server.overworld().getDataStorage().computeIfAbsent(TYPE)` — the same five-part shape as `LukeSavedData`.
- `finalizeSpawn` claims the singleton after `super.finalizeSpawn(...)` (which equips the weapon), discarding UUID-mismatched duplicates.
- UUID-guarded `clear()` in **both** `die(DamageSource)` and `remove(RemovalReason)` for `KILLED`/`DISCARDED` only.

### 3.3 Signature ability — "Shoots First" quickdraw

`HanQuickdrawGoal` in `entity.ai`, added at goal priority 1 in `registerGoals()` (above the inherited priority-2 `BlasterAttackGoal`).

Behavior: the **first shot against each newly acquired target** fires with a short windup and double damage; after that one shot, the goal stops and normal `BlasterAttackGoal` pacing (`SwMobConstants.FIRE_INTERVAL_TICKS = 30`) takes over.

State machine (single field — unit-testable as pure logic in a `QuickdrawState` helper class):

- Track `lastAmbushedTargetId : UUID` (nullable).
- `canUse()` = Han `usesBlaster()` && has a living, visible target && target UUID `!= lastAmbushedTargetId`.
- On `start()`: windup counter = `QUICKDRAW_WINDUP_TICKS = 8`.
- On windup expiry: fire via `BlasterPistolItem.fireFromMob(mob, target, 2 * BlasterPistolItem.DAMAGE, color)` (the same static entry point `BlasterAttackGoal.tick()` uses, with doubled damage), record the target UUID into `lastAmbushedTargetId`, and stop.
- The goal sets `Flag.LOOK` (same flag set as `BlasterAttackGoal`), so at priority 1 it cleanly suspends the priority-2 blaster goal during the windup — no normal bolt can fire mid-windup, and `BlasterAttackGoal.start()` resetting its cooldown to 30 ticks means no double-tap on handoff.
- Re-acquiring a *different* target re-arms the quickdraw; re-acquiring the *same* target does not (only one ambush per continuous acquaintance; the single-field memory means switching back and forth re-arms, which is acceptable and keeps the state trivially small).

### 3.4 Spawning, egg, alignment

- Joins `NamedCharacterSpawner` with one added `tryRollCharacter(sl, HanSavedData.get(...), ModEntities.HAN_SOLO.get(), JEDI_BIOMES, JEDI_STRUCTURES, false)` line — same biomes/anchor as Luke/Obi-Wan, no escort. Existing constants (`CHECK_INTERVAL_TICKS = 1200`, `SPAWN_CHANCE = 0.15f`) are untouched.
- Spawn egg registered in `Registration` following the existing egg pattern (plain `DeferredItem<SpawnEggItem>` — no colors at registration in this version). Colors go in the `EGGS` table in `starwars/tools/gen_spawn_eggs.py` as an `(primary, secondary)` RGB tuple: base `0xE8E0D0` (off-white shirt), accents `0x2B2B2B` (black vest).
- Alignment: **no new alignment code.** As a `SwFaction.LIGHT` combatant he automatically inherits the existing deltas (`Alignment.deltaForKill(LIGHT) = -5`, hit = −1) — killing Han pushes the player toward the Empire exactly as killing Luke does.

### 3.5 Art

Editable `han_solo.bbmodel` generated by `tools/gen_bbmodels.py` + committed; texture via `tools/gen_textures.py`. Silhouette requirements: black vest layered over white shirt (vest as a distinct geometry layer or clearly shaded texture band), dark hair, holster detail on the right hip. 3+ tones per material region.

## 4. Princess Leia

### 4.1 Entity

`PrincessLeiaEntity extends SwMob`, registered as `"princess_leia"`, `MobCategory.CREATURE`, `.sized(0.6f, 1.9f)`, `.clientTrackingRange(10)`.

- `getFaction()` → `SwFaction.LIGHT`
- `usesBlaster()` → `true`; `usesRifleBlaster()` → `false`
- `getWeaponStack()` → existing blaster pistol
- Attributes: `MAX_HEALTH = 70.0`, `ATTACK_DAMAGE = 7.0`, `MOVEMENT_SPEED = 0.32`, `FOLLOW_RANGE = 32.0`
- Constructor calls `setPersistenceRequired()`.

### 4.2 Singleton lifecycle

Identical pattern to §3.2 with `LeiaSavedData`, `FILE_ID = "starwars_leia"`.

### 4.3 Signature ability — "Rebel Rally" aura

`LeiaRallyGoal` in `entity.ai`, goal priority 1.

- **Cadence:** at most one pulse per `RALLY_INTERVAL_TICKS = 240` (12 s), tracked by a cooldown counter inside the goal.
- **Trigger (`canUse()`):** cooldown elapsed **and** combat is happening nearby — Leia herself has a target, or at least one eligible ally within radius has an attack target.
- **Pulse effect:** every eligible ally within `RALLY_RADIUS = 12.0` blocks receives **Resistance I + Regeneration I** for `RALLY_DURATION_TICKS = 160` (8 s). Resistance (not Strength) because blaster damage in this codebase is a flat constant passed straight to `hurtServer(...)` — it never reads the `ATTACK_DAMAGE` attribute Strength modifies, so Strength would be a silent no-op for every blaster-wielding ally (Han, Leia herself). Resistance is applied on the *victim* side of the damage pipeline and benefits everyone.
- **Eligible allies:** (a) any `SwCombatant` mob whose `getFaction() == SwFaction.LIGHT` (includes Leia herself), and (b) any player whose alignment score is **strictly positive** (score > 0 — a neutral score of 0 does not qualify). Alignment is read through the existing `AlignmentAttachment` accessor used by `SwTargetGoal`/`AlignmentEvents`.
- **Feedback:** a ring of `ParticleTypes.END_ROD` particles at the rally radius edge (server-side `ServerLevel.sendParticles`, ~24 points around the circle at Leia's Y + 1) and vanilla `SoundEvents.BEACON_ACTIVATE` at 1.4 pitch, 0.8 volume. No new audio assets.
- **Pure logic extraction:** target-eligibility predicate and cooldown arithmetic live in a static-method helper `RallyMath` (package `faction` or `entity.ai`) so unit tests cover faction filtering, the score > 0 boundary, and radius math without engine classes.

### 4.4 Spawning, egg, alignment, art

- One added `tryRollCharacter(...)` line with `LeiaSavedData`, `JEDI_BIOMES`, `JEDI_STRUCTURES`, no escort.
- Spawn egg colors (in the `gen_spawn_eggs.py` `EGGS` table, per §3.4): base `0xF2EEE6` (white robes), accents `0x5A4030` (brown hair).
- Alignment: inherited LIGHT deltas, no new code.
- Art: `princess_leia.bbmodel` — **side hair buns modeled as geometry** (two cubes flanking the head; this is the mandatory silhouette feature), white senatorial robe with belt detail, 3+ tones.

## 5. Landspeeder (X-34)

### 5.1 Entity and registration

`LandspeederEntity` in `com.tweeks.starwars.entity`, extending the vanilla vehicle base class that boats extend in this MC version (`VehicleEntity` as of recent versions — implementer confirms the exact class and its hurt/wobble/drop plumbing from decompiled sources, per repo norm). It is **not** a `Mob`: no AI, no attributes, no pathfinding.

Registered in `ModEntities` as `"landspeeder"`, `MobCategory.MISC`, `.sized(2.0f, 0.8f)`, `.clientTrackingRange(10)`.

Faction AI ignores it: it is not a `SwCombatant`, so `SwTargetGoal` never targets it (players and stray hits can still damage it).

### 5.2 Ride plumbing (boat pattern — lift from decompiled `AbstractBoat`)

- `getControllingPassenger()` returns the first passenger when it is a `Player` (boat behavior).
- **Client-simulated driving:** when `isLocalInstanceAuthoritative()` (the current name of the boat-era `isControlledByLocalInstance` — `Entity.java:3508` in the decompiled 26.1.2 sources), the driving client runs the physics tick and the engine's standard vehicle-move packet sync propagates position — exactly the mechanism that makes vanilla boats feel responsive. Rider input (forward/strafe) is read the way `AbstractBoat` reads its controlling player's input in this version.
- Two seats: driver and passenger seated side-by-side in the cockpit (±0.25 lateral offset from hull center — matches the modeled seat cubes in the bbmodel geometry), implemented consistently across Java, bbmodel, and Bedrock (deviation from the original draft's front/back layout: side-by-side matches the X-34's actual cockpit); `positionRider` offsets derive from the bbmodel geometry. `getMaxPassengers() = 2` (or the version's equivalent hook).
- Interaction: right-click boards (driver seat first, then passenger); dismount via the normal sneak-dismount path.
- Riders take **no fall damage** while seated (the speeder zeroes `fallDistance` for itself and its passengers each tick; note this version uses direct field assignment `entity.fallDistance = 0`).

### 5.3 Hover physics (`HoverPhysics` — pure, unit-testable)

All tuning constants live in `LandspeederEntity`; the math lives in a static pure helper `HoverPhysics` (no engine imports) so tests cover it directly.

- **Ground sensing:** each physics tick, clip straight down from the hull up to `HOVER_SCAN_DEPTH = 3.0` blocks. **Fluid surfaces count as ground** (clip with fluid mode that stops at any fluid surface) — the speeder skims flat over water.
- **Spring:** target height `HOVER_HEIGHT = 0.5` above sensed ground. Vertical acceleration = `SPRING_STIFFNESS = 0.10 * (target − current)` − `SPRING_DAMPING = 0.40 * verticalVelocity`, clamped to ±`MAX_VERTICAL_ACCEL = 0.15`/tick. This glides up and down gentle slopes and floats over 1-block gaps (the scan still finds ground within 3 blocks).
- **Free fall:** when the downward scan finds no ground within 3.0 blocks, gravity `−0.04`/tick applies with terminal velocity `−0.6`/tick. No landing damage (see §5.2).
- **Forward drive:** with forward input, accelerate `FORWARD_ACCEL = 0.06`/tick along the facing, capped at `MAX_SPEED = 0.7` blocks/tick (≈ 1.7× vanilla boat top speed). Reverse input: 40 % of forward accel, capped at 0.2. No input: horizontal velocity × `FRICTION = 0.95` per tick (drifty coast-down).
- **Steering:** left/right input yaws `TURN_RATE = 3.5°`/tick; velocity direction blends toward the new facing at 20 %/tick (slight drift rather than rail-turning).
- **Collision:** solid-wall collisions stop movement via normal entity collision resolution; no crash damage (YAGNI).

### 5.4 Health, destruction, pickup

- `MAX_HULL_HEALTH = 40.0f`, tracked as a synched entity data float (plus the base vehicle class's built-in hurt wobble for feedback).
- Any damage source hurts it (creative-player punch instant-breaks, mirroring boats). No regeneration — hull health only goes down (YAGNI).
- **Hull health persists across chunk reloads:** saved/loaded via the version's `ValueInput`/`ValueOutput` save-data hooks (precedent: `NullRiftEntity` in wildwest). Without this, a reload would silently restore full health, contradicting the no-regeneration rule.
- At ≤ 0: eject all passengers, spawn a burst of `ParticleTypes.LARGE_SMOKE` + `ParticleTypes.CRIT`, play `SoundEvents.IRON_GOLEM_DEATH` (1.3 pitch), drop one landspeeder item (skipped when broken by a creative-mode player, boat-style), discard the entity.
- Hurt feedback sound: `SoundEvents.IRON_GOLEM_HURT` at 1.2 pitch. No new audio assets anywhere in this feature.

### 5.5 Item, recipe, tab

- `LandspeederItem` in `item`: boat-item-style placement — raycast (`ClipContext`) from the player, spawn the entity on the hit ground position with the player's yaw, shrink the stack (not in creative), emit the entity-place game event. `stacksTo(1)`.
- Registered in `Registration` as `"landspeeder"` and added to the existing Star Wars creative tab alongside the other items/eggs.
- **Recipe** (shaped, data-generated with the module's existing recipe provider):
  ```
  i r i
  i i i
  ```
  `i` = `minecraft:iron_ingot` (×5), `r` = `minecraft:redstone_block` (×1) → 1 landspeeder.
- Loot/pick-block: `getPickResult` returns the item.

### 5.6 Rendering and art

- `landspeeder.bbmodel` (committed, editable): X-34 silhouette — long low hull, open two-seat cockpit, curved windshield, **three rear turbine pods** (one center, two flanking), sand-orange body with weathering tones + dark cockpit interior. Texture via the gen-texture pipeline, 3+ tones per region.
- `LandspeederModel` + `LandspeederRenderer` (client): renders the bbmodel-exported geometry; **banking** — roll the model up to ±12° proportional to current yaw rate; **hover bob** — gentle ±0.03-block sinusoidal idle bob (client-visual only, period ~2 s); reuse the hurt-wobble tilt from the vehicle base class.
- 2D item sprite (side profile) via the item-texture pipeline.

## 6. Bedrock translation

- **Han/Leia:** flow through the existing translator entity pipeline (superclass goal walking picks up the `SwMob` goal set). `HanQuickdrawGoal` and `LeiaRallyGoal` are custom goals → like the six existing custom goals in `bedrock-out/starwars/behavior_pack/scripts/goals/`, they emit as cache-miss TODO stubs unless a `:translate --with-llm` run is made; either state is recorded honestly in `UNTRANSLATABLE.md`. (Running `--with-llm` to fill all eight stubs is an optional follow-up, not a gate for this feature.)
- **Singleton honesty fix (pre-existing gap):** `bedrock-out/starwars/UNTRANSLATABLE.md` currently has **no** entry about singleton SavedData uniqueness for the four existing named characters. This expansion adds singleton-uniqueness entries for **all six** named characters (Vader, Luke, Obi-Wan, Boba Fett, Han, Leia) — Bedrock output has no equivalent of the per-server one-alive guarantee.
- **Landspeeder:** the translator gains a minimal **vehicle path**. Verified current behavior: `EntityAnalyzer` does *not* skip a non-`Mob` `VehicleEntity` subclass — it would emit it as a walking mob (walk navigation, static jump, pushable, no seats). The vehicle path must therefore **replace the default component set**, not layer on top of it: emit `minecraft:rideable` (2 seats, driver first) + `minecraft:input_ground_controlled` + `minecraft:movement` + `minecraft:health`, and suppress the walking-mob defaults. Because the landspeeder has no attributes (the usual source for movement/health components), the path reads `MAX_SPEED` and `MAX_HULL_HEALTH` as static constants from the entity class (the constant-folding machinery already exists). Scope guard: this path may key specifically off `LandspeederEntity` rather than generically detecting any vehicle (one known consumer — YAGNI on generalization), but must live in the translator, not as hand-edits to `bedrock-out/`.
- **Recipe:** the translator already emits shaped recipes (`RecipeTransform.kt`, `minecraft:recipe_shaped`; six starwars recipes are in `bedrock-out/starwars/behavior_pack/recipes/` today) — the landspeeder recipe **will** be emitted, firmly in scope.
- **Documented Bedrock deviations:** ground-driven instead of hover; no banking/bob visuals; item-place-to-spawn approximated (Bedrock spawn-egg-like item or scripted item use — whichever the existing item pipeline supports).
- Regenerate `bedrock-out/starwars/` and commit; **never touch** other modules' `bedrock-out/` trees.

## 7. Testing

Unit tests (JUnit, same source set as the existing 48):

- **`HoverPhysicsTest`:** spring converges to hover height from above and below without overshoot beyond clamp; damping prevents oscillation growth; no-ground-within-scan → gravity path; fluid-as-ground behavior is exercised at the predicate level.
- **`QuickdrawStateTest`:** fresh target → ambush available exactly once; same target retained → not available; target switch → re-armed; null-target transitions safe.
- **`RallyMathTest`:** LIGHT mob eligible, EMPIRE/NEUTRAL mob not; player score 1 eligible, 0 and −1 not; radius boundary inclusive at 12.0; cooldown arithmetic.
- **Datagen determinism:** rerunning datagen after adding the recipe/loot/egg entries is byte-identical on second run (existing convention).
- Translator: new/changed translator code gets tests in the translator module's existing suites (vehicle-path emission, Han/Leia goal handling).

Deferred to the user's dev-client smoke session: drive feel, seat offsets, banking/bob visuals, rally particles, quickdraw feel, Bedrock in-game behavior.

## 8. New-file map (Java module)

```
starwars/src/main/java/com/tweeks/starwars/
  entity/HanSoloEntity.java            entity/HanSavedData.java
  entity/PrincessLeiaEntity.java       entity/LeiaSavedData.java
  entity/LandspeederEntity.java
  entity/ai/HanQuickdrawGoal.java      entity/ai/QuickdrawState.java
  entity/ai/LeiaRallyGoal.java         entity/ai/RallyMath.java
  entity/vehicle/HoverPhysics.java     (new vehicle subpackage; pure math, no engine imports)
  item/LandspeederItem.java
  client/model/ + client/renderer/ for all three new entities
Modified: ModEntities, Registration, NamedCharacterSpawner, ClientSetup,
          lang/loot/recipe data providers, asset JSON (no model datagen
          provider exists — models come from the Python tools + hand-authored
          JSON), tools/gen_* scripts.
```

## 9. Open items intentionally deferred

- Pass-2 review Minors from the main project (tracer clamp, Vader pull, saber sweep) are a separate pending decision — not bundled into this expansion.
