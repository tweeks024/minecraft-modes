package com.tweeks.starwars;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
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

    public static final DeferredItem<SpawnEggItem> STORMTROOPER_SPAWN_EGG = ITEMS.registerItem(
        "stormtrooper_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.STORMTROOPER.get()));

    public static final DeferredItem<SpawnEggItem> BATTLE_DROID_SPAWN_EGG = ITEMS.registerItem(
        "battle_droid_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.BATTLE_DROID.get()));

    public static final DeferredItem<SpawnEggItem> JEDI_KNIGHT_SPAWN_EGG = ITEMS.registerItem(
        "jedi_knight_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.JEDI_KNIGHT.get()));

    public static final DeferredItem<SpawnEggItem> DARTH_VADER_SPAWN_EGG = ITEMS.registerItem(
        "darth_vader_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.DARTH_VADER.get()));

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
                    // Later tasks append their items here.
                })
                .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
