package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.model.BanditLeaderModel;
import com.tweeks.wildwest.entity.BanditLeaderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link BanditLeaderEntity}: humanoid +
 * {@link BanditLeaderModel}'s wide-brim hat + bandanna + cape.
 */
public class BanditLeaderRenderer
        extends HumanoidMobRenderer<BanditLeaderEntity, HumanoidRenderState, BanditLeaderModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/bandit_leader.png");

    public BanditLeaderRenderer(EntityRendererProvider.Context context) {
        super(context, new BanditLeaderModel(context.bakeLayer(BanditLeaderModel.LAYER_LOCATION)), 0.5F);
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
