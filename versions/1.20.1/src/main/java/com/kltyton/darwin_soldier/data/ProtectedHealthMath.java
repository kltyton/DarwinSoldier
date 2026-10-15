package com.kltyton.darwin_soldier.data;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.util.UUID;
import java.util.Collection;

public final class ProtectedHealthMath {
    private ProtectedHealthMath() {}
    public static double compensationNeeded(double base, Collection<AttributeModifier> modifiers, UUID excluded, double contribution) {
        return ProtectedHealthComputation.compensationNeeded(base, modifiers.stream()
                .filter(modifier -> !modifier.getId().equals(excluded))
                .map(modifier -> new ProtectedHealthComputation.Term(switch (modifier.getOperation()) {
                    case ADDITION -> ProtectedHealthComputation.Operation.ADDITION;
                    case MULTIPLY_BASE -> ProtectedHealthComputation.Operation.MULTIPLY_BASE;
                    case MULTIPLY_TOTAL -> ProtectedHealthComputation.Operation.MULTIPLY_TOTAL;
                }, modifier.getAmount())).toList(), contribution);
    }
}
