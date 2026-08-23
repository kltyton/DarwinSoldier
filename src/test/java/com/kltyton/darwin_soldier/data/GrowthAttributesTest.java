package com.kltyton.darwin_soldier.data;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.kltyton.darwin_soldier.config.DarwinConfig;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GrowthAttributesTest {
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("19775021-3f12-40f9-83e4-b23ed9555210");
    private static final UUID HEALTH_PROTECTION_MODIFIER_ID =
            UUID.fromString("7b3d0f4a-9d2c-4f5e-8a1b-3c9d0e1f2a34");
    private static final UUID FOREIGN_MODIFIER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @BeforeAll
    static void loadDefaultConfig() {
        CommentedConfig config = CommentedConfig.inMemory();
        DarwinConfig.SPEC.correct(config);
        DarwinConfig.SPEC.setConfig(config);
    }

    @Test
    void healthContributionFollowsCoreHealthGrowthAndIgnoresDerivedAttributesFlag() {
        PlayerGrowthData data = new PlayerGrowthData();
        data.setEnabled(true);
        data.setHealthPointsDebug(10);

        DarwinConfig.DERIVED_ATTRIBUTES_ENABLED.set(false);
        try {
            assertEquals(10.0D * DarwinConfig.HEALTH_PER_POINT.get(),
                    GrowthAttributes.protectedHealthContribution(data), 1.0E-6D);
        } finally {
            DarwinConfig.DERIVED_ATTRIBUTES_ENABLED.set(true);
        }
    }

    @Test
    void disabledPlayerHasNoProtectedHealthContribution() {
        PlayerGrowthData data = new PlayerGrowthData();
        data.setHealthPointsDebug(10);

        assertEquals(0.0D, GrowthAttributes.protectedHealthContribution(data), 1.0E-6D);
    }

    @Test
    void zeroHealthPointsHaveNoProtectedHealthContribution() {
        PlayerGrowthData data = new PlayerGrowthData();
        data.setEnabled(true);

        assertEquals(0.0D, GrowthAttributes.protectedHealthContribution(data), 1.0E-6D);
    }

    @Test
    void exactOwnedModifierIsLeftUntouched() {
        AttributeInstance instance = healthInstance();
        AttributeModifier existing = new AttributeModifier(HEALTH_MODIFIER_ID, "existing growth", 15.0D,
                AttributeModifier.Operation.ADDITION);
        instance.addPermanentModifier(existing);

        GrowthAttributes.ensureOwnedModifier(instance, HEALTH_MODIFIER_ID, "Darwin soldier health growth", 15.0D);

        assertSame(existing, instance.getModifier(HEALTH_MODIFIER_ID));
        assertEquals(1, instance.getModifiers().size());
        assertEquals(35.0D, instance.getValue(), 1.0E-6D);
    }

    @Test
    void missingOwnedModifierIsAddedWithExactAmount() {
        AttributeInstance instance = healthInstance();

        GrowthAttributes.ensureOwnedModifier(instance, HEALTH_MODIFIER_ID, "Darwin soldier health growth", 15.0D);

        AttributeModifier applied = instance.getModifier(HEALTH_MODIFIER_ID);
        assertNotNull(applied);
        assertEquals(15.0D, applied.getAmount(), 1.0E-6D);
        assertEquals(AttributeModifier.Operation.ADDITION, applied.getOperation());
        assertEquals(35.0D, instance.getValue(), 1.0E-6D);
    }

    @Test
    void wrongOwnedAmountIsReplacedAndForeignModifiersSurvive() {
        AttributeInstance instance = healthInstance();
        instance.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER_ID, "stale growth", 7.0D,
                AttributeModifier.Operation.ADDITION));
        AttributeModifier foreign = new AttributeModifier(FOREIGN_MODIFIER_ID, "foreign penalty", -20.0D,
                AttributeModifier.Operation.ADDITION);
        instance.addPermanentModifier(foreign);

        GrowthAttributes.ensureOwnedModifier(instance, HEALTH_MODIFIER_ID, "Darwin soldier health growth", 15.0D);

        assertEquals(15.0D, instance.getModifier(HEALTH_MODIFIER_ID).getAmount(), 1.0E-6D);
        assertSame(foreign, instance.getModifier(FOREIGN_MODIFIER_ID));
        assertEquals(2, instance.getModifiers().size());
        assertEquals(15.0D, instance.getValue(), 1.0E-6D);
    }

    @Test
    void zeroAmountRemovesOnlyOwnedModifier() {
        AttributeInstance instance = healthInstance();
        instance.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER_ID, "growth", 15.0D,
                AttributeModifier.Operation.ADDITION));
        AttributeModifier foreign = new AttributeModifier(FOREIGN_MODIFIER_ID, "foreign", 3.0D,
                AttributeModifier.Operation.ADDITION);
        instance.addPermanentModifier(foreign);

        GrowthAttributes.ensureOwnedModifier(instance, HEALTH_MODIFIER_ID, "Darwin soldier health growth", 0.0D);

        assertNull(instance.getModifier(HEALTH_MODIFIER_ID));
        assertSame(foreign, instance.getModifier(FOREIGN_MODIFIER_ID));
        assertEquals(23.0D, instance.getValue(), 1.0E-6D);
    }

    @Test
    void widenedAttributeReceivesTheFullDarwinFloorAbove1024() {
        AttributeInstance instance = highRangeHealthInstance();
        instance.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER_ID, "Darwin health growth", 2_000.0D,
                AttributeModifier.Operation.ADDITION));
        instance.addPermanentModifier(new AttributeModifier(FOREIGN_MODIFIER_ID, "foreign death penalty", -600.0D,
                AttributeModifier.Operation.ADDITION));

        double needed = ProtectedHealthMath.compensationNeeded(instance.getBaseValue(), instance.getModifiers(),
                HEALTH_PROTECTION_MODIFIER_ID, 2_000.0D);
        instance.addTransientModifier(new AttributeModifier(HEALTH_PROTECTION_MODIFIER_ID, "protection", needed,
                AttributeModifier.Operation.ADDITION));

        assertEquals(2_000.0D, instance.getValue(), 1.0E-6D);
    }

    private static AttributeInstance healthInstance() {
        Attribute attribute = new RangedAttribute("darwin_soldier.test.max_health", 20.0D, 0.0D, 1024.0D);
        AttributeInstance instance = new AttributeInstance(attribute, ignored -> {
        });
        instance.setBaseValue(20.0D);
        return instance;
    }

    private static AttributeInstance highRangeHealthInstance() {
        Attribute attribute = new RangedAttribute("darwin_soldier.test.widened_max_health",
                20.0D, 0.0D, 1_000_000.0D);
        AttributeInstance instance = new AttributeInstance(attribute, ignored -> {
        });
        instance.setBaseValue(20.0D);
        return instance;
    }
}
