package com.technobecet.minerscompass.util;

import com.technobecet.minerscompass.MinersCompassMod;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DynamicOreDetection {
    
    private static Set<Block> discoveredOres = null;
    
    public static Set<Block> getDiscoveredOres() {
        if (discoveredOres == null) {
            discoveredOres = discoverOres();
            MinersCompassMod.LOGGER.info("Discovered {} ore blocks across all loaded mods", discoveredOres.size());
        }
        return discoveredOres;
    }
    
    public static void refreshOreList() {
        discoveredOres = null;
        getDiscoveredOres();
    }
    
    private static Set<Block> discoverOres() {
        Set<Block> ores = new HashSet<>();
        
        MinersCompassMod.LOGGER.info("Starting block registry scan...");
        MinersCompassMod.LOGGER.info("Total blocks in registry: {}", Registries.BLOCK.size());
        
        // Add force-included blocks first
        Set<Block> forceIncluded = CustomOreTypeConfig.getForceIncludedBlocks();
        ores.addAll(forceIncluded);
        MinersCompassMod.LOGGER.info("Added {} force-included blocks", forceIncluded.size());
        
        int totalBlocks = 0;
        int checkedBlocks = 0;
        Set<String> foundMods = new HashSet<>();
        
        for (Block block : Registries.BLOCK) {
            totalBlocks++;
            String blockId = Registries.BLOCK.getId(block).toString();
            String namespace = Registries.BLOCK.getId(block).getNamespace();
            foundMods.add(namespace);
            
            if (isOreBlock(block)) {
                ores.add(block);
                checkedBlocks++;
                MinersCompassMod.LOGGER.info("✓ Discovered ore: {}", blockId);
                
                // Also log why it was detected
                if (CustomOreTypeConfig.isCustomOreBlock(block)) {
                    MinersCompassMod.LOGGER.info("  → Detected as custom ore type");
                } else {
                    MinersCompassMod.LOGGER.info("  → Detected via keyword matching");
                }
            }
        }
        
        MinersCompassMod.LOGGER.info("Block scan complete:");
        MinersCompassMod.LOGGER.info("  Total blocks scanned: {}", totalBlocks);
        MinersCompassMod.LOGGER.info("  Mods found: {}", foundMods.size());
        MinersCompassMod.LOGGER.info("  Ore blocks discovered: {} ({} automatic + {} force-included)", 
            ores.size(), checkedBlocks, forceIncluded.size());
        
        // Log some example mod namespaces found
        MinersCompassMod.LOGGER.info("Example mods found: {}", 
            foundMods.stream().limit(10).toArray());
        
        return ores;
    }
    
    private static boolean isOreBlock(Block block) {
        Identifier blockId = Registries.BLOCK.getId(block);
        String id = blockId.toString().toLowerCase();
        
        // Check if this specific block is excluded
        if (isBlockExcluded(id)) {
            return false;
        }
        
        // Check if it's a custom ore type first
        if (CustomOreTypeConfig.isCustomOreBlock(block)) {
            return true;
        }
        
        if (isExcludedMod(blockId.getNamespace())) {
            return false;
        }
        
        // Use whitelist approach - check against known ore patterns
        Optional<String> oreTypeOpt = com.technobecet.minerscompass.util.OrePatterns.identifyOre(id);
        if (oreTypeOpt.isPresent()) {
            // Check if this ore type is excluded
            if (isOreTypeExcluded(oreTypeOpt.get())) {
                return false;
            }
            
            // Verify it has ore-like properties
            boolean hasProperHardness = hasProperHardness(block);
            boolean isToolRequired = block.getDefaultState().isToolRequired();
            return hasProperHardness && isToolRequired;
        }
        
        return false;
    }
    

    
    private static boolean hasProperHardness(Block block) {
        float hardness = block.getHardness();
        return hardness >= MinersCompassMod.config.minimumHardness && hardness < 50.0f;
    }
    
    private static boolean isExcludedMod(String namespace) {
        List<String> excludedMods = MinersCompassMod.config.excludedMods;
        return excludedMods != null && excludedMods.contains(namespace);
    }
    
    private static boolean isOreTypeExcluded(String oreType) {
        List<String> excludedOreTypes = MinersCompassMod.config.excludedOreTypes;
        if (excludedOreTypes == null) return false;
        
        for (String excluded : excludedOreTypes) {
            // Skip comment lines
            if (excluded.startsWith("//")) continue;
            
            if (excluded.trim().equalsIgnoreCase(oreType)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean isBlockExcluded(String blockId) {
        List<String> excludedBlocks = MinersCompassMod.config.excludedBlocks;
        if (excludedBlocks == null) return false;
        
        for (String excluded : excludedBlocks) {
            // Skip comment lines
            if (excluded.startsWith("//")) continue;
            
            if (excluded.trim().equalsIgnoreCase(blockId)) {
                return true;
            }
        }
        return false;
    }
    

    
    public static boolean isValidOre(Block block) {
        return getDiscoveredOres().contains(block);
    }
    
    public static String getBlockId(Block block) {
        return Registries.BLOCK.getId(block).toString();
    }
}