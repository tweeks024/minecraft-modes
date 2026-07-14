package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.JawaModel;
import com.tweeks.starwars.entity.JawaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link JawaEntity}: shrunken humanoid via {@link JawaModel}.
 * Texture: {@code textures/entity/jawa.png}.
 */
public class JawaRenderer
        extends HumanoidMobRenderer<JawaEntity, HumanoidRenderState, JawaModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/jawa.png");

    public JawaRenderer(EntityRendererProvider.Context context) {
        super(context, new JawaModel(context.bakeLayer(JawaModel.LAYER_LOCATION)), 0.3F);
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
