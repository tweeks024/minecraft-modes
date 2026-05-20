package com.tweeks.wildwest.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Player-summoned minion summoned by the Reaper Scythe's right-click cast.
 * Persistent, owner-tagged via {@link java.util.UUID}, follows owner and
 * fights hostiles. Idle behavior: mines precious ores within 3 blocks.
 *
 * <p>Stub — full behavior added in later plan tasks.
 */
public class ScytheSkeletonEntity extends Monster {
    public ScytheSkeletonEntity(EntityType<? extends ScytheSkeletonEntity> type, Level level) {
        super(type, level);
    }
}
