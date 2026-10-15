package com.kltyton.darwin_soldier.data;

import java.util.Collection;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;


public final class AttributeModifiers {
    private AttributeModifiers() {}

    public static AttributeModifier get(AttributeInstance instance, UUID id) { return instance.getModifier(id); }
    public static void remove(AttributeInstance instance, UUID id) { instance.removeModifier(id); }
    public static void permanent(AttributeInstance instance, UUID id, String name, double amount) { instance.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION)); }
    public static void temporary(AttributeInstance instance, UUID id, String name, double amount) { instance.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION)); }
    public static double amount(AttributeModifier modifier) { return modifier.getAmount(); }
    public static boolean additive(AttributeModifier modifier) { return modifier.getOperation() == AttributeModifier.Operation.ADDITION; }
    public static double compensation(double base, Collection<AttributeModifier> modifiers, UUID excluded, double contribution) {
        return ProtectedHealthMath.compensationNeeded(base, modifiers, excluded, contribution);
    }
}
