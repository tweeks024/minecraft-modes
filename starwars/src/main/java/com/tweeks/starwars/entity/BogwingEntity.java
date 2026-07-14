package com.tweeks.starwars.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Bogwing: a swamp flapper. AMBIENT scenery in the bat mold — the erratic
 * flutter is the decompiled 26.1 {@code Bat}'s steering verbatim (random
 * nearby target block, signum-chase), minus the roost/resting state.
 * Passive, near-silent, drops a single feather.
 */
public class BogwingEntity extends AmbientCreature {

    public static final double MAX_HEALTH = 6.0;

    private @Nullable BlockPos targetPosition;

    public BogwingEntity(EntityType<? extends BogwingEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH);
    }

    @Override
    public void tick() {
        super.tick();
        // Airborne float: damp vertical drift (Bat#tick).
        this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.6, 1.0));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Bat's flutter, sans resting: chase a random nearby air block,
        // re-rolled when reached, occupied, or on a whim.
        if (this.targetPosition != null
                && (!level.isEmptyBlock(this.targetPosition)
                    || this.targetPosition.getY() <= level.getMinY())) {
            this.targetPosition = null;
        }
        if (this.targetPosition == null
                || this.random.nextInt(30) == 0
                || this.targetPosition.closerToCenterThan(this.position(), 2.0)) {
            this.targetPosition = BlockPos.containing(
                this.getX() + this.random.nextInt(7) - this.random.nextInt(7),
                this.getY() + this.random.nextInt(6) - 2.0,
                this.getZ() + this.random.nextInt(7) - this.random.nextInt(7));
        }

        double dx = this.targetPosition.getX() + 0.5 - this.getX();
        double dy = this.targetPosition.getY() + 0.1 - this.getY();
        double dz = this.targetPosition.getZ() + 0.5 - this.getZ();
        Vec3 movement = this.getDeltaMovement();
        Vec3 newMovement = movement.add(
            (Math.signum(dx) * 0.5 - movement.x) * 0.1F,
            (Math.signum(dy) * 0.7F - movement.y) * 0.1F,
            (Math.signum(dz) * 0.5 - movement.z) * 0.1F);
        this.setDeltaMovement(newMovement);
        float yRotD = (float) (Mth.atan2(newMovement.z, newMovement.x) * 180.0F / (float) Math.PI) - 90.0F;
        float rotDiff = Mth.wrapDegrees(yRotD - this.getYRot());
        this.zza = 0.5F;
        this.setYRot(this.getYRot() + rotDiff);
    }

    // ---------- bat-family physicality ----------

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    // ---------- sounds (silent-ish) ----------

    @Override
    protected float getSoundVolume() {
        return 0.1F;
    }

    @Override
    public @Nullable SoundEvent getAmbientSound() {
        return null;   // no idle chatter
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }
}
