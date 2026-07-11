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

        add(com.tweeks.starwars.Registration.BLASTER_PISTOL.get(), "Blaster Pistol");
        add(com.tweeks.starwars.Registration.BLASTER_RIFLE.get(), "Blaster Rifle");
        add(com.tweeks.starwars.Registration.LIGHTSABER.get(), "Lightsaber");
        add(com.tweeks.starwars.Registration.STORMTROOPER_SPAWN_EGG.get(), "Stormtrooper Spawn Egg");
        add(com.tweeks.starwars.Registration.BATTLE_DROID_SPAWN_EGG.get(), "Battle Droid Spawn Egg");
        add(com.tweeks.starwars.Registration.JEDI_KNIGHT_SPAWN_EGG.get(), "Jedi Knight Spawn Egg");
        add(com.tweeks.starwars.ModEntities.STORMTROOPER.get(), "Stormtrooper");
        add(com.tweeks.starwars.ModEntities.BATTLE_DROID.get(), "Battle Droid");
        add(com.tweeks.starwars.ModEntities.JEDI_KNIGHT.get(), "Jedi Knight");
        add("subtitle.starwars.blaster_fire", "Blaster fires");
        add("subtitle.starwars.saber_ignite", "Lightsaber ignites");
        add("subtitle.starwars.saber_clash", "Lightsabers clash");

        add("death.attack.starwars.blaster_bolt", "%1$s was vaporized");
        add("death.attack.starwars.blaster_bolt.player", "%1$s was vaporized by %2$s");
        add("death.attack.starwars.blaster_bolt.item", "%1$s was vaporized by %2$s using %3$s");
        add("death.attack.starwars.lightsaber", "%1$s was cut down by %2$s");
        add("death.attack.starwars.force_lightning", "%1$s was electrocuted by %2$s");
    }
}
