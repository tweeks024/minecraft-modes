package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.model.WalkerModel;
import com.tweeks.wildwest.entity.WalkerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class WalkerRenderer
        extends HumanoidMobRenderer<WalkerEntity, HumanoidRenderState, WalkerModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/walker.png");

    public WalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new WalkerModel(context.bakeLayer(WalkerModel.LAYER_LOCATION)), 0.5F);
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
