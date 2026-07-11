package com.tweeks.starwars.item;

import com.tweeks.starwars.StarWarsDamageTypes;
import com.tweeks.starwars.entity.SwMob;
import com.tweeks.starwars.faction.ModAttachments;
import com.tweeks.starwars.faction.PacifyAttachment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Server-side casts for the five {@link ForcePower}s. All methods return
 * {@code true} if the cast should burn its cooldown; {@code false} means
 * "no valid target" and the caller (HolocronItem) leaves the cooldown
 * untouched.
 */
public final class ForcePowers {
    private ForcePowers() {}

    public static final double PUSH_RADIUS = 6.0;
    public static final double PUSH_STRENGTH = 1.2;
    public static final double PUSH_LIFT = 0.4;
    public static final double PULL_RANGE = 10.0;
    public static final double PULL_STRENGTH = 0.9;
    public static final double LEAP_HORIZONTAL = 1.2;
    public static final double LEAP_VERTICAL = 0.9;
    public static final double MIND_TRICK_RADIUS = 8.0;
    public static final int MIND_TRICK_DURATION_TICKS = 200;
    public static final double LIGHTNING_RADIUS = 8.0;
    public static final int LIGHTNING_MAX_TARGETS = 3;
    public static final float LIGHTNING_DAMAGE = 6.0F;

    /** Returns true if the cast did anything (misses still count for push/leap). */
    public static boolean cast(ForcePower power, ServerPlayer player, ServerLevel level) {
        return switch (power) {
            case PUSH -> push(player, level);
            case PULL -> pull(player, level);
            case LEAP -> leap(player);
            case MIND_TRICK -> mindTrick(player, level);
            case LIGHTNING -> lightning(player, level);
        };
    }

    private static List<LivingEntity> livingNear(ServerPlayer player, ServerLevel level, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class,
            player.getBoundingBox().inflate(radius),
            e -> e != player && e.isAlive());
    }

    private static boolean push(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getViewVector(1.0F);
        for (LivingEntity e : livingNear(player, level, PUSH_RADIUS)) {
            Vec3 to = e.position().subtract(player.position());
            if (to.lengthSqr() < 1.0e-4) continue;
            if (to.normalize().dot(look) < 0.5) continue;   // 60-degree cone
            Vec3 flat = new Vec3(to.x, 0, to.z).normalize().scale(PUSH_STRENGTH);
            e.setDeltaMovement(e.getDeltaMovement().add(flat.x, PUSH_LIFT, flat.z));
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM,
            player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
        return true;
    }

    private static boolean pull(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getViewVector(1.0F);
        LivingEntity best = livingNear(player, level, PULL_RANGE).stream()
            .filter(e -> e.position().subtract(player.position()).normalize().dot(look) > 0.7)
            .min(Comparator.comparingDouble(player::distanceToSqr))
            .orElse(null);
        if (best == null) return false;
        Vec3 toPlayer = player.position().subtract(best.position()).normalize()
            .scale(PULL_STRENGTH);
        best.setDeltaMovement(toPlayer.x, toPlayer.y * 0.5 + 0.3, toPlayer.z);
        best.hurtMarked = true;
        return true;
    }

    private static boolean leap(ServerPlayer player) {
        Vec3 look = player.getViewVector(1.0F);
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize().scale(LEAP_HORIZONTAL);
        player.setDeltaMovement(flat.x, LEAP_VERTICAL, flat.z);
        player.hurtMarked = true;
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0));
        return true;
    }

    /**
     * Pacify note (honesty over ambition): vanilla mobs already targeting
     * the caster have their target cleared here but may re-acquire on a
     * later tick — vanilla target goals don't consult
     * {@link ModAttachments#PACIFIED}. Full pacify (the mob refuses to
     * re-target the caster for the duration) only holds for
     * {@link SwMob}s, whose {@link com.tweeks.starwars.entity.ai.SwTargetGoal}
     * checks the attachment in {@code canUse()}.
     */
    private static boolean mindTrick(ServerPlayer player, ServerLevel level) {
        long until = level.getGameTime() + MIND_TRICK_DURATION_TICKS;
        boolean any = false;
        for (LivingEntity e : livingNear(player, level, MIND_TRICK_RADIUS)) {
            if (!(e instanceof Mob mob)) continue;
            if (mob.getTarget() != player) {
                // Only pacify things currently hostile to the caster, plus all SwMobs.
                if (!(mob instanceof SwMob)) continue;
            }
            mob.setTarget(null);
            mob.setData(ModAttachments.PACIFIED.get(), new PacifyAttachment(until));
            any = true;
        }
        return any;
    }

    private static boolean lightning(ServerPlayer player, ServerLevel level) {
        // Deliberately indiscriminate: nearest 3 living entities, no
        // line-of-sight or hostility filter — dark-side power, chains onto
        // pets/villagers through walls by design (matches the spec's letter).
        List<LivingEntity> targets = livingNear(player, level, LIGHTNING_RADIUS).stream()
            .sorted(Comparator.comparingDouble(player::distanceToSqr))
            .limit(LIGHTNING_MAX_TARGETS)
            .toList();
        if (targets.isEmpty()) return false;
        for (LivingEntity e : targets) {
            e.hurtServer(level, StarWarsDamageTypes.forceLightning(player), LIGHTNING_DAMAGE);
            Vec3 mid = player.getEyePosition().add(e.position().add(0, e.getBbHeight() * 0.5, 0))
                .scale(0.5);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                mid.x, mid.y, mid.z, 20, 0.3, 0.3, 0.3, 0.05);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 15, 0.2, 0.4, 0.2, 0.05);
        }
        return true;
    }
}
