package com.kltyton.darwin_soldier.platform;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Path;

public final class ModEnvironment {
    private ModEnvironment() {}
    public static boolean isLoaded(String id) { return ModList.get().isLoaded(id); }
    public static Path configDirectory() { return FMLPaths.CONFIGDIR.get(); }
}
