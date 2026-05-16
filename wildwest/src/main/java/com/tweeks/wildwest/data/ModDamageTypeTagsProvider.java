package com.tweeks.wildwest.data;

import com.tweeks.wildwest.WildWestDamageTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.tags.DamageTypeTags;

import java.util.concurrent.CompletableFuture;

public class ModDamageTypeTagsProvider extends DamageTypeTagsProvider {

    public ModDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(DamageTypeTags.IS_PROJECTILE).add(WildWestDamageTypes.GUNSHOT);
        tag(DamageTypeTags.IS_FIRE).add(WildWestDamageTypes.METEOR);
        tag(DamageTypeTags.IS_EXPLOSION).add(WildWestDamageTypes.CANNONBALL);
    }
}
