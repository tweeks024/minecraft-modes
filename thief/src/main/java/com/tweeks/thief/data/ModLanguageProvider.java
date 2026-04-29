package com.tweeks.thief.data;

import com.tweeks.thief.Registration;
import com.tweeks.thief.ThiefMod;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(output, ThiefMod.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + ThiefMod.MOD_ID, "Thief");
        add(Registration.BLACKJACK.get(), "Blackjack");
        add(Registration.THIEF_SPAWN_EGG.get(), "Thief Spawn Egg");
        add(Registration.THIEF.get(), "Thief");
        add("death.attack.blackjack", "%1$s was sapped by %2$s");
        add("message.thief.no_hideout_location", "No suitable hideout location nearby.");
    }
}
