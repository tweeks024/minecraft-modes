package com.tweeks.thief;

import com.mojang.logging.LogUtils;
import com.tweeks.thief.entity.ThiefEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

@Mod(ThiefMod.MOD_ID)
public class ThiefMod {
    public static final String MOD_ID = "thief";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ThiefMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Thief mod loading");
        Registration.register(modEventBus);
        modEventBus.addListener(ThiefMod::registerEntityAttributes);
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(Registration.THIEF.get(), ThiefEntity.createAttributes().build());
    }
}
