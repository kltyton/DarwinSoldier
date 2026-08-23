package com.kltyton.darwin_soldier.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombatMathTest {
    @Test
    void minimumDamageMatchesAttackGrowthExample() {
        float minimum = CombatMath.minimumEffectiveDamage(100, 0.4D, 0.75D);

        assertEquals(30.0F, minimum, 0.0001F);
        assertEquals(30.0F, CombatMath.applyMinimum(5.0F, minimum), 0.0001F);
        assertEquals(80.0F, CombatMath.applyMinimum(80.0F, minimum), 0.0001F);
        assertEquals(0.0F, CombatMath.applyMinimum(0.0F, minimum), 0.0001F);
    }

    @Test
    void nonMeleeWindowOnlyAddsTheMissingDifference() {
        assertEquals(7.0F, CombatMath.missingDamage(23.0F, 30.0F), 0.0001F);
        assertEquals(0.0F, CombatMath.missingDamage(30.0F, 30.0F), 0.0001F);
        assertEquals(0.0F, CombatMath.missingDamage(45.0F, 30.0F), 0.0001F);
    }
}
