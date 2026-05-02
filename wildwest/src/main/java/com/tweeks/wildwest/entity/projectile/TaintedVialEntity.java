package com.tweeks.wildwest.entity.projectile;

import com.tweeks.wildwest.effect.ModEffects;
import com.tweeks.wildwest.entity.ai.zombified.InfectionImmunity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class TaintedVialEntity extends ThrowableItemProjectile {
    private static final double SPLASH_RADIUS = 3.0;
    private static final int FESTERING_DURATION_TICKS = 60 * 20;

    public TaintedVialEntity(EntityType<? extends TaintedVialEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDefaultItem() {
        return com.tweeks.wildwest.Registration.TAINTED_VIAL.get();
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(this.level() instanceof ServerLevel sl)) return;

        Vec3 center = hit.getLocation();

        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
            center.x, center.y, center.z, 30, SPLASH_RADIUS, 0.5, SPLASH_RADIUS, 0.0);
        sl.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK,
            SoundSource.NEUTRAL, 1.0F, 1.0F);

        AABB box = AABB.ofSize(center, SPLASH_RADIUS * 2, SPLASH_RADIUS * 2, SPLASH_RADIUS * 2);
        sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.position().distanceTo(center) <= SPLASH_RADIUS)
            .forEach(e -> {
                if (InfectionImmunity.isImmune(e)) return;
                if (e.hasEffect(ModEffects.ZOMBIFIED)) return;
                e.addEffect(new MobEffectInstance(ModEffects.FESTERING_WOUND,
                    FESTERING_DURATION_TICKS, 0, false, true));
            });

        this.discard();
    }
}
