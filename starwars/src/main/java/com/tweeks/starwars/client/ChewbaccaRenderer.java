package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.ChewbaccaModel;
import com.tweeks.starwars.entity.ChewbaccaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link ChewbaccaEntity}: humanoid via {@link ChewbaccaModel}.
 * Texture: {@code textures/entity/chewbacca.png}. The held bowcaster comes
 * from the parent's built-in ItemInHandLayer.
 */
public class ChewbaccaRenderer
        extends HumanoidMobRenderer<ChewbaccaEntity, HumanoidRenderState, ChewbaccaModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/chewbacca.png");

    public ChewbaccaRenderer(EntityRendererProvider.Context context) {
        super(context, new ChewbaccaModel(context.bakeLayer(ChewbaccaModel.LAYER_LOCATION)), 0.6F);
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
