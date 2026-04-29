package com.tweeks.securitycore;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SecurityCoreMod.MOD_ID)
public class SecurityCoreMod {
    public static final String MOD_ID = "securitycore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SecurityCoreMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Security Core loading — provides shared interfaces for the Security Pack series");
    }
}
