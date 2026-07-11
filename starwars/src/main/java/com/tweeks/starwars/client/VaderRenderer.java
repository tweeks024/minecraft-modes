package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.VaderModel;
import com.tweeks.starwars.entity.DarthVaderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link DarthVaderEntity}: humanoid skeleton + {@link VaderModel}'s
 * helmet/cape/chest-panel cubes. Texture: {@code textures/entity/darth_vader.png}.
 * Shadow radius bumped to {@code 0.6F} (vs. the mod's usual {@code 0.5F}) to
 * mark him out as an elite among the humanoid-sized mobs. Held item comes
 * from the parent {@link HumanoidMobRenderer}'s built-in ItemInHandLayer.
 */
public class VaderRenderer
        extends HumanoidMobRenderer<DarthVaderEntity, HumanoidRenderState, VaderModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/darth_vader.png");

    public VaderRenderer(EntityRendererProvider.Context context) {
        super(context, new VaderModel(context.bakeLayer(VaderModel.LAYER_LOCATION)), 0.6F);
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
