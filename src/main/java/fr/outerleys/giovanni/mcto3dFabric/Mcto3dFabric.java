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

import fr.outerleys.giovanni.mcto3dFabric.selection.SelectionManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class Mcto3dFabric implements ModInitializer {

    // Static instance so the Client (particles) can access it
    public static final SelectionManager MANAGER = new SelectionManager();

    @Override
    public void onInitialize() {

        // 1. LEFT CLICK (Attack) -> Position 1
        // Using AttackBlockCallback
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            // Verify main hand and Golden Hoe
            if (hand != Hand.MAIN_HAND || player.getStackInHand(hand).getItem() != Items.GOLDEN_HOE) {
                return ActionResult.PASS; // Let normal game behavior proceed if not the tool
            }

            if (!world.isClient()) {
                MANAGER.setPos1(player.getUuid(), pos);
                player.sendMessage(Text.literal("§aPosition 1 set! (Left Click)"), true);
            }

            // IMPORTANT: Return SUCCESS to indicate "action handled".
            // This PREVENTS breaking the block (otherwise you'd destroy your build while selecting)
            return ActionResult.SUCCESS;
        });

        // 2. RIGHT CLICK (Use) -> Position 2
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand != Hand.MAIN_HAND || player.getStackInHand(hand).getItem() != Items.GOLDEN_HOE) {
                return ActionResult.PASS;
            }

            if (!world.isClient()) {
                MANAGER.setPos2(player.getUuid(), hitResult.getBlockPos());
                player.sendMessage(Text.literal("§bPosition 2 set! (Right Click)"), true);
            }

            // Return SUCCESS to prevent the hoe from tilling dirt/grass
            return ActionResult.SUCCESS;
        });
    }
}