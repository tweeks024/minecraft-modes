package com.tweeks.starwars.data;

import com.tweeks.starwars.ModEntities;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModBiomeModifierProvider {
    private ModBiomeModifierProvider() {}

    private static final ResourceKey<BiomeModifier> ADD_STORMTROOPERS = ResourceKey.create(
        NeoForgeRegistries.Keys.BIOME_MODIFIERS,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "add_stormtroopers"));

    private static final ResourceKey<BiomeModifier> ADD_BATTLE_DROIDS = ResourceKey.create(
        NeoForgeRegistries.Keys.BIOME_MODIFIERS,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "add_battle_droids"));

    public static void bootstrap(BootstrapContext<BiomeModifier> ctx) {
        var biomes = ctx.lookup(Registries.BIOME);

        HolderSet<Biome> trooperBiomes = HolderSet.direct(
            biomes.getOrThrow(Biomes.PLAINS),
            biomes.getOrThrow(Biomes.DESERT),
            biomes.getOrThrow(Biomes.SAVANNA),
            biomes.getOrThrow(Biomes.BADLANDS));

        ctx.register(ADD_STORMTROOPERS, new BiomeModifiers.AddSpawnsBiomeModifier(
            trooperBiomes,
            WeightedList.of(java.util.List.of(new Weighted<>(
                new MobSpawnSettings.SpawnerData(ModEntities.STORMTROOPER.get(), 2, 4), 8)))));

        HolderSet<Biome> droidBiomes = HolderSet.direct(
            biomes.getOrThrow(Biomes.DESERT),
            biomes.getOrThrow(Biomes.BADLANDS));

        ctx.register(ADD_BATTLE_DROIDS, new BiomeModifiers.AddSpawnsBiomeModifier(
            droidBiomes,
            WeightedList.of(java.util.List.of(new Weighted<>(
                new MobSpawnSettings.SpawnerData(ModEntities.BATTLE_DROID.get(), 3, 5), 6)))));
    }
}
