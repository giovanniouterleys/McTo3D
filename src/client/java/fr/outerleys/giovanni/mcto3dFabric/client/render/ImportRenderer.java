/*
 * McTo3D
 * Copyright (c) 2026 Giovanni Outerleys
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package fr.outerleys.giovanni.mcto3dFabric.client.render;

import fr.outerleys.giovanni.mcto3dFabric.client.utils.ImportManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Map;

public class ImportRenderer {

    public static void register() {
        WorldRenderEvents.END_MAIN.register(ImportRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        if (!ImportManager.isActive || ImportManager.currentVoxels == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        MatrixStack matrices = context.matrices();
        Vec3d camPos = client.gameRenderer.getCamera().getPos();

        // Retrieve dynamic tick delta for smooth interpolation
        float tickDelta = client.getRenderTickCounter().getDynamicDeltaTicks();

        // Calculate target position based on player look vector
        Vec3d playerEyes = client.player.getEyePos();
        Vec3d lookDir = client.player.getRotationVec(tickDelta);
        Vec3d targetPos = playerEyes.add(lookDir.multiply(ImportManager.distance));

        matrices.push();
        // Move to the target position relative to camera
        matrices.translate(targetPos.x - camPos.x, targetPos.y - camPos.y, targetPos.z - camPos.z);

        // Apply rotation (90 degrees steps)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ImportManager.rotationSteps * 90f));

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) consumers = client.getBufferBuilders().getEntityVertexConsumers();

        // Use getDebugFilledBox() as it handles translucency and depth correctly for colored quads
        // without needing complex custom render layers.
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getDebugFilledBox());

        for (Map.Entry<BlockPos, BlockState> entry : ImportManager.currentVoxels.entrySet()) {
            BlockPos pos = entry.getKey();

            // Optimization: Only render the outer shell of the object
            if (ImportManager.isEdge(pos)) {
                matrices.push();
                matrices.translate(pos.getX(), pos.getY(), pos.getZ());

                // Get the block's default map color since we don't have world context for the ghost block
                int colorInt = entry.getValue().getBlock().getDefaultMapColor().color;

                float r = ((colorInt >> 16) & 0xFF) / 255f;
                float g = ((colorInt >> 8) & 0xFF) / 255f;
                float b = (colorInt & 0xFF) / 255f;
                float a = 0.4f; // 40% opacity

                // Draw the ghost cube
                drawGhostCube(matrices, buffer, r, g, b, a);

                matrices.pop();
            }
        }

        matrices.pop();
    }

    private static void drawGhostCube(MatrixStack stack, VertexConsumer buffer, float r, float g, float b, float a) {
        Matrix4f m = stack.peek().getPositionMatrix();

        // Cube slightly smaller (0.01 padding) to avoid Z-fighting if overlapping with real blocks
        float min = 0.01f;
        float max = 0.99f;

        // Face BOTTOM
        vertex(buffer, m, min,min,min, r,g,b,a); vertex(buffer, m, max,min,min, r,g,b,a); vertex(buffer, m, max,min,max, r,g,b,a); vertex(buffer, m, min,min,max, r,g,b,a);
        // Face TOP
        vertex(buffer, m, min,max,max, r,g,b,a); vertex(buffer, m, max,max,max, r,g,b,a); vertex(buffer, m, max,max,min, r,g,b,a); vertex(buffer, m, min,max,min, r,g,b,a);
        // Face NORTH
        vertex(buffer, m, min,min,min, r,g,b,a); vertex(buffer, m, min,max,min, r,g,b,a); vertex(buffer, m, max,max,min, r,g,b,a); vertex(buffer, m, max,min,min, r,g,b,a);
        // Face SOUTH
        vertex(buffer, m, max,min,max, r,g,b,a); vertex(buffer, m, max,max,max, r,g,b,a); vertex(buffer, m, min,max,max, r,g,b,a); vertex(buffer, m, min,min,max, r,g,b,a);
        // Face WEST
        vertex(buffer, m, min,min,max, r,g,b,a); vertex(buffer, m, min,max,max, r,g,b,a); vertex(buffer, m, min,max,min, r,g,b,a); vertex(buffer, m, min,min,min, r,g,b,a);
        // Face EST
        vertex(buffer, m, max,min,min, r,g,b,a); vertex(buffer, m, max,max,min, r,g,b,a); vertex(buffer, m, max,max,max, r,g,b,a); vertex(buffer, m, max,min,max, r,g,b,a);
    }

    private static void vertex(VertexConsumer b, Matrix4f m, float x, float y, float z, float r, float g, float flB, float a) {
        // .next() is handled automatically in modern versions
        b.vertex(m, x, y, z).color(r, g, flB, a);
    }
}