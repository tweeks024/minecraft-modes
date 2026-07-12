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
        add(com.tweeks.starwars.Registration.DARTH_VADER_SPAWN_EGG.get(), "Darth Vader Spawn Egg");
        add(com.tweeks.starwars.Registration.LUKE_SKYWALKER_SPAWN_EGG.get(), "Luke Skywalker Spawn Egg");
        add(com.tweeks.starwars.Registration.OBI_WAN_SPAWN_EGG.get(), "Obi-Wan Kenobi Spawn Egg");
        add(com.tweeks.starwars.Registration.ASTROMECH_SPAWN_EGG.get(), "Astromech Droid Spawn Egg");
        add(com.tweeks.starwars.Registration.BOBA_FETT_SPAWN_EGG.get(), "Boba Fett Spawn Egg");
        add(com.tweeks.starwars.Registration.HAN_SOLO_SPAWN_EGG.get(), "Han Solo Spawn Egg");
        add(com.tweeks.starwars.Registration.PRINCESS_LEIA_SPAWN_EGG.get(), "Princess Leia Spawn Egg");
        add(com.tweeks.starwars.ModEntities.STORMTROOPER.get(), "Stormtrooper");
        add(com.tweeks.starwars.ModEntities.BATTLE_DROID.get(), "Battle Droid");
        add(com.tweeks.starwars.ModEntities.JEDI_KNIGHT.get(), "Jedi Knight");
        add(com.tweeks.starwars.ModEntities.DARTH_VADER.get(), "Darth Vader");
        add(com.tweeks.starwars.ModEntities.LUKE_SKYWALKER.get(), "Luke Skywalker");
        add(com.tweeks.starwars.ModEntities.OBI_WAN.get(), "Obi-Wan Kenobi");
        add(com.tweeks.starwars.ModEntities.BOBA_FETT.get(), "Boba Fett");
        add(com.tweeks.starwars.ModEntities.ASTROMECH.get(), "Astromech Droid");
        add(com.tweeks.starwars.ModEntities.HAN_SOLO.get(), "Han Solo");
        add(com.tweeks.starwars.ModEntities.PRINCESS_LEIA.get(), "Princess Leia");
        add(com.tweeks.starwars.Registration.STORMTROOPER_HELMET.get(), "Stormtrooper Helmet");
        add(com.tweeks.starwars.Registration.STORMTROOPER_CHESTPLATE.get(), "Stormtrooper Chestplate");
        add(com.tweeks.starwars.Registration.STORMTROOPER_LEGGINGS.get(), "Stormtrooper Leggings");
        add(com.tweeks.starwars.Registration.STORMTROOPER_BOOTS.get(), "Stormtrooper Boots");
        add(com.tweeks.starwars.Registration.HOLOCRON.get(), "Kyber Holocron");
        add("subtitle.starwars.blaster_fire", "Blaster fires");
        add("subtitle.starwars.saber_ignite", "Lightsaber ignites");
        add("subtitle.starwars.saber_clash", "Lightsabers clash");
        add("subtitle.starwars.force_cast", "Force power cast");
        add("subtitle.starwars.force_lightning", "Force lightning crackles");

        add("force_power.starwars.push", "Force Push");
        add("force_power.starwars.pull", "Force Pull");
        add("force_power.starwars.leap", "Force Leap");
        add("force_power.starwars.mind_trick", "Mind Trick");
        add("force_power.starwars.lightning", "Force Lightning");

        add("key.starwars.open_force_picker", "Open Force Picker");
        add("screen.starwars.force_radial", "Force Powers");
        add("screen.starwars.force_radial.prompt", "Select Power");

        add("death.attack.starwars.blaster_bolt", "%1$s was vaporized by %2$s");
        add("death.attack.starwars.blaster_bolt.player", "%1$s was vaporized by %2$s");
        add("death.attack.starwars.blaster_bolt.item", "%1$s was vaporized by %2$s using %3$s");
        add("death.attack.starwars.lightsaber", "%1$s was cut down by %2$s");
        add("death.attack.starwars.force_lightning", "%1$s was electrocuted by %2$s");
    }
}
