package com.tweeks.wildwest.spawning;

import com.tweeks.wildwest.entity.BanditEntity;
import com.tweeks.wildwest.entity.BanditLeaderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;

public final class OutlawSpawnRules {
    private OutlawSpawnRules() {}

    public static boolean checkSpawnRules(EntityType<? extends BanditEntity> type,
                                          ServerLevelAccessor level,
                                          EntitySpawnReason reason,
                                          BlockPos pos,
                                          RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }

    public static boolean checkLeaderSpawnRules(EntityType<? extends BanditLeaderEntity> type,
                                                ServerLevelAccessor level,
                                                EntitySpawnReason reason,
                                                BlockPos pos,
                                                RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }
}
