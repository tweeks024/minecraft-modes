package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.RedstoneGolemEntity;
import com.tweeks.wildwest.entity.projectile.RedstoneBombEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Ranged bomb throw — Redstone Golem launches a {@link RedstoneBombEntity}
 * at its target with a ballistic arc. Activates when the target is within
 * {@link #MIN_RANGE}-{@link #MAX_RANGE} blocks and has line-of-sight. After
 * a {@link #WIND_UP_TICKS}-tick wind-up (CREEPER_PRIMED sound), the bomb is
 * shot with velocity {@link #PROJECTILE_VELOCITY} and a slight upward bias
 * for the arc. Cooldown {@link #COOLDOWN_TICKS}.
 */
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
