package com.tweeks.starwars.world;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    private ModStructures() {}

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
        DeferredRegister.create(Registries.STRUCTURE_TYPE, StarWarsMod.MOD_ID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
        DeferredRegister.create(Registries.STRUCTURE_PIECE, StarWarsMod.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<EscapePodStructure>> ESCAPE_POD_TYPE =
        STRUCTURE_TYPES.register("escape_pod", () -> () -> EscapePodStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> ESCAPE_POD_PIECE =
        STRUCTURE_PIECES.register("escape_pod",
            () -> (StructurePieceType) EscapePodPiece::new);

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
    }
}
