package com.tweeks.creeperskin;

import com.tweeks.creeperskin.item.CreeperArmorMaterials;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(CreeperSkinMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreeperSkinMod.MOD_ID);

    public static final DeferredItem<Item> CREEPER_HELMET = ITEMS.registerItem("creeper_helmet",
        Item::new,
        p -> p.humanoidArmor(CreeperArmorMaterials.CREEPER, ArmorType.HELMET)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredItem<Item> CREEPER_CHESTPLATE = ITEMS.registerItem("creeper_chestplate",
        Item::new,
        p -> p.humanoidArmor(CreeperArmorMaterials.CREEPER, ArmorType.CHESTPLATE)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredItem<Item> CREEPER_LEGGINGS = ITEMS.registerItem("creeper_leggings",
        Item::new,
        p -> p.humanoidArmor(CreeperArmorMaterials.CREEPER, ArmorType.LEGGINGS)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredItem<Item> CREEPER_BOOTS = ITEMS.registerItem("creeper_boots",
        Item::new,
        p -> p.humanoidArmor(CreeperArmorMaterials.CREEPER, ArmorType.BOOTS)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREEPER_TAB =
        CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + CreeperSkinMod.MOD_ID))
                .icon(() -> CREEPER_HELMET.get().getDefaultInstance())
                .displayItems((params, output) -> {
                    output.accept(CREEPER_HELMET.get());
                    output.accept(CREEPER_CHESTPLATE.get());
                    output.accept(CREEPER_LEGGINGS.get());
                    output.accept(CREEPER_BOOTS.get());
                })
                .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
