package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.RancorModel;
import com.tweeks.starwars.entity.RancorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link RancorEntity}. Texture: {@code textures/entity/rancor.png}.
 */
public class RancorRenderer extends MobRenderer<RancorEntity, LivingEntityRenderState, RancorModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/rancor.png");

    public RancorRenderer(EntityRendererProvider.Context context) {
        super(context, new RancorModel(context.bakeLayer(RancorModel.LAYER_LOCATION)), 1.2F);
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
