# Security Guard Mod — Design

**Date:** 2026-04-29
**Status:** Approved (pending user review of this doc)
**Series:** First mod in the `minecraft-mods` series.

## Goal

Add a friendly, player-deployable humanoid mob — the Security Guard — that defends players and villagers from hostile mobs by whacking them with a baton. Conceptually a "human iron golem," but with stun-based combat instead of raw damage.

## Platform Decision

**Java only (NeoForge, MC 1.21.x)** for v1.

Java mods (NeoForge/Fabric) and Bedrock add-ons are entirely separate ecosystems with no shared codebase. A Bedrock port is deferred to a future spec; the only assets reusable across the platform divide will be source artwork (textures may need format/resolution conversion, models must be reauthored).

## Scope

### In scope (v1)
- One new entity: Security Guard
- One new spawn mechanism: 3-iron-block I-shape + Guard Helmet trigger item
- One new craftable item: Guard Helmet (spawn trigger only, not equippable armor)
- Baton rendered on the guard via a custom render layer (no item registration)
- Creative spawn egg
- Custom humanoid model + textures
- Custom attack animation (overhead baton swing)
- Custom sounds (idle, hurt, death, attack swing, attack impact, footsteps)
- Loot table (empty drops, balance)
- English localization

### Out of scope (deferred to future versions)
- Player-wieldable baton as a weapon
- Guard armor variants or rank progression
- Guard barracks / police station structure or world generation
- Patrol behavior tied to a placed "post" block
- Multi-guard squad coordination
- Bedrock add-on port
- Localization beyond English

## Mob Specification

### Identity
- Entity ID: `securityguard:guard`
- Display name: "Security Guard"
- Category: `MobCategory.MISC` (matches iron golem — does not count toward hostile/passive mob caps)
- Size: ~1.95 blocks tall × 0.6 wide (slightly taller than a villager)

### Stats
| Attribute | Value | Reasoning |
|---|---|---|
| Max health | 50 | Half of iron golem; humanoid not metal |
| Attack damage | 5 | Moderate; the stun is the real weapon |
| Movement speed | 0.32 | Faster than golem (0.25), slower than player (0.4); "patrol pace" |
| Attack cooldown | 1.2 s | Stun lasts 3 s, so hits chain |
| Knockback resistance | 0.5 | Sturdy but not immovable |
| Follow range | 32 | Standard hostile-tracking range |

### Combat: Stun-on-Hit
On each successful melee hit, the guard applies to the target:
- **Slowness II** for 3 seconds
- **Weakness I** for 3 seconds
- **Small explicit knockback** of 0.2 (in addition to the standard attack-damage knockback)

Combined with the 1.2 s attack cooldown, this means a stunned target stays slowed and weakened for nearly the full re-strike window — letting the guard reliably chain hits on a single target. The small extra knockback "resets" the target's momentum for an instant, making the Slowness application feel impactful rather than silent.

### Targeting (iron-golem rules)
The guard targets:
- All vanilla hostile mobs that iron golems target (zombies, skeletons, creepers, spiders, drowned, husks, pillagers, vindicators, evokers, witches, slimes, magma cubes, blazes, ghasts when reachable, hoglins/zoglins, piglin brutes, raiders, ravagers, vexes, wardens — match the iron golem target list at MC 1.21.x)
- Players with sufficiently negative reputation in a nearby village (matches iron golem behavior)
- Any entity that has attacked the guard
- Any entity that has attacked a nearby villager (revenge-on-aggressor goal)

Never targets: passive mobs, neutral mobs that haven't aggressed, players in good standing.

### Behavior Goals (priority order)
1. Float (swim out of water)
2. Melee attack current target (`BatonStrikeGoal`, custom)
3. Move toward target (`MoveTowardsTargetGoal`)
4. Wander randomly when no target
5. Look at nearby villagers / players
6. Idle look around

### Targeting Goals
1. Hurt-by-target (revenge)
2. Defend villagers (revenge on attackers of villagers)
3. Iron-golem-style hostile-mob targeting

### Persistence & Despawn
- Persistent: never despawns naturally, regardless of distance from player.
- Can be killed normally by hostile mobs or players.
- Drops nothing (intentional — prevents iron farming via recursive guard construction).

### Appearance
- Humanoid silhouette (player/villager scale, no abstract proportions).
- Dark navy uniform: long-sleeve shirt + trousers.
- Peaked cap (think traditional security guard / police cap).
- Black boots, black belt with simple buckle.
- Holds baton in right hand; left hand swings naturally.
- Skin tone: neutral default; randomization optional in v2.

### Animations
- Idle: subtle breathing / slight head sway.
- Walk: standard humanoid walk cycle, baton hand swings less than free hand.
- Attack: overhead baton swing, ~0.4 s, lands on the cooldown beat.
- Hurt: standard knockback + red flash.
- Death: collapse to side, despawns after the standard 20-tick animation.

### Sounds
v1 reuses villager sound events with pitch adjustments to keep scope tight; custom audio can be a v2 polish pass.

| Event | Source |
|---|---|
| Ambient/idle | `entity.villager.ambient`, pitch 0.85 |
| Hurt | `entity.villager.hurt`, pitch 0.85 |
| Death | `entity.villager.death`, pitch 0.85 |
| Step | `entity.player.step` (default) |
| Attack swing | `entity.player.attack.sweep` |
| Attack hit | `entity.player.attack.crit` |

Custom sound files can replace these later without changing the entity code, since they go through the registered sound events.

## Spawn Mechanism

### Construction Recipe (in-world)
- Place 3 iron blocks vertically (a single column).
- Right-click the top of the stack with a **Guard Helmet** item.
- The 3 iron blocks and the helmet are consumed; a Security Guard spawns at the base of the column.

The vertical I-shape is intentionally distinct from the iron golem's T-shape so the two recipes don't conflict and players can identify which they're building.

### Crafting: Guard Helmet
Standard 3×3 crafting recipe:
```
[ ][I][ ]
[I][D][I]
[ ][ ][ ]
```
Where `I` = iron ingot, `D` = blue dye. Outputs 1 Guard Helmet.

The Guard Helmet is a regular item — not equippable armor in v1, only a spawn trigger. (Making it wearable would invite "why doesn't it give protection" expectations and is deferred.)

### Creative Spawn Egg
- ID: `securityguard:guard_spawn_egg`
- Primary color: navy (uniform)
- Secondary color: silver/gray (cap badge)
- Appears in the mod's creative tab.

## Items

### Guard Helmet (`securityguard:guard_helmet`)
- Stack size: 64
- Use action: when used (`useOn`) on the top face of a block, checks for a 3-tall iron block column starting at that block. If matched, consumes the helmet, replaces the iron blocks with air, spawns a `SecurityGuardEntity` at the base.
- No other in-world function.
- **Not equippable in v1**: by default a plain `Item` does not gain the `Equippable` data component (introduced in 1.21.2+), so it cannot be worn in the head slot. Verified by ensuring no `equippable` data component is attached and the item is not added to any vanilla wearable tag. If a future version wants this as a cosmetic hat, add the `Equippable` data component then.

### Baton (no item)
The baton is **not** registered as an item. It exists only as a model rendered directly by the guard's renderer — see `SecurityGuardRenderer` below. This avoids any chance of the baton "leaking" into player inventories via dupe bugs, drop-on-death edge cases, or `/give` commands, and removes the need to maintain an unused item registration.

A future v2 player-wieldable baton can be added as a separate, properly-stat'd item without needing to remove or rename anything from v1.

## Architecture

### Module layout
```
minecraft-mods/
├── docs/superpowers/specs/
├── shared-assets/                      # cross-mod art source files (future)
└── securityguard/
    ├── build.gradle
    ├── gradle.properties
    ├── settings.gradle
    └── src/main/
        ├── java/com/tweeks/securityguard/
        │   ├── SecurityGuardMod.java         # @Mod entry point, common setup
        │   ├── Registration.java             # DeferredRegisters for entities/items/sounds/tabs
        │   ├── entity/
        │   │   ├── SecurityGuardEntity.java
        │   │   └── ai/
        │   │       └── BatonStrikeGoal.java
        │   ├── item/
        │   │   └── GuardHelmetItem.java
        │   ├── client/
        │   │   ├── ClientSetup.java          # renderer + model registration (client-only)
        │   │   ├── model/
        │   │   │   └── SecurityGuardModel.java
        │   │   └── renderer/
        │   │       └── SecurityGuardRenderer.java
        │   └── data/
        │       ├── DataGenerators.java
        │       ├── ModRecipeProvider.java
        │       ├── ModLootTableProvider.java
        │       ├── ModEntityLootProvider.java
        │       └── ModLanguageProvider.java
        ├── resources/
        │   ├── META-INF/neoforge.mods.toml
        │   ├── pack.mcmeta
        │   └── assets/securityguard/
        │       ├── lang/en_us.json           # generated by datagen
        │       ├── models/item/...
        │       ├── models/entity/...
        │       ├── textures/entity/security_guard.png
        │       ├── textures/item/guard_helmet.png
        │       ├── textures/entity/baton.png  # baton skin (rendered by SecurityGuardRenderer, no item)
        │       └── sounds.json               # remaps to villager sounds initially
        └── test/
            └── java/com/tweeks/securityguard/
                └── ... (unit tests for pattern detection)
```

### Key class responsibilities

- **`SecurityGuardMod`** — `@Mod` entry. Subscribes to mod-bus events: entity attribute creation, registries, client setup. Holds the mod ID constant.
- **`Registration`** — All `DeferredRegister` instances for entities, items, sounds, creative tabs, and the entity attributes supplier. Single source of truth for registry IDs.
- **`SecurityGuardEntity`** — Extends `net.minecraft.world.entity.animal.IronGolem` is tempting but couples too tightly to golem internals. Instead extends `AbstractGolem` (the shared parent of iron/snow golems) and reimplements the golem-style targeting goal set. Sets `setPlayerCreated(true)` after construction so it's marked as a player-built defender. Holds attribute defaults via a static `createAttributes()` method.
- **`BatonStrikeGoal`** — Custom melee attack goal. Extends `MeleeAttackGoal`; overrides `checkAndPerformAttack` to (1) apply 5 damage, (2) apply Slowness II + Weakness I (60-tick duration) to the target, and (3) call `target.knockback(0.2, ...)` for the explicit momentum reset.
- **`GuardHelmetItem`** — Overrides `useOn(UseOnContext)`. On the server side: validates `level.mayInteract(player, pos)` at each of the 3 iron-block positions (respects spawn protection / WorldGuard-style protection plugins), validates the 3-iron-block column above the clicked face, validates clear air above for the entity to fit, swaps blocks to air, spawns the entity, decrements the helmet stack, and plays a spawn sound + particle. Returns `InteractionResult.CONSUME` when matched and permitted, `FAIL` if a protection check denies, `PASS` otherwise.
- **`SecurityGuardModel`** — Extends `HumanoidModel<SecurityGuardEntity>`. Adds the cap as a child cube of the head part. Defines `LayerDefinition` via `createBodyLayer()`.
- **`SecurityGuardRenderer`** — Extends `MobRenderer`. Adds a custom `BatonHeldLayer` (subclass of `RenderLayer`) that renders the baton model directly in the right hand using the entity's right-arm transform — does **not** go through `ItemInHandLayer`, since there is no baton item to read from a hand slot.
- **`ClientSetup`** — Subscribes to `EntityRenderersEvent.RegisterRenderers` and `EntityRenderersEvent.RegisterLayerDefinitions`. Client-side only via `@Mod.EventBusSubscriber(value = Dist.CLIENT)`.
- **`DataGenerators`** + providers — Generate `en_us.json`, recipe JSON, and loot tables at build time from Java code rather than hand-writing JSON.

### Data flow: spawning a guard

```
Player right-clicks Guard Helmet on top of a 3-iron-block column
    ↓
GuardHelmetItem.useOn fires (server side)
    ↓
For each of the 3 column positions: level.mayInteract(player, pos)?
    ↓ yes (any no → return FAIL, no consumption)
Check column: blockAt(pos), blockAt(pos.below()), blockAt(pos.below(2)) all IRON_BLOCK?
    ↓ yes (no → return PASS)
Check 2 blocks of clear air above for entity to fit?
    ↓ yes (no → return PASS)
Replace those 3 positions with AIR
    ↓
SecurityGuardEntity guard = new SecurityGuardEntity(EntityType, level)
guard.moveTo(pos.below(2).getCenter())
guard.setPlayerCreated(true)
level.addFreshEntity(guard)
    ↓
Decrement helmet stack
Play sound: entity.iron_golem.repair (placeholder spawn ack)
Spawn particle: ITEM_GUARD_HELMET cracking
    ↓
Return InteractionResult.CONSUME
```

### Data flow: combat

```
SecurityGuardEntity tick
    ↓
Targeting goals select a target (hostile mob or village threat)
    ↓
BatonStrikeGoal.tick()
    ↓
If target in range AND attack cooldown ≤ 0:
    target.hurt(level.damageSources().mobAttack(this), 5.0f)
    target.addEffect(new MobEffectInstance(SLOWNESS, 60, 1))   # 60 ticks = 3s, amplifier 1 = II
    target.addEffect(new MobEffectInstance(WEAKNESS, 60, 0))   # amplifier 0 = I
    target.knockback(0.2, this.getX() - target.getX(), this.getZ() - target.getZ())  # explicit momentum reset
    Reset cooldown to 24 ticks (1.2s)
    Trigger swing animation on client (already standard for melee goal)
    Play attack swing sound, then attack hit sound on hit

Note: MobEffectInstance application is automatically synced from server to clients
by NeoForge's standard entity tracking — no custom packets required.
```

## Error Handling

- **Guard Helmet on non-iron column**: returns `InteractionResult.PASS`. No message needed; player will figure it out from the recipe documentation. (Avoiding chat spam from common mistakes.)
- **Helmet on iron column but not enough room above for entity**: spawn aborts, helmet not consumed, returns `PASS`. The 3-iron column means we need ~2 blocks of clear air above the bottom iron block for the guard to fit. Validate before consuming.
- **Helmet used inside a protected region**: `level.mayInteract(player, pos)` returns false for one or more column positions → returns `InteractionResult.FAIL`, no blocks modified, no helmet consumed. This respects spawn protection, WorldGuard regions, and other protection plugins exactly the way vanilla block-break checks do.
- **Server vs client**: All world mutation in `useOn` guarded by `level.isClientSide()` checks. Spawning happens on the server only.
- **Missing translations**: NeoForge falls back to the translation key, which is human-readable. No defensive code needed.

## Testing

### Unit tests (JUnit 5, no MC runtime)
- `GuardHelmetSpawnPatternTest` — pure-logic test of column detection. Mock the level access.
  - 3 iron blocks: matches
  - 2 iron blocks + dirt: no match
  - Iron column with no air above: no match
  - Iron column at world height limit: no match (no room)

### Manual integration testing
Run `./gradlew runClient` to launch a dev MC instance with the mod loaded. Verify in a creative world:
1. Guard Helmet recipe appears in the recipe book and produces the helmet.
2. Placing 3 iron blocks + helmet trigger spawns a guard. The blocks vanish.
3. Guard idles, then chases and attacks a spawned zombie. Stun visibly applied (look for slowness particles).
4. Spawn egg works from creative inventory.
5. Guard is persistent across chunk reload.
6. Guard does not attack the player or villagers.
7. Guard takes damage and dies normally; no item drops.

### GameTest (NeoForge framework, optional for v1)
If time allows: a `@GameTest` that spawns a guard and a zombie in a structure, asserts the zombie dies within N ticks. Skip if it adds material time — manual testing covers this.

## Implementation Order

A staged sequence that lets each step be independently tested before moving on:

1. **Project scaffold & registry plumbing** — Gradle multi-module setup, NeoForge MDK skeleton in `securityguard/`, mod entry point, `Registration` class with empty `DeferredRegister` instances, creative tab registered (initially empty). Verify the mod loads in `runClient`.
2. **Guard Helmet item + recipe** — register the item, add the crafting recipe via datagen, place it in the creative tab. Verify the recipe works in-game.
3. **Entity registration + minimal model** — register `SecurityGuardEntity` (default `AbstractGolem` behavior, no custom AI yet), `SecurityGuardModel`, `SecurityGuardRenderer` with the cap but no baton yet. Verify spawning via `/summon securityguard:guard` shows a humanoid with a cap.
4. **Spawn-on-helmet logic** — implement `GuardHelmetItem.useOn` with column detection, mayInteract checks, and air-clearance check. Verify the in-world recipe works.
5. **Spawn egg** — register the spawn egg with navy/silver colors. Verify it appears in the creative tab and spawns a guard.
6. **Baton render layer** — add the `BatonHeldLayer` to `SecurityGuardRenderer`, render the baton model in the right hand. Verify visually.
7. **AI & combat** — implement `BatonStrikeGoal` with stun + knockback, wire up the targeting goal set (revenge, defend villagers, iron-golem-style hostile targeting). Verify with a zombie in `runClient`.
8. **Sounds** — register sound events that map to villager sounds with pitch shift. Wire to entity sound methods.
9. **Loot table (empty), language file, polish** — datagen the empty entity loot table and `en_us.json`. Run through the full manual test checklist.

## Build & Distribution

- Gradle multi-module (root project owns `securityguard` as a subproject) so future mods slot in as siblings.
- NeoForge MDK template (MDK-26.1.2-ModDevGradle) as the starting point for the subproject's build files.
- **Minecraft 26.1.2** (latest as of April 2026; Mojang switched to year-based versioning).
- **NeoForge 26.1.2.30-beta** (latest stable for MC 26.1.2).
- **Java 25** (required by MC 26.1.2; Mojang ships JRE 25 with the launcher).
- **ModDevGradle 2.0.141** (Gradle plugin; significantly different from 1.x — uses `src/main/templates/META-INF/neoforge.mods.toml` instead of `src/main/resources/META-INF/`).
- **Gradle 9.2.1** via the bundled wrapper.
- No external runtime dependencies beyond NeoForge itself.
- No Parchment mappings (not yet released for 26.1.x; using Mojang's official mappings only).
- Output: `securityguard-<version>.jar` in `securityguard/build/libs/`.
- Versioning: `0.1.0` for v1.
- License: MIT.

## Open Questions / Decisions Pending User Confirmation

These have all been answered or assumed in the design above. None block implementation. Listed for the user to override:

1. **Mod author / package name**: defaulted to `com.tweeks.securityguard`. Override if you want a different organization name.
2. **License**: defaulted to MIT in scaffold.
3. **Localization beyond English**: out of scope for v1.
4. **Guard skin variation**: out of scope for v1; one fixed appearance.
