package com.tweeks.starwars.client;

import com.tweeks.starwars.network.S2CGalaxyMapPacket;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-side handler: opens the galaxy map with fresh server data. */
public final class GalaxyMapClientHandler {
    private GalaxyMapClientHandler() {
    }

    public static void handle(S2CGalaxyMapPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null) {
                mc.setScreen(new GalaxyMapScreen(pkt));
            }
        });
    }
}
