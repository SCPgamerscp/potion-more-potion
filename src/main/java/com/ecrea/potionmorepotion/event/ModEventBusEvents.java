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
            for (BlessingDefinition definition : ModMobEffects.DEFINITIONS.values()) {
                BrewingRecipeRegistry.addRecipe(
                        StrictNBTIngredient.of(
                                PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD)),
                        Ingredient.of(definition.brewingIngredient().get()),
                        PotionUtils.setPotion(new ItemStack(Items.POTION),
                                ModPotions.POTIONS_MAP.get(definition.id()).get())
                );
            }
        });
    }
}
