package com.tweeks.wildwest.spawning;

import com.tweeks.wildwest.entity.GrimReaperSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Spawn-rules predicate for the Grim Reaper. Overworld-only, night,
 * open sky, strict darkness (light &lt; 4), singleton-gated.
 */
public final class GrimReaperSpawnRules {
    private GrimReaperSpawnRules() {}

    public static boolean checkSpawnRules(EntityType<? extends Monster> type,
                                          ServerLevelAccessor level,
                                          EntitySpawnReason reason,
                                          BlockPos pos,
                                          RandomSource random) {
        if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
            return false;
        }

        if (level.getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }

        if (!level.canSeeSky(pos)) {
            return false;
        }

        if (level.getMaxLocalRawBrightness(pos) >= 4) {
            return false;
        }

        MinecraftServer server = level.getLevel().getServer();
        if (server == null) return false;
        if (GrimReaperSavedData.get(server).isAlive()) return false;

        return true;
    }
}
