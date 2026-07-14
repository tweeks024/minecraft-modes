package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.TuskenRaiderModel;
import com.tweeks.starwars.entity.TuskenRaiderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link TuskenRaiderEntity}: stormtrooper-rig humanoid via
 * {@link TuskenRaiderModel}. Texture: {@code textures/entity/tusken_raider.png}.
 */
public class TuskenRaiderRenderer
        extends HumanoidMobRenderer<TuskenRaiderEntity, HumanoidRenderState, TuskenRaiderModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/tusken_raider.png");

    public TuskenRaiderRenderer(EntityRendererProvider.Context context) {
        super(context, new TuskenRaiderModel(context.bakeLayer(TuskenRaiderModel.LAYER_LOCATION)), 0.5F);
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
