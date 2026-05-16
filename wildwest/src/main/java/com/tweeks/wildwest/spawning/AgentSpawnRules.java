package com.tweeks.wildwest.spawning;

import com.tweeks.wildwest.entity.AgentSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Spawn-rules predicate composing:
 *  <ol>
 *      <li>Vanilla {@link Monster#checkMonsterSpawnRules} (light ≤ 7 etc.)</li>
 *      <li>Overworld branch: requires open sky (mythic night-sky flavor)</li>
 *      <li>End branch: no sky requirement (End is partial-sky / no day cycle)</li>
 *      <li>Reject all other dimensions defensively</li>
 *      <li>Singleton gate from {@link AgentSavedData}</li>
 *  </ol>
 */
public final class AgentSpawnRules {
    private AgentSpawnRules() {}

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
        } else if (dim == Level.END) {
            // No sky requirement in the End.
        } else {
            // Defensive: biome modifiers shouldn't fire elsewhere, but reject anyway.
            return false;
        }

        MinecraftServer server = level.getLevel().getServer();
        if (server == null) return false;
        if (AgentSavedData.get(server).isAlive()) return false;
        return true;
    }
}
