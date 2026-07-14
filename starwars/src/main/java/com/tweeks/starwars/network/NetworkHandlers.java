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
        // Client-bound handlers are registered as lambdas, NOT method
        // references: a method reference resolves the client class at
        // registration time, which crashes the dedicated server (client
        // classes are masked there). A lambda defers linkage until a packet
        // actually arrives, which only happens on the client.
        reg.playToClient(
            S2CBlasterTracerPacket.TYPE,
            S2CBlasterTracerPacket.STREAM_CODEC,
            (pkt, ctx) -> TracerClientHandler.handle(pkt, ctx));
        reg.playToServer(
            C2SSelectPowerPacket.TYPE,
            C2SSelectPowerPacket.STREAM_CODEC,
            C2SSelectPowerPacket::handle);
        reg.playToClient(
            S2COpenPlanetPickerPacket.TYPE,
            S2COpenPlanetPickerPacket.STREAM_CODEC,
            (pkt, ctx) -> com.tweeks.starwars.client.PlanetPickerClientHandler.handle(pkt, ctx));
        reg.playToServer(
            C2SSelectPlanetPacket.TYPE,
            C2SSelectPlanetPacket.STREAM_CODEC,
            C2SSelectPlanetPacket::handle);
        reg.playToClient(
            S2CGalaxyMapPacket.TYPE,
            S2CGalaxyMapPacket.STREAM_CODEC,
            (pkt, ctx) -> com.tweeks.starwars.client.GalaxyMapClientHandler.handle(pkt, ctx));
    }
}
