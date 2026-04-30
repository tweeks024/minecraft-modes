package com.tweeks.wildwest;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

public final class WildWestDamageTypes {
    private WildWestDamageTypes() {}

    public static final ResourceKey<DamageType> GUNSHOT = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "gunshot"));

    public static DamageSource gunshot(Entity attacker) {
        return new DamageSource(
            attacker.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(GUNSHOT),
            attacker);
    }
}
