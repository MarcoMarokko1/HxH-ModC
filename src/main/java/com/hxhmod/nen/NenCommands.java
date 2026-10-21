package com.hxhmod.nen;

import com.hxhmod.HxhMod;
import com.hxhmod.network.ModNetwork;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * /nen choose gon|killua   (jeder)
 * /nen level <1-10>, /nen fill, /nen reset   (nur mit Cheats / OP)
 */
@Mod.EventBusSubscriber(modid = HxhMod.MODID)
public final class NenCommands {

    private static NenType parse(String id) {
        if (id.equalsIgnoreCase("gon") || id.equalsIgnoreCase("enhancer")) return NenType.ENHANCER;
        if (id.equalsIgnoreCase("killua") || id.equalsIgnoreCase("transmuter")) return NenType.TRANSMUTER;
        if (id.equalsIgnoreCase("xeno") || id.equalsIgnoreCase("zeno")) return NenType.XENO;
        if (id.equalsIgnoreCase("pouf") || id.equalsIgnoreCase("puff") || id.equalsIgnoreCase("shaiapouf")) return NenType.POUF;
        return null;
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("nen")
                .then(Commands.literal("choose").then(Commands.argument("character", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("gon", "killua", "xeno", "pouf"), b))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            NenType t = parse(StringArgumentType.getString(c, "character"));
                            if (t == null) {
                                c.getSource().sendFailure(Component.literal("Wähle gon, killua, xeno oder pouf."));
                                return 0;
                            }
                            NenLogic.awaken(p, t);
                            return 1;
                        })))
                .then(Commands.literal("level").requires(s -> s.hasPermission(2) || s.getServer().isSingleplayer())
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, NenData.MAX_LEVEL))
                                .executes(c -> {
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    NenData.setLevel(p, IntegerArgumentType.getInteger(c, "level"));
                                    NenData.setAura(p, NenData.maxAura(p));
                                    ModNetwork.sync(p);
                                    c.getSource().sendSuccess(() -> Component.literal("Nen-Level: " + NenData.getLevel(p)), false);
                                    return 1;
                                })))
                .then(Commands.literal("fill").requires(s -> s.hasPermission(2) || s.getServer().isSingleplayer()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    NenData.setAura(p, NenData.maxAura(p));
                    for (Ability a : Abilities.ALL) Cooldowns.set(p, a.id, 0);
                    ModNetwork.sync(p);
                    return 1;
                }))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2) || s.getServer().isSingleplayer()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    NenData.reset(p);
                    ModNetwork.sync(p);
                    c.getSource().sendSuccess(() -> Component.literal("Nen zurückgesetzt"), false);
                    return 1;
                })));
    }

    private NenCommands() {}
}
