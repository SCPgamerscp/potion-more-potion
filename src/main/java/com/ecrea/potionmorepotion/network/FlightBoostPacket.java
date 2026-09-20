package com.ecrea.potionmorepotion.network;

import com.ecrea.potionmorepotion.effect.ModMobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server every tick while the player holds the Jump key (Space)
 * during elytra gliding with the Flight Blessing active.
 * Applies vanilla-accurate rocket propulsion acceleration in the look direction.
 */
public class FlightBoostPacket {

    public FlightBoostPacket() {
    }

    public FlightBoostPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            var effectObj = ModMobEffects.EFFECTS.get("flight_blessing");
            if (effectObj == null || !player.hasEffect(effectObj.get())) {
                return;
            }

            if (!player.isFallFlying()) {
                return;
            }

            // Apply vanilla firework rocket propulsion physics
            Vec3 look = player.getLookAngle();
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.add(
                    look.x * 0.1D + (look.x * 1.5D - motion.x) * 0.5D,
                    look.y * 0.1D + (look.y * 1.5D - motion.y) * 0.5D,
                    look.z * 0.1D + (look.z * 1.5D - motion.z) * 0.5D
            ));
            player.hasImpulse = true;

            ServerLevel level = player.serverLevel();
            level.sendParticles(ParticleTypes.FIREWORK,
                    player.getX() - look.x * 0.5,
                    player.getY() + 0.3 - look.y * 0.5,
                    player.getZ() - look.z * 0.5,
                    2, -look.x * 0.1, -look.y * 0.1, -look.z * 0.1, 0.05);

            // Play firework whoosh sound periodically
            if (player.tickCount % 10 == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS,
                        0.6F, 1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F);
            }
        });
        return true;
    }
}
