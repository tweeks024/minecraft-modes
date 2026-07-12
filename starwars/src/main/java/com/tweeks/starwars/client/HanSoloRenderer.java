package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.HanSoloModel;
import com.tweeks.starwars.entity.HanSoloEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link HanSoloEntity}: humanoid skeleton + {@link HanSoloModel}
 * vest cube. Texture: {@code textures/entity/han_solo.png}. Held item comes
 * from the parent {@link HumanoidMobRenderer}'s built-in ItemInHandLayer.
 */
public class HanSoloRenderer
        extends HumanoidMobRenderer<HanSoloEntity, HumanoidRenderState, HanSoloModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/han_solo.png");

    public HanSoloRenderer(EntityRendererProvider.Context context) {
        super(context, new HanSoloModel(context.bakeLayer(HanSoloModel.LAYER_LOCATION)), 0.5F);
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
