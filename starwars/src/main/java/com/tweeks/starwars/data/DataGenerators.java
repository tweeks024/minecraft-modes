package com.tweeks.starwars.data;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = StarWarsMod.MOD_ID)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherDataServer(GatherDataEvent.Server event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        RegistrySetBuilder builder = new RegistrySetBuilder()
            .add(Registries.DAMAGE_TYPE, ModDamageTypeProvider::bootstrap)
            .add(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                 ModBiomeModifierProvider::bootstrap);
        gen.addProvider(true, new DatapackBuiltinEntriesProvider(
            output, lookup, builder, Set.of(StarWarsMod.MOD_ID)));

        gen.addProvider(true, new ModDamageTypeTagsProvider(output, lookup));
        gen.addProvider(true, new ModRecipeProvider.Runner(output, lookup));

        gen.addProvider(true, new net.minecraft.data.loot.LootTableProvider(
            output,
            Set.of(),
            java.util.List.of(new net.minecraft.data.loot.LootTableProvider.SubProviderEntry(
                ModEntityLootProvider::new,
                net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY)),
            lookup));
    }

    @SubscribeEvent
    public static void gatherDataClient(GatherDataEvent.Client event) {
        DataGenerator gen = event.getGenerator();
        gen.addProvider(true, new ModLanguageProvider(gen.getPackOutput()));
    }
}
