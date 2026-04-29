package com.tweeks.securityguard;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(SecurityGuardMod.MOD_ID);

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, SecurityGuardMod.MOD_ID);

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(Registries.SOUND_EVENT, SecurityGuardMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SecurityGuardMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SECURITY_GUARD_TAB =
        CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + SecurityGuardMod.MOD_ID))
                .icon(() -> Items.IRON_HELMET.getDefaultInstance())  // Guard Helmet replaces in Task 4
                .displayItems((params, output) -> {
                    // populated by individual register calls in later tasks
                })
                .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
