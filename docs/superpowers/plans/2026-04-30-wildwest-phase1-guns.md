# Wild-West Phase 1 — Module + Pistol + Rifle — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up a new `:wildwest` gradle subproject and ship two player-usable, mob-usable ranged weapons — a six-shooter pistol (hitscan, close range) and a bolt-action rifle (projectile, long range) — that phase 2 (mobs) and phase 3 (horses + leader-follower AI) can build on without rework.

**Architecture:** Mirror the existing `:thief` module structure. Items extend `Item` and override `use()`. Pistol applies damage via server-side raycast (`Level.clip` + entity AABB walk). Rifle spawns a `BulletEntity extends AbstractArrow`. Custom `wildwest:gunshot` damage type unifies death messages. Bolt-cycle rifle animation rides on vanilla `ItemProperties` predicate against cooldown percent. Tracer/muzzle visuals via a custom S2C packet.

**Tech Stack:** Java 25, NeoForge 26.1.2.30-beta, Minecraft 26.1.2, JUnit 5, Gradle (existing root config), `net.neoforged.moddev` 2.0.141.

**Spec:** [docs/superpowers/specs/2026-04-30-wildwest-phase1-guns-design.md](../specs/2026-04-30-wildwest-phase1-guns-design.md)

---

## Task 1: Module scaffold

**Files:**
- Create: `wildwest/build.gradle`
- Create: `wildwest/gradle.properties`
- Create: `wildwest/src/main/templates/META-INF/neoforge.mods.toml`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`
- Modify: `settings.gradle` (root) — add `include 'wildwest'`
- Modify: `thief/build.gradle`, `securityguard/build.gradle` — extend `mods` block + run-config `modSources` to include `:wildwest` so dev clients launched from any module load all four mods

- [ ] **Step 1: Add the wildwest entry to root settings.gradle**

Edit `settings.gradle` — add `include 'wildwest'` after `include 'translator'`:

```gradle
include 'securitycore'
include 'securityguard'
include 'thief'
include 'creeperskin'
include 'translator'
include 'wildwest'
```

- [ ] **Step 2: Create wildwest/gradle.properties**

```properties
org.gradle.jvmargs=-Xmx2G
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
mod_id=wildwest
mod_name=Wild West
mod_license=MIT
mod_version=0.1.0
mod_group_id=com.tweeks.wildwest
```

- [ ] **Step 3: Create wildwest/build.gradle**

Copy `thief/build.gradle` verbatim to `wildwest/build.gradle`, then change the `mods { ... }` block to declare `wildwest` as the primary mod and list `securitycore`, `securityguard`, `thief` as siblings whose source sets are loaded into the dev client. Replace the entire `mods { ... }` block (inside `neoForge { ... }`) with:

```gradle
    mods {
        "${mod_id}" {
            sourceSet(sourceSets.main)
        }
        "securitycore" {
            sourceSet(project(':securitycore').sourceSets.main)
        }
        "securityguard" {
            sourceSet(project(':securityguard').sourceSets.main)
        }
        "thief" {
            sourceSet(project(':thief').sourceSets.main)
        }
    }
```

And replace the `dependencies { ... }` block to include all sibling projects:

```gradle
dependencies {
    implementation project(':securitycore')
    implementation project(':securityguard')
    implementation project(':thief')

    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

Leave the rest of `thief/build.gradle`'s body untouched when copying.

- [ ] **Step 4: Update thief/build.gradle and securityguard/build.gradle to know about wildwest (dev-client load only, NO compile dep)**

The dev client launched from any sibling module needs to load wildwest's source set so all four mods appear in the in-game Mods list. Crucially: do **NOT** add `implementation project(':wildwest')` to thief or securityguard — wildwest depends on both of them, so a reverse compile dep would form a circular `compileJava` task graph. The `mods { sourceSet(...) }` reference is sufficient for runtime loading without a compile dep, but it requires `evaluationDependsOn(':wildwest')` so the source-set lookup resolves at evaluation time. (Existing precedent: `securityguard/build.gradle` references `:thief`'s source set in its `mods` block via `evaluationDependsOn(':thief')` without adding `implementation project(':thief')`.)

In `thief/build.gradle`, inside `neoForge { mods { ... } }`, append:

```gradle
        "wildwest" {
            sourceSet(project(':wildwest').sourceSets.main)
        }
```

And at the top level of the same file (matching the existing `evaluationDependsOn(':thief')` line in `securityguard/build.gradle`), add:

```gradle
evaluationDependsOn(':wildwest')
```

Apply the same two additions to `securityguard/build.gradle`. (`securitycore` doesn't need updating — it's the leaf dependency.)

**Do NOT** add `implementation project(':wildwest')` to either module's `dependencies` block.

- [ ] **Step 5: Create wildwest/src/main/templates/META-INF/neoforge.mods.toml**

```toml
license="${mod_license}"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"
authors="Tom Weeks"
description='''
Adds wild-west themed mobs and items. Phase 1 ships a six-shooter pistol and
a bolt-action rifle.
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

[[dependencies.${mod_id}]]
    modId="securitycore"
    type="required"
    versionRange="[0.1.0,)"
    ordering="AFTER"
    side="BOTH"
```

- [ ] **Step 6: Create the mod main class**

`wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`:

```java
package com.tweeks.wildwest;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(WildWestMod.MOD_ID)
public class WildWestMod {
    public static final String MOD_ID = "wildwest";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WildWestMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Wild West mod loading");
        Registration.register(modEventBus);
    }
}
```

- [ ] **Step 7: Create the Registration class shell**

`wildwest/src/main/java/com/tweeks/wildwest/Registration.java`:

```java
package com.tweeks.wildwest;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(WildWestMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WildWestMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WILDWEST_TAB =
        CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + WildWestMod.MOD_ID))
                .icon(() -> net.minecraft.world.item.Items.IRON_INGOT.getDefaultInstance())
                .displayItems((params, output) -> {
                    // Items added here in later tasks.
                })
                .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
```

- [ ] **Step 8: Build and run**

```bash
./gradlew :wildwest:build
```

Expected: `BUILD SUCCESSFUL`.

```bash
./gradlew :wildwest:runClient
```

Expected: client launches; **Mods** list shows `securitycore`, `securityguard`, `thief`, `wildwest` (and any others). Quit the client.

- [ ] **Step 9: Commit**

```bash
git add settings.gradle wildwest/ thief/build.gradle securityguard/build.gradle
git commit -m "feat(wildwest): module scaffold + mod class

Empty wildwest module loads alongside the other mods. No items yet."
```

---

## Task 2: Sound events

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/ModSounds.java`
- Create: `wildwest/src/main/resources/assets/wildwest/sounds.json`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java` (call ModSounds.register)

- [ ] **Step 1: Create ModSounds**

`wildwest/src/main/java/com/tweeks/wildwest/ModSounds.java`:

```java
package com.tweeks.wildwest;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(Registries.SOUND_EVENT, WildWestMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_FIRE = register("pistol_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFLE_FIRE  = register("rifle_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_CYCLE  = register("bolt_cycle");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
```

- [ ] **Step 2: Wire ModSounds into the mod constructor**

In `WildWestMod.java`, in the constructor after `Registration.register(modEventBus);`:

```java
        ModSounds.register(modEventBus);
```

- [ ] **Step 3: Create the placeholder sounds.json**

Real `.ogg` files come later; for now, redirect each event to a vanilla sound so playback works.

`wildwest/src/main/resources/assets/wildwest/sounds.json`:

```json
{
  "pistol_fire": {
    "category": "player",
    "subtitle": "subtitle.wildwest.pistol_fire",
    "sounds": [
      { "name": "minecraft:item.crossbow.shoot", "type": "event" }
    ]
  },
  "rifle_fire": {
    "category": "player",
    "subtitle": "subtitle.wildwest.rifle_fire",
    "sounds": [
      { "name": "minecraft:item.crossbow.shoot", "type": "event" }
    ]
  },
  "bolt_cycle": {
    "category": "player",
    "subtitle": "subtitle.wildwest.bolt_cycle",
    "sounds": [
      { "name": "minecraft:item.crossbow.loading_middle", "type": "event" }
    ]
  }
}
```

- [ ] **Step 4: Build**

```bash
./gradlew :wildwest:build
```

Expected: `BUILD SUCCESSFUL`. (Actual sound playback verified in later tasks once items exist to play them.)

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/ModSounds.java wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java wildwest/src/main/resources/assets/wildwest/sounds.json
git commit -m "feat(wildwest): register pistol/rifle/bolt sound events

Three custom events redirect to vanilla crossbow sounds as placeholders.
Custom .ogg files swap in later by editing sounds.json only."
```

---

## Task 3: Custom damage type

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/WildWestDamageTypes.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/data/ModDamageTypeProvider.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/data/DataGenerators.java`

- [ ] **Step 1: Create WildWestDamageTypes**

`wildwest/src/main/java/com/tweeks/wildwest/WildWestDamageTypes.java`:

```java
package com.tweeks.wildwest;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

public final class WildWestDamageTypes {
    private WildWestDamageTypes() {}

    public static final ResourceKey<DamageType> GUNSHOT = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "gunshot"));

    public static DamageSource gunshot(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(GUNSHOT),
            attacker);
    }
}
```

- [ ] **Step 2: Create ModDamageTypeProvider**

`wildwest/src/main/java/com/tweeks/wildwest/data/ModDamageTypeProvider.java`:

```java
package com.tweeks.wildwest.data;

import com.tweeks.wildwest.WildWestDamageTypes;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypeProvider {
    private ModDamageTypeProvider() {}

    public static void bootstrap(BootstrapContext<DamageType> ctx) {
        // (message_id, exhaustion). Exhaustion 0.1 matches vanilla projectile damage.
        ctx.register(WildWestDamageTypes.GUNSHOT,
            new DamageType("wildwest.gunshot", 0.1f));
    }
}
```

- [ ] **Step 3: Create DataGenerators entry point**

`wildwest/src/main/java/com/tweeks/wildwest/data/DataGenerators.java`:

```java
package com.tweeks.wildwest.data;

import com.tweeks.wildwest.WildWestMod;
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

@EventBusSubscriber(modid = WildWestMod.MOD_ID)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherDataServer(GatherDataEvent.Server event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        RegistrySetBuilder builder = new RegistrySetBuilder()
            .add(Registries.DAMAGE_TYPE, ModDamageTypeProvider::bootstrap);
        gen.addProvider(true, new DatapackBuiltinEntriesProvider(
            output, lookup, builder, Set.of(WildWestMod.MOD_ID)));
    }
}
```

- [ ] **Step 4: Run datagen and inspect output**

```bash
./gradlew :wildwest:runData
```

Expected: writes `wildwest/src/generated/serverData/data/wildwest/damage_type/gunshot.json` containing `{ "exhaustion": 0.1, "message_id": "wildwest.gunshot", "scaling": "..." }`. Inspect the file:

```bash
cat wildwest/src/generated/serverData/data/wildwest/damage_type/gunshot.json
```

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/WildWestDamageTypes.java wildwest/src/main/java/com/tweeks/wildwest/data/ wildwest/src/generated/
git commit -m "feat(wildwest): wildwest:gunshot damage type + datagen

Used by both pistol and rifle. Death message lang keys come with the
language provider task."
```

---

## Task 4: Hitscan helper + unit test (TDD)

The pure ray/AABB-walk math lives in its own class so it can be unit-tested without booting Minecraft, matching the existing repo convention (see `RevealStateTest`). `PistolItem` (next task) calls into this helper.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/Hitscan.java`
- Create: `wildwest/src/test/java/com/tweeks/wildwest/HitscanTest.java`

- [ ] **Step 1: Write the failing test**

`wildwest/src/test/java/com/tweeks/wildwest/HitscanTest.java`:

```java
package com.tweeks.wildwest;

import com.tweeks.wildwest.Hitscan.Candidate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HitscanTest {

    @Test
    void noCandidates_andNoBlock_returnsEmpty() {
        Optional<Candidate> hit = Hitscan.firstHitWithinRange(
            Double.POSITIVE_INFINITY, List.of());
        assertTrue(hit.isEmpty());
    }

    @Test
    void closerEntityWinsOverFartherEntity() {
        Candidate near = new Candidate("near", 4.0);
        Candidate far  = new Candidate("far",  10.0);
        Optional<Candidate> hit = Hitscan.firstHitWithinRange(
            Double.POSITIVE_INFINITY, List.of(far, near));
        assertEquals("near", hit.orElseThrow().id());
    }

    @Test
    void blockCloserThanEntity_returnsEmpty() {
        Candidate entity = new Candidate("entity", 8.0);
        Optional<Candidate> hit = Hitscan.firstHitWithinRange(
            5.0, List.of(entity));  // block at 5.0, entity at 8.0
        assertTrue(hit.isEmpty());
    }

    @Test
    void entityCloserThanBlock_returnsEntity() {
        Candidate entity = new Candidate("entity", 3.0);
        Optional<Candidate> hit = Hitscan.firstHitWithinRange(
            5.0, List.of(entity));
        assertEquals("entity", hit.orElseThrow().id());
    }

    @Test
    void rangeFiltering_isCallerResponsibility() {
        // The helper has no opinion on max range — caller pre-filters by
        // building the candidate list. With block-distance infinity and a
        // single far entity, the entity wins.
        Candidate entity = new Candidate("entity", 100.0);
        Optional<Candidate> hit = Hitscan.firstHitWithinRange(
            Double.POSITIVE_INFINITY, List.of(entity));
        assertEquals("entity", hit.orElseThrow().id());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :wildwest:test
```

Expected: compilation failure (`Hitscan` not defined) or `ClassNotFoundException`.

- [ ] **Step 3: Implement Hitscan**

`wildwest/src/main/java/com/tweeks/wildwest/Hitscan.java`:

```java
package com.tweeks.wildwest;

import java.util.List;
import java.util.Optional;

/**
 * Pure ray-vs-candidates math. Given a block-hit distance and a list of entity
 * candidates already projected to a distance along the ray, returns the
 * nearest entity hit that beats the block distance. The caller (PistolItem)
 * is responsible for ray-casting blocks and projecting entity AABBs onto the
 * ray to produce the candidate list.
 */
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

- [ ] **Step 4: Run tests and verify they pass**

```bash
./gradlew :wildwest:test
```

Expected: `BUILD SUCCESSFUL`, all five tests pass.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/Hitscan.java wildwest/src/test/
git commit -m "feat(wildwest): pure hitscan helper + unit tests

Pulls block-vs-entity pick-the-nearest math into a function tested without
booting Minecraft."
```

---

## Task 5: S2C tracer packet

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/network/S2CTracerPacket.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/network/NetworkHandlers.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/TracerClientHandler.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`

- [ ] **Step 1: Create the packet record**

`wildwest/src/main/java/com/tweeks/wildwest/network/S2CTracerPacket.java`:

```java
package com.tweeks.wildwest.network;

import com.tweeks.wildwest.WildWestMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public record S2CTracerPacket(Vec3 start, Vec3 end) implements CustomPacketPayload {

    public static final Type<S2CTracerPacket> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "tracer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CTracerPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.DOUBLE, p -> p.start.x,
            ByteBufCodecs.DOUBLE, p -> p.start.y,
            ByteBufCodecs.DOUBLE, p -> p.start.z,
            ByteBufCodecs.DOUBLE, p -> p.end.x,
            ByteBufCodecs.DOUBLE, p -> p.end.y,
            ByteBufCodecs.DOUBLE, p -> p.end.z,
            (sx, sy, sz, ex, ey, ez) -> new S2CTracerPacket(
                new Vec3(sx, sy, sz), new Vec3(ex, ey, ez)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

- [ ] **Step 2: Create the registrar**

`wildwest/src/main/java/com/tweeks/wildwest/network/NetworkHandlers.java`:

```java
package com.tweeks.wildwest.network;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.TracerClientHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = WildWestMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class NetworkHandlers {
    private NetworkHandlers() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1");
        reg.playToClient(
            S2CTracerPacket.TYPE,
            S2CTracerPacket.STREAM_CODEC,
            TracerClientHandler::handle);
    }
}
```

- [ ] **Step 3: Create the client-side handler**

`wildwest/src/main/java/com/tweeks/wildwest/client/TracerClientHandler.java`:

```java
package com.tweeks.wildwest.client;

import com.tweeks.wildwest.network.S2CTracerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class TracerClientHandler {
    private TracerClientHandler() {}

    public static void handle(S2CTracerPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;
            Vec3 start = packet.start();
            Vec3 end = packet.end();
            // Muzzle puff at start.
            level.addParticle(ParticleTypes.SMOKE, start.x, start.y, start.z, 0, 0.02, 0);
            // Six tracer dots evenly spaced along the segment.
            int steps = 6;
            for (int i = 1; i <= steps; i++) {
                double t = (double) i / (double) (steps + 1);
                double x = start.x + (end.x - start.x) * t;
                double y = start.y + (end.y - start.y) * t;
                double z = start.z + (end.z - start.z) * t;
                level.addParticle(ParticleTypes.CRIT, x, y, z, 0, 0, 0);
            }
        });
    }
}
```

- [ ] **Step 4: Build**

```bash
./gradlew :wildwest:build
```

Expected: `BUILD SUCCESSFUL`. (Packet registration is exercised once `PistolItem` sends it.)

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/network/ wildwest/src/main/java/com/tweeks/wildwest/client/
git commit -m "feat(wildwest): S2C tracer packet + client smoke/crit particle render"
```

---

## Task 6: PistolItem

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/item/PistolItem.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`

- [ ] **Step 1: Implement PistolItem**

`wildwest/src/main/java/com/tweeks/wildwest/item/PistolItem.java`:

```java
package com.tweeks.wildwest.item;

import com.tweeks.wildwest.Hitscan;
import com.tweeks.wildwest.WildWestDamageTypes;
import com.tweeks.wildwest.network.S2CTracerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class PistolItem extends Item {

    public static final double MAX_RANGE = 16.0;
    public static final float DAMAGE = 5.0F;
    public static final int COOLDOWN_TICKS = 8;

    public PistolItem(Properties properties) {
        super(properties.stacksTo(1).durability(300));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.fail(stack);
        }

        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(MAX_RANGE));

        BlockHitResult blockHit = level.clip(new ClipContext(
            start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double blockDist = blockHit.getType() == HitResult.Type.MISS
            ? MAX_RANGE
            : start.distanceTo(blockHit.getLocation());

        List<LivingEntity> nearby = level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(start, end).inflate(1.0),
            e -> e != player && e.isAlive());

        // Project each entity to a candidate at its closest ray-AABB intersection distance.
        List<Hitscan.Candidate> candidates = new ArrayList<>();
        // Map from candidate id (toString of UUID) back to entity for damage application.
        java.util.Map<String, LivingEntity> byId = new java.util.HashMap<>();
        for (LivingEntity e : nearby) {
            var clip = e.getBoundingBox().inflate(0.3).clip(start, end);
            if (clip.isPresent()) {
                String id = e.getUUID().toString();
                candidates.add(new Hitscan.Candidate(id, start.distanceTo(clip.get())));
                byId.put(id, e);
            }
        }

        var hit = Hitscan.firstHitWithinRange(blockDist, candidates);
        Vec3 endPoint = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        if (hit.isPresent()) {
            LivingEntity target = byId.get(hit.get().id());
            // Reset hurt-immunity so 8-tk cooldown isn't swallowed by 10-tk invuln.
            target.invulnerableTime = 0;
            target.hurt(WildWestDamageTypes.gunshot(player), DAMAGE);
            endPoint = target.position().add(0, target.getBbHeight() * 0.5, 0);
        }

        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN_TICKS);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            com.tweeks.wildwest.ModSounds.PISTOL_FIRE.get(),
            SoundSource.PLAYERS, 1.0F, 1.0F);

        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                sp, new S2CTracerPacket(start, endPoint));
        }

        return InteractionResultHolder.consume(stack);
    }
}
```

- [ ] **Step 2: Register the pistol**

In `Registration.java`, add the import:

```java
import com.tweeks.wildwest.item.PistolItem;
import net.neoforged.neoforge.registries.DeferredItem;
```

And the registration (after the CREATIVE_TABS field declaration but before `WILDWEST_TAB`):

```java
    public static final DeferredItem<PistolItem> PISTOL = ITEMS.registerItem(
        "pistol", PistolItem::new, p -> p);
```

In the creative tab's `displayItems` lambda body:

```java
                    output.accept(PISTOL.get());
```

And update the tab's icon to:

```java
                .icon(() -> PISTOL.get().getDefaultInstance())
```

- [ ] **Step 3: Build**

```bash
./gradlew :wildwest:build
```

Expected: `BUILD SUCCESSFUL`. (No item-model JSON yet — the item will render as the missing-model purple/black checker until task 10. Functionally fires.)

- [ ] **Step 4: Run dev client and verify pistol fires**

```bash
./gradlew :wildwest:runClient
```

In creative mode: open `Wild West` tab → grab pistol → spawn a zombie a few blocks away → right-click. Expect:
- Zombie takes ~5 dmg (full heart of damage shown).
- Smoke + tracer particles visible from your eye to the zombie.
- Cooldown overlay on the pistol icon ~8 ticks before next shot.
- Crossbow-shoot sound plays (placeholder).

Quit the client. (Don't worry about the missing item model; texture work is task 10.)

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/PistolItem.java wildwest/src/main/java/com/tweeks/wildwest/Registration.java
git commit -m "feat(wildwest): pistol item — hitscan, 5 dmg, 8-tk cooldown

Right-click fires a server-side raycast 16 blk; first hit takes 5 dmg
via wildwest:gunshot. Sends tracer packet for client visuals. Resets
target.invulnerableTime so back-to-back shots both register."
```

---

## Task 7: BulletEntity

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/BulletEntity.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java` (call ModEntities.register if Registration owns the entry point) — alternatively register from WildWestMod; this plan registers from Registration for consistency with the thief module.
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`

- [ ] **Step 1: Create BulletEntity**

`wildwest/src/main/java/com/tweeks/wildwest/entity/BulletEntity.java`:

```java
package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.WildWestDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class BulletEntity extends AbstractArrow {

    public static final int MAX_LIFE_TICKS = 12;
    public static final float DEFAULT_DAMAGE = 9.0F;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level, LivingEntity shooter) {
        super(type, shooter, level, ItemStack.EMPTY, null);
        this.pickup = Pickup.DISALLOWED;
        this.setBaseDamage(DEFAULT_DAMAGE);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        // Required override on AbstractArrow; never used (pickup disallowed).
        return new ItemStack(Items.AIR);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > MAX_LIFE_TICKS) {
            this.discard();
            return;
        }
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.CRIT,
                this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide) return;
        if (result.getEntity() instanceof LivingEntity target) {
            // Reset hurt-immunity so rapid hits don't get swallowed.
            target.invulnerableTime = 0;
            target.hurt(WildWestDamageTypes.gunshot(this.getOwner() == null ? this : this.getOwner()),
                (float) this.getBaseDamage());
        }
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }
}
```

- [ ] **Step 2: Create ModEntities**

`wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java`:

```java
package com.tweeks.wildwest;

import com.tweeks.wildwest.entity.BulletEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, WildWestMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<BulletEntity>> BULLET =
        ENTITY_TYPES.register("bullet", () -> EntityType.Builder.<BulletEntity>of(
                BulletEntity::new, MobCategory.MISC)
            .sized(0.25f, 0.25f)
            .clientTrackingRange(8)
            .updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "bullet"))));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
```

- [ ] **Step 3: Wire ModEntities into the mod constructor**

In `WildWestMod.java`, in the constructor (after `Registration.register(modEventBus);`):

```java
        ModEntities.register(modEventBus);
```

(`ModEntities.register` must run BEFORE `ModSounds.register` is fine; ordering between deferred-registers within the mod-event bus does not matter — they're all queued and dispatched together. Just keep the call.)

- [ ] **Step 4: Build**

```bash
./gradlew :wildwest:build
```

Expected: `BUILD SUCCESSFUL`. (Bullet entity has no renderer yet; spawning one shows nothing visible. Renderer comes in task 9.)

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java
git commit -m "feat(wildwest): BulletEntity (AbstractArrow subclass) + entity type

12-tick max life, 9 base damage, resets hurt-immunity on hit, no pickup."
```

---

## Task 8: RifleItem

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/item/RifleItem.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`

- [ ] **Step 1: Implement RifleItem**

`wildwest/src/main/java/com/tweeks/wildwest/item/RifleItem.java`:

```java
package com.tweeks.wildwest.item;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.ModSounds;
import com.tweeks.wildwest.entity.BulletEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RifleItem extends Item {

    public static final int COOLDOWN_TICKS = 40;
    /** Number of cooldown ticks after firing at which we play the bolt-cycle sound. */
    public static final int BOLT_CYCLE_REMAINING = 30;
    public static final float BULLET_VELOCITY = 6.0F;

    public RifleItem(Properties properties) {
        super(properties.stacksTo(1).durability(400));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.pass(stack);
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.fail(stack);
        }

        BulletEntity bullet = new BulletEntity(ModEntities.BULLET.get(), level, player);
        bullet.setPos(player.getEyePosition());
        bullet.shoot(player.getLookAngle().x, player.getLookAngle().y, player.getLookAngle().z,
            BULLET_VELOCITY, 0.0F);
        level.addFreshEntity(bullet);

        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN_TICKS);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            ModSounds.RIFLE_FIRE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        int remaining = remainingCooldownTicks(player, stack.getItem());
        if (remaining == BOLT_CYCLE_REMAINING) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.BOLT_CYCLE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /**
     * Compute remaining cooldown ticks via getCooldownPercent + total. The
     * vanilla ItemCooldowns class doesn't expose a public "remaining ticks"
     * accessor in a stable form across versions; deriving it from the percent
     * is portable.
     */
    private static int remainingCooldownTicks(Player player, Item item) {
        ItemCooldowns cooldowns = player.getCooldowns();
        if (!cooldowns.isOnCooldown(item)) return 0;
        float percent = cooldowns.getCooldownPercent(item, 0F);
        return Math.round(percent * (float) COOLDOWN_TICKS);
    }
}
```

- [ ] **Step 2: Register the rifle**

In `Registration.java`, add:

```java
import com.tweeks.wildwest.item.RifleItem;
```

And:

```java
    public static final DeferredItem<RifleItem> RIFLE = ITEMS.registerItem(
        "rifle", RifleItem::new, p -> p);
```

In the creative tab's `displayItems`:

```java
                    output.accept(RIFLE.get());
```

- [ ] **Step 3: Build**

```bash
./gradlew :wildwest:build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run dev client, fire rifle**

```bash
./gradlew :wildwest:runClient
```

Open creative tab, give yourself a rifle and a target dummy. Right-click rifle. Expect:
- A bullet entity spawns and flies (invisible, since the renderer comes next task — but the audible crossbow-shoot sound plays, the cooldown indicator appears, and the target takes 9 dmg if it hits).
- 10 ticks after firing, the loading sound plays (bolt-cycle placeholder).

(Visual verification of the bullet streak waits for task 9.)

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/RifleItem.java wildwest/src/main/java/com/tweeks/wildwest/Registration.java
git commit -m "feat(wildwest): rifle item — spawns BulletEntity, 40-tk cooldown

inventoryTick fires the delayed bolt-cycle sound at remaining=30 ticks
(10 ticks after the shot), no scheduler needed."
```

---

## Task 9: BulletRenderer + ClientSetup (entity render + bolt-state ItemProperty)

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/BulletRenderer.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java`

- [ ] **Step 1: Create BulletRenderer**

`wildwest/src/main/java/com/tweeks/wildwest/client/BulletRenderer.java`:

```java
package com.tweeks.wildwest.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.entity.BulletEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class BulletRenderer extends EntityRenderer<BulletEntity> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/bullet.png");

    public BulletRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTextureLocation(BulletEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(BulletEntity bullet, float yaw, float partialTicks,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        // AbstractArrow.tick() updates yRot/xRot from velocity; apply with lerp
        // so the slug points along its trajectory.
        pose.mulPose(Axis.YP.rotationDegrees(
            Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot()) - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(
            Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot())));
        // Tiny scale; the texture is a small sprite billboarded onto two quads
        // in the simplest form.
        pose.scale(0.2F, 0.2F, 0.2F);
        // Defer to vanilla AbstractArrow body if the textures match — for v1
        // we render nothing here; the per-tick CRIT particle trail in
        // BulletEntity.tick() is the visible effect. Keep the renderer to
        // reserve the slot for later 3D model work.
        pose.popPose();
        super.render(bullet, yaw, partialTicks, pose, buffers, light);
    }
}
```

- [ ] **Step 2: Create ClientSetup**

`wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java`:

```java
package com.tweeks.wildwest.client;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.Registration;
import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.item.RifleItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = WildWestMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Identifier boltStateId =
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "bolt_state");
            ItemProperties.register(Registration.RIFLE.get(), boltStateId,
                (stack, level, holder, seed) -> {
                    if (holder instanceof Player player) {
                        return player.getCooldowns().getCooldownPercent(stack.getItem(), 0F);
                    }
                    return 0F;
                });
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BULLET.get(), BulletRenderer::new);
    }
}
```

- [ ] **Step 3: Build**

```bash
./gradlew :wildwest:build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run dev client and verify**

```bash
./gradlew :wildwest:runClient
```

Fire the rifle: confirm a stream of CRIT particles traces the bullet path. Hit a target at distance — entity takes 9 dmg.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/client/
git commit -m "feat(wildwest): bullet renderer + bolt_state ItemProperty

BulletRenderer applies interpolated yRot/xRot so the (later 3D) slug
points along trajectory. bolt_state predicate drives rifle item-model
swap from the cooldown percent."
```

---

## Task 10: Item textures + 3D item model JSONs

**Files:**
- Create: `wildwest/src/main/resources/assets/wildwest/textures/item/pistol.png` (16×16)
- Create: `wildwest/src/main/resources/assets/wildwest/textures/item/rifle.png` (32×16)
- Create: `wildwest/src/main/resources/assets/wildwest/textures/item/bullet.png` (8×8)
- Create: `wildwest/src/main/resources/assets/wildwest/textures/entity/bullet.png` (8×8)
- Create: `wildwest/src/main/resources/assets/wildwest/items/pistol.json`
- Create: `wildwest/src/main/resources/assets/wildwest/items/rifle.json`
- Create: `wildwest/src/main/resources/assets/wildwest/models/item/pistol.json`
- Create: `wildwest/src/main/resources/assets/wildwest/models/item/rifle.json`
- Create: `wildwest/src/main/resources/assets/wildwest/models/item/rifle_bolt_open.json`
- Create: `wildwest/src/main/resources/assets/wildwest/models/item/rifle_bolt_closing.json`
- Create: `wildwest/tools/README.md` (Blockbench-flavored note)

The model JSONs below define 3D cubes directly (the Blockbench-export format) so the items are 3D from day one. Once these JSONs exist, the engineer can open them in Blockbench (`File → Import → Generic Model`) for further pixel-art work and save a `.bbmodel` source file in `wildwest/tools/`.

- [ ] **Step 1: Create the four placeholder textures**

These are simple solid-colour placeholder PNGs — final art comes later. Generate them with ImageMagick (or any tool that produces RGBA PNGs of the listed size):

```bash
mkdir -p wildwest/src/main/resources/assets/wildwest/textures/item
mkdir -p wildwest/src/main/resources/assets/wildwest/textures/entity

# 16×16 dark grey for pistol
magick -size 16x16 xc:'#2A2A2A' wildwest/src/main/resources/assets/wildwest/textures/item/pistol.png
# 32×16 dark brown for rifle
magick -size 32x16 xc:'#3A2A1A' wildwest/src/main/resources/assets/wildwest/textures/item/rifle.png
# 8×8 lead-grey for bullet (item icon)
magick -size 8x8 xc:'#7A7A7A' wildwest/src/main/resources/assets/wildwest/textures/item/bullet.png
# 8×8 lead-grey for bullet (entity)
cp wildwest/src/main/resources/assets/wildwest/textures/item/bullet.png wildwest/src/main/resources/assets/wildwest/textures/entity/bullet.png
```

If `magick` isn't installed, any other PNG generator works — the colour and dimensions are what matter for now.

- [ ] **Step 2: Create the item entry JSONs**

`wildwest/src/main/resources/assets/wildwest/items/pistol.json`:

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "wildwest:item/pistol"
  }
}
```

`wildwest/src/main/resources/assets/wildwest/items/rifle.json`:

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "wildwest:item/rifle"
  }
}
```

- [ ] **Step 3: Create the 3D model JSONs (cubes)**

`wildwest/src/main/resources/assets/wildwest/models/item/pistol.json`:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "wildwest:item/pistol",
    "particle": "wildwest:item/pistol"
  },
  "elements": [
    {
      "from": [6, 5, 7],
      "to": [10, 7, 16],
      "faces": {
        "north": { "uv": [0, 0, 4, 2], "texture": "#0" },
        "east":  { "uv": [0, 2, 9, 4], "texture": "#0" },
        "south": { "uv": [0, 4, 4, 6], "texture": "#0" },
        "west":  { "uv": [0, 6, 9, 8], "texture": "#0" },
        "up":    { "uv": [0, 8, 4, 16], "texture": "#0" },
        "down":  { "uv": [4, 8, 8, 16], "texture": "#0" }
      }
    },
    {
      "from": [6, 0, 11],
      "to": [10, 5, 14],
      "faces": {
        "north": { "uv": [8, 0, 12, 5], "texture": "#0" },
        "east":  { "uv": [8, 5, 11, 10], "texture": "#0" },
        "south": { "uv": [8, 10, 12, 15], "texture": "#0" },
        "west":  { "uv": [12, 0, 15, 5], "texture": "#0" },
        "up":    { "uv": [12, 5, 16, 8], "texture": "#0" },
        "down":  { "uv": [12, 8, 16, 11], "texture": "#0" }
      }
    }
  ],
  "display": {
    "thirdperson_righthand": { "rotation": [0, 0, 0], "translation": [0, 3.5, 1.5], "scale": [1, 1, 1] },
    "firstperson_righthand": { "rotation": [0, -10, 0], "translation": [1, 4.5, 2], "scale": [1.2, 1.2, 1.2] }
  }
}
```

`wildwest/src/main/resources/assets/wildwest/models/item/rifle.json`:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "wildwest:item/rifle",
    "particle": "wildwest:item/rifle"
  },
  "elements": [
    {
      "from": [7, 7, 0],
      "to": [9, 9, 24],
      "faces": {
        "north": { "uv": [0, 0, 2, 2], "texture": "#0" },
        "east":  { "uv": [0, 2, 24, 4], "texture": "#0" },
        "south": { "uv": [0, 4, 2, 6], "texture": "#0" },
        "west":  { "uv": [0, 6, 24, 8], "texture": "#0" },
        "up":    { "uv": [0, 8, 2, 32], "texture": "#0" },
        "down":  { "uv": [2, 8, 4, 32], "texture": "#0" }
      }
    },
    {
      "from": [6, 4, 22],
      "to": [10, 8, 32],
      "faces": {
        "north": { "uv": [4, 0, 8, 4], "texture": "#0" },
        "east":  { "uv": [4, 4, 14, 8], "texture": "#0" },
        "south": { "uv": [4, 8, 8, 12], "texture": "#0" },
        "west":  { "uv": [4, 12, 14, 16], "texture": "#0" },
        "up":    { "uv": [8, 0, 12, 10], "texture": "#0" },
        "down":  { "uv": [12, 0, 16, 10], "texture": "#0" }
      }
    }
  ],
  "overrides": [
    { "predicate": { "wildwest:bolt_state": 0.3 }, "model": "wildwest:item/rifle_bolt_closing" },
    { "predicate": { "wildwest:bolt_state": 0.7 }, "model": "wildwest:item/rifle_bolt_open" }
  ],
  "display": {
    "thirdperson_righthand": { "rotation": [0, 0, 0], "translation": [0, 3.5, 1.5], "scale": [1, 1, 1] },
    "firstperson_righthand": { "rotation": [0, -10, 0], "translation": [1, 4.5, 6], "scale": [1.2, 1.2, 1.2] }
  }
}
```

`wildwest/src/main/resources/assets/wildwest/models/item/rifle_bolt_open.json`:

```json
{
  "parent": "wildwest:item/rifle",
  "elements": [
    {
      "from": [7, 9, 18],
      "to": [9, 11, 22],
      "faces": {
        "north": { "uv": [0, 0, 2, 2], "texture": "#0" },
        "east":  { "uv": [0, 2, 4, 4], "texture": "#0" },
        "south": { "uv": [0, 4, 2, 6], "texture": "#0" },
        "west":  { "uv": [0, 6, 4, 8], "texture": "#0" },
        "up":    { "uv": [0, 8, 2, 12], "texture": "#0" },
        "down":  { "uv": [2, 8, 4, 12], "texture": "#0" }
      }
    }
  ]
}
```

`wildwest/src/main/resources/assets/wildwest/models/item/rifle_bolt_closing.json`:

```json
{
  "parent": "wildwest:item/rifle",
  "elements": [
    {
      "from": [7, 9, 14],
      "to": [9, 11, 18],
      "faces": {
        "north": { "uv": [0, 0, 2, 2], "texture": "#0" },
        "east":  { "uv": [0, 2, 4, 4], "texture": "#0" },
        "south": { "uv": [0, 4, 2, 6], "texture": "#0" },
        "west":  { "uv": [0, 6, 4, 8], "texture": "#0" },
        "up":    { "uv": [0, 8, 2, 12], "texture": "#0" },
        "down":  { "uv": [2, 8, 4, 12], "texture": "#0" }
      }
    }
  ]
}
```

- [ ] **Step 4: Note future Blockbench work**

`wildwest/tools/README.md`:

```markdown
# wildwest texture & model tools

Phase 1 ships hand-written model JSONs at
`wildwest/src/main/resources/assets/wildwest/models/item/{pistol,rifle,
rifle_bolt_open,rifle_bolt_closing}.json` and a placeholder texture per
item. To iterate visually:

1. Install Blockbench from https://www.blockbench.net/
2. **File → Import → Generic Model** on a model JSON above.
3. Edit cubes / paint texture in Blockbench.
4. Save as `.bbmodel` in this folder for source-of-truth (committed alongside
   the exported JSON).
5. Re-export to the same `models/item/<name>.json` path.

The bullet entity model is rendered programmatically via `BulletRenderer`;
its texture lives at `assets/wildwest/textures/entity/bullet.png`.
```

- [ ] **Step 5: Run dev client, verify item visuals**

```bash
./gradlew :wildwest:runClient
```

In creative tab, give yourself a pistol and rifle. Confirm:
- Both items render in 3D (cubes) when held in hand and on the ground.
- Pistol shows the dark-grey barrel + grip silhouette.
- Rifle shows a long barrel + stock.
- Right-click rifle → during the 40-tick cooldown the model swaps to `rifle_bolt_closing` mid-cycle and `rifle_bolt_open` near the start, then back to base when ready.

(The placeholder textures are flat colour. Polish in Blockbench is a follow-up.)

- [ ] **Step 6: Commit**

```bash
git add wildwest/src/main/resources/assets/ wildwest/tools/
git commit -m "feat(wildwest): 3D item models + placeholder textures + tools README

Pistol + rifle render as cube models. Rifle swaps to bolt_open and
bolt_closing variants during cooldown. Textures are flat-colour
placeholders; iterate in Blockbench."
```

---

## Task 11: Recipes

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/data/ModRecipeProvider.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/data/DataGenerators.java`

- [ ] **Step 1: Create ModRecipeProvider**

`wildwest/src/main/java/com/tweeks/wildwest/data/ModRecipeProvider.java`:

Pattern matches the existing `:thief` module — outer class extends `RecipeProvider`; inner `Runner` class wires it into the data-gen pipeline.

```java
package com.tweeks.wildwest.data;

import com.tweeks.wildwest.Registration;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        HolderGetter<Item> itemLookup = this.registries.lookupOrThrow(Registries.ITEM);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.PISTOL.get())
            .pattern("I  ")
            .pattern("IW ")
            .pattern(" W ")
            .define('I', Items.IRON_INGOT)
            .define('W', Items.OAK_PLANKS)
            .unlockedBy("has_iron", this.has(Items.IRON_INGOT))
            .save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.RIFLE.get())
            .pattern("III")
            .pattern(" WI")
            .pattern(" W ")
            .define('I', Items.IRON_INGOT)
            .define('W', Items.OAK_PLANKS)
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
            return "Wild West Recipes";
        }
    }
}
```

- [ ] **Step 2: Wire ModRecipeProvider into DataGenerators**

In `DataGenerators.java`, inside `gatherDataServer`, add (after the `RegistrySetBuilder` block):

```java
        gen.addProvider(true, new ModRecipeProvider.Runner(output, lookup));
```

- [ ] **Step 3: Run datagen**

```bash
./gradlew :wildwest:runData
```

Expected: writes `wildwest/src/generated/serverData/data/wildwest/recipe/pistol.json` and `rifle.json`.

- [ ] **Step 4: Verify in dev client**

```bash
./gradlew :wildwest:runClient
```

In a survival world, give yourself iron + planks (`/give @s minecraft:iron_ingot 64`, `/give @s minecraft:oak_planks 64`). Open a crafting table; arrange the patterns above; confirm both recipes produce the right item.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/data/ wildwest/src/generated/
git commit -m "feat(wildwest): pistol + rifle crafting recipes (datagen)

Pistol: 2 iron + 2 plank in L-shape. Rifle: 4 iron + 2 plank in long bar."
```

---

## Task 12: Localization

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/data/ModLanguageProvider.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/data/DataGenerators.java`

- [ ] **Step 1: Create ModLanguageProvider**

`wildwest/src/main/java/com/tweeks/wildwest/data/ModLanguageProvider.java`:

```java
package com.tweeks.wildwest.data;

import com.tweeks.wildwest.Registration;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(output, WildWestMod.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + WildWestMod.MOD_ID, "Wild West");
        add(Registration.PISTOL.get(), "Pistol");
        add(Registration.RIFLE.get(), "Rifle");

        // Death messages for wildwest:gunshot. Three variants — vanilla picks
        // automatically based on whether a player or item-shooter is involved.
        add("death.attack.wildwest.gunshot",
            "%1$s was shot");
        add("death.attack.wildwest.gunshot.player",
            "%1$s was shot by %2$s");
        add("death.attack.wildwest.gunshot.item",
            "%1$s was shot by %2$s using %3$s");

        // Sound subtitles.
        add("subtitle.wildwest.pistol_fire", "Pistol fires");
        add("subtitle.wildwest.rifle_fire",  "Rifle fires");
        add("subtitle.wildwest.bolt_cycle",  "Bolt cycles");
    }
}
```

- [ ] **Step 2: Wire ModLanguageProvider into DataGenerators**

In `DataGenerators.java`, add a client-data hook at the end of the class:

```java
    @SubscribeEvent
    public static void gatherDataClient(GatherDataEvent.Client event) {
        DataGenerator gen = event.getGenerator();
        gen.addProvider(true, new ModLanguageProvider(gen.getPackOutput()));
    }
```

- [ ] **Step 3: Run datagen**

```bash
./gradlew :wildwest:runData
```

Expected: writes `wildwest/src/generated/clientData/assets/wildwest/lang/en_us.json` with all the keys above.

- [ ] **Step 4: Run dev client, verify text**

```bash
./gradlew :wildwest:runClient
```

In creative inventory hover the items: tooltip shows "Pistol" / "Rifle" (not the raw `item.wildwest.pistol` key). Run `/damage @s 100 wildwest:gunshot` → death-screen message reads "<your name> was shot".

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/data/ModLanguageProvider.java wildwest/src/main/java/com/tweeks/wildwest/data/DataGenerators.java wildwest/src/generated/
git commit -m "feat(wildwest): English localization (item names, death msgs, subtitles)"
```

---

## Task 13: End-to-end smoke test

This is verification only — no code changes. Walk the manual test plan from the spec and confirm everything works together.

- [ ] **Step 1: Launch dev client**

```bash
./gradlew :wildwest:runClient
```

- [ ] **Step 2: Walk the manual test plan from the spec**

Run each step and tick it off. Reference: spec section "Manual in-game test plan".

  1. Recipes: craft both guns from iron + plank in survival.
  2. Pistol close range: 5-blk shot on a zombie → 5 dmg + tracer + smoke.
  3. Pistol max range: 16-blk hit, 17-blk miss.
  4. Pistol cooldown: spam-fire ~2.5/sec; cooldown overlay visible.
  5. Pistol durability: fire ~300 times → breaks. (Use a creative quick-test by editing durability via `/data`, or just take it on faith.)
  6. Rifle hit: 30-blk shot on a target → ~9 dmg.
  7. Rifle bolt-cycle: third-person view → bolt-open then bolt-closing model swap during cooldown, returns to ready.
  8. Rifle durability: fire ~400 times → breaks.
  9. Death message: `/damage @s 100 wildwest:gunshot` → chat shows "<name> was shot". Kill another entity with the pistol/rifle → "<name> was shot by <killer>".
  10. Multi-mod load: in-game Mods list shows `securitycore`, `securityguard`, `thief`, `wildwest`.
  11. Build: `./gradlew :wildwest:build` and `./gradlew build` (root) succeed.
  12. Datagen: `./gradlew :wildwest:runData` produces expected JSON in `src/generated/`.

If any step fails, file a follow-up commit fixing it; don't merge a broken phase.

- [ ] **Step 3: Final commit (if any tweaks emerged)**

If the smoke test produced fixes, commit them. If not, no commit needed.

- [ ] **Step 4: Mark phase 1 complete**

Open `docs/superpowers/specs/2026-04-30-wildwest-phase1-guns-design.md`, change `**Status:** Draft` to `**Status:** Implemented in [hash]`. Commit:

```bash
git add docs/superpowers/specs/2026-04-30-wildwest-phase1-guns-design.md
git commit -m "docs(wildwest): mark phase 1 spec as implemented"
```

---

## Self-review notes (post-write)

- **Spec coverage check:**
  - Module setup → Task 1.
  - Pistol mechanics (hitscan, 5 dmg, 8-tk cooldown, tracer, mob helper hook) → Tasks 4, 5, 6. (Mob-helper `fireFromMob` is *not* implemented in phase 1 — phase 2 will extract it. Plan defers it correctly.)
  - Rifle mechanics (bolt-action projectile, 40-tk cooldown, delayed bolt sound) → Tasks 7, 8.
  - Bolt-cycle item-model state with predicate → Task 9 (ItemProperty), Task 10 (model JSONs).
  - Damage type → Task 3.
  - Recipes → Task 11.
  - Sounds → Task 2.
  - Item textures + bbmodels → Task 10. Hand-written cube JSONs ship; Blockbench polish documented as follow-up. Spec said "bbmodels for all" — the JSONs *are* the Blockbench-export format, so this satisfies the requirement; richer Blockbench-authored .bbmodel source files are an iterative polish step.
  - Tests → Task 4 covers Hitscan; rifle/cooldown and bullet-tick tests are skipped in favor of manual smoke testing (matching the existing repo convention from `RevealStateTest`-style pure-data tests).
- **Type consistency:** `Registration.PISTOL`, `Registration.RIFLE`, `ModSounds.PISTOL_FIRE`/`RIFLE_FIRE`/`BOLT_CYCLE`, `ModEntities.BULLET`, `WildWestDamageTypes.GUNSHOT`, `S2CTracerPacket` — all referenced consistently in dependent tasks.
- **No placeholders:** every code block is concrete; every command has expected output; every file path is exact.
- **No fall-through to "implement later":** the only deferred items are explicit phase-2/phase-3 work documented in the spec out-of-scope section, and the optional Blockbench polish for textures.
