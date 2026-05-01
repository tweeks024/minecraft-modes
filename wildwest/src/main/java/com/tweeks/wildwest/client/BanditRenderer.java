package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.model.BanditModel;
import com.tweeks.wildwest.entity.BanditEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link BanditEntity}: humanoid + {@link BanditModel}'s
 * face-bandanna cube.
 */
public class BanditRenderer
        extends HumanoidMobRenderer<BanditEntity, HumanoidRenderState, BanditModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/bandit.png");

    public BanditRenderer(EntityRendererProvider.Context context) {
        super(context, new BanditModel(context.bakeLayer(BanditModel.LAYER_LOCATION)), 0.5F);
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
