package com.tweeks.creeperskin;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(CreeperSkinMod.MOD_ID)
public class CreeperSkinMod {
    public static final String MOD_ID = "creeperskin";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreeperSkinMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Creeper Skin mod loading");
        Registration.register(modEventBus);
        NeoForge.EVENT_BUS.register(SetBonusHandler.class);
    }
}
