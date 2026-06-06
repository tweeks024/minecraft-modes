package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.entity.SkeletonPirateEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Skeleton Pirate renderer.
 *
 * <p>Spec authorizes a fallback to {@link HumanoidMobRenderer} when vanilla
 * {@link net.minecraft.client.renderer.entity.SkeletonRenderer} generics are
 * incompatible. Here {@code SkeletonPirateEntity} extends
 * {@link com.tweeks.wildwest.entity.WildWestMob}, not the vanilla {@code
 * Skeleton} class — so {@code SkeletonRenderer<SkeletonPirateEntity>} would
 * not compile. We therefore render the bony pirate as a humanoid silhouette
 * with the 64×32 skeleton-textured skin, mirroring the canonical
 * {@code HerobrineRenderer} shape used elsewhere in this mod.
 */
public class SkeletonPirateRenderer
        extends HumanoidMobRenderer<SkeletonPirateEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/skeleton_pirate.png");

    public SkeletonPirateRenderer(EntityRendererProvider.Context context) {
        // SKELETON layer (64x32 UV layout) matches the classic-format texture.
        // PLAYER layer (64x64) would read UVs outside the texture bounds.
        super(context,
            new HumanoidModel<>(context.bakeLayer(ModelLayers.SKELETON)),
            0.5F);
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
