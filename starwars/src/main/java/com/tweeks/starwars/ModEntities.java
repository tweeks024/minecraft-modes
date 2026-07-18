package com.tweeks.starwars;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, StarWarsMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.StormtrooperEntity>> STORMTROOPER =
        ENTITY_TYPES.register("stormtrooper", () -> EntityType.Builder.<com.tweeks.starwars.entity.StormtrooperEntity>of(
                com.tweeks.starwars.entity.StormtrooperEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "stormtrooper"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.BattleDroidEntity>> BATTLE_DROID =
        ENTITY_TYPES.register("battle_droid", () -> EntityType.Builder.<com.tweeks.starwars.entity.BattleDroidEntity>of(
                com.tweeks.starwars.entity.BattleDroidEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.9f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "battle_droid"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.JediKnightEntity>> JEDI_KNIGHT =
        ENTITY_TYPES.register("jedi_knight", () -> EntityType.Builder.<com.tweeks.starwars.entity.JediKnightEntity>of(
                com.tweeks.starwars.entity.JediKnightEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "jedi_knight"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.DarthVaderEntity>> DARTH_VADER =
        ENTITY_TYPES.register("darth_vader", () -> EntityType.Builder.<com.tweeks.starwars.entity.DarthVaderEntity>of(
                com.tweeks.starwars.entity.DarthVaderEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 2.0f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "darth_vader"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.DarthMaulEntity>> DARTH_MAUL =
        ENTITY_TYPES.register("darth_maul", () -> EntityType.Builder.<com.tweeks.starwars.entity.DarthMaulEntity>of(
                com.tweeks.starwars.entity.DarthMaulEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.9f)
            .clientTrackingRange(12)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "darth_maul"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.LukeSkywalkerEntity>> LUKE_SKYWALKER =
        ENTITY_TYPES.register("luke_skywalker", () -> EntityType.Builder.<com.tweeks.starwars.entity.LukeSkywalkerEntity>of(
                com.tweeks.starwars.entity.LukeSkywalkerEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "luke_skywalker"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.ObiWanEntity>> OBI_WAN =
        ENTITY_TYPES.register("obi_wan", () -> EntityType.Builder.<com.tweeks.starwars.entity.ObiWanEntity>of(
                com.tweeks.starwars.entity.ObiWanEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "obi_wan"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.BobaFettEntity>> BOBA_FETT =
        ENTITY_TYPES.register("boba_fett", () -> EntityType.Builder.<com.tweeks.starwars.entity.BobaFettEntity>of(
                com.tweeks.starwars.entity.BobaFettEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "boba_fett"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.AstromechEntity>> ASTROMECH =
        ENTITY_TYPES.register("astromech", () -> EntityType.Builder.<com.tweeks.starwars.entity.AstromechEntity>of(
                com.tweeks.starwars.entity.AstromechEntity::new, MobCategory.CREATURE)
            .sized(0.7f, 1.1f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "astromech"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.HanSoloEntity>> HAN_SOLO =
        ENTITY_TYPES.register("han_solo", () -> EntityType.Builder.<com.tweeks.starwars.entity.HanSoloEntity>of(
                com.tweeks.starwars.entity.HanSoloEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "han_solo"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.PrincessLeiaEntity>> PRINCESS_LEIA =
        ENTITY_TYPES.register("princess_leia", () -> EntityType.Builder.<com.tweeks.starwars.entity.PrincessLeiaEntity>of(
                com.tweeks.starwars.entity.PrincessLeiaEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.9f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "princess_leia"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.LandspeederEntity>> LANDSPEEDER =
        ENTITY_TYPES.register("landspeeder", () -> EntityType.Builder.<com.tweeks.starwars.entity.LandspeederEntity>of(
                com.tweeks.starwars.entity.LandspeederEntity::new, MobCategory.MISC)
            .sized(2.0f, 0.8f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "landspeeder"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.JawaEntity>> JAWA =
        ENTITY_TYPES.register("jawa", () -> EntityType.Builder.<com.tweeks.starwars.entity.JawaEntity>of(
                com.tweeks.starwars.entity.JawaEntity::new, MobCategory.CREATURE)
            .sized(0.5f, 1.1f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "jawa"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.TuskenRaiderEntity>> TUSKEN_RAIDER =
        ENTITY_TYPES.register("tusken_raider", () -> EntityType.Builder.<com.tweeks.starwars.entity.TuskenRaiderEntity>of(
                com.tweeks.starwars.entity.TuskenRaiderEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.9f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "tusken_raider"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.BanthaEntity>> BANTHA =
        ENTITY_TYPES.register("bantha", () -> EntityType.Builder.<com.tweeks.starwars.entity.BanthaEntity>of(
                com.tweeks.starwars.entity.BanthaEntity::new, MobCategory.CREATURE)
            .sized(1.6f, 2.2f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "bantha"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.RebelTrooperEntity>> REBEL_TROOPER =
        ENTITY_TYPES.register("rebel_trooper", () -> EntityType.Builder.<com.tweeks.starwars.entity.RebelTrooperEntity>of(
                com.tweeks.starwars.entity.RebelTrooperEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.8f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "rebel_trooper"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.ProbeDroidEntity>> PROBE_DROID =
        ENTITY_TYPES.register("probe_droid", () -> EntityType.Builder.<com.tweeks.starwars.entity.ProbeDroidEntity>of(
                com.tweeks.starwars.entity.ProbeDroidEntity::new, MobCategory.MONSTER)
            .sized(0.9f, 1.6f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "probe_droid"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.WampaEntity>> WAMPA =
        ENTITY_TYPES.register("wampa", () -> EntityType.Builder.<com.tweeks.starwars.entity.WampaEntity>of(
                com.tweeks.starwars.entity.WampaEntity::new, MobCategory.MONSTER)
            .sized(1.2f, 2.6f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "wampa"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.TauntaunEntity>> TAUNTAUN =
        ENTITY_TYPES.register("tauntaun", () -> EntityType.Builder.<com.tweeks.starwars.entity.TauntaunEntity>of(
                com.tweeks.starwars.entity.TauntaunEntity::new, MobCategory.CREATURE)
            .sized(0.9f, 2.0f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "tauntaun"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.SnowtrooperEntity>> SNOWTROOPER =
        ENTITY_TYPES.register("snowtrooper", () -> EntityType.Builder.<com.tweeks.starwars.entity.SnowtrooperEntity>of(
                com.tweeks.starwars.entity.SnowtrooperEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "snowtrooper"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.DragonsnakeEntity>> DRAGONSNAKE =
        ENTITY_TYPES.register("dragonsnake", () -> EntityType.Builder.<com.tweeks.starwars.entity.DragonsnakeEntity>of(
                com.tweeks.starwars.entity.DragonsnakeEntity::new, MobCategory.MONSTER)
            .sized(1.4f, 0.6f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "dragonsnake"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.BogwingEntity>> BOGWING =
        ENTITY_TYPES.register("bogwing", () -> EntityType.Builder.<com.tweeks.starwars.entity.BogwingEntity>of(
                com.tweeks.starwars.entity.BogwingEntity::new, MobCategory.AMBIENT)
            .sized(0.7f, 0.4f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "bogwing"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.YodaEntity>> YODA =
        ENTITY_TYPES.register("yoda", () -> EntityType.Builder.<com.tweeks.starwars.entity.YodaEntity>of(
                com.tweeks.starwars.entity.YodaEntity::new, MobCategory.CREATURE)
            .sized(0.5f, 0.8f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "yoda"))));

    // ---- wave 3: vehicles, siege boss, ambient droid ----

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.SpeederBikeEntity>> SPEEDER_BIKE =
        ENTITY_TYPES.register("speeder_bike", () -> EntityType.Builder.<com.tweeks.starwars.entity.SpeederBikeEntity>of(
                com.tweeks.starwars.entity.SpeederBikeEntity::new, MobCategory.MISC)
            .sized(1.6f, 0.8f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "speeder_bike"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.XwingEntity>> XWING =
        ENTITY_TYPES.register("xwing", () -> EntityType.Builder.<com.tweeks.starwars.entity.XwingEntity>of(
                com.tweeks.starwars.entity.XwingEntity::new, MobCategory.MISC)
            .sized(2.8f, 1.4f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "xwing"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.TieFighterEntity>> TIE_FIGHTER =
        ENTITY_TYPES.register("tie_fighter", () -> EntityType.Builder.<com.tweeks.starwars.entity.TieFighterEntity>of(
                com.tweeks.starwars.entity.TieFighterEntity::new, MobCategory.MISC)
            .sized(2.4f, 2.2f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "tie_fighter"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.AtAtEntity>> AT_AT =
        ENTITY_TYPES.register("at_at", () -> EntityType.Builder.<com.tweeks.starwars.entity.AtAtEntity>of(
                com.tweeks.starwars.entity.AtAtEntity::new, MobCategory.MONSTER)
            .sized(5.0f, 9.0f)
            .eyeHeight(8.5f)
            .clientTrackingRange(12)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "at_at"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.BandDroidEntity>> BAND_DROID =
        ENTITY_TYPES.register("band_droid", () -> EntityType.Builder.<com.tweeks.starwars.entity.BandDroidEntity>of(
                com.tweeks.starwars.entity.BandDroidEntity::new, MobCategory.CREATURE)
            .sized(0.5f, 1.4f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "band_droid"))));

    // ---- companions: tameable follower + carryable rare ----

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.ChewbaccaEntity>> CHEWBACCA =
        ENTITY_TYPES.register("chewbacca", () -> EntityType.Builder.<com.tweeks.starwars.entity.ChewbaccaEntity>of(
                com.tweeks.starwars.entity.ChewbaccaEntity::new, MobCategory.CREATURE)
            .sized(0.9f, 2.4f)
            .clientTrackingRange(12)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "chewbacca"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.GroguEntity>> GROGU =
        ENTITY_TYPES.register("grogu", () -> EntityType.Builder.<com.tweeks.starwars.entity.GroguEntity>of(
                com.tweeks.starwars.entity.GroguEntity::new, MobCategory.CREATURE)
            .sized(0.4f, 0.6f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "grogu"))));

    // ---- forest native: small melee fighter of the Rebellion ----

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.EwokEntity>> EWOK =
        ENTITY_TYPES.register("ewok", () -> EntityType.Builder.<com.tweeks.starwars.entity.EwokEntity>of(
                com.tweeks.starwars.entity.EwokEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.0f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "ewok"))));

    // ---- Jabba's Palace: the caged beast + the crime lord (piece-spawned) ----

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.RancorEntity>> RANCOR =
        ENTITY_TYPES.register("rancor", () -> EntityType.Builder.<com.tweeks.starwars.entity.RancorEntity>of(
                com.tweeks.starwars.entity.RancorEntity::new, MobCategory.MONSTER)
            .sized(2.4f, 4.4f)
            .eyeHeight(3.8f)
            .clientTrackingRange(12)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "rancor"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.JabbaEntity>> JABBA =
        ENTITY_TYPES.register("jabba", () -> EntityType.Builder.<com.tweeks.starwars.entity.JabbaEntity>of(
                com.tweeks.starwars.entity.JabbaEntity::new, MobCategory.CREATURE)
            .sized(2.0f, 2.0f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "jabba"))));

    // ---- the endgame: Emperor Palpatine, enthroned on Coruscant ----

    public static final DeferredHolder<EntityType<?>, EntityType<com.tweeks.starwars.entity.PalpatineEntity>> PALPATINE =
        ENTITY_TYPES.register("palpatine", () -> EntityType.Builder.<com.tweeks.starwars.entity.PalpatineEntity>of(
                com.tweeks.starwars.entity.PalpatineEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.9f)
            .clientTrackingRange(12)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "palpatine"))));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
