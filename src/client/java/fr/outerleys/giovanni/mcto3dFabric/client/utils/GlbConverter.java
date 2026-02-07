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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class GlbConverter {

    public static File convertGlbToObj(String base64Glb, File outputObjFile) throws Exception {
        byte[] glbBytes = Base64.getDecoder().decode(base64Glb);
        ByteBuffer buffer = ByteBuffer.wrap(glbBytes).order(ByteOrder.LITTLE_ENDIAN);

        // --- 1. GLB Header ---
        if (buffer.getInt() != 0x46546C67) throw new IOException("Invalid GLB header");
        buffer.getInt(); // Version
        buffer.getInt(); // Length

        // --- 2. JSON Chunk ---
        int jsonChunkLength = buffer.getInt();
        if (buffer.getInt() != 0x4E4F534A) throw new IOException("Error reading JSON Chunk");

        byte[] jsonBytes = new byte[jsonChunkLength];
        buffer.get(jsonBytes);
        String jsonStr = new String(jsonBytes, StandardCharsets.UTF_8);
        JsonObject gltf = new Gson().fromJson(jsonStr, JsonObject.class);

        // --- 3. Binary Chunk ---
        int binChunkLength = buffer.getInt();
        if (buffer.getInt() != 0x004E4942) throw new IOException("Error reading Binary Chunk");
        int binStart = buffer.position();

        // --- 4. Image Extraction (Texture) ---
        String textureFilename = null;
        if (gltf.has("images") && gltf.getAsJsonArray("images").size() > 0) {
            JsonObject imageJson = gltf.getAsJsonArray("images").get(0).getAsJsonObject();
            int bufferViewIdx = imageJson.get("bufferView").getAsInt();
            String mimeType = imageJson.get("mimeType").getAsString(); // e.g., image/jpeg or image/png

            String ext = mimeType.contains("png") ? ".png" : ".jpg";
            textureFilename = outputObjFile.getName().replace(".obj", ext);
            File textureFile = new File(outputObjFile.getParent(), textureFilename);

            // Extract image bytes
            byte[] imageBytes = extractBufferView(gltf, buffer, binStart, bufferViewIdx);
            try (FileOutputStream fos = new FileOutputStream(textureFile)) {
                fos.write(imageBytes);
            }
            System.out.println("Texture extracted: " + textureFilename);
        }

        // --- 5. Geometry Extraction ---
        JsonObject mesh = gltf.getAsJsonArray("meshes").get(0).getAsJsonObject();
        JsonObject primitive = mesh.getAsJsonArray("primitives").get(0).getAsJsonObject();

        int posIdx = primitive.getAsJsonObject("attributes").get("POSITION").getAsInt();
        List<float[]> vertices = extractVec3(gltf, buffer, binStart, posIdx);

        int indicesIdx = primitive.get("indices").getAsInt();
        List<int[]> faces = extractIndices(gltf, buffer, binStart, indicesIdx);

        // Extract UVs (Texture Coordinates)
        List<float[]> uvs = new ArrayList<>();
        if (primitive.getAsJsonObject("attributes").has("TEXCOORD_0")) {
            int uvIdx = primitive.getAsJsonObject("attributes").get("TEXCOORD_0").getAsInt();
            uvs = extractVec2(gltf, buffer, binStart, uvIdx);
        }

        // --- 6. MTL Writing (Material File) ---
        String mtlFilename = outputObjFile.getName().replace(".obj", ".mtl");
        if (textureFilename != null) {
            File mtlFile = new File(outputObjFile.getParent(), mtlFilename);
            try (FileWriter w = new FileWriter(mtlFile)) {
                w.write("newmtl default_ai\n");
                w.write("Kd 1.0 1.0 1.0\n");
                w.write("map_Kd " + textureFilename + "\n");
            }
        }

        // --- 7. OBJ Writing ---
        try (FileWriter writer = new FileWriter(outputObjFile)) {
            writer.write("# Nvidia Trellis via McTo3D\n");
            if (textureFilename != null) writer.write("mtllib " + mtlFilename + "\n");

            // Vertices (v)
            for (float[] v : vertices) writer.write(String.format("v %f %f %f\n", v[0], v[1], v[2]));

            // Texture Coords (vt)
            // Y inversion is often necessary for OBJ format
            for (float[] uv : uvs) writer.write(String.format("vt %f %f\n", uv[0], 1.0f - uv[1]));

            // Faces (f v/vt/vn)
            writer.write("usemtl default_ai\n");
            boolean hasUV = !uvs.isEmpty();

            for (int[] f : faces) {
                // OBJ indices start at 1, not 0
                int a = f[0] + 1;
                int b = f[1] + 1;
                int c = f[2] + 1;
                if (hasUV) {
                    // f v/vt v/vt v/vt
                    writer.write(String.format("f %d/%d %d/%d %d/%d\n", a, a, b, b, c, c));
                } else {
                    writer.write(String.format("f %d %d %d\n", a, b, c));
                }
            }
        }

        return outputObjFile;
    }

    // --- Extraction Helpers ---

    private static byte[] extractBufferView(JsonObject gltf, ByteBuffer buffer, int binStart, int bufferViewIdx) {
        JsonObject bufferView = gltf.getAsJsonArray("bufferViews").get(bufferViewIdx).getAsJsonObject();
        int byteOffset = bufferView.get("byteOffset").getAsInt();
        int byteLength = bufferView.get("byteLength").getAsInt();
        byte[] data = new byte[byteLength];
        buffer.position(binStart + byteOffset);
        buffer.get(data);
        return data;
    }

    private static List<float[]> extractVec3(JsonObject gltf, ByteBuffer buffer, int binStart, int accessorIdx) {
        return extractFloats(gltf, buffer, binStart, accessorIdx, 3);
    }

    private static List<float[]> extractVec2(JsonObject gltf, ByteBuffer buffer, int binStart, int accessorIdx) {
        return extractFloats(gltf, buffer, binStart, accessorIdx, 2);
    }

    private static List<float[]> extractFloats(JsonObject gltf, ByteBuffer buffer, int binStart, int accessorIdx, int componentCount) {
        JsonObject accessor = gltf.getAsJsonArray("accessors").get(accessorIdx).getAsJsonObject();
        int bufferViewIdx = accessor.get("bufferView").getAsInt();
        int count = accessor.get("count").getAsInt();
        JsonObject bufferView = gltf.getAsJsonArray("bufferViews").get(bufferViewIdx).getAsJsonObject();
        int byteOffset = bufferView.get("byteOffset").getAsInt();

        buffer.position(binStart + byteOffset);
        List<float[]> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            float[] vec = new float[componentCount];
            for (int k = 0; k < componentCount; k++) vec[k] = buffer.getFloat();
            list.add(vec);
        }
        return list;
    }

    private static List<int[]> extractIndices(JsonObject gltf, ByteBuffer buffer, int binStart, int accessorIdx) {
        JsonObject accessor = gltf.getAsJsonArray("accessors").get(accessorIdx).getAsJsonObject();
        int bufferViewIdx = accessor.get("bufferView").getAsInt();
        int count = accessor.get("count").getAsInt();
        int compType = accessor.get("componentType").getAsInt();
        JsonObject bufferView = gltf.getAsJsonArray("bufferViews").get(bufferViewIdx).getAsJsonObject();
        int byteOffset = bufferView.get("byteOffset").getAsInt();

        buffer.position(binStart + byteOffset);
        List<int[]> list = new ArrayList<>();
        for (int i = 0; i < count / 3; i++) {
            int a = readInt(buffer, compType);
            int b = readInt(buffer, compType);
            int c = readInt(buffer, compType);
            list.add(new int[]{a, b, c});
        }
        return list;
    }

    private static int readInt(ByteBuffer b, int type) {
        if (type == 5123) return b.getShort() & 0xFFFF; // Unsigned Short
        if (type == 5125) return b.getInt();            // Unsigned Int
        return 0;
    }
}