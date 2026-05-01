# Wild-West Phase 3 — Horse Mounts + Leader-Follower AI — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Layer two coordinated mechanics on top of phase 2's mobs: leaders (sherrif, bandit-leader) spawn riding a vanilla `Horse` (tamed + saddled) with 2 same-faction followers nearby, and lone followers within 16 blk of a same-faction leader follow organically and adopt the leader's target.

**Architecture:** No new entity types or items. Adds an abstract `WildWestMob.isLeader()` method (returns true for sherrif/bandit-leader, false for deputy/bandit). Two new AI goals (`FollowLeaderGoal`, `LeaderTargetCopyGoal`) live in `entity/ai/`, gated follower-only via `isLeader()`. Pack-spawn logic lives in a `LeaderEntourageSpawner` helper called from the existing `LawmanVillageSpawner` (sherrif path) and a new `BanditLeaderPackSpawner` listening to `FinalizeMobSpawnEvent` (bandit-leader path). Pure-logic helper `FollowDecision.choose(...)` is unit-tested without booting Minecraft.

**Tech Stack:** Java 25, NeoForge 26.1.2.30-beta, Minecraft 26.1.2, JUnit 5.

**Spec:** [docs/superpowers/specs/2026-04-30-wildwest-phase3-mounts-leaders-design.md](../specs/2026-04-30-wildwest-phase3-mounts-leaders-design.md)

**API lessons carried forward (phase 1 + 2):**
- `Identifier` not `ResourceLocation`.
- `LivingEntity.hurtServer((ServerLevel) level, source, dmg)` — `hurt` is deprecated.
- `MobEffects.SLOWNESS` not `MOVEMENT_SLOWDOWN`.
- `Item.inventoryTick(ItemStack, ServerLevel, Entity, EquipmentSlot)`.
- `level.isClientSide()` is a method.
- `WildWestMob` save/load uses `ValueOutput`/`ValueInput`, not `CompoundTag` directly.
- `MobSpawnSettings.SpawnerData` no longer carries weight; wrap in `Weighted<>`.
- Static counters in `LevelTickEvent.Post` need dimension gating to avoid multi-dim tick multiplication.
- Inside entity classes, `Registration` is name-shadowed by an inherited symbol — use FQN `com.tweeks.wildwest.Registration.X`.

---

## Task 1: FollowDecision pure helper + unit tests (TDD)

The pure-logic seam: given my faction, a list of nearby mobs sorted by distance, and a current leader (possibly null/dead), pick the leader I should follow. Tested without booting Minecraft.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/FollowDecision.java`
- Create: `wildwest/src/test/java/com/tweeks/wildwest/entity/ai/FollowDecisionTest.java`

The decision function takes simple inputs that don't require Minecraft mocking. To avoid mocking `WildWestMob` (an abstract class extending `PathfinderMob`), the helper accepts a small interface `FollowDecision.Candidate` representing the bits of mob state we care about. Tests use plain Java objects implementing `Candidate`.

- [ ] **Step 1: Write failing tests**

`wildwest/src/test/java/com/tweeks/wildwest/entity/ai/FollowDecisionTest.java`:

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.ai.FollowDecision.Candidate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FollowDecisionTest {

    /** Test double for FollowDecision.Candidate. */
    static class TestCandidate implements Candidate {
        private final boolean leader;
        private final boolean lawman;
        private final boolean alive;
        private final String label;

        TestCandidate(String label, boolean leader, boolean lawman, boolean alive) {
            this.label = label;
            this.leader = leader;
            this.lawman = lawman;
            this.alive = alive;
        }

        @Override public boolean isLeader() { return leader; }
        @Override public boolean isLawman() { return lawman; }
        @Override public boolean isAlive() { return alive; }
        @Override public String toString() { return label; }
    }

    @Test
    void emptyList_noCurrentLeader_returnsEmpty() {
        Optional<Candidate> result = FollowDecision.choose(true, List.of(), null);
        assertTrue(result.isEmpty());
    }

    @Test
    void onlyFollowers_returnsEmpty() {
        Candidate dep = new TestCandidate("deputy", false, true, true);
        Optional<Candidate> result = FollowDecision.choose(true, List.of(dep), null);
        assertTrue(result.isEmpty());
    }

    @Test
    void oneSameFactionLeader_picksThatLeader() {
        Candidate sherrif = new TestCandidate("sherrif", true, true, true);
        Optional<Candidate> result = FollowDecision.choose(true, List.of(sherrif), null);
        assertEquals("sherrif", result.orElseThrow().toString());
    }

    @Test
    void crossFactionLeader_returnsEmpty() {
        Candidate banditLeader = new TestCandidate("bandit_leader", true, false, true);
        Optional<Candidate> result = FollowDecision.choose(true, List.of(banditLeader), null);
        assertTrue(result.isEmpty());
    }

    @Test
    void multipleSameFactionLeaders_picksFirstInList() {
        // The list is pre-sorted by distance, so first = nearest.
        Candidate near = new TestCandidate("near", true, true, true);
        Candidate far  = new TestCandidate("far",  true, true, true);
        Optional<Candidate> result = FollowDecision.choose(true, List.of(near, far), null);
        assertEquals("near", result.orElseThrow().toString());
    }

    @Test
    void alreadyFollowingAliveLeader_keepsCurrentLeader() {
        Candidate current = new TestCandidate("current", true, true, true);
        Candidate other = new TestCandidate("other", true, true, true);
        Optional<Candidate> result = FollowDecision.choose(true, List.of(other), current);
        assertEquals("current", result.orElseThrow().toString());
    }

    @Test
    void currentLeaderDead_picksNewNearest() {
        Candidate dead = new TestCandidate("dead", true, true, false);
        Candidate alive = new TestCandidate("alive", true, true, true);
        Optional<Candidate> result = FollowDecision.choose(true, List.of(alive), dead);
        assertEquals("alive", result.orElseThrow().toString());
    }
}
```

- [ ] **Step 2: Run tests, verify they fail**

```bash
./gradlew :wildwest:test
```

Expected: compile error — `FollowDecision` and `Candidate` don't exist yet.

- [ ] **Step 3: Implement FollowDecision**

`wildwest/src/main/java/com/tweeks/wildwest/entity/ai/FollowDecision.java`:

```java
package com.tweeks.wildwest.entity.ai;

import java.util.List;
import java.util.Optional;

/**
 * Pure decision logic for "which leader should this mob follow?". Takes a
 * faction flag and a list of nearby mob candidates pre-sorted by distance
 * (nearest first); returns the chosen leader if any. Tested without booting
 * Minecraft via plain test doubles.
 */
public final class FollowDecision {
    private FollowDecision() {}

    /**
     * Minimal interface representing the bits of {@code WildWestMob} state
     * the decision logic needs. Production code passes {@code WildWestMob}
     * instances; tests pass plain Java objects.
     */
    public interface Candidate {
        boolean isLeader();
        boolean isLawman();
        boolean isAlive();
    }

    /**
     * @param myFactionIsLawman           true if the deciding mob is a Lawman
     * @param nearbyCandidates            list of nearby mobs, sorted nearest-first; excludes the deciding mob
     * @param currentLeader               the mob currently being followed, or null
     * @return                            the chosen leader, or empty if none available
     */
    public static <C extends Candidate> Optional<C> choose(
            boolean myFactionIsLawman,
            List<C> nearbyCandidates,
            C currentLeader) {
        // Stick with the current leader if alive — avoids leader-flip thrash
        // when two leaders are nearly equidistant.
        if (currentLeader != null && currentLeader.isAlive()) {
            return Optional.of(currentLeader);
        }
        // Otherwise pick the nearest same-faction leader.
        for (C c : nearbyCandidates) {
            if (!c.isLeader()) continue;
            if (c.isLawman() != myFactionIsLawman) continue;
            return Optional.of(c);
        }
        return Optional.empty();
    }
}
```

- [ ] **Step 4: Run tests, verify they pass**

```bash
./gradlew :wildwest:test
```

Expected: all 7 new tests pass (plus existing HitscanTest 5/5, WeaponModeTest 6/6, FactionPredicateTest 3/3).

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ai/FollowDecision.java wildwest/src/test/java/com/tweeks/wildwest/entity/ai/FollowDecisionTest.java
git commit -m "feat(wildwest): FollowDecision pure helper + 7 unit tests

Decides which same-faction leader a follower should track, given a
distance-sorted nearby-mob list and the current leader. Sticks with
current-alive-leader to avoid flip-thrash; otherwise picks nearest
same-faction leader. Pure logic, tested without booting Minecraft."
```

---

## Task 2: Add isLeader() abstract method + override in 4 mob classes

`WildWestMob` already has `usesRifle()` and `isLawman()` abstract methods. Add `isLeader()` alongside.

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/WildWestMob.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/SherrifEntity.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/BanditLeaderEntity.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/DeputyEntity.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/BanditEntity.java`

- [ ] **Step 1: Add abstract method to WildWestMob**

In `WildWestMob.java`, find the existing abstract methods:

```java
    public abstract boolean usesRifle();
    public abstract boolean isLawman();
```

Add a third one:

```java
    /** True if this mob is a leader (sherrif, bandit-leader). False for footsoldiers. */
    public abstract boolean isLeader();
```

- [ ] **Step 2: Override in SherrifEntity**

In `SherrifEntity.java`, after the existing `isLawman()` override, add:

```java
    @Override
    public boolean isLeader() { return true; }
```

- [ ] **Step 3: Override in BanditLeaderEntity**

In `BanditLeaderEntity.java`, after the existing `isLawman()` override:

```java
    @Override
    public boolean isLeader() { return true; }
```

- [ ] **Step 4: Override in DeputyEntity**

In `DeputyEntity.java`:

```java
    @Override
    public boolean isLeader() { return false; }
```

- [ ] **Step 5: Override in BanditEntity**

In `BanditEntity.java`:

```java
    @Override
    public boolean isLeader() { return false; }
```

- [ ] **Step 6: Build to confirm all four overrides compile**

```bash
./gradlew :wildwest:build
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/
git commit -m "feat(wildwest): add WildWestMob.isLeader() abstract + 4 overrides

Sherrif/BanditLeader return true; Deputy/Bandit return false. Used by
phase-3 follower-only AI goals."
```

---

## Task 3: FollowLeaderGoal + LeaderTargetCopyGoal + install in WildWestMob

Both goals are follower-only (gated on `!isLeader()`). They share a `followingLeader` reference field on the mob.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/FollowLeaderGoal.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/LeaderTargetCopyGoal.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/WildWestMob.java` (add `followingLeader` field + getter/setter, install both goals)

- [ ] **Step 1: Add followingLeader field + accessors to WildWestMob**

In `WildWestMob.java`, after the `tickCounter` field:

```java
    /** The leader this follower is currently tracking, or null. Null for leaders themselves. */
    private WildWestMob followingLeader = null;

    public WildWestMob getFollowingLeader() {
        return this.followingLeader;
    }

    public void setFollowingLeader(WildWestMob leader) {
        this.followingLeader = leader;
    }
```

(Public getter/setter is acceptable here — both AI goals read and mutate this field; encapsulation behind a method makes mocking easier in any future test.)

- [ ] **Step 2: Create FollowLeaderGoal**

`wildwest/src/main/java/com/tweeks/wildwest/entity/ai/FollowLeaderGoal.java`:

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.WildWestMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Followers (Deputy/Bandit) navigate to within 8 blk of the nearest same-faction
 * leader within 16 blk. Loses interest at 24 blk. Sticks with current leader if
 * still alive (no flip-thrash).
 */
public class FollowLeaderGoal extends Goal {

    private static final double ACQUIRE_RANGE = 16.0;
    private static final double LOSE_INTEREST_RANGE = 24.0;
    private static final double FOLLOW_DISTANCE = 8.0;
    private static final double SPEED = 1.0;

    private final WildWestMob self;

    public FollowLeaderGoal(WildWestMob self) {
        this.self = self;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (self.isLeader()) return false;

        // Wrap WildWestMobs as FollowDecision.Candidate (they already have
        // matching method signatures, but we need to satisfy the type bound).
        WildWestMob current = self.getFollowingLeader();
        AABB scanArea = self.getBoundingBox().inflate(ACQUIRE_RANGE);
        List<WildWestMob> nearby = new ArrayList<>(
            self.level().getEntitiesOfClass(WildWestMob.class, scanArea, m -> m != self));
        nearby.sort(Comparator.comparingDouble(self::distanceTo));

        var chosen = FollowDecision.choose(self.isLawman(), nearby, current);
        if (chosen.isEmpty()) {
            self.setFollowingLeader(null);
            return false;
        }
        self.setFollowingLeader((WildWestMob) chosen.get());
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        WildWestMob leader = self.getFollowingLeader();
        if (leader == null) return false;
        if (!leader.isAlive()) return false;
        return self.distanceTo(leader) <= LOSE_INTEREST_RANGE;
    }

    @Override
    public void tick() {
        WildWestMob leader = self.getFollowingLeader();
        if (leader == null) return;
        if (self.distanceTo(leader) > FOLLOW_DISTANCE) {
            self.getNavigation().moveTo(leader, SPEED);
        }
    }

    @Override
    public void stop() {
        self.setFollowingLeader(null);
        self.getNavigation().stop();
    }
}
```

The `(WildWestMob) chosen.get()` cast is safe: we passed `List<WildWestMob>` into `choose(...)`, so the returned `Candidate` is a `WildWestMob`. The cast is needed because the helper's signature is `<C extends Candidate>` returning `Optional<C>`, and the JVM's runtime type erasure doesn't preserve the C bound at the call site.

- [ ] **Step 3: Create LeaderTargetCopyGoal**

`wildwest/src/main/java/com/tweeks/wildwest/entity/ai/LeaderTargetCopyGoal.java`:

```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.WildWestMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

/**
 * Idle followers adopt their leader's current target. Engaged followers
 * (already have their own target) ignore this. Highest priority in the
 * target selector so it fires before the faction-target goal.
 */
public class LeaderTargetCopyGoal extends TargetGoal {

    private final WildWestMob follower;

    public LeaderTargetCopyGoal(WildWestMob follower) {
        super(follower, false);  // mustSee = false
        this.follower = follower;
    }

    @Override
    public boolean canUse() {
        if (follower.isLeader()) return false;
        if (follower.getTarget() != null) return false;
        WildWestMob leader = follower.getFollowingLeader();
        if (leader == null) return false;
        LivingEntity leaderTarget = leader.getTarget();
        return leaderTarget != null && leaderTarget.isAlive();
    }

    @Override
    public void start() {
        WildWestMob leader = follower.getFollowingLeader();
        if (leader != null && leader.getTarget() != null) {
            follower.setTarget(leader.getTarget());
        }
        super.start();
    }
}
```

If `TargetGoal` constructor signature is `(Mob, boolean mustSee)` (most common), the above compiles. If it's `(Mob, Class<?>, int chance, boolean mustSee, boolean mustReach, Predicate)` or another variant, adapt — the pattern of "use TargetGoal as the base for a follower-target-copy goal" is what matters; the exact super-call signature is the implementation detail.

- [ ] **Step 4: Install both goals in WildWestMob.registerGoals()**

In `WildWestMob.java`, in `registerGoals()`:
- After `this.goalSelector.addGoal(2, new WildWestMeleeAttackGoal(this, 1.0, true));` add:
  ```java
        this.goalSelector.addGoal(4, new com.tweeks.wildwest.entity.ai.FollowLeaderGoal(this));
  ```
- At the very top of the target-selector additions (before `this.targetSelector.addGoal(1, new HurtByTargetGoal(this));`) add:
  ```java
        this.targetSelector.addGoal(0, new com.tweeks.wildwest.entity.ai.LeaderTargetCopyGoal(this));
  ```

(Goals' own `canUse()` checks `if (self.isLeader()) return false;` so leaders skip them. Installing them on all `WildWestMob` instances is intentionally simple — no per-subclass branching.)

- [ ] **Step 5: Build**

```bash
./gradlew :wildwest:build
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/
git commit -m "feat(wildwest): FollowLeaderGoal + LeaderTargetCopyGoal

Followers (priority 4 goal) navigate within 8 blk of nearest same-faction
leader within 16 blk; lose interest at 24 blk. Idle followers (priority 0
target goal) adopt leader's current target. Both gated follower-only via
isLeader() check; leaders skip them."
```

---

## Task 4: LeaderEntourageSpawner helper class

Static helper that does the horse-mount + 2-follower-spawn dance. Called from both leader-spawn integration paths.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/spawning/LeaderEntourageSpawner.java`

- [ ] **Step 1: Create LeaderEntourageSpawner**

`wildwest/src/main/java/com/tweeks/wildwest/spawning/LeaderEntourageSpawner.java`:

```java
package com.tweeks.wildwest.spawning;

import com.tweeks.wildwest.entity.WildWestMob;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Markings;
import net.minecraft.world.entity.animal.horse.Variant;

/**
 * Spawns a leader's mount (vanilla Horse, tamed + saddled) and 2 followers
 * within a 4-blk radius around the leader. Followers get
 * setPersistenceRequired so they don't despawn at distance.
 */
public final class LeaderEntourageSpawner {
    private LeaderEntourageSpawner() {}

    /** Number of follower mobs spawned alongside each leader. */
    public static final int FOLLOWER_COUNT = 2;
    /** Spawn radius for followers around the leader's position. */
    public static final int FOLLOWER_RADIUS = 4;
    /** Buffed horse HP so the rider isn't trivially dismounted by one rifle shot. */
    public static final double HORSE_MAX_HEALTH = 30.0;

    public static <T extends WildWestMob> void spawnEntourage(
            ServerLevel level,
            WildWestMob leader,
            EntityType<T> followerType,
            Variant horseVariant,
            Markings horseMarkings) {
        spawnHorseAndMount(level, leader, horseVariant, horseMarkings);
        spawnFollowers(level, leader, followerType);
    }

    private static void spawnHorseAndMount(
            ServerLevel level, WildWestMob leader, Variant variant, Markings markings) {
        Horse horse = new Horse(EntityType.HORSE, level);
        horse.setPos(leader.getX(), leader.getY(), leader.getZ());
        horse.setVariant(variant);
        horse.setMarkings(markings);
        horse.setTamed(true);
        // Modern signature varies — `equipSaddle(SoundSource)` is the 1.21.x form.
        // Adapt if MC 26.1.2 changed it. Goal: horse is rideable.
        horse.equipSaddle(SoundSource.NEUTRAL);
        horse.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HORSE_MAX_HEALTH);
        horse.setHealth((float) HORSE_MAX_HEALTH);
        level.addFreshEntity(horse);
        leader.startRiding(horse, true);
    }

    private static <T extends WildWestMob> void spawnFollowers(
            ServerLevel level, WildWestMob leader, EntityType<T> followerType) {
        RandomSource random = level.getRandom();
        BlockPos leaderPos = leader.blockPosition();
        for (int i = 0; i < FOLLOWER_COUNT; i++) {
            int dx = random.nextInt(FOLLOWER_RADIUS * 2 + 1) - FOLLOWER_RADIUS;
            int dz = random.nextInt(FOLLOWER_RADIUS * 2 + 1) - FOLLOWER_RADIUS;
            BlockPos pos = leaderPos.offset(dx, 0, dz);
            T follower = followerType.create(level, EntitySpawnReason.NATURAL);
            if (follower == null) continue;
            follower.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            follower.setPersistenceRequired();
            level.addFreshEntity(follower);
        }
    }
}
```

API caveats (adapt at compile time if any of these don't match MC 26.1.2):
- `horse.setVariant(Variant)` and `horse.setMarkings(Markings)` may be component-data based now (check via `horse.setComponent(DataComponents.HORSE_VARIANT, ...)`). If the direct setters are missing, use the data-component path.
- `horse.equipSaddle(SoundSource)` — may have changed to `equipSaddle(ItemStack, SoundSource)` in 26.1.2 to track which saddle item. If so, pass `new ItemStack(Items.SADDLE)` as the first arg.
- `setPersistenceRequired()` is the modern name; old name was `setPersistent(boolean)`.
- `EntityType.create(Level, EntitySpawnReason)` — `EntitySpawnReason` is the modern parameter (legacy was `MobSpawnType`).

- [ ] **Step 2: Build**

```bash
./gradlew :wildwest:build
```

Expected: BUILD SUCCESSFUL. If any of the API caveats above hits, adapt and re-build until clean.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/spawning/LeaderEntourageSpawner.java
git commit -m "feat(wildwest): LeaderEntourageSpawner helper

Spawns horse (tamed/saddled, custom variant + markings, 30 HP) at the
leader's position, mounts the leader, then spawns 2 followers within
4 blk with setPersistenceRequired."
```

---

## Task 5: Wire LawmanVillageSpawner to call entourage spawner after sherrif spawn

Modify the existing sherrif spawn path: after a successful `EntityType.spawn(...)`, call `LeaderEntourageSpawner.spawnEntourage(...)` with deputy follower type + chestnut horse coat.

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/spawning/LawmanVillageSpawner.java`

- [ ] **Step 1: Find the sherrif spawn line**

The existing code looks like this (around line 90-100 of the file):

```java
            if (sherrifCount < MAX_SHERRIFS_PER_VILLAGE && sl.getRandom().nextFloat() < SHERRIF_SPAWN_CHANCE) {
                spawn(sl, ModEntities.SHERRIF.get(), center);
            }
```

(Read the actual file first; the variable names should match the existing code.)

- [ ] **Step 2: Replace the sherrif-spawn invocation with an entourage-aware version**

After the existing `spawn(sl, ModEntities.SHERRIF.get(), center)` returns, we need access to the spawned `SherrifEntity` to mount it. Update `spawn` (the private helper at the bottom of the file) so it returns the spawned entity, then post-process for sherrifs.

Look at the existing `spawn(...)` method (around the bottom of the file):

```java
    private static <T extends Entity> void spawn(ServerLevel sl, EntityType<T> type, BlockPos pos) {
        // ... existing body returns void
    }
```

Change it to return `T`:

```java
    private static <T extends Entity> T spawn(ServerLevel sl, EntityType<T> type, BlockPos pos) {
        int x = pos.getX() + sl.getRandom().nextInt(11) - 5;
        int z = pos.getZ() + sl.getRandom().nextInt(11) - 5;
        int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos spawnPos = new BlockPos(x, y, z);
        return type.spawn(sl, spawnPos, EntitySpawnReason.NATURAL);
    }
```

(`EntityType.spawn` already returns the spawned `T` — `T extends Entity` may be `null` if the spawn was vetoed, so callers must null-check.)

In the sherrif-spawn branch, replace:

```java
            if (sherrifCount < MAX_SHERRIFS_PER_VILLAGE && sl.getRandom().nextFloat() < SHERRIF_SPAWN_CHANCE) {
                spawn(sl, ModEntities.SHERRIF.get(), center);
            }
```

with:

```java
            if (sherrifCount < MAX_SHERRIFS_PER_VILLAGE && sl.getRandom().nextFloat() < SHERRIF_SPAWN_CHANCE) {
                com.tweeks.wildwest.entity.SherrifEntity sherrif = spawn(sl, ModEntities.SHERRIF.get(), center);
                if (sherrif != null) {
                    com.tweeks.wildwest.spawning.LeaderEntourageSpawner.spawnEntourage(
                        sl, sherrif, ModEntities.DEPUTY.get(),
                        net.minecraft.world.entity.animal.horse.Variant.CHESTNUT,
                        net.minecraft.world.entity.animal.horse.Markings.WHITE_FIELD);
                }
            }
```

(Deputies still spawn solo via the existing deputy-spawn branch in this same file — we DON'T add an entourage to deputy spawns; entourage is leader-only.)

- [ ] **Step 3: Build**

```bash
./gradlew :wildwest:build
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/spawning/LawmanVillageSpawner.java
git commit -m "feat(wildwest): wire sherrif spawn through LeaderEntourageSpawner

After a sherrif spawns from a village tick, summon its chestnut horse
+ 2 deputy followers. Deputy solo spawns are untouched."
```

---

## Task 6: BanditLeaderPackSpawner (FinalizeMobSpawnEvent listener)

Bandit-leader spawns are driven by the NeoForge biome modifier (not by our code), so we hook the post-spawn event to attach the entourage.

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/spawning/BanditLeaderPackSpawner.java`

- [ ] **Step 1: Create BanditLeaderPackSpawner**

```java
package com.tweeks.wildwest.spawning;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.entity.BanditLeaderEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.horse.Markings;
import net.minecraft.world.entity.animal.horse.Variant;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeMobSpawnEvent;

/**
 * Listens to FinalizeMobSpawnEvent — fired at the end of every mob-spawn
 * pipeline including biome-modifier-driven natural spawns. When a
 * BanditLeaderEntity finishes spawning naturally, attach the entourage
 * (black horse + 2 bandits).
 *
 * Filters strictly on natural spawns and on entity class to avoid recursive
 * spawning when our own follower spawns happen to also fire the event.
 */
@EventBusSubscriber(modid = WildWestMod.MOD_ID)
public final class BanditLeaderPackSpawner {
    private BanditLeaderPackSpawner() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeMobSpawnEvent event) {
        // Only natural biome spawns get an entourage. Spawn-eggs, /summon, and
        // our own pack-spawn followers don't.
        if (event.getSpawnType() != EntitySpawnReason.NATURAL) return;
        if (!(event.getEntity() instanceof BanditLeaderEntity leader)) return;
        if (!(leader.level() instanceof ServerLevel sl)) return;

        // Pre-flight: skip if leader is already mounted (re-spawn / re-load case).
        if (leader.getVehicle() != null) return;

        LeaderEntourageSpawner.spawnEntourage(
            sl, leader, ModEntities.BANDIT.get(),
            Variant.BLACK, Markings.NONE);
    }
}
```

If the actual NeoForge event is named differently in MC 26.1.2 (e.g., `MobSpawnEvent.FinalizeSpawn` or `MobSpawnEvent.PositionCheck`), adapt — semantically we want "fired once per natural mob spawn after the entity is in the world." `FinalizeMobSpawnEvent` was the modern form in 1.21+.

If `event.getSpawnType()` doesn't exist as a method (the field/method may be `getReason()` or `getSpawnReason()`), adapt to whichever the event class exposes.

- [ ] **Step 2: Build**

```bash
./gradlew :wildwest:build
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/spawning/BanditLeaderPackSpawner.java
git commit -m "feat(wildwest): BanditLeaderPackSpawner — entourage on biome spawn

Subscribes to FinalizeMobSpawnEvent. When a bandit-leader spawns
naturally, attach black horse + 2 bandit followers. Filtered strictly
on natural-reason + entity class + no-existing-vehicle to prevent
recursion."
```

---

## Task 7: End-to-end smoke test

Verification only — no code change unless something fails.

- [ ] **Step 1: Launch dev client**

```bash
./gradlew :wildwest:runClient
```

- [ ] **Step 2: Walk the manual smoke test from the spec**

Reference: design spec section "Manual smoke test" (steps 1-11). Tick each off:
1. Sherrif egg → mounted on chestnut horse + 2 deputies nearby, persistent.
2. Bandit-leader egg → black horse + 2 bandits nearby.
3. Shoot horse → rider drops to ground combat.
4. Shoot rider → horse persists, player can claim.
5. Coordinated targeting: hit sherrif → deputies adopt player target.
6. Organic follow: lone deputy 12 blk away → joins patrol.
7. Lose-interest range: 30-blk separation → deputy stops following.
8. Leader death: kill sherrif → deputies revert to individual AI.
9. Plains-village natural spawn (after 30+ in-game minutes).
10. Bandit-leader natural spawn at night in plains/savanna.
11. `./gradlew :wildwest:build :wildwest:runServerData :wildwest:runClientData` all succeed.

- [ ] **Step 3: Mark phase 3 complete in the spec**

Open `docs/superpowers/specs/2026-04-30-wildwest-phase3-mounts-leaders-design.md`, change `**Status:** Draft` to `**Status:** Implemented in [hash]`. Commit:

```bash
git add docs/superpowers/specs/2026-04-30-wildwest-phase3-mounts-leaders-design.md
git commit -m "docs(wildwest): mark phase 3 spec as implemented"
```

---

## Self-review notes

- **Spec coverage check:**
  - Spawn-mounted leaders → Tasks 4 + 5 + 6 (helper + sherrif path + bandit-leader path).
  - Pack-spawn (group of 3) → Task 4 (helper) + Tasks 5/6 (callers).
  - Organic-follow AI → Task 3 (`FollowLeaderGoal`).
  - Coordinated targeting → Task 3 (`LeaderTargetCopyGoal`).
  - `isLeader()` abstract method → Task 2.
  - Pure-logic helper + tests → Task 1.
  - Smoke test → Task 7.
- **Type consistency:** `WildWestMob.isLeader()`, `WildWestMob.getFollowingLeader()`, `WildWestMob.setFollowingLeader(WildWestMob)`, `LeaderEntourageSpawner.spawnEntourage(ServerLevel, WildWestMob, EntityType<? extends WildWestMob>, Variant, Markings)`, `FollowDecision.choose(boolean, List<C>, C)` — all referenced consistently across tasks.
- **No placeholders:** every code block is concrete; every command has expected output. The "implementer adapts" notes are intentional reactive-instruction escapes for known API drift, not deferred work.
- **Tests:** Task 1 has unit tests; subsequent tasks are verified via build + Task 7 smoke test. Matches phase-1 + phase-2 convention.
- **7 tasks** total — smaller than phase 1 (13) and phase 2 (15+1). Reasonable since phase 3 adds zero new entity types or items, just behavior layered on existing mobs.
