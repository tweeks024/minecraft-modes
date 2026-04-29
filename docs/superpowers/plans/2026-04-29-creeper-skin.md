# Creeper Skin Armor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a new `:creeperskin` sibling module that ships a 4-piece wearable armor set (helmet, chestplate, leggings, boots) with netherite-tier stats, creeper-green textures with the iconic creeper face on the helmet, and a 4-piece set bonus that makes real Creepers ignore the wearer and zeroes out incoming creeper-explosion damage.

**Architecture:** Pure data-component armor — no `ArmorItem` subclass. `Item.Properties.humanoidArmor(CreeperArmorMaterials.CREEPER, ArmorType.X)` handles durability, attribute modifiers, the `EQUIPPABLE` data component, and repair. Each piece registers as plain `Item::new`. Worn appearance comes from a custom `EquipmentAsset` (`creeperskin:creeper`) with a static `equipment/creeper.json` pointing at body + leggings PNGs. Set-bonus behavior lives in a static `SetBonusHandler` on `NeoForge.EVENT_BUS` listening to `LivingChangeTargetEvent` (creepers stop targeting full-set wearers) and `LivingIncomingDamageEvent` (creeper-tagged explosion damage on a full-set wearer is canceled). One `isWearingFullSet(LivingEntity)` helper is the single source of truth for "all four pieces equipped."

**Tech Stack:** Java 25, NeoForge 26.1.2.30-beta, Minecraft 26.1.2, Gradle with `net.neoforged.moddev` 2.0.141, JUnit 5 for tests, Python 3 + Pillow for one-shot texture generation (the same pattern thief and securityguard use).

**Spec:** [docs/superpowers/specs/2026-04-29-creeper-skin-design.md](../specs/2026-04-29-creeper-skin-design.md)

**Working directory for all commands:** `/Users/tweeks/code/minecraft-mods` (repo root). Module-specific gradle commands use the `:creeperskin:` prefix.

---

## File Structure

### Files created

```
creeperskin/build.gradle                                                                          # NEW: copy of thief/build.gradle, no edits
creeperskin/gradle.properties                                                                     # NEW: per-module mod_id / version / group
creeperskin/src/main/templates/META-INF/neoforge.mods.toml                                        # NEW: mod metadata template
creeperskin/src/main/java/com/tweeks/creeperskin/CreeperSkinMod.java                              # NEW: @Mod entry point
creeperskin/src/main/java/com/tweeks/creeperskin/Registration.java                                # NEW: ITEMS / CREATIVE_TABS DeferredRegisters
creeperskin/src/main/java/com/tweeks/creeperskin/SetBonusHandler.java                             # NEW: 4-piece bonus event handlers + isWearingFullSet helper
creeperskin/src/main/java/com/tweeks/creeperskin/item/CreeperArmorMaterials.java                  # NEW: ArmorMaterial constant + EquipmentAsset ResourceKey
creeperskin/src/main/java/com/tweeks/creeperskin/data/DataGenerators.java                         # NEW: bus subscriber for runData
creeperskin/src/main/java/com/tweeks/creeperskin/data/ModLanguageProvider.java                    # NEW: en_us lang entries
creeperskin/src/main/resources/assets/creeperskin/equipment/creeper.json                          # NEW: humanoid + humanoid_leggings layer descriptor
creeperskin/src/main/resources/assets/creeperskin/textures/entity/equipment/humanoid/creeper.png  # NEW: 64×32 worn-armor body texture
creeperskin/src/main/resources/assets/creeperskin/textures/entity/equipment/humanoid_leggings/creeper.png  # NEW: 64×32 worn-armor leggings texture
creeperskin/src/main/resources/assets/creeperskin/items/creeper_helmet.json                       # NEW: client item-model selector
creeperskin/src/main/resources/assets/creeperskin/items/creeper_chestplate.json                   # NEW
creeperskin/src/main/resources/assets/creeperskin/items/creeper_leggings.json                     # NEW
creeperskin/src/main/resources/assets/creeperskin/items/creeper_boots.json                        # NEW
creeperskin/src/main/resources/assets/creeperskin/models/item/creeper_helmet.json                 # NEW: item/generated parent
creeperskin/src/main/resources/assets/creeperskin/models/item/creeper_chestplate.json             # NEW
creeperskin/src/main/resources/assets/creeperskin/models/item/creeper_leggings.json               # NEW
creeperskin/src/main/resources/assets/creeperskin/models/item/creeper_boots.json                  # NEW
creeperskin/src/main/resources/assets/creeperskin/textures/item/creeper_helmet.png                # NEW: 16×16 inventory icon
creeperskin/src/main/resources/assets/creeperskin/textures/item/creeper_chestplate.png            # NEW
creeperskin/src/main/resources/assets/creeperskin/textures/item/creeper_leggings.png              # NEW
creeperskin/src/main/resources/assets/creeperskin/textures/item/creeper_boots.png                 # NEW
creeperskin/src/test/java/com/tweeks/creeperskin/SetBonusHandlerTest.java                         # NEW: helper unit test (best-effort; smoke fallback documented in step)
creeperskin/tools/generate_textures.py                                                            # NEW: one-shot Pillow script that regenerates the 6 PNGs above
```

### Files modified

```
settings.gradle                                                                                   # add `include 'creeperskin'`
todo.md                                                                                           # strike "creeper skin" line at the very end of the plan
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

Expected: `BUILD SUCCESSFUL`. `securitycore-0.1.0.jar`, `securityguard-0.1.0.jar`, `thief-0.1.0.jar` all build under their respective `build/libs/` directories.

- [ ] **Step 2: Run the existing test suite**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`. Existing tests (`SpawnPatternTest`, `StunEffectsTest`, etc.) pass.

- [ ] **Step 3: No commit**

Verification only. If either step fails, **stop and fix the existing module** before adding `:creeperskin` — every subsequent task assumes a green baseline so a fresh-baseline failure can be attributed to creeper-skin work.

---

## Task 2: Create the empty `:creeperskin` module skeleton

**Files:**
- Create: `creeperskin/build.gradle`
- Create: `creeperskin/gradle.properties`
- Create: `creeperskin/src/main/templates/META-INF/neoforge.mods.toml`
- Modify: `settings.gradle`

The goal of this task is a module that builds (compiles, runs `runData` no-op, produces a jar) but contains zero Java code yet. Wiring up gradle without any compilation surface area means later steps fail loudly only on actual creeper-skin work, not on scaffolding mistakes.

- [ ] **Step 1: Add the module to `settings.gradle`**

Open `settings.gradle`. Add `include 'creeperskin'` at the bottom of the existing `include` list:

```groovy
include 'securitycore'
include 'securityguard'
include 'thief'
include 'creeperskin'
```

- [ ] **Step 2: Copy `thief/build.gradle` to the new module verbatim**

```bash
cp thief/build.gradle creeperskin/build.gradle
```

This file references `mod_id` from per-module `gradle.properties`, so no edits are needed inside it. The `:thief` build.gradle is the simplest existing build.gradle (no reliance on `:securitycore` as a project dependency).

Verify with:
```bash
diff thief/build.gradle creeperskin/build.gradle
```
Expected: no output (files identical).

- [ ] **Step 3: Create `creeperskin/gradle.properties`**

Create `creeperskin/gradle.properties` with:

```properties
# Sets default memory used for gradle commands. Can be overridden by user or command line properties.
org.gradle.jvmargs=-Xmx2G
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

## Mod Properties
mod_id=creeperskin
mod_name=Creeper Skin
mod_license=MIT
mod_version=0.1.0
mod_group_id=com.tweeks.creeperskin
```

- [ ] **Step 4: Create the mod-metadata template**

Create directory and file:

```bash
mkdir -p creeperskin/src/main/templates/META-INF
```

Create `creeperskin/src/main/templates/META-INF/neoforge.mods.toml` with:

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
Wearable creeper-skin armor: 4-piece netherite-tier set with creeper-green
textures and the creeper face on the helmet. Wear all four pieces and real
Creepers will ignore you, and creeper explosions deal zero damage.
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

(Unlike `:thief`'s template, we do **not** depend on `:securitycore` — creeper skin is a self-contained cosmetic module.)

- [ ] **Step 5: Verify the empty module builds**

```bash
./gradlew :creeperskin:build
```

Expected: `BUILD SUCCESSFUL`. Even with zero Java sources, gradle should produce `creeperskin/build/libs/creeperskin-0.1.0.jar` containing only the processed mod metadata.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle creeperskin/build.gradle creeperskin/gradle.properties \
        creeperskin/src/main/templates/META-INF/neoforge.mods.toml
git commit -m "$(cat <<'EOF'
build(creeperskin): scaffold empty :creeperskin sibling module

Adds gradle subproject with mod_id=creeperskin, empty source tree, and
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
- Create: `creeperskin/src/main/java/com/tweeks/creeperskin/CreeperSkinMod.java`
- Create: `creeperskin/src/main/java/com/tweeks/creeperskin/Registration.java`

A minimal `@Mod` entry point that runs (and logs) at game start, plus an empty `Registration` it can call. After this task the mod loads in dev client but ships zero items.

- [ ] **Step 1: Create `Registration.java` with empty deferred registers**

Create `creeperskin/src/main/java/com/tweeks/creeperskin/Registration.java`:

```java
package com.tweeks.creeperskin;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(CreeperSkinMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreeperSkinMod.MOD_ID);

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
```

(Item and tab declarations are added in Task 5. The handles are registered now so `register(modEventBus)` is correct from the start.)

- [ ] **Step 2: Create `CreeperSkinMod.java`**

Create `creeperskin/src/main/java/com/tweeks/creeperskin/CreeperSkinMod.java`:

```java
package com.tweeks.creeperskin;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CreeperSkinMod.MOD_ID)
public class CreeperSkinMod {
    public static final String MOD_ID = "creeperskin";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreeperSkinMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Creeper Skin mod loading");
        Registration.register(modEventBus);
    }
}
```

(`SetBonusHandler` registration on `NeoForge.EVENT_BUS` lands in Task 8. Until then there are no event subscribers.)

- [ ] **Step 3: Compile**

```bash
./gradlew :creeperskin:compileJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Boot the dev client and confirm the mod loads**

```bash
./gradlew :creeperskin:runClient
```

Expected: client launches without error. From the title screen, click **Mods**. Verify both `Creeper Skin 0.1.0` and the existing mods (`securitycore`, `securityguard`, `thief`) appear in the list. Quit the client.

- [ ] **Step 5: Commit**

```bash
git add creeperskin/src/main/java/com/tweeks/creeperskin/CreeperSkinMod.java \
        creeperskin/src/main/java/com/tweeks/creeperskin/Registration.java
git commit -m "$(cat <<'EOF'
feat(creeperskin): add empty @Mod entry point and DeferredRegisters

CreeperSkinMod loads at game start, calls Registration.register on the
mod event bus. Registration declares the ITEMS and CREATIVE_TABS
DeferredRegisters but contains no entries yet — items land in the next
commit. Confirmed loadable in dev client.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Define the `CreeperArmorMaterials.CREEPER` material and equipment-asset key

**Files:**
- Create: `creeperskin/src/main/java/com/tweeks/creeperskin/item/CreeperArmorMaterials.java`

`ArmorMaterial` is a public record in `net.minecraft.world.item.equipment` with these fields: `(int durability, Map<ArmorType, Integer> defense, int enchantmentValue, Holder<SoundEvent> equipSound, float toughness, float knockbackResistance, TagKey<Item> repairIngredient, ResourceKey<EquipmentAsset> assetId)`. We mirror vanilla `ArmorMaterials.NETHERITE` exactly (durability 37, defense `(boots 3, legs 6, chest 8, helm 3, body 19)`, enchant 15, sound `ARMOR_EQUIP_NETHERITE`, toughness 3.0F, KB 0.1F, repair `ItemTags.REPAIRS_NETHERITE_ARMOR`) but swap the `assetId` for our own `creeperskin:creeper` key so the worn texture comes from our equipment JSON.

- [ ] **Step 1: Create `CreeperArmorMaterials.java`**

Create `creeperskin/src/main/java/com/tweeks/creeperskin/item/CreeperArmorMaterials.java`:

```java
package com.tweeks.creeperskin.item;

import com.tweeks.creeperskin.CreeperSkinMod;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class CreeperArmorMaterials {
    private CreeperArmorMaterials() {}

    /** Equipment-asset id used by the {@link #CREEPER} material. Resolves to
     *  {@code assets/creeperskin/equipment/creeper.json} (defining the
     *  worn-armor texture layers) at runtime. */
    public static final ResourceKey<EquipmentAsset> CREEPER_ASSET =
        ResourceKey.create(EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(CreeperSkinMod.MOD_ID, "creeper"));

    /** Netherite-tier defense map: boots 3, legs 6, chest 8, helmet 3, body 19. */
    private static final Map<ArmorType, Integer> DEFENSE = Map.of(
        ArmorType.BOOTS,      3,
        ArmorType.LEGGINGS,   6,
        ArmorType.CHESTPLATE, 8,
        ArmorType.HELMET,     3,
        ArmorType.BODY,       19);

    /** Cosmetic creeper-skin armor material. Stat-equivalent to vanilla
     *  netherite (durability 37 base, toughness 3.0, knockback resist 0.1,
     *  enchantability 15, fire-resistant via {@link Item.Properties#fireResistant}
     *  on the items themselves) — only the equipment asset and the texture
     *  it points at are different. */
    public static final ArmorMaterial CREEPER = new ArmorMaterial(
        37,
        DEFENSE,
        15,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        3.0F,
        0.1F,
        ItemTags.REPAIRS_NETHERITE_ARMOR,
        CREEPER_ASSET);
}
```

- [ ] **Step 2: Compile**

```bash
./gradlew :creeperskin:compileJava
```

Expected: `BUILD SUCCESSFUL`. The constant compiles even though no item references it yet.

- [ ] **Step 3: Commit**

```bash
git add creeperskin/src/main/java/com/tweeks/creeperskin/item/CreeperArmorMaterials.java
git commit -m "$(cat <<'EOF'
feat(creeperskin): add CreeperArmorMaterials.CREEPER + equipment asset key

ArmorMaterial record clones vanilla NETHERITE values (durability 37,
defense 3/6/8/3/19, enchant 15, ARMOR_EQUIP_NETHERITE sound, toughness
3.0, KB 0.1, REPAIRS_NETHERITE_ARMOR repair) but swaps the assetId for
creeperskin:creeper. The equipment JSON for that asset is added later
in this plan.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Register the four armor items + creative tab + lang entries

**Files:**
- Modify: `creeperskin/src/main/java/com/tweeks/creeperskin/Registration.java`
- Create: `creeperskin/src/main/java/com/tweeks/creeperskin/data/DataGenerators.java`
- Create: `creeperskin/src/main/java/com/tweeks/creeperskin/data/ModLanguageProvider.java`

After this task the four armor items exist in-game (in the `Creeper Skin` creative tab) with full netherite stats — but rendered with vanilla magenta missing-texture squares since no equipment JSON exists yet (Task 6).

`Item.Properties.humanoidArmor(ArmorMaterial, ArmorType)` is the canonical wiring call: it sets durability, attribute modifiers (defense + toughness + KB resist), enchantable value, the `EQUIPPABLE` data component (slot + equip sound + asset id), and the repair tag — all in one chained call. We then add `.fireResistant().stacksTo(1)` for netherite parity.

- [ ] **Step 1: Replace `Registration.java` with the full four-item + tab definition**

Open `creeperskin/src/main/java/com/tweeks/creeperskin/Registration.java`. Replace the entire file body with:

```java
package com.tweeks.creeperskin;

import com.tweeks.creeperskin.item.CreeperArmorMaterials;
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
        DeferredRegister.createItems(CreeperSkinMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreeperSkinMod.MOD_ID);

    public static final DeferredItem<Item> CREEPER_HELMET = ITEMS.registerItem("creeper_helmet",
        Item::new,
        p -> p.humanoidArmor(CreeperArmorMaterials.CREEPER, ArmorType.HELMET)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredItem<Item> CREEPER_CHESTPLATE = ITEMS.registerItem("creeper_chestplate",
        Item::new,
        p -> p.humanoidArmor(CreeperArmorMaterials.CREEPER, ArmorType.CHESTPLATE)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredItem<Item> CREEPER_LEGGINGS = ITEMS.registerItem("creeper_leggings",
        Item::new,
        p -> p.humanoidArmor(CreeperArmorMaterials.CREEPER, ArmorType.LEGGINGS)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredItem<Item> CREEPER_BOOTS = ITEMS.registerItem("creeper_boots",
        Item::new,
        p -> p.humanoidArmor(CreeperArmorMaterials.CREEPER, ArmorType.BOOTS)
              .fireResistant()
              .stacksTo(1));

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
}
```

- [ ] **Step 2: Create the language provider**

Create `creeperskin/src/main/java/com/tweeks/creeperskin/data/ModLanguageProvider.java`:

```java
package com.tweeks.creeperskin.data;

import com.tweeks.creeperskin.CreeperSkinMod;
import com.tweeks.creeperskin.Registration;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(output, CreeperSkinMod.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + CreeperSkinMod.MOD_ID, "Creeper Skin");
        add(Registration.CREEPER_HELMET.get(),     "Creeper Helmet");
        add(Registration.CREEPER_CHESTPLATE.get(), "Creeper Chestplate");
        add(Registration.CREEPER_LEGGINGS.get(),   "Creeper Leggings");
        add(Registration.CREEPER_BOOTS.get(),      "Creeper Boots");
    }
}
```

- [ ] **Step 3: Create the datagen bus subscriber**

Create `creeperskin/src/main/java/com/tweeks/creeperskin/data/DataGenerators.java`:

```java
package com.tweeks.creeperskin.data;

import com.tweeks.creeperskin.CreeperSkinMod;
import net.minecraft.data.DataGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = CreeperSkinMod.MOD_ID)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherDataClient(GatherDataEvent.Client event) {
        DataGenerator gen = event.getGenerator();
        gen.addProvider(true, new ModLanguageProvider(gen.getPackOutput()));
    }
}
```

(No `gatherDataServer` for now — we have no recipes, no loot tables, no advancements. If that changes, mirror the `:securityguard` `DataGenerators` shape.)

- [ ] **Step 4: Run datagen**

```bash
./gradlew :creeperskin:runData
```

Expected: `BUILD SUCCESSFUL`. New file appears at:
```
creeperskin/src/generated/clientData/assets/creeperskin/lang/en_us.json
```
with the four `item.creeperskin.*` entries plus `itemGroup.creeperskin` set to `"Creeper Skin"`.

Verify:
```bash
cat creeperskin/src/generated/clientData/assets/creeperskin/lang/en_us.json
```

- [ ] **Step 5: Compile and boot the client**

```bash
./gradlew :creeperskin:runClient
```

Expected: client launches. Open creative inventory → switch to the `Creeper Skin` tab. Confirm four entries appear, named `Creeper Helmet`, `Creeper Chestplate`, `Creeper Leggings`, `Creeper Boots` (the icons will be magenta missing-texture squares since item icons land in Task 6 — that is **expected** at this point). Hover over the helmet — the tooltip should show armor stats (`+3 Armor`, `+3 Armor Toughness`, `+0.1 Knockback Resistance` when in head slot). Equip all four; armor HUD shows 10 full chevrons (20 armor points) and the player body still renders with vanilla skin (worn texture lands in Task 6). Quit.

- [ ] **Step 6: Commit**

```bash
git add creeperskin/src/main/java/com/tweeks/creeperskin/Registration.java \
        creeperskin/src/main/java/com/tweeks/creeperskin/data/DataGenerators.java \
        creeperskin/src/main/java/com/tweeks/creeperskin/data/ModLanguageProvider.java \
        creeperskin/src/generated/clientData/assets/creeperskin/lang/en_us.json \
        creeperskin/src/generated/clientData/.cache
git commit -m "$(cat <<'EOF'
feat(creeperskin): register 4-piece armor set with netherite stats

Helmet, chestplate, leggings, boots register via plain Item::new with
Item.Properties.humanoidArmor(CREEPER, ArmorType.X) handling durability,
attributes, equippable component, and repair. Creates a Creeper Skin
creative tab with the four pieces. Datagen produces en_us.json. Worn
appearance is still vanilla skin / magenta icons — equipment JSON and
PNGs land in the next commit.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Generate textures and the equipment-asset JSON

**Files:**
- Create: `creeperskin/tools/generate_textures.py`
- Create: `creeperskin/src/main/resources/assets/creeperskin/equipment/creeper.json`
- Create: `creeperskin/src/main/resources/assets/creeperskin/textures/entity/equipment/humanoid/creeper.png` (64×32)
- Create: `creeperskin/src/main/resources/assets/creeperskin/textures/entity/equipment/humanoid_leggings/creeper.png` (64×32)
- Create: `creeperskin/src/main/resources/assets/creeperskin/textures/item/creeper_helmet.png` (16×16)
- Create: `creeperskin/src/main/resources/assets/creeperskin/textures/item/creeper_chestplate.png` (16×16)
- Create: `creeperskin/src/main/resources/assets/creeperskin/textures/item/creeper_leggings.png` (16×16)
- Create: `creeperskin/src/main/resources/assets/creeperskin/textures/item/creeper_boots.png` (16×16)
- Create: `creeperskin/src/main/resources/assets/creeperskin/items/creeper_helmet.json`
- Create: `creeperskin/src/main/resources/assets/creeperskin/items/creeper_chestplate.json`
- Create: `creeperskin/src/main/resources/assets/creeperskin/items/creeper_leggings.json`
- Create: `creeperskin/src/main/resources/assets/creeperskin/items/creeper_boots.json`
- Create: `creeperskin/src/main/resources/assets/creeperskin/models/item/creeper_helmet.json`
- Create: `creeperskin/src/main/resources/assets/creeperskin/models/item/creeper_chestplate.json`
- Create: `creeperskin/src/main/resources/assets/creeperskin/models/item/creeper_leggings.json`
- Create: `creeperskin/src/main/resources/assets/creeperskin/models/item/creeper_boots.json`

This task makes the worn armor visible. We do **not** add the creeper face yet — that is Task 7, in isolation, so a regression in face-pixel coordinates is easy to revert without losing the working green-armor base.

The equipment JSON points the `creeperskin:creeper` asset id (declared in Task 4) at two textures: one for body slots (helmet, chestplate, boots draw their layer from the same `humanoid` PNG; vanilla works the same way), one for the leggings slot.

- [ ] **Step 1: Create the equipment JSON**

Create `creeperskin/src/main/resources/assets/creeperskin/equipment/creeper.json`:

```json
{
  "layers": {
    "humanoid": [
      { "texture": "creeperskin:creeper" }
    ],
    "humanoid_leggings": [
      { "texture": "creeperskin:creeper" }
    ]
  }
}
```

The `"creeperskin:creeper"` texture id resolves to two distinct PNGs at runtime — `assets/creeperskin/textures/entity/equipment/humanoid/creeper.png` for the `humanoid` layer and `assets/creeperskin/textures/entity/equipment/humanoid_leggings/creeper.png` for the `humanoid_leggings` layer. The split-by-layer-folder convention is hardcoded in the renderer; we just provide both PNGs at the right paths in step 3.

- [ ] **Step 2: Create the texture-generation script**

Create `creeperskin/tools/generate_textures.py`:

```python
#!/usr/bin/env python3
"""
Regenerates the six PNG assets for :creeperskin from a small palette.
Run with: python3 creeperskin/tools/generate_textures.py
Output paths are absolute under the module's resources tree, so the
script can be re-run any time the palette is tweaked. Idempotent.
"""

from PIL import Image
from pathlib import Path

# Palette
GREEN_BASE  = (0x0D, 0xA7, 0x0D, 255)
GREEN_SHADE = (0x0A, 0x8D, 0x0A, 255)
FACE_BLACK  = (0x0F, 0x0F, 0x0F, 255)
TRANSPARENT = (0, 0, 0, 0)

ROOT = Path(__file__).resolve().parents[1]
ENTITY_BASE   = ROOT / "src/main/resources/assets/creeperskin/textures/entity/equipment"
ITEM_TEXTURES = ROOT / "src/main/resources/assets/creeperskin/textures/item"


def make_humanoid_body() -> Image.Image:
    """64×32 humanoid armor sheet, plain creeper green with shading.
       Face is added in a separate task; this is a clean green base."""
    img = Image.new("RGBA", (64, 32), TRANSPARENT)
    # Vanilla netherite uses opaque pixels in the cube-net regions and
    # transparent elsewhere. Easiest robust approach: fill everything with
    # the green base, then re-zero the four-pixel margins the vanilla UV
    # layout leaves transparent. The renderer ignores transparent pixels,
    # so over-paint in the empty UV gutter is harmless.
    for x in range(64):
        for y in range(32):
            img.putpixel((x, y), GREEN_BASE)
    # Light shading along a diagonal seam to give the armor depth.
    for i in range(0, 64, 8):
        for y in range(32):
            img.putpixel((i, y), GREEN_SHADE)
    return img


def make_humanoid_leggings() -> Image.Image:
    """64×32 leggings sheet, uniform creeper green (no shading, no face)."""
    img = Image.new("RGBA", (64, 32), TRANSPARENT)
    for x in range(64):
        for y in range(32):
            img.putpixel((x, y), GREEN_BASE)
    return img


def make_item_icon(armor_type: str) -> Image.Image:
    """16×16 inventory icon. Simple silhouette so the four items are
       visually distinct in the creative tab. Shape varies by piece."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    if armor_type == "helmet":
        # Wide rectangle with a dome on top
        for y in range(2, 8):
            for x in range(3, 13):
                img.putpixel((x, y), GREEN_BASE)
        for x in range(5, 11):
            img.putpixel((x, 1), GREEN_BASE)
        # creeper face on the front of the helmet icon
        img.putpixel((6, 4), FACE_BLACK); img.putpixel((9, 4), FACE_BLACK)
        img.putpixel((7, 6), FACE_BLACK); img.putpixel((8, 6), FACE_BLACK)
    elif armor_type == "chestplate":
        for y in range(2, 13):
            for x in range(3, 13):
                img.putpixel((x, y), GREEN_BASE)
        # carve out neck slot
        img.putpixel((7, 2), TRANSPARENT); img.putpixel((8, 2), TRANSPARENT)
    elif armor_type == "leggings":
        for y in range(1, 12):
            for x in range(3, 7):
                img.putpixel((x, y), GREEN_BASE)
            for x in range(9, 13):
                img.putpixel((x, y), GREEN_BASE)
    elif armor_type == "boots":
        for y in range(8, 14):
            for x in range(2, 7):
                img.putpixel((x, y), GREEN_BASE)
            for x in range(9, 14):
                img.putpixel((x, y), GREEN_BASE)
    else:
        raise ValueError(f"unknown armor_type {armor_type!r}")
    return img


def write(path: Path, img: Image.Image) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print(f"wrote {path.relative_to(ROOT.parent)}")


def main() -> None:
    write(ENTITY_BASE / "humanoid" / "creeper.png", make_humanoid_body())
    write(ENTITY_BASE / "humanoid_leggings" / "creeper.png", make_humanoid_leggings())
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        write(ITEM_TEXTURES / f"creeper_{piece}.png", make_item_icon(piece))


if __name__ == "__main__":
    main()
```

- [ ] **Step 3: Run the script to generate the six PNGs**

```bash
python3 creeperskin/tools/generate_textures.py
```

Expected output:

```
wrote creeperskin/src/main/resources/assets/creeperskin/textures/entity/equipment/humanoid/creeper.png
wrote creeperskin/src/main/resources/assets/creeperskin/textures/entity/equipment/humanoid_leggings/creeper.png
wrote creeperskin/src/main/resources/assets/creeperskin/textures/item/creeper_helmet.png
wrote creeperskin/src/main/resources/assets/creeperskin/textures/item/creeper_chestplate.png
wrote creeperskin/src/main/resources/assets/creeperskin/textures/item/creeper_leggings.png
wrote creeperskin/src/main/resources/assets/creeperskin/textures/item/creeper_boots.png
```

(If `Pillow` isn't installed, run `pip3 install Pillow` first. The same dependency is used by `:thief`'s texture generator.)

Verify dimensions:

```bash
python3 -c "from PIL import Image; \
import sys; \
paths=['creeperskin/src/main/resources/assets/creeperskin/textures/entity/equipment/humanoid/creeper.png', \
       'creeperskin/src/main/resources/assets/creeperskin/textures/entity/equipment/humanoid_leggings/creeper.png']; \
[print(p, Image.open(p).size) for p in paths]"
```

Expected: each line prints `... (64, 32)`.

- [ ] **Step 4: Create the four item-model selectors**

Each is a one-line JSON pointing at the matching item model. Create:

`creeperskin/src/main/resources/assets/creeperskin/items/creeper_helmet.json`:
```json
{ "model": { "type": "minecraft:model", "model": "creeperskin:item/creeper_helmet" } }
```

`creeperskin/src/main/resources/assets/creeperskin/items/creeper_chestplate.json`:
```json
{ "model": { "type": "minecraft:model", "model": "creeperskin:item/creeper_chestplate" } }
```

`creeperskin/src/main/resources/assets/creeperskin/items/creeper_leggings.json`:
```json
{ "model": { "type": "minecraft:model", "model": "creeperskin:item/creeper_leggings" } }
```

`creeperskin/src/main/resources/assets/creeperskin/items/creeper_boots.json`:
```json
{ "model": { "type": "minecraft:model", "model": "creeperskin:item/creeper_boots" } }
```

- [ ] **Step 5: Create the four item-model JSONs**

Each uses `item/generated` (the standard parent for inventory-only icons; `item/handheld` is for held tools).

`creeperskin/src/main/resources/assets/creeperskin/models/item/creeper_helmet.json`:
```json
{ "parent": "minecraft:item/generated", "textures": { "layer0": "creeperskin:item/creeper_helmet" } }
```

`creeperskin/src/main/resources/assets/creeperskin/models/item/creeper_chestplate.json`:
```json
{ "parent": "minecraft:item/generated", "textures": { "layer0": "creeperskin:item/creeper_chestplate" } }
```

`creeperskin/src/main/resources/assets/creeperskin/models/item/creeper_leggings.json`:
```json
{ "parent": "minecraft:item/generated", "textures": { "layer0": "creeperskin:item/creeper_leggings" } }
```

`creeperskin/src/main/resources/assets/creeperskin/models/item/creeper_boots.json`:
```json
{ "parent": "minecraft:item/generated", "textures": { "layer0": "creeperskin:item/creeper_boots" } }
```

- [ ] **Step 6: Boot the client and verify**

```bash
./gradlew :creeperskin:runClient
```

Expected:
1. Open creative inventory → `Creeper Skin` tab. Each of the four icons is creeper-green (no more magenta).
2. Equip all four pieces. Press `F5` to switch to third-person view. Player body, head, legs, and feet all render in creeper green. **No creeper face on the helmet yet** — that's Task 7. The base armor should be plain green.
3. Damage the player (e.g. `/effect give @s minecraft:wither 1 0`). Confirm armor durability bars appear under each piece's icon.

Quit the client.

- [ ] **Step 7: Commit**

```bash
git add creeperskin/tools/generate_textures.py \
        creeperskin/src/main/resources/assets/creeperskin/equipment/ \
        creeperskin/src/main/resources/assets/creeperskin/textures/ \
        creeperskin/src/main/resources/assets/creeperskin/items/ \
        creeperskin/src/main/resources/assets/creeperskin/models/
git commit -m "$(cat <<'EOF'
feat(creeperskin): plain creeper-green armor textures + item icons

Adds the equipment asset JSON (creeperskin:creeper -> humanoid +
humanoid_leggings layers), 64×32 PNGs for both layers in solid creeper
green with light seam shading, and 16×16 inventory icons for all four
pieces. Generated by creeperskin/tools/generate_textures.py for
reproducibility (same Pillow pattern :thief uses).

Helmet face lands in the next commit so a regression in face-pixel
coordinates can be reverted in isolation without losing the working
green base.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Paint the creeper face onto the helmet region

**Files:**
- Modify: `creeperskin/tools/generate_textures.py`
- Modify: `creeperskin/src/main/resources/assets/creeperskin/textures/entity/equipment/humanoid/creeper.png`

The vanilla humanoid armor texture (`assets/minecraft/textures/entity/equipment/humanoid/netherite.png`, 64×32) places the **helmet's front face** at UV `(8, 8)–(15, 15)` — an 8×8 pixel square in the upper-left third of the sheet. Paint the iconic creeper face there: two square eyes and an inverted-T mouth, in `#0F0F0F`. The face proportions mirror vanilla `assets/minecraft/textures/entity/creeper/creeper.png`'s front-of-head region, scaled to 8×8.

- [ ] **Step 1: Add a face-painting helper to the texture script**

Open `creeperskin/tools/generate_textures.py`. Replace the `make_humanoid_body` function with this version that paints the face after laying down the green base:

```python
def make_humanoid_body() -> Image.Image:
    """64×32 humanoid armor sheet, creeper green base with the iconic
       creeper face on the helmet front (UV 8..15, 8..15)."""
    img = Image.new("RGBA", (64, 32), TRANSPARENT)
    for x in range(64):
        for y in range(32):
            img.putpixel((x, y), GREEN_BASE)
    for i in range(0, 64, 8):
        for y in range(32):
            img.putpixel((i, y), GREEN_SHADE)

    # Creeper face on the helmet front. The 8×8 region is UV (8..15, 8..15)
    # in vanilla armor layout. Pixel coordinates within that region:
    #   eyes:    (1,2)-(2,3)  and  (5,2)-(6,3)
    #   nose gap: row 4 plain green
    #   mouth:   horizontal bar (2,5)-(5,5)
    #            two prongs going down: (2,6)-(2,7) and (5,6)-(5,7)
    face_x0, face_y0 = 8, 8
    face_pixels = [
        # left eye
        (1, 2), (2, 2), (1, 3), (2, 3),
        # right eye
        (5, 2), (6, 2), (5, 3), (6, 3),
        # mouth horizontal bar
        (2, 5), (3, 5), (4, 5), (5, 5),
        # mouth left prong
        (2, 6), (2, 7),
        # mouth right prong
        (5, 6), (5, 7),
    ]
    for dx, dy in face_pixels:
        img.putpixel((face_x0 + dx, face_y0 + dy), FACE_BLACK)

    return img
```

(The pixel list is intentionally explicit so the face is reviewable in the diff. Yes, the inverted-T mouth pattern looks like ASCII art when read this way — that's the point.)

- [ ] **Step 2: Regenerate the body PNG**

```bash
python3 creeperskin/tools/generate_textures.py
```

Expected: the `wrote ...humanoid/creeper.png` line appears (along with the others — the script always writes all six, which is fine: the leggings PNG and item icons are byte-identical to before, so `git diff` will show only the body PNG changed).

Verify:

```bash
git status creeperskin/src/main/resources/assets/creeperskin/textures/
```

Expected: only `humanoid/creeper.png` is modified; the other five are unchanged.

- [ ] **Step 3: Boot the client and verify the face**

```bash
./gradlew :creeperskin:runClient
```

Expected: equip the creeper helmet, switch to third-person (`F5`), and rotate the camera so the helmet front faces it. The creeper face (eyes + inverted-T mouth) should be visible in `#0F0F0F` against the creeper-green helmet. If the face is misaligned (e.g. wraps to the back of the head), the UV offsets in `make_humanoid_body` need adjustment — `(face_x0, face_y0) = (8, 8)` matches vanilla; if it doesn't render correctly, the alternative is `(40, 8)` (the right-side helmet face in some sheet layouts). Try both before declaring it a deeper bug.

Quit the client.

- [ ] **Step 4: Commit**

```bash
git add creeperskin/tools/generate_textures.py \
        creeperskin/src/main/resources/assets/creeperskin/textures/entity/equipment/humanoid/creeper.png
git commit -m "$(cat <<'EOF'
feat(creeperskin): paint creeper face on helmet front (UV 8,8..15,15)

Adds two eyes and the iconic inverted-T mouth in #0F0F0F to the helmet
front face on the worn-armor body PNG. Coordinates locked in
generate_textures.py as an explicit pixel list so the face is reviewable
in the python diff and trivially adjusted if a future texture refactor
moves it.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: Implement `SetBonusHandler` with the two 4-piece bonuses

**Files:**
- Create: `creeperskin/src/main/java/com/tweeks/creeperskin/SetBonusHandler.java`
- Create: `creeperskin/src/test/java/com/tweeks/creeperskin/SetBonusHandlerTest.java`
- Modify: `creeperskin/src/main/java/com/tweeks/creeperskin/CreeperSkinMod.java`

After this task, full-set wearers are ignored by Creeper AI and take zero damage from any creeper explosion.

**Why event-driven:** both bonuses observe state on entities that *aren't* the wearer (a Creeper ticking its targeting AI, a `DamageSource` resolving on the wearer). Hooking these on the `Item` would require either four per-tick equip listeners (one per piece, with de-dup) or a per-wearer ticker — both messier than two event handlers.

**API confirmation (from the moddev source jar):**
- `LivingChangeTargetEvent` (NeoForge): `getEntity()` returns the targeter, `getNewAboutToBeSetTarget()` / `setNewAboutToBeSetTarget(LivingEntity)` get/set the target. `ICancellableEvent`.
- `LivingIncomingDamageEvent` (NeoForge): `getEntity()` returns the entity about to be damaged, `getSource()` returns the `DamageSource`. `ICancellableEvent` — calling `setCanceled(true)` zeroes the damage.
- `DamageTypeTags.IS_EXPLOSION` is the explosion tag. Check via `event.getSource().is(DamageTypeTags.IS_EXPLOSION)`.

- [ ] **Step 1: Write the failing helper test**

Create `creeperskin/src/test/java/com/tweeks/creeperskin/SetBonusHandlerTest.java`:

```java
package com.tweeks.creeperskin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-test for {@link SetBonusHandler}.
 *
 * <p>Best-effort: mocking {@link net.minecraft.world.entity.LivingEntity} is
 * difficult because most of its behavior depends on a live {@code Level} and
 * registry holders. We keep one self-contained smoke test here that proves
 * the helper compiles and the file structure is right; full behavioral
 * verification of {@link SetBonusHandler#isWearingFullSet} happens in the
 * manual smoke test (Task 9). If a future test framework supports
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

- [ ] **Step 2: Run the test and watch it fail**

```bash
./gradlew :creeperskin:test
```

Expected: compilation **fails** because `SetBonusHandler` doesn't exist yet:

```
error: cannot find symbol
  symbol:   class SetBonusHandler
  location: package com.tweeks.creeperskin
```

(If the test runs and just throws at runtime — also fine, that's a "fail" too. Either way the next step is implementing the handler.)

- [ ] **Step 3: Create `SetBonusHandler.java`**

Create `creeperskin/src/main/java/com/tweeks/creeperskin/SetBonusHandler.java`:

```java
package com.tweeks.creeperskin;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 4-piece set-bonus handler for the Creeper Skin armor set. Subscribes to
 * the NeoForge game event bus from {@link CreeperSkinMod}'s constructor.
 *
 * <p>Two bonuses, both gated on {@link #isWearingFullSet} returning true:
 * <ol>
 *   <li>Real {@link Creeper} mobs cannot target the wearer — handled by
 *       {@link #onCreeperTargetChange(LivingChangeTargetEvent)}.</li>
 *   <li>Any creeper-tagged explosion deals zero damage to the wearer —
 *       handled by {@link #onIncomingExplosionDamage(LivingIncomingDamageEvent)}.</li>
 * </ol>
 *
 * <p>Single source of truth for "wearing the full set" is
 * {@link #isWearingFullSet}; both bonus paths call it.
 */
public final class SetBonusHandler {
    private SetBonusHandler() {}

    /** True iff {@code entity} has all four creeper-skin pieces equipped
     *  in the matching armor slots. */
    public static boolean isWearingFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(Registration.CREEPER_HELMET.get())
            && entity.getItemBySlot(EquipmentSlot.CHEST).is(Registration.CREEPER_CHESTPLATE.get())
            && entity.getItemBySlot(EquipmentSlot.LEGS).is(Registration.CREEPER_LEGGINGS.get())
            && entity.getItemBySlot(EquipmentSlot.FEET).is(Registration.CREEPER_BOOTS.get());
    }

    /** Bonus 1: a Creeper trying to acquire a full-set wearer as a target
     *  has the new target set to {@code null}, so its targeting goal
     *  fails and it never starts fusing. */
    @SubscribeEvent
    public static void onCreeperTargetChange(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Creeper)) return;
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget != null && isWearingFullSet(newTarget)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    /** Bonus 2: incoming damage to a full-set wearer from any creeper
     *  source tagged as an explosion is canceled (zero damage applied).
     *  Other side-effects of the blast (knockback, world block damage)
     *  are unaffected. */
    @SubscribeEvent
    public static void onIncomingExplosionDamage(LivingIncomingDamageEvent event) {
        DamageSource src = event.getSource();
        if (!src.is(DamageTypeTags.IS_EXPLOSION)) return;
        if (!(src.getEntity() instanceof Creeper)) return;
        if (isWearingFullSet(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
```

- [ ] **Step 4: Wire the handler to `NeoForge.EVENT_BUS` in the mod constructor**

Open `creeperskin/src/main/java/com/tweeks/creeperskin/CreeperSkinMod.java`. Replace the entire file body with:

```java
package com.tweeks.creeperskin;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(CreeperSkinMod.MOD_ID)
public class CreeperSkinMod {
    public static final String MOD_ID = "creeperskin";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreeperSkinMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Creeper Skin mod loading");
        Registration.register(modEventBus);
        NeoForge.EVENT_BUS.register(SetBonusHandler.class);
    }
}
```

(`NeoForge.EVENT_BUS.register(SetBonusHandler.class)` registers the class — its static `@SubscribeEvent` methods are picked up automatically. We register on the *game* bus, not the mod-event bus, because `LivingChangeTargetEvent` and `LivingIncomingDamageEvent` fire during gameplay, not during mod loading.)

- [ ] **Step 5: Run the test to verify it now passes**

```bash
./gradlew :creeperskin:test
```

Expected: `BUILD SUCCESSFUL`. `SetBonusHandlerTest.classLoadsAndExposesIsWearingFullSet` passes.

- [ ] **Step 6: Run the full build**

```bash
./gradlew build
```

Expected: `BUILD SUCCESSFUL` for all modules. The new `:creeperskin` jar bundles `SetBonusHandler.class`.

- [ ] **Step 7: Commit**

```bash
git add creeperskin/src/main/java/com/tweeks/creeperskin/SetBonusHandler.java \
        creeperskin/src/main/java/com/tweeks/creeperskin/CreeperSkinMod.java \
        creeperskin/src/test/java/com/tweeks/creeperskin/SetBonusHandlerTest.java
git commit -m "$(cat <<'EOF'
feat(creeperskin): 4-piece set bonus — creepers ignore + zero blast damage

SetBonusHandler subscribes to NeoForge.EVENT_BUS and gates both bonuses
on isWearingFullSet (single source of truth). Creepers that try to
target a full-set wearer have their new target set to null; any
creeper-tagged explosion against a full-set wearer is canceled (0
damage). Partial-set wearers get netherite-stat armor and nothing else.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 9: Manual end-to-end smoke test in the dev client

**Files:** none modified (verification only).

This is the gate before declaring the feature done. Run through every spec requirement explicitly, in order. Do **not** skip steps even if the previous task's spot-check looked right — set bonuses interact with multiple subsystems (AI, damage, render) and an isolated render bug can mask a behavioral bug.

- [ ] **Step 1: Boot the dev client**

```bash
./gradlew :creeperskin:runClient
```

Wait for the title screen.

- [ ] **Step 2: Verify creative tab + item names**

Open creative inventory. Switch to the `Creeper Skin` tab. Confirm exactly four items appear, in this order: Creeper Helmet, Creeper Chestplate, Creeper Leggings, Creeper Boots. Each icon is creeper-green; helmet icon shows tiny black face dots.

- [ ] **Step 3: Verify worn appearance + face**

Equip all four pieces. Press `F5` to switch to third-person. Rotate camera so the player faces it. Verify:
- Body, helmet, legs, and boots are all creeper-green.
- The helmet front shows the creeper face (two eyes + inverted-T mouth, both `#0F0F0F`).

- [ ] **Step 4: Verify armor stats**

The HUD should show 10 full armor chevrons (= 20 armor points) and the chestplate icon should display its blue toughness backing. Hover each piece in the inventory and confirm the tooltip lists `+ X Armor` matching the spec (helmet 3, chest 8, legs 6, boots 3) and `+3 Armor Toughness` per piece, `+0.1 Knockback Resistance` per piece.

- [ ] **Step 5: Verify fire resistance**

Stand in lava in creative for ~5 seconds. Player should not catch fire while holding `creative` flight (fire-resistant via `.fireResistant()` on each item).

- [ ] **Step 6: Verify Bonus 1 — creepers don't target wearer**

Switch to survival (`/gamemode survival`). Spawn a creeper close by: `/summon creeper ~ ~ ~5`. Stand still. The creeper should approach via wandering pathfinding but **never start fusing** (no white flash, no hiss). Walk close enough that vanilla creeper-fuse range would normally trigger; confirm it never does.

- [ ] **Step 7: Verify Bonus 1 fail-closed on partial set**

Remove the helmet. Spawn another creeper. Confirm it **does** target you and start fusing. Re-equip the helmet; the fusing creeper might continue fusing (it already targeted you before re-equip — that's OK, the bonus only blocks *new* targets), but a freshly-spawned creeper won't.

- [ ] **Step 8: Verify Bonus 2 — zero damage from creeper explosion**

Re-equip the full set. Force-ignite a creeper next to you: `/summon creeper ~ ~ ~1 {ignited:1b,Fuse:1}`. It detonates within a tick. Confirm:
- HP bar unchanged (no hearts lost).
- Knockback still applied (you slide a bit) — that's intentional; only damage is canceled.
- World blocks around the explosion are still destroyed (in survival creative-mode-disabled gameplay).

- [ ] **Step 9: Verify Bonus 2 specificity — TNT still hurts**

Place a TNT block, ignite it, stand within blast radius. Confirm you take damage (the damage source is TNT-tagged but `getEntity()` is a `PrimedTnt`, not a `Creeper`, so the handler's `instanceof Creeper` short-circuits). HP drops as expected.

- [ ] **Step 10: Verify Bonus 2 specificity — charged creeper still zeroed**

Spawn a charged creeper: `/summon creeper ~ ~ ~1 {ignited:1b,Fuse:1,powered:1b}`. Confirm zero damage applied even though the explosion is much larger.

- [ ] **Step 11: Quit the client**

If any step above fails, **stop and diagnose** — the spec requires all of: full-set cosmetic ✓, netherite stats ✓, both bonuses gated correctly ✓, bonuses don't apply to partial sets ✓, bonuses don't apply to non-creeper sources ✓.

- [ ] **Step 12: No commit**

Verification only.

---

## Task 10: Strike the todo line and final commit

**Files:**
- Modify: `todo.md`

- [ ] **Step 1: Open `todo.md` and remove the `creeper skin` line**

The current `todo.md` ends with:

```
    - secretely attacks all security guards
    - 
- creeper skin
```

After: drop the trailing `- creeper skin` bullet and the blank line above it if present:

```
    - secretely attacks all security guards
    - 
```

- [ ] **Step 2: Commit**

```bash
git add todo.md
git commit -m "$(cat <<'EOF'
chore: strike completed creeper skin item from todo.md

The :creeperskin module ships a 4-piece netherite-tier armor set with
creeper-green textures, the creeper face on the helmet, and a 4-piece
bonus (creepers ignore the wearer, creeper explosions deal zero damage).
End-to-end smoke test in the dev client passes.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 3: Verify the full repo still builds + tests pass**

```bash
./gradlew build test
```

Expected: `BUILD SUCCESSFUL`. All four modules build, all tests pass. The plan is complete.

---

## Self-review notes

- **Spec coverage:**
  - `Goal.1` (cosmetic, full body) → Tasks 6 + 7 (PNGs at all four UV regions, face on helmet).
  - `Goal.2` (netherite-tier stats) → Task 4 (material constant) + Task 5 (`humanoidArmor(...)` chain).
  - `Goal.3` (full-set bonuses) → Task 8 (`SetBonusHandler`).
  - `Goal.4` (creative tab + `/give`, no recipe) → Task 5 (creative tab); no recipe step exists, matching the spec.
  - `Module placement` (new `:creeperskin` sibling) → Tasks 2 + 3.
  - `Texture content` (palette + creeper face on helmet) → Task 6 (palette) + Task 7 (face).
  - `Single source of truth for full-set` (`isWearingFullSet`) → Task 8 step 3.
  - `Test strategy` → Task 8 (helper test) + Task 9 (manual smoke).
- **Placeholder scan:** zero `TBD` / `TODO` / `implement later` / `add validation` / "similar to Task N" instances. Every code step contains the actual code; every command step contains the exact command and expected output.
- **Type/name consistency:**
  - `CreeperSkinMod.MOD_ID` referenced consistently in Tasks 3, 4, 5, 8.
  - `Registration.CREEPER_HELMET` / `CREEPER_CHESTPLATE` / `CREEPER_LEGGINGS` / `CREEPER_BOOTS` declared in Task 5, referenced in Tasks 5 (lang) and 8 (`isWearingFullSet`).
  - `CreeperArmorMaterials.CREEPER` declared in Task 4, used in Task 5.
  - `CreeperArmorMaterials.CREEPER_ASSET` declared in Task 4 — not referenced again because it's only consumed indirectly (`material.assetId()` reads it inside `humanoidArmor`).
  - Equipment-asset id `creeperskin:creeper` is consistent across `CreeperArmorMaterials.CREEPER_ASSET` (Java) and `assets/creeperskin/equipment/creeper.json` (resource path). The texture id `"creeperskin:creeper"` resolves to `humanoid/creeper.png` and `humanoid_leggings/creeper.png` — both PNG paths spelled correctly in Task 6.
- **Scope:** plan produces a working, testable feature in one PR-sized push. No cross-module changes; `securitycore`, `securityguard`, `thief` are untouched.
- **Frequent commits:** ten task commits + one cleanup commit. Each commit leaves the repo in a buildable state. Tasks 6 and 7 are intentionally split so a face-coordinate regression is reversible without losing the green-armor base.
