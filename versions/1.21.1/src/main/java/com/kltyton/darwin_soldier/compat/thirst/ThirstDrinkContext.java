package com.kltyton.darwin_soldier.compat.thirst;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class ThirstDrinkContext {
    private static final ThreadLocal<Player> PLAYER = new ThreadLocal<>();

    private ThirstDrinkContext() {
    }

    public static void set(Player player) {
        PLAYER.set(player);
    }

    public static void clear() {
        PLAYER.remove();
    }

    @Nullable
    public static Player player() {
        return PLAYER.get();
    }
}
