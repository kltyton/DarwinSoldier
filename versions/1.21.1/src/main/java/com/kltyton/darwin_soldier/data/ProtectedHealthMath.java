package com.kltyton.darwin_soldier.data;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.ResourceLocation;
import java.util.Collection;

public final class ProtectedHealthMath {
    private ProtectedHealthMath() {}
    public static double compensationNeeded(double base, Collection<AttributeModifier> modifiers, ResourceLocation excluded, double contribution) {
        return ProtectedHealthComputation.compensationNeeded(base, modifiers.stream()
                .filter(modifier -> !modifier.id().equals(excluded))
                .map(modifier -> new ProtectedHealthComputation.Term(switch (modifier.operation()) {
                    case ADD_VALUE -> ProtectedHealthComputation.Operation.ADDITION;
                    case ADD_MULTIPLIED_BASE -> ProtectedHealthComputation.Operation.MULTIPLY_BASE;
                    case ADD_MULTIPLIED_TOTAL -> ProtectedHealthComputation.Operation.MULTIPLY_TOTAL;
                }, modifier.amount())).toList(), contribution);
    }
}
