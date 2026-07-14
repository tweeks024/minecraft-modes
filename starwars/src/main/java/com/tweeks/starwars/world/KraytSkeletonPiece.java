package com.tweeks.starwars.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A single krayt dragon skeleton, generated as a scattered feature. A pure
 * landmark: bone blocks only — no chest, no mobs.
 *
 * <p>Extends {@link ScatteredFeaturePiece} so it inherits vanilla's proven
 * placement machinery: a random horizontal orientation, local-to-world
 * coordinate mapping through the oriented bounding box, terrain-height snapping
 * via {@code updateAverageGroundHeight}, and persistence of the derived ground
 * height plus dimensions across save/reload — the same pattern as
 * {@link EscapePodPiece}.
 */
public class KraytSkeletonPiece extends ScatteredFeaturePiece {

    /**
     * @param minBlockX world X of the chunk's min corner (the skeleton's local x=0)
     * @param minBlockZ world Z of the chunk's min corner (the skeleton's local z=0)
     */
    public KraytSkeletonPiece(RandomSource random, int minBlockX, int minBlockZ) {
        super(ModStructures.KRAYT_SKELETON_PIECE.get(), minBlockX, 64, minBlockZ,
            KraytSkeletonLayout.SIZE_X, KraytSkeletonLayout.SIZE_Y, KraytSkeletonLayout.SIZE_Z,
            getRandomHorizontalDirection(random));
    }

    public KraytSkeletonPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.KRAYT_SKELETON_PIECE.get(), tag);
    }

    // addAdditionalSaveData is inherited from ScatteredFeaturePiece: it persists
    // Width/Height/Depth and the derived HPos so reloads reproduce the same box.

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // Snap the box to the average ground height under it before placing any
        // blocks; bails out (leaving the skeleton ungenerated in this chunk
        // pass) if no columns of the box fall inside the current chunk.
        if (!this.updateAverageGroundHeight(level, box, 0)) {
            return;
        }
        for (var p : KraytSkeletonLayout.placements()) {
            // Bone-block striations follow the anatomy: vertebrae (the spine
            // line) run lengthwise along z, everything else (skull, ribs)
            // stands vertical. placeBlock rotates the axis with the piece's
            // orientation.
            boolean spine = p.dx() == KraytSkeletonLayout.SPINE_X && p.dy() <= 3;
            var state = Blocks.BONE_BLOCK.defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, spine ? Direction.Axis.Z : Direction.Axis.Y);
            this.placeBlock(level, state, p.dx(), p.dy(), p.dz(), box);
        }
    }
}
