package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.Config;
import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.BlessingMobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Applies "50% damage reduction" per active blessing effect to ALL entities (not just players),
 * for every damage source (melee, fall, fire, drown, poison, etc). This is independent from and
 * stacks multiplicatively with vanilla Resistance, since it is not implemented as an attribute
 * modifier or as vanilla Resistance amplification.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID)
public class BlessingDamageHandler {

    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }

        var snowballObj = com.ecrea.potionmorepotion.effect.ModMobEffects.EFFECTS.get("snowball_blessing");
        if (snowballObj != null && entity.hasEffect(snowballObj.get())) {
            if (entity.getTicksFrozen() > 0) {
                entity.setTicksFrozen(0);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();

        var snowballObj = com.ecrea.potionmorepotion.effect.ModMobEffects.EFFECTS.get("snowball_blessing");
        if (snowballObj != null && entity.hasEffect(snowballObj.get())) {
            if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FREEZE)) {
                event.setCanceled(true);
                return;
            }
        }

        int blessingCount = 0;
        for (MobEffectInstance instance : entity.getActiveEffects()) {
            if (instance.getEffect() instanceof BlessingMobEffect) {
                blessingCount++;
            }
        }

        if (blessingCount == 0) {
            return;
        }

        float remainingFraction = 1.0F;
        for (int i = 0; i < blessingCount; i++) {
            remainingFraction *= (1.0F - Config.damageReduction);
        }

        event.setAmount(event.getAmount() * remainingFraction);
    }
}
