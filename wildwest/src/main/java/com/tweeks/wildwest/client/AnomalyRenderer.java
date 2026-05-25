package com.tweeks.wildwest.client;

import com.tweeks.wildwest.client.model.AnomalyModel;
import com.tweeks.wildwest.entity.AnomalyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link AnomalyEntity}. Uses {@link AnomalyModel} (a villager-
 * shaped mesh copied from vanilla {@code VillagerModel}, plus a hinged
 * lower-jaw bone) and points at the vanilla villager base texture so the
 * disguised state reads as a plain villager. The {@link AnomalyRenderState}
 * carries the {@code revealed} flag that drives the jaw-open animation.
 *
 * <p>Texture is the vanilla path {@code minecraft:textures/entity/villager/villager.png}
 * — every client already ships this asset, so the mod doesn't need to vend
 * its own placeholder. When an artist authors a custom skin, swap the
 * identifier to {@code wildwest:textures/entity/anomaly.png} and ship the PNG.
 */
public class AnomalyRenderer
        extends MobRenderer<AnomalyEntity, AnomalyRenderState, AnomalyModel> {

    private static final Identifier TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/villager/villager.png");

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
