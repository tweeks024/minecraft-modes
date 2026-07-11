package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.ObiWanModel;
import com.tweeks.starwars.entity.ObiWanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link ObiWanEntity}: humanoid skeleton + {@link ObiWanModel}'s
 * robe/hood cubes. Texture: {@code textures/entity/obi_wan.png}. Held item
 * comes from the parent {@link HumanoidMobRenderer}'s built-in ItemInHandLayer.
 */
public class ObiWanRenderer
        extends HumanoidMobRenderer<ObiWanEntity, HumanoidRenderState, ObiWanModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/obi_wan.png");

    public ObiWanRenderer(EntityRendererProvider.Context context) {
        super(context, new ObiWanModel(context.bakeLayer(ObiWanModel.LAYER_LOCATION)), 0.5F);
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
