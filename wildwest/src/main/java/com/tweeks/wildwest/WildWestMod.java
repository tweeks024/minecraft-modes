package com.tweeks.wildwest;

import com.mojang.logging.LogUtils;
import com.tweeks.wildwest.entity.BanditEntity;
import com.tweeks.wildwest.entity.BanditLeaderEntity;
import com.tweeks.wildwest.entity.DeputyEntity;
import com.tweeks.wildwest.entity.SherrifEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

@Mod(WildWestMod.MOD_ID)
public class WildWestMod {
    public static final String MOD_ID = "wildwest";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WildWestMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Wild West mod loading");
        // Entity types must register before items so SpawnEggItem can resolve ModEntities.*.get().
        ModEntities.register(modEventBus);
        Registration.register(modEventBus);
        ModSounds.register(modEventBus);
        modEventBus.addListener(WildWestMod::registerEntityAttributes);
        modEventBus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) -> {
            event.register(ModEntities.BANDIT.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.wildwest.spawning.OutlawSpawnRules::checkSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.BANDIT_LEADER.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.wildwest.spawning.OutlawSpawnRules::checkLeaderSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.DEPUTY.get(), DeputyEntity.createAttributes().build());
        event.put(ModEntities.SHERRIF.get(), SherrifEntity.createAttributes().build());
        event.put(ModEntities.BANDIT.get(), BanditEntity.createAttributes().build());
        event.put(ModEntities.BANDIT_LEADER.get(), BanditLeaderEntity.createAttributes().build());
    }
}
