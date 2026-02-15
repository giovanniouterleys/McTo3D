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
    private static final Set<BlockState> GRAYSCALE_BLOCKS = new HashSet<>();

    static {
        // --- WHITES & OFF-WHITES ---
        registerGrayscale(Blocks.WHITE_CONCRETE, 207, 213, 214);
        registerGrayscale(Blocks.WHITE_WOOL, 233, 236, 236);
        registerGrayscale(Blocks.SNOW_BLOCK, 249, 254, 254);
        registerGrayscale(Blocks.QUARTZ_BLOCK, 235, 230, 225);
        registerGrayscale(Blocks.SMOOTH_QUARTZ, 235, 230, 225);
        registerGrayscale(Blocks.CALCITE, 225, 225, 220);
        registerGrayscale(Blocks.DIORITE, 220, 220, 220);
        registerGrayscale(Blocks.POLISHED_DIORITE, 200, 200, 200);
        registerGrayscale(Blocks.BIRCH_LOG, 215, 215, 210);
        registerGrayscale(Blocks.MUSHROOM_STEM, 209, 203, 196);
        registerGrayscale(Blocks.IRON_BLOCK, 220, 220, 220);

        // --- GRAYS & STONES ---
        registerGrayscale(Blocks.LIGHT_GRAY_CONCRETE, 125, 125, 115);
        registerGrayscale(Blocks.LIGHT_GRAY_WOOL, 142, 142, 134);
        registerGrayscale(Blocks.ANDESITE, 130, 131, 131);
        registerGrayscale(Blocks.POLISHED_ANDESITE, 135, 136, 136);
        registerGrayscale(Blocks.STONE, 125, 125, 125);
        registerGrayscale(Blocks.STONE_BRICKS, 120, 120, 120);
        registerGrayscale(Blocks.COBBLESTONE, 100, 100, 100);
        registerGrayscale(Blocks.MOSSY_COBBLESTONE, 108, 118, 92);
        registerGrayscale(Blocks.TUFF, 108, 109, 102);
        registerGrayscale(Blocks.CLAY, 158, 164, 176);
        registerGrayscale(Blocks.GRAVEL, 126, 124, 122);
        registerGrayscale(Blocks.DEAD_TUBE_CORAL_BLOCK, 127, 127, 127);
        registerGrayscale(Blocks.DEAD_BRAIN_CORAL_BLOCK, 122, 122, 122);

        // --- DARK GRAYS & BLACKS ---
        registerGrayscale(Blocks.GRAY_CONCRETE, 54, 57, 61);
        registerGrayscale(Blocks.GRAY_WOOL, 62, 68, 71);
        registerGrayscale(Blocks.ACACIA_LOG, 103, 98, 93);
        registerGrayscale(Blocks.DEEPSLATE, 80, 80, 83);
        registerGrayscale(Blocks.COBBLED_DEEPSLATE, 75, 75, 78);
        registerGrayscale(Blocks.POLISHED_DEEPSLATE, 70, 70, 73);
        registerGrayscale(Blocks.DEEPSLATE_BRICKS, 65, 65, 68);
        registerGrayscale(Blocks.DEEPSLATE_TILES, 55, 55, 58);
        registerGrayscale(Blocks.BASALT, 81, 81, 84);
        registerGrayscale(Blocks.POLISHED_BASALT, 68, 68, 71);
        registerGrayscale(Blocks.BLACKSTONE, 42, 38, 43);
        registerGrayscale(Blocks.POLISHED_BLACKSTONE, 50, 45, 50);
        registerGrayscale(Blocks.POLISHED_BLACKSTONE_BRICKS, 42, 38, 43);
        registerGrayscale(Blocks.BLACK_CONCRETE, 8, 10, 15);
        registerGrayscale(Blocks.BLACK_WOOL, 20, 21, 25);
        registerGrayscale(Blocks.OBSIDIAN, 20, 18, 29);
        registerGrayscale(Blocks.COAL_BLOCK, 16, 16, 16);
        registerGrayscale(Blocks.BEDROCK, 47, 47, 47);

        // --- BROWNS, DIRT & WOODS ---
        register(Blocks.BROWN_CONCRETE, 96, 59, 31);
        register(Blocks.BROWN_WOOL, 114, 71, 40);
        register(Blocks.DIRT, 134, 96, 67);
        register(Blocks.COARSE_DIRT, 119, 85, 59);
        register(Blocks.ROOTED_DIRT, 139, 106, 79);
        register(Blocks.PACKED_MUD, 140, 107, 86);
        register(Blocks.MUD_BRICKS, 138, 110, 89);
        register(Blocks.SOUL_SOIL, 78, 62, 53);
        register(Blocks.PODZOL, 90, 63, 28);
        register(Blocks.GRANITE, 149, 103, 85);
        register(Blocks.POLISHED_GRANITE, 154, 106, 89);
        register(Blocks.BRICKS, 150, 97, 83);
        register(Blocks.TERRACOTTA, 152, 94, 67);
        register(Blocks.BROWN_TERRACOTTA, 76, 50, 35);
        register(Blocks.SPRUCE_PLANKS, 114, 84, 56);
        register(Blocks.SPRUCE_LOG, 58, 40, 24);
        register(Blocks.STRIPPED_SPRUCE_LOG, 115, 89, 57);
        register(Blocks.DARK_OAK_PLANKS, 66, 43, 20);
        register(Blocks.DARK_OAK_LOG, 46, 30, 16);
        register(Blocks.STRIPPED_DARK_OAK_LOG, 97, 72, 49);
        register(Blocks.OAK_PLANKS, 162, 130, 78);
        register(Blocks.OAK_LOG, 107, 83, 51);
        register(Blocks.STRIPPED_OAK_LOG, 177, 145, 96);
        register(Blocks.JUNGLE_PLANKS, 160, 115, 80);
        register(Blocks.JUNGLE_LOG, 85, 67, 25);
        register(Blocks.STRIPPED_JUNGLE_LOG, 172, 127, 83);
        register(Blocks.COMPOSTER, 113, 83, 53);
        register(Blocks.NOTE_BLOCK, 100, 68, 46);
        register(Blocks.JUKEBOX, 100, 68, 46);
        register(Blocks.BOOKSHELF, 117, 86, 54);

        // --- REDS & PINKS ---
        register(Blocks.RED_CONCRETE, 142, 32, 32);
        register(Blocks.RED_WOOL, 160, 39, 34);
        register(Blocks.RED_TERRACOTTA, 143, 61, 46);
        register(Blocks.NETHERRACK, 100, 30, 30);
        register(Blocks.NETHER_BRICKS, 44, 21, 26);
        register(Blocks.RED_NETHER_BRICKS, 73, 16, 18);
        register(Blocks.NETHER_WART_BLOCK, 114, 2, 2);
        register(Blocks.MANGROVE_PLANKS, 118, 54, 51);
        register(Blocks.MANGROVE_LOG, 85, 36, 32);
        register(Blocks.STRIPPED_MANGROVE_LOG, 138, 72, 63);
        register(Blocks.PINK_CONCRETE, 213, 101, 142);
        register(Blocks.PINK_WOOL, 237, 141, 172);
        register(Blocks.PINK_TERRACOTTA, 161, 77, 78);
        register(Blocks.CHERRY_PLANKS, 226, 170, 183);
        register(Blocks.CHERRY_LOG, 225, 171, 184); // Inside color approx
        register(Blocks.STRIPPED_CHERRY_LOG, 229, 180, 192);

        // --- ORANGES & YELLOWS ---
        register(Blocks.ORANGE_CONCRETE, 224, 97, 0);
        register(Blocks.ORANGE_WOOL, 240, 118, 19);
        register(Blocks.ORANGE_TERRACOTTA, 161, 83, 37);
        register(Blocks.RED_SAND, 191, 103, 33);
        register(Blocks.RED_SANDSTONE, 181, 97, 31);
        register(Blocks.ACACIA_PLANKS, 168, 90, 50);
        register(Blocks.STRIPPED_ACACIA_LOG, 173, 92, 50);
        register(Blocks.PUMPKIN, 218, 126, 31);
        register(Blocks.HONEYCOMB_BLOCK, 229, 148, 29);
        register(Blocks.COPPER_BLOCK, 192, 107, 79);
        register(Blocks.CUT_COPPER, 192, 107, 79);
        register(Blocks.TERRACOTTA, 152, 94, 67);
        register(Blocks.YELLOW_CONCRETE, 240, 175, 21);
        register(Blocks.YELLOW_WOOL, 248, 197, 39);
        register(Blocks.YELLOW_TERRACOTTA, 186, 133, 35);
        register(Blocks.GOLD_BLOCK, 246, 208, 61);
        register(Blocks.RAW_GOLD_BLOCK, 228, 178, 62);
        register(Blocks.SAND, 219, 211, 160);
        register(Blocks.SANDSTONE, 216, 203, 155);
        register(Blocks.SMOOTH_SANDSTONE, 216, 203, 155);
        register(Blocks.BIRCH_PLANKS, 196, 179, 123);
        register(Blocks.STRIPPED_BIRCH_LOG, 199, 182, 128);
        register(Blocks.END_STONE, 221, 223, 165);
        register(Blocks.END_STONE_BRICKS, 221, 223, 165);
        register(Blocks.HAY_BLOCK, 172, 141, 8);
        register(Blocks.SPONGE, 195, 188, 73);
        register(Blocks.WET_SPONGE, 170, 173, 59);
        register(Blocks.BAMBOO_PLANKS, 212, 196, 108);

        // --- GREENS & LIMES ---
        register(Blocks.GREEN_CONCRETE, 73, 91, 36);
        register(Blocks.GREEN_WOOL, 84, 109, 27);
        register(Blocks.GREEN_TERRACOTTA, 76, 83, 42);
        register(Blocks.MOSS_BLOCK, 88, 108, 45);
        register(Blocks.OAK_LEAVES, 55, 100, 30); // Approx default
        register(Blocks.SPRUCE_LEAVES, 62, 94, 62);
        register(Blocks.JUNGLE_LEAVES, 44, 154, 30);
        register(Blocks.DRIED_KELP_BLOCK, 56, 62, 40);
        register(Blocks.LIME_CONCRETE, 94, 169, 24);
        register(Blocks.LIME_WOOL, 112, 185, 25);
        register(Blocks.LIME_TERRACOTTA, 103, 117, 53);
        register(Blocks.EMERALD_BLOCK, 46, 204, 113);
        register(Blocks.MELON, 118, 146, 38);

        // --- CYANS, TEALS & AQUAS ---
        register(Blocks.CYAN_CONCRETE, 21, 119, 136);
        register(Blocks.CYAN_WOOL, 21, 137, 145);
        register(Blocks.CYAN_TERRACOTTA, 87, 92, 92);
        register(Blocks.PRISMARINE, 99, 156, 151);
        register(Blocks.PRISMARINE_BRICKS, 99, 171, 162);
        register(Blocks.DARK_PRISMARINE, 51, 87, 82);
        register(Blocks.WARPED_PLANKS, 43, 104, 99);
        register(Blocks.WARPED_STEM, 58, 142, 140);
        register(Blocks.WARPED_HYPHAE, 58, 142, 140);
        register(Blocks.STRIPPED_WARPED_HYPHAE, 58, 142, 140);
        register(Blocks.OXIDIZED_COPPER, 86, 163, 147);
        register(Blocks.WEATHERED_COPPER, 109, 160, 130);
        register(Blocks.EXPOSED_COPPER, 161, 125, 103);
        register(Blocks.DIAMOND_BLOCK, 97, 217, 211);

        // --- BLUES ---
        register(Blocks.BLUE_CONCRETE, 44, 46, 143);
        register(Blocks.BLUE_WOOL, 53, 57, 157);
        register(Blocks.BLUE_TERRACOTTA, 74, 59, 91);
        register(Blocks.LAPIS_BLOCK, 30, 67, 140);
        register(Blocks.LIGHT_BLUE_CONCRETE, 35, 137, 198);
        register(Blocks.LIGHT_BLUE_WOOL, 58, 175, 217);
        register(Blocks.LIGHT_BLUE_TERRACOTTA, 113, 108, 137);
        register(Blocks.PACKED_ICE, 141, 180, 250);
        register(Blocks.BLUE_ICE, 116, 167, 253);

        // --- PURPLES & MAGENTAS ---
        register(Blocks.PURPLE_CONCRETE, 100, 31, 156);
        register(Blocks.PURPLE_WOOL, 121, 42, 172);
        register(Blocks.PURPLE_TERRACOTTA, 118, 69, 86);
        register(Blocks.AMETHYST_BLOCK, 133, 97, 191);
        register(Blocks.PURPUR_BLOCK, 169, 125, 169);
        register(Blocks.PURPUR_PILLAR, 171, 129, 171);
        register(Blocks.MAGENTA_CONCRETE, 169, 48, 159);
        register(Blocks.MAGENTA_WOOL, 189, 68, 179);
        register(Blocks.MAGENTA_TERRACOTTA, 149, 88, 108);
        register(Blocks.CRIMSON_PLANKS, 101, 31, 51);
        register(Blocks.CRIMSON_STEM, 101, 31, 51);
        register(Blocks.STRIPPED_CRIMSON_STEM, 141, 58, 80);
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
        boolean hasColor = inputSaturation > 0.05f;

        for (Map.Entry<BlockState, Color> entry : COLOR_PALETTE.entrySet()) {
            BlockState state = entry.getKey();
            Color target = entry.getValue();

            double dR = target.getRed() - r;
            double dG = target.getGreen() - g;
            double dB = target.getBlue() - b;

            double distance = (3.0 * dR * dR) + (4.0 * dG * dG) + (2.0 * dB * dB);

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