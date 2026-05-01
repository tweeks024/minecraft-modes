package com.tweeks.craftee.item;

import com.tweeks.craftee.CrafteeMod;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class CrafteeArmorMaterials {
    private CrafteeArmorMaterials() {}

    /** Equipment-asset id used by the {@link #CRAFTEE} material. Resolves to
     *  {@code assets/craftee/equipment/craftee.json} (defining the worn-armor
     *  texture layers) at runtime. */
    public static final ResourceKey<EquipmentAsset> CRAFTEE_ASSET =
        ResourceKey.create(EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(CrafteeMod.MOD_ID, "craftee"));

    /** Netherite-tier defense map: boots 3, legs 6, chest 8, helmet 3, body 19. */
    private static final Map<ArmorType, Integer> DEFENSE = Map.of(
        ArmorType.BOOTS,      3,
        ArmorType.LEGGINGS,   6,
        ArmorType.CHESTPLATE, 8,
        ArmorType.HELMET,     3,
        ArmorType.BODY,       19);

    /** Cosmetic craftee armor material. Stat-equivalent to vanilla netherite
     *  (durability 37 base, toughness 3.0, knockback resist 0.1,
     *  enchantability 15, fire-resistant via {@link Item.Properties#fireResistant}
     *  on the items themselves) — only the equipment asset and the texture it
     *  points at are different. */
    public static final ArmorMaterial CRAFTEE = new ArmorMaterial(
        37,
        DEFENSE,
        15,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        3.0F,
        0.1F,
        ItemTags.REPAIRS_NETHERITE_ARMOR,
        CRAFTEE_ASSET);
}
