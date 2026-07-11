package com.tweeks.starwars.world;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * A single crashed escape pod, generated as a scattered feature.
 *
 * <p>Extends {@link ScatteredFeaturePiece} so it inherits vanilla's proven
 * placement machinery: a random horizontal orientation (the torn front may face
 * any direction), local-to-world coordinate mapping through the oriented
 * bounding box, terrain-height snapping via {@code updateAverageGroundHeight},
 * and persistence of the derived ground height (HPos) plus dimensions across
 * save/reload.
 */
public class EscapePodPiece extends ScatteredFeaturePiece {

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "chests/escape_pod"));

    /**
     * @param minBlockX world X of the chunk's min corner (the pod's local x=0)
     * @param minBlockZ world Z of the chunk's min corner (the pod's local z=0)
     */
    public EscapePodPiece(RandomSource random, int minBlockX, int minBlockZ) {
        super(ModStructures.ESCAPE_POD_PIECE.get(), minBlockX, 64, minBlockZ,
            EscapePodLayout.SIZE_X, EscapePodLayout.SIZE_Y, EscapePodLayout.SIZE_Z,
            getRandomHorizontalDirection(random));
    }

    public EscapePodPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.ESCAPE_POD_PIECE.get(), tag);
    }

    // addAdditionalSaveData is inherited from ScatteredFeaturePiece: it persists
    // Width/Height/Depth and the derived HPos so reloads reproduce the same box.

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // Snap the box to the average ground height under it before placing any
        // blocks; bails out (leaving the pod ungenerated in this chunk pass) if
        // no columns of the box fall inside the current chunk.
        if (!this.updateAverageGroundHeight(level, box, 0)) {
            return;
        }
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
