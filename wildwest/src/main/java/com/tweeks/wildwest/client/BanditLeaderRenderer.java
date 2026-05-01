package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.entity.BanditLeaderEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link BanditLeaderEntity}: vanilla {@link HumanoidModel} baked
 * from {@link ModelLayers#PLAYER}, bandit-leader texture, plus
 * {@link WildWestHeldItemLayer} extension hook. Item-in-hand rendering is
 * delivered by the parent {@link HumanoidMobRenderer} via its built-in
 * {@code ItemInHandLayer}.
 */
public class BanditLeaderRenderer
        extends HumanoidMobRenderer<BanditLeaderEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/bandit_leader.png");

    public BanditLeaderRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
