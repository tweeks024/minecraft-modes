package com.tweeks.thief;

import com.mojang.logging.LogUtils;
import com.tweeks.thief.entity.ThiefEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

@Mod(ThiefMod.MOD_ID)
public class ThiefMod {
    public static final String MOD_ID = "thief";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ThiefMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Thief mod loading");
        Registration.register(modEventBus);
        modEventBus.addListener(ThiefMod::registerEntityAttributes);
        modEventBus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) ->
            event.register(Registration.THIEF.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ThiefEntity::checkSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE));
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(Registration.THIEF.get(), ThiefEntity.createAttributes().build());
    }

    @net.neoforged.fml.common.EventBusSubscriber(modid = MOD_ID)
    public static class GameEvents {

        private static int tickCounter = 0;
        private static final int CHECK_INTERVAL_TICKS = 6000;

        @net.neoforged.bus.api.SubscribeEvent
        public static void onServerTick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
            if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel sl)) return;
            if (++tickCounter < CHECK_INTERVAL_TICKS) return;
            tickCounter = 0;

            if (sl.getRandom().nextFloat() > 0.10f) return;

            java.util.List<ThiefEntity> existing = sl.getEntitiesOfClass(
                ThiefEntity.class,
                net.minecraft.world.phys.AABB.ofSize(net.minecraft.world.phys.Vec3.ZERO, 0, 0, 0).inflate(10_000_000));
            if (!existing.isEmpty()) return;

            sl.players().stream().findAny().ifPresent(player -> {
                ThiefEntity thief = Registration.THIEF.get().create(sl,
                    net.minecraft.world.entity.EntitySpawnReason.NATURAL);
                if (thief == null) return;
                net.minecraft.core.BlockPos pos = player.blockPosition();
                thief.snapTo((double) pos.getX(), (double) pos.getY(), (double) pos.getZ(), 0.0f, 0.0f);
                sl.addFreshEntity(thief);
            });
        }
    }
}
