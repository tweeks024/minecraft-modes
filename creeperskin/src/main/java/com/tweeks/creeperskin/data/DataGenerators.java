package com.tweeks.creeperskin.data;

import com.tweeks.creeperskin.CreeperSkinMod;
import net.minecraft.data.DataGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = CreeperSkinMod.MOD_ID)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherDataClient(GatherDataEvent.Client event) {
        DataGenerator gen = event.getGenerator();
        gen.addProvider(true, new ModLanguageProvider(gen.getPackOutput()));
    }
}
