package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * エンダーパールの加護: 5倍速・無限エンダーパール投擲ハンドラー
 * ブロック/エンティティ操作、および盾・弓・食事などのアイテム使用を最優先します。
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID)
public class EnderPearlBlessingHandler {

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        // 空中での右クリック時に発動
        handlePearlThrow(event.getEntity(), event.getLevel(), event.getHand());
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();

        // 優先度判定: 盾、弓、クロスボウ、食べ物、飲み物などの使用アクションを持つアイテムはそちらを優先
        UseAnim anim = itemStack.getUseAnimation();
        if (anim != UseAnim.NONE) {
            return; // 盾ガード、弓、食事等を優先して投擲しない
        }

        handlePearlThrow(player, event.getLevel(), event.getHand());
    }

    private static void handlePearlThrow(Player player, Level level, InteractionHand hand) {
        if (!player.hasEffect(ModMobEffects.EFFECTS.get("ender_pearl_blessing").get())) {
            return;
        }

        // オフハンドとの二重発動を防止
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_PEARL_THROW, SoundSource.NEUTRAL,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            ThrownEnderpearl enderpearl = new ThrownEnderpearl(level, player);
            enderpearl.setItem(new ItemStack(Items.ENDER_PEARL));
            // バニラ速度 1.5F の 5倍 = 7.5F (クールダウンなし連射)
            enderpearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 7.5F, 1.0F);
            level.addFreshEntity(enderpearl);
        }

        player.awardStat(Stats.ITEM_USED.get(Items.ENDER_PEARL));
    }
}