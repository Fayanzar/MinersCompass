package com.technobecet.minerscompass.datagen;

import com.technobecet.minerscompass.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class ModRecipeGenerator extends FabricRecipeProvider {
    public ModRecipeGenerator(FabricDataOutput output,
                              CompletableFuture<RegistryWrapper.WrapperLookup> registryLookupFuture) {
        super(output, registryLookupFuture);
    }

    @Override
    public void generate(RecipeExporter exporter) {
        ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, ModItems.ORE_COMPASS)
                .pattern("GRE")
                .pattern("NCN")
                .pattern("DBD")
                .input('G', Items.GOLD_BLOCK)
                .input('R', Items.REDSTONE_BLOCK)
                .input('E', Items.EMERALD_BLOCK)
                .input('N', Items.NETHERITE_SCRAP)
                .input('C', Items.COMPASS)
                .input('D', Items.DIAMOND)
                .input('B', Items.DIAMOND_BLOCK)
                .criterion(hasItem(Items.GOLD_BLOCK), conditionsFromItem(Items.GOLD_BLOCK))
                .criterion(hasItem(Items.REDSTONE_BLOCK), conditionsFromItem(Items.REDSTONE_BLOCK))
                .criterion(hasItem(Items.EMERALD_BLOCK), conditionsFromItem(Items.EMERALD_BLOCK))
                .criterion(hasItem(Items.NETHERITE_SCRAP), conditionsFromItem(Items.NETHERITE_SCRAP))
                .criterion(hasItem(Items.RECOVERY_COMPASS), conditionsFromItem(Items.RECOVERY_COMPASS))
                .criterion(hasItem(Items.DIAMOND), conditionsFromItem(Items.DIAMOND))
                .criterion(hasItem(Items.DIAMOND_BLOCK), conditionsFromItem(Items.DIAMOND_BLOCK))
                .offerTo(exporter, Identifier.of(getRecipeName(ModItems.ORE_COMPASS)));
    }
}
