package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.WampaModel;
import com.tweeks.starwars.entity.WampaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link WampaEntity}. Texture: {@code textures/entity/wampa.png}.
 */
public class WampaRenderer extends MobRenderer<WampaEntity, LivingEntityRenderState, WampaModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/wampa.png");

    public WampaRenderer(EntityRendererProvider.Context context) {
        super(context, new WampaModel(context.bakeLayer(WampaModel.LAYER_LOCATION)), 0.8F);
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
