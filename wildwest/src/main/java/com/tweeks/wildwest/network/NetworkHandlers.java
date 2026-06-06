package com.tweeks.wildwest.network;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.TracerClientHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = WildWestMod.MOD_ID)
public final class NetworkHandlers {
    private NetworkHandlers() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1");
        reg.playToClient(
            S2CTracerPacket.TYPE,
            S2CTracerPacket.STREAM_CODEC,
            TracerClientHandler::handle);
        reg.playToServer(
            C2SSetActiveStonePacket.TYPE,
            C2SSetActiveStonePacket.STREAM_CODEC,
            C2SSetActiveStonePacket::handle);
        reg.playToServer(
            C2SSetGauntletCommandsPacket.TYPE,
            C2SSetGauntletCommandsPacket.STREAM_CODEC,
            C2SSetGauntletCommandsPacket::handle);
    }
}
