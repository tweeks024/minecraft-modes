package com.tweeks.wildwest.data;

import com.tweeks.wildwest.Registration;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(output, WildWestMod.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + WildWestMod.MOD_ID, "Wild West");
        add(Registration.PISTOL.get(), "Pistol");
        add(Registration.RIFLE.get(), "Rifle");

        add("death.attack.wildwest.gunshot",
            "%1$s was shot");
        add("death.attack.wildwest.gunshot.player",
            "%1$s was shot by %2$s");
        add("death.attack.wildwest.gunshot.item",
            "%1$s was shot by %2$s using %3$s");

        add("subtitle.wildwest.pistol_fire", "Pistol fires");
        add("subtitle.wildwest.rifle_fire",  "Rifle fires");
        add("subtitle.wildwest.bolt_cycle",  "Bolt cycles");
    }
}
