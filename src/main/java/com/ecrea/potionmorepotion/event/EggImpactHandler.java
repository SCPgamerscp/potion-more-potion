package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles projectile impacts for Egg Blessing eggs.
 * When a blessing egg hits:
 * - If target is a Chicken: Completely cancels impact (no damage, no knockback, passes through).
 * - If target is the shooter (owner): Completely cancels impact (prevents self-damage and knockback).
 * - If target is another LivingEntity: Deals 10.0 damage (5 hearts) with player attribution,
 *   and allows vanilla egg shatter / chick hatching logic to proceed normally.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID)
public class EggImpactHandler {

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof ThrownEgg egg) {
            if (egg.getTags().contains("blessing_egg")) {
                if (event.getRayTraceResult() instanceof EntityHitResult entityHitResult) {
                    Entity hitEntity = entityHitResult.getEntity();
                    Entity owner = egg.getOwner();

                    // 1. Completely harmless to chickens (no damage, no knockback, passes through)
                    if (hitEntity instanceof Chicken) {
                        event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
                        return;
                    }

                    // 2. Self-hit protection: completely ignore shooter
                    if (hitEntity == owner) {
                        event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
                        return;
                    }

                    // 3. Deal 10.0 damage (5 hearts) to any other LivingEntity
                    if (hitEntity instanceof LivingEntity target) {
                        if (owner instanceof ServerPlayer player) {
                            target.setLastHurtByPlayer(player);
                        }
                        DamageSource damageSource = egg.damageSources().thrown(egg, owner);
                        target.hurt(damageSource, 10.0F);
                    }
                }
            }
        }
    }
}
