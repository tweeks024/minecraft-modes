package com.tweeks.starwars.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class MoistureFarmStructure extends Structure {

    public static final MapCodec<MoistureFarmStructure> CODEC = simpleCodec(MoistureFarmStructure::new);

    public MoistureFarmStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG,
            builder -> builder.addPiece(new MoistureFarmPiece(
                context.random(),
                context.chunkPos().getMinBlockX(),
                context.chunkPos().getMinBlockZ())));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.MOISTURE_FARM_TYPE.get();
    }
}
