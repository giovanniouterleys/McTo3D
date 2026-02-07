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
import fr.outerleys.giovanni.mcto3dFabric.utils.PlaceBlockPayload; // IMPORT IMPORTANT
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking; // IMPORT IMPORTANT
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

public class KeyInputHandler {

    // Define a unique category object.
    // Translation key will be: key.category.mcto3d.general
    public static final KeyBinding.Category MCTO3D_CATEGORY = KeyBinding.Category.create(Identifier.of("mcto3d", "general"));

    public static KeyBinding placeKey;
    public static KeyBinding cancelKey;
    public static KeyBinding rotateKey;
    public static KeyBinding moveFarKey;
    public static KeyBinding moveNearKey;

    public static void register() {
        // 1. Register KeyBindings
        placeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mcto3d.place",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_ENTER,
                MCTO3D_CATEGORY
        ));

        cancelKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mcto3d.cancel",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_BACKSPACE,
                MCTO3D_CATEGORY
        ));

        rotateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mcto3d.rotate",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT,
                MCTO3D_CATEGORY
        ));

        moveFarKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mcto3d.far",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UP,
                MCTO3D_CATEGORY
        ));

        moveNearKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mcto3d.near",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_DOWN,
                MCTO3D_CATEGORY
        ));

        // 2. Event Logic (Per-tick handling)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (!ImportManager.isActive) return;

            // VALIDATE (Place blocks)
            while (placeKey.wasPressed()) {
                placeGhostBlocks(client);
            }

            // CANCEL
            while (cancelKey.wasPressed()) {
                ImportManager.clear();
                client.player.sendMessage(Text.literal("§cImport cancelled."), true);
            }

            // ROTATION
            while (rotateKey.wasPressed()) {
                ImportManager.rotationSteps = (ImportManager.rotationSteps + 1) % 4;
                client.player.sendMessage(Text.literal("§eRotation: " + (ImportManager.rotationSteps * 90) + "°"), true);
            }

            // DISTANCE CONTROL
            if (moveFarKey.isPressed()) {
                ImportManager.distance = Math.min(ImportManager.distance + 0.5, 50.0);
                client.player.sendMessage(Text.literal("§bDistance: " + String.format("%.1f", ImportManager.distance) + "m"), true);
            }

            if (moveNearKey.isPressed()) {
                ImportManager.distance = Math.max(ImportManager.distance - 0.5, 2.0);
                client.player.sendMessage(Text.literal("§bDistance: " + String.format("%.1f", ImportManager.distance) + "m"), true);
            }
        });
    }

    private static void placeGhostBlocks(MinecraftClient client) {
        if (client.player == null || ImportManager.currentVoxels == null) return;

        float tickDelta = client.getRenderTickCounter().getDynamicDeltaTicks();

        // Calculate origin position based on view vector
        Vec3d eyes = client.player.getEyePos();
        Vec3d look = client.player.getRotationVec(tickDelta);
        Vec3d originD = eyes.add(look.multiply(ImportManager.distance));
        BlockPos origin = new BlockPos((int)originD.x, (int)originD.y, (int)originD.z);

        int count = 0;
        int rotation = ImportManager.rotationSteps;

        for (Map.Entry<BlockPos, BlockState> entry : ImportManager.currentVoxels.entrySet()) {
            BlockPos rel = entry.getKey();
            int newX = rel.getX();
            int newZ = rel.getZ();

            // Mathematical rotation of relative coordinates
            for (int i = 0; i < rotation; i++) {
                int tempX = newX;
                newX = -newZ;
                newZ = tempX;
            }

            BlockPos finalPos = origin.add(newX, rel.getY(), newZ);

            if (client.world != null) {
                // --- OLD WAY (Ghost Blocks) ---
                // client.world.setBlockState(finalPos, entry.getValue(), 3);

                // --- NEW WAY (Networking) ---
                // We send a packet to the server for each block
                // (Note: For very large objects, we should batch this, but for <5000 blocks it's okay)
                int rawId = Block.getRawIdFromState(entry.getValue());
                var payload = new PlaceBlockPayload(finalPos, rawId);

                if (ClientPlayNetworking.canSend(PlaceBlockPayload.ID)) {
                    ClientPlayNetworking.send(payload);
                    count++;
                }
            }
        }

        client.player.sendMessage(Text.literal("§aObject placed (" + count + " blocks)!"), false);
        ImportManager.clear();
    }
}