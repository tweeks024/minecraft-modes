package com.tweeks.securityguard;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SecurityGuardMod.MOD_ID)
public class SecurityGuardMod {
    public static final String MOD_ID = "securityguard";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SecurityGuardMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Security Guard mod loading");
        Registration.register(modEventBus);
    }
}
