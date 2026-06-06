package com.tweeks.wildwest.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tweeks.wildwest.Registration;
import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.item.InfinityStone;
import com.tweeks.wildwest.item.ModDataComponents;
import com.tweeks.wildwest.network.C2SSetActiveStonePacket;
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
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Client-side keybind that cycles the held Infinity Gauntlet's active
 * stone forward by one (0 → 1 → ... → 5 → 0). Default key: G.
 *
 * <p>The spec originally called for a radial picker {@code Screen}, but
 * MC 26.1.2 overhauled the {@code Screen} rendering API to use
 * {@code GuiGraphicsExtractor} with no clear migration path for custom
 * screens — see <a href="../../../../../../../../docs/superpowers/specs/2026-06-06-infinity-gauntlet-design.md">the design doc</a>.
 * Cycling is a pragmatic fallback that ships the feature today; the
 * radial picker can be revisited in a follow-up once examples exist
 * elsewhere in the codebase.
 */
@EventBusSubscriber(modid = WildWestMod.MOD_ID, value = Dist.CLIENT)
public final class InfinityGauntletKeybind {
    private InfinityGauntletKeybind() {}

    public static final KeyMapping CYCLE_STONE = new KeyMapping(
        "key.wildwest.infinity_gauntlet_cycle",
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_G,
        KeyMapping.Category.MISC);

    @SubscribeEvent
    public static void onRegister(RegisterKeyMappingsEvent event) {
        event.register(CYCLE_STONE);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        Player player = mc.player;
        if (player == null) return;

        while (CYCLE_STONE.consumeClick()) {
            InteractionHand hand = findGauntletHand(player);
            if (hand == null) continue;
            ItemStack stack = player.getItemInHand(hand);
            int current = stack.getOrDefault(ModDataComponents.ACTIVE_STONE.get(), 0);
            int next = (current + 1) % InfinityStone.values().length;
            ClientPacketDistributor.sendToServer(
                new C2SSetActiveStonePacket(next, hand == InteractionHand.MAIN_HAND));
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
