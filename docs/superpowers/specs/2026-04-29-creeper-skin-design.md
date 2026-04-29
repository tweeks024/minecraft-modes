# Creeper Skin Armor — Design Spec

**Date:** 2026-04-29
**Status:** Design awaiting user review; no plan yet.
**Predecessors:** none. New sibling module; no shared code reuse from `securitycore` required.

## Goal

Add a wearable 4-piece "Creeper Skin" armor set in a new `:creeperskin` module:

1. **Cosmetic.** Player rendered with creeper-green armor on every body region; helmet shows the iconic creeper face. Pure visual swap of standard armor textures — no model replacement.
2. **Stats.** Netherite-tier protection: helmet 3, chest 8, leggings 6, boots 3 (20 armor total); toughness 3.0 per piece; knockback resistance 0.1 per piece; fire-resistant. Full netherite-equivalent durability and enchantability.
3. **Set bonus (full 4-piece worn):**
   - Real Creeper mobs **do not target** the wearer (no path/no fuse).
   - Wearer takes **zero damage** from any creeper explosion (charged or normal).
4. **Acquisition.** Items show up in a `Creeper Skin` creative tab and can be `/give`n. **No survival recipe in this iteration.**

## Non-goals

- No survival crafting recipe, smithing template, or loot drop.
- No active disguise behavior beyond the two listed bonuses (no hiss, no detonation, no scaring villagers, no charged-creeper hostility flip).
- No PvP balance pass — partial set wearers behave as plain netherite-stat armor with no bonus.
- No mob-head slot interaction (the helmet is real armor, not a `MobHead`).
- No interaction with the existing `:securityguard` or `:thief` modules. No shared registry types.
- No model replacement for the player while worn — the player keeps the player skeleton; only armor textures change.

## Architecture

### Module placement

| Component | Module | Reason |
|---|---|---|
| Whole feature | `:creeperskin` (new) | Self-contained cosmetic armor; no shared primitives needed in `securitycore`. Matches existing pattern (`:thief`, `:securityguard` are sibling feature modules). |

### New module scaffold

Mirror `:thief` exactly, since it's the lightest existing module:

- `settings.gradle` — add `include 'creeperskin'`
- `creeperskin/build.gradle` — copy from `thief/build.gradle` verbatim, no edits needed (it reads `mod_id` from the per-module `gradle.properties`).
- `creeperskin/gradle.properties`:
  ```properties
  org.gradle.jvmargs=-Xmx2G
  org.gradle.daemon=true
  org.gradle.parallel=true
  org.gradle.caching=true
  org.gradle.configuration-cache=true
  mod_id=creeperskin
  mod_name=Creeper Skin
  mod_license=MIT
  mod_version=0.1.0
  mod_group_id=com.tweeks.creeperskin
  ```
- `creeperskin/src/main/templates/META-INF/neoforge.mods.toml` — copy `:thief`'s template; rename mod metadata.
- `creeperskin/src/main/java/com/tweeks/creeperskin/CreeperSkinMod.java` — entry point with `@Mod("creeperskin")`, calls `Registration.register(modEventBus)` and `NeoForge.EVENT_BUS.register(SetBonusHandler.class)`.

### Components

#### 1. `CreeperArmorMaterial` — the shared armor material

**Location:** `creeperskin/src/main/java/com/tweeks/creeperskin/item/CreeperArmorMaterial.java`

A static factory exposing a single `Holder<ArmorMaterial>` (or whatever the registered-holder API in this NeoForge version expects). The material declares:

- **`durability` per slot** — match netherite: HEAD 407, CHEST 592, LEGS 555, FEET 481 (i.e. base × `ArmorItem.HEALTH_PER_SLOT[slot]`, base = 37 like vanilla netherite).
- **`defense` per slot** — `{ feet: 3, legs: 6, chest: 8, head: 3 }`.
- **`enchantmentValue`** — 15 (netherite parity).
- **`equipSound`** — `SoundEvents.ARMOR_EQUIP_NETHERITE` (or the equivalent registered sound key in this NeoForge version).
- **`repairIngredient`** — `Ingredient.of(Items.NETHERITE_INGOT)`. Even without a recipe in this iteration, an anvil-repair path on `NETHERITE_INGOT` keeps it consistent and is one extra line.
- **`toughness`** — 3.0.
- **`knockbackResistance`** — 0.1.
- **`assetId`** / equipment-model resource location — `creeperskin:creeper` (drives texture lookup; see Textures section).

**Approach (verify exact API at implementation time):** Two equally-valid paths in current NeoForge:

- **Option A — vanilla `ArmorMaterials.NETHERITE` clone via `Holder` registration.** Build an `ArmorMaterial` with the values above and register it under `Registries.ARMOR_MATERIAL` with key `creeperskin:creeper`. Items reference the holder when constructed.
- **Option B — pure data class.** If this NeoForge version still uses an enum-style `ArmorMaterial` interface (no registry), implement the interface directly as a singleton.

**Decision rule at implementation:** First try Option A. If `Registries.ARMOR_MATERIAL` doesn't exist in this version, fall back to Option B. End-user behavior is identical.

#### 2. `CreeperArmorItem` — armor item class (or just `ArmorItem` directly)

**Location:** `creeperskin/src/main/java/com/tweeks/creeperskin/item/CreeperArmorItem.java` *(only if a custom subclass is needed)*

If vanilla `ArmorItem` is sufficient, **no custom class** — just register four `DeferredItem<ArmorItem>` entries with `ArmorItem::new` and the appropriate `Item.Properties`. The set-bonus logic lives in an event handler, not on the item, so subclassing is unnecessary.

We commit to **registering with vanilla `ArmorItem`** unless the implementation discovers a per-item hook is needed. If it is, this section is replaced by a thin `CreeperArmorItem extends ArmorItem` with no overridden behavior beyond construction.

Each `Item.Properties` chain:
- `.durability(<slot durability from material>)` (or rely on `ArmorItem` constructor to derive from material)
- `.fireResistant()` — netherite parity
- `.stacksTo(1)` — armor stacks of 1 (`ArmorItem` default; explicit if needed)

#### 3. `Registration.java` — register items, set bonus, creative tab

**Location:** `creeperskin/src/main/java/com/tweeks/creeperskin/Registration.java`

Pattern is a near-clone of `thief/Registration.java`:

```java
public static final DeferredRegister.Items ITEMS =
    DeferredRegister.createItems(CreeperSkinMod.MOD_ID);

public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
    DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreeperSkinMod.MOD_ID);

public static final DeferredItem<ArmorItem> CREEPER_HELMET   = ITEMS.registerItem("creeper_helmet",
    p -> new ArmorItem(CreeperArmorMaterial.MATERIAL, ArmorItem.Type.HELMET, p),
    p -> p.fireResistant().stacksTo(1));
public static final DeferredItem<ArmorItem> CREEPER_CHESTPLATE = ITEMS.registerItem("creeper_chestplate",
    p -> new ArmorItem(CreeperArmorMaterial.MATERIAL, ArmorItem.Type.CHESTPLATE, p),
    p -> p.fireResistant().stacksTo(1));
public static final DeferredItem<ArmorItem> CREEPER_LEGGINGS = ITEMS.registerItem("creeper_leggings",
    p -> new ArmorItem(CreeperArmorMaterial.MATERIAL, ArmorItem.Type.LEGGINGS, p),
    p -> p.fireResistant().stacksTo(1));
public static final DeferredItem<ArmorItem> CREEPER_BOOTS = ITEMS.registerItem("creeper_boots",
    p -> new ArmorItem(CreeperArmorMaterial.MATERIAL, ArmorItem.Type.BOOTS, p),
    p -> p.fireResistant().stacksTo(1));

public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREEPER_TAB =
    CREATIVE_TABS.register("main", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + CreeperSkinMod.MOD_ID))
            .icon(() -> CREEPER_HELMET.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(CREEPER_HELMET.get());
                output.accept(CREEPER_CHESTPLATE.get());
                output.accept(CREEPER_LEGGINGS.get());
                output.accept(CREEPER_BOOTS.get());
            })
            .build());

public static void register(IEventBus modEventBus) {
    ITEMS.register(modEventBus);
    CREATIVE_TABS.register(modEventBus);
}
```

**Verify at implementation:** exact `ArmorItem` constructor signature in this NeoForge version (some recent versions take `Holder<ArmorMaterial>` directly; others take a `Type` plus a properties chain). Adjust the lambdas accordingly. The set of registered items, their IDs, and the creative-tab wiring don't change.

#### 4. `SetBonusHandler` — event-driven 4-piece bonus

**Location:** `creeperskin/src/main/java/com/tweeks/creeperskin/SetBonusHandler.java`

Static, `@EventBusSubscriber` style class registered to `NeoForge.EVENT_BUS` (game event bus, not mod bus). Implements two handlers and one helper:

```java
public final class SetBonusHandler {
    private SetBonusHandler() {}

    /** True iff entity has all four creeper-skin pieces equipped in their armor slots. */
    public static boolean isWearingFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(Registration.CREEPER_HELMET.get())
            && entity.getItemBySlot(EquipmentSlot.CHEST).is(Registration.CREEPER_CHESTPLATE.get())
            && entity.getItemBySlot(EquipmentSlot.LEGS).is(Registration.CREEPER_LEGGINGS.get())
            && entity.getItemBySlot(EquipmentSlot.FEET).is(Registration.CREEPER_BOOTS.get());
    }

    /** Bonus 1: Creepers stop targeting full-set wearers. */
    @SubscribeEvent
    public static void onCreeperTargetChange(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Creeper)) return;
        LivingEntity newTarget = event.getNewTarget();
        if (newTarget != null && isWearingFullSet(newTarget)) {
            event.setNewTarget(null);   // or event.setCanceled(true) — see verification
        }
    }

    /** Bonus 2: Full-set wearer takes no damage from any creeper explosion. */
    @SubscribeEvent
    public static void onIncomingExplosionDamage(LivingIncomingDamageEvent event) {
        DamageSource src = event.getSource();
        if (!src.is(DamageTypeTags.IS_EXPLOSION)) return;
        if (!(src.getEntity() instanceof Creeper)) return;
        if (isWearingFullSet(event.getEntity())) {
            event.setCanceled(true);    // zero damage applied
        }
    }
}
```

**Verify at implementation:** exact event class names in this NeoForge version (`LivingChangeTargetEvent`, `LivingIncomingDamageEvent`) and whether the cancel API is `setCanceled(true)` or `setNewTarget(null)`. The handler's *intent* — drop creeper targeting + zero out creeper-explosion damage on full-set wearers — does not change.

**Why event-driven, not item-side?** The two bonuses are observable on entities other than the wearer (a creeper deciding what to attack, a damage source resolving against a wearer). Hooking them on the item would require either an `Equipable` per-tick callback (per-piece, would fire 4× and need de-duping) or a per-target ticker. Both are messier than two small event handlers.

#### 5. Equipment textures (worn-armor visuals)

NeoForge's vanilla armor-texture path uses the equipment-model resource location declared on the `ArmorMaterial`. With `assetId = creeperskin:creeper`:

- **Body texture (helmet, chestplate, boots layer):**
  `creeperskin/src/main/resources/assets/creeperskin/textures/entity/equipment/humanoid/creeper.png`
- **Legs texture (leggings layer):**
  `creeperskin/src/main/resources/assets/creeperskin/textures/entity/equipment/humanoid/creeper_leggings.png` *(or `_legs.png` — verify exact suffix)*

**Verify at implementation:** the exact filename suffix for the leggings layer (`_legs`, `_leggings`, or `_layer_2`) and whether this NeoForge version requires an `equipment/<id>.json` asset definition pointing at these PNGs. If JSON is required, add `creeperskin/src/main/resources/assets/creeperskin/equipment/creeper.json` with the standard `{ "layers": { "humanoid": [{ "texture": "creeperskin:creeper" }], "humanoid_leggings": [{ "texture": "creeperskin:creeper" }] } }` shape — adjust to whatever schema this version uses.

**Texture content (palette, locked here; pixel-by-pixel finalised at implementation):**

- **Body PNG (humanoid layer):** vanilla armor UV layout. Solid creeper-green base (`#0DA70D`), with a single-pixel horizontal seam darker (`#0A8D0A`) along the model joint lines for definition. The chest and helmet share this layer; the helmet face region (front of head, the ~8×8 area on the standard humanoid armor UV) gets the **iconic creeper face**: two square eyes (`#0F0F0F`), open mouth in inverted-T shape (`#0F0F0F`). Use the standard creeper face proportions from `assets/minecraft/textures/entity/creeper/creeper.png`, scaled to the armor UV's head-front region.
- **Leggings PNG:** same green base, no face, no seam highlights — uniform creeper green across the leg UV.

Palette:
- Body green: `#0DA70D`
- Seam shadow: `#0A8D0A`
- Creeper face black: `#0F0F0F`

#### 6. Inventory icon textures (4 PNGs)

`creeperskin/src/main/resources/assets/creeperskin/textures/item/`:
- `creeper_helmet.png` — 16×16, helmet silhouette in `#0DA70D` with creeper face on the front-facing portion.
- `creeper_chestplate.png` — 16×16, chestplate silhouette in `#0DA70D`.
- `creeper_leggings.png` — 16×16, leggings silhouette in `#0DA70D`.
- `creeper_boots.png` — 16×16, boots silhouette in `#0DA70D`.

Use the vanilla diamond/iron/netherite armor icon shapes as silhouettes; recolor to the creeper palette. Keep the helmet-face detail readable at 16×16 (large pixel eyes/mouth).

#### 7. Item model + asset JSONs

Mirror the `:thief` `blackjack` pattern:

- `creeperskin/src/main/resources/assets/creeperskin/items/<id>.json`:
  ```json
  { "model": { "type": "minecraft:model", "model": "creeperskin:item/<id>" } }
  ```
- `creeperskin/src/main/resources/assets/creeperskin/models/item/<id>.json`:
  ```json
  { "parent": "minecraft:item/generated", "textures": { "layer0": "creeperskin:item/<id>" } }
  ```

Four pairs total (one per piece). `item/generated` parent (not `handheld`) since these aren't held tools.

#### 8. Lang via datagen

Add `ModLanguageProvider` (mirror `:securityguard`'s):

```
itemGroup.creeperskin → "Creeper Skin"
item.creeperskin.creeper_helmet      → "Creeper Helmet"
item.creeperskin.creeper_chestplate  → "Creeper Chestplate"
item.creeperskin.creeper_leggings    → "Creeper Leggings"
item.creeperskin.creeper_boots       → "Creeper Boots"
```

Run `:creeperskin:runData` after registration; commit generated JSONs.

## Data flow

### Wearing armor (cosmetic + stats path)
1. Player equips `creeper_*` items into the four armor slots.
2. Vanilla armor pipeline applies the `CreeperArmorMaterial` defense / toughness / knockback resistance values automatically.
3. Vanilla render pipeline draws the body texture from `assets/creeperskin/textures/entity/equipment/humanoid/creeper.png` (and the leggings counterpart) onto the standard humanoid armor model — no client-side renderer code needed.

### Set bonus 1 — creepers don't target wearer
1. A `Creeper` ticks its targeting AI; vanilla `NearestAttackableTargetGoal` selects a nearby player.
2. NeoForge fires `LivingChangeTargetEvent` with `entity = creeper`, `newTarget = player`.
3. `SetBonusHandler.onCreeperTargetChange` runs: confirms `entity instanceof Creeper`, calls `isWearingFullSet(newTarget)`, nulls out the new target.
4. Creeper proceeds with no target this tick; on the next pathfinding tick it picks a different target or wanders.

### Set bonus 2 — zero creeper-explosion damage
1. Creeper detonates near full-set wearer.
2. Vanilla explosion damages every entity in radius via `DamageSource` with `IS_EXPLOSION` tag and `getEntity() == creeper`.
3. NeoForge fires `LivingIncomingDamageEvent` with that source on the wearer.
4. `SetBonusHandler.onIncomingExplosionDamage` cancels the event — wearer takes 0 damage. Knockback and other side effects (item damage from blast wave, world block damage) are unaffected; only the entity damage to the wearer is suppressed.

## Testing strategy

### Unit tests (`:creeperskin:test`)

Add `SetBonusHandlerTest`:
- `isWearingFullSet` returns `true` when all four registered items occupy the right slots; `false` if any slot is empty, has the wrong piece, or has a vanilla armor piece.
- Mock `LivingChangeTargetEvent`: handler nulls the target only when `entity instanceof Creeper` AND the new target wears the full set; otherwise leaves event untouched.
- Mock `LivingIncomingDamageEvent`: handler cancels only when source is creeper-tagged explosion AND entity wears the full set.

If mocking Minecraft entity classes proves heavyweight, fall back to a smoke-test that constructs the handler calls without throwing — manual smoke-test in dev client covers behavior.

### Manual smoke test (dev client)

1. `./gradlew :creeperskin:runClient`. Both `Security Core` (transitively if pulled by another module — *not* expected here) and `Creeper Skin` should load. Confirm `Creeper Skin` mod appears in the mods screen.
2. Open creative inventory → `Creeper Skin` tab → confirm 4 items: helmet, chestplate, leggings, boots; each icon shows creeper green; helmet shows the face.
3. Equip all four pieces. Verify in third-person view that the player body is creeper-green head to toe, and the helmet shows the creeper face.
4. Verify armor HUD: 10 full armor icons (20 points) — netherite parity.
5. Take fall damage / fire damage to confirm fire-resistant + standard armor mitigation work.
6. Spawn a creeper near the player. Confirm it does *not* approach to fuse — wanders or targets a nearby cow/villager instead.
7. Remove the helmet. Spawn a creeper. Confirm it now targets the player normally (set bonus is gated on full set).
8. Re-equip helmet. Spawn a creeper, force-detonate (use `/summon creeper ~ ~ ~ {ignited:1b,Fuse:1}` or equivalent). Confirm wearer takes 0 damage. Spawn a charged creeper next to the wearer and detonate; confirm 0 damage.
9. Check that *non-creeper* explosions still damage the wearer (e.g. TNT) — bonus is creeper-specific.

### Datagen check

Run `:creeperskin:runData` after registration changes; commit any new generated files.

## Files

### New files

```
settings.gradle                                                          (modified — add include)
creeperskin/build.gradle
creeperskin/gradle.properties
creeperskin/src/main/templates/META-INF/neoforge.mods.toml
creeperskin/src/main/java/com/tweeks/creeperskin/CreeperSkinMod.java
creeperskin/src/main/java/com/tweeks/creeperskin/Registration.java
creeperskin/src/main/java/com/tweeks/creeperskin/SetBonusHandler.java
creeperskin/src/main/java/com/tweeks/creeperskin/item/CreeperArmorMaterial.java
creeperskin/src/main/java/com/tweeks/creeperskin/data/ModLanguageProvider.java
creeperskin/src/main/resources/assets/creeperskin/items/creeper_helmet.json
creeperskin/src/main/resources/assets/creeperskin/items/creeper_chestplate.json
creeperskin/src/main/resources/assets/creeperskin/items/creeper_leggings.json
creeperskin/src/main/resources/assets/creeperskin/items/creeper_boots.json
creeperskin/src/main/resources/assets/creeperskin/models/item/creeper_helmet.json
creeperskin/src/main/resources/assets/creeperskin/models/item/creeper_chestplate.json
creeperskin/src/main/resources/assets/creeperskin/models/item/creeper_leggings.json
creeperskin/src/main/resources/assets/creeperskin/models/item/creeper_boots.json
creeperskin/src/main/resources/assets/creeperskin/textures/item/creeper_helmet.png
creeperskin/src/main/resources/assets/creeperskin/textures/item/creeper_chestplate.png
creeperskin/src/main/resources/assets/creeperskin/textures/item/creeper_leggings.png
creeperskin/src/main/resources/assets/creeperskin/textures/item/creeper_boots.png
creeperskin/src/main/resources/assets/creeperskin/textures/entity/equipment/humanoid/creeper.png
creeperskin/src/main/resources/assets/creeperskin/textures/entity/equipment/humanoid/creeper_leggings.png
creeperskin/src/test/java/com/tweeks/creeperskin/SetBonusHandlerTest.java   (best-effort)
```

If this NeoForge version requires an equipment-model JSON, also:
```
creeperskin/src/main/resources/assets/creeperskin/equipment/creeper.json
```

### Modified files

```
settings.gradle
    — add `include 'creeperskin'`

todo.md
    — strike "creeper skin" once shipped (separate commit, optional)
```

### Deleted files

None.

## Open questions / verification at implementation

1. **Exact NeoForge `ArmorItem` constructor and material registry** for version `26.1.2.30-beta`. Two paths (registered `Holder<ArmorMaterial>` vs. enum-style singleton) are spec'd; implementation picks whichever the version supports. End-user behavior is identical.
2. **Equipment-texture filename suffix for the leggings layer** (`_legs`, `_leggings`, or `_layer_2`). Implementation greps the vanilla netherite assets in this version's deobf jar to confirm the exact suffix and whether an `equipment/<id>.json` is required.
3. **Event class names** for target-change and incoming-damage in this NeoForge version. The spec assumes `LivingChangeTargetEvent` and `LivingIncomingDamageEvent`; confirm and adjust the import and `setCanceled` semantics if the API drifted.
4. **Pixel-by-pixel texture details.** Palette and layout zones are spec'd; final pixel placement is decided at implementation while inspecting the result in the dev client.

These are minor open points whose resolution doesn't change the design. They're flagged so the implementer doesn't get stuck if a method signature drifted.

## Self-review notes

- **Single source of truth for set membership:** `SetBonusHandler.isWearingFullSet` is the only place that defines "full set" — both bonus paths call it.
- **Module placement matches existing convention:** sibling-feature module pattern (`thief`, `securityguard`); no shared primitive justifies adding to `securitycore`.
- **Bonuses gated on the *full* set, not partial wear** — locked in `isWearingFullSet`; partial-set wearers get netherite-stat armor and nothing else.
- **No retroactive changes to other modules.** No edits to `securitycore`, `securityguard`, or `thief`.
- **Scope sized for one PR:** new module, four registered items, one event handler, one material, datagen lang, asset textures. No cross-cutting refactor.
- **Numeric stats cited only here** (defense `3/8/6/3`, toughness `3.0`, KB `0.1`, durability base `37`) — no second source of these numbers exists in the spec, so they can't drift.
- **No survival recipe** — explicitly deferred; spec calls out that adding one later is a pure data addition.
- **Version-agnostic API references** — all NeoForge API mentions flagged "verify at implementation"; no hard assumption on a specific Minecraft version beyond what's in `gradle.properties`.
