package com.tweeks.wildwest.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Passive death-save trinket dropped by Null. No active use — right-click
 * does nothing. Activation is event-driven (see
 * {@link com.tweeks.wildwest.event.VoidMarkHandler}): on otherwise-lethal
 * damage, one stack from the player's main inventory is consumed and the
 * player is teleported to their respawn point at 1 HP.
 *
 * <p>Returns {@link InteractionResult#PASS} so vanilla right-click behavior
 * (block placement, etc.) is unaffected. Mirrors NeoForge 26.x's
 * {@code InteractionResult}-returning {@code use} signature (the older
 * {@code InteractionResultHolder<ItemStack>} shape was removed).
 */
public class VoidMarkItem extends Item {

    public VoidMarkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }
}
