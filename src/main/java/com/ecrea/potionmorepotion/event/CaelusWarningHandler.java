package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Sends chat warnings when Caelus API is not installed:
 * 1. Upon world/server login (PlayerLoggedInEvent).
 * 2. When Flight Blessing effect is applied to a player (MobEffectEvent.Added).
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID)
public class CaelusWarningHandler {

    private static final String CAELUS_MOD_ID = "caelus";

    public static boolean isCaelusLoaded() {
        return ModList.get().isLoaded(CAELUS_MOD_ID);
    }

    /**
     * Warn player when logging into the world if Caelus API is not installed.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (isCaelusLoaded()) {
            return;
        }

        Player player = event.getEntity();
        if (player != null && !player.level().isClientSide) {
            Component message = Component.literal("[Potion More Potion] ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.translatable("message.potionmorepotion.caelus_missing_login")
                            .withStyle(ChatFormatting.RED));
            player.sendSystemMessage(message);
        }
    }

    /**
     * Warn player when Flight Blessing is added if Caelus API is not installed.
     */
    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        if (isCaelusLoaded()) {
            return;
        }

        var flightEffect = ModMobEffects.EFFECTS.get("flight_blessing");
        if (flightEffect == null || event.getEffectInstance().getEffect() != flightEffect.get()) {
            return;
        }

        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            Component message = Component.literal("[")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.translatable("effect.potionmorepotion.flight_blessing")
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("] ")
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.translatable("message.potionmorepotion.caelus_missing_potion")
                            .withStyle(ChatFormatting.RED));
            player.sendSystemMessage(message);
        }
    }
}
