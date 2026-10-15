package com.kltyton.darwin_soldier.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class StressSound {
    private StressSound() {}
    public static void play(ServerPlayer player) {
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.05F);
    }
}
