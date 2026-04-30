# Bedrock Translator — Design

**Date:** 2026-04-30
**Status:** Draft (pending user review of this doc)
**Series:** Tooling subproject. Not a mod; an internal CLI for porting the existing four mods (`securitycore`, `securityguard`, `creeperskin`, `thief`) from Java/NeoForge to Bedrock Add-Ons so they can be played on Switch via Realms.

## Goal

Add a `:translator` Gradle subproject that takes any mod in this repo as input and emits a working Bedrock Add-On (behavior pack + resource pack) into `bedrock-out/<mod_id>/`. The translator combines a deterministic JSON-to-JSON pipeline with a Java-source pattern matcher backed by an LLM fallback for custom AI/behavior code that has no mechanical equivalent.

The user-visible end state: `./gradlew :translator:translate` produces, for each mod in this repo, a Bedrock Add-On usable in a Realm-hosted world that Switch clients can join.

## Non-goals

- General-purpose translator for arbitrary mods on the internet. Designed for the four mods in this repo; generalization is opportunistic, not required.
- Round-trip editing (Bedrock → Java). One-way only.
- Translation of mixin patches, custom damage types, custom dimensions. These have no Bedrock equivalent and are logged as `UNTRANSLATABLE.md` entries per mod.
- Dependency on Blockbench being installed on the build machine. `.bbmodel` conversion is implemented natively in Kotlin.
- Java mods on retail Switch via sideloading. Switch deployment is exclusively via Realms hosting an Add-On enabled world.

## Platform

JVM 21 (matches existing Gradle toolchain in `gradle.properties`). Kotlin for the translator implementation. Dependencies: **JavaParser** (with `JavaSymbolSolver`) for source AST + type resolution, **anthropic-java** SDK for LLM calls. Bedrock target version pinned in `translator/bedrock-target.json`; default at project start is the latest stable Bedrock release available (currently 1.21.80).

## Architecture

Two pipelines, deliberately decoupled, plus a `.bbmodel` converter.

```
   Mod source (e.g. securityguard/)
        │
        ├──► [JSON Pipeline] ◄── deterministic, no LLM
        │      reads:  src/generated/serverData/**, src/main/resources/**
        │      writes: recipes, loot tables, lang, sounds, textures,
        │              item icons, item_texture.json atlas
        │
        ├──► [Bbmodel Converter] ◄── deterministic, no LLM
        │      reads:  <mod>/tools/*.bbmodel
        │      writes: resource_pack/models/entity/*.geo.json,
        │              resource_pack/animations/*.animation.json
        │
        └──► [Java Pipeline] ◄── pattern matcher + LLM
               reads:  src/main/java/** (with full classpath type resolution)
               writes: entity JSON (manifest, components),
                       item JSON (component-based),
                       behavior_pack/scripts/*.js for custom behavior,
                       UNTRANSLATABLE.md for things with no equivalent

   ─► bedrock-out/<mod_id>/{behavior_pack,resource_pack}/
```

The two pipelines share nothing at runtime. The JSON pipeline runs in milliseconds and has golden-file unit tests. The Java pipeline is the only place the LLM is invoked, and only on the subset of inputs the deterministic pattern matcher cannot handle. If the LLM portion fails or produces poor output, only the Java pipeline must be re-run.

## Module layout

```
translator/
  build.gradle                         # Kotlin + JavaParser + anthropic-java
  bedrock-target.json                  # pinned Bedrock format versions
  src/main/kotlin/com/tweeks/translator/
    Cli.kt                             # entrypoint
    discover/
      ModDiscovery.kt                  # walks settings.gradle, classifies mods
    json/
      RecipeTransform.kt
      LootTableTransform.kt
      LangTransform.kt
      AssetCopier.kt                   # textures, sounds + item_texture.json atlas
    bbmodel/
      BbmodelConverter.kt              # native .bbmodel → .geo.json + .animation.json
    java/
      JavaSourceLoader.kt              # JavaParser + JavaSymbolSolver
      ClasspathResolver.kt             # extracts runtime classpath from sibling Gradle projects
      EntityAnalyzer.kt                # attributes, goals, registration → Bedrock entity JSON
      ItemAnalyzer.kt                  # item properties, custom logic → Bedrock item JSON
      goals/
        VanillaGoalCatalog.kt          # vanilla AI goal FQN → minecraft:behavior.* mappings
        GoalMatcher.kt                 # AST → component or LLM ticket
      llm/
        ClaudeClient.kt                # anthropic-java SDK wrapper, prompt cache enabled
        TranslationPrompt.kt           # system prompt construction
        ConfidenceGate.kt              # high/medium/low routing
        Cache.kt                       # source-hash → output, skips API on cache hit
    manifest/
      UuidGen.kt                       # deterministic UUIDv5 generation
      ManifestWriter.kt                # behavior_pack + resource_pack manifest.json
    emit/
      AddonWriter.kt                   # builds Bedrock dir structure
      Untranslatable.kt                # writes UNTRANSLATABLE.md
  src/main/resources/
    bedrock-api/
      server-1.21.80.d.ts              # vendored @minecraft/server type declarations
      worked-examples.md               # canonical event-driven idioms for the LLM prompt
  src/test/kotlin/...
  goldens/                             # checked-in expected outputs per mod, per pipeline
```

## JSON pipeline (deterministic)

NeoForge datagen output is the easy half. `src/generated/serverData/data/<modid>/` already contains vanilla-format JSON for recipes, loot tables, advancements, lang. `src/generated/clientData/assets/<modid>/lang/` similarly. These transform mechanically:

- **Recipes** — vanilla shaped/shapeless recipe JSON → Bedrock crafting recipe JSON. Ingredient namespaces remapped (`minecraft:iron_ingot` stays; `securityguard:guard_helmet` stays). Format version pinned per `bedrock-target.json`.
- **Loot tables** — vanilla loot JSON → Bedrock loot table JSON. Pool semantics line up; functions (`set_count`, `enchant_with_levels`) map to Bedrock equivalents where they exist.
- **Lang** — `en_us.json` translation keys reformatted into Bedrock's `texts/en_US.lang` flat key=value text format.
- **Sounds** — `sounds.json` reformatted into Bedrock's `sounds/sound_definitions.json` schema.
- **Textures** — copied through, preserving paths; `item_texture.json` atlas generated by walking all item textures and emitting `texture_data.<short_name>.textures` entries (Bedrock requires this dictionary; Java's per-item `models/item/*.json` does not exist on Bedrock).
- **Item icons / models** — vanilla `models/item/*.json` examined for textures referenced; the textures are routed through the `item_texture.json` atlas above.

Every transform is unit-tested with a golden file in `goldens/<mod>/<category>/<file>.json`. Diff = test fail.

## Bbmodel converter

`.bbmodel` v4+ is JSON. Blockbench was designed with Bedrock as a first-class target, so its internal data model — bones (pivot, rotation, parent), cubes (origin, size, uv, inflate, mirror), animation channels (bone, channel, keyframes) — maps almost directly onto Bedrock's geometry and animation schemas.

`BbmodelConverter` parses `.bbmodel` JSON, walks the bone/cube tree, and emits two files per model:
- `resource_pack/models/entity/<name>.geo.json` — Bedrock geometry
- `resource_pack/animations/<name>.animation.json` — Bedrock animations (if any are defined in the `.bbmodel`)

For features the `.bbmodel` includes that have no Bedrock geometry equivalent (Java-only render flags, exotic UV modes), the converter logs a warning and degrades gracefully.

**Pivot and rotation-origin translation.** The `.bbmodel` JSON stores cube `origin` and bone `pivot` in Blockbench's global (model-space) coordinates. Bedrock geometry expects bone `pivot` in global model space (matches), but cube `origin` is interpreted differently between renderers depending on whether `bind_pose_rotation` and `parent` are set. Java's `ModelPart`, by contrast, uses parent-local coordinates throughout. Because the source-of-truth in this repo is `.bbmodel` (not Java `ModelPart`), the converter only needs the Blockbench → Bedrock mapping, not the Java mapping. However, the converter must still:

1. Walk the bone tree once to compute each bone's parent chain.
2. Emit cube `origin` values in the coordinate system Bedrock geometry expects for the chosen `format_version` (1.16.0 in `bedrock-target.json`).
3. Verify visual alignment in-game during Phase 1 against a known-good Bedrock test entity. Phase 1's verification step explicitly includes manually spawning the security guard via a temporary spawn-egg JSON and confirming limb attachment points are correct. If alignment is off, add a small offset-correction step in `BbmodelConverter` rather than hand-editing the emitted `.geo.json`.

This is fully deterministic and unit-testable with golden files. Lives in the `bbmodel/` package but conceptually belongs to the deterministic side of the project alongside the JSON pipeline. Critically: this means `.bbmodel` files remain the source of truth, and Bedrock geometry is regenerated on every build — Java mod and Bedrock Add-On stay visually in sync without manual export discipline.

## Java pipeline

### Source loading and type resolution

`JavaSourceLoader` configures JavaParser with a `JavaSymbolSolver` over a `CombinedTypeSolver`:

```
CombinedTypeSolver:
  ├─ ReflectionTypeSolver()                          # JDK
  ├─ JavaParserTypeSolver(currentMod/src/main/java)  # the mod itself
  ├─ JavaParserTypeSolver(depMod/src/main/java)      # for each depended-on sibling mod
  └─ JarTypeSolver(jar) for jar in runtimeClasspath  # NeoForge, Minecraft, mappings
```

`ClasspathResolver` extracts each mod's `runtimeClasspath` configuration via the Gradle API at translator-task configuration time, so no manual jar-pointing is required.

Symbol resolution is slow on cold start (every jar's class index must be parsed). The resolved type-graph for a mod-classpath-hash is cached to `translator/.cache/symbols/<hash>.bin`. First run on a mod is ~10–30s; subsequent runs are effectively free unless the classpath changes.

The translator operates per Gradle subproject — it cannot be invoked on a single `.java` file in isolation. This matches the CLI shape (`./gradlew :translator:translate --args="securityguard"`).

### Entity analysis

`EntityAnalyzer` for each mod entity class:
1. Pulls `EntityType.Builder` registration data from the mod's `Registration.java`/`ModEntities.java` (size, tracking range, spawn category).
2. Pulls attribute values from the static `createAttributes()` method (max health, attack damage, movement speed, follow range, knockback resistance).
3. Walks `registerGoals()` to collect AI goals in priority order. Each goal is handed to `GoalMatcher`.
4. Inspects entity fields. Fields that have no Bedrock-component equivalent (custom NBT-backed state like `RevealState`, `ThiefEntity` reveal timers) are flagged for the **Data Syncer** pattern: a JS module under `behavior_pack/scripts/data/<entity_id>_state.ts` using `entity.setDynamicProperty()` / `getDynamicProperty()` for persistence. The LLM prompt is instructed to prefer dynamic properties over inventing fake components.
5. Emits `behavior_pack/entities/<entity_id>.json` (server-side components) and `resource_pack/entity/<entity_id>.entity.json` (client-side render controllers, tied to the `BbmodelConverter` outputs).

### Item analysis

`ItemAnalyzer` for each mod item class:
1. Reads constructor arguments for stack size, durability, rarity, food properties.
2. Recognizes vanilla `Item` subclasses (`SwordItem`, `ArmorItem`) and emits matching Bedrock item components.
3. Extracts custom behavior overrides (`use`, `hurtEnemy`, `inventoryTick`) and routes them to the Java pipeline LLM stage as event handlers in `behavior_pack/scripts/items/<item_id>.ts`.

### Goal matching

`VanillaGoalCatalog` maps vanilla AI goal FQNs to `minecraft:behavior.*` JSON components. Initial catalog covers ~40 commonly-used vanilla goals (movement, targeting, look, idle, swim, panic, melee/ranged attack, plus golem-specific goals relevant to `SecurityGuardEntity`). Catalog grows as the four mods reveal new ones.

`GoalMatcher` for each goal:
- If the goal class FQN hits the catalog **and** the constructor arguments are simple literals (numbers, booleans, references to other simple values), emit the JSON component directly. **High confidence.** No LLM call.
- If the goal class extends a vanilla goal but adds overrides, **or** is a custom class composed of recognizable primitives (move + attack + cooldown), call the LLM with a structured prompt. The LLM self-rates confidence. If ≥ 0.8, write `behavior_pack/scripts/goals/<goal>.ts` and commit. Otherwise demote to Low.
- Otherwise (custom class with novel logic — `StealFromChestGoal`, `ReturnToHideoutGoal`), emit a `// TODO LLM:` JS stub with the original Java pasted in a comment + a description, and add an entry to `UNTRANSLATABLE.md`.

### LLM integration

- **Model:** `claude-opus-4-7`. Sonnet 4.6 is a viable cost-saving substitute; opus is preferred for novel-logic correctness on Medium-bucket translations.
- **System prompt:** assembled at startup from three sources, all marked for Anthropic prompt-cache so they're charged once per session:
  1. The vendored `@minecraft/server` type declarations (`bedrock-api/server-<version>.d.ts`) for the pinned Bedrock target.
  2. Worked examples (`bedrock-api/worked-examples.md`) showing canonical event-driven idioms (event subscription, `system.runInterval` with sensible cadences, `entity.setDynamicProperty` for state).
  3. Performance rules (see below).
- **Per-request user message:** the original Java goal source, the resolved type info (parent class, called methods, referenced fields), and the surrounding context (entity class summary, mod manifest).
- **Output cache key:** `sha256(java_source + resolved_symbol_closure + classpath_fingerprint + prompt_version + model_id + bedrock_api_version)`. Stored at `translator/.cache/llm/<key>.json`. Invalidates automatically when any input changes.
  - `resolved_symbol_closure`: For each method or field the goal class references that resolves outside its own source file (via `JavaSymbolSolver`), the FQN plus the resolved declaration's signature hash (parameter types, return type, declared exceptions). This catches the case where a utility class the goal depends on changes signature without the goal's own source changing — e.g. `SecurityCoreUtil.applyStun(Entity)` becoming `applyStun(Entity, int)` would have produced a stale cache hit under a `java_source`-only key.
  - `classpath_fingerprint`: SHA-256 of the sorted list of `(jarFileName, jarSha256)` pairs for the resolved `runtimeClasspath`. Coarser than the symbol closure but cheap to compute, and busts the cache on any NeoForge/Minecraft version bump even when the resolved closure doesn't visibly change.

### Performance rules in the system prompt

The LLM is instructed (with worked examples for each) to:

1. Translate discrete-event Java logic (entity hurt, block placed, item used, entity died) to `world.afterEvents.*` / `world.beforeEvents.*`. Never poll for these.
2. For periodic state checks (cooldown countdown, AI re-targeting, distance check), use `system.runInterval(fn, ticks)` with the **largest** interval that preserves correctness. Default to 20 (1 second) unless the source explicitly uses a tighter tick rate.
3. Pool intervals where possible — share a single `runInterval` for multiple pieces of logic that want the same cadence.
4. Never use `system.run()` for periodic logic (it is one-shot).
5. If the source's tick semantics genuinely cannot be loosened, output `runInterval(fn, 1)` with a `// PERF:` comment explaining why. The `// PERF:` markers are searchable audit points for code review.

### Confidence gate

| Confidence | Source | Action |
|---|---|---|
| **High** | Vanilla goal class, simple constructor args, catalog hit | Emit `minecraft:behavior.*` JSON directly. No LLM call. |
| **Medium** | Extends vanilla goal with overrides, OR custom class composed of recognizable primitives | LLM called. If self-rated ≥ 0.8, write JS + commit. Else demote to Low. |
| **Low** | Custom class with novel logic | Emit `// TODO LLM:` JS stub + `UNTRANSLATABLE.md` entry. No auto-shipped LLM output. |

The asymmetry is intentional: the cost of a wrong High call is ~zero (catalog matches are mechanical). The cost of a wrong Low call is also ~zero (it's a TODO; the human writes it). The expensive failure mode is a confidently-wrong Medium output, which is why the LLM-self-rated threshold gates commit.

## Manifest and UUID generation

Manifest UUIDs are derived deterministically:

```
namespace = UUIDv5(DNS_NAMESPACE, "minecraft-mods.tweeks.dev")
header_uuid     = UUIDv5(namespace, mod_id + ":header")
behavior_module = UUIDv5(namespace, mod_id + ":modules.behavior")
resource_module = UUIDv5(namespace, mod_id + ":modules.resource")
core_dependency = UUIDv5(namespace, "securitycore:header")  # for sibling-mod refs
```

Same input → same UUID forever. Stable across reruns, machines, and `git clone`. This is critical: Bedrock identifies installed packs by header UUID, so non-deterministic UUIDs would break existing world saves on every translator rerun.

## Asset atlas (item_texture.json)

Bedrock requires a single dictionary at `resource_pack/textures/item_texture.json` mapping `texture_data.<short_name>.textures` → relative texture path. Java has no equivalent; per-item `models/item/*.json` files reference textures individually.

`AssetCopier` walks each mod's `assets/<modid>/textures/item/` once per translation, builds the dictionary, writes it. `terrain_texture.json` follows the same pattern when blocks appear.

## securitycore deduplication

`securitycore` becomes its own behavior pack at `bedrock-out/securitycore/behavior_pack/`. Sibling mods (`securityguard`, `thief`) declare it via Bedrock's `dependencies` array in their `manifest.json` with the `securitycore:header` UUID and version. Users install all required packs together; Bedrock resolves the dependency at world load. This mirrors NeoForge's required-dependency model.

## Format-version pinning

`translator/bedrock-target.json`:

```json
{
  "min_engine_version": [1, 21, 80],
  "format_versions": {
    "entity": "1.21.0",
    "item": "1.21.0",
    "recipe": "1.20.10",
    "geometry": "1.16.0",
    "animation": "1.10.0",
    "loot_table": "1.20.10"
  },
  "scripting_api_version": "1.21.80"
}
```

Single source of truth for Bedrock's per-document format-version regime. The `scripting_api_version` field selects which `bedrock-api/server-<version>.d.ts` is loaded into the LLM system prompt. Updating Bedrock target = swap one config = consistent change across all output.

## Output is committed

`bedrock-out/` is checked into git. Translation reruns produce diffs that surface in PRs. If a developer hand-edits a file in `bedrock-out/` and reruns, source-hash mismatch causes the translator to overwrite with a warning. To preserve hand edits, do not run the translator. (Hand edits should be promoted to either the source mod or the translator itself.)

## Lifecycle integration

| Lifecycle hook | Behavior |
|---|---|
| `./gradlew build` (default) | Translator does **not** run. Java mods build normally. |
| `./gradlew :translator:translate` | Manual, full translation, may call LLM, writes `bedrock-out/`. |
| `./gradlew check` (CI) | Runs `:translator:translate --diff --no-llm` (cache-only). If `bedrock-out/` is out of sync with sources, fails the build with the diff and instructions. |
| `./gradlew :translator:publish` (Phase 5) | Builds `.mcaddon` zips from `bedrock-out/`. No translation. |

The CI drift gate is load-bearing. Without it, `bedrock-out/` silently rots. With it, a Java change that affects Bedrock cannot land without the corresponding Bedrock change in the same PR. `--no-llm` ensures CI never spends LLM tokens — it relies on the cache, and a cache miss fails CI with "run :translate locally and commit."

## CLI

```
./gradlew :translator:translate                          # all mods
./gradlew :translator:translate --args="securityguard"   # one mod
./gradlew :translator:translate --args="--diff"          # dry run, show diff vs committed output
./gradlew :translator:translate --args="--no-llm"        # skip LLM, use cache or TODO
./gradlew :translator:translate --args="--clear-cache"   # invalidate source-hash cache
```

## Untranslatable features per mod

Captured at design time so expectations are calibrated. Each mod's `UNTRANSLATABLE.md` will be regenerated by every translation; the content here is the design-time prediction.

**securitycore:**
- `StunningMeleeGoal` — composite of vanilla melee + custom stun timer. **Medium** bucket: LLM-translated to a `runInterval` + `system.runTimeout` pattern.
- `api.SecurityAlly` / `api.SecurityHostile` — Java marker interfaces. No Bedrock equivalent. Translated as **family tags** on the Bedrock entity JSON (`minecraft:type_family.family: ["security_ally", ...]`); cross-mod targeting becomes family-based.

**securityguard:**
- All vanilla goals (`MoveTowardsTargetGoal`, `OfferFlowerGoal`, `LookAtPlayerGoal`, `DefendVillageTargetGoal`, etc.) — **High** bucket, direct catalog hits.
- `GuardTargetHostilesGoal` — **Medium** bucket, custom class but composed of standard targeting primitives.
- `client.renderer.SecurityGuardRenderer` — dropped entirely. Bedrock renders entities from resource pack geometry/render-controllers; the Java renderer has no analog.
- `client.model.SecurityGuardModel`, `BatonModel` — dropped (replaced by `BbmodelConverter` output of `tools/security_guard.bbmodel` and `tools/baton.bbmodel`).

**creeperskin:**
- Standard armor item set — **High** bucket, vanilla `ArmorItem`, mechanical translation.
- Set-bonus logic (creepers ignore wearer + zero blast damage) — **Medium** bucket, LLM-translated to a `world.afterEvents.entityHurt` handler that checks for full-set wearing and modifies/cancels damage. The "creepers ignore" half becomes a `world.afterEvents.dataDrivenEntityTrigger` or a periodic re-targeting check; LLM picks the lighter-weight option.
- `tools/creeper_armor_leggings.bbmodel` — `BbmodelConverter` output.

**thief:**
- All vanilla movement/wandering goals — **High**.
- `BlackjackStrikeGoal`, `FleeAndFireCrossbowGoal` — **Medium**, melee/ranged primitives.
- `WanderInVillageGoal`, `SecretGuardTargetGoal` — **Medium**, stretching the LLM but tractable.
- `ReturnToHideoutGoal`, `StealFromChestGoal` — **Low**. Heavy chest-NBT-touching logic. `// TODO LLM:` stubs; manual JS using Bedrock's `Container` API on block components.
- `world.HideoutPlacer` — **Low**. Bedrock structure spawn API is very limited. Likely manual JS using `world.getDimension().runCommand("structure load ...")` invocations triggered by world-init events. Logged in `UNTRANSLATABLE.md`.
- `ModDamageTypes.java` (`SHIV` damage type) — Bedrock has no custom damage types. Falls back to generic `entity_attack`. Logged.
- `entity.RevealState` (custom NBT field tracking disguise state) — **Data Syncer** pattern: dynamic property on the entity, JS module manages reveal/hide transitions.

## Testing

- **JSON pipeline:** golden-file tests. `goldens/securityguard/recipe/guard_helmet.json` is the expected Bedrock output for a known input. Diff = test fail.
- **Bbmodel converter:** golden-file tests over the three real `.bbmodel` files in this repo.
- **Java pipeline pattern matcher:** unit tests over fixtures of vanilla goal usages, with `JavaSymbolSolver` configured against a fixture classpath.
- **LLM integration:** mocked Anthropic client returns canned responses; tests verify confidence gate routes correctly. No live API calls in CI.
- **End-to-end:** `./gradlew :translator:translate --diff --no-llm` in CI fails the build if `bedrock-out/` is out of sync with sources.

## Phased build plan

1. **Phase 0 — skeleton.** Gradle subproject, CLI bootstrap, `ModDiscovery` walking `settings.gradle`, empty pipelines, deterministic UUID generation, manifest writer producing valid empty Add-On scaffolds. Verifiable: `./gradlew :translator:translate --args="securityguard"` writes a Bedrock pack that loads in Bedrock with no content but no errors.
2. **Phase 1 — JSON pipeline + bbmodel converter.** Recipes, loot tables, lang, sounds, asset copy, `item_texture.json` atlas, `BbmodelConverter`. Golden tests for all transforms. Verifiable: `securityguard` items appear in a Bedrock world with correct icons, names, and recipes; the security guard model and baton model render correctly when manually placed via spawn-egg JSON (added by Phase 2).
3. **Phase 2 — Java pipeline, deterministic only.** `JavaSourceLoader` with `JavaSymbolSolver`, `ClasspathResolver`, `EntityAnalyzer`, `ItemAnalyzer`, `VanillaGoalCatalog`, `GoalMatcher` (High bucket only). No LLM. Verifiable: securityguard spawns from a spawn egg, walks around with vanilla AI, defends the village, but does not use any custom goals (e.g. no `StunningMeleeGoal` behavior).
4. **Phase 3 — LLM augmentation.** `ClaudeClient`, `TranslationPrompt`, `ConfidenceGate`, `Cache`, vendored `@minecraft/server` declarations, worked-examples prompt fragment. Translates Medium-bucket goals. Verifiable: securityguard fights mobs with translated `StunningMeleeGoal` behavior; output JS reviewable in `bedrock-out/securityguard/behavior_pack/scripts/`.
5. **Phase 4 — generalize across mods.** Run on `creeperskin` and `thief`. Each mod produces an `UNTRANSLATABLE.md`. Pack dependencies wired for `securitycore`. Verifiable: all four mods produce Bedrock Add-Ons that load together without errors.
6. **Phase 5 — packaging + Realm deployment guide.** `.mcaddon` zipping task, install instructions, Realm-based Switch deployment guide added to `docs/`.

## Defaults selected during brainstorming

- Bedrock target: 1.21.80 at project start. Bumpable via `bedrock-target.json`.
- Output committed to `bedrock-out/` in this repo.
- Mod discovery from `settings.gradle` (no manual list).
- One-way only. No round-trip from Bedrock back to Java.
- `claude-opus-4-7` for Medium-bucket translations.
- LLM cache stored locally and gitignored. Translation outputs committed.
- Manual CLI invocation; CI enforces drift via `--diff --no-llm`. No translator in default `build`.

## Open items deferred

- `.mcaddon` signing for Marketplace submission. Out of scope; Realm hosting is the v1 deployment path.
- Block translation. None of the four mods add blocks today; `terrain_texture.json` atlas and block-component generation are stubs in Phase 1, fleshed out when needed.
- Multi-locale `.lang` generation. Lang transform handles `en_us` only; additional locales added when source mods produce them.
