package com.tweeks.wildwest;

import com.mojang.logging.LogUtils;
import com.tweeks.wildwest.effect.ModEffects;
import com.tweeks.wildwest.entity.BanditEntity;
import com.tweeks.wildwest.entity.BanditLeaderEntity;
import com.tweeks.wildwest.entity.DeputyEntity;
import com.tweeks.wildwest.entity.AgentCloneEntity;
import com.tweeks.wildwest.entity.AgentEntity;
import com.tweeks.wildwest.entity.HerobrineEntity;
import com.tweeks.wildwest.entity.GrimReaperEntity;
import com.tweeks.wildwest.entity.NullEntity;
import com.tweeks.wildwest.entity.PirateCaptainEntity;
import com.tweeks.wildwest.entity.PirateEntity;
import com.tweeks.wildwest.entity.RedstoneGolemEntity;
import com.tweeks.wildwest.entity.ScytheSkeletonEntity;
import com.tweeks.wildwest.entity.SherrifEntity;
import com.tweeks.wildwest.entity.SkeletonPirateEntity;
import com.tweeks.wildwest.entity.SteveStackerEntity;
import com.tweeks.wildwest.entity.WalkerEntity;
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
        ModBlocks.register(modEventBus);
        Registration.register(modEventBus);
        com.tweeks.wildwest.item.ModDataComponents.register(modEventBus);
        com.tweeks.wildwest.effect.ModAttachments.register(modEventBus);
        ModEffects.register(modEventBus);
        ModSounds.register(modEventBus);
        modEventBus.addListener(WildWestMod::registerEntityAttributes);

        // Register the Void Mark damage-save handler against the GAME bus.
        // ZombieVirusHandler uses @EventBusSubscriber to auto-register, but
        // this one is colocated explicitly to keep the wiring discoverable
        // alongside the spawn-placement registrations below.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(
            com.tweeks.wildwest.event.VoidMarkHandler.class);

        modEventBus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) -> {
            event.register(ModEntities.BANDIT.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.wildwest.spawning.OutlawSpawnRules::checkSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.ANOMALY.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.BANDIT_LEADER.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.wildwest.spawning.OutlawSpawnRules::checkLeaderSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.WALKER.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.STEVE_STACKER.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.HEROBRINE.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.wildwest.spawning.HerobrineSpawnRules::checkSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.AGENT.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.wildwest.spawning.AgentSpawnRules::checkSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.NULL.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.wildwest.spawning.NullSpawnRules::checkSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.GRIM_REAPER.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.wildwest.spawning.GrimReaperSpawnRules::checkSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.PIRATE.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.SKELETON_PIRATE.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.PIRATE_CAPTAIN.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.CRAB.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.wildwest.entity.CrabEntity::checkCrabSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.DEPUTY.get(), DeputyEntity.createAttributes().build());
        event.put(ModEntities.SHERRIF.get(), SherrifEntity.createAttributes().build());
        event.put(ModEntities.BANDIT.get(), BanditEntity.createAttributes().build());
        event.put(ModEntities.ANOMALY.get(), com.tweeks.wildwest.entity.AnomalyEntity.createAttributes().build());
        event.put(ModEntities.BANDIT_LEADER.get(), BanditLeaderEntity.createAttributes().build());
        event.put(ModEntities.WALKER.get(), WalkerEntity.createAttributes().build());
        event.put(ModEntities.STEVE_STACKER.get(), SteveStackerEntity.createAttributes().build());
        event.put(ModEntities.HEROBRINE.get(), HerobrineEntity.createAttributes().build());
        event.put(ModEntities.AGENT.get(), AgentEntity.createAttributes().build());
        event.put(ModEntities.AGENT_CLONE.get(), AgentCloneEntity.createAttributes().build());
        event.put(ModEntities.NULL.get(), NullEntity.createAttributes().build());
        event.put(ModEntities.GRIM_REAPER.get(), GrimReaperEntity.createAttributes().build());
        event.put(ModEntities.SCYTHE_SKELETON.get(), ScytheSkeletonEntity.createAttributes().build());
        event.put(ModEntities.PIRATE.get(), PirateEntity.createAttributes().build());
        event.put(ModEntities.SKELETON_PIRATE.get(), SkeletonPirateEntity.createAttributes().build());
        event.put(ModEntities.PIRATE_CAPTAIN.get(), PirateCaptainEntity.createAttributes().build());
        event.put(ModEntities.REDSTONE_GOLEM.get(), RedstoneGolemEntity.createAttributes().build());
        event.put(ModEntities.CRAB.get(), com.tweeks.wildwest.entity.CrabEntity.createAttributes().build());
    }
}
