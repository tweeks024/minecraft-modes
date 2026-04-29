package com.tweeks.thief;

import com.tweeks.thief.item.BlackjackItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(ThiefMod.MOD_ID);

    public static final DeferredItem<BlackjackItem> BLACKJACK = ITEMS.registerItem("blackjack",
        BlackjackItem::new,
        p -> p);

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
