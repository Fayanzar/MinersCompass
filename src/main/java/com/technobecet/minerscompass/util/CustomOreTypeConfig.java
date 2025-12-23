package com.technobecet.minerscompass.util;

import com.technobecet.minerscompass.MinersCompassMod;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;

public class CustomOreTypeConfig {
    
    public static class CustomOreDefinition {
        public final String name;
        public final Formatting color;
        public final List<String> blockIds;
        
        public CustomOreDefinition(String name, Formatting color, List<String> blockIds) {
            this.name = name;
            this.color = color;
            this.blockIds = blockIds;
        }
    }
    
    public static List<CustomOreDefinition> parseCustomOreTypes() {
        List<CustomOreDefinition> customOres = new ArrayList<>();
        
        // Safe getter for custom ore types
        List<String> customOreTypesList = MinersCompassMod.config.customOreTypes;
        if (customOreTypesList == null || customOreTypesList.isEmpty()) {
            return customOres;
        }
        
        for (String configEntry : customOreTypesList) {
            try {
                // Skip comment lines
                if (configEntry.trim().startsWith("//") || configEntry.trim().isEmpty()) {
                    continue;
                }
                
                CustomOreDefinition definition = parseCustomOreEntry(configEntry);
                if (definition != null) {
                    customOres.add(definition);
                    MinersCompassMod.LOGGER.info("Registered custom ore type: {} with {} blocks", 
                        definition.name, definition.blockIds.size());
                }
            } catch (Exception e) {
                MinersCompassMod.LOGGER.warn("Failed to parse custom ore config entry: {}", configEntry, e);
            }
        }
        
        return customOres;
    }
    
    private static CustomOreDefinition parseCustomOreEntry(String configEntry) {
        // Format: "oreName:COLOR:block1,block2,block3"
        // Example: "vibranium:LIGHT_PURPLE:vibranium_ore,vibranium_deepslate_ore"
        
        String[] parts = configEntry.split(":", 3);
        if (parts.length != 3) {
            MinersCompassMod.LOGGER.warn("Invalid custom ore config format: {}. Expected format: 'name:COLOR:block1,block2'", configEntry);
            return null;
        }
        
        String oreName = parts[0].trim();
        String colorName = parts[1].trim();
        String blockList = parts[2].trim();
        
        // Parse color
        Formatting color = parseColor(colorName);
        if (color == null) {
            MinersCompassMod.LOGGER.warn("Invalid color '{}' for ore '{}'. Using default gray.", colorName, oreName);
            color = Formatting.GRAY;
        }
        
        // Parse block IDs
        List<String> blockIds = Arrays.asList(blockList.split(","));
        blockIds = blockIds.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        
        if (blockIds.isEmpty()) {
            MinersCompassMod.LOGGER.warn("No valid block IDs found for custom ore: {}", oreName);
            return null;
        }
        
        return new CustomOreDefinition(oreName, color, blockIds);
    }
    
    private static Formatting parseColor(String colorName) {
        try {
            return Formatting.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try common color aliases
            Map<String, Formatting> colorAliases = Map.of(
                "purple", Formatting.LIGHT_PURPLE,
                "lightpurple", Formatting.LIGHT_PURPLE,
                "darkpurple", Formatting.DARK_PURPLE,
                "pink", Formatting.LIGHT_PURPLE,
                "orange", Formatting.GOLD,
                "cyan", Formatting.AQUA,
                "lightblue", Formatting.BLUE,
                "darkblue", Formatting.DARK_BLUE
            );
            
            return colorAliases.get(colorName.toLowerCase());
        }
    }
    
    public static void registerCustomOreTypes() {
        List<CustomOreDefinition> customOres = parseCustomOreTypes();
        
        for (CustomOreDefinition customOre : customOres) {
            // Create DynamicOreType for each custom ore
            DynamicOreType oreType = DynamicOreType.create(
                customOre.name.toLowerCase(),
                customOre.color,
                capitalizeFirst(customOre.name) + " Ore"
            );
            
            // Validate that the blocks exist
            List<Block> validBlocks = new ArrayList<>();
            for (String blockId : customOre.blockIds) {
                try {
                    Identifier id = Identifier.of(blockId);
                    Block block = Registries.BLOCK.get(id);
                    if (block != null && !block.equals(Registries.BLOCK.get(Identifier.of("air")))) {
                        validBlocks.add(block);
                    } else {
                        MinersCompassMod.LOGGER.warn("Block '{}' not found for custom ore '{}'", blockId, customOre.name);
                    }
                } catch (Exception e) {
                    MinersCompassMod.LOGGER.warn("Invalid block ID '{}' for custom ore '{}'", blockId, customOre.name, e);
                }
            }
            
            if (validBlocks.isEmpty()) {
                MinersCompassMod.LOGGER.warn("No valid blocks found for custom ore '{}', skipping registration", customOre.name);
                continue;
            }
            
            MinersCompassMod.LOGGER.info("Successfully registered custom ore '{}' with {} valid blocks", 
                customOre.name, validBlocks.size());
        }
    }
    
    public static Set<Block> getForceIncludedBlocks() {
        Set<Block> forceIncluded = new HashSet<>();
        
        // Safe getter for force include blocks
        List<String> forceIncludeBlocksList = MinersCompassMod.config.forceIncludeBlocks;
        if (forceIncludeBlocksList == null || forceIncludeBlocksList.isEmpty()) {
            return forceIncluded;
        }
        
        for (String blockId : forceIncludeBlocksList) {
            try {
                // Skip comment lines
                if (blockId.trim().startsWith("//") || blockId.trim().isEmpty()) {
                    continue;
                }
                
                Identifier id = Identifier.of(blockId.trim());
                Block block = Registries.BLOCK.get(id);
                if (block != null && !block.equals(Registries.BLOCK.get(Identifier.of("air")))) {
                    forceIncluded.add(block);
                    MinersCompassMod.LOGGER.info("Force-included block: {}", blockId);
                } else {
                    MinersCompassMod.LOGGER.warn("Force-include block '{}' not found", blockId);
                }
            } catch (Exception e) {
                MinersCompassMod.LOGGER.warn("Invalid force-include block ID: {}", blockId, e);
            }
        }
        
        return forceIncluded;
    }
    
    public static boolean isCustomOreBlock(Block block) {
        List<CustomOreDefinition> customOres = parseCustomOreTypes();
        String blockId = Registries.BLOCK.getId(block).toString();
        
        for (CustomOreDefinition customOre : customOres) {
            if (customOre.blockIds.contains(blockId)) {
                return true;
            }
        }
        
        return false;
    }
    
    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}