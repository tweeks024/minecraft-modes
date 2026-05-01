package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.entity.DeputyEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link DeputyEntity}: vanilla {@link HumanoidModel} baked from
 * {@link ModelLayers#PLAYER}, deputy texture, plus {@link WildWestHeldItemLayer}
 * for future hand-item customisation. (The parent {@link HumanoidMobRenderer}
 * already adds vanilla {@code ItemInHandLayer} which renders the synced
 * MAINHAND item, so no override of {@code extractRenderState} is needed —
 * {@code rightHandItemStack} / {@code rightHandItemState} are populated by
 * the super-class.)
 */
public class DeputyRenderer
        extends HumanoidMobRenderer<DeputyEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/deputy.png");

    public DeputyRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new WildWestHeldItemLayer(this));
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
