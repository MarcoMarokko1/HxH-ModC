package com.hxhmod;

import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenLogic;
import com.hxhmod.nen.NenType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Wasserglas-Test: Rechtsklick erweckt Nen und würfelt den Nen-Typ (Spezialisten sind selten). */
public class WaterGlassItem extends Item {
    public WaterGlassItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (NenData.getType(sp) != null) {
                sp.displayClientMessage(Component.translatable("msg.hxhmod.already"), true);
                return InteractionResultHolder.fail(stack);
            }
            NenLogic.awaken(sp, NenType.roll(level.random));
            if (!sp.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
