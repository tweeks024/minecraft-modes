package com.tweeks.thief.client.renderer;

import com.tweeks.securitycore.client.HeldItemLayer;
import com.tweeks.thief.ThiefMod;
import com.tweeks.thief.client.model.BlackjackModel;
import com.tweeks.thief.client.model.ThiefModel;
import com.tweeks.thief.entity.RevealState;
import com.tweeks.thief.entity.ThiefEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class ThiefRenderer
        extends HumanoidMobRenderer<ThiefEntity, HumanoidRenderState, ThiefModel> {

    private static final Identifier DISGUISED_TEXTURE = Identifier.fromNamespaceAndPath(
        ThiefMod.MOD_ID, "textures/entity/thief_disguised.png");
    private static final Identifier REVEALED_TEXTURE = Identifier.fromNamespaceAndPath(
        ThiefMod.MOD_ID, "textures/entity/thief_revealed.png");
    private static final Identifier BLACKJACK_TEXTURE = Identifier.fromNamespaceAndPath(
        ThiefMod.MOD_ID, "textures/entity/blackjack.png");

    private RevealState lastRevealState = RevealState.DISGUISED;

    public ThiefRenderer(EntityRendererProvider.Context context) {
        super(context, new ThiefModel(context.bakeLayer(ThiefModel.LAYER_LOCATION)), 0.5f);
        BlackjackModel blackjackModel = new BlackjackModel(context.bakeLayer(BlackjackModel.LAYER_LOCATION));
        this.addLayer(new HeldItemLayer<>(this,
            blackjackModel,
            BLACKJACK_TEXTURE,
            -0.0625f, 0.625f, 0.0f,
            180.0f));
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public void extractRenderState(ThiefEntity entity, HumanoidRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        lastRevealState = entity.getRevealState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return lastRevealState == RevealState.DISGUISED || lastRevealState == RevealState.SUSPICIOUS
            ? DISGUISED_TEXTURE
            : REVEALED_TEXTURE;
    }
}
