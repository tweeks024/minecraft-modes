package com.tweeks.securityguard.client.renderer;

import com.tweeks.securityguard.SecurityGuardMod;
import com.tweeks.securityguard.client.model.SecurityGuardModel;
import com.tweeks.securityguard.entity.SecurityGuardEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class SecurityGuardRenderer
        extends HumanoidMobRenderer<SecurityGuardEntity, HumanoidRenderState, SecurityGuardModel> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        SecurityGuardMod.MOD_ID, "textures/entity/security_guard.png");

    public SecurityGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new SecurityGuardModel(context.bakeLayer(SecurityGuardModel.LAYER_LOCATION)), 0.5f);
        this.addLayer(new BatonHeldLayer(this, context));
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
