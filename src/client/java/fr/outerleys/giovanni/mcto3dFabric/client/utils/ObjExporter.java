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
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class ObjExporter {

    private int vertexCount = 1;
    private final Set<String> registeredMaterials = new HashSet<>();

    public static final String MODE_NONE = "VIDES";
    public static final String MODE_COLOR = "COULEUR";
    public static final String MODE_TEXTURES = "TEXTURES";

    public void export(Cuboid cuboid, World world, File baseFile, String mode, float scale, Consumer<Float> progressCallback) throws IOException {
        System.out.println("--- START EXPORT OBJ (" + mode + ") ---");

        long totalBlocks = (long) (cuboid.getMaxX() - cuboid.getMinX() + 1) * (long) (cuboid.getMaxY() - cuboid.getMinY() + 1) * (long) (cuboid.getMaxZ() - cuboid.getMinZ() + 1);
        long currentBlock = 0;

        vertexCount = 1;
        registeredMaterials.clear();

        File objFile = new File(baseFile.getParent(), baseFile.getName() + ".obj");
        File mtlFile = new File(baseFile.getParent(), baseFile.getName() + ".mtl");

        String textureDirName = baseFile.getName() + "_textures";
        File textureDir = new File(baseFile.getParent(), textureDirName);

        if (mode.equals(MODE_TEXTURES) && !textureDir.exists()) {
            textureDir.mkdirs();
        }

        try (BufferedWriter objWriter = new BufferedWriter(new FileWriter(objFile));
             BufferedWriter mtlWriter = new BufferedWriter(new FileWriter(mtlFile))) {

            objWriter.write("# Exported by McTo3D\n");
            objWriter.write("mtllib " + mtlFile.getName() + "\n");

            mtlWriter.write("# Material Library\n");
            writeMaterialColor(mtlWriter, "default_white", 1.0f, 1.0f, 1.0f);

            BlockPos.Mutable mutablePos = new BlockPos.Mutable();

            for (int x = cuboid.getMinX(); x <= cuboid.getMaxX(); x++) {

                // Update progress callback
                float p = (float) currentBlock / totalBlocks;
                progressCallback.accept(p);

                for (int y = cuboid.getMinY(); y <= cuboid.getMaxY(); y++) {
                    for (int z = cuboid.getMinZ(); z <= cuboid.getMaxZ(); z++) {
                        currentBlock++;
                        mutablePos.set(x, y, z);

                        BlockState state = world.getBlockState(mutablePos);
                        if (state.isAir()) continue;

                        double relX = x - cuboid.getMinX();
                        double relY = y - cuboid.getMinY();
                        double relZ = z - cuboid.getMinZ();

                        // --- MATERIAL HANDLING ---
                        int tintColor = -1;
                        if (mode.equals(MODE_TEXTURES)) {
                            try {
                                tintColor = MinecraftClient.getInstance().getBlockColors().getColor(state, world, mutablePos, 0);
                            } catch (Exception e) {
                                // Ignore color tint errors
                            }
                        }

                        String materialName = "mat_" + Registries.BLOCK.getId(state.getBlock()).getPath();
                        if (tintColor != -1) {
                            materialName += "_" + Integer.toHexString(tintColor);
                        }

                        if (!registeredMaterials.contains(materialName)) {
                            boolean materialWritten = false;

                            if (mode.equals(MODE_COLOR)) {
                                int colorInt = state.getMapColor(world, mutablePos).color;
                                float r = ((colorInt >> 16) & 0xFF) / 255.0f;
                                float g = ((colorInt >> 8) & 0xFF) / 255.0f;
                                float b = (colorInt & 0xFF) / 255.0f;
                                writeMaterialColor(mtlWriter, materialName, r, g, b);
                                materialWritten = true;

                            } else if (mode.equals(MODE_TEXTURES)) {
                                Sprite sprite = getSprite(state);
                                if (sprite != null) {
                                    try {
                                        String textureFilename = sprite.getContents().getId().getPath().replace("/", "_");
                                        if (tintColor != -1) textureFilename += "_tinted_" + Integer.toHexString(tintColor);

                                        File textureFile = new File(textureDir, textureFilename + ".png");
                                        if (!textureFile.exists()) {
                                            saveSprite(sprite, textureFile, tintColor);
                                        }

                                        writeMaterialTexture(mtlWriter, materialName, textureFilename, textureDirName);
                                        materialWritten = true;
                                    } catch(Exception e) {
                                        // Fallback if texture saving fails
                                    }
                                }
                            }

                            // Fallback to simple color map if texture failed or mode is NONE/COLOR fallback
                            if (!materialWritten) {
                                int colorInt = state.getMapColor(world, mutablePos).color;
                                float r = ((colorInt >> 16) & 0xFF) / 255.0f;
                                float g = ((colorInt >> 8) & 0xFF) / 255.0f;
                                float b = (colorInt & 0xFF) / 255.0f;
                                writeMaterialColor(mtlWriter, materialName, r, g, b);
                            }
                            registeredMaterials.add(materialName);
                        }

                        String currentMat = registeredMaterials.contains(materialName) ? materialName : "default_white";

                        // --- GEOMETRY GENERATION ---
                        if (isPlantOrFlat(state.getBlock())) {
                            writePlantCrossObj(objWriter, relX, relY, relZ, currentMat, scale);
                        } else {
                            VoxelShape shape = state.getOutlineShape(world, mutablePos);
                            if (!shape.isEmpty()) {
                                // Using local variable for IOException handling inside lambda
                                String finalCurrentMat = currentMat;
                                shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                                    try {
                                        writeBoxObj(objWriter,
                                                (relX + minX) * scale, (relY + minY) * scale, (relZ + minZ) * scale,
                                                (relX + maxX) * scale, (relY + maxY) * scale, (relZ + maxZ) * scale,
                                                finalCurrentMat);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
        progressCallback.accept(1.0f);
    }

    // --- Helpers ---

    private void saveSprite(Sprite sprite, File outputFile, int tintColor) {
        if (outputFile.exists()) return;

        try {
            Identifier spriteId = sprite.getContents().getId();
            // Correct identifier creation for texture path
            Identifier resourceLocation = Identifier.of(
                    spriteId.getNamespace(),
                    "textures/" + spriteId.getPath() + ".png"
            );

            Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(resourceLocation);

            if (resource.isPresent()) {
                try (InputStream inputStream = resource.get().getInputStream()) {
                    NativeImage originalImage = NativeImage.read(inputStream);

                    if (tintColor != -1) {
                        int w = originalImage.getWidth();
                        int h = originalImage.getHeight();
                        // Creating a new image for tinted version
                        NativeImage tintedImage = new NativeImage(w, h, true);

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
                                    // Reconstruct ARGB
                                    int newPixel = (alpha << 24) | (newR << 16) | (newG << 8) | newB;
                                    tintedImage.setColorArgb(x, y, newPixel);
                                } else {
                                    tintedImage.setColorArgb(x, y, 0);
                                }
                            }
                        }
                        tintedImage.writeTo(outputFile.toPath());
                        tintedImage.close();
                    } else {
                        originalImage.writeTo(outputFile.toPath());
                    }
                    originalImage.close();
                }
            }
        } catch (Exception e) {
            System.err.println("Error saving texture: " + outputFile.getName());
        }
    }

    private Sprite getSprite(BlockState state) {
        try {
            // FIX: Use 'var' to avoid import issues with BakedModel
            // The location of BakedModel changes between versions/mappings (render.model vs resources.model)
            var model = MinecraftClient.getInstance().getBlockRenderManager().getModel(state);
            return model.particleSprite();
        } catch (Exception e) {
            return null;
        }
    }

    // --- OBJ Writing Methods ---

    private void writeBoxObj(BufferedWriter w, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, String matName) throws IOException {
        // Vertices
        w.write(String.format(Locale.US, "v %f %f %f\n", minX, minY, maxZ)); // 1
        w.write(String.format(Locale.US, "v %f %f %f\n", maxX, minY, maxZ)); // 2
        w.write(String.format(Locale.US, "v %f %f %f\n", maxX, minY, minZ)); // 3
        w.write(String.format(Locale.US, "v %f %f %f\n", minX, minY, minZ)); // 4
        w.write(String.format(Locale.US, "v %f %f %f\n", minX, maxY, maxZ)); // 5
        w.write(String.format(Locale.US, "v %f %f %f\n", maxX, maxY, maxZ)); // 6
        w.write(String.format(Locale.US, "v %f %f %f\n", maxX, maxY, minZ)); // 7
        w.write(String.format(Locale.US, "v %f %f %f\n", minX, maxY, minZ)); // 8

        // Texture Coords (Standard Cube Mapping)
        w.write("vt 0.0 0.0\n");
        w.write("vt 1.0 0.0\n");
        w.write("vt 1.0 1.0\n");
        w.write("vt 0.0 1.0\n");

        // Normals
        w.write("vn 0.0 -1.0 0.0\n"); // Down
        w.write("vn 0.0 1.0 0.0\n");  // Up
        w.write("vn 0.0 0.0 1.0\n");  // South
        w.write("vn 0.0 0.0 -1.0\n"); // North
        w.write("vn -1.0 0.0 0.0\n"); // West
        w.write("vn 1.0 0.0 0.0\n");  // East

        w.write("usemtl " + matName + "\n");

        // Bottom (4-3-2-1) Normal 1
        writeQuad(w, 4, 3, 2, 1, 1);
        // Top (5-6-7-8) Normal 2
        writeQuad(w, 5, 6, 7, 8, 2);
        // South (1-2-6-5) Normal 3
        writeQuad(w, 1, 2, 6, 5, 3);
        // North (3-4-8-7) Normal 4
        writeQuad(w, 3, 4, 8, 7, 4);
        // West (4-1-5-8) Normal 5
        writeQuad(w, 4, 1, 5, 8, 5);
        // East (2-3-7-6) Normal 6
        writeQuad(w, 2, 3, 7, 6, 6);

        vertexCount += 8;
    }

    private void writeQuad(BufferedWriter w, int v1, int v2, int v3, int v4, int normalIdx) throws IOException {
        // Absolute vertex index
        int base = vertexCount - 1;

        w.write(String.format("f %d/1/%d %d/2/%d %d/3/%d %d/4/%d\n",
                base+v1, normalIdx,
                base+v2, normalIdx,
                base+v3, normalIdx,
                base+v4, normalIdx));
    }

    private void writePlantCrossObj(BufferedWriter w, double x, double y, double z, String mat, float scale) throws IOException {
        double thick = 0.01; // Thin planes for plants
        double off = (1.0 - thick) / 2.0;
        double h = 1.0;

        // Diagonal 1
        writeBoxObj(w,
                (x + off) * scale, y * scale, (z + 0.0) * scale,
                (x + off + thick) * scale, (y + h) * scale, (z + 1.0) * scale,
                mat);

        // Diagonal 2
        writeBoxObj(w,
                (x + 0.0) * scale, y * scale, (z + off) * scale,
                (x + 1.0) * scale, (y + h) * scale, (z + off + thick) * scale,
                mat);
    }

    private void writeMaterialColor(BufferedWriter w, String name, float r, float g, float b) throws IOException {
        w.write("newmtl " + name + "\n");
        w.write(String.format(Locale.US, "Kd %f %f %f\n", r, g, b));
        w.write("d 1.0\n");
        w.write("illum 2\n\n");
    }

    private void writeMaterialTexture(BufferedWriter w, String name, String textureName, String dirName) throws IOException {
        w.write("newmtl " + name + "\n");
        w.write("Kd 1.0 1.0 1.0\n");
        w.write("map_Kd " + dirName + "/" + textureName + ".png\n\n");
    }

    private boolean isPlantOrFlat(Block block) {
        return block instanceof PlantBlock || block instanceof TorchBlock || block instanceof AbstractPlantPartBlock;
    }
}