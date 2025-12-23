package com.technobecet.minerscompass.item;

import com.technobecet.minerscompass.MinersCompassMod;
import com.technobecet.minerscompass.item.custom.OreCompass;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item ORE_COMPASS = registerItem("ore_compass", new OreCompass(new Item.Settings()));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(MinersCompassMod.MOD_ID, name), item);
    }

    private static void itemGroupTools(FabricItemGroupEntries entries) {
        entries.add(ORE_COMPASS);
    }

    public static void registerModItems() {
        MinersCompassMod.LOGGER.info("Registering Mod Items for " + MinersCompassMod.MOD_ID);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(ModItems::itemGroupTools);
    }
}
