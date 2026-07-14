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
        add(com.tweeks.starwars.ModEntities.LANDSPEEDER.get(), "Landspeeder");
        add(com.tweeks.starwars.Registration.STORMTROOPER_HELMET.get(), "Stormtrooper Helmet");
        add(com.tweeks.starwars.Registration.STORMTROOPER_CHESTPLATE.get(), "Stormtrooper Chestplate");
        add(com.tweeks.starwars.Registration.STORMTROOPER_LEGGINGS.get(), "Stormtrooper Leggings");
        add(com.tweeks.starwars.Registration.STORMTROOPER_BOOTS.get(), "Stormtrooper Boots");
        add(com.tweeks.starwars.Registration.HOLOCRON.get(), "Kyber Holocron");
        add(com.tweeks.starwars.Registration.LANDSPEEDER.get(), "Landspeeder");
        add(com.tweeks.starwars.Registration.HAN_SOLO_HELMET.get(), "Han Solo's Helmet");
        add(com.tweeks.starwars.Registration.HAN_SOLO_CHESTPLATE.get(), "Han Solo's Chestplate");
        add(com.tweeks.starwars.Registration.HAN_SOLO_LEGGINGS.get(), "Han Solo's Leggings");
        add(com.tweeks.starwars.Registration.HAN_SOLO_BOOTS.get(), "Han Solo's Boots");
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

        add(com.tweeks.starwars.Registration.STAR_COMPASS.get(), "Star Compass");
        add(com.tweeks.starwars.Registration.HYPERSPACE_PORTAL.get(), "Hyperspace Portal");
        add("screen.starwars.planet_radial", "Hyperspace Navigation");
        add("screen.starwars.planet_radial.prompt", "Choose Destination");
        add("starwars.planet.tatooine", "Tatooine");
        add("starwars.planet.andor", "Andor");
        add("starwars.planet.coruscant", "Coruscant");
        add("starwars.planet.dagobah", "Dagobah");
        add("starwars.planet.hoth", "Hoth");
        add("starwars.planet.death_star", "Death Star");
        add("starwars.planet.home", "Home");
        add(com.tweeks.starwars.Registration.CANTINA_RECORD.get(), "Music Disc");
        add("jukebox_song.starwars.cantina_band", "Figrin's Fizz - Cantina Swing");
        add(com.tweeks.starwars.Registration.GALAXY_MAP.get(), "Galaxy Map");
        add(com.tweeks.starwars.Registration.BLUE_KYBER_ORE.get(), "Blue Kyber Ore");
        add(com.tweeks.starwars.Registration.GREEN_KYBER_ORE.get(), "Green Kyber Ore");
        add(com.tweeks.starwars.Registration.PURPLE_KYBER_ORE.get(), "Purple Kyber Ore");
        add(com.tweeks.starwars.Registration.KYBER_CRYSTAL.get(), "Kyber Crystal");
        add(com.tweeks.starwars.Registration.SABER_HILT.get(), "Lightsaber Hilt");
        add("starwars.kyber.color.blue", "Attunement: Blue");
        add("starwars.kyber.color.green", "Attunement: Green");
        add("starwars.kyber.color.red", "Attunement: Sith Red");
        add("starwars.kyber.color.purple", "Attunement: Purple");
        add("starwars.kyber.hint_bleed", "A dark heart could bleed it red...");
        add("starwars.kyber.resists", "The crystal resists your touch - only the dark side can bleed it");
        add("starwars.kyber.bled", "The crystal screams and bleeds Sith red");
        add("screen.starwars.galaxy_map", "Galaxy Map");
        add("screen.starwars.galaxy_map.charted", "Charted");
        add("screen.starwars.galaxy_map.uncharted", "Uncharted");
        add("screen.starwars.galaxy_map.gates", "Gates: %s");
        add("screen.starwars.galaxy_map.nearby", "Nearest gates in this world");
        add("screen.starwars.galaxy_map.no_gates", "No gates charted here yet - build one!");
        add("screen.starwars.galaxy_map.gate_line", "%s gate at %s, %s, %s (%sm away)");

        add(com.tweeks.starwars.ModEntities.JAWA.get(), "Jawa");
        add(com.tweeks.starwars.ModEntities.TUSKEN_RAIDER.get(), "Tusken Raider");
        add(com.tweeks.starwars.ModEntities.BANTHA.get(), "Bantha");
        add(com.tweeks.starwars.ModEntities.REBEL_TROOPER.get(), "Rebel Trooper");
        add(com.tweeks.starwars.ModEntities.PROBE_DROID.get(), "Imperial Probe Droid");
        add(com.tweeks.starwars.ModEntities.WAMPA.get(), "Wampa");
        add(com.tweeks.starwars.ModEntities.TAUNTAUN.get(), "Tauntaun");
        add(com.tweeks.starwars.ModEntities.SNOWTROOPER.get(), "Snowtrooper");
        add(com.tweeks.starwars.ModEntities.DRAGONSNAKE.get(), "Dragonsnake");
        add(com.tweeks.starwars.ModEntities.BOGWING.get(), "Bogwing");
        add(com.tweeks.starwars.ModEntities.YODA.get(), "Yoda");
        add(com.tweeks.starwars.ModEntities.SPEEDER_BIKE.get(), "Speeder Bike");
        add(com.tweeks.starwars.ModEntities.XWING.get(), "X-wing");
        add(com.tweeks.starwars.ModEntities.TIE_FIGHTER.get(), "TIE Fighter");
        add(com.tweeks.starwars.ModEntities.AT_AT.get(), "AT-AT Walker");
        add(com.tweeks.starwars.ModEntities.BAND_DROID.get(), "Cantina Band Droid");
        add(com.tweeks.starwars.ModEntities.CHEWBACCA.get(), "Chewbacca");
        add(com.tweeks.starwars.ModEntities.GROGU.get(), "Grogu");
        add(com.tweeks.starwars.Registration.SPEEDER_BIKE.get(), "Speeder Bike");
        add(com.tweeks.starwars.Registration.XWING.get(), "X-wing");
        add(com.tweeks.starwars.Registration.TIE_FIGHTER.get(), "TIE Fighter");
        add(com.tweeks.starwars.Registration.AT_AT_SPAWN_EGG.get(), "AT-AT Walker Spawn Egg");
        add(com.tweeks.starwars.Registration.BAND_DROID_SPAWN_EGG.get(), "Cantina Band Droid Spawn Egg");
        add(com.tweeks.starwars.Registration.JAWA_SPAWN_EGG.get(), "Jawa Spawn Egg");
        add(com.tweeks.starwars.Registration.TUSKEN_RAIDER_SPAWN_EGG.get(), "Tusken Raider Spawn Egg");
        add(com.tweeks.starwars.Registration.BANTHA_SPAWN_EGG.get(), "Bantha Spawn Egg");
        add(com.tweeks.starwars.Registration.REBEL_TROOPER_SPAWN_EGG.get(), "Rebel Trooper Spawn Egg");
        add(com.tweeks.starwars.Registration.PROBE_DROID_SPAWN_EGG.get(), "Imperial Probe Droid Spawn Egg");
        add(com.tweeks.starwars.Registration.WAMPA_SPAWN_EGG.get(), "Wampa Spawn Egg");
        add(com.tweeks.starwars.Registration.TAUNTAUN_SPAWN_EGG.get(), "Tauntaun Spawn Egg");
        add(com.tweeks.starwars.Registration.SNOWTROOPER_SPAWN_EGG.get(), "Snowtrooper Spawn Egg");
        add(com.tweeks.starwars.Registration.DRAGONSNAKE_SPAWN_EGG.get(), "Dragonsnake Spawn Egg");
        add(com.tweeks.starwars.Registration.BOGWING_SPAWN_EGG.get(), "Bogwing Spawn Egg");
        add(com.tweeks.starwars.Registration.YODA_SPAWN_EGG.get(), "Yoda Spawn Egg");
        add(com.tweeks.starwars.Registration.CHEWBACCA_SPAWN_EGG.get(), "Chewbacca Spawn Egg");
        add(com.tweeks.starwars.Registration.GROGU_SPAWN_EGG.get(), "Grogu Spawn Egg");
        add("starwars.gate.invalid", "The gate frame is incomplete - build an iron block rectangle (2x3 up to 8x8 inside)");
        add("starwars.gate.already_there", "This gate already leads to the world you are standing in");
        add("starwars.gate.locked", "Destination locked: %s");

        add("death.attack.starwars.blaster_bolt", "%1$s was vaporized by %2$s");
        add("death.attack.starwars.blaster_bolt.player", "%1$s was vaporized by %2$s");
        add("death.attack.starwars.blaster_bolt.item", "%1$s was vaporized by %2$s using %3$s");
        add("death.attack.starwars.lightsaber", "%1$s was cut down by %2$s");
        add("death.attack.starwars.force_lightning", "%1$s was electrocuted by %2$s");
    }
}
