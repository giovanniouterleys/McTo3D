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

import fr.outerleys.giovanni.mcto3dFabric.utils.PlaceBlockPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class PrinterManager {

    private static boolean printing = false;
    private static List<Map.Entry<BlockPos, BlockState>> printQueue = new ArrayList<>();
    private static int currentIndex = 0;
    private static BlockPos originPos;
    private static int rotationSteps;

    // Gestion du timing
    private static long lastTime = 0;
    private static float delayMs = 50;
    private static float timeAccumulator = 0;

    public static void startPrint(Map<BlockPos, BlockState> voxels, BlockPos origin, int rotation, float speedMs) {
        if (voxels == null || voxels.isEmpty()) return;

        // 1. COPIE ET TRI (Y -> X -> Z)
        printQueue = new ArrayList<>(voxels.entrySet());
        printQueue.sort((e1, e2) -> {
            int dy = Integer.compare(e1.getKey().getY(), e2.getKey().getY());
            if (dy != 0) return dy;
            int dx = Integer.compare(e1.getKey().getX(), e2.getKey().getX());
            if (dx != 0) return dx;
            return Integer.compare(e1.getKey().getZ(), e2.getKey().getZ());
        });

        // 2. CONFIGURATION
        originPos = origin;
        rotationSteps = rotation;
        delayMs = speedMs;
        currentIndex = 0;
        printing = true;
        lastTime = System.currentTimeMillis();
        timeAccumulator = 0;

        // --- CORRECTION ICI : ON CACHE L'HOLOGRAMME ---
        // On désactive l'affichage "fantôme" pour ne voir que les vrais blocs se poser
        ImportManager.isActive = false;

        System.out.println("Impression 3D démarrée : " + printQueue.size() + " blocs.");
    }

    public static void tick(MinecraftClient client) {
        if (!printing || client.player == null) return;

        long now = System.currentTimeMillis();
        long delta = now - lastTime;
        lastTime = now;

        timeAccumulator += delta;

        // Boucle pour rattraper le temps (permet de poser plusieurs blocs par tick si delayMs est petit)
        while (timeAccumulator >= delayMs) {
            if (currentIndex >= printQueue.size()) {
                finish(client);
                return;
            }

            Map.Entry<BlockPos, BlockState> entry = printQueue.get(currentIndex);
            placeBlock(entry.getKey(), entry.getValue());

            currentIndex++;
            timeAccumulator -= delayMs;
        }
    }

    private static void placeBlock(BlockPos rel, BlockState state) {
        // Rotation mathématique
        int newX = rel.getX();
        int newZ = rel.getZ();
        for (int i = 0; i < rotationSteps; i++) {
            int tempX = newX;
            newX = -newZ;
            newZ = tempX;
        }

        BlockPos target = originPos.add(newX, rel.getY(), newZ);

        // Envoi réseau
        int rawId = Block.getRawIdFromState(state);
        if (ClientPlayNetworking.canSend(PlaceBlockPayload.ID)) {
            ClientPlayNetworking.send(new PlaceBlockPayload(target, rawId));
        }
    }

    private static void finish(MinecraftClient client) {
        printing = false;
        client.player.sendMessage(Text.literal("§aImpression 3D terminée !"), true);
        // ImportManager.clear() est déjà fait implicitement car on a mis isActive à false au début,
        // mais on peut le rappeler pour être sûr de vider la mémoire.
        ImportManager.clear();
    }

    public static boolean isPrinting() { return printing; }

    public static void stop() {
        printing = false;
        printQueue.clear();
        ImportManager.isActive = false; // Sécurité si on stop manuellement
    }
}