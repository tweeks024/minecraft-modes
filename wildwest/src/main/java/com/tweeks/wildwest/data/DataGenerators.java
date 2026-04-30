package com.tweeks.wildwest.data;

import com.tweeks.wildwest.WildWestMod;
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

@EventBusSubscriber(modid = WildWestMod.MOD_ID)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherDataServer(GatherDataEvent.Server event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        RegistrySetBuilder builder = new RegistrySetBuilder()
            .add(Registries.DAMAGE_TYPE, ModDamageTypeProvider::bootstrap);
        gen.addProvider(true, new DatapackBuiltinEntriesProvider(
            output, lookup, builder, Set.of(WildWestMod.MOD_ID)));

        gen.addProvider(true, new ModRecipeProvider.Runner(output, lookup));
    }
}
