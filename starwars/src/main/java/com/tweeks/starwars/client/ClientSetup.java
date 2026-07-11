package com.tweeks.starwars.client;

import com.tweeks.starwars.ModEntities;
import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.BattleDroidModel;
import com.tweeks.starwars.client.model.StormtrooperModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = StarWarsMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.STORMTROOPER.get(), StormtrooperRenderer::new);
        event.registerEntityRenderer(ModEntities.BATTLE_DROID.get(), BattleDroidRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StormtrooperModel.LAYER_LOCATION, StormtrooperModel::createBodyLayer);
        event.registerLayerDefinition(BattleDroidModel.LAYER_LOCATION, BattleDroidModel::createBodyLayer);
    }
}
