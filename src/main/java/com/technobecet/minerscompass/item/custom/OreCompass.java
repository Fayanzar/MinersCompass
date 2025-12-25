package com.technobecet.minerscompass.item.custom;

import com.technobecet.minerscompass.MinersCompassMod;
import com.technobecet.minerscompass.client.gui.OreSelectionScreen;
import com.technobecet.minerscompass.util.DynamicOreDetection;
import com.technobecet.minerscompass.util.DynamicOreType;
import com.technobecet.minerscompass.util.OreTypeManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class OreCompass extends Item {

    public static final String SELECTED_ORES_TYPES_KEY = "SelectedOresTypes";
    public static final String TRACKED_BLOCKS_KEY = "TrackedBlocks";
    public static final String TRACKED_ORES_TYPES_KEY = "TrackedOresTypes";
    public static final String TARGET_BLOCKS_POS_KEY = "TargetBlocksPos";
    public static final String TARGET_BLOCKS_DIMENSION_KEY = "TargetBlocksDimension";
    public static final String TARGET_BLOCKS_TRACKED_KEY = "TargetBlocksTracked";

    public OreCompass(Settings settings) {
        super(settings);
    }

    @Nullable
    public static List<GlobalPos> getTrackedPos(NbtCompound nbt) {
        if (nbt == null) return null;

        boolean hasTrackedValue = hasNbtKey(nbt, TARGET_BLOCKS_TRACKED_KEY);
        if (!hasTrackedValue) return null;

        boolean tracked = nbt.getBoolean(TARGET_BLOCKS_TRACKED_KEY);

        Optional<RegistryKey<World>> worldKey;
        boolean hasPosKey = hasNbtKey(nbt, TARGET_BLOCKS_POS_KEY);
        boolean hasDimKey = hasNbtKey(nbt, TARGET_BLOCKS_DIMENSION_KEY);

        if (hasPosKey && hasDimKey && tracked && (worldKey = getTrackedDimension(nbt)).isPresent()) {
            var blockPositions = nbt.getList(TARGET_BLOCKS_POS_KEY, NbtElement.INT_ARRAY_TYPE);
            return getGlobalPos(blockPositions, worldKey);
        }
        return null;
    }

    private static List<GlobalPos> getGlobalPos(NbtList blockPositions, Optional<RegistryKey<World>> worldKey) {
        List<GlobalPos> globalPositions = new ArrayList<>();
        for (int i = 0; i < blockPositions.size(); i++) {
            var blockPos = blockPositions.getIntArray(i);
            if (blockPos.length != 3) continue;

            var newBlockPos = new BlockPos(blockPos[0], blockPos[1], blockPos[2]);
            globalPositions.add(GlobalPos.create(worldKey.get(), newBlockPos));
        }
        if (globalPositions.isEmpty()) return null;
        return globalPositions;
    }

    public static Optional<List<BlockPos>> findBlocks(ItemStack stack, World world, Entity entity, boolean force) {
        if (world.isClient) return Optional.empty();
        if (force)
            MinersCompassMod.LOGGER.info("Finding blocks for player: {} in world: {}", entity.getName().getString(), world.getRegistryKey().getValue());

        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        var nbt = new NbtCompound();
        if (data != null) nbt = data.copyNbt();
        Set<DynamicOreType> selectedOreTypes = getSelectedOreTypesFromNbt(nbt);
        if (force)
            MinersCompassMod.LOGGER.info("Selected ore types: {}", selectedOreTypes.size());
        
        if (selectedOreTypes.isEmpty()) {
            if (force)
                MinersCompassMod.LOGGER.info("No ore types selected - clearing NBT and returning empty");
            clearNbtRecords(stack);
            return Optional.empty();
        }

        List<Block> targetBlocks = OreTypeManager.getAllBlocksForOreTypes(selectedOreTypes);
        if (force)
            MinersCompassMod.LOGGER.info("Target blocks found: {}", targetBlocks.size());
        
        if (targetBlocks.isEmpty()) {
            if (force)
                MinersCompassMod.LOGGER.info("No target blocks found - clearing NBT and returning empty");
            clearNbtRecords(stack);
            return Optional.empty();
        }

        data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data != null) nbt = data.copyNbt();
        var trackedPosList = getTrackedPos(nbt);
        if (!force && trackedPosList != null) {
            boolean needsSearch = false;
            List<BlockPos> blockPosList = new ArrayList<>();
            for (var trackedPos : trackedPosList) {
                BlockState trackedBlockState = world.getBlockState(trackedPos.pos());
                if (targetBlocks.contains(trackedBlockState.getBlock()))
                    blockPosList.add(trackedPos.pos());
                else needsSearch = true;
            }
            if (!needsSearch)
                if (!blockPosList.isEmpty()) return Optional.of(blockPosList);
                else {
                    var dimKey = getTrackedDimension(nbt);
                    if (dimKey.isPresent() && !dimKey.get().toString().equals(entity.getWorld().getRegistryKey().toString()))
                        return Optional.empty();
                }
        }
        var closest = findBlocksInNearbyChunks(stack, world, entity.getBlockPos(), targetBlocks);
        
        playSoundOnStateChange(world, entity, stack, closest);
        writeNbt(world.getRegistryKey(), closest, stack);

        return closest;
    }

    private static Optional<RegistryKey<World>> getTrackedDimension(NbtCompound nbt) {
        return World.CODEC.parse(NbtOps.INSTANCE, nbt.get(TARGET_BLOCKS_DIMENSION_KEY)).result();
    }

    private static void playSoundOnStateChange(World world, Entity entity, ItemStack stack, Optional<List<BlockPos>> closest) {
        if (closest.isPresent()) {
            playSound(world, entity, true, closest);
            return;
        }
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt;
        if (data != null) nbt = data.copyNbt();
        else nbt = new NbtCompound();
        var trackedPos = getTrackedPos(nbt);
        if (trackedPos != null) {
            playSound(world, entity, false, closest);
        }
    }

    private static void writeNbt(RegistryKey<World> worldKey, Optional<List<BlockPos>> closest, ItemStack stack) {
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt;
        if (data != null) nbt = data.copyNbt();
        else return;
        if (nbt == null) return;

        NbtList targetBlockPosKeys = new NbtList();

        if (closest.isPresent()) {
            List<BlockPos> positions = closest.get();
            for (var pos : positions)
                targetBlockPosKeys.add(NbtHelper.fromBlockPos(pos));
            nbt.put(TARGET_BLOCKS_POS_KEY, targetBlockPosKeys);
            World.CODEC.encodeStart(NbtOps.INSTANCE, worldKey)
                    .resultOrPartial(MinersCompassMod.LOGGER::error)
                    .ifPresent(nbtElement -> nbt.put(TARGET_BLOCKS_DIMENSION_KEY, nbtElement));
            nbt.putBoolean(TARGET_BLOCKS_TRACKED_KEY, true);
        } else {
            targetBlockPosKeys.add(NbtHelper.fromBlockPos(BlockPos.ORIGIN));
            nbt.put(TARGET_BLOCKS_POS_KEY, targetBlockPosKeys);
            nbt.putBoolean(TARGET_BLOCKS_TRACKED_KEY, false);
            nbt.remove(TARGET_BLOCKS_DIMENSION_KEY);
        }

        var component = NbtComponent.of(nbt);
        stack.set(DataComponentTypes.CUSTOM_DATA, component);
    }

    public static void playSound(World world, Entity entity, boolean success, Optional<List<BlockPos>> closest) {
        MinersCompassMod.LOGGER.info("Closest ore blocks found: {}", closest.isPresent() ? closest.get() : "none");
        world.playSound(null, entity.getBlockPos(),
                success ? SoundEvents.ITEM_LODESTONE_COMPASS_LOCK : SoundEvents.BLOCK_FIRE_EXTINGUISH,
                SoundCategory.PLAYERS, 1f, 1f);
    }

    // Find the closest block of each tracked ore type
    private static Optional<List<BlockPos>> findBlocksInNearbyChunks(ItemStack itemStack, World world, BlockPos entPos, List<Block> targetBlocks) {
        int chunkRadius = Math.max(0, MinersCompassMod.config.chunkRadius);
        Long2ObjectOpenHashMap<Set<Pair<BlockPos, Block>>> blocks = new Long2ObjectOpenHashMap<>();
        var chunkPos = world.getChunk(entPos).getPos();

        BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
        int startY = world.getBottomY();
        int endY = world.getTopY();

        for (int x = chunkPos.x - chunkRadius; x <= chunkPos.x + chunkRadius; x++) {
            for (int z = chunkPos.z - chunkRadius; z <= chunkPos.z + chunkRadius; z++) {
                Chunk chunk = world.getChunk(x, z);
                for (int i = 0; i < 16; ++i) {
                    for (int j = startY; j < endY; ++j) {
                        for (int k = 0; k < 16; ++k) {
                            mutableBlockPos.set(x * 16 + i, j, z * 16 + k);
                            BlockState blockState = world.getBlockState(mutableBlockPos);
                            if (targetBlocks.contains(blockState.getBlock())) {
                                blocks.computeIfAbsent(chunk.getPos().toLong(), key -> new HashSet<>())
                                        .add(new Pair<>(mutableBlockPos.toImmutable(), blockState.getBlock()));
                            }
                        }
                    }
                }
            }
        }
        return getClosestBlocksPos(itemStack, blocks, entPos, world);
    }

    private static Optional<List<BlockPos>> getClosestBlocksPos(ItemStack itemStack, Long2ObjectOpenHashMap<Set<Pair<BlockPos, Block>>> blockMap, BlockPos entPos, World world) {
        List<Pair<BlockPos, Block>> closestBlocks = new ArrayList<>();
        var trackMultipleBlocks = MinersCompassMod.config.trackMultipleBlocks;
        var blockPosGrouped = blockMap.values().stream().flatMap(Set::stream)
                .collect(Collectors.groupingBy(p -> OreTypeManager.getOreTypeForBlock(p.getRight())));

        double globalClosestDistanceSq = Double.MAX_VALUE;
        BlockPos globalClosestPos = null;
        Block globalClosestBlock = null;

        for (var blocks : blockPosGrouped.values()) {
            double closestDistanceSq = Double.MAX_VALUE;
            BlockPos closestPos = null;
            Block closestBlock = null;
            for (var block : blocks) {
                double dx = block.getLeft().getX() - entPos.getX();
                double dy = block.getLeft().getY() - entPos.getY();
                double dz = block.getLeft().getZ() - entPos.getZ();
                double distanceSq = dx * dx + dy * dy + dz * dz;

                if (distanceSq < closestDistanceSq && trackMultipleBlocks) {
                    closestPos = block.getLeft();
                    closestDistanceSq = distanceSq;
                    closestBlock = world.getBlockState(closestPos).getBlock();
                }

                if (distanceSq < globalClosestDistanceSq && !trackMultipleBlocks) {
                    globalClosestPos = block.getLeft();
                    globalClosestDistanceSq = distanceSq;
                    globalClosestBlock = world.getBlockState(globalClosestPos).getBlock();
                }
            }
            if (trackMultipleBlocks) closestBlocks.add(new Pair<>(closestPos, closestBlock));
        }

        if (closestBlocks.isEmpty() && (globalClosestPos == null || globalClosestBlock == null)) return Optional.empty();

        if (!trackMultipleBlocks)
            closestBlocks = Collections.singletonList(new Pair<>(globalClosestPos, globalClosestBlock));

        var data = itemStack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt;
        if (data != null) nbt = data.copyNbt();
        else nbt = new NbtCompound();

        NbtList blockKeys = new NbtList();
        NbtList oreTypes = new NbtList();
        for (var closestBlock : closestBlocks) {
            var nbtBlockString = NbtString.of(Registries.BLOCK.getId(closestBlock.getRight()).toString());
            var oreType = OreTypeManager.getOreTypeForBlock(closestBlock.getRight());
            oreType.ifPresent(type -> oreTypes.add(NbtString.of(type.getId())));
            blockKeys.add(nbtBlockString);
        }
        nbt.put(TRACKED_BLOCKS_KEY, blockKeys);
        nbt.put(TRACKED_ORES_TYPES_KEY, oreTypes);

        NbtComponent component = NbtComponent.of(nbt);
        itemStack.set(DataComponentTypes.CUSTOM_DATA, component);

        return Optional.of(closestBlocks.stream().map(Pair::getLeft).toList());
    }

    public static Set<DynamicOreType> getSelectedOreTypesFromNbt(NbtCompound nbt) {
        if (nbt == null) return Collections.emptySet();
        
        Set<DynamicOreType> oreTypes = new HashSet<>();
        for (String key : nbt.getKeys()) {
            if (key.startsWith(SELECTED_ORES_TYPES_KEY)) {
                String oreTypeId = nbt.getString(key);
                // Find existing ore type by ID
                for (DynamicOreType type : DynamicOreType.getAllTypes()) {
                    if (type.getId().equals(oreTypeId)) {
                        oreTypes.add(type);
                        break;
                    }
                }
            }
        }
        return oreTypes;
    }

    private static void saveSelectedOreTypesToNbt(ItemStack stack, Set<DynamicOreType> oreTypes) {
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt;
        if (data != null) nbt = data.copyNbt();
        else return;
        if (nbt == null) return;

        clearOreTypeKeys(nbt);

        int index = 0;
        for (DynamicOreType oreType : oreTypes) {
            nbt.putString(SELECTED_ORES_TYPES_KEY + index, oreType.getId());
            index++;
        }

        NbtComponent component = NbtComponent.of(nbt);
        stack.set(DataComponentTypes.CUSTOM_DATA, component);
    }

    private static void clearOreTypeKeys(NbtCompound nbt) {
        List<String> keysToRemove = new ArrayList<>();
        for (String key : nbt.getKeys()) {
            if (key.startsWith(SELECTED_ORES_TYPES_KEY)) {
                keysToRemove.add(key);
            }
        }
        keysToRemove.forEach(nbt::remove);
    }

    private static boolean hasNbtKey(NbtCompound nbt, String key) {
        return nbt != null && nbt.contains(key);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        MinersCompassMod.LOGGER.info("Ore compass right-clicked by {} in world: {} (isClient: {})", 
            user.getName().getString(), world.getRegistryKey().getValue(), world.isClient);
            
        ItemStack itemStack = user.getStackInHand(hand);

        if (user.isSneaking()) {
            if (world.isClient) {
                return TypedActionResult.success(itemStack);
            }
            clearNbtRecords(itemStack);
            user.sendMessage(Text.translatable("item.miners-compass.ore_compass.cleared_all"), true);
            return TypedActionResult.success(itemStack);
        }

        if (world.isClient) {
            // Open GUI on client side
            openOreSelectionGui(this, user, itemStack);
            return TypedActionResult.success(itemStack);
        }

        return TypedActionResult.success(itemStack);
    }

    @Environment(EnvType.CLIENT)
    private void openOreSelectionGui(Item item, PlayerEntity user, ItemStack itemStack) {
        MinecraftClient.getInstance().setScreen(
            new OreSelectionScreen(item, user, itemStack)
        );
    }

    private static void clearNbtRecords(ItemStack stack) {
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt;
        if (data != null) nbt = data.copyNbt();
        else return;
        if (nbt == null) return;
        List<String> keysToRemove = new ArrayList<>();
        for (String key : nbt.getKeys()) {
            if (key.startsWith(SELECTED_ORES_TYPES_KEY) || key.equals(TARGET_BLOCKS_POS_KEY) ||
                key.equals(TARGET_BLOCKS_DIMENSION_KEY) || key.equals(TARGET_BLOCKS_TRACKED_KEY) ||
                key.equals(TRACKED_BLOCKS_KEY) || key.equals(TRACKED_ORES_TYPES_KEY)) {
                keysToRemove.add(key);
            }
        }
        keysToRemove.forEach(nbt::remove);

        NbtComponent component = NbtComponent.of(nbt);
        stack.set(DataComponentTypes.CUSTOM_DATA, component);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        ItemStack itemStack = context.getStack();
        BlockState clickedBlockState = context.getWorld().getBlockState(context.getBlockPos());
        Block clickedBlock = clickedBlockState.getBlock();

        if (player != null && player.isSneaking()) {
            if (!DynamicOreDetection.isValidOre(clickedBlock)) {
                String blockName = clickedBlock.getName().getString();
                player.sendMessage(Text.translatable("item.miners-compass.ore_compass.not_ore", blockName), true);
                return ActionResult.SUCCESS;
            }

            Optional<DynamicOreType> oreTypeOpt = OreTypeManager.getOreTypeForBlock(clickedBlock);
            if (oreTypeOpt.isEmpty()) {
                player.sendMessage(Text.translatable("item.miners-compass.ore_compass.unknown_ore"), true);
                return ActionResult.SUCCESS;
            }

            DynamicOreType oreType = oreTypeOpt.get();
            var data = itemStack.get(DataComponentTypes.CUSTOM_DATA);
            NbtCompound nbt;
            if (data != null) nbt = data.copyNbt();
            else nbt = new NbtCompound();
            Set<DynamicOreType> selectedOreTypes = getSelectedOreTypesFromNbt(nbt);

            if (selectedOreTypes.contains(oreType)) {
                selectedOreTypes.remove(oreType);
                OreTypeManager.sendOreTypeMessage(player, oreType, false);
                if (selectedOreTypes.isEmpty()) {
                    clearNbtRecords(itemStack);
                }
            } else {
                if (selectedOreTypes.size() < MinersCompassMod.config.maxBlocks) {
                    selectedOreTypes.add(oreType);
                    OreTypeManager.sendOreTypeMessage(player, oreType, true);
                } else {
                    player.sendMessage(Text.translatable("item.miners-compass.ore_compass.max_ore_types"), true);
                    return ActionResult.SUCCESS;
                }
            }

            saveSelectedOreTypesToNbt(itemStack, selectedOreTypes);
            return ActionResult.SUCCESS;
        }

        return super.useOnBlock(context);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType tooltipType) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;
        if (player.isCreative()) return;

        var world = MinecraftClient.getInstance().world;
        if (world == null) return;

        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt = new NbtCompound();
        if (data != null) nbt = data.copyNbt();

        Set<DynamicOreType> selectedOreTypes = getSelectedOreTypesFromNbt(nbt);

        if (getTrackedPos(nbt) != null) {
            StringBuilder trackedOreTypeName = new StringBuilder();
            if (hasNbtKey(nbt, TRACKED_ORES_TYPES_KEY)) {
                var oreTypesIds = nbt.getList(TRACKED_ORES_TYPES_KEY, NbtElement.STRING_TYPE);
                for (var oreTypeId : oreTypesIds)
                    for (DynamicOreType type : DynamicOreType.getAllTypes())
                        if (type.getId().equals(oreTypeId.asString()))
                            trackedOreTypeName.append(type.getDisplayName()).append("; ");
            }
            if (trackedOreTypeName.isEmpty()) trackedOreTypeName.append("Unknown");

            tooltip.add(Text.translatable("tooltip.miners-compass.ore_compass.hint").formatted(Formatting.GRAY));

            var dimKey = getTrackedDimension(nbt);
            if (dimKey.isPresent() && !dimKey.get().toString().equals(world.getRegistryKey().toString())) {
                tooltip.add(Text.translatable("tooltip.miners-compass.ore_compass.wrong_dim1", trackedOreTypeName.toString())
                        .formatted(Formatting.DARK_RED).formatted(Formatting.BOLD));
                tooltip.add(Text.translatable("tooltip.miners-compass.ore_compass.wrong_dim2")
                        .formatted(Formatting.DARK_RED).formatted(Formatting.BOLD));
            } else {
                tooltip.add(Text.translatable("tooltip.miners-compass.ore_compass.locked_on", trackedOreTypeName.toString())
                        .formatted(Formatting.RED));
            }
        } else if (!selectedOreTypes.isEmpty()) {
            tooltip.add(Text.translatable("tooltip.miners-compass.ore_compass.not_found")
                    .formatted(Formatting.DARK_PURPLE));
        } else {
            tooltip.add(Text.translatable("tooltip.miners-compass.ore_compass.no_ore_types")
                    .formatted(Formatting.DARK_PURPLE));
        }

        if (!selectedOreTypes.isEmpty()) {
            if (Screen.hasShiftDown()) {
                tooltip.add(Text.translatable("tooltip.miners-compass.ore_compass.selected_ore_types")
                        .formatted(Formatting.YELLOW));
                for (DynamicOreType oreType : selectedOreTypes) {
                    int variantCount = OreTypeManager.getVariantCount(oreType);
                    String displayText = variantCount > 1
                            ? oreType.getDisplayName() + " (" + variantCount + " variants)"
                            : oreType.getDisplayName();
                    tooltip.add(Text.literal(" - " + displayText).formatted(oreType.getColor()));
                }
            } else {
                tooltip.add(Text.translatable("tooltip.miners-compass.ore_compass.tooltip"));
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        findBlocks(stack, world, entity, false);
    }
}