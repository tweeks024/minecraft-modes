package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.GroguModel;
import com.tweeks.starwars.entity.GroguEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link GroguEntity}: tiny humanoid via {@link GroguModel}.
 * Texture: {@code textures/entity/grogu.png}.
 */
public class GroguRenderer
        extends HumanoidMobRenderer<GroguEntity, HumanoidRenderState, GroguModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/grogu.png");

    public GroguRenderer(EntityRendererProvider.Context context) {
        super(context, new GroguModel(context.bakeLayer(GroguModel.LAYER_LOCATION)), 0.3F);
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
