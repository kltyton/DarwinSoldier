package com.kltyton.darwin_soldier.data.generation;

import com.kltyton.darwin_soldier.Darwin_soldier;
import com.kltyton.darwin_soldier.combat.DarwinDamageTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public final class DarwinDamageTypeTagsProvider extends DamageTypeTagsProvider {
    public DarwinDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                        ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Darwin_soldier.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(DamageTypeTags.BYPASSES_COOLDOWN).add(
                DarwinDamageTypes.HUNTING_SHOCK,
                DarwinDamageTypes.INTERNAL_INJURY,
                DarwinDamageTypes.MINIMUM_DAMAGE_CORRECTION);
        tag(DamageTypeTags.NO_IMPACT).add(
                DarwinDamageTypes.HUNTING_SHOCK,
                DarwinDamageTypes.INTERNAL_INJURY,
                DarwinDamageTypes.MINIMUM_DAMAGE_CORRECTION);
        tag(DamageTypeTags.BYPASSES_ARMOR).add(DarwinDamageTypes.MINIMUM_DAMAGE_CORRECTION);
        tag(DamageTypeTags.BYPASSES_SHIELD).add(DarwinDamageTypes.MINIMUM_DAMAGE_CORRECTION);
        tag(DamageTypeTags.BYPASSES_EFFECTS).add(DarwinDamageTypes.MINIMUM_DAMAGE_CORRECTION);
        tag(DamageTypeTags.BYPASSES_RESISTANCE).add(DarwinDamageTypes.MINIMUM_DAMAGE_CORRECTION);
        tag(DamageTypeTags.BYPASSES_ENCHANTMENTS).add(DarwinDamageTypes.MINIMUM_DAMAGE_CORRECTION);
    }
}
