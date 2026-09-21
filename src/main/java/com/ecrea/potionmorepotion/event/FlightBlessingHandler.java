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
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side handler for the Flight Blessing (Blessing of Flight).
 * 1. Double-tap Jump (Space) to activate elytra glide and liftoff into the air (ground or mid-air).
 * 2. Hold Jump while gliding to continuously accelerate with rocket firework propulsion in look direction.
 * 3. Continues flying even through water.
 * 4. Only cancels flight upon landing on solid ground.
 * 5. Single jump remains 100% normal vanilla jumping.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID, value = Dist.CLIENT)
public class FlightBlessingHandler {

    private static boolean wasJumpDown = false;
    private static int doubleTapTimer = 0;
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
            doubleTapTimer = 0;
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
            doubleTapTimer = 0;
            glideTicks = 0;
            return;
        }

        boolean isJumpDown = mc.options.keyJump.isDown() && mc.screen == null;
        boolean justPressed = isJumpDown && !wasJumpDown;

        // Decrement double-tap timer every tick
        if (doubleTapTimer > 0) {
            doubleTapTimer--;
        }

        // 1. Double-tap detection for initiating flight
        if (!isGliding && !player.isFallFlying()) {
            if (justPressed) {
                if (doubleTapTimer > 0) {
                    // Double-tap triggered!
                    doubleTapTimer = 0;
                    isGliding = true;
                    glideTicks = 0;
                    player.startFallFlying();
                    ModMessages.sendToServer(new FlightGlidePacket(true));

                    // Liftoff boost: launch upward smoothly into the air
                    Vec3 motion = player.getDeltaMovement();
                    Vec3 look = player.getLookAngle();
                    player.setDeltaMovement(new Vec3(
                            motion.x * 0.5D + look.x * 0.2D,
                            Math.max(motion.y, 0.55D),
                            motion.z * 0.5D + look.z * 0.2D
                    ));
                    player.hasImpulse = true;
                } else {
                    // First tap recorded, start 7-tick window (approx 0.35s)
                    doubleTapTimer = 7;
                }
            }
        }

        // 2. Flight & Glide active logic
        if (isGliding || player.isFallFlying()) {
            isGliding = true;
            glideTicks++;
            player.startFallFlying();

            // Landing check: Only cancel when on solid ground AND has clearance to stand up (keeps glide active in 1-block gaps)
            if (glideTicks > 5 && player.onGround() && !player.isInWater() && canStandUp(player)) {
                isGliding = false;
                glideTicks = 0;
                player.stopFallFlying();
                ModMessages.sendToServer(new FlightGlidePacket(false));
            } else {
                // Rocket propulsion while holding Jump
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
        }

        wasJumpDown = isJumpDown;
    }

    /**
     * Checks whether there is enough vertical clearance above the player to stand up (1.8m height).
     * Only checks [y + 0.6m, y + 1.8m] to avoid false collisions with the floor/ground.
     */
    public static boolean canStandUp(LocalPlayer player) {
        AABB overheadBox = new AABB(
                player.getX() - 0.29D, player.getY() + 0.6D, player.getZ() - 0.29D,
                player.getX() + 0.29D, player.getY() + 1.8D, player.getZ() + 0.29D
        );
        return player.level().noCollision(player, overheadBox);
    }
}
