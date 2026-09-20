package com.ecrea.potionmorepotion.network;

import com.ecrea.potionmorepotion.effect.ModMobEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server when the player initiates elytra glide in mid-air
 * with the Flight Blessing active without wearing an elytra.
 */
public class FlightGlidePacket {

    public FlightGlidePacket() {
    }

    public FlightGlidePacket(FriendlyByteBuf buf) {
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

            if (!player.onGround() && !player.isInWater() && !player.hasEffect(MobEffects.LEVITATION)) {
                player.startFallFlying();
            }
        });
        return true;
    }
}
