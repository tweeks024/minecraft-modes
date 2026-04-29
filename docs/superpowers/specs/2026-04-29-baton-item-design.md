# Usable Baton — Design Spec

**Date:** 2026-04-29
**Status:** Design approved; awaiting plan
**Predecessors:**
- [2026-04-29 multi-module restructure](../plans/2026-04-29-multi-module-restructure.md) — established `:securitycore` as the home for shared AI helpers and `StunningMeleeGoal`.

## Goal

Make the Security Guard's baton a real, wieldable item:

1. **Visual fix.** Replace the placeholder white-on-white `entity/baton.png` with a readable police-nightstick texture (black rubber grip on the bottom third, dark polished-wood shaft on the upper two-thirds). Add a matching inventory icon at `item/baton.png`.
2. **Make it an item.** Register a new `BatonItem` so players can hold and swing it. Iron-tier weapon stats (251 durability, +6 attack damage) plus the same on-hit stun the Guard's AI applies (Slowness II + Weakness I for 3 s, knockback 0.2). Available from the Security Guard creative tab; no recipe, no drop.

The baton's stun behavior must be identical for player swings and Guard AI swings, so the Guard's `StunningMeleeGoal` and the new `BatonItem` share one stun-application helper.

## Non-goals

- No survival recipe or guard-drop in this iteration.
- No right-click charge / power-attack mechanic.
- No PvP-balance tuning. The baton is a flavor item; if players hit each other with it, the same Slowness II + Weakness I applies.
- No changes to how the Guard renders the baton on its hand. The visual fix is a texture replacement only; `BatonModel` and `HeldItemLayer` are unchanged.

## Architecture

### Module placement

| Component | Module | Reason |
|---|---|---|
| `StunEffects` static helper | `securitycore` | Pure stun primitive; reusable by future Thief blackjack and other Security Pack mobs. |
| `StunningMeleeGoal` (refactor) | `securitycore` | Already there; updated to call `StunEffects`. |
| `BatonItem` | `securityguard` | Concrete Guard weapon; specific to this mod. |
| Texture assets | `securityguard` | Guard owns the baton's appearance. |

### Components

#### 1. `StunEffects` — new static helper

**Location:** `securitycore/src/main/java/com/tweeks/securitycore/ai/StunEffects.java`

**API:**
```java
public final class StunEffects {
    private StunEffects() {}

    /**
     * Applies the Security Pack stun bundle to {@code target}: Slowness +
     * Weakness for {@code durationTicks}, plus a horizontal knockback away
     * from {@code attacker}. No-op if {@code target} is dead.
     */
    public static void applyStun(LivingEntity attacker,
                                 LivingEntity target,
                                 int durationTicks,
                                 int slownessAmplifier,
                                 int weaknessAmplifier,
                                 double knockbackStrength) { ... }
}
```

The implementation mirrors what's currently inlined in `StunningMeleeGoal.checkAndPerformAttack`:

```java
if (!target.isAlive()) return;
target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, durationTicks, slownessAmplifier));
target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, weaknessAmplifier));
target.knockback(knockbackStrength,
    attacker.getX() - target.getX(),
    attacker.getZ() - target.getZ());
```

#### 2. `StunningMeleeGoal` — refactor to delegate

`checkAndPerformAttack` keeps the swing/cooldown/`doHurtTarget` plumbing but replaces its inline effect block with one call:

```java
StunEffects.applyStun(this.mob, target,
    stunDurationTicks, slownessAmplifier, weaknessAmplifier, knockbackStrength);
```

Behavior is identical; the goal just stops owning the effect-application detail.

#### 3. `BatonItem` — new player-wieldable weapon

**Location:** `securityguard/src/main/java/com/tweeks/securityguard/item/BatonItem.java`

**Approach (verify exact API at implementation time):** NeoForge 26.1.2 ships with a transitional item-attributes model. The two viable paths are:

- **Option A — extend `SwordItem` with `Tiers.IRON`.** If `SwordItem` and the `Tier` API still exist in 26.1.2, this is the smallest change. `SwordItem` already wires +6 damage / iron tier and gives the right disassemble/inventory behavior. We override `hurtEnemy(ItemStack, LivingEntity, LivingEntity)` (or whatever the post-`postHurtEnemy` hook is named in 26.1.2 — see verification below) to call `StunEffects.applyStun(attacker, target, 60, 1, 0, 0.2)`.
- **Option B — extend `Item` with explicit `Item.Properties`.** Configure `durability(250)`, `attributes(ItemAttributeModifiers...)` with attack-damage and attack-speed modifiers matching iron sword (+6 damage, 1.6 attack speed equivalent), then implement the on-hit hook directly.

**Decision rule at implementation:** First try Option A. If `SwordItem` is removed/renamed in 26.1.2, fall back to Option B. The end-user behavior is identical either way.

**Stun parameters used by the override:** `60, 1, 0, 0.2` — exactly matching `SecurityGuardEntity`'s `StunningMeleeGoal` arguments (Task 8 of the multi-module restructure plan), so AI- and player-swung batons are indistinguishable in effect.

#### 4. `Registration.java` — register and surface in creative tab

Add:

```java
public static final DeferredItem<BatonItem> BATON = ITEMS.registerItem("baton",
    BatonItem::new,
    p -> p.durability(250));   // attribute modifiers configured inside BatonItem
```

(Exact `Properties` chain depends on Option A vs B above.)

In `SECURITY_GUARD_TAB.displayItems(...)` add `output.accept(BATON.get());` after the spawn egg.

#### 5. Item model + asset JSONs

Mirror the guard-helmet pattern:

- `securityguard/src/main/resources/assets/securityguard/items/baton.json`
  ```json
  {
    "model": {
      "type": "minecraft:model",
      "model": "securityguard:item/baton"
    }
  }
  ```
- `securityguard/src/main/resources/assets/securityguard/models/item/baton.json`
  ```json
  {
    "parent": "minecraft:item/handheld",
    "textures": {
      "layer0": "securityguard:item/baton"
    }
  }
  ```
  (`item/handheld` instead of `item/generated` so the icon orients correctly when held in first-person — same parent vanilla swords use.)

#### 6. Lang entry

`ModLanguageProvider` adds: `Item.baton → "Baton"`.

#### 7. Textures

**`securityguard/src/main/resources/assets/securityguard/textures/entity/baton.png` (16×16, REPLACE existing white placeholder)**

Used by `BatonModel` (1×6×1 cube). The model calls `texOffs(0, 0)` and the texture sheet is declared 16×16 (`LayerDefinition.create(mesh, 16, 16)`). The standard Minecraft cube UV unwrap for a 1×6×1 cube starting at (0,0) on a 16×16 sheet uses these regions:

- Top face: (1,0)–(2,1), 1×1 px — wood-end colour
- Bottom face: (2,0)–(3,1), 1×1 px — black grip-end colour
- Four side faces: each 1 px wide × 6 px tall; horizontal strip at rows 1–7, columns 0..7

We paint the side strips so the bottom ~2 of the 6 vertical pixels are black grip and the upper 4 are dark-wood with a single-pixel highlight stripe for grain. This means the held end (bottom of the model, where the hand grips) reads as the rubber handle; the business end (top) reads as polished wood.

Palette (RGB hex):
- Grip black: `#1a1a1a` with one row of `#2a2a2a` for grip ridge
- Wood dark: `#3d2716`
- Wood mid: `#5a3a20`
- Wood highlight: `#7a4f2c` (single pixel for grain)

**`securityguard/src/main/resources/assets/securityguard/textures/item/baton.png` (16×16, NEW)**

Inventory icon. Diagonal baton sprite running lower-left to upper-right (matches the `item/handheld` orientation Minecraft uses for held-tool first-person rendering — the hand grips the lower-left end). Roughly:

- Lower-left ~5 pixels: black grip (`#1a1a1a` core, `#2a2a2a` highlight on the topside).
- Upper-right ~10 pixels: wood shaft (`#3d2716` core, `#5a3a20` along the lit edge, `#7a4f2c` single highlight pixel).
- Pommel cap on the lower-left endpoint: 1 px of `#0d0d0d` for definition.
- Single transparent pixel border around the diagonal so it doesn't bleed at icon edges.

The exact pixel layout is finalised at implementation time; the spec locks the palette and orientation, not pixel-by-pixel coordinates.

## Data flow

### Player swings baton
1. Player left-clicks an entity while holding `BatonItem`.
2. Vanilla combat path: `Player.attack(Entity)` → `LivingEntity.hurt(...)` → `Item.hurtEnemy(stack, target, attacker)` (or its 26.1.2 equivalent hook) on the held item.
3. `BatonItem.hurtEnemy` calls `StunEffects.applyStun(attacker, target, 60, 1, 0, 0.2)`.
4. Server applies effects; clients see slowed/weakened mob with knockback.

### Guard swings baton (AI)
1. `StunningMeleeGoal.checkAndPerformAttack` runs every cooldown tick.
2. After `doHurtTarget`, the goal calls `StunEffects.applyStun(this.mob, target, ...)` with the same numbers passed via its constructor (`SecurityGuardEntity` already passes `60, 1, 0, 0.2`).
3. Same effect outcome on the target.

The two paths converge on `StunEffects` — there is no second copy of the effect logic.

## Testing strategy

### Unit tests (`:securitycore:test`)

Add `StunEffectsTest`:
- Verifies `applyStun` no-ops when `target.isAlive()` returns false (mock).
- Verifies `applyStun` calls `addEffect` twice with `MobEffects.SLOWNESS` (correct duration + amplifier) and `MobEffects.WEAKNESS`.
- Verifies `applyStun` calls `knockback` with the strength and the correct `attacker.x - target.x`, `attacker.z - target.z` deltas.

Existing `SpawnPatternTest` continues to pass unchanged.

If mocking `LivingEntity` proves awkward (the class has many final methods), the unit-test scope can be reduced to a smoke test that simply constructs the call without throwing — manual smoke test in dev client covers the behavior.

### Manual smoke test (dev client)

1. `./gradlew :securityguard:runClient`. Both `Security Core` and `Security Guard` should load.
2. Open creative inventory → Security Guard tab → confirm three items: Guard Helmet, Guard Spawn Egg, **Baton** (new).
3. Pick up baton. Inspect the inventory icon — should be a diagonal nightstick with black grip + wood shaft, *not* white.
4. Spawn a Zombie. Hit it once with the baton. Verify:
   - Zombie takes ~6 damage (heart bar shows iron-sword damage).
   - Zombie shows Slowness II particles and Weakness I particles for ~3 s.
   - Zombie gets knocked back slightly.
5. Spawn a Guard via the helmet ritual. Confirm baton on its hand renders with the new texture (no longer white). Have the Guard fight the Zombie — same stun behavior.
6. Verify baton durability bar appears after enough swings; confirm it breaks after ~250 hits (or test by `/give` repeatedly damaging).

### Datagen check

Run `:securityguard:runData` after the registration changes; commit any new generated files (item-model JSON, lang entry).

## Files

### New files

```
securitycore/src/main/java/com/tweeks/securitycore/ai/StunEffects.java
securitycore/src/test/java/com/tweeks/securitycore/ai/StunEffectsTest.java   (best-effort)
securityguard/src/main/java/com/tweeks/securityguard/item/BatonItem.java
securityguard/src/main/resources/assets/securityguard/items/baton.json
securityguard/src/main/resources/assets/securityguard/models/item/baton.json
securityguard/src/main/resources/assets/securityguard/textures/item/baton.png
```

### Modified files

```
securitycore/src/main/java/com/tweeks/securitycore/ai/StunningMeleeGoal.java
    — delegate effect application to StunEffects.applyStun

securityguard/src/main/java/com/tweeks/securityguard/Registration.java
    — register BATON DeferredItem; surface in SECURITY_GUARD_TAB

securityguard/src/main/java/com/tweeks/securityguard/data/ModLanguageProvider.java
    — add Baton lang entry

securityguard/src/main/resources/assets/securityguard/textures/entity/baton.png
    — repaint from white placeholder to police-nightstick palette
```

### Deleted files

None.

## Open questions / verification at implementation

1. **Exact NeoForge 26.1.2 item-attack API.** Confirm whether `SwordItem` + `Tiers.IRON` still exists or if `Item.Properties.attributes(...)` with explicit `ItemAttributeModifiers` is the only path. Either yields the same in-game stats; commit message should record which path was used so future maintainers know.
2. **`hurtEnemy` hook name.** Verify `Item.hurtEnemy(ItemStack, LivingEntity, LivingEntity)` is still the override point in 26.1.2, or if it's renamed (e.g. `postHurtEnemy`). The spec assumes `hurtEnemy`; implementation grep-confirms.
3. **Texture pixel-by-pixel.** The palette and zone-layout are spec'd; final pixel placement is decided at implementation while inspecting the result in the dev client.

These are minor open points whose resolution doesn't change the design. They're flagged so the implementer doesn't get stuck if a method signature drifted.

## Self-review notes

- Single source of truth for stun behavior: ✓ (everything routes through `StunEffects.applyStun`).
- Module placement matches the existing securitycore-as-primitives convention: ✓.
- No retroactive changes to entity rendering or guard model: ✓.
- Numeric stun parameters cited in two places (the spec and the implementation) match exactly: `60, 1, 0, 0.2`.
- Scope sized for one PR; depends on completed multi-module restructure landing first: ✓.
