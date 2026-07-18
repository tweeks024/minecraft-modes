package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.StarWarsDamageTypes;
import com.tweeks.starwars.entity.PalpatineEntity;
import com.tweeks.starwars.faction.SwCombatant;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Unlimited power: the Emperor's signature Force lightning. When the target is
 * {@value #MIN_RANGE}-{@value #MAX_RANGE} blocks away with line of sight,
 * Palpatine roots in place and pours out arcing lightning for
 * {@link #ZAP_DURATION_TICKS} ticks — {@link StarWarsDamageTypes#forceLightning}
 * damage every {@link #DAMAGE_INTERVAL} ticks that chains onto the nearest few
 * living things (never his own Imperials), rendered as {@link
 * ParticleTypes#ELECTRIC_SPARK} arcs from his hands, the same visual the
 * player's dark-side lightning power uses. A long cooldown keeps it a dreaded
 * burst rather than a constant beam.
 */
public class PalpatineLightningGoal extends Goal {

    public static final int ZAP_DURATION_TICKS = 50;
    public static final int COOLDOWN_TICKS = 140;
    public static final int DAMAGE_INTERVAL = 10;
    public static final double MIN_RANGE = 4.0;
    public static final double MAX_RANGE = 18.0;
    public static final float DAMAGE = 3.0F;
    public static final int CHAIN_TARGETS = 3;
    public static final double CHAIN_RADIUS = 8.0;

    private final PalpatineEntity emperor;
    private int zapTicks;
    private int cooldown;

    public PalpatineLightningGoal(PalpatineEntity emperor) {
        this.emperor = emperor;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        LivingEntity target = emperor.getTarget();
        if (target == null || !target.isAlive()) return false;
        double dist = emperor.distanceTo(target);
        return dist >= MIN_RANGE && dist <= MAX_RANGE
            && emperor.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = emperor.getTarget();
        return zapTicks > 0 && target != null && target.isAlive()
            && emperor.distanceTo(target) <= MAX_RANGE + 3.0;
    }

    @Override
    public void start() {
        this.zapTicks = ZAP_DURATION_TICKS;
        this.emperor.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.zapTicks = 0;
        this.cooldown = COOLDOWN_TICKS;
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        LivingEntity target = emperor.getTarget();
        if (target == null) return;
        emperor.getLookControl().setLookAt(target, 30, 30);
        emperor.getNavigation().stop();
        zapTicks--;

        if (!(emperor.level() instanceof ServerLevel sl)) return;

        // The primary bolt plus a short chain onto the nearest others — his
        // own Imperials are spared (dark-side power, but not suicidal).
        List<LivingEntity> victims = chainTargets(sl, target);
        boolean strike = zapTicks % DAMAGE_INTERVAL == 0;
        for (LivingEntity v : victims) {
            arc(sl, v);
            if (strike) {
                v.hurtServer(sl, StarWarsDamageTypes.forceLightning(emperor), DAMAGE);
                v.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 1));
            }
        }
        if (strike) {
            sl.playSound(null, emperor.getX(), emperor.getY(), emperor.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.5F, 1.6F);
        }
    }

    /** The primary target plus up to {@link #CHAIN_TARGETS}-1 nearby non-Imperials. */
    private List<LivingEntity> chainTargets(ServerLevel sl, LivingEntity primary) {
        AABB box = emperor.getBoundingBox().inflate(CHAIN_RADIUS);
        List<LivingEntity> chained = sl.getEntitiesOfClass(LivingEntity.class, box, e ->
                e != emperor && e != primary && e.isAlive() && !isImperial(e))
            .stream()
            .sorted(Comparator.comparingDouble(emperor::distanceToSqr))
            .limit(CHAIN_TARGETS - 1L)
            .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        chained.add(0, primary);
        return chained;
    }

    private static boolean isImperial(LivingEntity e) {
        return e instanceof SwCombatant sc && sc.getFaction() == SwFaction.EMPIRE;
    }

    /** Electric-spark arc from the Emperor's hands to a victim. */
    private void arc(ServerLevel sl, LivingEntity victim) {
        Vec3 from = emperor.getEyePosition();
        Vec3 to = victim.position().add(0, victim.getBbHeight() * 0.5, 0);
        Vec3 mid = from.add(to).scale(0.5);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, mid.x, mid.y, mid.z, 18, 0.3, 0.3, 0.3, 0.06);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
            to.x, to.y, to.z, 12, 0.2, 0.4, 0.2, 0.06);
    }
}
