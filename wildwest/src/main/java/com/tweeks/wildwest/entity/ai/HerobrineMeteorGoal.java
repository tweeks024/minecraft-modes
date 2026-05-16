package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.entity.HerobrineEntity;
import com.tweeks.wildwest.entity.projectile.MeteorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Spawns one {@link MeteorEntity} every ~8 s, falling straight down from above
 * a random ring point 6–14 blocks around Herobrine. Ceiling-aware: in
 * dimensions with {@code hasCeiling()} (Nether-style), uses a downward raycast
 * to find an air pocket beneath solid blocks instead of spawning above the
 * bedrock ceiling.
 */
public class HerobrineMeteorGoal extends Goal {

    private static final int COOLDOWN_TICKS = 160; // 8 s
    private static final double RING_MIN = 6.0;
    private static final double RING_MAX = 14.0;
    private static final int FALL_HEIGHT = 30;
    private static final int CEILING_MIN_POCKET = 4;

    private final HerobrineEntity boss;
    private int cooldown = 0;

    public HerobrineMeteorGoal(HerobrineEntity boss) {
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
    public boolean canContinueToUse() { return false; }

    @Override
    public void start() {
        this.cooldown = COOLDOWN_TICKS;
        if (!(this.boss.level() instanceof ServerLevel sl)) return;

        // Pick a ring point (uniform angle, uniform radius in [RING_MIN, RING_MAX]).
        double angle = this.boss.getRandom().nextDouble() * 2.0 * Math.PI;
        double radius = RING_MIN + this.boss.getRandom().nextDouble() * (RING_MAX - RING_MIN);
        double xzX = this.boss.getX() + Math.cos(angle) * radius;
        double xzZ = this.boss.getZ() + Math.sin(angle) * radius;

        Vec3 spawn = pickSpawnY(sl, xzX, xzZ);
        if (spawn == null) return; // no valid pocket; cooldown still resets

        MeteorEntity meteor = ModEntities.METEOR.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
        if (meteor == null) return;
        meteor.setPos(spawn.x, spawn.y, spawn.z);
        meteor.setDeltaMovement(0.0, -0.4, 0.0); // initial downward kick to avoid hover before gravity kicks in
        meteor.setOwner(this.boss);
        sl.addFreshEntity(meteor);
    }

    /**
     * Compute the spawn Y. Caps at {@code level.getMaxY() - 1}. In ceilinged
     * dimensions, finds the first air pocket of {@link #CEILING_MIN_POCKET}
     * vertical clearance via downward scan from {@code maxY}.
     */
    private static Vec3 pickSpawnY(ServerLevel sl, double xzX, double xzZ) {
        BlockPos seed = BlockPos.containing(xzX, sl.getMaxY(), xzZ);
        int maxY = sl.getMaxY() - 1;

        if (sl.dimensionType().hasCeiling()) {
            // Scan down for first run of CEILING_MIN_POCKET air blocks above a non-air block.
            // Return the TOP of the pocket so the meteor has the full pocket height to fall.
            int pocketRun = 0;
            int pocketTop = 0;
            for (int y = maxY; y > sl.getMinY() + 1; y--) {
                BlockPos p = new BlockPos(seed.getX(), y, seed.getZ());
                if (sl.getBlockState(p).isAir()) {
                    if (pocketRun == 0) pocketTop = y;
                    pocketRun++;
                    if (pocketRun >= CEILING_MIN_POCKET) {
                        return new Vec3(xzX, pocketTop, xzZ);
                    }
                } else {
                    pocketRun = 0;
                }
            }
            return null;
        }

        // Open-sky path: 30 blocks above the surface, capped at maxY.
        BlockPos top = sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, seed);
        int spawnY = Math.min(top.getY() + FALL_HEIGHT, maxY);
        return new Vec3(xzX, spawnY, xzZ);
    }
}
