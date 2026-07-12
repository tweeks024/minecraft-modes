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

public final class HanSoloArmorMaterials {
    private HanSoloArmorMaterials() {}

    /** Equipment-asset id used by the {@link #HAN_SOLO} material. Resolves
     *  to {@code assets/starwars/equipment/han_solo.json} (defining the
     *  worn-armor texture layers) at runtime. */
    public static final ResourceKey<EquipmentAsset> HAN_SOLO_ASSET =
        ResourceKey.create(EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "han_solo"));

    /** Netherite-tier defense — every value verified against decompiled
     *  {@code ArmorMaterials.NETHERITE} (net.minecraft.world.item.equipment.ArmorMaterials):
     *  boots 3, legs 6, chest 8, helmet 3, body 19. */
    private static final Map<ArmorType, Integer> DEFENSE = Map.of(
        ArmorType.BOOTS,      3,
        ArmorType.LEGGINGS,   6,
        ArmorType.CHESTPLATE, 8,
        ArmorType.HELMET,     3,
        ArmorType.BODY,       19);

    public static final ArmorMaterial HAN_SOLO = new ArmorMaterial(
        37,
        DEFENSE,
        15,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        3.0F,
        0.1F,
        ItemTags.REPAIRS_NETHERITE_ARMOR,
        HAN_SOLO_ASSET);
}
