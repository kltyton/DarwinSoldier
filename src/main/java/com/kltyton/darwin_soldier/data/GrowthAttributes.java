package com.kltyton.darwin_soldier.data;

import com.kltyton.darwin_soldier.config.DarwinConfig;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class GrowthAttributes {
    private static final double MAX_REPRESENTABLE_HEALTH = Float.MAX_VALUE;
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("19775021-3f12-40f9-83e4-b23ed9555210");
    private static final UUID ATTACK_MODIFIER_ID = UUID.fromString("3fbdcd4c-8673-45d4-a102-567f6c4c90b0");
    private static final UUID DEFENSE_MODIFIER_ID = UUID.fromString("5d7ed4bf-dbd2-4ee9-9215-5b911bfc2ba1");
    private static final UUID ARMOR_TOUGHNESS_MODIFIER_ID = UUID.fromString("90502534-5e36-45fc-b7da-45bd348f1d2a");
    private static final UUID HEALTH_PROTECTION_MODIFIER_ID = UUID.fromString("7b3d0f4a-9d2c-4f5e-8a1b-3c9d0e1f2a34");
    private static final String HEALTH_PROTECTION_MODIFIER_NAME = "Darwin soldier health protection";

    private GrowthAttributes() {
    }

    public static void apply(ServerPlayer player, PlayerGrowthData data) {
        ensureOwnedModifier(player.getAttribute(Attributes.MAX_HEALTH), HEALTH_MODIFIER_ID,
                "Darwin soldier health growth", protectedHealthContribution(data));
        applyModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_MODIFIER_ID,
                "Darwin soldier attack growth", data.isEnabled() ? data.getAttackPoints() * attackPerPoint() : 0.0D);
        applyModifier(player.getAttribute(Attributes.ARMOR), DEFENSE_MODIFIER_ID,
                "Darwin soldier defense growth", data.isEnabled() ? data.getDefensePoints() * defensePerPoint() : 0.0D);
        applyModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), ARMOR_TOUGHNESS_MODIFIER_ID,
                "Darwin soldier armor toughness growth", data.isEnabled() ? getArmorToughnessBonus(data) : 0.0D);

        reconcileProtectedHealth(player, data);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * Reconciles the deterministic transient compensation modifier so effective
     * MAX_HEALTH is at least the max-health contribution earned from Darwin
     * health points. The owned permanent health growth modifier is re-ensured
     * first with the exact current contribution, so the deficit is only ever
     * caused by foreign modifiers. The compensation is removed when the deficit
     * disappears; base health and foreign modifiers are never restored or
     * rewritten, and current HP is never healed here.
     */
    public static void reconcileProtectedHealth(ServerPlayer player, PlayerGrowthData data) {
        AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
        if (instance == null) {
            return;
        }
        double contribution = protectedHealthContribution(data);
        ensureOwnedModifier(instance, HEALTH_MODIFIER_ID, "Darwin soldier health growth", contribution);
        if (contribution <= 0.0D) {
            instance.removeModifier(HEALTH_PROTECTION_MODIFIER_ID);
            return;
        }
        double needed = ProtectedHealthMath.compensationNeeded(instance.getBaseValue(), instance.getModifiers(),
                HEALTH_PROTECTION_MODIFIER_ID, contribution);
        if (needed <= 0.0D) {
            instance.removeModifier(HEALTH_PROTECTION_MODIFIER_ID);
            return;
        }
        AttributeModifier current = instance.getModifier(HEALTH_PROTECTION_MODIFIER_ID);
        if (current == null || current.getAmount() != needed) {
            instance.removeModifier(HEALTH_PROTECTION_MODIFIER_ID);
            instance.addTransientModifier(new AttributeModifier(HEALTH_PROTECTION_MODIFIER_ID,
                    HEALTH_PROTECTION_MODIFIER_NAME, needed, AttributeModifier.Operation.ADDITION));
        }
    }

    public static double protectedHealthContribution(PlayerGrowthData data) {
        if (!data.isEnabled()) {
            return 0.0D;
        }
        double contribution = Math.max(0, data.getHealthPoints()) * healthPerPoint();
        if (!Double.isFinite(contribution)) {
            return MAX_REPRESENTABLE_HEALTH;
        }
        return Math.min(MAX_REPRESENTABLE_HEALTH, Math.max(0.0D, contribution));
    }

    public static void fillHealthToMax(ServerPlayer player) {
        player.setHealth(player.getMaxHealth());
    }

    public static double getHealingAmplification(PlayerGrowthData data) {
        if (!data.isEnabled() || !DarwinConfig.DERIVED_ATTRIBUTES_ENABLED.get()) {
            return 0.0D;
        }
        int steps = data.getHealthPoints() / Math.max(1, DarwinConfig.HEALING_AMPLIFICATION_HEALTH_POINTS.get());
        return steps * DarwinConfig.HEALING_AMPLIFICATION_PER_STEP.get();
    }

    public static double healthPerPoint() {
        return DarwinConfig.HEALTH_PER_POINT.get();
    }

    public static double attackPerPoint() {
        return DarwinConfig.ATTACK_PER_POINT.get();
    }

    public static double defensePerPoint() {
        return DarwinConfig.DEFENSE_PER_POINT.get();
    }

    public static double getMinimumEffectiveDamage(PlayerGrowthData data) {
        if (!data.isEnabled() || !DarwinConfig.MINIMUM_EFFECTIVE_DAMAGE_ENABLED.get()) {
            return 0.0D;
        }
        return data.getAttackPoints() * attackPerPoint() * DarwinConfig.MINIMUM_EFFECTIVE_DAMAGE_RATIO.get();
    }

    public static double getArmorPierce(PlayerGrowthData data) {
        if (!data.isEnabled() || !DarwinConfig.DERIVED_ATTRIBUTES_ENABLED.get()) {
            return 0.0D;
        }
        int steps = data.getAttackPoints() / Math.max(1, DarwinConfig.ARMOR_PIERCE_ATTACK_POINTS.get());
        return steps * DarwinConfig.ARMOR_PIERCE_PER_STEP.get();
    }

    public static double getArmorToughnessBonus(PlayerGrowthData data) {
        if (!data.isEnabled() || !DarwinConfig.DERIVED_ATTRIBUTES_ENABLED.get()) {
            return 0.0D;
        }
        int steps = data.getDefensePoints() / Math.max(1, DarwinConfig.ARMOR_TOUGHNESS_DEFENSE_POINTS.get());
        return steps * DarwinConfig.ARMOR_TOUGHNESS_PER_STEP.get();
    }

    private static void applyModifier(AttributeInstance instance, UUID id, String name, double amount) {
        if (instance == null) {
            return;
        }

        instance.removeModifier(id);
        if (amount != 0.0D) {
            instance.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }

    /**
     * Idempotently re-ensures exactly one owned modifier with the given UUID and
     * amount. An existing owned ADDITION modifier with the exact expected amount
     * is left untouched; a missing or wrong owned modifier is replaced; a zero
     * amount removes the owned modifier. Foreign modifiers are never touched.
     */
    static void ensureOwnedModifier(AttributeInstance instance, UUID id, String name, double amount) {
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (amount == 0.0D) {
            if (current != null) {
                instance.removeModifier(id);
            }
            return;
        }
        if (current != null && current.getOperation() == AttributeModifier.Operation.ADDITION
                && current.getAmount() == amount) {
            return;
        }
        instance.removeModifier(id);
        instance.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }
}
