package com.tweeks.craftee.data;

import com.tweeks.craftee.CrafteeMod;
import com.tweeks.craftee.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        net.minecraft.core.HolderGetter<Item> itemLookup = this.registries.lookupOrThrow(Registries.ITEM);

        Ingredient template  = Ingredient.of(Registration.CRAFTEE_UPGRADE_SMITHING_TEMPLATE.get());
        Ingredient netherite = Ingredient.of(Items.NETHERITE_INGOT);

        smithingUpgrade("craftee_helmet",     template, Items.DIAMOND_HELMET,     netherite, Registration.CRAFTEE_HELMET.get());
        smithingUpgrade("craftee_chestplate", template, Items.DIAMOND_CHESTPLATE, netherite, Registration.CRAFTEE_CHESTPLATE.get());
        smithingUpgrade("craftee_leggings",   template, Items.DIAMOND_LEGGINGS,   netherite, Registration.CRAFTEE_LEGGINGS.get());
        smithingUpgrade("craftee_boots",      template, Items.DIAMOND_BOOTS,      netherite, Registration.CRAFTEE_BOOTS.get());

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.MISC, Registration.CRAFTEE_UPGRADE_SMITHING_TEMPLATE.get(), 2)
            .pattern("PPP")
            .pattern("PNP")
            .pattern("PPP")
            .define('P', Items.PAPER)
            .define('N', Items.NETHERITE_SCRAP)
            .unlockedBy("has_netherite_scrap", this.has(Items.NETHERITE_SCRAP))
            .save(this.output);
    }

    private void smithingUpgrade(String resultId,
                                 Ingredient template,
                                 Item base,
                                 Ingredient addition,
                                 Item result) {
        SmithingTransformRecipeBuilder.smithing(template, Ingredient.of(base), addition, RecipeCategory.COMBAT, result)
            .unlocks("has_netherite_ingot", this.has(Items.NETHERITE_INGOT))
            .save(this.output, CrafteeMod.MOD_ID + ":" + resultId + "_smithing");
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
            return "Craftee Recipes";
        }
    }
}
