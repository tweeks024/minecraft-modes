package com.tweeks.starwars;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(StarWarsMod.MOD_ID)
public class StarWarsMod {
    public static final String MOD_ID = "starwars";
    public static final Logger LOGGER = LogUtils.getLogger();

    public StarWarsMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Star Wars mod loading");
        // Entity types register before items so SpawnEggItem can resolve ModEntities.*.get().
        ModEntities.register(modEventBus);
        Registration.register(modEventBus);
        ModSounds.register(modEventBus);
        com.tweeks.starwars.item.ModDataComponents.register(modEventBus);
        com.tweeks.starwars.faction.ModAttachments.register(modEventBus);
        com.tweeks.starwars.world.ModStructures.register(modEventBus);

        modEventBus.addListener(StarWarsMod::registerEntityAttributes);

        modEventBus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) -> {
            event.register(ModEntities.STORMTROOPER.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.starwars.spawning.TrooperSpawnRules::checkSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.BATTLE_DROID.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.starwars.spawning.TrooperSpawnRules::checkSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            // Jedi Knight is a CREATURE, not a MONSTER — monster spawn rules
            // (darkness required) don't apply; use vanilla's generic mob rule.
            event.register(ModEntities.JEDI_KNIGHT.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.PathfinderMob::checkMobSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            // Astromech is a CREATURE too — same generic mob rule as Jedi Knight.
            event.register(ModEntities.ASTROMECH.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.PathfinderMob::checkMobSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
    }

    private static void registerEntityAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(ModEntities.STORMTROOPER.get(),
            com.tweeks.starwars.entity.StormtrooperEntity.createAttributes().build());
        event.put(ModEntities.BATTLE_DROID.get(),
            com.tweeks.starwars.entity.BattleDroidEntity.createAttributes().build());
        event.put(ModEntities.JEDI_KNIGHT.get(),
            com.tweeks.starwars.entity.JediKnightEntity.createAttributes().build());
        event.put(ModEntities.DARTH_VADER.get(),
            com.tweeks.starwars.entity.DarthVaderEntity.createAttributes().build());
        event.put(ModEntities.LUKE_SKYWALKER.get(),
            com.tweeks.starwars.entity.LukeSkywalkerEntity.createAttributes().build());
        event.put(ModEntities.OBI_WAN.get(),
            com.tweeks.starwars.entity.ObiWanEntity.createAttributes().build());
        event.put(ModEntities.BOBA_FETT.get(),
            com.tweeks.starwars.entity.BobaFettEntity.createAttributes().build());
        event.put(ModEntities.ASTROMECH.get(),
            com.tweeks.starwars.entity.AstromechEntity.createAttributes().build());
        event.put(ModEntities.HAN_SOLO.get(),
            com.tweeks.starwars.entity.HanSoloEntity.createAttributes().build());
    }
}
