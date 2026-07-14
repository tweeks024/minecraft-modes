package com.tweeks.starwars.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class FerrixTownStructure extends Structure {

    public static final MapCodec<FerrixTownStructure> CODEC = simpleCodec(FerrixTownStructure::new);

    public FerrixTownStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG,
            builder -> builder.addPiece(new FerrixTownPiece(
                context.random(),
                context.chunkPos().getMinBlockX(),
                context.chunkPos().getMinBlockZ())));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.FERRIX_TOWN_TYPE.get();
    }
}
