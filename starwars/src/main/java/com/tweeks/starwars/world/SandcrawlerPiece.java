package com.tweeks.starwars.world;

import com.tweeks.starwars.ModEntities;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * A single Jawa sandcrawler, generated as a scattered feature.
 *
 * <p>Extends {@link ScatteredFeaturePiece} so it inherits vanilla's proven
 * placement machinery: a random horizontal orientation, local-to-world
 * coordinate mapping through the oriented bounding box, terrain-height snapping
 * via {@code updateAverageGroundHeight}, and persistence of the derived ground
 * height plus dimensions across save/reload — the same pattern as
 * {@link EscapePodPiece}.
 */
public class SandcrawlerPiece extends ScatteredFeaturePiece {

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "chests/sandcrawler"));

    /**
     * @param minBlockX world X of the chunk's min corner (the crawler's local x=0)
     * @param minBlockZ world Z of the chunk's min corner (the crawler's local z=0)
     */
    public SandcrawlerPiece(RandomSource random, int minBlockX, int minBlockZ) {
        super(ModStructures.SANDCRAWLER_PIECE.get(), minBlockX, 64, minBlockZ,
            SandcrawlerLayout.SIZE_X, SandcrawlerLayout.SIZE_Y, SandcrawlerLayout.SIZE_Z,
            getRandomHorizontalDirection(random));
    }

    public SandcrawlerPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.SANDCRAWLER_PIECE.get(), tag);
    }

    // addAdditionalSaveData is inherited from ScatteredFeaturePiece: it persists
    // Width/Height/Depth and the derived HPos so reloads reproduce the same box.

    /** Weathered rust banding: three terracotta shades cycling by height. */
    private static BlockState hullBand(int dy) {
        return switch (dy % 3) {
            case 0 -> Blocks.BROWN_TERRACOTTA.defaultBlockState();
            case 1 -> Blocks.TERRACOTTA.defaultBlockState();
            default -> Blocks.RED_TERRACOTTA.defaultBlockState();
        };
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // Snap the box to the average ground height under it before placing any
        // blocks; bails out (leaving the crawler ungenerated in this chunk pass)
        // if no columns of the box fall inside the current chunk.
        if (!this.updateAverageGroundHeight(level, box, 0)) {
            return;
        }
        for (var p : SandcrawlerLayout.placements()) {
            var state = switch (p.kind()) {
                case HULL, SLOPE -> hullBand(p.dy());
                case TREAD -> ((p.dx() + p.dz()) % 2 == 0
                    ? Blocks.COAL_BLOCK : Blocks.POLISHED_BASALT).defaultBlockState();
                case DECK -> Blocks.SMOOTH_STONE.defaultBlockState();
                case AIR -> Blocks.AIR.defaultBlockState();
                case CHEST -> null;                          // handled via createChest
                case ASTROMECH -> Blocks.AIR.defaultBlockState(); // carve air so the droid doesn't suffocate on slopes; entity spawn below
            };
            if (state != null) {
                this.placeBlock(level, state, p.dx(), p.dy(), p.dz(), box);
            } else if (p.kind() == SandcrawlerLayout.Kind.CHEST) {
                this.createChest(level, box, random, p.dx(), p.dy(), p.dz(), LOOT);
            }
        }

        // Salvage droids: spawn the astromechs in the hold at generation time.
        // Coordinates are mapped local->world through the oriented box
        // (getWorldPos), so the markers rotate/mirror with the piece — same
        // pattern as EscapePodPiece. Skipped if a marker falls outside the
        // current chunk's box (the piece may straddle chunk borders).
        for (var p : SandcrawlerLayout.placements()) {
            if (p.kind() != SandcrawlerLayout.Kind.ASTROMECH) continue;
            EntityType<? extends Mob> type = ModEntities.ASTROMECH.get();
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
