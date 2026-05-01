package com.tweeks.wildwest.data;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.WildWestMod;
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

    private static final ResourceKey<BiomeModifier> ADD_BANDITS = ResourceKey.create(
        NeoForgeRegistries.Keys.BIOME_MODIFIERS,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "add_bandits"));

    private static final ResourceKey<BiomeModifier> ADD_BANDIT_LEADERS = ResourceKey.create(
        NeoForgeRegistries.Keys.BIOME_MODIFIERS,
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "add_bandit_leaders"));

    public static void bootstrap(BootstrapContext<BiomeModifier> ctx) {
        var biomes = ctx.lookup(Registries.BIOME);

        HolderSet<Biome> targetBiomes = HolderSet.direct(
            biomes.getOrThrow(Biomes.PLAINS),
            biomes.getOrThrow(Biomes.SAVANNA),
            biomes.getOrThrow(Biomes.SAVANNA_PLATEAU),
            biomes.getOrThrow(Biomes.DESERT));

        ctx.register(ADD_BANDITS, new BiomeModifiers.AddSpawnsBiomeModifier(
            targetBiomes,
            WeightedList.of(java.util.List.of(new Weighted<>(
                new MobSpawnSettings.SpawnerData(ModEntities.BANDIT.get(), 1, 3), 5)))));

        ctx.register(ADD_BANDIT_LEADERS, new BiomeModifiers.AddSpawnsBiomeModifier(
            targetBiomes,
            WeightedList.of(java.util.List.of(new Weighted<>(
                new MobSpawnSettings.SpawnerData(ModEntities.BANDIT_LEADER.get(), 1, 1), 1)))));
    }
}
