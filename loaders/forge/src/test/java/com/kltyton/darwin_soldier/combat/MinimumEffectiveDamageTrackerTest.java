package com.kltyton.darwin_soldier.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimumEffectiveDamageTrackerTest {
    @Test
    void forgeFakePlayersAreExcludedFromPlayerGrowthDamage() {
        assertFalse(MinimumEffectiveDamageTracker.isEligiblePlayerSourceType(FakePlayer.class),
                "Forge fake players, including Create deployers, must not inherit owner growth damage");
        assertTrue(MinimumEffectiveDamageTracker.isEligiblePlayerSourceType(ServerPlayer.class),
                "Real server players must remain eligible for growth damage");
    }
}
