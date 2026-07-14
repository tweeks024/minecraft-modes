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
 * Vader's castle on Mustafar, generated as a scattered feature.
 *
 * <p>Extends {@link ScatteredFeaturePiece} so it inherits vanilla's proven
 * placement machinery: a random horizontal orientation, local-to-world
 * coordinate mapping through the oriented bounding box, terrain-height snapping
 * via {@code updateAverageGroundHeight}, and persistence of the derived ground
 * height plus dimensions across save/reload — the same pattern as
 * {@link EscapePodPiece}.
 */
public class VaderCastlePiece extends ScatteredFeaturePiece {

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "chests/vader_castle"));

    /**
     * @param minBlockX world X of the chunk's min corner (the castle's local x=0)
     * @param minBlockZ world Z of the chunk's min corner (the castle's local z=0)
     */
    public VaderCastlePiece(RandomSource random, int minBlockX, int minBlockZ) {
        super(ModStructures.VADER_CASTLE_PIECE.get(), minBlockX, 64, minBlockZ,
            VaderCastleLayout.SIZE_X, VaderCastleLayout.SIZE_Y, VaderCastleLayout.SIZE_Z,
            getRandomHorizontalDirection(random));
    }

    public VaderCastlePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.VADER_CASTLE_PIECE.get(), tag);
    }

    // addAdditionalSaveData is inherited from ScatteredFeaturePiece: it persists
    // Width/Height/Depth and the derived HPos so reloads reproduce the same box.

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // Snap the box to the average ground height under it before placing any
        // blocks; bails out (leaving the castle ungenerated in this chunk pass)
        // if no columns of the box fall inside the current chunk.
        if (!this.updateAverageGroundHeight(level, box, 0)) {
            return;
        }
        for (var p : VaderCastleLayout.placements()) {
            var state = switch (p.kind()) {
                // Blackstone walls veined with polished blackstone.
                case WALL -> (p.dx() + p.dy() + p.dz()) % 3 == 0
                    ? Blocks.POLISHED_BLACKSTONE.defaultBlockState()
                    : Blocks.BLACKSTONE.defaultBlockState();
                case PILLAR -> Blocks.BASALT.defaultBlockState();
                case FLOOR -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                case THRONE -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
                case MAGMA -> Blocks.MAGMA_BLOCK.defaultBlockState();
                // Vader's seat marker is left empty: Vader is a singleton spawned
                // by NamedCharacterSpawner, never by the structure piece.
                case GATE_AIR, AIR, VADER_SPAWN -> Blocks.AIR.defaultBlockState();
                case BRAZIER, CHEST -> null;                 // handled below
                case STORMTROOPER -> Blocks.AIR.defaultBlockState(); // carve air so the guard doesn't suffocate; entity spawn below
            };
            if (state != null) {
                this.placeBlock(level, state, p.dx(), p.dy(), p.dz(), box);
            } else if (p.kind() == VaderCastleLayout.Kind.CHEST) {
                this.createChest(level, box, random, p.dx(), p.dy(), p.dz(), LOOT);
            } else {
                // Brazier: an everburning fire on a netherrack pocket for the
                // lava glow.
                this.placeBlock(level, Blocks.NETHERRACK.defaultBlockState(),
                    p.dx(), p.dy(), p.dz(), box);
                this.placeBlock(level, Blocks.FIRE.defaultBlockState(),
                    p.dx(), p.dy() + 1, p.dz(), box);
            }
        }

        // The garrison: spawn the stormtrooper guards at generation time.
        // Coordinates are mapped local->world through the oriented box
        // (getWorldPos), so the guard posts rotate/mirror with the piece — same
        // pattern as EchoBasePiece's garrison loop. Vader himself is NOT spawned
        // here (see VADER_SPAWN above). Skipped if a marker falls outside the
        // current chunk's box.
        for (var p : VaderCastleLayout.placements()) {
            EntityType<? extends Mob> type = switch (p.kind()) {
                case STORMTROOPER -> ModEntities.STORMTROOPER.get();
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
            // trooper's weapon, and addFreshEntityWithPassengers does not call it.
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                EntitySpawnReason.STRUCTURE, null);
            level.addFreshEntityWithPassengers(mob);
        }
    }
}
