/*
 * McTo3D
 * Copyright (c) 2026 Giovanni Outerleys
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package fr.outerleys.giovanni.mcto3dFabric.utils;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

// Ceci définit la structure du paquet : une Position et un ID de bloc
public record PlaceBlockPayload(BlockPos pos, int stateId) implements CustomPayload {

    // L'identifiant unique du paquet
    public static final CustomPayload.Id<PlaceBlockPayload> ID = new CustomPayload.Id<>(Identifier.of("mcto3d", "place_block"));

    // Le Codec qui explique à Minecraft comment transformer ce record en bytes et inversement
    // BlockPos.PACKET_CODEC existe dans ton fichier, c'est parfait !
    public static final PacketCodec<RegistryByteBuf, PlaceBlockPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, PlaceBlockPayload::pos,
            PacketCodecs.INTEGER, PlaceBlockPayload::stateId,
            PlaceBlockPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}