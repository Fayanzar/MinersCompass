package com.technobecet.minerscompass.util;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DynamicOreType {
    private static final Map<String, DynamicOreType> REGISTERED_TYPES = new HashMap<>();

    // Predefined types with nice colors
    public static final DynamicOreType COAL = create("coal", Formatting.DARK_GRAY, "Coal Ore");
    public static final DynamicOreType IRON = create("iron", Formatting.WHITE, "Iron Ore");
    public static final DynamicOreType GOLD = create("gold", Formatting.YELLOW, "Gold Ore");
    public static final DynamicOreType DIAMOND = create("diamond", Formatting.AQUA, "Diamond Ore");
    public static final DynamicOreType EMERALD = create("emerald", Formatting.GREEN, "Emerald Ore");
    public static final DynamicOreType COPPER = create("copper", Formatting.GOLD, "Copper Ore");
    public static final DynamicOreType LAPIS = create("lapis", Formatting.BLUE, "Lapis Lazuli Ore");
    public static final DynamicOreType REDSTONE = create("redstone", Formatting.RED, "Redstone Ore");
    public static final DynamicOreType QUARTZ = create("quartz", Formatting.WHITE, "Quartz Ore");
    public static final DynamicOreType NETHERITE = create("netherite", Formatting.DARK_PURPLE, "Ancient Debris");

    private final String keyword;
    private final String id;
    private final Formatting color;
    private final String displayName;

    private DynamicOreType(String keyword, String id, Formatting color, String displayName) {
        this.keyword = keyword;
        this.id = id;
        this.color = color;
        this.displayName = displayName;
    }

    public static DynamicOreType create(String keyword, Formatting color, String displayName) {
        String id = keyword.toLowerCase();
        DynamicOreType existing = REGISTERED_TYPES.get(id);
        if (existing != null) {
            return existing;
        }

        DynamicOreType newType = new DynamicOreType(keyword, id, color, displayName);
        REGISTERED_TYPES.put(id, newType);
        return newType;
    }

    public static DynamicOreType fromBlock(Block block) {
        Identifier blockId = Registries.BLOCK.getId(block);
        String blockIdString = blockId.toString();

        // Check custom ore types first
        DynamicOreType customType = getCustomOreType(block);
        if (customType != null) {
            return customType;
        }

        // Use whitelist approach to identify ore
        Optional<String> oreIdOpt = OrePatterns.identifyOre(blockIdString);
        if (oreIdOpt.isEmpty()) {
            return null;
        }

        String oreId = oreIdOpt.get();

        // Check if we already have this ore type
        DynamicOreType existing = REGISTERED_TYPES.get(oreId);
        if (existing != null) {
            return existing;
        }

        // Get predefined ore definition
        Optional<OrePatterns.OreDefinition> defOpt = OrePatterns.getOreDefinition(oreId);
        if (defOpt.isEmpty()) {
            return null;
        }

        OrePatterns.OreDefinition definition = defOpt.get();

        // Create new ore type with predefined name and color
        return create(oreId, definition.color, definition.displayName);
    }

    private static DynamicOreType getCustomOreType(Block block) {
        List<CustomOreTypeConfig.CustomOreDefinition> customOres = CustomOreTypeConfig.parseCustomOreTypes();
        String blockId = Registries.BLOCK.getId(block).toString();

        for (CustomOreTypeConfig.CustomOreDefinition customOre : customOres) {
            if (customOre.blockIds.contains(blockId)) {
                return create(customOre.name.toLowerCase(), customOre.color,
                        capitalizeFirst(customOre.name) + " Ore");
            }
        }

        return null;
    }

    private static String extractOreName(Identifier blockId) {
        String path = blockId.getPath().toLowerCase();
        String namespace = blockId.getNamespace();

        // Special hardcoded cases first
        Map<String, String> specialCases = new HashMap<>();
        specialCases.put("ancient_debris", "netherite");
        specialCases.put("crystalline_core", "crystalline");
        specialCases.put("tigers_eye_blackstone_ore", "tigers_eye");
        specialCases.put("tigers_eye_basalt_ore", "tigers_eye");
        specialCases.put("nether_quartz_ore", "quartz");

        // Butcher mod special cases (non-standard naming)
        specialCases.put("sulfurore", "sulfur");
        specialCases.put("deepslatesulfurore", "sulfur");

        String specialCase = specialCases.get(path);
        if (specialCase != null) {
            return specialCase;
        }

        // Enhanced patterns to extract ore names (order matters - most specific first)
        Pattern[] patterns = {
                // Multi-word ores (tigers_eye, midas_gold, etc.) - MUST come first
                Pattern.compile("(\\w+_\\w+)_ore$"),              // tigers_eye_ore -> tigers_eye
                Pattern.compile("(\\w+_\\w+)_blackstone_ore$"),   // tigers_eye_blackstone_ore -> tigers_eye
                Pattern.compile("(\\w+_\\w+)_basalt_ore$"),       // tigers_eye_basalt_ore -> tigers_eye
                Pattern.compile("(midas_gold)_ore$"),             // midas_gold_ore -> midas_gold

                // Location-specific patterns (more specific)
                Pattern.compile("deepslate_(\\w+)_ore$"),          // deepslate_sulfur_ore -> sulfur
                Pattern.compile("(\\w+)_ore_deepslate$"),          // sulfur_ore_deepslate -> sulfur
                Pattern.compile("nether_(\\w+)_ore$"),             // nether_zinc_ore -> zinc
                Pattern.compile("end_(\\w+)_ore$"),                // end_zinc_ore -> zinc
                Pattern.compile("end_stone_(\\w+)_ore$"),          // end_stone_zinc_ore -> zinc
                Pattern.compile("blackstone_(\\w+)_ore$"),         // blackstone_zinc_ore -> zinc
                Pattern.compile("(\\w+)_blackstone_ore$"),         // zinc_blackstone_ore -> zinc
                Pattern.compile("basalt_(\\w+)_ore$"),             // basalt_zinc_ore -> zinc
                Pattern.compile("(\\w+)_basalt_ore$"),             // zinc_basalt_ore -> zinc
                Pattern.compile("smooth_basalt_(\\w+)_ore$"),      // smooth_basalt_zinc_ore -> zinc
                Pattern.compile("tuff_(\\w+)_ore$"),               // tuff_zinc_ore -> zinc
                Pattern.compile("calcite_(\\w+)_ore$"),            // calcite_zinc_ore -> zinc

                // Location-specific patterns
                Pattern.compile("deepslate_(\\w+)_ore$"),          // deepslate_zinc_ore -> zinc
                Pattern.compile("(\\w+)_ore_deepslate$"),          // zinc_ore_deepslate -> zinc
                Pattern.compile("nether_(\\w+)_ore$"),             // nether_zinc_ore -> zinc
                Pattern.compile("end_(\\w+)_ore$"),                // end_zinc_ore -> zinc
                Pattern.compile("end_stone_(\\w+)_ore$"),          // end_stone_zinc_ore -> zinc
                Pattern.compile("blackstone_(\\w+)_ore$"),         // blackstone_zinc_ore -> zinc
                Pattern.compile("(\\w+)_blackstone_ore$"),         // zinc_blackstone_ore -> zinc
                Pattern.compile("basalt_(\\w+)_ore$"),             // basalt_zinc_ore -> zinc
                Pattern.compile("(\\w+)_basalt_ore$"),             // zinc_basalt_ore -> zinc
                Pattern.compile("smooth_basalt_(\\w+)_ore$"),      // smooth_basalt_zinc_ore -> zinc
                Pattern.compile("tuff_(\\w+)_ore$"),               // tuff_zinc_ore -> zinc
                Pattern.compile("calcite_(\\w+)_ore$"),            // calcite_zinc_ore -> zinc
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(path);
            if (matcher.find()) {
                String oreName = matcher.group(1);

                // Clean up and validate
                oreName = cleanOreName(oreName);
                if (isValidOreName(oreName)) {
                    return oreName;
                }
            }
        }

        // Fallback: if path contains "ore" anywhere, try to extract nearby words
        if (path.contains("ore")) {
            String[] parts = path.split("_");
            // Look for the most significant part (usually the material name)
            for (String part : parts) {
                if (!isIgnoredWord(part) && isValidOreName(part) && !part.equals("ore")) {
                    return part;
                }
            }

            // If we still haven't found anything, try harder with adjacent words to "ore"
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("ore") || parts[i].equals("ores")) {
                    // Check the word before "ore"
                    if (i > 0 && isValidOreName(parts[i - 1]) && !isIgnoredWord(parts[i - 1])) {
                        return parts[i - 1];
                    }
                    // Check the word after "ore" 
                    if (i < parts.length - 1 && isValidOreName(parts[i + 1]) && !isIgnoredWord(parts[i + 1])) {
                        return parts[i + 1];
                    }
                }
            }
        }

        return null;
    }

    private static String cleanOreName(String oreName) {
        // Handle multi-word ore names
        if (oreName.contains("_")) {
            // Special cases for multi-word names
            return switch (oreName) {
                case "tigers_eye" -> "tigers_eye";
                case "midas_gold" -> "midas_gold";
                default -> {
                    // For other cases, take the most descriptive part
                    String[] parts = oreName.split("_");
                    yield parts[parts.length - 1];
                }
            };
        }
        return oreName;
    }

    private static boolean isIgnoredWord(String word) {
        Set<String> ignoredWords = Set.of(
                "ore", "ores", "deepslate", "nether", "end", "stone", "raw",
                "blackstone", "basalt", "smooth", "tuff", "calcite", "block"
        );
        return ignoredWords.contains(word);
    }

    private static boolean isValidOreName(String name) {
        // Filter out common non-ore words
        Set<String> excludedWords = Set.of(
                "stone", "cobble", "smooth", "polished", "cracked", "mossy",
                "stairs", "slab", "wall", "fence", "door", "trapdoor", "button",
                "pressure", "plate", "block", "bricks", "brick", "tile", "tiles",
                "decorative", "decoration", "fake", "dummy", "creative",
                "deepslatesulfurore", "sulfurore", // Don't accept these concatenated names as-is
                // Chipped mod decorative blocks
                "bordered", "cutting", "glow", "lode", "loreful", "marble", "solar",
                "tiny", "layed", "layered", "engraved", "chiseled",
                // Other common non-ore words
                "ingot", "nugget", "dust", "powder", "gear", "rod", "tool",
                "armor", "helmet", "chestplate", "leggings", "boots", "sword",
                "pickaxe", "axe", "shovel", "hoe", "crystallized", "leaves"
        );

        return name.length() >= 3 && !excludedWords.contains(name);
    }

    private static DynamicOreType createDynamicOreType(String oreName) {
        // Generate appropriate color based on ore name
        Formatting color = generateColorForOre(oreName);

        // Create display name
        String displayName = capitalizeFirst(oreName) + " Ore";

        return create(oreName, color, displayName);
    }

    private static Formatting generateColorForOre(String oreName) {
        // Assign colors based on ore name characteristics
        Map<String, Formatting> colorMap = new HashMap<>();

        // Vanilla-style ores
        colorMap.put("zinc", Formatting.GRAY);
        colorMap.put("tin", Formatting.WHITE);
        colorMap.put("lead", Formatting.DARK_GRAY);
        colorMap.put("silver", Formatting.WHITE);
        colorMap.put("aluminum", Formatting.LIGHT_PURPLE);
        colorMap.put("aluminium", Formatting.LIGHT_PURPLE);
        colorMap.put("copper", Formatting.GOLD);

        // Mythic Metals ores (from your datapack)
        colorMap.put("adamantite", Formatting.DARK_PURPLE);
        colorMap.put("aquarium", Formatting.AQUA);
        colorMap.put("banglum", Formatting.YELLOW);
        colorMap.put("carmot", Formatting.RED);
        colorMap.put("kyber", Formatting.GREEN);
        colorMap.put("manganese", Formatting.DARK_GRAY);
        colorMap.put("morkite", Formatting.BLUE);
        colorMap.put("midas_gold", Formatting.GOLD);
        colorMap.put("mythril", Formatting.LIGHT_PURPLE);
        colorMap.put("orichalcum", Formatting.YELLOW);
        colorMap.put("osmium", Formatting.BLUE);
        colorMap.put("palladium", Formatting.WHITE);
        colorMap.put("platinum", Formatting.WHITE);
        colorMap.put("prometheum", Formatting.RED);
        colorMap.put("runite", Formatting.DARK_GREEN);
        colorMap.put("starrite", Formatting.YELLOW);
        colorMap.put("stormyx", Formatting.DARK_BLUE);
        colorMap.put("unobtainium", Formatting.DARK_PURPLE);

        // Soulsweapons ores
        colorMap.put("moonstone", Formatting.BLUE);
        colorMap.put("verglas", Formatting.AQUA);

        // Other mod ores from your datapack
        colorMap.put("tigers_eye", Formatting.YELLOW);
        colorMap.put("sulphur", Formatting.YELLOW);
        colorMap.put("sulfur", Formatting.YELLOW);
        colorMap.put("salt", Formatting.WHITE);
        colorMap.put("onyx", Formatting.DARK_GRAY);
        colorMap.put("gleaming", Formatting.YELLOW);
        colorMap.put("crystalline", Formatting.LIGHT_PURPLE);

        // Gems and special materials
        colorMap.put("amber", Formatting.YELLOW);
        colorMap.put("ender", Formatting.DARK_GREEN);
        colorMap.put("ruby", Formatting.RED);
        colorMap.put("sapphire", Formatting.BLUE);
        colorMap.put("topaz", Formatting.YELLOW);
        colorMap.put("amethyst", Formatting.LIGHT_PURPLE);
        colorMap.put("quartz", Formatting.WHITE);
        colorMap.put("netherite", Formatting.DARK_PURPLE);

        Formatting predefinedColor = colorMap.get(oreName.toLowerCase());
        if (predefinedColor != null) {
            return predefinedColor;
        }

        // Generate color based on hash of name for consistency
        Formatting[] availableColors = {
                Formatting.AQUA, Formatting.BLUE, Formatting.DARK_AQUA,
                Formatting.DARK_BLUE, Formatting.DARK_GREEN, Formatting.DARK_PURPLE,
                Formatting.GREEN, Formatting.LIGHT_PURPLE, Formatting.YELLOW,
                Formatting.GOLD, Formatting.RED, Formatting.GRAY
        };

        int colorIndex = Math.abs(oreName.hashCode()) % availableColors.length;
        return availableColors[colorIndex];
    }

    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static Collection<DynamicOreType> getAllTypes() {
        return REGISTERED_TYPES.values();
    }

    public static List<DynamicOreType> getKnownTypes() {
        return REGISTERED_TYPES.values().stream()
                .sorted(Comparator.comparing(DynamicOreType::getDisplayName))
                .toList();
    }

    public String getKeyword() {
        return keyword;
    }

    public String getId() {
        return id;
    }

    public Formatting getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Text getFormattedName() {
        return Text.literal(displayName).formatted(color);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DynamicOreType that = (DynamicOreType) obj;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return displayName + " (" + id + ")";
    }
}