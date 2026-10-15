package com.kltyton.darwin_soldier.data;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.datafix.DataFixTypes;

public abstract class PlatformSavedData extends SavedData {
    protected abstract CompoundTag saveTag(CompoundTag tag);
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { return saveTag(tag); }
    public static <T extends SavedData> T storage(MinecraftServer server, Supplier<T> factory, Function<CompoundTag, T> reader, String id) {
        return server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(factory, (tag, registries) -> reader.apply(tag), DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES), id);
    }
}
