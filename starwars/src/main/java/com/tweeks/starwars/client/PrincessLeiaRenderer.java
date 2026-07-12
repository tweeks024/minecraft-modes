package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.PrincessLeiaModel;
import com.tweeks.starwars.entity.PrincessLeiaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link PrincessLeiaEntity}: humanoid skeleton +
 * {@link PrincessLeiaModel} buns/robe-skirt cubes. Texture:
 * {@code textures/entity/princess_leia.png}. Held item comes from the parent
 * {@link HumanoidMobRenderer}'s built-in ItemInHandLayer.
 */
public class PrincessLeiaRenderer
        extends HumanoidMobRenderer<PrincessLeiaEntity, HumanoidRenderState, PrincessLeiaModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/princess_leia.png");

    public PrincessLeiaRenderer(EntityRendererProvider.Context context) {
        super(context, new PrincessLeiaModel(context.bakeLayer(PrincessLeiaModel.LAYER_LOCATION)), 0.5F);
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
