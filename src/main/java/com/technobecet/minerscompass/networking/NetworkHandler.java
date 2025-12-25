package com.technobecet.minerscompass.networking;

import com.technobecet.minerscompass.MinersCompassMod;
import com.technobecet.minerscompass.item.ModItems;
import com.technobecet.minerscompass.item.custom.OreCompass;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;

import static com.technobecet.minerscompass.item.custom.OreCompass.SELECTED_ORES_TYPES_KEY;

public class NetworkHandler {
    
    public static void registerServerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(OreSelectionSyncPacket.ID, (packet, context) -> {
            var player = context.player();
            // Handle the packet on the server thread
            player.server.execute(() -> {
                MinersCompassMod.LOGGER.info("Received ore selection sync from player: {}", player.getName().getString());
                MinersCompassMod.LOGGER.info("Selected ore types: {}", packet.selectedOreTypeIds());
                
                // Find the compass in player's inventory
                ItemStack compassStack = null;
                if (player.getMainHandStack().getItem() == ModItems.ORE_COMPASS) {
                    compassStack = player.getMainHandStack();
                } else if (player.getOffHandStack().getItem() == ModItems.ORE_COMPASS) {
                    compassStack = player.getOffHandStack();
                } else {
                    // Search in inventory
                    for (int i = 0; i < player.getInventory().size(); i++) {
                        ItemStack stack = player.getInventory().getStack(i);
                        if (stack.getItem() == ModItems.ORE_COMPASS) {
                            compassStack = stack;
                            break;
                        }
                    }
                }
                
                if (compassStack == null) {
                    MinersCompassMod.LOGGER.warn("Could not find compass in player inventory");
                    return;
                }
                
                // Update the compass NBT on server side
                var data = compassStack.get(DataComponentTypes.CUSTOM_DATA);
                NbtCompound nbt;
                if (data != null) nbt = data.copyNbt();
                else nbt = new NbtCompound();
                
                // Clear existing ore type keys
                List<String> keysToRemove = new ArrayList<>();
                for (String key : nbt.getKeys()) {
                    if (key.startsWith(SELECTED_ORES_TYPES_KEY)) {
                        keysToRemove.add(key);
                    }
                }
                keysToRemove.forEach(nbt::remove);
                
                // Save new selection
                int index = 0;
                for (String oreTypeId : packet.selectedOreTypeIds()) {
                    nbt.putString(SELECTED_ORES_TYPES_KEY + index, oreTypeId);
                    index++;
                }

                NbtComponent component = NbtComponent.of(nbt);
                compassStack.set(DataComponentTypes.CUSTOM_DATA, component);

                MinersCompassMod.LOGGER.info("Updated compass NBT with ore types: {}", packet.selectedOreTypeIds());

                var pos = OreCompass.findBlocks(compassStack, player.getServerWorld(), player, true);
                OreCompass.playSound(player.getWorld(), player, pos.isPresent(), pos);
            });
        });
    }
}