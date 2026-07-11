package com.tweeks.starwars;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(StarWarsMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StarWarsMod.MOD_ID);

    public static final DeferredItem<com.tweeks.starwars.item.BlasterPistolItem> BLASTER_PISTOL =
        ITEMS.registerItem("blaster_pistol", com.tweeks.starwars.item.BlasterPistolItem::new, p -> p);

    public static final DeferredItem<com.tweeks.starwars.item.BlasterRifleItem> BLASTER_RIFLE =
        ITEMS.registerItem("blaster_rifle", com.tweeks.starwars.item.BlasterRifleItem::new, p -> p);

    public static final DeferredItem<com.tweeks.starwars.item.LightsaberItem> LIGHTSABER =
        ITEMS.registerItem("lightsaber", com.tweeks.starwars.item.LightsaberItem::new, p -> p);

    public static final DeferredItem<com.tweeks.starwars.item.HolocronItem> HOLOCRON =
        ITEMS.registerItem("holocron", com.tweeks.starwars.item.HolocronItem::new, p -> p);

    public static final DeferredItem<SpawnEggItem> STORMTROOPER_SPAWN_EGG = ITEMS.registerItem(
        "stormtrooper_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.STORMTROOPER.get()));

    public static final DeferredItem<SpawnEggItem> BATTLE_DROID_SPAWN_EGG = ITEMS.registerItem(
        "battle_droid_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.BATTLE_DROID.get()));

    public static final DeferredItem<SpawnEggItem> JEDI_KNIGHT_SPAWN_EGG = ITEMS.registerItem(
        "jedi_knight_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.JEDI_KNIGHT.get()));

    public static final DeferredItem<SpawnEggItem> DARTH_VADER_SPAWN_EGG = ITEMS.registerItem(
        "darth_vader_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.DARTH_VADER.get()));

    public static final DeferredItem<SpawnEggItem> LUKE_SKYWALKER_SPAWN_EGG = ITEMS.registerItem(
        "luke_skywalker_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.LUKE_SKYWALKER.get()));

    public static final DeferredItem<SpawnEggItem> OBI_WAN_SPAWN_EGG = ITEMS.registerItem(
        "obi_wan_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.OBI_WAN.get()));

    public static final DeferredItem<Item> STORMTROOPER_HELMET = ITEMS.registerItem("stormtrooper_helmet",
        Item::new,
        p -> p.humanoidArmor(com.tweeks.starwars.item.StormtrooperArmorMaterials.STORMTROOPER,
                net.minecraft.world.item.equipment.ArmorType.HELMET)
              .stacksTo(1));

    public static final DeferredItem<Item> STORMTROOPER_CHESTPLATE = ITEMS.registerItem("stormtrooper_chestplate",
        Item::new,
        p -> p.humanoidArmor(com.tweeks.starwars.item.StormtrooperArmorMaterials.STORMTROOPER,
                net.minecraft.world.item.equipment.ArmorType.CHESTPLATE)
              .stacksTo(1));

    public static final DeferredItem<Item> STORMTROOPER_LEGGINGS = ITEMS.registerItem("stormtrooper_leggings",
        Item::new,
        p -> p.humanoidArmor(com.tweeks.starwars.item.StormtrooperArmorMaterials.STORMTROOPER,
                net.minecraft.world.item.equipment.ArmorType.LEGGINGS)
              .stacksTo(1));

    public static final DeferredItem<Item> STORMTROOPER_BOOTS = ITEMS.registerItem("stormtrooper_boots",
        Item::new,
        p -> p.humanoidArmor(com.tweeks.starwars.item.StormtrooperArmorMaterials.STORMTROOPER,
                net.minecraft.world.item.equipment.ArmorType.BOOTS)
              .stacksTo(1));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STARWARS_TAB =
        CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + StarWarsMod.MOD_ID))
                .icon(() -> BLASTER_PISTOL.get().getDefaultInstance())
                .displayItems((params, output) -> {
                    output.accept(BLASTER_PISTOL.get());
                    output.accept(BLASTER_RIFLE.get());
                    for (com.tweeks.starwars.item.SaberColor color : com.tweeks.starwars.item.SaberColor.values()) {
                        output.accept(com.tweeks.starwars.item.LightsaberItem.stackWithColor(color));
                    }
                    output.accept(STORMTROOPER_SPAWN_EGG.get());
                    output.accept(BATTLE_DROID_SPAWN_EGG.get());
                    output.accept(JEDI_KNIGHT_SPAWN_EGG.get());
                    output.accept(DARTH_VADER_SPAWN_EGG.get());
                    output.accept(LUKE_SKYWALKER_SPAWN_EGG.get());
                    output.accept(OBI_WAN_SPAWN_EGG.get());
                    output.accept(STORMTROOPER_HELMET.get());
                    output.accept(STORMTROOPER_CHESTPLATE.get());
                    output.accept(STORMTROOPER_LEGGINGS.get());
                    output.accept(STORMTROOPER_BOOTS.get());
                    output.accept(HOLOCRON.get());
                    // Later tasks append their items here.
                })
                .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
