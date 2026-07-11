package com.tweeks.starwars.world;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

public class EscapePodPiece extends StructurePiece {

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "chests/escape_pod"));

    public EscapePodPiece(BlockPos origin) {
        super(ModStructures.ESCAPE_POD_PIECE.get(), 0,
            new BoundingBox(origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + EscapePodLayout.SIZE_X - 1,
                origin.getY() + EscapePodLayout.SIZE_Y - 1,
                origin.getZ() + EscapePodLayout.SIZE_Z - 1));
        this.setOrientation(null);
    }

    public EscapePodPiece(StructurePieceSerializationContext ctx, net.minecraft.nbt.CompoundTag tag) {
        super(ModStructures.ESCAPE_POD_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx,
                                         net.minecraft.nbt.CompoundTag tag) {
        // Bounding box handled by the base class; no extra state.
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        for (var p : EscapePodLayout.placements()) {
            var state = switch (p.kind()) {
                case SHELL -> Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                case FLOOR -> Blocks.GRAY_CONCRETE.defaultBlockState();
                case AIR -> Blocks.AIR.defaultBlockState();
                case CHEST -> null;   // handled below via createChest
            };
            if (state != null) {
                this.placeBlock(level, state, p.dx(), p.dy(), p.dz(), box);
            } else {
                this.createChest(level, box, random, p.dx(), p.dy(), p.dz(), LOOT);
            }
        }
    }
}
