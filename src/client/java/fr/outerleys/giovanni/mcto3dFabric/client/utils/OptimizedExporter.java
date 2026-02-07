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
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class OptimizedExporter {

    public static final String MODE_STL = "STL";
    public static final String MODE_OBJ_COLOR = "OBJ_COLOR";
    public static final String MODE_OBJ_TEXTURE = "OBJ_TEXTURE";

    private static class MergedQuad {
        double minX, minY, minZ, maxX, maxY, maxZ;
        Direction face;
        BlockState state;
        int width, length;

        MergedQuad(double x1, double y1, double z1, double x2, double y2, double z2, Direction face, BlockState state, int w, int l) {
            this.minX = x1; this.minY = y1; this.minZ = z1;
            this.maxX = x2; this.maxY = y2; this.maxZ = z2;
            this.face = face; this.state = state;
            this.width = w; this.length = l;
        }
    }

    public void export(Cuboid cuboid, World world, File baseFile, String mode, boolean solidify) throws IOException {
        System.out.println("--- START OPTIMIZED EXPORT (" + mode + ") [Solidify: " + solidify + "] ---");

        // 1. Generate main faces (Greedy Meshing)
        List<MergedQuad> quads = meshChunk(cuboid, world, mode, solidify);

        // 2. NON-MANIFOLD FIX (Micro-connectors)
        // Adds tiny geometry in diagonals to ensure the model is printable (slicers dislike touching edges)
        addDiagonalFixes(quads, cuboid, world, solidify);

        System.out.println("-> Faces generated (with fix): " + quads.size());

        if (mode.equals(MODE_STL)) {
            writeBinaryStl(quads, baseFile);
        } else {
            writeObj(quads, baseFile, mode, world);
        }
    }

    // --- DIAGONAL SCANNER ---
    private void addDiagonalFixes(List<MergedQuad> quads, Cuboid c, World world, boolean solidify) {
        // Need a temporary HeightMap to know where the solid part ends
        int sizeX = c.getMaxX() - c.getMinX() + 1;
        int sizeZ = c.getMaxZ() - c.getMinZ() + 1;
        int[][] heightMap = new int[sizeX][sizeZ];

        if (solidify) {
            for (int[] row : heightMap) Arrays.fill(row, Integer.MIN_VALUE);
            BlockPos.Mutable p = new BlockPos.Mutable();
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    int realX = c.getMinX() + x;
                    int realZ = c.getMinZ() + z;
                    for (int y = c.getMaxY(); y >= c.getMinY(); y--) {
                        p.set(realX, y, realZ);
                        BlockState s = world.getBlockState(p);
                        if (!s.isAir() && !isPlantOrFlat(s)) { heightMap[x][z] = y; break; }
                    }
                }
            }
        }

        // Scan the cuboid for Checkerboard patterns
        // [A][ ]
        // [ ][B]
        // If A and B are solid, but the empty spaces are air, they need connection.

        double eps = 0.02; // Connector thickness (very thin)

        for (int x = c.getMinX(); x < c.getMaxX(); x++) { // < MaxX because we check x+1
            for (int z = c.getMinZ(); z < c.getMaxZ(); z++) { // < MaxZ because we check z+1
                for (int y = c.getMinY(); y <= c.getMaxY(); y++) {

                    // Get effective state of the 4 blocks
                    boolean b00 = isSolidEffective(world, x, y, z, c, heightMap, solidify);     // x, z
                    boolean b11 = isSolidEffective(world, x+1, y, z+1, c, heightMap, solidify); // x+1, z+1
                    boolean b10 = isSolidEffective(world, x+1, y, z, c, heightMap, solidify);   // x+1, z
                    boolean b01 = isSolidEffective(world, x, y, z+1, c, heightMap, solidify);   // x, z+1

                    // Case 1: Diagonal /
                    // [ ][X]
                    // [X][ ]
                    if (b10 && b01 && !b00 && !b11) {
                        // Add micro pillar at center
                        addMicroConnector(quads, x, y, z, c, 1.0, 0.0, 1.0, 0.0, eps);
                    }

                    // Case 2: Diagonal \
                    // [X][ ]
                    // [ ][X]
                    if (b00 && b11 && !b10 && !b01) {
                        addMicroConnector(quads, x, y, z, c, 1.0, 1.0, 1.0, 1.0, eps);
                    }
                }
            }
        }
    }

    private void addMicroConnector(List<MergedQuad> quads, int x, int y, int z, Cuboid c, double offX, double offZ, double centerX, double centerZ, double eps) {
        // Create a small junction cube
        // Relative coords
        double rx = x - c.getMinX();
        double ry = y - c.getMinY();
        double rz = z - c.getMinZ();

        // Position exactly at intersection
        double cx = rx + offX;
        double cz = rz + offZ;

        // Micro cube centered on the problematic edge
        double minX = cx - eps; double maxX = cx + eps;
        double minZ = cz - eps; double maxZ = cz + eps;
        double minY = ry;       double maxY = ry + 1.0;

        // Add 6 faces for this micro-cube
        // Using Stone as filler material
        BlockState filler = Blocks.STONE.getDefaultState();

        quads.add(new MergedQuad(minX, minY, minZ, maxX, maxY, maxZ, Direction.UP, filler, 1, 1));
        quads.add(new MergedQuad(minX, minY, minZ, maxX, maxY, maxZ, Direction.DOWN, filler, 1, 1));
        quads.add(new MergedQuad(minX, minY, minZ, maxX, maxY, maxZ, Direction.NORTH, filler, 1, 1));
        quads.add(new MergedQuad(minX, minY, minZ, maxX, maxY, maxZ, Direction.SOUTH, filler, 1, 1));
        quads.add(new MergedQuad(minX, minY, minZ, maxX, maxY, maxZ, Direction.EAST, filler, 1, 1));
        quads.add(new MergedQuad(minX, minY, minZ, maxX, maxY, maxZ, Direction.WEST, filler, 1, 1));
    }

    private boolean isSolidEffective(World world, int x, int y, int z, Cuboid c, int[][] heightMap, boolean solidify) {
        BlockState s = getEffectiveState(world, x, y, z, c, heightMap, solidify);
        // Considered solid if not air and not a plant (so a full cube)
        return !s.isAir() && !isPlantOrFlat(s);
    }

    // --- CORE SYSTEM (Greedy Meshing) ---
    private List<MergedQuad> meshChunk(Cuboid c, World world, String mode, boolean solidify) {
        List<MergedQuad> result = new ArrayList<>();

        // 1. PRE-CALCULATE HEIGHTMAP
        int sizeX = c.getMaxX() - c.getMinX() + 1;
        int sizeZ = c.getMaxZ() - c.getMinZ() + 1;
        int[][] heightMap = new int[sizeX][sizeZ];

        if (solidify) {
            for (int[] row : heightMap) Arrays.fill(row, Integer.MIN_VALUE);
            BlockPos.Mutable p = new BlockPos.Mutable();
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    int realX = c.getMinX() + x;
                    int realZ = c.getMinZ() + z;
                    for (int y = c.getMaxY(); y >= c.getMinY(); y--) {
                        p.set(realX, y, realZ);
                        BlockState s = world.getBlockState(p);
                        if (!s.isAir() && !isPlantOrFlat(s)) { heightMap[x][z] = y; break; }
                    }
                }
            }
        }

        // 2. GREEDY MESHING LOOP
        for (Direction dir : Direction.values()) {
            boolean isY = dir.getAxis() == Direction.Axis.Y;
            boolean isX = dir.getAxis() == Direction.Axis.X;

            int uMin = isX ? c.getMinY() : c.getMinX();
            int uMax = isX ? c.getMaxY() : c.getMaxX();
            int vMin = isY ? c.getMinZ() : c.getMinZ();
            if (dir.getAxis() == Direction.Axis.Z) vMin = c.getMinY();
            int vMax = isY ? c.getMaxZ() : c.getMaxZ();
            if (dir.getAxis() == Direction.Axis.Z) vMax = c.getMaxY();

            int dMin = isY ? c.getMinY() : (isX ? c.getMinX() : c.getMinZ());
            int dMax = isY ? c.getMaxY() : (isX ? c.getMaxX() : c.getMaxZ());

            for (int d = dMin; d <= dMax; d++) {
                for (int v = vMin; v <= vMax; v++) {
                    BlockState lastState = null;
                    int runStartU = -1;

                    for (int u = uMin; u <= uMax + 1; u++) {
                        BlockState currentState = null;

                        if (u <= uMax) {
                            int x, y, z;
                            if (isY) { x = u; y = d; z = v; }
                            else if (isX) { x = d; y = u; z = v; }
                            else { x = u; y = v; z = d; }

                            BlockState s = getEffectiveState(world, x, y, z, c, heightMap, solidify);

                            if (!s.isAir()) {
                                int nX = x + dir.getOffsetX();
                                int nY = y + dir.getOffsetY();
                                int nZ = z + dir.getOffsetZ();
                                BlockState neighborState = getEffectiveState(world, nX, nY, nZ, c, heightMap, solidify);

                                // Optimization: Only mesh if neighbor is not fully opaque
                                if (!neighborState.isOpaqueFullCube()) {
                                    currentState = s;
                                }
                            }
                        }

                        boolean matches = false;
                        if (lastState != null && currentState != null) {
                            if (lastState.getBlock() == currentState.getBlock()) {
                                matches = true;
                                if (mode.equals(MODE_OBJ_COLOR)) {
                                    matches = (lastState.getMapColor(world, null) == currentState.getMapColor(world, null));
                                }
                            }
                        }

                        if (!matches) {
                            if (lastState != null) {
                                int lengthRect = u - runStartU;
                                addQuad(result, dir, lastState, isX, isY, d, v, runStartU, u, c, lengthRect);
                            }
                            lastState = currentState;
                            runStartU = u;
                        }
                    }
                }
            }
        }
        return result;
    }

    private BlockState getEffectiveState(World world, int x, int y, int z, Cuboid c, int[][] heightMap, boolean solidify) {
        if (x < c.getMinX() || x > c.getMaxX() ||
                y < c.getMinY() || y > c.getMaxY() ||
                z < c.getMinZ() || z > c.getMaxZ()) {
            return Blocks.AIR.getDefaultState();
        }

        BlockPos pos = new BlockPos(x, y, z);
        BlockState realState = world.getBlockState(pos);

        if (solidify) {
            int relX = x - c.getMinX();
            int relZ = z - c.getMinZ();
            if ((realState.isAir() || isPlantOrFlat(realState)) && heightMap != null) {
                if (y < heightMap[relX][relZ]) {
                    return Blocks.STONE.getDefaultState();
                }
            }
        }
        return realState;
    }

    private void addQuad(List<MergedQuad> result, Direction dir, BlockState state, boolean isX, boolean isY, int d, int v, int startU, int endU, Cuboid c, int length) {
        int sX=0, sY=0, sZ=0, eX=0, eY=0, eZ=0;
        if (isY) { sX=startU; sY=d; sZ=v;  eX=endU; eY=d+1; eZ=v+1; if(dir==Direction.DOWN) {eY=d; sY=d;} else {sY=d+1;eY=d+1;} }
        else if (isX) { sX=d; sY=startU; sZ=v; eX=d+1; eY=endU; eZ=v+1; if(dir==Direction.WEST) {eX=d; sX=d;} else {sX=d+1;eX=d+1;} }
        else { sX=startU; sY=v; sZ=d; eX=endU; eY=v+1; eZ=d+1; if(dir==Direction.NORTH) {eZ=d; sZ=d;} else {sZ=d+1;eZ=d+1;} }

        double minX = Math.min(sX, eX); double maxX = Math.max(sX, eX);
        double minY = Math.min(sY, eY); double maxY = Math.max(sY, eY);
        double minZ = Math.min(sZ, eZ); double maxZ = Math.max(sZ, eZ);

        // Adjust coordinates based on face direction to create a flat quad
        if (dir == Direction.UP) { minY = d + 1; maxY = d + 1; }
        if (dir == Direction.DOWN) { minY = d; maxY = d; }
        if (dir == Direction.EAST) { minX = d + 1; maxX = d + 1; }
        if (dir == Direction.WEST) { minX = d; maxX = d; }
        if (dir == Direction.SOUTH) { minZ = d + 1; maxZ = d + 1; }
        if (dir == Direction.NORTH) { minZ = d; maxZ = d; }

        if (dir == Direction.UP || dir == Direction.DOWN) { maxX = endU; minX = startU; maxZ = v + 1; minZ = v; }
        if (dir == Direction.EAST || dir == Direction.WEST) { maxY = endU; minY = startU; maxZ = v + 1; minZ = v; }
        if (dir == Direction.SOUTH || dir == Direction.NORTH) { maxX = endU; minX = startU; maxY = v + 1; minY = v; }

        minX -= c.getMinX(); maxX -= c.getMinX();
        minY -= c.getMinY(); maxY -= c.getMinY();
        minZ -= c.getMinZ(); maxZ -= c.getMinZ();

        result.add(new MergedQuad(minX, minY, minZ, maxX, maxY, maxZ, dir, state, length, 1));
    }

    private boolean isPlantOrFlat(BlockState s) {
        return s.getBlock() instanceof PlantBlock || s.getBlock() instanceof TorchBlock;
    }

    // --- WRITERS ---

    private void writeBinaryStl(List<MergedQuad> quads, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(new byte[80]); // 80-byte header
            ByteBuffer countBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            fos.write(countBuf.putInt(quads.size() * 2).array()); // *2 because 1 quad = 2 triangles
            ByteBuffer buf = ByteBuffer.allocate(50).order(ByteOrder.LITTLE_ENDIAN);
            for (MergedQuad q : quads) writeQuadToStl(fos, buf, q);
        }
    }

    private void writeQuadToStl(FileOutputStream fos, ByteBuffer buf, MergedQuad q) throws IOException {
        float nx = q.face.getOffsetX(); float ny = q.face.getOffsetY(); float nz = q.face.getOffsetZ();
        float x1,y1,z1, x2,y2,z2, x3,y3,z3, x4,y4,z4;

        // Define corners based on axis
        if (q.face.getAxis() == Direction.Axis.Y) {
            x1 = (float)q.minX; z1 = (float)q.minZ; x2 = (float)q.maxX; z2 = (float)q.minZ;
            x3 = (float)q.maxX; z3 = (float)q.maxZ; x4 = (float)q.minX; z4 = (float)q.maxZ;
            y1=y2=y3=y4 = (float)q.minY;
        } else if (q.face.getAxis() == Direction.Axis.Z) {
            x1 = (float)q.minX; y1 = (float)q.minY; x2 = (float)q.maxX; y2 = (float)q.minY;
            x3 = (float)q.maxX; y3 = (float)q.maxY; x4 = (float)q.minX; y4 = (float)q.maxY;
            z1=z2=z3=z4 = (float)q.minZ;
        } else {
            z1 = (float)q.minZ; y1 = (float)q.minY; z2 = (float)q.maxZ; y2 = (float)q.minY;
            z3 = (float)q.maxZ; y3 = (float)q.maxY; z4 = (float)q.minZ; y4 = (float)q.maxY;
            x1=x2=x3=x4 = (float)q.minX;
        }

        // Write two triangles
        writeTriangle(fos, buf, x1,y1,z1, x2,y2,z2, x3,y3,z3, nx,ny,nz);
        writeTriangle(fos, buf, x1,y1,z1, x3,y3,z3, x4,y4,z4, nx,ny,nz);
    }

    private void writeTriangle(FileOutputStream fos, ByteBuffer buf, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float nx, float ny, float nz) throws IOException {
        buf.clear();
        buf.putFloat(nx); buf.putFloat(ny); buf.putFloat(nz);
        buf.putFloat(x1); buf.putFloat(y1); buf.putFloat(z1);
        buf.putFloat(x2); buf.putFloat(y2); buf.putFloat(z2);
        buf.putFloat(x3); buf.putFloat(y3); buf.putFloat(z3);
        buf.putShort((short) 0); // Attribute byte count
        fos.write(buf.array());
    }

    private void writeObj(List<MergedQuad> quads, File baseFile, String mode, World world) throws IOException {
        File objFile = new File(baseFile.getParent(), baseFile.getName() + ".obj");
        File mtlFile = new File(baseFile.getParent(), baseFile.getName() + ".mtl");
        File texDir = new File(baseFile.getParent(), baseFile.getName() + "_textures");

        if (mode.equals(MODE_OBJ_TEXTURE) && !texDir.exists()) texDir.mkdirs();

        Set<String> materials = new HashSet<>();
        int vertexOffset = 1; // OBJ indices start at 1

        try (BufferedWriter w = new BufferedWriter(new FileWriter(objFile));
             BufferedWriter mtl = new BufferedWriter(new FileWriter(mtlFile))) {

            w.write("mtllib " + mtlFile.getName() + "\n");

            for (MergedQuad q : quads) {
                String matName = getMaterialName(q.state, world, q.face, mode);

                if (!materials.contains(matName)) {
                    writeMaterial(mtl, matName, q.state, world, mode, texDir, baseFile.getName() + "_textures");
                    materials.add(matName);
                }

                writeObjVertex(w, q); // Writes 4 vertices

                // UV Mapping
                float uScale = mode.equals(MODE_OBJ_TEXTURE) ? q.width : 1.0f;
                float vScale = mode.equals(MODE_OBJ_TEXTURE) ? q.length : 1.0f;

                w.write(String.format(Locale.US, "vt 0.0 0.0\n"));
                w.write(String.format(Locale.US, "vt %f 0.0\n", uScale));
                w.write(String.format(Locale.US, "vt %f %f\n", uScale, vScale));
                w.write(String.format(Locale.US, "vt 0.0 %f\n", vScale));

                w.write(String.format(Locale.US, "vn %d %d %d\n", q.face.getOffsetX(), q.face.getOffsetY(), q.face.getOffsetZ()));

                w.write("usemtl " + matName + "\n");

                // Face indices (v/vt/vn)
                // -4 is the 4th vertex from the end, etc.
                // Using negative indices is cleaner when writing linearly
                w.write("f -4/-4/-1 -3/-3/-1 -2/-2/-1 -1/-1/-1\n");
            }
        }
    }

    private void writeObjVertex(BufferedWriter w, MergedQuad q) throws IOException {
        double x1,y1,z1, x2,y2,z2, x3,y3,z3, x4,y4,z4;

        // Define vertices counter-clockwise for proper normal direction in OBJ
        if (q.face.getAxis() == Direction.Axis.Y) {
            y1=y2=y3=y4 = q.minY;
            if (q.face == Direction.UP) { x1=q.minX; z1=q.maxZ;  x2=q.maxX; z2=q.maxZ; x3=q.maxX; z3=q.minZ; x4=q.minX; z4=q.minZ; }
            else { x1=q.minX; z1=q.minZ;  x2=q.maxX; z2=q.minZ; x3=q.maxX; z3=q.maxZ; x4=q.minX; z4=q.maxZ; }
        } else if (q.face.getAxis() == Direction.Axis.Z) {
            z1=z2=z3=z4 = q.minZ;
            if (q.face == Direction.SOUTH) { x1=q.minX; y1=q.minY; x2=q.maxX; y2=q.minY; x3=q.maxX; y3=q.maxY; x4=q.minX; y4=q.maxY; }
            else { x1=q.maxX; y1=q.minY; x2=q.minX; y2=q.minY; x3=q.minX; y3=q.maxY; x4=q.maxX; y4=q.maxY; }
        } else {
            x1=x2=x3=x4 = q.minX;
            if (q.face == Direction.EAST) { z1=q.maxZ; y1=q.minY; z2=q.minZ; y2=q.minY; z3=q.minZ; y3=q.maxY; z4=q.maxZ; y4=q.maxY; }
            else { z1=q.minZ; y1=q.minY; z2=q.maxZ; y2=q.minY; z3=q.maxZ; y3=q.maxY; z4=q.minZ; y4=q.maxY; }
        }

        w.write(String.format(Locale.US, "v %f %f %f\n", x1,y1,z1));
        w.write(String.format(Locale.US, "v %f %f %f\n", x2,y2,z2));
        w.write(String.format(Locale.US, "v %f %f %f\n", x3,y3,z3));
        w.write(String.format(Locale.US, "v %f %f %f\n", x4,y4,z4));
    }

    private String getMaterialName(BlockState state, World world, Direction face, String mode) {
        String name = "mat_" + Registries.BLOCK.getId(state.getBlock()).getPath();
        if (mode.equals(MODE_OBJ_TEXTURE)) {
            int color = -1;
            try { color = MinecraftClient.getInstance().getBlockColors().getColor(state, world, null, 0); } catch(Exception e){}
            if (color != -1) name += "_" + Integer.toHexString(color);
        }
        return name;
    }

    private void writeMaterial(BufferedWriter mtl, String name, BlockState state, World world, String mode, File texDir, String dirName) throws IOException {
        mtl.write("newmtl " + name + "\n");
        if (mode.equals(MODE_OBJ_COLOR)) {
            int c = state.getMapColor(world, null).color;
            float r = ((c >> 16) & 0xFF) / 255f; float g = ((c >> 8) & 0xFF) / 255f; float b = (c & 0xFF) / 255f;
            mtl.write(String.format(Locale.US, "Kd %f %f %f\n", r, g, b));
        } else {
            mtl.write("Kd 1.0 1.0 1.0\n");
            try {
                // Using 'var' to handle potential mapping differences for BakedModel
                var model = MinecraftClient.getInstance().getBlockRenderManager().getModel(state);
                Sprite s = model.particleSprite();

                if (s != null) {
                    String texName = s.getContents().getId().getPath().replace("/", "_") + ".png";

                    // Call the helper method to save texture
                    // Note: Passing -1 for tint color if we want raw texture, or the computed color if needed
                    // For simplicity in optimized mode, we often skip tinting or do it simply.
                    // Here we pass 0 or a color if retrieved.
                    int tint = -1;
                    try { tint = MinecraftClient.getInstance().getBlockColors().getColor(state, world, null, 0); } catch(Exception e){}

                    saveSprite(s, new File(texDir, texName), tint);
                    mtl.write("map_Kd " + dirName + "/" + texName + "\n");
                }
            } catch(Exception e) {}
        }
        mtl.write("\n");
    }

    private void saveSprite(Sprite sprite, File outputFile, int tintColor) {
        if (outputFile.exists()) return;

        try {
            Identifier spriteId = sprite.getContents().getId();
            Identifier resourceLocation = Identifier.of(
                    spriteId.getNamespace(),
                    "textures/" + spriteId.getPath() + ".png"
            );

            var resource = MinecraftClient.getInstance().getResourceManager().getResource(resourceLocation);

            if (resource.isPresent()) {
                try (java.io.InputStream inputStream = resource.get().getInputStream()) {
                    NativeImage originalImage = NativeImage.read(inputStream);

                    if (tintColor != -1) {
                        int w = originalImage.getWidth();
                        int h = originalImage.getHeight();

                        try (NativeImage tintedImage = new NativeImage(w, h, true)) {
                            int tintR = (tintColor >> 16) & 0xFF;
                            int tintG = (tintColor >> 8) & 0xFF;
                            int tintB = (tintColor) & 0xFF;

                            for (int x = 0; x < w; x++) {
                                for (int y = 0; y < h; y++) {
                                    int pixelColor = originalImage.getColorArgb(x, y);

                                    int alpha = (pixelColor >> 24) & 0xFF;
                                    int red = (pixelColor >> 16) & 0xFF;
                                    int green = (pixelColor >> 8) & 0xFF;
                                    int blue = (pixelColor) & 0xFF;

                                    if (alpha > 0) {
                                        int newR = (red * tintR) / 255;
                                        int newG = (green * tintG) / 255;
                                        int newB = (blue * tintB) / 255;
                                        int newPixel = (alpha << 24) | (newR << 16) | (newG << 8) | newB;
                                        tintedImage.setColorArgb(x, y, newPixel);
                                    } else {
                                        tintedImage.setColorArgb(x, y, 0);
                                    }
                                }
                            }
                            tintedImage.writeTo(outputFile.toPath());
                        }
                    } else {
                        originalImage.writeTo(outputFile.toPath());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error saving texture: " + outputFile.getName());
        }
    }
}