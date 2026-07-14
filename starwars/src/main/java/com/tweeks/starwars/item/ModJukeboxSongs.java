package com.tweeks.starwars.item;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;

/**
 * Jukebox songs (datapack registry). The cantina tune is an original
 * synthesized swing number — the famous one is copyrighted, ours just
 * shares the mood.
 */
public final class ModJukeboxSongs {
    public static final ResourceKey<JukeboxSong> CANTINA_BAND = ResourceKey.create(
        Registries.JUKEBOX_SONG, Identifier.fromNamespaceAndPath("starwars", "cantina_band"));

    private ModJukeboxSongs() {
    }

    public static void bootstrap(BootstrapContext<JukeboxSong> ctx) {
        HolderGetter<SoundEvent> sounds = ctx.lookup(Registries.SOUND_EVENT);
        ctx.register(CANTINA_BAND, new JukeboxSong(
            sounds.getOrThrow(ResourceKey.create(Registries.SOUND_EVENT,
                Identifier.fromNamespaceAndPath("starwars", "cantina_band"))),
            Component.translatable("jukebox_song.starwars.cantina_band"),
            51.0F,
            14));
    }
}
