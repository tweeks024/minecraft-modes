package com.tweeks.starwars.world.planet;

import com.mojang.serialization.MapCodec;
import com.tweeks.starwars.StarWarsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Chunk generator codecs — needed so planet level stems can serialize. */
public final class ModChunkGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
        DeferredRegister.create(Registries.CHUNK_GENERATOR, StarWarsMod.MOD_ID);

    public static final java.util.function.Supplier<MapCodec<CoruscantChunkGenerator>> CORUSCANT =
        CHUNK_GENERATORS.register("coruscant", () -> CoruscantChunkGenerator.CODEC);

    private ModChunkGenerators() {
    }

    public static void register(IEventBus modEventBus) {
        CHUNK_GENERATORS.register(modEventBus);
    }
}
