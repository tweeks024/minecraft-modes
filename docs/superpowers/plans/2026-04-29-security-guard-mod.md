# Security Guard Mod Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship v1 of the Security Guard mod — a friendly humanoid mob for NeoForge MC 1.21.x that defends players and villagers using stun-on-hit baton attacks, deployable via a 3-iron-block + Guard Helmet construction recipe.

**Architecture:** Single Gradle subproject (`securityguard/`) under a multi-module root that will host the future mod series. Entity extends `AbstractGolem` for friendly-mob defaults; AI uses standard Minecraft goal system; baton is rendered via a custom `RenderLayer` (no item registration). Spawn logic is extracted into a pure-function `SpawnPattern` utility for unit-testability without a Minecraft runtime.

**Tech Stack:** Java 21, NeoForge 1.21.1 MDK (latest stable for 1.21.x), Gradle 8.x, JUnit 5 for unit tests, NeoForge GameTest framework for optional in-game tests.

**Spec:** [`docs/superpowers/specs/2026-04-29-security-guard-mod-design.md`](../specs/2026-04-29-security-guard-mod-design.md)

---

## File Structure

### Root project files
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `gradle.properties`
- Create: `.gitignore`

### Subproject: `securityguard/`
**Build:**
- Create: `securityguard/build.gradle`
- Create: `securityguard/gradle.properties`

**Resources / metadata:**
- Create: `securityguard/src/main/resources/META-INF/neoforge.mods.toml`
- Create: `securityguard/src/main/resources/pack.mcmeta`

**Java source — common:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/SecurityGuardMod.java` — `@Mod` entry point, mod ID constant, event bus wiring
- Create: `securityguard/src/main/java/com/tweeks/securityguard/Registration.java` — all `DeferredRegister` instances + creative tab
- Create: `securityguard/src/main/java/com/tweeks/securityguard/entity/SecurityGuardEntity.java` — entity class extending `AbstractGolem`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/entity/ai/BatonStrikeGoal.java` — custom melee goal applying stun + knockback
- Create: `securityguard/src/main/java/com/tweeks/securityguard/item/GuardHelmetItem.java` — `useOn` triggers spawn pattern detection
- Create: `securityguard/src/main/java/com/tweeks/securityguard/item/SpawnPattern.java` — pure-function column validation, unit-testable
- Create: `securityguard/src/main/java/com/tweeks/securityguard/sound/ModSounds.java` — sound event registration

**Java source — client only:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/client/ClientSetup.java` — renderer + layer-definition registration
- Create: `securityguard/src/main/java/com/tweeks/securityguard/client/model/SecurityGuardModel.java` — humanoid model with cap
- Create: `securityguard/src/main/java/com/tweeks/securityguard/client/model/BatonModel.java` — single-cube baton model
- Create: `securityguard/src/main/java/com/tweeks/securityguard/client/renderer/SecurityGuardRenderer.java` — extends `MobRenderer`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/client/renderer/BatonHeldLayer.java` — render layer drawing the baton in the right hand

**Java source — datagen:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/data/DataGenerators.java` — datagen entry point
- Create: `securityguard/src/main/java/com/tweeks/securityguard/data/ModRecipeProvider.java`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/data/ModEntityLootProvider.java`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/data/ModLanguageProvider.java`

**Assets:**
- Create: `securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png` (placeholder)
- Create: `securityguard/src/main/resources/assets/securityguard/textures/entity/baton.png` (placeholder)
- Create: `securityguard/src/main/resources/assets/securityguard/textures/item/guard_helmet.png` (placeholder)
- Create: `securityguard/src/main/resources/assets/securityguard/models/item/guard_helmet.json`
- Create: `securityguard/src/main/resources/assets/securityguard/sounds.json`

**Tests:**
- Create: `securityguard/src/test/java/com/tweeks/securityguard/item/SpawnPatternTest.java`

---

## Prerequisites

The implementing engineer needs:
- Java 21 JDK installed (`java -version` should report 21+)
- Git
- About 4 GB of disk for Gradle cache + MC dev artifacts
- Internet for the first Gradle build (downloads NeoForge MDK dependencies)
- ImageMagick OR Python 3 with Pillow OR a 16×16 PNG editor (for placeholder textures)

No prior Minecraft modding knowledge required — every NeoForge concept is explained at first use.

---

## Task 1: Root Gradle Multi-Module Setup

Sets up the umbrella project so future mods slot in as siblings.

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `gradle.properties`
- Create: `.gitignore`

- [ ] **Step 1: Create `settings.gradle`**

```groovy
rootProject.name = 'minecraft-mods'

include 'securityguard'
```

- [ ] **Step 2: Create root `build.gradle` (intentionally minimal)**

```groovy
// Root project is purely an aggregator.
// All mod-specific build logic lives in each subproject's build.gradle.
```

- [ ] **Step 3: Create root `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx4G
org.gradle.daemon=true
org.gradle.parallel=true
```

- [ ] **Step 4: Create `.gitignore`**

```gitignore
# Gradle
.gradle/
build/
out/
*.iml
.idea/

# NeoForge dev
run/
runs/
logs/
crash-reports/

# OS
.DS_Store
Thumbs.db

# IDEs
.vscode/
.classpath
.project
.settings/
```

- [ ] **Step 5: Verify Gradle accepts the structure**

Run: `gradle projects` (or `gradle --no-daemon projects` if no global daemon)

Expected output includes:
```
Root project 'minecraft-mods'
+--- Project ':securityguard'
```

(`gradle` will print a warning that `:securityguard` has no `build.gradle` yet — that's fine, fixed in Task 2.)

- [ ] **Step 6: Commit**

```bash
git add settings.gradle build.gradle gradle.properties .gitignore
git commit -m "chore: initialize Gradle multi-module root for mod series"
```

---

## Task 2: NeoForge MDK Skeleton for `securityguard/`

Brings in the NeoForge build plugin, dependencies, and dev-run tasks. After this task, an empty mod will load in a dev Minecraft client.

**Files:**
- Create: `securityguard/build.gradle`
- Create: `securityguard/gradle.properties`
- Create: `securityguard/src/main/resources/META-INF/neoforge.mods.toml`
- Create: `securityguard/src/main/resources/pack.mcmeta`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/SecurityGuardMod.java`

- [ ] **Step 1: Create `securityguard/gradle.properties`**

These constants are referenced by `build.gradle` and `neoforge.mods.toml`. Versions match the NeoForge 1.21.1 MDK release.

```properties
# Build
mod_id=securityguard
mod_name=Security Guard
mod_version=0.1.0
mod_group_id=com.tweeks.securityguard
mod_authors=Tom Weeks
mod_description=Adds a friendly humanoid security guard mob who defends players and villagers with a stun baton.
mod_license=MIT

# Minecraft
minecraft_version=1.21.1
minecraft_version_range=[1.21.1,1.22)
neo_version=21.1.84
neo_version_range=[21.1.0,)
loader_version_range=[4,)

# Mappings (Mojang's official)
mapping_channel=official
mapping_version=1.21.1
```

- [ ] **Step 2: Create `securityguard/build.gradle`**

```groovy
plugins {
    id 'java-library'
    id 'eclipse'
    id 'idea'
    id 'maven-publish'
    id 'net.neoforged.moddev' version '1.0.21'
}

version = mod_version
group = mod_group_id

base {
    archivesName = mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = project.neo_version

    parchment {
        mappingsVersion = '2024.07.28'
        minecraftVersion = '1.21'
    }

    runs {
        configureEach {
            systemProperty 'forge.logging.markers', 'REGISTRIES'
            systemProperty 'forge.logging.console.level', 'debug'
            logLevel = org.slf4j.event.Level.DEBUG
        }

        client {
            client()
            systemProperty 'neoforge.enabledGameTestNamespaces', mod_id
        }

        server {
            server()
            programArgument '--nogui'
            systemProperty 'neoforge.enabledGameTestNamespaces', mod_id
        }

        gameTestServer {
            type = 'gameTestServer'
            systemProperty 'neoforge.enabledGameTestNamespaces', mod_id
        }

        data {
            data()
            programArguments.addAll '--mod', mod_id, '--all',
                '--output', file('src/generated/resources/').getAbsolutePath(),
                '--existing', file('src/main/resources/').getAbsolutePath()
        }
    }

    mods {
        "${mod_id}" {
            sourceSet sourceSets.main
        }
    }
}

sourceSets.main.resources { srcDir 'src/generated/resources' }

repositories {
    mavenLocal()
}

dependencies {
    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test {
    useJUnitPlatform()
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    options.release = 21
}

tasks.withType(ProcessResources).configureEach {
    var replaceProperties = [
        minecraft_version       : minecraft_version,
        minecraft_version_range : minecraft_version_range,
        neo_version             : neo_version,
        neo_version_range       : neo_version_range,
        loader_version_range    : loader_version_range,
        mod_id                  : mod_id,
        mod_name                : mod_name,
        mod_license             : mod_license,
        mod_version             : mod_version,
        mod_authors             : mod_authors,
        mod_description         : mod_description
    ]
    inputs.properties replaceProperties

    filesMatching(['META-INF/neoforge.mods.toml', 'pack.mcmeta']) {
        expand replaceProperties
    }
}

publishing {
    publications {
        register('mavenJava', MavenPublication) {
            from components.java
        }
    }
    repositories {
        maven {
            url "file://${project.projectDir}/repo"
        }
    }
}
```

- [ ] **Step 3: Create `securityguard/src/main/resources/META-INF/neoforge.mods.toml`**

The `${...}` placeholders are filled by the `ProcessResources` task above.

```toml
modLoader="javafml"
loaderVersion="${loader_version_range}"
license="${mod_license}"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"
authors="${mod_authors}"
description='''${mod_description}'''

[[dependencies.${mod_id}]]
    modId="neoforge"
    type="required"
    versionRange="${neo_version_range}"
    ordering="NONE"
    side="BOTH"

[[dependencies.${mod_id}]]
    modId="minecraft"
    type="required"
    versionRange="${minecraft_version_range}"
    ordering="NONE"
    side="BOTH"
```

- [ ] **Step 4: Create `securityguard/src/main/resources/pack.mcmeta`**

```json
{
  "pack": {
    "description": "${mod_name}",
    "pack_format": 34
  }
}
```

(`pack_format` 34 corresponds to MC 1.21.x. If the engineer is targeting a different 1.21.x sub-version, this number may need adjusting — see https://minecraft.wiki/w/Pack_format.)

- [ ] **Step 5: Create the minimal mod entry point at `securityguard/src/main/java/com/tweeks/securityguard/SecurityGuardMod.java`**

```java
package com.tweeks.securityguard;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SecurityGuardMod.MOD_ID)
public class SecurityGuardMod {
    public static final String MOD_ID = "securityguard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public SecurityGuardMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Security Guard mod loading");
    }
}
```

- [ ] **Step 6: Run `./gradlew build` to verify the project configures and compiles**

From the repo root:
```bash
./gradlew :securityguard:build
```

(First run will download NeoForge dependencies — expect 5-15 min depending on connection.)

Expected: `BUILD SUCCESSFUL`. A jar appears at `securityguard/build/libs/securityguard-0.1.0.jar`.

- [ ] **Step 7: Run the dev client to verify the mod loads**

```bash
./gradlew :securityguard:runClient
```

Expected: Minecraft launcher window opens, then the title screen. Open `Mods` from the title screen — `Security Guard` appears in the list with version `0.1.0`. Quit the game.

In the build log you should see one line: `Security Guard mod loading`.

- [ ] **Step 8: Commit**

```bash
git add securityguard/ .gitignore
git commit -m "feat(securityguard): scaffold NeoForge MDK for securityguard mod"
```

---

## Task 3: `Registration` Plumbing + Empty Creative Tab

Sets up `DeferredRegister` instances we'll use in subsequent tasks. Registers an empty creative tab so the mod has a "shelf" in the creative inventory.

**Files:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/Registration.java`
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/SecurityGuardMod.java`

- [ ] **Step 1: Create `Registration.java`**

```java
package com.tweeks.securityguard;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(SecurityGuardMod.MOD_ID);

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, SecurityGuardMod.MOD_ID);

    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(Registries.SOUND_EVENT, SecurityGuardMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SecurityGuardMod.MOD_ID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<CreativeModeTab, CreativeModeTab>
        SECURITY_GUARD_TAB = CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + SecurityGuardMod.MOD_ID))
                .icon(() -> Items.IRON_HELMET.getDefaultInstance())  // replaced with Guard Helmet icon in Task 4
                .displayItems((params, output) -> {
                    // populated by individual register calls in later tasks
                })
                .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
```

- [ ] **Step 2: Wire `Registration.register` into the mod entry point**

Replace `SecurityGuardMod.java` body:

```java
package com.tweeks.securityguard;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SecurityGuardMod.MOD_ID)
public class SecurityGuardMod {
    public static final String MOD_ID = "securityguard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public SecurityGuardMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Security Guard mod loading");
        Registration.register(modEventBus);
    }
}
```

- [ ] **Step 3: Build to verify it compiles**

```bash
./gradlew :securityguard:build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run the client and verify the (empty) creative tab appears**

```bash
./gradlew :securityguard:runClient
```

Open a creative world. Open the creative inventory. Scroll the tabs — there should be a new tab whose icon is an iron helmet (placeholder; real icon set in Task 4) and whose title is the raw translation key `itemGroup.securityguard` (real label set in Task 11). The tab opens but is empty. Quit.

- [ ] **Step 5: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/Registration.java \
        securityguard/src/main/java/com/tweeks/securityguard/SecurityGuardMod.java
git commit -m "feat(securityguard): add registration class and creative tab"
```

---

## Task 4: Guard Helmet Item (No Spawn Logic Yet)

Adds the Guard Helmet item, registers it, places it in the creative tab, sets it as the tab icon. No `useOn` behavior yet — that comes in Task 6.

**Files:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/item/GuardHelmetItem.java`
- Create: `securityguard/src/main/resources/assets/securityguard/textures/item/guard_helmet.png` (16×16 placeholder)
- Create: `securityguard/src/main/resources/assets/securityguard/models/item/guard_helmet.json`
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/Registration.java`

- [ ] **Step 1: Create `GuardHelmetItem.java`**

For now this is a thin subclass of `Item`. We make it a class (rather than just `new Item(...)`) so `useOn` overrides have somewhere to live in Task 6.

```java
package com.tweeks.securityguard.item;

import net.minecraft.world.item.Item;

public class GuardHelmetItem extends Item {
    public GuardHelmetItem(Properties properties) {
        super(properties);
    }
}
```

- [ ] **Step 2: Register the item in `Registration.java`**

Add this static field above `register(...)`, right under the `DeferredRegister` declarations:

```java
    public static final net.neoforged.neoforge.registries.DeferredItem<GuardHelmetItem>
        GUARD_HELMET = ITEMS.register("guard_helmet",
            () -> new GuardHelmetItem(new net.minecraft.world.item.Item.Properties().stacksTo(64)));
```

Add the import at the top:

```java
import com.tweeks.securityguard.item.GuardHelmetItem;
```

- [ ] **Step 3: Update the creative tab to use Guard Helmet as its icon and to include the helmet**

Replace the `SECURITY_GUARD_TAB` registration in `Registration.java` with:

```java
    public static final net.neoforged.neoforge.registries.DeferredHolder<CreativeModeTab, CreativeModeTab>
        SECURITY_GUARD_TAB = CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + SecurityGuardMod.MOD_ID))
                .icon(() -> GUARD_HELMET.get().getDefaultInstance())
                .displayItems((params, output) -> {
                    output.accept(GUARD_HELMET.get());
                    // entity spawn egg added in Task 7
                })
                .build());
```

(The forward reference to `GUARD_HELMET` is fine because the lambda is invoked lazily after registries are populated.)

- [ ] **Step 4: Create the placeholder texture file**

Choose ONE of the following, all of which produce a 16×16 solid-color PNG. Replace with real art later.

Option A — ImageMagick:
```bash
mkdir -p securityguard/src/main/resources/assets/securityguard/textures/item
magick -size 16x16 xc:'#3060c0' \
    securityguard/src/main/resources/assets/securityguard/textures/item/guard_helmet.png
```

Option B — Python with Pillow:
```bash
mkdir -p securityguard/src/main/resources/assets/securityguard/textures/item
python3 -c "from PIL import Image; Image.new('RGBA',(16,16),(48,96,192,255)).save('securityguard/src/main/resources/assets/securityguard/textures/item/guard_helmet.png')"
```

Option C — any image editor: create a 16×16 PNG of any color and save to that path.

- [ ] **Step 5: Create `securityguard/src/main/resources/assets/securityguard/models/item/guard_helmet.json`**

This is the standard "generated item" model that turns a flat texture into the 3D-pixel item shape MC uses for everything from sticks to swords.

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "securityguard:item/guard_helmet"
  }
}
```

- [ ] **Step 6: Build**

```bash
./gradlew :securityguard:build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Run client and verify the item**

```bash
./gradlew :securityguard:runClient
```

In a creative world: open the creative inventory, find the Security Guard tab — its icon should now be the placeholder helmet. Inside, the Guard Helmet item appears. Pick it up; it has the placeholder texture. Its name displays as `item.securityguard.guard_helmet` (real name in Task 11). Quit.

- [ ] **Step 8: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/item/ \
        securityguard/src/main/java/com/tweeks/securityguard/Registration.java \
        securityguard/src/main/resources/assets/securityguard/
git commit -m "feat(securityguard): add Guard Helmet item with placeholder texture"
```

---

## Task 5: `SpawnPattern` Utility (Pure Function, Test-First)

The column-validation logic that `GuardHelmetItem.useOn` will call in Task 6. Built first, with tests, because it's the only piece of game logic in the mod that's purely functional and worth unit-testing.

**Files:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/item/SpawnPattern.java`
- Create: `securityguard/src/test/java/com/tweeks/securityguard/item/SpawnPatternTest.java`

- [ ] **Step 1: Write failing tests at `securityguard/src/test/java/com/tweeks/securityguard/item/SpawnPatternTest.java`**

```java
package com.tweeks.securityguard.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnPatternTest {

    /** Tiny BlockGetter-style accessor: positions not in the map default to AIR. */
    private static Function<BlockPos, BlockState> world(Map<BlockPos, BlockState> blocks) {
        return pos -> blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Test
    void threeIronBelowAndTwoAirAbove_isValid() {
        BlockPos top = new BlockPos(0, 64, 0);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        blocks.put(top, Blocks.IRON_BLOCK.defaultBlockState());
        blocks.put(top.below(), Blocks.IRON_BLOCK.defaultBlockState());
        blocks.put(top.below(2), Blocks.IRON_BLOCK.defaultBlockState());
        // top.above() and top.above(2) are AIR by default

        assertTrue(SpawnPattern.matches(world(blocks), top));
    }

    @Test
    void onlyTwoIron_invalid() {
        BlockPos top = new BlockPos(0, 64, 0);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        blocks.put(top, Blocks.IRON_BLOCK.defaultBlockState());
        blocks.put(top.below(), Blocks.IRON_BLOCK.defaultBlockState());
        blocks.put(top.below(2), Blocks.DIRT.defaultBlockState());

        assertFalse(SpawnPattern.matches(world(blocks), top));
    }

    @Test
    void ironColumnButObstructedAbove_invalid() {
        BlockPos top = new BlockPos(0, 64, 0);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        blocks.put(top, Blocks.IRON_BLOCK.defaultBlockState());
        blocks.put(top.below(), Blocks.IRON_BLOCK.defaultBlockState());
        blocks.put(top.below(2), Blocks.IRON_BLOCK.defaultBlockState());
        blocks.put(top.above(), Blocks.STONE.defaultBlockState());  // ceiling

        assertFalse(SpawnPattern.matches(world(blocks), top));
    }

    @Test
    void allAir_invalid() {
        BlockPos top = new BlockPos(0, 64, 0);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        assertFalse(SpawnPattern.matches(world(blocks), top));
    }

    @Test
    void mixedNonIronInColumn_invalid() {
        BlockPos top = new BlockPos(0, 64, 0);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        blocks.put(top, Blocks.IRON_BLOCK.defaultBlockState());
        blocks.put(top.below(), Blocks.GOLD_BLOCK.defaultBlockState());  // wrong block
        blocks.put(top.below(2), Blocks.IRON_BLOCK.defaultBlockState());

        assertFalse(SpawnPattern.matches(world(blocks), top));
    }
}
```

- [ ] **Step 2: Run tests, confirm they fail with "cannot find symbol SpawnPattern"**

```bash
./gradlew :securityguard:test
```

Expected: compile failure referencing `SpawnPattern` not existing.

- [ ] **Step 3: Implement `SpawnPattern.java`**

```java
package com.tweeks.securityguard.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

/**
 * Pure-function validation of the Security Guard spawn column.
 *
 * <p>A valid pattern is:
 * <ul>
 *   <li>The clicked-on block is an iron block (the "top" of the column).</li>
 *   <li>The two blocks directly below it are also iron blocks.</li>
 *   <li>The two blocks directly above the top are air-passable (so the entity, ~2 blocks tall, has room to materialize at the base of the column).</li>
 * </ul>
 *
 * <p>Extracted as a pure function so it can be unit-tested without spinning up a Minecraft server.
 * The {@link Function} parameter abstracts over either a real {@code Level} or a test fake.
 */
public final class SpawnPattern {
    private SpawnPattern() {}

    public static boolean matches(Function<BlockPos, BlockState> blockAt, BlockPos top) {
        for (int dy = 0; dy >= -2; dy--) {
            if (!blockAt.apply(top.offset(0, dy, 0)).is(Blocks.IRON_BLOCK)) {
                return false;
            }
        }
        for (int dy = 1; dy <= 2; dy++) {
            if (!blockAt.apply(top.offset(0, dy, 0)).isAir()) {
                return false;
            }
        }
        return true;
    }
}
```

- [ ] **Step 4: Run tests, confirm they pass**

```bash
./gradlew :securityguard:test
```

Expected: all 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/item/SpawnPattern.java \
        securityguard/src/test/java/com/tweeks/securityguard/item/SpawnPatternTest.java
git commit -m "feat(securityguard): add SpawnPattern utility with unit tests"
```

---

## Task 6: Entity Class (Bare Minimum, No AI Yet)

Registers the entity type so we can spawn it via `/summon`. Default `AbstractGolem` AI is fine for this task — custom AI lands in Task 9. Done before the renderer (Task 8) so we have something to render.

**Files:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/entity/SecurityGuardEntity.java`
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/Registration.java`
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/SecurityGuardMod.java`

- [ ] **Step 1: Create `SecurityGuardEntity.java`**

```java
package com.tweeks.securityguard.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import org.jetbrains.annotations.Nullable;

/**
 * The Security Guard entity. Subclasses {@link IronGolem} for its target-selection,
 * persistence, and friendly-mob defaults; we override stats and combat behavior in
 * later tasks. Marked player-created on natural construction (see GuardHelmetItem).
 */
public class SecurityGuardEntity extends IronGolem {

    public SecurityGuardEntity(EntityType<? extends SecurityGuardEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IronGolem.createAttributes()
            .add(Attributes.MAX_HEALTH, 50.0)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
                                        DifficultyInstance difficulty,
                                        MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnData) {
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData);
    }
}
```

(Yes — we extend `IronGolem` despite the spec preferring `AbstractGolem`. Reason: `IronGolem` is the only public subclass that implements `Enemy`-aware target selection out of the box, and reimplementing that whole goal set just to skip a couple of unwanted defaults — repair counter, poppy offering — costs more lines than it saves. We'll override the unwanted methods explicitly in Task 9. Update the spec's architecture note in the same commit if you want to keep the docs accurate.

**Note on iron-ingot healing:** subclassing `IronGolem` means the guard inherits its iron-ingot repair behavior — right-clicking the guard with an iron ingot heals it. This is kept as a feature for v1: it's lore-consistent ("guard wears iron armor; iron repairs it") and matches a behavior players already know from iron golems. If you want to disable it later, override `mobInteract` in this class.)

- [ ] **Step 2: Register the entity type in `Registration.java`**

Add below the `GUARD_HELMET` field:

```java
    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<com.tweeks.securityguard.entity.SecurityGuardEntity>>
        SECURITY_GUARD = ENTITY_TYPES.register("guard",
            () -> EntityType.Builder.<com.tweeks.securityguard.entity.SecurityGuardEntity>of(
                    com.tweeks.securityguard.entity.SecurityGuardEntity::new,
                    net.minecraft.world.entity.MobCategory.MISC)
                .sized(0.6f, 1.95f)
                .clientTrackingRange(10)
                .build(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    SecurityGuardMod.MOD_ID, "guard").toString()));
```

- [ ] **Step 3: Wire up entity attribute registration in `SecurityGuardMod.java`**

This subscribes a static method to NeoForge's mod-bus event for entity attribute creation.

Replace `SecurityGuardMod.java` with:

```java
package com.tweeks.securityguard;

import com.tweeks.securityguard.entity.SecurityGuardEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SecurityGuardMod.MOD_ID)
public class SecurityGuardMod {
    public static final String MOD_ID = "securityguard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public SecurityGuardMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Security Guard mod loading");
        Registration.register(modEventBus);
        modEventBus.addListener(SecurityGuardMod::registerEntityAttributes);
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(Registration.SECURITY_GUARD.get(), SecurityGuardEntity.createAttributes().build());
    }
}
```

- [ ] **Step 4: Build**

```bash
./gradlew :securityguard:build
```

Expected: `BUILD SUCCESSFUL`. (No client-side renderer yet, so spawning the entity in-game will result in an invisible mob with the default hitbox — fixed in Task 8.)

- [ ] **Step 5: Run client, summon the entity, verify it exists**

```bash
./gradlew :securityguard:runClient
```

In a creative world:
1. Open chat with `T`.
2. Type `/summon securityguard:guard`.
3. Press F3+B to show entity hitboxes.

Expected: a hitbox appears in front of you, sized roughly 0.6 × 1.95 blocks. The entity is invisible (no renderer registered yet). It moves around using default iron-golem AI. Hit it with your fist — it takes damage. Quit the game.

- [ ] **Step 6: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/entity/SecurityGuardEntity.java \
        securityguard/src/main/java/com/tweeks/securityguard/Registration.java \
        securityguard/src/main/java/com/tweeks/securityguard/SecurityGuardMod.java
git commit -m "feat(securityguard): register Security Guard entity (no renderer yet)"
```

---

## Task 7: Spawn Egg + Creative Tab Entry

Standard NeoForge spawn egg registered alongside the entity, added to the creative tab.

**Files:**
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/Registration.java`

- [ ] **Step 1: Register the spawn egg**

Add this field below the `SECURITY_GUARD` entity registration:

```java
    public static final net.neoforged.neoforge.registries.DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem>
        GUARD_SPAWN_EGG = ITEMS.register("guard_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(
                SECURITY_GUARD,
                0x162e5e,  // primary: navy uniform
                0xb0b0b0,  // secondary: silver cap badge
                new net.minecraft.world.item.Item.Properties()));
```

(`DeferredSpawnEggItem` is a NeoForge subclass of `SpawnEggItem` that handles the late binding from the spawn egg back to a `DeferredHolder`-wrapped entity type. It lives in `net.neoforged.neoforge.common`. Don't use the vanilla `SpawnEggItem` — it requires the entity type to be present at construction time, which it isn't with deferred registration.)

- [ ] **Step 2: Add the spawn egg to the creative tab**

Update the `displayItems` lambda in `SECURITY_GUARD_TAB`:

```java
                .displayItems((params, output) -> {
                    output.accept(GUARD_HELMET.get());
                    output.accept(GUARD_SPAWN_EGG.get());
                })
```

- [ ] **Step 3: Build**

```bash
./gradlew :securityguard:build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run client and verify the spawn egg works**

```bash
./gradlew :securityguard:runClient
```

In a creative world: open the creative inventory → Security Guard tab. The Guard Spawn Egg appears alongside the helmet. Right-click on the ground with the egg — an entity hitbox appears (still invisible, fixed in Task 8). Quit.

- [ ] **Step 5: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/Registration.java
git commit -m "feat(securityguard): add Security Guard spawn egg"
```

---

## Task 8: Entity Renderer + Humanoid Model (No Baton Yet)

Makes the entity visible. Humanoid model with an extra "cap" cube on the head. Baton render layer added in the next task.

**Files:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/client/ClientSetup.java`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/client/model/SecurityGuardModel.java`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/client/renderer/SecurityGuardRenderer.java`
- Create: `securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png` (placeholder)

- [ ] **Step 1: Create `SecurityGuardModel.java`**

```java
package com.tweeks.securityguard.client.model;

import com.tweeks.securityguard.entity.SecurityGuardEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import com.tweeks.securityguard.SecurityGuardMod;

public class SecurityGuardModel extends HumanoidModel<SecurityGuardEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SecurityGuardMod.MOD_ID, "guard"),
            "main");

    public SecurityGuardModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        // Start from the standard humanoid mesh, then add the cap to the head.
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.getChild("head");
        // Cap brim: thin disc above the head
        head.addOrReplaceChild(
            "cap_brim",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.5f, -9.0f, -4.5f, 9, 1, 9),
            PartPose.ZERO);
        // Cap crown: shorter cube on top of the brim
        head.addOrReplaceChild(
            "cap_crown",
            CubeListBuilder.create()
                .texOffs(32, 10)
                .addBox(-3.5f, -11.5f, -3.5f, 7, 2, 7),
            PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }
}
```

- [ ] **Step 2: Create `SecurityGuardRenderer.java`**

```java
package com.tweeks.securityguard.client.renderer;

import com.tweeks.securityguard.SecurityGuardMod;
import com.tweeks.securityguard.client.model.SecurityGuardModel;
import com.tweeks.securityguard.entity.SecurityGuardEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class SecurityGuardRenderer
        extends HumanoidMobRenderer<SecurityGuardEntity, SecurityGuardModel> {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(SecurityGuardMod.MOD_ID, "textures/entity/security_guard.png");

    public SecurityGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new SecurityGuardModel(context.bakeLayer(SecurityGuardModel.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(SecurityGuardEntity entity) {
        return TEXTURE;
    }
}
```

- [ ] **Step 3: Create `ClientSetup.java`**

```java
package com.tweeks.securityguard.client;

import com.tweeks.securityguard.Registration;
import com.tweeks.securityguard.SecurityGuardMod;
import com.tweeks.securityguard.client.model.SecurityGuardModel;
import com.tweeks.securityguard.client.renderer.SecurityGuardRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = SecurityGuardMod.MOD_ID, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Registration.SECURITY_GUARD.get(), SecurityGuardRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SecurityGuardModel.LAYER_LOCATION, SecurityGuardModel::createBodyLayer);
    }
}
```

- [ ] **Step 4: Create the placeholder texture**

The model uses a 64×64 texture sheet (standard for `HumanoidModel`). For the placeholder, generate a solid-colored 64×64 PNG.

ImageMagick:
```bash
mkdir -p securityguard/src/main/resources/assets/securityguard/textures/entity
magick -size 64x64 xc:'#162e5e' \
    securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png
```

Python:
```bash
mkdir -p securityguard/src/main/resources/assets/securityguard/textures/entity
python3 -c "from PIL import Image; Image.new('RGBA',(64,64),(22,46,94,255)).save('securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png')"
```

- [ ] **Step 5: Build**

```bash
./gradlew :securityguard:build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Run client, summon the guard, verify it renders**

```bash
./gradlew :securityguard:runClient
```

In a creative world: `/summon securityguard:guard`. The entity is now visible — a navy-blue humanoid with a cap-shape on its head. (It looks blocky and underdetailed; that's the placeholder texture — real art replaces this later.) Quit.

- [ ] **Step 7: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/client/ \
        securityguard/src/main/resources/assets/securityguard/textures/entity/
git commit -m "feat(securityguard): add humanoid model and renderer for Security Guard"
```

---

## Task 9: Baton Model + Render Layer

Renders a small baton model in the guard's right hand, attached to the right-arm `ModelPart` so it swings naturally with the arm during walk/attack animations.

**Files:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/client/model/BatonModel.java`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/client/renderer/BatonHeldLayer.java`
- Create: `securityguard/src/main/resources/assets/securityguard/textures/entity/baton.png` (placeholder)
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/client/renderer/SecurityGuardRenderer.java`
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/client/ClientSetup.java`

- [ ] **Step 1: Create `BatonModel.java`**

A single elongated cube (the baton). Built as its own model so the layer can render it independently of the entity model.

```java
package com.tweeks.securityguard.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tweeks.securityguard.SecurityGuardMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public class BatonModel {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SecurityGuardMod.MOD_ID, "baton"),
            "main");

    private final ModelPart root;

    public BatonModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        // 1x1x6 stick, anchored so the "handle" sits in the hand
        partRoot.addOrReplaceChild(
            "baton",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-0.5f, 0.0f, -0.5f, 1, 6, 1),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16);
    }

    public void render(PoseStack pose, VertexConsumer buffer, int packedLight, int packedOverlay) {
        root.render(pose, buffer, packedLight, packedOverlay);
    }
}
```

- [ ] **Step 2: Create `BatonHeldLayer.java`**

Attaches to the entity renderer, walks into the right-arm coordinate space during render, and draws the baton there.

```java
package com.tweeks.securityguard.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tweeks.securityguard.SecurityGuardMod;
import com.tweeks.securityguard.client.model.BatonModel;
import com.tweeks.securityguard.client.model.SecurityGuardModel;
import com.tweeks.securityguard.entity.SecurityGuardEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class BatonHeldLayer
        extends RenderLayer<SecurityGuardEntity, SecurityGuardModel> {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(SecurityGuardMod.MOD_ID, "textures/entity/baton.png");

    private final BatonModel batonModel;

    public BatonHeldLayer(RenderLayerParent<SecurityGuardEntity, SecurityGuardModel> parent,
                          EntityRendererProvider.Context context) {
        super(parent);
        this.batonModel = new BatonModel(context.bakeLayer(BatonModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack pose,
                       MultiBufferSource buffers,
                       int packedLight,
                       SecurityGuardEntity entity,
                       float limbSwing,
                       float limbSwingAmount,
                       float partialTicks,
                       float ageInTicks,
                       float netHeadYaw,
                       float headPitch) {
        pose.pushPose();
        // Walk into the right-arm's coordinate space so we inherit walk/attack rotation.
        getParentModel().rightArm.translateAndRotate(pose);

        // Move to the hand position at the bottom of the arm cube (arm is 12 tall in vanilla).
        pose.translate(-0.0625f, 0.625f, 0.0f);
        // Baton points straight down out of the fist (model is built upward; flip).
        pose.mulPose(Axis.XP.rotationDegrees(180.0f));

        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        batonModel.render(pose, buffer, packedLight, OverlayTexture.NO_OVERLAY);

        pose.popPose();
    }
}
```

- [ ] **Step 3: Add the layer in `SecurityGuardRenderer`**

Modify the constructor body:

```java
    public SecurityGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new SecurityGuardModel(context.bakeLayer(SecurityGuardModel.LAYER_LOCATION)), 0.5f);
        this.addLayer(new BatonHeldLayer(this, context));
    }
```

- [ ] **Step 4: Register the baton layer definition in `ClientSetup`**

Add this inside `registerLayerDefinitions`, below the existing call:

```java
        event.registerLayerDefinition(BatonModel.LAYER_LOCATION, BatonModel::createLayer);
```

Add the import:

```java
import com.tweeks.securityguard.client.model.BatonModel;
```

- [ ] **Step 5: Create the baton placeholder texture**

```bash
magick -size 16x16 xc:'#1e1e1e' \
    securityguard/src/main/resources/assets/securityguard/textures/entity/baton.png
```
Or with Python:
```bash
python3 -c "from PIL import Image; Image.new('RGBA',(16,16),(30,30,30,255)).save('securityguard/src/main/resources/assets/securityguard/textures/entity/baton.png')"
```

- [ ] **Step 6: Build**

```bash
./gradlew :securityguard:build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Run client and verify the baton renders**

```bash
./gradlew :securityguard:runClient
```

`/summon securityguard:guard`. A small dark stick should now hang from the guard's right hand. Walk around the guard — the baton swings with the arm during the guard's idle walk cycle. (If the baton appears in the wrong place — through the hand or floating — the translate values in `BatonHeldLayer` are off; tweak the `pose.translate` line. The values above are tested but humanoid arm pivots vary by `0.0625` increments.)

Quit.

- [ ] **Step 8: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/client/model/BatonModel.java \
        securityguard/src/main/java/com/tweeks/securityguard/client/renderer/BatonHeldLayer.java \
        securityguard/src/main/java/com/tweeks/securityguard/client/renderer/SecurityGuardRenderer.java \
        securityguard/src/main/java/com/tweeks/securityguard/client/ClientSetup.java \
        securityguard/src/main/resources/assets/securityguard/textures/entity/baton.png
git commit -m "feat(securityguard): render baton in guard's right hand via custom layer"
```

---

## Task 10: Helmet `useOn` — Spawn-on-Construct

Wires the helmet item to the spawn pattern. Uses `SpawnPattern.matches` from Task 5 (now exercised in real game world via the helmet).

**Files:**
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/item/GuardHelmetItem.java`

- [ ] **Step 1: Replace `GuardHelmetItem.java` with the full implementation**

```java
package com.tweeks.securityguard.item;

import com.tweeks.securityguard.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import com.tweeks.securityguard.entity.SecurityGuardEntity;

public class GuardHelmetItem extends Item {

    public GuardHelmetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos top = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        // 1. Pure-function pattern check (works on both client & server)
        if (!SpawnPattern.matches(level::getBlockState, top)) {
            return InteractionResult.PASS;
        }

        // 2. Permission check at every position we'd modify (respects spawn protection / WorldGuard)
        for (int dy = 0; dy >= -2; dy--) {
            BlockPos pos = top.offset(0, dy, 0);
            if (!level.mayInteract(player, pos)) {
                return InteractionResult.FAIL;
            }
        }

        // 3. Server-side world mutation only
        if (level instanceof ServerLevel serverLevel) {
            for (int dy = 0; dy >= -2; dy--) {
                serverLevel.setBlockAndUpdate(top.offset(0, dy, 0), Blocks.AIR.defaultBlockState());
            }

            BlockPos spawnAt = top.below(2);
            SecurityGuardEntity guard = Registration.SECURITY_GUARD.get().create(serverLevel);
            if (guard != null) {
                guard.moveTo(
                    spawnAt.getX() + 0.5,
                    spawnAt.getY(),
                    spawnAt.getZ() + 0.5,
                    player.getYRot() + 180.0f,  // face the player
                    0.0f);
                guard.setPlayerCreated(true);
                guard.finalizeSpawn(serverLevel,
                    serverLevel.getCurrentDifficultyAt(spawnAt),
                    MobSpawnType.MOB_SUMMONED, null);
                serverLevel.addFreshEntity(guard);

                serverLevel.playSound(null, spawnAt,
                    SoundEvents.IRON_GOLEM_REPAIR, SoundSource.BLOCKS, 1.0f, 1.0f);
            }

            ItemStack stack = ctx.getItemInHand();
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
```

- [ ] **Step 2: Build**

```bash
./gradlew :securityguard:build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run client, verify the construction recipe**

```bash
./gradlew :securityguard:runClient
```

In a creative world:
1. Place 3 iron blocks in a vertical column (jump-place 2, then place 1 more).
2. Take a Guard Helmet from the creative inventory.
3. Right-click on the top of the column with the helmet.

Expected: the 3 iron blocks vanish, an iron-golem-repair sound plays, a Security Guard appears at the base of the column facing you. Try a 2-block column → nothing happens, helmet not consumed. Try the recipe inside spawn protection (you can't easily test this in single-player; deferred to a multiplayer / `mayInteract` testing pass).

Quit.

- [ ] **Step 4: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/item/GuardHelmetItem.java
git commit -m "feat(securityguard): spawn Security Guard on Guard Helmet construction"
```

---

## Task 11: BatonStrikeGoal — Stun + Knockback Combat

Replaces the inherited iron-golem melee goal with a stun-applying version. Iron golem's default `MeleeAttackGoal` is registered in `IronGolem.registerGoals`, so we override `registerGoals` in our entity to swap it out.

**Files:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/entity/ai/BatonStrikeGoal.java`
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/entity/SecurityGuardEntity.java`

- [ ] **Step 1: Create `BatonStrikeGoal.java`**

```java
package com.tweeks.securityguard.entity.ai;

import com.tweeks.securityguard.entity.SecurityGuardEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Melee goal for the Security Guard. Standard reach + cooldown behavior, but each
 * landed hit applies Slowness II + Weakness I for 3 seconds and a small explicit
 * knockback to "reset" the target's momentum so the slowness feels visible.
 */
public class BatonStrikeGoal extends MeleeAttackGoal {

    private static final int STUN_DURATION_TICKS = 60;       // 3 seconds
    private static final int SLOWNESS_AMPLIFIER = 1;          // Slowness II
    private static final int WEAKNESS_AMPLIFIER = 0;          // Weakness I
    private static final double KNOCKBACK_STRENGTH = 0.2;

    public BatonStrikeGoal(SecurityGuardEntity guard) {
        // speedModifier=1.0, followingTargetEvenIfNotSeen=true (matches iron golem)
        super(guard, 1.0, true);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        // Use vanilla cooldown + range gating
        if (this.canPerformAttack(target)) {
            this.resetAttackCooldown();
            this.mob.swing(this.mob.getUsedItemHand());
            this.mob.doHurtTarget(target);

            // Apply stun + extra knockback only if the target is still alive after the hit
            if (target.isAlive()) {
                target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, STUN_DURATION_TICKS, SLOWNESS_AMPLIFIER));
                target.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, STUN_DURATION_TICKS, WEAKNESS_AMPLIFIER));
                target.knockback(
                    KNOCKBACK_STRENGTH,
                    this.mob.getX() - target.getX(),
                    this.mob.getZ() - target.getZ());
            }
        }
    }
}
```

- [ ] **Step 2: Override `registerGoals` and tweak attack interval in `SecurityGuardEntity`**

Add these methods to `SecurityGuardEntity`:

```java
    @Override
    protected void registerGoals() {
        // Re-add iron golem's full goal set, but swap MeleeAttackGoal for ours.
        this.goalSelector.addGoal(1, new com.tweeks.securityguard.entity.ai.BatonStrikeGoal(this));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal(this, 0.9, 32.0f));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal(this, 0.6, false));
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal(this, 0.6));
        this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.OfferFlowerGoal(this));
        this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this,
            net.minecraft.world.entity.player.Player.class, 6.0f));
        this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal(this));
        this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new com.tweeks.securityguard.entity.SecurityGuardEntity.GuardTargetHostilesGoal(this));
        this.targetSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.ResetUniversalAngerTargetGoal<>(this, false));
    }

    /**
     * Targets any {@link net.minecraft.world.entity.Mob} that implements {@link net.minecraft.world.entity.monster.Enemy}
     * (zombies, skeletons, spiders, pillagers, etc.) within follow range. Excludes creepers because aggro-ing one at melee
     * range would just create a TNT trap that kills the guard, the player, and the village it's defending.
     *
     * <p>Player-targeting (for players with bad village reputation) comes from {@link net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal}
     * + the village reputation/anger system, not from this goal.
     */
    public static class GuardTargetHostilesGoal
            extends net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<net.minecraft.world.entity.Mob> {
        public GuardTargetHostilesGoal(SecurityGuardEntity guard) {
            super(guard, net.minecraft.world.entity.Mob.class, 5, false, false,
                (target, level) -> target instanceof net.minecraft.world.entity.monster.Enemy
                                && !(target instanceof net.minecraft.world.entity.monster.Creeper));
        }
    }
```

(Creeper is excluded because aggro-ing a creeper at melee range would be a TNT trap. If you want creepers in scope, drop the exclusion in the predicate.)

- [ ] **Step 3: Build**

```bash
./gradlew :securityguard:build
```

Expected: `BUILD SUCCESSFUL`. If it fails on missing `MoveBackToVillageGoal` / `GolemRandomStrollInVillageGoal`, those classes do exist in MC 1.21.1 — verify the import path matches your NeoForge / mappings setup.

- [ ] **Step 4: Manual combat test in client**

```bash
./gradlew :securityguard:runClient
```

In a creative world:
1. `/summon securityguard:guard`
2. `/summon zombie ~ ~ ~5` — spawn a zombie 5 blocks away
3. Watch.

Expected: the guard turns to the zombie, walks to it, swings the right arm, and on each hit you see slowness particles (gray swirls) and weakness particles (gray X) on the zombie. The zombie stays slowed and is dead within 5-10 hits.

Then: hit the guard with your sword once. Expected: nothing — the guard does not retaliate against players (player-created flag from Task 10 + iron-golem default behavior). Hit a villager nearby and run — the guard chases you. (`HurtByTargetGoal` chains via the `DefendVillageTargetGoal`.)

Quit.

- [ ] **Step 5: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/entity/ai/BatonStrikeGoal.java \
        securityguard/src/main/java/com/tweeks/securityguard/entity/SecurityGuardEntity.java
git commit -m "feat(securityguard): add stun-on-hit baton combat with custom AI goal set"
```

---

## Task 12: Sound Events

Registers sound events that map to villager sounds with a pitch shift. These are registered as standalone sound events (not overridden vanilla events) so they can be remapped to custom audio in v2 without breaking existing maps.

**Files:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/sound/ModSounds.java`
- Create: `securityguard/src/main/resources/assets/securityguard/sounds.json`
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/Registration.java`
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/entity/SecurityGuardEntity.java`

- [ ] **Step 1: Create `ModSounds.java`**

```java
package com.tweeks.securityguard.sound;

import com.tweeks.securityguard.SecurityGuardMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModSounds {
    private ModSounds() {}

    public static DeferredHolder<SoundEvent, SoundEvent> AMBIENT;
    public static DeferredHolder<SoundEvent, SoundEvent> HURT;
    public static DeferredHolder<SoundEvent, SoundEvent> DEATH;

    public static void register(DeferredRegister<SoundEvent> registry) {
        AMBIENT = registry.register("guard_ambient", soundEvent("guard_ambient"));
        HURT    = registry.register("guard_hurt",    soundEvent("guard_hurt"));
        DEATH   = registry.register("guard_death",   soundEvent("guard_death"));
    }

    private static Supplier<SoundEvent> soundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SecurityGuardMod.MOD_ID, name);
        return () -> SoundEvent.createVariableRangeEvent(id);
    }
}
```

- [ ] **Step 2: Wire `ModSounds.register` into `Registration.register`**

Modify the `register` method in `Registration.java` to call sound registration before returning:

```java
    public static void register(IEventBus modEventBus) {
        com.tweeks.securityguard.sound.ModSounds.register(SOUND_EVENTS);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
```

(The `ModSounds.register` call must run before `SOUND_EVENTS.register(modEventBus)` so that the deferred objects are populated when the mod-bus event fires.)

- [ ] **Step 3: Create `securityguard/src/main/resources/assets/securityguard/sounds.json`**

This file maps each registered sound event to a vanilla audio file with a pitch shift. NeoForge has no JSON-based pitch override per-sound-entry in 1.21, so we instead reference the same `.ogg` files vanilla uses; pitch is applied at the call site (overridden in entity sound methods below).

```json
{
  "guard_ambient": {
    "subtitle": "subtitles.entity.villager.ambient",
    "sounds": [
      "minecraft:entity/villager/idle1",
      "minecraft:entity/villager/idle2",
      "minecraft:entity/villager/idle3"
    ]
  },
  "guard_hurt": {
    "subtitle": "subtitles.entity.villager.hurt",
    "sounds": [
      "minecraft:entity/villager/hit1",
      "minecraft:entity/villager/hit2",
      "minecraft:entity/villager/hit3",
      "minecraft:entity/villager/hit4"
    ]
  },
  "guard_death": {
    "subtitle": "subtitles.entity.villager.death",
    "sounds": [
      "minecraft:entity/villager/death"
    ]
  }
}
```

- [ ] **Step 4: Wire sound events into `SecurityGuardEntity`**

Add these methods:

```java
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return com.tweeks.securityguard.sound.ModSounds.AMBIENT.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return com.tweeks.securityguard.sound.ModSounds.HURT.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return com.tweeks.securityguard.sound.ModSounds.DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        // Slightly lower than default villager (which uses 1.0 ± 0.2 random) → "tougher" voice
        return 0.85f * super.getVoicePitch();
    }
```

- [ ] **Step 5: Build**

```bash
./gradlew :securityguard:build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Run client, verify sounds**

```bash
./gradlew :securityguard:runClient
```

Spawn a guard, stand near it. After ~10-30 seconds it makes pitch-shifted villager idle noises. Hit it with your sword (it won't retaliate but will play a pitch-shifted hurt grunt). Kill it; pitch-shifted death sound plays. Quit.

- [ ] **Step 7: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/sound/ \
        securityguard/src/main/resources/assets/securityguard/sounds.json \
        securityguard/src/main/java/com/tweeks/securityguard/Registration.java \
        securityguard/src/main/java/com/tweeks/securityguard/entity/SecurityGuardEntity.java
git commit -m "feat(securityguard): wire pitch-shifted villager sounds for the guard"
```

---

## Task 13: Datagen — Recipe, Loot Table, Language

Replaces hand-written JSON with Java data providers run via `./gradlew runData`. Means future MC version bumps only require fixing Java, not JSONs.

**Files:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/data/DataGenerators.java`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/data/ModRecipeProvider.java`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/data/ModEntityLootProvider.java`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/data/ModLanguageProvider.java`

- [ ] **Step 1: Create `DataGenerators.java`**

```java
package com.tweeks.securityguard.data;

import com.tweeks.securityguard.SecurityGuardMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = SecurityGuardMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        gen.addProvider(event.includeServer(), new ModRecipeProvider(output, lookup));

        gen.addProvider(event.includeServer(), new LootTableProvider(
            output,
            Set.of(),
            List.of(new LootTableProvider.SubProviderEntry(
                ModEntityLootProvider::new, LootContextParamSets.ENTITY)),
            lookup));

        gen.addProvider(event.includeClient(), new ModLanguageProvider(output));
    }
}
```

- [ ] **Step 2: Create `ModRecipeProvider.java`**

```java
package com.tweeks.securityguard.data;

import com.tweeks.securityguard.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void buildRecipes(RecipeOutput out) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Registration.GUARD_HELMET.get())
            .pattern(" I ")
            .pattern("IDI")
            .define('I', Items.IRON_INGOT)
            .define('D', Items.BLUE_DYE)
            .unlockedBy("has_iron", has(Items.IRON_INGOT))
            .save(out);
    }
}
```

- [ ] **Step 3: Create `ModEntityLootProvider.java`**

The guard drops nothing (per spec — prevents iron farms). We still register an empty loot table so MC doesn't log a warning.

```java
package com.tweeks.securityguard.data;

import com.tweeks.securityguard.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.stream.Stream;

public class ModEntityLootProvider extends EntityLootSubProvider {

    public ModEntityLootProvider(HolderLookup.Provider lookup) {
        super(FeatureFlags.REGISTRY.allFlags(), lookup);
    }

    @Override
    public void generate() {
        // Empty loot table — guard drops nothing on death.
        this.add(Registration.SECURITY_GUARD.get(), LootTable.lootTable());
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return BuiltInRegistries.ENTITY_TYPE.stream()
            .filter(t -> t == Registration.SECURITY_GUARD.get());
    }
}
```

- [ ] **Step 4: Create `ModLanguageProvider.java`**

```java
package com.tweeks.securityguard.data;

import com.tweeks.securityguard.Registration;
import com.tweeks.securityguard.SecurityGuardMod;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(output, SecurityGuardMod.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + SecurityGuardMod.MOD_ID, "Security Guard");
        add(Registration.GUARD_HELMET.get(), "Guard Helmet");
        add(Registration.GUARD_SPAWN_EGG.get(), "Security Guard Spawn Egg");
        add(Registration.SECURITY_GUARD.get(), "Security Guard");

        // Subtitles for sounds (referenced by sounds.json — currently mapped to vanilla
        // villager subtitles; if you want guard-specific subtitles, also override here)
    }
}
```

- [ ] **Step 5: Run datagen**

```bash
./gradlew :securityguard:runData
```

Expected: `BUILD SUCCESSFUL`. New files appear under `securityguard/src/generated/resources/`:
- `data/securityguard/recipe/guard_helmet.json`
- `data/securityguard/loot_table/entities/guard.json`
- `assets/securityguard/lang/en_us.json`

Inspect them — they should look like normal MC data files.

- [ ] **Step 6: Run client and verify the recipe + names**

```bash
./gradlew :securityguard:runClient
```

In a survival world:
1. Open the recipe book — Guard Helmet should appear under crafting recipes (you may need iron ingot in your inventory for it to show as "available").
2. Craft it (1 iron ingot, 1 blue dye, 2 more iron ingots in the pattern from the spec).
3. Open the creative tab — title now reads "Security Guard" instead of the raw key.
4. Inventory tooltips show "Guard Helmet", "Security Guard Spawn Egg", and entity name on summon shows "Security Guard".

Quit.

- [ ] **Step 7: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/data/ \
        securityguard/src/generated/
git commit -m "feat(securityguard): add datagen for recipe, loot table, and translations"
```

---

## Task 14: Final Verification Pass

Walk through every spec requirement in a single dev-client session. Captures regressions accumulated over the prior tasks before declaring v1 done.

**Files:**
- None (manual testing only)

- [ ] **Step 1: Launch dev client**

```bash
./gradlew :securityguard:runClient
```

- [ ] **Step 2: Run the verification checklist**

Walk through each item in a creative world (then switch to survival for the recipe one):

- [ ] Mod appears in the title screen `Mods` list with name "Security Guard" and version 0.1.0.
- [ ] Creative tab "Security Guard" exists, icon is the Guard Helmet, contains Guard Helmet + Spawn Egg.
- [ ] Item names display as "Guard Helmet" and "Security Guard Spawn Egg".
- [ ] Spawn egg spawns a visible Security Guard with cap and baton.
- [ ] Construction recipe (3 iron blocks + Guard Helmet on top) spawns a guard, consumes the materials, plays sound.
- [ ] Construction with only 2 iron blocks does nothing (helmet not consumed).
- [ ] **Ceiling test**: build a 3-iron-block column with a stone ceiling 1 block above — right-clicking with the helmet does nothing, helmet not consumed (verifies `SpawnPattern`'s air-clearance check prevents suffocation-on-spawn).
- [ ] Right-click the guard with an iron ingot — it heals (inherited `IronGolem` repair behavior, intentionally kept for v1).
- [ ] Guard idles, makes occasional pitch-shifted villager noises.
- [ ] Spawn a zombie nearby — guard targets it, swings baton, zombie gets slowness + weakness particles, dies in a few hits.
- [ ] Guard does not attack the player (try hitting it with a sword — no retaliation).
- [ ] Guard chases the player who attacks a villager.
- [ ] Hit guard until it dies — death sound plays, no item drops.
- [ ] Reload the world — surviving guard is still present at its last position.
- [ ] In survival mode: craft Guard Helmet using 4 iron ingots + 1 blue dye in the recipe pattern.

- [ ] **Step 3: If anything fails, file a follow-up task. If everything passes, tag v0.1.0**

```bash
git tag -a v0.1.0 -m "Security Guard mod v0.1.0"
```

- [ ] **Step 4: Build the release jar**

```bash
./gradlew :securityguard:build
```

Verify `securityguard/build/libs/securityguard-0.1.0.jar` exists. This is the shippable mod.

- [ ] **Step 5: Final commit + tag push (if applicable)**

```bash
git status  # confirm clean
# Already tagged in step 3; if there's a remote, push:
# git push origin main --tags
```

---

## Spec Coverage Map

Every spec section/requirement maps to one or more tasks above:

| Spec section | Tasks |
|---|---|
| Project / repo layout | 1, 2 |
| Mod identity (id, NeoForge, 1.21.x) | 2 |
| Entity stats | 6 (attributes), 11 (combat) |
| Combat: stun + knockback | 11 |
| Targeting (iron-golem rules) | 11 |
| Persistence & no-drops | 6 (default), 13 (empty loot table) |
| Appearance, model, cap | 8 |
| Animations (walk, attack swing) | 8 (model), 11 (swing trigger) |
| Sounds | 12 |
| Construction recipe (3 iron + helmet) | 5 (pattern), 10 (useOn) |
| Guard Helmet crafting recipe | 13 |
| Guard Helmet not equippable in v1 | 4 (item construction with no Equippable component) |
| Spawn egg | 7 |
| Baton (no item, render-layer only) | 9 |
| Creative tab | 3, 4, 7 |
| `mayInteract` protection compatibility | 10 |
| `setPlayerCreated(true)` | 10 |
| Effects auto-sync (no custom packets) | 11 (relies on standard `addEffect`) |
| Unit testing of pure logic | 5 |
| Manual integration test checklist | 14 |
| GameTest (optional) | (deferred — out of v1 scope per spec) |
| Datagen for recipe / loot / lang | 13 |
| Java 21, Gradle, NeoForge 1.21.1 | 2 |
| MIT license | 2 (`mod_license` property) |
| Out-of-scope items (player baton, armor variants, structures, Bedrock, multi-lang) | (intentionally absent from plan) |
