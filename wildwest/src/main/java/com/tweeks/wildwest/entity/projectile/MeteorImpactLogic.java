package com.tweeks.wildwest.entity.projectile;

/**
 * Pure decision logic for meteor block-impact rewriting. Extracted as a
 * standalone class so it can be unit-tested without booting Minecraft —
 * the test passes booleans directly instead of a {@code BlockState}.
 *
 * <p>The actual {@code BlockState}-aware call site in {@link MeteorEntity}
 * computes {@code isAir} via {@code BlockState.isAir()} and {@code isDragonImmune}
 * via {@code state.is(BlockTags.DRAGON_IMMUNE)} before delegating here.
 */
public final class MeteorImpactLogic {
    private MeteorImpactLogic() {}

    /**
     * @param isAir          whether the impact block's state is air
     * @param isDragonImmune whether the impact block is in {@code #minecraft:dragon_immune}
     * @param isLiquid       whether the impact block has a non-empty fluid state
     *                       (e.g. water/lava source or waterlogged); skipping
     *                       these avoids draining rivers and turning lava
     *                       sources into magma blocks
     * @return {@code true} if the impact block should be replaced with magma
     */
    public static boolean shouldReplaceWithMagma(boolean isAir, boolean isDragonImmune, boolean isLiquid) {
        if (isAir) return false;
        if (isDragonImmune) return false;
        if (isLiquid) return false;
        return true;
    }
}
