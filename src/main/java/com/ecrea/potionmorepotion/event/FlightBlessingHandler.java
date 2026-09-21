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
 * 1. Jump while airborne to activate vanilla elytra glide (just like equipping an Elytra).
 * 2. Hold Jump while gliding to accelerate with rocket firework propulsion in look direction.
 * 3. Lands naturally upon touching ground, transitioning smoothly to crawling (Pose.SWIMMING) in 1-block gaps.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID, value = Dist.CLIENT)
public class FlightBlessingHandler {

    private static boolean wasJumpDown = false;
    private static int glideTicks = 0;
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
            glideTicks = 0;
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
            glideTicks = 0;
            return;
        }

        boolean isJumpDown = mc.options.keyJump.isDown() && mc.screen == null;
        boolean justPressed = isJumpDown && !wasJumpDown;

        // 1. Ground state handling: strictly reset all flight state when on solid ground.
        // Returning here guarantees that normal ground jumping (vanilla jumpFromGround) is never interfered with.
        if (player.onGround()) {
            if (isGliding || player.isFallFlying()) {
                isGliding = false;
                glideTicks = 0;
                player.stopFallFlying();
                ModMessages.sendToServer(new FlightGlidePacket(false));
            }
            wasJumpDown = isJumpDown;
            return;
        }

        // 2. Vanilla-style Elytra deployment:
        // Vanilla requires: airborne (!onGround), falling (motion.y < 0.0), not already flying, not in water, not levitating
        if (!isGliding && !player.isFallFlying()) {
            if (justPressed && player.getDeltaMovement().y < 0.0D && !player.isInWater() && !player.hasEffect(MobEffects.LEVITATION)) {
                isGliding = true;
                glideTicks = 0;
                player.startFallFlying();
                ModMessages.sendToServer(new FlightGlidePacket(true));
            }
        }

        // 3. Active Flight & Glide logic
        if (isGliding || player.isFallFlying()) {
            isGliding = true;
            glideTicks++;
            player.startFallFlying();

            // Rocket propulsion while holding Jump in mid-air
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

                // Visual firework smoke trail
                player.level().addParticle(ParticleTypes.FIREWORK,
                        player.getX() - look.x * 0.5,
                        player.getY() + 0.3 - look.y * 0.5,
                        player.getZ() - look.z * 0.5,
                        -look.x * 0.1, -look.y * 0.1, -look.z * 0.1);

                // Sound effect periodically
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
