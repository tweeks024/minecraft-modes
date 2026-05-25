package com.tweeks.wildwest.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Redstone bomb thrown by the Redstone Golem. On block-hit, entity-hit, or
 * after a 5-second fuse, detonates with distance-attenuated AoE damage in a
 * 3-block radius. Strict anti-grief design: NO {@code level.explode(...)}
 * call — only entity damage + knockback + cosmetic particles/sound.
 *
 * <p>The owner (the golem itself) is exempted from self-damage; this matches
 * the splash-exclusion pattern in {@link TaintedVialEntity}.
 *
 * <p>Rendered via the vanilla {@link net.minecraft.client.renderer.entity.ThrownItemRenderer}
 * keyed off {@link #getDefaultItem()}; mirrors {@link MeteorEntity} and
 * {@link CannonballEntity}.
 */
public class RedstoneBombEntity extends ThrowableItemProjectile {

    public static final float EXPLOSION_RADIUS = 3.0f;
    public static final int FUSE_TICKS = 100; // 5 seconds @ 20 tps
    public static final float BASE_DAMAGE = 6.0f;
    public static final double KNOCKBACK_STRENGTH = 1.2;

    private int fuse;

    public RedstoneBombEntity(EntityType<? extends RedstoneBombEntity> type, Level level) {
        super(type, level);
    }

    public RedstoneBombEntity(Level level, LivingEntity owner) {
        this(com.tweeks.wildwest.ModEntities.REDSTONE_BOMB.get(), level);
        this.setOwner(owner);
        Vec3 eye = owner.getEyePosition();
        this.setPos(eye.x, eye.y - 0.2, eye.z);
    }

    @Override
    protected Item getDefaultItem() {
        // Renderer shows this item as the projectile sprite (TNT for now).
        return Items.TNT;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && ++this.fuse >= FUSE_TICKS) {
            detonate(this.position());
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(this.level() instanceof ServerLevel)) return;
        detonate(result.getLocation());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(this.level() instanceof ServerLevel)) return;
        detonate(result.getLocation());
    }

    private void detonate(Vec3 center) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        if (this.isRemoved()) return;

        Entity owner = this.getOwner();
        Entity self = this;
        AABB aabb = AABB.ofSize(center, EXPLOSION_RADIUS * 2, EXPLOSION_RADIUS * 2, EXPLOSION_RADIUS * 2);
        List<LivingEntity> nearby = sl.getEntitiesOfClass(LivingEntity.class, aabb,
            e -> e != self && e != owner && e.isAlive());

        for (LivingEntity target : nearby) {
            double dist = target.position().distanceTo(center);
            if (dist > EXPLOSION_RADIUS) continue;
            float damage = BASE_DAMAGE * (float) Math.max(0.0, 1.0 - (dist / EXPLOSION_RADIUS));
            if (damage <= 0.0f) continue;
            target.invulnerableTime = 0;
            target.hurtServer(sl, sl.damageSources().explosion(this, owner), damage);
            target.knockback(KNOCKBACK_STRENGTH,
                center.x - target.position().x,
                center.z - target.position().z);
        }

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
            center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
        for (int i = 0; i < 8; i++) {
            sl.sendParticles(ParticleTypes.EXPLOSION,
                center.x + (this.random.nextDouble() - 0.5),
                center.y + (this.random.nextDouble() - 0.5),
                center.z + (this.random.nextDouble() - 0.5),
                1, 0.0, 0.0, 0.0, 0.0);
        }
        sl.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
            SoundSource.HOSTILE, 1.0F, 1.0F);

        this.discard();
    }
}
