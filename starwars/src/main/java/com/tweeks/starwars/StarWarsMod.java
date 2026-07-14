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
        com.tweeks.starwars.world.planet.ModChunkGenerators.register(modEventBus);
        com.tweeks.starwars.item.ModRecipeSerializers.register(modEventBus);

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

            // EMPIRE-like monsters share the trooper darkness rule.
            event.register(ModEntities.PROBE_DROID.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.starwars.spawning.TrooperSpawnRules::checkSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.SNOWTROOPER.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.starwars.spawning.TrooperSpawnRules::checkSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            // Non-imperial hostiles use the plain vanilla monster darkness rule.
            event.register(ModEntities.TUSKEN_RAIDER.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.WAMPA.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            // CREATUREs (and Yoda, who never naturally spawns anyway) use the
            // generic mob rule, like the Jedi Knight above.
            event.register(ModEntities.JAWA.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.PathfinderMob::checkMobSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.BANTHA.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.PathfinderMob::checkMobSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.REBEL_TROOPER.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.PathfinderMob::checkMobSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.TAUNTAUN.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.PathfinderMob::checkMobSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.YODA.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.PathfinderMob::checkMobSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            // Dragonsnake spawns IN water (guardian-style placement).
            event.register(ModEntities.DRAGONSNAKE.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.tweeks.starwars.entity.DragonsnakeEntity::checkDragonsnakeSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            // Bogwing: AMBIENT flier — ground placement + generic mob rule
            // (the vanilla bat registers ON_GROUND too; its extra darkness
            // checks live in Bat::checkBatSpawnRules, which we don't need).
            event.register(ModEntities.BOGWING.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.Mob::checkMobSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            // AT-AT is a MONSTER but a DAYTIME siege walker — the monster
            // darkness rule (checkMonsterSpawnRules) would wrongly forbid it
            // in the light. Use the generic mob rule instead. (Natural-spawn
            // cost/biome wiring is owned elsewhere; this only sets HOW it may
            // place once a spawn is attempted.)
            event.register(ModEntities.AT_AT.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.PathfinderMob::checkMobSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            // band_droid deliberately has NO spawn placement: it never spawns
            // naturally (placed only inside the Mos Eisley cantina).
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
        event.put(ModEntities.PRINCESS_LEIA.get(),
            com.tweeks.starwars.entity.PrincessLeiaEntity.createAttributes().build());
        event.put(ModEntities.JAWA.get(),
            com.tweeks.starwars.entity.JawaEntity.createAttributes().build());
        event.put(ModEntities.TUSKEN_RAIDER.get(),
            com.tweeks.starwars.entity.TuskenRaiderEntity.createAttributes().build());
        event.put(ModEntities.BANTHA.get(),
            com.tweeks.starwars.entity.BanthaEntity.createAttributes().build());
        event.put(ModEntities.REBEL_TROOPER.get(),
            com.tweeks.starwars.entity.RebelTrooperEntity.createAttributes().build());
        event.put(ModEntities.PROBE_DROID.get(),
            com.tweeks.starwars.entity.ProbeDroidEntity.createAttributes().build());
        event.put(ModEntities.WAMPA.get(),
            com.tweeks.starwars.entity.WampaEntity.createAttributes().build());
        event.put(ModEntities.TAUNTAUN.get(),
            com.tweeks.starwars.entity.TauntaunEntity.createAttributes().build());
        // Snowtrooper reuses the stormtrooper attribute set (inherited static).
        event.put(ModEntities.SNOWTROOPER.get(),
            com.tweeks.starwars.entity.StormtrooperEntity.createAttributes().build());
        event.put(ModEntities.DRAGONSNAKE.get(),
            com.tweeks.starwars.entity.DragonsnakeEntity.createAttributes().build());
        event.put(ModEntities.BOGWING.get(),
            com.tweeks.starwars.entity.BogwingEntity.createAttributes().build());
        event.put(ModEntities.YODA.get(),
            com.tweeks.starwars.entity.YodaEntity.createAttributes().build());
        // wave 3: siege boss + ambient musician (the vehicles are
        // VehicleEntity, not Mobs — they take no attribute supplier).
        event.put(ModEntities.AT_AT.get(),
            com.tweeks.starwars.entity.AtAtEntity.createAttributes().build());
        event.put(ModEntities.BAND_DROID.get(),
            com.tweeks.starwars.entity.BandDroidEntity.createAttributes().build());
    }
}
