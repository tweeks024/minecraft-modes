package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Hazard-zone entity spawned by {@link com.tweeks.wildwest.entity.ai.NullRiftGoal}.
 * Two-phase server-side lifecycle:
 *  <ol>
 *      <li>Telegraph (0–40 ticks, 2s): growing portal-particle ring, no damage.</li>
 *      <li>Active (40–120 ticks, 4s): dense particles, pulls in living entities,
 *          deals 2 dmg every 10 ticks (= 4 dps sustained), discards ItemEntity instances.</li>
 *  </ol>
 *
 * <p>All gameplay logic runs server-side; particles are dispatched via
 * {@link ServerLevel#sendParticles}. Client renderer is a no-op.
 *
 * <p>Invulnerable to all damage — rifts cannot be destroyed, they tick out.
 */
public class NullRiftEntity extends Entity {

    private static final int TELEGRAPH_END = 40;
    private static final int ACTIVE_END = 120;
    private static final int DAMAGE_TICK_PERIOD = 10;
    private static final float DAMAGE_PER_HIT = 2.0f;
    private static final double PULL_MAGNITUDE = 0.15;

    private int ageTicks = 0;

    public NullRiftEntity(EntityType<? extends NullRiftEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No synced fields — client only needs entity existence; visuals are server-pushed particles.
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.ageTicks = input.getIntOr("AgeTicks", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("AgeTicks", this.ageTicks);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Rifts are indestructible; they tick out on their own timer.
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel sl)) return;

        this.ageTicks++;
        if (this.ageTicks >= ACTIVE_END) {
            this.discard();
            return;
        }

        if (this.ageTicks < TELEGRAPH_END) {
            this.runTelegraph(sl);
        } else {
            this.runActive(sl);
        }
    }

    private void runTelegraph(ServerLevel sl) {
        double radius = 1.5 * (this.ageTicks / (double) TELEGRAPH_END);
        int particleCount = 12;
        for (int i = 0; i < particleCount; i++) {
            double angle = (i / (double) particleCount) * 2.0 * Math.PI;
            double px = this.getX() + Math.cos(angle) * radius;
            double pz = this.getZ() + Math.sin(angle) * radius;
            sl.sendParticles(ParticleTypes.PORTAL, px, this.getY() + 0.1, pz,
                1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void runActive(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.PORTAL,
            this.getX(), this.getY() + 1.0, this.getZ(),
            20, 1.5, 1.5, 1.5, 0.1);
        sl.sendParticles(ParticleTypes.SMOKE,
            this.getX(), this.getY() + 0.5, this.getZ(),
            8, 1.4, 0.3, 1.4, 0.0);

        boolean damageThisTick = (this.ageTicks % DAMAGE_TICK_PERIOD == 0);

        List<LivingEntity> living = sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox());
        for (LivingEntity e : living) {
            if (e.getType() == ModEntities.NULL.get()) continue; // Null himself is immune.
            if (damageThisTick) {
                e.hurt(sl.damageSources().magic(), DAMAGE_PER_HIT);
            }
            double dx = this.getX() - e.getX();
            double dz = this.getZ() - e.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0.001) {
                double pullX = (dx / len) * PULL_MAGNITUDE;
                double pullZ = (dz / len) * PULL_MAGNITUDE;
                e.setDeltaMovement(e.getDeltaMovement().add(new Vec3(pullX, 0, pullZ)));
            }
        }

        List<ItemEntity> items = sl.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox());
        for (ItemEntity item : items) {
            item.discard();
        }
    }
}
