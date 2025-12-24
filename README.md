# The Ultimate Mining Companion! 🧭⛏️

The **Miner's Compass** is a revolutionary tool that transforms how you mine in Minecraft. Whether you're playing
vanilla or with 100+ mods, this compass intelligently detects and guides you to the ores you need most.

**Perfect for modded servers, large modpacks, and vanilla players alike!**

## 🚀 What's New in Version 2.0

### **Automatic Modded Ore Detection**

No more manual configuration! The compass **automatically detects ores from ANY mod**:

- ✅ **Create** - Zinc ore
- ✅ **Mythic Metals** - All 20+ ores (Adamantite, Mythril, Orichalcum, etc.)
- ✅ **Immersive Engineering** - Aluminum, Lead, Nickel, Silver, Uranium
- ✅ **SoulsWeapons** - Moonstone, Verglas
- ✅ **Blue Skies** - Charoite, Diopside, Pyrope, and more
- ✅ **Many more other mods** supported automatically

### **Intelligent Ore Grouping**

Smart grouping system that combines ore variants:

- **"Iron Ore"** includes both `iron_ore` and `deepslate_iron_ore`
- **"Moonstone Ore"** groups all moonstone variants
- **Proper names** - No more "Moon Ore" or "Deepslate Ore"

### **Modern GUI System**

- **Right-click** to open sleek ore selection screen
- **Visual ore list** with proper colors and names
- **Multi-select** your favorite ores
- **Search and find** exactly what you need

### **Powerful Configuration**

Complete control over ore detection:

- **Exclude entire ore types** (`"coal", "zinc"`)
- **Exclude specific blocks** (`"minecraft:coal_ore"`)
- **Add custom ores** from any mod
- **Performance tuning** for large modpacks

## 🎯 Key Features

### **Universal Mod Compatibility**

Works with **ANY modpack** out of the box:

- **Automatic detection** of 80+ common modded ores
- **Fallback patterns** for unknown mods
- **Custom ore support** for edge cases
- **Future-proof** design for new mods

### **Intelligent Navigation**

- **Dynamic targeting** - Always points to the closest ore
- **Multi-ore tracking** - Search for up to 20 ore types simultaneously
- **Chunk-based scanning** - Configurable search radius
- **Cross-dimension support** - Works in Nether, End, and modded dimensions

### **Advanced Filtering**

- **Hardness filtering** - Excludes decorative blocks
- **Mod exclusions** - Ignore problematic mods
- **Smart grouping** - Related ores grouped together
- **Custom categories** - Create your own ore types

### **Performance Optimized**

- **Efficient scanning** - Minimal impact on game performance
- **Configurable radius** - Balance between coverage and performance
- **Smart caching** - Remembers previous searches
- **Async processing** - Doesn't block gameplay

## 🎮 How to Use

### **Getting Started**

1. **Craft the compass** with the recipe below
2. **Right-click** to open the ore selection GUI
3. **Select ores** you want to find
4. **Click "Done"** and start mining!

### **Advanced Usage**

- **Shift-right-click** on ore blocks to quickly add/remove them
- **Shift-right-click air** to clear all selections
- **Configure** in `.minecraft/config/miners-compass.json5`

### **Crafting Recipe**

![Crafting Recipe](https://cdn.modrinth.com/data/tPs9k0db/images/911f5ac3881396fe92675c434f9a6ad87eaaa68e.png)

## ⚙️ Complete Configuration Guide

### **Full Configuration Example**

```json5
{
  // Show debug lines leading to the ore block
  "showDirectionLines": true,
  // How many chunks around player to search (0-100)
  // Higher values = larger area but more performance impact
  "chunkRadius": 2,
  // Maximum ore types selectable at once (0-20)
  // Balance between flexibility and performance
  "maxBlocks": 3,
  // Enable tracking blocks of different ore types simultaneously
  // Moderately impacts performance
  "trackMultipleBlocks": false,
  // Enable automatic detection of modded ores
  // Set false to only use custom definitions
  "enableAutoDiscovery": true,
  // Group ore variants together (coal_ore + deepslate_coal_ore = "Coal Ore")
  // Set false to treat each block separately
  "groupSimilarOres": true,
  // Minimum block hardness for ore detection (0.0-50.0)
  // Higher values filter out soft decorative blocks
  "minimumHardness": 1.0,
  // Additional keywords to help detect missed ores
  // Add terms commonly used in mod ore names
  "additionalOreKeywords": [
    "gem",
    "crystal",
    "ingot",
    "core",
    "debris"
  ],
  // Exclude entire mods from ore detection
  // Add mod IDs that cause false positives
  "excludedMods": [
    "decorative_blocks",
    "chisel"
  ],
  // Add custom ore types not detected automatically
  // Format: "oreName:COLOR:blockId1,blockId2"
  "customOreTypes": [
    "exquisite:BLACK:arphex:exquisite_ore"
  ],
  // Force include specific blocks as ores
  // For blocks that should be detected but aren't
  "forceIncludeBlocks": [],
  // Exclude entire ore types (removes all variants)
  // Hide ore categories you don't want
  "excludedOreTypes": [],
  // Exclude specific ore blocks while keeping the type
  // Fine-grained control over individual blocks
  "excludedBlocks": []
}
```

### **Configuration Options Explained**

#### **Basic Settings**

- **`showDirectionLines`** - Show the lines indicating the exact direction to the 
    ore block
- **`chunkRadius`** - Search area around player (larger = more lag)
- **`maxBlocks`** - How many ore types you can select simultaneously
- **`trackMultipleBlocks`** - Track blocks from different ore types simultaneously
- **`enableAutoDiscovery`** - Automatically find modded ores vs manual only
- **`groupSimilarOres`** - Combine variants vs separate entries
- **`minimumHardness`** - Filter out soft decorative blocks

#### **Detection Enhancement**

- **`additionalOreKeywords`** - Help detect ores with unusual names
- **`customOreTypes`** - Add ores that aren't detected automatically
- **`forceIncludeBlocks`** - Force specific blocks to be treated as ores

#### **Exclusion Controls**

- **`excludedMods`** - Ignore entire mods that cause problems
- **`excludedOreTypes`** - Hide entire ore categories (all coal, all zinc, etc.)
- **`excludedBlocks`** - Hide specific blocks while keeping the ore type

### **Common Configuration Examples**

#### **Performance-Optimized**

```json5
{
  "showDirectionLines": true,
  "chunkRadius": 2,
  "maxBlocks": 3,
  "trackMultipleBlocks": false,
  "minimumHardness": 2.0,
  "excludedOreTypes": [
    "coal",
    "iron",
    "copper"
  ]
}
```

#### **Rare Ores Only**

```json5
{
  "excludedOreTypes": [
    "coal",
    "iron",
    "copper",
    "zinc",
    "tin",
    "lead",
    "aluminum"
  ]
}
```

#### **Vanilla-Only Setup**

```json5
{
  "excludedMods": [
    "create",
    "mythicmetals",
    "immersiveengineering",
    "soulsweapons"
  ]
}
```

#### **No Deepslate Variants**

```json5
{
  "excludedBlocks": [
    "minecraft:deepslate_coal_ore",
    "minecraft:deepslate_iron_ore",
    "minecraft:deepslate_gold_ore",
    "create:deepslate_zinc_ore"
  ]
}
```

### **Available Colors for Custom Ores**

`WHITE`, `YELLOW`, `GOLD`, `RED`, `DARK_RED`, `GREEN`, `DARK_GREEN`, `AQUA`, `DARK_AQUA`, `BLUE`, `DARK_BLUE`,
`LIGHT_PURPLE`, `DARK_PURPLE`, `GRAY`, `DARK_GRAY`, `BLACK`

## 🔄 Migration from v1.x

**v1.x users**: Your old config will work, but consider upgrading:

### **Old Config (v1.x)**

```json5
{
  "chunkRadius": 2,
  "maxBlocks": 3
}
```

### **New Config (v2.0)**

```json5
{
  // Basic settings (compatible)
  "chunkRadius": 2,
  "maxBlocks": 3,
  // New features
  "showDirectionLines": true,
  "trackMultipleBlocks": false,
  "enableAutoDiscovery": true,
  "excludedOreTypes": [
    "coal",
    "iron"
  ],
  "customOreTypes": [
    "mythril:AQUA:modname:mythril_ore"
  ]
}
```

**Migration is automatic** - your old config continues working while new features become available!

# Q&A

### Forge/Backport?

For forge
support, [Sinytra Connector](https://modrinth.com/mod/connector), [Connector Extras](https://modrinth.com/mod/connector-extras), [Forgified Fabric API](https://modrinth.com/mod/forgified-fabric-api)
combination tested and works as expected.

### Y-Axis Direction?

For those interested in knowing the Y-axis direction, I highly recommend
using [Compass3D](https://modrinth.com/mod/compass3d), developed by [AdamRaichu](https://modrinth.com/user/AdamRaichu).
My gratitude goes out to him for integrating support for this feature into the mod.
