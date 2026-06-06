package com.tweeks.wildwest.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tweeks.wildwest.Registration;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * Client-side keybind that opens the {@link RadialPickerScreen} when the
 * player is holding an Infinity Gauntlet. Default key: G.
 */
@EventBusSubscriber(modid = WildWestMod.MOD_ID, value = Dist.CLIENT)
public final class InfinityGauntletKeybind {
    private InfinityGauntletKeybind() {}

    public static final KeyMapping OPEN_RADIAL = new KeyMapping(
        "key.wildwest.infinity_gauntlet_radial",
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_G,
        KeyMapping.Category.MISC);

    @SubscribeEvent
    public static void onRegister(RegisterKeyMappingsEvent event) {
        event.register(OPEN_RADIAL);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        Player player = mc.player;
        if (player == null) return;

        boolean opened = false;
        while (OPEN_RADIAL.consumeClick()) {
            // Drain queued clicks but only open the screen once. Without this,
            // rapid taps would call setScreen() repeatedly, leaking instances
            // of RadialPickerScreen whose lifecycle never completes.
            if (opened) continue;
            InteractionHand hand = findGauntletHand(player);
            if (hand != null) {
                mc.setScreen(new RadialPickerScreen(hand == InteractionHand.MAIN_HAND));
                opened = true;
            }
        }
    }

    private static InteractionHand findGauntletHand(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(Registration.INFINITY_GAUNTLET.get())) return InteractionHand.MAIN_HAND;
        ItemStack off = player.getOffhandItem();
        if (off.is(Registration.INFINITY_GAUNTLET.get())) return InteractionHand.OFF_HAND;
        return null;
    }
}
