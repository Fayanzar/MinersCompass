package com.technobecet.minerscompass.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.technobecet.minerscompass.MinersCompassMod;
import com.technobecet.minerscompass.item.custom.OreCompass;
import com.technobecet.minerscompass.util.DynamicOreType;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class DirectionLines {
    public static void drawDirections (WorldRenderContext context) {
        if (!MinersCompassMod.config.showDirectionLines) return;

        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        var matrixStack = context.matrixStack();
        if (matrixStack == null) return;

        var stack = player.getInventory().getMainHandStack();
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt;
        if (data != null) nbt = data.copyNbt();
        else nbt = new NbtCompound();

        var trackedPos = OreCompass.getTrackedPos(nbt);
        if (trackedPos == null) return;

        var oreType = OreCompass.getTrackedOreType(nbt);
        var formatting = Formatting.RED;
        if (oreType != null) {
            formatting = DynamicOreType.generateColorForOre(oreType);
        }

        var color = formatting.getColorValue();
        if (color == null) color = 0x33FF0000;
        else color = 0x33000000 + color;

        var camera = context.camera();

        var cameraPos = camera.getPos();
        var playerPos = player.getPos();
        var playerPrevPos = new Vec3d(player.prevX, player.prevY, player.prevZ);
        var tick = context.tickCounter().getTickDelta(true);

        var transformationMatrix = matrixStack.peek().getPositionMatrix();
        var playerPosInterpolated = new Vec3d(
                playerPrevPos.x + (playerPos.x - playerPrevPos.x) * (double)tick,
                playerPrevPos.y + (playerPos.y - playerPrevPos.y) * (double)tick,
                playerPrevPos.z + (playerPos.z - playerPrevPos.z) * (double)tick);
        var translationVec = playerPosInterpolated.add(cameraPos.negate());

        var blockX = trackedPos.pos().getX() + 0.5 - playerPosInterpolated.x;
        var blockY = trackedPos.pos().getY() + 0.5 - playerPosInterpolated.y;
        var blockZ = trackedPos.pos().getZ() + 0.5 - playerPosInterpolated.z;

        Vec3d blockRelPos = new Vec3d(blockX, blockY - 1, blockZ);
        if (blockRelPos.length() > 1) blockRelPos = blockRelPos.normalize();

        transformationMatrix.translate((float)translationVec.getX(), (float)translationVec.getY(), (float)translationVec.getZ());

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Vec2f orth = new Vec2f((float) blockRelPos.z, -(float) blockRelPos.x);
        orth = orth.normalize();

        buffer.vertex(transformationMatrix, orth.x / 10, 1, orth.y / 10)
                .color(color);
        buffer.vertex(transformationMatrix, -orth.x / 10, 1, -orth.y / 10)
                .color(color);
        buffer.vertex(transformationMatrix,
                        (float)blockRelPos.x - orth.x / 10,
                        (float)blockRelPos.y + 1,
                        (float)blockRelPos.z - orth.y / 10)
                .color(color);
        buffer.vertex(transformationMatrix,
                        (float)blockRelPos.x + orth.x / 10,
                        (float)blockRelPos.y + 1,
                        (float)blockRelPos.z + orth.y / 10)
                .color(color);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
