package com.tweeks.securityguard.data;

import com.tweeks.securityguard.Registration;
import com.tweeks.securityguard.SecurityGuardMod;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(output, SecurityGuardMod.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + SecurityGuardMod.MOD_ID, "Security Guard");
        add(Registration.GUARD_HELMET.get(), "Guard Helmet");
        add(Registration.GUARD_SPAWN_EGG.get(), "Security Guard Spawn Egg");
        add(Registration.SECURITY_GUARD.get(), "Security Guard");
    }
}
