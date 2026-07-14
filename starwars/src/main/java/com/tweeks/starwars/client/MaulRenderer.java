package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.MaulModel;
import com.tweeks.starwars.entity.DarthMaulEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for {@link DarthMaulEntity}: humanoid skeleton + {@link MaulModel}'s
 * Zabrak horn crown. Texture: {@code textures/entity/darth_maul.png}. Shadow
 * radius {@code 0.5F} (the mod's usual humanoid size — Maul is lithe, not the
 * bulked-up elite that earns Vader's {@code 0.6F}). Held item (saberstaff,
 * equipped by the integrator) comes from the parent
 * {@link HumanoidMobRenderer}'s built-in ItemInHandLayer.
 */
public class MaulRenderer
        extends HumanoidMobRenderer<DarthMaulEntity, HumanoidRenderState, MaulModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/darth_maul.png");

    public MaulRenderer(EntityRendererProvider.Context context) {
        super(context, new MaulModel(context.bakeLayer(MaulModel.LAYER_LOCATION)), 0.5F);
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
