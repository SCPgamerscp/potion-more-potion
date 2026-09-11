package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side true auto-click handler for Ignore I-Frames Blessing.
 * Detects left-click (keyAttack) hold every tick:
 * - If aiming at an entity (normal mob or multipart boss part such as Ender Dragon parts),
 *   executes 100 true vanilla attack clicks via mc.gameMode.attack().
 * - If aiming at air (MISS), swings the player's arm for combat feedback.
 * - If aiming at a block (BLOCK), leaves vanilla block mining completely untouched
 *   so grass, flowers, dirt, stone, etc. can be broken normally.
 * - Automatically respects ForgeMod.ENTITY_REACH since vanilla's mc.hitResult
 *   and mc.gameMode.attack() natively use player.getEntityReach().
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
        if (player == null || mc.level == null || mc.screen != null || mc.gameMode == null) {
            return;
        }

        var effectObj = ModMobEffects.EFFECTS.get("ignore_iframes_blessing");
        if (effectObj == null || !player.hasEffect(effectObj.get())) {
            return;
        }

        if (!mc.options.keyAttack.isDown()) {
            return;
        }

        HitResult hit = mc.hitResult;
        Entity target = null;
        if (hit instanceof EntityHitResult entityHit) {
            target = entityHit.getEntity();
        } else if (mc.crosshairPickEntity != null) {
            target = mc.crosshairPickEntity;
        }

        if (target != null && target.isAlive() && target != player) {
            // True vanilla auto-click: 100 attacks per tick
            for (int i = 0; i < 100; i++) {
                if (!target.isAlive()) {
                    break;
                }
                mc.gameMode.attack(player, target);
            }
            player.swing(InteractionHand.MAIN_HAND);
        } else if (hit == null || hit.getType() == HitResult.Type.MISS) {
            player.swing(InteractionHand.MAIN_HAND);
        }
        // When hit is a block (Type.BLOCK), do nothing: vanilla block mining runs normally without interruption!
    }
}
