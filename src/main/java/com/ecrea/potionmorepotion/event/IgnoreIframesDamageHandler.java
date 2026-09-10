package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles the Ignore I-Frames (invulnerability frame negation) mechanic.
 * Whenever an attacker with the Ignore I-Frames Blessing attacks any target,
 * the target's invulnerableTime and lastHurt are reset to 0 before and after
 * the damage calculation, ensuring every single attack hits with full damage.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID)
public class IgnoreIframesDamageHandler {

    private static boolean hasIgnoreIframes(Entity entity) {
        if (entity instanceof LivingEntity living) {
            var effect = ModMobEffects.EFFECTS.get("ignore_iframes_blessing");
            return effect != null && living.hasEffect(effect.get());
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (hasIgnoreIframes(event.getEntity())) {
            Entity target = event.getTarget();
            if (target instanceof LivingEntity livingTarget) {
                livingTarget.invulnerableTime = 0;
                livingTarget.hurtTime = 0;
            } else if (target instanceof net.minecraftforge.entity.PartEntity<?> part &&
                    part.getParent() instanceof LivingEntity parentLiving) {
                parentLiving.invulnerableTime = 0;
                parentLiving.hurtTime = 0;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (hasIgnoreIframes(attacker)) {
            LivingEntity target = event.getEntity();
            target.invulnerableTime = 0;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (hasIgnoreIframes(attacker)) {
            LivingEntity target = event.getEntity();
            target.invulnerableTime = 0;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (hasIgnoreIframes(attacker)) {
            LivingEntity target = event.getEntity();
            // Reset after hurt logic has executed so the next hit is not blocked
            target.invulnerableTime = 0;
        }
    }
}
