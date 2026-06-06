package com.tweeks.wildwest.data;

import com.tweeks.wildwest.WildWestDamageTypes;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypeProvider {
    private ModDamageTypeProvider() {}

    public static void bootstrap(BootstrapContext<DamageType> ctx) {
        ctx.register(WildWestDamageTypes.GUNSHOT,
            new DamageType("wildwest.gunshot", 0.1f));
        ctx.register(WildWestDamageTypes.CLUB,
            new DamageType("wildwest.club", 0.1f));
        ctx.register(WildWestDamageTypes.KNIFE,
            new DamageType("wildwest.knife", 0.1f));
        ctx.register(WildWestDamageTypes.METEOR,
            new DamageType("wildwest.meteor", 0.1f));
        ctx.register(WildWestDamageTypes.CANNONBALL,
            new DamageType("wildwest.cannonball", 0.1f));
        ctx.register(WildWestDamageTypes.PISTON_PUNCH,
            new DamageType("wildwest.piston_punch", 0.1f));
        ctx.register(WildWestDamageTypes.INFINITY_POWER,
            new DamageType("wildwest.infinity_power", 0.1f));
        ctx.register(WildWestDamageTypes.INFINITY_SOUL,
            new DamageType("wildwest.infinity_soul", 0.1f));
    }
}
