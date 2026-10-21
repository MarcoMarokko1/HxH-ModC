package com.hxhmod.client;

import com.hxhmod.HxhMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import com.hxhmod.ModEntities;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import com.hxhmod.ModParticles;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = HxhMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
        e.register(Keys.USE);
        e.register(Keys.NEXT);
        e.register(Keys.PREV);
        e.register(Keys.TEN);
        e.register(Keys.ZETSU);
        e.register(Keys.REN);
        e.register(Keys.GYO);
        e.register(Keys.EN);
    }

    /** PlayerAnimator-Ebene registrieren. Fehlt die Bibliothek, läuft die Mod trotzdem (nur ohne Körper-Animationen). */
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            try {
                ClientAnim.register();
            } catch (Throwable t) {
                HxhMod.LOGGER.warn("PlayerAnimator nicht gefunden - Körper-Animationen sind deaktiviert.", t);
            }
        });
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.FX.get(), FxRenderer::new);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(FxModel.LAYER, FxModel::create);
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(ModParticles.GLOW.get(), sp -> new NenParticle.Provider(sp, NenParticle.Kind.GLOW));
        e.registerSpriteSet(ModParticles.FLAME.get(), sp -> new NenParticle.Provider(sp, NenParticle.Kind.FLAME));
        e.registerSpriteSet(ModParticles.SPARK.get(), sp -> new NenParticle.Provider(sp, NenParticle.Kind.SPARK));
        e.registerSpriteSet(ModParticles.BLAST.get(), sp -> new NenParticle.Provider(sp, NenParticle.Kind.BLAST));
        e.registerSpriteSet(ModParticles.BLAST_BIG.get(), sp -> new NenParticle.Provider(sp, NenParticle.Kind.BLAST_BIG));
        e.registerSpriteSet(ModParticles.RING.get(), sp -> new NenParticle.Provider(sp, NenParticle.Kind.RING));
        e.registerSpriteSet(ModParticles.STREAK.get(), sp -> new NenParticle.Provider(sp, NenParticle.Kind.STREAK));
        e.registerSpriteSet(ModParticles.DRAGON.get(), sp -> new NenParticle.Provider(sp, NenParticle.Kind.DRAGON));
        e.registerSpriteSet(ModParticles.DRAGON_BIG.get(), sp -> new NenParticle.Provider(sp, NenParticle.Kind.DRAGON_BIG));
        e.registerSpriteSet(ModParticles.BOLT.get(), sp -> new NenParticle.Provider(sp, NenParticle.Kind.BOLT));
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("nen_hud", NenHud.OVERLAY);
        e.registerAboveAll("cutscene_bars", ClientCutscene.BARS);
    }

    private ClientModEvents() {}
}
