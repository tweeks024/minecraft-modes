package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.SpeederBikeModel;
import com.tweeks.starwars.entity.SpeederBikeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** Renders the {@link SpeederBikeEntity} (hover frame — no pitch). */
public class SpeederBikeRenderer extends VehicleRenderer<SpeederBikeEntity, SpeederBikeModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/speeder_bike.png");
    // Lowest authored point is the chassis/rail underside at jy+jh=20 ->
    // y_m=(24+20)/16=2.75; sit it ~0.1 above the entity origin.
    private static final float MODEL_Y_TRANSLATE = -(44.0F / 16.0F + 0.1F); // -2.85

    public SpeederBikeRenderer(EntityRendererProvider.Context context) {
        super(context, new SpeederBikeModel(context.bakeLayer(SpeederBikeModel.LAYER_LOCATION)),
            TEXTURE, MODEL_Y_TRANSLATE, false, 0.8F);
    }
}
