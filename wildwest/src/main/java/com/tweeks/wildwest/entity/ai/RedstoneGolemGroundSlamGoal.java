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

/**
 * Ground slam — Redstone Golem's signature AOE attack. Triggers when a target
 * is within {@link #TRIGGER_RADIUS} blocks. After a 1s wind-up (TNT_PRIMED
 * sound + smoke), deals {@link #DAMAGE} to all living entities within
 * {@link #DAMAGE_RADIUS} (excluding the golem itself), applies knockback of
 * strength {@link #KNOCKBACK_STRENGTH} plus a vertical lift, and emits
 * redstone-dust + explosion particles. Cooldown {@link #COOLDOWN_TICKS}.
 */
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
