package com.tweeks.securityguard.sound;

import com.tweeks.securityguard.SecurityGuardMod;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModSounds {
    private ModSounds() {}

    public static DeferredHolder<SoundEvent, SoundEvent> AMBIENT;
    public static DeferredHolder<SoundEvent, SoundEvent> HURT;
    public static DeferredHolder<SoundEvent, SoundEvent> DEATH;

    public static void register(DeferredRegister<SoundEvent> registry) {
        AMBIENT = registry.register("guard_ambient", soundEvent("guard_ambient"));
        HURT    = registry.register("guard_hurt",    soundEvent("guard_hurt"));
        DEATH   = registry.register("guard_death",   soundEvent("guard_death"));
    }

    private static Supplier<SoundEvent> soundEvent(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(SecurityGuardMod.MOD_ID, name);
        return () -> SoundEvent.createVariableRangeEvent(id);
    }
}
