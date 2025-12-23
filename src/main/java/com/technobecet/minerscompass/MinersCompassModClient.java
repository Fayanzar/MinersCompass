package com.technobecet.minerscompass;

import com.technobecet.minerscompass.client.render.DirectionLines;
import com.technobecet.minerscompass.item.ModItems;
import com.technobecet.minerscompass.item.custom.OreCompass;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.item.CompassAnglePredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public class MinersCompassModClient implements ClientModInitializer {

    CompassAnglePredicateProvider ANGLE_DELEGATE = new CompassAnglePredicateProvider((world, stack, entity) -> {
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt;
        if (data != null) nbt = data.copyNbt();
        else nbt = new NbtCompound();
        return OreCompass.getTrackedPos(nbt);
    });

    private static float getSpinningAngle(ClientWorld world) {
        Long t = world.getTime() % 32L;
        return t.floatValue() / 32.0f;
    }

    @Override
    public void onInitializeClient() {
        ModelPredicateProviderRegistry.register(ModItems.ORE_COMPASS, Identifier.of("angle"),
                (stack, world, entity, i) -> {
                    var data = stack.get(DataComponentTypes.CUSTOM_DATA);
                    NbtCompound nbt;
                    if (data != null) nbt = data.copyNbt();
                    else nbt = new NbtCompound();
                    var pos = OreCompass.getTrackedPos(nbt);
                    if (pos == null && world != null) {
                        return getSpinningAngle(world);
                    }

                    return ANGLE_DELEGATE.unclampedCall(stack, world, entity, i);
                });
        WorldRenderEvents.LAST.register(DirectionLines::drawDirections);
    }
}
