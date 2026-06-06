package com.tweeks.wildwest.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The Infinity Gauntlet. Six stones, each with its own active ability.
 *
 * <p>State lives on the stack via two {@link ModDataComponents}:
 * {@code ACTIVE_STONE} (index 0..5) and {@code COOLDOWNS} (long[6]).
 *
 * <p>This skeleton registers the item only — abilities and tooltip are
 * filled in by later tasks.
 */
public class InfinityGauntletItem extends Item {

    public static final int DURABILITY = 500;

    public InfinityGauntletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }
}
