package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles projectile impacts for Snowball Blessing snowballs.
 * When a blessing snowball hits an entity, it deals 6 damage, freezes it for 20 seconds (400 ticks),
 * and applies Slowness V for 20 seconds.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID)
public class SnowballImpactHandler {

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof Snowball snowball) {
            if (snowball.getTags().contains("blessing_snowball")) {
                if (event.getRayTraceResult() instanceof EntityHitResult entityHitResult) {
                    Entity hitEntity = entityHitResult.getEntity();
                    if (hitEntity instanceof LivingEntity target) {
                        Entity owner = snowball.getOwner();
                        DamageSource damageSource = snowball.damageSources().thrown(snowball, owner);

                        // 6.0 damage (3 hearts)
                        target.hurt(damageSource, 6.0F);

                        // 20 seconds (400 ticks) powder snow freeze
                        target.setTicksFrozen(400);

                        // 20 seconds (400 ticks) Slowness V (amplifier 4)
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 4));
                    }
                }
            }
        }
    }
}
