package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.SwMob;
import com.tweeks.starwars.faction.AlignmentEvents;
import com.tweeks.starwars.faction.SwCombatant;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;

/**
 * Leia's "Rebel Rally": when combat is happening nearby and the 12s cadence
 * has elapsed, pulse Resistance I + Regeneration I (8s) to every eligible
 * ally within 12 blocks — Light-faction mobs (Leia included) and strictly
 * Light-aligned players (score > 0). Eligibility/cadence math lives in
 * {@link RallyMath}. No goal flags: the rally is an aura and must not
 * suppress Leia's own blaster goal.
 */
public class LeiaRallyGoal extends Goal {

    private final SwMob mob;
    private long lastPulseGameTime = -RallyMath.RALLY_INTERVAL_TICKS;

    public LeiaRallyGoal(SwMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!(mob.level() instanceof ServerLevel sl)) return false;
        if (!RallyMath.isReady(sl.getGameTime(), lastPulseGameTime)) return false;
        return combatNearby(sl);
    }

    @Override
    public boolean canContinueToUse() {
        return false; // one-shot pulse; start() does all the work
    }

    @Override
    public void start() {
        ServerLevel sl = (ServerLevel) mob.level();
        for (LivingEntity ally : findEligibleAllies(sl)) {
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                RallyMath.RALLY_DURATION_TICKS, 0));
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                RallyMath.RALLY_DURATION_TICKS, 0));
        }
        // Particle ring at the rally radius edge, 24 points, Y + 1.
        for (int i = 0; i < 24; i++) {
            double angle = (i / 24.0) * Math.PI * 2.0;
            sl.sendParticles(ParticleTypes.END_ROD,
                mob.getX() + Math.cos(angle) * RallyMath.RALLY_RADIUS,
                mob.getY() + 1.0,
                mob.getZ() + Math.sin(angle) * RallyMath.RALLY_RADIUS,
                1, 0.0, 0.0, 0.0, 0.0);
        }
        sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
            SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.8f, 1.4f);
        this.lastPulseGameTime = sl.getGameTime();
    }

    /** Combat nearby = Leia has a target, or an eligible ally mob does. */
    private boolean combatNearby(ServerLevel sl) {
        if (mob.getTarget() != null) return true;
        for (LivingEntity ally : findEligibleAllies(sl)) {
            if (ally instanceof Mob m && m.getTarget() != null) return true;
        }
        return false;
    }

    private List<LivingEntity> findEligibleAllies(ServerLevel sl) {
        return sl.getEntitiesOfClass(LivingEntity.class,
            mob.getBoundingBox().inflate(RallyMath.RALLY_RADIUS),
            e -> RallyMath.isWithinRadius(e.distanceToSqr(mob)) && isEligible(e));
    }

    private static boolean isEligible(LivingEntity e) {
        if (e instanceof SwCombatant c) return RallyMath.isEligibleFaction(c.getFaction());
        if (e instanceof Player p) return RallyMath.isEligibleScore(AlignmentEvents.getScore(p));
        return false;
    }
}
