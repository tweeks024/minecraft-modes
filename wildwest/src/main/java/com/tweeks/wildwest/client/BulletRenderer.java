package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.entity.BulletEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for the bullet projectile. The visible per-tick effect is the CRIT
 * particle emitted in {@link BulletEntity#tick()}; this renderer is a thin
 * stub over {@link ArrowRenderer}, which already handles the interpolated
 * yRot/xRot pose-stack rotation that points the slug along trajectory.
 *
 * <p>Reserved for later 3D model work. {@code v1} ships the texture only.
 */
public class BulletRenderer extends ArrowRenderer<BulletEntity, ArrowRenderState> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/bullet.png");

    public BulletRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    protected Identifier getTextureLocation(ArrowRenderState state) {
        return TEXTURE;
    }

    @Override
    public ArrowRenderState createRenderState() {
        return new ArrowRenderState();
    }
}
