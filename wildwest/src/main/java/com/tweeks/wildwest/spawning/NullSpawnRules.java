package com.tweeks.wildwest.spawning;

import com.tweeks.wildwest.entity.NullSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Spawn-rules predicate for Null. Composes:
 *  <ol>
 *      <li>Vanilla {@link Monster#checkMonsterSpawnRules} (light ≤ 7 etc.)</li>
 *      <li>Overworld branch: requires open sky (mythic night-sky flavor)</li>
 *      <li>Nether + End branches: no sky requirement (Nether has no sky, End is partial)</li>
 *      <li>Reject all other dimensions defensively</li>
 *      <li>Singleton gate from {@link NullSavedData}</li>
 *  </ol>
 */
public final class NullSpawnRules {
    private NullSpawnRules() {}

    public static boolean checkSpawnRules(EntityType<? extends Monster> type,
                                          ServerLevelAccessor level,
                                          EntitySpawnReason reason,
                                          BlockPos pos,
                                          RandomSource random) {
        if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
            return false;
        }

        var dim = level.getLevel().dimension();
        if (dim == Level.OVERWORLD) {
            if (!level.canSeeSky(pos)) return false;
        } else if (dim != Level.NETHER && dim != Level.END) {
            return false;
        }

        MinecraftServer server = level.getLevel().getServer();
        if (server == null) return false;
        if (NullSavedData.get(server).isAlive()) return false;
        return true;
    }
}
