# Redstone Golem Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the sixth apex-tier boss `wildwest:redstone_golem` per [docs/superpowers/specs/2026-05-25-redstone-golem-design.md](../specs/2026-05-25-redstone-golem-design.md). Player-built tank archetype with melee, ground-slam shockwave, redstone-bomb throw. Drops Piston Gauntlet (right-click punch / aim-down rocket-jump).

**Architecture:** New `RedstoneGolemEntity extends Monster` (no singleton, no natural spawn). Construction triggered by `EntityJoinLevelEvent` for `PrimedTnt` over a T-pattern of redstone blocks. New `RedstoneBombEntity extends ThrowableProjectile`. New `PistonGauntletItem extends Item`. Sibling files in `wildwest/src/main/java/com/tweeks/wildwest/...` follow the GrimReaperEntity pattern.

**Tech Stack:** Java 25, NeoForge (version per `wildwest/gradle.properties`), JUnit 5 for tests, NeoForge data-generation (`ModDamageTypeProvider`) for damage-type JSON.

**Working directory:** `/Users/tweeks/code/minecraft-mods` (Gradle module: `wildwest`).

**Build / test commands:**
- Full module test: `./gradlew :wildwest:test`
- Single test class: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.item.PistonGauntletItemTest"`
- Datagen (re-generate damage_type JSON after editing `ModDamageTypeProvider`): `./gradlew :wildwest:runData`
- Compile only: `./gradlew :wildwest:compileJava`

**Reference files** (read-only — do not modify; mirror patterns):
- Entity sibling: `wildwest/src/main/java/com/tweeks/wildwest/entity/GrimReaperEntity.java`
- Boss with no spawn egg (none yet — design choice for this boss is no egg)
- Projectile sibling: `wildwest/src/main/java/com/tweeks/wildwest/entity/projectile/MeteorEntity.java`
- Item with cooldown: `wildwest/src/main/java/com/tweeks/wildwest/item/ReaperScytheItem.java`
- Damage type bootstrap: `wildwest/src/main/java/com/tweeks/wildwest/data/ModDamageTypeProvider.java`
- Test smoke pattern: `wildwest/src/test/java/com/tweeks/wildwest/item/ReaperScytheItemTest.java`
- Entity registration: `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java`
- Item registration + creative tab: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`

**Style notes:**
- 4-space indentation, fully-qualified class names where used only once (matches `GrimReaperEntity`).
- Avoid `// added` / `// removed` comments — straight code only.
- Only add a doc comment on a class if it states *why* the class exists (matches GrimReaperEntity header).
- Don't write multi-paragraph javadoc. One short line max.

---

## File Structure

**Create:**
- `wildwest/src/main/java/com/tweeks/wildwest/entity/RedstoneGolemEntity.java` — entity class
- `wildwest/src/main/java/com/tweeks/wildwest/entity/projectile/RedstoneBombEntity.java` — projectile class
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/RedstoneGolemGroundSlamGoal.java` — close-range AOE goal
- `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/RedstoneGolemThrowBombGoal.java` — ranged goal
- `wildwest/src/main/java/com/tweeks/wildwest/event/RedstoneGolemConstructionHandler.java` — PrimedTnt listener
- `wildwest/src/main/java/com/tweeks/wildwest/item/PistonGauntletItem.java` — signature drop
- `wildwest/src/main/java/com/tweeks/wildwest/client/RedstoneGolemRenderer.java` — entity renderer
- `wildwest/src/main/java/com/tweeks/wildwest/client/model/RedstoneGolemModel.java` — entity model
- `wildwest/src/main/java/com/tweeks/wildwest/client/RedstoneBombRenderer.java` — bomb renderer
- `wildwest/src/main/resources/data/wildwest/loot_table/entities/redstone_golem.json` — loot table
- `wildwest/src/main/resources/assets/wildwest/textures/entity/redstone_golem.png` — texture (256×128, placeholder colored block OK)
- `wildwest/src/main/resources/assets/wildwest/textures/entity/redstone_bomb.png` — texture (16×16)
- `wildwest/src/main/resources/assets/wildwest/textures/item/piston_gauntlet.png` — item icon (16×16)
- `wildwest/src/main/resources/assets/wildwest/models/item/piston_gauntlet.json` — item model
- `wildwest/src/test/java/com/tweeks/wildwest/event/RedstoneGolemConstructionHandlerTest.java`
- `wildwest/src/test/java/com/tweeks/wildwest/entity/RedstoneGolemEntityTest.java`
- `wildwest/src/test/java/com/tweeks/wildwest/entity/projectile/RedstoneBombEntityTest.java`
- `wildwest/src/test/java/com/tweeks/wildwest/item/PistonGauntletItemTest.java`
- `wildwest/src/test/java/com/tweeks/wildwest/entity/ai/RedstoneGolemGoalsTest.java`

**Modify:**
- `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java` — register REDSTONE_GOLEM + REDSTONE_BOMB
- `wildwest/src/main/java/com/tweeks/wildwest/Registration.java` — register PISTON_GAUNTLET + add to creative tab
- `wildwest/src/main/java/com/tweeks/wildwest/WildWestDamageTypes.java` — add PISTON_PUNCH key + accessor
- `wildwest/src/main/java/com/tweeks/wildwest/data/ModDamageTypeProvider.java` — bootstrap PISTON_PUNCH
- `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java` — register entity attributes, event handler
- `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java` — register renderer + model layer
- `wildwest/src/main/resources/assets/wildwest/lang/en_us.json` — translation keys

---

## Task 1: Entity skeleton + registration + attributes

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/RedstoneGolemEntity.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`
- Test: `wildwest/src/test/java/com/tweeks/wildwest/entity/RedstoneGolemEntityTest.java`

- [ ] **Step 1: Create the failing test**

File `wildwest/src/test/java/com/tweeks/wildwest/entity/RedstoneGolemEntityTest.java`:
```java
package com.tweeks.wildwest.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Constants smoke test. Catches accidental drift of HP / speed / damage
 * values that the spec promises. Full spawn flow is exercised manually
 * in dev-client.
 */
class RedstoneGolemEntityTest {

    @Test
    void constants_matchSpec() {
        assertEquals(280.0, RedstoneGolemEntity.MAX_HEALTH);
        assertEquals(10.0, RedstoneGolemEntity.ATTACK_DAMAGE);
        assertEquals(0.22, RedstoneGolemEntity.MOVEMENT_SPEED);
        assertEquals(14.0, RedstoneGolemEntity.ARMOR);
        assertEquals(1.0, RedstoneGolemEntity.KNOCKBACK_RESISTANCE);
        assertEquals(48.0, RedstoneGolemEntity.FOLLOW_RANGE);
        assertEquals(100, RedstoneGolemEntity.XP_DROP);
    }

    @Test
    void boss_bar_color_red_notched_10() {
        assertEquals("RED", RedstoneGolemEntity.BOSS_BAR_COLOR_NAME);
        assertEquals("NOTCHED_10", RedstoneGolemEntity.BOSS_BAR_OVERLAY_NAME);
    }
}
```

- [ ] **Step 2: Run test to verify it fails (class doesn't exist)**

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.entity.RedstoneGolemEntityTest"`
Expected: FAIL with `cannot find symbol class RedstoneGolemEntity`.

- [ ] **Step 3: Create the entity class**

File `wildwest/src/main/java/com/tweeks/wildwest/entity/RedstoneGolemEntity.java`:
```java
package com.tweeks.wildwest.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Sixth apex boss. Player-built tank: T-pattern of redstone blocks topped
 * with TNT spawns one via {@link com.tweeks.wildwest.event.RedstoneGolemConstructionHandler}.
 * No singleton — multiple instances allowed.
 */
public class RedstoneGolemEntity extends Monster {

    public static final double MAX_HEALTH = 280.0;
    public static final double ATTACK_DAMAGE = 10.0;
    public static final double MOVEMENT_SPEED = 0.22;
    public static final double ARMOR = 14.0;
    public static final double KNOCKBACK_RESISTANCE = 1.0;
    public static final double FOLLOW_RANGE = 48.0;
    public static final int XP_DROP = 100;

    public static final String BOSS_BAR_COLOR_NAME = "RED";
    public static final String BOSS_BAR_OVERLAY_NAME = "NOTCHED_10";

    private final ServerBossEvent bossBar;

    public RedstoneGolemEntity(EntityType<? extends RedstoneGolemEntity> type, Level level) {
        super(type, level);
        this.bossBar = new ServerBossEvent(
            Mth.createInsecureUUID(this.random),
            Component.translatable("entity.wildwest.redstone_golem"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(4, new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.ARMOR, ARMOR)
            .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
            .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    @Override
    public boolean causeFallDamage(double distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected int getBaseExperienceReward(net.minecraft.server.level.ServerLevel level) {
        return XP_DROP;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.entity.RedstoneGolemEntityTest"`
Expected: PASS (both tests green).

- [ ] **Step 5: Register the entity type**

In `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java`, add the import alongside existing entity imports:
```java
import com.tweeks.wildwest.entity.RedstoneGolemEntity;
```

Add the registration AFTER the `PIRATE_CAPTAIN` block (before `METEOR`):
```java
public static final DeferredHolder<EntityType<?>, EntityType<RedstoneGolemEntity>> REDSTONE_GOLEM =
    ENTITY_TYPES.register("redstone_golem", () -> EntityType.Builder.<RedstoneGolemEntity>of(
            RedstoneGolemEntity::new, MobCategory.MONSTER)
        .sized(1.4f, 2.7f)
        .clientTrackingRange(10)
        .build(ResourceKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "redstone_golem"))));
```

- [ ] **Step 6: Wire attribute registration in WildWestMod**

Open `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`. Find the existing `EntityAttributeCreationEvent` handler (search for `GrimReaperEntity.createAttributes()` — there's an `addAttributes` call). Add the new line:
```java
event.put(ModEntities.REDSTONE_GOLEM.get(), RedstoneGolemEntity.createAttributes().build());
```

Add the import at the top of `WildWestMod.java`:
```java
import com.tweeks.wildwest.entity.RedstoneGolemEntity;
```

- [ ] **Step 7: Compile + verify**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.entity.RedstoneGolemEntityTest"`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/RedstoneGolemEntity.java \
        wildwest/src/test/java/com/tweeks/wildwest/entity/RedstoneGolemEntityTest.java \
        wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java \
        wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java
git commit -m "$(cat <<'EOF'
feat(wildwest): RedstoneGolemEntity skeleton + registration

280 HP tank with melee + boss bar. No singleton, no natural spawn (placeholder
for construction trigger added in next commit). XP drop 100.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Renderer + model + texture wiring

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/RedstoneGolemRenderer.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/model/RedstoneGolemModel.java`
- Create: `wildwest/src/main/resources/assets/wildwest/textures/entity/redstone_golem.png` (placeholder)
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java`

This task has no unit tests — rendering is dev-client-verified only (matches the rest of the codebase; `BanditRenderer` / `GrimReaperRenderer` have no tests).

- [ ] **Step 1: Create the model class**

File `wildwest/src/main/java/com/tweeks/wildwest/client/model/RedstoneGolemModel.java`:
```java
package com.tweeks.wildwest.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

public class RedstoneGolemModel<T extends LivingEntity> extends HierarchicalModel<T> {

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public RedstoneGolemModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -12.0F, -4.0F, 8.0F, 10.0F, 8.0F),
            PartPose.offset(0.0F, -7.0F, -2.0F));

        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 40).addBox(-9.0F, -2.0F, -6.0F, 18.0F, 12.0F, 11.0F),
            PartPose.offset(0.0F, -7.0F, 0.0F));

        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(60, 21).addBox(-13.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F),
            PartPose.offset(0.0F, -7.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(60, 58).addBox(9.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F),
            PartPose.offset(0.0F, -7.0F, 0.0F));

        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(37, 0).addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F),
            PartPose.offset(-4.0F, 11.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(60, 0).mirror().addBox(-2.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F),
            PartPose.offset(5.0F, 11.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * (float) (Math.PI / 180.0);
        this.head.xRot = headPitch * (float) (Math.PI / 180.0);
        this.rightLeg.xRot = -1.5F * triangleWave(limbSwing, 13.0F) * limbSwingAmount;
        this.leftLeg.xRot = 1.5F * triangleWave(limbSwing, 13.0F) * limbSwingAmount;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;
        this.rightArm.xRot = (-0.2F + 1.5F * triangleWave(limbSwing, 13.0F)) * limbSwingAmount;
        this.leftArm.xRot = (-0.2F - 1.5F * triangleWave(limbSwing, 13.0F)) * limbSwingAmount;
    }

    private static float triangleWave(float p, float period) {
        return (Math.abs(p % period - period * 0.5F) - period * 0.25F) / (period * 0.25F);
    }
}
```

> If you find the `HierarchicalModel` superclass missing or in a different package in the current NeoForge build (some versions renamed it `EntityModel`), check `wildwest/src/main/java/com/tweeks/wildwest/client/model/BanditModel.java` for the current import and mirror it. Adjust constructor + `root()` accordingly.

- [ ] **Step 2: Create the renderer class**

File `wildwest/src/main/java/com/tweeks/wildwest/client/RedstoneGolemRenderer.java`:
```java
package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.model.RedstoneGolemModel;
import com.tweeks.wildwest.entity.RedstoneGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceLocation;

public class RedstoneGolemRenderer extends MobRenderer<RedstoneGolemEntity, RedstoneGolemModel<RedstoneGolemEntity>> {

    private static final ResourceLocation TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/redstone_golem.png");

    public RedstoneGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new RedstoneGolemModel<>(context.bakeLayer(ClientSetup.REDSTONE_GOLEM_LAYER)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(RedstoneGolemEntity entity) {
        return TEXTURE;
    }
}
```

> If your `MobRenderer` signature has changed (NeoForge 26.x sometimes uses `MobRenderer<E, RenderState, Model>` with separate render-state objects), open `SteveStackerRenderer.java` or `GrimReaperRenderer.java` and mirror the exact generic parameters used there.

- [ ] **Step 3: Wire renderer + layer in ClientSetup**

In `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java`, near other `ModelLayerLocation` constants (search for `GRIM_REAPER_LAYER` or similar):
```java
public static final ModelLayerLocation REDSTONE_GOLEM_LAYER = new ModelLayerLocation(
    Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "redstone_golem"), "main");
```

Wherever the existing renderer registrations live (look for `EntityRenderers.register(...)` calls or an event handler subscribed to `EntityRenderersEvent.RegisterRenderers`), add:
```java
event.registerEntityRenderer(ModEntities.REDSTONE_GOLEM.get(), RedstoneGolemRenderer::new);
```

Wherever the existing layer definitions are registered (look for `event.registerLayerDefinition(...)` calls under `EntityRenderersEvent.RegisterLayerDefinitions`), add:
```java
event.registerLayerDefinition(REDSTONE_GOLEM_LAYER, RedstoneGolemModel::createBodyLayer);
```

Add the imports as needed at the top:
```java
import com.tweeks.wildwest.client.model.RedstoneGolemModel;
import com.tweeks.wildwest.ModEntities;
```
(If already imported, skip.)

- [ ] **Step 4: Create the placeholder texture**

Use a 256×128 PNG with a solid redstone-red rectangle on the body region. The file path is:
`wildwest/src/main/resources/assets/wildwest/textures/entity/redstone_golem.png`

Acceptable for v1: any 256×128 PNG with a recognizable redstone-red colored body. For a quick valid placeholder:
```bash
# In repo root. Requires ImageMagick (`brew install imagemagick`).
mkdir -p wildwest/src/main/resources/assets/wildwest/textures/entity
magick -size 256x128 xc:'#aa0000' wildwest/src/main/resources/assets/wildwest/textures/entity/redstone_golem.png
```

If `magick` is unavailable, copy any existing 256×128 entity texture (e.g., from vanilla iron_golem unpack) and rename it to `redstone_golem.png` as a stand-in. The actual pixel-art polish is out of scope for this plan.

- [ ] **Step 5: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/client/RedstoneGolemRenderer.java \
        wildwest/src/main/java/com/tweeks/wildwest/client/model/RedstoneGolemModel.java \
        wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java \
        wildwest/src/main/resources/assets/wildwest/textures/entity/redstone_golem.png
git commit -m "$(cat <<'EOF'
feat(wildwest): RedstoneGolem renderer + model wiring

Iron-golem-shaped model with placeholder redstone-red texture. Dev-client
verification deferred per pattern.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Construction trigger (PrimedTnt listener)

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/event/RedstoneGolemConstructionHandler.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestMod.java`
- Test: `wildwest/src/test/java/com/tweeks/wildwest/event/RedstoneGolemConstructionHandlerTest.java`

- [ ] **Step 1: Write the failing pattern-matcher test**

File `wildwest/src/test/java/com/tweeks/wildwest/event/RedstoneGolemConstructionHandlerTest.java`:
```java
package com.tweeks.wildwest.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Constants smoke test for construction handler. Pattern-match logic is
 * exercised in dev-client by placing TNT atop a redstone-T pattern.
 */
class RedstoneGolemConstructionHandlerTest {

    @Test
    void constants_matchSpec() {
        assertEquals("wildwest:golem_consumed", RedstoneGolemConstructionHandler.CONSUMED_TAG);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.event.RedstoneGolemConstructionHandlerTest"`
Expected: FAIL with `cannot find symbol class RedstoneGolemConstructionHandler`.

- [ ] **Step 3: Implement the handler**

File `wildwest/src/main/java/com/tweeks/wildwest/event/RedstoneGolemConstructionHandler.java`:
```java
package com.tweeks.wildwest.event;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.entity.RedstoneGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.joml.Vector3f;

import com.tweeks.wildwest.WildWestMod;

@EventBusSubscriber(modid = WildWestMod.MOD_ID)
public final class RedstoneGolemConstructionHandler {

    public static final String CONSUMED_TAG = "wildwest:golem_consumed";

    private RedstoneGolemConstructionHandler() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof PrimedTnt primedTnt)) return;
        if (event.getLevel().isClientSide()) return;
        if (primedTnt.getPersistentData().getBoolean(CONSUMED_TAG)) return;

        Level level = (Level) event.getLevel();
        BlockPos headPos = primedTnt.blockPosition();

        if (tryMatch(level, headPos, Direction.Axis.X) || tryMatch(level, headPos, Direction.Axis.Z)) {
            spawnGolem(level, headPos, primedTnt);
        }
    }

    private static boolean tryMatch(Level level, BlockPos headPos, Direction.Axis axis) {
        BlockPos shoulderCenter = headPos.below();
        BlockPos shoulderPos = shoulderCenter.relative(axis, 1);
        BlockPos shoulderNeg = shoulderCenter.relative(axis, -1);
        BlockPos torso = headPos.below(2);

        return isRedstone(level, shoulderCenter)
            && isRedstone(level, shoulderPos)
            && isRedstone(level, shoulderNeg)
            && isRedstone(level, torso);
    }

    private static boolean isRedstone(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.REDSTONE_BLOCK);
    }

    private static void spawnGolem(Level level, BlockPos headPos, PrimedTnt primedTnt) {
        BlockPos torso = headPos.below(2);
        BlockPos shoulderCenter = headPos.below();

        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            BlockPos sCenter = headPos.below();
            BlockPos sPos = sCenter.relative(axis, 1);
            BlockPos sNeg = sCenter.relative(axis, -1);
            if (isRedstone(level, sCenter) && isRedstone(level, sPos) && isRedstone(level, sNeg)) {
                level.setBlock(sPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(sNeg, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                break;
            }
        }
        level.setBlock(shoulderCenter, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(torso, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

        primedTnt.getPersistentData().putBoolean(CONSUMED_TAG, true);
        primedTnt.discard();

        Vec3 spawnAt = Vec3.atBottomCenterOf(torso.above());
        RedstoneGolemEntity golem = ModEntities.REDSTONE_GOLEM.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (golem != null) {
            golem.moveTo(spawnAt.x, spawnAt.y, spawnAt.z, 0.0F, 0.0F);
            level.addFreshEntity(golem);
        }

        level.playSound(null, spawnAt.x, spawnAt.y, spawnAt.z,
            SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 1.0F, 1.0F);

        if (level instanceof ServerLevel serverLevel) {
            DustParticleOptions dust = new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 1.5F);
            for (int i = 0; i < 20; i++) {
                double dx = level.random.nextGaussian() * 0.5;
                double dy = level.random.nextDouble() * 1.5;
                double dz = level.random.nextGaussian() * 0.5;
                serverLevel.sendParticles(dust,
                    spawnAt.x + dx, spawnAt.y + dy, spawnAt.z + dz,
                    1, 0, 0, 0, 0);
            }
        }
    }
}
```

> The `EntitySpawnReason` enum in some NeoForge versions is `MobSpawnType` instead. If you get a compile error on `EntitySpawnReason.MOB_SUMMONED`, search any existing entity in `entity/` for `.create(level,` and mirror the exact import (e.g., `MobSpawnType.MOB_SUMMONED`).

> The `DustParticleOptions` constructor signature has shifted across NeoForge versions. If `new DustParticleOptions(Vector3f, float)` fails, check vanilla `RedstoneWireBlock` source via your IDE for the current constructor.

- [ ] **Step 4: Verify event subscription registration in WildWestMod**

The `@EventBusSubscriber(modid = WildWestMod.MOD_ID)` annotation auto-registers static `@SubscribeEvent` methods on the NeoForge main event bus (the one `EntityJoinLevelEvent` fires on). No manual `NeoForge.EVENT_BUS.register(...)` call is needed.

Confirm by looking at `wildwest/src/main/java/com/tweeks/wildwest/event/VoidMarkHandler.java` — it should use the same pattern.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.event.RedstoneGolemConstructionHandlerTest"`
Expected: PASS.

- [ ] **Step 6: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/event/RedstoneGolemConstructionHandler.java \
        wildwest/src/test/java/com/tweeks/wildwest/event/RedstoneGolemConstructionHandlerTest.java
git commit -m "$(cat <<'EOF'
feat(wildwest): RedstoneGolem construction trigger via PrimedTnt listener

T-pattern of redstone blocks + TNT head spawns the golem. Listens on
PrimedTnt to avoid racing the redstone-block auto-priming. Two-axis
pattern check, particles + sound on success.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Ground slam AI goal

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/RedstoneGolemGroundSlamGoal.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/RedstoneGolemEntity.java` (add goal to registerGoals)
- Test: `wildwest/src/test/java/com/tweeks/wildwest/entity/ai/RedstoneGolemGoalsTest.java`

- [ ] **Step 1: Write the failing constants test**

File `wildwest/src/test/java/com/tweeks/wildwest/entity/ai/RedstoneGolemGoalsTest.java`:
```java
package com.tweeks.wildwest.entity.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedstoneGolemGoalsTest {

    @Test
    void groundSlam_constants_matchSpec() {
        assertEquals(20, RedstoneGolemGroundSlamGoal.WIND_UP_TICKS);
        assertEquals(160, RedstoneGolemGroundSlamGoal.COOLDOWN_TICKS);
        assertEquals(5.0, RedstoneGolemGroundSlamGoal.TRIGGER_RADIUS);
        assertEquals(4.0, RedstoneGolemGroundSlamGoal.DAMAGE_RADIUS);
        assertEquals(4.0f, RedstoneGolemGroundSlamGoal.DAMAGE);
        assertEquals(2.5, RedstoneGolemGroundSlamGoal.KNOCKBACK_STRENGTH);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.entity.ai.RedstoneGolemGoalsTest"`
Expected: FAIL with `cannot find symbol class RedstoneGolemGroundSlamGoal`.

- [ ] **Step 3: Implement the goal**

File `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/RedstoneGolemGroundSlamGoal.java`:
```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.RedstoneGolemEntity;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class RedstoneGolemGroundSlamGoal extends Goal {

    public static final int WIND_UP_TICKS = 20;
    public static final int COOLDOWN_TICKS = 160;
    public static final double TRIGGER_RADIUS = 5.0;
    public static final double DAMAGE_RADIUS = 4.0;
    public static final float DAMAGE = 4.0f;
    public static final double KNOCKBACK_STRENGTH = 2.5;

    private final RedstoneGolemEntity golem;
    private int cooldown;
    private int windUp;

    public RedstoneGolemGroundSlamGoal(RedstoneGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        LivingEntity target = golem.getTarget();
        if (target == null || !target.isAlive()) return false;
        return golem.distanceToSqr(target) <= TRIGGER_RADIUS * TRIGGER_RADIUS;
    }

    @Override
    public boolean canContinueToUse() {
        return windUp > 0;
    }

    @Override
    public void start() {
        windUp = WIND_UP_TICKS;
        golem.level().playSound(null, golem.getX(), golem.getY(), golem.getZ(),
            SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    @Override
    public void tick() {
        windUp--;
        if (windUp <= 0) {
            slam();
        } else if (windUp % 4 == 0 && golem.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                golem.getX(), golem.getY() + 0.1, golem.getZ(),
                4, 0.5, 0.0, 0.5, 0.0);
        }
    }

    private void slam() {
        if (!(golem.level() instanceof ServerLevel serverLevel)) {
            cooldown = COOLDOWN_TICKS;
            return;
        }
        AABB aabb = golem.getBoundingBox().inflate(DAMAGE_RADIUS);
        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, aabb,
            e -> e != golem && e.isAlive());

        for (LivingEntity target : nearby) {
            target.hurt(serverLevel.damageSources().mobAttack(golem), DAMAGE);
            target.knockback(KNOCKBACK_STRENGTH, golem.getX() - target.getX(), golem.getZ() - target.getZ());
            Vec3 m = target.getDeltaMovement();
            target.setDeltaMovement(m.x, Math.max(m.y, 0.6), m.z);
            if (target instanceof net.minecraft.server.level.ServerPlayer sp) {
                sp.hurtMarked = true;
            }
        }

        serverLevel.playSound(null, golem.getX(), golem.getY(), golem.getZ(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.6F, 1.0F);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
            golem.getX(), golem.getY() + 0.2, golem.getZ(),
            1, 0, 0, 0, 0);
        BlockParticleOption blockDust = new BlockParticleOption(ParticleTypes.BLOCK,
            Blocks.REDSTONE_BLOCK.defaultBlockState());
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * Math.PI * 2.0;
            double rx = Math.cos(angle) * DAMAGE_RADIUS;
            double rz = Math.sin(angle) * DAMAGE_RADIUS;
            serverLevel.sendParticles(blockDust,
                golem.getX() + rx, golem.getY() + 0.1, golem.getZ() + rz,
                1, 0, 0.3, 0, 0.05);
        }

        cooldown = COOLDOWN_TICKS;
    }
}
```

> `SoundEvents.GENERIC_EXPLODE` is a `Holder<SoundEvent>` in newer NeoForge — hence `.value()`. If a sibling goal uses the bare `SoundEvents.GENERIC_EXPLODE`, mirror that form.

- [ ] **Step 4: Wire the goal into registerGoals**

In `wildwest/src/main/java/com/tweeks/wildwest/entity/RedstoneGolemEntity.java`, replace the existing `registerGoals` method:
```java
@Override
protected void registerGoals() {
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(1, new com.tweeks.wildwest.entity.ai.RedstoneGolemGroundSlamGoal(this));
    this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
    this.goalSelector.addGoal(4, new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
    this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

    this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
}
```

(Priority slot 2 is reserved for the bomb-throw goal added in Task 6.)

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.entity.ai.RedstoneGolemGoalsTest"`
Expected: PASS.

- [ ] **Step 6: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ai/RedstoneGolemGroundSlamGoal.java \
        wildwest/src/main/java/com/tweeks/wildwest/entity/RedstoneGolemEntity.java \
        wildwest/src/test/java/com/tweeks/wildwest/entity/ai/RedstoneGolemGoalsTest.java
git commit -m "$(cat <<'EOF'
feat(wildwest): RedstoneGolem ground slam AI goal

1s wind-up + AOE damage (4 dmg, radius 4) + knockback (2.5) + 8s cooldown.
TNT_PRIMED windup, GENERIC_EXPLODE impact (no actual explosion call).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Redstone bomb projectile + renderer

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/projectile/RedstoneBombEntity.java`
- Create: `wildwest/src/main/java/com/tweeks/wildwest/client/RedstoneBombRenderer.java`
- Create: `wildwest/src/main/resources/assets/wildwest/textures/entity/redstone_bomb.png`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java` (register REDSTONE_BOMB)
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java` (register renderer)
- Test: `wildwest/src/test/java/com/tweeks/wildwest/entity/projectile/RedstoneBombEntityTest.java`

- [ ] **Step 1: Write the failing constants test**

File `wildwest/src/test/java/com/tweeks/wildwest/entity/projectile/RedstoneBombEntityTest.java`:
```java
package com.tweeks.wildwest.entity.projectile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedstoneBombEntityTest {

    @Test
    void constants_matchSpec() {
        assertEquals(3.0f, RedstoneBombEntity.EXPLOSION_RADIUS);
        assertEquals(100, RedstoneBombEntity.FUSE_TICKS);
        assertEquals(6.0f, RedstoneBombEntity.BASE_DAMAGE);
        assertEquals(1.2, RedstoneBombEntity.KNOCKBACK_STRENGTH);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.entity.projectile.RedstoneBombEntityTest"`
Expected: FAIL with `cannot find symbol class RedstoneBombEntity`.

- [ ] **Step 3: Implement the bomb entity**

File `wildwest/src/main/java/com/tweeks/wildwest/entity/projectile/RedstoneBombEntity.java`:
```java
package com.tweeks.wildwest.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class RedstoneBombEntity extends ThrowableProjectile {

    public static final float EXPLOSION_RADIUS = 3.0f;
    public static final int FUSE_TICKS = 100;
    public static final float BASE_DAMAGE = 6.0f;
    public static final double KNOCKBACK_STRENGTH = 1.2;

    private int fuse;

    public RedstoneBombEntity(EntityType<? extends RedstoneBombEntity> type, Level level) {
        super(type, level);
    }

    public RedstoneBombEntity(Level level, LivingEntity owner) {
        this(com.tweeks.wildwest.ModEntities.REDSTONE_BOMB.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getEyePosition().x, owner.getEyePosition().y - 0.2, owner.getEyePosition().z);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        super.tick();
        fuse++;
        if (fuse >= FUSE_TICKS && !level().isClientSide()) {
            detonate();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) {
            detonate();
        }
    }

    private void detonate() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        Entity owner = getOwner();
        AABB aabb = getBoundingBox().inflate(EXPLOSION_RADIUS);
        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, aabb,
            e -> e.isAlive() && e != owner);

        for (LivingEntity target : nearby) {
            double dist = target.position().distanceTo(position());
            float damage = BASE_DAMAGE * (float) Math.max(0.0, 1.0 - (dist / EXPLOSION_RADIUS));
            if (damage <= 0.0f) continue;
            target.hurt(serverLevel.damageSources().explosion(this, owner), damage);
            target.knockback(KNOCKBACK_STRENGTH,
                position().x - target.position().x,
                position().z - target.position().z);
        }

        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
            getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        for (int i = 0; i < 8; i++) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                getX() + (random.nextDouble() - 0.5),
                getY() + (random.nextDouble() - 0.5),
                getZ() + (random.nextDouble() - 0.5),
                1, 0, 0, 0, 0);
        }
        serverLevel.playSound(null, getX(), getY(), getZ(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 1.0F);
        discard();
    }
}
```

> If `ThrowableProjectile`'s `getDefaultGravity` is not the right override name in your NeoForge version, look at vanilla `Snowball` or `ThrownPotion` for the current API. The spec target is gravity ~0.04.

> `setOwner(LivingEntity)` accepts `Entity` in newer NeoForge versions — check the parameter type of `Projectile.setOwner` in your version.

- [ ] **Step 4: Register the entity type**

In `ModEntities.java`, alongside `REDSTONE_GOLEM`, add the import:
```java
import com.tweeks.wildwest.entity.projectile.RedstoneBombEntity;
```

Register:
```java
public static final DeferredHolder<EntityType<?>, EntityType<RedstoneBombEntity>> REDSTONE_BOMB =
    ENTITY_TYPES.register("redstone_bomb", () -> EntityType.Builder.<RedstoneBombEntity>of(
            RedstoneBombEntity::new, MobCategory.MISC)
        .sized(0.4f, 0.4f)
        .clientTrackingRange(4)
        .updateInterval(10)
        .build(ResourceKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "redstone_bomb"))));
```

- [ ] **Step 5: Implement the renderer**

File `wildwest/src/main/java/com/tweeks/wildwest/client/RedstoneBombRenderer.java`:
```java
package com.tweeks.wildwest.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.entity.projectile.RedstoneBombEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RedstoneBombRenderer extends EntityRenderer<RedstoneBombEntity> {

    private static final ResourceLocation TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/redstone_bomb.png");

    private final ItemRenderer itemRenderer;

    public RedstoneBombRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(RedstoneBombEntity entity, float entityYaw, float partialTicks, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.scale(0.5F, 0.5F, 0.5F);
        ItemStack stack = new ItemStack(Items.TNT);
        itemRenderer.renderStatic(stack, net.minecraft.world.item.ItemDisplayContext.GROUND,
            packedLight, OverlayTexture.NO_OVERLAY, pose, buffer, entity.level(), 0);
        pose.popPose();
        super.render(entity, entityYaw, partialTicks, pose, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RedstoneBombEntity entity) {
        return TEXTURE;
    }
}
```

> If `ItemRenderer.renderStatic` signature has shifted, look at how `CannonballEntity` or `BulletEntity` is rendered for the current correct call.

- [ ] **Step 6: Wire renderer in ClientSetup**

Where the other entity renderers are registered, add:
```java
event.registerEntityRenderer(ModEntities.REDSTONE_BOMB.get(), RedstoneBombRenderer::new);
```

- [ ] **Step 7: Create the placeholder texture**

```bash
mkdir -p wildwest/src/main/resources/assets/wildwest/textures/entity
magick -size 16x16 xc:'#cc0000' wildwest/src/main/resources/assets/wildwest/textures/entity/redstone_bomb.png
```

(Texture isn't used by the renderer as-shown — we render the TNT itemstack — but the file must exist for the renderer's `getTextureLocation` contract. Alternative: skip the file and have `getTextureLocation` return a vanilla path. Keep it for spec completeness.)

- [ ] **Step 8: Run test to verify it passes**

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.entity.projectile.RedstoneBombEntityTest"`
Expected: PASS.

- [ ] **Step 9: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 10: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/projectile/RedstoneBombEntity.java \
        wildwest/src/main/java/com/tweeks/wildwest/client/RedstoneBombRenderer.java \
        wildwest/src/main/java/com/tweeks/wildwest/ModEntities.java \
        wildwest/src/main/java/com/tweeks/wildwest/client/ClientSetup.java \
        wildwest/src/main/resources/assets/wildwest/textures/entity/redstone_bomb.png \
        wildwest/src/test/java/com/tweeks/wildwest/entity/projectile/RedstoneBombEntityTest.java
git commit -m "$(cat <<'EOF'
feat(wildwest): RedstoneBomb projectile + TNT-itemstack renderer

ThrowableProjectile with 0.04 gravity, 5s fuse, 3-block-radius AOE.
Distance-attenuated damage; owner exempted. No block damage.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Bomb throw AI goal

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/RedstoneGolemThrowBombGoal.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/entity/RedstoneGolemEntity.java` (wire goal)
- Modify: `wildwest/src/test/java/com/tweeks/wildwest/entity/ai/RedstoneGolemGoalsTest.java` (add constants test)

- [ ] **Step 1: Add the failing constants test**

In `RedstoneGolemGoalsTest.java`, add a new method:
```java
@Test
void throwBomb_constants_matchSpec() {
    assertEquals(12, RedstoneGolemThrowBombGoal.WIND_UP_TICKS);
    assertEquals(100, RedstoneGolemThrowBombGoal.COOLDOWN_TICKS);
    assertEquals(6.0, RedstoneGolemThrowBombGoal.MIN_RANGE);
    assertEquals(16.0, RedstoneGolemThrowBombGoal.MAX_RANGE);
    assertEquals(1.4f, RedstoneGolemThrowBombGoal.PROJECTILE_VELOCITY);
    assertEquals(2.0f, RedstoneGolemThrowBombGoal.PROJECTILE_INACCURACY);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.entity.ai.RedstoneGolemGoalsTest"`
Expected: FAIL with `cannot find symbol class RedstoneGolemThrowBombGoal`.

- [ ] **Step 3: Implement the goal**

File `wildwest/src/main/java/com/tweeks/wildwest/entity/ai/RedstoneGolemThrowBombGoal.java`:
```java
package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.RedstoneGolemEntity;
import com.tweeks.wildwest.entity.projectile.RedstoneBombEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class RedstoneGolemThrowBombGoal extends Goal {

    public static final int WIND_UP_TICKS = 12;
    public static final int COOLDOWN_TICKS = 100;
    public static final double MIN_RANGE = 6.0;
    public static final double MAX_RANGE = 16.0;
    public static final float PROJECTILE_VELOCITY = 1.4f;
    public static final float PROJECTILE_INACCURACY = 2.0f;

    private final RedstoneGolemEntity golem;
    private int cooldown;
    private int windUp;

    public RedstoneGolemThrowBombGoal(RedstoneGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        LivingEntity target = golem.getTarget();
        if (target == null || !target.isAlive()) return false;
        double distSqr = golem.distanceToSqr(target);
        if (distSqr < MIN_RANGE * MIN_RANGE || distSqr > MAX_RANGE * MAX_RANGE) return false;
        return golem.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        return windUp > 0;
    }

    @Override
    public void start() {
        windUp = WIND_UP_TICKS;
        golem.level().playSound(null, golem.getX(), golem.getY(), golem.getZ(),
            SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    @Override
    public void tick() {
        LivingEntity target = golem.getTarget();
        if (target != null) golem.getLookControl().setLookAt(target, 30.0F, 30.0F);
        windUp--;
        if (windUp <= 0) throwBomb();
    }

    private void throwBomb() {
        LivingEntity target = golem.getTarget();
        if (target == null) {
            cooldown = COOLDOWN_TICKS;
            return;
        }
        Vec3 from = golem.getEyePosition().add(0, -0.2, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 dir = to.subtract(from).normalize();

        RedstoneBombEntity bomb = new RedstoneBombEntity(golem.level(), golem);
        bomb.setPos(from.x, from.y, from.z);
        bomb.shoot(dir.x, dir.y + 0.2, dir.z, PROJECTILE_VELOCITY, PROJECTILE_INACCURACY);
        golem.level().addFreshEntity(bomb);

        golem.level().playSound(null, golem.getX(), golem.getY(), golem.getZ(),
            SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.0F, 0.8F);

        cooldown = COOLDOWN_TICKS;
    }
}
```

- [ ] **Step 4: Wire the goal into registerGoals**

In `RedstoneGolemEntity.registerGoals`, replace the existing body with:
```java
@Override
protected void registerGoals() {
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(1, new com.tweeks.wildwest.entity.ai.RedstoneGolemGroundSlamGoal(this));
    this.goalSelector.addGoal(2, new com.tweeks.wildwest.entity.ai.RedstoneGolemThrowBombGoal(this));
    this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
    this.goalSelector.addGoal(4, new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
    this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

    this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
}
```

- [ ] **Step 5: Run tests**

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.entity.ai.RedstoneGolemGoalsTest"`
Expected: both tests PASS.

- [ ] **Step 6: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/entity/ai/RedstoneGolemThrowBombGoal.java \
        wildwest/src/main/java/com/tweeks/wildwest/entity/RedstoneGolemEntity.java \
        wildwest/src/test/java/com/tweeks/wildwest/entity/ai/RedstoneGolemGoalsTest.java
git commit -m "$(cat <<'EOF'
feat(wildwest): RedstoneGolem bomb throw AI goal

Range 6-16, LOS-gated. 0.6s wind-up + 5s cooldown. Spawns RedstoneBombEntity
with ballistic arc + skeleton-tier inaccuracy.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Piston Gauntlet item + damage type

**Files:**
- Create: `wildwest/src/main/java/com/tweeks/wildwest/item/PistonGauntletItem.java`
- Create: `wildwest/src/main/resources/assets/wildwest/models/item/piston_gauntlet.json`
- Create: `wildwest/src/main/resources/assets/wildwest/textures/item/piston_gauntlet.png`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/WildWestDamageTypes.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/data/ModDamageTypeProvider.java`
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`
- Test: `wildwest/src/test/java/com/tweeks/wildwest/item/PistonGauntletItemTest.java`

- [ ] **Step 1: Write the failing constants test**

File `wildwest/src/test/java/com/tweeks/wildwest/item/PistonGauntletItemTest.java`:
```java
package com.tweeks.wildwest.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PistonGauntletItemTest {

    @Test
    void constants_matchSpec() {
        assertEquals(30, PistonGauntletItem.COOLDOWN_TICKS);
        assertEquals(4.0, PistonGauntletItem.RAY_DISTANCE);
        assertEquals(4.0f, PistonGauntletItem.HIT_DAMAGE);
        assertEquals(2.0, PistonGauntletItem.HIT_KNOCKBACK);
        assertEquals(1.5, PistonGauntletItem.SELF_LAUNCH_VELOCITY);
        assertEquals(250, PistonGauntletItem.DURABILITY);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.item.PistonGauntletItemTest"`
Expected: FAIL with `cannot find symbol class PistonGauntletItem`.

- [ ] **Step 3: Add the PISTON_PUNCH damage type key + accessor**

In `wildwest/src/main/java/com/tweeks/wildwest/WildWestDamageTypes.java`, add the key alongside others:
```java
public static final ResourceKey<DamageType> PISTON_PUNCH = ResourceKey.create(
    Registries.DAMAGE_TYPE,
    Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "piston_punch"));
```

Add the accessor:
```java
public static DamageSource pistonPunch(Entity attacker) {
    return new DamageSource(
        attacker.level().registryAccess()
            .lookupOrThrow(Registries.DAMAGE_TYPE)
            .getOrThrow(PISTON_PUNCH),
        attacker);
}
```

- [ ] **Step 4: Bootstrap the damage type in ModDamageTypeProvider**

In `wildwest/src/main/java/com/tweeks/wildwest/data/ModDamageTypeProvider.java`, add:
```java
ctx.register(WildWestDamageTypes.PISTON_PUNCH,
    new DamageType("wildwest.piston_punch", 0.1f));
```

- [ ] **Step 5: Regenerate the damage_type JSON files**

Run: `./gradlew :wildwest:runData`
Expected: BUILD SUCCESSFUL. The file `wildwest/src/generated/serverData/data/wildwest/damage_type/piston_punch.json` should be created.

- [ ] **Step 6: Implement the item class**

File `wildwest/src/main/java/com/tweeks/wildwest/item/PistonGauntletItem.java`:
```java
package com.tweeks.wildwest.item;

import com.tweeks.wildwest.WildWestDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class PistonGauntletItem extends Item {

    public static final int COOLDOWN_TICKS = 30;
    public static final double RAY_DISTANCE = 4.0;
    public static final float HIT_DAMAGE = 4.0f;
    public static final double HIT_KNOCKBACK = 2.0;
    public static final double SELF_LAUNCH_VELOCITY = 1.5;
    public static final int DURABILITY = 250;

    public PistonGauntletItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RAY_DISTANCE));

        BlockHitResult blockHit = level.clip(new ClipContext(eye, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        AABB rayAabb = new AABB(eye, end).inflate(0.5);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, eye, end,
            rayAabb, e -> e != player && e.isAlive());

        boolean hitEntity = entityHit != null
            && (blockHit.getType() == HitResult.Type.MISS
                || entityHit.getLocation().distanceToSqr(eye) < blockHit.getLocation().distanceToSqr(eye));

        if (hitEntity && entityHit.getEntity() instanceof LivingEntity target) {
            target.hurt(WildWestDamageTypes.pistonPunch(player), HIT_DAMAGE);
            target.knockback(HIT_KNOCKBACK, -look.x, -look.z);
        } else {
            player.setDeltaMovement(player.getDeltaMovement().add(
                -look.x * SELF_LAUNCH_VELOCITY,
                -look.y * SELF_LAUNCH_VELOCITY,
                -look.z * SELF_LAUNCH_VELOCITY));
            if (player instanceof ServerPlayer sp) sp.hurtMarked = true;
        }

        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 8; i++) {
                double t = i / 8.0;
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    eye.x + look.x * RAY_DISTANCE * t,
                    eye.y + look.y * RAY_DISTANCE * t,
                    eye.z + look.z * RAY_DISTANCE * t,
                    1, 0, 0, 0, 0);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.0F, 1.0F);

        player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN_TICKS);
        stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND
            ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);

        return InteractionResultHolder.success(stack);
    }
}
```

> The `getCooldowns().isOnCooldown(...)` / `addCooldown(...)` API takes `Item` in newer NeoForge versions, `ItemStack` in older ones. If the call fails, mirror `ReaperScytheItem.use(...)` for the exact form.

> `stack.hurtAndBreak(int, LivingEntity, EquipmentSlot)` is the current NeoForge signature. Older versions take `Consumer<LivingEntity>`. Mirror existing items if compile fails.

> `level.damageSources().explosion(...)` was used in the bomb — for pistonPunch we use the wildwest-defined static accessor instead. Both are valid.

- [ ] **Step 7: Register the item + add to creative tab**

In `wildwest/src/main/java/com/tweeks/wildwest/Registration.java`, alongside other items:
```java
import com.tweeks.wildwest.item.PistonGauntletItem;
```

```java
public static final DeferredItem<PistonGauntletItem> PISTON_GAUNTLET = ITEMS.registerItem(
    "piston_gauntlet",
    PistonGauntletItem::new,
    p -> p.stacksTo(1).durability(PistonGauntletItem.DURABILITY).rarity(Rarity.RARE));
```

In the creative tab `displayItems` block (the lambda), add after `REAPER_SCYTHE`:
```java
output.accept(PISTON_GAUNTLET.get());
```

- [ ] **Step 8: Create the item model JSON**

File `wildwest/src/main/resources/assets/wildwest/models/item/piston_gauntlet.json`:
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "wildwest:item/piston_gauntlet"
  }
}
```

- [ ] **Step 9: Create the placeholder item texture**

```bash
mkdir -p wildwest/src/main/resources/assets/wildwest/textures/item
magick -size 16x16 xc:'#7a7a7a' wildwest/src/main/resources/assets/wildwest/textures/item/piston_gauntlet.png
```

- [ ] **Step 10: Run all related tests + compile**

Run: `./gradlew :wildwest:test --tests "com.tweeks.wildwest.item.PistonGauntletItemTest"`
Expected: PASS.

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 11: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/PistonGauntletItem.java \
        wildwest/src/main/java/com/tweeks/wildwest/WildWestDamageTypes.java \
        wildwest/src/main/java/com/tweeks/wildwest/data/ModDamageTypeProvider.java \
        wildwest/src/main/java/com/tweeks/wildwest/Registration.java \
        wildwest/src/main/resources/assets/wildwest/models/item/piston_gauntlet.json \
        wildwest/src/main/resources/assets/wildwest/textures/item/piston_gauntlet.png \
        wildwest/src/generated/serverData/data/wildwest/damage_type/piston_punch.json \
        wildwest/src/test/java/com/tweeks/wildwest/item/PistonGauntletItemTest.java
git commit -m "$(cat <<'EOF'
feat(wildwest): PistonGauntlet item + piston_punch damage type

Right-click punches entity (4 dmg + knockback) OR rocket-jumps self when
no entity in range. 1.5s cooldown, 250 durability. New piston_punch damage
type registered via datagen.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: Loot table + translations

**Files:**
- Create: `wildwest/src/main/resources/data/wildwest/loot_table/entities/redstone_golem.json`
- Modify: `wildwest/src/main/resources/assets/wildwest/lang/en_us.json`

- [ ] **Step 1: Create the loot table JSON**

File `wildwest/src/main/resources/data/wildwest/loot_table/entities/redstone_golem.json`:
```json
{
  "type": "minecraft:entity",
  "pools": [
    {
      "rolls": 1.0,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "wildwest:piston_gauntlet"
        }
      ]
    },
    {
      "rolls": 1.0,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:redstone",
          "functions": [
            {
              "function": "minecraft:set_count",
              "count": { "type": "minecraft:uniform", "min": 5.0, "max": 9.0 }
            }
          ]
        }
      ]
    },
    {
      "rolls": 1.0,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:tnt",
          "functions": [
            {
              "function": "minecraft:set_count",
              "count": { "type": "minecraft:uniform", "min": 0.0, "max": 2.0 }
            }
          ]
        }
      ]
    }
  ]
}
```

- [ ] **Step 2: Add translation keys**

Open `wildwest/src/main/resources/assets/wildwest/lang/en_us.json`. Add the following keys alongside the existing entity / item entries (the file is a flat JSON object — insert in any order, but conventionally grouped by feature):
```json
"entity.wildwest.redstone_golem": "Redstone Golem",
"entity.wildwest.redstone_bomb": "Redstone Bomb",
"item.wildwest.piston_gauntlet": "Piston Gauntlet",
"item.wildwest.piston_gauntlet.tooltip.use": "Press right-click to punch.",
"item.wildwest.piston_gauntlet.tooltip.launch": "Aim down to rocket-jump.",
"death.attack.wildwest.piston_punch": "%1$s was punched into next week by %2$s",
"death.attack.wildwest.piston_punch.player": "%1$s was punched into next week by %2$s using %3$s",
"subtitles.entity.wildwest.redstone_golem.ambient": "Redstone Golem rumbles",
"subtitles.entity.wildwest.redstone_golem.hurt": "Redstone Golem hurts",
"subtitles.entity.wildwest.redstone_golem.death": "Redstone Golem breaks"
```

(Insert as comma-separated JSON members, not literally as shown. Take care not to leave a trailing comma if inserting at the end of the file.)

- [ ] **Step 3: Compile + run all tests (sanity check)**

Run: `./gradlew :wildwest:test`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 4: Commit**

```bash
git add wildwest/src/main/resources/data/wildwest/loot_table/entities/redstone_golem.json \
        wildwest/src/main/resources/assets/wildwest/lang/en_us.json
git commit -m "$(cat <<'EOF'
feat(wildwest): RedstoneGolem loot table + en_us translations

Guaranteed Piston Gauntlet, 5-9 redstone, 0-2 TNT. Death messages for
piston_punch. Boss bar / sound subtitles.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 9: Final integration smoke + tooltip

**Files:**
- Modify: `wildwest/src/main/java/com/tweeks/wildwest/item/PistonGauntletItem.java` (add `appendHoverText`)

- [ ] **Step 1: Add the tooltip override**

In `PistonGauntletItem.java`, add the method:
```java
@Override
public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
    tooltip.add(net.minecraft.network.chat.Component.translatable("item.wildwest.piston_gauntlet.tooltip.use").withStyle(net.minecraft.ChatFormatting.GRAY));
    tooltip.add(net.minecraft.network.chat.Component.translatable("item.wildwest.piston_gauntlet.tooltip.launch").withStyle(net.minecraft.ChatFormatting.GRAY));
}
```

> The 5-arg `appendHoverText` form on NeoForge 26.1.2: open `ReaperScytheItem.appendHoverText` and mirror the exact parameter list — there have been signature shifts. The form above (4 args + new `Item.TooltipContext`) is the current target.

- [ ] **Step 2: Compile**

Run: `./gradlew :wildwest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run all tests**

Run: `./gradlew :wildwest:test`
Expected: ALL PASS.

- [ ] **Step 4: Run the full module build**

Run: `./gradlew :wildwest:build`
Expected: BUILD SUCCESSFUL. Watch for any new compile warnings introduced by these changes.

- [ ] **Step 5: Commit**

```bash
git add wildwest/src/main/java/com/tweeks/wildwest/item/PistonGauntletItem.java
git commit -m "$(cat <<'EOF'
feat(wildwest): PistonGauntlet tooltip with translation keys

Two-line gray tooltip describing punch + rocket-jump modes.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 10: Bedrock parity note (optional, deferrable)

**Files:**
- Modify: `bedrock-out/wildwest/UNTRANSLATABLE.md` (if it exists; otherwise skip)

- [ ] **Step 1: Check if the Bedrock translator output exists**

Run: `ls bedrock-out/wildwest/UNTRANSLATABLE.md`

If absent (file not found): SKIP this task and commit nothing.

If present: continue to Step 2.

- [ ] **Step 2: Add an entry for the redstone golem construction**

Append to `bedrock-out/wildwest/UNTRANSLATABLE.md`:
```markdown

## Redstone Golem construction trigger (2026-05-25)

The Java-side Redstone Golem spawns via a NeoForge `EntityJoinLevelEvent` listener
that fires when a `PrimedTnt` joins the level over a T-shape of `redstone_block`.
Bedrock has no equivalent event hook for primed TNT entity spawns; the translator
should treat this construction recipe as not portable. The mob itself (entity,
attributes, AI goals) can be translated if/when the translator gains support for
the bomb-throwing custom goal.
```

- [ ] **Step 3: Commit**

```bash
git add bedrock-out/wildwest/UNTRANSLATABLE.md
git commit -m "$(cat <<'EOF'
docs(bedrock-out): note RedstoneGolem construction trigger untranslatable

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Final Verification Checklist

After all tasks above are complete:

- [ ] Run the full test suite: `./gradlew :wildwest:test`. All tests pass.
- [ ] Compile cleanly: `./gradlew :wildwest:compileJava`. No new warnings introduced.
- [ ] Build the JAR: `./gradlew :wildwest:build`. BUILD SUCCESSFUL.
- [ ] Check `git log --oneline -15` — you should have approximately 9–10 new commits, each focused on one task.
- [ ] (Deferred to user) Manual dev-client smoke: place the T-pattern, fight a golem, equip the gauntlet. Document failures.

## Notes for the Implementer

- **TDD discipline:** every task in this plan starts with a failing test and verifies it fails BEFORE writing implementation. Do NOT skip this — the failure verification is what proves the test is connected to the code under test.
- **Compile-not-test-fail issues:** the tests are constants smoke tests by design (matching the project's existing pattern in `ReaperScytheItemTest`). Real behavioral verification happens in the deferred dev-client manual smoke.
- **NeoForge API drift:** several spots in this plan flag "if this signature has changed, mirror the sibling file." Take that seriously — do NOT guess; look at the actual sibling and copy the exact form.
- **Frequent commits:** commit per task. Do not bundle. Each commit should compile and pass the tests added so far.
- **No scope creep:** if you notice unrelated code that could be improved, leave it alone for this PR. Per the project's CLAUDE.md / memory, scope discipline is required.
