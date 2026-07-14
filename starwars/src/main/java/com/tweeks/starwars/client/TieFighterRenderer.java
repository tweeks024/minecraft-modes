package com.tweeks.starwars.client;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.TieFighterModel;
import com.tweeks.starwars.entity.TieFighterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** Renders the {@link TieFighterEntity} (pitches with flight). */
public class TieFighterRenderer extends VehicleRenderer<TieFighterEntity, TieFighterModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/tie_fighter.png");
    // Lowest authored point is the solar-panel underside at jy+jh=22 ->
    // y_m=(24+22)/16=2.875.
    private static final float MODEL_Y_TRANSLATE = -(46.0F / 16.0F + 0.1F); // -2.975

    public TieFighterRenderer(EntityRendererProvider.Context context) {
        super(context, new TieFighterModel(context.bakeLayer(TieFighterModel.LAYER_LOCATION)),
            TEXTURE, MODEL_Y_TRANSLATE, true, 1.0F);
    }
}
