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

    /** Kyber crystal color index into SaberColor.values(); defaults to 0 (BLUE). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> KYBER_COLOR =
        COMPONENTS.registerComponentType(
            "kyber_color",
            builder -> builder
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Active ForcePower index 0..4. Defaults to 0 (PUSH). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ACTIVE_POWER =
        COMPONENTS.registerComponentType(
            "active_power",
            builder -> builder
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Per-power cooldown expiry gameTime ticks, List<Long> length 5. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<Long>>> POWER_COOLDOWNS =
        COMPONENTS.registerComponentType(
            "power_cooldowns",
            builder -> builder
                .persistent(Codec.LONG.listOf())
                .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG.apply(
                    net.minecraft.network.codec.ByteBufCodecs.list(ForceCooldowns.SLOT_COUNT))));

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
