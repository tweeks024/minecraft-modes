package com.tweeks.starwars.world;

import com.tweeks.starwars.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A wampa's ice-cave lair, generated as a scattered feature. No chest — the
 * only prize is surviving the resident.
 *
 * <p>Extends {@link ScatteredFeaturePiece} so it inherits vanilla's proven
 * placement machinery: a random horizontal orientation, local-to-world
 * coordinate mapping through the oriented bounding box, terrain-height snapping
 * via {@code updateAverageGroundHeight}, and persistence of the derived ground
 * height plus dimensions across save/reload — the same pattern as
 * {@link EscapePodPiece}.
 */
public class WampaCavePiece extends ScatteredFeaturePiece {

    /**
     * @param minBlockX world X of the chunk's min corner (the cave's local x=0)
     * @param minBlockZ world Z of the chunk's min corner (the cave's local z=0)
     */
    public WampaCavePiece(RandomSource random, int minBlockX, int minBlockZ) {
        super(ModStructures.WAMPA_CAVE_PIECE.get(), minBlockX, 64, minBlockZ,
            WampaCaveLayout.SIZE_X, WampaCaveLayout.SIZE_Y, WampaCaveLayout.SIZE_Z,
            getRandomHorizontalDirection(random));
    }

    public WampaCavePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.WAMPA_CAVE_PIECE.get(), tag);
    }

    // addAdditionalSaveData is inherited from ScatteredFeaturePiece: it persists
    // Width/Height/Depth and the derived HPos so reloads reproduce the same box.

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // Snap the box to the average ground height under it before placing any
        // blocks; bails out (leaving the cave ungenerated in this chunk pass)
        // if no columns of the box fall inside the current chunk.
        if (!this.updateAverageGroundHeight(level, box, 0)) {
            return;
        }
        for (var p : WampaCaveLayout.placements()) {
            var state = switch (p.kind()) {
                case SHELL -> Blocks.PACKED_ICE.defaultBlockState();
                case SPIKE -> Blocks.BLUE_ICE.defaultBlockState();
                case BONE -> Blocks.BONE_BLOCK.defaultBlockState();
                case AIR -> Blocks.AIR.defaultBlockState();
                case WAMPA -> Blocks.AIR.defaultBlockState(); // carve air so the beast doesn't suffocate on slopes; entity spawn below
            };
            this.placeBlock(level, state, p.dx(), p.dy(), p.dz(), box);
        }

        // The wampa: spawn the lair's resident at generation time. Coordinates
        // are mapped local->world through the oriented box (getWorldPos), so
        // the marker rotates/mirrors with the piece — same pattern as the
        // ImperialOutpostPiece garrison loop. Skipped if the marker falls
        // outside the current chunk's box.
        for (var p : WampaCaveLayout.placements()) {
            if (p.kind() != WampaCaveLayout.Kind.WAMPA) continue;
            EntityType<? extends Mob> type = ModEntities.WAMPA.get();
            BlockPos worldPos = this.getWorldPos(p.dx(), p.dy(), p.dz());
            if (!box.isInside(worldPos)) continue;
            Mob mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (mob == null) continue;
            mob.setPersistenceRequired();
            mob.snapTo(worldPos.getX() + 0.5, worldPos.getY(), worldPos.getZ() + 0.5,
                random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                EntitySpawnReason.STRUCTURE, null);
            level.addFreshEntityWithPassengers(mob);
        }
    }
}
