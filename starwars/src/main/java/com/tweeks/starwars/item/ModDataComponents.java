package com.tweeks.starwars.item;

import com.mojang.serialization.Codec;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, StarWarsMod.MOD_ID);

    /** Blade color index into SaberColor.values(); defaults to 0 (BLUE). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BLADE_COLOR =
        COMPONENTS.registerComponentType(
            "blade_color",
            builder -> builder
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.VAR_INT));

    // ACTIVE_POWER + POWER_COOLDOWNS join in Milestone 5.

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
