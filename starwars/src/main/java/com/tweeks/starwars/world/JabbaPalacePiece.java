package com.tweeks.starwars.world;

import com.tweeks.starwars.ModEntities;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Jabba's Palace on Tatooine, generated as a scattered feature: a domed
 * sandstone fortress whose throne room looks down through an iron grate onto a
 * stone rancor pit. The piece places the {@link JabbaPalaceLayout} blocks, then
 * spawns the two residents — the rancor in the pit and Jabba on his dais — and
 * fills the treasure chest beneath the dais.
 *
 * <p>Extends {@link ScatteredFeaturePiece} for the same reasons as
 * {@link VaderCastlePiece}: a random horizontal orientation, local-to-world
 * mapping through the oriented bounding box, terrain-height snapping via
 * {@code updateAverageGroundHeight}, and save/reload persistence of the box.
 */
public class JabbaPalacePiece extends ScatteredFeaturePiece {

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "chests/jabba_palace"));

    /**
     * @param minBlockX world X of the chunk's min corner (the palace's local x=0)
     * @param minBlockZ world Z of the chunk's min corner (the palace's local z=0)
     */
    public JabbaPalacePiece(RandomSource random, int minBlockX, int minBlockZ) {
        super(ModStructures.JABBA_PALACE_PIECE.get(), minBlockX, 64, minBlockZ,
            JabbaPalaceLayout.SIZE_X, JabbaPalaceLayout.SIZE_Y, JabbaPalaceLayout.SIZE_Z,
            getRandomHorizontalDirection(random));
    }

    public JabbaPalacePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.JABBA_PALACE_PIECE.get(), tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // Snap the box to the average ground height before placing anything;
        // bail out (leaving the palace ungenerated in this chunk pass) if none
        // of its columns fall inside the current chunk.
        if (!this.updateAverageGroundHeight(level, box, 0)) {
            return;
        }

        for (var p : JabbaPalaceLayout.placements()) {
            var state = switch (p.kind()) {
                case WALL -> Blocks.SANDSTONE.defaultBlockState();
                case SMOOTH -> Blocks.SMOOTH_SANDSTONE.defaultBlockState();
                case CUT -> Blocks.CUT_SANDSTONE.defaultBlockState();
                case PILLAR -> Blocks.CHISELED_SANDSTONE.defaultBlockState();
                case PIT_WALL -> Blocks.STONE_BRICKS.defaultBlockState();
                case GRATE -> Blocks.IRON_BARS.defaultBlockState();
                case CARPET -> Blocks.ORANGE_CARPET.defaultBlockState();
                case DAIS -> Blocks.CUT_RED_SANDSTONE.defaultBlockState();
                case TORCH -> Blocks.TORCH.defaultBlockState();
                // Apex light: a glowstone keystone needs no support and never
                // pops loose during worldgen, unlike a floating lantern.
                case LANTERN -> Blocks.GLOWSTONE.defaultBlockState();
                case BONE -> Blocks.BONE_BLOCK.defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
                // Markers and openings are carved to air; entities spawn below.
                case GATE_AIR, AIR, RANCOR, JABBA -> Blocks.AIR.defaultBlockState();
                case CHEST -> null;                               // handled below
            };
            if (state != null) {
                this.placeBlock(level, state, p.dx(), p.dy(), p.dz(), box);
            } else {
                // Air out the chest cell first so the chest isn't wedged, then
                // fill it from the palace loot table.
                this.placeBlock(level, Blocks.AIR.defaultBlockState(), p.dx(), p.dy(), p.dz(), box);
                this.createChest(level, box, random, p.dx(), p.dy(), p.dz(), LOOT);
            }
        }

        // The residents: rancor in the pit, Jabba on the dais. Coordinates map
        // local->world through the oriented box (getWorldPos), so both rotate
        // and mirror with the piece — same pattern as the VaderCastlePiece
        // garrison. Skipped if a marker falls outside the current chunk's box.
        for (var p : JabbaPalaceLayout.placements()) {
            EntityType<? extends Mob> type = switch (p.kind()) {
                case RANCOR -> ModEntities.RANCOR.get();
                case JABBA -> ModEntities.JABBA.get();
                default -> null;
            };
            if (type == null) continue;
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
