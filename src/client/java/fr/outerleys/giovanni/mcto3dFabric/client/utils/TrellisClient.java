/*
 * McTo3D
 * Copyright (c) 2026 Giovanni Outerleys
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

/*
 * McTo3D
 * Copyright (c) 2026 Giovanni Outerleys
 */

package fr.outerleys.giovanni.mcto3dFabric.client.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class TrellisClient {

    private static final String URL = "https://ai.api.nvidia.com/v1/genai/microsoft/trellis";

    public static CompletableFuture<String> generate3DModel(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. API Key Check
                String apiKey = ModConfig.getInstance().getApiKey();
                if (apiKey == null || apiKey.length() < 10) {
                    throw new RuntimeException("API Key Missing or Invalid. Use /mcto3d apikey <key>");
                }

                // 2. JSON Payload
                JsonObject json = new JsonObject();
                json.addProperty("prompt", prompt);
                json.addProperty("slat_cfg_scale", 3.0);
                json.addProperty("ss_cfg_scale", 7.5);
                json.addProperty("slat_sampling_steps", 25);
                json.addProperty("ss_sampling_steps", 25);
                json.addProperty("seed", 0);

                String requestBody = new Gson().toJson(json);

                // 3. Request
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(URL))
                        .header("Authorization", apiKey)
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                System.out.println("McTo3D AI: Sending prompt '" + prompt + "'...");
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // 4. ERROR HANDLING (Crucial Step)
                String body = response.body();
                int status = response.statusCode();

                if (status != 200) {
                    // Try to parse the error message from JSON
                    String errorMsg = "HTTP " + status;
                    try {
                        JsonObject errJson = new Gson().fromJson(body, JsonObject.class);
                        if (errJson.has("detail")) {
                            // Nvidia often puts errors in "detail"
                            errorMsg += ": " + errJson.get("detail").getAsString();
                        } else if (errJson.has("message")) {
                            errorMsg += ": " + errJson.get("message").getAsString();
                        } else {
                            errorMsg += " - " + body; // Raw body if structure unknown
                        }
                    } catch (Exception ignored) {
                        errorMsg += " - " + body;
                    }
                    throw new RuntimeException("Nvidia Error: " + errorMsg);
                }

                // 5. SUCCESS HANDLING
                JsonObject responseJson = new Gson().fromJson(body, JsonObject.class);
                if (responseJson.has("artifacts")) {
                    JsonArray artifacts = responseJson.getAsJsonArray("artifacts");
                    if (artifacts.size() > 0) {
                        return artifacts.get(0).getAsJsonObject().get("base64").getAsString();
                    }
                }

                throw new RuntimeException("AI generated no artifacts (Prompt might be filtered).");

            } catch (Exception e) {
                // Propagate exception to be caught in the main thread
                throw new CompletionException(e);
            }
        });
    }
}