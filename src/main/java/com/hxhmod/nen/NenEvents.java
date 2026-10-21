package com.hxhmod.nen;

import com.hxhmod.HxhConfig;
import com.hxhmod.HxhMod;
import com.hxhmod.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HxhMod.MODID)
public final class NenEvents {

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) Scheduler.tick();
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent e) {
        Scheduler.clear();
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        NenType type = NenData.getType(p);
        if (type == null) return;
        ServerLevel level = p.serverLevel();

        int stance = NenData.getStance(p);
        float max = NenData.maxAura(p);
        double regen = HxhConfig.AURA_REGEN.get() / 20.0;
        switch (stance) {
            case 1 -> regen *= HxhConfig.TEN_REGEN_MULT.get();
            case 2 -> regen *= HxhConfig.ZETSU_REGEN_MULT.get();
            case 3 -> regen = -HxhConfig.REN_DRAIN.get() / 20.0;
            default -> { }
        }
        float aura = Mth.clamp((float) (NenData.getAura(p) + regen), 0, max);
        NenData.setAura(p, aura);

        if (stance == 3) {
            if (aura <= 0) {
                NenData.setStance(p, 0);
                p.displayClientMessage(Component.translatable("msg.hxhmod.ren_exhausted"), true);
                ModNetwork.sync(p);
            } else {
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, NenData.getLevel(p) >= 6 ? 1 : 0, true, false));
            }
        } else if (stance == 2) {
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, true, false));
        }

        if (p.tickCount % 4 == 0) FX.aura(level, p, type, stance);
        if (p.tickCount % 5 == 0) ModNetwork.sync(p);

        // Riesen-Aufprall: nach dem Sprung zu Boden schmettern, bei Landung Schockwelle
        Long armed = Pending.SLAM.get(p.getUUID());
        if (armed != null) {
            long age = Scheduler.now() - armed;
            Vec3 m = p.getDeltaMovement();
            if (age == 9) {
                p.setDeltaMovement(m.x * 0.2, -2.4, m.z * 0.2);
                p.hurtMarked = true;
            }
            if (age > 9) FX.line(level, NenType.ENHANCER.dust(1.4f), p.position().add(0, 2, 0), p.position(), 0.5);
            if (age > 10 && p.onGround()) {
                Pending.SLAM.remove(p.getUUID());
                Abilities.slamImpact(p, level);
            } else if (age > 90) {
                Pending.SLAM.remove(p.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || NenData.getType(p) == null) return;
        if (e.getSource().is(DamageTypeTags.IS_FALL) && Pending.noFall(p)) {
            e.setCanceled(true);
            return;
        }
        int stance = NenData.getStance(p);
        if (stance == 1) {
            double red = HxhConfig.TEN_REDUCTION.get();
            float amount = e.getAmount();
            double cost = amount * red * HxhConfig.TEN_AURA_PER_DAMAGE.get();
            double eff = cost > 0 ? Math.min(1.0, NenData.getAura(p) / cost) : 1.0;
            if (eff > 0) {
                e.setAmount((float) (amount * (1 - red * eff)));
                NenData.setAura(p, (float) (NenData.getAura(p) - cost * eff));
                FX.burst(p.serverLevel(), NenData.getType(p).dust(1.3f), p.position().add(0, 1, 0), 10, 0.1);
            }
        } else if (stance == 2) {
            e.setAmount((float) (e.getAmount() * HxhConfig.ZETSU_TAKEN.get()));
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        CompoundTag old = e.getOriginal().getPersistentData();
        if (old.contains(Player.PERSISTED_NBT_TAG)) {
            e.getEntity().getPersistentData().put(Player.PERSISTED_NBT_TAG, old.getCompound(Player.PERSISTED_NBT_TAG).copy());
            if (e.isWasDeath()) NenData.setStance(e.getEntity(), 0);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) ModNetwork.sync(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) ModNetwork.sync(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        Cooldowns.clear(e.getEntity().getUUID());
        Pending.clear(e.getEntity().getUUID());
        Combos.clear(e.getEntity().getUUID());
    }

    private NenEvents() {}
}
