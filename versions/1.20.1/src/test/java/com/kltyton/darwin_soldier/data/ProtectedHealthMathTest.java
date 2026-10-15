package com.kltyton.darwin_soldier.data;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtectedHealthMathTest {
    private static final UUID COMPENSATION_ID = UUID.fromString("7b3d0f4a-9d2c-4f5e-8a1b-3c9d0e1f2a34");

    @Test
    void unhealthyDyingStyleNegativeAdditionIsCompensatedExactlyToContribution() {
        List<AttributeModifier> modifiers = List.of(
                new AttributeModifier(UUID.randomUUID(), "unhealthy penalty", -20.0D,
                        AttributeModifier.Operation.ADDITION));

        double needed = ProtectedHealthMath.compensationNeeded(20.0D, modifiers, COMPENSATION_ID, 15.0D);

        assertEquals(15.0D, needed, 1.0E-6D);
    }

    @Test
    void compensationIsRemovedWhenNoDeficitRemains() {
        assertEquals(0.0D, ProtectedHealthMath.compensationNeeded(20.0D, List.of(), COMPENSATION_ID, 15.0D),
                1.0E-6D);
    }

    @Test
    void compensationHandlesPositiveTotalScaling() {
        List<AttributeModifier> modifiers = List.of(
                new AttributeModifier(UUID.randomUUID(), "unhealthy penalty", -20.0D,
                        AttributeModifier.Operation.ADDITION),
                new AttributeModifier(UUID.randomUUID(), "scaling", 0.5D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));

        double needed = ProtectedHealthMath.compensationNeeded(20.0D, modifiers, COMPENSATION_ID, 30.0D);

        assertEquals(20.0D, needed, 1.0E-6D);
    }

    @Test
    void positiveScalingAloneDoesNotCreateADeficit() {
        List<AttributeModifier> modifiers = List.of(
                new AttributeModifier(UUID.randomUUID(), "scaling", 0.5D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));

        assertEquals(0.0D, ProtectedHealthMath.compensationNeeded(20.0D, modifiers, COMPENSATION_ID, 30.0D),
                1.0E-6D);
    }

    @Test
    void zeroContributionNeverCompensates() {
        List<AttributeModifier> modifiers = List.of(
                new AttributeModifier(UUID.randomUUID(), "unhealthy penalty", -20.0D,
                        AttributeModifier.Operation.ADDITION));

        assertEquals(0.0D, ProtectedHealthMath.compensationNeeded(20.0D, modifiers, COMPENSATION_ID, 0.0D),
                1.0E-6D);
    }

    @Test
    void degenerateNegativeTotalMultiplierIsNotNeutralized() {
        List<AttributeModifier> modifiers = List.of(
                new AttributeModifier(UUID.randomUUID(), "total -100%", -1.0D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));

        assertEquals(0.0D, ProtectedHealthMath.compensationNeeded(20.0D, modifiers, COMPENSATION_ID, 15.0D),
                1.0E-6D);
    }

    @Test
    void ownCompensationModifierIsExcludedFromTheDeficit() {
        List<AttributeModifier> modifiers = List.of(
                new AttributeModifier(COMPENSATION_ID, "existing compensation", 100.0D,
                        AttributeModifier.Operation.ADDITION),
                new AttributeModifier(UUID.randomUUID(), "unhealthy penalty", -20.0D,
                        AttributeModifier.Operation.ADDITION));

        double needed = ProtectedHealthMath.compensationNeeded(20.0D, modifiers, COMPENSATION_ID, 15.0D);

        assertEquals(15.0D, needed, 1.0E-6D);
    }

    @Test
    void contributionAboveVanillaRangeIsProtectedWithoutA1024Ceiling() {
        List<AttributeModifier> modifiers = List.of(
                new AttributeModifier(UUID.randomUUID(), "Darwin health growth", 2_000.0D,
                        AttributeModifier.Operation.ADDITION),
                new AttributeModifier(UUID.randomUUID(), "foreign death penalty", -600.0D,
                        AttributeModifier.Operation.ADDITION));

        double needed = ProtectedHealthMath.compensationNeeded(
                20.0D, modifiers, COMPENSATION_ID, 2_000.0D);

        assertEquals(580.0D, needed, 1.0E-6D);
    }
}
