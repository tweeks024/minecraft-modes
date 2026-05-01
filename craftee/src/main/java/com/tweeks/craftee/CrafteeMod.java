package com.tweeks.craftee;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(CrafteeMod.MOD_ID)
public class CrafteeMod {
    public static final String MOD_ID = "craftee";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CrafteeMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Craftee mod loading");
        Registration.register(modEventBus);
        NeoForge.EVENT_BUS.register(SetBonusHandler.class);
    }
}
