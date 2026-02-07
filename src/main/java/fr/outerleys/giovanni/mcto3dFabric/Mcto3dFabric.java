/*
 * McTo3D
 * Copyright (c) 2026 Giovanni Outerleys
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package fr.outerleys.giovanni.mcto3dFabric;

import fr.outerleys.giovanni.mcto3dFabric.utils.PlaceBlockPayload;
import fr.outerleys.giovanni.mcto3dFabric.selection.SelectionManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class Mcto3dFabric implements ModInitializer {

    // Static instance so the Client (particles) can access it
    public static final SelectionManager MANAGER = new SelectionManager();

    @Override
    public void onInitialize() {

        // --- 1. NETWORKING REGISTRATION (For Ghost Block fixing) ---
        // Register the payload type (Client to Server)
        PayloadTypeRegistry.playC2S().register(PlaceBlockPayload.ID, PlaceBlockPayload.CODEC);

        // Register the packet receiver (Logic executed on the Server)
        ServerPlayNetworking.registerGlobalReceiver(PlaceBlockPayload.ID, (payload, context) -> {
            // The logic must be executed on the main server thread
            context.server().execute(() -> {
                var player = context.player();

                // Security check: Ensure player is in a valid world
                if (player != null && player.getEntityWorld() != null) {

                    // Optional: You could check permissions here (e.g., if (player.hasPermissionLevel(2)))

                    // Place the REAL block in the world based on the ID received from client
                    player.getEntityWorld().setBlockState(
                            payload.pos(),
                            Block.getStateFromRawId(payload.stateId()),
                            3 // Flag 3 = Notify neighbors & Update client
                    );
                }
            });
        });

        // --- 2. SELECTION TOOL (Golden Hoe) ---

        // LEFT CLICK (Attack) -> Set Position 1
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            // Verify Main Hand and Golden Hoe
            if (hand != Hand.MAIN_HAND || player.getStackInHand(hand).getItem() != Items.GOLDEN_HOE) {
                return ActionResult.PASS; // Let normal game behavior proceed
            }

            // Logic runs on Server side (to save state)
            if (!world.isClient()) {
                MANAGER.setPos1(player.getUuid(), pos);
                player.sendMessage(Text.literal("§aPosition 1 set! (Left Click)"), true);
            }

            // IMPORTANT: Return SUCCESS to prevent breaking the block
            return ActionResult.SUCCESS;
        });

        // RIGHT CLICK (Use) -> Set Position 2
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand != Hand.MAIN_HAND || player.getStackInHand(hand).getItem() != Items.GOLDEN_HOE) {
                return ActionResult.PASS;
            }

            if (!world.isClient()) {
                MANAGER.setPos2(player.getUuid(), hitResult.getBlockPos());
                player.sendMessage(Text.literal("§bPosition 2 set! (Right Click)"), true);
            }

            // Return SUCCESS to prevent hoeing the dirt (farmland creation)
            return ActionResult.SUCCESS;
        });
    }
}