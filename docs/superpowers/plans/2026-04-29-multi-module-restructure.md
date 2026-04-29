# Multi-Module Restructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the single-mod gradle project into a 2-module gradle project with a new `securitycore` library mod that the existing `securityguard` mod depends on, in preparation for adding a `thief` mod.

**Architecture:** Add a sibling `securitycore/` module that holds reusable interfaces (`SecurityAlly`, `SecurityHostile`) and reusable client/AI code (`HeldItemLayer`, `StunningMeleeGoal`) extracted from `securityguard/`. After this plan, the Guard mob behaves identically in-game; the only change is where its support classes live.

**Tech Stack:** Java 25, NeoForge 26.1.2.30-beta, Minecraft 26.1.2, gradle with `net.neoforged.moddev` 2.0.141 plugin, JUnit 5.

**Spec:** [docs/superpowers/specs/2026-04-29-thief-mob-design.md](../specs/2026-04-29-thief-mob-design.md) — sections "Repo Restructure" and "Multi-mod gradle wiring".

**Working directory for all commands:** `/Users/tweeks/code/minecraft-mods` (repo root). Module-specific commands use the gradle project syntax `:securitycore:taskname` etc.

---

## File Structure

### Files created

```
gradle.properties                                                # ADD: minecraft_version, neo_version, etc. (hoisted from securityguard/)
securitycore/build.gradle                                        # NEW: per-module gradle config, mirrors securityguard/build.gradle
securitycore/gradle.properties                                   # NEW: securitycore-specific mod_id, mod_version
securitycore/src/main/templates/META-INF/neoforge.mods.toml      # NEW: mod metadata template
securitycore/src/main/java/com/tweeks/securitycore/SecurityCoreMod.java        # NEW: empty entrypoint
securitycore/src/main/java/com/tweeks/securitycore/api/SecurityAlly.java       # NEW: marker interface
securitycore/src/main/java/com/tweeks/securitycore/api/SecurityHostile.java    # NEW: marker interface
securitycore/src/main/java/com/tweeks/securitycore/ai/StunningMeleeGoal.java   # NEW: extracted from BatonStrikeGoal
securitycore/src/main/java/com/tweeks/securitycore/client/HeldItemLayer.java   # NEW: extracted from BatonHeldLayer
```

### Files modified

```
settings.gradle                                                                # ADD include 'securitycore'
build.gradle                                                                   # (still empty, or trivial comment update)
securityguard/gradle.properties                                                # REMOVE minecraft_version/neo_version (now at root)
securityguard/build.gradle                                                     # ADD dependencies { implementation project(':securitycore') } + run aggregation
securityguard/src/main/templates/META-INF/neoforge.mods.toml                   # ADD securitycore as required dependency
securityguard/src/main/java/com/tweeks/securityguard/entity/SecurityGuardEntity.java   # USE StunningMeleeGoal + SecurityHostile predicate
securityguard/src/main/java/com/tweeks/securityguard/client/renderer/SecurityGuardRenderer.java   # USE HeldItemLayer
```

### Files deleted

```
securityguard/src/main/java/com/tweeks/securityguard/entity/ai/BatonStrikeGoal.java
securityguard/src/main/java/com/tweeks/securityguard/client/renderer/BatonHeldLayer.java
```

---

## Task 1: Establish baseline — confirm current build is green

**Files:** none modified.

- [ ] **Step 1: Run the existing build and test suite to confirm a clean starting point**

Run:
```bash
./gradlew :securityguard:build
```
Expected: `BUILD SUCCESSFUL`. The artifact `securityguard/build/libs/securityguard-0.1.0.jar` exists.

- [ ] **Step 2: Run the existing JUnit tests**

Run:
```bash
./gradlew :securityguard:test
```
Expected: `BUILD SUCCESSFUL`. All `SpawnPatternTest` cases pass.

- [ ] **Step 3: Note the baseline in the commit log for reference**

No file changes; no commit. This task only verifies the starting state. If either step fails, **stop and fix before proceeding** — every subsequent task assumes a green baseline.

---

## Task 2: Hoist version coordinates to root `gradle.properties`

**Files:**
- Modify: `gradle.properties` (root)
- Modify: `securityguard/gradle.properties`

- [ ] **Step 1: Add Minecraft/NeoForge version coordinates to root `gradle.properties`**

Append to `gradle.properties` (repo root):
```properties

# Shared platform versions for all mod modules.
# Update these in one place to keep all sibling mods aligned.
minecraft_version=26.1.2
minecraft_version_range=[26.1.2]
neo_version=26.1.2.30-beta
```

- [ ] **Step 2: Remove the same three properties from `securityguard/gradle.properties`**

Open `securityguard/gradle.properties`. Delete lines:
```properties
# Environment Properties
# https://projects.neoforged.net/neoforged/neoforge for latest versions
minecraft_version=26.1.2
minecraft_version_range=[26.1.2]
neo_version=26.1.2.30-beta
```

The remaining file should keep only the JVM args at the top and the `## Mod Properties` block at the bottom (`mod_id`, `mod_name`, `mod_license`, `mod_version`, `mod_group_id`).

- [ ] **Step 3: Verify gradle still resolves the properties**

Run:
```bash
./gradlew :securityguard:properties | grep -E '(minecraft_version|neo_version)'
```
Expected output includes:
```
minecraft_version: 26.1.2
minecraft_version_range: [26.1.2]
neo_version: 26.1.2.30-beta
```

(Subprojects automatically inherit properties from the root `gradle.properties` — no code change needed in `securityguard/build.gradle`.)

- [ ] **Step 4: Confirm `securityguard` still builds with hoisted properties**

Run:
```bash
./gradlew :securityguard:build
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add gradle.properties securityguard/gradle.properties
git commit -m "$(cat <<'EOF'
build: hoist Minecraft/NeoForge version coordinates to root gradle.properties

Moves minecraft_version, minecraft_version_range, and neo_version from
securityguard/gradle.properties to the repo root so future sibling mod
modules (securitycore, thief) inherit the same platform versions
without drift.
EOF
)"
```

---

## Task 3: Create `securitycore` module skeleton + register in settings

**Files:**
- Create: `securitycore/gradle.properties`
- Create: `securitycore/build.gradle`
- Create: `securitycore/src/main/templates/META-INF/neoforge.mods.toml`
- Create: `securitycore/src/main/java/com/tweeks/securitycore/SecurityCoreMod.java`
- Modify: `settings.gradle`

- [ ] **Step 1: Create the directory tree**

Run:
```bash
mkdir -p securitycore/src/main/java/com/tweeks/securitycore
mkdir -p securitycore/src/main/templates/META-INF
mkdir -p securitycore/src/main/resources
```

- [ ] **Step 2: Create `securitycore/gradle.properties`**

Create file `securitycore/gradle.properties` with:
```properties
## Mod Properties
mod_id=securitycore
mod_name=Security Core
mod_license=MIT
mod_version=0.1.0
mod_group_id=com.tweeks.securitycore
```

- [ ] **Step 3: Create `securitycore/build.gradle`**

Create file `securitycore/build.gradle` with the same structure as `securityguard/build.gradle`. Full content:

```gradle
plugins {
    id 'java-library'
    id 'maven-publish'
    id 'net.neoforged.moddev' version '2.0.141'
    id 'idea'
}

version = mod_version
group = mod_group_id

sourceSets.main.resources {
    srcDir('src/generated/clientData')
    srcDir('src/generated/serverData')
    exclude("**/*.bbmodel")
    exclude("src/generated/**/.cache")
}

repositories {
    // Add additional repositories here if required.
}

base {
    archivesName = mod_id
}

// Mojang ships Java 25 to end users in 26.1.2, so mods target Java 25.
java.toolchain.languageVersion = JavaLanguageVersion.of(25)

neoForge {
    version = project.neo_version

    runs {
        client {
            client()
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        }

        server {
            server()
            programArgument '--nogui'
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        }

        gameTestServer {
            type = "gameTestServer"
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        }

        clientData {
            clientData()
            programArguments.addAll '--mod', project.mod_id, '--all',
                '--output', file('src/generated/clientData/').getAbsolutePath(),
                '--existing', file('src/main/resources/').getAbsolutePath()
        }

        serverData {
            serverData()
            programArguments.addAll '--mod', project.mod_id, '--all',
                '--output', file('src/generated/serverData/').getAbsolutePath(),
                '--existing', file('src/main/resources/').getAbsolutePath()
        }

        configureEach {
            systemProperty 'forge.logging.markers', 'REGISTRIES'
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        "${mod_id}" {
            sourceSet(sourceSets.main)
        }
    }
}

configurations {
    runtimeClasspath.extendsFrom localRuntime
}

dependencies {
    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

sourceSets.test.compileClasspath += sourceSets.main.compileClasspath
sourceSets.test.runtimeClasspath += sourceSets.main.runtimeClasspath

test {
    useJUnitPlatform()
}

var generateModMetadata = tasks.register("generateModMetadata", ProcessResources) {
    var replaceProperties = [
            minecraft_version      : minecraft_version,
            minecraft_version_range: minecraft_version_range,
            neo_version            : neo_version,
            mod_id                 : mod_id,
            mod_name               : mod_name,
            mod_license            : mod_license,
            mod_version            : mod_version,
    ]
    inputs.properties replaceProperties
    expand replaceProperties
    from "src/main/templates"
    into "build/generated/sources/modMetadata"
}
sourceSets.main.resources.srcDir generateModMetadata
neoForge.ideSyncTask generateModMetadata

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

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

idea {
    module {
        downloadSources = true
        downloadJavadoc = true
    }
}
```

- [ ] **Step 4: Create the mod metadata template**

Create file `securitycore/src/main/templates/META-INF/neoforge.mods.toml` with:

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
Shared interfaces and helpers for the Security Pack mod series.
Provides marker interfaces (SecurityAlly, SecurityHostile) and
reusable AI/client classes consumed by Security Pack mods.
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

(The `securityguard` template is missing the `modLoader="javafml"` line at the top — that may be inherited from defaults. Keep this template explicit; if you find later that the existing `securityguard` template works without it, you can remove from this one too.)

- [ ] **Step 5: Verify `modLoader` line is needed by checking `securityguard`'s working template**

Run:
```bash
head -5 securityguard/src/main/templates/META-INF/neoforge.mods.toml
```

If the first line is `license="${mod_license}"` (no `modLoader` line), then `modLoader` is supplied automatically by the moddev plugin and you should **remove the `modLoader="javafml"` and `loaderVersion="[1,)"` lines from the new `securitycore` template** to match.

- [ ] **Step 6: Create the empty mod entrypoint**

Create file `securitycore/src/main/java/com/tweeks/securitycore/SecurityCoreMod.java` with:

```java
package com.tweeks.securitycore;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SecurityCoreMod.MOD_ID)
public class SecurityCoreMod {
    public static final String MOD_ID = "securitycore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SecurityCoreMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Security Core loading — provides shared interfaces for the Security Pack series");
    }
}
```

- [ ] **Step 7: Register `securitycore` in root `settings.gradle`**

Modify `settings.gradle`. Change:
```gradle
include 'securityguard'
```
to:
```gradle
include 'securitycore'
include 'securityguard'
```

- [ ] **Step 8: Build the new module standalone**

Run:
```bash
./gradlew :securitycore:build
```
Expected: `BUILD SUCCESSFUL`. A jar appears at `securitycore/build/libs/securitycore-0.1.0.jar`.

If the build fails with a "modLoader missing" or similar mods.toml validation error, restore the `modLoader="javafml"` + `loaderVersion="[1,)"` lines at the top of the template (i.e. the alternative outcome of Step 5).

- [ ] **Step 9: Commit**

```bash
git add settings.gradle securitycore/
git commit -m "$(cat <<'EOF'
feat(securitycore): add empty securitycore module skeleton

Sets up the multi-project structure with a new sibling securitycore
module that mirrors the build configuration of securityguard. Module
contains only an empty SecurityCoreMod entrypoint at this stage; the
shared interfaces and extracted classes land in subsequent commits.
EOF
)"
```

---

## Task 4: Add `SecurityHostile` and `SecurityAlly` marker interfaces

**Files:**
- Create: `securitycore/src/main/java/com/tweeks/securitycore/api/SecurityHostile.java`
- Create: `securitycore/src/main/java/com/tweeks/securitycore/api/SecurityAlly.java`

- [ ] **Step 1: Create the api package directory**

Run:
```bash
mkdir -p securitycore/src/main/java/com/tweeks/securitycore/api
```

- [ ] **Step 2: Create `SecurityHostile`**

Create file `securitycore/src/main/java/com/tweeks/securitycore/api/SecurityHostile.java`:

```java
package com.tweeks.securitycore.api;

/**
 * Marker for entities that Security Pack defenders (Guards) should attack on sight.
 * Implementing this interface opts an entity into Guard targeting without requiring
 * it to also implement the vanilla {@link net.minecraft.world.entity.monster.Enemy}
 * interface — useful for mobs that should be hostile only to Guards (not to other
 * vanilla aggressive systems like Iron Golems' default behavior).
 */
public interface SecurityHostile {
}
```

- [ ] **Step 3: Create `SecurityAlly`**

Create file `securitycore/src/main/java/com/tweeks/securitycore/api/SecurityAlly.java`:

```java
package com.tweeks.securitycore.api;

/**
 * Marker for entities that Security Pack defenders should never target.
 * Reserved for future use (e.g. allied mobs, pet wolves a Guard owner has tamed).
 * The Guard's current target predicate uses class checks for villagers/players;
 * this marker is the opt-in path for cross-mod entities that want to be treated
 * as friendly.
 */
public interface SecurityAlly {
}
```

- [ ] **Step 4: Verify the module compiles**

Run:
```bash
./gradlew :securitycore:compileJava
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add securitycore/src/main/java/com/tweeks/securitycore/api/
git commit -m "$(cat <<'EOF'
feat(securitycore): add SecurityHostile and SecurityAlly marker interfaces

Pure marker interfaces with no methods. SecurityHostile is the opt-in
hook that lets cross-mod entities be targeted by Guards without
implementing vanilla Enemy. SecurityAlly is reserved for future use.
EOF
)"
```

---

## Task 5: Extract `StunningMeleeGoal` into `securitycore`

**Files:**
- Create: `securitycore/src/main/java/com/tweeks/securitycore/ai/StunningMeleeGoal.java`

- [ ] **Step 1: Create the ai package directory**

Run:
```bash
mkdir -p securitycore/src/main/java/com/tweeks/securitycore/ai
```

- [ ] **Step 2: Create the parameterised goal**

The original `BatonStrikeGoal` (at `securityguard/src/main/java/com/tweeks/securityguard/entity/ai/BatonStrikeGoal.java`) has hardcoded constants. The extracted version takes them as constructor parameters so both Guard's baton and Thief's blackjack can share the class.

Create file `securitycore/src/main/java/com/tweeks/securitycore/ai/StunningMeleeGoal.java`:

```java
package com.tweeks.securitycore.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * A {@link MeleeAttackGoal} that, on each successful hit, applies stun effects
 * (Slowness + Weakness) and a small knockback to the target. Designed to be
 * shared between the Guard's baton and the Thief's blackjack — the caller
 * supplies all numeric parameters so each weapon can have distinct feel.
 */
public class StunningMeleeGoal extends MeleeAttackGoal {

    private final int stunDurationTicks;
    private final int slownessAmplifier;
    private final int weaknessAmplifier;
    private final double knockbackStrength;

    /**
     * @param mob                 the attacker
     * @param speedModifier       movement-speed multiplier while pursuing target (vanilla MeleeAttackGoal arg)
     * @param followingTargetEvenIfNotSeen  vanilla MeleeAttackGoal arg
     * @param stunDurationTicks   how long Slowness + Weakness last on the target (20 ticks = 1 second)
     * @param slownessAmplifier   amplifier for Slowness (0 = level I, 1 = level II)
     * @param weaknessAmplifier   amplifier for Weakness (0 = level I, 1 = level II)
     * @param knockbackStrength   horizontal knockback applied on hit (vanilla unit; 0.4 ≈ standard)
     */
    public StunningMeleeGoal(PathfinderMob mob,
                             double speedModifier,
                             boolean followingTargetEvenIfNotSeen,
                             int stunDurationTicks,
                             int slownessAmplifier,
                             int weaknessAmplifier,
                             double knockbackStrength) {
        super(mob, speedModifier, followingTargetEvenIfNotSeen);
        this.stunDurationTicks = stunDurationTicks;
        this.slownessAmplifier = slownessAmplifier;
        this.weaknessAmplifier = weaknessAmplifier;
        this.knockbackStrength = knockbackStrength;
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        if (this.canPerformAttack(target)) {
            this.resetAttackCooldown();
            this.mob.swing(this.mob.getUsedItemHand());
            this.mob.doHurtTarget((ServerLevel) this.mob.level(), target);

            if (target.isAlive()) {
                target.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, stunDurationTicks, slownessAmplifier));
                target.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, stunDurationTicks, weaknessAmplifier));
                target.knockback(
                    knockbackStrength,
                    this.mob.getX() - target.getX(),
                    this.mob.getZ() - target.getZ());
            }
        }
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run:
```bash
./gradlew :securitycore:compileJava
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add securitycore/src/main/java/com/tweeks/securitycore/ai/
git commit -m "$(cat <<'EOF'
feat(securitycore): extract StunningMeleeGoal from securityguard's BatonStrikeGoal

Generalises the goal so the stun parameters (duration, slowness/weakness
amplifiers, knockback strength) are constructor arguments rather than
hardcoded. Lets the Guard's baton and the upcoming Thief's blackjack
share one implementation while feeling distinct. The original
BatonStrikeGoal still exists; it gets replaced and deleted in a later
commit so the Guard build stays green between tasks.
EOF
)"
```

---

## Task 6: Extract `HeldItemLayer` into `securitycore`

**Files:**
- Create: `securitycore/src/main/java/com/tweeks/securitycore/client/HeldItemLayer.java`

- [ ] **Step 1: Create the client package directory**

Run:
```bash
mkdir -p securitycore/src/main/java/com/tweeks/securitycore/client
```

- [ ] **Step 2: Create the parameterised layer**

The original `BatonHeldLayer` hardcodes the texture, model, translation, and rotation. The extracted version takes all of them via constructor.

Create file `securitycore/src/main/java/com/tweeks/securitycore/client/HeldItemLayer.java`:

```java
package com.tweeks.securitycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Renders an item-shaped {@link Model} in the parent humanoid's right hand.
 * Generalised from the Guard's baton-rendering layer so future humanoid mobs
 * (e.g. Thief's blackjack) can reuse the same hand-anchoring math.
 *
 * @param <M> the parent humanoid model type whose {@code rightArm} bone we attach to
 */
public class HeldItemLayer<M extends HumanoidModel<HumanoidRenderState>>
        extends RenderLayer<HumanoidRenderState, M> {

    private final Model<HumanoidRenderState> heldModel;
    private final Identifier texture;
    private final float translateX;
    private final float translateY;
    private final float translateZ;
    private final float xRotationDegrees;

    /**
     * @param parent       the parent humanoid renderer
     * @param heldModel    the item-shaped model to render in the hand
     * @param texture      texture for the held model
     * @param translateX   X offset from the right-arm bone origin (block units, 1/16 = 1 pixel)
     * @param translateY   Y offset
     * @param translateZ   Z offset
     * @param xRotationDegrees  rotation about the X axis applied after translation
     */
    public HeldItemLayer(RenderLayerParent<HumanoidRenderState, M> parent,
                         Model<HumanoidRenderState> heldModel,
                         Identifier texture,
                         float translateX,
                         float translateY,
                         float translateZ,
                         float xRotationDegrees) {
        super(parent);
        this.heldModel = heldModel;
        this.texture = texture;
        this.translateX = translateX;
        this.translateY = translateY;
        this.translateZ = translateZ;
        this.xRotationDegrees = xRotationDegrees;
    }

    @Override
    public void submit(PoseStack pose,
                       SubmitNodeCollector collector,
                       int lightCoords,
                       HumanoidRenderState state,
                       float yRot,
                       float xRot) {
        pose.pushPose();
        getParentModel().rightArm.translateAndRotate(pose);
        pose.translate(translateX, translateY, translateZ);
        pose.mulPose(Axis.XP.rotationDegrees(xRotationDegrees));

        collector.submitModel(
            heldModel,
            state,
            pose,
            texture,
            lightCoords,
            OverlayTexture.NO_OVERLAY,
            -1,
            null
        );

        pose.popPose();
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run:
```bash
./gradlew :securitycore:compileJava
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add securitycore/src/main/java/com/tweeks/securitycore/client/
git commit -m "$(cat <<'EOF'
feat(securitycore): extract HeldItemLayer from securityguard's BatonHeldLayer

Parameterises the held-model, texture, translation, and rotation so
future humanoid mobs (Thief's blackjack) can reuse the same hand-
anchoring math. The original BatonHeldLayer still exists; it gets
replaced and deleted in a later commit so the Guard build stays green.
EOF
)"
```

---

## Task 7: Wire `securityguard` to depend on `:securitycore`

**Files:**
- Modify: `securityguard/build.gradle`
- Modify: `securityguard/src/main/templates/META-INF/neoforge.mods.toml`

- [ ] **Step 1: Add the project dependency to `securityguard/build.gradle`**

Open `securityguard/build.gradle`. Find the existing `dependencies { ... }` block (around line 80):
```gradle
dependencies {
    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

Replace with:
```gradle
dependencies {
    implementation project(':securitycore')

    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

- [ ] **Step 2: Add `securitycore` as a required mod dependency in `securityguard`'s mods.toml**

Open `securityguard/src/main/templates/META-INF/neoforge.mods.toml`. Append after the existing `dependencies.${mod_id}` blocks (after the `[[dependencies.${mod_id}]]` block that declares the `minecraft` dependency):

```toml

[[dependencies.${mod_id}]]
    modId="securitycore"
    type="required"
    versionRange="[0.1.0,)"
    ordering="AFTER"
    side="BOTH"
```

- [ ] **Step 3: Verify securityguard still compiles (it doesn't yet USE securitycore classes, but the linkage must be valid)**

Run:
```bash
./gradlew :securityguard:compileJava
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Verify securityguard still builds end-to-end**

Run:
```bash
./gradlew :securityguard:build
```
Expected: `BUILD SUCCESSFUL`. Existing tests pass.

- [ ] **Step 5: Commit**

```bash
git add securityguard/build.gradle securityguard/src/main/templates/META-INF/neoforge.mods.toml
git commit -m "$(cat <<'EOF'
feat(securityguard): depend on :securitycore module

Adds the gradle project-dependency on :securitycore and declares it as
a required mod dependency in neoforge.mods.toml so NeoForge loads
securitycore before securityguard at runtime. No code in securityguard
uses securitycore yet — that swap happens in subsequent commits.
EOF
)"
```

---

## Task 8: Replace `BatonStrikeGoal` with `StunningMeleeGoal` in the Guard

**Files:**
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/entity/SecurityGuardEntity.java`
- Delete: `securityguard/src/main/java/com/tweeks/securityguard/entity/ai/BatonStrikeGoal.java`

- [ ] **Step 1: Update `SecurityGuardEntity` to use `StunningMeleeGoal` from core**

Open `securityguard/src/main/java/com/tweeks/securityguard/entity/SecurityGuardEntity.java`. Find the goal-registration line (line 31):
```java
this.goalSelector.addGoal(1, new com.tweeks.securityguard.entity.ai.BatonStrikeGoal(this));
```

Replace with:
```java
this.goalSelector.addGoal(1, new com.tweeks.securitycore.ai.StunningMeleeGoal(
    this, 1.0, true, 60, 1, 0, 0.2));
```

The numeric arguments preserve the original `BatonStrikeGoal` behavior exactly:
- `1.0, true` — speedModifier and followingTargetEvenIfNotSeen (originals at `BatonStrikeGoal.java:18`)
- `60` ticks stun duration (was `STUN_DURATION_TICKS`)
- `1` slowness amplifier = Slowness II (was `SLOWNESS_AMPLIFIER`)
- `0` weakness amplifier = Weakness I (was `WEAKNESS_AMPLIFIER`)
- `0.2` knockback strength (was `KNOCKBACK_STRENGTH`)

- [ ] **Step 2: Build to confirm the swap compiles**

Run:
```bash
./gradlew :securityguard:compileJava
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Delete the original `BatonStrikeGoal.java`**

Run:
```bash
git rm securityguard/src/main/java/com/tweeks/securityguard/entity/ai/BatonStrikeGoal.java
```

(If the directory `securityguard/src/main/java/com/tweeks/securityguard/entity/ai/` becomes empty after this delete, leave the empty directory — git ignores it and we don't want stray rmdir commands.)

- [ ] **Step 4: Build the whole securityguard module to confirm nothing else referenced the deleted class**

Run:
```bash
./gradlew :securityguard:build
```
Expected: `BUILD SUCCESSFUL`. Existing tests pass.

- [ ] **Step 5: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/entity/SecurityGuardEntity.java
git commit -m "$(cat <<'EOF'
refactor(securityguard): use shared StunningMeleeGoal from securitycore

Replaces the in-mod BatonStrikeGoal with the parameterised
StunningMeleeGoal from :securitycore. Numeric arguments preserve the
exact existing Guard behavior (1.2s stun, Slowness II, Weakness I,
knockback 0.2). Deletes the now-unreferenced BatonStrikeGoal.java.
EOF
)"
```

---

## Task 9: Replace `BatonHeldLayer` with `HeldItemLayer` in the Guard renderer

**Files:**
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/client/renderer/SecurityGuardRenderer.java`
- Delete: `securityguard/src/main/java/com/tweeks/securityguard/client/renderer/BatonHeldLayer.java`

- [ ] **Step 1: Update `SecurityGuardRenderer` to construct `HeldItemLayer` directly**

Open `securityguard/src/main/java/com/tweeks/securityguard/client/renderer/SecurityGuardRenderer.java`. Replace the entire file content with:

```java
package com.tweeks.securityguard.client.renderer;

import com.tweeks.securitycore.client.HeldItemLayer;
import com.tweeks.securityguard.SecurityGuardMod;
import com.tweeks.securityguard.client.model.BatonModel;
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

    private static final Identifier BATON_TEXTURE = Identifier.fromNamespaceAndPath(
        SecurityGuardMod.MOD_ID, "textures/entity/baton.png");

    public SecurityGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new SecurityGuardModel(context.bakeLayer(SecurityGuardModel.LAYER_LOCATION)), 0.5f);
        BatonModel batonModel = new BatonModel(context.bakeLayer(BatonModel.LAYER_LOCATION));
        this.addLayer(new HeldItemLayer<>(this,
            batonModel,
            BATON_TEXTURE,
            -0.0625f, 0.625f, 0.0f,
            180.0f));
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

The translation `(-0.0625f, 0.625f, 0.0f)` and rotation `180.0f` come directly from the original `BatonHeldLayer.submit()` (was `BatonHeldLayer.java:38-39`).

- [ ] **Step 2: Build to confirm it compiles**

Run:
```bash
./gradlew :securityguard:compileJava
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Delete the original `BatonHeldLayer.java`**

Run:
```bash
git rm securityguard/src/main/java/com/tweeks/securityguard/client/renderer/BatonHeldLayer.java
```

- [ ] **Step 4: Build the whole module to confirm nothing else referenced it**

Run:
```bash
./gradlew :securityguard:build
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/client/renderer/SecurityGuardRenderer.java
git commit -m "$(cat <<'EOF'
refactor(securityguard): use shared HeldItemLayer from securitycore

Replaces the in-mod BatonHeldLayer with the parameterised HeldItemLayer
from :securitycore. Translation (-0.0625, 0.625, 0) and 180-degree X
rotation preserved verbatim from the original layer so the baton renders
identically to before. Deletes the now-unreferenced BatonHeldLayer.java.
EOF
)"
```

---

## Task 10: Update `GuardTargetHostilesGoal` predicate to include `SecurityHostile`

**Files:**
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/entity/SecurityGuardEntity.java`

- [ ] **Step 1: Broaden the target predicate**

Open `securityguard/src/main/java/com/tweeks/securityguard/entity/SecurityGuardEntity.java`. Find the inner class `GuardTargetHostilesGoal` (line 68-75):

```java
public static class GuardTargetHostilesGoal
        extends net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<net.minecraft.world.entity.Mob> {
    public GuardTargetHostilesGoal(SecurityGuardEntity guard) {
        super(guard, net.minecraft.world.entity.Mob.class, 5, false, false,
            (target, level) -> target instanceof net.minecraft.world.entity.monster.Enemy
                            && !(target instanceof net.minecraft.world.entity.monster.Creeper));
    }
}
```

Replace the predicate with one that ALSO matches `SecurityHostile`:

```java
public static class GuardTargetHostilesGoal
        extends net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<net.minecraft.world.entity.Mob> {
    public GuardTargetHostilesGoal(SecurityGuardEntity guard) {
        super(guard, net.minecraft.world.entity.Mob.class, 5, false, false,
            (target, level) -> target instanceof com.tweeks.securitycore.api.SecurityHostile
                            || (target instanceof net.minecraft.world.entity.monster.Enemy
                                && !(target instanceof net.minecraft.world.entity.monster.Creeper)));
    }
}
```

(`SecurityHostile` mobs do NOT get the Creeper exclusion — that exclusion exists because Creepers explode when nearby, and our mods control which entities implement `SecurityHostile`, so we don't need defensive filtering there.)

- [ ] **Step 2: Build**

Run:
```bash
./gradlew :securityguard:build
```
Expected: `BUILD SUCCESSFUL`. Existing tests still pass.

- [ ] **Step 3: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/entity/SecurityGuardEntity.java
git commit -m "$(cat <<'EOF'
feat(securityguard): target SecurityHostile entities in addition to vanilla Enemy

GuardTargetHostilesGoal predicate now matches any mob implementing
securitycore's SecurityHostile marker, OR any non-Creeper vanilla Enemy.
This is the hook that lets the upcoming Thief mob (in revealed states)
opt into Guard targeting without implementing Enemy itself.
EOF
)"
```

---

## Task 11: Add multi-module run aggregation so `runClient` loads both mods

**Files:**
- Modify: `securityguard/build.gradle`
- Modify: `securitycore/build.gradle`

- [ ] **Step 1: Add `modSources` aggregation to `securityguard/build.gradle`**

Open `securityguard/build.gradle`. Find the `runs { ... }` block inside `neoForge { ... }` (around line 32). For each of the four real run blocks (`client`, `server`, `clientData`, `serverData` — skip `gameTestServer` and `configureEach`), add a `modSources.add(...)` line that pulls in `securitycore`'s main source set.

The `client` block currently looks like:
```gradle
client {
    client()
    systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
}
```

Change to:
```gradle
client {
    client()
    modSources.add(project(':securitycore').sourceSets.main)
    systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
}
```

Apply the same `modSources.add(project(':securitycore').sourceSets.main)` line to the `server`, `clientData`, and `serverData` blocks.

- [ ] **Step 2: Add `modSources` aggregation to `securitycore/build.gradle`**

Open `securitycore/build.gradle`. Apply the symmetric change so launching from `:securitycore` also loads `:securityguard`. In the `client`, `server`, `clientData`, `serverData` blocks, add:
```gradle
modSources.add(project(':securityguard').sourceSets.main)
```

This lets devs launch a dev client from either module and get both mods loaded.

- [ ] **Step 3: Verify the configuration is valid by triggering an IDE sync (or a dry run)**

Run:
```bash
./gradlew :securityguard:tasks --group neoforged > /dev/null 2>&1 && echo "config OK"
./gradlew :securitycore:tasks --group neoforged > /dev/null 2>&1 && echo "config OK"
```
Expected: both lines print `config OK`. Any "modSources" / "project" error means the gradle syntax is wrong; recheck spelling.

- [ ] **Step 4: Commit**

```bash
git add securityguard/build.gradle securitycore/build.gradle
git commit -m "$(cat <<'EOF'
build: aggregate sibling module source sets into NeoForge run configs

Each module's client/server/clientData/serverData runs now include the
other module's main source set via modSources.add(...). Devs can launch
runClient from either :securityguard or :securitycore and have both
mods loaded together. Required because each module is a standalone
NeoForge mod; without aggregation, a runClient would only load that
module's mod metadata.
EOF
)"
```

---

## Task 12: Full build + test suite + datagen verification

**Files:** none modified.

- [ ] **Step 1: Build all modules**

Run:
```bash
./gradlew build
```
Expected: `BUILD SUCCESSFUL`. Both `securitycore-0.1.0.jar` and `securityguard-0.1.0.jar` exist under `*/build/libs/`.

- [ ] **Step 2: Run the test suite**

Run:
```bash
./gradlew test
```
Expected: `BUILD SUCCESSFUL`. The `SpawnPatternTest` cases (5 tests) all pass.

- [ ] **Step 3: Re-run securityguard datagen to confirm it still produces identical output**

The repo already contains generated resources at `securityguard/src/generated/`. Re-running datagen and diffing against git is the cleanest "no behavior change" check for the refactor.

Run:
```bash
./gradlew :securityguard:runData
git status securityguard/src/generated/
```
Expected: `git status` reports no changes (the datagen output is identical to what was committed before the refactor). If there ARE diffs, inspect them — most likely they are timestamp/cache files. The cache directory `.cache/` is the only directory expected to potentially churn; revert any unintentional changes to actual JSON files.

If non-cache files changed, revert them:
```bash
git checkout -- securityguard/src/generated/clientData/assets/
git checkout -- securityguard/src/generated/serverData/data/
```
And investigate why datagen output drifted (probably a code change broke a provider).

- [ ] **Step 4: No commit needed for verification**

Verification only. If everything passed, proceed to Task 13 (manual smoke test).

If anything failed, **stop and fix before continuing**. The whole point of this restructure is that Guard behavior is preserved exactly.

---

## Task 13: Manual smoke test in dev client

**Files:** none modified.

- [ ] **Step 1: Launch the dev client**

Run:
```bash
./gradlew :securityguard:runClient
```
Wait for Minecraft to load to the title screen.

- [ ] **Step 2: Verify both mods are listed**

In the title screen, click **Mods**. Confirm BOTH appear in the list:
- "Security Core 0.1.0"
- "Security Guard 0.1.0"

If "Security Core" is missing, the `modSources` aggregation in Task 11 didn't apply. Stop, fix `securityguard/build.gradle`, re-run.

- [ ] **Step 3: Smoke-test Guard spawn + baton attack**

Create a new Creative-mode flat world. In-game:
1. Open the creative inventory, find the "Security Guard" tab, grab the **Guard Spawn Egg** (or alternatively: build the iron-block I-shape and place a Guard Helmet on top — that's the canonical spawn pattern).
2. Spawn a Guard with the egg.
3. Spawn a Zombie nearby (creative inventory or `/summon zombie ~ ~ ~`).
4. **Verify:** the Guard targets the Zombie, runs to it, swings the baton, and the Zombie visibly slows down (Slowness II) and weakens (Weakness I) for ~3 seconds after each hit.

If the baton swings but no stun applies → `StunningMeleeGoal` parameters don't match. Check Task 8 Step 1 numbers against the original constants in the deleted `BatonStrikeGoal`.

- [ ] **Step 4: Smoke-test baton renders on the Guard's hand**

Visually inspect the Guard from multiple angles. The wooden baton should be visible in the Guard's right hand, oriented vertically (handle up, business end down, same as before the refactor).

If the baton is missing or in the wrong position → `HeldItemLayer` constructor args don't match the original `BatonHeldLayer`. Check Task 9 Step 1 translation/rotation values.

- [ ] **Step 5: Close the client; no commit needed**

Verification only. If both smoke tests pass, the restructure is complete.

---

## Task 14: Update top-level docs

**Files:**
- Modify: `hoto.md` (existing how-to-install doc)

- [ ] **Step 1: Update `hoto.md` to reflect the new jar layout**

Open `hoto.md`. The existing instructions reference `securityguard/build/libs/securityguard-0.1.0.jar` as the only artifact. After this restructure, BOTH `securitycore-0.1.0.jar` AND `securityguard-0.1.0.jar` must be installed.

Find the section that shows the cp command:
```
cp securityguard/build/libs/securityguard-0.1.0.jar ~/Library/Application\ Support/minecraft/mods/
```

Replace with:
```
cp securitycore/build/libs/securitycore-0.1.0.jar ~/Library/Application\ Support/minecraft/mods/
cp securityguard/build/libs/securityguard-0.1.0.jar ~/Library/Application\ Support/minecraft/mods/
```

And add a sentence above explaining: "The Security Guard mod now requires the Security Core library mod. Install both jars."

- [ ] **Step 2: Commit**

```bash
git add hoto.md
git commit -m "$(cat <<'EOF'
docs(hoto): install both securitycore and securityguard jars

Reflects the multi-module restructure: Security Guard now depends on
Security Core, so both jars must be dropped into the mods folder.
EOF
)"
```

---

## Task 15: Final integration check + plan-complete commit

**Files:** none modified.

- [ ] **Step 1: One final clean build from scratch**

Run:
```bash
./gradlew clean build
```
Expected: `BUILD SUCCESSFUL`. Both jars rebuild from a fresh state.

- [ ] **Step 2: Confirm the plan checkbox state in this file**

Open `docs/superpowers/plans/2026-04-29-multi-module-restructure.md`. Verify every `- [ ]` step is now `- [x]`. (If executing via subagent-driven-development, the orchestrator handles this; if executing inline, mark them by hand.)

- [ ] **Step 3: Done — no further commit. Move on to writing the Thief mob plan.**

The next step in the project is to invoke the brainstorming/writing-plans flow again for the Thief mob itself, building on top of this restructured base. The Thief plan was deliberately scoped out of this plan to keep both PRs focused.

---

## Self-review notes (for the implementer)

- Every task ends with `git commit` so the repo can be bisected if a smoke test later reveals a regression. If a step is genuinely verification-only (Tasks 1, 12, 13, 15), no commit is created — that's intentional.
- The order of tasks matters: extracting classes (Tasks 5, 6) BEFORE swapping their use (Tasks 8, 9) BEFORE deleting the originals (still in Tasks 8, 9). This sequence keeps `:securityguard:build` green at every commit. If a subagent ever skips ahead and deletes an original before swapping the call site, the build breaks until the swap lands.
- The numeric arguments in Task 8 (`60, 1, 0, 0.2`) are the most common "subagent guesses wrong" failure point. They MUST match the original constants in the deleted `BatonStrikeGoal`. Re-check those values from the original file (preserved in git history) if a smoke test shows different stun timing.
- The translation `(-0.0625f, 0.625f, 0.0f)` and `180.0f` rotation in Task 9 must likewise come verbatim from the original `BatonHeldLayer.submit` body, NOT be re-derived.
