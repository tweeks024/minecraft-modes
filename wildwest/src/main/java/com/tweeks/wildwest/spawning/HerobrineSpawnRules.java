package com.tweeks.wildwest.spawning;

import com.tweeks.wildwest.entity.HerobrineSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Spawn-rules predicate composing:
 *  <ol>
 *      <li>Vanilla {@link Monster#checkMonsterSpawnRules} (light ≤ 7 etc.)</li>
 *      <li>Open-sky requirement (mythic flavor)</li>
 *      <li>Singleton gate from {@link HerobrineSavedData}</li>
 *  </ol>
 */
public final class HerobrineSpawnRules {
    private HerobrineSpawnRules() {}

    public static boolean checkSpawnRules(EntityType<? extends Monster> type,
                                          ServerLevelAccessor level,
                                          EntitySpawnReason reason,
                                          BlockPos pos,
                                          RandomSource random) {
        if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        if (!level.canSeeSky(pos)) {
            return false;
        }
        MinecraftServer server = level.getLevel().getServer();
        if (server == null) return false;
        if (HerobrineSavedData.get(server).isAlive()) return false;
        return true;
    }
}
