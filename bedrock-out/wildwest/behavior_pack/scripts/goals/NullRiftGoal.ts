// TODO LLM: cache miss; run :translate --with-llm to translate
// Goal: com.tweeks.wildwest.entity.ai.NullRiftGoal
//
// This file is a placeholder. Either:
//   1. Run `./gradlew :translator:translate --with-llm` with ANTHROPIC_API_KEY set, or
//   2. Hand-translate the Java below to @minecraft/server event handlers.

/*
Original Java source — translate this:

package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.entity.NullEntity;
import com.tweeks.wildwest.entity.NullRiftEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Cooldown-gated rift spawner. Every 5 seconds, if Null can see the target
 * and the target is within 32 blocks (horizontal), spawn a NullRiftEntity at
 * the target's current XZ position snapped to ground.
 *
 * <p>Fire-and-forget — the rift entity owns its own 2s telegraph + 4s active
 * lifecycle and self-discards.
 */
public class NullRiftGoal extends Goal {

    private static final int COOLDOWN_TICKS = 100; // 5s @ 20tps
    private static final double RANGE = 32.0;

    private final NullEntity boss;
    private int cooldown = 0;

    public NullRiftGoal(NullEntity boss) {
        this.boss = boss;
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.boss.getTarget();
        if (target == null || !target.isAlive()) return false;

        double dxz2 = (target.getX() - this.boss.getX()) * (target.getX() - this.boss.getX())
                    + (target.getZ() - this.boss.getZ()) * (target.getZ() - this.boss.getZ());
        if (dxz2 > RANGE * RANGE) return false;

        return this.boss.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        LivingEntity target = this.boss.getTarget();
        if (target == null) return;
        this.cooldown = COOLDOWN_TICKS;
        if (!(this.boss.level() instanceof ServerLevel sl)) return;

        int tx = (int) Math.floor(target.getX());
        int tz = (int) Math.floor(target.getZ());
        int groundY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tx, tz);
        double rx = target.getX();
        double rz = target.getZ();

        NullRiftEntity rift = new NullRiftEntity(ModEntities.NULL_RIFT.get(), sl);
        rift.setPos(rx, groundY, rz);
        sl.addFreshEntity(rift);

        sl.playSound(null, BlockPos.containing(rx, groundY, rz),
            SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 0.6f, 0.4f);
    }
}

*/

// Empty handler so Bedrock's script engine accepts the file.
export {};
