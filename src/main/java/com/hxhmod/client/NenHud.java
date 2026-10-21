package com.hxhmod.client;

import com.hxhmod.nen.Abilities;
import com.hxhmod.nen.Ability;
import com.hxhmod.nen.NenType;
import com.hxhmod.HxhMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/** Aura-Leiste, Haltung, gewählte Fähigkeit, Cooldown und Ren-Vignette. */
public final class NenHud {
    public static final IGuiOverlay OVERLAY = (gui, g, partialTick, w, h) -> {
        if (ClientNenData.type < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        NenType t = NenType.byOrdinal(ClientNenData.type);
        if (t == null) return;
        int rgb = t.rgb();
        int col = 0xFF000000 | rgb;

        // Weißer Impact-Blitz
        if (ClientNenData.flashTicks > 0) {
            int fa = Mth.clamp((int) (220f * ClientNenData.flashTicks / (float) Math.max(1, ClientNenData.flashMax)), 0, 255);
            g.fill(0, 0, w, h, (fa << 24) | 0xFFFFFF);
        }

        // Ren-Vignette: pulsierender Rand in Aura-Farbe
        if (ClientNenData.stance == 3) {
            int a = (int) (45 + 25 * Math.sin((mc.player.tickCount + partialTick) * 0.3));
            int c = (a << 24) | rgb;
            g.fillGradient(0, 0, w, 48, c, 0);
            g.fillGradient(0, h - 48, w, h, 0, c);
            g.fillGradient(0, 0, 32, h, c, 0);
            g.fillGradient(w - 32, 0, w, h, 0, c);
        }

        int x = 10, y = h - 50, bw = 110;
        g.drawString(mc.font, Component.translatable("nen.hxhmod.type." + t.id).append("  Lv." + ClientNenData.level), x, y - 22, col, true);
        g.drawString(mc.font, Component.translatable("hud.hxhmod.stance." + ClientNenData.stance), x, y - 11, 0xFFFFFFFF, true);

        // Aura-Leiste
        g.fill(x - 1, y - 1, x + bw + 1, y + 9, 0xFF000000);
        g.fill(x, y, x + bw, y + 8, 0xFF202020);
        float frac = Mth.clamp(ClientNenData.aura / Math.max(1f, ClientNenData.maxAura), 0f, 1f);
        g.fill(x, y, x + (int) (bw * frac), y + 8, col);
        g.drawString(mc.font, (int) ClientNenData.aura + "/" + (int) ClientNenData.maxAura, x + bw + 6, y, 0xFFFFFFFF, true);

        // XP-Leiste (dünn)
        int need = 100 * ClientNenData.level;
        float xf = ClientNenData.level >= 10 ? 1f : Mth.clamp(ClientNenData.xp / (float) need, 0f, 1f);
        g.fill(x, y + 10, x + bw, y + 12, 0xFF202020);
        g.fill(x, y + 10, x + (int) (bw * xf), y + 12, 0xFFFFFFFF);

        // Gewählte Fähigkeit + Icon-Leiste
        List<Ability> list = Abilities.forType(t);
        int selIdx = Math.floorMod(ClientNenData.selected, list.size());
        Ability ab = list.get(selIdx);
        RenderSystem.enableBlend();
        for (int i = 0; i < list.size(); i++) {
            Ability a2 = list.get(i);
            int sx = x + i * 24, sy = y - 50;
            boolean sel = i == selIdx;
            boolean lock = ClientNenData.level < a2.minLevel;
            g.fill(sx - 1, sy - 1, sx + 21, sy + 21, sel ? col : 0xFF303030);
            g.fill(sx, sy, sx + 20, sy + 20, 0xFF101010);
            g.blit(icon(a2.id), sx + 1, sy + 1, 18, 18, 0f, 0f, 32, 32, 32, 32);
            if (lock) g.fill(sx, sy, sx + 20, sy + 20, 0xB0000000);
            else if (sel && ClientNenData.cdLeft > 0) {
                int ch = (int) (20 * (ClientNenData.cdLeft / (float) Math.max(1, ClientNenData.cdTotal)));
                g.fill(sx, sy + 20 - ch, sx + 20, sy + 20, 0x90000000);
            }
        }
        boolean locked = ClientNenData.level < ab.minLevel;
        int tcol = locked ? 0xFFFF5555 : (ClientNenData.aura < ab.cost ? 0xFFFFAA00 : 0xFFFFFFFF);
        MutableComponent line = Component.literal("> ").append(Component.translatable(ab.langKey()))
                .append(locked ? " (Lv." + ab.minLevel + ")" : " [" + ab.cost + "]");
        g.drawString(mc.font, line, x, y + 16, tcol, true);

        // Cooldown
        if (ClientNenData.cdLeft > 0) {
            int cw = (int) (bw * (ClientNenData.cdLeft / (float) Math.max(1, ClientNenData.cdTotal)));
            g.fill(x, y + 27, x + cw, y + 29, 0xFFAAAAAA);
        }
    };

    private static final java.util.Map<String, ResourceLocation> ICONS = new java.util.HashMap<>();

    private static ResourceLocation icon(String id) {
        return ICONS.computeIfAbsent(id, k -> new ResourceLocation(HxhMod.MODID, "textures/gui/ability/" + k + ".png"));
    }

    private NenHud() {}
}
