package com.tweeks.starwars.spawning;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;

public final class TrooperSpawnRules {
    private TrooperSpawnRules() {}

    public static boolean checkSpawnRules(EntityType<? extends Mob> type,
                                          ServerLevelAccessor level,
                                          EntitySpawnReason reason,
                                          BlockPos pos,
                                          RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }
}
