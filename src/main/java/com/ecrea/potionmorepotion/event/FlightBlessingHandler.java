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
 * 1. Allows player to initiate elytra gliding (Fall Flying) without wearing an elytra by pressing Jump in mid-air.
 * 2. Continuously accelerates the player in their look direction like vanilla firework rockets while Jump is held down during flight.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID, value = Dist.CLIENT)
public class FlightBlessingHandler {

    private static boolean wasJumpDown = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null) {
            wasJumpDown = false;
            return;
        }

        var effectObj = ModMobEffects.EFFECTS.get("flight_blessing");
        if (effectObj == null || !player.hasEffect(effectObj.get())) {
            wasJumpDown = mc.options.keyJump.isDown();
            return;
        }

        boolean isJumpDown = mc.options.keyJump.isDown();

        // 1. Initiate glide when pressing jump in mid-air (vanilla elytra behavior)
        if (!player.onGround() && !player.isFallFlying() && !player.isInWater() && !player.hasEffect(MobEffects.LEVITATION)) {
            if (isJumpDown && !wasJumpDown) {
                player.startFallFlying();
                ModMessages.sendToServer(new FlightGlidePacket());
            }
        }

        // 2. While gliding: keep fall-flying active and handle rocket boost acceleration
        if (player.isFallFlying()) {
            // Keep client fall flying state active even without chestplate elytra
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
