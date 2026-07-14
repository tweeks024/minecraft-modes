package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.YodaEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Yoda's Force acrobatics: when the target is {@link #MIN_RANGE}-{@link
 * #MAX_RANGE} blocks out and he's grounded, spring at it in a shallow arc
 * (the {@link LukeLeapGoal} pattern, tuned smaller and snappier for the
 * little green whirlwind). Slow Falling covers the landing.
 */
public class YodaLeapGoal extends Goal {

    public static final double MIN_RANGE = 4.0;
    public static final double MAX_RANGE = 10.0;
    public static final int COOLDOWN_TICKS = 80;
    public static final double HORIZONTAL_SPEED = 0.85;
    public static final double VERTICAL_BOOST = 0.5;

    private final YodaEntity yoda;
    private int cooldown;

    public YodaLeapGoal(YodaEntity yoda) {
        this.yoda = yoda;
        this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        LivingEntity target = yoda.getTarget();
        if (target == null || !target.isAlive() || !yoda.onGround()) return false;
        double dist = yoda.distanceTo(target);
        return dist >= MIN_RANGE && dist <= MAX_RANGE;
    }

    @Override
    public void start() {
        LivingEntity target = yoda.getTarget();
        if (target == null) return;
        Vec3 toTarget = target.position().subtract(yoda.position());
        Vec3 flat = new Vec3(toTarget.x, 0, toTarget.z).normalize().scale(HORIZONTAL_SPEED);
        yoda.setDeltaMovement(flat.x, VERTICAL_BOOST, flat.z);
        // Slow Falling for the flight (see LukeLeapGoal's note: assigning
        // fallDistance at launch is ineffective).
        yoda.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0));
        this.cooldown = COOLDOWN_TICKS;
    }

    @Override
    public boolean canContinueToUse() { return false; }
}
