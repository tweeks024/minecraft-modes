package com.tweeks.securityguard;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SecurityGuardMod.MOD_ID)
public class SecurityGuardMod {
    public static final String MOD_ID = "securityguard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public SecurityGuardMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Security Guard mod loading");
    }
}
