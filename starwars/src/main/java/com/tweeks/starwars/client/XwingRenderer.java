package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.XwingModel;
import com.tweeks.starwars.entity.XwingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** Renders the {@link XwingEntity} (pitches with flight). */
public class XwingRenderer extends VehicleRenderer<XwingEntity, XwingModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/xwing.png");
    // Lowest authored point ~ jy+jh=20 (engines/fuselage) -> y_m=2.75.
    private static final float MODEL_Y_TRANSLATE = -(44.0F / 16.0F + 0.1F); // -2.85

    public XwingRenderer(EntityRendererProvider.Context context) {
        super(context, new XwingModel(context.bakeLayer(XwingModel.LAYER_LOCATION)),
            TEXTURE, MODEL_Y_TRANSLATE, true, 1.2F);
    }
}
