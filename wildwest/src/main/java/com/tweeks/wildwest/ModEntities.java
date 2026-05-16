package com.tweeks.wildwest;

import com.tweeks.wildwest.entity.BanditEntity;
import com.tweeks.wildwest.entity.BanditLeaderEntity;
import com.tweeks.wildwest.entity.BulletEntity;
import com.tweeks.wildwest.entity.DeputyEntity;
import com.tweeks.wildwest.entity.AgentCloneEntity;
import com.tweeks.wildwest.entity.AgentEntity;
import com.tweeks.wildwest.entity.HerobrineEntity;
import com.tweeks.wildwest.entity.SherrifEntity;
import com.tweeks.wildwest.entity.SteveStackerEntity;
import com.tweeks.wildwest.entity.WalkerEntity;
import com.tweeks.wildwest.entity.projectile.MeteorEntity;
import com.tweeks.wildwest.entity.projectile.TaintedVialEntity;
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
        DeferredRegister.create(Registries.ENTITY_TYPE, WildWestMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<BulletEntity>> BULLET =
        ENTITY_TYPES.register("bullet", () -> EntityType.Builder.<BulletEntity>of(
                BulletEntity::new, MobCategory.MISC)
            .sized(0.25f, 0.25f)
            .clientTrackingRange(8)
            .updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "bullet"))));

    public static final DeferredHolder<EntityType<?>, EntityType<DeputyEntity>> DEPUTY =
        ENTITY_TYPES.register("deputy", () -> EntityType.Builder.<DeputyEntity>of(
                DeputyEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "deputy"))));

    public static final DeferredHolder<EntityType<?>, EntityType<SherrifEntity>> SHERRIF =
        ENTITY_TYPES.register("sherrif", () -> EntityType.Builder.<SherrifEntity>of(
                SherrifEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "sherrif"))));

    public static final DeferredHolder<EntityType<?>, EntityType<BanditEntity>> BANDIT =
        ENTITY_TYPES.register("bandit", () -> EntityType.Builder.<BanditEntity>of(
                BanditEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "bandit"))));

    public static final DeferredHolder<EntityType<?>, EntityType<BanditLeaderEntity>> BANDIT_LEADER =
        ENTITY_TYPES.register("bandit_leader", () -> EntityType.Builder.<BanditLeaderEntity>of(
                BanditLeaderEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "bandit_leader"))));

    public static final DeferredHolder<EntityType<?>, EntityType<WalkerEntity>> WALKER =
        ENTITY_TYPES.register("walker", () -> EntityType.Builder.<WalkerEntity>of(
                WalkerEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "walker"))));

    public static final DeferredHolder<EntityType<?>, EntityType<SteveStackerEntity>> STEVE_STACKER =
        ENTITY_TYPES.register("steve_stacker", () -> EntityType.Builder.<SteveStackerEntity>of(
                SteveStackerEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 5.85f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "steve_stacker"))));

    public static final DeferredHolder<EntityType<?>, EntityType<HerobrineEntity>> HEROBRINE =
        ENTITY_TYPES.register("herobrine", () -> EntityType.Builder.<HerobrineEntity>of(
                HerobrineEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "herobrine"))));

    public static final DeferredHolder<EntityType<?>, EntityType<AgentEntity>> AGENT =
        ENTITY_TYPES.register("the_agent", () -> EntityType.Builder.<AgentEntity>of(
                AgentEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "the_agent"))));

    public static final DeferredHolder<EntityType<?>, EntityType<AgentCloneEntity>> AGENT_CLONE =
        ENTITY_TYPES.register("the_agent_clone", () -> EntityType.Builder.<AgentCloneEntity>of(
                AgentCloneEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "the_agent_clone"))));

    public static final DeferredHolder<EntityType<?>, EntityType<MeteorEntity>> METEOR =
        ENTITY_TYPES.register("meteor", () -> EntityType.Builder.<MeteorEntity>of(
                MeteorEntity::new, MobCategory.MISC)
            .sized(0.5f, 0.5f)
            .clientTrackingRange(64)
            .updateInterval(2)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "meteor"))));

    public static final DeferredHolder<EntityType<?>, EntityType<TaintedVialEntity>> TAINTED_VIAL_PROJECTILE =
        ENTITY_TYPES.register("tainted_vial_projectile", () -> EntityType.Builder.<TaintedVialEntity>of(
                TaintedVialEntity::new, MobCategory.MISC)
            .sized(0.25f, 0.25f)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "tainted_vial_projectile"))));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
