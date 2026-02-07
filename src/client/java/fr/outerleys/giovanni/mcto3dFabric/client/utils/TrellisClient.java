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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class TrellisClient {

    private static final String URL = "https://ai.api.nvidia.com/v1/genai/microsoft/trellis";

    public static CompletableFuture<String> generate3DModel(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Retrieve API Key from Config
                String apiKey = ModConfig.getInstance().getApiKey();

                // Security: If no key is set, stop immediately
                if (apiKey == null || apiKey.isEmpty() || apiKey.length() < 10) {
                    System.err.println("ERROR: No API key configured! Use /mcto3d apikey <your_key>");
                    return null;
                }

                // 2. Prepare JSON Payload
                JsonObject json = new JsonObject();
                json.addProperty("prompt", prompt);
                json.addProperty("slat_cfg_scale", 3.0);
                json.addProperty("ss_cfg_scale", 7.5);
                json.addProperty("slat_sampling_steps", 25);
                json.addProperty("ss_sampling_steps", 25);
                json.addProperty("seed", 0); // 0 = Random seed

                String requestBody = new Gson().toJson(json);

                // 3. Send Request
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(URL))
                        .header("Authorization", apiKey) // Use the dynamic key from config
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                System.out.println("Sending prompt to Nvidia Trellis: " + prompt);
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject responseJson = new Gson().fromJson(response.body(), JsonObject.class);
                    JsonArray artifacts = responseJson.getAsJsonArray("artifacts");

                    if (artifacts.size() > 0) {
                        // Return the Base64 string of the first result
                        return artifacts.get(0).getAsJsonObject().get("base64").getAsString();
                    }
                } else {
                    System.err.println("API Error: " + response.statusCode() + " - " + response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null; // Failure
        });
    }
}