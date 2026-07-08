package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.Config;
import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.BlessingMobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;

/**
 * Non-Mixin fix for tipped arrow duration (Mixin caused a crash in-game, so this uses a
 * plain Forge event + reflection instead).
 *
 * Vanilla's Arrow#doPostHurtEffects divides a tipped arrow's potion-derived effect duration
 * by 8 (minimum 1 tick) compared to the base potion. Since our base potions are always baked
 * at Config.baseDurationTicks, a genuine arrow hit always produces an instance whose duration
 * is EXACTLY Math.max(Config.baseDurationTicks / 8, 1) at the moment MobEffectEvent.Added fires.
 *
 * We only rewrite the duration when it matches that EXACT computed value (not just "duration
 * is short"), so this cannot misfire for /effect give with no duration (600 ticks), infinite
 * (-1), or any other explicit duration that doesn't happen to equal that specific number.
 * This was the actual bug in the previous version of this handler.
 *
 * The duration field is mutated in place via reflection on the SAME MobEffectInstance object
 * that LivingEntity#addEffect is about to store, since MobEffectEvent.Added fires before that
 * storage happens; calling addEffect again from inside this handler would race with the
 * in-progress outer call and get overwritten, so direct field mutation is used instead.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID)
public class ArrowBlessingDurationHandler {

    private static Field durationField;

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || !(instance.getEffect() instanceof BlessingMobEffect)) {
            return;
        }

        int duration = instance.getDuration();

        // Infinite duration (-1) and zero/negative must never be touched.
        if (duration <= 0) {
            return;
        }

        // Vanilla arrows divide the source potion duration by 8 (minimum 1 tick).
        // Only rewrite the duration when it EXACTLY matches that computed value,
        // so /effect give with any other explicit duration is left alone.
        int expectedArrowDuration = Math.max(Config.baseDurationTicks / 8, 1);
        if (instance.getAmplifier() == 0 && duration == expectedArrowDuration) {
            setDuration(instance, Config.arrowDurationTicks);
        }
    }

    private static void setDuration(MobEffectInstance instance, int duration) {
        try {
            if (durationField == null) {
                try {
                    durationField = MobEffectInstance.class.getDeclaredField("f_19503_");
                } catch (NoSuchFieldException e) {
                    durationField = MobEffectInstance.class.getDeclaredField("duration");
                }
                durationField.setAccessible(true);
            }
            durationField.setInt(instance, duration);
        } catch (ReflectiveOperationException e) {
            PotionMorePotionMod.LOGGER.error("Failed to set blessing arrow duration", e);
        }
    }
}
