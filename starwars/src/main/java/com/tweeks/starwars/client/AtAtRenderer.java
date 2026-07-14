package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.AtAtModel;
import com.tweeks.starwars.entity.AtAtEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Renderer for the {@link AtAtEntity}. Large shadow (radius 3.0) to match the walker's footprint. */
public class AtAtRenderer extends MobRenderer<AtAtEntity, LivingEntityRenderState, AtAtModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/at_at.png");

    public AtAtRenderer(EntityRendererProvider.Context context) {
        super(context, new AtAtModel(context.bakeLayer(AtAtModel.LAYER_LOCATION)), 3.0F);
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
