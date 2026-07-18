package com.tweeks.starwars.client;

import com.tweeks.starwars.ModEntities;
import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.AstromechModel;
import com.tweeks.starwars.client.model.AtAtModel;
import com.tweeks.starwars.client.model.BandDroidModel;
import com.tweeks.starwars.client.model.BanthaModel;
import com.tweeks.starwars.client.model.BattleDroidModel;
import com.tweeks.starwars.client.model.BobaFettModel;
import com.tweeks.starwars.client.model.BogwingModel;
import com.tweeks.starwars.client.model.ChewbaccaModel;
import com.tweeks.starwars.client.model.EwokModel;
import com.tweeks.starwars.client.model.GroguModel;
import com.tweeks.starwars.client.model.DragonsnakeModel;
import com.tweeks.starwars.client.model.HanSoloModel;
import com.tweeks.starwars.client.model.JawaModel;
import com.tweeks.starwars.client.model.JediKnightModel;
import com.tweeks.starwars.client.model.LandspeederModel;
import com.tweeks.starwars.client.model.LukeModel;
import com.tweeks.starwars.client.model.MaulModel;
import com.tweeks.starwars.client.model.ObiWanModel;
import com.tweeks.starwars.client.model.PrincessLeiaModel;
import com.tweeks.starwars.client.model.ProbeDroidModel;
import com.tweeks.starwars.client.model.RebelTrooperModel;
import com.tweeks.starwars.client.model.SpeederBikeModel;
import com.tweeks.starwars.client.model.StormtrooperModel;
import com.tweeks.starwars.client.model.TauntaunModel;
import com.tweeks.starwars.client.model.TieFighterModel;
import com.tweeks.starwars.client.model.XwingModel;
import com.tweeks.starwars.client.model.TuskenRaiderModel;
import com.tweeks.starwars.client.model.VaderModel;
import com.tweeks.starwars.client.model.WampaModel;
import com.tweeks.starwars.client.model.YodaModel;
import com.tweeks.starwars.client.model.RancorModel;
import com.tweeks.starwars.client.model.JabbaModel;
import com.tweeks.starwars.client.model.PalpatineModel;
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
        event.registerEntityRenderer(ModEntities.DARTH_MAUL.get(), MaulRenderer::new);
        event.registerEntityRenderer(ModEntities.LUKE_SKYWALKER.get(), LukeRenderer::new);
        event.registerEntityRenderer(ModEntities.OBI_WAN.get(), ObiWanRenderer::new);
        event.registerEntityRenderer(ModEntities.BOBA_FETT.get(), BobaFettRenderer::new);
        event.registerEntityRenderer(ModEntities.ASTROMECH.get(), AstromechRenderer::new);
        event.registerEntityRenderer(ModEntities.HAN_SOLO.get(), HanSoloRenderer::new);
        event.registerEntityRenderer(ModEntities.PRINCESS_LEIA.get(), PrincessLeiaRenderer::new);
        event.registerEntityRenderer(ModEntities.LANDSPEEDER.get(), LandspeederRenderer::new);
        event.registerEntityRenderer(ModEntities.JAWA.get(), JawaRenderer::new);
        event.registerEntityRenderer(ModEntities.TUSKEN_RAIDER.get(), TuskenRaiderRenderer::new);
        event.registerEntityRenderer(ModEntities.BANTHA.get(), BanthaRenderer::new);
        event.registerEntityRenderer(ModEntities.REBEL_TROOPER.get(), RebelTrooperRenderer::new);
        event.registerEntityRenderer(ModEntities.PROBE_DROID.get(), ProbeDroidRenderer::new);
        event.registerEntityRenderer(ModEntities.WAMPA.get(), WampaRenderer::new);
        event.registerEntityRenderer(ModEntities.TAUNTAUN.get(), TauntaunRenderer::new);
        event.registerEntityRenderer(ModEntities.SNOWTROOPER.get(), SnowtrooperRenderer::new);
        event.registerEntityRenderer(ModEntities.DRAGONSNAKE.get(), DragonsnakeRenderer::new);
        event.registerEntityRenderer(ModEntities.BOGWING.get(), BogwingRenderer::new);
        event.registerEntityRenderer(ModEntities.YODA.get(), YodaRenderer::new);
        // wave 3
        event.registerEntityRenderer(ModEntities.SPEEDER_BIKE.get(), SpeederBikeRenderer::new);
        event.registerEntityRenderer(ModEntities.XWING.get(), XwingRenderer::new);
        event.registerEntityRenderer(ModEntities.TIE_FIGHTER.get(), TieFighterRenderer::new);
        event.registerEntityRenderer(ModEntities.AT_AT.get(), AtAtRenderer::new);
        event.registerEntityRenderer(ModEntities.BAND_DROID.get(), BandDroidRenderer::new);
        // companions
        event.registerEntityRenderer(ModEntities.CHEWBACCA.get(), ChewbaccaRenderer::new);
        event.registerEntityRenderer(ModEntities.GROGU.get(), GroguRenderer::new);
        event.registerEntityRenderer(ModEntities.EWOK.get(), EwokRenderer::new);
        event.registerEntityRenderer(ModEntities.RANCOR.get(), RancorRenderer::new);
        event.registerEntityRenderer(ModEntities.JABBA.get(), JabbaRenderer::new);
        event.registerEntityRenderer(ModEntities.PALPATINE.get(), PalpatineRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StormtrooperModel.LAYER_LOCATION, StormtrooperModel::createBodyLayer);
        event.registerLayerDefinition(BattleDroidModel.LAYER_LOCATION, BattleDroidModel::createBodyLayer);
        event.registerLayerDefinition(JediKnightModel.LAYER_LOCATION, JediKnightModel::createBodyLayer);
        event.registerLayerDefinition(VaderModel.LAYER_LOCATION, VaderModel::createBodyLayer);
        event.registerLayerDefinition(MaulModel.LAYER_LOCATION, MaulModel::createBodyLayer);
        event.registerLayerDefinition(LukeModel.LAYER_LOCATION, LukeModel::createBodyLayer);
        event.registerLayerDefinition(ObiWanModel.LAYER_LOCATION, ObiWanModel::createBodyLayer);
        event.registerLayerDefinition(BobaFettModel.LAYER_LOCATION, BobaFettModel::createBodyLayer);
        event.registerLayerDefinition(AstromechModel.LAYER_LOCATION, AstromechModel::createBodyLayer);
        event.registerLayerDefinition(HanSoloModel.LAYER_LOCATION, HanSoloModel::createBodyLayer);
        event.registerLayerDefinition(PrincessLeiaModel.LAYER_LOCATION, PrincessLeiaModel::createBodyLayer);
        event.registerLayerDefinition(LandspeederModel.LAYER_LOCATION, LandspeederModel::createBodyLayer);
        event.registerLayerDefinition(JawaModel.LAYER_LOCATION, JawaModel::createBodyLayer);
        event.registerLayerDefinition(TuskenRaiderModel.LAYER_LOCATION, TuskenRaiderModel::createBodyLayer);
        event.registerLayerDefinition(BanthaModel.LAYER_LOCATION, BanthaModel::createBodyLayer);
        event.registerLayerDefinition(RebelTrooperModel.LAYER_LOCATION, RebelTrooperModel::createBodyLayer);
        event.registerLayerDefinition(ProbeDroidModel.LAYER_LOCATION, ProbeDroidModel::createBodyLayer);
        event.registerLayerDefinition(WampaModel.LAYER_LOCATION, WampaModel::createBodyLayer);
        event.registerLayerDefinition(TauntaunModel.LAYER_LOCATION, TauntaunModel::createBodyLayer);
        // Snowtrooper reuses StormtrooperModel's layer — no new definition.
        event.registerLayerDefinition(DragonsnakeModel.LAYER_LOCATION, DragonsnakeModel::createBodyLayer);
        event.registerLayerDefinition(BogwingModel.LAYER_LOCATION, BogwingModel::createBodyLayer);
        event.registerLayerDefinition(YodaModel.LAYER_LOCATION, YodaModel::createBodyLayer);
        // wave 3
        event.registerLayerDefinition(SpeederBikeModel.LAYER_LOCATION, SpeederBikeModel::createBodyLayer);
        event.registerLayerDefinition(XwingModel.LAYER_LOCATION, XwingModel::createBodyLayer);
        event.registerLayerDefinition(TieFighterModel.LAYER_LOCATION, TieFighterModel::createBodyLayer);
        event.registerLayerDefinition(AtAtModel.LAYER_LOCATION, AtAtModel::createBodyLayer);
        event.registerLayerDefinition(BandDroidModel.LAYER_LOCATION, BandDroidModel::createBodyLayer);
        // companions
        event.registerLayerDefinition(ChewbaccaModel.LAYER_LOCATION, ChewbaccaModel::createBodyLayer);
        event.registerLayerDefinition(GroguModel.LAYER_LOCATION, GroguModel::createBodyLayer);
        event.registerLayerDefinition(EwokModel.LAYER_LOCATION, EwokModel::createBodyLayer);
        event.registerLayerDefinition(RancorModel.LAYER_LOCATION, RancorModel::createBodyLayer);
        event.registerLayerDefinition(JabbaModel.LAYER_LOCATION, JabbaModel::createBodyLayer);
        event.registerLayerDefinition(PalpatineModel.LAYER_LOCATION, PalpatineModel::createBodyLayer);
    }
}
