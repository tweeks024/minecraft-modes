package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.YodaModel;
import com.tweeks.starwars.entity.YodaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link YodaEntity}: tiny humanoid via {@link YodaModel}.
 * Texture: {@code textures/entity/yoda.png}. The green saber comes from
 * the parent's built-in ItemInHandLayer.
 */
public class YodaRenderer
        extends HumanoidMobRenderer<YodaEntity, HumanoidRenderState, YodaModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/yoda.png");

    public YodaRenderer(EntityRendererProvider.Context context) {
        super(context, new YodaModel(context.bakeLayer(YodaModel.LAYER_LOCATION)), 0.3F);
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
