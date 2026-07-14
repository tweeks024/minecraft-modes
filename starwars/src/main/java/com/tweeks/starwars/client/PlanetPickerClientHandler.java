package com.tweeks.starwars.client;

import com.tweeks.starwars.network.S2COpenPlanetPickerPacket;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-side handler: opens the planet picker for a validated gate. */
public final class PlanetPickerClientHandler {
    private PlanetPickerClientHandler() {
    }

    public static void handle(S2COpenPlanetPickerPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null) {
                mc.setScreen(new PlanetPickerScreen(pkt.origin(), pkt.axisX()));
            }
        });
    }
}
