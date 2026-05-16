package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.HerobrineEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Teleports Herobrine every ~4 seconds. Distance-aware:
 *  <ul>
 *      <li>far → close gap toward target</li>
 *      <li>close → open gap away from target</li>
 *      <li>mid → random horizontal reposition</li>
 *  </ul>
 *
 * <p>Pure destination math is in {@link HerobrineTeleportTarget} (unit-tested).
 * This goal handles Y-snapping via the heightmap and clearance validation.
 */
public class HerobrineTeleportGoal extends Goal {

    private static final int COOLDOWN_TICKS = 80; // 4 s at 20 tps
    private static final int CLEARANCE_RETRIES = 5;

    private final HerobrineEntity boss;
    private int cooldown = 0;

    public HerobrineTeleportGoal(HerobrineEntity boss) {
        this.boss = boss;
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.boss.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return false; // one-shot
    }

    @Override
    public void start() {
        LivingEntity target = this.boss.getTarget();
        if (target == null) return;
        this.cooldown = COOLDOWN_TICKS;

        if (!(this.boss.level() instanceof ServerLevel sl)) return;

        // Pick destination via pure helper.
        HerobrineTeleportTarget.Rng rng = this.boss.getRandom()::nextDouble;
        HerobrineTeleportTarget.Result picked = HerobrineTeleportTarget.pick(
            this.boss.getX(), this.boss.getZ(),
            target.getX(), target.getZ(), rng);

        // Y-snap + clearance. NeoForge's getHeightmapPos for MOTION_BLOCKING_NO_LEAVES
        // can return either "first air above surface" or "the surface block itself"
        // depending on minor version; we try BOTH topPos and topPos.above() before
        // perturbing, so a heightmap returning the solid surface block doesn't trap us.
        double destX = picked.x();
        double destZ = picked.z();
        double destY = 0;
        boolean destFound = false;
        for (int retry = 0; retry < CLEARANCE_RETRIES; retry++) {
            BlockPos topPos = sl.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(destX, this.boss.getY(), destZ));

            // Try topPos itself (heightmap returned the air block above ground).
            if (sl.getBlockState(topPos).isAir()
                && sl.getBlockState(topPos.above()).isAir()) {
                destY = topPos.getY();
                destFound = true;
                break;
            }

            // Try topPos.above() (heightmap returned the solid surface block).
            BlockPos above = topPos.above();
            if (sl.getBlockState(above).isAir()
                && sl.getBlockState(above.above()).isAir()) {
                destY = above.getY();
                destFound = true;
                break;
            }

            // Retry with a small lateral perturbation.
            double angle = this.boss.getRandom().nextDouble() * 2.0 * Math.PI;
            destX += Math.cos(angle) * 1.5;
            destZ += Math.sin(angle) * 1.5;
        }
        if (!destFound) {
            // Couldn't find a valid spot — skip this teleport, cooldown stays reset.
            // Bool flag instead of Y < 0 sentinel: overworld Y can be negative
            // (deep-slate / ancient cities at Y -30 to -50).
            return;
        }

        // Source-side particles + sound.
        sl.sendParticles(ParticleTypes.PORTAL,
            this.boss.getX(), this.boss.getY() + 1.0, this.boss.getZ(),
            16, 0.5, 1.0, 0.5, 0.0);
        sl.playSound(null, this.boss.getX(), this.boss.getY(), this.boss.getZ(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8f, 1.0f);

        // Move + zero fall distance.
        this.boss.teleportTo(destX, destY, destZ);
        this.boss.fallDistance = 0;

        // Destination-side particles + sound.
        sl.sendParticles(ParticleTypes.PORTAL, destX, destY + 1.0, destZ,
            16, 0.5, 1.0, 0.5, 0.0);
        sl.playSound(null, destX, destY, destZ, SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.HOSTILE, 0.8f, 1.0f);
    }
}
