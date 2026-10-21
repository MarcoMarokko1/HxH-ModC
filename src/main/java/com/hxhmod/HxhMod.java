package com.hxhmod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import com.hxhmod.network.ModNetwork;

@Mod(HxhMod.MODID)
public class HxhMod {
    public static final String MODID = "hxhmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HxhMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModParticles.PARTICLES.register(bus);
        ModEntities.ENTITIES.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(ModItems::addToTab);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, HxhConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(ModNetwork::register);
    }
}
