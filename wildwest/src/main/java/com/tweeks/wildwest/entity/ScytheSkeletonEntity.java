package com.tweeks.wildwest.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.level.Level;

/**
 * Player-summoned minion summoned by the Reaper Scythe's right-click cast.
 * Persistent, owner-tagged via {@link java.util.UUID}, follows owner and
 * fights hostiles. Idle behavior: mines precious ores within 3 blocks.
 *
 * <p>Stub — full behavior added in later plan tasks.
 */
public class ScytheSkeletonEntity extends Skeleton {
    public ScytheSkeletonEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }
}
