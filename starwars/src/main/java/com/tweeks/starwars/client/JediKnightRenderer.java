package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.JediKnightModel;
import com.tweeks.starwars.entity.JediKnightEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link JediKnightEntity}: humanoid skeleton + {@link JediKnightModel}'s
 * robe/hood cubes. Texture: {@code textures/entity/jedi_knight.png}. Held item
 * comes from the parent {@link HumanoidMobRenderer}'s built-in ItemInHandLayer.
 */
public class JediKnightRenderer
        extends HumanoidMobRenderer<JediKnightEntity, HumanoidRenderState, JediKnightModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/jedi_knight.png");

    public JediKnightRenderer(EntityRendererProvider.Context context) {
        super(context, new JediKnightModel(context.bakeLayer(JediKnightModel.LAYER_LOCATION)), 0.5F);
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
