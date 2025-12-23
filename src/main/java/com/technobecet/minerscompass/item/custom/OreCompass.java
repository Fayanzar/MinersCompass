package com.technobecet.minerscompass.item.custom;

import com.technobecet.minerscompass.MinersCompassMod;
import com.technobecet.minerscompass.util.DynamicOreDetection;
import com.technobecet.minerscompass.util.DynamicOreType;
import com.technobecet.minerscompass.util.OreTypeManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
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

public class OreCompass extends Item {

    public static final String SELECTED_ORE_TYPES_KEY = "SelectedOreTypes";
    public static final String TRACKED_BLOCK_KEY = "TrackedBlock";
    public static final String TRACKED_ORE_TYPE_KEY = "TrackedOreType";
    public static final String TARGET_BLOCK_POS_KEY = "TargetBlockPos";
    public static final String TARGET_BLOCK_DIMENSION_KEY = "TargetBlockDimension";
    public static final String TARGET_BLOCK_TRACKED_KEY = "TargetBlockTracked";

    public OreCompass(Settings settings) {
        super(settings);
    }

    @Nullable
    public static GlobalPos getTrackedPos(NbtCompound nbt) {
        if (nbt == null) return null;

        boolean hasTrackedValue = hasNbtKey(nbt, TARGET_BLOCK_TRACKED_KEY);
        if (!hasTrackedValue) return null;
        
        boolean tracked = nbt.getBoolean(TARGET_BLOCK_TRACKED_KEY);

        Optional<RegistryKey<World>> worldKey;
        boolean hasPosKey = hasNbtKey(nbt, TARGET_BLOCK_POS_KEY);
        boolean hasDimKey = hasNbtKey(nbt, TARGET_BLOCK_DIMENSION_KEY);

        if (hasPosKey && hasDimKey && tracked && (worldKey = getTrackedDimension(nbt)).isPresent()) {
            var blockPos = NbtHelper.toBlockPos(nbt, TARGET_BLOCK_POS_KEY);
            if (blockPos.isPresent()) return GlobalPos.create(worldKey.get(), blockPos.get());
        }
        return null;
    }

    public static String getTrackedOreType(NbtCompound nbt) {
        if (nbt == null) return null;

        var hasTrackedOreType = hasNbtKey(nbt, TRACKED_ORE_TYPE_KEY);
        if (!hasTrackedOreType) return null;

        return nbt.getString(TRACKED_ORE_TYPE_KEY);
    }

    private static Optional<BlockPos> findBlocks(ItemStack stack, World world, Entity entity, boolean force) {
        if (world.isClient) {
            return Optional.empty();
        }

        MinersCompassMod.LOGGER.info("Finding blocks for player: {} in world: {}", entity.getName().getString(), world.getRegistryKey().getValue());

        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        var nbt = new NbtCompound();
        if (data != null) nbt = data.copyNbt();
        Set<DynamicOreType> selectedOreTypes = getSelectedOreTypesFromNbt(nbt);
        MinersCompassMod.LOGGER.info("Selected ore types: {}", selectedOreTypes.size());
        
        if (selectedOreTypes.isEmpty()) {
            MinersCompassMod.LOGGER.info("No ore types selected - clearing NBT and returning empty");
            clearNbtRecords(stack);
            return Optional.empty();
        }

        List<Block> targetBlocks = OreTypeManager.getAllBlocksForOreTypes(selectedOreTypes);
        MinersCompassMod.LOGGER.info("Target blocks found: {}", targetBlocks.size());
        
        if (targetBlocks.isEmpty()) {
            MinersCompassMod.LOGGER.info("No target blocks found - clearing NBT and returning empty");
            clearNbtRecords(stack);
            return Optional.empty();
        }

        data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data != null) nbt = data.copyNbt();
        var trackedPos = getTrackedPos(nbt);
        if (!force && trackedPos != null) {
            BlockState trackedBlockState = world.getBlockState(trackedPos.pos());
            if (targetBlocks.contains(trackedBlockState.getBlock())) {
                return Optional.of(trackedPos.pos());
            } else {
                var dimKey = getTrackedDimension(nbt);
                if (dimKey.isPresent() && !dimKey.get().toString().equals(entity.getWorld().getRegistryKey().toString())) {
                    return Optional.empty();
                }
            }
        }

        Optional<BlockPos> closest = findBlocksInNearbyChunks(stack, world, entity.getBlockPos(), targetBlocks);
        MinersCompassMod.LOGGER.info("Closest ore block found: {}", closest.isPresent() ? closest.get() : "none");
        
        playSoundOnStateChange(world, entity, stack, closest);
        writeNbt(world.getRegistryKey(), closest, stack);

        return closest;
    }

    private static Optional<RegistryKey<World>> getTrackedDimension(NbtCompound nbt) {
        return World.CODEC.parse(NbtOps.INSTANCE, nbt.get(TARGET_BLOCK_DIMENSION_KEY)).result();
    }

    private static void playSoundOnStateChange(World world, Entity entity, ItemStack stack, Optional<BlockPos> closest) {
        if (closest.isPresent()) {
            playSound(world, entity, true);
            return;
        }
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt;
        if (data != null) nbt = data.copyNbt();
        else nbt = new NbtCompound();
        var trackedPos = getTrackedPos(nbt);
        if (!closest.isPresent() && trackedPos != null) {
            playSound(world, entity, false);
        }
    }

    private static void writeNbt(RegistryKey<World> worldKey, Optional<BlockPos> closest, ItemStack stack) {
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt;
        if (data != null) nbt = data.copyNbt();
        else return;
        if (nbt == null) return;
        
        if (closest.isPresent()) {
            BlockPos pos = closest.get();
            nbt.put(TARGET_BLOCK_POS_KEY, NbtHelper.fromBlockPos(pos));
            World.CODEC.encodeStart(NbtOps.INSTANCE, worldKey)
                    .resultOrPartial(MinersCompassMod.LOGGER::error)
                    .ifPresent(nbtElement -> nbt.put(TARGET_BLOCK_DIMENSION_KEY, nbtElement));
            nbt.putBoolean(TARGET_BLOCK_TRACKED_KEY, true);
        } else {
            nbt.put(TARGET_BLOCK_POS_KEY, NbtHelper.fromBlockPos(BlockPos.ORIGIN));
            nbt.putBoolean(TARGET_BLOCK_TRACKED_KEY, false);
            nbt.remove(TARGET_BLOCK_DIMENSION_KEY);
        }

        var component = NbtComponent.of(nbt);
        stack.set(DataComponentTypes.CUSTOM_DATA, component);
    }

    private static void playSound(World world, Entity entity, boolean success) {
        world.playSound(null, entity.getBlockPos(),
                success ? SoundEvents.ITEM_LODESTONE_COMPASS_LOCK : SoundEvents.BLOCK_FIRE_EXTINGUISH,
                SoundCategory.PLAYERS, 1f, 1f);
    }

    private static Optional<BlockPos> findBlocksInNearbyChunks(ItemStack itemStack, World world, BlockPos entPos, List<Block> targetBlocks) {
        int chunkRadius = Math.max(0, MinersCompassMod.config.chunkRadius);
        Long2ObjectOpenHashMap<Set<BlockPos>> blocks = new Long2ObjectOpenHashMap<>();
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
                                        .add(mutableBlockPos.toImmutable());
                            }
                        }
                    }
                }
            }
        }
        return getClosestBlockPos(itemStack, blocks, entPos, world);
    }

    private static Optional<BlockPos> getClosestBlockPos(ItemStack itemStack, Long2ObjectOpenHashMap<Set<BlockPos>> blockMap, BlockPos entPos, World world) {
        double closestDistanceSq = Double.MAX_VALUE;
        BlockPos closestPos = null;
        Block closestBlock = null;

        for (Set<BlockPos> positions : blockMap.values()) {
            for (BlockPos blockPos : positions) {
                double dx = blockPos.getX() - entPos.getX();
                double dy = blockPos.getY() - entPos.getY();
                double dz = blockPos.getZ() - entPos.getZ();
                double distanceSq = dx * dx + dy * dy + dz * dz;

                if (distanceSq < closestDistanceSq) {
                    closestPos = blockPos;
                    closestDistanceSq = distanceSq;
                    closestBlock = world.getBlockState(closestPos).getBlock();
                }
            }
        }

        if (closestPos != null && closestBlock != null) {
            var data = itemStack.get(DataComponentTypes.CUSTOM_DATA);
            NbtCompound nbt;
            if (data != null) nbt = data.copyNbt();
            else nbt = new NbtCompound();
            nbt.putString(TRACKED_BLOCK_KEY, Registries.BLOCK.getId(closestBlock).toString());
            
            Optional<DynamicOreType> oreType = OreTypeManager.getOreTypeForBlock(closestBlock);
            oreType.ifPresent(type -> nbt.putString(TRACKED_ORE_TYPE_KEY, type.getId()));

            NbtComponent component = NbtComponent.of(nbt);
            itemStack.set(DataComponentTypes.CUSTOM_DATA, component);
        }

        return Optional.ofNullable(closestPos);
    }

    private static Set<DynamicOreType> getSelectedOreTypesFromNbt(NbtCompound nbt) {
        if (nbt == null) return Collections.emptySet();
        
        Set<DynamicOreType> oreTypes = new HashSet<>();
        for (String key : nbt.getKeys()) {
            if (key.startsWith(SELECTED_ORE_TYPES_KEY)) {
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
            nbt.putString(SELECTED_ORE_TYPES_KEY + index, oreType.getId());
            index++;
        }

        NbtComponent component = NbtComponent.of(nbt);
        stack.set(DataComponentTypes.CUSTOM_DATA, component);
    }

    private static void clearOreTypeKeys(NbtCompound nbt) {
        List<String> keysToRemove = new ArrayList<>();
        for (String key : nbt.getKeys()) {
            if (key.startsWith(SELECTED_ORE_TYPES_KEY)) {
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
        var data = itemStack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt;
        if (data != null) nbt = data.copyNbt();
        else nbt = new NbtCompound();

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
            openOreSelectionGui(itemStack);
            return TypedActionResult.success(itemStack);
        }

        Set<DynamicOreType> selectedOreTypes = getSelectedOreTypesFromNbt(nbt);
        if (selectedOreTypes.isEmpty()) {
            user.sendMessage(Text.translatable("item.miners-compass.ore_compass.no_ore_types"), true);
            return TypedActionResult.success(itemStack);
        }

        user.getItemCooldownManager().set(this, 100);
        Optional<BlockPos> pos = findBlocks(user.getMainHandStack(), world, user, true);
        playSound(world, user, pos.isPresent());

        return TypedActionResult.success(itemStack);
    }

    @net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    private void openOreSelectionGui(ItemStack itemStack) {
        net.minecraft.client.MinecraftClient.getInstance().setScreen(
            new com.technobecet.minerscompass.client.gui.OreSelectionScreen(itemStack)
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
            if (key.startsWith(SELECTED_ORE_TYPES_KEY) || key.equals(TARGET_BLOCK_POS_KEY) || 
                key.equals(TARGET_BLOCK_DIMENSION_KEY) || key.equals(TARGET_BLOCK_TRACKED_KEY) || 
                key.equals(TRACKED_BLOCK_KEY) || key.equals(TRACKED_ORE_TYPE_KEY)) {
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
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        findBlocks(stack, world, entity, false);
    }
}