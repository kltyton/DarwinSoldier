package com.kltyton.darwin_soldier.combat;

import com.kltyton.darwin_soldier.Darwin_soldier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

public final class DarwinDamageTypes {
    public static final ResourceKey<DamageType> HUNTING_SHOCK = key("hunting_shock");
    public static final ResourceKey<DamageType> INTERNAL_INJURY = key("internal_injury");
    public static final ResourceKey<DamageType> MINIMUM_DAMAGE_CORRECTION = key("minimum_damage_correction");

    private DarwinDamageTypes() {
    }

    public static void bootstrap(BootstapContext<DamageType> context) {
        context.register(HUNTING_SHOCK, new DamageType("darwinHuntingShock", DamageScaling.NEVER, 0.0F));
        context.register(INTERNAL_INJURY, new DamageType("darwinInternalInjury", DamageScaling.NEVER, 0.0F));
        context.register(MINIMUM_DAMAGE_CORRECTION, new DamageType("darwinMinimumDamage", DamageScaling.NEVER, 0.0F));
    }

    public static DamageSource huntingShock(ServerLevel level, ServerPlayer attacker) {
        return new DamageSource(holder(level, HUNTING_SHOCK), null, attacker);
    }

    public static DamageSource internalInjury(ServerLevel level, ServerPlayer attacker) {
        return new DamageSource(holder(level, INTERNAL_INJURY), null, attacker);
    }

    public static DamageSource minimumDamageCorrection(ServerLevel level, ServerPlayer attacker) {
        return new DamageSource(holder(level, MINIMUM_DAMAGE_CORRECTION), null, attacker);
    }

    public static boolean isInternal(DamageSource source) {
        return source.is(HUNTING_SHOCK) || source.is(INTERNAL_INJURY) || source.is(MINIMUM_DAMAGE_CORRECTION);
    }

    private static Holder.Reference<DamageType> holder(ServerLevel level, ResourceKey<DamageType> key) {
        return level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key);
    }

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(Darwin_soldier.MODID, path));
    }
}
