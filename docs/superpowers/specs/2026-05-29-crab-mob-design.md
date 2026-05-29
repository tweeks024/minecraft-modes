# Crab Mob — Design Spec

**Date:** 2026-05-29
**Mod:** wildwest (Java + Bedrock parity)
**Status:** Approved, ready for implementation plan

## 1. Summary

Add a Crab mob to the Wild West mod, modeled on the vanilla 1.21 wiki crab
(https://minecraft.wiki/w/Crab) with three custom behavior changes:

1. Drops nothing on death (no claw drop, no XP-only).
2. Neutral to players and villagers — pinches back only when attacked. When
   attacked, every Crab within an 8-block radius is alerted and targets the
   attacker (swarm response).
3. Always-on hostility toward vanilla hostile mobs (entities matching the
   `minecraft:monster` tag — zombies, skeletons, spiders, creepers, etc.).

Otherwise the Crab matches vanilla parity: 6 HP, 2 damage, 0.5 movement speed,
breeds with seagrass, baby crabs follow parent, animated claw extension during
the pinch attack.

## 2. Approach

Java implementation: `CrabEntity extends Animal implements NeutralMob`.
Rationale:

- `Animal` provides breeding, baby growth, panic, and food/tempt plumbing for
  free.
- `NeutralMob` provides the anger-timer + remembered-attacker persistence used
  by vanilla wolves and iron golems. Anger lasts a uniformly random 20–39
  seconds (`TimeUtil.rangeOfSeconds(20, 39)`).
- Swarm broadcast lives in a small static helper invoked from `hurt()`. Each
  crab independently runs `HurtByTargetGoal` for itself; the helper just calls
  `setTarget` + `setLastHurtByMob` on nearby siblings so the same goal chain
  fires for them.

Rejected alternatives:

- `PathfinderMob` direct — would require reimplementing breeding and anger
  persistence (~200 lines of boilerplate).
- `TamableAnimal` — adds tame/sit slots the spec does not need; misleads
  readers into thinking the crab is tameable.

## 3. File Layout

### 3.1 Java (wildwest mod)

```
wildwest/src/main/java/com/tweeks/wildwest/
├── entity/
│   ├── CrabEntity.java                    (NEW — Animal + NeutralMob)
│   └── ai/
│       └── CrabAlertSwarmHelper.java      (NEW — static swarm broadcast)
├── client/
│   ├── CrabRenderer.java                  (NEW)
│   ├── CrabModel.java                     (NEW — claw bone)
│   └── CrabAnimations.java                (NEW — claw extend keyframes)
├── ModEntities.java                       (REGISTER CrabEntity)
├── ModSpawnPlacements.java                (REGISTER spawn rules)
├── data/WildWestBiomeModifiers.java       (ADD beach + warm ocean spawn)
└── data/WildWestLootTables.java           (ADD empty crab loot table)
```

### 3.2 Java assets/data

```
wildwest/src/main/resources/assets/wildwest/
├── geo/crab.geo.json                      (NEW — bbmodel export)
├── textures/entity/crab.png               (NEW)
└── lang/en_us.json                        (ADD "entity.wildwest.crab")

wildwest/src/main/resources/data/wildwest/
├── loot_tables/entities/crab.json         (NEW — empty pools array)
└── tags/worldgen/biome/spawns_crab.json   (NEW — beach + warm_ocean)
```

### 3.3 Bedrock mirror

```
bedrock-out/wildwest/
├── behavior_pack/
│   ├── entities/crab.json                 (NEW)
│   ├── loot_tables/entities/crab.json     (NEW — empty)
│   └── scripts/goals/
│       ├── CrabSwarmTargetGoal.ts         (NEW)
│       └── CrabMeleePinchGoal.ts          (NEW)
├── resource_pack/
│   ├── entity/crab.entity.json            (NEW)
│   ├── textures/entity/crab.png           (NEW — shared with Java)
│   └── animations/crab.animation.json     (NEW — claw extend)
└── UNTRANSLATABLE.md                      (ADD crab name entry if needed)
```

### 3.4 Tests

```
wildwest/src/test/java/com/tweeks/wildwest/entity/
└── CrabEntityTest.java                    (NEW)
```

## 4. Java Implementation Details

### 4.1 CrabEntity skeleton

```java
public class CrabEntity extends Animal implements NeutralMob {
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private static final double SWARM_RADIUS = 8.0;

    private int remainingPersistentAngerTime;
    private @Nullable UUID persistentAngerTarget;

    public CrabEntity(EntityType<? extends CrabEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, 6.0)
            .add(Attributes.MOVEMENT_SPEED, 0.5)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
        this.goalSelector.addGoal(2, new CrabMeleePinchGoal(this, 1.0, true));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.2, s -> s.is(Items.SEAGRASS), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
        this.targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (!level().isClientSide && src.getEntity() instanceof LivingEntity attacker) {
            CrabAlertSwarmHelper.alertNearby(this, attacker, SWARM_RADIUS);
        }
        return super.hurt(src, amount);
    }

    @Override public boolean isFood(ItemStack s) { return s.is(Items.SEAGRASS); }

    @Override
    public CrabEntity getBreedOffspring(ServerLevel l, AgeableMob p) {
        return ModEntities.CRAB.get().create(l);
    }

    // NeutralMob impl (standard boilerplate)
    @Override public int getRemainingPersistentAngerTime() { return remainingPersistentAngerTime; }
    @Override public void setRemainingPersistentAngerTime(int t) { this.remainingPersistentAngerTime = t; }
    @Override public @Nullable UUID getPersistentAngerTarget() { return persistentAngerTarget; }
    @Override public void setPersistentAngerTarget(@Nullable UUID id) { this.persistentAngerTarget = id; }
    @Override public void startPersistentAngerTimer() {
        setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(random));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) updatePersistentAnger((ServerLevel) level(), true);
    }
}
```

### 4.2 Swarm helper

```java
public final class CrabAlertSwarmHelper {
    private CrabAlertSwarmHelper() {}

    public static void alertNearby(CrabEntity src, LivingEntity attacker, double radius) {
        AABB box = src.getBoundingBox().inflate(radius);
        for (CrabEntity other : src.level().getEntitiesOfClass(CrabEntity.class, box)) {
            if (other == src) continue;
            if (other.isBaby()) continue;
            if (other.getTarget() != null) continue;
            other.setTarget(attacker);
            other.setLastHurtByMob(attacker);
            other.startPersistentAngerTimer();
            other.setPersistentAngerTarget(attacker.getUUID());
        }
    }
}
```

### 4.3 Pinch goal (claw-extend animation trigger)

`CrabMeleePinchGoal` subclasses `MeleeAttackGoal`. Override
`checkAndPerformAttack`: when the parent class returns true (a hit was
applied), call `crab.level().broadcastEntityEvent(crab, EVENT_ID_PINCH)`
where `EVENT_ID_PINCH = (byte) 60`. Server→client broadcast triggers
`CrabEntity.handleEntityEvent(60)`, which starts the client-only
`AnimationState clawExtendState`. Client-side `CrabAnimations.PINCH`
keyframes the claw bones.

### 4.4 Registration

- `ModEntities.CRAB` — `EntityType.Builder.of(CrabEntity::new, MobCategory.CREATURE).sized(0.5F, 0.5F)`
- `ModSpawnPlacements`: `SpawnPlacements.register(ModEntities.CRAB.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules)`
- `WildWestBiomeModifiers`: add Crab spawn (weight 6, min 2, max 4) to biome
  tag `#wildwest:spawns_crab`. Tag JSON contains `minecraft:beach` and
  `minecraft:warm_ocean`.
- Attribute registration via `EntityAttributeCreationEvent`.

### 4.5 Renderer + model

- Geometry from a bbmodel export `wildwest/tools/crab.bbmodel` → `crab.geo.json`.
- `CrabModel` exposes the `claw_left` and `claw_right` bones for animation
  state binding. `CrabAnimations.PINCH` defines a 0.5s animation scaling the
  claws on local Y from 1.0 → 1.6 → 1.0.

## 5. Bedrock Implementation Details

### 5.1 Entity definition (`behavior_pack/entities/crab.json`)

- Format version `1.20.40`
- Identifier: `wildwest:crab`
- `is_spawnable: true`, `is_summonable: true`
- Family: `["crab", "animal", "mob"]`
- Components:
  - `minecraft:health` (6)
  - `minecraft:movement` (0.5)
  - `minecraft:attack` (2)
  - `minecraft:breedable` (seagrass)
  - `minecraft:behavior.float`
  - `minecraft:behavior.panic`
  - `minecraft:behavior.melee_attack`
  - `minecraft:behavior.hurt_by_target`
  - `minecraft:behavior.nearest_attackable_target` with filter
    `family: monster`
  - `minecraft:loot { table: "loot_tables/entities/crab.json" }`

Swarm broadcast is implemented entirely in script (Section 5.2) — no custom
entity event is declared on the crab JSON.

### 5.2 Scripts

- `CrabSwarmTargetGoal.ts`: subscribes to `entityHurt`. When a `wildwest:crab`
  is hurt by a non-crab living entity, scan an 8-block radius for other
  `wildwest:crab` entities and call `target = attacker` on each via
  `entity.setProperty` + a custom property + `setTarget` runtime command.
  Skip babies. Skip crabs that already have a target.
- `CrabMeleePinchGoal.ts`: mirrors Java pinch goal — fires animation event
  via `entity.playAnimation('animation.crab.pinch')` when attack swings.

### 5.3 Resource pack

- `entity/crab.entity.json` — references `geometry.wildwest.crab` and the
  shared `textures/entity/crab` texture.
- `animations/crab.animation.json` — `animation.crab.pinch` controller entry
  scaling claw bones.

## 6. Spawning

- Weight 6, group size 2–4. Matches wiki crab.
- Biome tag `#wildwest:spawns_crab` contains:
  - `minecraft:beach`
  - `minecraft:warm_ocean`
- Spawn predicate: `Animal::checkAnimalSpawnRules` (sky-light ≥ 9, valid
  ground block).

## 7. Loot Table

`loot_tables/entities/crab.json`:

```json
{
  "type": "minecraft:entity",
  "pools": []
}
```

Empty pools array. No claw, no XP-from-table. Death-XP from the engine still
applies (~1–3 XP) consistent with other Animal subclasses; the spec only
forbids item drops, not XP.

## 8. Tests

`CrabEntityTest.java` covers:

| # | Test | Asserts |
|---|------|---------|
| 1 | `hurt_alertsCrabsWithinRadius_eight` | Crab within 8 blocks of hurt crab gets `getTarget() == attacker` |
| 2 | `hurt_doesNotAlertCrabsOutsideRadius` | Crab at 9 blocks unchanged |
| 3 | `hurt_doesNotOverrideExistingTarget` | Crab already targeting X keeps X |
| 4 | `hurt_doesNotAlertBabies` | Baby crabs ignored |
| 5 | `isNotHostileToPlayer_byDefault` | Idle crab has no target near player |
| 6 | `targetsMonster_byDefault` | Zombie within range becomes target |
| 7 | `dropsNothing_onDeath` | Killed crab generates zero item entities |
| 8 | `breeds_withSeagrass_producesBaby` | Two fed crabs spawn one baby |

Existing test patterns to mirror: `AnomalyEntityTest`, `RedstoneGolemEntityTest`.

## 9. Translations

- Java `lang/en_us.json`: `"entity.wildwest.crab": "Crab"`
- Bedrock `texts/en_US.lang`: `entity.wildwest:crab.name=Crab`

## 10. Out of Scope

- No taming.
- No leashing logic beyond `Animal` default (which is fine — crabs are
  leashable as a side effect of `Animal`).
- No ride/saddle.
- No claw item or drop.
- No XP table override (engine default stays).
- No ship/structure integration.
- No interaction with existing wildwest apex bosses.
