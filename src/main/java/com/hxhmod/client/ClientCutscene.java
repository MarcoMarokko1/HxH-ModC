package com.hxhmod.client;

import com.hxhmod.HxhMod;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * Kamera-Cutscenes bei großen Attacken: Die Kamera wird auf eine unsichtbare Hilfsfigur gesetzt, die entlang
 * vorgegebener Keyframes fliegt (Zoom, Fahrt, harte Schnitte). Esc bricht ab. Eingaben sind währenddessen gesperrt.
 */
@Mod.EventBusSubscriber(modid = HxhMod.MODID, value = Dist.CLIENT)
public final class ClientCutscene {

    /** frame 0 = relativ zum Anwender (vorn = Blickrichtung), frame 1 = relativ zum Fokuspunkt (vorn = weg vom Anwender). */
    private record Key(float t, boolean cut, int frame, float cr, float cu, float cf, float tr, float tu, float tf, float fov) {}

    private static Key k(float t, int fr, float cr, float cu, float cf, float tr, float tu, float tf, float fov) {
        return new Key(t, false, fr, cr, cu, cf, tr, tu, tf, fov);
    }

    private static Key cut(float t, int fr, float cr, float cu, float cf, float tr, float tu, float tf, float fov) {
        return new Key(t, true, fr, cr, cu, cf, tr, tu, tf, fov);
    }

    private static final Map<String, Key[]> PATHS = new HashMap<>();

    static {
        // Jajanken Gu: Nahaufnahme der Faust, harter Schnitt auf Krater-Totale, Orbit, Rückflug
        PATHS.put("gu", new Key[]{
                k(0.00f, 0, -2.6f, 0.5f, 2.2f, 0f, 1.2f, 0.6f, 0.95f),
                k(0.20f, 0, -1.5f, 0.3f, 1.5f, 0.2f, 1.3f, 0.7f, 0.75f),
                k(0.37f, 0, -0.9f, 0.6f, 1.1f, 0.3f, 1.4f, 0.9f, 0.55f),
                cut(0.385f, 1, 3.5f, 1.5f, -2.0f, 0f, 0.5f, 0f, 1.0f),
                k(0.55f, 1, 4.5f, 2.4f, -0.5f, 0f, 0.8f, 0f, 1.1f),
                k(0.80f, 1, 3.0f, 3.5f, 2.5f, 0f, 0.5f, 0f, 1.05f),
                k(1.00f, 0, 0f, 1.62f, 0f, 0f, 1.62f, 6f, 1.0f)});
        // Jajanken Pa: Zoom auf die Hand, Schnitt über die Schulter, Welle fliegt weg
        PATHS.put("pa", new Key[]{
                k(0.00f, 0, 1.8f, 1.0f, 1.8f, 0f, 1.3f, 0.6f, 0.8f),
                k(0.26f, 0, 1.2f, 1.3f, 1.4f, 0f, 1.3f, 1.0f, 0.6f),
                cut(0.28f, 0, -1.4f, 1.5f, -2.0f, 0f, 1.3f, 8f, 1.15f),
                k(0.60f, 0, -2.5f, 2.2f, -3.0f, 0f, 1.0f, 8f, 1.1f),
                k(1.00f, 0, 0f, 1.62f, 0f, 0f, 1.62f, 6f, 1.0f)});
        // Jajanken Choki: kurzer Zoom, Schnitt hinter den Rücken
        PATHS.put("choki", new Key[]{
                k(0.00f, 0, 1.6f, 1.2f, 1.6f, 0f, 1.3f, 0.8f, 0.8f),
                k(0.28f, 0, 1.0f, 1.3f, 1.1f, 0f, 1.3f, 1.0f, 0.65f),
                cut(0.30f, 0, 0.5f, 1.6f, -1.8f, 0f, 1.2f, 10f, 1.12f),
                k(0.65f, 0, -0.6f, 1.9f, -2.8f, 0f, 1.2f, 10f, 1.1f),
                k(1.00f, 0, 0f, 1.62f, 0f, 0f, 1.62f, 6f, 1.0f)});
        // Narukami: Froschperspektive auf den erhobenen Arm, Schnitt auf die Einschlagstelle
        PATHS.put("narukami", new Key[]{
                k(0.00f, 0, 0f, 0.15f, 2.2f, 0f, 2.6f, 0f, 0.85f),
                k(0.14f, 0, 1.0f, 0.3f, 1.6f, 0f, 3.0f, 0f, 0.7f),
                k(0.27f, 0, 0.6f, 0.5f, 1.2f, 0f, 3.2f, 0f, 0.6f),
                cut(0.285f, 1, 5f, 0.6f, -6f, 0f, 2f, 0f, 0.9f),
                k(0.45f, 1, 4f, 1.5f, -4f, 0f, 1.5f, 0f, 0.8f),
                k(0.70f, 1, 6f, 4f, 2f, 0f, 1f, 0f, 1.05f),
                k(1.00f, 0, 0f, 1.62f, 0f, 0f, 1.62f, 6f, 1.0f)});
        // Drachenlanze: Zoom auf die Hand, Schnitt über die Schulter, Kamera folgt dem Drachen
        PATHS.put("lance", new Key[]{
                k(0.00f, 0, -1.6f, 1.6f, 1.8f, 0f, 1.4f, 0.6f, 0.85f),
                k(0.27f, 0, -1.0f, 1.5f, 1.2f, 0f, 1.4f, 1.2f, 0.65f),
                cut(0.31f, 0, 1.4f, 1.7f, -1.8f, 0f, 1.4f, 10f, 1.15f),
                k(0.60f, 0, 2.4f, 2.2f, -2.6f, 0f, 1.2f, 14f, 1.1f),
                k(1.00f, 0, 0f, 1.62f, 0f, 0f, 1.62f, 6f, 1.0f)});
        // Dragon Dive: Froschperspektive auf die erhobenen Arme, Schnitt auf die Totale mit dem Drachen über dem Ziel
        PATHS.put("dive", new Key[]{
                k(0.00f, 0, 0f, 0.2f, 2.0f, 0f, 3.0f, 0f, 0.85f),
                k(0.16f, 0, 0.8f, 0.4f, 1.4f, 0f, 3.4f, 0f, 0.65f),
                cut(0.19f, 1, 7f, 3.5f, -9f, 0f, 6f, 0f, 0.95f),
                k(0.35f, 1, 8f, 5f, -7f, 0f, 2f, 0f, 1.0f),
                k(0.65f, 1, 6f, 9f, -2f, 0f, 0.5f, 0f, 1.05f),
                k(1.00f, 0, 0f, 1.62f, 0f, 0f, 1.62f, 6f, 1.0f)});
        // Drachensturm: Drache windet sich hoch, Schnitt auf den Sturzflug, Nahaufnahme beim Einschlag, Vogelperspektive
        PATHS.put("ult", new Key[]{
                k(0.00f, 0, 3.5f, 0.4f, 3.5f, 0f, 2.5f, 0f, 0.9f),
                k(0.12f, 0, -3.5f, 1.0f, 2.5f, 0f, 5f, 0f, 0.85f),
                k(0.24f, 0, 0f, 2.0f, 6.0f, 0f, 10f, 0f, 0.8f),
                cut(0.25f, 1, 9f, 6f, -12f, 0f, 3f, 0f, 0.9f),
                k(0.36f, 1, 5f, 1.5f, -6f, 0f, 1f, 0f, 0.7f),
                k(0.55f, 1, 10f, 8f, -4f, 0f, 2f, 0f, 1.05f),
                k(0.80f, 1, 2f, 15f, -3f, 0f, 0f, 0f, 1.1f),
                k(1.00f, 0, 0f, 1.62f, 0f, 0f, 1.62f, 6f, 1.0f)});
        // Kokon: Umkreisung des Ziels, während der Kokon wächst
        PATHS.put("cocoon", new Key[]{
                k(0.00f, 1, 3f, 1.6f, -4f, 0f, 1f, 0f, 0.85f),
                k(0.50f, 1, -3f, 1.2f, -3.5f, 0f, 1f, 0f, 0.7f),
                k(0.85f, 1, 0f, 2.5f, -3f, 0f, 1f, 0f, 0.65f),
                k(1.00f, 0, 0f, 1.62f, 0f, 0f, 1.62f, 6f, 1.0f)});
        // Schwarmsturm: Flügel öffnen sich, Schnitt auf das Schwarm-Zentrum, Umkreisung
        PATHS.put("swarm", new Key[]{
                k(0.00f, 0, 0f, 1.0f, 3.0f, 0f, 2.0f, 0f, 0.9f),
                k(0.20f, 0, 2.5f, 2.2f, 2.5f, 0f, 2.4f, 0f, 0.8f),
                cut(0.26f, 1, 8f, 6f, -10f, 0f, 2f, 0f, 0.95f),
                k(0.60f, 1, -9f, 8f, -6f, 0f, 1f, 0f, 1.0f),
                k(1.00f, 0, 0f, 1.62f, 0f, 0f, 1.62f, 6f, 1.0f)});
        // Wirbelsturm: Orbit um den drehenden Anwender
        PATHS.put("whirlwind", new Key[]{
                k(0.00f, 0, 0f, 2.2f, 3.2f, 0f, 1f, 0f, 0.95f),
                k(0.25f, 0, 3.2f, 2.0f, 0.5f, 0f, 1f, 0f, 0.85f),
                k(0.50f, 0, 0.5f, 1.8f, -3.2f, 0f, 1f, 0f, 0.85f),
                k(0.75f, 0, -3.2f, 1.8f, -0.5f, 0f, 1f, 0f, 0.9f),
                k(1.00f, 0, 0f, 1.62f, 0f, 0f, 1.62f, 6f, 1.0f)});
    }

    private static boolean active;
    private static int tick, duration;
    private static float[] times, fovs;
    private static boolean[] cuts;
    private static Vec3[] camW, tgtW;
    private static ArmorStand cam;
    private static CameraType prevType;
    private static float lockYaw, lockPitch, curFov = 1f;

    public static boolean isActive() { return active; }

    private static Vec3 world(Vec3 origin, Vec3 fwd, float r, float u, float f) {
        Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
        return origin.add(right.scale(r)).add(0, u, 0).add(fwd.scale(f));
    }

    public static void start(String id, int ticks, Vec3 focus) {
        Minecraft mc = Minecraft.getInstance();
        if (active || mc.player == null || mc.level == null || mc.screen != null) return;
        Key[] keys = PATHS.get(id);
        if (keys == null || ticks < 4) return;

        float yaw = mc.player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 o0 = mc.player.position();
        Vec3 f0 = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 d1 = focus.subtract(o0).multiply(1, 0, 1);
        Vec3 f1 = d1.lengthSqr() < 0.01 ? f0 : d1.normalize();

        int n = keys.length;
        times = new float[n];
        fovs = new float[n];
        cuts = new boolean[n];
        camW = new Vec3[n];
        tgtW = new Vec3[n];
        for (int i = 0; i < n; i++) {
            Key key = keys[i];
            Vec3 origin = key.frame == 0 ? o0 : focus;
            Vec3 fwd = key.frame == 0 ? f0 : f1;
            times[i] = key.t;
            fovs[i] = key.fov;
            cuts[i] = key.cut;
            camW[i] = world(origin, fwd, key.cr, key.cu, key.cf);
            tgtW[i] = world(origin, fwd, key.tr, key.tu, key.tf);
        }

        cam = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
        cam.setNoGravity(true);
        lockYaw = mc.player.getYRot();
        lockPitch = mc.player.getXRot();
        prevType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        duration = ticks;
        tick = 0;
        active = true;
        apply(0f);
        mc.setCameraEntity(cam);
        mc.gameRenderer.setRenderHand(false);
    }

    private static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (!active) return;
        active = false;
        curFov = 1f;
        if (mc.player != null) mc.setCameraEntity(mc.player);
        if (prevType != null) mc.options.setCameraType(prevType);
        mc.gameRenderer.setRenderHand(true);
        cam = null;
    }

    private static void apply(float partial) {
        float time = Mth.clamp((tick + partial) / (float) duration, 0f, 1f);
        int i = 0;
        while (i < times.length - 2 && time >= times[i + 1]) i++;
        Vec3 pos, tgt;
        float fov;
        if (cuts[i + 1] && time < times[i + 1]) {
            pos = camW[i];
            tgt = tgtW[i];
            fov = fovs[i];
        } else {
            float u = times[i + 1] > times[i] ? (time - times[i]) / (times[i + 1] - times[i]) : 1f;
            u = Mth.clamp(u, 0f, 1f);
            u = u * u * (3f - 2f * u);
            pos = camW[i].lerp(camW[i + 1], u);
            tgt = tgtW[i].lerp(tgtW[i + 1], u);
            fov = Mth.lerp(u, fovs[i], fovs[i + 1]);
        }
        curFov = fov;
        Vec3 d = tgt.subtract(pos);
        double hor = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (-(Mth.atan2(d.y, hor) * Mth.RAD_TO_DEG));
        cam.setPos(pos.x, pos.y - cam.getEyeHeight(), pos.z);
        cam.xo = cam.xOld = cam.getX();
        cam.yo = cam.yOld = cam.getY();
        cam.zo = cam.zOld = cam.getZ();
        cam.setYRot(yaw);
        cam.setXRot(pitch);
        cam.yRotO = yaw;
        cam.xRotO = pitch;
        cam.yHeadRot = yaw;
        cam.yHeadRotO = yaw;
    }

    @SubscribeEvent
    public static void renderTick(TickEvent.RenderTickEvent e) {
        if (!active || e.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || cam == null) {
            stop();
            return;
        }
        apply(e.renderTickTime);
        mc.player.setYRot(lockYaw);
        mc.player.setXRot(lockPitch);
        mc.player.yRotO = lockYaw;
        mc.player.xRotO = lockPitch;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent e) {
        if (!active || e.phase != TickEvent.Phase.END) return;
        if (++tick >= duration) stop();
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov e) {
        if (active) e.setFOV(e.getFOV() * curFov);
    }

    @SubscribeEvent
    public static void noInteract(InputEvent.InteractionKeyMappingTriggered e) {
        if (active) {
            e.setCanceled(true);
            e.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void noMove(MovementInputUpdateEvent e) {
        if (!active) return;
        e.getInput().leftImpulse = 0;
        e.getInput().forwardImpulse = 0;
        e.getInput().jumping = false;
        e.getInput().shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void hideHud(RenderGuiOverlayEvent.Pre e) {
        if (active && !e.getOverlay().id().getPath().equals("cutscene_bars")) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        active = false;
        curFov = 1f;
        cam = null;
    }

    /** Schwarze Kinobalken oben und unten, blenden sanft ein und aus. */
    public static final IGuiOverlay BARS = (gui, g, partialTick, w, h) -> {
        if (!active) return;
        float t = tick + partialTick;
        float k = Mth.clamp(Math.min(t / 6f, (duration - t) / 6f), 0f, 1f);
        int bh = (int) (h * 0.11f * k);
        g.fill(0, 0, w, bh, 0xFF000000);
        g.fill(0, h - bh, w, h, 0xFF000000);
    };

    private ClientCutscene() {}
}
