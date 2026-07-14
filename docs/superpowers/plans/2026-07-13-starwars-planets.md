# Star Wars Planets & Hyperspace Gates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Three planet dimensions (Tatooine, Andor, Coruscant) with planet-true terrain, skies, mobs and landmark structures, connected by player-built iron hyperspace gates ignited with a Star Compass and a 4-wedge radial picker; honest Bedrock output.

**Architecture:** Dimensions/biomes/noise settings ride the existing `DatapackBuiltinEntriesProvider` seam (`DataGenerators`). Coruscant is a custom `ChunkGenerator` over a pure `CityLayout`. Gates reuse the vanilla `Portal` interface + `TeleportTransition`, the `ForcePickerScreen`/`SwRadialMath` radial, and the established C2S/S2C payload pattern. Structures follow the `EscapePodLayout/Piece/Structure` triplet. Full spec: `docs/superpowers/specs/2026-07-13-starwars-planets-design.md`.

**Tech Stack:** Java (NeoForge 26.1.2, `starwars`), Kotlin (`translator` honesty), Python (art tools), JUnit.

## Global Constraints

- All engine values/API shapes verified against decompiled 26.1.2 sources (moddev jars), never memory.
- NO placeholder art; stdlib-only deterministic generators.
- Work on `main`; never touch other modules' WIP; `bedrock-out/` only via the translator.
- Pure-logic unit tests for every new pure class; suites + `driftCheck` green; datagen byte-deterministic.
- Commits `feat(starwars): ...` / `feat(translator): ...` with the Claude Fable co-author trailer.

---

### Task 1: Planet core (keys, dimension types, noise, biomes, stems)
**Files:** `world/planet/Planet.java`, `world/planet/PlanetBiomes.java`, `world/planet/PlanetDimensions.java` (datagen bootstraps), `data/DataGenerators.java` (add DIMENSION_TYPE / NOISE_SETTINGS / BIOME / LEVEL_STEM), regen `src/generated/serverData`.
- [ ] Planet enum (levels/types/stems, wedges, StringRepresentable) + PlanetBiomes keys
- [ ] Dimension types with 26.1 EnvironmentAttributeMap skies (Coruscant: fixed-time dusk, no clock)
- [ ] Noise settings: overworld record rebuilt with desert / highland surface rules; biomes with planet spawns/features; stems
- [ ] `runServerData` twice → byte-stable JSON committed

### Task 2: Coruscant city generator
**Files:** `world/planet/CityLayout.java` (pure), `world/planet/CoruscantChunkGenerator.java`, `ModChunkGenerators` register, `CityLayoutTest`.
- [ ] CityLayout: grid/spec/column functions, palettes, plazas, pads, lit windows
- [ ] Generator: fillFromNoise + heightmaps, base height/column, mob seeding
- [ ] Tests: determinism, street alignment, bounds, variants

### Task 3: Hyperspace gates
**Files:** `Registration.java` (BLOCKS register + star_compass + tab), `block/HyperspacePortalBlock.java`, `block/GateShape.java`, `block/GateBuilder.java`, `block/PortalLink.java`, `block/PortalRecords.java`, `item/StarCompassItem.java`, `network/{C2SSelectPlanetPacket,S2COpenPlanetPickerPacket}.java` + `NetworkHandlers`, `client/PlanetPickerScreen.java`, blockstates/models, tests (`GateShapeTest`, `GateBuilderTest`, `PortalLinkTest`, `PlanetTest`).
- [ ] Block + state props + film dissolve + Portal impl (20/40-tick transitions)
- [ ] Compass use → validate → S2C open → radial → C2S select → fill film
- [ ] PortalLink/PortalRecords/GateBuilder arrival pipeline + return gates
- [ ] Pure tests green

### Task 4: Planet structures (parallel agent)
**Files:** `world/{MoistureFarm,Sandcrawler,FerrixTown,KraytSkeleton}{Layout,Piece,Structure}.java`, `ModStructures.java`, `data/ModStructureProvider.java`, chest loot JSON ×3, structure tags, 4 layout tests.
- [ ] Four triplets + wiring + loot + biome-set extensions for existing structures

### Task 5: Client polish
**Files:** `client/PlanetPickerScreen.java` (Task 3), `client/PlanetSkies.java` (Tatooine twin-sun skybox via `RegisterCustomEnvironmentEffectRendererEvent` + `NeoForgeEnvironmentAttributes.CUSTOM_SKYBOX`; timeboxed — attribute-only skies are the fallback), `ClientSetup.java`.
- [ ] Radial screen; twin suns or documented fallback

### Task 6: Spawning
**Files:** `spawning/NamedCharacterSpawner.java` (per-character dimension map: Boba→Tatooine, Vader→Coruscant), `StarWarsMod.java` (spawn placements for planet mobs if needed).
- [ ] Biome spawner lists (Task 1 biomes) + singleton gates + placement rules

### Task 7: Assets & lang (partial parallel agent)
**Files:** `tools/gen_block_textures.py` (portal films ×4 + mcmeta), `gen_item_textures.py` (star_compass), blockstates/models JSON, `ModLanguageProvider`, `ModRecipeProvider` (star_compass), regen client/server data.
- [ ] Textures deterministic + finished; models/blockstates; lang; recipe

### Task 8: Verification
- [ ] `:starwars:test` green (all new pure tests), datagen byte-stable, `runGameTestServer`-level boot sanity if cheap

### Task 9: Translator honesty + regen
**Files:** `translator/src/main/kotlin/.../json/WorldgenStructureScanner.kt` (or sibling `DimensionScanner.kt`), `java/BlockScanner` (record-only), tests, regenerated `bedrock-out/starwars`.
- [ ] Dimension/biome/noise/dimension_type + block registrations recorded in UNTRANSLATABLE.md
- [ ] `:translator:translate` + `:translator:test` + `driftCheck` green; bedrock-out committed
