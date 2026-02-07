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

public class Cuboid {
    private final int xMin, yMin, zMin, xMax, yMax, zMax;

    public Cuboid(BlockPos p1, BlockPos p2) {
        this.xMin = Math.min(p1.getX(), p2.getX());
        this.yMin = Math.min(p1.getY(), p2.getY());
        this.zMin = Math.min(p1.getZ(), p2.getZ());
        this.xMax = Math.max(p1.getX(), p2.getX());
        this.yMax = Math.max(p1.getY(), p2.getY());
        this.zMax = Math.max(p1.getZ(), p2.getZ());
    }
    public int getMinX() { return xMin; }
    public int getMinY() { return yMin; }
    public int getMinZ() { return zMin; }
    public int getMaxX() { return xMax; }
    public int getMaxY() { return yMax; }
    public int getMaxZ() { return zMax; }
}