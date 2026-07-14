package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.EwokModel;
import com.tweeks.starwars.entity.EwokEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link EwokEntity}: small furry forest native via
 * {@link EwokModel}. Texture: {@code textures/entity/ewok.png}. Shadow 0.4.
 */
public class EwokRenderer
        extends HumanoidMobRenderer<EwokEntity, HumanoidRenderState, EwokModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/ewok.png");

    public EwokRenderer(EntityRendererProvider.Context context) {
        super(context, new EwokModel(context.bakeLayer(EwokModel.LAYER_LOCATION)), 0.4F);
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
