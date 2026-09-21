package com.ecrea.potionmorepotion.potion;

import com.ecrea.potionmorepotion.Config;
import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.BlessingDefinition;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registers the base (normal bottle) Potion for every blessing definition.
 * Splash / Lingering / Tipped Arrow variants are handled automatically by vanilla's
 * generic container-conversion recipes (gunpowder / dragon's breath / arrow), so no
 * extra registration is needed here for those three variants.
 */
public class ModPotions {

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, PotionMorePotionMod.MOD_ID);

    public static final Map<String, RegistryObject<Potion>> POTIONS_MAP = new LinkedHashMap<>();

    public static final RegistryObject<Potion> ULTIMATE_CURSE = POTIONS.register("ultimate_curse", () -> new Potion(
            "ultimate_curse",
            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12000, 6),
            new MobEffectInstance(MobEffects.POISON, 12000, 6),
            new MobEffectInstance(MobEffects.WEAKNESS, 12000, 6),
            new MobEffectInstance(ModMobEffects.IGNORE_IFRAMES_CURSE.get(), 12000, 0)
    ));

    static {
        for (BlessingDefinition definition : ModMobEffects.DEFINITIONS.values()) {
            registerPotion(definition);
        }
    }

    private static void registerPotion(BlessingDefinition definition) {
        RegistryObject<Potion> potion = POTIONS.register(definition.id(), () -> new Potion(
                definition.id(),
                new MobEffectInstance(ModMobEffects.EFFECTS.get(definition.id()).get(), Config.baseDurationTicks, 0)
        ));
        POTIONS_MAP.put(definition.id(), potion);
    }

    public static void register(IEventBus eventBus) {
        POTIONS.register(eventBus);
    }
}
