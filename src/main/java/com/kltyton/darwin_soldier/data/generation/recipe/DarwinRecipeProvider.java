package com.kltyton.darwin_soldier.data.generation.recipe;

import com.kltyton.darwin_soldier.Darwin_soldier;
import com.kltyton.darwin_soldier.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import java.util.concurrent.CompletableFuture;

public final class DarwinRecipeProvider extends RecipeProvider {
    public DarwinRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.DARWIN_SERUM.get())
                .requires(Items.DIAMOND)
                .requires(Items.GOLDEN_APPLE)
                .requires(Items.TOTEM_OF_UNDYING)
                .requires(DataComponentIngredient.of(false, DataComponents.POTION_CONTENTS,
                        new PotionContents(Potions.STRONG_STRENGTH), Items.POTION))
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(output, ResourceLocation.fromNamespaceAndPath(Darwin_soldier.MODID, "darwin_serum"));
    }
}
