package com.tweeks.wildwest.client;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.model.BanditLeaderModel;
import com.tweeks.wildwest.client.model.BanditModel;
import com.tweeks.wildwest.client.model.DeputyModel;
import com.tweeks.wildwest.client.model.SherrifModel;
import com.tweeks.wildwest.client.model.SteveStackerModel;
import com.tweeks.wildwest.client.model.WalkerModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * Client-only setup for the wildwest mod.
 *
 * <p>In MC 26.1.2 the legacy {@code ItemProperties.register(...)} API was
 * removed; the bolt-cycle rifle model swap that the plan called {@code
 * bolt_state} is now driven from the item-model JSON via the built-in
 * {@code minecraft:cooldown} numeric property selector
 * (see {@link net.minecraft.client.renderer.item.properties.numeric.Cooldown}).
 * Task 10 will define that JSON; no Java-side predicate registration is
 * needed here.
 */
@EventBusSubscriber(modid = WildWestMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BULLET.get(), BulletRenderer::new);
        event.registerEntityRenderer(ModEntities.DEPUTY.get(), DeputyRenderer::new);
        event.registerEntityRenderer(ModEntities.SHERRIF.get(), SherrifRenderer::new);
        event.registerEntityRenderer(ModEntities.BANDIT.get(), BanditRenderer::new);
        event.registerEntityRenderer(ModEntities.BANDIT_LEADER.get(), BanditLeaderRenderer::new);
        event.registerEntityRenderer(ModEntities.WALKER.get(), WalkerRenderer::new);
        event.registerEntityRenderer(ModEntities.STEVE_STACKER.get(), SteveStackerRenderer::new);
        event.registerEntityRenderer(ModEntities.HEROBRINE.get(), HerobrineRenderer::new);
        event.registerEntityRenderer(ModEntities.ENTITY_303.get(), Entity303Renderer::new);
        event.registerEntityRenderer(ModEntities.ENTITY_303_CLONE.get(), Entity303CloneRenderer::new);
        event.registerEntityRenderer(ModEntities.TAINTED_VIAL_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.METEOR.get(), ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        ZombifiedRenderHandler.registerModifier(event);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DeputyModel.LAYER_LOCATION, DeputyModel::createBodyLayer);
        event.registerLayerDefinition(SherrifModel.LAYER_LOCATION, SherrifModel::createBodyLayer);
        event.registerLayerDefinition(BanditModel.LAYER_LOCATION, BanditModel::createBodyLayer);
        event.registerLayerDefinition(BanditLeaderModel.LAYER_LOCATION, BanditLeaderModel::createBodyLayer);
        event.registerLayerDefinition(WalkerModel.LAYER_LOCATION, WalkerModel::createBodyLayer);
        event.registerLayerDefinition(SteveStackerModel.LAYER_LOCATION, SteveStackerModel::createBodyLayer);
    }
}
