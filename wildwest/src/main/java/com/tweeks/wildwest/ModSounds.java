package com.tweeks.wildwest;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(Registries.SOUND_EVENT, WildWestMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_FIRE = register("pistol_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFLE_FIRE  = register("rifle_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_CYCLE  = register("bolt_cycle");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
