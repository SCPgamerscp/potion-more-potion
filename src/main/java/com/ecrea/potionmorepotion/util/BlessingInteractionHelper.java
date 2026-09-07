package com.ecrea.potionmorepotion.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Helper to determine whether special breath attacks (Fire Charge, Snowball) should be suppressed
 * in order to prioritize block placement, block interactions (chests, doors, crafting tables),
 * and entity interactions (villagers, horses).
 */
public class BlessingInteractionHelper {

    public static boolean shouldSuppressBreath(Minecraft mc, LocalPlayer player) {
        // 1. Actively using an item with action (shield blocking, bow pulling, eating, drinking)
        if (player.isUsingItem()) {
            ItemStack usingItem = player.getUseItem();
            if (usingItem.getUseAnimation() != UseAnim.NONE) {
                return true;
            }
        }

        HitResult hit = mc.hitResult;
        if (hit == null) {
            return false;
        }

        // 2. Interacting with interactable entities (Villager trading, horse riding, chest carts)
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (!player.isShiftKeyDown() && (target instanceof Villager || target instanceof AbstractHorse || target instanceof ContainerEntity)) {
                return true;
            }
        }

        // 3. Interacting with blocks
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            // If the player holds a BlockItem in main-hand or off-hand, prioritize block placement!
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            if (main.getItem() instanceof BlockItem || off.getItem() instanceof BlockItem) {
                return true;
            }

            // If not sneaking, prioritize interacting with interactive blocks (chests, crafting tables, doors, buttons, etc.)
            if (!player.isShiftKeyDown() && mc.level != null) {
                BlockPos pos = blockHit.getBlockPos();
                BlockState state = mc.level.getBlockState(pos);
                Block block = state.getBlock();

                if (block instanceof EntityBlock
                        || block instanceof DoorBlock
                        || block instanceof TrapDoorBlock
                        || block instanceof FenceGateBlock
                        || block instanceof ButtonBlock
                        || block instanceof LeverBlock
                        || block instanceof BedBlock
                        || block instanceof AnvilBlock
                        || block instanceof CraftingTableBlock
                        || block instanceof EnchantmentTableBlock
                        || block instanceof BellBlock
                        || block instanceof ComposterBlock
                        || block instanceof LecternBlock
                        || block instanceof DiodeBlock
                        || block instanceof JukeboxBlock
                        || block instanceof CakeBlock
                        || block instanceof RespawnAnchorBlock) {
                    return true;
                }
            }
        }

        return false;
    }
}
