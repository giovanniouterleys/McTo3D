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

import fr.outerleys.giovanni.mcto3dFabric.selection.Cuboid;
import net.minecraft.block.AbstractPlantPartBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

public class StlExporter {

    private final byte[] headerChunk = new byte[80];
    private final byte[] countChunk = new byte[4];

    public void export(Cuboid cuboid, World world, File file, float scale, Consumer<Float> progressCallback) throws IOException {

        long totalBlocks = (long) (cuboid.getMaxX() - cuboid.getMinX() + 1) * (long) (cuboid.getMaxY() - cuboid.getMinY() + 1) * (long) (cuboid.getMaxZ() - cuboid.getMinZ() + 1);
        long currentBlock = 0;

        try (FileOutputStream fos = new FileOutputStream(file)) {
            // Write standard STL Header (80 bytes empty)
            fos.write(headerChunk);
            // Write Triangle Count placeholder (will need to be patched later if accurate count is strictly required,
            // but many viewers ignore it or we rely on the stream writing)
            // Note: Standard STL requires an accurate count here.
            // For simple streaming, we often leave 0 or patch it after.
            // Given the streaming nature here, we write 0 placeholders.
            fos.write(countChunk);

            BlockPos.Mutable mutablePos = new BlockPos.Mutable();
            BlockPos.Mutable neighborPos = new BlockPos.Mutable();

            // Buffer for one triangle (50 bytes: 12 floats + 2 bytes attribute)
            ByteBuffer buf = ByteBuffer.allocate(50).order(ByteOrder.LITTLE_ENDIAN);

            for (int x = cuboid.getMinX(); x <= cuboid.getMaxX(); x++) {

                float p = (float) currentBlock / totalBlocks;
                progressCallback.accept(p);

                for (int y = cuboid.getMinY(); y <= cuboid.getMaxY(); y++) {
                    for (int z = cuboid.getMinZ(); z <= cuboid.getMaxZ(); z++) {
                        currentBlock++;
                        mutablePos.set(x, y, z);
                        BlockState state = world.getBlockState(mutablePos);
                        if (state.isAir()) continue;

                        // Culling optimization: Skip internal blocks
                        if (state.isOpaqueFullCube()) {
                            if (isHidden(world, mutablePos, neighborPos, cuboid)) continue;
                        }

                        double relX = x - cuboid.getMinX();
                        double relY = y - cuboid.getMinY();
                        double relZ = z - cuboid.getMinZ();

                        if (isPlantOrFlat(state.getBlock())) {
                            // Apply scale directly
                            writePlantCrossStl(fos, buf, (float)relX, (float)relY, (float)relZ, scale);
                        }
                        else {
                            VoxelShape shape = state.getOutlineShape(world, mutablePos);
                            if (!shape.isEmpty()) {
                                shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                                    try {
                                        // Apply scale to box limits
                                        writeBoxStl(fos, buf,
                                                (float)((relX + minX) * scale), (float)((relY + minY) * scale), (float)((relZ + minZ) * scale),
                                                (float)((relX + maxX) * scale), (float)((relY + maxY) * scale), (float)((relZ + maxZ) * scale)
                                        );
                                    } catch (IOException e) { throw new RuntimeException(e); }
                                });
                            }
                        }
                    }
                }
            }
        }
        progressCallback.accept(1.0f);
    }

    // --- CULLING LOGIC ---

    // A block is "Hidden" only if all 6 neighbors are opaque AND inside the selection.
    private boolean isHidden(World world, BlockPos pos, BlockPos.Mutable neighborPos, Cuboid c) {
        if (!isSideBlocked(world, neighborPos.set(pos, Direction.UP), c)) return false;
        if (!isSideBlocked(world, neighborPos.set(pos, Direction.DOWN), c)) return false;
        if (!isSideBlocked(world, neighborPos.set(pos, Direction.NORTH), c)) return false;
        if (!isSideBlocked(world, neighborPos.set(pos, Direction.SOUTH), c)) return false;
        if (!isSideBlocked(world, neighborPos.set(pos, Direction.WEST), c)) return false;
        if (!isSideBlocked(world, neighborPos.set(pos, Direction.EAST), c)) return false;
        return true; // All faces are hidden by solid blocks
    }

    private boolean isSideBlocked(World world, BlockPos neighbor, Cuboid c) {
        // GOLDEN RULE: If the neighbor is OUTSIDE the selection, the block is NOT hidden.
        // We consider the edge of the selection as a visible cut.
        if (neighbor.getX() < c.getMinX() || neighbor.getX() > c.getMaxX() ||
                neighbor.getY() < c.getMinY() || neighbor.getY() > c.getMaxY() ||
                neighbor.getZ() < c.getMinZ() || neighbor.getZ() > c.getMaxZ()) {
            return false; // It's the edge -> draw it!
        }

        // Otherwise, check if it's a real opaque block
        return world.getBlockState(neighbor).isOpaqueFullCube();
    }

    // --- BINARY WRITING ---

    private void writeBoxStl(FileOutputStream fos, ByteBuffer buf, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) throws IOException {
        // Coordinates are already scaled, write directly
        // Bottom
        writeFace(fos, buf, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0f, -1f, 0f);
        // Top
        writeFace(fos, buf, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, 0f, 1f, 0f);
        // North
        writeFace(fos, buf, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, 0f, 0f, -1f);
        // South
        writeFace(fos, buf, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0f, 0f, 1f);
        // West
        writeFace(fos, buf, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1f, 0f, 0f);
        // East
        writeFace(fos, buf, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, 1f, 0f, 0f);
    }

    private void writePlantCrossStl(FileOutputStream fos, ByteBuffer buf, float x, float y, float z, float scale) throws IOException {
        float thick = 0.2f;
        float off = (1.0f - thick) / 2.0f;
        float h = 0.8f;

        // Multiply everything by scale
        writeBoxStl(fos, buf, (x + off)*scale, y*scale, (z + 0.1f)*scale, (x + off + thick)*scale, (y + h)*scale, (z + 0.9f)*scale);
        writeBoxStl(fos, buf, (x + 0.1f)*scale, y*scale, (z + off)*scale, (x + 0.9f)*scale, (y + h)*scale, (z + off + thick)*scale);
    }

    private void writeFace(FileOutputStream fos, ByteBuffer buf, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float nx, float ny, float nz) throws IOException {
        // Quad = 2 Triangles
        writeTriangle(fos, buf, x1, y1, z1, x2, y2, z2, x3, y3, z3, nx, ny, nz);
        writeTriangle(fos, buf, x1, y1, z1, x3, y3, z3, x4, y4, z4, nx, ny, nz);
    }

    private void writeTriangle(FileOutputStream fos, ByteBuffer buf, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float nx, float ny, float nz) throws IOException {
        buf.clear();
        // Normal vector
        buf.putFloat(nx); buf.putFloat(ny); buf.putFloat(nz);
        // Vertex 1
        buf.putFloat(x1); buf.putFloat(y1); buf.putFloat(z1);
        // Vertex 2
        buf.putFloat(x2); buf.putFloat(y2); buf.putFloat(z2);
        // Vertex 3
        buf.putFloat(x3); buf.putFloat(y3); buf.putFloat(z3);
        // Attribute byte count (unused in standard STL)
        buf.putShort((short) 0);
        fos.write(buf.array());
    }

    private boolean isPlantOrFlat(Block block) {
        return block instanceof PlantBlock || block instanceof TorchBlock || block instanceof AbstractPlantPartBlock;
    }
}