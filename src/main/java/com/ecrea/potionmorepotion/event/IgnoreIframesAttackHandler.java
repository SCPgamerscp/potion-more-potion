package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import com.ecrea.potionmorepotion.network.ModMessages;
import com.ecrea.potionmorepotion.network.RapidAttackPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side handler for the Ignore I-Frames Blessing.
 * Detects left-click (keyAttack) hold every tick, and if aiming at a living entity,
 * sends a RapidAttackPacket to trigger 100 attacks per tick.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID, value = Dist.CLIENT)
public class IgnoreIframesAttackHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        if (mc.screen != null) {
            return;
        }

        var effectObj = ModMobEffects.EFFECTS.get("ignore_iframes_blessing");
        if (effectObj == null || !player.hasEffect(effectObj.get())) {
            return;
        }

        if (!mc.options.keyAttack.isDown()) {
            return;
        }

        Entity target = mc.crosshairPickEntity;
        if (target == null && mc.hitResult instanceof EntityHitResult entityHit) {
            target = entityHit.getEntity();
        }

        if (target != null && target.isAlive() && target.isAttackable() && target != player) {
            ModMessages.sendToServer(new RapidAttackPacket(target.getId()));
            player.swing(InteractionHand.MAIN_HAND);
        }
    }
}
