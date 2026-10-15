package com.kltyton.darwin_soldier.registry;

import com.kltyton.darwin_soldier.Darwin_soldier;
import com.kltyton.darwin_soldier.item.DarwinSerumItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Darwin_soldier.MODID);

    public static final DeferredHolder<Item, Item> DARWIN_SERUM = ITEMS.register("darwin_serum",
            () -> new DarwinSerumItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
