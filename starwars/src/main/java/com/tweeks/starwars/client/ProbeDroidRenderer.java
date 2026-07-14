package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.ProbeDroidModel;
import com.tweeks.starwars.entity.ProbeDroidEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link ProbeDroidEntity}. Texture: {@code textures/entity/probe_droid.png}.
 * No shadow-to-ground contact look: small shadow, since the droid hovers.
 */
public class ProbeDroidRenderer extends MobRenderer<ProbeDroidEntity, LivingEntityRenderState, ProbeDroidModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/probe_droid.png");

    public ProbeDroidRenderer(EntityRendererProvider.Context context) {
        super(context, new ProbeDroidModel(context.bakeLayer(ProbeDroidModel.LAYER_LOCATION)), 0.4F);
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
