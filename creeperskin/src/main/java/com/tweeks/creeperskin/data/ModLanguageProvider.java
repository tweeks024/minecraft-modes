package com.tweeks.creeperskin.data;

import com.tweeks.creeperskin.CreeperSkinMod;
import com.tweeks.creeperskin.Registration;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(output, CreeperSkinMod.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + CreeperSkinMod.MOD_ID, "Creeper Skin");
        add(Registration.CREEPER_HELMET.get(),     "Creeper Helmet");
        add(Registration.CREEPER_CHESTPLATE.get(), "Creeper Chestplate");
        add(Registration.CREEPER_LEGGINGS.get(),   "Creeper Leggings");
        add(Registration.CREEPER_BOOTS.get(),      "Creeper Boots");
    }
}
