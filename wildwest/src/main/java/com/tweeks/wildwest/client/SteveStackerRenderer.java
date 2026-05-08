package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.model.SteveStackerModel;
import com.tweeks.wildwest.entity.SteveStackerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class SteveStackerRenderer
        extends MobRenderer<SteveStackerEntity, SteveStackerRenderState, SteveStackerModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/steve_stacker.png");

    public SteveStackerRenderer(EntityRendererProvider.Context context) {
        super(context, new SteveStackerModel(context.bakeLayer(SteveStackerModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public SteveStackerRenderState createRenderState() {
        return new SteveStackerRenderState();
    }

    @Override
    public void extractRenderState(SteveStackerEntity entity, SteveStackerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.stackHeight = entity.getStackHeight();
    }

    @Override
    public Identifier getTextureLocation(SteveStackerRenderState state) {
        return TEXTURE;
    }
}
