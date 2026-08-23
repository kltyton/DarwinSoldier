package com.kltyton.darwin_soldier.registry;

import com.kltyton.darwin_soldier.Darwin_soldier;
import com.kltyton.darwin_soldier.item.DarwinSerumItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Darwin_soldier.MODID);

    public static final RegistryObject<Item> DARWIN_SERUM = ITEMS.register("darwin_serum",
            () -> new DarwinSerumItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
