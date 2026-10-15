package com.kltyton.darwin_soldier.platform;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.ForgeMod;

public final class PlayerAccess {
    private PlayerAccess() {}
    public static boolean fakePlayerType(Class<?> type) { return FakePlayer.class.isAssignableFrom(type); }
    public static double entityReach(ServerPlayer player) { return player.getAttributeValue(ForgeMod.ENTITY_REACH.get()); }
}
