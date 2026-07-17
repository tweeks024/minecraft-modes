package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.JabbaModel;
import com.tweeks.starwars.entity.JabbaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link JabbaEntity}. Texture: {@code textures/entity/jabba.png}.
 */
public class JabbaRenderer extends MobRenderer<JabbaEntity, LivingEntityRenderState, JabbaModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/jabba.png");

    public JabbaRenderer(EntityRendererProvider.Context context) {
        super(context, new JabbaModel(context.bakeLayer(JabbaModel.LAYER_LOCATION)), 0.9F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
