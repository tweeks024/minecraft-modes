package com.tweeks.wildwest;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(WildWestMod.MOD_ID)
public class WildWestMod {
    public static final String MOD_ID = "wildwest";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WildWestMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Wild West mod loading");
        Registration.register(modEventBus);
        ModSounds.register(modEventBus);
        ModEntities.register(modEventBus);
    }
}
