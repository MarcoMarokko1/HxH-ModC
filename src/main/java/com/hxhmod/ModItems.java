package com.hxhmod;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HxhMod.MODID);

    public static final RegistryObject<Item> WATER_GLASS =
            ITEMS.register("water_glass", () -> new WaterGlassItem(new Item.Properties().stacksTo(16)));

    public static void addToTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            e.accept(WATER_GLASS);
        }
    }

    private ModItems() {}
}
