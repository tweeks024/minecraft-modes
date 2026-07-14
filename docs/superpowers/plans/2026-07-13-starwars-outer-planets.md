# Star Wars Outer Planets, Mob Roster & Mos Eisley Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dagobah + Hoth dimensions, 11 new mobs across all worlds (incl. rideable tauntauns and a Yoda singleton), and a full Mos Eisley with the cantina + an original jukebox swing record.

**Architecture:** Everything extends wave-1 seams: planet enum/dimension bootstraps, SwMob/faction/goal AI, named-character singletons, Layout/Piece/Structure triplets, deterministic art generators, translator honesty scanners. Spec: `docs/superpowers/specs/2026-07-13-starwars-outer-planets-design.md`.

**Tech Stack:** Java (NeoForge 26.1.2), Python art/music tools (stdlib + ffmpeg for OGG), Kotlin translator (regen only), JUnit.

## Global Constraints

Same as wave 1 (decompiled-source API verification, no placeholder art, deterministic tools, pure-logic tests, honest Bedrock output, `feat(starwars):` commits with the Fable trailer).

---

### Task 1: Dimensions & gate expansion (done in-line)
- [x] Planet enum → 6, picker → 6 wedges, film textures/models/blockstate ×6, platforms (mud bricks / packed ice)
- [x] Dagobah: fog attrs + marsh noise/surface + mangrove biome (frogs/slimes now, roster later)
- [x] Hoth: crisp attrs + snow/packed-ice surface + powder-snow traps + barren biome
- [x] Cantina music chain: gen_cantina_song.py (original tune) → OGG → sound event → JukeboxSong → cantina_record item + icon

### Task 2: Mob roster (parallel agent)
- [ ] 11 entities + goals + models/renderers + loot + placements + pure tests (Jawa barter, Tauntaun snow speed)

### Task 3: Mob art (parallel agent)
- [ ] Rigs (gen_bbmodels), skins (gen_textures), egg icons (gen_spawn_eggs), egg item JSONs — deterministic

### Task 4: Structures (agent, after Task 2)
- [ ] Mos Eisley (~48×48 town + cantina w/ jukebox + guaranteed record chest, Docking Bay 94, checkpoint markers)
- [ ] Yoda's hut (YODA anchor), X-wing wreck (Dagobah), Echo Base (rebel garrison), wampa cave
- [ ] Loot, tags (hut → jedi tag), ModStructures/Provider wiring, layout tests

### Task 5: Integration & ship (owner)
- [ ] Registration spawn eggs + tab, biome roster spawns, Yoda spawner routing (Dagobah)
- [ ] Lang for all mobs/eggs/planets/disc; runClientData/runServerData; suites green; boot smoke
- [ ] Translator regen (mobs/eggs/geo/textures translate; dimensions + jukebox song honesty), driftCheck green
- [ ] Docs, commit, push
