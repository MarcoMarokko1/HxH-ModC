package com.hxhmod.client;

import com.hxhmod.HxhMod;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Spielt Körper-Animationen aus assets/hxhmod/player_animation/*.json über die PlayerAnimator-Bibliothek. */
public final class ClientAnim {
    public static final ResourceLocation LAYER = new ResourceLocation(HxhMod.MODID, "animation");

    private static IAnimation createLayer(AbstractClientPlayer player) {
        return new ModifierLayer<>();
    }

    public static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 42, ClientAnim::createLayer);
    }

    @SuppressWarnings("unchecked")
    public static void play(int entityId, String name) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Entity e = mc.level.getEntity(entityId);
            if (!(e instanceof AbstractClientPlayer player)) return;
            Object layer = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER);
            if (!(layer instanceof ModifierLayer<?>)) return;
            KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(new ResourceLocation(HxhMod.MODID, name));
            if (anim == null) {
                HxhMod.LOGGER.warn("Animation nicht gefunden: {}", name);
                return;
            }
            ((ModifierLayer<IAnimation>) layer).setAnimation(new KeyframeAnimationPlayer(anim));
        } catch (Throwable t) {
            HxhMod.LOGGER.warn("Animation konnte nicht abgespielt werden: {}", name, t);
        }
    }

    private ClientAnim() {}
}
