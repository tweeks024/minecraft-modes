# Craftee Skin Armor — Design Spec

**Date:** 2026-04-30
**Status:** Design awaiting user review; no plan yet.
**Predecessors:** `docs/superpowers/specs/2026-04-29-creeper-skin-design.md`. The `:creeperskin` module is the structural template for this mod (sibling-feature pattern, four-piece netherite-tier set + event-driven full-set bonus + datagen lang). No code is shared at runtime; the Craftee mod is fully independent.

## Goal

Add a wearable 4-piece "Craftee" armor set in a new `:craftee` module:

1. **Cosmetic.** Black armor with an orange filmstrip stripe down each piece — a content-creator visual motif. Pure texture swap on the standard humanoid armor model; no model replacement.
2. **Stats.** Netherite-tier protection: helmet 3, chest 8, leggings 6, boots 3 (20 armor total); toughness 3.0 per piece; knockback resistance 0.1 per piece; fire-resistant. Full netherite-equivalent durability and enchantability.
3. **Set bonus (full 4-piece worn).** A "content creator mobility" package, applied as three persistent attribute modifiers that turn on/off as the player enters/leaves the full-set state:
   - **Movement speed:** +20% (`Attributes.MOVEMENT_SPEED`, `MULTIPLY_BASE`, `+0.20`).
   - **Jump strength:** +0.30 (`Attributes.JUMP_STRENGTH`, `ADD_VALUE`).
   - **Step height:** +0.50 (`Attributes.STEP_HEIGHT`, `ADD_VALUE`) — clears full 1-block obstacles automatically.
4. **Acquisition.**
   - **Smithing-template recipe** in the smithing table: `craftee_upgrade_smithing_template` + diamond armor piece + 1 netherite ingot → matching craftee armor piece. Pattern mirrors vanilla's netherite upgrade.
   - **Template recipe** in the crafting table: 1 netherite scrap centre + 8 paper ringing it → 2 craftee_upgrade_smithing_template. Mirrors vanilla's "duplicate template" recipe shape with paper instead of diamond — the "creator" motif. Unlike vanilla, which requires owning a template to duplicate, this recipe creates the first template from scrap, so no bastion loot is needed to bootstrap.
   - Items also appear in a `Craftee` creative tab and can be `/give`n.

## Non-goals

- **No new mob, structure, or worldgen.** Template is craftable; no dungeon/loot drop wiring.
- **No partial-set bonus.** Wear all four or you get plain netherite-stat armor and nothing else. Locked in `SetBonusHandler.isWearingFullSet`.
- **No active abilities** (no double-jump, no dash button, no sprint trigger). Only the three passive attribute modifiers above.
- **No animation, particles, or sound on bonus activation.** Equip sound is the standard netherite equip sound.
- **No PvP balance pass.** The mod is balanced for survival single-player; PvP servers can disable via permission tools.
- **No mob-head slot interaction.** The helmet is real armor, not a `MobHead`.
- **No interaction with `:securityguard`, `:thief`, or `:creeperskin`.** No shared registry types, no cross-module references.

## Architecture

### Module placement

| Component | Module | Reason |
|---|---|---|
| Whole feature | `:craftee` (new) | Self-contained cosmetic-armor + smithing recipe; no shared primitive justifies adding to `:securitycore`. Matches existing pattern (`:creeperskin`, `:thief`, `:securityguard`). |

### New module scaffold

Mirror `:creeperskin` exactly — closest structural match (single armor set + set-bonus event handler + datagen lang).

- `settings.gradle` — add `include 'craftee'`.
- `craftee/build.gradle` — copy `creeperskin/build.gradle` verbatim. The build script reads `mod_id` from per-module `gradle.properties`, so no edits needed beyond the properties file.
- `craftee/gradle.properties`:
  ```properties
  org.gradle.jvmargs=-Xmx2G
  org.gradle.daemon=true
  org.gradle.parallel=true
  org.gradle.caching=true
  org.gradle.configuration-cache=true
  mod_id=craftee
  mod_name=Craftee
  mod_license=MIT
  mod_version=0.1.0
  mod_group_id=com.tweeks.craftee
  ```
- `craftee/src/main/templates/META-INF/neoforge.mods.toml` — clone `:creeperskin`'s template; rewrite `displayName` to `"Craftee"` and `description` to summarise the mobility set.
- `craftee/src/main/java/com/tweeks/craftee/CrafteeMod.java` — entry point with `@Mod("craftee")`; calls `Registration.register(modEventBus)` and `NeoForge.EVENT_BUS.register(SetBonusHandler.class)`.

### Components

#### 1. `CrafteeArmorMaterials` — armor material

**Location:** `craftee/src/main/java/com/tweeks/craftee/item/CrafteeArmorMaterials.java`

Direct port of `creeperskin/src/main/java/com/tweeks/creeperskin/item/CreeperArmorMaterials.java` with three identifier swaps:

- Asset key: `craftee:craftee` (resolves to `assets/craftee/equipment/craftee.json`).
- Constant name: `CRAFTEE` (replaces `CREEPER`).
- Mod-id references throughout: `CrafteeMod.MOD_ID`.

All numeric stats are identical to `:creeperskin`: durability base 37, defenses `{boots 3, legs 6, chest 8, helmet 3, body 19}`, enchantability 15, equip sound `SoundEvents.ARMOR_EQUIP_NETHERITE`, toughness 3.0, knockback resist 0.1, repair tag `ItemTags.REPAIRS_NETHERITE_ARMOR`. Centralising the numbers in this one file keeps a single source of truth per module.

#### 2. `Registration.java` — items, smithing template, creative tab

**Location:** `craftee/src/main/java/com/tweeks/craftee/Registration.java`

Mirrors `creeperskin/src/main/java/com/tweeks/creeperskin/Registration.java` with two additions: a fifth registered item (the smithing template) and the creative tab including it.

```java
public static final DeferredRegister.Items ITEMS =
    DeferredRegister.createItems(CrafteeMod.MOD_ID);

public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
    DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CrafteeMod.MOD_ID);

public static final DeferredItem<Item> CRAFTEE_HELMET = ITEMS.registerItem("craftee_helmet",
    Item::new,
    p -> p.humanoidArmor(CrafteeArmorMaterials.CRAFTEE, ArmorType.HELMET).fireResistant().stacksTo(1));
public static final DeferredItem<Item> CRAFTEE_CHESTPLATE = ITEMS.registerItem("craftee_chestplate", /* CHESTPLATE */ ...);
public static final DeferredItem<Item> CRAFTEE_LEGGINGS  = ITEMS.registerItem("craftee_leggings",  /* LEGGINGS  */ ...);
public static final DeferredItem<Item> CRAFTEE_BOOTS     = ITEMS.registerItem("craftee_boots",     /* BOOTS     */ ...);

/** Smithing template item. Registered with vanilla SmithingTemplateItem behaviour
 *  (rarity, tooltip lines for "applies to" / "ingredient" / "upgrade"). */
public static final DeferredItem<SmithingTemplateItem> CRAFTEE_UPGRADE_SMITHING_TEMPLATE =
    ITEMS.registerItem("craftee_upgrade_smithing_template",
        p -> SmithingTemplateItem.createUpgradeTemplate("craftee_upgrade", p),
        p -> p);

public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CRAFTEE_TAB =
    CREATIVE_TABS.register("main", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + CrafteeMod.MOD_ID))
            .icon(() -> CRAFTEE_HELMET.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(CRAFTEE_HELMET.get());
                output.accept(CRAFTEE_CHESTPLATE.get());
                output.accept(CRAFTEE_LEGGINGS.get());
                output.accept(CRAFTEE_BOOTS.get());
                output.accept(CRAFTEE_UPGRADE_SMITHING_TEMPLATE.get());
            })
            .build());
```

**Verify at implementation:** the exact factory method on `SmithingTemplateItem` (NeoForge / vanilla MC sometimes ships either `createUpgradeTemplate(String)`, `createNetheriteUpgradeTemplate()`, or a constructor taking `Component`s for the tooltip lines). If the static factory is unavailable, build the template via the public constructor, supplying `Component.translatable` for the four tooltip strings (`upgrade`, `applies_to`, `ingredients`, `base_slot_description`, `additions_slot_description`). End-user behaviour — a netherite-style template that drives a `SmithingTransformRecipe` — does not change.

#### 3. `SetBonusHandler` — full-set attribute bonuses

**Location:** `craftee/src/main/java/com/tweeks/craftee/SetBonusHandler.java`

Static class registered to `NeoForge.EVENT_BUS` from the mod constructor. Single source of truth for "full set worn" via `isWearingFullSet`. Three attribute modifiers (speed, jump, step) are added when the wearer is in full-set state and removed otherwise; the handler checks state every player tick and only mutates when state actually changed (no per-tick attribute thrash).

```java
public final class SetBonusHandler {
    private SetBonusHandler() {}

    /** Stable IDs for the three modifiers. ResourceLocation-keyed so the
     *  modifier survives reload and is removable by id. */
    static final ResourceLocation SPEED_ID =
        ResourceLocation.fromNamespaceAndPath(CrafteeMod.MOD_ID, "set_bonus_speed");
    static final ResourceLocation JUMP_ID =
        ResourceLocation.fromNamespaceAndPath(CrafteeMod.MOD_ID, "set_bonus_jump");
    static final ResourceLocation STEP_ID =
        ResourceLocation.fromNamespaceAndPath(CrafteeMod.MOD_ID, "set_bonus_step");

    /** Modifier specs: amount + operation, paired with the attribute holder. */
    private static final AttributeModifier SPEED_MOD =
        new AttributeModifier(SPEED_ID, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final AttributeModifier JUMP_MOD =
        new AttributeModifier(JUMP_ID, 0.30, AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier STEP_MOD =
        new AttributeModifier(STEP_ID, 0.50, AttributeModifier.Operation.ADD_VALUE);

    public static boolean isWearingFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(Registration.CRAFTEE_HELMET.get())
            && entity.getItemBySlot(EquipmentSlot.CHEST).is(Registration.CRAFTEE_CHESTPLATE.get())
            && entity.getItemBySlot(EquipmentSlot.LEGS).is(Registration.CRAFTEE_LEGGINGS.get())
            && entity.getItemBySlot(EquipmentSlot.FEET).is(Registration.CRAFTEE_BOOTS.get());
    }

    /** Per-tick state sync. Adds modifiers when the wearer enters full-set
     *  state, removes them when they leave it. No-op when state is unchanged. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean fullSet = isWearingFullSet(player);
        syncModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MOD, fullSet);
        syncModifier(player, Attributes.JUMP_STRENGTH, JUMP_MOD, fullSet);
        syncModifier(player, Attributes.STEP_HEIGHT, STEP_MOD, fullSet);
    }

    private static void syncModifier(LivingEntity entity,
                                     Holder<Attribute> attribute,
                                     AttributeModifier mod,
                                     boolean shouldHave) {
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst == null) return;
        boolean has = inst.hasModifier(mod.id());
        if (shouldHave && !has) inst.addPermanentModifier(mod);
        else if (!shouldHave && has) inst.removeModifier(mod.id());
    }

    /** Strip our modifiers when armor item leaves the wearer's body
     *  (player death, container moves outside ticking). Defensive only —
     *  the per-tick path will catch most cases. */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        clearAll(player);
    }

    private static void clearAll(LivingEntity entity) {
        for (var pair : List.of(
                Map.entry(Attributes.MOVEMENT_SPEED, SPEED_ID),
                Map.entry(Attributes.JUMP_STRENGTH, JUMP_ID),
                Map.entry(Attributes.STEP_HEIGHT, STEP_ID))) {
            AttributeInstance inst = entity.getAttribute(pair.getKey());
            if (inst != null) inst.removeModifier(pair.getValue());
        }
    }
}
```

**Verify at implementation:**

- Exact event class for "every player tick after vanilla logic":  NeoForge ships `PlayerTickEvent.Post` in 1.21.x branches; in this 26.x version it may be the same or `LivingEvent.LivingTickEvent` filtered to `Player`. Either path is acceptable; both fire server-side per player.
- `AttributeModifier` constructor signature — recent versions take `(ResourceLocation, double, Operation)`; older took `(UUID, String, double, Operation)`. Spec assumes the `ResourceLocation` form.
- `AttributeInstance` API — `hasModifier(ResourceLocation)`, `addPermanentModifier(AttributeModifier)`, `removeModifier(ResourceLocation)` are stable in this branch.
- `MULTIPLY_BASE` operation name in this version (sometimes `ADD_MULTIPLIED_BASE`). Spec uses `ADD_MULTIPLIED_BASE`.

The handler's intent — "full-set wearer gets +20% speed / +0.30 jump / +0.50 step until they take any piece off" — does not change.

#### 4. Equipment textures (worn-armor visuals)

Vanilla armor-texture path. With asset key `craftee:craftee`:

- **Body:** `craftee/src/main/resources/assets/craftee/textures/entity/equipment/humanoid/craftee.png`
- **Leggings:** `craftee/src/main/resources/assets/craftee/textures/entity/equipment/humanoid_leggings/craftee.png`
- **Equipment JSON:** `craftee/src/main/resources/assets/craftee/equipment/craftee.json`:
  ```json
  {
    "layers": {
      "humanoid":          [{ "texture": "craftee:craftee" }],
      "humanoid_leggings": [{ "texture": "craftee:craftee" }]
    }
  }
  ```

**Texture content (palette + layout, locked here; exact pixels finalised at implementation):**

- **Body PNG (humanoid layer):** vanilla armor UV layout. Solid black base (`#0F0F0F`). One vertical orange filmstrip stripe (`#F08A1F`, 2 pixels wide) running down the centre-front of the chestplate UV and the centre-front of the helmet UV. Inside the orange stripe, evenly-spaced black "sprocket holes" — small 1×1 black squares at vertical intervals — give the filmstrip-perforation effect. Helmet has the same stripe motif on the front face panel.
- **Leggings PNG:** same black base; the filmstrip stripe runs down the centre-front of each leg UV (two parallel stripes total, mirroring the leg geometry). No sprocket holes on legs (at 1× UV scale they'd render as a single noisy pixel column — leave the legs as plain stripes).

**Palette:**
- Base black: `#0F0F0F`
- Filmstrip orange: `#F08A1F`
- Sprocket-hole black: `#0F0F0F` (same as base; produced by *not* painting orange in those pixels)

#### 5. Inventory icon textures (5 PNGs)

`craftee/src/main/resources/assets/craftee/textures/item/`:
- `craftee_helmet.png` — 16×16. Helmet silhouette, black base, single 1px-wide orange vertical stripe centred on the front face.
- `craftee_chestplate.png` — 16×16. Chestplate silhouette, black, single 1px orange vertical stripe centred.
- `craftee_leggings.png` — 16×16. Leggings silhouette, black, two 1px orange vertical stripes (one per leg).
- `craftee_boots.png` — 16×16. Boots silhouette, black, small 1px orange dash on the front of each boot.
- `craftee_upgrade_smithing_template.png` — 16×16. Vanilla netherite-template silhouette (the diamond-shape outline), recoloured: black plate background, orange diamond ring, with a single white "C" marker in the centre slot. (The "C" is the only place we use non-orange colour, signalling the template's craftee theme without copying the channel logo.)

#### 6. Item model + asset JSONs

Mirror `:creeperskin` — vanilla `item/generated` parent for all five.

- `craftee/src/main/resources/assets/craftee/items/<id>.json`:
  ```json
  { "model": { "type": "minecraft:model", "model": "craftee:item/<id>" } }
  ```
- `craftee/src/main/resources/assets/craftee/models/item/<id>.json`:
  ```json
  { "parent": "minecraft:item/generated", "textures": { "layer0": "craftee:item/<id>" } }
  ```

Five pairs total (one per piece + template).

#### 7. Datagen — recipes and lang

**Location:** `craftee/src/main/java/com/tweeks/craftee/data/`

- `DataGenerators.java` — registers both providers, mirroring `:creeperskin`'s.
- `ModRecipeProvider.java` — generates two recipes:
  1. **Smithing transform recipe**, four entries (one per armor slot). Use `SmithingTransformRecipeBuilder.smithing(template, base, addition, category, result)`:
     - `template` = `Ingredient.of(CRAFTEE_UPGRADE_SMITHING_TEMPLATE)`
     - `base` = `Ingredient.of(Items.DIAMOND_HELMET)` *(and chest/legs/boots for the other three)*
     - `addition` = `Ingredient.of(Items.NETHERITE_INGOT)`
     - `category` = `RecipeCategory.COMBAT`
     - `result` = `CRAFTEE_HELMET.get()` *(etc.)*
     - Each call writes a recipe ID like `craftee:craftee_helmet_smithing`.
  2. **Template-craft shaped recipe** (regular crafting table):
     - Pattern:
       ```
       PPP
       PNP
       PPP
       ```
       Where `N` = `Items.NETHERITE_SCRAP`, `P` = `Items.PAPER`. Total inputs: 1 netherite scrap + 8 paper. Result: 2× `CRAFTEE_UPGRADE_SMITHING_TEMPLATE`.
     - Mirrors vanilla's "duplicate netherite_upgrade_smithing_template" recipe shape (centre ingredient ringed by 8 surround items), with diamond → paper for the creator/filmstrip motif. Unlike vanilla — which requires owning a template to duplicate — this recipe creates the first template from netherite scrap, so the player doesn't need bastion loot to bootstrap.
- `ModLanguageProvider.java` — port of `:creeperskin`'s. Item-name keys are stable; the smithing-template tooltip keys depend on the exact strings produced by `SmithingTemplateItem.createUpgradeTemplate` (or whatever factory we use) — at implementation time, run `:craftee:runClient`, observe the placeholder tooltip lines (e.g. `item.craftee.smithing_template.craftee_upgrade.applies_to`), and add a translation for each. Initial set:
  ```
  itemGroup.craftee                                    → "Craftee"
  item.craftee.craftee_helmet                          → "Craftee Helmet"
  item.craftee.craftee_chestplate                      → "Craftee Chestplate"
  item.craftee.craftee_leggings                        → "Craftee Leggings"
  item.craftee.craftee_boots                           → "Craftee Boots"
  item.craftee.craftee_upgrade_smithing_template       → "Craftee Upgrade Smithing Template"
  ```
  Add tooltip-line keys (`*.applies_to`, `*.ingredients`, `*.base_slot_description`, `*.additions_slot_description`, `upgrade.*`) once their exact paths are observed in the running client.

Run `:craftee:runData` after registration; commit generated JSONs.

## Data flow

### Wearing armor (cosmetic + stats path)
1. Player equips `craftee_*` items into the four armor slots.
2. Vanilla armor pipeline applies `CrafteeArmorMaterials.CRAFTEE` defense / toughness / knockback resistance values.
3. Vanilla render pipeline draws the body and leggings textures from `assets/craftee/textures/entity/equipment/humanoid{,_leggings}/craftee.png` onto the standard humanoid armor model.

### Set bonus — entering / leaving full-set state
1. Every server tick, NeoForge fires `PlayerTickEvent.Post` for each connected player.
2. `SetBonusHandler.onPlayerTick` evaluates `isWearingFullSet(player)`.
3. For each of the three target attributes (`MOVEMENT_SPEED`, `JUMP_STRENGTH`, `STEP_HEIGHT`):
   - If `fullSet` is true and the modifier's `ResourceLocation` is not present on the attribute instance → `addPermanentModifier(...)`.
   - If `fullSet` is false and the modifier *is* present → `removeModifier(id)`.
   - Else → no-op.
4. Vanilla movement / jump / step-height code reads the modified attribute values on its next tick and applies them naturally.

### Smithing-table recipe
1. Player places `craftee_upgrade_smithing_template` in template slot, diamond armor piece in base slot, netherite ingot in addition slot.
2. Vanilla smithing UI matches against the registered `SmithingTransformRecipe` for that combination, produces the matching `craftee_*` armor in the output slot.
3. Taking the output consumes one of each input — the template is consumed (vanilla netherite-template behaviour), so additional armor pieces require additional templates.

### Template recipe (crafting table)
1. Player arranges 1 netherite scrap (centre slot) ringed by 8 paper in a 3×3 crafting grid. Total: 1 scrap + 8 paper → 2 templates.
2. Vanilla crafting matcher resolves the shaped recipe and outputs 2× `craftee_upgrade_smithing_template`.

## Testing strategy

### Unit tests (`:craftee:test`)

Add `SetBonusHandlerTest`, mirroring `:creeperskin/SetBonusHandlerTest.java`:

- `isWearingFullSet` returns `true` only when all four registered items occupy the right slots; `false` if any slot is empty, has the wrong piece, or has a vanilla armor piece.
- Verify modifier-id constants are stable: `SPEED_ID`, `JUMP_ID`, `STEP_ID` resolve to the expected namespace+path.
- The "state sync" path can't easily be unit-tested without booting Minecraft (it depends on `AttributeInstance`); cover via the manual smoke test below.

If mocking entity classes proves heavyweight, fall back to a smoke-test that constructs the handler calls without throwing — the manual smoke test in dev client covers the behaviour.

### Manual smoke test (dev client)

1. `./gradlew :craftee:runClient`. Confirm the `Craftee` mod appears on the mods screen and loads without errors.
2. Open creative inventory → `Craftee` tab → confirm five items: helmet, chestplate, leggings, boots, upgrade smithing template. Icons match the spec (black + orange filmstrip).
3. Equip all four pieces. In third-person view, the player is rendered black with an orange filmstrip stripe down the chest, helmet front, and each leg.
4. Verify armor HUD: 10 full armor icons (20 points) — netherite parity.
5. Sprint along a long surface; note the increased speed (`/attribute @s minecraft:movement_speed get` should show the modifier present and the resulting value boosted by 20%).
6. Jump straight up in place; note the increased peak height versus vanilla.
7. Walk into a 1-block step; player auto-steps over without jumping.
8. Remove one piece (e.g. helmet). Confirm the modifiers vanish: speed/jump/step values return to vanilla within one tick. Re-equip; modifiers return.
9. Open a smithing table. Place one upgrade template, one diamond helmet, one netherite ingot. Output is `craftee_helmet`. Repeat for chestplate, leggings, boots.
10. Open a crafting table. Lay out 1 netherite scrap centre, 8 paper around it. Output is 2× `craftee_upgrade_smithing_template`.
11. Verify a partial set (e.g. helmet only) gives plain netherite-stat armor with no movement / jump / step changes.
12. Disconnect from a single-player world while wearing the full set; reconnect; modifiers re-apply correctly within one tick (no double-stack).

### Datagen check

Run `:craftee:runData` after registration changes; commit any new generated JSON files (recipes + lang). Confirm the smithing-transform recipe JSONs and the template-craft recipe JSON are produced, and the `en_us.json` lang file contains all keys above.

## Files

### New files

```
settings.gradle                                                          (modified — add include)
craftee/build.gradle
craftee/gradle.properties
craftee/src/main/templates/META-INF/neoforge.mods.toml
craftee/src/main/java/com/tweeks/craftee/CrafteeMod.java
craftee/src/main/java/com/tweeks/craftee/Registration.java
craftee/src/main/java/com/tweeks/craftee/SetBonusHandler.java
craftee/src/main/java/com/tweeks/craftee/item/CrafteeArmorMaterials.java
craftee/src/main/java/com/tweeks/craftee/data/DataGenerators.java
craftee/src/main/java/com/tweeks/craftee/data/ModLanguageProvider.java
craftee/src/main/java/com/tweeks/craftee/data/ModRecipeProvider.java
craftee/src/main/resources/assets/craftee/equipment/craftee.json
craftee/src/main/resources/assets/craftee/items/craftee_helmet.json
craftee/src/main/resources/assets/craftee/items/craftee_chestplate.json
craftee/src/main/resources/assets/craftee/items/craftee_leggings.json
craftee/src/main/resources/assets/craftee/items/craftee_boots.json
craftee/src/main/resources/assets/craftee/items/craftee_upgrade_smithing_template.json
craftee/src/main/resources/assets/craftee/models/item/craftee_helmet.json
craftee/src/main/resources/assets/craftee/models/item/craftee_chestplate.json
craftee/src/main/resources/assets/craftee/models/item/craftee_leggings.json
craftee/src/main/resources/assets/craftee/models/item/craftee_boots.json
craftee/src/main/resources/assets/craftee/models/item/craftee_upgrade_smithing_template.json
craftee/src/main/resources/assets/craftee/textures/item/craftee_helmet.png
craftee/src/main/resources/assets/craftee/textures/item/craftee_chestplate.png
craftee/src/main/resources/assets/craftee/textures/item/craftee_leggings.png
craftee/src/main/resources/assets/craftee/textures/item/craftee_boots.png
craftee/src/main/resources/assets/craftee/textures/item/craftee_upgrade_smithing_template.png
craftee/src/main/resources/assets/craftee/textures/entity/equipment/humanoid/craftee.png
craftee/src/main/resources/assets/craftee/textures/entity/equipment/humanoid_leggings/craftee.png
craftee/src/test/java/com/tweeks/craftee/SetBonusHandlerTest.java
```

### Modified files

```
settings.gradle
    — add `include 'craftee'`
```

### Deleted files

None.

## Open questions / verification at implementation

1. **Smithing template construction API.** `SmithingTemplateItem.createUpgradeTemplate(String)` is the simplest path; if absent in this NeoForge build, fall back to the public constructor with `Component.translatable` arguments for the five tooltip lines (`upgrade`, `applies_to`, `ingredients`, `base_slot_description`, `additions_slot_description`).
2. **Player-tick event class.** `PlayerTickEvent.Post` vs. `LivingEvent.LivingTickEvent` filtered to `Player`. Both fire server-side every tick; either is acceptable.
3. **`AttributeModifier.Operation` enum value.** Spec uses `ADD_MULTIPLIED_BASE`; some recent versions ship `MULTIPLY_BASE`. End-user behaviour is identical (`+20%` of base movement speed).
4. **Equipment-asset JSON schema.** Spec writes the simple two-layer form mirroring `:creeperskin`. Verify against the actual `:creeperskin` JSON at runtime; if the schema in this build is different, mirror it exactly.
5. **Pixel-by-pixel texture details.** Palette and layout zones are spec'd; exact pixel placement is decided at implementation while inspecting the result in the dev client.

These are minor open points whose resolution doesn't change the design. They're flagged so the implementer doesn't get stuck if a method signature drifted.

## Self-review notes

- **Single source of truth for set membership:** `SetBonusHandler.isWearingFullSet`. Both modifier-sync calls go through it.
- **Single source of truth for modifier IDs:** `SPEED_ID`, `JUMP_ID`, `STEP_ID` are declared once at the top of `SetBonusHandler` and used by both add and remove paths — no chance of drift.
- **No per-tick attribute thrash.** `syncModifier` only mutates when the present/absent state actually disagrees with the desired state. Reading `hasModifier` is O(1).
- **Module placement matches existing convention:** sibling-feature module pattern (`:creeperskin`, `:thief`, `:securityguard`).
- **Bonuses gated on the *full* set, not partial wear** — locked in `isWearingFullSet`.
- **Numeric stats cited only here** (defenses `3/8/6/3`, toughness `3.0`, KB `0.1`, durability base `37`, modifier values `+0.20 / +0.30 / +0.50`) — no second source of these numbers exists in the spec, so they can't drift.
- **Recipe path is purely datagen** — no Java recipe class, just `SmithingTransformRecipeBuilder` and `ShapedRecipeBuilder` calls. Adding more recipes later is a pure data addition.
- **Version-agnostic API references** — all NeoForge / vanilla API mentions flagged "verify at implementation"; no hard assumption on a Minecraft version beyond what's in `gradle.properties`.
- **No retroactive changes to other modules.** No edits to `:securitycore`, `:securityguard`, `:thief`, or `:creeperskin`.
- **Scope sized for one PR:** new module, five registered items, one event handler, one material, datagen lang + recipes, asset textures. No cross-cutting refactor.
