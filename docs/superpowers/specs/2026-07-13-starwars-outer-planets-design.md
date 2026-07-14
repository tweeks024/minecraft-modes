# Star Wars Outer Planets (Dagobah, Hoth), Mob Roster & Mos Eisley — Design Spec

**Date:** 2026-07-13 (second wave, same day as the planets feature)
**Module:** `starwars`
**Approach:** Extend the shipped planet/gate system with two more dimensions, give every world its own creatures (11 new entities riding the existing SwMob/faction/goal machinery), and put a full Mos Eisley — cantina, jukebox, original swing record — on Tatooine.

## 1. Goals

1. **Dagobah** — fog-bound mangrove marsh (sea level 64 turns lowlands to marsh; fog start 12/end 90; swamp water/foliage tints). Yoda's hut (with Yoda), a crashed X-wing in the shallows, dragonsnakes in the water, bogwings in the air, frogs and slimes in the muck.
2. **Hoth** — blinding snowfields with glacier crags above y96, powder-snow traps, frozen seas. Echo Base (rebel garrison + hangar), wampa caves, rideable tauntauns, snowtroopers, probe droids.
3. **Mob roster per world** (all in the faction system, so rebels-vs-imperials firefights and wampa-hunts-tauntaun happen ambiently): Jawa (iron-ingot barter), Tusken Raider, Bantha (Tatooine); Rebel Trooper (Andor, Hoth); Probe Droid (Coruscant, Hoth); Wampa, Tauntaun (rideable, faster on snow), Snowtrooper (Hoth); Dragonsnake, Bogwing, **Yoda** — a named LIGHT singleton like Vader/Luke, home dimension Dagobah (Hoth/Dagobah).
4. **Mos Eisley** on Tatooine: large sandstone town structure — street grid, dome buildings, Docking Bay 94 pad, vaporators, stormtrooper checkpoint, jawas — centred on **the cantina**: bar interior, jukebox, and a chest that guarantees the `cantina_record` music disc.
5. **Cantina music, honestly**: the film's tune is copyrighted, so `tools/gen_cantina_song.py` synthesizes an ORIGINAL ~50s swing number (D-minor AABA, swung eighths, square-wave lead + walking bass + hats; deterministic stdlib synth, ffmpeg → OGG). Wired as `starwars:cantina_band` sound event → `JukeboxSong` datapack entry (51s, comparator 14) → `cantina_record` item (`jukeboxPlayable`, Rarity.RARE). Display name "Figrin's Fizz — Cantina Swing".

## 2. Key decisions

- Planet enum grows to 6 (radial: Tatooine, Andor, Coruscant, Dagobah, Hoth, Home; picker radius 95px). Blockstate property gains the two values; film textures/models/blockstate cover 6 planets (murky green / glacier blue films). Gate platforms: mud bricks on Dagobah, packed ice on Hoth.
- Both new worlds reuse the overworld noise router with planet surface rules (like Tatooine/Andor) — no new chunk generators.
- Rideable tauntaun follows the vanilla ridden-mob plumbing (mount on interact, rider steers, snow speed bonus in a pure tested helper); no saddle item.
- Yoda mirrors the named-character singleton pattern exactly (YodaSavedData, spawner roll in Dagobah anchored to his hut via the jedi structure tag).
- Bedrock: the 11 mobs, their eggs, geo, textures, loot and the record item/recipe-less disc all translate through the existing pipeline; the two dimensions and the jukebox song get honesty entries (the dimension scanner picks the new files up automatically; jukebox_song may need a scanner note if it lands outside scanned paths — verify at regen).

## 3. Out of scope

Bantha/tauntaun breeding, tusken-ride-bantha pairing, Dagobah dark-side cave vision, Hoth blizzard weather system, AT-AT anything.
