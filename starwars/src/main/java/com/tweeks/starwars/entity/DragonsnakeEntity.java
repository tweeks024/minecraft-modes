package com.tweeks.starwars.entity;

import com.tweeks.starwars.faction.SwCombatant;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * Dragonsnake: a swamp ambush predator (the thing that ate R2 on Dagobah).
 * NEUTRAL in the faction war ({@link SwCombatant} marker) but aggressive
 * in its element: an aquatic hostile that swims via
 * {@link WaterBoundPathNavigation} + a fish-style move control (mirrored
 * from the decompiled 26.1 AbstractFish/Guardian), and only targets
 * players who are touching water. Out of water it flops — Guardian-style
 * hops, biased toward any nearby water it can find.
 */
public class DragonsnakeEntity extends Monster implements SwCombatant {

    public static final double MAX_HEALTH = 24.0;
    public static final double ATTACK_DAMAGE = 5.0;
    public static final double MOVEMENT_SPEED = 0.7;

    public DragonsnakeEntity(EntityType<? extends DragonsnakeEntity> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.moveControl = new DragonsnakeMoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    public SwFaction getFaction() { return SwFaction.NEUTRAL; }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0, 40));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Only players touching water are prey — and the hunt is called off
        // the moment they climb out.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
            this, Player.class, 10, true, false, (target, level) -> target.isInWater()) {
            @Override
            public boolean canContinueToUse() {
                LivingEntity current = this.mob.getTarget();
                return current != null && current.isInWater() && super.canContinueToUse();
            }
        });
    }

    /**
     * Water spawn rule (registered with {@code SpawnPlacementTypes.IN_WATER}
     * in StarWarsMod): mirrors the Guardian's — hostile-difficulty world and
     * actually in water, spawners exempt from the fluid check.
     */
    public static boolean checkDragonsnakeSpawnRules(EntityType<DragonsnakeEntity> type,
                                                     ServerLevelAccessor level,
                                                     EntitySpawnReason reason,
                                                     BlockPos pos,
                                                     RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
            && (EntitySpawnReason.isSpawner(reason)
                || level.getFluidState(pos).is(FluidTags.WATER));
    }

    // ---------- in-water locomotion (fish-style) ----------

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        // AbstractFish#travelInWater: self-propelled glide with drag; sink
        // gently when idle.
        this.moveRelative(0.01F, input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
        if (this.getTarget() == null) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
        }
    }

    @Override
    public void aiStep() {
        if (this.isInWater()) {
            // Aquatic: never drowns.
            this.setAirSupply(this.getMaxAirSupply());
        } else if (this.onGround() && this.verticalCollision) {
            // Beached: flop back toward water (Guardian/AbstractFish hop,
            // biased toward the nearest water block found by a short scan).
            Vec3 bias = this.findWaterwardHop();
            this.setDeltaMovement(this.getDeltaMovement().add(bias.x, 0.4, bias.z));
            this.setYRot(this.random.nextFloat() * 360.0F);
            this.setOnGround(false);
            this.needsSync = true;
            this.playSound(SoundEvents.GUARDIAN_FLOP, 1.0F, 1.0F);
        }
        super.aiStep();
    }

    /** Horizontal hop impulse: toward nearby water if any, else random. */
    private Vec3 findWaterwardHop() {
        BlockPos origin = this.blockPosition();
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos probe = origin.offset(
                this.random.nextInt(17) - 8,
                2 - this.random.nextInt(6),
                this.random.nextInt(17) - 8);
            if (this.level().getFluidState(probe).is(FluidTags.WATER)) {
                Vec3 toward = Vec3.atCenterOf(probe).subtract(this.position());
                Vec3 flat = new Vec3(toward.x, 0.0, toward.z);
                if (flat.lengthSqr() > 1.0E-4) {
                    return flat.normalize().scale(0.3);
                }
            }
        }
        return new Vec3(
            (this.random.nextFloat() * 2.0F - 1.0F) * 0.3F,
            0.0,
            (this.random.nextFloat() * 2.0F - 1.0F) * 0.3F);
    }

    // ---------- sounds ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWater() ? SoundEvents.AXOLOTL_IDLE_WATER : SoundEvents.AXOLOTL_IDLE_AIR;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AXOLOTL_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AXOLOTL_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.6F;
    }

    /**
     * Fish-style move control (mirrors the decompiled
     * {@code AbstractFish.FishMoveControl}): buoyancy nudge while
     * submerged, lerped speed, and free 3-D steering toward the wanted
     * position while the navigation is running.
     */
    private static class DragonsnakeMoveControl extends MoveControl {
        private final DragonsnakeEntity snake;

        DragonsnakeMoveControl(DragonsnakeEntity snake) {
            super(snake);
            this.snake = snake;
        }

        @Override
        public void tick() {
            if (this.snake.isEyeInFluid(FluidTags.WATER)) {
                this.snake.setDeltaMovement(this.snake.getDeltaMovement().add(0.0, 0.005, 0.0));
            }

            if (this.operation == MoveControl.Operation.MOVE_TO && !this.snake.getNavigation().isDone()) {
                float targetSpeed = (float) (this.speedModifier
                    * this.snake.getAttributeValue(Attributes.MOVEMENT_SPEED));
                this.snake.setSpeed(Mth.lerp(0.125F, this.snake.getSpeed(), targetSpeed));
                double xd = this.wantedX - this.snake.getX();
                double yd = this.wantedY - this.snake.getY();
                double zd = this.wantedZ - this.snake.getZ();
                if (yd != 0.0) {
                    double dd = Math.sqrt(xd * xd + yd * yd + zd * zd);
                    this.snake.setDeltaMovement(this.snake.getDeltaMovement()
                        .add(0.0, this.snake.getSpeed() * (yd / dd) * 0.1, 0.0));
                }
                if (xd != 0.0 || zd != 0.0) {
                    float yRotD = (float) (Mth.atan2(zd, xd) * 180.0F / (float) Math.PI) - 90.0F;
                    this.snake.setYRot(this.rotlerp(this.snake.getYRot(), yRotD, 90.0F));
                    this.snake.yBodyRot = this.snake.getYRot();
                }
            } else {
                this.snake.setSpeed(0.0F);
            }
        }
    }
}
