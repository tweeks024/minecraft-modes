package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.model.DeputyModel;
import com.tweeks.wildwest.entity.DeputyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link DeputyEntity}: humanoid skeleton + {@link DeputyModel}'s
 * cowboy-hat cubes. Texture: {@code textures/entity/deputy.png}. Held item
 * comes from the parent {@link HumanoidMobRenderer}'s built-in ItemInHandLayer.
 */
public class DeputyRenderer
        extends HumanoidMobRenderer<DeputyEntity, HumanoidRenderState, DeputyModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/deputy.png");

    public DeputyRenderer(EntityRendererProvider.Context context) {
        super(context, new DeputyModel(context.bakeLayer(DeputyModel.LAYER_LOCATION)), 0.5F);
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
