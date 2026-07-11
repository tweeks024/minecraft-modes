package com.tweeks.starwars;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(StarWarsMod.MOD_ID)
public class StarWarsMod {
    public static final String MOD_ID = "starwars";
    public static final Logger LOGGER = LogUtils.getLogger();

    public StarWarsMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Star Wars mod loading");
        // Entity types register before items so SpawnEggItem can resolve ModEntities.*.get().
        ModEntities.register(modEventBus);
        Registration.register(modEventBus);
        ModSounds.register(modEventBus);
        com.tweeks.starwars.faction.ModAttachments.register(modEventBus);
    }
}
