# Han Solo Armor (Scoundrel Set) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A full 4-piece Han Solo armor set with netherite-equivalent stats, a "Scoundrel's Luck" full-set bonus (first blaster shot against each new target deals double damage), finished art, and honest Bedrock output.

**Architecture:** Material/items/art mirror the proven stormtrooper-armor pipeline (`StormtrooperArmorMaterials`, `Registration.humanoidArmor`, `ARMOR_PIECES` bbmodels, worn-layer painter in `gen_textures.py`). The set bonus is one hook in `BlasterPistolItem.use()` backed by `ScoundrelLuck` — a `Disguise`-style full-set check plus a transient server-side `UUID → QuickdrawState` map reusing the already-tested quickdraw state machine.

**Tech Stack:** Java (NeoForge, `starwars` module), Kotlin (`translator`), Python (art tools), JUnit.

## Global Constraints

Copied from the spec (`docs/superpowers/specs/2026-07-12-han-solo-armor-design.md`):

- **NO placeholder art:** 3+ tones per material region, painted shading; bbmodel sources committed and editable.
- **Work on `main` directly, NO branch operations.** Stage ONLY files your task names. NEVER touch pre-existing user WIP in `thief/`, `securityguard/`, `wildwest/`, `craftee/`, root gradle files, or their `bedrock-out/` trees.
- **All netherite numbers are lifted verbatim from decompiled `ArmorMaterials.NETHERITE`** (sources jar extracted under `*/build/moddev/artifacts` — `find starwars/build/moddev -name "*sources*"`); the figures in this plan are expected values to CONFIRM, not authorities. Same for the fire-resistance mechanism (check decompiled `Items.java`'s netherite armor registrations).
- **Bedrock honesty:** the set bonus gets an `UNTRANSLATABLE.md` entry; `bedrock-out/` is generated, never hand-edited.
- **Tool ordering:** texture generators run BEFORE `gen_bbmodels.py`; regenerating must leave all sibling generated files byte-identical.
- Datagen byte-deterministic (run twice, second run zero diff). Suites currently green: `:starwars:test` 68, `:translator:test` 168.
- Commits `feat(starwars): ...` / `feat(translator): ...` ending with `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`.
- Inside entity classes `com.tweeks.starwars.Registration` must be fully-qualified (DebugValueSource shadowing) — does NOT apply to `ScoundrelLuck`/items (not entity subclasses); plain import is fine there, matching `Disguise`.

---

### Task 1: HanSoloArmorMaterials + items + equipment asset

**Files:**
- Create: `starwars/src/main/java/com/tweeks/starwars/item/HanSoloArmorMaterials.java`
- Create: `starwars/src/main/resources/assets/starwars/equipment/han_solo.json`
- Modify: `starwars/src/main/java/com/tweeks/starwars/Registration.java`
- Modify: `starwars/src/main/java/com/tweeks/starwars/data/ModLanguageProvider.java`

**Interfaces:**
- Consumes: `StormtrooperArmorMaterials` (shape donor, read it first), decompiled `ArmorMaterials.java` + `Items.java`.
- Produces: `HanSoloArmorMaterials.HAN_SOLO` (`ArmorMaterial`); `Registration.HAN_SOLO_HELMET/CHESTPLATE/LEGGINGS/BOOTS` (`DeferredItem<Item>`) — Tasks 2 (set check), 4 (recipes) rely on these exact names.

- [ ] **Step 1: Verify netherite facts in decompiled sources**

Locate `ArmorMaterials.java` and `Items.java` in the extracted sources jar. Record in your report: the exact `NETHERITE` constructor arguments (expected: durability multiplier 37; DEFENSE map BOOTS 3 / LEGGINGS 6 / CHESTPLATE 8 / HELMET 3 / BODY 11; enchant value 15; `SoundEvents.ARMOR_EQUIP_NETHERITE`; toughness 3.0F; knockback resistance 0.1F; `ItemTags.REPAIRS_NETHERITE_ARMOR`) and how netherite armor items get fire resistance (expected: a `.fireResistant()`-style item-properties call on each item registration, not a material field — confirm the exact method name on `Item.Properties`).

- [ ] **Step 2: Write `HanSoloArmorMaterials`**

Mirror `StormtrooperArmorMaterials.java`'s exact shape with the verified netherite values:

```java
package com.tweeks.starwars.item;

import com.tweeks.starwars.StarWarsMod;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class HanSoloArmorMaterials {
    private HanSoloArmorMaterials() {}

    /** Resolves to assets/starwars/equipment/han_solo.json at runtime. */
    public static final ResourceKey<EquipmentAsset> HAN_SOLO_ASSET =
        ResourceKey.create(EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "han_solo"));

    /** Netherite-tier defense — every value verified against decompiled ArmorMaterials.NETHERITE. */
    private static final Map<ArmorType, Integer> DEFENSE = Map.of(
        ArmorType.BOOTS,      3,
        ArmorType.LEGGINGS,   6,
        ArmorType.CHESTPLATE, 8,
        ArmorType.HELMET,     3,
        ArmorType.BODY,       11);

    public static final ArmorMaterial HAN_SOLO = new ArmorMaterial(
        37,
        DEFENSE,
        15,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        3.0F,
        0.1F,
        ItemTags.REPAIRS_NETHERITE_ARMOR,
        HAN_SOLO_ASSET);
}
```

(Adjust any argument that differs from the decompiled `NETHERITE` — the decompiled source wins; record deviations.)

- [ ] **Step 3: Register the four items**

In `Registration.java` after the landspeeder, mirroring the stormtrooper pieces exactly, plus the verified fire-resistance property:

```java
    public static final DeferredItem<Item> HAN_SOLO_HELMET = ITEMS.registerItem("han_solo_helmet",
        Item::new,
        p -> p.humanoidArmor(com.tweeks.starwars.item.HanSoloArmorMaterials.HAN_SOLO,
                net.minecraft.world.item.equipment.ArmorType.HELMET)
              .stacksTo(1)
              .fireResistant());   // exact method per Step 1's Items.java check
```

…and the same for `HAN_SOLO_CHESTPLATE` (`ArmorType.CHESTPLATE`), `HAN_SOLO_LEGGINGS` (`ArmorType.LEGGINGS`), `HAN_SOLO_BOOTS` (`ArmorType.BOOTS`). Creative tab: four `output.accept(...)` lines after `LANDSPEEDER`.

- [ ] **Step 4: Equipment asset JSON**

`starwars/src/main/resources/assets/starwars/equipment/han_solo.json`, mirroring `equipment/stormtrooper.json`:

```json
{
  "layers": {
    "humanoid": [
      { "texture": "starwars:han_solo" }
    ],
    "humanoid_leggings": [
      { "texture": "starwars:han_solo" }
    ]
  }
}
```

(The two worn-layer PNGs this references arrive in Task 5 — acceptable cross-task gap, same as the landspeeder item JSON preceding its texture.)

- [ ] **Step 5: Lang entries**

```java
        add(com.tweeks.starwars.Registration.HAN_SOLO_HELMET.get(), "Han Solo's Helmet");
        add(com.tweeks.starwars.Registration.HAN_SOLO_CHESTPLATE.get(), "Han Solo's Chestplate");
        add(com.tweeks.starwars.Registration.HAN_SOLO_LEGGINGS.get(), "Han Solo's Leggings");
        add(com.tweeks.starwars.Registration.HAN_SOLO_BOOTS.get(), "Han Solo's Boots");
```

- [ ] **Step 6: Build + datagen twice (determinism)**

```bash
./gradlew :starwars:build
./gradlew :starwars:runServerData :starwars:runClientData
git status --porcelain starwars/
./gradlew :starwars:runServerData :starwars:runClientData
git status --porcelain starwars/   # no additional diff
```

Expected: green (68 tests), lang JSON updated once.

- [ ] **Step 7: Commit**

```bash
git add starwars/src/main/java/com/tweeks/starwars/item/HanSoloArmorMaterials.java starwars/src/main/resources/assets/starwars/equipment/han_solo.json starwars/src/main/java/com/tweeks/starwars/Registration.java starwars/src/main/java/com/tweeks/starwars/data/ModLanguageProvider.java starwars/src/generated
git commit -m "feat(starwars): Han Solo armor material and items — netherite-grade

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: ScoundrelLuck (set check + per-player state + logout cleanup)

**Files:**
- Create: `starwars/src/main/java/com/tweeks/starwars/faction/ScoundrelLuck.java`
- Test: `starwars/src/test/java/com/tweeks/starwars/faction/ScoundrelLuckStateTest.java`

**Interfaces:**
- Consumes: `QuickdrawState` (existing, `entity.ai`), `Registration.HAN_SOLO_*` (Task 1), `Disguise` (shape donor), `AlignmentEvents` (event-subscriber donor).
- Produces: `ScoundrelLuck.isWearingFullHanSoloSet(LivingEntity)`, `ScoundrelLuck.stateFor(UUID)`, `ScoundrelLuck.clear(UUID)` — Task 3's hook consumes the first two.

- [ ] **Step 1: Write the failing test** (pure part only — the four-slot check is engine-coupled and must NOT be called from the test: calling it would class-init `Registration` and crash outside a game context; `stateFor`/`clear` never touch it)

```java
package com.tweeks.starwars.faction;

import com.tweeks.starwars.entity.ai.QuickdrawState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ScoundrelLuckStateTest {

    private static final UUID PLAYER_A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID PLAYER_B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID TARGET = UUID.fromString("00000000-0000-0000-0000-0000000000c3");

    @AfterEach
    public void cleanup() {
        ScoundrelLuck.clear(PLAYER_A);
        ScoundrelLuck.clear(PLAYER_B);
    }

    @Test
    public void stateForReturnsSameInstancePerPlayer() {
        assertSame(ScoundrelLuck.stateFor(PLAYER_A), ScoundrelLuck.stateFor(PLAYER_A));
    }

    @Test
    public void statesAreIndependentAcrossPlayers() {
        ScoundrelLuck.stateFor(PLAYER_A).markAmbushed(TARGET);
        assertFalse(ScoundrelLuck.stateFor(PLAYER_A).canAmbush(TARGET));
        assertTrue(ScoundrelLuck.stateFor(PLAYER_B).canAmbush(TARGET));
    }

    @Test
    public void clearDropsTheEntry() {
        QuickdrawState before = ScoundrelLuck.stateFor(PLAYER_A);
        before.markAmbushed(TARGET);
        ScoundrelLuck.clear(PLAYER_A);
        assertNotSame(before, ScoundrelLuck.stateFor(PLAYER_A));
        assertTrue(ScoundrelLuck.stateFor(PLAYER_A).canAmbush(TARGET));
    }
}
```

- [ ] **Step 2: Run to verify it fails** — `./gradlew :starwars:test --tests "*ScoundrelLuckStateTest"` → FAIL (class missing).

- [ ] **Step 3: Write the implementation**

Verify the logout event's exact FQN first (`grep -rn "PlayerLoggedOut" ~/.gradle` is slow — check the NeoForge jar sources the same way earlier tasks did, or grep sibling modules for an existing subscriber; expected `net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent`).

```java
package com.tweeks.starwars.faction;

import com.tweeks.starwars.Registration;
import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.entity.ai.QuickdrawState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "Scoundrel's Luck" full-set bonus for the Han Solo armor: the wearer's
 * first blaster shot against each newly acquired target deals double damage
 * (the player-side twin of HanQuickdrawGoal). Per-player ambush memory is a
 * transient server-side map reusing {@link QuickdrawState} — deliberately
 * NOT persisted: restart/logout clears it. Entries are removed on logout;
 * access only from the server thread (item use + the logout event).
 */
@EventBusSubscriber(modid = StarWarsMod.MOD_ID)
public final class ScoundrelLuck {
    private ScoundrelLuck() {}

    private static final Map<UUID, QuickdrawState> STATES = new HashMap<>();

    public static boolean isWearingFullHanSoloSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(Registration.HAN_SOLO_HELMET.get())
            && entity.getItemBySlot(EquipmentSlot.CHEST).is(Registration.HAN_SOLO_CHESTPLATE.get())
            && entity.getItemBySlot(EquipmentSlot.LEGS).is(Registration.HAN_SOLO_LEGGINGS.get())
            && entity.getItemBySlot(EquipmentSlot.FEET).is(Registration.HAN_SOLO_BOOTS.get());
    }

    public static QuickdrawState stateFor(UUID playerId) {
        return STATES.computeIfAbsent(playerId, id -> new QuickdrawState());
    }

    public static void clear(UUID playerId) {
        STATES.remove(playerId);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity().getUUID());
    }
}
```

- [ ] **Step 4: Run to verify it passes** — `./gradlew :starwars:test --tests "*ScoundrelLuckStateTest"` → PASS, 3 tests; then full `./gradlew :starwars:build` green.

- [ ] **Step 5: Commit**

```bash
git add starwars/src/main/java/com/tweeks/starwars/faction/ScoundrelLuck.java starwars/src/test/java/com/tweeks/starwars/faction/ScoundrelLuckStateTest.java
git commit -m "feat(starwars): ScoundrelLuck — full-set check and per-player quickdraw memory

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: Scoundrel's Luck hook in the blaster fire path

**Files:**
- Modify: `starwars/src/main/java/com/tweeks/starwars/item/BlasterPistolItem.java` (the player `use()` hit-resolution block, ~lines 103-108)

**Interfaces:**
- Consumes: `ScoundrelLuck.isWearingFullHanSoloSet` / `stateFor` (Task 2), `QuickdrawState.canAmbush/markAmbushed`, existing `this.getDamage()` virtual (rifle inherits `use()` and overrides `getDamage()` — the hook covers both weapons with no rifle change).
- Produces: double-damage first shot + `CRIT` particle burst for full-set wearers.

- [ ] **Step 1: Apply the hook**

Replace the current hit block in `use()`:

```java
        if (hit.isPresent()) {
            LivingEntity target = byId.get(hit.get().id());
            target.invulnerableTime = 0;
            float damage = this.getDamage();
            // Scoundrel's Luck: a full Han Solo set doubles the first shot
            // against each newly acquired target — mark only on a landed
            // hit (misses never consume the ambush). See ScoundrelLuck.
            if (ScoundrelLuck.isWearingFullHanSoloSet(player)) {
                QuickdrawState state = ScoundrelLuck.stateFor(player.getUUID());
                if (state.canAmbush(target.getUUID())) {
                    damage = 2 * this.getDamage();
                    state.markAmbushed(target.getUUID());
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        8, 0.3, 0.3, 0.3, 0.1);
                }
            }
            target.hurtServer(serverLevel, blasterSource, damage);
            endPoint = target.position().add(0, target.getBbHeight() * 0.5, 0);
        }
```

Imports: `com.tweeks.starwars.faction.ScoundrelLuck`, `com.tweeks.starwars.entity.ai.QuickdrawState`, `net.minecraft.core.particles.ParticleTypes`. Do NOT touch `fireFromMob` (mob path keeps its own goal-driven quickdraw). Update the class javadoc with one line about the set-bonus hook.

- [ ] **Step 2: Build** — `./gradlew :starwars:build` green (71 tests).

- [ ] **Step 3: Commit**

```bash
git add starwars/src/main/java/com/tweeks/starwars/item/BlasterPistolItem.java
git commit -m "feat(starwars): Scoundrel's Luck hook — full-set double-damage first shot

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: Recipes

**Files:**
- Modify: `starwars/src/main/java/com/tweeks/starwars/data/ModRecipeProvider.java`

**Interfaces:**
- Consumes: `Registration.HAN_SOLO_*` (Task 1); donor: the stormtrooper armor recipes in the same file.
- Produces: four shaped recipes, leather + one netherite ingot each.

- [ ] **Step 1: Add the four recipes** after the landspeeder recipe (`RecipeCategory.COMBAT`, `unlockedBy("has_netherite_ingot", this.has(Items.NETHERITE_INGOT))` on each):

```java
        // Han Solo armor: leather body + one netherite ingot per piece —
        // netherite-grade stats at netherite-anchored cost (spec §5).
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.HAN_SOLO_HELMET.get())
            .pattern("LNL")
            .pattern("L L")
            .define('L', Items.LEATHER)
            .define('N', Items.NETHERITE_INGOT)
            .unlockedBy("has_netherite_ingot", this.has(Items.NETHERITE_INGOT))
            .save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.HAN_SOLO_CHESTPLATE.get())
            .pattern("L L")
            .pattern("LNL")
            .pattern("LLL")
            .define('L', Items.LEATHER)
            .define('N', Items.NETHERITE_INGOT)
            .unlockedBy("has_netherite_ingot", this.has(Items.NETHERITE_INGOT))
            .save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.HAN_SOLO_LEGGINGS.get())
            .pattern("LNL")
            .pattern("L L")
            .pattern("L L")
            .define('L', Items.LEATHER)
            .define('N', Items.NETHERITE_INGOT)
            .unlockedBy("has_netherite_ingot", this.has(Items.NETHERITE_INGOT))
            .save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.HAN_SOLO_BOOTS.get())
            .pattern("N L")
            .pattern("L L")
            .define('L', Items.LEATHER)
            .define('N', Items.NETHERITE_INGOT)
            .unlockedBy("has_netherite_ingot", this.has(Items.NETHERITE_INGOT))
            .save(this.output);
```

- [ ] **Step 2: Datagen twice + determinism check** (Task 1 Step 6 commands). Expected: four new recipe JSONs + advancements, byte-stable.

- [ ] **Step 3: Build green, commit**

```bash
git add starwars/src/main/java/com/tweeks/starwars/data/ModRecipeProvider.java starwars/src/generated
git commit -m "feat(starwars): Han Solo armor recipes — leather + netherite ingot

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 5: Art — worn layers, armor bbmodels, item sprites (ART GATE)

**Files:**
- Modify: `starwars/tools/gen_textures.py` (Han armor-layer painter + `__main__` additions)
- Modify: `starwars/tools/gen_bbmodels.py` (parameterize armor texture basename; four `han_solo_armor_*` entries)
- Modify: `starwars/tools/gen_item_textures.py` (four sprites)
- Create (generated): `starwars/src/main/resources/assets/starwars/textures/entity/equipment/humanoid/han_solo.png`, `.../humanoid_leggings/han_solo.png`, four `starwars/tools/han_solo_armor_*.bbmodel`, four `starwars/src/main/resources/assets/starwars/textures/item/han_solo_*.png`

**Interfaces:**
- Consumes: `paint_stormtrooper_armor_layers` (the worn-layer donor — its UV-extent comments are the authoritative map of which pixels vanilla samples), `ARMOR_PIECES`/`write_armor_bbmodel` (`gen_bbmodels.py:445-451, 561-568`), `STORMTROOPER_*_CUBES` tables (reused as-is — same silhouette, different paint).
- Produces: committed art referenced by Task 1's equipment asset and the item registrations.

- [ ] **Step 1: Han palette + worn-layer painter in `gen_textures.py`**

Add after the stormtrooper armor-layer section, reusing the existing `HAN_*` palette constants (shirt/vest/trouser/stripe/belt/skin already defined for the entity texture) plus:

```python
HAN_CAP      = (0x4A, 0x35, 0x22, 0xFF)   # dark-brown cap (reuses HAN_HAIR tone)
HAN_CAP_HI   = (0x64, 0x4A, 0x32, 0xFF)
HAN_CAP_DK   = (0x33, 0x24, 0x16, 0xFF)
HAN_BOOT     = (0x54, 0x3A, 0x24, 0xFF)   # brown boots
HAN_BOOT_HI  = (0x6E, 0x50, 0x34, 0xFF)
HAN_BOOT_DK  = (0x3A, 0x28, 0x18, 0xFF)

def paint_han_solo_armor_layers(humanoid_rgba, leggings_rgba):
    """Worn-armor sheets for the Han Solo equipment asset. Same sampled
    UV extents as paint_stormtrooper_armor_layers (that function's region
    comments are the authoritative map — copy its rect() extents exactly,
    substituting palettes):
      humanoid sheet — head block: HAN_CAP 3-tone cap with HAN_CAP_DK
        band row; body block: HAN_VEST over HAN_SHIRT with the open-front
        column (shirt tone showing through center), HAN_BELT+HAN_BUCKLE
        waist rows; arm block: HAN_SHIRT sleeves with HAN_VEST shoulder
        strap rows and HAN_SHIRT_DIM elbow fold; leg block (BOOTS on this
        sheet): HAN_BOOT 3-tone on the lower rows only, transparent above.
      leggings sheet — leg block only: HAN_TROUSER 3-tone with the 1px
        HAN_STRIPE bloodstripe column and HAN_TROUSER_DK inner shadow.
    Every region: 3+ tones (art gate)."""
    fill_buf(humanoid_rgba, (0, 0, 0, 0))
    fill_buf(leggings_rgba, (0, 0, 0, 0))
    # BODY: copy paint_stormtrooper_armor_layers' rect() calls region-for-
    # region (same coordinates), substituting the palettes above per the
    # docstring. Every palette constant must appear in the output.
```

The `rect()` bodies are direction, not literal code — the donor's coordinates are ground truth. Add to `__main__`, after the stormtrooper layer writes:

```python
    humanoid_rgba = bytearray(ARMOR_W * ARMOR_H * 4)
    leggings_rgba = bytearray(ARMOR_W * ARMOR_H * 4)
    paint_han_solo_armor_layers(humanoid_rgba, leggings_rgba)
    write_png(os.path.join(humanoid_dir, 'han_solo.png'), humanoid_rgba,
              width=ARMOR_W, height=ARMOR_H)
    write_png(os.path.join(leggings_dir, 'han_solo.png'), leggings_rgba,
              width=ARMOR_W, height=ARMOR_H)
    print('han solo armor layers')
```

- [ ] **Step 2: Parameterize the armor bbmodel texture + add the four pieces in `gen_bbmodels.py`**

`write_armor_bbmodel` hardcodes `'stormtrooper.png'` (line 562). Change `ARMOR_PIECES` values to 3-tuples `(cubes, folder, texture_basename)` — existing entries gain `'stormtrooper.png'` explicitly (their output must stay byte-identical) — and thread the basename through:

```python
# piece_name -> (cubes, equipment texture subfolder, texture file)
ARMOR_PIECES = {
    'stormtrooper_armor_helmet':     (STORMTROOPER_HELMET_CUBES, 'humanoid', 'stormtrooper.png'),
    'stormtrooper_armor_chestplate': (STORMTROOPER_CHESTPLATE_CUBES, 'humanoid', 'stormtrooper.png'),
    'stormtrooper_armor_leggings':   (STORMTROOPER_LEGGINGS_CUBES, 'humanoid_leggings', 'stormtrooper.png'),
    'stormtrooper_armor_boots':      (STORMTROOPER_BOOTS_CUBES, 'humanoid', 'stormtrooper.png'),
    # Han Solo set reuses the stormtrooper cube tables (same worn
    # silhouette) with the han_solo equipment texture.
    'han_solo_armor_helmet':     (STORMTROOPER_HELMET_CUBES, 'humanoid', 'han_solo.png'),
    'han_solo_armor_chestplate': (STORMTROOPER_CHESTPLATE_CUBES, 'humanoid', 'han_solo.png'),
    'han_solo_armor_leggings':   (STORMTROOPER_LEGGINGS_CUBES, 'humanoid_leggings', 'han_solo.png'),
    'han_solo_armor_boots':      (STORMTROOPER_BOOTS_CUBES, 'humanoid', 'han_solo.png'),
}
```

```python
def write_armor_bbmodel(out_dir, piece_name, cube_specs, texture_dir, folder, texture_basename='stormtrooper.png'):
    texture_path = os.path.join(texture_dir, 'equipment', folder, texture_basename)
```

…and the `__main__` loop unpacks the 3-tuple and passes it. Check `armor_texture_record`/`build_armor_bbmodel` for other hardcoded `'stormtrooper'` strings (the texture record's `name`/`relative_path` fields) and thread the basename there too.

- [ ] **Step 3: Four item sprites in `gen_item_textures.py`** — follow the file's per-item function + dispatch pattern (read it first; the stormtrooper armor sprites are the donors): cap (brown 3-tone dome), chest (black vest silhouette over off-white shirt with open front), leggings (navy with red stripe columns), boots (brown 3-tone pair). Reuse this task's palettes.

- [ ] **Step 4: Run the tools — textures FIRST, then bbmodels** (empty-embed race):

```bash
python3 starwars/tools/gen_textures.py starwars/src/main/resources/assets/starwars/textures/entity
python3 starwars/tools/gen_item_textures.py starwars/src/main/resources/assets/starwars/textures/item
python3 starwars/tools/gen_bbmodels.py starwars/tools
git status --porcelain starwars/
```

Expected new files only: 2 worn-layer PNGs, 4 sprites, 4 bbmodels (+ the 3 modified tools). **All sibling generated files byte-identical** — especially the four stormtrooper armor bbmodels (the 3-tuple refactor must not change their bytes). If any stormtrooper bbmodel diffs, the parameterization leaked — fix before committing.

- [ ] **Step 5: ART GATE** — view both worn-layer PNGs and all four sprites as images: 3+ tones per region, vest open-front visible on the body block, bloodstripe on the leggings sheet, cap band, boot shading; transparent outside the sampled UV extents (compare against the stormtrooper layers' silhouette). Iterate until pass.

- [ ] **Step 6: Build sanity + commit**

```bash
./gradlew :starwars:build -q
git add starwars/tools/gen_textures.py starwars/tools/gen_bbmodels.py starwars/tools/gen_item_textures.py starwars/tools/han_solo_armor_helmet.bbmodel starwars/tools/han_solo_armor_chestplate.bbmodel starwars/tools/han_solo_armor_leggings.bbmodel starwars/tools/han_solo_armor_boots.bbmodel starwars/src/main/resources/assets/starwars/textures/entity/equipment/humanoid/han_solo.png starwars/src/main/resources/assets/starwars/textures/entity/equipment/humanoid_leggings/han_solo.png starwars/src/main/resources/assets/starwars/textures/item/han_solo_helmet.png starwars/src/main/resources/assets/starwars/textures/item/han_solo_chestplate.png starwars/src/main/resources/assets/starwars/textures/item/han_solo_leggings.png starwars/src/main/resources/assets/starwars/textures/item/han_solo_boots.png
git commit -m "feat(starwars): Han Solo armor art — worn layers, bbmodels, item sprites

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

(Also add item model JSONs if the stormtrooper armor items have hand-authored `items/*.json` + `models/item/*.json` — check `assets/starwars/items/stormtrooper_helmet.json`; if present, mirror all four pairs for the Han pieces and include them in this commit.)

---

### Task 6: Translator — Scoundrel's Luck honesty entry

**Files:**
- Modify: `translator/src/main/kotlin/com/tweeks/translator/java/ItemAnalyzer.kt`
- Test: `translator/src/test/kotlin/com/tweeks/translator/java/ItemAnalyzerTest.kt` (additions)

**Interfaces:**
- Consumes: `Untranslatable.recordItemCustomBehavior(modId, itemId, summary)` (existing recorder), `ItemAnalyzer`'s existing custom-behavior detection.
- Produces: an `UNTRANSLATABLE.md` line stating the set bonus is Java-only.

- [ ] **Step 1: Investigate.** Read `ItemAnalyzer.kt`: find where it detects/records custom item behavior for `BlasterPistolItem` today (it already emits entries for the module's custom-use items). Determine the least invasive way to surface the set bonus: expected shape — when an analyzed item class's AST references the simple name `ScoundrelLuck` (a `NameExpr`/`ClassOrInterfaceType` scan, same technique as `EntityAnalyzer`'s SavedData detection), call:

```kotlin
unt.recordItemCustomBehavior(
    mod.modId, reg.itemId,
    "Scoundrel's Luck set bonus (full Han Solo set doubles the first blaster " +
        "shot against each new target) is server-side Java logic — absent on Bedrock.",
)
```

If the existing custom-behavior entry for the pistol already quotes method summaries that would include this, extending its summary is equally acceptable — pick whichever matches the file's structure and record the choice.

- [ ] **Step 2: TDD** — add a test in `ItemAnalyzerTest`'s existing fixture style: an item fixture whose `use()` references `ScoundrelLuck` records the set-bonus entry; a fixture without it does not. RED → implement → GREEN.

- [ ] **Step 3: Full suite** — `./gradlew :translator:test` green (168+).

- [ ] **Step 4: Commit**

```bash
git add translator/src/main/kotlin/com/tweeks/translator/java/ItemAnalyzer.kt translator/src/test/kotlin/com/tweeks/translator/java/ItemAnalyzerTest.kt
git commit -m "feat(translator): record Scoundrel's Luck set bonus as Bedrock-untranslatable

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 7: Regenerate bedrock-out/starwars

**Files:** `bedrock-out/starwars/**` ONLY (generated).

- [ ] **Step 1: Run the scoped translate twice** — `./gradlew :translator:translate --args="starwars"` (NO `--with-llm`); second run must produce zero additional diff; `git status --porcelain bedrock-out/` shows only `bedrock-out/starwars/`.

- [ ] **Step 2: Verify:**

```bash
ls bedrock-out/starwars/behavior_pack/items/ | grep han_solo          # 4 armor items
ls bedrock-out/starwars/behavior_pack/recipes/ | grep han_solo        # 4 recipes
ls bedrock-out/starwars/resource_pack/attachables/ | grep han_solo    # 4 worn attachables
grep -n "Scoundrel" bedrock-out/starwars/UNTRANSLATABLE.md            # set-bonus entry
```

- [ ] **Step 3: Commit**

```bash
git add bedrock-out/starwars
git commit -m "feat(starwars): regenerate Bedrock addon — Han Solo armor set

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 8: Final sweep (controller-run)

- [ ] `./gradlew :starwars:test :translator:test :starwars:build` — all green.
- [ ] Datagen determinism final check; WIP-contamination check (`git status --porcelain` filtered to non-feature paths shows only the user's pre-existing WIP).
- [ ] Ledger + smoke-checklist additions: full-set first-shot double damage with crit burst (pistol AND rifle), no bonus at 3/4 pieces, netherite-feel protection + fire resistance (stand in fire/lava with the item in inventory — items shouldn't burn), worn look on the player (cap/vest/bloodstripe/boots), Bedrock attachables.
- [ ] Final whole-branch review (most capable model) over the feature range with the accumulated Minor list; ONE fix subagent for any findings; re-review; update memory.

## Self-review notes (already applied)

- Spec §3 fire-resistance check → Task 1 Steps 1/3. §4 miss-doesn't-consume → Task 3 (mark inside `hit.isPresent()`). §4 logout cleanup → Task 2. §5 patterns/materials verbatim → Task 4. §6 donor-extent painting + sibling-byte-identity (3-tuple refactor risk called out) → Task 5. §7 honesty entry + regen → Tasks 6-7. §8 pure tests + engine-coupled exclusions (Registration class-init hazard documented in the test step) → Task 2.
- Type consistency: `HAN_SOLO_HELMET/CHESTPLATE/LEGGINGS/BOOTS`, `stateFor(UUID)`, `isWearingFullHanSoloSet(LivingEntity)`, `HanSoloArmorMaterials.HAN_SOLO` consistent across Tasks 1-5.
