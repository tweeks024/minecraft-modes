package com.tweeks.starwars.data;

import com.tweeks.starwars.Registration;
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

        // Armor: 1 quartz-tier set, vanilla iron-armor shapes, quartz in place of ingots.
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.STORMTROOPER_HELMET.get())
            .pattern("QQQ")
            .pattern("Q Q")
            .define('Q', Items.QUARTZ)
            .unlockedBy("has_quartz", this.has(Items.QUARTZ))
            .save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.STORMTROOPER_CHESTPLATE.get())
            .pattern("Q Q")
            .pattern("QQQ")
            .pattern("QQQ")
            .define('Q', Items.QUARTZ)
            .unlockedBy("has_quartz", this.has(Items.QUARTZ))
            .save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.STORMTROOPER_LEGGINGS.get())
            .pattern("QQQ")
            .pattern("Q Q")
            .pattern("Q Q")
            .define('Q', Items.QUARTZ)
            .unlockedBy("has_quartz", this.has(Items.QUARTZ))
            .save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.STORMTROOPER_BOOTS.get())
            .pattern("Q Q")
            .pattern("Q Q")
            .define('Q', Items.QUARTZ)
            .unlockedBy("has_quartz", this.has(Items.QUARTZ))
            .save(this.output);

        // Blasters: iron ingots + redstone + iron nuggets (astromech "parts").
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.BLASTER_PISTOL.get())
            .pattern("II")
            .pattern("RN")
            .define('I', Items.IRON_INGOT)
            .define('R', Items.REDSTONE)
            .define('N', Items.IRON_NUGGET)
            .unlockedBy("has_redstone", this.has(Items.REDSTONE))
            .save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.BLASTER_RIFLE.get())
            .pattern("III")
            .pattern(" RN")
            .define('I', Items.IRON_INGOT)
            .define('R', Items.REDSTONE)
            .define('N', Items.IRON_NUGGET)
            .unlockedBy("has_redstone", this.has(Items.REDSTONE))
            .save(this.output);

        // Landspeeder: hull row + engine (5 iron, 1 redstone block).
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.TRANSPORTATION, Registration.LANDSPEEDER.get())
            .pattern("IRI")
            .pattern("III")
            .define('I', Items.IRON_INGOT)
            .define('R', Items.REDSTONE_BLOCK)
            .unlockedBy("has_iron", this.has(Items.IRON_INGOT))
            .save(this.output);

        // Han Solo armor: leather body + one netherite ingot per piece —
        // netherite-grade stats at netherite-anchored cost (spec §5).
        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.HAN_SOLO_HELMET.get())
            .pattern("LNL")
            .pattern("L L")
            .define('L', Items.LEATHER)
            .define('N', Items.NETHERITE_INGOT)
            .unlockedBy("has_netherite_ingot", this.has(Items.NETHERITE_INGOT))
            .save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.HAN_SOLO_CHESTPLATE.get())
            .pattern("L L")
            .pattern("LNL")
            .pattern("LLL")
            .define('L', Items.LEATHER)
            .define('N', Items.NETHERITE_INGOT)
            .unlockedBy("has_netherite_ingot", this.has(Items.NETHERITE_INGOT))
            .save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.HAN_SOLO_LEGGINGS.get())
            .pattern("LNL")
            .pattern("L L")
            .pattern("L L")
            .define('L', Items.LEATHER)
            .define('N', Items.NETHERITE_INGOT)
            .unlockedBy("has_netherite_ingot", this.has(Items.NETHERITE_INGOT))
            .save(this.output);

        ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.COMBAT, Registration.HAN_SOLO_BOOTS.get())
            .pattern("N L")
            .pattern("L L")
            .define('L', Items.LEATHER)
            .define('N', Items.NETHERITE_INGOT)
            .unlockedBy("has_netherite_ingot", this.has(Items.NETHERITE_INGOT))
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
            return "Star Wars Recipes";
        }
    }
}
