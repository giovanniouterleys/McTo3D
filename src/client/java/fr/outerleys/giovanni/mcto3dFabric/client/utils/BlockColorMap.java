/*
 * McTo3D
 * Copyright (c) 2026 Giovanni Outerleys
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package fr.outerleys.giovanni.mcto3dFabric.client.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlockColorMap {

    private static final Map<BlockState, Color> COLOR_PALETTE = new HashMap<>();
    // List of blocks considered "Grayscale" (neutral/no saturation) to apply penalties
    private static final Set<BlockState> GRAYSCALE_BLOCKS = new HashSet<>();

    static {
        // --- BROWNS / WOODS / DIRT (Essential for buildings, terrain, etc.) ---
        register(Blocks.DIRT, 134, 96, 67);
        register(Blocks.COARSE_DIRT, 119, 85, 59);
        register(Blocks.PACKED_MUD, 140, 107, 86);     // Excellent dull brown
        register(Blocks.MUD_BRICKS, 138, 110, 89);
        register(Blocks.BRICKS, 150, 97, 83);          // Red/Brown brick
        register(Blocks.GRANITE, 149, 103, 85);
        register(Blocks.POLISHED_GRANITE, 154, 106, 89);
        register(Blocks.TERRACOTTA, 152, 94, 67);      // Clay brown
        register(Blocks.BROWN_TERRACOTTA, 76, 50, 35); // Very dark brown
        register(Blocks.BROWN_CONCRETE, 96, 59, 31);   // Chocolate brown
        register(Blocks.BROWN_WOOL, 114, 71, 40);
        register(Blocks.SPRUCE_PLANKS, 114, 84, 56);   // Dark wood
        register(Blocks.DARK_OAK_PLANKS, 66, 43, 20);  // Very dark wood
        register(Blocks.OAK_PLANKS, 162, 130, 78);     // Light/Golden wood
        register(Blocks.JUNGLE_PLANKS, 160, 115, 80);
        register(Blocks.NETHER_BRICKS, 44, 21, 26);    // Dark reddish brown

        // --- COLORED GRAYS (For aged metals) ---
        register(Blocks.GRAY_TERRACOTTA, 57, 41, 35);       // Warm gray (almost brown)
        register(Blocks.LIGHT_GRAY_TERRACOTTA, 135, 107, 98); // Beige gray

        // --- GREENS / BLUES / TEAL (Statue of Liberty style) ---
        register(Blocks.PRISMARINE, 99, 156, 151);
        register(Blocks.PRISMARINE_BRICKS, 99, 171, 162);
        register(Blocks.DARK_PRISMARINE, 51, 87, 82);
        register(Blocks.WARPED_PLANKS, 43, 104, 99);
        register(Blocks.STRIPPED_WARPED_HYPHAE, 58, 142, 140);
        register(Blocks.OXIDIZED_COPPER, 86, 163, 147);
        register(Blocks.WEATHERED_COPPER, 109, 160, 130);
        register(Blocks.EXPOSED_COPPER, 161, 125, 103); // Tarnished copper
        register(Blocks.MOSSY_COBBLESTONE, 108, 118, 92);

        // --- GOLD / YELLOW ---
        register(Blocks.GOLD_BLOCK, 246, 208, 61);
        register(Blocks.RAW_GOLD_BLOCK, 228, 178, 62);
        register(Blocks.YELLOW_CONCRETE, 240, 175, 21);
        register(Blocks.ORANGE_CONCRETE, 224, 97, 0);

        // --- WOOLS ---
        register(Blocks.CYAN_WOOL, 21, 137, 145);
        register(Blocks.GREEN_WOOL, 84, 109, 27);
        register(Blocks.LIME_WOOL, 112, 185, 25);
        register(Blocks.BLUE_WOOL, 53, 57, 157);
        register(Blocks.LIGHT_BLUE_WOOL, 58, 175, 217);
        register(Blocks.RED_WOOL, 160, 39, 34);

        // --- CONCRETES ---
        register(Blocks.CYAN_CONCRETE, 21, 119, 136);
        register(Blocks.GREEN_CONCRETE, 73, 91, 36);
        register(Blocks.LIME_CONCRETE, 94, 169, 24);
        register(Blocks.BLUE_CONCRETE, 44, 46, 143);
        register(Blocks.LIGHT_BLUE_CONCRETE, 35, 137, 198);
        register(Blocks.RED_CONCRETE, 142, 32, 32);

        // --- TERRACOTTA ---
        register(Blocks.CYAN_TERRACOTTA, 87, 92, 92);
        register(Blocks.GREEN_TERRACOTTA, 76, 83, 42);
        register(Blocks.LIME_TERRACOTTA, 103, 117, 53);
        register(Blocks.LIGHT_BLUE_TERRACOTTA, 113, 108, 137);
        register(Blocks.RED_TERRACOTTA, 143, 61, 46);

        // --- NEUTRALS (Grayscale) ---
        registerGrayscale(Blocks.WHITE_CONCRETE, 207, 213, 214);
        registerGrayscale(Blocks.GRAY_CONCRETE, 54, 57, 61);
        registerGrayscale(Blocks.LIGHT_GRAY_CONCRETE, 125, 125, 115);
        registerGrayscale(Blocks.BLACK_CONCRETE, 8, 10, 15);
        registerGrayscale(Blocks.STONE, 125, 125, 125);
        registerGrayscale(Blocks.COBBLESTONE, 100, 100, 100);
        registerGrayscale(Blocks.IRON_BLOCK, 220, 220, 220); // Light metal
    }

    private static void register(Block block, int r, int g, int b) {
        COLOR_PALETTE.put(block.getDefaultState(), new Color(r, g, b));
    }

    private static void registerGrayscale(Block block, int r, int g, int b) {
        BlockState state = block.getDefaultState();
        COLOR_PALETTE.put(state, new Color(r, g, b));
        GRAYSCALE_BLOCKS.add(state);
    }

    public static BlockState getClosestBlock(int r, int g, int b) {
        BlockState bestBlock = Blocks.WHITE_CONCRETE.getDefaultState();
        double minDistance = Double.MAX_VALUE;

        // Saturation Analysis (RGB -> HSB)
        float[] inputHSB = Color.RGBtoHSB(r, g, b, null);
        float inputSaturation = inputHSB[1];

        // Threshold to determine if the input color is "colorful" (> 5% saturation)
        boolean hasColor = inputSaturation > 0.05f;

        for (Map.Entry<BlockState, Color> entry : COLOR_PALETTE.entrySet()) {
            BlockState state = entry.getKey();
            Color target = entry.getValue();

            double dR = target.getRed() - r;
            double dG = target.getGreen() - g;
            double dB = target.getBlue() - b;

            // --- BALANCED WEIGHTING FORMULA ---
            // Standard approximate perceptual weighting: 3*R, 4*G, 2*B.
            // This prevents Green from dominating too much while respecting Red/Brown tones.
            double distance = (3.0 * dR * dR) + (4.0 * dG * dG) + (2.0 * dB * dB);

            // Grayscale Penalty
            // If the input has color, heavily penalize grayscale blocks to force a colored match.
            if (hasColor && GRAYSCALE_BLOCKS.contains(state)) {
                distance += 10000;
            }

            if (distance < minDistance) {
                minDistance = distance;
                bestBlock = state;
            }
        }
        return bestBlock;
    }
}