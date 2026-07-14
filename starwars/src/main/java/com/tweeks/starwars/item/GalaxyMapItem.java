package com.tweeks.starwars.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.tweeks.starwars.network.S2CGalaxyMapPacket;
import com.tweeks.starwars.world.gate.PortalRecords;
import com.tweeks.starwars.world.planet.Planet;
import com.tweeks.starwars.world.planet.PlanetVisits;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A navigator's chart of the six worlds: which you've set foot on, how many
 * gates each has, and where the nearest gates in your current world stand.
 * Server gathers the data (visited attachment + per-level PortalRecords) and
 * ships it to the client screen.
 */
public class GalaxyMapItem extends Item {
    private static final int MAX_NEARBY_GATES = 6;

    public GalaxyMapItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        int visitedMask = PlanetVisits.get(serverPlayer).toMask();

        List<Integer> gateCounts = new ArrayList<>(Planet.COUNT);
        for (Planet planet : Planet.values()) {
            ServerLevel planetLevel = serverPlayer.level().getServer().getLevel(planet.levelKey());
            gateCounts.add(planetLevel == null ? 0 : PortalRecords.get(planetLevel).all().size());
        }

        List<S2CGalaxyMapPacket.GateInfo> nearby = PortalRecords.get(serverPlayer.level()).all().stream()
            .sorted(Comparator.comparingDouble(g -> g.origin().distSqr(serverPlayer.blockPosition())))
            .limit(MAX_NEARBY_GATES)
            .map(g -> new S2CGalaxyMapPacket.GateInfo(g.origin(), g.destination().ordinal()))
            .toList();

        PacketDistributor.sendToPlayer(serverPlayer,
            new S2CGalaxyMapPacket(visitedMask, gateCounts, nearby));
        return InteractionResult.CONSUME;
    }
}
