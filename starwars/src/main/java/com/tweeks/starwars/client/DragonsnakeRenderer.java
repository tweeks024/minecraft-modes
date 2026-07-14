package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.DragonsnakeModel;
import com.tweeks.starwars.entity.DragonsnakeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link DragonsnakeEntity}. Texture: {@code textures/entity/dragonsnake.png}.
 */
public class DragonsnakeRenderer extends MobRenderer<DragonsnakeEntity, LivingEntityRenderState, DragonsnakeModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/dragonsnake.png");

    public DragonsnakeRenderer(EntityRendererProvider.Context context) {
        super(context, new DragonsnakeModel(context.bakeLayer(DragonsnakeModel.LAYER_LOCATION)), 0.6F);
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
