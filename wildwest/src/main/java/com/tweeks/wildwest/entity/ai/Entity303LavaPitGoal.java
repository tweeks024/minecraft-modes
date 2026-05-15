package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.Entity303Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.EnumSet;

/**
 * Carves a 3×3×3 lava pit near the target and teleports them to the rim so
 * they fall in. Long cooldown — this is the "punish" finisher, not constant
 * pressure. Skipped silently if any block in the carve volume is
 * {@link BlockTags#DRAGON_IMMUNE} (bedrock, end portal frame, etc.).
 */
public class Entity303LavaPitGoal extends Goal {

    private static final int COOLDOWN_TICKS = 400; // 20 s
    private static final double MAX_DISTANCE = 32.0;
    private static final int PIT_HALF = 1;          // 3×3 footprint (half-width 1)
    private static final int PIT_DEPTH = 3;         // total carve depth, bottom = lava
    private static final double ANCHOR_MIN_DIST = 5.0;
    private static final double ANCHOR_MAX_DIST = 8.0;

    private final Entity303Entity boss;

    public Entity303LavaPitGoal(Entity303Entity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        if (this.boss.getLavaPitCooldown() > 0) return false;
        LivingEntity target = this.boss.getTarget();
        if (target == null || !target.isAlive()) return false;
        return this.boss.distanceTo(target) <= MAX_DISTANCE;
    }

    @Override
    public boolean canContinueToUse() { return false; }

    @Override
    public void start() {
        LivingEntity target = this.boss.getTarget();
        if (target == null) return;
        this.boss.setLavaPitCooldown(COOLDOWN_TICKS);

        if (!(this.boss.level() instanceof ServerLevel sl)) return;

        // Pick a horizontal anchor 5–8 blocks from the target in a random
        // direction. Anchored on target XZ for surprise factor.
        double angle = this.boss.getRandom().nextDouble() * 2.0 * Math.PI;
        double dist = ANCHOR_MIN_DIST + this.boss.getRandom().nextDouble() * (ANCHOR_MAX_DIST - ANCHOR_MIN_DIST);
        double anchorX = target.getX() + Math.cos(angle) * dist;
        double anchorZ = target.getZ() + Math.sin(angle) * dist;

        // Find the topmost solid block at the anchor XZ. Heightmap returns
        // either the first air position above ground OR the ground block
        // itself depending on NeoForge minor version — handle both.
        BlockPos heightmapPos = sl.getHeightmapPos(
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            BlockPos.containing(anchorX, target.getY(), anchorZ));
        BlockPos groundTop = sl.getBlockState(heightmapPos).isAir()
            ? heightmapPos.below()
            : heightmapPos;
        int floorY = groundTop.getY();
        int centerX = groundTop.getX();
        int centerZ = groundTop.getZ();

        // Need enough depth below to dig PIT_DEPTH layers.
        if (floorY - PIT_DEPTH <= sl.getMinY() + 1) return;

        // Refuse if any block in the carve volume is dragon-immune (bedrock,
        // end portal frame, end gateway, etc.).
        for (int dx = -PIT_HALF; dx <= PIT_HALF; dx++) {
            for (int dz = -PIT_HALF; dz <= PIT_HALF; dz++) {
                for (int dy = 0; dy < PIT_DEPTH; dy++) {
                    BlockPos p = new BlockPos(centerX + dx, floorY - dy, centerZ + dz);
                    if (sl.getBlockState(p).is(BlockTags.DRAGON_IMMUNE)) return;
                }
            }
        }

        // Carve the top PIT_DEPTH-1 layers to air; bottom layer = lava source.
        for (int dx = -PIT_HALF; dx <= PIT_HALF; dx++) {
            for (int dz = -PIT_HALF; dz <= PIT_HALF; dz++) {
                for (int dy = 0; dy < PIT_DEPTH - 1; dy++) {
                    BlockPos p = new BlockPos(centerX + dx, floorY - dy, centerZ + dz);
                    sl.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                }
                BlockPos lavaPos = new BlockPos(centerX + dx, floorY - (PIT_DEPTH - 1), centerZ + dz);
                sl.setBlockAndUpdate(lavaPos, Blocks.LAVA.defaultBlockState());
            }
        }

        // Teleport target 1 block above the (now-carved) rim so they fall in.
        double teleX = centerX + 0.5;
        double teleY = floorY + 1.0;
        double teleZ = centerZ + 0.5;
        target.teleportTo(teleX, teleY, teleZ);

        // Telegraph + threat audio.
        sl.sendParticles(ParticleTypes.LAVA,
            teleX, floorY, teleZ, 32, 0.8, 0.5, 0.8, 0.05);
        sl.playSound(null, centerX, floorY, centerZ,
            SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 1.0f, 0.6f);
    }
}
