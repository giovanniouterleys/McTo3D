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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class MtlParser {

    /**
     * Parses an MTL file to extract material colors.
     * Returns a Map: Material Name -> RGB Color array [r, g, b]
     */
    public static Map<String, int[]> loadMaterials(File mtlFile) {
        Map<String, int[]> materials = new HashMap<>();
        String currentMaterialName = null;
        int[] currentRgb = new int[]{255, 255, 255}; // Default to white

        System.out.println("--- START MTL ANALYSIS: " + mtlFile.getName() + " ---");

        try (BufferedReader reader = new BufferedReader(new FileReader(mtlFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");

                // 1. Start of a new material
                if (parts[0].equals("newmtl")) {
                    // Save the previous material if it exists
                    if (currentMaterialName != null) {
                        materials.put(currentMaterialName, currentRgb);
                    }
                    currentMaterialName = parts[1];
                    currentRgb = new int[]{255, 255, 255}; // Reset
                    System.out.println("New Material detected: " + currentMaterialName);
                }
                // 2. Diffuse Color (Kd r g b) - Values between 0.0 and 1.0
                else if (parts[0].equals("Kd") && parts.length >= 4) {
                    float r = Float.parseFloat(parts[1]);
                    float g = Float.parseFloat(parts[2]);
                    float b = Float.parseFloat(parts[3]);
                    currentRgb[0] = (int)(r * 255);
                    currentRgb[1] = (int)(g * 255);
                    currentRgb[2] = (int)(b * 255);
                    // We don't log Kd here because map_Kd (texture) might override it later
                }
                // 3. Texture (map_Kd file.bmp/png) - OVERRIDES Kd color
                else if (parts[0].equals("map_Kd") && parts.length >= 2) {
                    String textureFileName = line.substring(7).trim(); // Get everything after "map_Kd "
                    File textureFile = new File(mtlFile.getParent(), textureFileName);

                    if (textureFile.exists()) {
                        try {
                            System.out.println("  > Reading texture: " + textureFileName);
                            // Read image and calculate average color
                            int[] avgColor = getAverageColor(textureFile);
                            currentRgb = avgColor;
                            System.out.println("  > Calculated average color: R=" + avgColor[0] + " G=" + avgColor[1] + " B=" + avgColor[2]);
                        } catch (Exception e) {
                            System.err.println("  > ERROR reading image: " + e.getMessage());
                        }
                    } else {
                        System.err.println("  > ERROR: Texture not found: " + textureFileName);
                    }
                }
            }
            // Save the last material
            if (currentMaterialName != null) {
                materials.put(currentMaterialName, currentRgb);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return materials;
    }

    // Calculates the average color of an image file
    private static int[] getAverageColor(File imageFile) throws Exception {
        BufferedImage img = ImageIO.read(imageFile);
        if (img == null) throw new Exception("ImageIO could not decode the image (unknown format?)");

        long sumR = 0, sumG = 0, sumB = 0;
        int width = img.getWidth();
        int height = img.getHeight();
        long pixelCount = 0;

        // Scan all pixels
        // (Optimization: we could skip pixels for speed, but accuracy is preferred here)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = img.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xff;

                if (alpha > 0) { // Ignore transparent pixels
                    sumR += (pixel >> 16) & 0xff;
                    sumG += (pixel >> 8) & 0xff;
                    sumB += (pixel) & 0xff;
                    pixelCount++;
                }
            }
        }

        if (pixelCount == 0) return new int[]{255, 255, 255};

        return new int[]{
                (int)(sumR / pixelCount),
                (int)(sumG / pixelCount),
                (int)(sumB / pixelCount)
        };
    }
}