package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.Config;
import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.BlessingMobEffect;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles damage immunity and reduction for blessing effects:
 * - Snowball Blessing: Immune to freezing.
 * - Ender Dragon Blessing: Completely immune (like vanilla Fire Resistance) to self-inflicted
 *   dragon fireballs, explosions, dragon breath, and lingering purple clouds (harm magic).
 * - Potion Blessing: Completely immune (like vanilla Fire Resistance) to self-inflicted
 *   splash potions of harm (indirect magic).
 * - 50% damage reduction for all active blessings against other external damage sources.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID)
public class BlessingDamageHandler {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }

        var snowballObj = ModMobEffects.EFFECTS.get("snowball_blessing");
        if (snowballObj != null && entity.hasEffect(snowballObj.get())) {
            if (entity.getTicksFrozen() > 0) {
                entity.setTicksFrozen(0);
            }
        }
    }

    /**
     * Cancels damage BEFORE hurt logic (hurtTime, hurt animation, red flash, sound, knockback) runs,
     * achieving the exact same effect as vanilla Fire Resistance.
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();

        // 1. Snowball Blessing: immune to freeze damage
        var snowballObj = ModMobEffects.EFFECTS.get("snowball_blessing");
        if (snowballObj != null && entity.hasEffect(snowballObj.get())) {
            if (source.is(DamageTypes.FREEZE)) {
                event.setCanceled(true);
                return;
            }
        }

        // 2. Ender Dragon Blessing: completely immune like Fire Resistance to:
        //    - Dragon breath
        //    - Self-fired dragon fireballs (direct hits or explosions)
        //    - Self-fired area effect clouds (dragon breath purple clouds / harm)
        var dragonObj = ModMobEffects.EFFECTS.get("ender_dragon_blessing");
        if (dragonObj != null && entity.hasEffect(dragonObj.get())) {
            // Dragon breath damage type
            if (source.is(DamageTypes.DRAGON_BREATH)) {
                event.setCanceled(true);
                return;
            }

            // Dragon Fireball direct hit or explosion from self
            if (source.getDirectEntity() instanceof DragonFireball dfb) {
                if (dfb.getOwner() == entity || dfb.getOwner() == null) {
                    event.setCanceled(true);
                    return;
                }
            }

            // AreaEffectCloud created by dragon fireballs
            if (source.getDirectEntity() instanceof AreaEffectCloud cloud) {
                if (cloud.getOwner() == entity || cloud.getParticle().getType() == ParticleTypes.DRAGON_BREATH) {
                    event.setCanceled(true);
                    return;
                }
            }

            // Self-inflicted explosions or magic (from dragon fireball impact & clouds)
            if (source.getEntity() == entity) {
                if (source.is(DamageTypes.EXPLOSION) ||
                    source.is(DamageTypes.PLAYER_EXPLOSION) ||
                    source.is(DamageTypes.INDIRECT_MAGIC) ||
                    source.is(DamageTypes.MAGIC)) {
                    event.setCanceled(true);
                    return;
                }
            }
        }

        // 3. Potion Blessing: completely immune like Fire Resistance to:
        //    - Self-thrown splash potions (harming II)
        //    - Indirect magic / magic where attacker is self
        var potionObj = ModMobEffects.EFFECTS.get("potion_blessing");
        if (potionObj != null && entity.hasEffect(potionObj.get())) {
            // Direct projectile was a ThrownPotion thrown by player
            if (source.getDirectEntity() instanceof ThrownPotion potion) {
                if (potion.getOwner() == entity || potion.getOwner() == null) {
                    event.setCanceled(true);
                    return;
                }
            }

            // Self-inflicted magic / indirect magic
            if (source.getEntity() == entity &&
                    (source.is(DamageTypes.INDIRECT_MAGIC) ||
                     source.is(DamageTypes.MAGIC))) {
                event.setCanceled(true);
                return;
            }
        }

        // 4. Fang Blessing: completely immune to Evoker Fangs damage
        var fangObj = ModMobEffects.EFFECTS.get("fang_blessing");
        if (fangObj != null && entity.hasEffect(fangObj.get())) {
            if (source.getDirectEntity() instanceof EvokerFangs) {
                event.setCanceled(true);
                return;
            }
        }

        // 5. Lightning Blessing: completely immune to Lightning Bolt damage
        var lightningObj = ModMobEffects.EFFECTS.get("lightning_blessing");
        if (lightningObj != null && entity.hasEffect(lightningObj.get())) {
            if (source.is(DamageTypes.LIGHTNING_BOLT)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /**
     * Intercepts lightning strikes caused by players with Lightning Blessing.
     * Cancels vanilla environmental damage and applies player-attributed lightning damage
     * so that kills count towards the player (XP drops, loot, advancements).
     */
    @SubscribeEvent
    public static void onEntityStruckByLightning(EntityStruckByLightningEvent event) {
        LightningBolt bolt = event.getLightning();
        ServerPlayer cause = bolt.getCause();
        if (cause != null) {
            var lightningObj = ModMobEffects.EFFECTS.get("lightning_blessing");
            if (lightningObj != null && cause.hasEffect(lightningObj.get())) {
                Entity entity = event.getEntity();

                // If struck entity is the caster or has active lightning_blessing, cancel completely
                if (entity == cause || (entity instanceof LivingEntity living && living.hasEffect(lightningObj.get()))) {
                    event.setCanceled(true);
                    return;
                }

                // Cancel vanilla environmental thunderHit
                event.setCanceled(true);

                // Inflict player-attributed lightning damage (counts as player kill, XP, loot, advancements)
                var damageTypeHolder = entity.level().registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(DamageTypes.LIGHTNING_BOLT);
                DamageSource source = new DamageSource(damageTypeHolder, bolt, cause);
                entity.hurt(source, bolt.getDamage());

                if (!bolt.getTags().contains("no_fire")) {
                    entity.setRemainingFireTicks(entity.getRemainingFireTicks() + 1);
                    if (entity.getRemainingFireTicks() == 0) {
                        entity.setSecondsOnFire(8);
                    }
                } else {
                    entity.clearFire();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        // Protect players with ender_dragon_blessing from explosion knockback/damage caused by dragon fireballs
        event.getAffectedEntities().removeIf(e -> {
            if (e instanceof LivingEntity living) {
                var dragonObj = ModMobEffects.EFFECTS.get("ender_dragon_blessing");
                return dragonObj != null && living.hasEffect(dragonObj.get());
            }
            return false;
        });
    }

    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        var potionObj = ModMobEffects.EFFECTS.get("potion_blessing");
        var dragonObj = ModMobEffects.EFFECTS.get("ender_dragon_blessing");

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
        DamageSource source = event.getSource();

        // Safety fallback: ensure complete cancellation for any hurt event that might bypass attack event
        var snowballObj = ModMobEffects.EFFECTS.get("snowball_blessing");
        if (snowballObj != null && entity.hasEffect(snowballObj.get())) {
            if (source.is(DamageTypes.FREEZE)) {
                event.setCanceled(true);
                return;
            }
        }

        var dragonObj = ModMobEffects.EFFECTS.get("ender_dragon_blessing");
        if (dragonObj != null && entity.hasEffect(dragonObj.get())) {
            if (source.is(DamageTypes.DRAGON_BREATH)) {
                event.setCanceled(true);
                return;
            }
            if (source.getDirectEntity() instanceof DragonFireball dfb && (dfb.getOwner() == entity || dfb.getOwner() == null)) {
                event.setCanceled(true);
                return;
            }
            if (source.getDirectEntity() instanceof AreaEffectCloud cloud && (cloud.getOwner() == entity || cloud.getParticle().getType() == ParticleTypes.DRAGON_BREATH)) {
                event.setCanceled(true);
                return;
            }
            if (source.getEntity() == entity &&
                    (source.is(DamageTypes.EXPLOSION) ||
                     source.is(DamageTypes.PLAYER_EXPLOSION) ||
                     source.is(DamageTypes.INDIRECT_MAGIC) ||
                     source.is(DamageTypes.MAGIC))) {
                event.setCanceled(true);
                return;
            }
        }

        var potionObj = ModMobEffects.EFFECTS.get("potion_blessing");
        if (potionObj != null && entity.hasEffect(potionObj.get())) {
            if (source.getDirectEntity() instanceof ThrownPotion potion && (potion.getOwner() == entity || potion.getOwner() == null)) {
                event.setCanceled(true);
                return;
            }
            if (source.getEntity() == entity &&
                    (source.is(DamageTypes.INDIRECT_MAGIC) ||
                     source.is(DamageTypes.MAGIC))) {
                event.setCanceled(true);
                return;
            }
        }

        var fangObj = ModMobEffects.EFFECTS.get("fang_blessing");
        if (fangObj != null && entity.hasEffect(fangObj.get())) {
            if (source.getDirectEntity() instanceof EvokerFangs) {
                event.setCanceled(true);
                return;
            }
        }

        var lightningObj = ModMobEffects.EFFECTS.get("lightning_blessing");
        if (lightningObj != null && entity.hasEffect(lightningObj.get())) {
            if (source.is(DamageTypes.LIGHTNING_BOLT)) {
                event.setCanceled(true);
                return;
            }
        }

        // Apply 50% damage reduction for each active blessing
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
