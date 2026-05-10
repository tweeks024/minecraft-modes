package com.tweeks.wildwest.entity.projectile;

import com.tweeks.wildwest.WildWestDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Meteor projectile. Used in two flavors:
 *  <ul>
 *      <li>Herobrine summons — spawned 30 blocks above ground, falls straight
 *          down via gravity. {@code directHitDamage} stays at default 6.</li>
 *      <li>Meteor Staff — spawned at player eye, velocity = look × 1.5,
 *          falls in an arc. {@code directHitDamage} set to 20 for 10-heart hits.</li>
 *  </ul>
 *
 * <p>On impact (block or entity) the meteor:
 *  <ol>
 *      <li>Replaces the impact block with magma (unless dragon-immune)</li>
 *      <li>Sets adjacent flammable air to fire</li>
 *      <li>Deals AoE damage to nearby entities (excluding any directly-hit entity)</li>
 *      <li>Plays explosion sound + lava/smoke particles</li>
 *  </ol>
 *
 * <p>NO actual {@code level.explode(...)} call — the design forbids block
 * destruction beyond the single impact tile.
 */
public class MeteorEntity extends ThrowableItemProjectile {

    public static final int DEFAULT_DIRECT_DAMAGE = 6;
    public static final int AOE_DAMAGE = 6;
    public static final double AOE_RADIUS = 2.0;

    private int directHitDamage = DEFAULT_DIRECT_DAMAGE;

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIRE_CHARGE;
    }

    public void setDirectHitDamage(int damage) {
        this.directHitDamage = damage;
    }

    public int getDirectHitDamage() {
        return this.directHitDamage;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(this.level() instanceof ServerLevel sl)) return;

        Entity directlyHit = result.getEntity();
        if (directlyHit instanceof LivingEntity living) {
            living.invulnerableTime = 0;
            living.hurtServer(sl,
                WildWestDamageTypes.meteor(this.getOwner() == null ? this : this.getOwner()),
                (float) this.directHitDamage);
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

    /**
     * @param sl              the server level
     * @param impactLocation  precise impact point
     * @param directlyHitEntity entity to exclude from AoE (or {@code null} for
     *                          block hits)
     */
    private void applyImpact(ServerLevel sl, Vec3 impactLocation, Entity directlyHitEntity) {
        BlockPos impactPos = BlockPos.containing(impactLocation);
        BlockState impactState = sl.getBlockState(impactPos);

        boolean replace = MeteorImpactLogic.shouldReplaceWithMagma(
            impactState.isAir(),
            impactState.is(BlockTags.DRAGON_IMMUNE));

        if (replace) {
            sl.setBlockAndUpdate(impactPos, Blocks.MAGMA_BLOCK.defaultBlockState());
        }

        // Adjacent fire (4-neighborhood horizontal).
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = impactPos.relative(dir);
            if (sl.getBlockState(neighbor).isAir()
                && BaseFireBlock.canBePlacedAt(sl, neighbor, Direction.UP)) {
                sl.setBlockAndUpdate(neighbor, Blocks.FIRE.defaultBlockState());
            }
        }

        // AoE: damage other entities; explicitly exclude the directly-hit entity
        // (do NOT rely on invulnerableTime — fire-typed sources can bypass i-frames).
        // Capture 'this' as Entity so the != comparison is type-compatible with LivingEntity.
        Entity self = this;
        AABB aoeBox = AABB.ofSize(impactLocation, AOE_RADIUS * 2, AOE_RADIUS * 2, AOE_RADIUS * 2);
        List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, aoeBox,
            e -> e != directlyHitEntity && e != self && e.position().distanceTo(impactLocation) <= AOE_RADIUS);
        for (LivingEntity victim : victims) {
            victim.hurtServer(sl, WildWestDamageTypes.meteorAoe(sl), (float) AOE_DAMAGE);
        }

        // Particles + sound.
        sl.sendParticles(ParticleTypes.LAVA,
            impactLocation.x, impactLocation.y, impactLocation.z,
            24, 0.5, 0.3, 0.5, 0.0);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE,
            impactLocation.x, impactLocation.y, impactLocation.z,
            16, 0.5, 0.3, 0.5, 0.02);
        sl.playSound(null, impactPos, SoundEvents.GENERIC_EXPLODE.value(),
            SoundSource.HOSTILE, 1.0f, 0.8f);
    }
}
