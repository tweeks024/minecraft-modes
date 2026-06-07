package com.tweeks.wildwest.item;

import com.mojang.serialization.Codec;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, WildWestMod.MOD_ID);

    /** Active stone index 0..5. Defaults to 0 (POWER) via {@code InfinityStone.byIndex}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ACTIVE_STONE =
        COMPONENTS.registerComponentType(
            "active_stone",
            builder -> builder
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.VAR_INT));

    /**
     * Per-stone custom commands as a {@code List<String>} of length 6.
     * An empty string (or absent slot) means "use the stone's built-in
     * ability". Edited via the {@code GauntletEditorScreen}; executed
     * by {@link InfinityGauntletItem#use} as the player.
     *
     * <p>Network codec caps per-string length and list size to bound
     * payload size — a misbehaving server (or corrupted persistent NBT)
     * can't deliver multi-MB component syncs to connected clients.
     */
    public static final int MAX_COMMAND_LENGTH = 256;
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<String>>> COMMANDS =
        COMPONENTS.registerComponentType(
            "commands",
            builder -> builder
                .persistent(Codec.STRING.listOf())
                .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.stringUtf8(MAX_COMMAND_LENGTH)
                    .apply(net.minecraft.network.codec.ByteBufCodecs.list(InfinityCommands.SLOT_COUNT))));

    /**
     * Per-stone cooldown timestamps as a {@code List<Long>} of length 6,
     * where each entry is the {@code Level#gameTime()} at which that
     * stone becomes available again. Absent component is treated as
     * "all zero" — no cooldown active.
     *
     * <p>Stored as {@code List<Long>} (not {@code long[]}) because
     * NeoForge's {@code CommonHooks.validateComponent} rejects raw
     * arrays — Java arrays use identity equality, but data components
     * require value equality for sync change-detection.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<Long>>> COOLDOWNS =
        COMPONENTS.registerComponentType(
            "cooldowns",
            builder -> builder
                .persistent(Codec.LONG.listOf())
                .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG.apply(
                    net.minecraft.network.codec.ByteBufCodecs.list(InfinityCooldowns.SLOT_COUNT))));

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
