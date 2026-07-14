// TODO LLM: cache miss; run :translate --with-llm to translate
// Goal: com.tweeks.wildwest.entity.ai.LeaderTargetCopyGoal
//
// This file is a placeholder. Either:
//   1. Run `./gradlew :translator:translate --with-llm` with ANTHROPIC_API_KEY set, or
//   2. Hand-translate the Java below to @minecraft/server event handlers.

/*
Original Java source — translate this:

package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.WildWestMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import java.util.EnumSet;

public class LeaderTargetCopyGoal extends TargetGoal {

    private final WildWestMob follower;

    public LeaderTargetCopyGoal(WildWestMob follower) {
        super(follower, false);  // mustSee = false
        this.follower = follower;
        // Goal.Flag.TARGET locks out lower-priority target goals while this one runs,
        // so the copied target isn't immediately overwritten by HurtByTargetGoal /
        // LawmanTargetGoal / OutlawTargetGoal on the very next selector tick.
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
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
    public boolean canContinueToUse() {
        // Re-engage if the leader switched targets — keep the patrol coordinated
        // even when the leader changes its mind mid-fight.
        WildWestMob leader = follower.getFollowingLeader();
        if (leader == null || !leader.isAlive()) return false;
        LivingEntity leaderTarget = leader.getTarget();
        if (leaderTarget == null || !leaderTarget.isAlive()) return false;
        // If the leader's target diverged from the follower's, re-sync.
        if (follower.getTarget() != leaderTarget) {
            follower.setTarget(leaderTarget);
        }
        return true;
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

*/

// Empty handler so Bedrock's script engine accepts the file.
export {};
