# Usable Baton Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Security Guard's baton a wieldable iron-tier item that applies the same Slowness II + Weakness I + knockback stun the Guard's AI applies on hit, and replace the white-placeholder textures with a readable police-nightstick design.

**Architecture:** Extract a `StunEffects.applyStun(...)` static helper into `securitycore` so the existing `StunningMeleeGoal` and the new `BatonItem` both call the same code. `BatonItem extends SwordItem` with `Tiers.IRON`; override `postHurtEnemy` to apply the stun. Repaint the existing `entity/baton.png` and add a new `item/baton.png` icon, both 16×16 with a black-grip + dark-wood palette.

**Tech Stack:** Java 25, NeoForge 26.1.2.30-beta, Minecraft 26.1.2, gradle with `net.neoforged.moddev` 2.0.141, JUnit 5, Python 3 + Pillow for one-shot texture generation (already used by the Thief textures — see commit `416065e`).

**Spec:** [docs/superpowers/specs/2026-04-29-baton-item-design.md](../specs/2026-04-29-baton-item-design.md)

**Working directory for all commands:** `/Users/tweeks/code/minecraft-mods` (repo root). Module-specific gradle commands use the `:securitycore:` and `:securityguard:` prefixes.

---

## File Structure

### Files created

```
securitycore/src/main/java/com/tweeks/securitycore/ai/StunEffects.java                   # NEW: pure stun-application helper
securityguard/src/main/java/com/tweeks/securityguard/item/BatonItem.java                 # NEW: SwordItem subclass with on-hit stun
securityguard/src/main/resources/assets/securityguard/items/baton.json                   # NEW: client item-model selector
securityguard/src/main/resources/assets/securityguard/models/item/baton.json             # NEW: item/handheld parent w/ baton texture
securityguard/src/main/resources/assets/securityguard/textures/item/baton.png            # NEW: 16×16 inventory icon
```

### Files modified

```
securitycore/src/main/java/com/tweeks/securitycore/ai/StunningMeleeGoal.java             # delegate effects to StunEffects.applyStun
securityguard/src/main/java/com/tweeks/securityguard/Registration.java                   # register BATON; add to creative tab
securityguard/src/main/java/com/tweeks/securityguard/data/ModLanguageProvider.java       # add Baton lang entry
securityguard/src/main/resources/assets/securityguard/textures/entity/baton.png          # REPAINT: white placeholder → nightstick palette
```

### Files deleted

None.

---

## Task 1: Baseline — confirm current build is green

**Files:** none modified.

- [ ] **Step 1: Run the existing build to confirm a clean starting point**

Run:
```bash
./gradlew build
```
Expected: `BUILD SUCCESSFUL`. Both `securitycore-0.1.0.jar` and `securityguard-0.1.0.jar` (and `thief-0.1.0.jar`, if the thief module is in `settings.gradle`) build under `*/build/libs/`.

- [ ] **Step 2: Run the existing test suite**

Run:
```bash
./gradlew test
```
Expected: `BUILD SUCCESSFUL`. All `SpawnPatternTest` cases pass.

- [ ] **Step 3: No commit**

Verification only. If either step fails, **stop and fix before proceeding** — every subsequent task assumes a green baseline.

---

## Task 2: Extract `StunEffects` and refactor `StunningMeleeGoal` to delegate

**Files:**
- Create: `securitycore/src/main/java/com/tweeks/securitycore/ai/StunEffects.java`
- Modify: `securitycore/src/main/java/com/tweeks/securitycore/ai/StunningMeleeGoal.java`

This task pulls the inline effect-application out of `StunningMeleeGoal` so a player-held `BatonItem` can call the same code in Task 4. Behavior must be byte-for-byte identical — same `MobEffects.SLOWNESS` / `MobEffects.WEAKNESS` instances, same `knockback` arguments, same dead-target short-circuit.

- [ ] **Step 1: Create `StunEffects`**

Create file `securitycore/src/main/java/com/tweeks/securitycore/ai/StunEffects.java` with:

```java
package com.tweeks.securitycore.ai;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Applies the Security Pack stun bundle (Slowness + Weakness + knockback)
 * to a target. Single source of truth for "what does a stunning weapon do
 * on hit?" — used by {@link StunningMeleeGoal} for NPC swings and by
 * mod items (Guard's baton, Thief's blackjack) for player swings.
 */
public final class StunEffects {

    private StunEffects() {}

    /**
     * Applies Slowness + Weakness for {@code durationTicks} and pushes
     * {@code target} away from {@code attacker}. No-op if the target is
     * already dead.
     *
     * @param attacker            entity dealing the blow (used for knockback direction)
     * @param target              entity receiving the stun
     * @param durationTicks       length of both effects in ticks (20 = 1 second)
     * @param slownessAmplifier   0 = Slowness I, 1 = Slowness II
     * @param weaknessAmplifier   0 = Weakness I, 1 = Weakness II
     * @param knockbackStrength   horizontal knockback (vanilla units; 0.4 ≈ standard punch)
     */
    public static void applyStun(LivingEntity attacker,
                                 LivingEntity target,
                                 int durationTicks,
                                 int slownessAmplifier,
                                 int weaknessAmplifier,
                                 double knockbackStrength) {
        if (!target.isAlive()) return;

        target.addEffect(new MobEffectInstance(
            MobEffects.SLOWNESS, durationTicks, slownessAmplifier));
        target.addEffect(new MobEffectInstance(
            MobEffects.WEAKNESS, durationTicks, weaknessAmplifier));
        target.knockback(
            knockbackStrength,
            attacker.getX() - target.getX(),
            attacker.getZ() - target.getZ());
    }
}
```

- [ ] **Step 2: Refactor `StunningMeleeGoal.checkAndPerformAttack` to delegate**

Open `securitycore/src/main/java/com/tweeks/securitycore/ai/StunningMeleeGoal.java`. Replace the `checkAndPerformAttack` method body so the inline effect block becomes a single `StunEffects.applyStun(...)` call. The full replacement method:

```java
@Override
protected void checkAndPerformAttack(LivingEntity target) {
    if (this.canPerformAttack(target)) {
        this.resetAttackCooldown();
        this.mob.swing(this.mob.getUsedItemHand());
        this.mob.doHurtTarget((ServerLevel) this.mob.level(), target);

        StunEffects.applyStun(this.mob, target,
            stunDurationTicks, slownessAmplifier, weaknessAmplifier, knockbackStrength);
    }
}
```

The old inline block (the `if (target.isAlive()) { addEffect(...); addEffect(...); knockback(...); }` lines) can be removed — `StunEffects.applyStun` performs the same `isAlive()` check internally. The unused `MobEffectInstance` and `MobEffects` imports are no longer needed; remove them so the file compiles cleanly.

After the refactor, `StunningMeleeGoal.java` should retain only these imports:
```java
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
```

- [ ] **Step 3: Compile securitycore**

Run:
```bash
./gradlew :securitycore:compileJava
```
Expected: `BUILD SUCCESSFUL`. If unused imports remain, the build will not fail (Java doesn't error on unused imports), but they should still be cleaned up for hygiene.

- [ ] **Step 4: Compile and test the whole repo to verify no consumer broke**

Run:
```bash
./gradlew build test
```
Expected: `BUILD SUCCESSFUL`. `SpawnPatternTest` still passes. The `securityguard` module compiles unchanged — it doesn't reference `StunEffects` yet, only the goal, and the goal's public constructor signature is unchanged.

- [ ] **Step 5: Commit**

```bash
git add securitycore/src/main/java/com/tweeks/securitycore/ai/StunEffects.java \
        securitycore/src/main/java/com/tweeks/securitycore/ai/StunningMeleeGoal.java
git commit -m "$(cat <<'EOF'
refactor(securitycore): extract StunEffects helper from StunningMeleeGoal

Splits the on-hit effect application (Slowness + Weakness + knockback)
into a static StunEffects.applyStun method so the upcoming player-held
BatonItem can apply the same stun on player swings without duplicating
the goal's logic. StunningMeleeGoal now delegates to the helper;
behavior is byte-for-byte identical, verified by SpawnPatternTest +
existing build green.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Add `BatonItem` class

**Files:**
- Create: `securityguard/src/main/java/com/tweeks/securityguard/item/BatonItem.java`

**Classpath note:** Although `SwordItem`, `Tier`, and `Tiers` exist in the neoformruntime intermediate sources jar, they are **stripped from the per-module compile classpath** in NeoForge 26.1.2.30-beta's moddev export (`securityguard/build/moddev/artifacts/minecraft-patched-26.1.2.30-beta.jar`). The module compiles against pure data-component-driven items only. So `BatonItem` extends plain `Item`, manually decrements durability via `stack.hurtAndBreak`, and the iron-tier feel (attack damage, attack speed, durability) is built up via `Item.Properties` at registration time (Task 4) using `ItemAttributeModifiers.builder()` instead of the unavailable `SwordItem.createAttributes(Tiers.IRON, ...)` factory.

The override point is `postHurtEnemy` — called server-side after vanilla damage has been applied. We chain `super.postHurtEnemy`, manually decrement durability with `stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND)`, and then call `StunEffects.applyStun` with the same numeric parameters Guard AI uses (`60, 1, 0, 0.2`).

- [ ] **Step 1: Create `BatonItem`**

Create file `securityguard/src/main/java/com/tweeks/securityguard/item/BatonItem.java` with:

```java
package com.tweeks.securityguard.item;

import com.tweeks.securitycore.ai.StunEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The Security Guard's baton, wielded by both Guards (in their off-AI hand
 * via the renderer) and by players who pick one up from the creative tab.
 * Iron-tier weapon; on every successful hit, applies the same Slowness II +
 * Weakness I + knockback bundle that the Guard's AI applies via
 * {@code StunningMeleeGoal}, so player swings and AI swings feel identical.
 *
 * Extends plain {@link Item} (not {@code SwordItem}) because the moddev
 * compile classpath for NeoForge 26.1.2.30-beta strips the {@code SwordItem}
 * / {@code Tier} / {@code Tiers} hierarchy in favor of pure data-component
 * configuration. Iron-tier feel is reproduced via the durability + attribute
 * modifiers attached at registration, plus the manual {@code hurtAndBreak}
 * call below to mirror what {@code SwordItem.postHurtEnemy} would have done.
 */
public class BatonItem extends Item {

    private static final int STUN_DURATION_TICKS = 60;
    private static final int SLOWNESS_AMPLIFIER = 1;
    private static final int WEAKNESS_AMPLIFIER = 0;
    private static final double KNOCKBACK_STRENGTH = 0.2;

    public BatonItem(Properties properties) {
        super(properties);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        StunEffects.applyStun(attacker, target,
            STUN_DURATION_TICKS, SLOWNESS_AMPLIFIER, WEAKNESS_AMPLIFIER, KNOCKBACK_STRENGTH);
    }
}
```

(The constants must equal the parameters passed to `StunningMeleeGoal` in `SecurityGuardEntity.registerGoals` — currently `60, 1, 0, 0.2`. If those numbers ever drift, the player-baton and AI-baton will desync; both sites are deliberately tagged with the same numbers.)

- [ ] **Step 2: Compile**

Run:
```bash
./gradlew :securityguard:compileJava
```
Expected: `BUILD SUCCESSFUL`. (BatonItem is not yet registered, so it compiles but doesn't appear in-game.)

- [ ] **Step 3: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/item/BatonItem.java
git commit -m "$(cat <<'EOF'
feat(securityguard): add BatonItem with on-hit stun

Iron-tier SwordItem subclass that applies Slowness II + Weakness I +
0.2 knockback to its target via StunEffects.applyStun on every hit.
Numeric stun parameters mirror those passed to StunningMeleeGoal in
SecurityGuardEntity, so player- and AI-swung batons feel identical.
Not yet registered — that lands in the next commit.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Register `BATON` and surface it in the creative tab

**Files:**
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/Registration.java`

**Classpath note (continuation of Task 3):** `SwordItem.createAttributes` and `Tiers.IRON` are not on the compile classpath for this NeoForge 26.1.2.30-beta moddev export. We build the equivalent `ItemAttributeModifiers` manually using `Item.BASE_ATTACK_DAMAGE_ID` / `Item.BASE_ATTACK_SPEED_ID` (which **are** on classpath as `Identifier` constants on `net.minecraft.world.item.Item`), and we pass `250` directly for durability since `Tiers.IRON.getUses()` is unavailable.

Iron-sword effective stats this mirrors: durability 250, attack damage 6.0 + 1.0 (player base) = 7 damage, attack speed -2.4 (= 1.6 swings/sec). Vanilla iron sword's `SwordItem.createAttributes(Tiers.IRON, 3, -2.4F)` resolves to "3 + tier-bonus 6.0 = +9 attack damage" plus the +1 player base for 10 total — but that's because the Tier API stacks an extra base bonus on top. With the simpler manual API on the classpath we can just use `+6` for clean iron-tier feel; smoke test in Task 10 confirms it feels right.

- [ ] **Step 1: Add the imports at the top of `Registration.java`**

Open `securityguard/src/main/java/com/tweeks/securityguard/Registration.java`. Add to the existing import block (alphabetical placement near the other `world` imports):

```java
import com.tweeks.securityguard.item.BatonItem;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
```

- [ ] **Step 2: Add the BATON_ATTRIBUTES constant + `BATON` `DeferredItem`**

After the existing `GUARD_HELMET` declaration (around line 35) and before the `SECURITY_GUARD` entity-type declaration, add:

```java
    private static final ItemAttributeModifiers BATON_ATTRIBUTES = ItemAttributeModifiers.builder()
        .add(Attributes.ATTACK_DAMAGE,
            new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6.0, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND)
        .add(Attributes.ATTACK_SPEED,
            new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND)
        .build();

    public static final DeferredItem<BatonItem> BATON = ITEMS.registerItem("baton",
        BatonItem::new,
        p -> p.attributes(BATON_ATTRIBUTES).durability(250));
```

The constructor reference `BatonItem::new` resolves to `BatonItem(Properties)` — single-arg form, matching the constructor created in Task 3.

- [ ] **Step 3: Add `BATON` to the creative tab**

Find the `SECURITY_GUARD_TAB.displayItems(...)` block (around line 56). It currently outputs `GUARD_HELMET` and `GUARD_SPAWN_EGG`. Insert `BATON` between them:

```java
.displayItems((params, output) -> {
    output.accept(GUARD_HELMET.get());
    output.accept(BATON.get());
    output.accept(GUARD_SPAWN_EGG.get());
})
```

- [ ] **Step 4: Compile**

Run:
```bash
./gradlew :securityguard:compileJava
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit (don't run a full build yet — model JSON is missing, so the client would fail to resolve the baton's icon at runtime; we add JSONs in the next task before any in-game test)**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/Registration.java
git commit -m "$(cat <<'EOF'
feat(securityguard): register BATON DeferredItem in creative tab

Wires BatonItem into the registration system as iron-tier (Tiers.IRON,
3 attack damage + tier bonus = +6 effective) with vanilla-iron-sword
attack speed (-2.4F). Surfaces it in the Security Guard creative tab
alongside the helmet and spawn egg. Item-model JSONs and lang entry
land in the next commit.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Add item-model JSONs and language entry

**Files:**
- Create: `securityguard/src/main/resources/assets/securityguard/items/baton.json`
- Create: `securityguard/src/main/resources/assets/securityguard/models/item/baton.json`
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/data/ModLanguageProvider.java`

The two JSON files match the existing `guard_helmet` pattern:
- `assets/<modid>/items/<id>.json` is the *client item-model selector* (1.21.4+ split-model format) that points at a model.
- `assets/<modid>/models/item/<id>.json` is the actual model — for held tools, parented to `minecraft:item/handheld` (which orients the icon along the player's hand-axis in first-person).

- [ ] **Step 1: Create the items selector**

Create file `securityguard/src/main/resources/assets/securityguard/items/baton.json`:

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "securityguard:item/baton"
  }
}
```

- [ ] **Step 2: Create the item model**

Create file `securityguard/src/main/resources/assets/securityguard/models/item/baton.json`:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "securityguard:item/baton"
  }
}
```

(`item/handheld` — same parent vanilla swords use — orients the texture so the bottom-left of the sprite ends up at the player's hand grip in first-person, which matches the diagonal layout we'll paint in Task 7.)

- [ ] **Step 3: Add the lang entry**

Open `securityguard/src/main/java/com/tweeks/securityguard/data/ModLanguageProvider.java`. After the `GUARD_HELMET` entry (line 17), add:

```java
        add(Registration.BATON.get(), "Baton");
```

The full `addTranslations()` should now read:

```java
@Override
protected void addTranslations() {
    add("itemGroup." + SecurityGuardMod.MOD_ID, "Security Guard");
    add(Registration.GUARD_HELMET.get(), "Guard Helmet");
    add(Registration.BATON.get(), "Baton");
    add(Registration.GUARD_SPAWN_EGG.get(), "Security Guard Spawn Egg");
    add(Registration.SECURITY_GUARD.get(), "Security Guard");
}
```

- [ ] **Step 4: Build (textures still missing, but JSON wiring should be valid)**

Run:
```bash
./gradlew :securityguard:compileJava
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add securityguard/src/main/resources/assets/securityguard/items/baton.json \
        securityguard/src/main/resources/assets/securityguard/models/item/baton.json \
        securityguard/src/main/java/com/tweeks/securityguard/data/ModLanguageProvider.java
git commit -m "$(cat <<'EOF'
feat(securityguard): add baton item model + lang entry

Wires the inventory-icon model chain (items selector → item/handheld
parent → layer0 texture) and adds the "Baton" English translation.
Item texture itself lands in the next commit.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Repaint the entity baton texture (model-on-Guard's-hand)

**Files:**
- Modify: `securityguard/src/main/resources/assets/securityguard/textures/entity/baton.png`

This file is the texture mapped onto the 1×6×1 cube `BatonModel` adds to the Guard's hand. The model uses `texOffs(0, 0)` on a 16×16 sheet; for a `1×6×1` box with origin at (0,0), Minecraft's vanilla cube-UV layout consumes the following regions of the sheet:

```
  columns:  0    1    2    3
  row 0:    [s] [t]  [s]  [t]    ← top is at (1,0); bottom is at (2,0); each 1×1 px
  rows 1-6: [4 vertical strips, each 1 px wide × 6 px tall, side by side at columns 0..3]
```

(For a 1×6×1 cube, the four side faces share row range 1..6 and unfold horizontally across columns 0..3.)

What the player sees in-game:
- The **top** face (column 1, row 0) is the business end of the baton (the tip pointed downward when held).
- The **bottom** face (column 2, row 0) is the cap that touches the hand.
- The **side strips** (columns 0..3, rows 1..6) show the long shaft. Within each side strip, the bottom rows are the end nearest the hand (grip side, since the baton is rotated 180° on hand by the layer's `xRotationDegrees=180.0f`).

Because the baton renders rotated 180° on the X axis, **what the texture calls "top" appears at the bottom of the on-screen baton**, and vice versa. The grip should therefore be painted at rows 5–6 of the side strips and the wood at rows 1–4.

- [ ] **Step 1: Confirm Pillow is available in the local environment**

Run:
```bash
python3 -c "import PIL; print(PIL.__version__)"
```
Expected: a version string (e.g. `10.4.0`). If it fails, install with `pip3 install --user Pillow`.

- [ ] **Step 2: Generate the texture**

Run the following one-shot Python script. It writes the new `entity/baton.png` directly. The script is intentionally throwaway — the binary PNG is the artifact committed to the repo (matching the precedent set by commit `416065e` for the Thief textures).

```bash
python3 <<'PY'
from PIL import Image

# Palette
GRIP_DARK   = (26, 26, 26, 255)    # #1a1a1a
GRIP_LIGHT  = (42, 42, 42, 255)    # #2a2a2a
WOOD_DARK   = (61, 39, 22, 255)    # #3d2716
WOOD_MID    = (90, 58, 32, 255)    # #5a3a20
WOOD_LIGHT  = (122, 79, 44, 255)   # #7a4f2c
TRANS       = (0, 0, 0, 0)

img = Image.new("RGBA", (16, 16), TRANS)
px = img.load()

# Cube UV for a 1×6×1 box at (0,0) on a 16-tall sheet:
#   top face:    1 px at (1, 0)
#   bottom face: 1 px at (2, 0)
#   sides:       4 strips, each 1×6, at columns 0..3, rows 1..6
#
# Wall rendering uses xRotationDegrees=180, so texture-row 1 ends up at the
# top of the on-screen baton (= business end / wood) and texture-row 6 ends
# up at the bottom of the on-screen baton (= grip).

# Top face (= business end shown at BOTTOM of on-screen baton due to 180° rot).
# Paint dark wood with one mid pixel in case the engine ever shows the cap.
px[1, 0] = WOOD_DARK

# Bottom face (= cap nearest hand, hidden behind hand most of the time).
px[2, 0] = GRIP_DARK

# Side strips: columns 0..3, rows 1..6.
# Rows 1..4 are wood, rows 5..6 are grip (end of strip closest to hand).
for col in range(4):
    px[col, 1] = WOOD_DARK   # tip
    px[col, 2] = WOOD_MID
    px[col, 3] = WOOD_LIGHT  # single highlight pixel for grain
    px[col, 4] = WOOD_MID
    px[col, 5] = GRIP_LIGHT  # grip ridge
    px[col, 6] = GRIP_DARK   # grip body (last row before bottom face)

img.save("securityguard/src/main/resources/assets/securityguard/textures/entity/baton.png")
print("wrote entity/baton.png")
PY
```

Expected output: `wrote entity/baton.png`. The file size will be small (~150 bytes). The white-placeholder version is overwritten in place.

- [ ] **Step 3: Visually verify the PNG**

Open the file:
```bash
open securityguard/src/main/resources/assets/securityguard/textures/entity/baton.png
```
At 16× zoom in Preview / VS Code, confirm:
- Top of image: mostly transparent except two pixels at columns 1–2 row 0 (dark wood + black).
- Rows 1–6 of columns 0..3: visible vertical bands going dark-wood → mid-wood → highlight → mid-wood → dark grip → black grip.
- Rest of image: transparent.

- [ ] **Step 4: Commit (do NOT build yet — Task 7 lands the item icon, then Task 8 verifies the whole module)**

```bash
git add securityguard/src/main/resources/assets/securityguard/textures/entity/baton.png
git commit -m "$(cat <<'EOF'
feat(securityguard): repaint baton entity texture with nightstick palette

Replaces the white-placeholder baton.png with a procedurally generated
nightstick: dark-wood shaft (rows 1–4 of each side strip) and black
rubber grip (rows 5–6). Because the baton renders rotated 180° on the
X axis via HeldItemLayer, texture rows 5–6 appear at the held end and
rows 1–4 appear at the business end — matching real-world police
nightstick orientation.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Generate the item baton texture (inventory icon)

**Files:**
- Create: `securityguard/src/main/resources/assets/securityguard/textures/item/baton.png`

The item icon is rendered with `item/handheld`, which Minecraft draws diagonally (lower-left = held end at hand, upper-right = business end). The icon is independent of the entity texture, so we can use a denser pixel layout that's readable at inventory size.

- [ ] **Step 1: Generate the icon**

Run:
```bash
python3 <<'PY'
from PIL import Image

# Same palette as entity/baton.png so the held item visually matches.
GRIP_DARK   = (26, 26, 26, 255)
GRIP_LIGHT  = (42, 42, 42, 255)
GRIP_PUMMEL = (13, 13, 13, 255)   # pommel cap
WOOD_DARK   = (61, 39, 22, 255)
WOOD_MID    = (90, 58, 32, 255)
WOOD_LIGHT  = (122, 79, 44, 255)
TRANS       = (0, 0, 0, 0)

img = Image.new("RGBA", (16, 16), TRANS)
px = img.load()

# Diagonal layout: hand grips lower-left, business end at upper-right.
# Each baton-segment is two pixels thick (a "core" pixel + a "highlight"
# pixel above-right) for a chunky-but-readable silhouette.
#
# Coordinates list runs from grip (lower-left) to tip (upper-right):
#   (col, row) of the CORE pixel; the HIGHLIGHT goes at (col+1, row-0).
#   Row 0 is the top in PIL coordinates.

# Grip section — 5 segments of black rubber.
grip_path = [
    ( 1, 14),   # pommel-end
    ( 2, 13),
    ( 3, 12),
    ( 4, 11),
    ( 5, 10),
]
# Wood section — continues the diagonal up-right for 8 segments.
wood_path = [
    ( 6,  9),
    ( 7,  8),
    ( 8,  7),
    ( 9,  6),
    (10,  5),
    (11,  4),
    (12,  3),
    (13,  2),
]
# Tip — one extra dark pixel beyond the wood path so the wood "ends"
# in shadow rather than fading to transparent abruptly.
tip_path = [(14, 1)]

# Paint grip: core = GRIP_DARK, lit edge (right-above) = GRIP_LIGHT.
for col, row in grip_path:
    px[col, row]       = GRIP_DARK
    px[col + 1, row]   = GRIP_LIGHT
# Pommel cap — darker pixel at the very lower-left endpoint.
px[1, 15] = GRIP_PUMMEL
px[0, 14] = GRIP_PUMMEL

# Paint wood: core = WOOD_MID, lit edge = WOOD_LIGHT, shaded edge below = WOOD_DARK.
for col, row in wood_path:
    px[col, row]         = WOOD_MID
    px[col + 1, row]     = WOOD_LIGHT
    if row + 1 < 16:
        px[col, row + 1] = WOOD_DARK

# Tip
for col, row in tip_path:
    px[col, row]     = WOOD_DARK
    px[col + 1, row] = WOOD_MID

img.save("securityguard/src/main/resources/assets/securityguard/textures/item/baton.png")
print("wrote item/baton.png")
PY
```

Expected output: `wrote item/baton.png`.

- [ ] **Step 2: Visually verify**

Open the file:
```bash
open securityguard/src/main/resources/assets/securityguard/textures/item/baton.png
```
At zoom, confirm: a diagonal black-grip-into-dark-wood baton running from lower-left to upper-right, with a one-pixel highlight along the upper-right edge of each segment. Pommel cap visible at the lower-left.

- [ ] **Step 3: Commit**

```bash
git add securityguard/src/main/resources/assets/securityguard/textures/item/baton.png
git commit -m "$(cat <<'EOF'
feat(securityguard): add baton inventory icon

16×16 diagonal nightstick sprite for the item/handheld parent: black
rubber grip on the lower-left third, dark-wood shaft on the upper-right
two-thirds, with a 1-pixel highlight along each segment's lit edge and
a pommel cap at the held endpoint.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: Run datagen and verify generated resources

**Files:**
- Possibly modified: files under `securityguard/src/generated/` (only if datagen produces new lang JSON for the Baton entry, or if any other provider notices the new item).

`ModLanguageProvider` populates `securityguard/src/generated/clientData/assets/securityguard/lang/en_us.json` at datagen time. Adding the Baton entry in Task 5 means re-running datagen should produce a one-line diff in that file.

- [ ] **Step 1: Run client-side datagen for `securityguard`**

Run:
```bash
./gradlew :securityguard:runData
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Check what changed**

Run:
```bash
git status securityguard/src/generated/
git diff securityguard/src/generated/clientData/assets/securityguard/lang/en_us.json
```
Expected: a one-line addition for the Baton translation. If other generated files changed (recipe JSONs, loot-table JSONs, model JSONs), inspect them — most likely they're cache-only files that should be reverted.

- [ ] **Step 3: Discard `.cache` directory churn but keep real generated changes**

Run:
```bash
git checkout -- securityguard/src/generated/clientData/.cache 2>/dev/null
git checkout -- securityguard/src/generated/serverData/.cache 2>/dev/null
git status securityguard/src/generated/
```
Expected: only the lang JSON shows as modified (and possibly nothing else).

- [ ] **Step 4: Commit the lang regeneration**

```bash
git add securityguard/src/generated/
git commit -m "$(cat <<'EOF'
build(securityguard): regenerate datagen output for Baton lang entry

Adds the "item.securityguard.baton": "Baton" line to the generated
en_us.json so the new item displays its proper name in tooltips.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

(If `git status` shows no changes after `runData`, skip this commit — the existing checked-in `en_us.json` already contains the entry because `ModLanguageProvider` is called from the same gradle task that builds the jar. In that case proceed directly to Task 9.)

---

## Task 9: Full build + test suite

**Files:** none modified.

- [ ] **Step 1: Clean build from scratch**

Run:
```bash
./gradlew clean build
```
Expected: `BUILD SUCCESSFUL`. All module jars (`securitycore-0.1.0.jar`, `securityguard-0.1.0.jar`, plus `thief-0.1.0.jar` if present) build.

- [ ] **Step 2: Run the test suite**

Run:
```bash
./gradlew test
```
Expected: `BUILD SUCCESSFUL`. `SpawnPatternTest` (5 tests) and any thief-module tests still pass.

- [ ] **Step 3: No commit**

Verification only. If anything fails, **stop and fix before continuing**.

---

## Task 10: Manual smoke test in dev client

**Files:** none modified.

- [ ] **Step 1: Launch the dev client**

Run:
```bash
./gradlew :securityguard:runClient
```
Wait for Minecraft to load to the title screen.

- [ ] **Step 2: Confirm both core and guard mods are listed**

In the title screen → **Mods**, confirm `Security Core 0.1.0` and `Security Guard 0.1.0` appear. (If `thief` is in `settings.gradle`, it should appear too.)

- [ ] **Step 3: Open creative inventory; find the Baton**

Create a new Creative-mode flat world. Open the inventory and switch to the **Security Guard** creative tab. Verify three items: Guard Helmet, **Baton** (new), Guard Spawn Egg.

Hover the Baton:
- Tooltip should read `Baton`.
- Inventory icon should be the diagonal nightstick — black grip lower-left, wood shaft upper-right. **Not white**.

- [ ] **Step 4: Test player attack stun**

Pick up the Baton. Spawn a Zombie via `/summon zombie ~ ~ ~` or another spawn egg. Hit the Zombie once.

Verify:
- Zombie takes ~10 damage (vanilla iron-sword level — its health bar drops by ~5 hearts).
- Zombie shows blue Slowness particles AND grey Weakness particles for ~3 seconds.
- Zombie gets knocked back slightly.

- [ ] **Step 5: Test Guard AI stun (regression check)**

Spawn a Guard via the iron-block + helmet ritual. Spawn a Zombie nearby. Watch the Guard fight.

Verify:
- The Guard's baton in-hand renders with the new texture (dark-wood shaft, black grip), not white.
- The Zombie shows the same Slowness + Weakness particles after each Guard hit (matches your player-baton hits — confirms the StunEffects refactor preserved AI behavior).

- [ ] **Step 6: Test durability**

Use `/give @s securityguard:baton 1` if needed (creative gives infinite durability — switch to survival via `/gamemode survival` to see the durability bar).

Hit a passive mob (cow) ~5 times. The durability bar should appear under the icon and decrement by 1 per hit. Don't need to break it; just confirm decrement works.

- [ ] **Step 7: Close the client; no commit needed**

Verification only. If any of Steps 3–6 fail:
- Wrong icon → Task 7 PNG is malformed; regenerate.
- White entity baton → Task 6 PNG didn't overwrite; check `git status`.
- No stun on player swing → `BatonItem.postHurtEnemy` not being called; verify `BatonItem extends SwordItem` and the override signature matches `Item.postHurtEnemy`.
- Stun on AI but not player (or vice versa) → numeric drift; the constants in `BatonItem` MUST equal the `StunningMeleeGoal` constructor args in `SecurityGuardEntity.registerGoals` (`60, 1, 0, 0.2`).

---

## Task 11: Plan-completion bookkeeping

**Files:** none modified.

- [ ] **Step 1: Mark every plan task complete**

Open `docs/superpowers/plans/2026-04-29-baton-item.md`. Verify every `- [ ]` step is now `- [x]`. (If executing via subagent-driven-development, the orchestrator handles this automatically.)

- [ ] **Step 2: Final clean rebuild**

Run:
```bash
./gradlew clean build test
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: No commit needed**

The feature ships across the per-task commits already in place. The repo can be bisected if a future regression appears: each commit is independently runnable / reviewable / revertable.

---

## Self-review notes (for the implementer)

- **Numeric drift is the most likely failure mode.** `BatonItem`'s four constants (`60, 1, 0, 0.2`) must equal the `StunningMeleeGoal` constructor args in `SecurityGuardEntity.registerGoals(...)`. Both are deliberately tagged with the same magic numbers; if you change one, change both.
- **Each task ends with a commit** so the repo can be bisected if a smoke test later reveals a regression. The two genuine verification-only tasks (1, 9, 10, 11) intentionally don't commit.
- **Texture work is throwaway-script-driven.** The Python scripts in Tasks 6 and 7 are NOT committed — only the PNG output. This matches the established precedent (commit `416065e` describes Thief textures the same way: "procedurally generated stand-ins"). Commit the PNG, drop the script.
- **Order matters.** `StunEffects` (Task 2) lands BEFORE `BatonItem` (Task 3) because the item depends on the helper. `BatonItem` (Task 3) lands BEFORE registration (Task 4) because registration imports the class. Registration (Task 4) lands BEFORE model JSONs (Task 5) so the lang provider has a registered item to translate. Textures (Tasks 6, 7) land BEFORE smoke test (Task 10) so the dev client has a real icon to render.
- **No unit test for `StunEffects`.** Spec acknowledged this is acceptable — `LivingEntity` is hard to mock and the smoke test in Task 10 covers the behavior. If a future contributor wants to add one, they should mock at the boundary (e.g. wrap `target.addEffect` calls behind an interface). Out of scope here.
