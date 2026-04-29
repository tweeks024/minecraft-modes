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
        extends HumanoidMobRenderer<ThiefEntity, ThiefRenderer.State, ThiefModel<ThiefRenderer.State>> {

    private static final Identifier DISGUISED_TEXTURE = Identifier.fromNamespaceAndPath(
        ThiefMod.MOD_ID, "textures/entity/thief_disguised.png");
    private static final Identifier REVEALED_TEXTURE = Identifier.fromNamespaceAndPath(
        ThiefMod.MOD_ID, "textures/entity/thief_revealed.png");
    private static final Identifier BLACKJACK_TEXTURE = Identifier.fromNamespaceAndPath(
        ThiefMod.MOD_ID, "textures/entity/blackjack.png");

    public ThiefRenderer(EntityRendererProvider.Context context) {
        super(context, new ThiefModel<>(context.bakeLayer(ThiefModel.LAYER_LOCATION)), 0.5f);
        BlackjackModel<State> blackjackModel = new BlackjackModel<>(context.bakeLayer(BlackjackModel.LAYER_LOCATION));
        this.addLayer(new HeldItemLayer<State, ThiefModel<State>>(this,
            blackjackModel,
            BLACKJACK_TEXTURE,
            -0.0625f, 0.625f, 0.0f,
            180.0f));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ThiefEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.revealState = entity.getRevealState();
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return state.revealState == RevealState.DISGUISED || state.revealState == RevealState.SUSPICIOUS
            ? DISGUISED_TEXTURE
            : REVEALED_TEXTURE;
    }

    public static class State extends HumanoidRenderState {
        public RevealState revealState = RevealState.DISGUISED;
    }
}
