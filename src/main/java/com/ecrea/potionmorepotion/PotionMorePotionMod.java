package com.ecrea.potionmorepotion;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import com.ecrea.potionmorepotion.effect.ModMobEffects;
import com.ecrea.potionmorepotion.event.ModEventBusEvents;
import com.ecrea.potionmorepotion.potion.ModPotions;

@Mod(PotionMorePotionMod.MOD_ID)
public class PotionMorePotionMod {

    public static final String MOD_ID = "potionmorepotion";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PotionMorePotionMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModMobEffects.register(modEventBus);
        ModPotions.register(modEventBus);

        modEventBus.addListener(ModEventBusEvents::onCommonSetup);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
