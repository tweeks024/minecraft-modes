# Han Solo Armor (Scoundrel Set) — Design Spec

**Date:** 2026-07-12
**Module:** `starwars` (existing)
**Approach:** Approach A — set bonus hooked inside `BlasterPistolItem.use()` with a server-side per-player `QuickdrawState` map; material/items/art mirror the proven stormtrooper-armor pipeline with netherite-grade values.

## 1. Goals

1. A full 4-piece **Han Solo armor set** with **netherite-equivalent stats**, craftable, with finished art on every surface (item sprites, worn layers, editable bbmodels).
2. **Scoundrel's Luck** set bonus: wearing all four pieces, the player's first blaster shot against each newly acquired target deals double damage — the player-side version of Han's own quickdraw.
3. Bedrock output through the existing armor pipeline; the set bonus recorded honestly in `UNTRANSLATABLE.md`.

**Non-goals:** no new weapon, no smithing-table upgrade path, no Han-drop acquisition, no set-bonus persistence across restarts, no Bedrock set-bonus scripting.

## 2. Global constraints (inherited)

- NO placeholder art: 3+ tones per material region, painted shading, recognizable look (Han's outfit worn on the player). bbmodel sources committed and editable.
- Work on `main` directly; never stage or modify the user's pre-existing WIP in other modules or their `bedrock-out/` trees.
- Engine values come from decompiled sources, never memory — **all netherite numbers below are lifted verbatim from decompiled `ArmorMaterials.NETHERITE` at implementation time**; the figures in §3 are expected values to be confirmed, not authorities.
- Bedrock honesty: every untranslatable behavior gets an `UNTRANSLATABLE.md` entry.
- Unit tests for all pure logic; dev-client verification deferred to the user's smoke session.
- Tool ordering: run `gen_textures.py`-family scripts BEFORE `gen_bbmodels.py` (empty-embed race); box-UV unwraps must fit their canvas.
- Commits `feat(starwars): ...` style with the Claude Fable co-author trailer.

## 3. Material and items

`HanSoloArmorMaterials` in `item/`, mirroring `StormtrooperArmorMaterials`' exact five-part shape (asset key + DEFENSE map + `ArmorMaterial` constant), with netherite-grade values **verified against decompiled `ArmorMaterials.NETHERITE`** (expected: durability multiplier 37; defense HELMET 3 / CHESTPLATE 8 / LEGGINGS 6 / BOOTS 3 / BODY 11; enchant value 15; `SoundEvents.ARMOR_EQUIP_NETHERITE`; toughness 3.0F; knockback resistance 0.1F; `ItemTags.REPAIRS_NETHERITE_ARMOR`). Asset key `HAN_SOLO_ASSET` → `starwars:han_solo` (resolves to `assets/starwars/equipment/han_solo.json`).

Four items in `Registration`, mirroring the stormtrooper pieces' `humanoidArmor(...)` + `stacksTo(1)` registration exactly: `han_solo_helmet`, `han_solo_chestplate`, `han_solo_leggings`, `han_solo_boots`. Display names: "Han Solo's Helmet/Chestplate/Leggings/Boots". Creative tab entries after the landspeeder. Whether netherite-material armor items additionally need the fire-resistant item property in this version: check how vanilla netherite armor items get it (item property vs material) in decompiled sources and mirror — if it is an item-properties flag, include it (netherite parity means surviving lava like netherite does).

## 4. Set bonus — Scoundrel's Luck

`ScoundrelLuck` in `faction/` (sibling of `Disguise`, same shape):

- `isWearingFullHanSoloSet(LivingEntity)` — four-slot `getItemBySlot(...).is(...)` check mirroring `Disguise.isWearingFullStormtrooperSet` verbatim with the new items.
- A static server-side `Map<UUID, QuickdrawState>` (plain `HashMap`, access only from the server thread in `use()`): `stateFor(UUID playerId)` get-or-create. Transient by design — restart/logout clears ambush memory (documented; no cleanup listener needed beyond acceptance that entries are tiny and bounded by players-ever-seen; a `clear(UUID)` hook on player logout via the existing event-subscriber pattern keeps the map tidy and is included).
- **Reuses `QuickdrawState` as-is** (Task-1-tested single-target memory; only `canAmbush`/`markAmbushed` are used — the windup methods stay unused on the player path since player shots are instant).

**Hook** in `BlasterPistolItem.use()` (covers the rifle too — `BlasterRifleItem extends BlasterPistolItem` and inherits `use()`, only overriding `getDamage()`/`getCooldownTicks()`): at the existing hit-resolution site, replace the flat `this.getDamage()` with:

- if the shooter wears the full set (`ScoundrelLuck.isWearingFullHanSoloSet(player)`) and `state.canAmbush(target.getUUID())`: damage = `2 * this.getDamage()`, then `state.markAmbushed(target.getUUID())`, and send a `ParticleTypes.CRIT` burst at the target (server-side `sendParticles`, ~8 particles) as the ambush cue.
- otherwise: unchanged flat `this.getDamage()`.
- Misses do not consume the ambush (mark only on a landed hit). Removing any piece simply makes the check false; re-equipping resumes with the same memory (accepted).

Semantics match Han's goal: one ambush per continuously-tracked target, switching targets re-arms (including back-and-forth — the single-field memory is the spec'd behavior, not a bug).

## 5. Recipes

Data-generated shaped recipes (`ModRecipeProvider`, `RecipeCategory.COMBAT`, `unlockedBy("has_netherite_ingot", this.has(Items.NETHERITE_INGOT))`), leather body + one netherite ingot each — stormtrooper shapes with the center-most slot upgraded:

| Piece | Pattern | Materials |
|---|---|---|
| Helmet | `LNL` / `L L` | 4 leather + 1 netherite ingot |
| Chestplate | `L L` / `LNL` / `LLL` | 7 leather + 1 netherite ingot |
| Leggings | `LNL` / `L L` / `L L` | 6 leather + 1 netherite ingot |
| Boots | `N L` / `L L` | 3 leather + 1 netherite ingot |

`L` = `minecraft:leather`, `N` = `minecraft:netherite_ingot`. Full set: 4 ingots + 20 leather.

## 6. Art (art-gated)

- **Worn layers:** `assets/starwars/equipment/han_solo.json` mirroring `equipment/stormtrooper.json` (humanoid + humanoid_leggings layers → `starwars:han_solo`); two 64×32-convention worn-layer PNGs at `textures/entity/equipment/humanoid/han_solo.png` and `.../humanoid_leggings/han_solo.png`. Locate the tool that generated the stormtrooper worn layers first (grep `tools/` for "equipment" / "humanoid_leggings"); extend it. If those layers turn out to be produced by a script section without a per-mob dispatch — or hand-committed — add a Han painter following `gen_textures.py`'s structure and record which it was. Paint: dark-brown cap with band highlight (helmet region), black vest over off-white shirt with the open-front column (chest + arms), navy trousers with the `0xB03030` bloodstripe seam (leggings layer), brown boots with sole shadow (feet). 3+ tones per region; palettes reuse the `HAN_*` constants already in `gen_textures.py` where they fit.
- **bbmodels:** four `han_solo_armor_*.bbmodel` files via the existing `ARMOR_PIECES`/`write_armor_bbmodel` pipeline (stormtrooper-armor cube tables reused with the Han palette texture), committed and editable.
- **Item sprites:** four 16×16 sprites via `gen_item_textures.py` (cap, vest-over-shirt chest silhouette, trousers with bloodstripe, boots).
- Tool runs must leave all sibling generated files byte-identical.

## 7. Bedrock

The armor pieces flow through the existing item/recipe/attachable pipeline exactly as the stormtrooper set did (worn-armor attachables with the armor bbmodels, item icons, shaped recipes). Scoundrel's Luck is server-side Java item logic with no Bedrock equivalent → one honest `UNTRANSLATABLE.md` entry via the existing item-custom-behavior recorder (`recordItemCustomBehavior`), noting the set bonus is absent on Bedrock. Regenerate `bedrock-out/starwars` only.

## 8. Testing

- `ScoundrelLuckStateTest` (pure): the `UUID → QuickdrawState` map wrapper — get-or-create returns the same instance per player, distinct per player; `clear(UUID)` drops the entry; ambush semantics per player are independent. (The four-slot equipment check and the `use()` hook are engine-coupled — deferred to smoke.)
- `QuickdrawState` already covered (Task 1 of the previous plan).
- Datagen byte-deterministic (recipes/lang); translator suite green after the UNTRANSLATABLE recorder call; regen deterministic.
- Smoke checklist additions: full-set double-damage first shot with crit burst (pistol and rifle), no bonus at 3/4 pieces, netherite-grade protection feel, worn-layer look on the player model, Bedrock attachables.

## 9. New-file map

```
starwars/src/main/java/com/tweeks/starwars/
  item/HanSoloArmorMaterials.java
  faction/ScoundrelLuck.java
Modified: Registration, BlasterPistolItem (hook), ModRecipeProvider,
          ModLanguageProvider, tools/gen_* scripts,
          assets/starwars/equipment/han_solo.json (new asset JSON)
Tests: faction/ScoundrelLuckStateTest.java
Generated: 4 bbmodels, 2 worn-layer PNGs, 4 item sprites,
           datagen recipes/lang, bedrock-out/starwars regen
```
