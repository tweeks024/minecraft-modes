package com.tweeks.wildwest.data;

import com.tweeks.wildwest.ModEntities;
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

        // Hand weapons
        add(Registration.BILLY_CLUB.get(), "Billy Club");
        add(Registration.BANDIT_KNIFE.get(), "Bandit Knife");

        // Mob entity names
        add(ModEntities.DEPUTY.get(), "Deputy");
        add(ModEntities.SHERRIF.get(), "Sheriff");
        add(ModEntities.BANDIT.get(), "Bandit");
        add(ModEntities.BANDIT_LEADER.get(), "Bandit Leader");

        // Spawn eggs
        add(Registration.DEPUTY_SPAWN_EGG.get(), "Deputy Spawn Egg");
        add(Registration.SHERRIF_SPAWN_EGG.get(), "Sheriff Spawn Egg");
        add(Registration.BANDIT_SPAWN_EGG.get(), "Bandit Spawn Egg");
        add(Registration.BANDIT_LEADER_SPAWN_EGG.get(), "Bandit Leader Spawn Egg");

        // Death messages for new damage types (wildwest:club, wildwest:knife)
        add("death.attack.wildwest.club", "%1$s was clubbed by %2$s");
        add("death.attack.wildwest.knife", "%1$s was knifed by %2$s");
    }
}
