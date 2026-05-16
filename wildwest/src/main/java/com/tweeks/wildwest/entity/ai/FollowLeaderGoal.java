package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.WildWestMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class FollowLeaderGoal extends Goal {

    private static final double ACQUIRE_RANGE = 16.0;
    private static final double LOSE_INTEREST_RANGE = 24.0;
    private static final double FOLLOW_DISTANCE = 8.0;
    private static final double SPEED = 1.0;

    private final WildWestMob self;

    public FollowLeaderGoal(WildWestMob self) {
        this.self = self;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (self.isLeader()) return false;

        WildWestMob current = self.getFollowingLeader();
        AABB scanArea = self.getBoundingBox().inflate(ACQUIRE_RANGE);
        List<WildWestMob> nearby = new ArrayList<>(
            self.level().getEntitiesOfClass(WildWestMob.class, scanArea, m -> m != self));
        nearby.sort(Comparator.comparingDouble(self::distanceTo));

        var chosen = FollowDecision.choose(self.isLawman(), nearby, current);
        if (chosen.isEmpty()) {
            self.setFollowingLeader(null);
            return false;
        }
        self.setFollowingLeader(chosen.get());
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        WildWestMob leader = self.getFollowingLeader();
        if (leader == null) return false;
        if (!leader.isAlive()) return false;
        return self.distanceTo(leader) <= LOSE_INTEREST_RANGE;
    }

    @Override
    public void tick() {
        WildWestMob leader = self.getFollowingLeader();
        if (leader == null) return;
        if (self.distanceTo(leader) > FOLLOW_DISTANCE) {
            self.getNavigation().moveTo(leader, SPEED);
        }
    }

    @Override
    public void stop() {
        // Only clear the leader reference when the leader is actually gone
        // (dead or out of range). If the goal merely got preempted by a higher
        // priority MOVE-flag goal (e.g. melee), keep the leader so
        // LeaderTargetCopyGoal continues to coordinate targets mid-fight.
        WildWestMob leader = self.getFollowingLeader();
        if (leader == null || !leader.isAlive() || self.distanceTo(leader) > LOSE_INTEREST_RANGE) {
            self.setFollowingLeader(null);
        }
        self.getNavigation().stop();
    }
}
