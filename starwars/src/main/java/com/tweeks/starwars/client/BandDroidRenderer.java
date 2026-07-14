package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.BandDroidModel;
import com.tweeks.starwars.entity.BandDroidEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Renderer for the {@link BandDroidEntity}. */
public class BandDroidRenderer extends MobRenderer<BandDroidEntity, LivingEntityRenderState, BandDroidModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/band_droid.png");

    public BandDroidRenderer(EntityRendererProvider.Context context) {
        super(context, new BandDroidModel(context.bakeLayer(BandDroidModel.LAYER_LOCATION)), 0.3F);
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
