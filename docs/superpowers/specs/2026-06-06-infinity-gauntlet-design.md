# Infinity Gauntlet — Design

## Summary

Add an end-game, six-mode magic item to the `wildwest` mod: the **Infinity Gauntlet**. The gauntlet has six "stones" (Power, Space, Time, Mind, Reality, Soul) — each with its own active ability. The player swaps stones with a custom **radial picker** (default key: `G`) and activates the selected stone's ability with right-click. The item is obtainable in creative or via an end-game crafting recipe.

Single Java item, NeoForge 26.1.2. No Bedrock parity.

## Goals

- One self-contained item in the existing wildwest item-tier alongside `MeteorStaff`, `PistonGauntlet`, etc.
- Six distinct active abilities, balanced for survival play (cooldowns, durability).
- Polished mode-selection UX via radial picker — not shift-scroll or sneak-cycle.
- Per-stone cooldowns (independent), shared durability.

## Non-goals

- No infinity-stone collection meta-progression. Gauntlet always has all six modes.
- No literal "snap half of life in the dimension" — too far from the wildwest tone.
- No Bedrock port. Java-only.
- No HUD overlay icon. Active stone shown in item name + tooltip only.
- No enchantments. No anvil repair.

## User experience

1. Player obtains gauntlet (creative tab or shaped recipe).
2. Holds it in main or off hand. Default active stone: **Power**.
3. Presses `G` (configurable) → radial picker `Screen` opens, hot-mouse aims at one of six colored wedges.
4. Click a wedge OR release `G` over a wedge → that stone becomes active. Screen closes.
5. Right-click → activates the selected stone's ability. Cooldown timer on the hotbar slot reflects only that stone's cooldown.
6. Switching stones while another is on cooldown is allowed; cooldowns are per-stone.

Item display name reads "Infinity Gauntlet (Power)" / "(Space)" / etc. — the active stone is appended via `Item#getName()` override.

## Stones, in detail

Each stone has: ability description, cooldown (ticks), durability cost, color (used for particles + radial wedge), and sound.

| # | Stone | Color (RGB hex) | Cooldown | Durability cost | Ability |
|---|---|---|---|---|---|
| 0 | **Power** | `#A020F0` purple | 400t (20s) | 2 | AOE shockwave at player position: 6 damage + 3-block knockback to all `LivingEntity` hostiles within 5 blocks. Vanilla `ParticleTypes.EXPLOSION` ring + sound `ENTITY_GENERIC_EXPLODE`. Damage source: `WildWestDamageTypes.infinityPower(player)`. |
| 1 | **Space** | `#1E90FF` blue | 300t (15s) | 3 | Teleport to look-target up to 32 blocks. Uses `Level#clip` to find first block-collision along eye→eye+look*32; teleport to the position 1 block back from the hit (so the player lands in air). If no block hit (open sky), teleport full 32 blocks along look direction. Cancel fall damage on landing (set `fallDistance = 0`). `ParticleTypes.REVERSE_PORTAL` at origin and destination + sound `ENTITY_ENDERMAN_TELEPORT`. |
| 2 | **Time** | `#32CD32` green | 600t (30s) | 4 | Apply `MobEffects.MOVEMENT_SLOWDOWN` amplifier 3 (Slowness IV) and `MobEffects.DIG_SLOWDOWN` amplifier 2 (Mining Fatigue III) for 160t (8s) to all `LivingEntity` hostiles within 6 blocks of the player. Vanilla `ParticleTypes.GLOW` cloud + sound `BLOCK_BEACON_AMBIENT`. |
| 3 | **Mind** | `#FFD700` yellow | 500t (25s) | 3 | Charm 1 looked-at `Mob` (raytrace 8 blocks): for 15s the mob's AI target is overridden to "fight nearest hostile mob." Implementation: store the player's UUID in a transient mob-side data (NeoForge `AttachmentType<MindCharmAttachment>`) with an expiry tick; a tick-event handler iterates mobs with the attachment and sets `Mob#setTarget(...)` to the nearest hostile within 16 blocks. On expiry the attachment is removed and the mob's targeting reverts to its goals. Particles: `ParticleTypes.ENCHANT` swirling on the target. Sound: `ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM` on cast. |
| 4 | **Reality** | `#FF4500` red | 200t (10s) | 1 | Thanos-bubble: 1 looked-at hostile `Mob` (raytrace 8 blocks) is transformed for 60s into a `Bat`. Implementation: record the original entity type + serialized NBT in a `AttachmentType<RealityBubbleAttachment>` on the *new* (Bat) entity; after 60s the bat is killed silently and the original mob is re-summoned at the bat's position with restored NBT (full HP and effects). On player-kill of the bat: the original mob is restored at full HP (Thanos-bubble cannot be cheesed — the original is not destroyed). If the chunk unloads while the bat exists, the attachment is persisted and timer continues on chunk reload; if the bat dies in any other way (e.g. lava), restoration is skipped (acceptable v1 caveat). Red dust particle cloud + sound `ENTITY_EVOKER_CAST_SPELL`. |
| 5 | **Soul** | `#FFA500` orange | 240t (12s) | 2 | Soul siphon: ranged ray (raytrace 16 blocks) at first hit `LivingEntity` — deals 4 damage (damage type `WildWestDamageTypes.infinitySoul(player)`) AND heals player for 4 HP. If no entity hit, ability fails (no cooldown applied, no durability cost — same pattern as missed snowball). `ParticleTypes.SOUL` trail between player and target + sound `BLOCK_SOUL_SAND_BREAK`. |

### Damage types

Two new entries in `WildWestDamageTypes`:
- `INFINITY_POWER` — Power stone AOE
- `INFINITY_SOUL` — Soul stone ray

Reality and Time abilities don't deal damage. Mind, Space don't either.

### Cooldown model

Per-stone cooldowns are tracked as a `DataComponentType<long[]>` on the item stack: a 6-element array of "tick at which this stone becomes available again." Vanilla `Player#getCooldowns()` is shared across the whole item, so we can't use it for per-stone cooldowns. Instead we read the world's `gameTime()` and compare against `cooldownsUntil[activeStone]`.

On right-click:
```
if (level.gameTime() < cooldownsUntil[activeStone]) return InteractionResult.FAIL;
// ... ability code ...
cooldownsUntil[activeStone] = level.gameTime() + COOLDOWN_TICKS[activeStone];
```

The hotbar cooldown sweep visual (the vanilla overlay) is driven by `Player#getCooldowns()`, so we ALSO call `player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS[activeStone])` after a successful cast — this gives the active stone's cooldown a visible sweep, while the underlying per-stone tracking lives in the data component.

### Durability

500 total. Not enchantable, not repairable. Different stones consume different amounts (see table). Item breaks when durability hits 0 — no special break behavior.

### Active stone storage

Stored as `DataComponentType<Integer>` (0..5) on the stack. Default 0 (Power). Mutated by the radial picker via packet (see Network).

## Recipe (end-game brutal)

Shaped 3x3:

```
S D S
N * N
H T H
```

| Symbol | Item | Notes |
|---|---|---|
| `S` | `minecraft:echo_shard` | Ancient City Warden drop |
| `D` | `minecraft:wither_skeleton_skull` | Wither component (stand-in for "Dragon Head" — easier to source, still brutal) |
| `N` | `minecraft:netherite_block` | Gauntlet body |
| `*` | `minecraft:nether_star` | Center — requires Wither kill |
| `H` | `minecraft:heart_of_the_sea` | Elder Guardian / buried treasure |
| `T` | `minecraft:totem_of_undying` | Evoker drop |

Output: 1× `wildwest:infinity_gauntlet`. Crafting category `combat`. Unlock criterion: `has_nether_star`.

(Decision: substituted Wither Skeleton Skull for the harder-to-source "dragon head" — wither skeleton skulls also gate behind a Nether challenge and are mechanically identical for recipe purposes.)

## Architecture

### New files (Java)

- `item/InfinityGauntletItem.java` — main item class. Holds `use()`, `getName()`, `appendHoverText()`, and dispatches to per-stone handlers.
- `item/InfinityStone.java` — enum with the six stones. Each enum value holds: name, color, cooldown ticks, durability cost, sound event resource key, particle, abstract `cast(ServerLevel, ServerPlayer, ItemStack)` method.
- `item/InfinityGauntletComponents.java` — `DataComponentType` registrations: `ACTIVE_STONE` (Integer 0–5) and `COOLDOWNS` (long[] length 6).
- `effect/MindCharmAttachment.java` — small record `{UUID playerUuid, long expiresAtTick}`. `AttachmentType` registered for `Mob`.
- `effect/RealityBubbleAttachment.java` — small record `{CompoundTag originalNbt, ResourceLocation originalType, long expiresAtTick}`. `AttachmentType` registered for `Bat`.
- `effect/MindCharmTicker.java` — `@EventBusSubscriber` listening on `EntityTickEvent.Post` (or `ServerTickEvent` walking the level). Sets target on charmed mobs; clears attachment + reverts on expiry.
- `effect/RealityBubbleTicker.java` — same event hook; on expiry, despawns bat + spawns restored original entity.
- `network/C2SSetActiveStonePacket.java` — record `{int stoneIndex, InteractionHand hand}`. Server-side handler updates the held stack's `ACTIVE_STONE` component (validates the player is actually holding a gauntlet in that hand, rejects out-of-range stoneIndex).
- `client/InfinityGauntletKeybind.java` — `@EventBusSubscriber(Dist.CLIENT)` registering a `KeyMapping` named `key.wildwest.infinity_gauntlet_radial` (default `G`), category `key.categories.wildwest`. Handles `ClientTickEvent.Post` to detect press while holding gauntlet → opens radial screen.
- `client/RadialPickerScreen.java` — extends `Screen`. Renders 6 colored wedges around screen center. `mouseClicked` and `onClose` resolve which wedge mouse is over → fire `C2SSetActiveStonePacket` → close.

### Modified files (Java)

- `Registration.java` — register `INFINITY_GAUNTLET` `DeferredItem` (stack 1, durability 500, rarity `EPIC`); add to `WILDWEST_TAB.displayItems`.
- `WildWestMod.java` (or wherever components register) — register the two `DataComponentType`s on mod init. (Check existing registration site for components — if none exists, create `ModDataComponents.java`.)
- `WildWestDamageTypes.java` — add `INFINITY_POWER` and `INFINITY_SOUL` resource keys + factory methods.
- `data/ModDamageTypeProvider.java` — register the two new damage types in bootstrap.
- `data/ModRecipeProvider.java` — add the shaped recipe.
- `data/ModLanguageProvider.java` — add translations (item name, 6 stone names, tooltip lines, key binding category + name).
- `network/NetworkHandlers.java` — register `C2SSetActiveStonePacket` as `playToServer`.

### New files (resources)

- `assets/wildwest/models/item/infinity_gauntlet.json` — `parent: minecraft:item/generated`, texture `wildwest:item/infinity_gauntlet`.
- `assets/wildwest/textures/item/infinity_gauntlet.png` — 16×16 placeholder (gold gauntlet w/ 6 colored dots). v1 ships a placeholder; art polish is a follow-up like the meteor staff was.

## Data flow

### Opening the radial picker

1. Client tick event detects `G` press, queries main+off hand for gauntlet.
2. If found: open `RadialPickerScreen`.
3. Screen renders, captures mouse position relative to center → wedge index.

### Selecting a stone

1. Click or close-with-wedge-active → send `C2SSetActiveStonePacket(stoneIndex, hand)`.
2. Server handler validates: player holds a gauntlet in that hand AND `0 <= stoneIndex <= 5`.
3. Server updates the `ItemStack`'s `ACTIVE_STONE` component (which auto-syncs to client via vanilla item-stack sync).
4. Screen closes immediately on client (don't wait for server ack — UX feels snappier; if rejected, server-side stack state remains; client will see the bounce-back on next sync).

### Casting

1. Right-click → `Item#use()` server-side.
2. Read `ACTIVE_STONE` component → resolve `InfinityStone` enum.
3. Read `COOLDOWNS` component → check `gameTime() < cooldownsUntil[activeStone]` → fail.
4. Call `stone.cast(level, player, stack)`. The cast may return a success/fail flag.
5. On success: update `COOLDOWNS` component, call `player.getCooldowns().addCooldown(stack, cd)`, `stack.hurtAndBreak(durabilityCost, player, slot)`, `player.swing(hand)`.
6. On fail (Soul ray missed everything): no cooldown, no durability, return `InteractionResult.PASS`.

## Error handling

- **Player switches hands during radial open**: packet carries `InteractionHand`; server re-checks that the held stack is still a gauntlet. If not, silently drop the packet (no exception).
- **Reality bubble bat killed by lava / void**: restoration skipped. Acceptable v1 — documented as "don't tank the bat into hazards."
- **Reality bubble chunk unload**: the attachment is `serializable` so it survives unload. Ticker only acts on loaded chunks; on reload the timer continues from the persisted `expiresAtTick`.
- **Mind-charmed mob killed during charm**: attachment dies with the mob. No cleanup needed.
- **Server restart with active charm/bubble**: attachments persist via NeoForge's serializer; tick handlers resume on world load. Acceptable.
- **Invalid stone index in packet**: drop packet, log debug.
- **Cooldowns component missing on stack** (e.g. legacy stack from creative menu before component default applied): treat as all-zeros (no cooldowns active).
- **Recipe smelting/conflict**: none expected — wither_skeleton_skull is also used in Wither summon, but recipes don't conflict on consumption.

## Testing strategy

- **Unit tests** (where feasible — `wildwest` uses JUnit for testable goal logic): test `InfinityStone` cooldown-tick math + radial-angle → stone-index calculation.
- **Manual dev-client smoke test** (recorded in commit message at end, per repo convention): obtain via creative, cycle through all 6 stones via radial, cast each, verify cooldown sweep + durability decrement. Same caveat as recent items (`Meteor Staff visuals`, `Null boss`, etc.): smoke test is manual and deferred from CI.
- **Datagen**: run `gradlew runData` to produce the recipe JSON and damage_type JSONs; confirm they appear in `src/generated/serverData/`.

## Open considerations (deferred)

- Multi-player griefing: in v1 the gauntlet hits hostiles only; non-hostile players and villagers are not affected by Power AOE / Time slow / Reality / Mind / Soul ray. (Soul ray will hit any `LivingEntity` the ray touches — explicit decision: PvP-enabled.) Could add a PvP server config later.
- Sound polish: v1 uses vanilla sounds. Custom OGGs would be a follow-up.
- Tooltip lore line ("Built in the heart of a star…") — v1 ships functional tooltips only.

## Acceptance criteria

- [ ] `wildwest:infinity_gauntlet` registered, appears in creative tab.
- [ ] Recipe craftable and produces 1 gauntlet.
- [ ] Holding gauntlet + pressing `G` opens radial picker; releasing/clicking on a wedge sets active stone.
- [ ] Each of the 6 stones casts its ability when right-clicked, plays its particle/sound, applies its cooldown and durability cost.
- [ ] Per-stone cooldowns are independent.
- [ ] Item name updates to reflect active stone.
- [ ] Tooltip lists the active stone's ability description.
- [ ] No crashes on server restart with active mind charm / reality bubble.
- [ ] `gradlew runData` succeeds; generated damage_type + recipe JSONs are present.
- [ ] `gradlew build` succeeds.
