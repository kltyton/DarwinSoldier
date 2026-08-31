package com.kltyton.darwin_soldier.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.util.datafix.DataFixTypes;

public class GrowthSavedData extends SavedData {
    private static final String DATA_NAME = "darwin_soldier_growth";
    private static final Factory<GrowthSavedData> FACTORY = new Factory<>(
            GrowthSavedData::new, GrowthSavedData::load, DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);

    private final Map<UUID, PlayerGrowthData> players = new HashMap<>();

    public static GrowthSavedData get(ServerPlayer player) {
        return get(player.getServer());
    }

    public static GrowthSavedData get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getDataStorage()
                .computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static GrowthSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        GrowthSavedData savedData = new GrowthSavedData();
        CompoundTag playersTag = tag.getCompound("Players");
        for (String uuidText : playersTag.getAllKeys()) {
            if (playersTag.contains(uuidText, Tag.TAG_COMPOUND)) {
                try {
                    savedData.players.put(UUID.fromString(uuidText), PlayerGrowthData.load(playersTag.getCompound(uuidText)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore corrupt UUID entries instead of failing the whole server data file.
                }
            }
        }
        if (!savedData.players.isEmpty()) {
            savedData.setDirty();
        }
        return savedData;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag playersTag = new CompoundTag();
        players.forEach((uuid, data) -> playersTag.put(uuid.toString(), data.save()));
        tag.put("Players", playersTag);
        return tag;
    }

    public PlayerGrowthData getOrCreate(UUID uuid) {
        return players.computeIfAbsent(uuid, ignored -> new PlayerGrowthData());
    }
}
