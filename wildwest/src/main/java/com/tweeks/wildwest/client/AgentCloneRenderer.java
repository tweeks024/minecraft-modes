package com.tweeks.wildwest.client;

import com.tweeks.wildwest.entity.AgentCloneEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Visually identical to {@link AgentRenderer}. Separate class only so the
 * generic parameter binds to {@link AgentCloneEntity}; reuses the same
 * texture and eyes layer for pixel-perfect mimicry.
 */
public class AgentCloneRenderer
        extends HumanoidMobRenderer<AgentCloneEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    public AgentCloneRenderer(EntityRendererProvider.Context context) {
        super(context,
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
            0.5F);
        this.addLayer(new AgentEyesLayer(this));
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return AgentRenderer.TEXTURE;
    }
}
