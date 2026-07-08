package com.ecrea.potionmorepotion.effect;

import com.ecrea.potionmorepotion.Config;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import java.util.function.Consumer;

/**
 * Shared implementation for every "blessing" potion effect.
 * Heals 2 HP (configurable) every 20 ticks (once per second), regardless of hunger.
 * Damage reduction is NOT implemented here; it is applied globally for every active
 * BlessingMobEffect instance in BlessingDamageHandler (LivingHurtEvent), independent
 * from vanilla Resistance so the two can stack.
 *
 * The effect icon (inventory tooltip + HUD) reuses the actual material item's icon
 * (e.g. Oak Log) instead of a dedicated PNG. This is implemented via initializeClient(),
 * which only ever hands a Consumer<IClientMobEffectExtensions> to the caller: the
 * Consumer<T> parameter type is erased at the bytecode level, so this class itself never
 * directly references client-only render classes and stays safe to load on a dedicated
 * server (only the anonymous IClientMobEffectExtensions implementation, which Forge never
 * instantiates server-side, references them).
 */
public class BlessingMobEffect extends MobEffect {

    private final BlessingDefinition definition;

    public BlessingMobEffect(MobEffectCategory category, int color, BlessingDefinition definition) {
        super(category, color);
        this.definition = definition;
    }

    public BlessingDefinition getDefinition() {
        return definition;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            entity.heal(Config.healAmount);
        }
    }

    @Override
    public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(new IClientMobEffectExtensions() {
            @Override
            public boolean renderInventoryIcon(net.minecraft.world.effect.MobEffectInstance instance,
                                                net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen<?> screen,
                                                net.minecraft.client.gui.GuiGraphics guiGraphics,
                                                int x, int y, int blitOffset) {
                guiGraphics.renderFakeItem(new ItemStack(definition.iconItem().get()), x + 6, y + 7);
                return true;
            }

            @Override
            public boolean renderGuiIcon(net.minecraft.world.effect.MobEffectInstance instance,
                                          net.minecraft.client.gui.Gui gui,
                                          net.minecraft.client.gui.GuiGraphics guiGraphics,
                                          int x, int y, float z, float alpha) {
                guiGraphics.renderFakeItem(new ItemStack(definition.iconItem().get()), x + 3, y + 3);
                return true;
            }
        });
    }
}
