package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.Entity303Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.phys.AABB;

/**
 * Custom target acquisition for Entity 303 — overrides the default cylinder
 * search area used by {@link NearestAttackableTargetGoal} (which is only
 * ±4 blocks in Y, so 303 misses players standing on hills or buildings) with
 * a full cube of the entity's follow range. Also runs with {@code mustSee=false}
 * so 303 senses players through walls and foliage — fits the creepypasta
 * supernatural-awareness flavor and means a single tree or doorway can't
 * neutralize the boss.
 */
public class Entity303TargetGoal extends NearestAttackableTargetGoal<Player> {

    public Entity303TargetGoal(Entity303Entity boss) {
        // randomInterval=5 (0.25 s poll), mustSee=false, mustReach=false.
        super(boss, Player.class, 5, false, false, null);
    }

    @Override
    protected AABB getTargetSearchArea(double range) {
        // Cube instead of vanilla's (range × 4 × range) cylinder.
        return this.mob.getBoundingBox().inflate(range, range, range);
    }
}
