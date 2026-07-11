package com.tweeks.starwars.data;

import com.tweeks.starwars.StarWarsDamageTypes;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypeProvider {
    private ModDamageTypeProvider() {}

    public static void bootstrap(BootstrapContext<DamageType> ctx) {
        ctx.register(StarWarsDamageTypes.BLASTER_BOLT,
            new DamageType("starwars.blaster_bolt", 0.1f));
        ctx.register(StarWarsDamageTypes.LIGHTSABER,
            new DamageType("starwars.lightsaber", 0.1f));
        ctx.register(StarWarsDamageTypes.FORCE_LIGHTNING,
            new DamageType("starwars.force_lightning", 0.1f));
    }
}
