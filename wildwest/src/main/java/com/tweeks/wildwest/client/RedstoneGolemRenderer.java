package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.model.RedstoneGolemModel;
import com.tweeks.wildwest.entity.RedstoneGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for the {@link RedstoneGolemEntity}. Iron-golem-shaped mesh with
 * a placeholder redstone-red texture. Uses the vanilla
 * {@link LivingEntityRenderState} directly — no boss-specific synced data is
 * needed on the client at this stage.
 */
public class RedstoneGolemRenderer
        extends MobRenderer<RedstoneGolemEntity, LivingEntityRenderState, RedstoneGolemModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/redstone_golem.png");

    public RedstoneGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new RedstoneGolemModel(context.bakeLayer(RedstoneGolemModel.LAYER_LOCATION)), 0.7F);
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
