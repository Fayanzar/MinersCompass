package com.technobecet.minerscompass;

import com.technobecet.minerscompass.config.ModConfig;
import com.technobecet.minerscompass.item.ModItems;
import com.technobecet.minerscompass.networking.NetworkHandler;
import com.technobecet.minerscompass.networking.OreSelectionSyncPacket;
import com.technobecet.minerscompass.util.CustomOreTypeConfig;
import com.technobecet.minerscompass.util.DynamicOreDetection;
import com.technobecet.minerscompass.util.OreTypeManager;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinersCompassMod implements ModInitializer {
    public static final String MOD_ID = "miners-compass";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModConfig config;

    @Override
    public void onInitialize() {
        AutoConfig.register(ModConfig.class, JanksonConfigSerializer::new);

        PayloadTypeRegistry.playS2C().register(OreSelectionSyncPacket.ID, OreSelectionSyncPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(OreSelectionSyncPacket.ID, OreSelectionSyncPacket.CODEC);

        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        
        ModItems.registerModItems();
        NetworkHandler.registerServerPackets();
        
        if (config.enableAutoDiscovery) {
            LOGGER.info("Dynamic ore detection system will initialize when world loads");
            // Note: Actual initialization happens in OreTypeManager.initialize() when first accessed
        }
    }
}
