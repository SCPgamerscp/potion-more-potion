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
    public static void onLivingAttack(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();

        var snowballObj = com.ecrea.potionmorepotion.effect.ModMobEffects.EFFECTS.get("snowball_blessing");
        if (snowballObj != null && entity.hasEffect(snowballObj.get())) {
            if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FREEZE)) {
                event.setCanceled(true);
                return;
            }
        }

        var dragonObj = com.ecrea.potionmorepotion.effect.ModMobEffects.EFFECTS.get("ender_dragon_blessing");
        if (dragonObj != null && entity.hasEffect(dragonObj.get())) {
            if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.DRAGON_BREATH)) {
                event.setCanceled(true);
                return;
            }
        }

        var potionObj = com.ecrea.potionmorepotion.effect.ModMobEffects.EFFECTS.get("potion_blessing");
        if (potionObj != null && entity.hasEffect(potionObj.get())) {
            if (event.getSource().getEntity() == entity &&
                    (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC) ||
                     event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC))) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(net.minecraftforge.event.level.ExplosionEvent.Detonate event) {
        // Protect players with ender_dragon_blessing from explosion knockback caused by dragon fireballs
        event.getAffectedEntities().removeIf(e -> {
            if (e instanceof LivingEntity living) {
                var dragonObj = com.ecrea.potionmorepotion.effect.ModMobEffects.EFFECTS.get("ender_dragon_blessing");
                return dragonObj != null && living.hasEffect(dragonObj.get());
            }
            return false;
        });
    }

    @SubscribeEvent
    public static void onLivingKnockBack(net.minecraftforge.event.entity.living.LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        var potionObj = com.ecrea.potionmorepotion.effect.ModMobEffects.EFFECTS.get("potion_blessing");
        var dragonObj = com.ecrea.potionmorepotion.effect.ModMobEffects.EFFECTS.get("ender_dragon_blessing");

        // Cancel knockback if player has potion or dragon blessing and damage was self-inflicted
        if ((potionObj != null && entity.hasEffect(potionObj.get())) ||
            (dragonObj != null && entity.hasEffect(dragonObj.get()))) {
            if (entity.getLastDamageSource() != null && entity.getLastDamageSource().getEntity() == entity) {
                event.setCanceled(true);
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

        var dragonObj = com.ecrea.potionmorepotion.effect.ModMobEffects.EFFECTS.get("ender_dragon_blessing");
        if (dragonObj != null && entity.hasEffect(dragonObj.get())) {
            if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.DRAGON_BREATH)) {
                event.setCanceled(true);
                return;
            }
        }

        var potionObj = com.ecrea.potionmorepotion.effect.ModMobEffects.EFFECTS.get("potion_blessing");
        if (potionObj != null && entity.hasEffect(potionObj.get())) {
            if (event.getSource().getEntity() == entity &&
                    (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC) ||
                     event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC))) {
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
