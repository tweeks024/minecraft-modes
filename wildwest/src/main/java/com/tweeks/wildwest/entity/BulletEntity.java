package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.WildWestDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
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

    // 26.1.2 AbstractArrow has setBaseDamage but no public getBaseDamage,
    // so we mirror the value into our own field for the onHitEntity readback.
    private float damage = DEFAULT_DAMAGE;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level, LivingEntity shooter) {
        super(type, shooter, level, ItemStack.EMPTY, null);
        this.pickup = Pickup.DISALLOWED;
        this.setBaseDamage(DEFAULT_DAMAGE);
    }

    public void setDamage(float damage) {
        this.damage = damage;
        this.setBaseDamage(damage);
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
            // Read damage from the AbstractArrow field set by setBaseDamage()
            // so callers can tune per-shot damage (rifle vs pistol vs captain).
            // Fall back to the no-attacker source when the shooter has
            // chunk-unloaded — projectile-as-attacker breaks kill credit.
            Entity owner = this.getOwner();
            net.minecraft.world.damagesource.DamageSource source;
            if (owner != null) {
                source = WildWestDamageTypes.gunshot(owner);
            } else {
                // Owner has unloaded; mirror meteor/cannonball pattern with an
                // attacker-less variant of the source. Build inline because
                // there's no dedicated factory yet and this is the sole caller.
                source = new net.minecraft.world.damagesource.DamageSource(
                    this.level().registryAccess()
                        .lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getOrThrow(WildWestDamageTypes.GUNSHOT));
            }
            target.hurtServer((ServerLevel) this.level(), source, this.damage);
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
