package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 矢の加護: 5倍速・無限クリティカル矢発射ハンドラー
 * 弓不要で手から直接超高速（15.0F）の矢を放ちます。
 * 回収不可（DISALLOWED）、ブロック・アイテム操作優先。
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID)
public class ArrowBlessingShootHandler {

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        handleArrowShoot(event.getEntity(), event.getLevel(), event.getHand());
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();

        // 優先度判定: 盾、弓、クロスボウ、食事、またはブロック設置アイテムはそちらを優先
        if (itemStack.getItem() instanceof BlockItem) {
            return;
        }

        UseAnim anim = itemStack.getUseAnimation();
        if (anim != UseAnim.NONE) {
            return;
        }

        handleArrowShoot(player, event.getLevel(), event.getHand());
    }

    private static void handleArrowShoot(Player player, Level level, InteractionHand hand) {
        var effectObj = ModMobEffects.EFFECTS.get("arrow_blessing");
        if (effectObj == null || !player.hasEffect(effectObj.get())) {
            return;
        }

        // オフハンドとの二重発動を防止
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F));

        if (!level.isClientSide) {
            Arrow arrow = new Arrow(level, player);
            arrow.setCritArrow(true);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;

            // バニラ弓の最大初速 3.0F の 5倍 = 15.0F
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 15.0F, 0.5F);
            level.addFreshEntity(arrow);
        }
    }
}
