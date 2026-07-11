package com.tweeks.starwars.network;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.TracerClientHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = StarWarsMod.MOD_ID)
public final class NetworkHandlers {
    private NetworkHandlers() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1");
        reg.playToClient(
            S2CBlasterTracerPacket.TYPE,
            S2CBlasterTracerPacket.STREAM_CODEC,
            TracerClientHandler::handle);
        reg.playToServer(
            C2SSelectPowerPacket.TYPE,
            C2SSelectPowerPacket.STREAM_CODEC,
            C2SSelectPowerPacket::handle);
    }
}
