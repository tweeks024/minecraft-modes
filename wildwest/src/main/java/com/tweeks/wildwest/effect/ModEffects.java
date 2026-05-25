package com.tweeks.wildwest.effect;

import com.tweeks.wildwest.WildWestMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    private ModEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, WildWestMod.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> FESTERING_WOUND =
        EFFECTS.register("festering_wound", FesteringWoundEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> ZOMBIFIED =
        EFFECTS.register("zombified", ZombifiedEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> CURING_SHAKE =
        EFFECTS.register("curing_shake", CuringShakeEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> ANOMALY_BLEED =
        EFFECTS.register("anomaly_bleed", AnomalyBleedEffect::new);

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}
