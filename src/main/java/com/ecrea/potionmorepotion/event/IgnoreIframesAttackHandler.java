package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import com.ecrea.potionmorepotion.network.ModMessages;
import com.ecrea.potionmorepotion.network.RapidAttackPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

/**
 * Client-side handler for the Ignore I-Frames Blessing.
 * Detects left-click (keyAttack or GLFW mouse button) hold every tick:
 * - Continuously swings the player's arm (rapid swing animation).
 * - Suppresses block mining mode so holding attack never gets stuck mining.
 * - Performs custom entity raycast (up to 5.0 blocks) targeting both regular entities
 *   and multipart boss parts (like Ender Dragon parts).
 * - Sends RapidAttackPacket every tick when a target is in range to execute 100 attacks.
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

        // Check if attack key is held via key mapping OR raw GLFW mouse button
        boolean isAttackPressed = mc.options.keyAttack.isDown();
        if (!isAttackPressed) {
            long window = mc.getWindow().getWindow();
            isAttackPressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        }

        if (!isAttackPressed) {
            return;
        }

        // Find target entity in front of player (up to 5.0 blocks)
        Entity target = findTargetEntity(mc, player, 5.0D);

        if (target != null) {
            // When aiming at an enemy, prioritize attack: suppress block mining so it doesn't interrupt combat
            if (mc.gameMode != null) {
                mc.gameMode.stopDestroyBlock();
            }
            player.swing(InteractionHand.MAIN_HAND);
            ModMessages.sendToServer(new RapidAttackPacket(target.getId()));
        } else {
            // When no enemy is targeted:
            // - If aiming at empty air (miss), swing arm for combat readiness.
            // - If aiming at a block, do NOT suppress or swing, allowing vanilla block mining to work normally!
            if (mc.hitResult == null || mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                player.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    private static Entity findTargetEntity(Minecraft mc, LocalPlayer player, double reachDistance) {
        // 1. Check vanilla crosshair picks first
        if (mc.crosshairPickEntity != null && mc.crosshairPickEntity.isAlive()
                && mc.crosshairPickEntity.isAttackable() && mc.crosshairPickEntity != player) {
            return mc.crosshairPickEntity;
        }
        if (mc.hitResult instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (entity.isAlive() && entity.isAttackable() && entity != player) {
                return entity;
            }
        }

        // 2. Custom Raycast along eye vector
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 viewVec = player.getViewVector(1.0F);
        Vec3 reachVec = eyePos.add(viewVec.scale(reachDistance));
        AABB searchBox = player.getBoundingBox().expandTowards(viewVec.scale(reachDistance)).inflate(2.0D);

        double closestDist = reachDistance * reachDistance;
        Entity closestEntity = null;

        // Search all regular entities in range
        for (Entity e : player.level().getEntities(player, searchBox,
                entity -> !entity.isSpectator() && entity.isAttackable() && entity.isAlive() && entity != player)) {
            AABB aabb = e.getBoundingBox().inflate(0.3D);
            Optional<Vec3> hit = aabb.clip(eyePos, reachVec);
            if (hit.isPresent()) {
                double dist = eyePos.distanceToSqr(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestEntity = e;
                }
            }
        }

        // 3. Also explicitly check Ender Dragon multipart parts in range
        if (closestEntity == null) {
            for (EnderDragon dragon : player.level().getEntitiesOfClass(EnderDragon.class, searchBox)) {
                for (EnderDragonPart part : dragon.getSubEntities()) {
                    AABB aabb = part.getBoundingBox().inflate(0.3D);
                    Optional<Vec3> hit = aabb.clip(eyePos, reachVec);
                    if (hit.isPresent()) {
                        double dist = eyePos.distanceToSqr(hit.get());
                        if (dist < closestDist) {
                            closestDist = dist;
                            closestEntity = part;
                        }
                    }
                }
            }
        }

        return closestEntity;
    }
}
