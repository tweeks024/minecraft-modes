package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.model.CrabModel;
import com.tweeks.wildwest.entity.CrabEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class CrabRenderer extends MobRenderer<CrabEntity, CrabRenderState, CrabModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/crab.png");

    public CrabRenderer(EntityRendererProvider.Context context) {
        super(context, new CrabModel(context.bakeLayer(CrabModel.LAYER_LOCATION)), 0.3F);
    }

    @Override
    public CrabRenderState createRenderState() {
        return new CrabRenderState();
    }

    @Override
    public void extractRenderState(CrabEntity entity, CrabRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.pinchState.copyFrom(entity.pinchState);
    }

    @Override
    public Identifier getTextureLocation(CrabRenderState state) {
        return TEXTURE;
    }
}
