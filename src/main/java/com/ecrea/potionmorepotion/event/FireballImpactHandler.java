package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles projectile impacts for Fire Charge Blessing fireballs.
 * If a SmallFireball has the "no_block_fire" tag and hits a block,
 * cancels the impact event to prevent BaseFireBlock placement and discards the fireball.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID)
public class FireballImpactHandler {

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof SmallFireball fireball) {
            if (fireball.getTags().contains("no_block_fire")) {
                // If it hit a block, cancel to prevent placing fire on the ground, then discard the fireball
                if (event.getRayTraceResult().getType() == HitResult.Type.BLOCK) {
                    event.setCanceled(true);
                    fireball.discard();
                }
                // If it hit an entity (mob, player), let it pass through to deal normal fire & damage
            }
        }
    }
}
