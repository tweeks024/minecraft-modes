package com.tweeks.wildwest.data;

import com.tweeks.wildwest.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        net.minecraft.core.HolderGetter<Item> itemLookup = this.registries.lookupOrThrow(Registries.ITEM);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.PISTOL.get())
            .pattern("I  ")
            .pattern("IW ")
            .pattern(" W ")
            .define('I', Items.IRON_INGOT)
            .define('W', Items.OAK_PLANKS)
            .unlockedBy("has_iron", this.has(Items.IRON_INGOT))
            .save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.RIFLE.get())
            .pattern("III")
            .pattern(" WI")
            .pattern(" W ")
            .define('I', Items.IRON_INGOT)
            .define('W', Items.OAK_PLANKS)
            .unlockedBy("has_iron", this.has(Items.IRON_INGOT))
            .save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.BILLY_CLUB.get())
            .pattern(" I ")
            .pattern(" W ")
            .pattern(" W ")
            .define('I', Items.IRON_INGOT)
            .define('W', Items.OAK_PLANKS)
            .unlockedBy("has_iron", this.has(Items.IRON_INGOT))
            .save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.BANDIT_KNIFE.get())
            .pattern(" I")
            .pattern("L ")
            .define('I', Items.IRON_INGOT)
            .define('L', Items.LEATHER)
            .unlockedBy("has_iron", this.has(Items.IRON_INGOT))
            .save(this.output);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ModRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Wild West Recipes";
        }
    }
}
