package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.effect.BlessingDefinition;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import com.ecrea.potionmorepotion.potion.ModPotions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Registers "AWKWARD potion + material -> blessing potion" brewing recipes for every
 * blessing definition using Forge's BrewingRecipeRegistry API.
 *
 * This replaces the previous approach that used ObfuscationReflectionHelper to call the
 * private PotionBrewing.addMix method, which crashed in production because the method
 * name "addMix" is obfuscated to an SRG name in the reobfuscated jar.
 *
 * Splash / Lingering / Tipped Arrow conversions are vanilla's generic container recipes
 * (gunpowder / dragon's breath / arrow) and need no extra registration here.
 * JEI automatically picks these up via its built-in vanilla brewing category.
 */
public class ModEventBusEvents {

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            com.ecrea.potionmorepotion.network.ModMessages.register();

            for (BlessingDefinition definition : ModMobEffects.DEFINITIONS.values()) {
                BrewingRecipeRegistry.addRecipe(
                        StrictNBTIngredient.of(
                                PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD)),
                        Ingredient.of(definition.brewingIngredient().get()),
                        PotionUtils.setPotion(new ItemStack(Items.POTION),
                                ModPotions.POTIONS_MAP.get(definition.id()).get())
                );
            }

            // Recipe: Ignore I-Frames Blessing Potion + Fermented Spider Eye -> Ultimate Curse Potion
            var ignoreIframesPotion = ModPotions.POTIONS_MAP.get("ignore_iframes_blessing");
            if (ignoreIframesPotion != null) {
                BrewingRecipeRegistry.addRecipe(
                        StrictNBTIngredient.of(
                                PotionUtils.setPotion(new ItemStack(Items.POTION), ignoreIframesPotion.get())),
                        Ingredient.of(Items.FERMENTED_SPIDER_EYE),
                        PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotions.ULTIMATE_CURSE.get())
                );
            }

            // Dynamic Caelus API integration:
            // When Caelus API is present, bind the caelus:fall_flying attribute modifier to flight_blessing.
            // This turns the potion effect into a 100% native Elytra flight effect without any Mixins or hard dependencies!
            net.minecraft.resources.ResourceLocation caelusFlightId = net.minecraft.resources.ResourceLocation.tryParse("caelus:fall_flying");
            if (caelusFlightId != null && net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.containsKey(caelusFlightId)) {
                net.minecraft.world.entity.ai.attributes.Attribute flightAttribute = net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(caelusFlightId);
                if (flightAttribute != null) {
                    var flightObj = ModMobEffects.EFFECTS.get("flight_blessing");
                    if (flightObj != null) {
                        flightObj.get().addAttributeModifier(
                                flightAttribute,
                                "748D7064-6A60-4F59-8ABE-C2C23A6DD7A9",
                                1.0D,
                                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                        );
                        com.ecrea.potionmorepotion.PotionMorePotionMod.LOGGER.info("Successfully bound Caelus fall_flying attribute to flight_blessing for native elytra flight!");
                    }
                }
            }
        });
    }
}
