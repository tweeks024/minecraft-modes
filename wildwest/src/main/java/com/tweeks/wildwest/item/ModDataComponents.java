package com.tweeks.wildwest.item;

import com.mojang.serialization.Codec;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<String>>> COMMANDS =
        COMPONENTS.registerComponentType(
            "commands",
            builder -> builder
                .persistent(Codec.STRING.listOf())
                .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.apply(
                    net.minecraft.network.codec.ByteBufCodecs.list())));

    /**
     * Per-stone cooldown timestamps as a {@code long[]} of length 6, where
     * each entry is the {@code Level#gameTime()} at which that stone becomes
     * available again. Absent component is treated as "all zero" — no
     * cooldown active.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<long[]>> COOLDOWNS =
        COMPONENTS.registerComponentType(
            "cooldowns",
            builder -> builder
                .persistent(Codec.LONG.listOf().xmap(
                    list -> list.stream().mapToLong(Long::longValue).toArray(),
                    arr -> java.util.Arrays.stream(arr).boxed().toList()))
                .networkSynchronized(StreamCodec.of(
                    (buf, arr) -> {
                        buf.writeVarInt(arr.length);
                        for (long v : arr) buf.writeVarLong(v);
                    },
                    buf -> {
                        int n = buf.readVarInt();
                        long[] out = new long[n];
                        for (int i = 0; i < n; i++) out[i] = buf.readVarLong();
                        return out;
                    })));

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
