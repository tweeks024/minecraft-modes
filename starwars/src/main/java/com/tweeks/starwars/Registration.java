package com.tweeks.starwars;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
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

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STARWARS_TAB =
        CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + StarWarsMod.MOD_ID))
                .icon(() -> BLASTER_PISTOL.get().getDefaultInstance())
                .displayItems((params, output) -> {
                    output.accept(BLASTER_PISTOL.get());
                    output.accept(BLASTER_RIFLE.get());
                    // Later tasks append their items here.
                })
                .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
