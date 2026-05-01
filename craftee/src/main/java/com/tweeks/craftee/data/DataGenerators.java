package com.tweeks.craftee.data;

import com.tweeks.craftee.CrafteeMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = CrafteeMod.MOD_ID)
public final class DataGenerators {
    private DataGenerators() {}

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        DataGenerator gen = event.getGenerator();
        gen.addProvider(true, new ModLanguageProvider(gen.getPackOutput()));
    }

    @SubscribeEvent
    public static void gatherDataServer(GatherDataEvent.Server event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        gen.addProvider(true, new ModRecipeProvider.Runner(output, lookup));
    }
}
