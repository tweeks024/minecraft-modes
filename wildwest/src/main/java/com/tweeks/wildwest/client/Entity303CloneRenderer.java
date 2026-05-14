package com.tweeks.wildwest.client;

import com.tweeks.wildwest.entity.Entity303CloneEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Visually identical to {@link Entity303Renderer}. Separate class only so the
 * generic parameter binds to {@link Entity303CloneEntity}; reuses the same
 * texture and eyes layer for pixel-perfect mimicry.
 */
public class Entity303CloneRenderer
        extends HumanoidMobRenderer<Entity303CloneEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    public Entity303CloneRenderer(EntityRendererProvider.Context context) {
        super(context,
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
            0.5F);
        this.addLayer(new Entity303EyesLayer(this));
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return Entity303Renderer.TEXTURE;
    }
}
