package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.BogwingModel;
import com.tweeks.starwars.entity.BogwingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link BogwingEntity}. Texture: {@code textures/entity/bogwing.png}.
 */
public class BogwingRenderer extends MobRenderer<BogwingEntity, LivingEntityRenderState, BogwingModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/bogwing.png");

    public BogwingRenderer(EntityRendererProvider.Context context) {
        super(context, new BogwingModel(context.bakeLayer(BogwingModel.LAYER_LOCATION)), 0.2F);
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
