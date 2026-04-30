# Wild-West Mod Phase 1 — Module + Guns — Design

**Date:** 2026-04-30
**Status:** Draft (pending user review of this doc)
**Series:** Phase 1 of 3 for the Wild-West mod (third themed mod in `minecraft-mods`).

## Goal

Stand up a new `wildwest/` gradle subproject and ship two player-usable, mob-usable ranged weapons — a six-shooter pistol (hitscan, close range) and a bolt-action rifle (projectile, long range) — that later phases (mobs, horses, leader-follower AI) can build on without reworking the weapon layer.

## Platform

Java 25 / NeoForge 26.1.2.30-beta / Minecraft 26.1.2. Same target as `securityguard` and `thief`. Build plugin: `net.neoforged.moddev` 2.0.141.

## Phasing

The full Wild-West mod is decomposed into three phases, each with its own spec → plan → implementation:

| Phase | Scope | Status |
|---|---|---|
| **1 (this doc)** | `wildwest/` module setup + pistol + rifle | Draft |
| 2 | Four mobs (deputy, sherrif, bandit, bandit-leader) + hand weapons + faction markers | Future |
| 3 | Horse mounts + leader-follower AI polish | Future |

Phase 1 produces no mobs and no faction abstractions. Mobs and the `LawAlly`/`Outlaw` markers (or whatever shape phase 2 chooses) come in phase 2 — YAGNI for phase 1.

## Scope

### In scope (phase 1)

**Module:**
- New `wildwest/` gradle subproject. Mirrors `thief/build.gradle` (`net.neoforged.moddev` 2.0.141, Java 25 toolchain, NeoForge from root `gradle.properties`).
- Add `include 'wildwest'` to root `settings.gradle`.
- Module `gradle.properties`: `mod_id=wildwest`, `mod_name=Wild West`, `mod_version=0.1.0`, `mod_group_id=com.tweeks.wildwest`.
- `dependencies { implementation project(':securitycore') }` — wired even though phase 1 doesn't use core abstractions yet.
- All four modules' run blocks add each other's source sets so dev client loads `securitycore + securityguard + thief + wildwest`.
- `WildWestMod.java` main class. `Registration.java` declaring `DeferredRegister` for `Item`, `EntityType`, `DamageType`, `SoundEvent`.
- `neoforge.mods.toml` template at `wildwest/src/main/templates/META-INF/neoforge.mods.toml` declares `securitycore` as required dependency.

**Pistol (six-shooter, hitscan, close range):**
- `PistolItem extends Item`, stack size 1, durability 300.
- Right-click fires; cooldown 8 ticks (~2.5 shots/sec).
- Server-side raycast (block + entity) max range 16 blk; first-hit `LivingEntity` takes 5 dmg via `wildwest:gunshot` damage type.
- Static helper `fireFromMob(LivingEntity shooter, LivingEntity target)` for phase-2 mob AI to reuse.
- Tracer + muzzle particles via S2C packet; sound `pistol_fire`.
- bbmodel: single-state revolver.

**Rifle (bolt-action, projectile, long range):**
- `RifleItem extends Item`, stack size 1, durability 400.
- Right-click fires immediately; cooldown 40 ticks with visible bolt-cycle animation during the cooldown window.
- Spawns `BulletEntity` (custom `AbstractArrow` subclass), velocity = view × 6.0 b/tick, damage 9, max-life 12 ticks (~60–70 blk effective range allowing for gravity drop).
- Bolt-cycle anim driven by `ItemProperties.register(...)` predicate `bolt_state` (float 0.0–1.0 across cooldown); three bbmodel states (`ready`, `bolt_open`, `bolt_closing`) swapped via item-model JSON predicate overrides.
- Sound `rifle_fire` on shot; `bolt_cycle` queued ~10 ticks later.

**Custom damage type:**
- Single type `wildwest:gunshot`, used by both pistol and rifle.
- Lang keys: `death.attack.wildwest.gunshot` → `"%1$s was shot by %2$s"`; `death.attack.wildwest.gunshot.player` → `"%1$s was shot by %2$s using %3$s"`.
- Damage type tags: `is_projectile=true`, `bypasses_armor=false`. Datagen via `DamageTypeProvider`.

**Recipes (datagen):**
- Pistol: `I .` / `I W` / `. W` — iron ingot + plank, vertical L (barrel + grip).
- Rifle: `I I I` / `. W I` / `. W .` — three iron + two plank, long horizontal (barrel + stock).

**Sounds + particles:**
- Three custom sounds registered in `assets/wildwest/sounds.json`: `pistol_fire`, `rifle_fire`, `bolt_cycle`. Source: placeholder royalty-free `.ogg` files in `assets/wildwest/sounds/`. Swappable later.
- Particles: vanilla `SMOKE` at muzzle, vanilla `CRIT` line for tracer + bullet trail. No custom particle types in phase 1.

**bbmodels:**
- `wildwest/tools/pistol.bbmodel` — six-shooter, ~16×6×6 px envelope.
- `wildwest/tools/rifle.bbmodel` — three named display states (`ready`, `bolt_open`, `bolt_closing`), ~24×8×4 px envelope.
- `wildwest/tools/bullet.bbmodel` — projectile entity model, small lead slug.
- All exported to `assets/wildwest/models/item/*.json` and `assets/wildwest/models/entity/bullet.json`. Source `.bbmodel` files committed alongside generated JSON for editability.

**Datagen + localization:**
- `DataGenerators` entrypoint registering: `ModLanguageProvider` (item names, death messages, sound subtitles), `ModRecipeProvider`, `ModItemModelProvider` (auto-link to bbmodel JSON), `DamageTypeProvider`.
- English (`en_us`) only.

### Out of scope (phase 1)

- All four mobs (deputy, sherrif, bandit, bandit-leader) — **phase 2**.
- Hand weapons / melee weapons for any mob — **phase 2**.
- Faction marker interfaces (`LawAlly` / `Outlaw` or similar) in `securitycore` — **phase 2**.
- Horse mounts + leader-follower AI — **phase 3**.
- Ammo, magazines, bullets-as-inventory-item, reloads — **explicitly excluded** (design decision Q2=C: cooldown + durability replace ammo).
- Custom particle types (gunsmoke variant, tracer streak material).
- Localizations beyond English.
- Bedrock port.

## Design decisions

Captured here so future-you can audit context, not just outcomes.

| # | Decision | Chosen | Rejected |
|---|---|---|---|
| Q1 | Project decomposition | Three-phase split (module+guns → mobs → leaders+horses) | Mega-spec (one big plan); two-phase (footsoldiers+leaders together) |
| Q2 | Player-usability of guns | Player + mob usable, no ammo, cooldown + durability | Mob-only; full ammo + reload system |
| Q3 | Bullet mechanics | Hybrid: pistol hitscan, rifle projectile | All-hitscan; all-projectile |
| Q4 | Rifle firing model | Bolt-action: instant fire + visible rechamber animation in cooldown | Crossbow-style charge-and-release; plain cooldown without animation |
| Q5 | Stat balance | Standard preset (P: 5/16/8/300, R: 9/60/40/400) | Tame; Lethal |

## Module layout

```
wildwest/
├── build.gradle
├── gradle.properties
└── src/
    ├── main/
    │   ├── java/com/tweeks/wildwest/
    │   │   ├── WildWestMod.java
    │   │   ├── Registration.java
    │   │   ├── item/
    │   │   │   ├── PistolItem.java
    │   │   │   └── RifleItem.java
    │   │   ├── entity/
    │   │   │   └── BulletEntity.java
    │   │   ├── damage/
    │   │   │   └── WildWestDamageTypes.java
    │   │   ├── network/
    │   │   │   └── S2CTracerPacket.java
    │   │   ├── client/
    │   │   │   ├── ClientSetup.java       (registers item-property predicates, entity renderer)
    │   │   │   ├── BulletRenderer.java
    │   │   │   └── TracerRenderer.java    (handles S2CTracerPacket on client)
    │   │   └── data/
    │   │       ├── DataGenerators.java
    │   │       ├── ModRecipeProvider.java
    │   │       ├── ModLanguageProvider.java
    │   │       ├── ModItemModelProvider.java
    │   │       └── ModDamageTypeProvider.java
    │   ├── resources/
    │   │   ├── assets/wildwest/
    │   │   │   ├── lang/en_us.json                    (datagen output)
    │   │   │   ├── models/item/{pistol,rifle,bullet,rifle_bolt_open,rifle_bolt_closing}.json
    │   │   │   ├── models/entity/bullet.json
    │   │   │   ├── textures/item/{pistol,rifle,bullet}.png
    │   │   │   ├── textures/entity/bullet.png
    │   │   │   ├── sounds/{pistol_fire,rifle_fire,bolt_cycle}.ogg
    │   │   │   └── sounds.json
    │   │   └── data/wildwest/
    │   │       ├── damage_type/gunshot.json            (datagen)
    │   │       ├── recipe/{pistol,rifle}.json          (datagen)
    │   │       └── tags/damage_type/{is_projectile,bypasses_armor}.json (datagen)
    │   └── templates/META-INF/neoforge.mods.toml
    └── test/java/com/tweeks/wildwest/
        ├── PistolHitscanTest.java
        ├── RifleCooldownTest.java
        └── BulletEntityTest.java

wildwest/tools/
├── pistol.bbmodel
├── rifle.bbmodel
└── bullet.bbmodel
```

## Behavior detail

### Pistol firing (hitscan)

```
PistolItem.use(Level, Player, Hand):
  if level.isClientSide → return PASS
  if player.getCooldowns().isOnCooldown(stack) → return FAIL

  start = player.getEyePosition()
  end   = start + player.getViewVector(1.0).scale(16.0)

  blockHit  = level.clip(new ClipContext(start, end, BLOCK.COLLIDER, FLUID.NONE, player))
  blockDist = blockHit.type == MISS ? 16.0 : start.distanceTo(blockHit.getLocation())

  candidates = level.getEntities(player, new AABB(start, end), e ->
      e instanceof LivingEntity && e != player && e.isAlive())

  nearestEntityHit = null; nearestEntityDist = Infinity
  for entity in candidates:
    optional = entity.getBoundingBox().inflate(0.3).clip(start, end)
    if optional.present and start.distanceTo(optional.get()) < blockDist
                       and start.distanceTo(optional.get()) < nearestEntityDist:
      nearestEntityHit  = entity
      nearestEntityDist = start.distanceTo(optional.get())

  if nearestEntityHit:
    // Reset hurt-immunity so pistol cooldown (8 tk) isn't swallowed by vanilla
    // 10-tk invuln window when retargeting the same entity.
    nearestEntityHit.invulnerableTime = 0
    nearestEntityHit.hurt(WildWestDamageTypes.gunshot(player), 5.0F)

  stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND)
  player.getCooldowns().addCooldown(stack, 8)
  level.playSound(null, player, ModSounds.PISTOL_FIRE, SoundSource.PLAYERS, 1.0F, 1.0F)

  hitPoint = nearestEntityHit ? nearestEntityHit.position() : (blockHit.type == MISS ? end : blockHit.getLocation())
  PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new S2CTracerPacket(start, hitPoint))

  return SUCCESS
```

Mob-side helper:

```
PistolItem.fireFromMob(shooter, target):
  // Same hitscan, but uses shooter's eye + look direction toward target.
  // Adds slight inaccuracy (random Gaussian) so mobs don't pixel-perfect-snipe.
  // Tracer packet: when shooter is NOT a ServerPlayer, use
  //   PacketDistributor.sendToPlayersTrackingEntity(shooter, packet)
  // instead of `...AndSelf` (which assumes shooter IS a ServerPlayer).
  // Phase-1 player path uses AndSelf; phase-2 mob path branches on type.
```

### Rifle firing (bolt-action projectile)

```
RifleItem.use(Level, Player, Hand):
  if level.isClientSide → return PASS
  if player.getCooldowns().isOnCooldown(stack) → return FAIL

  bullet = new BulletEntity(level, player)
  bullet.setPos(player.eyePosition())
  bullet.shoot(player.lookX, player.lookY, player.lookZ, 6.0F, 0.0F)  // velocity, inaccuracy
  bullet.setBaseDamage(9.0)
  bullet.setMaxLifeTicks(12)
  level.addFreshEntity(bullet)

  stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND)
  player.getCooldowns().addCooldown(stack, 40)
  level.playSound(null, player, ModSounds.RIFLE_FIRE, SoundSource.PLAYERS, 1.0F, 1.0F)
  // bolt_cycle sound emitted by inventoryTick (see below) — not scheduled here
  return SUCCESS
```

The delayed `bolt_cycle` sound (~10 ticks after firing, mid-cooldown) is emitted from `RifleItem.inventoryTick`, not a side-channel scheduler. Stateless, leak-free, runs only while the rifle is in a player's inventory:

```java
@Override
public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
    if (level.isClientSide || !(entity instanceof Player player)) return;
    // ItemCooldowns: cooldown started at 40, drains by 1/tick. Tick 30 = ~10 ticks after fire.
    // Verify exact accessor at implementation; equivalents exist via getCooldownPercent + total.
    int remaining = remainingCooldownTicks(player, stack.getItem());
    if (remaining == 30) {
        level.playSound(null, player.blockPosition(), ModSounds.BOLT_CYCLE.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
```

`remainingCooldownTicks` is a helper that reads from `Player.getCooldowns()`. The exact API for "remaining ticks" varies by NeoForge minor version — `ItemCooldowns.getCooldownInstance(item)` exposes `endTime - level.getGameTime()` in this branch; a wrapper insulates the call site.

`BulletEntity extends AbstractArrow`:
- `pickup = NO_PICKUP`.
- `getDefaultHitGroundSoundEvent()` → `null` or quiet thud.
- Override `onHitEntity(EntityHitResult)`:
  - Reset `target.invulnerableTime = 0` before applying damage so rapid back-to-back shots (relevant once phase 2 ships multiple bandits firing at once) don't drop hits inside vanilla's 10-tk hurt-immunity window.
  - Apply `getBaseDamage()` via `wildwest:gunshot`. Then `discard()`.
- Override `onHitBlock(BlockHitResult)`: spawn `SMOKE` particle puff, `discard()`.
- Override `tick()`: every tick, spawn `CRIT` particle at current position. Despawn at age 12.

### Bolt-cycle item-model state

Client-side, in `ClientSetup`:

```
ItemProperties.register(ModItems.RIFLE.get(), ResourceLocation.fromNamespaceAndPath("wildwest", "bolt_state"),
    (stack, level, holder, seed) -> {
      if (holder instanceof Player player) {
        var cd = player.getCooldowns().getCooldownPercent(stack.getItem(), 0F);
        return cd;  // 0.0 = ready, 1.0 = just fired
      }
      return 0F;
    });
```

Item model JSON (`models/item/rifle.json`) has predicate overrides:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": { "layer0": "wildwest:item/rifle" },
  "overrides": [
    { "predicate": { "wildwest:bolt_state": 0.3 }, "model": "wildwest:item/rifle_bolt_closing" },
    { "predicate": { "wildwest:bolt_state": 0.7 }, "model": "wildwest:item/rifle_bolt_open" }
  ]
}
```

Predicate semantics: vanilla iterates overrides in declaration order and the **last** override whose `predicate.value ≤ current predicate value` wins. With ascending order:
- `bolt_state = 0.0` (ready) → neither override matches → base model (`rifle`) shown.
- `bolt_state = 0.5` (mid-cooldown) → `0.3` matches, `0.7` does not → `rifle_bolt_closing`.
- `bolt_state = 1.0` (just fired) → `0.3` matches, `0.7` matches → `rifle_bolt_open` (last match wins).

Walking forward in time after firing: cd starts at 1.0 and drains to 0.0, so the model lifecycle is `bolt_open` → `bolt_closing` → `ready`. Each variant is one of the three bbmodel states.

## Damage type definition

`data/wildwest/damage_type/gunshot.json` (datagen):

```json
{
  "message_id": "wildwest.gunshot",
  "scaling": "when_caused_by_living_non_player",
  "exhaustion": 0.1,
  "effects": "hurt",
  "death_message_type": "default"
}
```

Helper class:

```java
public final class WildWestDamageTypes {
    public static final ResourceKey<DamageType> GUNSHOT =
        ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("wildwest", "gunshot"));

    public static DamageSource gunshot(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(GUNSHOT),
            attacker);
    }
}
```

## Network packet

`S2CTracerPacket(Vec3 start, Vec3 end)` implementing `CustomPacketPayload`:
- Fields: `start`, `end` (encoded as 3 floats each).
- Handler (client): spawn ~6 evenly-spaced `CRIT` particles along the line segment, plus a `SMOKE` particle at `start`.
- Registration: in `WildWestMod` constructor via `RegisterPayloadHandlersEvent` with `PacketDistributor.sendToPlayersTrackingEntityAndSelf`.

## Testing

### Unit tests (JUnit, `wildwest/src/test/java/com/tweeks/wildwest/`)

- **`PistolHitscanTest`**
  - Mocked `Level` returns a `BlockHitResult` at distance D with one `LivingEntity` at distance D'.
  - Assert: D' < D → entity damaged, block ignored.
  - Assert: D < D' → entity NOT damaged.
  - Assert: zero entities → no damage, no exception.
- **`RifleCooldownTest`**
  - Fire rifle → assert `Cooldowns.isOnCooldown(rifle)` true.
  - Advance ticks by 39 → still on cooldown.
  - Advance one more tick (total 40) → off cooldown.
- **`BulletEntityTest`**
  - Spawn `BulletEntity`, set damage 9, simulate `onHitEntity` against a mocked `LivingEntity` → entity took 9 dmg.
  - Tick `BulletEntity` 12 times with no collision → `discard()` called.

### Manual in-game test plan

1. **Recipes:** `/give @s wildwest:pistol` works after crafting from iron + plank in the displayed pattern. Same for rifle.
2. **Pistol close range:** stand 5 blk from a zombie, right-click pistol → zombie takes 5 dmg, tracer line visible, smoke puff at muzzle.
3. **Pistol max range:** stand exactly 16 blk from a target dummy → still hits. Step back to 17 blk → no hit, no damage.
4. **Pistol cooldown:** spam right-click → fires roughly every 8 ticks (~2.5/sec); HUD cooldown overlay visible.
5. **Pistol durability:** fire 300 times → item breaks.
6. **Rifle hit:** right-click on target ~30 blk away → bullet entity flies, ~9 dmg on impact.
7. **Rifle bolt-cycle:** fire rifle in third-person view → see bolt-open then bolt-closing model swap during 40-tick cooldown, then back to ready.
8. **Rifle durability:** fire 400 times → breaks.
9. **Death message:** `/damage @s 100 wildwest:gunshot` → chat shows `"You were shot"` (or appropriate variant). Kill another entity → `"<name> was shot by <killer>"`.
10. **Multi-mod load:** `./gradlew :wildwest:runClient` launches dev client; in-game Mods list shows `securitycore`, `securityguard`, `thief`, `wildwest` all loaded.
11. **Build:** `./gradlew :wildwest:build` and `./gradlew build` (root) both succeed.
12. **Datagen:** `./gradlew :wildwest:runData` produces expected JSON in `src/generated/`.

## Open questions / risks

1. **Hitscan sync.** Server raycast can disagree with what the player saw client-side (lag, view interpolation). Acceptable for v1 — vanilla bow has the same class of issue. Revisit only if testing shows obvious feel-bad misses.
2. **bbmodel display transforms.** Each item bbmodel needs five+ camera transforms (first-person main hand, first-person off hand, third-person main, third-person off, ground, GUI, fixed). This is normal Blockbench export work but is fiddly. Plan to validate transforms during phase-1 implementation, not defer to phase 2.
3. **Cooldown predicate accuracy.** `Cooldowns.getCooldownPercent` returns 1.0 right after firing and 0.0 when ready — verify the predicate threshold direction matches the expected bolt-state lerp (0.0 = ready, 1.0 = just fired). If reversed, flip the override threshold values.
4. **`AbstractArrow` rendering.** Vanilla `ArrowRenderer` won't pick up our bbmodel automatically — we register a `BulletRenderer` (bound via `EntityRenderers.register` in `ClientSetup`) that loads the bbmodel-exported JSON and draws it. `AbstractArrow.tick()` updates `yRot` and `xRot` from velocity each tick but the renderer must apply them explicitly (`poseStack.mulPose(Axis.YP.rotationDegrees(...))` and `Axis.XP.rotationDegrees(...)`) using interpolated values (`Mth.lerp(partialTick, yRotO, yRot)`) so the lead slug points along its trajectory instead of facing world-axis-aligned.
5. **Sound asset placeholder strategy.** Phase-1 ships placeholder `.ogg` files (sine bursts or short royalty-free clips) committed to the repo. They WILL be heard in dev. Design assumes user replaces them in a follow-up; not a phase-1 deliverable to source final audio.
6. **Block-pierce question.** A bullet hitting a 1-block-thick wall with a window slit should pass through the slit. Hitscan handles this naturally via `ClipContext`; projectile relies on `AbstractArrow` collision which uses block hitboxes. No special handling needed.
