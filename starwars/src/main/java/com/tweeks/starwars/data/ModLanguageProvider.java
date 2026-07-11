package com.tweeks.starwars.data;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(output, StarWarsMod.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + StarWarsMod.MOD_ID, "Star Wars");

        add("death.attack.starwars.blaster_bolt", "%1$s was vaporized");
        add("death.attack.starwars.blaster_bolt.player", "%1$s was vaporized by %2$s");
        add("death.attack.starwars.blaster_bolt.item", "%1$s was vaporized by %2$s using %3$s");
        add("death.attack.starwars.lightsaber", "%1$s was cut down by %2$s");
        add("death.attack.starwars.force_lightning", "%1$s was electrocuted by %2$s");
    }
}
