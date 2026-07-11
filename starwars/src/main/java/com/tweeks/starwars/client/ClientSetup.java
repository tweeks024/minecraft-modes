package com.tweeks.starwars.client;

import com.tweeks.starwars.ModEntities;
import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.BattleDroidModel;
import com.tweeks.starwars.client.model.JediKnightModel;
import com.tweeks.starwars.client.model.StormtrooperModel;
import com.tweeks.starwars.client.model.VaderModel;
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
        event.registerEntityRenderer(ModEntities.JEDI_KNIGHT.get(), JediKnightRenderer::new);
        event.registerEntityRenderer(ModEntities.DARTH_VADER.get(), VaderRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StormtrooperModel.LAYER_LOCATION, StormtrooperModel::createBodyLayer);
        event.registerLayerDefinition(BattleDroidModel.LAYER_LOCATION, BattleDroidModel::createBodyLayer);
        event.registerLayerDefinition(JediKnightModel.LAYER_LOCATION, JediKnightModel::createBodyLayer);
        event.registerLayerDefinition(VaderModel.LAYER_LOCATION, VaderModel::createBodyLayer);
    }
}
