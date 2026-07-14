package com.tweeks.starwars.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class YodaHutStructure extends Structure {

    public static final MapCodec<YodaHutStructure> CODEC = simpleCodec(YodaHutStructure::new);

    public YodaHutStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG,
            builder -> builder.addPiece(new YodaHutPiece(
                context.random(),
                context.chunkPos().getMinBlockX(),
                context.chunkPos().getMinBlockZ())));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.YODA_HUT_TYPE.get();
    }
}
