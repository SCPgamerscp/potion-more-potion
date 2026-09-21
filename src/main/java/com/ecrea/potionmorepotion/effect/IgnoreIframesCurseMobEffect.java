package com.ecrea.potionmorepotion.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import java.util.function.Consumer;

/**
 * Curse of Ignoring I-Frames (harmful status effect).
 * Negates all invulnerability frames on the afflicted entity every tick,
 * ensuring incoming attacks, projectile hits, and DoT damage
 * hit continuously without i-frame blocking.
 */
public class IgnoreIframesCurseMobEffect extends MobEffect {

    public IgnoreIframesCurseMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.invulnerableTime = 0;
        entity.hurtTime = 0;
    }

    @Override
    public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(new IClientMobEffectExtensions() {
            @Override
            public boolean renderInventoryIcon(net.minecraft.world.effect.MobEffectInstance instance,
                                                net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen<?> screen,
                                                net.minecraft.client.gui.GuiGraphics guiGraphics,
                                                int x, int y, int blitOffset) {
                guiGraphics.renderFakeItem(new ItemStack(Items.FERMENTED_SPIDER_EYE), x + 6, y + 7);
                return true;
            }

            @Override
            public boolean renderGuiIcon(net.minecraft.world.effect.MobEffectInstance instance,
                                          net.minecraft.client.gui.Gui gui,
                                          net.minecraft.client.gui.GuiGraphics guiGraphics,
                                          int x, int y, float z, float alpha) {
                guiGraphics.renderFakeItem(new ItemStack(Items.FERMENTED_SPIDER_EYE), x + 3, y + 3);
                return true;
            }
        });
    }
}
