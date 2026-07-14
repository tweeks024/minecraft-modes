package com.tweeks.starwars.world.gate;

import java.util.Optional;

import com.tweeks.starwars.Registration;
import com.tweeks.starwars.world.planet.CityLayout;
import com.tweeks.starwars.world.planet.Planet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Chooses (or builds) the exit gate for a hyperspace jump. Coordinates carry
 * over 1:1; within {@link #REUSE_RADIUS} of the arrival point an existing
 * recorded gate is reused, otherwise a fresh return gate is stamped on the
 * surface — on Coruscant, snapped onto the nearest street so nobody arrives
 * on a 120-block rooftop.
 */
public final class PortalLink {
    public static final int REUSE_RADIUS = 96;

    private PortalLink() {
    }

    public static @Nullable TeleportTransition exitFor(ServerLevel target, Entity entity, Planet origin, boolean axisX) {
        BlockPos approx = target.getWorldBorder().clampToBounds(entity.getX(), entity.getY(), entity.getZ());
        PortalRecords records = PortalRecords.get(target);

        Optional<PortalRecords.GateRecord> candidate = PortalRecords.nearest(records.all(), approx, REUSE_RADIUS);
        if (candidate.isPresent()) {
            PortalRecords.GateRecord gate = candidate.get();
            if (target.getBlockState(gate.origin()).getBlock() instanceof HyperspacePortalBlock) {
                return transition(target, entity, gate.origin(), gate.axisX());
            }
            // Film is gone (frame broken, area rebuilt) — drop the stale
            // record and fall through to building a fresh gate.
            records.removeAt(gate.origin());
        }

        ArrivalSpot spot = arrivalSpot(target, approx, axisX);
        stampArrivalGate(target, spot.origin(), origin, spot.axisX());
        records.put(new PortalRecords.GateRecord(spot.origin(), spot.axisX(), origin));
        return transition(target, entity, spot.origin(), spot.axisX());
    }

    record ArrivalSpot(BlockPos origin, boolean axisX) {
    }

    private static TeleportTransition transition(ServerLevel target, Entity entity, BlockPos origin, boolean axisX) {
        Vec3 drop = dropPoint(origin, axisX);
        return new TeleportTransition(target, drop, Vec3.ZERO, entity.getYRot(), entity.getXRot(),
            TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
    }

    /** Centre of the 2-wide film, feet at interior floor level. */
    static Vec3 dropPoint(BlockPos origin, boolean axisX) {
        return axisX
            ? new Vec3(origin.getX() + 1.0, origin.getY(), origin.getZ() + 0.5)
            : new Vec3(origin.getX() + 0.5, origin.getY(), origin.getZ() + 1.0);
    }

    /** Where (and how oriented) a fresh arrival gate should be built. */
    private static ArrivalSpot arrivalSpot(ServerLevel target, BlockPos approx, boolean requestedAxisX) {
        Planet here = Planet.byLevel(target.dimension());
        if (here == Planet.CORUSCANT) {
            long snapped = snapToStreet(approx.getX(), approx.getZ());
            int x = unpackX(snapped);
            int z = unpackZ(snapped);
            // Snapping x means the street runs north-south: the gate stands
            // along it (plane on Z) so its 3-wide footprint fits the road.
            boolean axisX = x == approx.getX();
            return new ArrivalSpot(new BlockPos(x, CityLayout.STREET_Y + 1, z), axisX);
        }
        if (here == Planet.DEATH_STAR) {
            // The station is solid — snap onto a corridor deck so you never
            // arrive inside a wall (GateBuilder's CLEAR step then opens the
            // pocket around the frame).
            long seed = target.getSeed();
            int[] spot = com.tweeks.starwars.world.planet.StationLayout.arrivalPos(seed, approx.getX(), approx.getZ());
            return new ArrivalSpot(new BlockPos(spot[0], spot[1], spot[2]), requestedAxisX);
        }
        // Force the chunk into existence so the heightmap answers truthfully.
        target.getChunk(approx.getX() >> 4, approx.getZ() >> 4);
        int surface = target.getHeight(Heightmap.Types.MOTION_BLOCKING, approx.getX(), approx.getZ());
        int y = Mth.clamp(surface, target.getMinY() + 2, target.getMaxY() - GateBuilder.HEIGHT - 3);
        return new ArrivalSpot(new BlockPos(approx.getX(), y, approx.getZ()), requestedAxisX);
    }

    private static void stampArrivalGate(ServerLevel target, BlockPos origin, Planet boundTo, boolean axisX) {
        BlockState frame = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState platform = platformFor(Planet.byLevel(target.dimension()));
        BlockState film = Registration.HYPERSPACE_PORTAL.get().defaultBlockState()
            .setValue(HyperspacePortalBlock.PLANET, boundTo)
            .setValue(HyperspacePortalBlock.AXIS, axisX ? Direction.Axis.X : Direction.Axis.Z);
        for (GateBuilder.Placement p : GateBuilder.arrivalGate(axisX)) {
            BlockPos pos = origin.offset(p.dx(), p.dy(), p.dz());
            switch (p.kind()) {
                case CLEAR -> {
                    if (!target.getBlockState(pos).isAir()) {
                        target.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
                case PLATFORM -> target.setBlock(pos, platform, Block.UPDATE_ALL);
                case FRAME -> target.setBlock(pos, frame, Block.UPDATE_ALL);
                case PORTAL -> target.setBlock(pos, film, Block.UPDATE_ALL);
            }
        }
    }

    private static BlockState platformFor(@Nullable Planet planet) {
        if (planet == null) {
            return Blocks.STONE_BRICKS.defaultBlockState();
        }
        return switch (planet) {
            case TATOOINE -> Blocks.SMOOTH_SANDSTONE.defaultBlockState();
            case CORUSCANT -> Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
            case DAGOBAH -> Blocks.MUD_BRICKS.defaultBlockState();
            case HOTH -> Blocks.PACKED_ICE.defaultBlockState();
            case DEATH_STAR -> Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
            case ANDOR, HOME -> Blocks.STONE_BRICKS.defaultBlockState();
        };
    }

    // ------------------------------------------------------------------
    // Street snapping (pure, unit-tested)

    /** Nearest street-lane centre coordinate (local 2 within each 24-cell). */
    static int snapToLane(int coord) {
        int base = Math.floorDiv(coord, CityLayout.CELL) * CityLayout.CELL + 2;
        int best = base;
        for (int candidate : new int[]{base - CityLayout.CELL, base + CityLayout.CELL}) {
            if (Math.abs(candidate - coord) < Math.abs(best - coord)) {
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Snaps (x, z) onto the nearest street lane, moving only the axis that is
     * closer to a lane. Packed into a long as (x << 32 | z & 0xFFFFFFFF).
     */
    static long snapToStreet(int x, int z) {
        int laneX = snapToLane(x);
        int laneZ = snapToLane(z);
        int snappedX;
        int snappedZ;
        if (Math.abs(laneX - x) <= Math.abs(laneZ - z)) {
            snappedX = laneX;
            snappedZ = z;
        } else {
            snappedX = x;
            snappedZ = laneZ;
        }
        return ((long) snappedX << 32) | (snappedZ & 0xFFFFFFFFL);
    }

    static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    static int unpackZ(long packed) {
        return (int) packed;
    }
}
