package com.tweeks.craftee;

import com.tweeks.craftee.item.CrafteeArmorMaterials;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(CrafteeMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CrafteeMod.MOD_ID);

    public static final DeferredItem<Item> CRAFTEE_HELMET = ITEMS.registerItem("craftee_helmet",
        Item::new,
        p -> p.humanoidArmor(CrafteeArmorMaterials.CRAFTEE, ArmorType.HELMET)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredItem<Item> CRAFTEE_CHESTPLATE = ITEMS.registerItem("craftee_chestplate",
        Item::new,
        p -> p.humanoidArmor(CrafteeArmorMaterials.CRAFTEE, ArmorType.CHESTPLATE)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredItem<Item> CRAFTEE_LEGGINGS = ITEMS.registerItem("craftee_leggings",
        Item::new,
        p -> p.humanoidArmor(CrafteeArmorMaterials.CRAFTEE, ArmorType.LEGGINGS)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredItem<Item> CRAFTEE_BOOTS = ITEMS.registerItem("craftee_boots",
        Item::new,
        p -> p.humanoidArmor(CrafteeArmorMaterials.CRAFTEE, ArmorType.BOOTS)
              .fireResistant()
              .stacksTo(1));

    public static final DeferredItem<SmithingTemplateItem> CRAFTEE_UPGRADE_SMITHING_TEMPLATE =
        ITEMS.registerItem("craftee_upgrade_smithing_template",
            p -> new SmithingTemplateItem(
                Component.translatable("item.craftee.smithing_template.craftee_upgrade.applies_to"),
                Component.translatable("item.craftee.smithing_template.craftee_upgrade.ingredients"),
                Component.translatable("item.craftee.smithing_template.craftee_upgrade.base_slot_description"),
                Component.translatable("item.craftee.smithing_template.craftee_upgrade.additions_slot_description"),
                List.of(),
                List.of(),
                p),
            p -> p);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CRAFTEE_TAB =
        CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + CrafteeMod.MOD_ID))
                .icon(() -> CRAFTEE_HELMET.get().getDefaultInstance())
                .displayItems((params, output) -> {
                    output.accept(CRAFTEE_HELMET.get());
                    output.accept(CRAFTEE_CHESTPLATE.get());
                    output.accept(CRAFTEE_LEGGINGS.get());
                    output.accept(CRAFTEE_BOOTS.get());
                    output.accept(CRAFTEE_UPGRADE_SMITHING_TEMPLATE.get());
                })
                .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
