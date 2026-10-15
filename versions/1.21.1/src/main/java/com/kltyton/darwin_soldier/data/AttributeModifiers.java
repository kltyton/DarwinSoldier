package com.kltyton.darwin_soldier.data;

import java.util.Collection;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.ResourceLocation;

public final class AttributeModifiers {
    private AttributeModifiers() {}
    private static ResourceLocation key(UUID id) { return ResourceLocation.withDefaultNamespace(id.toString()); }
    public static AttributeModifier get(AttributeInstance instance, UUID id) { return instance.getModifier(key(id)); }
    public static void remove(AttributeInstance instance, UUID id) { instance.removeModifier(key(id)); }
    public static void permanent(AttributeInstance instance, UUID id, String name, double amount) { instance.addPermanentModifier(new AttributeModifier(key(id), amount, AttributeModifier.Operation.ADD_VALUE)); }
    public static void temporary(AttributeInstance instance, UUID id, String name, double amount) { instance.addTransientModifier(new AttributeModifier(key(id), amount, AttributeModifier.Operation.ADD_VALUE)); }
    public static double amount(AttributeModifier modifier) { return modifier.amount(); }
    public static boolean additive(AttributeModifier modifier) { return modifier.operation() == AttributeModifier.Operation.ADD_VALUE; }
    public static double compensation(double base, Collection<AttributeModifier> modifiers, UUID excluded, double contribution) {
        return ProtectedHealthMath.compensationNeeded(base, modifiers, key(excluded), contribution);
    }
}
