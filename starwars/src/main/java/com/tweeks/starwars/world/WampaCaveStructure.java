package com.tweeks.starwars.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class WampaCaveStructure extends Structure {

    public static final MapCodec<WampaCaveStructure> CODEC = simpleCodec(WampaCaveStructure::new);

    public WampaCaveStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG,
            builder -> builder.addPiece(new WampaCavePiece(
                context.random(),
                context.chunkPos().getMinBlockX(),
                context.chunkPos().getMinBlockZ())));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.WAMPA_CAVE_TYPE.get();
    }
}
