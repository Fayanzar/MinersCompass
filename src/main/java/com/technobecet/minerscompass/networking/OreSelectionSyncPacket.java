package com.technobecet.minerscompass.networking;

import com.technobecet.minerscompass.MinersCompassMod;
import com.technobecet.minerscompass.util.DynamicOreType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public record OreSelectionSyncPacket(Set<String> selectedOreTypeIds) implements CustomPayload {
    @Override
    public Id<?> getId() {
        return ID;
    }

    public static final CustomPayload.Id<OreSelectionSyncPacket> ID = new CustomPayload.Id<>(Identifier.of(MinersCompassMod.MOD_ID, "ore_selection_sync"));
    public static final PacketCodec<RegistryByteBuf, OreSelectionSyncPacket> CODEC = PacketCodec.of(OreSelectionSyncPacket::write, OreSelectionSyncPacket::new);

    public OreSelectionSyncPacket(PacketByteBuf buf) {
        this(readOreTypeIds(buf));
    }

    private static Set<String> readOreTypeIds(PacketByteBuf buf) {
        int size = buf.readVarInt();
        Set<String> oreTypeIds = new HashSet<>();
        for (int i = 0; i < size; i++) {
            oreTypeIds.add(buf.readString());
        }
        return oreTypeIds;
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(selectedOreTypeIds.size());
        for (String oreTypeId : selectedOreTypeIds) {
            buf.writeString(oreTypeId);
        }
    }

    public static OreSelectionSyncPacket create(Set<DynamicOreType> selectedOreTypes) {
        Set<String> oreTypeIds = new HashSet<>();
        for (DynamicOreType oreType : selectedOreTypes) {
            oreTypeIds.add(oreType.getId());
        }
        return new OreSelectionSyncPacket(oreTypeIds);
    }
}