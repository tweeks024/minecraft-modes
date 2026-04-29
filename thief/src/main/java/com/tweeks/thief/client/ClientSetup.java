package com.tweeks.thief.client;

import com.tweeks.thief.Registration;
import com.tweeks.thief.ThiefMod;
import com.tweeks.thief.client.model.BlackjackModel;
import com.tweeks.thief.client.model.ThiefModel;
import com.tweeks.thief.client.renderer.ThiefRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ThiefMod.MOD_ID, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Registration.THIEF.get(), ThiefRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ThiefModel.LAYER_LOCATION, ThiefModel::createBodyLayer);
        event.registerLayerDefinition(BlackjackModel.LAYER_LOCATION, BlackjackModel::createLayer);
    }
}
