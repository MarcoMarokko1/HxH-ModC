package com.hxhmod;

import com.hxhmod.nen.FxEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HxhMod.MODID);

    /** Unsichtbarer Träger für die 3D-Effekt-Modelle (Drache, Faust, Klingen, Blitze ...). */
    public static final RegistryObject<EntityType<FxEntity>> FX = ENTITIES.register("fx", () ->
            EntityType.Builder.<FxEntity>of(FxEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .noSave()
                    .noSummon()
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(Integer.MAX_VALUE)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("fx"));

    private ModEntities() {}
}
