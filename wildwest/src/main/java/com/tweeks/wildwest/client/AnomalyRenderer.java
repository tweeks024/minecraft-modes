package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.model.AnomalyModel;
import com.tweeks.wildwest.entity.AnomalyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link AnomalyEntity}. Uses a custom render-state subclass
 * so the model can swing its lower jaw open when the entity's synced
 * {@code DATA_REVEALED} flag is set.
 */
public class AnomalyRenderer
        extends HumanoidMobRenderer<AnomalyEntity, AnomalyRenderState, AnomalyModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/anomaly.png");

    public AnomalyRenderer(EntityRendererProvider.Context context) {
        super(context, new AnomalyModel(context.bakeLayer(AnomalyModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public AnomalyRenderState createRenderState() {
        return new AnomalyRenderState();
    }

    @Override
    public void extractRenderState(AnomalyEntity entity, AnomalyRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.revealed = entity.isRevealed();
    }

    @Override
    public Identifier getTextureLocation(AnomalyRenderState state) {
        return TEXTURE;
    }
}
