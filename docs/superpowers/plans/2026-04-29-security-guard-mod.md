# Security Guard Mod Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship v1 of the Security Guard mod — a friendly humanoid mob for NeoForge MC 26.1.2 that defends players and villagers using stun-on-hit baton attacks, deployable via a 3-iron-block + Guard Helmet construction recipe.

**Architecture:** Single Gradle subproject (`securityguard/`) under a multi-module root that will host the future mod series. Entity extends `IronGolem` for friendly-mob defaults + village reputation hooks; AI uses standard Minecraft goal system with a custom `BatonStrikeGoal` swapped in; baton is rendered via a custom `RenderLayer` (no item registration). Spawn logic is extracted into a pure-function `SpawnPattern` utility for unit-testability without a Minecraft runtime.

**Tech Stack:** **Java 25**, **Minecraft 26.1.2**, **NeoForge 26.1.2.30-beta**, **ModDevGradle 2.0.141**, **Gradle 9.2.1** (via wrapper), JUnit 5 for unit tests, NeoForge GameTest framework for optional in-game tests.

**Note on API drift:** The plan was originally drafted against MC 1.21.x but has been **rewritten for MC 26.1.2** below. Tasks 1–7 are already executed (see git commits); tasks 8–13 contain 26.1.2-correct code.

## 26.1.2 API Reference Sheet

The renames and pattern shifts that diverge from older NeoForge tutorials:

| 1.21.x name / pattern | 26.1.2 equivalent |
|---|---|
| `net.minecraft.resources.ResourceLocation` | `net.minecraft.resources.Identifier` |
| `net.minecraft.world.entity.animal.IronGolem` | `net.minecraft.world.entity.animal.golem.IronGolem` |
| `EntityType.Builder.build("modid:name")` (string) | `EntityType.Builder.build(ResourceKey<EntityType<?>>)` |
| `new SpawnEggItem(entityType, primary, secondary, props)` | `new SpawnEggItem(new Item.Properties().spawnEgg(entityType))` — colors baked into the texture |
| Item asset path: only `assets/<ns>/models/item/<id>.json` | **Two** files now: `assets/<ns>/items/<id>.json` (item-model definition) + `assets/<ns>/models/item/<id>.json` (geometry + texture) |
| `MobSpawnType.MOB_SUMMONED` | `EntitySpawnReason.MOB_SUMMONED` (also: `Mob.finalizeSpawn(...)` takes `EntitySpawnReason`) |
| `extends MobRenderer<Entity, Model>` (2-arg generic) | `extends MobRenderer<Entity, RenderState, Model>` (3-arg). Renderers now have a pre-extracted `RenderState` class. |
| `extends HumanoidModel<Entity>` | `extends HumanoidModel<S extends HumanoidRenderState>` (parameterized over render state, not entity) |
| `RenderLayer<T, M>.render(PoseStack, MultiBufferSource, int, T, ...)` | `RenderLayer<S, M>.submit(PoseStack, SubmitNodeCollector, int lightCoords, S state, float yRot, float xRot)` |
| `MultiBufferSource.getBuffer(RenderType)` then `model.renderToBuffer(...)` | `submitNodeCollector.submitModel(model, state, poseStack, Identifier texture, lightCoords, packedOverlay, color, null)` |
| Renderer needs `getTextureLocation(Entity)` | Renderer needs `getTextureLocation(S state)` + `createRenderState()` (returns a fresh state instance) |
| Animation: `model.setupAnim(entity, ...)` | Animation: `model.setupAnim(S state)` — driven from the pre-extracted state |
| `Item.Properties()` setters chained directly | Same, plus new component-based methods: `.spawnEgg(EntityType)`, `.component(...)`, `.food(props, consumable)` |

**`pack_format`:** 26.1.2 doesn't use a top-level `pack.mcmeta` in mod resources — NeoForge handles it.

## Execution Status

- ✅ **Task 1** (commit `4b041b8`) — Root Gradle multi-module setup
- ✅ **Task 2** (commit `42f6197`) — NeoForge 26.1.2 MDK skeleton (rebased from 1.21.x)
- ✅ **Task 3** (commit `053d1e9`) — Registration + creative tab
- ✅ **Task 4** (commit `67a7b1e`) — Guard Helmet item
- ✅ **Task 5** (commit `6e8220f`) — SpawnPattern + unit tests (predicate-based to avoid MC bootstrap)
- ✅ **Task 6** (commit `e04c505`) — Entity registration
- ✅ **Task 7** (commit `8d265b8`) — Spawn egg
- ⏳ **Task 8** — Renderer + model (rewritten below)
- ⏳ **Task 9** — Baton render layer (rewritten below)
- ⏳ **Task 10** — Helmet useOn spawn logic (rewritten below)
- ⏳ **Task 11** — BatonStrikeGoal combat AI (rewritten below)
- ⏳ **Task 12** — Sound events (rewritten below)
- ⏳ **Task 13** — Datagen (rewritten below)
- ⏳ **Task 14** — Final verification (manual; user runs `./gradlew runClient`)

The original Tasks 1-7 below contain 1.21.x-style snippets for historical reference; the actual committed code uses the 26.1.2 API.

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
- **Java 25 JDK** installed (`java -version` should report 25+; via Homebrew: `brew install openjdk` installs the latest, then add `/opt/homebrew/opt/openjdk/bin` to PATH)
- Git
- About 4 GB of disk for Gradle cache + MC dev artifacts
- Internet for the first Gradle build (downloads NeoForge MDK dependencies)
- ImageMagick OR Python 3 with Pillow OR a 16×16 PNG editor (for placeholder textures)
- **No need to install Gradle separately** — the NeoForge MDK ships with the Gradle wrapper (`./gradlew`)

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


# === REWRITTEN FOR MC 26.1.2 BELOW ===

The tasks below replace the 1.21.x-style snippets that originally followed Task 7. Use these for execution.

---

## Task 8: Entity Renderer + Humanoid Model (No Baton Yet) — 26.1.2

Makes the entity visible. Humanoid model with an extra "cap" cube on the head. Uses the modern `RenderState` pattern: `HumanoidRenderState` is sufficient for our purposes (no extra state to track), so we don't define a custom render state class.

**Files:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/client/ClientSetup.java`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/client/model/SecurityGuardModel.java`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/client/renderer/SecurityGuardRenderer.java`
- Create: `securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png` (placeholder)

- [ ] **Step 1: Create `SecurityGuardModel.java`**

```java
package com.tweeks.securityguard.client.model;

import com.tweeks.securityguard.SecurityGuardMod;
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

public class SecurityGuardModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(SecurityGuardMod.MOD_ID, "guard"),
        "main");

    public SecurityGuardModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        // Cap brim (thin disc on top of the head)
        head.addOrReplaceChild("cap_brim",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.5f, -9.0f, -4.5f, 9, 1, 9),
            PartPose.ZERO);
        // Cap crown (shorter cube on top of the brim)
        head.addOrReplaceChild("cap_crown",
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
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class SecurityGuardRenderer
        extends HumanoidMobRenderer<SecurityGuardEntity, HumanoidRenderState, SecurityGuardModel> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        SecurityGuardMod.MOD_ID, "textures/entity/security_guard.png");

    public SecurityGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new SecurityGuardModel(context.bakeLayer(SecurityGuardModel.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
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

- [ ] **Step 4: Create the placeholder 64×64 texture**

```bash
python3 -c "from PIL import Image; Image.new('RGBA',(64,64),(22,46,94,255)).save('securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png')"
```

- [ ] **Step 5: Build to verify it compiles**

```bash
./securityguard/gradlew :securityguard:build --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/client/ \
        securityguard/src/main/resources/assets/securityguard/textures/entity/
git commit -m "feat(securityguard): add humanoid model and renderer"
```

---

## Task 9: Baton Model + Render Layer — 26.1.2

Renders a baton in the guard's right hand via a custom `RenderLayer`. Uses the new `submit(...)` API and `SubmitNodeCollector.submitModel(...)` for queueing the draw.

**Files:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/client/model/BatonModel.java`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/client/renderer/BatonHeldLayer.java`
- Create: `securityguard/src/main/resources/assets/securityguard/textures/entity/baton.png`
- Modify: `SecurityGuardRenderer.java` (add the layer)
- Modify: `ClientSetup.java` (register baton layer definition)

- [ ] **Step 1: Create `BatonModel.java`**

```java
package com.tweeks.securityguard.client.model;

import com.tweeks.securityguard.SecurityGuardMod;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class BatonModel extends Model<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(SecurityGuardMod.MOD_ID, "baton"),
        "main");

    public BatonModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 1x1x6 stick, anchored at the handle
        root.addOrReplaceChild("baton",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-0.5f, 0.0f, -0.5f, 1, 6, 1),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16);
    }
}
```

- [ ] **Step 2: Create `BatonHeldLayer.java`**

```java
package com.tweeks.securityguard.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tweeks.securityguard.SecurityGuardMod;
import com.tweeks.securityguard.client.model.BatonModel;
import com.tweeks.securityguard.client.model.SecurityGuardModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class BatonHeldLayer extends RenderLayer<HumanoidRenderState, SecurityGuardModel> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        SecurityGuardMod.MOD_ID, "textures/entity/baton.png");

    private final BatonModel batonModel;

    public BatonHeldLayer(RenderLayerParent<HumanoidRenderState, SecurityGuardModel> parent,
                          EntityRendererProvider.Context context) {
        super(parent);
        this.batonModel = new BatonModel(context.bakeLayer(BatonModel.LAYER_LOCATION));
    }

    @Override
    public void submit(PoseStack pose,
                       SubmitNodeCollector collector,
                       int lightCoords,
                       HumanoidRenderState state,
                       float yRot,
                       float xRot) {
        pose.pushPose();
        // Walk into the right-arm coordinate space so the baton inherits walk/attack rotation.
        getParentModel().rightArm.translateAndRotate(pose);
        // Move to fist position at the bottom of the arm cube.
        pose.translate(-0.0625f, 0.625f, 0.0f);
        // Baton model is built upward; flip 180° so it points down out of the fist.
        pose.mulPose(Axis.XP.rotationDegrees(180.0f));

        collector.submitModel(
            batonModel,
            state,
            pose,
            TEXTURE,
            lightCoords,
            OverlayTexture.NO_OVERLAY,
            -1,    // color (white = no tint)
            null   // crumbling overlay (none)
        );

        pose.popPose();
    }
}
```

- [ ] **Step 3: Update `SecurityGuardRenderer` to add the layer**

Modify the constructor:

```java
    public SecurityGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new SecurityGuardModel(context.bakeLayer(SecurityGuardModel.LAYER_LOCATION)), 0.5f);
        this.addLayer(new BatonHeldLayer(this, context));
    }
```

Add the import:
```java
import com.tweeks.securityguard.client.renderer.BatonHeldLayer;
```

(self-import isn't needed if `BatonHeldLayer` is in the same package as `SecurityGuardRenderer`.)

- [ ] **Step 4: Update `ClientSetup` to register the baton layer**

Add inside `registerLayerDefinitions`:

```java
        event.registerLayerDefinition(BatonModel.LAYER_LOCATION, BatonModel::createLayer);
```

Add the import:
```java
import com.tweeks.securityguard.client.model.BatonModel;
```

- [ ] **Step 5: Create the baton texture**

```bash
python3 -c "from PIL import Image; Image.new('RGBA',(16,16),(30,30,30,255)).save('securityguard/src/main/resources/assets/securityguard/textures/entity/baton.png')"
```

- [ ] **Step 6: Build**

```bash
./securityguard/gradlew :securityguard:build --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/client/ \
        securityguard/src/main/resources/assets/securityguard/textures/entity/baton.png
git commit -m "feat(securityguard): render baton via custom RenderLayer (26.1.2 submit API)"
```

---

## Task 10: Helmet `useOn` — Spawn-on-Construct — 26.1.2

Replaces the stub `GuardHelmetItem.useOn` with the full pattern-detect + spawn flow. Uses `EntitySpawnReason.MOB_SUMMONED` (the renamed `MobSpawnType`).

**Files:**
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/item/GuardHelmetItem.java`

- [ ] **Step 1: Replace `GuardHelmetItem.java` with the full implementation**

```java
package com.tweeks.securityguard.item;

import com.tweeks.securityguard.Registration;
import com.tweeks.securityguard.entity.SecurityGuardEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

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

        // 1. Pure-function pattern check (works on both client and server)
        if (!SpawnPattern.matches(
                pos -> level.getBlockState(pos).is(Blocks.IRON_BLOCK),
                pos -> level.getBlockState(pos).isAir(),
                top)) {
            return InteractionResult.PASS;
        }

        // 2. Permission check at every position we'd modify (respects spawn protection)
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
            SecurityGuardEntity guard = Registration.SECURITY_GUARD.get().create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
            if (guard != null) {
                guard.moveTo(
                    spawnAt.getX() + 0.5,
                    spawnAt.getY(),
                    spawnAt.getZ() + 0.5,
                    player.getYRot() + 180.0f,
                    0.0f);
                guard.setPlayerCreated(true);
                guard.finalizeSpawn(serverLevel,
                    serverLevel.getCurrentDifficultyAt(spawnAt),
                    EntitySpawnReason.MOB_SUMMONED, null);
                serverLevel.addFreshEntity(guard);

                serverLevel.playSound(null, spawnAt,
                    SoundEvents.IRON_GOLEM_REPAIR, SoundSource.BLOCKS, 1.0f, 1.0f);
            }

            ItemStack stack = ctx.getItemInHand();
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
```

(Note: the predicate-based `SpawnPattern.matches` was set up in Task 5 to take two `Predicate<BlockPos>` args.)

- [ ] **Step 2: Build**

```bash
./securityguard/gradlew :securityguard:build --no-daemon
```

Expected: `BUILD SUCCESSFUL`. If `EntityType.create(level, EntitySpawnReason)` doesn't compile, fall back to `Registration.SECURITY_GUARD.get().create(serverLevel)` (single-arg overload) — vanilla still keeps it.

- [ ] **Step 3: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/item/GuardHelmetItem.java
git commit -m "feat(securityguard): spawn Security Guard on Guard Helmet construction"
```

---

## Task 11: BatonStrikeGoal — Stun + Knockback Combat — 26.1.2

Custom melee goal applies Slowness II + Weakness I + small knockback on hit. AI goal classes are still in `net.minecraft.world.entity.ai.goal`/`ai.goal.target` (no rename).

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

public class BatonStrikeGoal extends MeleeAttackGoal {

    private static final int STUN_DURATION_TICKS = 60;       // 3 seconds
    private static final int SLOWNESS_AMPLIFIER = 1;          // Slowness II
    private static final int WEAKNESS_AMPLIFIER = 0;          // Weakness I
    private static final double KNOCKBACK_STRENGTH = 0.2;

    public BatonStrikeGoal(SecurityGuardEntity guard) {
        super(guard, 1.0, true);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        if (this.canPerformAttack(target)) {
            this.resetAttackCooldown();
            this.mob.swing(this.mob.getUsedItemHand());
            this.mob.doHurtTarget((net.minecraft.server.level.ServerLevel) this.mob.level(), target);

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

(Note: in 26.1.x, `Mob.doHurtTarget` requires a `ServerLevel` argument as the first parameter. Cast `this.mob.level()` to `ServerLevel` since this goal only runs server-side. If the cast fails on a build, fall back to `this.mob.doHurtTarget(target)` if that overload still exists.)

- [ ] **Step 2: Override `registerGoals` in `SecurityGuardEntity`**

Add to the entity class:

```java
    @Override
    protected void registerGoals() {
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
        this.targetSelector.addGoal(3, new GuardTargetHostilesGoal(this));
        this.targetSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal<>(this, false));
    }

    /** Targets hostile mobs (Mob+Enemy) within follow range, except creepers. */
    public static class GuardTargetHostilesGoal
            extends net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<net.minecraft.world.entity.Mob> {
        public GuardTargetHostilesGoal(SecurityGuardEntity guard) {
            super(guard, net.minecraft.world.entity.Mob.class, 5, false, false,
                (target, level) -> target instanceof net.minecraft.world.entity.monster.Enemy
                                && !(target instanceof net.minecraft.world.entity.monster.Creeper));
        }
    }
```

- [ ] **Step 3: Build**

```bash
./securityguard/gradlew :securityguard:build --no-daemon
```

Expected: `BUILD SUCCESSFUL`. If `MoveBackToVillageGoal` / `GolemRandomStrollInVillageGoal` / `OfferFlowerGoal` got moved into the `golem` subpackage to match `IronGolem`'s relocation, fix the imports.

- [ ] **Step 4: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/entity/
git commit -m "feat(securityguard): add stun-on-hit baton combat goal"
```

---

## Task 12: Sound Events — 26.1.2

Registers sound events that map to villager sounds with a pitch shift (via `getVoicePitch()` override). API is unchanged from 1.21.x except `Identifier` instead of `ResourceLocation`.

**Files:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/sound/ModSounds.java`
- Create: `securityguard/src/main/resources/assets/securityguard/sounds.json`
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/Registration.java`
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/entity/SecurityGuardEntity.java`

- [ ] **Step 1: Create `ModSounds.java`**

```java
package com.tweeks.securityguard.sound;

import com.tweeks.securityguard.SecurityGuardMod;
import net.minecraft.resources.Identifier;
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
        Identifier id = Identifier.fromNamespaceAndPath(SecurityGuardMod.MOD_ID, name);
        return () -> SoundEvent.createVariableRangeEvent(id);
    }
}
```

- [ ] **Step 2: Wire `ModSounds.register` into `Registration.register`**

In `Registration.java`, modify the `register` method:

```java
    public static void register(IEventBus modEventBus) {
        com.tweeks.securityguard.sound.ModSounds.register(SOUND_EVENTS);
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
```

- [ ] **Step 3: Create `sounds.json`**

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

Save to `securityguard/src/main/resources/assets/securityguard/sounds.json`.

- [ ] **Step 4: Wire sound events into `SecurityGuardEntity`**

Add these overrides to the entity class:

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
        return 0.85f * super.getVoicePitch();
    }
```

- [ ] **Step 5: Build + commit**

```bash
./securityguard/gradlew :securityguard:build --no-daemon
git add securityguard/src/main/java/com/tweeks/securityguard/sound/ \
        securityguard/src/main/resources/assets/securityguard/sounds.json \
        securityguard/src/main/java/com/tweeks/securityguard/Registration.java \
        securityguard/src/main/java/com/tweeks/securityguard/entity/SecurityGuardEntity.java
git commit -m "feat(securityguard): wire pitch-shifted villager sounds for the guard"
```

---

## Task 13: Datagen — Recipe, Loot Table, Language — 26.1.2

The 26.1.2 `RecipeProvider` API uses a `Runner` inner class pattern (the provider takes its own `RecipeOutput` rather than receiving one in `buildRecipes`).

**Files:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/data/DataGenerators.java`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/data/ModRecipeProvider.java`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/data/ModEntityLootProvider.java`
- Create: `securityguard/src/main/java/com/tweeks/securityguard/data/ModLanguageProvider.java`

- [ ] **Step 1: Create `ModRecipeProvider.java`**

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

    public ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        ShapedRecipeBuilder.shaped(this.registries, RecipeCategory.MISC, Registration.GUARD_HELMET.get())
            .pattern(" I ")
            .pattern("IDI")
            .define('I', Items.IRON_INGOT)
            .define('D', Items.BLUE_DYE)
            .unlockedBy("has_iron", this.has(Items.IRON_INGOT))
            .save(this.output);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ModRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Security Guard Recipes";
        }
    }
}
```

- [ ] **Step 2: Create `ModEntityLootProvider.java`**

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
        this.add(Registration.SECURITY_GUARD.get(), LootTable.lootTable());
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return BuiltInRegistries.ENTITY_TYPE.stream()
            .filter(t -> t == Registration.SECURITY_GUARD.get());
    }
}
```

- [ ] **Step 3: Create `ModLanguageProvider.java`**

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
    }
}
```

- [ ] **Step 4: Create `DataGenerators.java`**

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

        gen.addProvider(event.includeServer(), new ModRecipeProvider.Runner(output, lookup));

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

(If `GatherDataEvent` requires a `client()` / `server()` accessor pattern in newer NeoForge, swap `event.includeServer()` accordingly — check the example MDK's `Config.java` neighbor for the current event shape.)

- [ ] **Step 5: Run datagen**

```bash
./securityguard/gradlew :securityguard:runData --no-daemon
```

Expected: `BUILD SUCCESSFUL`. New files appear under `securityguard/src/generated/resources/`:
- `data/securityguard/recipe/guard_helmet.json`
- `data/securityguard/loot_table/entities/guard.json`
- `assets/securityguard/lang/en_us.json`

- [ ] **Step 6: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/data/ \
        securityguard/src/generated/
git commit -m "feat(securityguard): add datagen for recipe, loot table, translations"
```

---

## Task 14: Final Verification Pass — 26.1.2

Manual play-test in the dev client. Engineer must run this; no automated equivalent.

- [ ] **Step 1: Launch dev client**

```bash
./securityguard/gradlew :securityguard:runClient
```

- [ ] **Step 2: Run the verification checklist**

In a creative world (then survival for the recipe item):

- [ ] Mod listed at title screen `Mods` with name "Security Guard" and version 0.1.0.
- [ ] "Security Guard" creative tab exists with Guard Helmet (icon) + Spawn Egg.
- [ ] Item names display as "Guard Helmet" and "Security Guard Spawn Egg" (post-datagen).
- [ ] Spawn egg spawns a visible humanoid Security Guard with cap and baton.
- [ ] In-world recipe: place 3 iron blocks in a column, right-click top with Guard Helmet → guard spawns, blocks consumed, sound plays.
- [ ] 2-block column: nothing happens, helmet not consumed.
- [ ] **Ceiling test**: 3-iron column with stone 1 block above the top → nothing happens, helmet not consumed.
- [ ] Spawn a zombie nearby → guard targets, swings, slowness + weakness particles appear, zombie dies in ~5 hits.
- [ ] Hit guard with sword → no retaliation against the player.
- [ ] Attack a villager → guard targets you.
- [ ] Hit guard until dead → death sound plays, no item drops.
- [ ] Reload world → surviving guard still present.
- [ ] Right-click guard with iron ingot → it heals (inherited from `IronGolem`, intentionally kept).
- [ ] Survival: craft Guard Helmet using 4 iron ingots + 1 blue dye in the recipe pattern.

- [ ] **Step 3: Tag v0.1.0 if all checks pass**

```bash
git tag -a v0.1.0 -m "Security Guard mod v0.1.0"
```

- [ ] **Step 4: Build the release jar**

```bash
./securityguard/gradlew :securityguard:build
ls securityguard/build/libs/securityguard-0.1.0.jar
```

This is the shippable mod jar (drop into a NeoForge 26.1.2 server's `mods/` folder).
