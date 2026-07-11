package com.tweeks.starwars.item;

import com.tweeks.starwars.StarWarsMod;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class StormtrooperArmorMaterials {
    private StormtrooperArmorMaterials() {}

    /** Equipment-asset id used by the {@link #STORMTROOPER} material. Resolves
     *  to {@code assets/starwars/equipment/stormtrooper.json} (defining the
     *  worn-armor texture layers) at runtime. */
    public static final ResourceKey<EquipmentAsset> STORMTROOPER_ASSET =
        ResourceKey.create(EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "stormtrooper"));

    /** Iron-tier defense: boots 2, legs 5, chest 6, helmet 2, body 5. */
    private static final Map<ArmorType, Integer> DEFENSE = Map.of(
        ArmorType.BOOTS,      2,
        ArmorType.LEGGINGS,   5,
        ArmorType.CHESTPLATE, 6,
        ArmorType.HELMET,     2,
        ArmorType.BODY,       5);

    public static final ArmorMaterial STORMTROOPER = new ArmorMaterial(
        15,
        DEFENSE,
        9,
        SoundEvents.ARMOR_EQUIP_IRON,
        0.0F,
        0.0F,
        ItemTags.REPAIRS_IRON_ARMOR,
        STORMTROOPER_ASSET);
}
