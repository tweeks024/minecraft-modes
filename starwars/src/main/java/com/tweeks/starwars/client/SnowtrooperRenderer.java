package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.StormtrooperModel;
import com.tweeks.starwars.entity.SnowtrooperEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link SnowtrooperEntity}: reuses the
 * {@link StormtrooperModel} geometry (same baked layer — no new layer
 * definition registered) with the snowtrooper texture. Texture:
 * {@code textures/entity/snowtrooper.png}.
 */
public class SnowtrooperRenderer
        extends HumanoidMobRenderer<SnowtrooperEntity, HumanoidRenderState, StormtrooperModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/snowtrooper.png");

    public SnowtrooperRenderer(EntityRendererProvider.Context context) {
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
