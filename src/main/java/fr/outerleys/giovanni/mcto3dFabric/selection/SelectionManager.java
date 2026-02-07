/*
 * McTo3D
 * Copyright (c) 2026 Giovanni Outerleys
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package fr.outerleys.giovanni.mcto3dFabric.selection;

import net.minecraft.util.math.BlockPos;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {
    private final Map<UUID, BlockPos> pos1 = new HashMap<>();
    private final Map<UUID, BlockPos> pos2 = new HashMap<>();
    // Storage for scale settings per player
    private final Map<UUID, Float> scales = new HashMap<>();

    public void setPos1(UUID id, BlockPos p) { pos1.put(id, p); }
    public void setPos2(UUID id, BlockPos p) { pos2.put(id, p); }

    // Set the export scale for a player
    public void setScale(UUID id, float scale) { scales.put(id, scale); }

    public BlockPos getPos1(UUID id) { return pos1.get(id); }
    public BlockPos getPos2(UUID id) { return pos2.get(id); }

    // Retrieve the scale (Default is 10.0f, meaning 1 block = 10mm = 1cm)
    public float getScale(UUID id) {
        return scales.getOrDefault(id, 10.0f);
    }

    public boolean hasSelection(UUID id) {
        return pos1.containsKey(id) && pos2.containsKey(id);
    }

    public Cuboid getSelection(UUID id) {
        if (!hasSelection(id)) return null;
        return new Cuboid(pos1.get(id), pos2.get(id));
    }
}