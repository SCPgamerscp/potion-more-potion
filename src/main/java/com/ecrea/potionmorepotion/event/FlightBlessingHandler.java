package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import com.ecrea.potionmorepotion.network.FlightBoostPacket;
import com.ecrea.potionmorepotion.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side flight booster for the Flight Blessing.
 * With Caelus API integration, all vanilla Elytra flight mechanics (airborne deployment,
 * 1-block gap navigation, crawling transition, and smooth landing) are 100% natively
 * handled by Minecraft itself without lag or desync.
 *
 * This handler provides the rocket firework propulsion while holding Jump during flight.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID, value = Dist.CLIENT)
public class FlightBlessingHandler {

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

        var effectObj = ModMobEffects.EFFECTS.get("flight_blessing");
        if (effectObj == null || !player.hasEffect(effectObj.get())) {
            return;
        }

        // Rocket propulsion while actively gliding and holding Jump
        if (player.isFallFlying() && mc.options.keyJump.isDown() && mc.screen == null) {
            Vec3 look = player.getLookAngle();
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.add(
                    look.x * 0.1D + (look.x * 1.5D - motion.x) * 0.5D,
                    look.y * 0.1D + (look.y * 1.5D - motion.y) * 0.5D,
                    look.z * 0.1D + (look.z * 1.5D - motion.z) * 0.5D
            ));
            player.hasImpulse = true;

            // Synchronize motion to server
            ModMessages.sendToServer(new FlightBoostPacket());

            // Firework smoke particles
            player.level().addParticle(ParticleTypes.FIREWORK,
                    player.getX() - look.x * 0.5,
                    player.getY() + 0.3 - look.y * 0.5,
                    player.getZ() - look.z * 0.5,
                    -look.x * 0.1, -look.y * 0.1, -look.z * 0.1);

            // Periodic launch sound
            if (player.tickCount % 10 == 0) {
                player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS,
                        0.6F, 1.0F + (player.level().random.nextFloat() - player.level().random.nextFloat()) * 0.2F, false);
            }
        }
    }
}
