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

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
