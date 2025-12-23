package com.technobecet.minerscompass.util;

import com.technobecet.minerscompass.MinersCompassMod;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> ORE_COMPASS_DETECTABLE_BLOCKS =
                createBlockTag("ore_compass_detectable_blocks");

        private static TagKey<Block> createBlockTag(String name) {
            return TagKey.of(RegistryKeys.BLOCK, Identifier.of(MinersCompassMod.MOD_ID, name));
        }

        public static TagKey<Block> createBlockTagFromId(String name) {
            return TagKey.of(RegistryKeys.BLOCK, Identifier.of(name));
        }

        private static TagKey<Block> createCommonBlockTag(String name) {
            return TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", name));
        }
    }
}
