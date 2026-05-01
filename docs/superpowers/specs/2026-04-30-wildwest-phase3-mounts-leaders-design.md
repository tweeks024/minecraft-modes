# Wild-West Mod Phase 3 — Horse Mounts + Leader-Follower AI — Design

**Date:** 2026-04-30
**Status:** Draft (pending user review of this doc)
**Series:** Phase 3 of 3 for the Wild-West mod. Phase 1 (`:wildwest` module + pistol + rifle) merged at `698f099`. Phase 2 (4 mobs + hand weapons + faction markers) merged at `c810655`.

## Goal

Layer two coordinated mechanics on top of the existing four mobs:

1. **Horse mounts**: sherrif and bandit-leader spawn riding a vanilla `Horse` (tamed + saddled). Player can shoot the horse to dismount the rider.
2. **Pack spawning + organic following**: each leader spawn brings 2 followers of the same faction (sherrif → 2 deputies; bandit-leader → 2 bandits). Plus, lone followers within 16 blk of a leader of their faction tag along organically. Followers adopt the leader's current target when idle.

Phase 3 adds **no new entity types**, **no new items**. Pure behavior layer on top of phase 2's mobs + vanilla `Horse`.

## Platform

Java 25 / NeoForge 26.1.2.30-beta / Minecraft 26.1.2. Same as phases 1 + 2.

## Phasing recap

| Phase | Scope | Status |
|---|---|---|
| 1 | `:wildwest` module + pistol + rifle | Merged @ `698f099` |
| 2 | 4 mobs + hand weapons + faction markers + basic AI | Merged @ `c810655` |
| **3 (this doc)** | Horse mounts + leader-follower AI | Draft |

## Scope

### In scope (phase 3)

**Horse mounts (vanilla `Horse`)**
- When a leader (sherrif or bandit-leader) spawns through any of our spawn paths, also spawn a vanilla `Horse` at the same position, set tamed + saddled, mount the leader on it via `leader.startRiding(horse, true)`.
- Visual coat differs by faction (vanilla horse variants):
  - Sherrif's horse: `Variant.CHESTNUT`, markings `WHITE_FIELD`.
  - Bandit-leader's horse: `Variant.BLACK`, markings `NONE`.
- Horse `MAX_HEALTH` set to 30 (slightly above default ~22) so the rider isn't trivially dismounted by a single arrow.
- Horse persists after rider death (vanilla `MobCategory.CREATURE` no-despawn-at-distance). Player can re-ride.
- If horse dies, vanilla auto-dismounts the rider, who continues ground combat.

**Pack spawning (group of 3 per leader)**
- Each leader spawn → also spawn 2 followers within a 4-blk radius around the leader's position.
  - Sherrif → 2 `DeputyEntity`.
  - Bandit-leader → 2 `BanditEntity`.
- Pack-spawned followers get `setPersistenceRequired(true)` so they don't despawn at distance — they belong to a known patrol.
- Pack-spawn fires only on **leader** spawn events; lone-follower spawns (existing biome-modifier path for bandits, existing village-tick path for deputies) are unchanged.

Sherrif pack-spawn integration: existing `LawmanVillageSpawner.spawn(...)` path is modified — after the sherrif spawns successfully, immediately spawn the horse + mount + 2 deputies.

Bandit-leader pack-spawn integration: a new `BanditLeaderPackSpawner` event handler subscribes to `FinalizeMobSpawnEvent` (NeoForge event fired at the end of every mob spawn pipeline including biome-modifier-driven spawns). When the event entity is a `BanditLeaderEntity` AND the spawn reason is NATURAL, run the same horse + 2 bandits pack-spawn.

**Organic-follow AI**
- New goal `FollowLeaderGoal extends Goal` installed on `WildWestMob.registerGoals()` at priority 4 (between attack goals and stroll). Internally gated on follower-only (`!this.isLeader()`):
  - On `canUse()`: scan a 16-blk AABB for any same-faction leader, set `this.followingLeader` to the nearest one. Returns true if found.
  - On `tick()`: if `followingLeader != null && followingLeader.isAlive()`, navigate to within 8 blk of it via `this.getNavigation().moveTo(leader, 1.0)`.
  - On `canContinueToUse()`: returns true while `followingLeader` is alive AND within 24 blk (the "lose interest" radius — slightly larger than acquisition so we don't thrash at the boundary).
- Pure-logic helper `FollowDecision.choose(myFaction, nearbyMobs, distancesTo) → Optional<WildWestMob>` extracted into its own class, unit-tested without booting Minecraft. Same pattern as phase-1's `Hitscan` and phase-2's `WeaponMode`.

**Coordinated targeting**
- New target-selector goal `LeaderTargetCopyGoal extends TargetGoal` installed on `WildWestMob.registerGoals()` at priority 0 (highest), follower-only:
  - On `canUse()`: returns true iff `this.getTarget() == null && followingLeader != null && followingLeader.getTarget() != null && followingLeader.getTarget().isAlive()`.
  - On `start()`: `this.setTarget(followingLeader.getTarget())`.
- Idle followers immediately adopt the leader's target. Already-engaged followers keep their own target. Produces the "patrol focuses on the same threat" effect.

**Tagging leaders for the AI**

Add a new abstract method `WildWestMob.isLeader()` returning `boolean`. `SherrifEntity` and `BanditLeaderEntity` return `true`; `DeputyEntity` and `BanditEntity` return `false`. Used by `FollowLeaderGoal` and `LeaderTargetCopyGoal` to gate themselves on follower-only behavior.

(Spec note: this is a simple symmetrical addition — no behavior change to leaders themselves; leaders simply skip the follow / target-copy goals.)

### Out of scope (phase 3)

- New custom-horse entity types or custom textures for horses (vanilla variants only).
- Pack scaling — always exactly 2 followers per leader.
- Player-side improvements to taming captured horses (vanilla taming UI applies).
- Loot-table additions for horses (vanilla `Horse` loot stays vanilla = none from natural-spawn horses).
- Leader respawn — if a sherrif dies, the village's deputies don't get a replacement leader assigned.
- Cross-leader awareness — two sherrifs in the same area don't coordinate; each has its own follower group.
- Patrol pathing — leaders don't actively walk to specific patrol points; they use vanilla wandering goals.
- Horse-side custom AI — horses are pure vanilla.

## Design decisions

| # | Decision | Chosen | Rejected |
|---|---|---|---|
| Q1 | Horse mechanic | Vanilla `Horse`, spawn-mounted | Custom subclass per faction; cosmetic-only mount |
| Q2 | Leader-follower behavior | Pack-spawn (group of 3) + organic follow | Pack-spawn only; organic-follow only |

## Architecture

### File-level changes

```
wildwest/src/main/java/com/tweeks/wildwest/
├── entity/ai/
│   ├── FollowLeaderGoal.java               NEW
│   ├── LeaderTargetCopyGoal.java           NEW
│   └── FollowDecision.java                 NEW (pure-logic helper for tests)
├── spawning/
│   ├── LawmanVillageSpawner.java           MOD (after sherrif spawn → horse + 2 deputies)
│   └── BanditLeaderPackSpawner.java        NEW (FinalizeMobSpawnEvent listener)
├── entity/
│   ├── WildWestMob.java                    MOD (followingLeader field, isLeader() abstract method, install 2 new goals follower-only)
│   ├── SherrifEntity.java                  MOD (isLeader() returns true)
│   ├── BanditLeaderEntity.java             MOD (isLeader() returns true)
│   ├── DeputyEntity.java                   MOD (isLeader() returns false)
│   └── BanditEntity.java                   MOD (isLeader() returns false)
└── WildWestMod.java                        MOD (no new explicit registration — BanditLeaderPackSpawner uses @EventBusSubscriber)

wildwest/src/test/java/com/tweeks/wildwest/entity/ai/
└── FollowDecisionTest.java                 NEW
```

### Behavior diagrams

**Spawn flow — sherrif:**

```
LawmanVillageSpawner.tick (every 6000 ticks, OVERWORLD only):
  for each player:
    village = getStructureWithPieceAt(player.pos, VILLAGE)
    if (village in range && roll < 5%):
      spawn Sherrif at random village pos
      ↓
      spawnLeaderEntourage(sherrif, ModEntities.DEPUTY, Variant.CHESTNUT, MARKINGS_WHITE_FIELD)
        ├── horse = vanilla Horse(level)
        │     setVariant(CHESTNUT, WHITE_FIELD)
        │     setTamed(true)
        │     equipSaddle()
        │     setMaxHealth(30)
        │     addFreshEntity()
        ├── sherrif.startRiding(horse, true)
        └── for i in 0..2:
              deputy = DeputyEntity::new
              deputy.setPos(sherrif.pos + random offset within 4 blk)
              deputy.setPersistenceRequired(true)
              addFreshEntity(deputy)
```

**Spawn flow — bandit-leader:**

```
NeoForge biome-modifier-driven spawn:
  vanilla mob spawner picks a tile, finds BANDIT_LEADER eligible, spawns it
    ↓
  FinalizeMobSpawnEvent fires
    ↓
  BanditLeaderPackSpawner.onSpawn:
    if (event.entity instanceof BanditLeaderEntity && event.reason == NATURAL):
      spawnLeaderEntourage(leader, ModEntities.BANDIT, Variant.BLACK, MARKINGS_NONE)
      (same shape as sherrif's entourage spawn)
```

**Follower runtime AI (Deputy / Bandit):**

```
Per-tick (priority 0, target selector):
  if (this.target == null && followingLeader != null && leader.target != null && leader.target.alive):
    this.setTarget(leader.target)

Per-tick (priority 4, goal selector):
  canUse():
    leader = nearestSameFactionLeader(within 16 blk)
    if leader: this.followingLeader = leader; return true
    return false
  tick():
    if followingLeader.distanceTo(this) > 8:
      navigation.moveTo(followingLeader, 1.0)
  canContinueToUse():
    return followingLeader.alive && distanceTo(followingLeader) <= 24
  stop():
    this.followingLeader = null
```

### Pure-logic helper

```java
public final class FollowDecision {
    public static Optional<WildWestMob> choose(
            boolean myFactionIsLawman,
            List<WildWestMob> nearbyMobsByDistance,  // sorted nearest-first; excludes self
            WildWestMob currentLeader) {
        // If we already follow someone alive, keep them (avoid leader-flip thrash).
        if (currentLeader != null && currentLeader.isAlive() && !currentLeader.isRemoved()) {
            return Optional.of(currentLeader);
        }
        // Otherwise pick nearest same-faction leader.
        for (WildWestMob m : nearbyMobsByDistance) {
            if (!m.isLeader()) continue;
            if (m.isLawman() != myFactionIsLawman) continue;
            return Optional.of(m);
        }
        return Optional.empty();
    }
}
```

The helper is the unit-testable seam. The actual `FollowLeaderGoal` builds the `nearbyMobsByDistance` list from `level.getEntitiesOfClass(...)` and calls into `choose(...)`.

## Spawn-mount helper (shared between sherrif + bandit-leader paths)

A static helper avoids duplication between `LawmanVillageSpawner` and `BanditLeaderPackSpawner`:

```java
public final class LeaderEntourageSpawner {
    private LeaderEntourageSpawner() {}

    public static void spawnEntourage(
            ServerLevel level,
            WildWestMob leader,
            EntityType<? extends WildWestMob> followerType,
            Variant horseVariant,
            Holder<Markings> horseMarkings) {
        BlockPos pos = leader.blockPosition();
        // Horse + mount.
        Horse horse = new Horse(EntityType.HORSE, level);
        horse.setPos(leader.position());
        horse.setVariant(horseVariant);
        horse.setMarkings(horseMarkings);
        horse.setTamed(true);
        horse.equipSaddle(SoundSource.NEUTRAL);  // or modern equivalent
        horse.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30.0);
        horse.setHealth(30.0F);
        level.addFreshEntity(horse);
        leader.startRiding(horse, true);

        // 2 followers nearby.
        var random = level.getRandom();
        for (int i = 0; i < 2; i++) {
            int dx = random.nextInt(9) - 4;
            int dz = random.nextInt(9) - 4;
            BlockPos followerPos = pos.offset(dx, 0, dz);
            WildWestMob follower = (WildWestMob) followerType.create(level, EntitySpawnReason.NATURAL);
            if (follower == null) continue;
            follower.setPos(followerPos.getX() + 0.5, followerPos.getY(), followerPos.getZ() + 0.5);
            follower.setPersistenceRequired();  // method may be no-arg or take boolean — adapt
            level.addFreshEntity(follower);
        }
    }
}
```

(`Markings` is `net.minecraft.world.entity.animal.horse.Markings`; modern API may have different setter names — implementer adapts.)

The exact `equipSaddle` / `setSaddle` API in MC 26.1.2 may differ slightly. The spec assumes the existence of *some* method to give a horse a saddle so it can be ridden; implementer adapts to whichever exists.

## Testing

### Unit tests (`wildwest/src/test/java/com/tweeks/wildwest/entity/ai/`)

- `FollowDecisionTest`:
  - Empty nearby list, no current leader → empty Optional.
  - Nearby list contains only follower-class same-faction mobs → empty Optional (only leaders count).
  - Nearby list contains one leader of same faction → that leader.
  - Nearby list contains one leader of *opposite* faction → empty Optional (cross-faction excluded).
  - Nearby list contains 2 leaders sorted nearest-first, same faction → nearest one.
  - Already following an alive leader → returns the same leader (no flip).
  - Already following a dead leader → picks new nearest same-faction leader.

(All implemented as pure tests; no Minecraft mocking. Faction is passed as `boolean myFactionIsLawman` and `WildWestMob.isLeader()` / `isLawman()` are stubbed via test doubles or fakes implementing those methods.)

### Manual smoke test

1. **Sherrif egg → mounted patrol.** Creative spawn-egg a sherrif; expect: chestnut-coat horse appears, sherrif sits on it, 2 deputies spawn within 4 blk. Persistent — wait 5 minutes, walk away, come back: still there.
2. **Bandit-leader egg → mounted gang.** Same with bandit-leader: black horse + 2 bandits.
3. **Shoot the horse.** Hit sherrif's horse with `:wildwest:rifle` until it dies (~3-4 hits at 9 dmg = 30 HP). Sherrif drops to ground, fights normally. Horse despawns / dies (vanilla behavior).
4. **Shoot the rider.** Kill the sherrif while mounted; horse persists as a wild tamed horse — player can right-click to ride.
5. **Coordinated targeting.** Spawn the patrol; player attacks the sherrif. Sherrif retaliates; both deputies (idle-no-target) adopt the player as target via `LeaderTargetCopyGoal` and join the attack.
6. **Organic follow.** Spawn a sherrif (already with 2 pack-spawn deputies). Spawn a third lone deputy 12 blk away. Within ~5 seconds the lone deputy paths over and joins the patrol.
7. **Lose-interest range.** Lure a follower deputy 30 blk from its sherrif (e.g., player runs away pulling aggro). Confirm the deputy stops following at 24 blk and reverts to its individual AI; if the player then re-engages, normal target-copy resumes when the deputy comes back into range.
8. **Leader death.** Kill the sherrif. Its 2 pack-spawn deputies plus any organic follower lose the `followingLeader` reference (FollowLeaderGoal's `canContinueToUse` returns false), continue fighting on individual AI.
9. **Plains-village natural spawn.** Generate a fresh world, find a plains village. Wait 30+ in-game minutes (or `/time` advance). Eventually a sherrif + 2 deputies + chestnut horse pack appears in the village.
10. **Bandit-leader natural spawn.** In plains/savanna at night, a bandit-leader spawn (rare, weight 1) brings the full mounted gang.
11. **Build verification.** `./gradlew :wildwest:build` and `:wildwest:runServerData :wildwest:runClientData` all succeed.

## Open questions / risks

1. **`setSaddle` / `equipSaddle` API.** Vanilla `Horse.equipSaddle(SoundSource source)` was the form in 1.21.x; in 26.1.2 it may have changed signature (e.g., `equipSaddle(ItemStack saddle, SoundSource source)` to track the saddle item). Implementer reads the actual signature when wiring `LeaderEntourageSpawner.spawnEntourage`. End user behavior — horse is rideable — does not change.
2. **`setPersistenceRequired` vs `setPersistent`.** Modern NeoForge uses `setPersistenceRequired()` (no-arg). If the actual method is `setPersistent(boolean)`, adapt. Goal: pack-spawned followers don't despawn at distance.
3. **`FinalizeMobSpawnEvent` payload.** This event fires for all natural mob spawns including ours. Make sure to filter by entity class (`event.getEntity() instanceof BanditLeaderEntity`) AND spawn reason (`event.getSpawnType() == EntitySpawnReason.NATURAL`) to avoid double-spawning packs when the leader spawns from a spawn egg or from `/summon`.
4. **Leader-flip thrash.** If two sherrifs are 8 blk apart, a deputy between them might flip-flop. Mitigated by `FollowDecision.choose` returning the same leader if already-following-alive — only re-evaluate when current leader is dead/removed.
5. **Mount-coupled spawn ordering.** `addFreshEntity(horse)` must complete before `leader.startRiding(horse, true)` — the horse must be in the level. If the leader was already added to the level *before* the horse, the riding relationship still works (vanilla allows it), but we add horse first to be safe.
6. **Horse pathing in villages.** A mounted sherrif may try to path through 1-block doorways. The `Horse` is 1.6 blk tall; vanilla pathing knows this. If the rider gets stuck, vanilla mount-AI should handle dismount eventually. Acceptable for v1 — revisit only if testing shows widespread stuck states.
7. **`Markings` API.** `net.minecraft.world.entity.animal.horse.Markings` is an enum in vanilla. Setting it via `horse.setMarkings(Markings)` may not be the modern API (could be a `Holder<Markings>` getter or set via component data). Adapt at implementation. End user behavior — visible coat pattern — degrades gracefully if markings can't be explicitly set (horse uses random vanilla pattern instead).
8. **Bandit-leader pack-spawn double-fire.** If `FinalizeMobSpawnEvent` fires for a leader spawned via *our own* pack-spawn helper (recursive spawn), we'd get exponential growth. Guard: in `BanditLeaderPackSpawner`, only act on `EntitySpawnReason.NATURAL`. Pack-spawn calls use `EntitySpawnReason.NATURAL` for the leader only? — actually the *leader* is spawned by the biome-modifier (NATURAL); the *followers* are spawned by us with NATURAL. The followers are not BanditLeaderEntity, so the class filter prevents recursion. Belt-and-suspenders: also pre-flight check that the leader is not already mounted (skip pack-spawn for re-spawns / re-loads).
