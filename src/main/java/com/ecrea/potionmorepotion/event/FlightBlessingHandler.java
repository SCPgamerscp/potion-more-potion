package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import com.ecrea.potionmorepotion.network.FlightBoostPacket;
import com.ecrea.potionmorepotion.network.FlightGlidePacket;
import com.ecrea.potionmorepotion.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side handler for the Flight Blessing (Blessing of Flight).
 * 1. Preserves normal vanilla jumping when tap-jumping on the ground.
 * 2. Seamlessly transitions into elytra glide (Fall Flying) and rocket firework propulsion
 *    when Jump (Space) is held down during jump peak, or pressed in mid-air.
 * 3. Safely terminates glide upon touching the ground or water.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID, value = Dist.CLIENT)
public class FlightBlessingHandler {

    private static boolean wasJumpDown = false;
    private static int airborneTicks = 0;
    private static int jumpHoldTicks = 0;
    private static boolean isGliding = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            wasJumpDown = false;
            airborneTicks = 0;
            jumpHoldTicks = 0;
            isGliding = false;
            return;
        }

        var effectObj = ModMobEffects.EFFECTS.get("flight_blessing");
        if (effectObj == null || !player.hasEffect(effectObj.get())) {
            if (isGliding) {
                isGliding = false;
                ModMessages.sendToServer(new FlightGlidePacket(false));
            }
            wasJumpDown = mc.options.keyJump.isDown();
            airborneTicks = 0;
            jumpHoldTicks = 0;
            return;
        }

        boolean isJumpDown = mc.options.keyJump.isDown() && mc.screen == null;
        if (isJumpDown) {
            jumpHoldTicks++;
        } else {
            jumpHoldTicks = 0;
        }

        // 1. On ground, in water, or under levitation: reset airborne and stop glide
        if (player.onGround() || player.isInWater() || player.hasEffect(MobEffects.LEVITATION)) {
            airborneTicks = 0;
            if (isGliding) {
                isGliding = false;
                player.stopFallFlying();
                ModMessages.sendToServer(new FlightGlidePacket(false));
            }
            wasJumpDown = isJumpDown;
            return;
        }

        // 2. In mid-air
        airborneTicks++;

        // Trigger glide if not already gliding
        if (!isGliding && !player.isFallFlying()) {
            boolean shouldStartGlide = false;

            // Pattern A: Held Jump from ground through the peak of the jump (airborneTicks >= 3 and vertical speed slowing down)
            if (isJumpDown && jumpHoldTicks >= 3 && (player.getDeltaMovement().y <= 0.1D || airborneTicks >= 6)) {
                shouldStartGlide = true;
            }

            // Pattern B: Pressed Jump newly while in mid-air (vanilla-style double-jump or air-jump)
            if (isJumpDown && !wasJumpDown) {
                shouldStartGlide = true;
            }

            // Pattern C: Holding Jump while falling significantly (e.g. walked off a cliff)
            if (isJumpDown && (player.fallDistance > 1.0F || player.getDeltaMovement().y < -0.3D)) {
                shouldStartGlide = true;
            }

            if (shouldStartGlide) {
                isGliding = true;
                player.startFallFlying();
                ModMessages.sendToServer(new FlightGlidePacket(true));

                // Give initial forward/upward boost so player doesn't plunge straight down
                Vec3 motion = player.getDeltaMovement();
                if (motion.y < 0) {
                    Vec3 look = player.getLookAngle();
                    player.setDeltaMovement(motion.x + look.x * 0.2D, Math.max(motion.y, 0.1D), motion.z + look.z * 0.2D);
                }
            }
        }

        // 3. While gliding: keep fall-flying active and handle rocket boost acceleration
        if (isGliding || player.isFallFlying()) {
            isGliding = true;
            player.startFallFlying();

            // When holding jump: apply vanilla rocket firework acceleration
            if (isJumpDown) {
                Vec3 look = player.getLookAngle();
                Vec3 motion = player.getDeltaMovement();
                player.setDeltaMovement(motion.add(
                        look.x * 0.1D + (look.x * 1.5D - motion.x) * 0.5D,
                        look.y * 0.1D + (look.y * 1.5D - motion.y) * 0.5D,
                        look.z * 0.1D + (look.z * 1.5D - motion.z) * 0.5D
                ));
                player.hasImpulse = true;

                // Send boost packet to server to synchronize motion
                ModMessages.sendToServer(new FlightBoostPacket());

                // Client visual firework smoke trail
                player.level().addParticle(ParticleTypes.FIREWORK,
                        player.getX() - look.x * 0.5,
                        player.getY() + 0.3 - look.y * 0.5,
                        player.getZ() - look.z * 0.5,
                        -look.x * 0.1, -look.y * 0.1, -look.z * 0.1);

                if (player.tickCount % 10 == 0) {
                    player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                            SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS,
                            0.6F, 1.0F + (player.level().random.nextFloat() - player.level().random.nextFloat()) * 0.2F, false);
                }
            }
        }

        wasJumpDown = isJumpDown;
    }
}
