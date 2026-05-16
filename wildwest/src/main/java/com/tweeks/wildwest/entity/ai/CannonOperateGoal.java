package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.ModBlocks;
import com.tweeks.wildwest.block.CannonBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Lets a mob fire an adjacent loaded wildwest:cannon block at its
 * current target. Checks the 3x3x3 cube around the mob for loaded cannons
 * and picks the first one whose facing roughly points at the target.
 *
 * <p>Cooldown is intentionally short (4 s) because reloading is a player-only
 * action — pirates never refill spent cannons.
 */
public class CannonOperateGoal extends Goal {

    private static final int COOLDOWN_TICKS = 80;
    private static final double MIN_TARGET_DISTANCE = 5.0;

    private final Mob mob;
    private int cooldown = 0;

    public CannonOperateGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (this.mob.distanceTo(target) < MIN_TARGET_DISTANCE) return false;
        if (!(this.mob.level() instanceof ServerLevel)) return false;
        return findLoadedCannon() != null;
    }

    @Override
    public boolean canContinueToUse() { return false; }

    @Override
    public void start() {
        this.cooldown = COOLDOWN_TICKS;
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;
        if (!(this.mob.level() instanceof ServerLevel sl)) return;

        BlockPos cannonPos = findLoadedCannon();
        if (cannonPos == null) return;

        BlockState state = sl.getBlockState(cannonPos);
        if (!(state.getBlock() instanceof CannonBlock)) return;

        Vec3 aim = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
        CannonBlock.aiFire(sl, cannonPos, state, this.mob, aim);
    }

    private BlockPos findLoadedCannon() {
        BlockPos origin = this.mob.blockPosition();
        ServerLevel sl = (ServerLevel) this.mob.level();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    BlockState s = sl.getBlockState(p);
                    if (s.is(ModBlocks.CANNON.get())
                        && s.getValue(CannonBlock.LOADED)) {
                        return p;
                    }
                }
            }
        }
        return null;
    }
}
