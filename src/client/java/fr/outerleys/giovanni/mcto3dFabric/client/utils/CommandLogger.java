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

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CommandLogger {

    private static final File LOG_FILE = new File(FabricLoader.getInstance().getGameDir().toFile(), "mcto3d_commands.log");
    private static final DateTimeFormatter DATES = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void log(String playerName, String command) {
        // Run in a separate thread to avoid slowing down the game loop (IO is slow)
        new Thread(() -> {
            try (FileWriter fw = new FileWriter(LOG_FILE, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                
                String time = LocalDateTime.now().format(DATES);
                pw.println("[" + time + "] [" + playerName + "] " + command);
                
            } catch (IOException e) {
                System.err.println("McTo3D: Failed to log command.");
                e.printStackTrace();
            }
        }).start();
    }
}