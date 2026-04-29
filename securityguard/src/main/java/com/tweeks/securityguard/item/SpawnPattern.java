package com.tweeks.securityguard.item;

import net.minecraft.core.BlockPos;

import java.util.function.Predicate;

/**
 * Pure-function validation of the Security Guard spawn column.
 *
 * <p>A valid pattern is:
 * <ul>
 *   <li>The clicked-on block and the two below it are all iron blocks.</li>
 *   <li>The two blocks directly above the top are passable (so the entity has room to spawn).</li>
 * </ul>
 *
 * <p>Takes predicates over {@link BlockPos} rather than reading a {@code Level}
 * so it can be unit-tested without booting the Minecraft registry system.
 */
public final class SpawnPattern {
    private SpawnPattern() {}

    public static boolean matches(Predicate<BlockPos> isIron, Predicate<BlockPos> isPassable, BlockPos top) {
        for (int dy = 0; dy >= -2; dy--) {
            if (!isIron.test(top.offset(0, dy, 0))) {
                return false;
            }
        }
        for (int dy = 1; dy <= 2; dy++) {
            if (!isPassable.test(top.offset(0, dy, 0))) {
                return false;
            }
        }
        return true;
    }
}
