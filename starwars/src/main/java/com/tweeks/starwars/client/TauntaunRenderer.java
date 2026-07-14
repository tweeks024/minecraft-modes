package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.TauntaunModel;
import com.tweeks.starwars.entity.TauntaunEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link TauntaunEntity}. Texture: {@code textures/entity/tauntaun.png}.
 */
public class TauntaunRenderer extends MobRenderer<TauntaunEntity, LivingEntityRenderState, TauntaunModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/tauntaun.png");

    public TauntaunRenderer(EntityRendererProvider.Context context) {
        super(context, new TauntaunModel(context.bakeLayer(TauntaunModel.LAYER_LOCATION)), 0.6F);
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
