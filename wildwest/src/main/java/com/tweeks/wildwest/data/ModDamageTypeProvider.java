package com.tweeks.wildwest.data;

import com.tweeks.wildwest.WildWestDamageTypes;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypeProvider {
    private ModDamageTypeProvider() {}

    public static void bootstrap(BootstrapContext<DamageType> ctx) {
        ctx.register(WildWestDamageTypes.GUNSHOT,
            new DamageType("wildwest.gunshot", 0.1f));
    }
}
