package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.BanthaModel;
import com.tweeks.starwars.entity.BanthaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link BanthaEntity}. Texture: {@code textures/entity/bantha.png}.
 */
public class BanthaRenderer extends MobRenderer<BanthaEntity, LivingEntityRenderState, BanthaModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/bantha.png");

    public BanthaRenderer(EntityRendererProvider.Context context) {
        super(context, new BanthaModel(context.bakeLayer(BanthaModel.LAYER_LOCATION)), 1.0F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
