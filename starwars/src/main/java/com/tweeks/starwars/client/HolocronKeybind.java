package com.tweeks.starwars.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.tweeks.starwars.Registration;
import com.tweeks.starwars.StarWarsMod;
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
import org.slf4j.Logger;

/**
 * Client-side keybind that opens the {@link ForcePickerScreen} when the
 * player is holding a Holocron. Default key: H.
 */
@EventBusSubscriber(modid = StarWarsMod.MOD_ID, value = Dist.CLIENT)
public final class HolocronKeybind {
    private HolocronKeybind() {}

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final KeyMapping OPEN_FORCE_PICKER = new KeyMapping(
        "key.starwars.open_force_picker",
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_H,
        KeyMapping.Category.MISC);

    @SubscribeEvent
    public static void onRegister(RegisterKeyMappingsEvent event) {
        event.register(OPEN_FORCE_PICKER);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        Player player = mc.player;
        if (player == null) return;

        boolean opened = false;
        while (OPEN_FORCE_PICKER.consumeClick()) {
            // Drain queued clicks but only open one screen. Without this,
            // rapid taps would call setScreen() repeatedly, leaking instances
            // whose lifecycle never completes.
            if (opened) continue;
            InteractionHand hand = findHolocronHand(player);
            if (hand == null) {
                continue;
            }
            boolean mainHand = hand == InteractionHand.MAIN_HAND;
            mc.setScreen(new ForcePickerScreen(mainHand));
            opened = true;
        }
    }

    private static InteractionHand findHolocronHand(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(Registration.HOLOCRON.get())) return InteractionHand.MAIN_HAND;
        ItemStack off = player.getOffhandItem();
        if (off.is(Registration.HOLOCRON.get())) return InteractionHand.OFF_HAND;
        return null;
    }
}
