# Craftee Skin Armor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a new `:craftee` sibling module that ships a 4-piece wearable armor set (helmet, chestplate, leggings, boots) with netherite-tier stats, black-with-orange-filmstrip textures, a 4-piece set bonus that adds +20% movement speed / +0.30 jump strength / +0.50 step height while worn, plus a `craftee_upgrade_smithing_template` item with a smithing-table transform recipe (template + diamond armor + netherite ingot → craftee armor) and a craftable bootstrap recipe (1 netherite scrap + 8 paper → 2 templates).

**Architecture:** Pure data-component armor — no `ArmorItem` subclass. `Item.Properties.humanoidArmor(CrafteeArmorMaterials.CRAFTEE, ArmorType.X)` handles durability, defense, and the `EQUIPPABLE` data component. Each piece registers as plain `Item::new`. Worn appearance comes from a custom `EquipmentAsset` (`craftee:craftee`) backed by a static `equipment/craftee.json` pointing at body + leggings PNGs. Set-bonus behaviour lives in a static `SetBonusHandler` on `NeoForge.EVENT_BUS` listening to `PlayerTickEvent.Post` — every tick it evaluates `isWearingFullSet(LivingEntity)` and idempotently adds or removes three persistent `AttributeModifier`s (speed / jump / step). One `isWearingFullSet` helper is the single source of truth for "all four pieces equipped." Recipes ship via vanilla datagen (`SmithingTransformRecipeBuilder` + `ShapedRecipeBuilder`).

**Tech Stack:** Java 25, NeoForge 26.1.2.30-beta, Minecraft 26.1.2, Gradle with `net.neoforged.moddev` 2.0.141, JUnit 5 for tests, Python 3 + Pillow for one-shot texture generation (the same pattern `:creeperskin`, `:thief`, `:securityguard` use).

**Spec:** [docs/superpowers/specs/2026-04-30-craftee-skin-design.md](../specs/2026-04-30-craftee-skin-design.md)

**Working directory for all commands:** `/Users/tweeks/code/minecraft-mods` (repo root). Module-specific gradle commands use the `:craftee:` prefix.

---

## File Structure

### Files created

```
craftee/build.gradle                                                                          # NEW: copy of creeperskin/build.gradle, no edits
craftee/gradle.properties                                                                     # NEW: per-module mod_id / version / group
craftee/src/main/templates/META-INF/neoforge.mods.toml                                        # NEW: mod metadata template
craftee/src/main/java/com/tweeks/craftee/CrafteeMod.java                                      # NEW: @Mod entry point
craftee/src/main/java/com/tweeks/craftee/Registration.java                                    # NEW: ITEMS / CREATIVE_TABS DeferredRegisters + 5 items + creative tab
craftee/src/main/java/com/tweeks/craftee/SetBonusHandler.java                                 # NEW: per-tick bonus handler + isWearingFullSet helper
craftee/src/main/java/com/tweeks/craftee/item/CrafteeArmorMaterials.java                      # NEW: ArmorMaterial constant + EquipmentAsset ResourceKey
craftee/src/main/java/com/tweeks/craftee/data/DataGenerators.java                             # NEW: bus subscriber for runData
craftee/src/main/java/com/tweeks/craftee/data/ModLanguageProvider.java                        # NEW: en_us lang entries
craftee/src/main/java/com/tweeks/craftee/data/ModRecipeProvider.java                          # NEW: smithing transform + template craft recipes
craftee/src/main/resources/assets/craftee/equipment/craftee.json                              # NEW: humanoid + humanoid_leggings layer descriptor
craftee/src/main/resources/assets/craftee/textures/entity/equipment/humanoid/craftee.png      # NEW: 64×32 worn-armor body texture
craftee/src/main/resources/assets/craftee/textures/entity/equipment/humanoid_leggings/craftee.png  # NEW: 64×32 worn-armor leggings texture
craftee/src/main/resources/assets/craftee/items/craftee_helmet.json                           # NEW: client item-model selector
craftee/src/main/resources/assets/craftee/items/craftee_chestplate.json                       # NEW
craftee/src/main/resources/assets/craftee/items/craftee_leggings.json                         # NEW
craftee/src/main/resources/assets/craftee/items/craftee_boots.json                            # NEW
craftee/src/main/resources/assets/craftee/items/craftee_upgrade_smithing_template.json        # NEW
craftee/src/main/resources/assets/craftee/models/item/craftee_helmet.json                     # NEW: item/generated parent
craftee/src/main/resources/assets/craftee/models/item/craftee_chestplate.json                 # NEW
craftee/src/main/resources/assets/craftee/models/item/craftee_leggings.json                   # NEW
craftee/src/main/resources/assets/craftee/models/item/craftee_boots.json                      # NEW
craftee/src/main/resources/assets/craftee/models/item/craftee_upgrade_smithing_template.json  # NEW
craftee/src/main/resources/assets/craftee/textures/item/craftee_helmet.png                    # NEW: 16×16 inventory icon
craftee/src/main/resources/assets/craftee/textures/item/craftee_chestplate.png                # NEW
craftee/src/main/resources/assets/craftee/textures/item/craftee_leggings.png                  # NEW
craftee/src/main/resources/assets/craftee/textures/item/craftee_boots.png                     # NEW
craftee/src/main/resources/assets/craftee/textures/item/craftee_upgrade_smithing_template.png # NEW
craftee/src/test/java/com/tweeks/craftee/SetBonusHandlerTest.java                             # NEW: helper unit test (best-effort; smoke fallback documented in step)
craftee/tools/generate_textures.py                                                            # NEW: one-shot Pillow script that regenerates the 7 PNGs above
```

### Files modified

```
settings.gradle                                                                               # add `include 'craftee'`
todo.md                                                                                       # strike "craftee skin" line at the very end of the plan, if present
```

### Files deleted

None.

---

## Task 1: Baseline — confirm current build is green

**Files:** none modified.

- [ ] **Step 1: Run the existing build**

```bash
./gradlew build
```

Expected: `BUILD SUCCESSFUL`. All sibling modules (`securitycore`, `securityguard`, `thief`, `creeperskin`, `translator`) build under their respective `build/libs/` directories.

- [ ] **Step 2: Run the existing test suite**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`. Existing tests pass.

- [ ] **Step 3: No commit**

Verification only. If either step fails, **stop and fix the existing module** before adding `:craftee` — every subsequent task assumes a green baseline so a fresh-baseline failure can be attributed to craftee work.

---

## Task 2: Create the empty `:craftee` module skeleton

**Files:**
- Create: `craftee/build.gradle`
- Create: `craftee/gradle.properties`
- Create: `craftee/src/main/templates/META-INF/neoforge.mods.toml`
- Modify: `settings.gradle`

The goal of this task is a module that builds (compiles, runs `runData` no-op, produces a jar) but contains zero Java code yet. Wiring up gradle without any compilation surface area means later steps fail loudly only on actual craftee work, not on scaffolding mistakes.

- [ ] **Step 1: Add the module to `settings.gradle`**

Open `settings.gradle`. Add `include 'craftee'` at the bottom of the existing `include` list:

```groovy
include 'securitycore'
include 'securityguard'
include 'thief'
include 'creeperskin'
include 'translator'
include 'craftee'
```

- [ ] **Step 2: Copy `creeperskin/build.gradle` to the new module verbatim**

```bash
cp creeperskin/build.gradle craftee/build.gradle
```

This file references `mod_id` from per-module `gradle.properties`, so no edits are needed inside it. `:creeperskin` is the closest structural template (single armor mod, no `:securitycore` dependency).

Verify with:
```bash
diff creeperskin/build.gradle craftee/build.gradle
```
Expected: no output (files identical).

- [ ] **Step 3: Create `craftee/gradle.properties`**

Create `craftee/gradle.properties` with:

```properties
# Sets default memory used for gradle commands. Can be overridden by user or command line properties.
org.gradle.jvmargs=-Xmx2G
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

## Mod Properties
mod_id=craftee
mod_name=Craftee
mod_license=MIT
mod_version=0.1.0
mod_group_id=com.tweeks.craftee
```

- [ ] **Step 4: Create the mod-metadata template**

Create directory and file:

```bash
mkdir -p craftee/src/main/templates/META-INF
```

Create `craftee/src/main/templates/META-INF/neoforge.mods.toml` with:

```toml
modLoader="javafml"
loaderVersion="[1,)"
license="${mod_license}"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"
authors="Tom Weeks"
description='''
Craftee armor — netherite-tier 4-piece set with a content-creator
filmstrip motif. Wear all four pieces for a mobility set bonus:
+20% movement speed, +0.30 jump strength, and +0.50 step height
(auto-step over single-block obstacles).
Crafted via a Craftee Upgrade Smithing Template (1 netherite scrap +
8 paper → 2 templates) applied to diamond armor with a netherite ingot.
'''

[[dependencies.${mod_id}]]
    modId="neoforge"
    type="required"
    versionRange="[${neo_version},)"
    ordering="NONE"
    side="BOTH"

[[dependencies.${mod_id}]]
    modId="minecraft"
    type="required"
    versionRange="${minecraft_version_range}"
    ordering="NONE"
    side="BOTH"
```

- [ ] **Step 5: Verify the empty module builds**

```bash
./gradlew :craftee:build
```

Expected: `BUILD SUCCESSFUL`. Even with zero Java sources, gradle should produce `craftee/build/libs/craftee-0.1.0.jar` containing only the processed mod metadata.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle craftee/build.gradle craftee/gradle.properties \
        craftee/src/main/templates/META-INF/neoforge.mods.toml
git commit -m "$(cat <<'EOF'
build(craftee): scaffold empty :craftee sibling module

Adds gradle subproject with mod_id=craftee, empty source tree, and
neoforge.mods.toml template. No Java sources yet — first compile target
lands in the next commit. Sized as its own commit so any later build
failure points at code, not scaffolding.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Wire the entry point and empty `Registration`

**Files:**
- Create: `craftee/src/main/java/com/tweeks/craftee/CrafteeMod.java`
- Create: `craftee/src/main/java/com/tweeks/craftee/Registration.java`

A minimal `@Mod` entry point that runs (and logs) at game start, plus an empty `Registration` it can call. After this task the mod loads in dev client but ships zero items.

- [ ] **Step 1: Create `Registration.java` with empty deferred registers**

Create `craftee/src/main/java/com/tweeks/craftee/Registration.java`:

```java
package com.tweeks.craftee;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(CrafteeMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CrafteeMod.MOD_ID);

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
```

(Item and tab declarations are added in Task 5. The handles are registered now so `register(modEventBus)` is correct from the start.)

- [ ] **Step 2: Create `CrafteeMod.java`**

Create `craftee/src/main/java/com/tweeks/craftee/CrafteeMod.java`:

```java
package com.tweeks.craftee;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CrafteeMod.MOD_ID)
public class CrafteeMod {
    public static final String MOD_ID = "craftee";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CrafteeMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Craftee mod loading");
        Registration.register(modEventBus);
    }
}
```

(`SetBonusHandler` registration on `NeoForge.EVENT_BUS` lands in Task 9. Until then there are no event subscribers.)

- [ ] **Step 3: Compile**

```bash
./gradlew :craftee:compileJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Boot the dev client and confirm the mod loads**

```bash
./gradlew :craftee:runClient
```

Expected: client launches without error. From the title screen, click **Mods**. Verify `Craftee 0.1.0` appears in the list alongside the existing mods. Quit the client.

- [ ] **Step 5: Commit**

```bash
git add craftee/src/main/java/com/tweeks/craftee/CrafteeMod.java \
        craftee/src/main/java/com/tweeks/craftee/Registration.java
git commit -m "$(cat <<'EOF'
feat(craftee): add empty @Mod entry point and DeferredRegisters

CrafteeMod loads at game start, calls Registration.register on the mod
event bus. Registration declares the ITEMS and CREATIVE_TABS
DeferredRegisters but contains no entries yet — items land in the next
commit. Confirmed loadable in dev client.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Define `CrafteeArmorMaterials.CRAFTEE` material and equipment-asset key

**Files:**
- Create: `craftee/src/main/java/com/tweeks/craftee/item/CrafteeArmorMaterials.java`

Direct port of `creeperskin/src/main/java/com/tweeks/creeperskin/item/CreeperArmorMaterials.java` with namespace + constant renames. All numeric stats (defense per slot, durability base, enchantability, toughness, KB, repair tag, equip sound) stay netherite-equivalent.

- [ ] **Step 1: Create `CrafteeArmorMaterials.java`**

Create directory and file:

```bash
mkdir -p craftee/src/main/java/com/tweeks/craftee/item
```

Create `craftee/src/main/java/com/tweeks/craftee/item/CrafteeArmorMaterials.java`:

```java
package com.tweeks.craftee.item;

import com.tweeks.craftee.CrafteeMod;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class CrafteeArmorMaterials {
    private CrafteeArmorMaterials() {}

    /** Equipment-asset id used by the {@link #CRAFTEE} material. Resolves to
     *  {@code assets/craftee/equipment/craftee.json} (defining the worn-armor
     *  texture layers) at runtime. */
    public static final ResourceKey<EquipmentAsset> CRAFTEE_ASSET =
        ResourceKey.create(EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(CrafteeMod.MOD_ID, "craftee"));

    /** Netherite-tier defense map: boots 3, legs 6, chest 8, helmet 3, body 19. */
    private static final Map<ArmorType, Integer> DEFENSE = Map.of(
        ArmorType.BOOTS,      3,
        ArmorType.LEGGINGS,   6,
        ArmorType.CHESTPLATE, 8,
        ArmorType.HELMET,     3,
        ArmorType.BODY,       19);

    /** Cosmetic craftee armor material. Stat-equivalent to vanilla netherite
     *  (durability 37 base, toughness 3.0, knockback resist 0.1,
     *  enchantability 15, fire-resistant via {@link Item.Properties#fireResistant}
     *  on the items themselves) — only the equipment asset and the texture it
     *  points at are different. */
    public static final ArmorMaterial CRAFTEE = new ArmorMaterial(
        37,
        DEFENSE,
        15,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        3.0F,
        0.1F,
        ItemTags.REPAIRS_NETHERITE_ARMOR,
        CRAFTEE_ASSET);
}
```

- [ ] **Step 2: Compile**

```bash
./gradlew :craftee:compileJava
```

Expected: `BUILD SUCCESSFUL`. If any import fails, cross-check against `creeperskin/src/main/java/com/tweeks/creeperskin/item/CreeperArmorMaterials.java` — the exact same imports work there.

- [ ] **Step 3: Commit**

```bash
git add craftee/src/main/java/com/tweeks/craftee/item/CrafteeArmorMaterials.java
git commit -m "$(cat <<'EOF'
feat(craftee): add CRAFTEE armor material (netherite-tier stats)

ArmorMaterial constant ported from :creeperskin with namespace and
asset-key renames. Defense map, durability base, enchantability,
toughness, knockback resist, repair tag, and equip sound match vanilla
netherite. Equipment asset key is craftee:craftee — resolved against
assets/craftee/equipment/craftee.json in Task 6.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Register the four armor items and creative tab

**Files:**
- Modify: `craftee/src/main/java/com/tweeks/craftee/Registration.java`

Adds the four armor items (`craftee_helmet`, `craftee_chestplate`, `craftee_leggings`, `craftee_boots`) and a creative tab. Smithing template lands in Task 12 — kept separate so any registration error in this task points at armor wiring, not template wiring.

- [ ] **Step 1: Replace `Registration.java` with item declarations**

Overwrite `craftee/src/main/java/com/tweeks/craftee/Registration.java` with:

```java
package com.tweeks.craftee;

import com.tweeks.craftee.item.CrafteeArmorMaterials;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(CrafteeMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CrafteeMod.MOD_ID);

    public static final DeferredItem<Item> CRAFTEE_HELMET = ITEMS.registerItem("craftee_helmet",
        Item::new,
        p -> p.humanoidArmor(CrafteeArmorMaterials.CRAFTEE, ArmorType.HELMET)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredItem<Item> CRAFTEE_CHESTPLATE = ITEMS.registerItem("craftee_chestplate",
        Item::new,
        p -> p.humanoidArmor(CrafteeArmorMaterials.CRAFTEE, ArmorType.CHESTPLATE)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredItem<Item> CRAFTEE_LEGGINGS = ITEMS.registerItem("craftee_leggings",
        Item::new,
        p -> p.humanoidArmor(CrafteeArmorMaterials.CRAFTEE, ArmorType.LEGGINGS)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredItem<Item> CRAFTEE_BOOTS = ITEMS.registerItem("craftee_boots",
        Item::new,
        p -> p.humanoidArmor(CrafteeArmorMaterials.CRAFTEE, ArmorType.BOOTS)
              .fireResistant()
              .stacksTo(1));

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
                })
                .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
```

- [ ] **Step 2: Compile**

```bash
./gradlew :craftee:compileJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit (no client run yet — assets land in Task 6/7)**

The dev client would launch and the items would *register* but render with the missing-texture purple checkerboard. We commit at this checkpoint anyway so a regression in the next asset task is bisectable.

```bash
git add craftee/src/main/java/com/tweeks/craftee/Registration.java
git commit -m "$(cat <<'EOF'
feat(craftee): register four armor items and creative tab

Items: craftee_helmet, craftee_chestplate, craftee_leggings,
craftee_boots — all stacks-to-1, fire-resistant, netherite-stat via
CrafteeArmorMaterials.CRAFTEE. Creative tab "Craftee" displays them
in slot order with the helmet as icon. Items will render with missing
textures until Task 6 + Task 7 ship the asset files.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Write the equipment asset JSON and generate worn-armor textures

**Files:**
- Create: `craftee/src/main/resources/assets/craftee/equipment/craftee.json`
- Create: `craftee/tools/generate_textures.py`
- Create: `craftee/src/main/resources/assets/craftee/textures/entity/equipment/humanoid/craftee.png`
- Create: `craftee/src/main/resources/assets/craftee/textures/entity/equipment/humanoid_leggings/craftee.png`

The two worn-armor PNGs follow vanilla armor UV layout (64×32 sheets, top half = body/helmet/boots layer 1, bottom half = arms/leg layer continuation). The `equipment/craftee.json` tells Minecraft which texture to draw for each layer of the humanoid armor model.

- [ ] **Step 1: Create the equipment asset JSON**

Create directory and file:

```bash
mkdir -p craftee/src/main/resources/assets/craftee/equipment
```

Create `craftee/src/main/resources/assets/craftee/equipment/craftee.json`:

```json
{
  "layers": {
    "humanoid": [
      { "texture": "craftee:craftee" }
    ],
    "humanoid_leggings": [
      { "texture": "craftee:craftee" }
    ]
  }
}
```

- [ ] **Step 2: Create the texture generator script**

Create directory:
```bash
mkdir -p craftee/tools
```

Create `craftee/tools/generate_textures.py`:

```python
#!/usr/bin/env python3
"""
Regenerates the seven PNG assets for :craftee from a small palette.
Run with: python3 craftee/tools/generate_textures.py
Output paths are absolute under the module's resources tree, so the
script can be re-run any time the palette is tweaked. Idempotent.
"""

from PIL import Image
from pathlib import Path

# Palette
BLACK_BASE   = (0x0F, 0x0F, 0x0F, 255)
ORANGE_STRIP = (0xF0, 0x8A, 0x1F, 255)
WHITE_MARK   = (0xFF, 0xFF, 0xFF, 255)
TRANSPARENT  = (0, 0, 0, 0)

ROOT = Path(__file__).resolve().parents[1]
ENTITY_BASE   = ROOT / "src/main/resources/assets/craftee/textures/entity/equipment"
ITEM_TEXTURES = ROOT / "src/main/resources/assets/craftee/textures/item"


def fill(img: Image.Image, color):
    for x in range(img.width):
        for y in range(img.height):
            img.putpixel((x, y), color)


def make_humanoid_body() -> Image.Image:
    """64×32 humanoid armor sheet. Black base with a 2px-wide vertical
       orange filmstrip stripe down the centre-front of the chestplate
       and helmet UVs. Sprocket holes are 1×1 black pixels every 4 rows.

       Vanilla armor UV layout (top half is helmet + chest + arms layer):
         - helmet front face: x=8..15, y=8..15 (8×8 block)
         - chestplate front:  x=20..27, y=20..31 (8×12 block)
         - arms-front:        x=44..47, y=20..31 (split L/R)
       The "centre-front" of helmet is x=11..12, of chest is x=23..24."""
    img = Image.new("RGBA", (64, 32), TRANSPARENT)
    fill(img, BLACK_BASE)

    # Helmet front stripe (x=11..12, y=8..15)
    for y in range(8, 16):
        img.putpixel((11, y), ORANGE_STRIP)
        img.putpixel((12, y), ORANGE_STRIP)
    # Helmet sprocket holes (every 4 rows starting y=10)
    for y in (10, 14):
        img.putpixel((11, y), BLACK_BASE)
        img.putpixel((12, y), BLACK_BASE)

    # Chestplate front stripe (x=23..24, y=20..31)
    for y in range(20, 32):
        img.putpixel((23, y), ORANGE_STRIP)
        img.putpixel((24, y), ORANGE_STRIP)
    # Chest sprocket holes (every 4 rows starting y=22)
    for y in (22, 26, 30):
        img.putpixel((23, y), BLACK_BASE)
        img.putpixel((24, y), BLACK_BASE)

    return img


def make_humanoid_leggings() -> Image.Image:
    """64×32 leggings sheet. Black base with two parallel vertical
       orange stripes — one for each leg.
         - left  leg front: x=4..7,  y=20..31, centre x=5..6
         - right leg front: x=20..23,y=20..31, centre x=21..22
       NOTE: the topmost row of the leggings stripe (y=20) must align
       with the bottommost row of the chestplate stripe (y=31 in body
       sheet) so the filmstrip reads as one continuous line. Vanilla
       UV mapping puts both at the player's waist; visually verify in
       dev client."""
    img = Image.new("RGBA", (64, 32), TRANSPARENT)
    fill(img, BLACK_BASE)

    # Left leg stripe
    for y in range(20, 32):
        img.putpixel((5, y), ORANGE_STRIP)
        img.putpixel((6, y), ORANGE_STRIP)
    # Right leg stripe
    for y in range(20, 32):
        img.putpixel((21, y), ORANGE_STRIP)
        img.putpixel((22, y), ORANGE_STRIP)

    return img


def make_item_helmet() -> Image.Image:
    """16×16 helmet inventory icon — black helmet silhouette with
       single-px orange stripe down the centre-front."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    # Dome
    for x in range(5, 11):
        img.putpixel((x, 1), BLACK_BASE)
    # Body of helmet
    for y in range(2, 8):
        for x in range(3, 13):
            img.putpixel((x, y), BLACK_BASE)
    # Orange stripe
    for y in range(1, 8):
        img.putpixel((8, y), ORANGE_STRIP)
    return img


def make_item_chestplate() -> Image.Image:
    """16×16 chestplate icon — black T-silhouette with central orange stripe."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    # Top yoke
    for x in range(2, 14):
        for y in range(2, 4):
            img.putpixel((x, y), BLACK_BASE)
    # Body
    for x in range(4, 12):
        for y in range(4, 14):
            img.putpixel((x, y), BLACK_BASE)
    # Orange stripe
    for y in range(2, 14):
        img.putpixel((8, y), ORANGE_STRIP)
    return img


def make_item_leggings() -> Image.Image:
    """16×16 leggings icon — two black legs with one orange stripe each."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    # Waistband
    for x in range(3, 13):
        for y in range(2, 4):
            img.putpixel((x, y), BLACK_BASE)
    # Left leg
    for x in range(3, 7):
        for y in range(4, 14):
            img.putpixel((x, y), BLACK_BASE)
    # Right leg
    for x in range(9, 13):
        for y in range(4, 14):
            img.putpixel((x, y), BLACK_BASE)
    # Stripes
    for y in range(2, 14):
        img.putpixel((4, y), ORANGE_STRIP)
        img.putpixel((11, y), ORANGE_STRIP)
    return img


def make_item_boots() -> Image.Image:
    """16×16 boots icon — two short black blocks with orange dashes."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    # Two boots
    for x in range(3, 7):
        for y in range(8, 14):
            img.putpixel((x, y), BLACK_BASE)
    for x in range(9, 13):
        for y in range(8, 14):
            img.putpixel((x, y), BLACK_BASE)
    # Orange dashes on the front
    for y in (10, 11):
        img.putpixel((4, y), ORANGE_STRIP)
        img.putpixel((10, y), ORANGE_STRIP)
    return img


def make_item_template() -> Image.Image:
    """16×16 smithing template icon — black plate with orange diamond
       outline and a white 'C' marker centred."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    # Black square plate
    for x in range(2, 14):
        for y in range(2, 14):
            img.putpixel((x, y), BLACK_BASE)
    # Orange diamond outline (corners cut)
    diamond = [
        (8, 2), (7, 3), (9, 3), (6, 4), (10, 4),
        (5, 5), (11, 5), (4, 6), (12, 6), (3, 7), (13, 7),
        (4, 8), (12, 8), (5, 9), (11, 9), (6, 10), (10, 10),
        (7, 11), (9, 11), (8, 12)
    ]
    for px in diamond:
        img.putpixel(px, ORANGE_STRIP)
    # White 'C' centred (4×5)
    c_pixels = [
        (7, 6), (8, 6), (9, 6),
        (7, 7),
        (7, 8),
        (7, 9),
        (7, 10), (8, 10), (9, 10),
    ]
    for px in c_pixels:
        img.putpixel(px, WHITE_MARK)
    return img


def write(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, "PNG")
    print(f"wrote {path.relative_to(ROOT.parent)}")


def main() -> None:
    write(make_humanoid_body(),     ENTITY_BASE / "humanoid"          / "craftee.png")
    write(make_humanoid_leggings(), ENTITY_BASE / "humanoid_leggings" / "craftee.png")
    write(make_item_helmet(),       ITEM_TEXTURES / "craftee_helmet.png")
    write(make_item_chestplate(),   ITEM_TEXTURES / "craftee_chestplate.png")
    write(make_item_leggings(),     ITEM_TEXTURES / "craftee_leggings.png")
    write(make_item_boots(),        ITEM_TEXTURES / "craftee_boots.png")
    write(make_item_template(),     ITEM_TEXTURES / "craftee_upgrade_smithing_template.png")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2.5: Generate the 7 textures**

```bash
python3 craftee/tools/generate_textures.py
```

Expected output:
```
wrote craftee/src/main/resources/assets/craftee/textures/entity/equipment/humanoid/craftee.png
wrote craftee/src/main/resources/assets/craftee/textures/entity/equipment/humanoid_leggings/craftee.png
wrote craftee/src/main/resources/assets/craftee/textures/item/craftee_helmet.png
wrote craftee/src/main/resources/assets/craftee/textures/item/craftee_chestplate.png
wrote craftee/src/main/resources/assets/craftee/textures/item/craftee_leggings.png
wrote craftee/src/main/resources/assets/craftee/textures/item/craftee_boots.png
wrote craftee/src/main/resources/assets/craftee/textures/item/craftee_upgrade_smithing_template.png
```

If `Pillow` is missing: `pip install Pillow` first. The script writes seven PNGs even though the smithing template item won't be registered until Task 12 — it's cheap to generate them now in one Python invocation.

- [ ] **Step 3: Verify the file sizes**

```bash
file craftee/src/main/resources/assets/craftee/textures/entity/equipment/humanoid/craftee.png
file craftee/src/main/resources/assets/craftee/textures/item/craftee_helmet.png
```

Expected (paraphrased):
```
.../humanoid/craftee.png:    PNG image data, 64 x 32, 8-bit/color RGBA, non-interlaced
.../item/craftee_helmet.png: PNG image data, 16 x 16, 8-bit/color RGBA, non-interlaced
```

- [ ] **Step 4: Commit**

```bash
git add craftee/src/main/resources/assets/craftee/equipment/craftee.json \
        craftee/src/main/resources/assets/craftee/textures/entity/equipment \
        craftee/src/main/resources/assets/craftee/textures/item \
        craftee/tools/generate_textures.py
git commit -m "$(cat <<'EOF'
feat(craftee): add equipment asset JSON, texture generator, and PNGs

Equipment asset craftee:craftee maps both humanoid and humanoid_leggings
layers to the craftee:craftee texture key. Pillow generator produces 7
PNGs in one shot: 2 worn-armor (64x32) + 5 inventory icons (16x16) for
the four armor pieces and the smithing template (registered in Task 12).
Re-run scripts/generate_textures.py any time the palette is tweaked.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Write item-model JSONs for the four armor pieces

**Files:**
- Create: `craftee/src/main/resources/assets/craftee/items/craftee_helmet.json`
- Create: `craftee/src/main/resources/assets/craftee/items/craftee_chestplate.json`
- Create: `craftee/src/main/resources/assets/craftee/items/craftee_leggings.json`
- Create: `craftee/src/main/resources/assets/craftee/items/craftee_boots.json`
- Create: `craftee/src/main/resources/assets/craftee/models/item/craftee_helmet.json`
- Create: `craftee/src/main/resources/assets/craftee/models/item/craftee_chestplate.json`
- Create: `craftee/src/main/resources/assets/craftee/models/item/craftee_leggings.json`
- Create: `craftee/src/main/resources/assets/craftee/models/item/craftee_boots.json`

Two-tier asset chain matching the rest of the repo: `assets/.../items/<id>.json` selects a model; `assets/.../models/item/<id>.json` is the model itself, parented to vanilla `item/generated` so the icon PNG lives in `textures/item/<id>.png`. Smithing-template asset chain lands in Task 13.

- [ ] **Step 1: Create the four `items/<id>.json` selector files**

Create directory:
```bash
mkdir -p craftee/src/main/resources/assets/craftee/items
```

Create `craftee/src/main/resources/assets/craftee/items/craftee_helmet.json`:
```json
{ "model": { "type": "minecraft:model", "model": "craftee:item/craftee_helmet" } }
```

Create `craftee/src/main/resources/assets/craftee/items/craftee_chestplate.json`:
```json
{ "model": { "type": "minecraft:model", "model": "craftee:item/craftee_chestplate" } }
```

Create `craftee/src/main/resources/assets/craftee/items/craftee_leggings.json`:
```json
{ "model": { "type": "minecraft:model", "model": "craftee:item/craftee_leggings" } }
```

Create `craftee/src/main/resources/assets/craftee/items/craftee_boots.json`:
```json
{ "model": { "type": "minecraft:model", "model": "craftee:item/craftee_boots" } }
```

- [ ] **Step 2: Create the four `models/item/<id>.json` model files**

Create directory:
```bash
mkdir -p craftee/src/main/resources/assets/craftee/models/item
```

Create `craftee/src/main/resources/assets/craftee/models/item/craftee_helmet.json`:
```json
{ "parent": "minecraft:item/generated", "textures": { "layer0": "craftee:item/craftee_helmet" } }
```

Create `craftee/src/main/resources/assets/craftee/models/item/craftee_chestplate.json`:
```json
{ "parent": "minecraft:item/generated", "textures": { "layer0": "craftee:item/craftee_chestplate" } }
```

Create `craftee/src/main/resources/assets/craftee/models/item/craftee_leggings.json`:
```json
{ "parent": "minecraft:item/generated", "textures": { "layer0": "craftee:item/craftee_leggings" } }
```

Create `craftee/src/main/resources/assets/craftee/models/item/craftee_boots.json`:
```json
{ "parent": "minecraft:item/generated", "textures": { "layer0": "craftee:item/craftee_boots" } }
```

- [ ] **Step 3: Build the module to confirm assets validate**

```bash
./gradlew :craftee:build
```

Expected: `BUILD SUCCESSFUL`. Gradle's resource processing copies the JSONs into the jar; no validator runs at this stage, so a malformed JSON would only surface in the dev client. Step 4 catches that.

- [ ] **Step 4: Commit**

```bash
git add craftee/src/main/resources/assets/craftee/items \
        craftee/src/main/resources/assets/craftee/models
git commit -m "$(cat <<'EOF'
feat(craftee): add item-model JSONs for the four armor pieces

Two-tier asset chain: items/<id>.json selects the model;
models/item/<id>.json parents item/generated and points layer0 at the
icon PNG. Mirrors :creeperskin's pattern. Smithing template asset chain
lands in Task 13.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: First runClient smoke test — armor visible, equippable, defensible

**Files:** none modified.

The mod now has registered items, an armor material, a creative tab, equipment textures, and item icons. Booting the dev client and exercising each piece confirms the asset chain wired up correctly before we add behaviour.

- [ ] **Step 1: Boot the dev client**

```bash
./gradlew :craftee:runClient
```

- [ ] **Step 2: Verify the items render in the creative inventory**

In the running client:
1. Create or load a creative-mode world.
2. Open inventory (`E`), navigate to the **Craftee** tab (uses helmet as icon).
3. Confirm 4 items present in slot order: helmet, chestplate, leggings, boots. Each icon shows a black silhouette with the orange filmstrip stripe.

If any icon is the missing-texture purple checkerboard: the `items/<id>.json` model selector or the `textures/item/<id>.png` is wrong for that piece. Re-check Task 6 and Task 7 outputs.

- [ ] **Step 3: Verify worn-armor textures**

1. Take all four pieces into your hotbar; equip them all to the appropriate armor slots.
2. Press `F5` (twice) to enter front-facing third-person view.
3. Confirm: player rendered with black armor head-to-toe; orange stripe runs from helmet front down through chestplate front and continues along each leg. No purple-checkerboard render anywhere on the armor.

If the worn texture is missing or wrong: the `equipment/craftee.json` is mis-wired or the `textures/entity/equipment/humanoid{,_leggings}/craftee.png` is at the wrong path. Cross-check against `creeperskin/src/main/resources/assets/creeperskin/equipment/creeper.json` and the matching texture paths.

- [ ] **Step 4: Verify defenses**

1. While wearing the full set, open the inventory and hover over each armor piece. Tooltip should show the netherite-equivalent armor / armor-toughness numbers (helmet +3, chest +8, legs +6, boots +3; toughness 3.0; KB resist 0.1).
2. Take fall damage from a 4-block drop; verify reduced damage versus naked.
3. Take fire damage from a torch; verify the `fireResistant` flag means the *items* don't burn (drop one in lava if you want to confirm — they survive).

- [ ] **Step 5: Verify NO bonus yet (negative test)**

Run `/attribute @s minecraft:movement_speed get`. Expected: vanilla base value (`0.10000000149011612`) with no `craftee:set_bonus_speed` modifier present in `/attribute @s minecraft:movement_speed get modifier`. Bonus lands in Task 10.

- [ ] **Step 6: Quit the dev client**

- [ ] **Step 7: No commit**

Verification only.

---

## Task 9: Write a smoke test for `SetBonusHandler.isWearingFullSet`

**Files:**
- Create: `craftee/src/test/java/com/tweeks/craftee/SetBonusHandlerTest.java`

A reflection-based test that the helper exists and has the expected shape. The handler itself doesn't exist yet — this is **the failing test** that drives the next task. Best-effort: `LivingEntity` is too tightly coupled to a live `Level`/`AttributeMap` to mock cheaply in a unit test, so behavioural verification happens in the dev-client smoke test (Task 11). The reflection check guards against the helper being deleted or renamed in a refactor.

- [ ] **Step 1: Create directories and the test file**

```bash
mkdir -p craftee/src/test/java/com/tweeks/craftee
```

Create `craftee/src/test/java/com/tweeks/craftee/SetBonusHandlerTest.java`:

```java
package com.tweeks.craftee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-test for {@link SetBonusHandler}.
 *
 * <p>Best-effort: mocking {@link net.minecraft.world.entity.LivingEntity} is
 * difficult because most of its behaviour depends on a live {@code Level} and
 * registry holders. We keep one self-contained smoke test here that proves
 * the helper compiles and the file structure is right; full behavioural
 * verification of {@link SetBonusHandler#isWearingFullSet} happens in the
 * manual smoke test (Task 11). If a future test framework supports
 * lightweight LivingEntity mocks (e.g. SpongeMixin test harness), expand
 * this class to drive {@code isWearingFullSet} with mocks.
 */
class SetBonusHandlerTest {

    @Test
    void classLoadsAndExposesIsWearingFullSet() throws Exception {
        // Reflection only — calling isWearingFullSet with null would NPE
        // since the helper dereferences entity.getItemBySlot(...). The
        // method-existence check guards against the helper being deleted
        // or renamed in a refactor.
        var method = SetBonusHandler.class.getMethod(
            "isWearingFullSet",
            net.minecraft.world.entity.LivingEntity.class);
        assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()),
            "isWearingFullSet should be static");
        assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()),
            "isWearingFullSet should be public");
    }
}
```

- [ ] **Step 2: Run the test — verify it fails (no `SetBonusHandler` class yet)**

```bash
./gradlew :craftee:test
```

Expected: compilation FAIL with `cannot find symbol: class SetBonusHandler` or `:craftee:compileTestJava` failure.

If gradle reports the test passing or skipping: confirm `:craftee:test` actually picked up the new source root (sometimes Gradle needs `--rerun-tasks`).

- [ ] **Step 3: No commit**

Failing test will be committed alongside the implementation in Task 10.

---

## Task 10: Implement `SetBonusHandler` and wire it into the mod

**Files:**
- Create: `craftee/src/main/java/com/tweeks/craftee/SetBonusHandler.java`
- Modify: `craftee/src/main/java/com/tweeks/craftee/CrafteeMod.java`

Three persistent attribute modifiers are kept in sync with `isWearingFullSet` on every `PlayerTickEvent.Post`. Idempotent — if the desired present/absent state already matches reality, the handler is a no-op for that tick. See spec for the rationale (per-tick beats event-driven for self-correction; no logout cleanup needed).

- [ ] **Step 1: Create `SetBonusHandler.java`**

Create `craftee/src/main/java/com/tweeks/craftee/SetBonusHandler.java`:

```java
package com.tweeks.craftee;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Per-tick set-bonus handler for the Craftee armor set. Subscribes to
 * the NeoForge game event bus from {@link CrafteeMod}'s constructor.
 *
 * <p>Three persistent {@link AttributeModifier}s are kept in sync with
 * {@link #isWearingFullSet}:
 * <ol>
 *   <li>{@code MOVEMENT_SPEED}: +20% (multiplicative on base).</li>
 *   <li>{@code JUMP_STRENGTH}: +0.30 (additive).</li>
 *   <li>{@code STEP_HEIGHT}:  +0.50 (additive — clears a full block).</li>
 * </ol>
 *
 * <p>The handler is idempotent: if the desired present/absent state
 * already matches the modifier's actual presence on the attribute, it is
 * a no-op for that tick. See the design spec for why per-tick beats
 * event-driven here, and why no logout/death cleanup is needed.
 */
public final class SetBonusHandler {
    private SetBonusHandler() {}

    private static final ResourceLocation SPEED_ID =
        ResourceLocation.fromNamespaceAndPath(CrafteeMod.MOD_ID, "set_bonus_speed");
    private static final ResourceLocation JUMP_ID =
        ResourceLocation.fromNamespaceAndPath(CrafteeMod.MOD_ID, "set_bonus_jump");
    private static final ResourceLocation STEP_ID =
        ResourceLocation.fromNamespaceAndPath(CrafteeMod.MOD_ID, "set_bonus_step");

    private static final AttributeModifier SPEED_MOD =
        new AttributeModifier(SPEED_ID, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final AttributeModifier JUMP_MOD =
        new AttributeModifier(JUMP_ID, 0.30, AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier STEP_MOD =
        new AttributeModifier(STEP_ID, 0.50, AttributeModifier.Operation.ADD_VALUE);

    /** True iff {@code entity} has all four craftee-skin pieces equipped
     *  in the matching armor slots. */
    public static boolean isWearingFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(Registration.CRAFTEE_HELMET.get())
            && entity.getItemBySlot(EquipmentSlot.CHEST).is(Registration.CRAFTEE_CHESTPLATE.get())
            && entity.getItemBySlot(EquipmentSlot.LEGS).is(Registration.CRAFTEE_LEGGINGS.get())
            && entity.getItemBySlot(EquipmentSlot.FEET).is(Registration.CRAFTEE_BOOTS.get());
    }

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
        if (shouldHave && !has) {
            inst.addPermanentModifier(mod);
        } else if (!shouldHave && has) {
            inst.removeModifier(mod.id());
        }
    }
}
```

- [ ] **Step 2: Wire the handler into `CrafteeMod.java`**

Replace `craftee/src/main/java/com/tweeks/craftee/CrafteeMod.java`:

```java
package com.tweeks.craftee;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(CrafteeMod.MOD_ID)
public class CrafteeMod {
    public static final String MOD_ID = "craftee";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CrafteeMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Craftee mod loading");
        Registration.register(modEventBus);
        NeoForge.EVENT_BUS.register(SetBonusHandler.class);
    }
}
```

- [ ] **Step 3: Run the unit test — verify it now passes**

```bash
./gradlew :craftee:test
```

Expected: `BUILD SUCCESSFUL`. The reflection assertion finds the public-static `isWearingFullSet(LivingEntity)`.

- [ ] **Step 4: Compile the full module**

```bash
./gradlew :craftee:compileJava
```

Expected: `BUILD SUCCESSFUL`. If `PlayerTickEvent.Post` import fails: this NeoForge version may package it as `LivingEvent.LivingTickEvent`. Switch to that and filter to `Player` instances at the top of `onPlayerTick`. The behaviour is identical.

- [ ] **Step 5: Commit**

```bash
git add craftee/src/main/java/com/tweeks/craftee/SetBonusHandler.java \
        craftee/src/main/java/com/tweeks/craftee/CrafteeMod.java \
        craftee/src/test/java/com/tweeks/craftee/SetBonusHandlerTest.java
git commit -m "$(cat <<'EOF'
feat(craftee): add SetBonusHandler — full-set mobility bonus

Three persistent attribute modifiers (speed +20%, jump +0.30, step
+0.50) are added when the wearer enters full-set state and removed
when they leave it. Per-tick state sync via PlayerTickEvent.Post is
self-correcting and idempotent. SetBonusHandlerTest is a reflection
smoke test (LivingEntity is too coupled to a live Level for unit
mocks; behaviour is verified via dev-client smoke in the next task).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 11: Smoke test the set bonus in the dev client

**Files:** none modified.

- [ ] **Step 1: Boot the dev client**

```bash
./gradlew :craftee:runClient
```

Load a creative world.

- [ ] **Step 2: Confirm bonus activates on full-set wear**

1. Equip all four craftee pieces.
2. Run `/attribute @s minecraft:movement_speed get modifier`. Expected: shows a `craftee:set_bonus_speed` modifier with amount `0.2` and operation `ADD_MULTIPLIED_BASE`.
3. Run `/attribute @s minecraft:jump_strength get modifier`. Expected: `craftee:set_bonus_jump` with amount `0.3` operation `ADD_VALUE`.
4. Run `/attribute @s minecraft:step_height get modifier`. Expected: `craftee:set_bonus_step` with amount `0.5` operation `ADD_VALUE`.
5. Sprint along a long flat surface; confirm visibly faster than naked sprint.
6. Walk into a 1-block step (single-block obstacle). The player auto-steps over without a jump input.
7. Jump in place; jump arc is taller than vanilla.

- [ ] **Step 3: Confirm bonus deactivates on partial set**

1. Remove the helmet (replace with empty slot).
2. Run `/attribute @s minecraft:movement_speed get modifier` again. Expected: `craftee:set_bonus_speed` is gone (within one tick — re-issue the command if needed). Same for jump and step.
3. Confirm visually: speed and step height return to vanilla.

- [ ] **Step 4: Confirm bonus re-activates on re-equip**

1. Re-equip the helmet.
2. Within one second, re-run the three `/attribute ... get modifier` commands. All three modifiers reappear.

- [ ] **Step 5: Death / respawn check (no ghost modifier)**

1. Equip full set, sprint to confirm boost active.
2. Run `/kill @s` (player dies; default rules drop the armor).
3. After respawn (with empty armor slots), run `/attribute @s minecraft:movement_speed get modifier`. Expected: no `craftee:*` modifiers present — the respawned entity has a fresh attribute map.
4. Pick up the dropped armor, re-equip; modifiers return.

- [ ] **Step 6: Disconnect / reconnect check (no double-stack)**

1. With full set worn and modifiers active, save-and-quit to title screen.
2. Re-load the same world.
3. Run `/attribute @s minecraft:movement_speed get`. Expected: amount reflects exactly one `+20%` modifier (e.g. base `0.10` × `1.20` = `0.12`), not stacked twice.

- [ ] **Step 7: Quit the dev client**

- [ ] **Step 8: No commit**

Verification only.

---

## Task 12: Register the smithing template item

**Files:**
- Modify: `craftee/src/main/java/com/tweeks/craftee/Registration.java`

Add the smithing template as a fifth registered item, included in the creative tab.

- [ ] **Step 1: Add the smithing template registration to `Registration.java`**

Open `craftee/src/main/java/com/tweeks/craftee/Registration.java`. Add the `SmithingTemplateItem` import and the new constant immediately after `CRAFTEE_BOOTS`. Add `output.accept(CRAFTEE_UPGRADE_SMITHING_TEMPLATE.get());` to the creative-tab `displayItems` lambda.

Final file:

```java
package com.tweeks.craftee;

import com.tweeks.craftee.item.CrafteeArmorMaterials;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(CrafteeMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CrafteeMod.MOD_ID);

    public static final DeferredItem<Item> CRAFTEE_HELMET = ITEMS.registerItem("craftee_helmet",
        Item::new,
        p -> p.humanoidArmor(CrafteeArmorMaterials.CRAFTEE, ArmorType.HELMET)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredItem<Item> CRAFTEE_CHESTPLATE = ITEMS.registerItem("craftee_chestplate",
        Item::new,
        p -> p.humanoidArmor(CrafteeArmorMaterials.CRAFTEE, ArmorType.CHESTPLATE)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredItem<Item> CRAFTEE_LEGGINGS = ITEMS.registerItem("craftee_leggings",
        Item::new,
        p -> p.humanoidArmor(CrafteeArmorMaterials.CRAFTEE, ArmorType.LEGGINGS)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredItem<Item> CRAFTEE_BOOTS = ITEMS.registerItem("craftee_boots",
        Item::new,
        p -> p.humanoidArmor(CrafteeArmorMaterials.CRAFTEE, ArmorType.BOOTS)
              .fireResistant()
              .stacksTo(1));

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

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
```

- [ ] **Step 2: Compile**

```bash
./gradlew :craftee:compileJava
```

Expected: `BUILD SUCCESSFUL`. If `SmithingTemplateItem.createUpgradeTemplate(String, Item.Properties)` is not available in this NeoForge version: fall back to the public constructor. The two valid alternative call shapes are:

  - `new SmithingTemplateItem(Component upgradeTitle, Component appliesTo, Component ingredient, Component baseSlotDesc, Component additionsSlotDesc, p)` — pass `Component.translatable("upgrade.craftee.craftee_upgrade")` etc.
  - A vanilla static factory like `SmithingTemplateItem.createNetheriteUpgradeTemplate(p)` adapted for our namespace.

Pick whichever exists. End-user behaviour (smithing-template-shaped item that drives a `SmithingTransformRecipe`) is the same.

- [ ] **Step 3: Commit**

```bash
git add craftee/src/main/java/com/tweeks/craftee/Registration.java
git commit -m "$(cat <<'EOF'
feat(craftee): register craftee_upgrade_smithing_template item

Fifth registered item: SmithingTemplateItem.createUpgradeTemplate keyed
to namespace "craftee_upgrade". Added to creative tab at the end of the
display list. Recipe wiring lands in Task 15.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 13: Add asset chain for the smithing template

**Files:**
- Create: `craftee/src/main/resources/assets/craftee/items/craftee_upgrade_smithing_template.json`
- Create: `craftee/src/main/resources/assets/craftee/models/item/craftee_upgrade_smithing_template.json`

PNG was already generated in Task 6. Only the two JSON files (selector + model) are missing.

- [ ] **Step 1: Create the items selector JSON**

Create `craftee/src/main/resources/assets/craftee/items/craftee_upgrade_smithing_template.json`:

```json
{ "model": { "type": "minecraft:model", "model": "craftee:item/craftee_upgrade_smithing_template" } }
```

- [ ] **Step 2: Create the model JSON**

Create `craftee/src/main/resources/assets/craftee/models/item/craftee_upgrade_smithing_template.json`:

```json
{ "parent": "minecraft:item/generated", "textures": { "layer0": "craftee:item/craftee_upgrade_smithing_template" } }
```

- [ ] **Step 3: Build to confirm**

```bash
./gradlew :craftee:build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add craftee/src/main/resources/assets/craftee/items/craftee_upgrade_smithing_template.json \
        craftee/src/main/resources/assets/craftee/models/item/craftee_upgrade_smithing_template.json
git commit -m "$(cat <<'EOF'
feat(craftee): add asset chain for the smithing template

Selector + model JSONs for craftee_upgrade_smithing_template, parented
to minecraft:item/generated and pointing layer0 at the icon PNG already
produced by tools/generate_textures.py.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 14: Wire datagen — `DataGenerators` and `ModLanguageProvider`

**Files:**
- Create: `craftee/src/main/java/com/tweeks/craftee/data/DataGenerators.java`
- Create: `craftee/src/main/java/com/tweeks/craftee/data/ModLanguageProvider.java`

`DataGenerators` is the bus subscriber that NeoForge calls during `:craftee:runData`. `ModLanguageProvider` produces `en_us.json` with the item-display strings. The recipe provider lands in Task 15 separately so a recipe-build failure doesn't block lang generation.

- [ ] **Step 1: Create `ModLanguageProvider.java`**

```bash
mkdir -p craftee/src/main/java/com/tweeks/craftee/data
```

Create `craftee/src/main/java/com/tweeks/craftee/data/ModLanguageProvider.java`:

```java
package com.tweeks.craftee.data;

import com.tweeks.craftee.CrafteeMod;
import com.tweeks.craftee.Registration;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(output, CrafteeMod.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + CrafteeMod.MOD_ID, "Craftee");
        add(Registration.CRAFTEE_HELMET.get(),     "Craftee Helmet");
        add(Registration.CRAFTEE_CHESTPLATE.get(), "Craftee Chestplate");
        add(Registration.CRAFTEE_LEGGINGS.get(),   "Craftee Leggings");
        add(Registration.CRAFTEE_BOOTS.get(),      "Craftee Boots");
        add(Registration.CRAFTEE_UPGRADE_SMITHING_TEMPLATE.get(), "Craftee Upgrade Smithing Template");
    }
}
```

(Smithing-template tooltip strings — `applies_to`, `ingredients`, `base_slot_description`, `additions_slot_description`, `upgrade.*` — are added in step 4 below once their exact keys are observed in the running client.)

- [ ] **Step 2: Create `DataGenerators.java`**

Create `craftee/src/main/java/com/tweeks/craftee/data/DataGenerators.java`:

```java
package com.tweeks.craftee.data;

import com.tweeks.craftee.CrafteeMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = CrafteeMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class DataGenerators {
    private DataGenerators() {}

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        event.createProvider(ModLanguageProvider::new);
    }
}
```

(Recipe provider is wired in Task 15.)

- [ ] **Step 3: Run datagen**

```bash
./gradlew :craftee:runData
```

Expected: `BUILD SUCCESSFUL`. New file appears at `craftee/src/generated/clientData/assets/craftee/lang/en_us.json` with the six entries above.

```bash
cat craftee/src/generated/clientData/assets/craftee/lang/en_us.json
```

- [ ] **Step 4: Observe smithing-template tooltip keys in the dev client**

```bash
./gradlew :craftee:runClient
```

In creative inventory, hover over the **Craftee Upgrade Smithing Template**. The tooltip will show placeholder text for each tooltip line — those placeholder strings are the lang keys. Note the four/five keys you see (typically resemble `item.craftee.smithing_template.craftee_upgrade.applies_to`, etc.). Quit the client.

Add the observed keys to `ModLanguageProvider.addTranslations()`. For example, if the placeholders read `item.craftee.smithing_template.craftee_upgrade.applies_to`:

```java
add("upgrade.craftee.craftee_upgrade",                                      "Craftee Upgrade");
add("item.craftee.smithing_template.craftee_upgrade.applies_to",            "Diamond Armor");
add("item.craftee.smithing_template.craftee_upgrade.ingredients",           "Netherite Ingot");
add("item.craftee.smithing_template.craftee_upgrade.base_slot_description", "Add diamond armor");
add("item.craftee.smithing_template.craftee_upgrade.additions_slot_description", "Add netherite ingot");
```

If your placeholder strings used a different prefix, swap accordingly — match the observed strings exactly.

- [ ] **Step 5: Re-run datagen**

```bash
./gradlew :craftee:runData
```

Expected: `BUILD SUCCESSFUL`. Re-check `en_us.json` contains the new tooltip strings.

- [ ] **Step 6: Smoke-test the tooltip displays the new strings**

```bash
./gradlew :craftee:runClient
```

Hover over the smithing template — tooltip now reads "Diamond Armor", "Netherite Ingot", etc. instead of the placeholder keys. Quit.

- [ ] **Step 7: Commit**

```bash
git add craftee/src/main/java/com/tweeks/craftee/data/DataGenerators.java \
        craftee/src/main/java/com/tweeks/craftee/data/ModLanguageProvider.java \
        craftee/src/generated/clientData/assets/craftee/lang/en_us.json
git commit -m "$(cat <<'EOF'
feat(craftee): wire datagen — language provider + DataGenerators

ModLanguageProvider produces en_us.json with display names for the four
armor pieces, smithing template, creative tab, and the smithing-template
tooltip lines (keys observed via runClient placeholder text). Recipe
provider lands in the next commit.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 15: Datagen recipes — smithing-transform x4 + template-craft

**Files:**
- Create: `craftee/src/main/java/com/tweeks/craftee/data/ModRecipeProvider.java`
- Modify: `craftee/src/main/java/com/tweeks/craftee/data/DataGenerators.java`

Five recipes total: four smithing-transform (helmet/chest/legs/boots) + one template-bootstrap (1 scrap + 8 paper → 2 templates). Both recipe types are stock vanilla builders.

- [ ] **Step 1: Create `ModRecipeProvider.java`**

Create `craftee/src/main/java/com/tweeks/craftee/data/ModRecipeProvider.java`:

```java
package com.tweeks.craftee.data;

import com.tweeks.craftee.CrafteeMod;
import com.tweeks.craftee.Registration;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput out) {
        Ingredient template = Ingredient.of(Registration.CRAFTEE_UPGRADE_SMITHING_TEMPLATE.get());
        Ingredient netheriteIngot = Ingredient.of(Items.NETHERITE_INGOT);

        smithingUpgrade(out, "craftee_helmet",     template, Items.DIAMOND_HELMET,     netheriteIngot, Registration.CRAFTEE_HELMET.get());
        smithingUpgrade(out, "craftee_chestplate", template, Items.DIAMOND_CHESTPLATE, netheriteIngot, Registration.CRAFTEE_CHESTPLATE.get());
        smithingUpgrade(out, "craftee_leggings",   template, Items.DIAMOND_LEGGINGS,   netheriteIngot, Registration.CRAFTEE_LEGGINGS.get());
        smithingUpgrade(out, "craftee_boots",      template, Items.DIAMOND_BOOTS,      netheriteIngot, Registration.CRAFTEE_BOOTS.get());

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Registration.CRAFTEE_UPGRADE_SMITHING_TEMPLATE.get(), 2)
            .pattern("PPP")
            .pattern("PNP")
            .pattern("PPP")
            .define('P', Items.PAPER)
            .define('N', Items.NETHERITE_SCRAP)
            .unlockedBy("has_netherite_scrap", has(Items.NETHERITE_SCRAP))
            .save(out, ResourceLocation.fromNamespaceAndPath(CrafteeMod.MOD_ID, "craftee_upgrade_smithing_template"));
    }

    private static void smithingUpgrade(RecipeOutput out,
                                        String resultId,
                                        Ingredient template,
                                        Item base,
                                        Ingredient addition,
                                        Item result) {
        SmithingTransformRecipeBuilder.smithing(template,
                                                Ingredient.of(base),
                                                addition,
                                                RecipeCategory.COMBAT,
                                                result)
            .unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT))
            .save(out, ResourceLocation.fromNamespaceAndPath(CrafteeMod.MOD_ID, resultId + "_smithing"));
    }
}
```

**Verify at implementation:** `SmithingTransformRecipeBuilder.smithing(...)` and `RecipeProvider`'s constructor signature differ slightly between Forge / NeoForge versions. If gradle reports a missing method, cross-reference the corresponding builder in the vanilla deobf source for `26.1.2` — the builder method is always called `smithing` but the parameter list ordering may shift. Adjust the call site to match.

- [ ] **Step 2: Wire the recipe provider into `DataGenerators.java`**

Replace `craftee/src/main/java/com/tweeks/craftee/data/DataGenerators.java`:

```java
package com.tweeks.craftee.data;

import com.tweeks.craftee.CrafteeMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = CrafteeMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class DataGenerators {
    private DataGenerators() {}

    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent.Client event) {
        event.createProvider(ModLanguageProvider::new);
    }

    @SubscribeEvent
    public static void gatherServerData(GatherDataEvent.Server event) {
        event.createProvider(ModRecipeProvider::new);
    }
}
```

(Recipes are server-data; lang is client-data. Splitting them across the two events keeps NeoForge's data-gen pipeline happy.)

- [ ] **Step 3: Run datagen**

```bash
./gradlew :craftee:runData
```

Expected: `BUILD SUCCESSFUL`. New files appear under:

```
craftee/src/generated/serverData/data/craftee/recipe/craftee_helmet_smithing.json
craftee/src/generated/serverData/data/craftee/recipe/craftee_chestplate_smithing.json
craftee/src/generated/serverData/data/craftee/recipe/craftee_leggings_smithing.json
craftee/src/generated/serverData/data/craftee/recipe/craftee_boots_smithing.json
craftee/src/generated/serverData/data/craftee/recipe/craftee_upgrade_smithing_template.json
```

Spot-check one of the smithing-transform JSONs:
```bash
cat craftee/src/generated/serverData/data/craftee/recipe/craftee_helmet_smithing.json
```

Expected shape:
```json
{
  "type": "minecraft:smithing_transform",
  "addition": { "item": "minecraft:netherite_ingot" },
  "base":     { "item": "minecraft:diamond_helmet" },
  "result":   { "id":   "craftee:craftee_helmet" },
  "template": { "item": "craftee:craftee_upgrade_smithing_template" }
}
```

(Exact key spellings vary slightly per version — `"id"` vs `"item"` for `result`.)

Spot-check the template-craft JSON:
```bash
cat craftee/src/generated/serverData/data/craftee/recipe/craftee_upgrade_smithing_template.json
```

Expected: `"type": "minecraft:crafting_shaped"`, pattern `["PPP", "PNP", "PPP"]`, key `P` → paper, `N` → netherite scrap, result count 2.

- [ ] **Step 4: Commit**

```bash
git add craftee/src/main/java/com/tweeks/craftee/data/ModRecipeProvider.java \
        craftee/src/main/java/com/tweeks/craftee/data/DataGenerators.java \
        craftee/src/generated/serverData/data/craftee/recipe
git commit -m "$(cat <<'EOF'
feat(craftee): datagen recipe provider — smithing transforms + template

Five recipes: four smithing-transform (template + diamond armor +
netherite ingot → matching craftee armor) plus a shaped 1 scrap + 8
paper → 2 templates bootstrap recipe. RecipeProvider wired into
DataGenerators on the GatherDataEvent.Server channel.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 16: End-to-end smoke test — recipes + bonus + visuals

**Files:** none modified.

Final manual verification covering every spec requirement at once.

- [ ] **Step 1: Boot the dev client in survival mode**

```bash
./gradlew :craftee:runClient
```

Switch to a survival-mode test world (or use `/gamemode survival` after `/give`-ing yourself the materials).

- [ ] **Step 2: Verify the template recipe**

1. `/give @s minecraft:netherite_scrap` and `/give @s minecraft:paper 8`.
2. Open a crafting table. Place 1 netherite scrap centre, 8 paper around it (PPP/PNP/PPP).
3. Output: 2× `craftee_upgrade_smithing_template`. Take both.

- [ ] **Step 3: Verify the smithing-transform recipes**

1. `/give @s minecraft:diamond_helmet`, `/give @s minecraft:diamond_chestplate`, `/give @s minecraft:diamond_leggings`, `/give @s minecraft:diamond_boots`.
2. `/give @s minecraft:netherite_ingot 4`.
3. Open a smithing table. Place one template, one diamond helmet, one netherite ingot. Output: `craftee_helmet`. Take it.
4. Repeat for chestplate, leggings, boots — each consumes one template, one diamond piece, one ingot. Total of 4 templates needed across the four pieces.

- [ ] **Step 4: Confirm the worn texture**

Equip all four. Press `F5` × 2. Player rendered black with a continuous orange filmstrip stripe down the chest, helmet front, and each leg. **Check waist alignment specifically:** during walk animation, the chest stripe and legs stripe should read as one continuous line, not jog left/right. If they don't, regenerate the leggings PNG with the legging stripe shifted ±1 px (edit `make_humanoid_leggings` in `tools/generate_textures.py`, re-run, re-test).

- [ ] **Step 5: Confirm the set bonus**

Run all three `/attribute @s minecraft:<id> get modifier` commands; each should show its `craftee:set_bonus_*` modifier present. Sprint, jump, and walk into a single-block step to feel the boosts.

- [ ] **Step 6: Confirm partial-set has no bonus**

Remove any one piece — within one tick, all three modifiers vanish. Re-equip — they return.

- [ ] **Step 7: Confirm tooltip translations**

Hover over each item in inventory. Names are "Craftee Helmet", "Craftee Chestplate", "Craftee Leggings", "Craftee Boots", "Craftee Upgrade Smithing Template". Smithing template tooltip shows "Craftee Upgrade", "Diamond Armor", "Netherite Ingot", "Add diamond armor", "Add netherite ingot" — no placeholder lang keys.

- [ ] **Step 8: Confirm netherite-equivalent defenses**

Damage yourself with `/damage @s 6` while wearing the full set. HP bar drops by less than the equivalent unarmored hit (netherite armor mitigation factors in). Items show standard durability decrements.

- [ ] **Step 9: Quit the dev client**

- [ ] **Step 10: Run the full repo build & test**

```bash
./gradlew build test
```

Expected: `BUILD SUCCESSFUL` across all modules. The new `:craftee` test passes; no existing module regressed.

- [ ] **Step 11: No commit**

Verification only — implementation is complete.

---

## Task 17: Final cleanup — strike `todo.md`, optional

**Files:**
- Modify: `todo.md` (optional)

`todo.md` at repo root tracks long-running mod ideas. If "craftee skin" or similar appears there, strike or remove the line.

- [ ] **Step 1: Inspect `todo.md` for any craftee-related entry**

```bash
grep -n -i "craftee" todo.md
```

If no match: skip steps 2 and 3.

- [ ] **Step 2: If a line exists, edit it out**

Open `todo.md` and remove the line referencing craftee (or strike it through with `~~text~~` if the file uses strikethrough convention).

- [ ] **Step 3: Commit (only if a change was made)**

```bash
git add todo.md
git commit -m "$(cat <<'EOF'
chore: strike craftee skin from todo list

Mod is shipped and verified end-to-end.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Done

After Task 16, the `:craftee` module ships:

- 4 armor pieces with netherite-tier stats and a black + orange-filmstrip cosmetic.
- A 4-piece set bonus: +20% movement speed, +0.30 jump strength, +0.50 step height.
- A `craftee_upgrade_smithing_template` item with a smithing-table recipe (template + diamond armor + netherite ingot → craftee armor) and a craftable bootstrap recipe (1 scrap + 8 paper → 2 templates).
- A `Craftee` creative tab.
- Full English lang strings.
- A reflection-based unit test that guards `isWearingFullSet` against future renames.
