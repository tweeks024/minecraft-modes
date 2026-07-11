package com.tweeks.starwars;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(Registries.SOUND_EVENT, StarWarsMod.MOD_ID);

    public static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, name)));
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> BLASTER_FIRE = register("blaster_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> SABER_IGNITE = register("saber_ignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> SABER_CLASH = register("saber_clash");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORCE_CAST = register("force_cast");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORCE_LIGHTNING_SOUND = register("force_lightning");

    // More sound events land here in later tasks via register("name").

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
