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

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import java.util.Map;

public class ImportManager {

    public static boolean isActive = false;
    public static Map<BlockPos, BlockState> currentVoxels = null;

    // Hologram parameters
    public static double distance = 30.0;
    public static int rotationSteps = 0; // 0 = 0°, 1 = 90°, 2 = 180°, etc.

    public static void clear() {
        isActive = false;
        currentVoxels = null;
        rotationSteps = 0;
        distance = 5.0;
    }

    // Checks if a block is "visible" (i.e., has at least one exposed face).
    // This prevents rendering the interior of the object (Massive optimization).
    public static boolean isEdge(BlockPos pos) {
        if (currentVoxels == null) return false;

        // If any neighbor is NOT in the map, then this block is on the edge
        if (!currentVoxels.containsKey(pos.up())) return true;
        if (!currentVoxels.containsKey(pos.down())) return true;
        if (!currentVoxels.containsKey(pos.north())) return true;
        if (!currentVoxels.containsKey(pos.south())) return true;
        if (!currentVoxels.containsKey(pos.east())) return true;
        if (!currentVoxels.containsKey(pos.west())) return true;

        return false; // Surrounded by blocks, hidden
    }
}