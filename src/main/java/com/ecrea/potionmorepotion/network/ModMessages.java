package com.ecrea.potionmorepotion.network;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {

    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(PotionMorePotionMod.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(FireBreathPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(FireBreathPacket::new)
                .encoder(FireBreathPacket::toBytes)
                .consumerMainThread(FireBreathPacket::handle)
                .add();

        net.messageBuilder(SnowballBreathPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SnowballBreathPacket::new)
                .encoder(SnowballBreathPacket::toBytes)
                .consumerMainThread(SnowballBreathPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        if (INSTANCE != null) {
            INSTANCE.sendToServer(message);
        }
    }
}
