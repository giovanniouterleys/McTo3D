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
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {

    private static ModConfig INSTANCE;
    private String apiKey = "";

    // The file will be located at: /run/config/mcto3d_secrets.json
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "mcto3d_secrets.json");

    // Singleton pattern to access config globally
    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ModConfig();
            INSTANCE.load();
        }
        return INSTANCE;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String key) {
        // Ensure the format is correct (Bearer ...)
        if (key != null && !key.startsWith("Bearer ") && !key.isEmpty()) {
            this.apiKey = "Bearer " + key;
        } else {
            this.apiKey = key;
        }
        save();
    }

    public void load() {
        if (!CONFIG_FILE.exists()) return;

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = new Gson().fromJson(reader, JsonObject.class);
            if (json.has("nvidia_api_key")) {
                this.apiKey = json.get("nvidia_api_key").getAsString();
            }
        } catch (IOException e) {
            System.err.println("Error loading McTo3D config: " + e.getMessage());
        }
    }

    public void save() {
        JsonObject json = new JsonObject();
        json.addProperty("nvidia_api_key", this.apiKey);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            new Gson().toJson(json, writer);
        } catch (IOException e) {
            System.err.println("Error saving McTo3D config: " + e.getMessage());
        }
    }
}