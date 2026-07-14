package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.RebelTrooperModel;
import com.tweeks.starwars.entity.RebelTrooperEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link RebelTrooperEntity}: stormtrooper-rig humanoid via
 * {@link RebelTrooperModel}. Texture: {@code textures/entity/rebel_trooper.png}.
 * Held blaster comes from the parent's built-in ItemInHandLayer.
 */
public class RebelTrooperRenderer
        extends HumanoidMobRenderer<RebelTrooperEntity, HumanoidRenderState, RebelTrooperModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/rebel_trooper.png");

    public RebelTrooperRenderer(EntityRendererProvider.Context context) {
        super(context, new RebelTrooperModel(context.bakeLayer(RebelTrooperModel.LAYER_LOCATION)), 0.5F);
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
