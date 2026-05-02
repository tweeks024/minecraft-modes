package com.tweeks.wildwest.effect;

import com.tweeks.wildwest.WildWestMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    private ModEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, WildWestMod.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> FESTERING_WOUND =
        EFFECTS.register("festering_wound",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x6B8E23) {});

    public static final DeferredHolder<MobEffect, MobEffect> ZOMBIFIED =
        EFFECTS.register("zombified",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x4A7C2E) {});

    public static final DeferredHolder<MobEffect, MobEffect> CURING_SHAKE =
        EFFECTS.register("curing_shake",
            () -> new MobEffect(MobEffectCategory.NEUTRAL, 0xFFD700) {});

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}
