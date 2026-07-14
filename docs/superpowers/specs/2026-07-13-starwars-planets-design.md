# Star Wars Planets & Hyperspace Gates — Design Spec

**Date:** 2026-07-13
**Module:** `starwars` (existing), `translator` (honesty only)
**Approach:** Three datapack-registered planet dimensions (Tatooine, Andor, Coruscant) built through the existing `DatapackBuiltinEntriesProvider` seam; travel via player-built iron **hyperspace gates** ignited with a **Star Compass** that opens the proven radial picker; Coruscant is a custom Java `ChunkGenerator` over a pure, unit-tested `CityLayout`. Bedrock cannot express custom dimensions — the addon keeps translating the portable content (items, recipes, loot, textures) and every world-shaping feature is recorded honestly in `UNTRANSLATABLE.md`.

## 1. Goals

1. **Three visitable planets**, each a real dimension with its own terrain, sky, mobs, and landmarks:
   - **Tatooine** — endless desert: the Dune Sea (rolling sand) and the Jundland Wastes (red-rock canyon country). Moisture farms, a Jawa sandcrawler, krayt-dragon skeletons. Boba Fett stalks here.
   - **Andor** — cool green Aldhani-style highlands with lochs, spruce groves, a brick Ferrix-style town, and imperial garrisons. Empire presence everywhere.
   - **Coruscant** — the endless city at perpetual dusk: procedural skyscraper blocks, lit windows, streets, plazas and landing pads. Battle droids patrol; Jedi are rumored; Vader rules.
2. **Hyperspace gates**: build a rectangular **iron block** frame (like a nether portal frame), right-click it with a craftable **Star Compass**, pick a destination on a 4-wedge radial (Tatooine / Andor / Coruscant / Home), and the frame fills with planet-tinted portal film. Step through; a return gate is auto-built at the far side. Kid-proof: there is always a way home.
3. All pure logic unit-tested; finished art only; honest Bedrock output.

**Non-goals:** no Bedrock planet/portal runtime (needs the future scripting-harness project — recorded, not attempted), no space flight, no new mob species, no per-planet quests, no custom music.

## 2. Global constraints (inherited)

- NO placeholder art: 3+ tones per region, painted shading; generators stdlib-only + deterministic.
- Work on `main` directly; never touch other modules' WIP or hand-edit `bedrock-out/`.
- Engine values/API shapes come from the decompiled 26.1.2 sources (extracted from the moddev jars), never from memory. Key verified 26.1.2 facts this design relies on:
  - `DimensionType` is a 16-field record: skybox is `DimensionType.Skybox` (`NONE|OVERWORLD|END`), atmosphere lives in an `EnvironmentAttributeMap` (`FOG_COLOR`, `SKY_COLOR`, `SUNRISE_SUNSET_COLOR`, `CLOUD_HEIGHT`, `STAR_BRIGHTNESS`, `BED_RULE`, …), day/night comes from `HolderSet<Timeline>` + optional `WorldClock`.
  - Portals: vanilla `Portal` interface (`getPortalDestination → TeleportTransition`, `getPortalTransitionTime`), `entity.setAsInsidePortal(portal, pos)` from `entityInside(state, level, pos, entity, InsideBlockEffectApplier, boolean)`; `Entity.teleport(TeleportTransition)`.
  - `NoiseGeneratorSettings.overworld(BootstrapContext, false, false)` is public — planet noise settings are that record reconstructed with custom `surfaceRule`/`defaultFluid`/`seaLevel`/aquifer flags (`NoiseRouterData` itself is protected).
  - `ChunkGenerator` contract per `FlatLevelSource`: `codec`, `fillFromNoise` (prime `OCEAN_FLOOR_WG`/`WORLD_SURFACE_WG` heightmaps), `getBaseHeight`, `getBaseColumn`, `buildSurface`, `applyCarvers`, `spawnOriginalMobs`, `getMinY/getGenDepth/getSeaLevel`, `getSpawnHeight`. Generator codecs register in `Registries.CHUNK_GENERATOR`.
  - Biomes: `Biome.BiomeBuilder` with `.setAttribute(EnvironmentAttributes...)` + `BiomeGenerationSettings.Builder(placedFeatures, carvers)` + `MobSpawnSettings.Builder`; sources `FixedBiomeSource(Holder<Biome>)` / `MultiNoiseBiomeSource.createFromList`.
- Unit tests for all pure logic; dev-client verification deferred to the user's smoke session.
- Datagen byte-deterministic; `bedrock-out/` regenerated via the translator, drift gate green.
- Commits `feat(starwars): ...` / `feat(translator): ...` with the Claude Fable co-author trailer.

## 3. Dimensions (datagen: `Registries.DIMENSION_TYPE` + `Registries.LEVEL_STEM`)

All three: `coordinateScale 1.0` (coordinates match across worlds — teleports preserve X/Z), skylight on, no ceiling, no dragon fight, infiniburn overworld, monster settings copied from overworld, timelines `TimelineTags.IN_OVERWORLD`.

| | tatooine | andor | coruscant |
|---|---|---|---|
| min_y / height | -64 / 384 | -64 / 384 | 0 / 256 |
| clock | overworld | overworld | **none + `hasFixedTime`** (perpetual dusk) |
| skybox | OVERWORLD | OVERWORLD | OVERWORLD (dim stars via attrs) |
| sky / fog | warm haze: sky `0xFFD9A0`-family, fog sand-tinted, cloud height 220 | cool clear: default-ish sky, slight teal fog | violet dusk: sky `0x2B1B4D`, fog `0x8E6BE8`-family, sunrise band amber, star brightness up |
| bed rule | can sleep | can sleep | can sleep (kids die; respawn must work) |

`Planet` enum (exists): `TATOOINE, ANDOR, CORUSCANT, HOME` → level/dimension-type/stem keys, radial wedge order, `StringRepresentable` for block state.

## 4. Terrain

**Tatooine** (`starwars:tatooine` noise settings): overworld router; `defaultFluid = air`, `seaLevel 63` retained for shaping but dry — ocean basins read as deep dune seas; aquifers/ore-veins off. Surface rules: `DUNE_SEA` → sand over sandstone (deep sandstone band), `JUNDLAND_WASTES` → red sand / orange+brown terracotta banding over stone; global bedrock floor. Biome source: `MultiNoiseBiomeSource` two-point list split on continentalness/erosion so wastes cluster like badlands. Features: fossils, desert vegetation (cacti/dead bush), default ores + disks — no trees, no springs, no lakes. Spawns: stormtrooper patrols (MONSTER weight 12), astromech (CREATURE 4), husk (MONSTER 6, night scavengers).

**Andor** (`starwars:andor`): overworld router untouched (water world shaping, lochs + rivers), custom surface: grass/dirt with stone exposed above ~y100 and coarse-dirt noise patches; single biome `ALDHANI_HIGHLANDS` via `FixedBiomeSource`. Features: sparse taiga spruces, forest rocks (boulders), default grass+flowers, ores, springs. Spawns: sheep/rabbit/fox livestock, stormtrooper patrols (10), vanilla night monsters off (kept out for highland calm — monster category only troopers).

**Coruscant** (`starwars:coruscant`): `CoruscantChunkGenerator` (registered `MapCodec`, `FixedBiomeSource(CORUSCANT_CITY)`) over pure `CityLayout`:

- 24-block city grid: 5-wide streets (light gray concrete, white dashed centerline, glowstone lamp posts at intersections), 19×19 plots.
- Per-cell seeded spec (`hash(cellX, cellZ, worldSeed)`): tower height 24–120 above street (y=64), 3 palettes (gray concrete, white/quartz, blackstone/iron), window column pattern with lit bands (sea lantern) so the dusk city glows; ~1/13 cells are plazas (quartz floor, lamps), ~1/17 landing pads (flat deck, pad circle).
- Below street: solid foundation to bedrock at min_y. `getBaseHeight`/`getBaseColumn` reflect real column tops so spawning + structures behave. `spawnOriginalMobs` mirrors the vanilla NaturalSpawner call.
- Spawns: battle droid (MONSTER 14), jedi knight (CREATURE 3).
- All layout decisions in `CityLayout` (pure): deterministic, cross-chunk street alignment, bounded heights — unit-tested.

## 5. Hyperspace gates

- **Star Compass** (`star_compass`, `Registration.ITEMS`): shaped recipe — compass center, gold ingot N/S/E/W, amethyst shard corners. `use` on an iron-block frame face: server validates the frame via `GateShape`, then S2C-opens the radial.
- **`GateShape`** (pure, world access abstracted behind predicates): finds a rectangular frame of iron blocks around the clicked face in plane X or Z; interior 2–8 wide × 3–8 tall, empty (air or replaceable); returns interior origin/axis/size. Unit-tested against synthetic grids (valid sizes, holes, corner-missing tolerance like vanilla, non-empty interior rejection).
- **`PlanetPickerScreen`** (client, mirrors `ForcePickerScreen` + `SwRadialMath` with `wedgeCount=4`): Tatooine top, Andor right, Coruscant bottom, Home left. Select → `C2SSelectPlanetPacket(framePos, wedge)`; server re-validates shape + range + not-current-planet, then fills the interior with portal film.
- **`HyperspacePortalBlock`** (first block in the mod; `DeferredRegister.createBlocks`): properties `PLANET` (enum, 4) + `AXIS` (X/Z); thin pane shape, no collision, light 11, unbreakable-by-hand like nether portal, no item form. Implements `Portal`: instant transit (transition time 0 — passing through the film is enough; re-entry bounce is prevented by the vanilla arrival cooldown); `entityInside → setAsInsidePortal`. Neighbor updates dissolve the film when the frame breaks (local frame-or-portal check, cascades). `onRemove` unregisters its gate record.
- **Destination** (`getPortalDestination`): target level from the `PLANET` property (HOME → overworld). Exit selection in **`PortalLink`** (pure decision core + thin level adapter): clamp X/Z to the border, look up **`PortalRecords`** (per-level `SavedData`: gate interior origin + axis + bound planet) within 96 blocks → nearest reusable gate; otherwise stamp a fresh arrival gate from **`GateBuilder`** (pure placement list: 4×5 iron frame, 2×3 film bound back to the origin planet, 5×3 platform + clearance carve; platform palette per planet — smooth sandstone / stone bricks / light gray concrete / stone bricks for home) at the heightmap surface (Coruscant: generator base height ⇒ streets/roofs), then teleport with `PLAY_PORTAL_SOUND + PLACE_PORTAL_TICKET`.
- Both ends' gates are recorded on ignite and on arrival-build; realistic worst case (records lost) still works — a new gate is stamped.

## 6. Structures & singletons

Four new `Layout`+`Piece`+`Structure` triplets (pure layouts, tested): `moisture_farm` + `sandcrawler` + `krayt_skeleton` (Tatooine biomes), `ferrix_town` (Andor). Chest loot per structure under `data/starwars/loot_table/chests/`. Existing structures spread: `imperial_outpost` also in `ALDHANI_HIGHLANDS` + `JUNDLAND_WASTES`; `jedi_ruin` also in `JUNDLAND_WASTES`.

`NamedCharacterSpawner`'s overworld gate becomes a per-character dimension map: Luke/Obi-Wan/Han/Leia → overworld (unchanged), **Boba Fett → Tatooine**, **Darth Vader → Coruscant**. Singleton bookkeeping stays anchored on overworld `SavedData` (already dimension-agnostic — verified in `SingletonState`).

## 7. Assets

- Portal film: `hyperspace_portal_{tatooine,andor,coruscant,home}.png` — 8-frame 16×128 animated strips + `.mcmeta` (`frametime 3, interpolate`), planet hues (amber/green/violet/blue), generated by new `tools/gen_block_textures.py`.
- `star_compass.png` via `gen_item_textures.py`.
- First block models in the mod: thin two-face portal pane models ×4 planets ×2 axes (`render_type: translucent`), `blockstates/hyperspace_portal.json` 8 variants. No BlockItem.
- Lang via `ModLanguageProvider`: item, block, screen title, 4 wedge labels, gate messages (`starwars.gate.*`).

## 8. Bedrock honesty (translator)

New record-only scanners + notes, keeping `driftCheck` green:
- `data/<mod>/dimension/*.json`, `dimension_type/*.json`, `worldgen/biome/*.json`, `worldgen/noise_settings/*.json` → "custom dimensions/biomes are not expressible in Bedrock add-ons" entries (mirrors the existing worldgen-structure scanner).
- Java `BLOCKS` DeferredRegister scan → per-block "custom block not translated" entry (first block in any mod here).
- Star Compass translates as a plain item; its `use` behavior lands in the existing custom-item-behavior notes.
- The planets themselves, gates, and the Coruscant generator are **not** approximated on Bedrock in this feature; a future `bedrock scripting harness` project (script module + entry point + teleport API surface) is the prerequisite and is out of scope.

## 9. Testing

Pure JUnit: `CityLayoutTest` (determinism, street continuity across cells, height bounds, palette/variant distribution, lit-window presence), `GateShapeTest`, `GateBuilderTest` (frame/film/platform counts, film bound to origin, clearance), `PortalLinkTest` (nearest-record choice, reuse radius, fallback), `PlanetTest` (wedge/level round-trips), 4 structure layout tests. Suites `:starwars:test` + `:translator:test` green; datagen re-run byte-stable; full `./gradlew check` (drift gate) green. In-game smoke on the dev client is the user's session.
