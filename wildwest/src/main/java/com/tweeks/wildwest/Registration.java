package com.tweeks.wildwest;

import com.tweeks.wildwest.item.PistolItem;
import com.tweeks.wildwest.item.RifleItem;
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
        DeferredRegister.createItems(WildWestMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WildWestMod.MOD_ID);

    public static final DeferredItem<PistolItem> PISTOL = ITEMS.registerItem(
        "pistol", PistolItem::new, p -> p);

    public static final DeferredItem<RifleItem> RIFLE = ITEMS.registerItem(
        "rifle", RifleItem::new, p -> p);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WILDWEST_TAB =
        CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + WildWestMod.MOD_ID))
                .icon(() -> PISTOL.get().getDefaultInstance())
                .displayItems((params, output) -> {
                    output.accept(PISTOL.get());
                    output.accept(RIFLE.get());
                })
                .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
