package com.tweeks.thief;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {
    private ModDamageTypes() {}

    public static final ResourceKey<DamageType> BLACKJACK = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(ThiefMod.MOD_ID, "blackjack"));
}
