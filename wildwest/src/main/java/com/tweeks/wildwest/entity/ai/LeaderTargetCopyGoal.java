package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.WildWestMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

public class LeaderTargetCopyGoal extends TargetGoal {

    private final WildWestMob follower;

    public LeaderTargetCopyGoal(WildWestMob follower) {
        super(follower, false);  // mustSee = false
        this.follower = follower;
    }

    @Override
    public boolean canUse() {
        if (follower.isLeader()) return false;
        if (follower.getTarget() != null) return false;
        WildWestMob leader = follower.getFollowingLeader();
        if (leader == null) return false;
        LivingEntity leaderTarget = leader.getTarget();
        return leaderTarget != null && leaderTarget.isAlive();
    }

    @Override
    public void start() {
        WildWestMob leader = follower.getFollowingLeader();
        if (leader != null && leader.getTarget() != null) {
            follower.setTarget(leader.getTarget());
        }
        super.start();
    }
}
