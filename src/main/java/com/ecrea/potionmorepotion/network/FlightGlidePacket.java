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

    public static final String NBT_FLIGHT_GLIDING = "pmp_flight_gliding";

    private final boolean start;

    public FlightGlidePacket() {
        this(true);
    }

    public FlightGlidePacket(boolean start) {
        this.start = start;
    }

    public FlightGlidePacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.start);
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
                player.getPersistentData().remove(NBT_FLIGHT_GLIDING);
                return;
            }

            if (this.start) {
                player.getPersistentData().putBoolean(NBT_FLIGHT_GLIDING, true);
                player.startFallFlying();
            } else {
                player.getPersistentData().remove(NBT_FLIGHT_GLIDING);
                player.stopFallFlying();
            }
        });
        return true;
    }
}
