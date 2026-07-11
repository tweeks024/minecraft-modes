package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.BobaFettModel;
import com.tweeks.starwars.entity.BobaFettEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link BobaFettEntity}: humanoid skeleton + {@link
 * BobaFettModel}'s helmet/rangefinder/jetpack cubes. Texture: {@code
 * textures/entity/boba_fett.png}. Shadow radius bumped to {@code 0.6F} (vs.
 * the mod's usual {@code 0.5F}) to mark him out as an elite among the
 * humanoid-sized mobs, matching {@code VaderRenderer}. Held item comes from
 * the parent {@link HumanoidMobRenderer}'s built-in ItemInHandLayer.
 */
public class BobaFettRenderer
        extends HumanoidMobRenderer<BobaFettEntity, HumanoidRenderState, BobaFettModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/boba_fett.png");

    public BobaFettRenderer(EntityRendererProvider.Context context) {
        super(context, new BobaFettModel(context.bakeLayer(BobaFettModel.LAYER_LOCATION)), 0.6F);
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
