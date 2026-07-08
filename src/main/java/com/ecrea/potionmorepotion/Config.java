package com.ecrea.potionmorepotion;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * COMMON config. Static fields keep the config-default values as a safe fallback
 * so that registration lambdas (which may run before the config file is fully loaded)
 * never see an uninitialized value; onLoad() refreshes them once Forge loads the file.
 *
 * NOTE: because Potion objects bake their MobEffectInstance durations in at registration
 * time, changing baseDurationSeconds / arrowDurationSeconds in the config requires a
 * game restart to take effect (this is an inherent limitation of vanilla's Potion class,
 * not specific to this mod).
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue BASE_DURATION_SECONDS = BUILDER
            .comment("Duration in seconds for normal / splash / lingering potions (default 600 = 10 minutes). Requires game restart to take effect.")
            .defineInRange("baseDurationSeconds", 600, 1, 24000);

    private static final ForgeConfigSpec.IntValue ARROW_DURATION_SECONDS = BUILDER
            .comment("Duration in seconds applied to tipped arrow effects (default 300 = 5 minutes). Requires game restart to take effect.")
            .defineInRange("arrowDurationSeconds", 300, 1, 24000);

    private static final ForgeConfigSpec.DoubleValue HEAL_AMOUNT_PER_SECOND = BUILDER
            .comment("HP healed every second (every 20 ticks) while a blessing effect is active.")
            .defineInRange("healAmountPerSecond", 2.0, 0.0, 100.0);

    private static final ForgeConfigSpec.DoubleValue DAMAGE_REDUCTION_PER_BLESSING = BUILDER
            .comment("Fraction of incoming damage reduced per active blessing effect (0.5 = 50%). Multiple blessings stack multiplicatively.")
            .defineInRange("damageReductionPerBlessing", 0.5, 0.0, 1.0);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int baseDurationTicks = 600 * 20;
    public static int arrowDurationTicks = 300 * 20;
    public static float healAmount = 2.0F;
    public static float damageReduction = 0.5F;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            baseDurationTicks = BASE_DURATION_SECONDS.get() * 20;
            arrowDurationTicks = ARROW_DURATION_SECONDS.get() * 20;
            healAmount = HEAL_AMOUNT_PER_SECOND.get().floatValue();
            damageReduction = DAMAGE_REDUCTION_PER_BLESSING.get().floatValue();
        }
    }
}
