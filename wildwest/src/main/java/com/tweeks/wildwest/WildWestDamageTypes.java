package com.tweeks.wildwest;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class WildWestDamageTypes {
    private WildWestDamageTypes() {}

    public static final ResourceKey<DamageType> GUNSHOT = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "gunshot"));

    public static final ResourceKey<DamageType> CLUB = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "club"));

    public static final ResourceKey<DamageType> KNIFE = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "knife"));

    public static final ResourceKey<DamageType> METEOR = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "meteor"));

    public static final ResourceKey<DamageType> CANNONBALL = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "cannonball"));

    public static final ResourceKey<DamageType> PISTON_PUNCH = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "piston_punch"));

    public static DamageSource gunshot(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(GUNSHOT),
            attacker);
    }

    public static DamageSource club(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(CLUB),
            attacker);
    }

    public static DamageSource knife(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(KNIFE),
            attacker);
    }

    public static DamageSource meteor(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(METEOR),
            attacker);
    }

    public static DamageSource meteorAoe(Level level) {
        return new DamageSource(
            level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(METEOR));
    }

    public static DamageSource cannonball(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(CANNONBALL),
            attacker);
    }

    public static DamageSource cannonballAoe(Level level) {
        return new DamageSource(
            level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(CANNONBALL));
    }

    public static DamageSource pistonPunch(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(PISTON_PUNCH),
            attacker);
    }
}
