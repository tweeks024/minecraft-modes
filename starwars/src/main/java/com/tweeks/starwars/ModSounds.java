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

    // Sound events land here in later tasks via register("name").

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
