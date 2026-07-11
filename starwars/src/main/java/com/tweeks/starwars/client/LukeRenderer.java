package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.LukeModel;
import com.tweeks.starwars.entity.LukeSkywalkerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link LukeSkywalkerEntity}: plain humanoid skeleton +
 * {@link LukeModel}. Texture: {@code textures/entity/luke_skywalker.png}.
 * Held item comes from the parent {@link HumanoidMobRenderer}'s built-in
 * ItemInHandLayer.
 */
public class LukeRenderer
        extends HumanoidMobRenderer<LukeSkywalkerEntity, HumanoidRenderState, LukeModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/luke_skywalker.png");

    public LukeRenderer(EntityRendererProvider.Context context) {
        super(context, new LukeModel(context.bakeLayer(LukeModel.LAYER_LOCATION)), 0.5F);
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
