package com.technobecet.minerscompass.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Config(name = "miners_compass")
public class ModConfig implements ConfigData {
    // Show direction lines leading to the ore block
    @ConfigEntry.Gui.Tooltip(count = 1)
    public boolean showDirectionLines = true;

    // How many chunks around the player to search for ores (0-100)
    // Higher values = larger search area but may impact performance
    @ConfigEntry.Gui.Tooltip(count = 1)
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int chunkRadius = 2;
    
    // Maximum number of ore types that can be selected at once (0-20)
    // Lower values = better performance, higher values = more flexibility
    @ConfigEntry.Gui.Tooltip(count = 1)
    @ConfigEntry.BoundedDiscrete(min = 0, max = 20)
    public int maxBlocks = 5;
    
    // Enable automatic detection of modded ores
    // Set to false to only use manually defined ore types
    @ConfigEntry.Gui.Tooltip(count = 1)
    public boolean enableAutoDiscovery = true;
    
    // Group similar ore variants together (e.g., coal_ore + deepslate_coal_ore = "Coal Ore")
    // Set to false to treat each ore block as separate
    @ConfigEntry.Gui.Tooltip(count = 1)
    public boolean groupSimilarOres = true;
    
    // Minimum block hardness required for ore detection (0.0-50.0)
    // Higher values filter out softer decorative blocks
    @ConfigEntry.Gui.Tooltip(count = 1)
    @ConfigEntry.BoundedDiscrete(min = 0, max = 10)
    public float minimumHardness = 1.0f;
    
    // Additional keywords to help detect ores that might be missed
    // Add terms commonly used in mod ore names
    @ConfigEntry.Gui.Tooltip(count = 1)
    public List<String> additionalOreKeywords = new ArrayList<>();
    
    // Exclude entire mods from ore detection
    // Add mod IDs to ignore all blocks from those mods
    @ConfigEntry.Gui.Tooltip(count = 1)
    public List<String> excludedMods = new ArrayList<>();
    
    // Add custom ore types that aren't detected automatically
    // Format: "oreName:COLOR:blockId1,blockId2,blockId3"
    // Available colors: WHITE, YELLOW, GOLD, RED, DARK_RED, GREEN, DARK_GREEN,
    //                   AQUA, DARK_AQUA, BLUE, DARK_BLUE, LIGHT_PURPLE, DARK_PURPLE,
    //                   GRAY, DARK_GRAY, BLACK
    @ConfigEntry.Gui.Tooltip(count = 3)
    public List<String> customOreTypes = new ArrayList<>();
    
    // Force include specific blocks that should be treated as ores
    // Use this for blocks that aren't detected automatically
    @ConfigEntry.Gui.Tooltip(count = 2)
    public List<String> forceIncludeBlocks = new ArrayList<>();
    
    // Exclude entire ore types from detection (removes all variants)
    // Use ore type names to hide specific ore categories completely
    // Available types: coal, iron, gold, diamond, emerald, copper, lapis, redstone, quartz,
    // netherite, zinc, adamantite, aquarium, banglum, carmot, kyber, manganese, morkite,
    // midas_gold, mythril, orichalcum, osmium, palladium, platinum, prometheum, runite,
    // silver, starrite, stormyx, tin, unobtainium, moonstone, blue_skies_moonstone, verglas,
    // aquite, charoite, diopside, pyrope, falsite, horizonite, ventium, sulfur, salt, onyx,
    // gleaming, tigers_eye, crystalline, aluminum, lead, nickel, uranium, and others
    @ConfigEntry.Gui.Tooltip(count = 4)
    public List<String> excludedOreTypes = new ArrayList<>();
    
    // Exclude specific ore blocks while keeping the ore type
    // Use this for fine-grained control (e.g., exclude surface ores but keep deepslate)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public List<String> excludedBlocks = new ArrayList<>();
    
    @Override
    public void validatePostLoad() throws ValidationException {
        // Ensure lists are mutable and initialized with defaults
        if (additionalOreKeywords == null) {
            additionalOreKeywords = new ArrayList<>();
        }
        if (additionalOreKeywords.isEmpty()) {
            additionalOreKeywords.addAll(Arrays.asList("gem", "crystal", "ingot", "core", "debris"));
        }
        
        if (excludedMods == null) {
            excludedMods = new ArrayList<>();
        }
        if (excludedMods.isEmpty()) {
            excludedMods.addAll(Arrays.asList("decorative_blocks", "chisel"));
        }
        
        if (customOreTypes == null) {
            customOreTypes = new ArrayList<>();
        }
        
        if (forceIncludeBlocks == null) {
            forceIncludeBlocks = new ArrayList<>();
        }
        
        if (excludedOreTypes == null) {
            excludedOreTypes = new ArrayList<>();
        }
        
        if (excludedBlocks == null) {
            excludedBlocks = new ArrayList<>();
        }

        
        // Ensure sane values
        if (chunkRadius < 0) chunkRadius = 2;
        if (maxBlocks < 1) maxBlocks = 5;
        if (minimumHardness < 0) minimumHardness = 1.0f;
    }
}
