package com.tweeks.wildwest.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Fifth apex boss for the :wildwest mod. Singleton, overworld-night spawn.
 * Glass-cannon minion master that raises Sharpness-3 skeletons and uses
 * Soul Lift to launch players for fall damage. Drops the Reaper Scythe.
 *
 * <p>Stub — full behavior added in later plan tasks.
 */
public class GrimReaperEntity extends Monster {
    public GrimReaperEntity(EntityType<? extends GrimReaperEntity> type, Level level) {
        super(type, level);
    }
}
