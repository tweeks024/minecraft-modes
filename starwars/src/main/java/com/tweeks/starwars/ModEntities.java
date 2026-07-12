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

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
