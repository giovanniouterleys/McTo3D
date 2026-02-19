    /*
     * McTo3D
     * Copyright (c) 2026 Giovanni Outerleys
     *
     * This program is free software: you can redistribute it and/or modify
     * it under the terms of the GNU General Public License as published by
     * the Free Software Foundation, either version 3 of the License, or
     * (at your option) any later version.
     */

    package fr.outerleys.giovanni.mcto3dFabric.client;

    import com.mojang.brigadier.arguments.FloatArgumentType;
    import com.mojang.brigadier.arguments.IntegerArgumentType;
    import com.mojang.brigadier.arguments.StringArgumentType;
    import fr.outerleys.giovanni.mcto3dFabric.Mcto3dFabric;
    import fr.outerleys.giovanni.mcto3dFabric.client.event.KeyInputHandler;
    import fr.outerleys.giovanni.mcto3dFabric.client.gui.ExportOverlay;
    import fr.outerleys.giovanni.mcto3dFabric.client.render.ImportRenderer;
    import fr.outerleys.giovanni.mcto3dFabric.client.utils.*;
    import fr.outerleys.giovanni.mcto3dFabric.selection.Cuboid;
    import fr.outerleys.giovanni.mcto3dFabric.utils.PlaceBlockPayload;
    import net.fabricmc.api.ClientModInitializer;
    import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
    import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
    import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
    import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
    import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
    import net.fabricmc.loader.api.FabricLoader;
    import net.minecraft.block.Block;
    import net.minecraft.block.BlockState;
    import net.minecraft.client.MinecraftClient;
    import net.minecraft.particle.ParticleTypes;
    import net.minecraft.text.Text;
    import net.minecraft.util.math.BlockPos;
    import net.minecraft.util.math.Vec3d;

    import java.io.File;
    import java.util.Map;
    import java.util.concurrent.CompletableFuture;

    public class Mcto3dFabricClient implements ClientModInitializer {

        @Override
        public void onInitializeClient() {
            ImportRenderer.register(); // Important: Registers the hologram renderer
            KeyInputHandler.register();

            // 0. REGISTER OVERLAY (LOADING BAR)
            HudRenderCallback.EVENT.register(new ExportOverlay());

            // 1. PARTICLES (Selection visualization)
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                var player = client.player;
                if (player == null) return;
                if (Mcto3dFabric.MANAGER.hasSelection(player.getUuid())) {
                    if (client.world != null && client.world.getTime() % 5 == 0) {
                        displayParticles(Mcto3dFabric.MANAGER.getSelection(player.getUuid()));
                    }
                }

                PrinterManager.tick(client);
            });

            // 2. COMMAND REGISTRATION
            ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

                // --- COMMAND: SET SCALE ---
                dispatcher.register(ClientCommandManager.literal("setscale")
                        .then(ClientCommandManager.argument("mm_per_block", FloatArgumentType.floatArg())
                                .executes(context -> {
                                    var player = context.getSource().getPlayer();
                                    float scale = FloatArgumentType.getFloat(context, "mm_per_block");

                                    Mcto3dFabric.MANAGER.setScale(player.getUuid(), scale);

                                    context.getSource().sendFeedback(Text.literal("§aScale set: 1 block = " + scale + " mm."));
                                    if (scale == 10.0f)
                                        context.getSource().sendFeedback(Text.literal("§7(i.e., 1 block = 1 cm)"));
                                    return 1;
                                })));

                // --- COMMAND: EXPORT ---
                dispatcher.register(ClientCommandManager.literal("export3d")
                        .then(ClientCommandManager.argument("filename", StringArgumentType.string())
                                .executes(context -> {
                                    var player = context.getSource().getPlayer();
                                    String projectName = StringArgumentType.getString(context, "filename");
                                    var client = MinecraftClient.getInstance();

                                    if (Mcto3dFabric.MANAGER.hasSelection(player.getUuid())) {

                                        // Initial Feedback
                                        context.getSource().sendFeedback(Text.literal("§eStarting background export... check the screen center!"));

                                        Cuboid selection = Mcto3dFabric.MANAGER.getSelection(player.getUuid());
                                        float scale = Mcto3dFabric.MANAGER.getScale(player.getUuid());
                                        var world = player.getEntityWorld();

                                        // ENABLE OVERLAY
                                        ExportOverlay.isVisible = true;
                                        ExportOverlay.progress = 0f;
                                        ExportOverlay.startTime = System.currentTimeMillis();

                                        CompletableFuture.runAsync(() -> {
                                            try {
                                                long startTime = System.currentTimeMillis();

                                                File exportsDir = new File(FabricLoader.getInstance().getGameDir().toFile(), "exports");
                                                File projectDir = new File(exportsDir, projectName);
                                                projectDir.mkdirs();

                                                // 1. Export RAW (STL)
                                                ExportOverlay.progress = 0f;
                                                StlExporter stlExporter = new StlExporter();
                                                stlExporter.export(selection, world, new File(projectDir, projectName + "_raw.stl"),
                                                        scale, (p) -> ExportOverlay.progress = p);

                                                // 2. Export COLOR (OBJ)
                                                ExportOverlay.progress = 0f;
                                                ObjExporter objExporter = new ObjExporter();
                                                objExporter.export(selection, world, new File(projectDir, projectName + "_color"), ObjExporter.MODE_COLOR,
                                                        scale, (p) -> ExportOverlay.progress = p);

                                                // 3. Export TEXTURE (OBJ)
                                                ExportOverlay.progress = 0f;
                                                objExporter.export(selection, world, new File(projectDir, projectName + "_texture"), ObjExporter.MODE_TEXTURES,
                                                        scale, (p) -> ExportOverlay.progress = p);

                                                long duration = System.currentTimeMillis() - startTime;

                                                // Success: Return to main thread
                                                client.execute(() -> {
                                                    ExportOverlay.isVisible = false;
                                                    player.sendMessage(Text.literal("§aExport completed successfully in " + duration + "ms!"), false);
                                                    player.sendMessage(Text.literal("§7Folder: exports/" + projectName), false);
                                                });

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                // Error: Return to main thread
                                                client.execute(() -> {
                                                    ExportOverlay.isVisible = false;
                                                    player.sendMessage(Text.literal("§cAn error occurred during export! Check console."), false);
                                                });
                                            }
                                        });

                                    } else {
                                        context.getSource().sendError(Text.literal("§cNo selection! Use the Golden Hoe."));
                                    }
                                    return 1;
                                })));

                // --- POS 1 ---
                dispatcher.register(ClientCommandManager.literal("pos1")
                        .executes(context -> {
                            var player = context.getSource().getPlayer();
                            BlockPos pos = player.getBlockPos();
                            Mcto3dFabric.MANAGER.setPos1(player.getUuid(), pos);
                            context.getSource().sendFeedback(Text.literal("§aPosition 1 set at feet (" + pos.toShortString() + ")"));
                            return 1;
                        }));

                // --- POS 2 ---
                dispatcher.register(ClientCommandManager.literal("pos2")
                        .executes(context -> {
                            var player = context.getSource().getPlayer();
                            BlockPos pos = player.getBlockPos();
                            Mcto3dFabric.MANAGER.setPos2(player.getUuid(), pos);
                            context.getSource().sendFeedback(Text.literal("§bPosition 2 set at feet (" + pos.toShortString() + ")"));
                            return 1;
                        }));

                // --- EXPAND ---
                dispatcher.register(ClientCommandManager.literal("expand")
                        .then(ClientCommandManager.argument("amount", IntegerArgumentType.integer())
                                .executes(context -> {
                                    var player = context.getSource().getPlayer();
                                    int amount = IntegerArgumentType.getInteger(context, "amount");

                                    if (!Mcto3dFabric.MANAGER.hasSelection(player.getUuid())) {
                                        context.getSource().sendError(Text.literal("§cSelect a zone with pos1/pos2 first!"));
                                        return 0;
                                    }

                                    BlockPos p1 = Mcto3dFabric.MANAGER.getPos1(player.getUuid());
                                    BlockPos p2 = Mcto3dFabric.MANAGER.getPos2(player.getUuid());

                                    if (p1.getY() >= p2.getY()) {
                                        Mcto3dFabric.MANAGER.setPos1(player.getUuid(), p1.up(amount));
                                    } else {
                                        Mcto3dFabric.MANAGER.setPos2(player.getUuid(), p2.up(amount));
                                    }

                                    context.getSource().sendFeedback(Text.literal("§dSelection expanded by " + amount + " blocks upwards!"));
                                    return 1;
                                })));
            });

            // --- IMPORT & UTILITY COMMANDS ---
            ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

// --- COMMAND: IMPORT 3D ---
                dispatcher.register(ClientCommandManager.literal("import3d")
                        // Subcommand 1: Local File Import
                        .then(ClientCommandManager.argument("filename", StringArgumentType.string())
                                .then(ClientCommandManager.argument("scale", FloatArgumentType.floatArg())
                                        .executes(context -> {
                                            String filename = StringArgumentType.getString(context, "filename");
                                            float scale = FloatArgumentType.getFloat(context, "scale");
                                            var player = context.getSource().getPlayer();

                                            // LOGGING: Track local import usage
                                            CommandLogger.log(player.getName().getString(), "/import3d " + filename + " " + scale);

                                            // Append .obj extension if the user forgot it
                                            if (!filename.endsWith(".obj")) filename += ".obj";
                                            File file = new File(FabricLoader.getInstance().getGameDir().toFile(), "imports/" + filename);

                                            if (file.exists()) {
                                                context.getSource().sendFeedback(Text.literal("§eLoading and Voxelizing... (Scale: " + scale + ")"));

                                                // Start Voxelization process
                                                var voxels = Voxelizer.loadAndVoxelize(file, scale);

                                                if (voxels.isEmpty()) {
                                                    context.getSource().sendError(Text.literal("§cNo blocks generated. Try increasing the Scale!"));
                                                } else {
                                                    ImportManager.currentVoxels = voxels;
                                                    ImportManager.isActive = true;
                                                    // Reset placement settings for a fresh import
                                                    ImportManager.rotationSteps = 0;
                                                    ImportManager.distance = 5.0;

                                                    context.getSource().sendFeedback(Text.literal("§aModel loaded! (" + voxels.size() + " blocks)"));
                                                    context.getSource().sendFeedback(Text.literal("§7Use /place3d or /print3d to confirm, Arrow Keys to rotate/move."));
                                                }
                                            } else {
                                                context.getSource().sendError(Text.literal("§cFile not found in /run/imports/"));
                                            }
                                            return 1;
                                        })))

                        // Subcommand 2: AI Generation
                        .then(ClientCommandManager.literal("ai")
                                .then(ClientCommandManager.argument("scale", FloatArgumentType.floatArg())
                                        .then(ClientCommandManager.argument("prompt", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    float scale = FloatArgumentType.getFloat(context, "scale");
                                                    String prompt = StringArgumentType.getString(context, "prompt");
                                                    var player = context.getSource().getPlayer();

                                                    // LOGGING: Track AI prompts
                                                    CommandLogger.log(player.getName().getString(), "/import3d ai " + scale + " " + prompt);

                                                    player.sendMessage(Text.literal("§dGenerating AI Model: " + prompt + "..."), false);

                                                    // ASYNC GENERATION with Robust Error Handling
                                                    TrellisClient.generate3DModel(prompt)
                                                            .thenAccept(base64 -> {
                                                                try {
                                                                    // PRE-CHECK: Did the API return an empty string due to a safety filter?
                                                                    if (base64 == null || base64.trim().isEmpty() || base64.length() < 50) {
                                                                        throw new IllegalArgumentException("Received empty or invalid data from API. The AI likely blocked your prompt for safety reasons.");
                                                                    }

                                                                    // 1. Prepare directories
                                                                    File importsDir = new File(FabricLoader.getInstance().getGameDir().toFile(), "imports");
                                                                    if (!importsDir.exists()) importsDir.mkdirs();

                                                                    String safeName = prompt.replaceAll("[^a-zA-Z0-9]", "_");
                                                                    if (safeName.length() > 15) safeName = safeName.substring(0, 15);
                                                                    File outputFile = new File(importsDir, "ai_" + safeName + ".obj");

                                                                    // 2. Convert GLB (Base64) to OBJ
                                                                    GlbConverter.convertGlbToObj(base64, outputFile);

                                                                    // 3. Return to Main Thread for Voxelization
                                                                    MinecraftClient.getInstance().execute(() -> {
                                                                        player.sendMessage(Text.literal("§aModel received! Voxelizing..."), false);

                                                                        var voxels = Voxelizer.loadAndVoxelize(outputFile, scale);

                                                                        if (!voxels.isEmpty()) {
                                                                            ImportManager.currentVoxels = voxels;
                                                                            ImportManager.isActive = true;
                                                                            ImportManager.rotationSteps = 0;
                                                                            ImportManager.distance = 5.0;
                                                                            player.sendMessage(Text.literal("§bObject ready! Use Arrows to adjust, /print3d to build."), false);
                                                                        } else {
                                                                            player.sendMessage(Text.literal("§cObject empty. Scale might be too small?"), false);
                                                                        }
                                                                    });

                                                                } catch (Exception e) {
                                                                    e.printStackTrace();

                                                                    // Custom error message generation to prevent "null" outputs
                                                                    String errorMsg = e.getMessage();
                                                                    if (e instanceof java.nio.BufferUnderflowException || errorMsg == null) {
                                                                        errorMsg = "API returned an empty 3D file (Safety filter triggered or generation failed).";
                                                                    }

                                                                    final String finalErrorMsg = errorMsg;

                                                                    // Report conversion errors safely to the user
                                                                    MinecraftClient.getInstance().execute(() ->
                                                                            player.sendMessage(Text.literal("§cConversion Error: " + finalErrorMsg), false)
                                                                    );
                                                                }
                                                            })
                                                            .exceptionally(ex -> {
                                                                // CATCH TRELLIS/NETWORK ERRORS (401, 403, Timeouts)
                                                                Throwable cause = ex.getCause(); // Unwrap the CompletionException
                                                                String msg = cause != null ? cause.getMessage() : ex.getMessage();

                                                                System.err.println("AI Generation Failed: " + msg);
                                                                CommandLogger.log(player.getName().getString(), msg);
                                                                        MinecraftClient.getInstance().execute(() -> {
                                                                    player.sendMessage(Text.literal("§cAI Failed: " + msg), false);
                                                                    if (msg.contains("401") || msg.contains("403")) {
                                                                        player.sendMessage(Text.literal("§7Check your API Key with /mcto3d apikey"), false);
                                                                    }
                                                                });
                                                                return null;
                                                            });

                                                    return 1;
                                                })))));
                // --- COMMAND: PLACE BLOCKS (CONFIRM) ---
                dispatcher.register(ClientCommandManager.literal("place3d")
                        .executes(context -> {
                            if (!ImportManager.isActive || ImportManager.currentVoxels == null) {
                                context.getSource().sendError(Text.literal("§cNo model loaded!"));
                                return 0;
                            }

                            var player = context.getSource().getPlayer();

                            Vec3d lookDir = player.getRotationVec(1.0f);
                            Vec3d centerPos = player.getEyePos().add(lookDir.multiply(ImportManager.distance));
                            BlockPos origin = new BlockPos((int) centerPos.x, (int) centerPos.y, (int) centerPos.z);

                            int count = 0;
                            int rotation = ImportManager.rotationSteps;

                            for (Map.Entry<BlockPos, BlockState> entry : ImportManager.currentVoxels.entrySet()) {
                                BlockPos rel = entry.getKey();

                                int newX = rel.getX();
                                int newZ = rel.getZ();
                                for (int i = 0; i < rotation; i++) {
                                    int tempX = newX;
                                    newX = -newZ;
                                    newZ = tempX;
                                }

                                BlockPos target = origin.add(newX, rel.getY(), newZ);

                                int rawId = Block.getRawIdFromState(entry.getValue());
                                var payload = new PlaceBlockPayload(target, rawId);

                                if (ClientPlayNetworking.canSend(PlaceBlockPayload.ID)) {
                                    ClientPlayNetworking.send(payload);
                                    count++;
                                }
                            }

                            context.getSource().sendFeedback(Text.literal("§aRequest sent for " + count + " blocks!"));
                            ImportManager.clear();
                            return 1;
                        }));
                // --- COMMAND: ROTATION (Legacy/Debug) ---
                dispatcher.register(ClientCommandManager.literal("rotate3d")
                        .then(ClientCommandManager.argument("angle", FloatArgumentType.floatArg())
                                .executes(context -> {
                                    // Rotation is now handled via KeyBindings (Arrow Keys)
                                    context.getSource().sendFeedback(Text.literal("§7Use Arrow Keys to rotate the model."));
                                    return 1;
                                })));

                // --- COMMAND: CANCEL ---
                dispatcher.register(ClientCommandManager.literal("clear3d")
                        .executes(context -> {
                            ImportManager.clear();
                            context.getSource().sendFeedback(Text.literal("§cImport cancelled."));
                            return 1;
                        }));

                // --- COMMAND: CONFIGURATION ---
                dispatcher.register(ClientCommandManager.literal("mcto3d")
                        .then(ClientCommandManager.literal("apikey")
                                .then(ClientCommandManager.argument("key", StringArgumentType.string())
                                        .executes(context -> {
                                            String newKey = StringArgumentType.getString(context, "key");
                                            var player = context.getSource().getPlayer();

                                            // Save to config
                                            ModConfig.getInstance().setApiKey(newKey);

                                            player.sendMessage(Text.literal("§aAPI Key saved successfully!"), false);
                                            player.sendMessage(Text.literal("§7(Stored in config/mcto3d_secrets.json)"), false);

                                            return 1;
                                        }))));
                // --- COMMAND : print3d (like import3d block by block) ---
                dispatcher.register(ClientCommandManager.literal("print3d")
                        // Argument optionnel : Vitesse en ms (par défaut 20ms)
                        .then(ClientCommandManager.argument("speed_ms", IntegerArgumentType.integer(1, 1000))
                                .executes(context -> {
                                    int speed = IntegerArgumentType.getInteger(context, "speed_ms");
                                    return startPrinting(context, speed);
                                }))
                        .executes(context -> {
                            return startPrinting(context, 20);
                        }));
                // --- COMMAND : Stop the printing ---
                dispatcher.register(ClientCommandManager.literal("stop3d")
                        .executes(context -> {
                            PrinterManager.stop();
                            context.getSource().sendFeedback(Text.literal("§cStop printing."));
                            return 1;
                        }));

            });
        }

        private int startPrinting(com.mojang.brigadier.context.CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> context, int speedMs) {
            if (!ImportManager.isActive || ImportManager.currentVoxels == null) {
                context.getSource().sendError(Text.literal("§cUse /import3d first."));
                return 0;
            }

            var player = context.getSource().getPlayer();

            // Calcul de l'origine (là où est l'hologramme)
            Vec3d lookDir = player.getRotationVec(1.0f);
            Vec3d centerPos = player.getEyePos().add(lookDir.multiply(ImportManager.distance));
            BlockPos origin = new BlockPos((int) centerPos.x, (int) centerPos.y, (int) centerPos.z);

            // Lancement de l'imprimante
            PrinterManager.startPrint(
                    ImportManager.currentVoxels,
                    origin,
                    ImportManager.rotationSteps,
                    speedMs
            );

            context.getSource().sendFeedback(Text.literal("§bDémarrage de l'impression... (" + speedMs + "ms/bloc)"));
            return 1;
        }

        private void displayParticles(Cuboid c) {
            var world = MinecraftClient.getInstance().world;
            if (world == null) return;

            double minX = c.getMinX(); double minY = c.getMinY(); double minZ = c.getMinZ();
            double maxX = c.getMaxX() + 1.0; double maxY = c.getMaxY() + 1.0; double maxZ = c.getMaxZ() + 1.0;
            double step = 0.25;

            // Particle loops to visualize the selection box
            for (double y = minY; y <= maxY; y += step) {
                spawnP(world, minX, y, minZ); spawnP(world, maxX, y, minZ);
                spawnP(world, minX, y, maxZ); spawnP(world, maxX, y, maxZ);
            }
            for (double x = minX; x <= maxX; x += step) {
                spawnP(world, x, minY, minZ); spawnP(world, x, maxY, minZ);
                spawnP(world, x, minY, maxZ); spawnP(world, x, maxY, maxZ);
            }
            for (double z = minZ; z <= maxZ; z += step) {
                spawnP(world, minX, minY, z); spawnP(world, maxX, minY, z);
                spawnP(world, minX, maxY, z); spawnP(world, maxX, maxY, z);
            }
        }

        private void spawnP(net.minecraft.client.world.ClientWorld w, double x, double y, double z) {
            w.addParticleClient(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
        }
    }