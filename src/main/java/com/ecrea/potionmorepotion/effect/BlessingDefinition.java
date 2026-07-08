package com.ecrea.potionmorepotion.effect;

import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * Data definition for a single "blessing" potion.
 * Adding a new blessing potion only requires one new entry in ModMobEffects.DEFINITIONS,
 * everything else (effect, potion, brewing recipe, lang keys) is generated from this record.
 */
public record BlessingDefinition(
        String id,
        int color,
        Supplier<Item> iconItem,
        Supplier<Item> brewingIngredient
) {
}
