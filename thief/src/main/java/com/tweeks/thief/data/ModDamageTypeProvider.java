package com.tweeks.thief.data;

import com.tweeks.thief.ModDamageTypes;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypeProvider {
    private ModDamageTypeProvider() {}

    public static void bootstrap(BootstrapContext<DamageType> ctx) {
        ctx.register(ModDamageTypes.BLACKJACK,
            new DamageType("blackjack", 0.1f));
    }
}
