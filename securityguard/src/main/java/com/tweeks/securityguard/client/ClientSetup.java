package com.tweeks.securityguard.client;

import com.tweeks.securityguard.Registration;
import com.tweeks.securityguard.SecurityGuardMod;
import com.tweeks.securityguard.client.model.BatonModel;
import com.tweeks.securityguard.client.model.SecurityGuardModel;
import com.tweeks.securityguard.client.renderer.SecurityGuardRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = SecurityGuardMod.MOD_ID, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Registration.SECURITY_GUARD.get(), SecurityGuardRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SecurityGuardModel.LAYER_LOCATION, SecurityGuardModel::createBodyLayer);
        event.registerLayerDefinition(BatonModel.LAYER_LOCATION, BatonModel::createLayer);
    }
}
