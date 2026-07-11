package com.tweeks.starwars.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class EscapePodStructure extends Structure {

    public static final MapCodec<EscapePodStructure> CODEC = simpleCodec(EscapePodStructure::new);

    public EscapePodStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG,
            builder -> builder.addPiece(new EscapePodPiece(
                context.chunkPos().getMiddleBlockPosition(0))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ESCAPE_POD_TYPE.get();
    }
}
