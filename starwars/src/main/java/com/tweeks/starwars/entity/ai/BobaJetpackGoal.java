package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.BobaFettEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Jetpack burst: when the target is 6-16 blocks away and Boba is on the
 * ground, rocket toward it in a high arc with flame particles and
 * slow-falling for the landing. 140-tick cooldown.
 */
public class BobaJetpackGoal extends Goal {

    public static final double MIN_RANGE = 6.0;
    public static final double MAX_RANGE = 16.0;
    public static final int COOLDOWN_TICKS = 140;
    public static final double HORIZONTAL_SPEED = 1.0;
    public static final double VERTICAL_BOOST = 0.8;

    private final BobaFettEntity boba;
    private int cooldown;

    public BobaJetpackGoal(BobaFettEntity boba) {
        this.boba = boba;
        this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        LivingEntity target = boba.getTarget();
        if (target == null || !target.isAlive() || !boba.onGround()) return false;
        double dist = boba.distanceTo(target);
        return dist >= MIN_RANGE && dist <= MAX_RANGE;
    }

    @Override
    public void start() {
        LivingEntity target = boba.getTarget();
        if (target == null) return;
        Vec3 toTarget = target.position().subtract(boba.position());
        Vec3 flat = new Vec3(toTarget.x, 0, toTarget.z).normalize().scale(HORIZONTAL_SPEED);
        boba.setDeltaMovement(flat.x, VERTICAL_BOOST, flat.z);
        boba.hurtMarked = true;
        boba.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0));
        if (boba.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME,
                boba.getX(), boba.getY() + 0.8, boba.getZ(), 20, 0.2, 0.3, 0.2, 0.02);
        }
        this.cooldown = COOLDOWN_TICKS;
    }

    @Override
    public boolean canContinueToUse() { return false; }
}
