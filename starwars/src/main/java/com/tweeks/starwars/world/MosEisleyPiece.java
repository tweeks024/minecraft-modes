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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The Mos Eisley spaceport, generated as a scattered feature.
 *
 * <p>Extends {@link ScatteredFeaturePiece} so it inherits vanilla's proven
 * placement machinery: a random horizontal orientation, local-to-world
 * coordinate mapping through the oriented bounding box, terrain-height snapping
 * via {@code updateAverageGroundHeight}, and persistence of the derived ground
 * height plus dimensions across save/reload — the same pattern as
 * {@link EscapePodPiece}.
 */
public class MosEisleyPiece extends ScatteredFeaturePiece {

    public static final ResourceKey<LootTable> CANTINA_LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "chests/cantina"));

    public static final ResourceKey<LootTable> DOCKING_LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "chests/mos_eisley"));

    /**
     * @param minBlockX world X of the chunk's min corner (the town's local x=0)
     * @param minBlockZ world Z of the chunk's min corner (the town's local z=0)
     */
    public MosEisleyPiece(RandomSource random, int minBlockX, int minBlockZ) {
        super(ModStructures.MOS_EISLEY_PIECE.get(), minBlockX, 64, minBlockZ,
            MosEisleyLayout.SIZE_X, MosEisleyLayout.SIZE_Y, MosEisleyLayout.SIZE_Z,
            getRandomHorizontalDirection(random));
    }

    public MosEisleyPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.MOS_EISLEY_PIECE.get(), tag);
    }

    // addAdditionalSaveData is inherited from ScatteredFeaturePiece: it persists
    // Width/Height/Depth and the derived HPos so reloads reproduce the same box.

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // Snap the box to the average ground height under it before placing any
        // blocks; bails out (leaving the town ungenerated in this chunk pass)
        // if no columns of the box fall inside the current chunk.
        if (!this.updateAverageGroundHeight(level, box, 0)) {
            return;
        }
        for (var p : MosEisleyLayout.placements()) {
            var state = switch (p.kind()) {
                case STREET -> Blocks.SAND.defaultBlockState();
                case FLOOR, WALL -> Blocks.SANDSTONE.defaultBlockState();
                case DOME, BAR -> Blocks.SMOOTH_SANDSTONE.defaultBlockState();
                case ROOF -> Blocks.CUT_SANDSTONE.defaultBlockState();
                case WINDOW -> Blocks.GLASS.defaultBlockState();
                case SEAT -> Blocks.SMOOTH_SANDSTONE_SLAB.defaultBlockState();
                case PAD, PAD_WALL -> Blocks.SMOOTH_STONE.defaultBlockState();
                case JUKEBOX -> Blocks.JUKEBOX.defaultBlockState();
                case LANTERN -> Blocks.LANTERN.defaultBlockState();
                case DOOR_AIR, AIR -> Blocks.AIR.defaultBlockState();
                case LAMP, VAPORATOR, TABLE, CHEST_CANTINA, CHEST_DOCKING -> null; // handled below
                case JAWA, STORMTROOPER, ASTROMECH -> Blocks.AIR.defaultBlockState(); // carve air so street life doesn't suffocate on slopes; entity spawn below
            };
            if (state != null) {
                this.placeBlock(level, state, p.dx(), p.dy(), p.dz(), box);
            } else {
                switch (p.kind()) {
                    case CHEST_CANTINA ->
                        this.createChest(level, box, random, p.dx(), p.dy(), p.dz(), CANTINA_LOOT);
                    case CHEST_DOCKING ->
                        this.createChest(level, box, random, p.dx(), p.dy(), p.dz(), DOCKING_LOOT);
                    case VAPORATOR -> {
                        // GX-8 vaporator: a slim 3-tall condenser mast — two
                        // wall-block segments topped with a lightning rod
                        // (same build as MoistureFarmPiece).
                        this.placeBlock(level, Blocks.ANDESITE_WALL.defaultBlockState(),
                            p.dx(), p.dy(), p.dz(), box);
                        this.placeBlock(level, Blocks.ANDESITE_WALL.defaultBlockState(),
                            p.dx(), p.dy() + 1, p.dz(), box);
                        this.placeBlock(level, Blocks.LIGHTNING_ROD.defaultBlockState(),
                            p.dx(), p.dy() + 2, p.dz(), box);
                    }
                    case LAMP -> {
                        // Street lamp: torch on a slim sandstone post.
                        this.placeBlock(level, Blocks.SANDSTONE_WALL.defaultBlockState(),
                            p.dx(), p.dy(), p.dz(), box);
                        this.placeBlock(level, Blocks.SANDSTONE_WALL.defaultBlockState(),
                            p.dx(), p.dy() + 1, p.dz(), box);
                        this.placeBlock(level, Blocks.TORCH.defaultBlockState(),
                            p.dx(), p.dy() + 2, p.dz(), box);
                    }
                    case TABLE -> {
                        // Cantina table: fence post with a pressure-plate top.
                        this.placeBlock(level, Blocks.SPRUCE_FENCE.defaultBlockState(),
                            p.dx(), p.dy(), p.dz(), box);
                        this.placeBlock(level, Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState(),
                            p.dx(), p.dy() + 1, p.dz(), box);
                    }
                    default -> { }
                }
            }
        }

        // Street life: spawn the living inhabitants at generation time.
        // Coordinates are mapped local->world through the oriented box
        // (getWorldPos), so the crowd rotates/mirrors with the piece — same
        // pattern as ImperialOutpostPiece's garrison loop. Skipped if a marker
        // falls outside the current chunk's box (a 48x48 town straddles
        // several chunks).
        for (var p : MosEisleyLayout.placements()) {
            EntityType<? extends Mob> type = switch (p.kind()) {
                case JAWA -> ModEntities.JAWA.get();
                case STORMTROOPER -> ModEntities.STORMTROOPER.get();
                case ASTROMECH -> ModEntities.ASTROMECH.get();
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
            // finalizeSpawn is load-bearing: SwMob#finalizeSpawn equips the
            // checkpoint troopers' weapons, and addFreshEntityWithPassengers
            // does not call it.
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                EntitySpawnReason.STRUCTURE, null);
            level.addFreshEntityWithPassengers(mob);
        }
    }
}
