package com.tweeks.craftee.data;

import com.tweeks.craftee.CrafteeMod;
import com.tweeks.craftee.Registration;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(output, CrafteeMod.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + CrafteeMod.MOD_ID, "Craftee");
        add(Registration.CRAFTEE_HELMET.get(),     "Craftee Helmet");
        add(Registration.CRAFTEE_CHESTPLATE.get(), "Craftee Chestplate");
        add(Registration.CRAFTEE_LEGGINGS.get(),   "Craftee Leggings");
        add(Registration.CRAFTEE_BOOTS.get(),      "Craftee Boots");
        add(Registration.CRAFTEE_UPGRADE_SMITHING_TEMPLATE.get(), "Craftee Upgrade Smithing Template");

        // Smithing template tooltip lines — keys come from the
        // Component.translatable(...) calls in Registration.java's
        // SmithingTemplateItem constructor. The 7-arg constructor in
        // 26.1.2 takes 4 Components (no upgrade-title slot) — the smithing
        // UI's title slot falls back to the item's own display name.
        add("item.craftee.smithing_template.craftee_upgrade.applies_to",                    "Diamond Armor");
        add("item.craftee.smithing_template.craftee_upgrade.ingredients",                   "Netherite Ingot");
        add("item.craftee.smithing_template.craftee_upgrade.base_slot_description",         "Add diamond armor");
        add("item.craftee.smithing_template.craftee_upgrade.additions_slot_description",    "Add netherite ingot");
    }
}
