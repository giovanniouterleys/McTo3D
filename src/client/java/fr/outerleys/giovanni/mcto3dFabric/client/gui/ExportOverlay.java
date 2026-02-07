/*
 * McTo3D
 * Copyright (c) 2026 Giovanni Outerleys
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package fr.outerleys.giovanni.mcto3dFabric.client.gui;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public class ExportOverlay implements HudRenderCallback {

    public static boolean isVisible = false;
    public static float progress = 0.0f;
    // We need the start time to calculate the estimated remaining time (ETA)
    public static long startTime = 0;

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!isVisible) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // --- CONFIGURATION ---
        int barWidth = 150;     // Bar width
        int barHeight = 6;      // Bar height (thin)

        // Position X: Centered
        int barX = (width / 2) - (barWidth / 2);

        // Position Y: Just above the Hotbar
        // Hotbar is ~22px, we move up 65px to be safe above hearts/hunger bar
        int barY = height - 65;

        // 1. Background (Dark Gray, no border)
        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF404040);

        // 2. Progress (Green)
        int progressWidth = (int) (barWidth * progress);
        context.fill(barX, barY, barX + progressWidth, barY + barHeight, 0xFF00FF00);

        // 3. Text Calculation (% and ETA)
        String infoText = getInfoText();

        // 4. Text Display (Right of the bar)
        // x = end of bar + 5 pixels margin
        int textX = barX + barWidth + 8;
        int textY = barY - 1; // Small adjustment to center vertically with the bar

        // Use Text.literal for modern Minecraft versions
        context.drawTextWithShadow(client.textRenderer, Text.literal(infoText), textX, textY, 0xFFFFFF);
    }

    private String getInfoText() {
        // Percentage
        int percent = (int)(progress * 100);

        // Estimated Time of Arrival (ETA)
        String eta = "...";

        // Wait until 2% progress to calculate, otherwise the estimate is too volatile
        if (progress > 0.02f) {
            long elapsed = System.currentTimeMillis() - startTime;

            // Rule of three: (Elapsed / Progress) = Total Estimated Time
            // Remaining = Total - Elapsed
            if (progress > 0) {
                long totalEstimated = (long) (elapsed / progress);
                long remaining = totalEstimated - elapsed;

                // Convert to seconds
                long secondsLeft = remaining / 1000;

                if (secondsLeft < 60) {
                    eta = secondsLeft + "s";
                } else {
                    long mins = secondsLeft / 60;
                    long secs = secondsLeft % 60;
                    eta = String.format("%dm %02ds", mins, secs);
                }
            }
        }

        return String.format("%d%% (%s)", percent, eta);
    }
}