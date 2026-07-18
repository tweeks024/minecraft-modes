package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.PalpatineModel;
import com.tweeks.starwars.entity.PalpatineEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link PalpatineEntity}: the hooded {@link PalpatineModel} on
 * the plain {@link MobRenderer} skeleton (no held-item layer — the Emperor
 * carries no weapon). Texture: {@code textures/entity/palpatine.png}.
 */
public class PalpatineRenderer
        extends MobRenderer<PalpatineEntity, LivingEntityRenderState, PalpatineModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/palpatine.png");

    public PalpatineRenderer(EntityRendererProvider.Context context) {
        super(context, new PalpatineModel(context.bakeLayer(PalpatineModel.LAYER_LOCATION)), 0.5F);
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
