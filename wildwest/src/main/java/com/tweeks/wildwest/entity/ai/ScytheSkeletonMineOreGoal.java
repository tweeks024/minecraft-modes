package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.ScytheSkeletonEntity;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Idle behavior: scan 3×3×3 cube around the skeleton, find the nearest
 * precious-metal ore (iron / gold / diamond / emerald / ancient debris,
 * including deepslate variants), path to it, mine it over 20 ticks. Drops
 * route into owner inventory, overflow falls at owner's feet.
 *
 * <p>Eligible only when the skeleton's idle counter exceeds 60 (3 seconds
 * of no target / no path / not adjacent to owner).
 */
public class ScytheSkeletonMineOreGoal extends Goal {

    private static final int IDLE_THRESHOLD = 60;
    private static final int SCAN_RADIUS = 3;
    private static final int Y_BOUND = 8;
    private static final int MINE_TIME_TICKS = 20;

    private static final Set<Block> PRECIOUS_ORES = Set.of(
        Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
        Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
        Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
        Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.ANCIENT_DEBRIS);

    private final ScytheSkeletonEntity skeleton;
    private BlockPos target;
    private int mineTimer = 0;

    public ScytheSkeletonMineOreGoal(ScytheSkeletonEntity skeleton) {
        this.skeleton = skeleton;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.skeleton.getTarget() != null) return false;
        if (this.skeleton.getIdleTicks() < IDLE_THRESHOLD) return false;
        Player owner = this.skeleton.getOwnerPlayer();
        if (owner == null) return false;
        if (this.skeleton.distanceToSqr(owner) > 32.0 * 32.0) return false;

        BlockPos found = findNearestOre();
        if (found == null) return false;
        if (Math.abs(found.getY() - owner.getBlockY()) > Y_BOUND) return false;

        this.target = found;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.target == null) return false;
        BlockState state = this.skeleton.level().getBlockState(this.target);
        return PRECIOUS_ORES.contains(state.getBlock());
    }

    @Override
    public void start() {
        if (this.target != null) {
            this.skeleton.getNavigation().moveTo(
                this.target.getX() + 0.5,
                this.target.getY(),
                this.target.getZ() + 0.5,
                1.0);
        }
        this.mineTimer = 0;
    }

    @Override
    public void tick() {
        if (this.target == null) return;
        if (!(this.skeleton.level() instanceof ServerLevel sl)) return;

        double distSq = this.skeleton.distanceToSqr(
            this.target.getX() + 0.5, this.target.getY() + 0.5, this.target.getZ() + 0.5);
        if (distSq > 1.5 * 1.5) {
            this.mineTimer = 0;
            return;
        }

        sl.sendParticles(ParticleTypes.CRIT,
            this.target.getX() + 0.5, this.target.getY() + 0.5, this.target.getZ() + 0.5,
            3, 0.2, 0.2, 0.2, 0.1);

        this.mineTimer++;
        if (this.mineTimer < MINE_TIME_TICKS) return;

        // Compute drops BEFORE destroying the block.
        BlockState state = sl.getBlockState(this.target);
        if (!PRECIOUS_ORES.contains(state.getBlock())) {
            this.target = null;
            return;
        }
        var drops = Block.getDrops(state, sl, this.target, sl.getBlockEntity(this.target));

        sl.destroyBlock(this.target, false, this.skeleton);

        Player owner = this.skeleton.getOwnerPlayer();
        for (ItemStack drop : drops) {
            if (owner == null) {
                sl.addFreshEntity(new ItemEntity(sl,
                    this.target.getX() + 0.5, this.target.getY() + 0.5, this.target.getZ() + 0.5,
                    drop));
                continue;
            }
            ItemStack toGive = drop.copy();
            if (!owner.getInventory().add(toGive)) {
                sl.addFreshEntity(new ItemEntity(sl,
                    owner.getX(), owner.getY(), owner.getZ(), toGive));
            }
        }

        this.target = null;
        this.skeleton.resetIdleTicks();
    }

    @Override
    public void stop() {
        this.target = null;
        this.mineTimer = 0;
    }

    private BlockPos findNearestOre() {
        BlockPos origin = this.skeleton.blockPosition();
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dy = -SCAN_RADIUS; dy <= SCAN_RADIUS; dy++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (!PRECIOUS_ORES.contains(this.skeleton.level().getBlockState(p).getBlock())) continue;
                    double dSq = origin.distSqr(p);
                    if (dSq < bestDistSq) {
                        bestDistSq = dSq;
                        best = p;
                    }
                }
            }
        }
        return best;
    }
}
