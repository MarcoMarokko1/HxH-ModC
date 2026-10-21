package com.hxhmod.client;

import com.hxhmod.HxhMod;
import com.hxhmod.network.ActionPacket;
import com.hxhmod.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HxhMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    private static void poll(KeyMapping key, int action) {
        while (key.consumeClick()) ModNetwork.CHANNEL.sendToServer(new ActionPacket(action));
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (ClientNenData.shakeTicks > 0) ClientNenData.shakeTicks--;
        if (ClientNenData.flashTicks > 0) ClientNenData.flashTicks--;
        if (ClientNenData.fovTicks > 0) ClientNenData.fovTicks--;
        if (ClientNenData.cdLeft > 0) ClientNenData.cdLeft--;
        if (mc.player == null || mc.screen != null) return;
        if (ClientCutscene.isActive()) {
            for (KeyMapping km : new KeyMapping[]{Keys.USE, Keys.NEXT, Keys.PREV, Keys.TEN, Keys.ZETSU, Keys.REN, Keys.GYO, Keys.EN})
                while (km.consumeClick()) { }
            return;
        }
        poll(Keys.USE, 0);
        poll(Keys.NEXT, 1);
        poll(Keys.PREV, 2);
        poll(Keys.TEN, 3);
        poll(Keys.ZETSU, 4);
        poll(Keys.REN, 5);
        poll(Keys.GYO, 6);
        poll(Keys.EN, 7);
    }

    /** Linksklick mit leerer Hand und Gegner in der Nähe = Nen-Kombo statt normalem Schlag. */
    @SubscribeEvent
    public static void attack(InputEvent.InteractionKeyMappingTriggered e) {
        if (!e.isAttack() || ClientCutscene.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        if (ClientNenData.type < 0 || ClientNenData.stance == 2) return;
        if (!mc.player.getMainHandItem().isEmpty()) return;
        boolean near = !mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(4.5),
                x -> x != mc.player && x.isAlive() && !(x instanceof ArmorStand)).isEmpty();
        if (!near) return;
        e.setCanceled(true);
        e.setSwingHand(false);
        ModNetwork.CHANNEL.sendToServer(new ActionPacket(8));
    }

    /** FOV-Stoß bei Aufprällen und Godspeed. */
    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov e) {
        if (ClientNenData.fovTicks > 0) {
            e.setFOV(e.getFOV() * (1.0 + ClientNenData.fovKick * Math.min(1.0, ClientNenData.fovTicks / 8.0)));
        }
    }

    /** Kamerawackeln bei großen Attacken. */
    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles e) {
        if (ClientNenData.shakeTicks <= 0) return;
        float f = ClientNenData.shakeIntensity * Math.min(1f, ClientNenData.shakeTicks / 6f);
        var rnd = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.random : null;
        if (rnd == null) return;
        e.setYaw(e.getYaw() + (rnd.nextFloat() - 0.5f) * f * 3f);
        e.setPitch(e.getPitch() + (rnd.nextFloat() - 0.5f) * f * 3f);
        e.setRoll(e.getRoll() + (rnd.nextFloat() - 0.5f) * f * 4f);
    }

    /** Ren: der eigene Charakter "atmet" mit pulsierender Aura (sichtbar in der 3rd-Person-Ansicht). */
    @SubscribeEvent
    public static void renderPre(RenderPlayerEvent.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.getEntity() == mc.player && ClientNenData.stance == 3) {
            float s = 1f + 0.025f * Mth.sin((mc.player.tickCount + e.getPartialTick()) * 0.9f);
            e.getPoseStack().pushPose();
            e.getPoseStack().scale(s, s, s);
        }
    }

    @SubscribeEvent
    public static void renderPost(RenderPlayerEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.getEntity() == mc.player && ClientNenData.stance == 3) e.getPoseStack().popPose();
    }

    private ClientEvents() {}
}
