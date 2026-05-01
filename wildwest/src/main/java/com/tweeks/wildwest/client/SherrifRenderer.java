package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.model.SherrifModel;
import com.tweeks.wildwest.entity.SherrifEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link SherrifEntity}: humanoid + {@link SherrifModel}'s
 * wide-brim hat + sheriff star.
 */
public class SherrifRenderer
        extends HumanoidMobRenderer<SherrifEntity, HumanoidRenderState, SherrifModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/sherrif.png");

    public SherrifRenderer(EntityRendererProvider.Context context) {
        super(context, new SherrifModel(context.bakeLayer(SherrifModel.LAYER_LOCATION)), 0.5F);
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
