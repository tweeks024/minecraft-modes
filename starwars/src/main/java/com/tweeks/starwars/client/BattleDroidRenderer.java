package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.BattleDroidModel;
import com.tweeks.starwars.entity.BattleDroidEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link BattleDroidEntity}: custom spindly skeleton via
 * {@link BattleDroidModel}. Texture: {@code textures/entity/battle_droid.png}.
 * Held item comes from the parent {@link HumanoidMobRenderer}'s built-in
 * ItemInHandLayer.
 */
public class BattleDroidRenderer
        extends HumanoidMobRenderer<BattleDroidEntity, HumanoidRenderState, BattleDroidModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/battle_droid.png");

    public BattleDroidRenderer(EntityRendererProvider.Context context) {
        super(context, new BattleDroidModel(context.bakeLayer(BattleDroidModel.LAYER_LOCATION)), 0.5F);
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
