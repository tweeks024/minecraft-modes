# Infinity Gauntlet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the `wildwest:infinity_gauntlet` end-game item: six selectable stones (Power, Space, Time, Mind, Reality, Soul) each with its own active ability, per-stone cooldowns, radial picker UX (default key `G`), crafted from end-game ingredients.

**Architecture:** Single Java `Item` whose right-click dispatches to one of six `InfinityStone` enum values. Active stone + per-stone cooldown timestamps live on the `ItemStack` as `DataComponent`s. A custom client `Screen` (radial picker) sends a C2S packet to mutate the active-stone component. Two mob-side `AttachmentType`s back the stateful stones (Mind charm, Reality bubble) via a server tick handler.

**Tech Stack:** NeoForge 26.1.2, Minecraft 26.1.2, Java 21, JUnit 5 for unit tests, vanilla data-generators for damage_type + recipe JSON.

**Spec:** `docs/superpowers/specs/2026-06-06-infinity-gauntlet-design.md`

---

## File Structure Overview

**New files (production code):**

| File | Responsibility |
|---|---|
| `item/InfinityStone.java` | Enum: 6 stones with their constants (cooldown, color, durability cost, sound, particle name). Pure data; no behavior. |
| `item/InfinityStoneAbility.java` | Interface `cast(ServerLevel, ServerPlayer, ItemStack) -> CastResult`. One impl per stone, kept inside `InfinityStone` enum constants. |
| `item/InfinityGauntletItem.java` | Item subclass: `use()`, `getName()`, `appendHoverText()`. Reads/writes stack components. |
| `item/ModDataComponents.java` | `DataComponentType` registry: `ACTIVE_STONE`, `COOLDOWNS`. |
| `effect/MindCharmAttachment.java` | Record `{UUID casterUuid, long expiresAtTick}`. Per-mob attachment. |
| `effect/RealityBubbleAttachment.java` | Record `{CompoundTag originalNbt, String originalTypeId, long expiresAtTick}`. Per-bat attachment. |
| `effect/ModAttachments.java` | `AttachmentType` registry for the two attachments above. |
| `effect/EffectTickHandler.java` | `@EventBusSubscriber` on `ServerTickEvent.Post`: walks every loaded `ServerLevel`, ticks Mind/Reality effects, restores on expiry. |
| `network/C2SSetActiveStonePacket.java` | Record `{int stoneIndex, boolean mainHand}`. Server handler sets `ACTIVE_STONE` on held stack. |
| `client/InfinityGauntletKeybind.java` | KeyMapping registration + client tick handler that opens radial picker on press. |
| `client/RadialPickerScreen.java` | `Screen` subclass: renders 6 colored wedges; resolves wedge from mouse; sends packet on click/close. |

**New files (resources):**

| File | Purpose |
|---|---|
| `assets/wildwest/models/item/infinity_gauntlet.json` | Item model JSON pointing at texture. |
| `assets/wildwest/textures/item/infinity_gauntlet.png` | 16×16 placeholder (gold gauntlet w/ six dots). |

**Modified files:**

| File | Change |
|---|---|
| `Registration.java` | Register `INFINITY_GAUNTLET` item; add to creative tab. |
| `WildWestMod.java` | Wire `ModDataComponents.register(modEventBus)` and `ModAttachments.register(modEventBus)`. |
| `WildWestDamageTypes.java` | Add `INFINITY_POWER` + `INFINITY_SOUL` resource keys + factories. |
| `data/ModDamageTypeProvider.java` | Bootstrap entries for the two new damage types. |
| `data/ModRecipeProvider.java` | Shaped recipe. |
| `data/ModLanguageProvider.java` | All English strings. |
| `network/NetworkHandlers.java` | Register `C2SSetActiveStonePacket` `playToServer`. |

**Test files:**

| File | Tests |
|---|---|
| `src/test/java/com/tweeks/wildwest/item/InfinityStoneTest.java` | Stone enum constants, indexing. |
| `src/test/java/com/tweeks/wildwest/item/InfinityGauntletCooldownTest.java` | Pure cooldown logic (`isOnCooldown`, `applyCooldown`). |
| `src/test/java/com/tweeks/wildwest/client/RadialMathTest.java` | Angle → wedge index math. |

---

## Phase 1 — Foundation (Stones enum, components, empty item)

### Task 1: Create `InfinityStone` enum with constants

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/item/InfinityStone.java`
- Create: `wildwest/src/test/java/com/tweeks/wildwest/item/InfinityStoneTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.tweeks.wildwest.item;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InfinityStoneTest {

    @Test
    void hasExactlySixStonesInDeclaredOrder() {
        InfinityStone[] stones = InfinityStone.values();
        assertEquals(6, stones.length);
        assertEquals("POWER",   stones[0].name());
        assertEquals("SPACE",   stones[1].name());
        assertEquals("TIME",    stones[2].name());
        assertEquals("MIND",    stones[3].name());
        assertEquals("REALITY", stones[4].name());
        assertEquals("SOUL",    stones[5].name());
    }

    @Test
    void byIndex_returnsCorrectStone() {
        assertSame(InfinityStone.POWER,   InfinityStone.byIndex(0));
        assertSame(InfinityStone.SOUL,    InfinityStone.byIndex(5));
    }

    @Test
    void byIndex_outOfRange_returnsPowerAsDefault() {
        assertSame(InfinityStone.POWER, InfinityStone.byIndex(-1));
        assertSame(InfinityStone.POWER, InfinityStone.byIndex(6));
        assertSame(InfinityStone.POWER, InfinityStone.byIndex(99));
    }

    @Test
    void cooldownsArePositiveTickCounts() {
        for (InfinityStone s : InfinityStone.values()) {
            assertTrue(s.cooldownTicks() > 0, s.name() + " must have positive cooldown");
            assertTrue(s.durabilityCost() > 0, s.name() + " must have positive durability cost");
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew test --tests com.tweeks.wildwest.item.InfinityStoneTest
```
Expected: compile failure ("cannot find symbol InfinityStone").

- [ ] **Step 3: Write minimal implementation**

```java
package com.tweeks.wildwest.item;

/**
 * The six Infinity Stones. Each constant carries the static config for
 * its ability: cooldown, durability cost, color (used for wedge fill +
 * particles), and a translation-key suffix.
 *
 * <p>The actual {@code cast(...)} logic lives in {@link InfinityGauntletItem}
 * keyed off the enum — the enum stays a pure value type for testability.
 */
public enum InfinityStone {

    POWER  (400, 2, 0xA020F0, "power"),
    SPACE  (300, 3, 0x1E90FF, "space"),
    TIME   (600, 4, 0x32CD32, "time"),
    MIND   (500, 3, 0xFFD700, "mind"),
    REALITY(200, 1, 0xFF4500, "reality"),
    SOUL   (240, 2, 0xFFA500, "soul");

    private final int cooldownTicks;
    private final int durabilityCost;
    private final int colorRgb;
    private final String translationSuffix;

    InfinityStone(int cooldownTicks, int durabilityCost, int colorRgb, String translationSuffix) {
        this.cooldownTicks = cooldownTicks;
        this.durabilityCost = durabilityCost;
        this.colorRgb = colorRgb;
        this.translationSuffix = translationSuffix;
    }

    public int cooldownTicks() { return cooldownTicks; }
    public int durabilityCost() { return durabilityCost; }
    public int colorRgb() { return colorRgb; }
    public String translationSuffix() { return translationSuffix; }

    public static InfinityStone byIndex(int index) {
        InfinityStone[] all = values();
        if (index < 0 || index >= all.length) return POWER;
        return all[index];
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew test --tests com.tweeks.wildwest.item.InfinityStoneTest
```
Expected: 4 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/InfinityStone.java wildwest/src/test/java/com/tweeks/wildwest/item/InfinityStoneTest.java
git commit -m "feat(wildwest): InfinityStone enum with six stones + constants"
```

---

### Task 2: Register `DataComponentType`s for active stone + cooldowns

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/item/ModDataComponents.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java` (add register call)

- [ ] **Step 1: Create the registry class**

```java
package com.tweeks.wildwest.item;

import com.mojang.serialization.Codec;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, WildWestMod.MOD_ID);

    /** Active stone index 0..5. Defaults to 0 (POWER) via {@code InfinityStone.byIndex}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ACTIVE_STONE =
        COMPONENTS.registerComponentType(
            "active_stone",
            builder -> builder
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.VAR_INT));

    /**
     * Per-stone cooldown timestamps as a {@code long[]} of length 6, where
     * each entry is the {@code Level#gameTime()} at which that stone becomes
     * available again. Absent component is treated as "all zero" — no
     * cooldown active. We use a plain {@code long[]} via the registry codec.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<long[]>> COOLDOWNS =
        COMPONENTS.registerComponentType(
            "cooldowns",
            builder -> builder
                .persistent(Codec.LONG.listOf().xmap(
                    list -> list.stream().mapToLong(Long::longValue).toArray(),
                    arr -> java.util.Arrays.stream(arr).boxed().toList()))
                .networkSynchronized(StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()),
                    arr -> java.util.Arrays.stream(arr).boxed().toList(),
                    list -> list.stream().mapToLong(Long::longValue).toArray())));

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
```

- [ ] **Step 2: Wire the registry into mod init**

In `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`, in the constructor right after `Registration.register(modEventBus);` add:

```java
        com.tweeks.wildwest.item.ModDataComponents.register(modEventBus);
```

- [ ] **Step 3: Compile**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew compileJava
```
Expected: BUILD SUCCESSFUL. If the `Codec.LONG.listOf().xmap(...)` line doesn't compile (NeoForge 26.1.2 may not expose `Codec.LONG` at that exact path), substitute:

```java
.persistent(Codec.LONG_STREAM.xmap(
    longStream -> longStream.toArray(),
    arr -> java.util.stream.LongStream.of(arr)))
```

…or fall back to `Codec.list(Codec.LONG).xmap(...)`. Try the simpler form first; if it fails, use whichever variant compiles.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/ModDataComponents.java wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java
git commit -m "feat(wildwest): register ACTIVE_STONE + COOLDOWNS data components"
```

---

### Task 3: Pure cooldown logic + tests

We extract cooldown reading/writing into static helpers we can unit test (no `ItemStack` needed — tests work directly with `long[]`).

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/item/InfinityCooldowns.java`
- Create: `wildwest/src/test/java/com/tweeks/wildwest/item/InfinityGauntletCooldownTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.tweeks.wildwest.item;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InfinityGauntletCooldownTest {

    @Test
    void allZeros_neverOnCooldown() {
        long[] cds = new long[6];
        for (int i = 0; i < 6; i++) {
            assertFalse(InfinityCooldowns.isOnCooldown(cds, i, 1000L));
        }
    }

    @Test
    void applyCooldown_setsExpiryAtNowPlusTicks() {
        long[] cds = new long[6];
        long[] next = InfinityCooldowns.applyCooldown(cds, /*stoneIdx*/ 2, /*nowTick*/ 1000L, /*cdTicks*/ 600);
        assertEquals(1600L, next[2]);
        assertEquals(0L, next[0]);
        assertEquals(0L, next[5]);
    }

    @Test
    void onCooldown_whenNowBeforeExpiry() {
        long[] cds = { 0, 0, 1600L, 0, 0, 0 };
        assertTrue(InfinityCooldowns.isOnCooldown(cds, 2, 1500L));
        assertFalse(InfinityCooldowns.isOnCooldown(cds, 2, 1600L));
        assertFalse(InfinityCooldowns.isOnCooldown(cds, 2, 1700L));
    }

    @Test
    void emptyArray_treatedAsAllZeros() {
        long[] empty = new long[0];
        for (int i = 0; i < 6; i++) {
            assertFalse(InfinityCooldowns.isOnCooldown(empty, i, 1000L));
        }
    }

    @Test
    void applyCooldown_onEmptyArray_returnsFullSixSlots() {
        long[] next = InfinityCooldowns.applyCooldown(new long[0], 3, 500L, 200);
        assertEquals(6, next.length);
        assertEquals(700L, next[3]);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew test --tests com.tweeks.wildwest.item.InfinityGauntletCooldownTest
```
Expected: compile failure ("cannot find symbol InfinityCooldowns").

- [ ] **Step 3: Write minimal implementation**

```java
package com.tweeks.wildwest.item;

import java.util.Arrays;

/**
 * Pure helpers for the per-stone cooldown {@code long[]} stored on the
 * gauntlet stack's {@link ModDataComponents#COOLDOWNS} component. Tests
 * cover these directly; the {@code Item} class is a thin wrapper.
 */
public final class InfinityCooldowns {
    private InfinityCooldowns() {}

    public static final int SLOT_COUNT = 6;

    public static boolean isOnCooldown(long[] cooldowns, int stoneIndex, long nowTick) {
        if (cooldowns == null || stoneIndex < 0 || stoneIndex >= cooldowns.length) {
            return false;
        }
        return nowTick < cooldowns[stoneIndex];
    }

    /**
     * Returns a new {@code long[]} of length {@link #SLOT_COUNT} with
     * {@code [stoneIndex]} set to {@code nowTick + cooldownTicks}. Other
     * entries copied from {@code cooldowns} (zero-padded if shorter).
     */
    public static long[] applyCooldown(long[] cooldowns, int stoneIndex, long nowTick, int cooldownTicks) {
        long[] next = new long[SLOT_COUNT];
        if (cooldowns != null) {
            System.arraycopy(cooldowns, 0, next, 0, Math.min(cooldowns.length, SLOT_COUNT));
        }
        if (stoneIndex >= 0 && stoneIndex < SLOT_COUNT) {
            next[stoneIndex] = nowTick + cooldownTicks;
        }
        return next;
    }

    public static long[] emptyCooldowns() {
        return new long[SLOT_COUNT];
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew test --tests com.tweeks.wildwest.item.InfinityGauntletCooldownTest
```
Expected: 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/InfinityCooldowns.java wildwest/src/test/java/com/tweeks/wildwest/item/InfinityGauntletCooldownTest.java
git commit -m "feat(wildwest): pure cooldown helpers for infinity gauntlet"
```

---

### Task 4: Empty `InfinityGauntletItem` + registration

This task creates a registered-but-non-functional item so we can confirm in-game presence before adding behavior.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/item/InfinityGauntletItem.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`

- [ ] **Step 1: Create the item class**

```java
package com.tweeks.wildwest.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The Infinity Gauntlet. Six stones, each with its own active ability.
 *
 * <p>State lives on the stack via two {@link ModDataComponents}:
 * {@code ACTIVE_STONE} (index 0..5) and {@code COOLDOWNS} (long[6]).
 *
 * <p>This skeleton registers the item only — abilities and tooltip are
 * filled in by later tasks.
 */
public class InfinityGauntletItem extends Item {

    public static final int DURABILITY = 500;

    public InfinityGauntletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }
}
```

- [ ] **Step 2: Register in Registration.java**

In `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`, add the import:

```java
import com.tweeks.wildwest.item.InfinityGauntletItem;
```

Add the deferred item registration (after `PISTON_GAUNTLET`):

```java
    public static final DeferredItem<InfinityGauntletItem> INFINITY_GAUNTLET = ITEMS.registerItem(
        "infinity_gauntlet",
        InfinityGauntletItem::new,
        p -> p.stacksTo(1).durability(InfinityGauntletItem.DURABILITY).rarity(Rarity.EPIC));
```

Add to `WILDWEST_TAB.displayItems` (anywhere in the lambda, by convention after `PISTON_GAUNTLET`):

```java
                    output.accept(INFINITY_GAUNTLET.get());
```

- [ ] **Step 3: Compile and run tests**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew compileJava test
```
Expected: BUILD SUCCESSFUL, all existing tests still pass.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/InfinityGauntletItem.java wildwest/src/main/java/com/tweeks/wildwest/Registration.java
git commit -m "feat(wildwest): register InfinityGauntlet item (no behavior yet)"
```

---

## Phase 2 — Damage types

### Task 5: Add `INFINITY_POWER` and `INFINITY_SOUL` damage types

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestDamageTypes.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/data/ModDamageTypeProvider.java`

- [ ] **Step 1: Add resource keys + factories to WildWestDamageTypes.java**

After the `PISTON_PUNCH` resource key block, add:

```java
    public static final ResourceKey<DamageType> INFINITY_POWER = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "infinity_power"));

    public static final ResourceKey<DamageType> INFINITY_SOUL = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "infinity_soul"));
```

After the `pistonPunch(...)` factory method, add:

```java
    public static DamageSource infinityPower(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(INFINITY_POWER),
            attacker);
    }

    public static DamageSource infinitySoul(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(INFINITY_SOUL),
            attacker);
    }
```

- [ ] **Step 2: Add bootstrap entries to ModDamageTypeProvider.java**

After the `PISTON_PUNCH` register line in `bootstrap()`, add:

```java
        ctx.register(WildWestDamageTypes.INFINITY_POWER,
            new DamageType("wildwest.infinity_power", 0.1f));
        ctx.register(WildWestDamageTypes.INFINITY_SOUL,
            new DamageType("wildwest.infinity_soul", 0.1f));
```

- [ ] **Step 3: Run data generators**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew runData
```
Expected: BUILD SUCCESSFUL. New files appear:
- `wildwest/src/generated/serverData/data/wildwest/damage_type/infinity_power.json`
- `wildwest/src/generated/serverData/data/wildwest/damage_type/infinity_soul.json`

- [ ] **Step 4: Verify the generated JSONs**

```bash
cat wildwest/src/generated/serverData/data/wildwest/damage_type/infinity_power.json
cat wildwest/src/generated/serverData/data/wildwest/damage_type/infinity_soul.json
```
Expected: each contains `"message_id": "wildwest.infinity_power"` (or soul) and `"exhaustion": 0.1`.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/WildWestDamageTypes.java wildwest/src/main/java/com/tweeks/wildwest/data/ModDamageTypeProvider.java wildwest/src/generated/serverData/data/wildwest/damage_type/infinity_power.json wildwest/src/generated/serverData/data/wildwest/damage_type/infinity_soul.json
git commit -m "feat(wildwest): infinity_power + infinity_soul damage types"
```

---

## Phase 3 — Power stone (proves the ability dispatch pattern)

### Task 6: Implement Power stone ability + wire dispatch in `use()`

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/item/InfinityGauntletItem.java`

- [ ] **Step 1: Replace the empty `use()` with dispatch logic + Power impl**

Replace the entire body of `InfinityGauntletItem.java` with:

```java
package com.tweeks.wildwest.item;

import com.tweeks.wildwest.WildWestDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class InfinityGauntletItem extends Item {

    public static final int DURABILITY = 500;

    public InfinityGauntletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InfinityStone stone = InfinityStone.byIndex(
            stack.getOrDefault(ModDataComponents.ACTIVE_STONE.get(), 0));

        long[] cds = stack.getOrDefault(
            ModDataComponents.COOLDOWNS.get(), InfinityCooldowns.emptyCooldowns());
        long now = level.getGameTime();
        if (InfinityCooldowns.isOnCooldown(cds, stone.ordinal(), now)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return InteractionResult.CONSUME;
        }

        boolean success = castStone(stone, (ServerLevel) level, (ServerPlayer) player, stack);
        if (!success) {
            return InteractionResult.PASS;
        }

        long[] nextCds = InfinityCooldowns.applyCooldown(cds, stone.ordinal(), now, stone.cooldownTicks());
        stack.set(ModDataComponents.COOLDOWNS.get(), nextCds);
        player.getCooldowns().addCooldown(stack, stone.cooldownTicks());

        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND
            ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(stone.durabilityCost(), player, slot);
        player.swing(hand);

        return InteractionResult.CONSUME;
    }

    /** Dispatch to the stone's ability. Return false to skip cooldown/durability. */
    private boolean castStone(InfinityStone stone, ServerLevel level, ServerPlayer player, ItemStack stack) {
        return switch (stone) {
            case POWER -> castPower(level, player);
            default -> false; // other stones implemented in later tasks
        };
    }

    private boolean castPower(ServerLevel level, ServerPlayer player) {
        double radius = 5.0;
        AABB area = player.getBoundingBox().inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == player) continue;
            if (!(target instanceof Enemy)) continue;
            target.hurt(WildWestDamageTypes.infinityPower(player), 6.0f);
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            target.knockback(3.0, -dx, -dz);
        }
        level.sendParticles(ParticleTypes.EXPLOSION,
            player.getX(), player.getY() + 0.5, player.getZ(),
            12, radius * 0.5, 0.5, radius * 0.5, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }
}
```

- [ ] **Step 2: Compile**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew compileJava
```
Expected: BUILD SUCCESSFUL. If `SoundEvents.GENERIC_EXPLODE` is named differently in 26.1.2 (verify by `grep -r "GENERIC_EXPLODE\|EXPLODE" /Users/tweeks/code/minecraft-mods/wildwest/src/main/java | head`), substitute with whatever the existing `PistonGauntletItem` peer mod uses — try `SoundEvents.GENERIC_EXPLODE` first; if missing fall back to `SoundEvents.GENERIC_EXTINGUISH_FIRE` while leaving a `// TODO sound polish` note (then remove it in Phase 7 polish).

- [ ] **Step 3: Run all tests**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew test
```
Expected: all tests PASS.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/InfinityGauntletItem.java
git commit -m "feat(wildwest): infinity gauntlet Power stone — AOE shockwave"
```

---

## Phase 4 — Remaining "simple" stones (Space, Time, Soul)

### Task 7: Implement Space stone (look-target teleport)

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/item/InfinityGauntletItem.java`

- [ ] **Step 1: Add Space case + helper to `InfinityGauntletItem.java`**

Add to the `switch` in `castStone`:

```java
            case SPACE -> castSpace(level, player);
```

Add the helper method (after `castPower`):

```java
    private boolean castSpace(ServerLevel level, ServerPlayer player) {
        double maxDist = 32.0;
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 end = eye.add(look.scale(maxDist));

        net.minecraft.world.phys.BlockHitResult hit = level.clip(
            new net.minecraft.world.level.ClipContext(eye, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));

        net.minecraft.world.phys.Vec3 target;
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
            target = end;
        } else {
            target = hit.getLocation().subtract(look.scale(1.0));
        }

        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
            player.getX(), player.getY() + 1.0, player.getZ(),
            20, 0.3, 0.5, 0.3, 0.01);

        player.teleportTo(target.x, target.y, target.z);
        player.fallDistance = 0.0f;

        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
            target.x, target.y + 1.0, target.z,
            20, 0.3, 0.5, 0.3, 0.01);
        level.playSound(null, target.x, target.y, target.z,
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }
```

- [ ] **Step 2: Compile**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew compileJava
```
Expected: BUILD SUCCESSFUL. If `player.fallDistance` field-access is no longer public in 26.1.2, switch to `player.resetFallDistance()` if that exists, otherwise `player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0.1, 1))` as a fallback to avoid splat damage.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/InfinityGauntletItem.java
git commit -m "feat(wildwest): infinity gauntlet Space stone — 32-block teleport"
```

---

### Task 8: Implement Time stone (Slowness IV + Mining Fatigue III AOE)

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/item/InfinityGauntletItem.java`

- [ ] **Step 1: Add Time case + helper**

Add to the `switch`:

```java
            case TIME -> castTime(level, player);
```

Add helper:

```java
    private boolean castTime(ServerLevel level, ServerPlayer player) {
        double radius = 6.0;
        AABB area = player.getBoundingBox().inflate(radius);
        int durationTicks = 160; // 8s
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == player) continue;
            if (!(target instanceof Enemy)) continue;
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 3));
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, durationTicks, 2));
        }
        level.sendParticles(ParticleTypes.GLOW,
            player.getX(), player.getY() + 1.0, player.getZ(),
            40, radius * 0.5, 0.5, radius * 0.5, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.5f);
        return true;
    }
```

- [ ] **Step 2: Compile**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew compileJava
```
Expected: BUILD SUCCESSFUL. NeoForge 26.1.2 effect class — if `MobEffects.MOVEMENT_SLOWDOWN` is renamed (e.g. just `SLOWNESS`), substitute. Quick check: `grep "MOVEMENT_SLOWDOWN\|SLOWNESS" wildwest/src/main/java -r`.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/InfinityGauntletItem.java
git commit -m "feat(wildwest): infinity gauntlet Time stone — AOE slow + fatigue"
```

---

### Task 9: Implement Soul stone (ray-hit drain)

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/item/InfinityGauntletItem.java`

- [ ] **Step 1: Add Soul case + helper**

Add to the `switch`:

```java
            case SOUL -> castSoul(level, player);
```

Add helper:

```java
    private boolean castSoul(ServerLevel level, ServerPlayer player) {
        double maxDist = 16.0;
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 end = eye.add(look.scale(maxDist));
        AABB rayAabb = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(0.5);

        net.minecraft.world.phys.EntityHitResult hit =
            net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, eye, end, rayAabb,
                e -> e != player && e.isAlive() && e instanceof LivingEntity,
                maxDist * maxDist);

        if (hit == null) return false; // missed — no cooldown

        LivingEntity target = (LivingEntity) hit.getEntity();
        target.hurt(WildWestDamageTypes.infinitySoul(player), 4.0f);
        player.heal(4.0f);

        // particle trail
        for (int i = 0; i <= 16; i++) {
            double t = i / 16.0;
            level.sendParticles(ParticleTypes.SOUL,
                eye.x + (target.getX() - eye.x) * t,
                eye.y + (target.getY() + target.getBbHeight() / 2 - eye.y) * t,
                eye.z + (target.getZ() - eye.z) * t,
                1, 0, 0, 0, 0);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SOUL_SAND_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }
```

- [ ] **Step 2: Compile**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew compileJava
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/InfinityGauntletItem.java
git commit -m "feat(wildwest): infinity gauntlet Soul stone — ranged drain"
```

---

## Phase 5 — Stateful stones (Mind, Reality) via attachments

### Task 10: Register `ModAttachments` + Mind / Reality attachment records

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/effect/MindCharmAttachment.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/effect/RealityBubbleAttachment.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/effect/ModAttachments.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`

- [ ] **Step 1: Create `MindCharmAttachment`**

```java
package com.tweeks.wildwest.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

public record MindCharmAttachment(UUID casterUuid, long expiresAtTick) {
    public static final Codec<MindCharmAttachment> CODEC = RecordCodecBuilder.create(i -> i.group(
        UUIDUtil.CODEC.fieldOf("caster").forGetter(MindCharmAttachment::casterUuid),
        Codec.LONG.fieldOf("expires_at_tick").forGetter(MindCharmAttachment::expiresAtTick)
    ).apply(i, MindCharmAttachment::new));
}
```

- [ ] **Step 2: Create `RealityBubbleAttachment`**

```java
package com.tweeks.wildwest.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

public record RealityBubbleAttachment(CompoundTag originalNbt, String originalTypeId, long expiresAtTick) {
    public static final Codec<RealityBubbleAttachment> CODEC = RecordCodecBuilder.create(i -> i.group(
        CompoundTag.CODEC.fieldOf("original_nbt").forGetter(RealityBubbleAttachment::originalNbt),
        Codec.STRING.fieldOf("original_type").forGetter(RealityBubbleAttachment::originalTypeId),
        Codec.LONG.fieldOf("expires_at_tick").forGetter(RealityBubbleAttachment::expiresAtTick)
    ).apply(i, RealityBubbleAttachment::new));
}
```

- [ ] **Step 3: Create `ModAttachments` registry**

```java
package com.tweeks.wildwest.effect;

import com.tweeks.wildwest.WildWestMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, WildWestMod.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<MindCharmAttachment>> MIND_CHARM =
        ATTACHMENTS.register("mind_charm",
            () -> AttachmentType.builder(() -> (MindCharmAttachment) null)
                .serialize(MindCharmAttachment.CODEC)
                .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RealityBubbleAttachment>> REALITY_BUBBLE =
        ATTACHMENTS.register("reality_bubble",
            () -> AttachmentType.builder(() -> (RealityBubbleAttachment) null)
                .serialize(RealityBubbleAttachment.CODEC)
                .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
```

- [ ] **Step 4: Wire register call**

In `WildWestMod.java` constructor, after `ModDataComponents.register(modEventBus);`:

```java
        com.tweeks.wildwest.effect.ModAttachments.register(modEventBus);
```

- [ ] **Step 5: Compile**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew compileJava
```
Expected: BUILD SUCCESSFUL. If `AttachmentType.builder(() -> null)` rejects untyped null, use the explicit-cast form shown above (it's there).

- [ ] **Step 6: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/effect/MindCharmAttachment.java wildwest/src/main/java/com/tweeks/wildwest/effect/RealityBubbleAttachment.java wildwest/src/main/java/com/tweeks/wildwest/effect/ModAttachments.java wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java
git commit -m "feat(wildwest): register mind_charm + reality_bubble attachments"
```

---

### Task 11: Implement Mind stone + tick handler

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/item/InfinityGauntletItem.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/effect/EffectTickHandler.java`

- [ ] **Step 1: Add Mind case + helper**

Add to the `switch` in `InfinityGauntletItem.castStone`:

```java
            case MIND -> castMind(level, player);
```

Add helper (uses `net.minecraft.world.entity.Mob` for `setTarget`):

```java
    private boolean castMind(ServerLevel level, ServerPlayer player) {
        double maxDist = 8.0;
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 end = eye.add(look.scale(maxDist));
        AABB rayAabb = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(0.5);

        net.minecraft.world.phys.EntityHitResult hit =
            net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, eye, end, rayAabb,
                e -> e != player && e.isAlive() && e instanceof net.minecraft.world.entity.Mob,
                maxDist * maxDist);

        if (hit == null) return false;

        net.minecraft.world.entity.Mob target = (net.minecraft.world.entity.Mob) hit.getEntity();
        long expiry = level.getGameTime() + 300; // 15s
        target.setData(com.tweeks.wildwest.effect.ModAttachments.MIND_CHARM.get(),
            new com.tweeks.wildwest.effect.MindCharmAttachment(player.getUUID(), expiry));

        level.sendParticles(ParticleTypes.ENCHANT,
            target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
            30, 0.3, 0.5, 0.3, 0.5);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }
```

- [ ] **Step 2: Create the tick handler**

```java
package com.tweeks.wildwest.effect;

import com.tweeks.wildwest.WildWestMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Server tick handler that drives Mind-charm + Reality-bubble timers.
 *
 * <p>Mind: each tick, every loaded mob with a {@code MIND_CHARM} attachment
 * gets its target re-set to the nearest hostile within 16 blocks (vanilla
 * AI would otherwise overwrite our target choice). On expiry the
 * attachment is removed.
 *
 * <p>Reality: handled in {@link RealityBubbleHandler} (separate file for
 * code clarity — that handler is registered in Task 12).
 */
@EventBusSubscriber(modid = WildWestMod.MOD_ID)
public final class EffectTickHandler {
    private EffectTickHandler() {}

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        // iterate loaded mobs cheaply via the entity getter
        for (net.minecraft.world.entity.Entity e : level.getAllEntities()) {
            if (!(e instanceof Mob mob)) continue;
            MindCharmAttachment charm = mob.getData(ModAttachments.MIND_CHARM.get());
            if (charm == null) continue;
            if (now >= charm.expiresAtTick()) {
                mob.removeData(ModAttachments.MIND_CHARM.get());
                continue;
            }
            // pick nearest hostile within 16 blocks (excluding self)
            AABB search = mob.getBoundingBox().inflate(16.0);
            net.minecraft.world.entity.LivingEntity nearest = null;
            double nearestSq = Double.MAX_VALUE;
            for (net.minecraft.world.entity.LivingEntity candidate :
                    level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, search)) {
                if (candidate == mob) continue;
                if (!(candidate instanceof Enemy)) continue;
                double dsq = candidate.distanceToSqr(mob);
                if (dsq < nearestSq) {
                    nearestSq = dsq;
                    nearest = candidate;
                }
            }
            if (nearest != null) {
                mob.setTarget(nearest);
            }
        }
    }
}
```

- [ ] **Step 3: Compile and run tests**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew compileJava test
```
Expected: BUILD SUCCESSFUL.

If `level.getAllEntities()` doesn't exist on `ServerLevel`, replace with `level.getEntities().getAll()` or iterate via `level.getEntities(EntityTypeTest.forClass(Mob.class), AABB, predicate)` — the goal is "every loaded Mob"; pick the existing API form. Quick reference grep: `grep -rn "getAllEntities\|getEntities()" wildwest/src/main/java | head`.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/InfinityGauntletItem.java wildwest/src/main/java/com/tweeks/wildwest/effect/EffectTickHandler.java
git commit -m "feat(wildwest): infinity gauntlet Mind stone + charm tick handler"
```

---

### Task 12: Implement Reality stone + bubble tick handler

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/item/InfinityGauntletItem.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/effect/EffectTickHandler.java`

- [ ] **Step 1: Add Reality case + helper**

Add to the `switch`:

```java
            case REALITY -> castReality(level, player);
```

Add helper:

```java
    private boolean castReality(ServerLevel level, ServerPlayer player) {
        double maxDist = 8.0;
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 end = eye.add(look.scale(maxDist));
        AABB rayAabb = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(0.5);

        net.minecraft.world.phys.EntityHitResult hit =
            net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, eye, end, rayAabb,
                e -> e != player && e.isAlive() && e instanceof net.minecraft.world.entity.Mob && e instanceof Enemy,
                maxDist * maxDist);

        if (hit == null) return false;

        net.minecraft.world.entity.Mob original = (net.minecraft.world.entity.Mob) hit.getEntity();

        net.minecraft.nbt.CompoundTag savedNbt = new net.minecraft.nbt.CompoundTag();
        original.saveAsPassenger(savedNbt);
        String typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
            .getKey(original.getType()).toString();

        net.minecraft.world.entity.animal.Bat bat =
            net.minecraft.world.entity.EntityType.BAT.create(level,
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (bat == null) return false;
        bat.moveTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), 0);
        bat.setData(com.tweeks.wildwest.effect.ModAttachments.REALITY_BUBBLE.get(),
            new com.tweeks.wildwest.effect.RealityBubbleAttachment(savedNbt, typeId, level.getGameTime() + 1200)); // 60s
        level.addFreshEntity(bat);
        original.discard();

        level.sendParticles(ParticleTypes.DUST_PLUME,
            original.getX(), original.getY() + 0.5, original.getZ(),
            30, 0.5, 0.5, 0.5, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }
```

- [ ] **Step 2: Add Reality tick to EffectTickHandler**

Inside `onLevelTick(...)`, after the Mind loop body (still inside the outer entity loop), add a second branch:

```java
            RealityBubbleAttachment bubble = mob.getData(ModAttachments.REALITY_BUBBLE.get());
            if (bubble != null && now >= bubble.expiresAtTick()) {
                restoreFromBubble(level, mob, bubble);
            }
```

Add the helper method at the bottom of the class:

```java
    private static void restoreFromBubble(ServerLevel level, Mob bat, RealityBubbleAttachment bubble) {
        net.minecraft.resources.ResourceLocation typeId =
            net.minecraft.resources.ResourceLocation.parse(bubble.originalTypeId());
        net.minecraft.world.entity.EntityType<?> type =
            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(typeId);
        if (type != null) {
            net.minecraft.world.entity.Entity restored = type.create(level,
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (restored != null) {
                restored.load(bubble.originalNbt());
                restored.moveTo(bat.getX(), bat.getY(), bat.getZ(), bat.getYRot(), 0);
                level.addFreshEntity(restored);
            }
        }
        bat.discard();
    }
```

- [ ] **Step 3: Compile**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew compileJava
```
Expected: BUILD SUCCESSFUL. NeoForge 26.x renamed `MobSpawnType` to `EntitySpawnReason` — if `EntitySpawnReason.MOB_SUMMONED` is missing, grep `grep -rn "MobSpawnType\|EntitySpawnReason" wildwest/src/main/java | head -3` to find what siblings use. Substitute as needed.

- [ ] **Step 4: Run all tests**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew test
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/InfinityGauntletItem.java wildwest/src/main/java/com/tweeks/wildwest/effect/EffectTickHandler.java
git commit -m "feat(wildwest): infinity gauntlet Reality stone — bubble swap to bat"
```

---

## Phase 6 — Radial picker UX

### Task 13: Pure radial math + tests

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/RadialMath.java`
- Create: `wildwest/src/test/java/com/tweeks/wildwest/client/RadialMathTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.tweeks.wildwest.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RadialMathTest {

    @Test
    void mouseAtCenter_returnsMinusOne() {
        assertEquals(-1, RadialMath.wedgeFromMouse(0, 0, 0, 0, /*deadzone*/ 10.0));
    }

    @Test
    void mouseInsideDeadzone_returnsMinusOne() {
        // 5 pixels right of center, deadzone 10 → -1
        assertEquals(-1, RadialMath.wedgeFromMouse(105, 100, 100, 100, 10.0));
    }

    @Test
    void mouseAbove_isWedgeZero() {
        // straight up from center
        assertEquals(0, RadialMath.wedgeFromMouse(100, 50, 100, 100, 10.0));
    }

    @Test
    void wedgesAreSixtyDegreesEach_inClockwiseOrder() {
        // angles measured clockwise from top:
        //  wedge 0 = up           (≈ -90° standard math angle)
        //  wedge 1 = upper-right  (≈ -30°)
        //  wedge 2 = lower-right  (≈  30°)
        //  wedge 3 = down         (≈  90°)
        //  wedge 4 = lower-left   (≈ 150°)
        //  wedge 5 = upper-left   (≈ -150°)
        int up         = RadialMath.wedgeFromMouse(100,  50, 100, 100, 10.0); // 0
        int upperRight = RadialMath.wedgeFromMouse(140,  70, 100, 100, 10.0); // 1
        int lowerRight = RadialMath.wedgeFromMouse(140, 130, 100, 100, 10.0); // 2
        int down       = RadialMath.wedgeFromMouse(100, 150, 100, 100, 10.0); // 3
        int lowerLeft  = RadialMath.wedgeFromMouse( 60, 130, 100, 100, 10.0); // 4
        int upperLeft  = RadialMath.wedgeFromMouse( 60,  70, 100, 100, 10.0); // 5
        assertEquals(0, up);
        assertEquals(1, upperRight);
        assertEquals(2, lowerRight);
        assertEquals(3, down);
        assertEquals(4, lowerLeft);
        assertEquals(5, upperLeft);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew test --tests com.tweeks.wildwest.client.RadialMathTest
```
Expected: compile failure ("cannot find symbol RadialMath").

- [ ] **Step 3: Write implementation**

```java
package com.tweeks.wildwest.client;

/**
 * Pure math for the radial stone picker. Tested in isolation so we
 * don't need a render context to verify wedge selection.
 *
 * <p>Convention: wedge 0 is straight up (12 o'clock), wedges proceed
 * clockwise. Six wedges of 60° each.
 */
public final class RadialMath {
    private RadialMath() {}

    public static int wedgeFromMouse(double mouseX, double mouseY,
                                     double centerX, double centerY,
                                     double deadzoneRadiusPx) {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distSq = dx * dx + dy * dy;
        if (distSq < deadzoneRadiusPx * deadzoneRadiusPx) return -1;

        // angle: 0 = up (north), increasing clockwise
        double angle = Math.atan2(dx, -dy); // dx, -dy → 0 at top, +π/2 to right
        if (angle < 0) angle += Math.PI * 2;

        double wedgeRad = Math.PI * 2 / 6;
        // shift by half-wedge so wedge 0 is centered on "straight up"
        double shifted = angle + wedgeRad / 2;
        if (shifted >= Math.PI * 2) shifted -= Math.PI * 2;

        return (int) (shifted / wedgeRad);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew test --tests com.tweeks.wildwest.client.RadialMathTest
```
Expected: 4 tests PASS. If the clockwise-order test fails on one edge case, the wedge boundary may have shifted by half — adjust the `+ wedgeRad/2` term to `- wedgeRad/2` and re-run; one of the two will pass for the convention in the test.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/client/RadialMath.java wildwest/src/test/java/com/tweeks/wildwest/client/RadialMathTest.java
git commit -m "feat(wildwest): radial picker math + tests"
```

---

### Task 14: C2S `SetActiveStone` packet

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/network/C2SSetActiveStonePacket.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/network/NetworkHandlers.java`

- [ ] **Step 1: Create the packet**

```java
package com.tweeks.wildwest.network;

import com.tweeks.wildwest.Registration;
import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.item.ModDataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SSetActiveStonePacket(int stoneIndex, boolean mainHand) implements CustomPacketPayload {

    public static final Type<C2SSetActiveStonePacket> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "set_active_stone"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSetActiveStonePacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, C2SSetActiveStonePacket::stoneIndex,
            ByteBufCodecs.BOOL,    C2SSetActiveStonePacket::mainHand,
            C2SSetActiveStonePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(C2SSetActiveStonePacket pkt, IPayloadContext ctx) {
        if (pkt.stoneIndex() < 0 || pkt.stoneIndex() > 5) return;
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            InteractionHand hand = pkt.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Registration.INFINITY_GAUNTLET.get())) return;
            stack.set(ModDataComponents.ACTIVE_STONE.get(), pkt.stoneIndex());
        });
    }
}
```

- [ ] **Step 2: Register in NetworkHandlers.java**

In `NetworkHandlers.register(...)`, add after the existing `playToClient(...)` call:

```java
        reg.playToServer(
            C2SSetActiveStonePacket.TYPE,
            C2SSetActiveStonePacket.STREAM_CODEC,
            C2SSetActiveStonePacket::handle);
```

- [ ] **Step 3: Compile**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew compileJava
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/network/C2SSetActiveStonePacket.java wildwest/src/main/java/com/tweeks/wildwest/network/NetworkHandlers.java
git commit -m "feat(wildwest): C2S set-active-stone packet"
```

---

### Task 15: KeyMapping + client open handler

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/InfinityGauntletKeybind.java`

- [ ] **Step 1: Create the keybind class**

```java
package com.tweeks.wildwest.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tweeks.wildwest.Registration;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = WildWestMod.MOD_ID, value = Dist.CLIENT)
public final class InfinityGauntletKeybind {
    private InfinityGauntletKeybind() {}

    public static final KeyMapping OPEN_RADIAL = new KeyMapping(
        "key.wildwest.infinity_gauntlet_radial",
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_G,
        "key.categories.wildwest");

    @SubscribeEvent
    public static void onRegister(RegisterKeyMappingsEvent event) {
        event.register(OPEN_RADIAL);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        Player player = mc.player;
        if (player == null) return;

        while (OPEN_RADIAL.consumeClick()) {
            InteractionHand hand = findGauntletHand(player);
            if (hand != null) {
                mc.setScreen(new RadialPickerScreen(hand == InteractionHand.MAIN_HAND));
            }
        }
    }

    private static InteractionHand findGauntletHand(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(Registration.INFINITY_GAUNTLET.get())) return InteractionHand.MAIN_HAND;
        ItemStack off = player.getOffhandItem();
        if (off.is(Registration.INFINITY_GAUNTLET.get())) return InteractionHand.OFF_HAND;
        return null;
    }
}
```

- [ ] **Step 2: Compile**

(`RadialPickerScreen` doesn't exist yet — will fail. That's expected; we add it next.)

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew compileJava
```
Expected: compile failure ("cannot find symbol RadialPickerScreen").

- [ ] **Step 3: No commit yet — proceed to Task 16.**

---

### Task 16: `RadialPickerScreen`

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/RadialPickerScreen.java`

- [ ] **Step 1: Create the screen**

```java
package com.tweeks.wildwest.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tweeks.wildwest.item.InfinityStone;
import com.tweeks.wildwest.network.C2SSetActiveStonePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Radial picker for the Infinity Gauntlet's six stones. Press the keybind
 * to open, hover/click a wedge to select, the screen closes on selection
 * or when the keybind is released.
 */
public class RadialPickerScreen extends Screen {

    private static final double DEADZONE_PX = 18.0;
    private static final int OUTER_RADIUS_PX = 90;
    private static final int INNER_RADIUS_PX = 24;

    private final boolean mainHand;
    private int hoveredWedge = -1;

    public RadialPickerScreen(boolean mainHand) {
        super(Component.translatable("screen.wildwest.infinity_gauntlet_radial"));
        this.mainHand = mainHand;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // dim background
        graphics.fillGradient(0, 0, this.width, this.height, 0x40000000, 0x80000000);

        int cx = this.width / 2;
        int cy = this.height / 2;
        this.hoveredWedge = RadialMath.wedgeFromMouse(mouseX, mouseY, cx, cy, DEADZONE_PX);

        // draw each wedge as a filled circle segment approximated by two triangle fan strips.
        // For simplicity (and since GuiGraphics doesn't offer arc fills directly), we render
        // each stone as a colored disc at its wedge center: easier to read at a glance.
        for (int i = 0; i < 6; i++) {
            InfinityStone stone = InfinityStone.byIndex(i);
            // wedge 0 = straight up, +60° per index, clockwise
            double angle = -Math.PI / 2 + i * (Math.PI / 3);
            int discX = cx + (int) (Math.cos(angle) * OUTER_RADIUS_PX) - 12;
            int discY = cy + (int) (Math.sin(angle) * OUTER_RADIUS_PX) - 12;

            int color = 0xFF000000 | (stone.colorRgb() & 0x00FFFFFF);
            int outline = (i == hoveredWedge) ? 0xFFFFFFFF : 0xFF202020;
            // draw outlined disc as a filled square w/ outline (24x24)
            graphics.fill(discX - 2, discY - 2, discX + 26, discY + 26, outline);
            graphics.fill(discX, discY, discX + 24, discY + 24, color);

            // stone name centered below the disc
            Component label = Component.translatable("item.wildwest.infinity_gauntlet.stone." + stone.translationSuffix());
            int textW = this.font.width(label);
            graphics.drawString(this.font, label, discX + 12 - textW / 2, discY + 28, 0xFFFFFFFF, true);
        }

        // center prompt
        Component prompt = Component.translatable("screen.wildwest.infinity_gauntlet_radial.prompt");
        int promptW = this.font.width(prompt);
        graphics.drawString(this.font, prompt, cx - promptW / 2, cy - 4, 0xFFCCCCCC, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredWedge >= 0) {
            select(hoveredWedge);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (hoveredWedge >= 0) {
            select(hoveredWedge);
        } else {
            super.onClose();
        }
    }

    private void select(int wedge) {
        PacketDistributor.sendToServer(new C2SSetActiveStonePacket(wedge, mainHand));
        super.onClose();
    }
}
```

- [ ] **Step 2: Compile**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew compileJava
```
Expected: BUILD SUCCESSFUL. If `graphics.fillGradient` signature differs, use `graphics.fill(0, 0, width, height, 0x80000000)` as a flat dim. If `Screen.onClose` is not overridable as void without super-call — adjust to `this.minecraft.setScreen(null);` instead of `super.onClose();`.

- [ ] **Step 3: Run all tests**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew test
```
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/client/InfinityGauntletKeybind.java wildwest/src/main/java/com/tweeks/wildwest/client/RadialPickerScreen.java
git commit -m "feat(wildwest): radial picker screen + G keybind for gauntlet"
```

---

### Task 17: Item name override + tooltip showing active stone

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/item/InfinityGauntletItem.java`

- [ ] **Step 1: Add `getName(ItemStack)` and `appendHoverText` overrides**

Add the following methods to `InfinityGauntletItem`:

```java
    @Override
    public net.minecraft.network.chat.Component getName(ItemStack stack) {
        InfinityStone stone = InfinityStone.byIndex(
            stack.getOrDefault(ModDataComponents.ACTIVE_STONE.get(), 0));
        return net.minecraft.network.chat.Component.translatable(
            "item.wildwest.infinity_gauntlet.named",
            net.minecraft.network.chat.Component.translatable(
                "item.wildwest.infinity_gauntlet.stone." + stone.translationSuffix()));
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> adder,
                                net.minecraft.world.item.TooltipFlag flag) {
        InfinityStone stone = InfinityStone.byIndex(
            stack.getOrDefault(ModDataComponents.ACTIVE_STONE.get(), 0));
        adder.accept(net.minecraft.network.chat.Component.translatable(
            "item.wildwest.infinity_gauntlet.tooltip." + stone.translationSuffix())
            .withStyle(net.minecraft.ChatFormatting.GRAY));
        adder.accept(net.minecraft.network.chat.Component.translatable(
            "item.wildwest.infinity_gauntlet.tooltip.swap")
            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
```

You'll also need imports at the top if your style prefers them — current code uses fully-qualified names. Either is fine.

- [ ] **Step 2: Compile**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew compileJava
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/InfinityGauntletItem.java
git commit -m "feat(wildwest): show active stone in item name + tooltip"
```

---

## Phase 7 — Recipe + lang + texture + final verification

### Task 18: Shaped recipe

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/data/ModRecipeProvider.java`

- [ ] **Step 1: Append the recipe in `buildRecipes()`**

After the existing `BANDIT_KNIFE` recipe block, add:

```java
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.INFINITY_GAUNTLET.get())
            .pattern("SDS")
            .pattern("N*N")
            .pattern("HTH")
            .define('S', Items.ECHO_SHARD)
            .define('D', Items.WITHER_SKELETON_SKULL)
            .define('N', Items.NETHERITE_BLOCK)
            .define('*', Items.NETHER_STAR)
            .define('H', Items.HEART_OF_THE_SEA)
            .define('T', Items.TOTEM_OF_UNDYING)
            .unlockedBy("has_nether_star", this.has(Items.NETHER_STAR))
            .save(this.output);
```

- [ ] **Step 2: Run datagen**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew runData
```
Expected: BUILD SUCCESSFUL. New file: `wildwest/src/generated/serverData/data/wildwest/recipe/infinity_gauntlet.json`.

- [ ] **Step 3: Inspect the generated recipe**

```bash
cat wildwest/src/generated/serverData/data/wildwest/recipe/infinity_gauntlet.json
```
Expected: `"type": "minecraft:crafting_shaped"`, `"pattern": ["SDS", "N*N", "HTH"]`, key map with all six entries.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/data/ModRecipeProvider.java wildwest/src/generated/serverData/data/wildwest/recipe/infinity_gauntlet.json
git commit -m "feat(wildwest): infinity gauntlet shaped crafting recipe"
```

---

### Task 19: Language strings via `ModLanguageProvider`

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/data/ModLanguageProvider.java`

- [ ] **Step 1: Read the existing provider**

First open `ModLanguageProvider.java` to see the existing pattern and pick the right place to add entries.

- [ ] **Step 2: Add all infinity-gauntlet translations**

Add (inside the `addTranslations` method, following the existing entry style):

```java
        add("item.wildwest.infinity_gauntlet", "Infinity Gauntlet");
        add("item.wildwest.infinity_gauntlet.named", "Infinity Gauntlet (%s)");
        add("item.wildwest.infinity_gauntlet.stone.power",   "Power");
        add("item.wildwest.infinity_gauntlet.stone.space",   "Space");
        add("item.wildwest.infinity_gauntlet.stone.time",    "Time");
        add("item.wildwest.infinity_gauntlet.stone.mind",    "Mind");
        add("item.wildwest.infinity_gauntlet.stone.reality", "Reality");
        add("item.wildwest.infinity_gauntlet.stone.soul",    "Soul");
        add("item.wildwest.infinity_gauntlet.tooltip.power",   "Right-click: AOE shockwave (5-block radius).");
        add("item.wildwest.infinity_gauntlet.tooltip.space",   "Right-click: blink up to 32 blocks toward look.");
        add("item.wildwest.infinity_gauntlet.tooltip.time",    "Right-click: slow nearby hostiles for 8s.");
        add("item.wildwest.infinity_gauntlet.tooltip.mind",    "Right-click: charm one looked-at mob for 15s.");
        add("item.wildwest.infinity_gauntlet.tooltip.reality", "Right-click: bubble one hostile (becomes a bat for 60s).");
        add("item.wildwest.infinity_gauntlet.tooltip.soul",    "Right-click: ranged siphon — 4 damage, 4 heal.");
        add("item.wildwest.infinity_gauntlet.tooltip.swap",    "Press [G] to swap stone.");
        add("key.wildwest.infinity_gauntlet_radial", "Open Infinity Gauntlet Radial");
        add("key.categories.wildwest", "Wild West");
        add("screen.wildwest.infinity_gauntlet_radial", "Infinity Gauntlet");
        add("screen.wildwest.infinity_gauntlet_radial.prompt", "Select Stone");
        add("death.attack.wildwest.infinity_power", "%1$s was snapped by %2$s");
        add("death.attack.wildwest.infinity_soul",  "%1$s's soul was siphoned by %2$s");
```

(Note: the `key.categories.wildwest` may already be present from prior wildwest content — `grep "key.categories.wildwest" wildwest/src/main/java/com/tweeks/wildwest/data/ModLanguageProvider.java`; if present, skip that line.)

- [ ] **Step 3: Run datagen**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew runData
```
Expected: BUILD SUCCESSFUL. `wildwest/src/generated/clientData/assets/wildwest/lang/en_us.json` updated with the new entries.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/data/ModLanguageProvider.java wildwest/src/generated/clientData/assets/wildwest/lang/en_us.json
git commit -m "feat(wildwest): infinity gauntlet translation strings"
```

---

### Task 20: Item model JSON + placeholder texture

**Files:**
- Create: `wildwest/src/main/resources/assets/wildwest/models/item/infinity_gauntlet.json`
- Create: `wildwest/src/main/resources/assets/wildwest/textures/item/infinity_gauntlet.png`

- [ ] **Step 1: Create the model JSON**

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "wildwest:item/infinity_gauntlet"
  }
}
```

- [ ] **Step 2: Create a placeholder texture**

Use ImageMagick to render a placeholder gold-square with 6 colored dots:

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest/src/main/resources/assets/wildwest/textures/item && \
magick -size 16x16 xc:'#806020' \
  -fill '#A020F0' -draw 'point 2,4'  -draw 'point 2,5' \
  -fill '#1E90FF' -draw 'point 8,4'  -draw 'point 8,5' \
  -fill '#32CD32' -draw 'point 13,4' -draw 'point 13,5' \
  -fill '#FFD700' -draw 'point 2,10' -draw 'point 2,11' \
  -fill '#FF4500' -draw 'point 8,10' -draw 'point 8,11' \
  -fill '#FFA500' -draw 'point 13,10' -draw 'point 13,11' \
  infinity_gauntlet.png
```

If ImageMagick is unavailable, use this Python one-liner as fallback (writes the same):

```bash
python3 -c "from PIL import Image; im = Image.new('RGBA', (16,16), (128,96,32,255)); pts = [((2,4),(0xA0,0x20,0xF0)), ((8,4),(0x1E,0x90,0xFF)), ((13,4),(0x32,0xCD,0x32)), ((2,10),(0xFF,0xD7,0x00)), ((8,10),(0xFF,0x45,0x00)), ((13,10),(0xFF,0xA5,0x00))]; [im.putpixel((x,y), c+(255,)) for ((x,y),c) in pts]; [im.putpixel((x,y+1), c+(255,)) for ((x,y),c) in pts]; im.save('infinity_gauntlet.png')"
```

- [ ] **Step 3: Verify the file exists**

```bash
ls -la wildwest/src/main/resources/assets/wildwest/textures/item/infinity_gauntlet.png
```
Expected: file exists, non-zero size.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/resources/assets/wildwest/models/item/infinity_gauntlet.json wildwest/src/main/resources/assets/wildwest/textures/item/infinity_gauntlet.png
git commit -m "feat(wildwest): infinity gauntlet item model + placeholder texture"
```

---

### Task 21: Final build + tests verification

**Files:** (none — verification only)

- [ ] **Step 1: Run full build**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew build
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run all tests**

```bash
cd /Users/tweeks/code/minecraft-mods/wildwest && ./gradlew test
```
Expected: all tests PASS, including the three new ones (`InfinityStoneTest`, `InfinityGauntletCooldownTest`, `RadialMathTest`).

- [ ] **Step 3: Confirm registry entries**

```bash
grep -c "infinity_gauntlet" wildwest/src/main/java/com/tweeks/wildwest/Registration.java
```
Expected: at least 2 occurrences (registration + creative tab entry).

- [ ] **Step 4: Confirm all generated JSONs are committed**

```bash
git status wildwest/src/generated
```
Expected: clean working tree. If new generated files remain, add + commit them.

- [ ] **Step 5: Confirm spec acceptance criteria**

Re-read `docs/superpowers/specs/2026-06-06-infinity-gauntlet-design.md` "Acceptance criteria" section. Tick off each criterion that is now satisfied. Note any deferred items.

- [ ] **Step 6: Manual dev-client smoke (deferred)**

Per repo convention (`Null boss`, `Meteor Staff`, etc. all defer manual dev-client smoke), record in the final commit message that dev-client smoke testing is deferred to user runs.

- [ ] **Step 7: Final commit (if anything uncommitted)**

If steps 1–4 surface anything uncommitted:

```bash
git add -A
git commit -m "$(cat <<'EOF'
chore(wildwest): finalize infinity gauntlet integration

All datagen outputs regenerated. Manual dev-client smoke test deferred.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Plan Self-Review

**Spec coverage check:**

| Spec section / acceptance criterion | Task(s) |
|---|---|
| `wildwest:infinity_gauntlet` registered + creative tab | Task 4 |
| Recipe craftable | Task 18 |
| `G` opens radial picker | Tasks 13, 15, 16 |
| 6 stones each cast their ability | Tasks 6 (Power), 7 (Space), 8 (Time), 9 (Soul), 11 (Mind), 12 (Reality) |
| Per-stone cooldowns | Tasks 2, 3, 6 (wired via `castStone` dispatch) |
| Item name shows active stone | Task 17 |
| Tooltip per-stone | Task 17, 19 |
| No crashes on restart w/ active charm/bubble | Task 10 (persistent codec) — verified by attachment serializer |
| `runData` succeeds, generated JSONs present | Tasks 5, 18, 19 |
| `gradlew build` succeeds | Task 21 |

All acceptance criteria mapped.

**Placeholder scan:** searched for "TBD", "fill in", "similar to" — none. Each step has runnable code or a concrete command.

**Type consistency:** `InfinityStone.byIndex(int)`, `InfinityCooldowns.applyCooldown(...)`, `ModDataComponents.ACTIVE_STONE`/`COOLDOWNS`, `ModAttachments.MIND_CHARM`/`REALITY_BUBBLE`, `C2SSetActiveStonePacket(int, boolean)` — all names used consistently across Tasks 1, 3, 6–9, 11–12, 14, 15, 16, 17.

**One known fragility:** Tasks 6–12 each include "if NeoForge 26.x renamed `X`, substitute `Y`" notes for vanilla API surface that has churned (`SoundEvents.*`, `MobEffects.*`, `MobSpawnType` → `EntitySpawnReason`, `Player.fallDistance`). These are intentional — the engineer should hit `./gradlew compileJava` between every task and resolve any name churn before committing. The named substitutions cover the most likely renames.
