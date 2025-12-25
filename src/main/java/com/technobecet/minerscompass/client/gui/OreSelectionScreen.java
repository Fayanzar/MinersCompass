package com.technobecet.minerscompass.client.gui;

import com.technobecet.minerscompass.MinersCompassMod;
import com.technobecet.minerscompass.networking.OreSelectionSyncPacket;
import com.technobecet.minerscompass.util.DynamicOreType;
import com.technobecet.minerscompass.util.OreTypeManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

import static com.technobecet.minerscompass.item.custom.OreCompass.SELECTED_ORES_TYPES_KEY;

public class OreSelectionScreen extends Screen {
    private final ItemStack compassStack;
    private final PlayerEntity user;
    private final Item item;
    private final Map<DynamicOreType, ButtonWidget> oreButtons = new HashMap<>();
    private final Set<DynamicOreType> selectedOreTypes = new HashSet<>();
    private ButtonWidget clearAllButton;

    public OreSelectionScreen(Item item, PlayerEntity user, ItemStack compassStack) {
        super(Text.translatable("screen.miners-compass.ore_selection"));
        this.compassStack = compassStack;
        this.user = user;
        this.item = item;
        loadSelectedOreTypes();
    }

    private void loadSelectedOreTypes() {
        var data = compassStack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return;
        var nbt = data.copyNbt();
        if (nbt == null) return;

        for (String key : nbt.getKeys()) {
            if (key.startsWith(SELECTED_ORES_TYPES_KEY)) {
                String oreTypeId = nbt.getString(key);
                for (DynamicOreType type : DynamicOreType.getAllTypes()) {
                    if (type.getId().equals(oreTypeId)) {
                        selectedOreTypes.add(type);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        int buttonHeight = 20;
        int buttonsPerRow = 2;
        int spacing = 10;

        var client = this.client;
        ButtonElementListWidget buttonWidget = new ButtonElementListWidget(client, width, height / 2, 50, buttonHeight + spacing);

        addDrawableChild(buttonWidget);

        // Ensure ore discovery is up to date when GUI opens
        OreTypeManager.initialize();
        List<DynamicOreType> availableOreTypes = OreTypeManager.getAvailableOreTypes();

        List<ButtonWidget> buttonRow = new ArrayList<>();

        for (DynamicOreType oreType : availableOreTypes) {
            ButtonWidget button = ButtonWidget.builder(
                    getOreTypeButtonText(oreType),
                    btn -> toggleOreType(oreType)
            ).build();
            oreButtons.put(oreType, button);

            buttonRow.add(button);
            if (buttonRow.size() == buttonsPerRow) {
                var buttonElement = new ButtonElementListWidget.ButtonElement(buttonRow);
                buttonWidget.addEntry(buttonElement);

                buttonRow = new ArrayList<>();
            }
        }

        // Clear All button
        clearAllButton = ButtonWidget.builder(
            Text.translatable("screen.miners-compass.ore_selection.clear_all"),
            btn -> clearAll()
        ).dimensions(width / 2 - 155, height - 40, 100, 20).build();
        addDrawableChild(clearAllButton);

        // Done button
        ButtonWidget doneButton = ButtonWidget.builder(
                Text.translatable("screen.miners-compass.ore_selection.done"),
                btn -> done()
        ).dimensions(width / 2 + 55, height - 40, 100, 20).build();
        addDrawableChild(doneButton);

        updateButtonStates();
    }

    private Text getOreTypeButtonText(DynamicOreType oreType) {
        int variantCount = OreTypeManager.getVariantCount(oreType);
        boolean isSelected = selectedOreTypes.contains(oreType);
        
        String buttonText = oreType.getDisplayName();
        if (variantCount > 1) {
            buttonText += " (" + variantCount + ")";
        }
        
        if (isSelected) {
            buttonText = "✓ " + buttonText;
        }
        
        return Text.literal(buttonText).formatted(isSelected ? oreType.getColor() : Formatting.GRAY);
    }

    private void toggleOreType(DynamicOreType oreType) {
        if (selectedOreTypes.contains(oreType)) {
            selectedOreTypes.remove(oreType);
        } else {
            if (selectedOreTypes.size() < MinersCompassMod.config.maxBlocks) {
                selectedOreTypes.add(oreType);
            }
        }
        updateButtonStates();
    }

    private void clearAll() {
        selectedOreTypes.clear();
        updateButtonStates();
    }

    private void done() {
        saveSelectedOreTypes();
        
        // Send packet to sync ore selection to server
        MinersCompassMod.LOGGER.info("Sending ore selection sync packet with ore types: {}", selectedOreTypes);
        OreSelectionSyncPacket packet = OreSelectionSyncPacket.create(selectedOreTypes);
        ClientPlayNetworking.send(packet);

        if (selectedOreTypes.isEmpty()) {
            user.sendMessage(Text.translatable("item.miners-compass.ore_compass.no_ore_types"), true);
            close();
            return;
        }

        user.getItemCooldownManager().set(item, 100);

        close();
    }

    private void updateButtonStates() {
        for (var entry : oreButtons.entrySet()) {
            var button = entry.getValue();
            var oreType = entry.getKey();
            button.setMessage(getOreTypeButtonText(oreType));

            // Disable button if max selection reached and not currently selected
            button.active = selectedOreTypes.size() < MinersCompassMod.config.maxBlocks || selectedOreTypes.contains(oreType);
        }
        
        clearAllButton.active = !selectedOreTypes.isEmpty();
    }

    private void saveSelectedOreTypes() {
        var data = compassStack.get(DataComponentTypes.CUSTOM_DATA);
        var nbt = new NbtCompound();
        if (data != null) nbt = data.copyNbt();
        
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
        for (DynamicOreType oreType : selectedOreTypes) {
            nbt.putString(SELECTED_ORES_TYPES_KEY + index, oreType.getId());
            index++;
        }

        var component = NbtComponent.of(nbt);
        compassStack.set(DataComponentTypes.CUSTOM_DATA, component);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        
        // Title
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
        
        // Selection info
        String selectionInfo = selectedOreTypes.size() + "/" + MinersCompassMod.config.maxBlocks + " ore types selected";
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(selectionInfo), width / 2, height - 65, 0xAAAAAA);
        
        // Instructions
        String instructions = "Click ore types to toggle selection";
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(instructions), width / 2, height - 55, 0x888888);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}