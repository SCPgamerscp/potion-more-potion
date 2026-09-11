package com.ecrea.potionmorepotion.effect;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry of every blessing effect.
 *
 * To add a new blessing potion in the future, add ONE line to the static block below.
 * Everything else (MobEffect registration, Potion registration, brewing recipe,
 * damage reduction handling) is driven automatically from this table.
 */
public class ModMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, PotionMorePotionMod.MOD_ID);

    public static final Map<String, BlessingDefinition> DEFINITIONS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<MobEffect>> EFFECTS = new LinkedHashMap<>();

    static {
        // id, liquid/particle color, icon material, brewing ingredient
        register("oak_blessing", 0x9C7A4B, () -> Items.OAK_LOG, () -> Items.OAK_LOG);
        register("birch_blessing", 0xE8E4D8, () -> Items.BIRCH_LOG, () -> Items.BIRCH_LOG);
        register("stone_blessing", 0x8C8C8C, () -> Items.STONE, () -> Items.STONE);
        register("cobblestone_blessing", 0x6B6B6B, () -> Items.COBBLESTONE, () -> Items.COBBLESTONE);
        register("deepslate_blessing", 0x3A3D42, () -> Items.DEEPSLATE, () -> Items.DEEPSLATE);
        register("cobbled_deepslate_blessing", 0x27282B, () -> Items.COBBLED_DEEPSLATE, () -> Items.COBBLED_DEEPSLATE);
        register("ender_pearl_blessing", 0x167C74, () -> Items.ENDER_PEARL, () -> Items.ENDER_PEARL);
        register("fire_charge_blessing", 0xE25822, () -> Items.FIRE_CHARGE, () -> Items.FIRE_CHARGE);
        register("snowball_blessing", 0xA0D8EF, () -> Items.SNOWBALL, () -> Items.SNOWBALL);
        register("arrow_blessing", 0xC0C0C0, () -> Items.ARROW, () -> Items.ARROW);
        register("potion_blessing", 0x9B59B6, () -> Items.NETHER_WART_BLOCK, () -> Items.NETHER_WART_BLOCK);
        register("ender_dragon_blessing", 0x4A154B, () -> Items.END_CRYSTAL, () -> Items.END_CRYSTAL);
        register("ignore_iframes_blessing", 0xFFA500, () -> Items.BLAZE_ROD, () -> Items.BLAZE_ROD);
        register("fang_blessing", 0x17DD62, () -> Items.EMERALD, () -> Items.EMERALD);
        register("lightning_blessing", 0x42C0FB, () -> Items.LIGHTNING_ROD, () -> Items.LIGHTNING_ROD);
        register("explosion_blessing", 0xCC2200, () -> Items.TNT, () -> Items.TNT);
    }

    private static void register(String id, int color, Supplier<Item> iconItem, Supplier<Item> brewingIngredient) {
        BlessingDefinition definition = new BlessingDefinition(id, color, iconItem, brewingIngredient);
        DEFINITIONS.put(id, definition);
        RegistryObject<MobEffect> effect = MOB_EFFECTS.register(id,
                () -> new BlessingMobEffect(MobEffectCategory.BENEFICIAL, color, definition));
        EFFECTS.put(id, effect);
    }

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
