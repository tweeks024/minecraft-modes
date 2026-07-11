package com.tweeks.starwars.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class ImperialOutpostStructure extends Structure {

    public static final MapCodec<ImperialOutpostStructure> CODEC = simpleCodec(ImperialOutpostStructure::new);

    public ImperialOutpostStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG,
            builder -> builder.addPiece(new ImperialOutpostPiece(
                context.random(),
                context.chunkPos().getMinBlockX(),
                context.chunkPos().getMinBlockZ())));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.IMPERIAL_OUTPOST_TYPE.get();
    }
}
