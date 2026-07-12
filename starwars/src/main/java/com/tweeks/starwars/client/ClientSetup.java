package com.tweeks.starwars.client;

import com.tweeks.starwars.ModEntities;
import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.AstromechModel;
import com.tweeks.starwars.client.model.BattleDroidModel;
import com.tweeks.starwars.client.model.BobaFettModel;
import com.tweeks.starwars.client.model.HanSoloModel;
import com.tweeks.starwars.client.model.JediKnightModel;
import com.tweeks.starwars.client.model.LukeModel;
import com.tweeks.starwars.client.model.ObiWanModel;
import com.tweeks.starwars.client.model.PrincessLeiaModel;
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
        event.registerEntityRenderer(ModEntities.LUKE_SKYWALKER.get(), LukeRenderer::new);
        event.registerEntityRenderer(ModEntities.OBI_WAN.get(), ObiWanRenderer::new);
        event.registerEntityRenderer(ModEntities.BOBA_FETT.get(), BobaFettRenderer::new);
        event.registerEntityRenderer(ModEntities.ASTROMECH.get(), AstromechRenderer::new);
        event.registerEntityRenderer(ModEntities.HAN_SOLO.get(), HanSoloRenderer::new);
        event.registerEntityRenderer(ModEntities.PRINCESS_LEIA.get(), PrincessLeiaRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StormtrooperModel.LAYER_LOCATION, StormtrooperModel::createBodyLayer);
        event.registerLayerDefinition(BattleDroidModel.LAYER_LOCATION, BattleDroidModel::createBodyLayer);
        event.registerLayerDefinition(JediKnightModel.LAYER_LOCATION, JediKnightModel::createBodyLayer);
        event.registerLayerDefinition(VaderModel.LAYER_LOCATION, VaderModel::createBodyLayer);
        event.registerLayerDefinition(LukeModel.LAYER_LOCATION, LukeModel::createBodyLayer);
        event.registerLayerDefinition(ObiWanModel.LAYER_LOCATION, ObiWanModel::createBodyLayer);
        event.registerLayerDefinition(BobaFettModel.LAYER_LOCATION, BobaFettModel::createBodyLayer);
        event.registerLayerDefinition(AstromechModel.LAYER_LOCATION, AstromechModel::createBodyLayer);
        event.registerLayerDefinition(HanSoloModel.LAYER_LOCATION, HanSoloModel::createBodyLayer);
        event.registerLayerDefinition(PrincessLeiaModel.LAYER_LOCATION, PrincessLeiaModel::createBodyLayer);
    }
}
