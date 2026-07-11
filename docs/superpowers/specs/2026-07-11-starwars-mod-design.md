# Star Wars Mod — Design

**Date:** 2026-07-11
**Status:** Approved design, pending implementation plan
**Module:** `starwars` (new NeoForge mod module, modid `starwars`)
**Targets:** Java (NeoForge, versions from root `gradle.properties`) + Bedrock via translator, from day one

## Goal

A comprehensive Star Wars mod covering four pillars: characters/mobs, weapons & gear,
Force powers, and worldgen structures. Every character ships with a committed, editable
`.bbmodel` source in `starwars/tools/` and finished textures — **no placeholder art
anywhere** (per repo policy). Built as a single ordered implementation plan (user chose
one mega-plan over phased vertical slices).

Personal-use mod: uses Disney/Lucasfilm IP, so it is not to be published to public mod
distribution sites.

## 1. Module & architecture

- New Gradle module `starwars`, added to `settings.gradle`. The translator discovers
  modules automatically from `settings.gradle` `include` lines filtered to those with
  `src/main/java`, so no translator config is needed.
- Layout mirrors wildwest: `StarWarsMod`, `Registration`, `ModEntities`, `ModSounds`,
  packages `entity/`, `entity/ai/`, `item/`, `client/`, `network/`, `spawning/`,
  `data/`, `world/`, plus `tools/` for bbmodel sources and Python generators.
- No compile dependency on any sibling mod module. Patterns are re-implemented locally
  (repo precedent: small pure helpers are copied per-mod and unit-tested per-mod).
- `gradle.properties`, `neoforge.mods.toml` template, and datagen wiring follow the
  wildwest module structure.

## 2. Characters & faction war

### Roster

| Character | Registry id | Faction | Role | Model base |
|---|---|---|---|---|
| Stormtrooper | `stormtrooper` | Empire | Common blaster mob, patrols in groups | Humanoid + helmet accessory cubes |
| Battle droid | `battle_droid` | Empire | Weak, squads of 3–5, blaster | Custom thin-limb skeleton |
| Darth Vader | `darth_vader` | Empire | Named elite: saber melee + dark powers | Humanoid + helmet/cape cubes |
| Boba Fett | `boba_fett` | Empire-aligned | Mini-boss: blaster + jetpack burst-leap | Humanoid + jetpack/helmet cubes |
| Jedi Knight | `jedi_knight` | Light | Generic saber melee, guards Jedi ruins | Humanoid + robe cubes |
| Luke Skywalker | `luke_skywalker` | Light | Named hero: saber + light powers | Humanoid |
| Obi-Wan Kenobi | `obi_wan` | Light | Named hero: saber + defensive powers | Humanoid + robe/hood cubes |
| Astromech droid | `astromech` | Neutral | Ambient utility droid, flees combat, drops parts | Custom skeleton: dome + cylindrical body + legs |

### Faction system

- `SwFaction` enum (`EMPIRE`, `LIGHT`, `NEUTRAL`) + `SwCombatant` marker interface with
  `getFaction()` — the `securitycore` `SecurityHostile` decoupling pattern, but internal
  to the module.
- Each combat mob's target selector gets `HurtByTargetGoal` plus a
  `NearestAttackableTargetGoal` whose predicate matches `SwCombatant` of the opposing
  faction. Empire and Light fight on sight.
- **Player alignment:** an `AttachmentType` on the player holding a light↔dark integer
  score. Damaging/killing a faction member moves the score toward the other side; using
  light/dark Force powers also moves it. Past a hostility threshold, the opposing
  faction's target goals include the player; between thresholds only `HurtByTargetGoal`
  applies. Serialization uses the wildwest `ModAttachments` pattern: null default
  supplier + `.serialize(CODEC, a -> a != null)` predicate, `hasData()` before reads.
  Threshold/scoring logic lives in a pure, unit-tested class.
- **Named-character singletons:** Vader, Luke, Obi-Wan, and Boba Fett each get a
  `BossSingletonSavedData` subclass (the shared base from the Null boss Phase 0
  refactor) so at most one of each exists per world. They spawn via entourage-style
  spawners (the `LeaderEntourageSpawner` pattern) anchored to their structures
  (section 5).

### Behavior notes

- Stormtroopers/battle droids: ranged AI goal that positions at range and calls the
  blaster's `fireFromMob` on a fire-rate timer (wildwest mob-gunfire pattern).
- Saber wielders: melee AI with sprint-approach; named characters add 1–2 signature
  Force moves on cooldown (Vader: choke-slow + pull; Luke: leap gap-closer; Obi-Wan:
  defensive push when crowded).
- Boba Fett: alternates hitscan bursts with a jetpack leap (vertical impulse +
  fall-damage immunity window).
- Astromech: `PathfinderMob` wander + panic; not a combatant; drops crafting parts used
  in blaster recipes.

## 3. Weapons & gear

### Blasters (pistol + rifle)

Re-implementation of the wildwest hitscan design, local to starwars:

- Pure `Hitscan`-style helper (candidate record + nearest-hit-before-wall selection),
  unit-tested Minecraft-free.
- Player path in `use()`: cooldown gate, eye-ray clip vs blocks, AABB entity gather,
  per-entity bounding-box clip, `hurtServer` with a starwars blaster damage type,
  durability, sound, tracer packet to tracking players.
- Mob path: static `fireFromMob(shooter, target)` with Gaussian aim jitter; AI goal owns
  the fire rate.
- Tracer color carried in the S2C payload: red for Empire shooters, blue for
  player/Light shooters.

### Lightsabers

- Single `LightsaberItem` with a **blade color data component** (BLUE, GREEN, RED,
  PURPLE) — the Infinity Gauntlet `ACTIVE_STONE` component pattern — rather than one
  item per color.
- High-damage melee with sweep; 3D voxel in-hand model (meteor-staff approach: voxel
  geometry + 2D GUI sprite) so it translates as geometry; emissive blade texture on the
  Java side (documented as untranslatable polish for Bedrock).
- Vader (red), Luke (green), Obi-Wan (blue), Jedi knights (blue/green) render sabers
  in-hand.

### Stormtrooper armor

- Four-piece set using the craftee pattern: `ArmorMaterial` + per-`ArmorType` defense
  map + `EquipmentAsset` resource key, iron-tier stats.
- `SetBonusHandler` (craftee/creeperskin pattern): wearing the **full set makes Empire
  mobs exclude you from auto-targeting** (disguise). Explicitly attacking them still
  provokes via `HurtByTargetGoal`.
- Pieces drop from stormtroopers (low chance) and Imperial outpost loot; craftable from
  parts.

## 4. Force powers

- **Kyber Holocron** item: rare drop from named characters + Jedi ruin loot.
- Keybind while holding opens a **radial picker** screen (starwars-local copy of the
  `RadialMath` wedge-math approach with deadzone, unit-tested; screen rendering follows
  the MC 26.1.2 `GuiGraphicsExtractor` pipeline the Infinity Gauntlet picker uses).
- Selection sent via `C2SSelectPowerPacket`, server-validated (range/held-item checks —
  the `C2SSetActiveStonePacket` handler pattern). Active power stored in a data
  component; per-power cooldowns in a `List<Long>` data component with pure helper class
  (the `InfinityCooldowns` pattern — `List<Long>` not `long[]`, for component-sync value
  equality).

| Power | Side | Effect | Cooldown ballpark |
|---|---|---|---|
| Force Push | neutral | Cone AoE knockback + brief slow | short |
| Force Pull | neutral | Yank nearest target toward player | short |
| Force Leap | light | High jump + no fall damage for the arc | medium |
| Mind Trick | light | Pacify nearby hostiles briefly (clear targets + no-retarget window) | long |
| Force Lightning | dark | Chain damage to up to 3 nearby targets | long |

- Light/dark power use moves the alignment score (section 2). Exact numbers are
  implementation-plan detail; the invariant is that sustained dark-side use makes Light
  faction hostile and vice versa.

## 5. Worldgen & structures

First datapack worldgen in the repo: registered `Structure`/`StructurePiece` with
datagen-provided structure sets and biome tags (not imperative `HideoutPlacer`-style
placement). All pieces are **code-generated** — layout math in pure, testable classes;
blocks placed in `StructurePiece.postProcess`. No NBT templates (no dev-client
dependency).

| Structure | Size | Biomes | Contents |
|---|---|---|---|
| Crashed escape pod | small | plains, desert | Blaster-pistol loot; astromech spawns nearby |
| Imperial outpost | medium | desert, badlands | Stormtrooper + battle droid garrison; Vader anchor-spawn with escort; armor-piece loot |
| Jedi ruin | medium | jungle, forest | Jedi knight guardians; Luke/Obi-Wan anchor spawns; holocron + lightsaber loot |

Natural mob spawning is structure-anchored for garrisons/named characters plus
biome-weighted ambient spawns for stormtrooper patrols and astromechs via the
`ModBiomeModifierProvider` datagen pattern (`AddSpawnsBiomeModifier`) and
`RegisterSpawnPlacementsEvent` rules in the `spawning/` package.

## 6. Art pipeline — bbmodels, no placeholders

- `starwars/tools/gen_bbmodels.py`: deterministic (namespace-UUID) generator following
  wildwest conventions — byte-identical re-runs, Java model class ↔ bbmodel cube
  one-for-one. Humanoid base skeleton + per-character accessory cubes; custom bone
  trees for battle droid and astromech.
- `starwars/tools/gen_textures.py`: finished, shaded textures at the crab-texture
  quality bar for all characters, weapons, and items. **No placeholder art ships at any
  point** — a character lands with its real texture in the same task that adds it.
- Committed `.bbmodel` sources for: all 8 characters, lightsaber, blaster pistol,
  blaster rifle, holocron, stormtrooper armor set. All openable/editable in Blockbench
  (File → Open).
- Spawn eggs via the `gen_spawn_eggs.py` approach.

## 7. Sounds

Repo convention (zero `.ogg` assets exist in any module): `sounds.json` maps starwars
events to fitting **vanilla event sounds** with subtitles — saber ignite/hum/clash,
blaster fire, droid chatter, power casts. `ModSounds` uses the standard
`DeferredRegister<SoundEvent>` + `createVariableRangeEvent` pattern.

## 8. Bedrock parity

- Translator runs at each internal milestone; `bedrock-out/starwars/` committed
  alongside Java changes; `--diff` keeps it honest in review.
- Expected `UNTRANSLATABLE.md` entries (documented, not silently dropped): radial
  picker screen, emissive saber blade rendering, datapack structures, alignment
  attachment logic (LLM-stubbed or documented), set-bonus disguise handler.
- Entities, bbmodel geometry, textures, loot tables, lang, recipes, spawn rules all go
  through existing translator stages.

## 9. Testing & error handling

- Pure-logic unit tests per repo style: hitscan candidate selection, radial wedge math,
  cooldown helpers, faction-targeting predicates, alignment threshold transitions,
  structure piece layout math, set-bonus detection.
- Attachment serialization uses the null-safe codec-predicate pattern to avoid the
  known world-save NPE class of bugs.
- Named-character singleton lifecycle (spawn/death/dimension) via `BossSingletonState`
  which is already unit-testable.
- Manual dev-client smoke testing deferred per repo norm; noted as follow-up at
  completion.

## 10. Internal build order

One implementation plan (~25–30 tasks), ordered:

1. **Foundation** — module scaffold, gradle/toml/datagen wiring, empty registrations,
   translator smoke run.
2. **Troopers + blasters + faction core** — Hitscan port, blaster items,
   stormtrooper + battle droid (models/textures/AI/spawning), `SwFaction`/`SwCombatant`,
   mutual targeting, alignment attachment.
3. **Lightsabers + named heroes** — saber item + render, Vader/Luke/Obi-Wan/Jedi knight,
   singleton SavedData, signature moves, faction-war integration.
4. **Armor + set bonus** — stormtrooper set, disguise bonus, loot/recipes.
5. **Force system** — holocron, radial picker, five powers, cooldown component,
   alignment coupling.
6. **World + specialists** — three structures with datagen, structure-anchored spawns,
   astromech, Boba Fett.
7. **Bedrock passes** — translator run + `bedrock-out/starwars` commit at the end of
   each numbered milestone above; final UNTRANSLATABLE audit.
8. **Final review** — code review, test sweep, spec-conformance check.

## Decisions log

- Approach C (single comprehensive plan) chosen by user over phased vertical slices.
- Java + Bedrock from day one (user).
- Code-generated structures over NBT templates (user; avoids dev-client dependency that
  stalled pirate ships).
- Held-item Force powers with radial picker (user).
- Faction-war dynamic (user).
- Core 8-character roster (user).
