package com.tweeks.starwars;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class StarWarsDamageTypes {
    private StarWarsDamageTypes() {}

    public static final ResourceKey<DamageType> BLASTER_BOLT = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "blaster_bolt"));

    public static final ResourceKey<DamageType> LIGHTSABER = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "lightsaber"));

    public static final ResourceKey<DamageType> FORCE_LIGHTNING = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "force_lightning"));

    public static DamageSource blasterBolt(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(BLASTER_BOLT),
            attacker);
    }

    public static DamageSource forceLightning(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(FORCE_LIGHTNING),
            attacker);
    }

    public static DamageSource forceLightningAoe(Level level) {
        return new DamageSource(
            level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(FORCE_LIGHTNING));
    }
}
