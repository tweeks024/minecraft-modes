package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.WildWestDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class BulletEntity extends AbstractArrow {

    public static final int MAX_LIFE_TICKS = 12;
    public static final float DEFAULT_DAMAGE = 9.0F;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level, LivingEntity shooter) {
        super(type, shooter, level, ItemStack.EMPTY, null);
        this.pickup = Pickup.DISALLOWED;
        this.setBaseDamage(DEFAULT_DAMAGE);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.AIR);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > MAX_LIFE_TICKS) {
            this.discard();
            return;
        }
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.CRIT,
                this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide()) return;
        if (result.getEntity() instanceof LivingEntity target) {
            target.invulnerableTime = 0;
            target.hurtServer((ServerLevel) this.level(),
                WildWestDamageTypes.gunshot(this.getOwner() == null ? this : this.getOwner()),
                DEFAULT_DAMAGE);
        }
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }
}
