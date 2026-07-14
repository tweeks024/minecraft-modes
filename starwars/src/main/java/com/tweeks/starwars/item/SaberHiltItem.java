package com.tweeks.starwars.item;

import net.minecraft.world.item.Item;

/**
 * An inert lightsaber hilt — the metal half of the weapon. Combine it with a
 * kyber crystal in a crafting grid ({@link KyberSaberRecipe}) to finish the
 * blade. Marker type only; all behaviour lives in the recipe.
 */
public class SaberHiltItem extends Item {
    public SaberHiltItem(Properties properties) {
        super(properties);
    }
}
