package com.tweeks.wildwest.entity.projectile;

import com.tweeks.wildwest.WildWestDamageTypes;
import net.minecraft.core.BlockPos;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Cannon shell projectile. On entity hit: 15 direct damage. On any impact: 4-block
 * AoE for 6 damage to other living entities (the directly-hit entity is excluded
 * from AoE — meteor pattern; we do NOT rely on invulnerableTime because
 * explosion-typed damage can bypass i-frames in some cases).
 *
 * <p>No block destruction. No magma. No fire spread. Pure entity damage +
 * explosion sound + smoke particles.
 */
public class CannonballEntity extends ThrowableItemProjectile {

    public static final int DIRECT_DAMAGE = 15;
    public static final int AOE_DAMAGE = 6;
    public static final double AOE_RADIUS = 4.0;

    public CannonballEntity(EntityType<? extends CannonballEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_NUGGET;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(this.level() instanceof ServerLevel sl)) return;

        Entity directlyHit = result.getEntity();
        if (directlyHit instanceof LivingEntity living) {
            Entity owner = this.getOwner();
            living.invulnerableTime = 0;
            living.hurtServer(sl,
                owner != null
                    ? WildWestDamageTypes.cannonball(owner)
                    : WildWestDamageTypes.cannonballAoe(sl),
                DIRECT_DAMAGE);
        }

        applyImpact(sl, result.getLocation(), directlyHit);
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(this.level() instanceof ServerLevel sl)) return;
        applyImpact(sl, result.getLocation(), null);
        this.discard();
    }

    private void applyImpact(ServerLevel sl, Vec3 impactLocation, Entity directlyHitEntity) {
        BlockPos impactPos = BlockPos.containing(impactLocation);

        // Capture 'this' as Entity so the != comparison is type-compatible with LivingEntity
        // (mirrors MeteorEntity#applyImpact).
        Entity self = this;
        AABB aoeBox = AABB.ofSize(impactLocation, AOE_RADIUS * 2, AOE_RADIUS * 2, AOE_RADIUS * 2);
        List<LivingEntity> nearby = sl.getEntitiesOfClass(LivingEntity.class, aoeBox,
            e -> e != self && e.isAlive() && e != directlyHitEntity);

        // Build pure candidate list, delegate to CannonballImpactLogic.victims for the radius filter.
        List<CannonballImpactLogic.Candidate> candidates = new ArrayList<>(nearby.size());
        var byId = new java.util.HashMap<String, LivingEntity>();
        for (LivingEntity le : nearby) {
            String id = le.getUUID().toString();
            candidates.add(new CannonballImpactLogic.Candidate(id, le.getX(), le.getY(), le.getZ()));
            byId.put(id, le);
        }
        var victimIds = CannonballImpactLogic.victims(
            impactLocation.x, impactLocation.y, impactLocation.z,
            AOE_RADIUS, candidates, null /* direct hit already excluded above */);
        for (String vid : victimIds) {
            LivingEntity v = byId.get(vid);
            if (v != null) {
                // Clear i-frames so AoE lands even if the victim was recently
                // hit by another source. Direct-hit branch already does this;
                // class doc explicitly says we should not rely on invulnerableTime.
                v.invulnerableTime = 0;
                v.hurtServer(sl, WildWestDamageTypes.cannonballAoe(sl), AOE_DAMAGE);
            }
        }

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
            impactLocation.x, impactLocation.y, impactLocation.z,
            1, 0.0, 0.0, 0.0, 0.0);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE,
            impactLocation.x, impactLocation.y, impactLocation.z,
            12, 0.5, 0.3, 0.5, 0.02);
        sl.playSound(null, impactPos, SoundEvents.GENERIC_EXPLODE.value(),
            SoundSource.HOSTILE, 1.0f, 0.9f);
    }
}
