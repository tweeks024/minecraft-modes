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
- One new internal item: Baton (held-item visual on the guard, not player-usable)
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

Combined with the 1.2 s attack cooldown, this means a stunned target stays slowed and weakened for nearly the full re-strike window — letting the guard reliably chain hits on a single target.

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

### Baton (`securityguard:baton`)
- Internal item used only as a visual hand-held by the guard.
- Stack size: 1
- Not in any creative tab.
- Not craftable.
- If somehow obtained (commands), it functions as a plain item with no special damage or effects.
- Reasoning: Keeps v1 scoped. Player-wieldable baton is a clean v2 feature: add damage, stun-on-hit, durability — none of which complicate v1.

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
        │   │   ├── GuardHelmetItem.java
        │   │   └── BatonItem.java
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
        │       ├── textures/item/baton.png
        │       └── sounds.json               # remaps to villager sounds initially
        └── test/
            └── java/com/tweeks/securityguard/
                └── ... (unit tests for pattern detection)
```

### Key class responsibilities

- **`SecurityGuardMod`** — `@Mod` entry. Subscribes to mod-bus events: entity attribute creation, registries, client setup. Holds the mod ID constant.
- **`Registration`** — All `DeferredRegister` instances for entities, items, sounds, creative tabs, and the entity attributes supplier. Single source of truth for registry IDs.
- **`SecurityGuardEntity`** — Extends `net.minecraft.world.entity.animal.IronGolem` is tempting but couples too tightly to golem internals. Instead extends `AbstractGolem` (the shared parent of iron/snow golems) and reimplements the golem-style targeting goal set. Holds attribute defaults via a static `createAttributes()` method.
- **`BatonStrikeGoal`** — Custom melee attack goal. Extends `MeleeAttackGoal`; overrides `checkAndPerformAttack` to apply Slowness II and Weakness I to the target on a successful hit.
- **`GuardHelmetItem`** — Overrides `useOn(UseOnContext)`. On the server side: validates the 3-iron-block column above the clicked face, swaps blocks to air, spawns the entity, decrements the helmet stack, and plays a spawn sound + particle. Returns `InteractionResult.CONSUME` when matched, `PASS` otherwise.
- **`BatonItem`** — Minimal `Item` subclass. No special methods overridden.
- **`SecurityGuardModel`** — Extends `HumanoidModel<SecurityGuardEntity>`. Adds the cap as a child cube of the head part. Defines `LayerDefinition` via `createBodyLayer()`.
- **`SecurityGuardRenderer`** — Extends `MobRenderer`. Adds an `ItemInHandLayer` so the baton renders in the right hand.
- **`ClientSetup`** — Subscribes to `EntityRenderersEvent.RegisterRenderers` and `EntityRenderersEvent.RegisterLayerDefinitions`. Client-side only via `@Mod.EventBusSubscriber(value = Dist.CLIENT)`.
- **`DataGenerators`** + providers — Generate `en_us.json`, recipe JSON, and loot tables at build time from Java code rather than hand-writing JSON.

### Data flow: spawning a guard

```
Player right-clicks Guard Helmet on top of a 3-iron-block column
    ↓
GuardHelmetItem.useOn fires (server side)
    ↓
Check column: blockAt(pos), blockAt(pos.below()), blockAt(pos.below(2)) all IRON_BLOCK?
    ↓ yes
Replace those 3 positions with AIR
    ↓
SecurityGuardEntity guard = new SecurityGuardEntity(EntityType, level)
guard.moveTo(pos.below(2).getCenter())
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
    Reset cooldown to 24 ticks (1.2s)
    Trigger swing animation on client (already standard for melee goal)
    Play attack swing sound, then attack hit sound on hit
```

## Error Handling

- **Guard Helmet on non-iron column**: returns `InteractionResult.PASS`. No message needed; player will figure it out from the recipe documentation. (Avoiding chat spam from common mistakes.)
- **Helmet on iron column but not enough room above for entity**: spawn aborts, helmet not consumed, returns `PASS`. The 3-iron column means we need ~2 blocks of clear air above the bottom iron block for the guard to fit. Validate before consuming.
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

## Build & Distribution

- Gradle multi-module (root project owns `securityguard` as a subproject) so future mods slot in as siblings.
- NeoForge MDK template as the starting point for the subproject's build files.
- Java 21 (required by MC 1.21.x).
- No external runtime dependencies beyond NeoForge itself.
- Output: `securityguard-<version>.jar` in `securityguard/build/libs/`.
- Versioning: `0.1.0` for v1.
- License: TBD by user (default to MIT in initial scaffold; easy to change).

## Open Questions / Decisions Pending User Confirmation

These have all been answered or assumed in the design above. None block implementation. Listed for the user to override:

1. **Mod author / package name**: defaulted to `com.tweeks.securityguard`. Override if you want a different organization name.
2. **License**: defaulted to MIT in scaffold.
3. **Localization beyond English**: out of scope for v1.
4. **Guard skin variation**: out of scope for v1; one fixed appearance.
