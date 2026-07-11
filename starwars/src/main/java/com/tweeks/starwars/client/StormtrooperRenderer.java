package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.StormtrooperModel;
import com.tweeks.starwars.entity.StormtrooperEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link StormtrooperEntity}: humanoid skeleton + {@link StormtrooperModel}'s
 * helmet cubes. Texture: {@code textures/entity/stormtrooper.png}. Held item
 * comes from the parent {@link HumanoidMobRenderer}'s built-in ItemInHandLayer.
 */
public class StormtrooperRenderer
        extends HumanoidMobRenderer<StormtrooperEntity, HumanoidRenderState, StormtrooperModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/stormtrooper.png");

    public StormtrooperRenderer(EntityRendererProvider.Context context) {
        super(context, new StormtrooperModel(context.bakeLayer(StormtrooperModel.LAYER_LOCATION)), 0.5F);
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
