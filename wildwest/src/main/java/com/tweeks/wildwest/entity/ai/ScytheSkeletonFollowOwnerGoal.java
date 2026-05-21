package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.ScytheSkeletonEntity;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;

/**
 * Wolf-style follow-owner for bone servants. Vanilla {@code FollowOwnerGoal}
 * is hardcoded to {@code TamableAnimal}, which we are not — so we own a
 * minimal port: stay within {@code startDistance} of owner, teleport behind
 * owner if distance exceeds {@code teleportDistance}.
 */
public class ScytheSkeletonFollowOwnerGoal extends Goal {

    private final ScytheSkeletonEntity skeleton;
    private final double speed;
    private final float startDistance;
    private final float teleportDistance;
    private final PathNavigation navigation;
    private LivingEntity owner;
    private int timeToRecalcPath;

    public ScytheSkeletonFollowOwnerGoal(ScytheSkeletonEntity skeleton,
                                         double speed,
                                         float startDistance,
                                         float teleportDistance) {
        this.skeleton = skeleton;
        this.speed = speed;
        this.startDistance = startDistance;
        this.teleportDistance = teleportDistance;
        this.navigation = skeleton.getNavigation();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Player owner = this.skeleton.getOwnerPlayer();
        if (owner == null || owner.isSpectator()) return false;
        if (this.skeleton.distanceToSqr(owner) < this.startDistance * this.startDistance) return false;
        this.owner = owner;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.owner == null) return false;
        return this.skeleton.distanceToSqr(this.owner) > (this.startDistance * 0.5f) * (this.startDistance * 0.5f);
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        if (this.owner != null) {
            this.navigation.moveTo(this.owner, this.speed);
        }
    }

    @Override
    public void stop() {
        this.owner = null;
        this.navigation.stop();
    }

    @Override
    public void tick() {
        if (this.owner == null) return;
        this.skeleton.getLookControl().setLookAt(this.owner, 10.0F, 30.0F);
        if (--this.timeToRecalcPath > 0) return;
        this.timeToRecalcPath = 10;

        if (this.skeleton.distanceToSqr(this.owner) >= this.teleportDistance * this.teleportDistance) {
            teleportBehindOwner();
        } else {
            this.navigation.moveTo(this.owner, this.speed);
        }
    }

    private void teleportBehindOwner() {
        BlockPos ownerPos = this.owner.blockPosition();
        var rng = this.skeleton.getRandom();
        for (int i = 0; i < 10; i++) {
            int dx = rng.nextInt(5) - 2;
            int dz = rng.nextInt(5) - 2;
            BlockPos candidate = ownerPos.offset(dx, 0, dz);
            if (isTeleportable(candidate)) {
                this.skeleton.snapTo(
                    candidate.getX() + 0.5,
                    candidate.getY(),
                    candidate.getZ() + 0.5,
                    this.skeleton.getYRot(),
                    this.skeleton.getXRot());
                this.navigation.stop();
                return;
            }
        }
    }

    private boolean isTeleportable(BlockPos pos) {
        LevelReader level = this.skeleton.level();
        var blockBelow = level.getBlockState(pos.below());
        var blockAt = level.getBlockState(pos);
        var blockAbove = level.getBlockState(pos.above());
        if (!blockBelow.isCollisionShapeFullBlock(level, pos.below())) return false;
        if (blockBelow.is(BlockTags.LEAVES)) return false;
        if (!blockAt.getCollisionShape(level, pos).isEmpty()) return false;
        if (!blockAbove.getCollisionShape(level, pos.above()).isEmpty()) return false;
        return true;
    }
}
