# Star Wars Mod Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A comprehensive Star Wars mod (`starwars` module): 8 characters with editable bbmodels and finished textures, blasters, lightsabers, stormtrooper armor with disguise set bonus, held-item Force powers with a radial picker, faction war (Empire vs Light) with player alignment, and three code-generated worldgen structures — Java + Bedrock.

**Architecture:** New NeoForge module mirroring wildwest's layout. Faction targeting via `SwCombatant` marker + `NearestAttackableTargetGoal` predicates; player alignment via a null-default `AttachmentType`; named characters (Vader, Luke, Obi-Wan, Boba Fett) are per-world singletons via `SavedData` (BossSingletonSavedData pattern); blasters re-implement the wildwest hitscan design; Force powers reuse the Infinity Gauntlet component/cooldown/radial-picker patterns; structures use registered `Structure`/`StructurePiece` with pure-code piece layouts.

**Tech Stack:** NeoForge (versions from root `gradle.properties` — do NOT hardcode; currently `minecraft_version=26.1.2`, `neo_version=26.1.2.30-beta`), Java 25 toolchain, JUnit 5, Python 3 for `tools/` generators, Kotlin translator for Bedrock output.

**Spec:** `docs/superpowers/specs/2026-07-11-starwars-mod-design.md`

## Global Constraints

- Package root `com.tweeks.starwars`, `MOD_ID = "starwars"`, mod name "Star Wars", license MIT, `mod_version=0.1.0`, `mod_group_id=com.tweeks.starwars`.
- **Work on `main` directly. Do NOT create, switch, or delete git branches. Do NOT rebase.** Commit after every task with `feat(starwars): ...` / `test(starwars): ...` style messages.
- This MC version uses `net.minecraft.resources.Identifier` (NOT `ResourceLocation`): `Identifier.fromNamespaceAndPath(ns, path)`, `Identifier.parse(s)`. Damage is applied with `LivingEntity.hurtServer(ServerLevel, DamageSource, float)`. Entity save I/O uses `ValueOutput`/`ValueInput` (`output.putByte`, `input.getByteOr`). Skeleton class lives at `net.minecraft.world.entity.monster.skeleton.Skeleton`. When any signature is in doubt, lift it verbatim from the named wildwest/craftee reference file — never from memory.
- Unit tests cover ONLY pure logic (constants, math, predicates, state POJOs). Entity classes trigger MC bootstrap on class-load and cannot be unit-tested. Run per-module tests with `./gradlew :starwars:test --tests <ClassName>`.
- Build check: `./gradlew :starwars:build`. Full check near the end: `./gradlew build`.
- Textures are FINISHED art at commit time — the repo forbids placeholder textures. Every texture task paints complete, shaded pixel art; every character commits an editable `.bbmodel` in `starwars/tools/`.
- `.bbmodel` generators must be deterministic: UUIDs via `uuid.uuid5` on the fixed namespace `00000000-0000-0000-0000-000000000001` (see `wildwest/tools/gen_bbmodels.py`).
- Bedrock pass at the end of each milestone: `./gradlew :translator:translate --args="starwars"` then commit `bedrock-out/starwars/` including its `UNTRANSLATABLE.md`. Never hand-edit generated files to hide gaps — gaps belong in UNTRANSLATABLE.md.
- Sounds map to vanilla event sounds in `sounds.json` (repo has zero `.ogg` assets). Custom `SoundEvent`s registered via `DeferredRegister<SoundEvent>` + `SoundEvent.createVariableRangeEvent`.
- Reference files (lift patterns/signatures from these): `wildwest/src/main/java/com/tweeks/wildwest/` — `WildWestMod.java`, `ModEntities.java`, `Hitscan.java`, `item/PistolItem.java`, `item/ModDataComponents.java`, `item/InfinityCooldowns.java`, `effect/ModAttachments.java`, `entity/BossSingletonSavedData.java`, `entity/BossSingletonState.java`, `entity/HerobrineSavedData.java`, `entity/ai/WildWestRangedAttackGoal.java`, `entity/ai/OutlawTargetGoal.java`, `network/*.java`, `client/ClientSetup.java`, `client/DeputyRenderer.java`, `client/model/DeputyModel.java`, `client/RadialMath.java`, `client/RadialPickerScreen.java`, `client/InfinityGauntletKeybind.java`, `data/*.java`; `craftee/src/main/java/com/tweeks/craftee/` — `Registration.java`, `SetBonusHandler.java`, `item/CrafteeArmorMaterials.java`; `wildwest/tools/gen_bbmodels.py`, `wildwest/tools/gen_textures.py`, `wildwest/tools/gen_spawn_eggs.py`.

---

## File Structure

All under `starwars/` unless noted. `J=src/main/java/com/tweeks/starwars`, `R=src/main/resources`, `T=src/test/java/com/tweeks/starwars`.

**Module scaffold:** `build.gradle`, `gradle.properties`, `src/main/templates/META-INF/neoforge.mods.toml`; root `settings.gradle` (modify).

**Core:** `J/StarWarsMod.java`, `J/Registration.java`, `J/ModEntities.java`, `J/ModSounds.java`, `J/StarWarsDamageTypes.java`, `J/Hitscan.java`.

**Faction:** `J/faction/SwFaction.java`, `SwCombatant.java`, `Alignment.java`, `AlignmentAttachment.java`, `AlignmentEvents.java`, `ModAttachments.java`, `Disguise.java`, `PacifyState.java`, `PacifyAttachment.java`.

**Entities:** `J/entity/SwMob.java`, `SwMobConstants.java`, `StormtrooperEntity.java`, `BattleDroidEntity.java`, `JediKnightEntity.java`, `DarthVaderEntity.java`, `LukeSkywalkerEntity.java`, `ObiWanEntity.java`, `AstromechEntity.java`, `BobaFettEntity.java`, `SingletonState.java`, `NamedCharacterSavedData.java`, `VaderSavedData.java`, `LukeSavedData.java`, `ObiWanSavedData.java`, `BobaFettSavedData.java`.

**AI:** `J/entity/ai/SwTargetGoal.java`, `TargetPredicates.java`, `BlasterAttackGoal.java`, `SaberMeleeGoal.java`, `VaderChokeGoal.java`, `LukeLeapGoal.java`, `ObiWanPushGoal.java`, `BobaJetpackGoal.java`.

**Items:** `J/item/ModDataComponents.java`, `BlasterPistolItem.java`, `BlasterRifleItem.java`, `LightsaberItem.java`, `SaberColor.java`, `HolocronItem.java`, `ForcePower.java`, `ForcePowers.java`, `ForceCooldowns.java`, `StormtrooperArmorMaterials.java`.

**Network:** `J/network/S2CBlasterTracerPacket.java`, `C2SSelectPowerPacket.java`, `NetworkHandlers.java`.

**Client:** `J/client/ClientSetup.java`, `TracerClientHandler.java`, `SwRadialMath.java`, `ForcePickerScreen.java`, `HolocronKeybind.java`, per-character `<Name>Renderer.java` + `model/<Name>Model.java` (Stormtrooper, BattleDroid, JediKnight, Vader, Luke, ObiWan, Astromech, BobaFett).

**Spawning:** `J/spawning/TrooperSpawnRules.java`, `NamedCharacterSpawner.java`.

**World:** `J/world/ModStructures.java`, `EscapePodStructure.java`, `EscapePodPiece.java`, `EscapePodLayout.java`, `ImperialOutpostStructure.java`, `ImperialOutpostPiece.java`, `ImperialOutpostLayout.java`, `JediRuinStructure.java`, `JediRuinPiece.java`, `JediRuinLayout.java`.

**Datagen:** `J/data/DataGenerators.java`, `ModDamageTypeProvider.java`, `ModDamageTypeTagsProvider.java`, `ModBiomeModifierProvider.java`, `ModLanguageProvider.java`, `ModEntityLootProvider.java`, `ModRecipeProvider.java`, `ModStructureProvider.java`.

**Tests:** `T/HitscanTest.java`, `T/faction/AlignmentTest.java`, `T/faction/TargetPredicatesTest.java`, `T/entity/SwMobConstantsTest.java`, `T/entity/SingletonStateTest.java`, `T/item/ForceCooldownsTest.java`, `T/client/SwRadialMathTest.java`, `T/world/EscapePodLayoutTest.java`, `T/world/ImperialOutpostLayoutTest.java`, `T/world/JediRuinLayoutTest.java`.

**Assets (`R/assets/starwars/`):** `lang/` (datagen), `textures/entity/*.png`, `textures/item/*.png`, `items/*.json`, `models/item/*.json`, `equipment/stormtrooper.json`, `sounds.json`. **Data (`R/data/starwars/`):** `loot_table/chests/*.json`.

**Tools:** `tools/gen_bbmodels.py`, `tools/gen_textures.py`, `tools/gen_item_textures.py`, `tools/gen_weapon_models.py`, `tools/gen_spawn_eggs.py`, `tools/*.bbmodel` (16 committed: 8 characters + lightsaber + blaster_pistol + blaster_rifle + holocron + 4 stormtrooper armor pieces).

**Bedrock:** `bedrock-out/starwars/**` (generated by translator, committed per milestone).

---

# Milestone 1 — Foundation

## Task 1: Module Scaffold

**Files:**
- Modify: `settings.gradle`
- Create: `starwars/build.gradle`, `starwars/gradle.properties`, `starwars/src/main/templates/META-INF/neoforge.mods.toml`, `starwars/src/main/java/com/tweeks/starwars/StarWarsMod.java`, `starwars/src/main/java/com/tweeks/starwars/Registration.java`, `starwars/src/main/java/com/tweeks/starwars/ModEntities.java`, `starwars/src/main/java/com/tweeks/starwars/ModSounds.java`

**Interfaces:**
- Produces: `StarWarsMod.MOD_ID` (`"starwars"`), `Registration.ITEMS`/`CREATIVE_TABS`/`register(IEventBus)`, `ModEntities.ENTITY_TYPES`/`register(IEventBus)`, `ModSounds.SOUNDS`/`register(IEventBus)`. Every later task registers through these.

- [ ] **Step 1: Add module to settings.gradle** — append after `include 'craftee'`:

```gradle
include 'starwars'
```

- [ ] **Step 2: Create `starwars/gradle.properties`**

```properties
## Mod Properties
mod_id=starwars
mod_name=Star Wars
mod_license=MIT
mod_version=0.1.0
mod_group_id=com.tweeks.starwars
```

- [ ] **Step 3: Create `starwars/build.gradle`** — copy `wildwest/build.gradle` verbatim, then delete the cross-mod pieces: remove the three `implementation project(':securitycore'|':securityguard'|':thief')` dependency lines and the `"securitycore"`/`"securityguard"`/`"thief"` blocks inside `neoForge.mods { }` (keep only the `"${mod_id}"` block). Keep everything else (Java 25 toolchain, generated-resources srcDirs, `generateModMetadata`, test wiring, junit-bom 5.10.2).

- [ ] **Step 4: Create `starwars/src/main/templates/META-INF/neoforge.mods.toml`**

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
Comprehensive Star Wars mod: Empire vs Light faction war, blasters,
lightsabers, Force powers, and worldgen structures.
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

- [ ] **Step 5: Create the four core Java classes**

`StarWarsMod.java`:

```java
package com.tweeks.starwars;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(StarWarsMod.MOD_ID)
public class StarWarsMod {
    public static final String MOD_ID = "starwars";
    public static final Logger LOGGER = LogUtils.getLogger();

    public StarWarsMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Star Wars mod loading");
        // Entity types register before items so SpawnEggItem can resolve ModEntities.*.get().
        ModEntities.register(modEventBus);
        Registration.register(modEventBus);
        ModSounds.register(modEventBus);
    }
}
```

`Registration.java`:

```java
package com.tweeks.starwars;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(StarWarsMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StarWarsMod.MOD_ID);

    // Items land here in later tasks. Creative tab is built in Task 6 once
    // there is an icon item to show.

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
```

`ModEntities.java`:

```java
package com.tweeks.starwars;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, StarWarsMod.MOD_ID);

    // Entity registrations land here in later tasks.

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
```

`ModSounds.java`:

```java
package com.tweeks.starwars;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(Registries.SOUND_EVENT, StarWarsMod.MOD_ID);

    public static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, name)));
    }

    // Sound events land here in later tasks via register("name").

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
```

- [ ] **Step 6: Build**

```bash
./gradlew :starwars:build
```
Expected: BUILD SUCCESSFUL. If `DeferredRegister.Items` or `registerItem` signatures fail, lift the exact form from `craftee/src/main/java/com/tweeks/craftee/Registration.java`.

- [ ] **Step 7: Commit**

```bash
git add settings.gradle starwars/
git commit -m "feat(starwars): module scaffold — gradle, mods.toml, core registration"
```

---

## Task 2: Damage Types + Datagen Wiring

**Files:**
- Create: `starwars/src/main/java/com/tweeks/starwars/StarWarsDamageTypes.java`, `.../data/DataGenerators.java`, `.../data/ModDamageTypeProvider.java`, `.../data/ModDamageTypeTagsProvider.java`, `.../data/ModLanguageProvider.java`

**Interfaces:**
- Produces: `StarWarsDamageTypes.blasterBolt(Entity attacker)`, `.lightsaber(Entity attacker)`, `.forceLightning(Entity attacker)`, `.forceLightningAoe(Level level)` → `DamageSource`; `DataGenerators` with `GatherDataEvent.Server`/`.Client` subscribers all later datagen tasks extend.

- [ ] **Step 1: Create `StarWarsDamageTypes.java`** — mirror `wildwest/src/main/java/com/tweeks/wildwest/WildWestDamageTypes.java` exactly (ResourceKey constants + `registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(...)` helpers), with three keys:

```java
package com.tweeks.starwars;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class StarWarsDamageTypes {
    private StarWarsDamageTypes() {}

    public static final ResourceKey<DamageType> BLASTER_BOLT = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "blaster_bolt"));

    public static final ResourceKey<DamageType> LIGHTSABER = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "lightsaber"));

    public static final ResourceKey<DamageType> FORCE_LIGHTNING = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "force_lightning"));

    public static DamageSource blasterBolt(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(BLASTER_BOLT),
            attacker);
    }

    public static DamageSource lightsaber(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(LIGHTSABER),
            attacker);
    }

    public static DamageSource forceLightning(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(FORCE_LIGHTNING),
            attacker);
    }

    public static DamageSource forceLightningAoe(Level level) {
        return new DamageSource(
            level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(FORCE_LIGHTNING));
    }
}
```

- [ ] **Step 2: Create the datagen classes** — copy the structure of the wildwest equivalents, s/wildwest/starwars/:
  - `ModDamageTypeProvider`: `bootstrap(BootstrapContext<DamageType>)` registering all three keys. The constructor is the 2-arg `new DamageType(msgId, exhaustion)` and **the msgId includes the namespace** — wildwest's are `"wildwest.gunshot"` etc. (vanilla builds the death key as `"death.attack." + msgId`, so an un-namespaced msgId breaks every death message). Register: `new DamageType("starwars.blaster_bolt", 0.1f)`, `new DamageType("starwars.lightsaber", 0.1f)`, `new DamageType("starwars.force_lightning", 0.1f)`.
  - `ModDamageTypeTagsProvider`: mirror wildwest's (tags like `DamageTypeTags.BYPASSES_ARMOR` — copy whichever tags wildwest applies to `GUNSHOT` onto `BLASTER_BOLT`, none for the melee types unless wildwest tags CLUB/KNIFE, in which case mirror those onto LIGHTSABER).
  - `ModLanguageProvider extends LanguageProvider` (`super(output, StarWarsMod.MOD_ID, "en_us")`) with initial entries:

```java
add("itemGroup." + StarWarsMod.MOD_ID, "Star Wars");
add("death.attack.starwars.blaster_bolt", "%1$s was vaporized");
add("death.attack.starwars.blaster_bolt.player", "%1$s was vaporized by %2$s");
add("death.attack.starwars.blaster_bolt.item", "%1$s was vaporized by %2$s using %3$s");
add("death.attack.starwars.lightsaber", "%1$s was cut down by %2$s");
add("death.attack.starwars.force_lightning", "%1$s was electrocuted by %2$s");
```

  - `DataGenerators`: copy `wildwest/src/main/java/com/tweeks/wildwest/data/DataGenerators.java` with only the DAMAGE_TYPE builder entry + language provider for now (biome modifiers, loot, recipes, structures join in later tasks):

```java
package com.tweeks.starwars.data;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = StarWarsMod.MOD_ID)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherDataServer(GatherDataEvent.Server event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        RegistrySetBuilder builder = new RegistrySetBuilder()
            .add(Registries.DAMAGE_TYPE, ModDamageTypeProvider::bootstrap);
        gen.addProvider(true, new DatapackBuiltinEntriesProvider(
            output, lookup, builder, Set.of(StarWarsMod.MOD_ID)));

        gen.addProvider(true, new ModDamageTypeTagsProvider(output, lookup));
    }

    @SubscribeEvent
    public static void gatherDataClient(GatherDataEvent.Client event) {
        DataGenerator gen = event.getGenerator();
        gen.addProvider(true, new ModLanguageProvider(gen.getPackOutput()));
    }
}
```

- [ ] **Step 3: Run datagen + build**

```bash
./gradlew :starwars:runClientData :starwars:runServerData :starwars:build
```
Expected: BUILD SUCCESSFUL; generated JSONs appear under `starwars/src/generated/serverData/data/starwars/damage_type/` and `.../clientData/assets/starwars/lang/en_us.json`.

- [ ] **Step 4: Commit**

```bash
git add starwars/
git commit -m "feat(starwars): damage types + datagen wiring"
```

---

# Milestone 2 — Faction Core, Blasters, Troopers

## Task 3: Faction Enum + Alignment Pure Logic + Tests

**Files:**
- Create: `.../faction/SwFaction.java`, `.../faction/SwCombatant.java`, `.../faction/Alignment.java`
- Test: `starwars/src/test/java/com/tweeks/starwars/faction/AlignmentTest.java`

**Interfaces:**
- Produces: `SwFaction { EMPIRE, LIGHT, NEUTRAL }`; `SwCombatant.getFaction()`; `Alignment` pure class — `HOSTILE_THRESHOLD = 50`, `KILL_DELTA = 5`, `HIT_DELTA = 1`, `POWER_DELTA = 2`, `MIN = -100`, `MAX = 100`, `clamp(int)`, `deltaForKill(SwFaction victim)`, `deltaForHit(SwFaction victim)`, `deltaForPower(boolean lightSide)`, `isHostileTo(int score, SwFaction faction)`. **Sign rule: positive score = light-side player; harming EMPIRE raises the score (+), harming LIGHT lowers it (−); EMPIRE turns hostile when score ≥ +50, LIGHT when score ≤ −50** (each faction attacks its enemy's champions).

- [ ] **Step 1: Write the failing test**

```java
package com.tweeks.starwars.faction;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AlignmentTest {

    @Test
    void constants_matchSpec() {
        assertEquals(50, Alignment.HOSTILE_THRESHOLD);
        assertEquals(5, Alignment.KILL_DELTA);
        assertEquals(1, Alignment.HIT_DELTA);
        assertEquals(2, Alignment.POWER_DELTA);
        assertEquals(-100, Alignment.MIN);
        assertEquals(100, Alignment.MAX);
    }

    @Test
    void harmingEmpire_movesTowardLight() {
        assertEquals(5, Alignment.deltaForKill(SwFaction.EMPIRE));
        assertEquals(1, Alignment.deltaForHit(SwFaction.EMPIRE));
    }

    @Test
    void harmingLight_movesTowardDark() {
        assertEquals(-5, Alignment.deltaForKill(SwFaction.LIGHT));
        assertEquals(-1, Alignment.deltaForHit(SwFaction.LIGHT));
    }

    @Test
    void harmingNeutral_noChange() {
        assertEquals(0, Alignment.deltaForKill(SwFaction.NEUTRAL));
        assertEquals(0, Alignment.deltaForHit(SwFaction.NEUTRAL));
    }

    @Test
    void powerUse_movesAlignment() {
        assertEquals(2, Alignment.deltaForPower(true));
        assertEquals(-2, Alignment.deltaForPower(false));
    }

    @Test
    void clamp_boundsScore() {
        assertEquals(100, Alignment.clamp(150));
        assertEquals(-100, Alignment.clamp(-150));
        assertEquals(7, Alignment.clamp(7));
    }

    @Test
    void hostility_thresholds() {
        // Player at +50 (light champion): Empire attacks, Light does not.
        assertTrue(Alignment.isHostileTo(50, SwFaction.EMPIRE));
        assertFalse(Alignment.isHostileTo(50, SwFaction.LIGHT));
        // Player at -50 (dark sider): Light attacks, Empire does not.
        assertTrue(Alignment.isHostileTo(-50, SwFaction.LIGHT));
        assertFalse(Alignment.isHostileTo(-50, SwFaction.EMPIRE));
        // Neutral band: nobody auto-targets.
        assertFalse(Alignment.isHostileTo(49, SwFaction.EMPIRE));
        assertFalse(Alignment.isHostileTo(-49, SwFaction.LIGHT));
        assertFalse(Alignment.isHostileTo(0, SwFaction.EMPIRE));
        // NEUTRAL faction never auto-targets.
        assertFalse(Alignment.isHostileTo(100, SwFaction.NEUTRAL));
        assertFalse(Alignment.isHostileTo(-100, SwFaction.NEUTRAL));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :starwars:test --tests AlignmentTest
```
Expected: FAIL — classes do not exist.

- [ ] **Step 3: Implement**

`SwFaction.java`:

```java
package com.tweeks.starwars.faction;

public enum SwFaction {
    EMPIRE,
    LIGHT,
    NEUTRAL;

    /** The faction this one attacks on sight. NEUTRAL fights nobody. */
    public SwFaction enemy() {
        return switch (this) {
            case EMPIRE -> LIGHT;
            case LIGHT -> EMPIRE;
            case NEUTRAL -> NEUTRAL;
        };
    }
}
```

`SwCombatant.java`:

```java
package com.tweeks.starwars.faction;

/**
 * Marker interface for faction-war participants. The decoupling seam
 * (SecurityHostile pattern): target goals test for this interface, never
 * for concrete entity classes.
 */
public interface SwCombatant {
    SwFaction getFaction();
}
```

`Alignment.java`:

```java
package com.tweeks.starwars.faction;

/**
 * Pure alignment math. Positive score = light-side player, negative = dark.
 * Harming EMPIRE raises the score; harming LIGHT lowers it. A faction turns
 * hostile once the player is a champion of its enemy (|score| >= threshold
 * on that side). Lives outside MC classes for unit testing.
 */
public final class Alignment {
    private Alignment() {}

    public static final int HOSTILE_THRESHOLD = 50;
    public static final int KILL_DELTA = 5;
    public static final int HIT_DELTA = 1;
    public static final int POWER_DELTA = 2;
    public static final int MIN = -100;
    public static final int MAX = 100;

    public static int clamp(int score) {
        return Math.max(MIN, Math.min(MAX, score));
    }

    public static int deltaForKill(SwFaction victim) {
        return switch (victim) {
            case EMPIRE -> KILL_DELTA;
            case LIGHT -> -KILL_DELTA;
            case NEUTRAL -> 0;
        };
    }

    public static int deltaForHit(SwFaction victim) {
        return switch (victim) {
            case EMPIRE -> HIT_DELTA;
            case LIGHT -> -HIT_DELTA;
            case NEUTRAL -> 0;
        };
    }

    public static int deltaForPower(boolean lightSide) {
        return lightSide ? POWER_DELTA : -POWER_DELTA;
    }

    /** True if {@code faction} auto-targets a player with {@code score}. */
    public static boolean isHostileTo(int score, SwFaction faction) {
        return switch (faction) {
            case EMPIRE -> score >= HOSTILE_THRESHOLD;
            case LIGHT -> score <= -HOSTILE_THRESHOLD;
            case NEUTRAL -> false;
        };
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :starwars:test --tests AlignmentTest
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add starwars/src/main/java/com/tweeks/starwars/faction/ starwars/src/test/
git commit -m "feat(starwars): faction enum + alignment pure logic with tests"
```

---

## Task 4: Alignment Attachment + Damage/Kill Event Hooks

**Files:**
- Create: `.../faction/AlignmentAttachment.java`, `.../faction/ModAttachments.java`, `.../faction/AlignmentEvents.java`
- Modify: `.../StarWarsMod.java`

**Interfaces:**
- Consumes: `Alignment`, `SwFaction`, `SwCombatant` (Task 3).
- Produces: `AlignmentAttachment(int score)` record with `CODEC`; `ModAttachments.ALIGNMENT` + `register(IEventBus)`; static helpers `AlignmentEvents.getScore(Player)` and `AlignmentEvents.adjustScore(Player, int delta)` used by Force powers (Task 24) and target goals (Task 7).

- [ ] **Step 1: Create `AlignmentAttachment.java`**

```java
package com.tweeks.starwars.faction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record AlignmentAttachment(int score) {
    public static final Codec<AlignmentAttachment> CODEC =
        RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("Score").forGetter(AlignmentAttachment::score)
        ).apply(instance, AlignmentAttachment::new));
}
```

- [ ] **Step 2: Create `ModAttachments.java`** — the wildwest null-default + serialize-predicate pattern, verbatim shape from `wildwest/src/main/java/com/tweeks/wildwest/effect/ModAttachments.java`:

```java
package com.tweeks.starwars.faction;

import com.tweeks.starwars.StarWarsMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, StarWarsMod.MOD_ID);

    // Null default + shouldSerialize-rejects-null: without the predicate,
    // getData() auto-populates a null record and the next world save NPEs
    // inside RecordCodecBuilder. Read paths use hasData() first.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AlignmentAttachment>> ALIGNMENT =
        ATTACHMENTS.register("alignment",
            () -> AttachmentType.<AlignmentAttachment>builder(() -> null)
                .serialize(AlignmentAttachment.CODEC, attachment -> attachment != null)
                .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
```

- [ ] **Step 3: Create `AlignmentEvents.java`**

```java
package com.tweeks.starwars.faction;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = StarWarsMod.MOD_ID)
public final class AlignmentEvents {
    private AlignmentEvents() {}

    public static int getScore(Player player) {
        if (!player.hasData(ModAttachments.ALIGNMENT.get())) return 0;
        AlignmentAttachment a = player.getData(ModAttachments.ALIGNMENT.get());
        return a == null ? 0 : a.score();
    }

    public static void adjustScore(Player player, int delta) {
        if (delta == 0) return;
        int next = Alignment.clamp(getScore(player) + delta);
        player.setData(ModAttachments.ALIGNMENT.get(), new AlignmentAttachment(next));
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof SwCombatant combatant)) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        // A lethal hit fires both this event and LivingDeathEvent; skip the
        // hit delta here so a kill scores exactly KILL_DELTA, not KILL+HIT.
        if (event.getEntity().isDeadOrDying()) return;
        adjustScore(player, Alignment.deltaForHit(combatant.getFaction()));
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof SwCombatant combatant)) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        adjustScore(player, Alignment.deltaForKill(combatant.getFaction()));
    }
}
```

If `LivingDamageEvent.Post` does not exist under that name, check the wildwest/securitycore event handlers for the current damage-event class (`grep -rn "LivingDamageEvent\|LivingIncomingDamageEvent" wildwest/ securitycore/ thief/`) and use the post-damage variant found there.

- [ ] **Step 4: Wire into `StarWarsMod` constructor** — add after `ModSounds.register(modEventBus);`:

```java
com.tweeks.starwars.faction.ModAttachments.register(modEventBus);
```

(`AlignmentEvents` self-registers via `@EventBusSubscriber`.)

- [ ] **Step 5: Build + commit**

```bash
./gradlew :starwars:build
git add starwars/
git commit -m "feat(starwars): player alignment attachment + damage/kill hooks"
```

---

## Task 5: Hitscan + Tracer Network Plumbing

**Files:**
- Create: `.../Hitscan.java`, `.../network/S2CBlasterTracerPacket.java`, `.../network/NetworkHandlers.java`, `.../client/TracerClientHandler.java`
- Test: `starwars/src/test/java/com/tweeks/starwars/HitscanTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `Hitscan.Candidate(String id, double distanceAlongRay)`, `Hitscan.firstHitWithinRange(double blockDistance, List<Candidate>) → Optional<Candidate>`; `S2CBlasterTracerPacket(Vec3 start, Vec3 end, int argbColor)`; color constants `S2CBlasterTracerPacket.COLOR_EMPIRE` (0xFFFF3020, red) and `COLOR_LIGHT` (0xFF3060FF, blue). Task 6/7 fire these.

- [ ] **Step 1: Write the failing test**

```java
package com.tweeks.starwars;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HitscanTest {

    @Test
    void picksNearestCandidate() {
        var hit = Hitscan.firstHitWithinRange(16.0, List.of(
            new Hitscan.Candidate("far", 10.0),
            new Hitscan.Candidate("near", 4.0)));
        assertTrue(hit.isPresent());
        assertEquals("near", hit.get().id());
    }

    @Test
    void wallBlocksCandidatesBehindIt() {
        var hit = Hitscan.firstHitWithinRange(5.0, List.of(
            new Hitscan.Candidate("behind_wall", 8.0)));
        assertTrue(hit.isEmpty());
    }

    @Test
    void emptyCandidates_missesCleanly() {
        assertTrue(Hitscan.firstHitWithinRange(16.0, List.of()).isEmpty());
    }

    @Test
    void candidateExactlyAtWallDistance_blocked() {
        var hit = Hitscan.firstHitWithinRange(6.0, List.of(
            new Hitscan.Candidate("at_wall", 6.0)));
        assertTrue(hit.isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :starwars:test --tests HitscanTest
```
Expected: FAIL — `Hitscan` does not exist.

- [ ] **Step 3: Create `Hitscan.java`** — identical logic to `wildwest/src/main/java/com/tweeks/wildwest/Hitscan.java`:

```java
package com.tweeks.starwars;

import java.util.List;
import java.util.Optional;

public final class Hitscan {
    private Hitscan() {}

    public record Candidate(String id, double distanceAlongRay) {}

    public static Optional<Candidate> firstHitWithinRange(
            double blockDistance, List<Candidate> candidates) {
        Candidate best = null;
        for (Candidate c : candidates) {
            if (c.distanceAlongRay() >= blockDistance) continue;
            if (best == null || c.distanceAlongRay() < best.distanceAlongRay()) {
                best = c;
            }
        }
        return Optional.ofNullable(best);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :starwars:test --tests HitscanTest
```
Expected: PASS.

- [ ] **Step 5: Create the tracer packet + registrar + client handler**

`S2CBlasterTracerPacket.java` (wildwest `S2CTracerPacket` shape + a color int):

```java
package com.tweeks.starwars.network;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public record S2CBlasterTracerPacket(Vec3 start, Vec3 end, int argbColor)
        implements CustomPacketPayload {

    /** Empire bolts render red, Light/player bolts blue. */
    public static final int COLOR_EMPIRE = 0xFFFF3020;
    public static final int COLOR_LIGHT = 0xFF3060FF;

    public static final Type<S2CBlasterTracerPacket> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "blaster_tracer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CBlasterTracerPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.DOUBLE, p -> p.start.x,
            ByteBufCodecs.DOUBLE, p -> p.start.y,
            ByteBufCodecs.DOUBLE, p -> p.start.z,
            ByteBufCodecs.DOUBLE, p -> p.end.x,
            ByteBufCodecs.DOUBLE, p -> p.end.y,
            ByteBufCodecs.DOUBLE, p -> p.end.z,
            ByteBufCodecs.INT, S2CBlasterTracerPacket::argbColor,
            (sx, sy, sz, ex, ey, ez, color) -> new S2CBlasterTracerPacket(
                new Vec3(sx, sy, sz), new Vec3(ex, ey, ez), color));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

`NetworkHandlers.java`:

```java
package com.tweeks.starwars.network;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.TracerClientHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = StarWarsMod.MOD_ID)
public final class NetworkHandlers {
    private NetworkHandlers() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1");
        reg.playToClient(
            S2CBlasterTracerPacket.TYPE,
            S2CBlasterTracerPacket.STREAM_CODEC,
            TracerClientHandler::handle);
        // C2SSelectPowerPacket joins in Milestone 5.
    }
}
```

`TracerClientHandler.java` — dust particles along the bolt line, colored from the packet:

```java
package com.tweeks.starwars.client;

import com.tweeks.starwars.network.S2CBlasterTracerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class TracerClientHandler {
    private TracerClientHandler() {}

    private static final double STEP = 0.5;

    public static void handle(S2CBlasterTracerPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            int rgb = pkt.argbColor() & 0xFFFFFF;
            DustParticleOptions dust = new DustParticleOptions(rgb, 0.6f);
            Vec3 delta = pkt.end().subtract(pkt.start());
            double len = delta.length();
            if (len < 1.0e-3) return;
            Vec3 dir = delta.scale(1.0 / len);
            for (double d = 0; d <= len; d += STEP) {
                Vec3 p = pkt.start().add(dir.scale(d));
                level.addParticle(dust, p.x, p.y, p.z, 0, 0, 0);
            }
        });
    }
}
```

If the `DustParticleOptions` constructor differs (some versions take a `Vector3f` color), lift the current constructor from any vanilla usage (`grep -rn "new DustParticleOptions" wildwest/` first; otherwise check the class in the NeoForge sources jar).

- [ ] **Step 6: Build + commit**

```bash
./gradlew :starwars:build :starwars:test
git add starwars/
git commit -m "feat(starwars): hitscan core + colored blaster tracer network path"
```

## Task 6: Blaster Items + Creative Tab + Item Assets + Sounds

**Files:**
- Create: `.../item/BlasterPistolItem.java`, `.../item/BlasterRifleItem.java`, `starwars/tools/gen_item_textures.py`, `starwars/src/main/resources/assets/starwars/items/blaster_pistol.json`, `.../items/blaster_rifle.json`, `.../models/item/blaster_pistol.json`, `.../models/item/blaster_rifle.json`, `.../textures/item/blaster_pistol.png`, `.../textures/item/blaster_rifle.png`, `.../sounds.json`
- Modify: `.../Registration.java`, `.../ModSounds.java`, `.../data/ModLanguageProvider.java`

**Interfaces:**
- Consumes: `Hitscan`, `S2CBlasterTracerPacket` (Task 5), `StarWarsDamageTypes.blasterBolt` (Task 2).
- Produces: `BlasterPistolItem` — `MAX_RANGE = 20.0`, `DAMAGE = 5.0F`, `COOLDOWN_TICKS = 10`, overridable `getDamage()`, `use(...)` player path, static `fireFromMob(LivingEntity shooter, LivingEntity target, float damage, int tracerColor)` (+ 2-arg overload defaulting `DAMAGE`/`COLOR_EMPIRE`); `BlasterRifleItem extends BlasterPistolItem` — `RIFLE_DAMAGE = 8.0F`, `RIFLE_COOLDOWN_TICKS = 20`, static `fireFromMob(...)` with rifle damage; `Registration.BLASTER_PISTOL`, `Registration.BLASTER_RIFLE`, `Registration.STARWARS_TAB`; `ModSounds.BLASTER_FIRE`.

- [ ] **Step 1: Create `BlasterPistolItem.java`** — port `wildwest/src/main/java/com/tweeks/wildwest/item/PistolItem.java` mechanically with these changes: package/class names, `MAX_RANGE = 20.0`, `COOLDOWN_TICKS = 10`, durability `450`, damage source `StarWarsDamageTypes.blasterBolt(...)`, sound `ModSounds.BLASTER_FIRE.get()`, tracer `new S2CBlasterTracerPacket(start, endPoint, tracerColor)`. Player path uses `S2CBlasterTracerPacket.COLOR_LIGHT`. Mob path signature gains the color param:

```java
public static void fireFromMob(LivingEntity shooter, LivingEntity target) {
    fireFromMob(shooter, target, DAMAGE, S2CBlasterTracerPacket.COLOR_EMPIRE);
}

public static void fireFromMob(LivingEntity shooter, LivingEntity target,
                               float damage, int tracerColor) {
    // body identical to PistolItem.fireFromMob with MAX_RANGE/damage-source/
    // sound swaps above, and the final send:
    // PacketDistributor.sendToPlayersTrackingEntity(
    //     shooter, new S2CBlasterTracerPacket(start, endPoint, tracerColor));
}
```

Copy the full method bodies from `PistolItem` — every line (Gaussian jitter `nextGaussian() * 0.05`, `invulnerableTime = 0`, AABB `.inflate(1.0)`, bounding-box `.inflate(0.3)`, `SoundSource.HOSTILE` for mobs / `SoundSource.PLAYERS` for players). Do not re-derive.

- [ ] **Step 2: Create `BlasterRifleItem.java`**

```java
package com.tweeks.starwars.item;

import com.tweeks.starwars.network.S2CBlasterTracerPacket;
import net.minecraft.world.entity.LivingEntity;

public class BlasterRifleItem extends BlasterPistolItem {

    public static final float RIFLE_DAMAGE = 8.0F;
    public static final int RIFLE_COOLDOWN_TICKS = 20;

    public BlasterRifleItem(Properties properties) {
        super(properties);
    }

    @Override
    public float getDamage() { return RIFLE_DAMAGE; }

    @Override
    public int getCooldownTicks() { return RIFLE_COOLDOWN_TICKS; }

    public static void fireFromMobRifle(LivingEntity shooter, LivingEntity target, int tracerColor) {
        fireFromMob(shooter, target, RIFLE_DAMAGE, tracerColor);
    }
}
```

For this to work, `BlasterPistolItem` must read cooldown via an overridable `public int getCooldownTicks() { return COOLDOWN_TICKS; }` in its `use(...)` (the wildwest original hardcodes the constant — make this one small improvement).

- [ ] **Step 3: Register items + creative tab in `Registration.java`** — add:

```java
public static final DeferredItem<com.tweeks.starwars.item.BlasterPistolItem> BLASTER_PISTOL =
    ITEMS.registerItem("blaster_pistol", com.tweeks.starwars.item.BlasterPistolItem::new, p -> p);

public static final DeferredItem<com.tweeks.starwars.item.BlasterRifleItem> BLASTER_RIFLE =
    ITEMS.registerItem("blaster_rifle", com.tweeks.starwars.item.BlasterRifleItem::new, p -> p);

public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STARWARS_TAB =
    CREATIVE_TABS.register("main", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + StarWarsMod.MOD_ID))
            .icon(() -> BLASTER_PISTOL.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(BLASTER_PISTOL.get());
                output.accept(BLASTER_RIFLE.get());
                // Later tasks append their items here.
            })
            .build());
```

- [ ] **Step 4: Sounds** — in `ModSounds`, add `public static final DeferredHolder<SoundEvent, SoundEvent> BLASTER_FIRE = register("blaster_fire");` and create `assets/starwars/sounds.json`:

```json
{
  "blaster_fire": {
    "category": "player",
    "subtitle": "subtitle.starwars.blaster_fire",
    "sounds": [
      { "name": "minecraft:entity.blaze.shoot", "type": "event", "pitch": 1.6 }
    ]
  }
}
```

Add to `ModLanguageProvider.addTranslations()`:

```java
add(com.tweeks.starwars.Registration.BLASTER_PISTOL.get(), "Blaster Pistol");
add(com.tweeks.starwars.Registration.BLASTER_RIFLE.get(), "Blaster Rifle");
add("subtitle.starwars.blaster_fire", "Blaster fires");
```

- [ ] **Step 5: Item textures + models.** Create `starwars/tools/gen_item_textures.py` reusing the chunk/PNG writer from `wildwest/tools/gen_textures.py` (copy `rect`, `write_png` with parametrized W/H=16). Paint finished 16×16 art (shaded, outlined — not flat silhouettes):
  - `blaster_pistol.png`: dark gunmetal body `(0x2A,0x2A,0x30)` with light edge highlight `(0x55,0x55,0x60)` along the top row of the body, black grip `(0x18,0x18,0x18)` angled by stacking 2px rects downward-right, muzzle tip `(0x66,0x30,0x30)`, scope notch `(0x80,0x80,0x88)`.
  - `blaster_rifle.png`: longer body across 14px, stock at left `(0x3A,0x30,0x28)` wood-brown with darker `(0x2A,0x22,0x1A)` underside shading, barrel + folding stock line, same gunmetal palette.

Write each element with explicit `rect(...)` calls in the script; run `python3 starwars/tools/gen_item_textures.py starwars/src/main/resources/assets/starwars/textures/item/`.

Item model JSONs follow the wildwest 2-file convention. `items/blaster_pistol.json`:

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "starwars:item/blaster_pistol"
  }
}
```

`models/item/blaster_pistol.json`:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": { "layer0": "starwars:item/blaster_pistol" }
}
```

Same pair for `blaster_rifle`. (Task 12 upgrades these flat models to 3D voxel models; the `items/*.json` selector files stay unchanged.)

- [ ] **Step 6: Build, datagen, commit**

```bash
./gradlew :starwars:build :starwars:runClientData
git add starwars/
git commit -m "feat(starwars): blaster pistol + rifle with hitscan, tracers, assets"
```

---

## Task 7: SwMob Base + Faction Target Goals + Tests

**Files:**
- Create: `.../entity/SwMob.java`, `.../entity/SwMobConstants.java`, `.../entity/ai/TargetPredicates.java`, `.../entity/ai/SwTargetGoal.java`, `.../entity/ai/BlasterAttackGoal.java`, `.../faction/Disguise.java`
- Test: `starwars/src/test/java/com/tweeks/starwars/entity/SwMobConstantsTest.java`, `starwars/src/test/java/com/tweeks/starwars/entity/ai/TargetPredicatesTest.java`

**Interfaces:**
- Consumes: `SwFaction`, `SwCombatant`, `Alignment`, `AlignmentEvents.getScore` (Tasks 3-4); `BlasterPistolItem.fireFromMob`, `BlasterRifleItem.fireFromMobRifle` (Task 6).
- Produces: `abstract SwMob extends PathfinderMob implements SwCombatant` with `registerGoals()` installing `FloatGoal`, `BlasterAttackGoal` (only when `usesBlaster()`), `WaterAvoidingRandomStrollGoal(0.6)`, `LookAtPlayerGoal`, `RandomLookAroundGoal`, `HurtByTargetGoal`, `SwTargetGoal`; abstract `usesBlaster()`, `usesRifleBlaster()`, `getWeaponStack()`; `TargetPredicates.shouldTarget(SwFaction myFaction, boolean targetIsCombatant, SwFaction targetFaction, boolean targetIsPlayer, int playerScore, boolean playerDisguisedAsEmpire)` pure boolean; `Disguise.isWearingFullStormtrooperSet(LivingEntity)` — **stub returning false until Task 21 fills it** (documented, compiles, later task replaces body); `SwMobConstants.TROOPER_*` stats. `BlasterAttackGoal(SwMob mob)` fires every `SwMobConstants.FIRE_INTERVAL_TICKS`.

- [ ] **Step 1: Write the failing tests**

`SwMobConstantsTest.java`:

```java
package com.tweeks.starwars.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SwMobConstantsTest {
    @Test
    void trooperStats_matchSpec() {
        assertEquals(20.0, SwMobConstants.TROOPER_MAX_HEALTH);
        assertEquals(0.30, SwMobConstants.TROOPER_MOVEMENT_SPEED);
        assertEquals(24.0, SwMobConstants.TROOPER_FOLLOW_RANGE);
        assertEquals(30, SwMobConstants.FIRE_INTERVAL_TICKS);
        assertEquals(12.0, SwMobConstants.DROID_MAX_HEALTH);
        assertEquals(0.28, SwMobConstants.DROID_MOVEMENT_SPEED);
    }
}
```

`TargetPredicatesTest.java` (package `com.tweeks.starwars.entity.ai`):

```java
package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.faction.SwFaction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TargetPredicatesTest {

    @Test
    void empireTargetsLightCombatant() {
        assertTrue(TargetPredicates.shouldTarget(
            SwFaction.EMPIRE, true, SwFaction.LIGHT, false, 0, false));
    }

    @Test
    void empireIgnoresOwnFaction_andNeutrals() {
        assertFalse(TargetPredicates.shouldTarget(
            SwFaction.EMPIRE, true, SwFaction.EMPIRE, false, 0, false));
        assertFalse(TargetPredicates.shouldTarget(
            SwFaction.EMPIRE, true, SwFaction.NEUTRAL, false, 0, false));
    }

    @Test
    void lightTargetsEmpireCombatant() {
        assertTrue(TargetPredicates.shouldTarget(
            SwFaction.LIGHT, true, SwFaction.EMPIRE, false, 0, false));
    }

    @Test
    void neutralFactionNeverAutoTargets() {
        assertFalse(TargetPredicates.shouldTarget(
            SwFaction.NEUTRAL, true, SwFaction.EMPIRE, false, 0, false));
        assertFalse(TargetPredicates.shouldTarget(
            SwFaction.NEUTRAL, false, SwFaction.NEUTRAL, true, -100, false));
    }

    @Test
    void playerTargetedOnlyPastThreshold() {
        // Light champion (+50): Empire hostile, Light not.
        assertTrue(TargetPredicates.shouldTarget(
            SwFaction.EMPIRE, false, SwFaction.NEUTRAL, true, 50, false));
        assertFalse(TargetPredicates.shouldTarget(
            SwFaction.LIGHT, false, SwFaction.NEUTRAL, true, 50, false));
        // Dark sider (-50): Light hostile.
        assertTrue(TargetPredicates.shouldTarget(
            SwFaction.LIGHT, false, SwFaction.NEUTRAL, true, -50, false));
        // Neutral-band player untargeted.
        assertFalse(TargetPredicates.shouldTarget(
            SwFaction.EMPIRE, false, SwFaction.NEUTRAL, true, 0, false));
    }

    @Test
    void stormtrooperDisguiseHidesPlayerFromEmpireOnly() {
        assertFalse(TargetPredicates.shouldTarget(
            SwFaction.EMPIRE, false, SwFaction.NEUTRAL, true, 100, true));
        assertTrue(TargetPredicates.shouldTarget(
            SwFaction.LIGHT, false, SwFaction.NEUTRAL, true, -50, true));
    }

    @Test
    void nonCombatantNonPlayer_neverTargeted() {
        assertFalse(TargetPredicates.shouldTarget(
            SwFaction.EMPIRE, false, SwFaction.NEUTRAL, false, 0, false));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :starwars:test --tests SwMobConstantsTest --tests TargetPredicatesTest
```
Expected: FAIL — classes do not exist.

- [ ] **Step 3: Implement pure classes**

`SwMobConstants.java`:

```java
package com.tweeks.starwars.entity;

public final class SwMobConstants {
    private SwMobConstants() {}

    public static final double TROOPER_MAX_HEALTH = 20.0;
    public static final double TROOPER_MOVEMENT_SPEED = 0.30;
    public static final double TROOPER_FOLLOW_RANGE = 24.0;
    /** Blaster AI goal fire interval (1.5s). */
    public static final int FIRE_INTERVAL_TICKS = 30;

    public static final double DROID_MAX_HEALTH = 12.0;
    public static final double DROID_MOVEMENT_SPEED = 0.28;
}
```

`TargetPredicates.java`:

```java
package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.faction.Alignment;
import com.tweeks.starwars.faction.SwFaction;

/**
 * Pure faction-war targeting decision. All world lookups (faction of the
 * candidate, player alignment score, disguise state) happen in the caller
 * (SwTargetGoal); this class only decides.
 */
public final class TargetPredicates {
    private TargetPredicates() {}

    public static boolean shouldTarget(SwFaction myFaction,
                                       boolean targetIsCombatant,
                                       SwFaction targetFaction,
                                       boolean targetIsPlayer,
                                       int playerScore,
                                       boolean playerDisguisedAsEmpire) {
        if (myFaction == SwFaction.NEUTRAL) return false;
        if (targetIsCombatant) {
            return targetFaction == myFaction.enemy();
        }
        if (targetIsPlayer) {
            if (myFaction == SwFaction.EMPIRE && playerDisguisedAsEmpire) return false;
            return Alignment.isHostileTo(playerScore, myFaction);
        }
        return false;
    }
}
```

`Disguise.java`:

```java
package com.tweeks.starwars.faction;

import net.minecraft.world.entity.LivingEntity;

/**
 * Stormtrooper-armor disguise check. Task 19 (armor set) fills the real
 * slot-by-slot check; until then no armor items exist, so nobody can be
 * disguised and this correctly returns false.
 */
public final class Disguise {
    private Disguise() {}

    public static boolean isWearingFullStormtrooperSet(LivingEntity entity) {
        return false;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :starwars:test --tests SwMobConstantsTest --tests TargetPredicatesTest
```
Expected: PASS.

- [ ] **Step 5: Implement the MC-coupled classes**

`SwTargetGoal.java`:

```java
package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.SwMob;
import com.tweeks.starwars.faction.AlignmentEvents;
import com.tweeks.starwars.faction.Disguise;
import com.tweeks.starwars.faction.SwCombatant;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

public class SwTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {

    public SwTargetGoal(SwMob mob) {
        super(mob, LivingEntity.class, 5, false, false,
            (target, level) -> {
                if (!target.isAlive()) return false;
                SwFaction myFaction = mob.getFaction();
                boolean isCombatant = target instanceof SwCombatant;
                SwFaction targetFaction = isCombatant
                    ? ((SwCombatant) target).getFaction() : SwFaction.NEUTRAL;
                boolean isPlayer = target instanceof Player;
                int score = isPlayer ? AlignmentEvents.getScore((Player) target) : 0;
                boolean disguised = isPlayer
                    && Disguise.isWearingFullStormtrooperSet(target);
                return TargetPredicates.shouldTarget(
                    myFaction, isCombatant, targetFaction, isPlayer, score, disguised);
            });
    }
}
```

`BlasterAttackGoal.java` — the `WildWestRangedAttackGoal` shape:

```java
package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.SwMob;
import com.tweeks.starwars.entity.SwMobConstants;
import com.tweeks.starwars.item.BlasterPistolItem;
import com.tweeks.starwars.item.BlasterRifleItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class BlasterAttackGoal extends Goal {

    private final SwMob mob;
    private int cooldown;

    public BlasterAttackGoal(SwMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.usesBlaster()
            && mob.getTarget() != null
            && mob.getTarget().isAlive()
            && mob.getSensing().hasLineOfSight(mob.getTarget());
    }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public void start() { this.cooldown = SwMobConstants.FIRE_INTERVAL_TICKS; }

    @Override
    public void stop() { this.cooldown = 0; }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        mob.getLookControl().setLookAt(target, 30, 30);
        if (--this.cooldown <= 0) {
            int color = mob.getTracerColor();
            if (mob.usesRifleBlaster()) {
                BlasterRifleItem.fireFromMobRifle(mob, target, color);
            } else {
                BlasterPistolItem.fireFromMob(mob, target, BlasterPistolItem.DAMAGE, color);
            }
            this.cooldown = SwMobConstants.FIRE_INTERVAL_TICKS;
        }
    }
}
```

`SwMob.java`:

```java
package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.ai.BlasterAttackGoal;
import com.tweeks.starwars.entity.ai.SwTargetGoal;
import com.tweeks.starwars.faction.SwCombatant;
import com.tweeks.starwars.faction.SwFaction;
import com.tweeks.starwars.network.S2CBlasterTracerPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class SwMob extends PathfinderMob implements SwCombatant {

    protected SwMob(EntityType<? extends SwMob> type, Level level) {
        super(type, level);
    }

    /** True for blaster-wielders (trooper, droid, Boba). Saber wielders return false. */
    public abstract boolean usesBlaster();

    /** True if the blaster is a rifle (heavier shot, slower isn't modeled — damage only). */
    public abstract boolean usesRifleBlaster();

    /** The item shown in the main hand (blaster or saber). */
    protected abstract ItemStack getWeaponStack();

    public int getTracerColor() {
        return getFaction() == SwFaction.LIGHT
            ? S2CBlasterTracerPacket.COLOR_LIGHT
            : S2CBlasterTracerPacket.COLOR_EMPIRE;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new BlasterAttackGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new SwTargetGoal(this));
    }

    @Override
    @org.jetbrains.annotations.Nullable
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.EntitySpawnReason reason,
            @org.jetbrains.annotations.Nullable net.minecraft.world.entity.SpawnGroupData spawnData) {
        net.minecraft.world.entity.SpawnGroupData result =
            super.finalizeSpawn(level, difficulty, reason, spawnData);
        this.setItemSlot(EquipmentSlot.MAINHAND, this.getWeaponStack());
        return result;
    }
}
```

The `finalizeSpawn` signature above is lifted from `wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineEntity.java` (the only repo mob that overrides it — it equips Herobrine's sword there); if it fails to compile, re-lift from that file. Named-character subclasses (Tasks 15-17, 31) override this again to also claim their singleton — they must call `super.finalizeSpawn(...)` so the weapon equip still runs.

- [ ] **Step 6: Build + commit**

```bash
./gradlew :starwars:build :starwars:test
git add starwars/
git commit -m "feat(starwars): SwMob base, faction targeting goals, blaster AI"
```

---

## Task 8: Stormtrooper + Battle Droid Entities (logic + registration)

**Files:**
- Create: `.../entity/StormtrooperEntity.java`, `.../entity/BattleDroidEntity.java`, `.../spawning/TrooperSpawnRules.java`
- Modify: `.../ModEntities.java`, `.../StarWarsMod.java`

**Interfaces:**
- Consumes: `SwMob`, `SwMobConstants` (Task 7), `Registration.BLASTER_PISTOL`/`BLASTER_RIFLE` (Task 6).
- Produces: `ModEntities.STORMTROOPER`, `ModEntities.BATTLE_DROID` (`MobCategory.MONSTER`, `.sized(0.6f, 1.95f)` trooper / `.sized(0.6f, 1.9f)` droid, `.clientTrackingRange(10)`); attributes + spawn placements registered in `StarWarsMod`.

- [ ] **Step 1: Create `StormtrooperEntity.java`**

```java
package com.tweeks.starwars.entity;

import com.tweeks.starwars.Registration;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class StormtrooperEntity extends SwMob implements Enemy {

    public StormtrooperEntity(EntityType<? extends StormtrooperEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, SwMobConstants.TROOPER_MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, SwMobConstants.TROOPER_MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, SwMobConstants.TROOPER_FOLLOW_RANGE);
    }

    @Override
    public SwFaction getFaction() { return SwFaction.EMPIRE; }

    @Override
    public boolean usesBlaster() { return true; }

    @Override
    public boolean usesRifleBlaster() { return true; }

    @Override
    protected ItemStack getWeaponStack() {
        return new ItemStack(Registration.BLASTER_RIFLE.get());
    }
}
```

- [ ] **Step 2: Create `BattleDroidEntity.java`** — same shape: `getFaction()` EMPIRE, `usesBlaster()` true, `usesRifleBlaster()` false, weapon `BLASTER_PISTOL`, attributes `DROID_MAX_HEALTH` / `DROID_MOVEMENT_SPEED` / `TROOPER_FOLLOW_RANGE`, and additionally override:

```java
@Override
public boolean fireImmune() { return false; }

// Droids clank instead of grunt: no ambient sound, metallic hurt sound.
@Override
protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
    return net.minecraft.sounds.SoundEvents.IRON_GOLEM_HURT;
}

@Override
protected net.minecraft.sounds.SoundEvent getDeathSound() {
    return net.minecraft.sounds.SoundEvents.IRON_GOLEM_DEATH;
}
```

- [ ] **Step 3: Register both in `ModEntities.java`** — the wildwest `EntityType.Builder` pattern verbatim:

```java
public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.StormtrooperEntity>> STORMTROOPER =
    ENTITY_TYPES.register("stormtrooper", () -> EntityType.Builder.<com.tweeks.starwars.entity.StormtrooperEntity>of(
            com.tweeks.starwars.entity.StormtrooperEntity::new, MobCategory.MONSTER)
        .sized(0.6f, 1.95f)
        .clientTrackingRange(10)
        .build(ResourceKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "stormtrooper"))));

public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.BattleDroidEntity>> BATTLE_DROID =
    ENTITY_TYPES.register("battle_droid", () -> EntityType.Builder.<com.tweeks.starwars.entity.BattleDroidEntity>of(
            com.tweeks.starwars.entity.BattleDroidEntity::new, MobCategory.MONSTER)
        .sized(0.6f, 1.9f)
        .clientTrackingRange(10)
        .build(ResourceKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "battle_droid"))));
```

(add the matching imports `MobCategory`, `ResourceKey`, `Identifier`, `Registries` as in wildwest's `ModEntities`).

- [ ] **Step 4: Spawn rules + wiring.** `TrooperSpawnRules.java`:

```java
package com.tweeks.starwars.spawning;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;

public final class TrooperSpawnRules {
    private TrooperSpawnRules() {}

    public static boolean checkSpawnRules(EntityType<? extends Mob> type,
                                          ServerLevelAccessor level,
                                          EntitySpawnReason reason,
                                          BlockPos pos,
                                          RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }
}
```

If the parameter type isn't `EntitySpawnReason` in this version, copy the exact signature from `wildwest/src/main/java/com/tweeks/wildwest/spawning/OutlawSpawnRules.java`.

In `StarWarsMod` constructor add the attribute listener + spawn placement listener (wildwest shape):

```java
modEventBus.addListener(StarWarsMod::registerEntityAttributes);

modEventBus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) -> {
    event.register(ModEntities.STORMTROOPER.get(),
        net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
        com.tweeks.starwars.spawning.TrooperSpawnRules::checkSpawnRules,
        net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
    event.register(ModEntities.BATTLE_DROID.get(),
        net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
        com.tweeks.starwars.spawning.TrooperSpawnRules::checkSpawnRules,
        net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
});
```

and the static method:

```java
private static void registerEntityAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
    event.put(ModEntities.STORMTROOPER.get(),
        com.tweeks.starwars.entity.StormtrooperEntity.createAttributes().build());
    event.put(ModEntities.BATTLE_DROID.get(),
        com.tweeks.starwars.entity.BattleDroidEntity.createAttributes().build());
}
```

- [ ] **Step 5: Build + commit**

```bash
./gradlew :starwars:build
git add starwars/
git commit -m "feat(starwars): stormtrooper + battle droid entities and registration"
```

---

## Task 9: Trooper Art — Models, Renderers, bbmodels, Textures, Eggs

**Files:**
- Create: `.../client/model/StormtrooperModel.java`, `.../client/model/BattleDroidModel.java`, `.../client/StormtrooperRenderer.java`, `.../client/BattleDroidRenderer.java`, `.../client/ClientSetup.java`, `starwars/tools/gen_bbmodels.py`, `starwars/tools/gen_textures.py`, `starwars/tools/gen_spawn_eggs.py`, `starwars/tools/stormtrooper.bbmodel`, `starwars/tools/battle_droid.bbmodel`, `assets/starwars/textures/entity/stormtrooper.png`, `.../battle_droid.png`, spawn-egg item JSONs/textures
- Modify: `.../Registration.java` (spawn eggs), `.../data/ModLanguageProvider.java`

**Interfaces:**
- Consumes: `ModEntities.STORMTROOPER`/`BATTLE_DROID` (Task 8).
- Produces: `StormtrooperModel.LAYER_LOCATION` + `createBodyLayer()`; `BattleDroidModel.LAYER_LOCATION` + `createBodyLayer()`; `ClientSetup` with `registerRenderers` + `registerLayerDefinitions` subscribers that every later character task extends; `Registration.STORMTROOPER_SPAWN_EGG`, `BATTLE_DROID_SPAWN_EGG`.

- [ ] **Step 1: `StormtrooperModel.java`** — DeputyModel pattern: humanoid + helmet cubes:

```java
package com.tweeks.starwars.client.model;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Stormtrooper: humanoid + helmet shell (a slightly-inflated head overlay
 * box) + chin vent ridge. Vanilla hat overlay hidden to avoid z-fighting.
 */
public class StormtrooperModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "stormtrooper"),
        "main");

    public StormtrooperModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        // Helmet shell: inflated head box using the hat-overlay UV region.
        head.addOrReplaceChild("helmet_shell",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, new CubeDeformation(0.6f)),
            PartPose.ZERO);
        // Chin vent ridge below the faceplate.
        head.addOrReplaceChild("chin_vent",
            CubeListBuilder.create()
                .texOffs(56, 16)
                .addBox(-1.5f, -1.5f, -5.0f, 3, 2, 1),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
```

- [ ] **Step 2: `BattleDroidModel.java`** — custom thin-limb skeleton. Same bone NAMES as `HumanoidModel` (`head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg`) so `HumanoidMobRenderer` animation works, but narrow boxes:

```java
package com.tweeks.starwars.client.model;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * B1 battle droid: spindly 2px limbs, narrow 6x10x3 torso, elongated
 * head with a 2px front snout. Replaces every humanoid box rather than
 * adding overlays; bone names/pivots stay vanilla so walk/swing animate.
 */
public class BattleDroidModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "battle_droid"),
        "main");

    public BattleDroidModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0f, -6.0f, -2.0f, 4, 6, 4)   // narrow skull
                .texOffs(16, 0).addBox(-1.0f, -3.0f, -5.0f, 2, 2, 3), // snout
            PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(0, 16).addBox(-3.0f, 0.0f, -1.5f, 6, 10, 3)
                .texOffs(18, 16).addBox(-1.0f, 10.0f, -1.0f, 2, 2, 2), // hip block
            PartPose.ZERO);
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(32, 16).addBox(-1.0f, -1.0f, -1.0f, 2, 12, 2),
            PartPose.offset(-4.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(40, 16).addBox(-1.0f, -1.0f, -1.0f, 2, 12, 2),
            PartPose.offset(4.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(48, 16).addBox(-1.0f, 0.0f, -1.0f, 2, 12, 2),
            PartPose.offset(-1.5f, 12.0f, 0.0f));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(56, 16).addBox(-1.0f, 0.0f, -1.0f, 2, 12, 2),
            PartPose.offset(1.5f, 12.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
```

- [ ] **Step 3: Renderers + ClientSetup** — `StormtrooperRenderer`/`BattleDroidRenderer` are copies of `wildwest/src/main/java/com/tweeks/wildwest/client/DeputyRenderer.java` with names/texture paths swapped (`textures/entity/stormtrooper.png`, `textures/entity/battle_droid.png`). `ClientSetup.java`:

```java
package com.tweeks.starwars.client;

import com.tweeks.starwars.ModEntities;
import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.BattleDroidModel;
import com.tweeks.starwars.client.model.StormtrooperModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = StarWarsMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.STORMTROOPER.get(), StormtrooperRenderer::new);
        event.registerEntityRenderer(ModEntities.BATTLE_DROID.get(), BattleDroidRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StormtrooperModel.LAYER_LOCATION, StormtrooperModel::createBodyLayer);
        event.registerLayerDefinition(BattleDroidModel.LAYER_LOCATION, BattleDroidModel::createBodyLayer);
    }
}
```

- [ ] **Step 4: bbmodel generator.** Create `starwars/tools/gen_bbmodels.py` by copying `wildwest/tools/gen_bbmodels.py` wholesale (keep `det_uuid`, bone constants, `HUMANOID_CUBES`, the Java→Blockbench coordinate conversion, and the writer) and replace the wildwest per-mob accessory tables with:

```python
STORMTROOPER_ACCESSORIES = [
    # (name, parent_bone, (jx, jy, jz, jw, jh, jd), uv, inflate)
    ('helmet_shell', HEAD_BONE, (-4.0, -8.0, -4.0, 8, 8, 8), (32, 0), 0.6),
    ('chin_vent',    HEAD_BONE, (-1.5, -1.5, -5.0, 3, 2, 1), (56, 16), 0.0),
]

# Battle droid REPLACES the humanoid cubes (custom skeleton).
BATTLE_DROID_CUBES = [
    ('head',      HEAD_BONE, (-2.0, -6.0, -2.0, 4, 6, 4),  (0, 0)),
    ('snout',     HEAD_BONE, (-1.0, -3.0, -5.0, 2, 2, 3),  (16, 0)),
    ('body',      BODY_BONE, (-3.0,  0.0, -1.5, 6, 10, 3), (0, 16)),
    ('hip_block', BODY_BONE, (-1.0, 10.0, -1.0, 2, 2, 2),  (18, 16)),
    ('right_arm', RARM_BONE, (-1.0, -1.0, -1.0, 2, 12, 2), (32, 16)),
    ('left_arm',  LARM_BONE, (-1.0, -1.0, -1.0, 2, 12, 2), (40, 16)),
    ('right_leg', RLEG_BONE, (-1.0,  0.0, -1.0, 2, 12, 2), (48, 16)),
    ('left_leg',  LLEG_BONE, (-1.0,  0.0, -1.0, 2, 12, 2), (56, 16)),
]

MOBS = {
    'stormtrooper': HUMANOID_CUBES + STORMTROOPER_ACCESSORIES,
    'battle_droid': BATTLE_DROID_CUBES,
}
```

If wildwest's cube tuples have no inflate slot, extend the writer to accept an optional 5th element (default 0.0) and emit Blockbench `"inflate"` on the cube. Note: the wildwest script writes each mob with explicit per-mob calls, not a loop — create the `MOBS` dict + a writer loop over it (later tasks then add one dict entry each), embedding each mob's texture PNG (`assets/starwars/textures/entity/<id>.png`) base64 like the wildwest script does. Run:

```bash
python3 starwars/tools/gen_bbmodels.py starwars/tools/
```

(after Step 5 so the textures exist to embed).

- [ ] **Step 5: Textures.** Create `starwars/tools/gen_textures.py` copying the `rect`/`fill`/`write_png`/`paint_humanoid_base` helpers from `wildwest/tools/gen_textures.py`. Finished-art bar: every visible region gets base color + at least one darker shade row/column (bottom + right edges) and one highlight (top edge) — no flat single-color limbs.

```python
# Stormtrooper: white plastoid armor, black bodysuit seams, black eye slits.
WHITE      = (0xEE, 0xEE, 0xF2, 0xFF)
WHITE_HI   = (0xFF, 0xFF, 0xFF, 0xFF)
WHITE_SH   = (0xC8, 0xC8, 0xD2, 0xFF)
SUIT_BLACK = (0x16, 0x16, 0x1A, 0xFF)
GRAY       = (0x8A, 0x8A, 0x94, 0xFF)

def paint_stormtrooper(rgba):
    fill(rgba, WHITE)
    # Head front (u 8..16, v 8..16): faceplate with eye slits + frown vent.
    rect(rgba, 8, 8, 16, 16, WHITE_HI)
    rect(rgba, 9, 10, 11, 11, SUIT_BLACK)   # left eye slit
    rect(rgba, 13, 10, 15, 11, SUIT_BLACK)  # right eye slit
    rect(rgba, 10, 13, 14, 14, GRAY)        # aerator frown
    rect(rgba, 11, 14, 13, 15, SUIT_BLACK)  # chin vent
    # Helmet-overlay region (u 32..64, v 0..16): white shell, gray brow band.
    rect(rgba, 32, 0, 64, 16, WHITE)
    rect(rgba, 40, 8, 56, 9, GRAY)
    # Body (u 16..40, v 16..32): chest plate + black under-suit midriff.
    rect(rgba, 16, 16, 40, 32, WHITE)
    rect(rgba, 20, 26, 36, 28, SUIT_BLACK)  # ab plate seam
    rect(rgba, 26, 20, 30, 24, GRAY)        # chest control box
    # Arms/legs: white with black joint seams + shading.
    for (u0, v0) in ((40, 16), (32, 48), (0, 16), (16, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, WHITE)
        rect(rgba, u0, v0 + 6, u0 + 16, v0 + 7, SUIT_BLACK)   # elbow/knee seam
        rect(rgba, u0, v0 + 14, u0 + 16, v0 + 16, WHITE_SH)   # lower shade
        rect(rgba, u0, v0, u0 + 16, v0 + 1, WHITE_HI)         # top highlight
    # Global shading pass: darken the right/bottom edge of head + body.
    rect(rgba, 30, 8, 32, 16, WHITE_SH)
    rect(rgba, 16, 30, 40, 32, WHITE_SH)
```

```python
# Battle droid: sand-tan metal, dark joints, black eye dots on the snouted head.
TAN     = (0xB8, 0xA0, 0x78, 0xFF)
TAN_HI  = (0xD2, 0xBC, 0x94, 0xFF)
TAN_SH  = (0x8E, 0x78, 0x56, 0xFF)
JOINT   = (0x50, 0x44, 0x32, 0xFF)
EYE     = (0x10, 0x10, 0x10, 0xFF)

def paint_battle_droid(rgba):
    fill(rgba, (0, 0, 0, 0))                # custom UV layout — transparent base
    rect(rgba, 0, 0, 16, 10, TAN)           # head region (u0..16, v0..10)
    rect(rgba, 4, 4, 5, 6, EYE)             # eyes on the head front rows
    rect(rgba, 7, 4, 8, 6, EYE)
    rect(rgba, 16, 0, 26, 5, TAN_SH)        # snout region
    rect(rgba, 0, 16, 18, 29, TAN)          # body strip
    rect(rgba, 0, 16, 18, 17, TAN_HI)
    rect(rgba, 0, 27, 18, 29, TAN_SH)
    rect(rgba, 18, 16, 26, 20, JOINT)       # hip block
    for u0 in (32, 40, 48, 56):             # four 2x12x2 limbs at v16
        rect(rgba, u0, 16, u0 + 8, 30, TAN)
        rect(rgba, u0, 21, u0 + 8, 22, JOINT)  # mid-joint
        rect(rgba, u0, 28, u0 + 8, 30, TAN_SH)
        rect(rgba, u0, 16, u0 + 8, 17, TAN_HI)
```

Wire both into a `MOBS = {'stormtrooper': paint_stormtrooper, 'battle_droid': paint_battle_droid}` writer loop targeting `starwars/src/main/resources/assets/starwars/textures/entity/`. Run it, then eyeball both PNGs (open them) — they must read as a stormtrooper (white armor, black eye slits) and a B1 droid (tan, spindly) before committing. Refine rects until they do; this is the no-placeholder gate.

- [ ] **Step 6: Spawn eggs.** In `Registration.java` (wildwest pattern):

```java
public static final DeferredItem<SpawnEggItem> STORMTROOPER_SPAWN_EGG = ITEMS.registerItem(
    "stormtrooper_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.STORMTROOPER.get()));

public static final DeferredItem<SpawnEggItem> BATTLE_DROID_SPAWN_EGG = ITEMS.registerItem(
    "battle_droid_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.BATTLE_DROID.get()));
```

Add both to the creative tab `displayItems`. Copy `wildwest/tools/gen_spawn_eggs.py` to `starwars/tools/` — note it generates egg TEXTURES only (white/black for the trooper, tan/brown for the droid); the egg `items/*.json` + `models/item/*.json` are hand-authored, so copy those from wildwest's committed egg JSONs with the ids swapped. Run the script and commit textures + JSONs. Lang: `add(...STORMTROOPER_SPAWN_EGG.get(), "Stormtrooper Spawn Egg")`, same for droid, plus entity names `add(ModEntities.STORMTROOPER.get(), "Stormtrooper")`, `add(ModEntities.BATTLE_DROID.get(), "Battle Droid")`.

- [ ] **Step 7: Build, datagen, verify art, commit**

```bash
./gradlew :starwars:build :starwars:runClientData
python3 starwars/tools/gen_textures.py
python3 starwars/tools/gen_bbmodels.py starwars/tools/
python3 starwars/tools/gen_spawn_eggs.py
git add starwars/
git commit -m "feat(starwars): trooper+droid models, renderers, bbmodels, finished textures, eggs"
```

---

## Task 10: Trooper Loot, Spawning, Language

**Files:**
- Create: `.../data/ModEntityLootProvider.java`, `.../data/ModBiomeModifierProvider.java`
- Modify: `.../data/DataGenerators.java`

**Interfaces:**
- Consumes: `ModEntities.STORMTROOPER`/`BATTLE_DROID`.
- Produces: `ModEntityLootProvider` (later tasks add tables), `ModBiomeModifierProvider.bootstrap` with `ADD_STORMTROOPERS` (weight 8, group 2–4: plains, desert, savanna, badlands) and `ADD_BATTLE_DROIDS` (weight 6, group 3–5: desert, badlands).

- [ ] **Step 1: Create `ModEntityLootProvider`** — mirror wildwest's class shell (`extends EntityLootSubProvider`, `FeatureFlags.REGISTRY.allFlags()`); in `generate()`:

```java
// Stormtrooper: 0-2 iron_nugget @60%; 25% blaster rifle drop (30%+ durability).
this.add(ModEntities.STORMTROOPER.get(),
    LootTable.lootTable()
        .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
            .add(LootItem.lootTableItem(Items.IRON_NUGGET).setWeight(60)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0f, 2.0f))))
            .add(EmptyLootItem.emptyItem().setWeight(40)))
        .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
            .when(LootItemRandomChanceCondition.randomChance(0.25f))
            .add(LootItem.lootTableItem(Registration.BLASTER_RIFLE.get())
                .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.0f, 0.7f))))));

// Battle droid: 1-3 iron_nugget @80%; 15% blaster pistol drop.
this.add(ModEntities.BATTLE_DROID.get(),
    LootTable.lootTable()
        .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
            .add(LootItem.lootTableItem(Items.IRON_NUGGET).setWeight(80)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 3.0f))))
            .add(EmptyLootItem.emptyItem().setWeight(20)))
        .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
            .when(LootItemRandomChanceCondition.randomChance(0.15f))
            .add(LootItem.lootTableItem(Registration.BLASTER_PISTOL.get())
                .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.0f, 0.7f))))));
```

Copy imports from `wildwest/src/main/java/com/tweeks/wildwest/data/ModEntityLootProvider.java`. Include the same `getKnownEntityTypes()` override style wildwest uses if present (check the tail of that file; mirror it for starwars entities).

- [ ] **Step 2: Create `ModBiomeModifierProvider`** — wildwest shape with:

```java
ctx.register(ADD_STORMTROOPERS, new BiomeModifiers.AddSpawnsBiomeModifier(
    trooperBiomes,   // PLAINS, DESERT, SAVANNA, BADLANDS
    WeightedList.of(java.util.List.of(new Weighted<>(
        new MobSpawnSettings.SpawnerData(ModEntities.STORMTROOPER.get(), 2, 4), 8)))));

ctx.register(ADD_BATTLE_DROIDS, new BiomeModifiers.AddSpawnsBiomeModifier(
    droidBiomes,     // DESERT, BADLANDS
    WeightedList.of(java.util.List.of(new Weighted<>(
        new MobSpawnSettings.SpawnerData(ModEntities.BATTLE_DROID.get(), 3, 5), 6)))));
```

- [ ] **Step 3: Wire into `DataGenerators.gatherDataServer`** — add to the `RegistrySetBuilder`:

```java
.add(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS,
     ModBiomeModifierProvider::bootstrap)
```

and add the loot provider (wildwest shape):

```java
gen.addProvider(true, new net.minecraft.data.loot.LootTableProvider(
    output,
    Set.of(),
    java.util.List.of(new net.minecraft.data.loot.LootTableProvider.SubProviderEntry(
        ModEntityLootProvider::new,
        net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY)),
    lookup));
```

- [ ] **Step 4: Datagen, build, commit**

```bash
./gradlew :starwars:runServerData :starwars:build
git add starwars/
git commit -m "feat(starwars): trooper/droid loot + biome spawn weighting"
```

---

## Task 11: Milestone 2 Bedrock Pass

**Files:**
- Create/modify: `bedrock-out/starwars/**` (generated)

- [ ] **Step 1: Run the translator**

```bash
./gradlew :translator:translate --args="starwars"
```
Expected: BUILD SUCCESSFUL; `bedrock-out/starwars/` now contains behavior/resource packs, and `bedrock-out/starwars/UNTRANSLATABLE.md` lists any gaps (hitscan custom behavior and tracer packets will appear — that is correct and expected).

- [ ] **Step 2: Inspect UNTRANSLATABLE.md** — read it; confirm nothing listed is a surprise (expected entries: blaster item custom `use` behavior, network payloads, attachment logic). If the translator crashed on a stage, fix the starwars-side input (not the translator) or record the failure verbatim in the task summary.

- [ ] **Step 3: Commit**

```bash
git add bedrock-out/starwars/
git commit -m "feat(starwars): milestone-2 bedrock translation"
```

---

# Milestone 3 — Lightsabers + Named Heroes

## Task 12: Lightsaber Item, Blade Colors, 3D Weapon Models

**Files:**
- Create: `.../item/SaberColor.java`, `.../item/ModDataComponents.java`, `.../item/LightsaberItem.java`, `starwars/tools/gen_weapon_models.py`, `starwars/tools/lightsaber.bbmodel`, `starwars/tools/blaster_pistol.bbmodel`, `starwars/tools/blaster_rifle.bbmodel`, `assets/starwars/items/lightsaber.json`, `assets/starwars/models/item/lightsaber_{blue,green,red,purple}.json`, `assets/starwars/textures/item/lightsaber_{blue,green,red,purple}.png`
- Modify: `.../StarWarsMod.java` (register components), `.../entity/SwMob.java` (melee goal for saber wielders), `.../Registration.java`, `.../ModSounds.java`, `assets/starwars/sounds.json`, `.../data/ModLanguageProvider.java`, and replace the flat `models/item/blaster_*.json` from Task 6 with generated 3D voxel models

**Interfaces:**
- Consumes: `StarWarsDamageTypes.lightsaber` (Task 2), `SwMob` (Task 7).
- Produces: `SaberColor { BLUE, GREEN, RED, PURPLE }` with `int argb()`, `String suffix()`, `static SaberColor byIndex(int)` (out-of-range → BLUE); `ModDataComponents.BLADE_COLOR` (`DataComponentType<Integer>`); `LightsaberItem` — `SABER_DAMAGE = 9.0F`, `stackWithColor(SaberColor)` static factory returning an `ItemStack` with the component set; `Registration.LIGHTSABER`; `ModSounds.SABER_IGNITE`, `SABER_HUM`, `SABER_CLASH`. Hero tasks equip via `LightsaberItem.stackWithColor(...)`.

- [ ] **Step 1: `SaberColor.java`**

```java
package com.tweeks.starwars.item;

public enum SaberColor {
    BLUE(0xFF3060FF, "blue"),
    GREEN(0xFF30E050, "green"),
    RED(0xFFFF2020, "red"),
    PURPLE(0xFFA030E0, "purple");

    private final int argb;
    private final String suffix;

    SaberColor(int argb, String suffix) {
        this.argb = argb;
        this.suffix = suffix;
    }

    public int argb() { return argb; }
    public String suffix() { return suffix; }

    public static SaberColor byIndex(int index) {
        return (index < 0 || index >= values().length) ? BLUE : values()[index];
    }
}
```

- [ ] **Step 2: `ModDataComponents.java`** (item package) — the wildwest `ACTIVE_STONE` pattern:

```java
package com.tweeks.starwars.item;

import com.mojang.serialization.Codec;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, StarWarsMod.MOD_ID);

    /** Blade color index into SaberColor.values(); defaults to 0 (BLUE). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BLADE_COLOR =
        COMPONENTS.registerComponentType(
            "blade_color",
            builder -> builder
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.VAR_INT));

    // ACTIVE_POWER + POWER_COOLDOWNS join in Milestone 5.

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
```

Wire `com.tweeks.starwars.item.ModDataComponents.register(modEventBus);` into `StarWarsMod`'s constructor.

- [ ] **Step 3: `LightsaberItem.java`.** Melee stat wiring: lift the exact `Item.Properties` weapon calls from `wildwest/src/main/java/com/tweeks/wildwest/item/RapierItem.java` (open it; it is the repo's canonical high-damage melee item on this MC version) and use damage `SABER_DAMAGE = 9.0F`, attack speed matching the rapier's, durability 1500, `stacksTo(1)`:

```java
package com.tweeks.starwars.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class LightsaberItem extends Item {   // extend what RapierItem extends if not Item

    public static final float SABER_DAMAGE = 9.0F;

    public LightsaberItem(Properties properties) {
        super(properties /* + the RapierItem-style weapon wiring */);
    }

    public static SaberColor colorOf(ItemStack stack) {
        return SaberColor.byIndex(stack.getOrDefault(
            ModDataComponents.BLADE_COLOR.get(), 0));
    }

    public static ItemStack stackWithColor(SaberColor color) {
        ItemStack stack = new ItemStack(com.tweeks.starwars.Registration.LIGHTSABER.get());
        stack.set(ModDataComponents.BLADE_COLOR.get(), color.ordinal());
        return stack;
    }
}
```

Register in `Registration`: `public static final DeferredItem<LightsaberItem> LIGHTSABER = ITEMS.registerItem("lightsaber", LightsaberItem::new, p -> p);` (with whatever property lambda the rapier uses) and add one stack per color to the creative tab via `displayItems` using `stackWithColor` (use the `output.accept(ItemStack)` overload).

- [ ] **Step 4: Melee goal for saber wielders** — in `SwMob.registerGoals()`, after the `BlasterAttackGoal` line add:

```java
this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.2, true) {
    @Override
    public boolean canUse() { return !SwMob.this.usesBlaster() && super.canUse(); }
    @Override
    public boolean canContinueToUse() { return !SwMob.this.usesBlaster() && super.canContinueToUse(); }
});
```

(`BlasterAttackGoal.canUse()` already returns false for non-blaster mobs, so exactly one of the two goals activates per mob.)

- [ ] **Step 5: 3D models + bbmodels.** Create `starwars/tools/gen_weapon_models.py`. Study `wildwest/tools/gen_meteor_staff.py` first and reuse its approach: the script emits BOTH the element-based `models/item/<name>.json` and a matching `tools/<name>.bbmodel` (deterministic UUIDs) from one cube list, plus GUI handling copied from how `assets/wildwest/items/meteor_staff.json` selects between 3D and GUI representations (mirror that JSON's structure exactly — open it and copy the selector shape). Cube lists (units = model pixels, y up, matching the wildwest pistol element style):

```python
# Lightsaber: hilt along the handle axis + glowing blade.
# One model per color; blade tint comes from the per-color texture strip.
LIGHTSABER_CUBES = [
    # (name, from, to, texture_region)
    ('pommel', (7, 0, 7),  (9, 2, 9),   'hilt_dark'),
    ('grip',   (7, 2, 7),  (9, 7, 9),   'hilt'),
    ('emitter',(6.5, 7, 6.5), (9.5, 9, 9.5), 'hilt_dark'),
    ('blade',  (7.25, 9, 7.25), (8.75, 24, 8.75), 'blade'),
]

BLASTER_PISTOL_CUBES = [
    ('body',   (6, 5, 4),  (10, 8, 13), 'metal'),
    ('barrel', (7, 6, 13), (9, 7.5, 16),'metal_dark'),
    ('grip',   (6.5, 1, 4),(9.5, 5, 7), 'grip'),
]

BLASTER_RIFLE_CUBES = [
    ('stock',  (6.5, 4, 0),(9.5, 7, 5), 'wood'),
    ('body',   (6, 5, 5),  (10, 8, 12), 'metal'),
    ('barrel', (7, 6, 12), (9, 7.5, 18),'metal_dark'),
    ('scope',  (7, 8, 7),  (9, 9.5, 10),'metal_dark'),
]
```

Textures: per-color `lightsaber_<color>.png` 16×16 — hilt grays (`0x9A9AA4` metal, `0x3A3A40` dark, `0x2A2A2E` grip) in the lower half, a solid saturated blade strip in the upper half using the `SaberColor` RGB values above, with a 1px white-hot core column inside the blade strip. Blaster textures reuse Task 6's palettes. The script maps each cube's `texture_region` to explicit UV rects on those textures. Run:

```bash
python3 starwars/tools/gen_weapon_models.py
```

which writes the four `models/item/lightsaber_<color>.json`, overwrites `models/item/blaster_{pistol,rifle}.json` with the 3D versions (keep the Task 6 `items/*.json` selector files pointing at the same model paths), and writes the three `.bbmodel` files. Open one `.bbmodel` in a JSON viewer to confirm cubes exist; verify byte-identical on re-run.

- [ ] **Step 6: Color-driven model selection.** `assets/starwars/items/lightsaber.json` selects the model by the `BLADE_COLOR` component. The item-model definition format on this MC version supports select-by-component — confirm the exact property syntax by finding a `"type": "minecraft:select"` example among vanilla item definitions (the client jar's `assets/minecraft/items/` — e.g. the crossbow/bundle definitions; extract with `unzip -p` on the versioned client jar under `~/.gradle` caches). Note: `assets/wildwest/items/rifle.json` is NOT a select example — it uses `minecraft:range_dispatch` on cooldown; the vanilla jar is the load-bearing reference here. Target shape:

```json
{
  "model": {
    "type": "minecraft:select",
    "property": "minecraft:component",
    "component": "starwars:blade_color",
    "cases": [
      { "when": 0, "model": { "type": "minecraft:model", "model": "starwars:item/lightsaber_blue" } },
      { "when": 1, "model": { "type": "minecraft:model", "model": "starwars:item/lightsaber_green" } },
      { "when": 2, "model": { "type": "minecraft:model", "model": "starwars:item/lightsaber_red" } },
      { "when": 3, "model": { "type": "minecraft:model", "model": "starwars:item/lightsaber_purple" } }
    ],
    "fallback": { "type": "minecraft:model", "model": "starwars:item/lightsaber_blue" }
  }
}
```

If component-select is unavailable in this version, fall back to four registered items (`lightsaber_blue` etc., one `DeferredItem` each sharing the `LightsaberItem` class, `stackWithColor` becomes a lookup of the right item) and record the deviation in the task summary — do NOT ship a saber whose color doesn't render.

**Spec deviation (record in task summary):** the spec's "emissive blade texture" is not achievable in data-driven item models (no full-bright render type without a custom item renderer). The blade uses maximally saturated bright colors with a white-hot core instead; note this in the commit body and let it surface in UNTRANSLATABLE-style honesty at Task 32.

- [ ] **Step 7: Sounds + lang.** `ModSounds`: add `SABER_IGNITE = register("saber_ignite")`, `SABER_CLASH = register("saber_clash")`. `sounds.json` additions:

```json
"saber_ignite": {
  "category": "player",
  "subtitle": "subtitle.starwars.saber_ignite",
  "sounds": [ { "name": "minecraft:block.beacon.activate", "type": "event", "pitch": 1.8 } ]
},
"saber_clash": {
  "category": "player",
  "subtitle": "subtitle.starwars.saber_clash",
  "sounds": [ { "name": "minecraft:entity.zombie.attack_iron_door", "type": "event", "pitch": 1.7 } ]
}
```

Lang: `add(Registration.LIGHTSABER.get(), "Lightsaber")` + the two subtitles (`"Lightsaber ignites"`, `"Lightsabers clash"`). Both sounds must actually play:
  - `SABER_CLASH` on hit: override the item's on-attack hook — use whichever hook `RapierItem`/`BanditKnifeItem` overrides for hit effects (`hurtEnemy` or `postHurtEnemy` — lift the exact override from those files); inside, `level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.SABER_CLASH.get(), SoundSource.PLAYERS, 0.8F, 1.0F)`.
  - `SABER_IGNITE` on right-click flourish: add to `LightsaberItem`:

```java
@Override
public InteractionResult use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (!level.isClientSide() && !player.getCooldowns().isOnCooldown(stack)) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            com.tweeks.starwars.ModSounds.SABER_IGNITE.get(),
            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        player.getCooldowns().addCooldown(stack, 20);
    }
    return InteractionResult.CONSUME;
}
```

(imports: `InteractionResult`, `InteractionHand`, `Player`, `Level`.)

- [ ] **Step 8: Build + commit**

```bash
./gradlew :starwars:build :starwars:runClientData
git add starwars/
git commit -m "feat(starwars): lightsaber item with blade colors, 3D weapon models + bbmodels"
```

---

## Task 13: Named-Character Singleton SavedData + Tests

**Files:**
- Create: `.../entity/SingletonState.java`, `.../entity/NamedCharacterSavedData.java`, `.../entity/VaderSavedData.java`, `.../entity/LukeSavedData.java`, `.../entity/ObiWanSavedData.java`, `.../entity/BobaFettSavedData.java`
- Test: `starwars/src/test/java/com/tweeks/starwars/entity/SingletonStateTest.java`

**Interfaces:**
- Consumes: nothing (self-contained).
- Produces: `SingletonState` POJO (`isAlive()`, `getCurrentId()`, `getDimensionId()`, `setAlive(UUID, String)`, `clear()`, `copyOf`); `NamedCharacterSavedData` abstract base (`isAlive()`, `getCurrentId()`, `getDimension()`, `setAlive(UUID, ResourceKey<Level>)`, `clear()`, protected static `buildCodec(Supplier<T>)`); four subclasses each exposing `CODEC`, `TYPE`, and `static get(MinecraftServer)`. File ids: `starwars_vader`, `starwars_luke`, `starwars_obi_wan`, `starwars_boba_fett`.

- [ ] **Step 1: Write the failing test**

```java
package com.tweeks.starwars.entity;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SingletonStateTest {

    @Test
    void freshState_isDead() {
        SingletonState s = new SingletonState();
        assertFalse(s.isAlive());
        assertNull(s.getCurrentId());
        assertNull(s.getDimensionId());
    }

    @Test
    void setAlive_storesIdAndDimension() {
        SingletonState s = new SingletonState();
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000042");
        s.setAlive(id, "minecraft:overworld");
        assertTrue(s.isAlive());
        assertEquals(id, s.getCurrentId());
        assertEquals("minecraft:overworld", s.getDimensionId());
    }

    @Test
    void clear_resetsEverything() {
        SingletonState s = new SingletonState();
        s.setAlive(UUID.randomUUID(), "minecraft:the_nether");
        s.clear();
        assertFalse(s.isAlive());
        assertNull(s.getCurrentId());
        assertNull(s.getDimensionId());
    }

    @Test
    void copyOf_copiesLiveState_andDeadState() {
        SingletonState live = new SingletonState();
        UUID id = UUID.randomUUID();
        live.setAlive(id, "minecraft:overworld");
        SingletonState copy = SingletonState.copyOf(live);
        assertTrue(copy.isAlive());
        assertEquals(id, copy.getCurrentId());

        SingletonState deadCopy = SingletonState.copyOf(new SingletonState());
        assertFalse(deadCopy.isAlive());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :starwars:test --tests SingletonStateTest
```
Expected: FAIL — class does not exist.

- [ ] **Step 3: Implement** — `SingletonState` is a byte-for-byte port of `wildwest/src/main/java/com/tweeks/wildwest/entity/BossSingletonState.java` (rename class, package `com.tweeks.starwars.entity`). `NamedCharacterSavedData` is a port of `wildwest/.../entity/BossSingletonSavedData.java` (same codec field names `"Alive"`/`"CurrentId"`/`"Dimension"`, same `buildCodec` shape, field type `SingletonState`). Each subclass is a port of `wildwest/.../entity/HerobrineSavedData.java`, e.g.:

```java
package com.tweeks.starwars.entity;

import com.mojang.serialization.Codec;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class VaderSavedData extends NamedCharacterSavedData {

    private static final String FILE_ID = "starwars_vader";

    public static final Codec<VaderSavedData> CODEC = buildCodec(VaderSavedData::new);

    public static final SavedDataType<VaderSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, FILE_ID),
        VaderSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    public VaderSavedData() {}

    public static VaderSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
```

Repeat for `LukeSavedData` (`starwars_luke`), `ObiWanSavedData` (`starwars_obi_wan`), `BobaFettSavedData` (`starwars_boba_fett`).

- [ ] **Step 4: Run test to verify it passes, then commit**

```bash
./gradlew :starwars:test --tests SingletonStateTest
git add starwars/
git commit -m "feat(starwars): named-character singleton SavedData (Vader/Luke/Obi-Wan/Boba)"
```

---

## Task 14: Jedi Knight (complete character)

**Files:**
- Create: `.../entity/JediKnightEntity.java`, `.../client/model/JediKnightModel.java`, `.../client/JediKnightRenderer.java`, `starwars/tools/jedi_knight.bbmodel`, `assets/starwars/textures/entity/jedi_knight.png`, spawn-egg assets
- Modify: `.../ModEntities.java`, `.../StarWarsMod.java` (attributes + placement), `.../Registration.java` (egg), `.../client/ClientSetup.java`, `.../data/ModLanguageProvider.java`, `.../data/ModEntityLootProvider.java`, `.../data/ModBiomeModifierProvider.java`, `starwars/tools/gen_bbmodels.py`, `starwars/tools/gen_textures.py`

**Interfaces:**
- Consumes: `SwMob`, `LightsaberItem.stackWithColor`, `SaberColor`.
- Produces: `ModEntities.JEDI_KNIGHT` (`MobCategory.CREATURE`, sized 0.6×1.95). Registry id `jedi_knight`.

- [ ] **Step 1: Entity class**

```java
package com.tweeks.starwars.entity;

import com.tweeks.starwars.faction.SwFaction;
import com.tweeks.starwars.item.LightsaberItem;
import com.tweeks.starwars.item.SaberColor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class JediKnightEntity extends SwMob {

    public static final double MAX_HEALTH = 30.0;
    public static final double ATTACK_DAMAGE = 7.0;
    public static final double MOVEMENT_SPEED = 0.32;

    public JediKnightEntity(EntityType<? extends JediKnightEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    public SwFaction getFaction() { return SwFaction.LIGHT; }

    @Override
    public boolean usesBlaster() { return false; }

    @Override
    public boolean usesRifleBlaster() { return false; }

    @Override
    protected ItemStack getWeaponStack() {
        // Half the knights ignite blue, half green.
        return LightsaberItem.stackWithColor(
            this.getRandom().nextBoolean() ? SaberColor.BLUE : SaberColor.GREEN);
    }
}
```

- [ ] **Step 2: Registration + attributes + placement.** `ModEntities.JEDI_KNIGHT` (`MobCategory.CREATURE`, `.sized(0.6f, 1.95f)`, `.clientTrackingRange(10)` — same builder shape as Task 8). Attributes line in `registerEntityAttributes`. Spawn placement with `TrooperSpawnRules::checkSpawnRules` is WRONG for a CREATURE-category mob (monster rules demand darkness). The repo has no generic CREATURE surface rule to lift (the crab's is beach-substrate-specific; deputies register none — they're village-spawner-driven), so use vanilla's generic mob rule directly: `net.minecraft.world.entity.PathfinderMob::checkMobSpawnRules`, verified by compiling; if that name doesn't resolve, find the current generic rule on `Mob`/`PathfinderMob` in the sources jar.

- [ ] **Step 3: Model — humanoid + robe.** `JediKnightModel` (DeputyModel pattern, LAYER_LOCATION id `jedi_knight`):

```java
PartDefinition body = root.getChild("body");
// Robe skirt: hangs from the hips over the top half of the legs.
body.addOrReplaceChild("robe_skirt",
    CubeListBuilder.create()
        .texOffs(32, 32)
        .addBox(-4.5f, 12.0f, -2.5f, 9, 7, 5),
    PartPose.ZERO);
PartDefinition head = root.getChild("head");
// Hood: inflated head shell, open at the front (single box is fine —
// the face texture region of the hood strip is left transparent).
head.addOrReplaceChild("hood",
    CubeListBuilder.create()
        .texOffs(32, 0)
        .addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, new CubeDeformation(0.5f)),
    PartPose.ZERO);
```

Renderer: DeputyRenderer copy, texture `textures/entity/jedi_knight.png`. Register renderer + layer in `ClientSetup`.

- [ ] **Step 4: Texture + bbmodel.** Add to `gen_textures.py`:

```python
# Jedi knight: cream tunic, brown robe/hood, tan belt.
TUNIC   = (0xD8, 0xCC, 0xB0, 0xFF)
TUNIC_S = (0xB4, 0xA8, 0x8C, 0xFF)
ROBE    = (0x6A, 0x50, 0x34, 0xFF)
ROBE_S  = (0x4E, 0x3A, 0x26, 0xFF)
BELT    = (0x3C, 0x2C, 0x1C, 0xFF)
SKIN    = (0xE8, 0xC0, 0x98, 0xFF)
HAIR    = (0x4A, 0x36, 0x22, 0xFF)

def paint_jedi_knight(rgba):
    fill(rgba, TUNIC)
    rect(rgba, 0, 0, 32, 8, HAIR)          # head top: hair
    rect(rgba, 8, 8, 16, 16, SKIN)         # face
    rect(rgba, 8, 8, 16, 10, HAIR)         # hairline
    rect(rgba, 10, 11, 11, 12, (0x20, 0x30, 0x50, 0xFF))  # eyes
    rect(rgba, 13, 11, 14, 12, (0x20, 0x30, 0x50, 0xFF))
    rect(rgba, 32, 0, 64, 16, ROBE)        # hood overlay strip
    rect(rgba, 40, 8, 48, 16, (0, 0, 0, 0))  # hood front opening (transparent)
    rect(rgba, 16, 16, 40, 32, TUNIC)      # body
    rect(rgba, 20, 28, 36, 30, BELT)       # belt
    rect(rgba, 16, 30, 40, 32, TUNIC_S)
    for (u0, v0) in ((40, 16), (32, 48)):  # arms: robe sleeves
        rect(rgba, u0, v0, u0 + 16, v0 + 16, ROBE)
        rect(rgba, u0, v0 + 13, u0 + 16, v0 + 16, ROBE_S)
    for (u0, v0) in ((0, 16), (16, 48)):   # legs: dark trousers
        rect(rgba, u0, v0, u0 + 16, v0 + 16, (0x50, 0x46, 0x38, 0xFF))
        rect(rgba, u0, v0 + 14, u0 + 16, v0 + 16, (0x38, 0x30, 0x26, 0xFF))
    rect(rgba, 32, 32, 64, 48, ROBE)       # robe skirt region (u32.., v32..)
    rect(rgba, 32, 45, 64, 48, ROBE_S)
```

Add to `gen_bbmodels.py`:

```python
JEDI_KNIGHT_ACCESSORIES = [
    ('robe_skirt', BODY_BONE, (-4.5, 12.0, -2.5, 9, 7, 5), (32, 32), 0.0),
    ('hood',       HEAD_BONE, (-4.0, -8.0, -4.0, 8, 8, 8), (32, 0), 0.5),
]
MOBS['jedi_knight'] = HUMANOID_CUBES + JEDI_KNIGHT_ACCESSORIES
```

Run both scripts; eyeball the PNG (cream tunic, brown hood — must read as a robed Jedi).

- [ ] **Step 5: Egg, loot, lang, spawns.**
  - Egg: `JEDI_KNIGHT_SPAWN_EGG` (Registration + gen_spawn_eggs.py entry, cream/brown) + creative tab.
  - Loot: 20% lightsaber drop is too generous for a common mob and sabers are the ruin's treasure — give `0-1 glowstone_dust @50%` + `10%` saber drop:

```java
this.add(ModEntities.JEDI_KNIGHT.get(),
    LootTable.lootTable()
        .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
            .add(LootItem.lootTableItem(Items.GLOWSTONE_DUST).setWeight(50)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0f, 1.0f))))
            .add(EmptyLootItem.emptyItem().setWeight(50)))
        .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
            .when(LootItemRandomChanceCondition.randomChance(0.10f))
            .add(LootItem.lootTableItem(Registration.LIGHTSABER.get()))));
```

  - Lang: `"Jedi Knight"` + egg name.
  - Spawns: `ADD_JEDI_KNIGHTS` biome modifier — weight 4, group 1–2, biomes FOREST, JUNGLE, TAIGA.

- [ ] **Step 6: Build, datagen, commit**

```bash
./gradlew :starwars:build :starwars:test :starwars:runClientData :starwars:runServerData
git add starwars/
git commit -m "feat(starwars): Jedi Knight — entity, art, loot, spawning"
```

---

## Task 15: Darth Vader (named elite + choke)

**Files:**
- Create: `.../entity/DarthVaderEntity.java`, `.../entity/ai/VaderChokeGoal.java`, `.../client/model/VaderModel.java`, `.../client/VaderRenderer.java`, `starwars/tools/darth_vader.bbmodel`, `assets/starwars/textures/entity/darth_vader.png`, spawn-egg assets
- Modify: `.../ModEntities.java`, `.../StarWarsMod.java`, `.../Registration.java`, `.../client/ClientSetup.java`, datagen providers, `starwars/tools/gen_bbmodels.py`, `starwars/tools/gen_textures.py`

**Interfaces:**
- Consumes: `SwMob`, `VaderSavedData` (Task 13), `LightsaberItem.stackWithColor(SaberColor.RED)`.
- Produces: `ModEntities.DARTH_VADER` (id `darth_vader`, `MobCategory.MONSTER`, sized 0.6×2.0); the die()-clears-singleton pattern all named characters copy.

- [ ] **Step 1: `VaderChokeGoal.java`** — ranged choke: slow + levitate + periodic damage, 10s cooldown:

```java
package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.StarWarsDamageTypes;
import com.tweeks.starwars.entity.DarthVaderEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Force choke: when the target is 4-10 blocks away with line of sight,
 * hold it for 3 seconds — levitation + slowness + 1 damage every 20 ticks.
 * 200-tick cooldown between chokes.
 */
public class VaderChokeGoal extends Goal {

    public static final int CHOKE_DURATION_TICKS = 60;
    public static final int COOLDOWN_TICKS = 200;
    public static final double MIN_RANGE = 4.0;
    public static final double MAX_RANGE = 10.0;

    private final DarthVaderEntity vader;
    private int chokeTicks;
    private int cooldown;

    public VaderChokeGoal(DarthVaderEntity vader) {
        this.vader = vader;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        LivingEntity target = vader.getTarget();
        if (target == null || !target.isAlive()) return false;
        double dist = vader.distanceTo(target);
        return dist >= MIN_RANGE && dist <= MAX_RANGE
            && vader.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = vader.getTarget();
        return chokeTicks > 0 && target != null && target.isAlive()
            && vader.distanceTo(target) <= MAX_RANGE + 2.0;
    }

    @Override
    public void start() {
        this.chokeTicks = CHOKE_DURATION_TICKS;
    }

    @Override
    public void stop() {
        this.chokeTicks = 0;
        this.cooldown = COOLDOWN_TICKS;
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        LivingEntity target = vader.getTarget();
        if (target == null) return;
        vader.getLookControl().setLookAt(target, 30, 30);
        chokeTicks--;
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 10, 0));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 2));
        if (chokeTicks % 20 == 0 && vader.level() instanceof ServerLevel sl) {
            target.hurtServer(sl, StarWarsDamageTypes.forceLightningAoe(sl), 1.0F);
        }
    }
}
```

If `MobEffects.SLOWNESS`/`LEVITATION` are named differently, `grep -rn "MobEffects\." wildwest/src/main/java | head` and use the current constants.

- [ ] **Step 2: `DarthVaderEntity.java`**

```java
package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.ai.VaderChokeGoal;
import com.tweeks.starwars.faction.SwFaction;
import com.tweeks.starwars.item.LightsaberItem;
import com.tweeks.starwars.item.SaberColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DarthVaderEntity extends SwMob implements Enemy {

    public static final double MAX_HEALTH = 120.0;
    public static final double ATTACK_DAMAGE = 12.0;
    public static final double MOVEMENT_SPEED = 0.32;
    public static final double KNOCKBACK_RESISTANCE = 0.8;

    public DarthVaderEntity(EntityType<? extends DarthVaderEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new VaderChokeGoal(this));
    }

    @Override
    public SwFaction getFaction() { return SwFaction.EMPIRE; }

    @Override
    public boolean usesBlaster() { return false; }

    @Override
    public boolean usesRifleBlaster() { return false; }

    @Override
    protected ItemStack getWeaponStack() {
        return LightsaberItem.stackWithColor(SaberColor.RED);
    }

    /**
     * Singleton lifecycle — mirrors HerobrineEntity exactly. finalizeSpawn
     * claims the singleton (and discards duplicates from spawn eggs or
     * /summon); die() and remove() both clear it, UUID-guarded so a
     * discarded duplicate can't wipe the live Vader's record. die+remove
     * redundancy is intentional: /kill-style discards skip die().
     */
    @Override
    @org.jetbrains.annotations.Nullable
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.EntitySpawnReason reason,
            @org.jetbrains.annotations.Nullable net.minecraft.world.entity.SpawnGroupData spawnData) {
        var result = super.finalizeSpawn(level, difficulty, reason, spawnData);
        var server = level.getLevel().getServer();
        if (server != null) {
            VaderSavedData saved = VaderSavedData.get(server);
            if (saved.isAlive() && !this.getUUID().equals(saved.getCurrentId())) {
                this.discard();   // another Vader already alive
                return result;
            }
            saved.setAlive(this.getUUID(), level.getLevel().dimension());
        }
        return result;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel sl) {
            VaderSavedData saved = VaderSavedData.get(sl.getServer());
            if (this.getUUID().equals(saved.getCurrentId())) {
                saved.clear();
            }
        }
    }

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        if (this.level() instanceof ServerLevel sl
                && (reason == net.minecraft.world.entity.Entity.RemovalReason.KILLED
                    || reason == net.minecraft.world.entity.Entity.RemovalReason.DISCARDED)) {
            VaderSavedData saved = VaderSavedData.get(sl.getServer());
            if (this.getUUID().equals(saved.getCurrentId())) {
                saved.clear();
            }
        }
        super.remove(reason);
    }
}
```

This lifecycle is a port of `wildwest/src/main/java/com/tweeks/wildwest/entity/HerobrineEntity.java` — open it side-by-side and match its `finalizeSpawn`/`die`/`remove` structure exactly (including the discard-before-claim ordering and any guard details this excerpt simplifies). Because `finalizeSpawn` claims the singleton, spawn eggs and `/summon` respect the one-Vader-per-world invariant too — the `NamedCharacterSpawner` (Task 18) then only needs its `isAlive()` pre-check, and its explicit `setAlive` call is a harmless re-claim of the same UUID.

- [ ] **Step 3: Registration** — `ModEntities.DARTH_VADER`, sized `0.6f, 2.0f`, tracking range 10, MONSTER; attributes; spawn placement NOT registered (Vader only spawns via the Task 18 spawner / structure anchor, never naturally).

- [ ] **Step 4: Art.** `VaderModel` accessories:

```java
PartDefinition head = root.getChild("head");
head.addOrReplaceChild("helmet_dome",
    CubeListBuilder.create().texOffs(32, 0)
        .addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, new CubeDeformation(0.7f)),
    PartPose.ZERO);
head.addOrReplaceChild("helmet_flare",   // the flared rim at the jaw
    CubeListBuilder.create().texOffs(32, 16)
        .addBox(-5.0f, -2.0f, -5.0f, 10, 2, 10),
    PartPose.ZERO);
PartDefinition body = root.getChild("body");
body.addOrReplaceChild("cape",
    CubeListBuilder.create().texOffs(44, 32)
        .addBox(-4.5f, 0.0f, 2.1f, 9, 20, 1),
    PartPose.ZERO);
body.addOrReplaceChild("chest_panel",
    CubeListBuilder.create().texOffs(56, 54)
        .addBox(-2.0f, 3.0f, -2.6f, 4, 3, 1),
    PartPose.ZERO);
```

Texture palette/paint (add `paint_darth_vader` to `gen_textures.py`): near-black base `(0x14,0x14,0x18)`, gloss highlight rows `(0x2E,0x2E,0x38)` on helmet dome top and shoulder tops, charcoal `(0x1E,0x1E,0x24)` limbs with black `(0x0A,0x0A,0x0C)` joint seams, silver-gray `(0x9A,0x9A,0xA4)` eye lenses (2×1 each at the faceplate eye rows), triangular aerator suggestion: 4×3 dark-gray `(0x50,0x50,0x58)` block at the mouth rows, chest panel region: 3 colored 1×1 buttons (red `(0xC03030)`, silver, red) on a `(0x3A,0x3A,0x42)` panel, cape region (u44..64, v32..53): flat black with `(0x1E,0x1E,0x24)` vertical fold stripes every 3px. Follow the `paint_stormtrooper` code style — explicit rects, top-highlight + bottom-shade per region.

`gen_bbmodels.py`:

```python
DARTH_VADER_ACCESSORIES = [
    ('helmet_dome',  HEAD_BONE, (-4.0, -8.0, -4.0, 8, 8, 8),  (32, 0), 0.7),
    ('helmet_flare', HEAD_BONE, (-5.0, -2.0, -5.0, 10, 2, 10),(32, 16), 0.0),
    ('cape',         BODY_BONE, (-4.5,  0.0,  2.1, 9, 20, 1), (44, 32), 0.0),
    ('chest_panel',  BODY_BONE, (-2.0,  3.0, -2.6, 4, 3, 1),  (56, 54), 0.0),
]
MOBS['darth_vader'] = HUMANOID_CUBES + DARTH_VADER_ACCESSORIES
```

Renderer: DeputyRenderer copy (texture `darth_vader.png`), shadow radius `0.6F`. ClientSetup wiring. Run both generator scripts; eyeball the PNG.

- [ ] **Step 5: Egg, loot, lang.** Egg black/red. Loot: 1 nether_star-free but meaningful — `2-4 obsidian @100%` + `30%` red-saber drop (`SetComponentsFunction` if available; otherwise drop the plain saber item — its default component renders blue, so instead add a `LootItem` note: use `LootItem.lootTableItem(...).apply(...)` with the component-setting loot function found via `grep -rn "SetComponents\|set_components" wildwest/ craftee/`; if none exists in this MC version, drop obsidian only and record it). Lang `"Darth Vader"` + egg. No biome spawns.

- [ ] **Step 6: Build, datagen, commit**

```bash
./gradlew :starwars:build :starwars:runClientData :starwars:runServerData
git add starwars/
git commit -m "feat(starwars): Darth Vader — singleton elite with force choke"
```

---

## Task 16: Luke Skywalker (named hero + leap)

**Files:**
- Create: `.../entity/LukeSkywalkerEntity.java`, `.../entity/ai/LukeLeapGoal.java`, `.../client/model/LukeModel.java`, `.../client/LukeRenderer.java`, `starwars/tools/luke_skywalker.bbmodel`, `assets/starwars/textures/entity/luke_skywalker.png`, spawn-egg assets
- Modify: same registration/datagen/tools files as Task 15

**Interfaces:**
- Consumes: `SwMob`, `LukeSavedData`, `LightsaberItem.stackWithColor(SaberColor.GREEN)`.
- Produces: `ModEntities.LUKE_SKYWALKER` (id `luke_skywalker`, `MobCategory.CREATURE`, sized 0.6×1.95).

- [ ] **Step 1: `LukeLeapGoal.java`** — gap-closer:

```java
package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.LukeSkywalkerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Force leap: when the target is 5-12 blocks away and Luke is on the
 * ground, launch toward it in a shallow arc. 100-tick cooldown.
 */
public class LukeLeapGoal extends Goal {

    public static final double MIN_RANGE = 5.0;
    public static final double MAX_RANGE = 12.0;
    public static final int COOLDOWN_TICKS = 100;
    public static final double HORIZONTAL_SPEED = 0.9;
    public static final double VERTICAL_BOOST = 0.55;

    private final LukeSkywalkerEntity luke;
    private int cooldown;

    public LukeLeapGoal(LukeSkywalkerEntity luke) {
        this.luke = luke;
        this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        LivingEntity target = luke.getTarget();
        if (target == null || !target.isAlive() || !luke.onGround()) return false;
        double dist = luke.distanceTo(target);
        return dist >= MIN_RANGE && dist <= MAX_RANGE;
    }

    @Override
    public void start() {
        LivingEntity target = luke.getTarget();
        if (target == null) return;
        Vec3 toTarget = target.position().subtract(luke.position());
        Vec3 flat = new Vec3(toTarget.x, 0, toTarget.z).normalize().scale(HORIZONTAL_SPEED);
        luke.setDeltaMovement(flat.x, VERTICAL_BOOST, flat.z);
        // No fall damage from a Force-assisted landing.
        luke.resetFallDistance();
        this.cooldown = COOLDOWN_TICKS;
    }

    @Override
    public boolean canContinueToUse() { return false; }
}
```

If `resetFallDistance()` doesn't exist, use the current field/method (`grep -rn "fallDistance" wildwest/src/main/java | head`).

- [ ] **Step 2: `LukeSkywalkerEntity.java`** — **required reading: Task 15's `DarthVaderEntity` code.** Luke is that file with these deltas: `MAX_HEALTH 100`, `ATTACK_DAMAGE 10`, `MOVEMENT_SPEED 0.34`, no knockback-resist line, faction LIGHT, green saber, `registerGoals()` adds `new LukeLeapGoal(this)` at priority 1 (instead of `VaderChokeGoal`), and every `VaderSavedData` reference becomes `LukeSavedData`. Keep the full singleton lifecycle verbatim: `setPersistenceRequired()` in the constructor, the claiming `finalizeSpawn` override (discard duplicates), and the UUID-guarded clears in both `die()` and `remove(RemovalReason)`. No `Enemy` interface (Light heroes are CREATURE-category).

- [ ] **Step 3: Registration** — `ModEntities.LUKE_SKYWALKER`, CREATURE, sized `0.6f, 1.95f`; attributes; no natural spawn placement (spawner/structure only).

- [ ] **Step 4: Art.** `LukeModel`: plain humanoid — model class with NO accessory cubes (still a distinct class + LAYER_LOCATION `luke_skywalker` for consistency and future edits). Texture `paint_luke_skywalker`: black-clad ROTJ Luke — black tunic `(0x1C,0x1C,0x20)` body/arms with `(0x30,0x30,0x36)` highlights, black trousers/boots (boots darker `(0x10,0x10,0x12)` bottom 4 rows of legs), sand-blond hair `(0xC8,0xA8,0x60)` head top + hairline, skin face `(0xE8,0xC0,0x98)`, blue eyes, tan glove stripe on the right arm wrist rows `(0x9A,0x84,0x60)`. bbmodel: `MOBS['luke_skywalker'] = HUMANOID_CUBES` (no accessories). Renderer copy; ClientSetup; run scripts; eyeball.

- [ ] **Step 5: Egg (black/blond), loot (`1-2 gold_ingot @60%`, `30%` green saber — same component caveat as Vader), lang `"Luke Skywalker"`, no biome spawns.**

- [ ] **Step 6: Build, datagen, commit**

```bash
./gradlew :starwars:build :starwars:runClientData :starwars:runServerData
git add starwars/
git commit -m "feat(starwars): Luke Skywalker — singleton hero with force leap"
```

---

## Task 17: Obi-Wan Kenobi (named hero + repulse)

**Files:**
- Create: `.../entity/ObiWanEntity.java`, `.../entity/ai/ObiWanPushGoal.java`, `.../client/model/ObiWanModel.java`, `.../client/ObiWanRenderer.java`, `starwars/tools/obi_wan.bbmodel`, `assets/starwars/textures/entity/obi_wan.png`, spawn-egg assets
- Modify: same registration/datagen/tools files as Task 15

**Interfaces:**
- Consumes: `SwMob`, `ObiWanSavedData`, `LightsaberItem.stackWithColor(SaberColor.BLUE)`.
- Produces: `ModEntities.OBI_WAN` (id `obi_wan`, CREATURE, sized 0.6×1.95).

- [ ] **Step 1: `ObiWanPushGoal.java`** — defensive repulse:

```java
package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.ObiWanEntity;
import com.tweeks.starwars.faction.SwCombatant;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Defensive Force push: when 2+ enemies crowd within 4 blocks, repulse
 * every enemy within 5 blocks (0.8 horizontal, 0.3 vertical). 160-tick
 * cooldown.
 */
public class ObiWanPushGoal extends Goal {

    public static final int CROWD_THRESHOLD = 2;
    public static final double CROWD_RADIUS = 4.0;
    public static final double PUSH_RADIUS = 5.0;
    public static final double PUSH_STRENGTH = 0.8;
    public static final double PUSH_LIFT = 0.3;
    public static final int COOLDOWN_TICKS = 160;

    private final ObiWanEntity obiWan;
    private int cooldown;

    public ObiWanPushGoal(ObiWanEntity obiWan) {
        this.obiWan = obiWan;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    private List<LivingEntity> enemiesWithin(double radius) {
        return obiWan.level().getEntitiesOfClass(
            LivingEntity.class,
            obiWan.getBoundingBox().inflate(radius),
            e -> e != obiWan && e.isAlive()
                && e instanceof SwCombatant c
                && c.getFaction() == SwFaction.EMPIRE);
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        return enemiesWithin(CROWD_RADIUS).size() >= CROWD_THRESHOLD;
    }

    @Override
    public void start() {
        for (LivingEntity enemy : enemiesWithin(PUSH_RADIUS)) {
            Vec3 away = enemy.position().subtract(obiWan.position());
            Vec3 flat = new Vec3(away.x, 0, away.z).normalize().scale(PUSH_STRENGTH);
            enemy.setDeltaMovement(enemy.getDeltaMovement().add(flat.x, PUSH_LIFT, flat.z));
            enemy.hurtMarked = true;   // force velocity sync to clients
        }
        this.cooldown = COOLDOWN_TICKS;
    }

    @Override
    public boolean canContinueToUse() { return false; }
}
```

If `hurtMarked` is not accessible/renamed, find how wildwest forces velocity sync (`grep -rn "hurtMarked\|setDeltaMovement" wildwest/src/main/java/com/tweeks/wildwest/entity/ai/ | head`) and mirror it.

- [ ] **Step 2: `ObiWanEntity.java`** — **required reading: Task 15's `DarthVaderEntity` code.** Same file with these deltas: `MAX_HEALTH 100`, `ATTACK_DAMAGE 9`, `MOVEMENT_SPEED 0.32`, faction LIGHT, blue saber, goal `new ObiWanPushGoal(this)` at priority 1, every `VaderSavedData` reference becomes `ObiWanSavedData`, CREATURE category, no `Enemy`. Keep the full singleton lifecycle verbatim (constructor persistence, claiming `finalizeSpawn`, UUID-guarded `die()` + `remove()` clears).

- [ ] **Step 3: Registration** — `ModEntities.OBI_WAN`, attributes, no natural placement.

- [ ] **Step 4: Art.** `ObiWanModel`: reuse the Jedi-knight accessory geometry exactly (robe_skirt + hood, same UVs — copy the two `addOrReplaceChild` blocks from `JediKnightModel.createBodyLayer`, LAYER_LOCATION `obi_wan`). Texture `paint_obi_wan`: Jedi-knight paint with these swaps — auburn-gray hair `(0x8A,0x6A,0x4A)`, full beard rows on the lower face `(0x7A,0x5E,0x42)` (face rows 13..16), lighter cream tunic `(0xE2,0xD8,0xC0)`, robe slightly grayer `(0x74,0x5C,0x42)`. bbmodel: `MOBS['obi_wan'] = HUMANOID_CUBES + JEDI_KNIGHT_ACCESSORIES` (reuse the same accessory list). Renderer/ClientSetup; run scripts; eyeball.

- [ ] **Step 5: Egg (cream/gray), loot (`1-2 emerald @60%`, `30%` blue saber — component caveat), lang `"Obi-Wan Kenobi"`, no biome spawns.**

- [ ] **Step 6: Build, datagen, commit**

```bash
./gradlew :starwars:build :starwars:runClientData :starwars:runServerData
git add starwars/
git commit -m "feat(starwars): Obi-Wan Kenobi — singleton hero with force repulse"
```

---

## Task 18: Named-Character Spawner

**Files:**
- Create: `.../spawning/NamedCharacterSpawner.java`
- Modify: `.../StarWarsMod.java` (if the mechanism needs explicit registration)

**Interfaces:**
- Consumes: `VaderSavedData`/`LukeSavedData`/`ObiWanSavedData` (Task 13), `ModEntities.DARTH_VADER`/`LUKE_SKYWALKER`/`OBI_WAN`/`STORMTROOPER`.
- Produces: server-tick spawner that keeps at most one of each named character alive per world. Task 31 adds Boba Fett to its roster table; Tasks 28-29 tighten spawn anchors to structures.

- [ ] **Step 1: Study the repo's spawner mechanism.** Read `wildwest/src/main/java/com/tweeks/wildwest/spawning/LeaderEntourageSpawner.java` and `LawmanVillageSpawner.java` end-to-end. Reuse their exact registration mechanism (event-bus subscription or custom-spawner registration — whatever they do) and their spawn-position validation helpers.

- [ ] **Step 2: Implement `NamedCharacterSpawner`** with this behavior (adapting the wildwest mechanism):
  - Every 1200 ticks (1 minute) per server level, roll each named character independently.
  - Skip a character if its SavedData `isAlive()`.
  - Chance per roll: 15%. Pick a random online player in the level; find a valid ground position 24–40 blocks away (reuse the wildwest position-picking helper pattern: heightmap surface, not in fluid, light-valid for the mob's category).
  - Biome gate: Vader — DESERT/BADLANDS/PLAINS; Luke and Obi-Wan — FOREST/JUNGLE/TAIGA/PLAINS. (Once structures exist, Tasks 25-26 re-anchor: prefer positions near the located structure.)
  - Spawn the entity via `EntityType.spawn(...)` / `create + addFreshEntity` (mirror wildwest), call `<X>SavedData.get(server).setAlive(entity.getUUID(), level.dimension())`.
  - Vader additionally spawns 3 stormtroopers in a 4-block ring around him (same ground validation per trooper; skip troopers whose spot fails).

```java
// Core roll, shared by all three characters:
private static void tryRollCharacter(ServerLevel level,
                                     NamedCharacterSavedData data,
                                     EntityType<? extends SwMob> type,
                                     Set<ResourceKey<Biome>> biomes,
                                     boolean withTrooperEscort) {
    if (data.isAlive()) return;
    if (level.random.nextFloat() >= 0.15f) return;
    // ...player pick + position pick (wildwest helper pattern)...
    // ...biome check via level.getBiome(pos).is(biomeKey) over the set...
    // ...spawn, then:
    data.setAlive(mob.getUUID(), level.dimension());
    if (withTrooperEscort) { /* 3x STORMTROOPER ring spawn */ }
}
```

Fill in the elided parts with the concrete wildwest helper code you read in Step 1 — copy, don't reinvent.

- [ ] **Step 3: Build + commit**

```bash
./gradlew :starwars:build
git add starwars/
git commit -m "feat(starwars): singleton-gated named character spawner"
```

---

## Task 19: Milestone 3 Bedrock Pass

Same procedure as Task 11:

- [ ] **Step 1:** `./gradlew :translator:translate --args="starwars"`
- [ ] **Step 2:** Read `bedrock-out/starwars/UNTRANSLATABLE.md`; expected new entries: saber color component/model-select, choke/leap/push AI goals (LLM-stub or deferred), singleton SavedData. No surprises unexplained.
- [ ] **Step 3:** `git add bedrock-out/starwars/ && git commit -m "feat(starwars): milestone-3 bedrock translation"`

---

# Milestone 4 — Stormtrooper Armor + Disguise

## Task 20: Stormtrooper Armor Set

**Files:**
- Create: `.../item/StormtrooperArmorMaterials.java`, `assets/starwars/equipment/stormtrooper.json`, `assets/starwars/textures/entity/equipment/humanoid/stormtrooper.png`, `assets/starwars/textures/entity/equipment/humanoid_leggings/stormtrooper.png`, item textures `stormtrooper_{helmet,chestplate,leggings,boots}.png`, `items/stormtrooper_*.json`, `models/item/stormtrooper_*.json`
- Modify: `.../Registration.java`, `.../data/ModLanguageProvider.java`, `.../data/ModRecipeProvider.java` (create), `.../data/DataGenerators.java`, `.../data/ModEntityLootProvider.java`, `starwars/tools/gen_item_textures.py`, `starwars/tools/gen_textures.py`

**Interfaces:**
- Consumes: `Registration.ITEMS` (Task 1).
- Produces: `StormtrooperArmorMaterials.STORMTROOPER` (`ArmorMaterial`) + `STORMTROOPER_ASSET`; `Registration.STORMTROOPER_HELMET/CHESTPLATE/LEGGINGS/BOOTS` — Task 21's `Disguise` reads these.

- [ ] **Step 1: `StormtrooperArmorMaterials.java`** — the craftee pattern with iron-tier stats:

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

public final class StormtrooperArmorMaterials {
    private StormtrooperArmorMaterials() {}

    public static final ResourceKey<EquipmentAsset> STORMTROOPER_ASSET =
        ResourceKey.create(EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "stormtrooper"));

    /** Iron-tier defense: boots 2, legs 5, chest 6, helmet 2, body 5. */
    private static final Map<ArmorType, Integer> DEFENSE = Map.of(
        ArmorType.BOOTS,      2,
        ArmorType.LEGGINGS,   5,
        ArmorType.CHESTPLATE, 6,
        ArmorType.HELMET,     2,
        ArmorType.BODY,       5);

    public static final ArmorMaterial STORMTROOPER = new ArmorMaterial(
        15,
        DEFENSE,
        9,
        SoundEvents.ARMOR_EQUIP_IRON,
        0.0F,
        0.0F,
        ItemTags.REPAIRS_IRON_ARMOR,
        STORMTROOPER_ASSET);
}
```

(Constructor arg order verified against `craftee/src/main/java/com/tweeks/craftee/item/CrafteeArmorMaterials.java` — keep identical ordering.)

- [ ] **Step 2: Register the four items** (craftee `Registration` pattern):

```java
public static final DeferredItem<Item> STORMTROOPER_HELMET = ITEMS.registerItem("stormtrooper_helmet",
    Item::new,
    p -> p.humanoidArmor(com.tweeks.starwars.item.StormtrooperArmorMaterials.STORMTROOPER,
            net.minecraft.world.item.equipment.ArmorType.HELMET)
          .stacksTo(1));
// ...CHESTPLATE / LEGGINGS / BOOTS identically with their ArmorType...
```

Add all four to the creative tab.

- [ ] **Step 3: Equipment asset + worn textures.** `assets/starwars/equipment/stormtrooper.json`:

```json
{
  "layers": {
    "humanoid": [
      { "texture": "starwars:stormtrooper" }
    ],
    "humanoid_leggings": [
      { "texture": "starwars:stormtrooper" }
    ]
  }
}
```

Worn-layer textures: check where craftee puts the PNGs the asset references (`find craftee/src/main/resources -name "*.png" | grep -i equip`) and mirror those exact paths for `starwars:stormtrooper`. Extend `gen_textures.py` with `paint_stormtrooper_armor_layers(...)` painting the two standard 64×32 armor-layer sheets: white plastoid plates with the same `WHITE/WHITE_SH/SUIT_BLACK` palette + seam lines as `paint_stormtrooper` (layer 1: helmet/chest/boots regions; layer 2: leggings region — copy the region map from the craftee layer PNGs' visible extents).

- [ ] **Step 4: Item textures + models.** Four 16×16 icons in `gen_item_textures.py` — white piece silhouettes with black seams (helmet: dome + black eye slits; chest: torso plate + ab seam; leggings: two columns; boots: two low blocks), each with 1px darker bottom/right edge shading. `items/*.json` + `models/item/*.json` pairs, `"parent": "minecraft:item/generated"`, `layer0` the icon.

- [ ] **Step 5: Recipes.** Create `ModRecipeProvider` mirroring `wildwest/src/main/java/com/tweeks/wildwest/data/ModRecipeProvider.java`'s class shell (incl. its `.Runner` inner class registration in `DataGenerators.gatherDataServer`). Four shaped recipes in vanilla armor shapes, ingredient: 1 iron ingot + white wool? No — keep it armor-priced: **iron ingots + white dye**: pattern like vanilla helmet from `III / I I` where `I` = iron ingot, plus 1 white dye centered where the shape allows; if mixing dye into the shape is awkward, use shapeless-over-shaped: shaped vanilla-iron-armor pattern with `quartz` (`Items.QUARTZ`) as the material (white, thematic, mid-cost). Use quartz. Example (helmet):

```java
ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, Registration.STORMTROOPER_HELMET.get())
    .pattern("QQQ")
    .pattern("Q Q")
    .define('Q', Items.QUARTZ)
    .unlockedBy("has_quartz", has(Items.QUARTZ))
    .save(output);
```

(lift the exact builder/method names — `shaped`, `has`, `save`, category enum — from wildwest's `ModRecipeProvider`; they shift across versions.)

Also add the blaster recipes here (the spec's astromech "parts" — iron nuggets + redstone — feed these):

```java
ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, Registration.BLASTER_PISTOL.get())
    .pattern("II")
    .pattern("RN")
    .define('I', Items.IRON_INGOT)
    .define('R', Items.REDSTONE)
    .define('N', Items.IRON_NUGGET)
    .unlockedBy("has_redstone", has(Items.REDSTONE))
    .save(output);

ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, Registration.BLASTER_RIFLE.get())
    .pattern("III")
    .pattern(" RN")
    .define('I', Items.IRON_INGOT)
    .define('R', Items.REDSTONE)
    .define('N', Items.IRON_NUGGET)
    .unlockedBy("has_redstone", has(Items.REDSTONE))
    .save(output);
```

- [ ] **Step 5b: Armor bbmodels.** The spec commits a bbmodel for the armor set, and the repo convention exists: `craftee/tools/craftee_armor_{helmet,chestplate,leggings,boots}.bbmodel`. Open `craftee_armor_helmet.bbmodel` as the structural template, then extend `starwars/tools/gen_bbmodels.py` with four armor entries producing `stormtrooper_armor_{helmet,chestplate,leggings,boots}.bbmodel` — standard armor-overlay geometry (helmet: head box inflate 1.0; chestplate: body box inflate 1.01 + both arm boxes inflate 1.0; leggings: body inflate 0.51 + both leg boxes inflate 0.5; boots: both leg boxes inflate 1.0), embedding the Step 3 armor-layer texture. Match whatever structural details the craftee bbmodels carry that the mob generator doesn't (check the JSON for texture-size or format differences). Run the generator and commit the four bbmodels.

- [ ] **Step 6: Loot integration.** In `ModEntityLootProvider`, add a third pool to the stormtrooper table: 10% chance, equal-weighted one-of-four armor pieces:

```java
.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
    .when(LootItemRandomChanceCondition.randomChance(0.10f))
    .add(LootItem.lootTableItem(Registration.STORMTROOPER_HELMET.get()))
    .add(LootItem.lootTableItem(Registration.STORMTROOPER_CHESTPLATE.get()))
    .add(LootItem.lootTableItem(Registration.STORMTROOPER_LEGGINGS.get()))
    .add(LootItem.lootTableItem(Registration.STORMTROOPER_BOOTS.get())))
```

- [ ] **Step 7: Lang** (4 item names + nothing else), **build, datagen, run generators, commit**

```bash
./gradlew :starwars:build :starwars:runClientData :starwars:runServerData
python3 starwars/tools/gen_item_textures.py starwars/src/main/resources/assets/starwars/textures/item/
python3 starwars/tools/gen_textures.py
git add starwars/
git commit -m "feat(starwars): stormtrooper armor set — items, worn layers, recipes, drops"
```

---

## Task 21: Disguise Set Bonus

**Files:**
- Modify: `.../faction/Disguise.java`
- Test: (already covered — `TargetPredicatesTest.stormtrooperDisguiseHidesPlayerFromEmpireOnly` exercises the predicate path; the slot check itself is MC-coupled)

**Interfaces:**
- Consumes: `Registration.STORMTROOPER_*` (Task 20).
- Produces: working `Disguise.isWearingFullStormtrooperSet`.

- [ ] **Step 1: Replace the stub body** (craftee `SetBonusHandler.isWearingFullSet` shape):

```java
public static boolean isWearingFullStormtrooperSet(LivingEntity entity) {
    return entity.getItemBySlot(EquipmentSlot.HEAD).is(Registration.STORMTROOPER_HELMET.get())
        && entity.getItemBySlot(EquipmentSlot.CHEST).is(Registration.STORMTROOPER_CHESTPLATE.get())
        && entity.getItemBySlot(EquipmentSlot.LEGS).is(Registration.STORMTROOPER_LEGGINGS.get())
        && entity.getItemBySlot(EquipmentSlot.FEET).is(Registration.STORMTROOPER_BOOTS.get());
}
```

(add imports `EquipmentSlot`, `Registration`). Behavioral note to preserve: disguise blocks new target ACQUISITION only — a mob already fighting the player keeps its target (`HurtByTargetGoal` unaffected). That is the spec's intent.

- [ ] **Step 2: Build, test-sweep, commit**

```bash
./gradlew :starwars:build :starwars:test
git add starwars/
git commit -m "feat(starwars): full stormtrooper set disguises player from Empire targeting"
```

---

## Task 22: Milestone 4 Bedrock Pass

Same procedure as Task 11: run translator, read `UNTRANSLATABLE.md` (expected additions: armor set-bonus logic), commit `bedrock-out/starwars/` as `"feat(starwars): milestone-4 bedrock translation"`.

---

# Milestone 5 — Force Powers

## Task 23: ForcePower Enum + Cooldowns + Components + Tests

**Files:**
- Create: `.../item/ForcePower.java`, `.../item/ForceCooldowns.java`, `.../faction/PacifyState.java`
- Modify: `.../item/ModDataComponents.java`
- Test: `starwars/src/test/java/com/tweeks/starwars/item/ForceCooldownsTest.java`

**Interfaces:**
- Consumes: `Alignment.POWER_DELTA` convention (Task 3).
- Produces: `ForcePower { PUSH, PULL, LEAP, MIND_TRICK, LIGHTNING }` with `int cooldownTicks()`, `int alignmentDelta()`, `String translationKey()`, `static ForcePower byIndex(int)` (out-of-range → PUSH); `ForceCooldowns` (`SLOT_COUNT = 5`, `isOnCooldown`, `getExpiry`, `applyCooldown`, `emptyCooldowns`); `ModDataComponents.ACTIVE_POWER` (Integer), `POWER_COOLDOWNS` (`List<Long>`); `PacifyState.isActive(long untilTick, long nowTick)`.

- [ ] **Step 1: Write the failing test**

```java
package com.tweeks.starwars.item;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ForceCooldownsTest {

    @Test
    void powerRoster_matchesSpec() {
        assertEquals(5, ForcePower.values().length);
        assertEquals(60, ForcePower.PUSH.cooldownTicks());
        assertEquals(60, ForcePower.PULL.cooldownTicks());
        assertEquals(120, ForcePower.LEAP.cooldownTicks());
        assertEquals(300, ForcePower.MIND_TRICK.cooldownTicks());
        assertEquals(200, ForcePower.LIGHTNING.cooldownTicks());
        // Alignment pull: light powers +2, dark -2, neutral 0.
        assertEquals(0, ForcePower.PUSH.alignmentDelta());
        assertEquals(0, ForcePower.PULL.alignmentDelta());
        assertEquals(2, ForcePower.LEAP.alignmentDelta());
        assertEquals(2, ForcePower.MIND_TRICK.alignmentDelta());
        assertEquals(-2, ForcePower.LIGHTNING.alignmentDelta());
    }

    @Test
    void byIndex_clampsToPush() {
        assertEquals(ForcePower.PUSH, ForcePower.byIndex(-1));
        assertEquals(ForcePower.PUSH, ForcePower.byIndex(99));
        assertEquals(ForcePower.LIGHTNING, ForcePower.byIndex(4));
    }

    @Test
    void cooldowns_applyAndExpire() {
        List<Long> cds = ForceCooldowns.emptyCooldowns();
        assertFalse(ForceCooldowns.isOnCooldown(cds, 0, 100L));
        cds = ForceCooldowns.applyCooldown(cds, 0, 100L, 60);
        assertTrue(ForceCooldowns.isOnCooldown(cds, 0, 100L));
        assertTrue(ForceCooldowns.isOnCooldown(cds, 0, 159L));
        assertFalse(ForceCooldowns.isOnCooldown(cds, 0, 160L));
        assertEquals(160L, ForceCooldowns.getExpiry(cds, 0));
        // Other slots untouched.
        assertFalse(ForceCooldowns.isOnCooldown(cds, 1, 100L));
    }

    @Test
    void cooldowns_nullAndShortListSafe() {
        assertFalse(ForceCooldowns.isOnCooldown(null, 0, 0L));
        assertFalse(ForceCooldowns.isOnCooldown(List.of(), 4, 0L));
        List<Long> padded = ForceCooldowns.applyCooldown(List.of(5L), 3, 10L, 20);
        assertEquals(5, padded.size());
        assertEquals(5L, padded.get(0));
        assertEquals(30L, padded.get(3));
    }
}
```

Also add to a new small test or the same file:

```java
@Test
void pacify_isActiveUntilExpiry() {
    assertTrue(com.tweeks.starwars.faction.PacifyState.isActive(200L, 199L));
    assertFalse(com.tweeks.starwars.faction.PacifyState.isActive(200L, 200L));
    assertFalse(com.tweeks.starwars.faction.PacifyState.isActive(0L, 5L));
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :starwars:test --tests ForceCooldownsTest
```
Expected: FAIL.

- [ ] **Step 3: Implement.** `ForcePower`:

```java
package com.tweeks.starwars.item;

import com.tweeks.starwars.faction.Alignment;

public enum ForcePower {
    PUSH(60, 0),
    PULL(60, 0),
    LEAP(120, Alignment.POWER_DELTA),
    MIND_TRICK(300, Alignment.POWER_DELTA),
    LIGHTNING(200, -Alignment.POWER_DELTA);

    private final int cooldownTicks;
    private final int alignmentDelta;

    ForcePower(int cooldownTicks, int alignmentDelta) {
        this.cooldownTicks = cooldownTicks;
        this.alignmentDelta = alignmentDelta;
    }

    public int cooldownTicks() { return cooldownTicks; }
    public int alignmentDelta() { return alignmentDelta; }
    public String translationKey() {
        return "force_power.starwars." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static ForcePower byIndex(int index) {
        return (index < 0 || index >= values().length) ? PUSH : values()[index];
    }
}
```

`ForceCooldowns`: byte-for-byte port of `wildwest/.../item/InfinityCooldowns.java` with `SLOT_COUNT = 5` and `emptyCooldowns()` returning `List.of(0L, 0L, 0L, 0L, 0L)` (keep the `List<Long>`-not-`long[]` doc comment — same NeoForge value-equality constraint).

`PacifyState`:

```java
package com.tweeks.starwars.faction;

/** Pure mind-trick pacification check: active strictly before the expiry tick. */
public final class PacifyState {
    private PacifyState() {}

    public static boolean isActive(long untilTick, long nowTick) {
        return nowTick < untilTick;
    }
}
```

`ModDataComponents` additions (same builder shapes as `BLADE_COLOR` / wildwest `COOLDOWNS`):

```java
/** Active ForcePower index 0..4. Defaults to 0 (PUSH). */
public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ACTIVE_POWER =
    COMPONENTS.registerComponentType(
        "active_power",
        builder -> builder
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));

/** Per-power cooldown expiry gameTime ticks, List<Long> length 5. */
public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<Long>>> POWER_COOLDOWNS =
    COMPONENTS.registerComponentType(
        "power_cooldowns",
        builder -> builder
            .persistent(Codec.LONG.listOf())
            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG.apply(
                net.minecraft.network.codec.ByteBufCodecs.list(ForceCooldowns.SLOT_COUNT))));
```

- [ ] **Step 4: Run tests, build, commit**

```bash
./gradlew :starwars:test --tests ForceCooldownsTest && ./gradlew :starwars:build
git add starwars/
git commit -m "feat(starwars): force power enum, cooldown helpers, data components"
```

---

## Task 24: Holocron Item + Power Implementations

**Files:**
- Create: `.../item/HolocronItem.java`, `.../item/ForcePowers.java`, `.../faction/PacifyAttachment.java`, holocron item assets (`items/holocron.json`, `models/item/holocron.json`, `textures/item/holocron.png`), `starwars/tools/holocron.bbmodel`
- Modify: `.../Registration.java`, `.../faction/ModAttachments.java`, `.../entity/ai/SwTargetGoal.java`, `.../ModSounds.java`, `sounds.json`, `.../data/ModLanguageProvider.java`, `.../data/ModEntityLootProvider.java` (named-character holocron drops), `starwars/tools/gen_weapon_models.py`

**Interfaces:**
- Consumes: `ForcePower`, `ForceCooldowns`, `ModDataComponents.ACTIVE_POWER`/`POWER_COOLDOWNS` (Task 23), `AlignmentEvents.adjustScore` (Task 4), `StarWarsDamageTypes.forceLightning` (Task 2).
- Produces: `Registration.HOLOCRON`; `HolocronItem.use` casting the active power; `ForcePowers.cast(ForcePower, ServerPlayer, ServerLevel) → boolean`; `PacifyAttachment(long until)` + `ModAttachments.PACIFIED`; `ModSounds.FORCE_CAST`, `FORCE_LIGHTNING_SOUND` — the Task 25 picker reads the same components.

- [ ] **Step 1: `PacifyAttachment` + registration.**

```java
package com.tweeks.starwars.faction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PacifyAttachment(long until) {
    public static final Codec<PacifyAttachment> CODEC =
        RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("Until").forGetter(PacifyAttachment::until)
        ).apply(instance, PacifyAttachment::new));
}
```

In `ModAttachments`, add `PACIFIED` with the same null-default + serialize-predicate pattern as `ALIGNMENT`.

- [ ] **Step 2: Gate `SwTargetGoal` on pacification** — add to the class:

```java
@Override
public boolean canUse() {
    if (this.mob.hasData(com.tweeks.starwars.faction.ModAttachments.PACIFIED.get())) {
        var p = this.mob.getData(com.tweeks.starwars.faction.ModAttachments.PACIFIED.get());
        if (p != null && com.tweeks.starwars.faction.PacifyState.isActive(
                p.until(), this.mob.level().getGameTime())) {
            return false;
        }
    }
    return super.canUse();
}
```

(`this.mob` is the protected field inherited from the vanilla goal; if it's named differently, store the `SwMob` in a constructor field.)

- [ ] **Step 3: `ForcePowers.java`** — all five casts, server-side:

```java
package com.tweeks.starwars.item;

import com.tweeks.starwars.StarWarsDamageTypes;
import com.tweeks.starwars.entity.SwMob;
import com.tweeks.starwars.faction.ModAttachments;
import com.tweeks.starwars.faction.PacifyAttachment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class ForcePowers {
    private ForcePowers() {}

    public static final double PUSH_RADIUS = 6.0;
    public static final double PUSH_STRENGTH = 1.2;
    public static final double PUSH_LIFT = 0.4;
    public static final double PULL_RANGE = 10.0;
    public static final double PULL_STRENGTH = 0.9;
    public static final double LEAP_HORIZONTAL = 1.2;
    public static final double LEAP_VERTICAL = 0.9;
    public static final double MIND_TRICK_RADIUS = 8.0;
    public static final int MIND_TRICK_DURATION_TICKS = 200;
    public static final double LIGHTNING_RADIUS = 8.0;
    public static final int LIGHTNING_MAX_TARGETS = 3;
    public static final float LIGHTNING_DAMAGE = 6.0F;

    /** Returns true if the cast did anything (misses still count for push/leap). */
    public static boolean cast(ForcePower power, ServerPlayer player, ServerLevel level) {
        return switch (power) {
            case PUSH -> push(player, level);
            case PULL -> pull(player, level);
            case LEAP -> leap(player);
            case MIND_TRICK -> mindTrick(player, level);
            case LIGHTNING -> lightning(player, level);
        };
    }

    private static List<LivingEntity> livingNear(ServerPlayer player, ServerLevel level, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class,
            player.getBoundingBox().inflate(radius),
            e -> e != player && e.isAlive());
    }

    private static boolean push(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getViewVector(1.0F);
        for (LivingEntity e : livingNear(player, level, PUSH_RADIUS)) {
            Vec3 to = e.position().subtract(player.position());
            if (to.lengthSqr() < 1.0e-4) continue;
            if (to.normalize().dot(look) < 0.5) continue;   // 60-degree cone
            Vec3 flat = new Vec3(to.x, 0, to.z).normalize().scale(PUSH_STRENGTH);
            e.setDeltaMovement(e.getDeltaMovement().add(flat.x, PUSH_LIFT, flat.z));
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM,
            player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
        return true;
    }

    private static boolean pull(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getViewVector(1.0F);
        LivingEntity best = livingNear(player, level, PULL_RANGE).stream()
            .filter(e -> e.position().subtract(player.position()).normalize().dot(look) > 0.7)
            .min(Comparator.comparingDouble(player::distanceToSqr))
            .orElse(null);
        if (best == null) return false;
        Vec3 toPlayer = player.position().subtract(best.position()).normalize()
            .scale(PULL_STRENGTH);
        best.setDeltaMovement(toPlayer.x, toPlayer.y * 0.5 + 0.3, toPlayer.z);
        best.hurtMarked = true;
        return true;
    }

    private static boolean leap(ServerPlayer player) {
        Vec3 look = player.getViewVector(1.0F);
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize().scale(LEAP_HORIZONTAL);
        player.setDeltaMovement(flat.x, LEAP_VERTICAL, flat.z);
        player.hurtMarked = true;
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0));
        return true;
    }

    private static boolean mindTrick(ServerPlayer player, ServerLevel level) {
        long until = level.getGameTime() + MIND_TRICK_DURATION_TICKS;
        boolean any = false;
        for (LivingEntity e : livingNear(player, level, MIND_TRICK_RADIUS)) {
            if (!(e instanceof Mob mob)) continue;
            if (mob.getTarget() != player) {
                // Only pacify things currently hostile to the caster, plus all SwMobs.
                if (!(mob instanceof SwMob)) continue;
            }
            mob.setTarget(null);
            mob.setData(ModAttachments.PACIFIED.get(), new PacifyAttachment(until));
            any = true;
        }
        return any;
    }

    private static boolean lightning(ServerPlayer player, ServerLevel level) {
        // Deliberately indiscriminate: nearest 3 living entities, no
        // line-of-sight or hostility filter — dark-side power, chains onto
        // pets/villagers through walls by design (matches the spec's letter).
        List<LivingEntity> targets = livingNear(player, level, LIGHTNING_RADIUS).stream()
            .sorted(Comparator.comparingDouble(player::distanceToSqr))
            .limit(LIGHTNING_MAX_TARGETS)
            .toList();
        if (targets.isEmpty()) return false;
        for (LivingEntity e : targets) {
            e.hurtServer(level, StarWarsDamageTypes.forceLightning(player), LIGHTNING_DAMAGE);
            Vec3 mid = player.getEyePosition().add(e.position().add(0, e.getBbHeight() * 0.5, 0))
                .scale(0.5);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                mid.x, mid.y, mid.z, 20, 0.3, 0.3, 0.3, 0.05);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 15, 0.2, 0.4, 0.2, 0.05);
        }
        return true;
    }
}
```

(Pacify note: vanilla mobs already targeting the caster get their target cleared but may re-acquire — full pacify only holds for `SwMob`s, whose `SwTargetGoal` honors the attachment. Record this limitation in the code comment and UNTRANSLATABLE-style honesty in the task summary.)

- [ ] **Step 4: `HolocronItem.java`** — the `InfinityGauntletItem.use` cooldown pattern:

```java
package com.tweeks.starwars.item;

import com.tweeks.starwars.ModSounds;
import com.tweeks.starwars.faction.AlignmentEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class HolocronItem extends Item {

    public HolocronItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            // CONSUME on the client (arm swing + immediate feedback), matching
            // the InfinityGauntletItem reference.
            return InteractionResult.CONSUME;
        }
        ForcePower power = ForcePower.byIndex(
            stack.getOrDefault(ModDataComponents.ACTIVE_POWER.get(), 0));
        List<Long> cds = stack.getOrDefault(
            ModDataComponents.POWER_COOLDOWNS.get(), ForceCooldowns.emptyCooldowns());
        long now = level.getGameTime();
        if (ForceCooldowns.isOnCooldown(cds, power.ordinal(), now)) {
            return InteractionResult.FAIL;
        }
        if (!ForcePowers.cast(power, serverPlayer, serverLevel)) {
            return InteractionResult.FAIL;   // no valid target: no cooldown burned
        }
        stack.set(ModDataComponents.POWER_COOLDOWNS.get(),
            ForceCooldowns.applyCooldown(cds, power.ordinal(), now, power.cooldownTicks()));
        // Mirror into the vanilla hotbar sweep for visual feedback.
        player.getCooldowns().addCooldown(stack, power.cooldownTicks());
        AlignmentEvents.adjustScore(serverPlayer, power.alignmentDelta());
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            (power == ForcePower.LIGHTNING ? ModSounds.FORCE_LIGHTNING_SOUND : ModSounds.FORCE_CAST).get(),
            SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
    }
}
```

Register `Registration.HOLOCRON` (`HolocronItem::new`, `p -> p`), add to creative tab.

- [ ] **Step 5: Sounds, lang, art, loot.**
  - `ModSounds`: `FORCE_CAST = register("force_cast")`, `FORCE_LIGHTNING_SOUND = register("force_lightning")`. `sounds.json`: `force_cast` → `minecraft:entity.illusioner.cast_spell` (pitch 1.3), `force_lightning` → `minecraft:entity.guardian.attack` (pitch 0.8), with subtitles.
  - Lang: `"Kyber Holocron"`, 5 power names via `add("force_power.starwars.push", "Force Push")` etc., 2 subtitles.
  - Art: extend `gen_weapon_models.py` with a `HOLOCRON_CUBES` voxel (a 6×6×6 centered cube `(5,5,5)→(11,11,11)` 'core' + 4 corner studs 1×1×1 at the top face corners, texture: deep blue `(0x20,0x38,0x80)` faces with cyan `(0x60,0xC8,0xE0)` edge lines + glowing white 2×2 center) emitting `models/item/holocron.json` + `tools/holocron.bbmodel` + `textures/item/holocron.png`; `items/holocron.json` plain model selector.
  - Loot: in `ModEntityLootProvider`, add to Vader/Luke/Obi-Wan tables a `25%` holocron pool (`LootItem.lootTableItem(Registration.HOLOCRON.get())` behind `LootItemRandomChanceCondition.randomChance(0.25f)`).

- [ ] **Step 6: Build, datagen, generators, commit**

```bash
./gradlew :starwars:build :starwars:test :starwars:runClientData :starwars:runServerData
python3 starwars/tools/gen_weapon_models.py
git add starwars/
git commit -m "feat(starwars): kyber holocron + five force powers"
```

---

## Task 25: Force Radial Picker (client + network)

**Files:**
- Create: `.../client/SwRadialMath.java`, `.../client/ForcePickerScreen.java`, `.../client/HolocronKeybind.java`, `.../network/C2SSelectPowerPacket.java`
- Modify: `.../network/NetworkHandlers.java`
- Test: `starwars/src/test/java/com/tweeks/starwars/client/SwRadialMathTest.java`

**Interfaces:**
- Consumes: `ForcePower`, `ForceCooldowns`, `ModDataComponents` (Task 23-24), `Registration.HOLOCRON`.
- Produces: `SwRadialMath.wedgeFromMouse(mouseX, mouseY, centerX, centerY, deadzonePx, wedgeCount) → int` (-1 in deadzone); `C2SSelectPowerPacket(int powerIndex, boolean mainHand)`.

- [ ] **Step 1: Write the failing test**

```java
package com.tweeks.starwars.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwRadialMathTest {

    private static final double CX = 100, CY = 100, DEAD = 18;

    @Test
    void deadzone_returnsMinusOne() {
        assertEquals(-1, SwRadialMath.wedgeFromMouse(105, 105, CX, CY, DEAD, 5));
    }

    @Test
    void straightUp_isWedgeZero() {
        assertEquals(0, SwRadialMath.wedgeFromMouse(100, 40, CX, CY, DEAD, 5));
    }

    @Test
    void clockwiseProgression_fiveWedges() {
        // 72-degree wedges, wedge 0 centered at 12 o'clock. Pointing right
        // (90 degrees) falls inside wedge 1 (span 36..108).
        assertEquals(1, SwRadialMath.wedgeFromMouse(160, 100, CX, CY, DEAD, 5));
        // Straight down (180 degrees) sits on the wedge 2/3 boundary; accept either.
        int down = SwRadialMath.wedgeFromMouse(100, 160, CX, CY, DEAD, 5);
        assertTrue(down == 2 || down == 3, "down was " + down);
    }

    @Test
    void pointingLeft_isWedgeFour() {
        assertEquals(4, SwRadialMath.wedgeFromMouse(40, 100, CX, CY, DEAD, 5));
    }

    @Test
    void neverReturnsWedgeCount() {
        // Sweep 360 degrees at radius 50; result always in [0, wedgeCount).
        for (int deg = 0; deg < 360; deg++) {
            double rad = Math.toRadians(deg);
            int w = SwRadialMath.wedgeFromMouse(
                CX + 50 * Math.sin(rad), CY - 50 * Math.cos(rad), CX, CY, DEAD, 5);
            assertTrue(w >= 0 && w < 5, "deg=" + deg + " w=" + w);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :starwars:test --tests SwRadialMathTest
```
Expected: FAIL.

- [ ] **Step 3: Implement `SwRadialMath`** — generalized port of `wildwest/.../client/RadialMath.java` (same atan2 convention, wedge 0 at 12 o'clock, clockwise) with `wedgeCount` parameter replacing the hardcoded 6, including the float-edge clamp (`wedge >= wedgeCount ? 0 : wedge`).

- [ ] **Step 4: Packet + handler.** `C2SSelectPowerPacket` — port of `wildwest/.../network/C2SSetActiveStonePacket.java`: validate `powerIndex` against `ForcePower.values().length`, verify held stack `.is(Registration.HOLOCRON.get())`, set `ModDataComponents.ACTIVE_POWER`, re-sync the vanilla cooldown sweep from `POWER_COOLDOWNS` exactly like the reference does (isOnCooldown → `addCooldown(remaining)`, else `removeCooldown(getCooldownGroup(stack))`). Register in `NetworkHandlers` via `reg.playToServer(C2SSelectPowerPacket.TYPE, C2SSelectPowerPacket.STREAM_CODEC, C2SSelectPowerPacket::handle)`.

- [ ] **Step 5: Screen + keybind.** Copy the class structure of `wildwest/.../client/RadialPickerScreen.java` (its `extractRenderState(GuiGraphicsExtractor, ...)`-pipeline rendering, mouse handling, close-sends-selection behavior) into `ForcePickerScreen` with: 5 wedges, labels `Component.translatable(power.translationKey())`, per-wedge dimming + remaining-seconds text when `ForceCooldowns.isOnCooldown(...)` (read the held stack's `POWER_COOLDOWNS`), wedge colors — PUSH `0xFF8090A0`, PULL `0xFF60A090`, LEAP `0xFF70C070`, MIND_TRICK `0xFFC0A040`, LIGHTNING `0xFF9040C0`. On click/close with hovered wedge: `ClientPacketDistributor.sendToServer(new C2SSelectPowerPacket(wedge, mainHand))` (same distributor class the reference uses). `HolocronKeybind`: copy `InfinityGauntletKeybind`'s registration + rapid-retap guard, key `H` (the reference uses `G` — no conflict), lang key `"key.starwars.open_force_picker"` → "Open Force Picker", category: the reference uses vanilla `KeyMapping.Category.MISC` — do the same (no custom category lang key); opens `ForcePickerScreen` only when a held stack is the holocron.

- [ ] **Step 6: Run tests, build, commit**

```bash
./gradlew :starwars:test --tests SwRadialMathTest && ./gradlew :starwars:build
git add starwars/
git commit -m "feat(starwars): force radial picker, keybind, selection packet"
```

---

## Task 26: Milestone 5 Bedrock Pass

Same procedure as Task 11: run translator, read `UNTRANSLATABLE.md` (expected additions: holocron use behavior, radial picker screen, keybind, pacify attachment), commit as `"feat(starwars): milestone-5 bedrock translation"`.

---

# Milestone 6 — Worldgen Structures + Specialists

**Shared context for Tasks 27-29 (read before each):** this repo has NO existing datapack worldgen — these tasks break new ground. The API pattern is vanilla's: a `Structure` subclass + `StructurePiece` subclass registered in the `STRUCTURE_TYPE`/`STRUCTURE_PIECE` code registries, plus datapack entries (`Registries.STRUCTURE`, `Registries.STRUCTURE_SET`) emitted through the existing `DatapackBuiltinEntriesProvider` in `DataGenerators`. When a constructor/override signature fails to compile, open the decompiled vanilla sources (the NeoForge sources jar in `~/.gradle/caches` — find it with `find ~/.gradle -name "*neoforge*sources*.jar" | head`) and lift the current signatures from `net.minecraft.world.level.levelgen.structure.structures.DesertPyramidStructure` + its piece, and `net.minecraft.data.worldgen.Structures`/`StructureSets` for the datagen shapes. Do not guess.

## Task 27: Structure Plumbing + Crashed Escape Pod

**Files:**
- Create: `.../world/ModStructures.java`, `.../world/EscapePodLayout.java`, `.../world/EscapePodStructure.java`, `.../world/EscapePodPiece.java`, `.../data/ModStructureProvider.java`, `starwars/src/main/resources/data/starwars/loot_table/chests/escape_pod.json`, `starwars/src/main/resources/data/starwars/tags/worldgen/structure/crash_sites.json`
- Modify: `.../StarWarsMod.java`, `.../data/DataGenerators.java`
- Test: `starwars/src/test/java/com/tweeks/starwars/world/EscapePodLayoutTest.java`

**Interfaces:**
- Consumes: `DataGenerators` (Task 2).
- Produces: `ModStructures` with `STRUCTURE_TYPES`/`STRUCTURE_PIECES` DeferredRegisters + `register(IEventBus)`, holders `ESCAPE_POD_TYPE`, `ESCAPE_POD_PIECE` (Tasks 28-29 add theirs); `ModStructureProvider.bootstrapStructures`/`bootstrapSets` (Tasks 28-29 extend); the `Layout → Piece.postProcess` pattern.

- [ ] **Step 1: Write the failing layout test**

```java
package com.tweeks.starwars.world;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EscapePodLayoutTest {

    @Test
    void exactlyOneChest() {
        long chests = EscapePodLayout.placements().stream()
            .filter(p -> p.kind() == EscapePodLayout.Kind.CHEST).count();
        assertEquals(1, chests);
    }

    @Test
    void allPlacementsInsideDeclaredBounds() {
        for (var p : EscapePodLayout.placements()) {
            assertTrue(p.dx() >= 0 && p.dx() < EscapePodLayout.SIZE_X, p.toString());
            assertTrue(p.dy() >= 0 && p.dy() < EscapePodLayout.SIZE_Y, p.toString());
            assertTrue(p.dz() >= 0 && p.dz() < EscapePodLayout.SIZE_Z, p.toString());
        }
    }

    @Test
    void shellIsMirroredInX() {
        List<EscapePodLayout.Placement> shell = EscapePodLayout.placements().stream()
            .filter(p -> p.kind() == EscapePodLayout.Kind.SHELL).toList();
        for (var p : shell) {
            int mirrored = EscapePodLayout.SIZE_X - 1 - p.dx();
            assertTrue(shell.stream().anyMatch(q ->
                    q.dx() == mirrored && q.dy() == p.dy() && q.dz() == p.dz()),
                "no mirror for " + p);
        }
    }

    @Test
    void hasInteriorAir() {
        assertTrue(EscapePodLayout.placements().stream()
            .anyMatch(p -> p.kind() == EscapePodLayout.Kind.AIR));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :starwars:test --tests EscapePodLayoutTest
```
Expected: FAIL.

- [ ] **Step 3: Implement `EscapePodLayout.java`** — a 5×4×5 dented pod shell, MC-free:

```java
package com.tweeks.starwars.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Crashed escape pod: a 5x4x5 rounded shell with a torn-open front (the
 * "crash damage" gap at z=0, x=1..3), floor, one loot chest, interior air.
 * Pure data so shape invariants are unit-testable without MC bootstrap.
 */
public final class EscapePodLayout {
    private EscapePodLayout() {}

    public static final int SIZE_X = 5;
    public static final int SIZE_Y = 4;
    public static final int SIZE_Z = 5;

    public enum Kind { SHELL, FLOOR, CHEST, AIR }

    public record Placement(int dx, int dy, int dz, Kind kind) {}

    public static List<Placement> placements() {
        List<Placement> out = new ArrayList<>();
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                boolean corner = (x == 0 || x == SIZE_X - 1) && (z == 0 || z == SIZE_Z - 1);
                if (corner) continue;                       // rounded corners
                out.add(new Placement(x, 0, z, Kind.FLOOR));
                boolean edge = x == 0 || x == SIZE_X - 1 || z == 0 || z == SIZE_Z - 1;
                for (int y = 1; y < SIZE_Y; y++) {
                    boolean roof = y == SIZE_Y - 1;
                    boolean tornFront = z == 0 && x >= 1 && x <= 3 && y <= 2;   // symmetric crash opening
                    if (tornFront) continue;                // crash opening
                    if (edge || roof) {
                        out.add(new Placement(x, y, z, Kind.SHELL));
                    } else {
                        out.add(new Placement(x, y, z, Kind.AIR));
                    }
                }
            }
        }
        // Loot chest against the back wall interior.
        out.removeIf(p -> p.dx() == 2 && p.dy() == 1 && p.dz() == 3);
        out.add(new Placement(2, 1, 3, Kind.CHEST));
        return out;
    }
}
```

The torn-front gap spans x=1..3, symmetric around the center column, so `shellIsMirroredInX` holds.

- [ ] **Step 4: Run test to verify it passes** (`./gradlew :starwars:test --tests EscapePodLayoutTest`).

- [ ] **Step 5: Code registries.** `ModStructures.java`:

```java
package com.tweeks.starwars.world;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    private ModStructures() {}

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
        DeferredRegister.create(Registries.STRUCTURE_TYPE, StarWarsMod.MOD_ID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
        DeferredRegister.create(Registries.STRUCTURE_PIECE, StarWarsMod.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<EscapePodStructure>> ESCAPE_POD_TYPE =
        STRUCTURE_TYPES.register("escape_pod", () -> () -> EscapePodStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> ESCAPE_POD_PIECE =
        STRUCTURE_PIECES.register("escape_pod",
            () -> (StructurePieceType) EscapePodPiece::new);

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
    }
}
```

Wire `com.tweeks.starwars.world.ModStructures.register(modEventBus);` into `StarWarsMod`.

- [ ] **Step 6: Structure + piece.** `EscapePodStructure.java`:

```java
package com.tweeks.starwars.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class EscapePodStructure extends Structure {

    public static final MapCodec<EscapePodStructure> CODEC = simpleCodec(EscapePodStructure::new);

    public EscapePodStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG,
            builder -> builder.addPiece(new EscapePodPiece(
                context.chunkPos().getMiddleBlockPosition(0))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ESCAPE_POD_TYPE.get();
    }
}
```

`EscapePodPiece.java`:

```java
package com.tweeks.starwars.world;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

public class EscapePodPiece extends StructurePiece {

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "chests/escape_pod"));

    public EscapePodPiece(BlockPos origin) {
        super(com.tweeks.starwars.world.ModStructures.ESCAPE_POD_PIECE.get(), 0,
            new BoundingBox(origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + EscapePodLayout.SIZE_X - 1,
                origin.getY() + EscapePodLayout.SIZE_Y - 1,
                origin.getZ() + EscapePodLayout.SIZE_Z - 1));
        this.setOrientation(null);
    }

    public EscapePodPiece(StructurePieceSerializationContext ctx, net.minecraft.nbt.CompoundTag tag) {
        super(com.tweeks.starwars.world.ModStructures.ESCAPE_POD_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx,
                                         net.minecraft.nbt.CompoundTag tag) {
        // Bounding box handled by the base class; no extra state.
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        for (var p : EscapePodLayout.placements()) {
            var state = switch (p.kind()) {
                case SHELL -> Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                case FLOOR -> Blocks.GRAY_CONCRETE.defaultBlockState();
                case AIR -> Blocks.AIR.defaultBlockState();
                case CHEST -> null;   // handled below via createChest
            };
            if (state != null) {
                this.placeBlock(level, state, p.dx(), p.dy(), p.dz(), box);
            } else {
                this.createChest(level, box, random, p.dx(), p.dy(), p.dz(), LOOT);
            }
        }
    }
}
```

API caveats (verify on first compile, per the milestone header): the `StructurePiece` tag-constructor and `addAdditionalSaveData` may take `CompoundTag` or the newer `ValueInput`/`ValueOutput` — lift from vanilla's `ScatteredFeaturePiece`; `createChest`'s loot parameter may be `ResourceKey<LootTable>` or `Identifier` — match the vanilla signature; the `StructurePieceType` functional shape may need `StructurePieceType.ContextlessType` — match how vanilla registers `ScatteredFeaturePiece` types.

- [ ] **Step 7: Datagen + loot + tag.** `ModStructureProvider.java`:

```java
package com.tweeks.starwars.data;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.world.EscapePodStructure;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

import java.util.Map;

public final class ModStructureProvider {
    private ModStructureProvider() {}

    public static final ResourceKey<Structure> ESCAPE_POD = ResourceKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "escape_pod"));

    public static final ResourceKey<StructureSet> ESCAPE_POD_SET = ResourceKey.create(
        Registries.STRUCTURE_SET,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "escape_pods"));

    public static void bootstrapStructures(BootstrapContext<Structure> ctx) {
        var biomes = ctx.lookup(Registries.BIOME);
        ctx.register(ESCAPE_POD, new EscapePodStructure(new Structure.StructureSettings(
            HolderSet.direct(
                biomes.getOrThrow(Biomes.PLAINS),
                biomes.getOrThrow(Biomes.DESERT)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.BEARD_THIN)));
    }

    public static void bootstrapSets(BootstrapContext<StructureSet> ctx) {
        var structures = ctx.lookup(Registries.STRUCTURE);
        ctx.register(ESCAPE_POD_SET, new StructureSet(
            structures.getOrThrow(ESCAPE_POD),
            new RandomSpreadStructurePlacement(24, 8, RandomSpreadType.LINEAR, 1977100501)));
    }
}
```

(If `Structure.StructureSettings` needs a different constructor — some versions use a builder — lift from vanilla `net.minecraft.data.worldgen.Structures`.) Add to `DataGenerators`' `RegistrySetBuilder`:

```java
.add(Registries.STRUCTURE, ModStructureProvider::bootstrapStructures)
.add(Registries.STRUCTURE_SET, ModStructureProvider::bootstrapSets)
```

`data/starwars/loot_table/chests/escape_pod.json`:

```json
{
  "type": "minecraft:chest",
  "pools": [
    {
      "rolls": { "type": "minecraft:uniform", "min": 2, "max": 4 },
      "entries": [
        { "type": "minecraft:item", "name": "starwars:blaster_pistol", "weight": 8 },
        { "type": "minecraft:item", "name": "minecraft:iron_ingot", "weight": 20,
          "functions": [ { "function": "minecraft:set_count",
                           "count": { "type": "minecraft:uniform", "min": 1, "max": 3 } } ] },
        { "type": "minecraft:item", "name": "minecraft:bread", "weight": 15,
          "functions": [ { "function": "minecraft:set_count",
                           "count": { "type": "minecraft:uniform", "min": 1, "max": 2 } } ] },
        { "type": "minecraft:item", "name": "minecraft:redstone", "weight": 12,
          "functions": [ { "function": "minecraft:set_count",
                           "count": { "type": "minecraft:uniform", "min": 2, "max": 5 } } ] }
      ]
    }
  ]
}
```

`data/starwars/tags/worldgen/structure/crash_sites.json`:

```json
{
  "values": [ "starwars:escape_pod" ]
}
```

- [ ] **Step 8: Verify generation.** Build + datagen, then confirm the structure JSON emitted under `starwars/src/generated/serverData/data/starwars/worldgen/structure/escape_pod.json` and structure-set JSON exist and reference `starwars:escape_pod`.

```bash
./gradlew :starwars:build :starwars:test :starwars:runServerData
ls starwars/src/generated/serverData/data/starwars/worldgen/structure/ \
   starwars/src/generated/serverData/data/starwars/worldgen/structure_set/
```

- [ ] **Step 9: Commit**

```bash
git add starwars/
git commit -m "feat(starwars): datapack worldgen plumbing + crashed escape pod structure"
```

---

## Task 28: Imperial Outpost Structure + Garrison + Vader Anchor

**Files:**
- Create: `.../world/ImperialOutpostLayout.java`, `.../world/ImperialOutpostStructure.java`, `.../world/ImperialOutpostPiece.java`, `data/starwars/loot_table/chests/imperial_outpost.json`, `data/starwars/tags/worldgen/structure/imperial.json`
- Modify: `.../world/ModStructures.java`, `.../data/ModStructureProvider.java`, `.../spawning/NamedCharacterSpawner.java`
- Test: `.../world/ImperialOutpostLayoutTest.java`

**Interfaces:**
- Consumes: the Task 27 pattern; `ModEntities.STORMTROOPER`/`BATTLE_DROID`; `NamedCharacterSpawner` (Task 18).
- Produces: `ModStructures.IMPERIAL_OUTPOST_TYPE`/`IMPERIAL_OUTPOST_PIECE`; `ModStructureProvider.IMPERIAL_OUTPOST`/`IMPERIAL_OUTPOST_SET`; garrison-at-generation pattern Task 29 reuses; structure-anchored Vader spawning.

- [ ] **Step 1: Layout + test.** `ImperialOutpostLayout` — 11×7×11: 1-block blackstone platform (y0), 4 corner pillars (polished blackstone, y1..5), perimeter walls (gray concrete, y1..3, with a 3-wide south gate at z=0 x=4..6), crenellated roof ring (y4 on wall tops, alternating), interior air, one chest at (2,1,8), four GARRISON markers (kind `GARRISON_TROOPER` ×3 at (3,1,5), (7,1,5), (5,1,7); `GARRISON_DROID` ×2 at (4,1,3), (6,1,3)). Same `Kind`/`Placement` record shape as `EscapePodLayout` with kinds `{ PLATFORM, PILLAR, WALL, ROOF, CHEST, AIR, GARRISON_TROOPER, GARRISON_DROID }` and `SIZE_X/Y/Z` constants. Test (`ImperialOutpostLayoutTest`) mirrors the escape-pod test: exactly 1 chest, 3 trooper + 2 droid markers, bounds check, gate opening exists (no WALL at (5,1,0)).

Write the full nested-loop generator in the same explicit style as `EscapePodLayout.placements()` — platform loop, pillar corners `(1,1),(1,9),(9,1),(9,9)`, wall edges minus the gate x-range, roof ring `if ((x + z) % 2 == 0)` on wall tops, then the fixed chest/garrison placements appended at the end.

- [ ] **Step 2: TDD cycle** — run test (fails), implement, run test (passes), exactly as Task 27 Steps 1-4.

- [ ] **Step 3: Structure + piece classes.** Repeat the `EscapePodStructure`/`EscapePodPiece` classes verbatim with `EscapePod → ImperialOutpost` name swaps, `ESCAPE_POD_TYPE → IMPERIAL_OUTPOST_TYPE` (register `"imperial_outpost"` for both type and piece in `ModStructures`), loot key `chests/imperial_outpost`, and this `postProcess` block-mapping:

```java
case PLATFORM -> Blocks.BLACKSTONE.defaultBlockState();
case PILLAR -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
case WALL -> Blocks.GRAY_CONCRETE.defaultBlockState();
case ROOF -> Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState();
case AIR -> Blocks.AIR.defaultBlockState();
case CHEST -> null;      // createChest with LOOT
case GARRISON_TROOPER, GARRISON_DROID -> null;   // entity spawn below
```

Garrison placement in `postProcess`, after the block loop:

```java
for (var p : ImperialOutpostLayout.placements()) {
    var type = switch (p.kind()) {
        case GARRISON_TROOPER -> com.tweeks.starwars.ModEntities.STORMTROOPER.get();
        case GARRISON_DROID -> com.tweeks.starwars.ModEntities.BATTLE_DROID.get();
        default -> null;
    };
    if (type == null) continue;
    BlockPos worldPos = this.getWorldPos(p.dx(), p.dy(), p.dz());
    if (!box.isInside(worldPos)) continue;   // piece may straddle chunk borders
    var mob = type.create(level.getLevel(), net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
    if (mob == null) continue;
    mob.setPersistenceRequired();
    mob.snapTo(worldPos.getX() + 0.5, worldPos.getY(), worldPos.getZ() + 0.5,
        random.nextFloat() * 360.0F, 0.0F);
    level.addFreshEntityWithPassengers(mob);
}
```

Signature caveats: `getWorldPos(x,y,z)` helper name, `EntityType.create(Level, EntitySpawnReason)` arity, and `snapTo` vs `moveTo` — lift all three from vanilla's elder-guardian placement in `OceanMonumentPieces` (sources jar) if compile fails.

- [ ] **Step 4: Datagen entries** in `ModStructureProvider`: `IMPERIAL_OUTPOST` structure (biomes DESERT + BADLANDS — `Biomes.DESERT`, `Biomes.BADLANDS`; no spawn overrides; SURFACE_STRUCTURES; `TerrainAdjustment.BEARD_THIN`), `IMPERIAL_OUTPOST_SET` (`RandomSpreadStructurePlacement(40, 12, RandomSpreadType.LINEAR, 1977100502)`). Loot JSON `chests/imperial_outpost.json`: 3-6 rolls — armor pieces (each weight 5), `starwars:blaster_rifle` (weight 8), iron ingots 2-4 (weight 20), quartz 2-5 (weight 15) — same JSON shape as the escape pod table with these entries. Tag `imperial.json`: `{ "values": [ "starwars:imperial_outpost" ] }`.

- [ ] **Step 5: Vader anchor.** In `NamedCharacterSpawner`, before picking a random position for Vader, attempt:

```java
BlockPos anchor = level.findNearestMapStructure(
    net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
        net.minecraft.resources.Identifier.fromNamespaceAndPath(com.tweeks.starwars.StarWarsMod.MOD_ID, "imperial")),
    player.blockPosition(), 100, false);
```

If `anchor != null` and within 256 blocks of the player, spawn Vader + escort at ground level near the anchor (reuse the position-validation helper with the anchor as the center); otherwise fall back to the existing random placement. Hoist the `TagKey` into a `private static final` field.

- [ ] **Step 6: Build, test, datagen, commit**

```bash
./gradlew :starwars:build :starwars:test :starwars:runServerData
git add starwars/
git commit -m "feat(starwars): imperial outpost structure with garrison + vader anchor"
```

---

## Task 29: Jedi Ruin Structure + Guardians + Hero Anchor

**Files:**
- Create: `.../world/JediRuinLayout.java`, `.../world/JediRuinStructure.java`, `.../world/JediRuinPiece.java`, `data/starwars/loot_table/chests/jedi_ruin.json`, `data/starwars/tags/worldgen/structure/jedi.json`
- Modify: `.../world/ModStructures.java`, `.../data/ModStructureProvider.java`, `.../spawning/NamedCharacterSpawner.java`
- Test: `.../world/JediRuinLayoutTest.java`

**Interfaces:**
- Consumes: Task 27/28 patterns; `ModEntities.JEDI_KNIGHT`.
- Produces: `ModStructures.JEDI_RUIN_TYPE`/`JEDI_RUIN_PIECE`; `ModStructureProvider.JEDI_RUIN`/`JEDI_RUIN_SET`; hero (Luke/Obi-Wan) spawns anchored to ruins.

- [ ] **Step 1: Layout + test.** `JediRuinLayout` — 9×5×9 ruined rotunda: mossy-stone-brick circular floor (skip the 4 far corners: manhattan distance from center > 6), 6 broken pillars around the ring at fixed offsets `(1,1),(1,7),(4,0),(4,8),(7,1),(7,7)` with randomized-look deterministic heights `[3,2,4,1,3,2]` (stone bricks, top block cracked — kind `PILLAR_CRACKED` for each pillar's top cube), a central raised dais (3×1×3 chiseled stone bricks at y1 centered), one chest on the dais center (y2), two `GUARDIAN_JEDI` markers at `(2,1,4)` and `(6,1,4)`. Kinds `{ FLOOR, PILLAR, PILLAR_CRACKED, DAIS, CHEST, GUARDIAN_JEDI }`. Test: exactly 1 chest, exactly 2 guardians, exactly 6 cracked pillar tops, bounds check.

Implement with the same explicit-loop style; TDD cycle as before.

- [ ] **Step 2: Structure + piece** — repeat the Task 27 classes with `JediRuin` names (`"jedi_ruin"` registrations), block mapping:

```java
case FLOOR -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
case PILLAR -> Blocks.STONE_BRICKS.defaultBlockState();
case PILLAR_CRACKED -> Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
case DAIS -> Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
case CHEST -> null;
case GUARDIAN_JEDI -> null;
```

Guardian placement: the Task 28 garrison loop with `JEDI_KNIGHT` for `GUARDIAN_JEDI`.

- [ ] **Step 3: Datagen entries**: `JEDI_RUIN` (biomes FOREST + JUNGLE — `Biomes.FOREST`, `Biomes.JUNGLE`; SURFACE_STRUCTURES; BEARD_THIN), `JEDI_RUIN_SET` (`RandomSpreadStructurePlacement(40, 12, RandomSpreadType.LINEAR, 1977100503)`). Loot `chests/jedi_ruin.json`: 2-4 rolls — `starwars:lightsaber` (weight 10), `starwars:holocron` (weight 6), `minecraft:glowstone_dust` 2-4 (weight 20), `minecraft:emerald` 1-3 (weight 15), `minecraft:book` (weight 12). Tag `jedi.json`: `{ "values": [ "starwars:jedi_ruin" ] }`.

- [ ] **Step 4: Hero anchor** — in `NamedCharacterSpawner`, apply the Task 28 anchor pattern to Luke and Obi-Wan with the `starwars:jedi` structure tag.

- [ ] **Step 5: Build, test, datagen, commit**

```bash
./gradlew :starwars:build :starwars:test :starwars:runServerData
git add starwars/
git commit -m "feat(starwars): jedi ruin structure with guardians + hero anchor"
```

---

## Task 30: Astromech Droid (complete character)

**Files:**
- Create: `.../entity/AstromechEntity.java`, `.../client/model/AstromechModel.java`, `.../client/AstromechRenderer.java`, `starwars/tools/astromech.bbmodel`, `assets/starwars/textures/entity/astromech.png`, spawn-egg assets
- Modify: `.../ModEntities.java`, `.../StarWarsMod.java`, `.../Registration.java`, `.../client/ClientSetup.java`, datagen providers, generator scripts

**Interfaces:**
- Consumes: `ModEntities`, datagen providers.
- Produces: `ModEntities.ASTROMECH` (id `astromech`, CREATURE, sized 0.7×1.1).

- [ ] **Step 1: Entity** — a plain `PathfinderMob` (not `SwMob` — it never fights; not `SwCombatant` — harming it is alignment-neutral):

```java
package com.tweeks.starwars.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class AstromechEntity extends PathfinderMob {

    public static final double MAX_HEALTH = 10.0;
    public static final double MOVEMENT_SPEED = 0.25;

    public AstromechEntity(EntityType<? extends AstromechEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;   // droid warble
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }
}
```

(If `SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM` doesn't resolve, pick the allay ambient constant that exists — `grep` the sources jar.)

- [ ] **Step 2: Registration** — `ModEntities.ASTROMECH`, `MobCategory.CREATURE`, `.sized(0.7f, 1.1f)`, tracking 10; attributes; spawn placement: the CREATURE surface rule used for Jedi Knight in Task 14.

- [ ] **Step 3: Model** — NOT humanoid. Mirror the crab trio: read `wildwest/.../client/model/CrabModel.java`, `client/CrabRenderer.java`, `client/CrabRenderState.java` and copy their structure (custom `EntityModel` with a render state, `MobRenderer` subclass). Bones and cubes for `AstromechModel.createBodyLayer()` (64×64 texture):

```java
PartDefinition root = mesh.getRoot();
// Cylindrical body approximated by an 8x10x8 box, pivot at ground.
root.addOrReplaceChild("body",
    CubeListBuilder.create().texOffs(0, 20).addBox(-4.0f, -14.0f, -4.0f, 8, 10, 8),
    PartPose.offset(0.0f, 24.0f, 0.0f));
// Dome head: 8x4x8 on top of the body, slightly inset.
root.addOrReplaceChild("head",
    CubeListBuilder.create()
        .texOffs(0, 0).addBox(-4.0f, -4.0f, -4.0f, 8, 4, 8)
        .texOffs(32, 0).addBox(-1.5f, -3.0f, -4.5f, 3, 2, 1),   // eye lens
    PartPose.offset(0.0f, 10.0f, 0.0f));
// Two side legs.
root.addOrReplaceChild("right_leg",
    CubeListBuilder.create().texOffs(32, 20).addBox(-1.0f, 0.0f, -1.5f, 2, 12, 3),
    PartPose.offset(-5.0f, 12.0f, 0.0f));
root.addOrReplaceChild("left_leg",
    CubeListBuilder.create().texOffs(42, 20).addBox(-1.0f, 0.0f, -1.5f, 2, 12, 3),
    PartPose.offset(5.0f, 12.0f, 0.0f));
```

(Adjust pivot y-values against how `CrabModel` anchors its parts — the root-offset convention must match or the droid floats/sinks. Note: `CrabModel.setupAnim` only animates a claw pinch, its legs are static — use the crab trio ONLY for class structure, not for walk animation.) Leg swing in `AstromechModel.setupAnim(state)`, the standard vanilla pair:

```java
this.rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F)
    * 1.4F * state.walkAnimationSpeed;
this.leftLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + (float) Math.PI)
    * 1.4F * state.walkAnimationSpeed;
```

(If the render state's walk-animation field names differ, lift them from vanilla `HumanoidModel.setupAnim` in the sources jar.)

- [ ] **Step 4: Texture + bbmodel.** `paint_astromech` in `gen_textures.py`: white body `(0xE8,0xE8,0xEC)` with two blue `(0x2850A0)` vertical panel stripes and small vent rects `(0x9098A0)`, blue dome `(0x2850A0)` with silver `(0xC0C8D0)` ring rows + black eye lens rect, silver legs with dark joint rows — explicit rects per the UV regions defined in Step 3 (`(0,0)..(32,12)` dome, `(0,20)..(32,38)` body, `(32,20)+` legs, `(32,0)` lens). bbmodel: astromech needs custom bones in `gen_bbmodels.py` — add an `ASTROMECH_CUBES` table using the same bone-tuple format as `BATTLE_DROID_CUBES` but with bones `body/head/right_leg/left_leg` at the Step 3 pivots (extend the script's bone table with `ASTRO_BODY_BONE = (0, 24, 0)`, `ASTRO_HEAD_BONE = (0, 10, 0)`, `ASTRO_RLEG_BONE = (-5, 12, 0)`, `ASTRO_LLEG_BONE = (5, 12, 0)`); `MOBS['astromech'] = ASTROMECH_CUBES`. Run scripts; eyeball (must read as an R2-style white/blue astromech).

- [ ] **Step 5: Egg (white/blue), loot (1-2 iron_nugget @80% + 1-2 redstone @60%), lang (`"Astromech Droid"` + egg), biome spawns (`ADD_ASTROMECHS`: weight 3, group 1-1, PLAINS + DESERT).**

- [ ] **Step 5b: Astromech at the crashed escape pod** (spec: "astromech spawns nearby"). Modify `EscapePodLayout`: add `Kind.ASTROMECH` and one marker placement at `(2, 1, 1)` (interior, near the torn front — replace the AIR placement there the same way the chest replaces its cell). Update `EscapePodLayoutTest` with `exactlyOneAstromechMarker()` and confirm `exactlyOneChest` still passes. Modify `EscapePodPiece.postProcess`: map `ASTROMECH -> null` in the block switch and add the Task 28 garrison-spawn loop with `ModEntities.ASTROMECH.get()` for the marker (persistent, `snapTo` at the marker cell). Run `./gradlew :starwars:test --tests EscapePodLayoutTest` — PASS.

- [ ] **Step 6: Build, datagen, generators, commit**

```bash
./gradlew :starwars:build :starwars:runClientData :starwars:runServerData
git add starwars/
git commit -m "feat(starwars): astromech droid — ambient utility mob"
```

---

## Task 31: Boba Fett (mini-boss)

**Files:**
- Create: `.../entity/BobaFettEntity.java`, `.../entity/ai/BobaJetpackGoal.java`, `.../client/model/BobaFettModel.java`, `.../client/BobaFettRenderer.java`, `starwars/tools/boba_fett.bbmodel`, `assets/starwars/textures/entity/boba_fett.png`, spawn-egg assets
- Modify: `.../ModEntities.java`, `.../StarWarsMod.java`, `.../Registration.java`, `.../client/ClientSetup.java`, `.../spawning/NamedCharacterSpawner.java`, datagen providers, generator scripts

**Interfaces:**
- Consumes: `SwMob`, `BobaFettSavedData` (Task 13), `Registration.BLASTER_RIFLE`, the Task 28 anchor pattern.
- Produces: `ModEntities.BOBA_FETT` (id `boba_fett`, MONSTER, sized 0.6×1.95).

- [ ] **Step 1: `BobaJetpackGoal.java`** — burst-leap toward distant targets:

```java
package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.BobaFettEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Jetpack burst: when the target is 6-16 blocks away and Boba is on the
 * ground, rocket toward it in a high arc with flame particles and
 * slow-falling for the landing. 140-tick cooldown.
 */
public class BobaJetpackGoal extends Goal {

    public static final double MIN_RANGE = 6.0;
    public static final double MAX_RANGE = 16.0;
    public static final int COOLDOWN_TICKS = 140;
    public static final double HORIZONTAL_SPEED = 1.0;
    public static final double VERTICAL_BOOST = 0.8;

    private final BobaFettEntity boba;
    private int cooldown;

    public BobaJetpackGoal(BobaFettEntity boba) {
        this.boba = boba;
        this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        LivingEntity target = boba.getTarget();
        if (target == null || !target.isAlive() || !boba.onGround()) return false;
        double dist = boba.distanceTo(target);
        return dist >= MIN_RANGE && dist <= MAX_RANGE;
    }

    @Override
    public void start() {
        LivingEntity target = boba.getTarget();
        if (target == null) return;
        Vec3 toTarget = target.position().subtract(boba.position());
        Vec3 flat = new Vec3(toTarget.x, 0, toTarget.z).normalize().scale(HORIZONTAL_SPEED);
        boba.setDeltaMovement(flat.x, VERTICAL_BOOST, flat.z);
        boba.hurtMarked = true;
        boba.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0));
        if (boba.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME,
                boba.getX(), boba.getY() + 0.8, boba.getZ(), 20, 0.2, 0.3, 0.2, 0.02);
        }
        this.cooldown = COOLDOWN_TICKS;
    }

    @Override
    public boolean canContinueToUse() { return false; }
}
```

- [ ] **Step 2: `BobaFettEntity.java`** — **required reading: Task 15's `DarthVaderEntity` code.** Same file with these deltas: MONSTER category + `Enemy`, `MAX_HEALTH 60`, `ATTACK_DAMAGE 6`, `MOVEMENT_SPEED 0.33`, faction EMPIRE, `usesBlaster()` true, `usesRifleBlaster()` true, weapon `BLASTER_RIFLE`, `registerGoals()` adds `new BobaJetpackGoal(this)` at priority 1, every `VaderSavedData` reference becomes `BobaFettSavedData`. Keep the full singleton lifecycle verbatim (constructor persistence, claiming `finalizeSpawn`, UUID-guarded `die()` + `remove()` clears).

- [ ] **Step 3: Registration** — `ModEntities.BOBA_FETT`, sized `0.6f, 1.95f`, MONSTER; attributes; no natural placement. Add Boba to `NamedCharacterSpawner`'s roster: `BobaFettSavedData`, biomes DESERT/BADLANDS, no escort, anchored to the `starwars:imperial` tag like Vader (Task 28 pattern).

- [ ] **Step 4: Art.** `BobaFettModel` accessories:

```java
PartDefinition head = root.getChild("head");
head.addOrReplaceChild("helmet_shell",
    CubeListBuilder.create().texOffs(32, 0)
        .addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, new CubeDeformation(0.6f)),
    PartPose.ZERO);
head.addOrReplaceChild("rangefinder",
    CubeListBuilder.create().texOffs(56, 16)
        .addBox(3.8f, -12.0f, -0.5f, 1, 4, 1),
    PartPose.ZERO);
PartDefinition body = root.getChild("body");
body.addOrReplaceChild("jetpack",
    CubeListBuilder.create().texOffs(44, 32)
        .addBox(-3.0f, 0.5f, 2.1f, 6, 8, 3),
    PartPose.ZERO);
```

`paint_boba_fett` in `gen_textures.py`: sage-green armor plates `(0x5A6E50)` over gray flightsuit `(0x60605C)` body/arms, green helmet region with the black T-visor (vertical 2px bar down the face center rows 10..15 + horizontal 6px bar at rows 10..11, color `(0x101012)`), weathering: 3-4 scattered 1×1 rust dots `(0x7A4A30)` on chest and helmet, jetpack region `(44..62, 32..43)`: green tanks with silver `(0xB0B4B8)` nozzle rows, red `(0xA03028)` accent stripes on the shoulder rows of both arms, rangefinder strip dark gray. bbmodel: `MOBS['boba_fett'] = HUMANOID_CUBES + BOBA_FETT_ACCESSORIES` with the three accessory tuples above (`(3.8, -12.0, -0.5, 1, 4, 1)` etc.). Renderer/ClientSetup; run scripts; eyeball (green armor + T-visor must read).

- [ ] **Step 5: Egg (green/red), loot (`100%` 2-4 gold_ingot + `50%` blaster rifle at 30%+ durability + `25%` holocron), lang `"Boba Fett"`, no biome spawns.**

- [ ] **Step 6: Build, datagen, generators, commit**

```bash
./gradlew :starwars:build :starwars:runClientData :starwars:runServerData
git add starwars/
git commit -m "feat(starwars): Boba Fett — jetpack mini-boss"
```

---

# Milestone 7 — Final Bedrock Pass + Audit

## Task 32: Milestone 6 Bedrock Pass + Full UNTRANSLATABLE Audit

- [ ] **Step 1:** `./gradlew :translator:translate --args="starwars"`
- [ ] **Step 2:** Read the full `bedrock-out/starwars/UNTRANSLATABLE.md` top to bottom. Every Java-only feature must appear (expected: hitscan/tracers, saber color select + emissive intent, radial picker + keybind, all custom AI goals unless LLM-translated, attachments, SavedData singletons, datapack structures, set-bonus disguise, holocron behavior). Anything missing from the doc that didn't translate = fix the doc pipeline input or file the gap in the task summary.
- [ ] **Step 3:** Drift check: `./gradlew :translator:translate --args="--diff starwars"` — if the args form differs, check `translator/src/main/kotlin/com/tweeks/translator/Cli.kt`'s usage header for how `--diff` combines with a mod filter. Expected: exit 0 after the Step 1 output is committed.
- [ ] **Step 4:** `git add bedrock-out/starwars/ && git commit -m "feat(starwars): milestone-6 bedrock translation + audit"`

---

# Milestone 8 — Final Verification

## Task 33: Full-Repo Verification + Review

- [ ] **Step 1: Full build + tests across the repo**

```bash
./gradlew build
```
Expected: BUILD SUCCESSFUL, all module tests green (starwars suite: Hitscan, Alignment, TargetPredicates, SwMobConstants, SingletonState, ForceCooldowns, SwRadialMath, EscapePodLayout, ImperialOutpostLayout, JediRuinLayout).

- [ ] **Step 2: Generator determinism** — run every `starwars/tools/gen_*.py` twice; `git status` must be clean after the second run (byte-identical outputs).

```bash
python3 starwars/tools/gen_textures.py && python3 starwars/tools/gen_item_textures.py starwars/src/main/resources/assets/starwars/textures/item/ && python3 starwars/tools/gen_bbmodels.py starwars/tools/ && python3 starwars/tools/gen_weapon_models.py && python3 starwars/tools/gen_spawn_eggs.py
git status --short   # expect: empty
```

- [ ] **Step 3: Asset completeness sweep** — verify all 16 bbmodels exist (`ls starwars/tools/*.bbmodel` → stormtrooper, battle_droid, jedi_knight, darth_vader, luke_skywalker, obi_wan, astromech, boba_fett, lightsaber, blaster_pistol, blaster_rifle, holocron, stormtrooper_armor_helmet, stormtrooper_armor_chestplate, stormtrooper_armor_leggings, stormtrooper_armor_boots); every registered entity has a texture in `textures/entity/`; every item has `items/*.json` + texture. No placeholder art anywhere.

- [ ] **Step 4: Spec conformance read-through** — open `docs/superpowers/specs/2026-07-11-starwars-mod-design.md`, walk sections 1-9, and confirm each shipped or has a recorded deviation in the relevant task summary.

- [ ] **Step 5: Code review** — use the superpowers:requesting-code-review skill over the full task range (or `/code-review high` on the accumulated diff), fix confirmed findings, commit fixes as `fix(starwars): review fixes`.

- [ ] **Step 6: Final commit + summary** — note deferred items honestly: manual dev-client smoke testing (repo norm), Bedrock parity gaps per UNTRANSLATABLE.md, saber-color loot-function caveat if hit (Tasks 15-17).

