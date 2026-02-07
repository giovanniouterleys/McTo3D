/*
 * McTo3D
 * Copyright (c) 2026 Giovanni Outerleys
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package fr.outerleys.giovanni.mcto3dFabric.client.event;

import fr.outerleys.giovanni.mcto3dFabric.client.utils.ImportManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Map;

public class InputHandler {

    public static void register() {

        // 1. LEFT CLICK (Rotation)
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (ImportManager.isActive) {
                // Ensure we are on client side
                if (player.getEntityWorld().isClient()) {
                    ImportManager.rotationSteps = (ImportManager.rotationSteps + 1) % 4;
                    player.sendMessage(Text.literal("§eRotation: " + (ImportManager.rotationSteps * 90) + "°"), true);
                }
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        // 2. RIGHT CLICK (Place)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (ImportManager.isActive) {
                if (player.getEntityWorld().isClient()) {
                    placeGhostBlocks(MinecraftClient.getInstance());
                }
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        // 3. MOUSE WHEEL / TICK EVENT
        // Logic for handling scroll or additional tick-based inputs.
        // Note: Direct mouse scroll detection often requires Mixins or specific API calls.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && ImportManager.isActive) {
                // Placeholder for scroll logic or alternative input methods
            }
        });
    }

    private static void placeGhostBlocks(MinecraftClient client) {
        if (client.player == null || ImportManager.currentVoxels == null) return;

        // Retrieve dynamic tick delta for smooth rendering/positioning
        float tickDelta = client.getRenderTickCounter().getDynamicDeltaTicks();

        Vec3d eyes = client.player.getEyePos();
        Vec3d look = client.player.getRotationVec(tickDelta);
        Vec3d originD = eyes.add(look.multiply(ImportManager.distance));
        BlockPos origin = new BlockPos((int)originD.x, (int)originD.y, (int)originD.z);

        int count = 0;
        int rotation = ImportManager.rotationSteps;

        for (Map.Entry<BlockPos, BlockState> entry : ImportManager.currentVoxels.entrySet()) {
            BlockPos rel = entry.getKey();
            int x = rel.getX();
            int z = rel.getZ();
            int newX = x;
            int newZ = z;

            // Apply rotation logic
            for (int i = 0; i < rotation; i++) {
                int tempX = newX;
                newX = -newZ;
                newZ = tempX;
            }

            BlockPos finalPos = origin.add(newX, rel.getY(), newZ);

            if (client.world != null) {
                client.world.setBlockState(finalPos, entry.getValue());
                count++;
            }
        }

        client.player.sendMessage(Text.literal("§aObject placed (" + count + " blocks)!"), false);
        ImportManager.clear();
    }
}