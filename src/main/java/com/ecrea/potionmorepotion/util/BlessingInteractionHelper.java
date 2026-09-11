package com.ecrea.potionmorepotion.util;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.item.SaddleItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Helper to determine whether special breath attacks (Fire Charge, Snowball, Potion, Dragon)
 * should be suppressed in order to prioritize:
 * 1. Block placement (holding BlockItem)
 * 2. Block interactions (chests, doors, crafting tables, anvils, etc.)
 * 3. Entity interactions (trading, mounting, feeding/breeding, shearing, milking, sitting pets, armor stands, etc.)
 *
 * When an entity interaction is NOT taking place (e.g. aiming at enemies, or aiming from a distance,
 * or holding items that don't interact with the entity), shooting is fully permitted so players can
 * attack or heal entities intentionally.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID, value = Dist.CLIENT)
public class BlessingInteractionHelper {

    private static long suppressUntilGameTime = 0L;

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof LocalPlayer localPlayer) {
            if (canInteractWithEntity(localPlayer, event.getTarget(), event.getHand())) {
                suppressUntilGameTime = event.getLevel().getGameTime() + 10L;
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof LocalPlayer localPlayer) {
            if (canInteractWithEntity(localPlayer, event.getTarget(), event.getHand())) {
                suppressUntilGameTime = event.getLevel().getGameTime() + 10L;
            }
        }
    }

    public static boolean shouldSuppressBreath(Minecraft mc, LocalPlayer player) {
        // 1. Actively using an item with action (shield blocking, bow pulling, eating, drinking)
        if (player.isUsingItem()) {
            ItemStack usingItem = player.getUseItem();
            if (usingItem.getUseAnimation() != UseAnim.NONE) {
                return true;
            }
        }

        // 2. Recent entity interaction buffer (suppress for 10 ticks = 0.5s after an interaction occurred)
        if (mc.level != null && mc.level.getGameTime() < suppressUntilGameTime) {
            return true;
        }

        HitResult hit = mc.hitResult;
        if (hit == null) {
            return false;
        }

        // 3. Interacting with interactable entities (Trading, mounting, petting, shearing, feeding, etc.)
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (canInteractWithEntity(player, target, InteractionHand.MAIN_HAND) ||
                    canInteractWithEntity(player, target, InteractionHand.OFF_HAND)) {
                return true;
            }
        }

        // 4. Interacting with blocks
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

    public static boolean canInteractWithEntity(LocalPlayer player, Entity target, InteractionHand hand) {
        if (target == null || !target.isAlive() || target == player) {
            return false;
        }

        // Check interaction reach distance (typically 3.0 blocks in survival)
        double reach = 3.0D;
        try {
            if (net.minecraftforge.common.ForgeMod.ENTITY_REACH.isPresent()) {
                reach = player.getAttributeValue(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());
            }
        } catch (Exception ignored) {
        }
        if (player.distanceToSqr(target) > reach * reach) {
            return false; // Beyond reach: allow shooting!
        }

        ItemStack held = player.getItemInHand(hand);

        // 1. General interaction items that interact with mobs
        if (held.getItem() instanceof NameTagItem && held.hasCustomHoverName()) {
            return true;
        }
        if (held.getItem() instanceof LeadItem && target instanceof Mob mob && mob.canBeLeashed(player)) {
            return true;
        }
        if (held.getItem() instanceof SaddleItem && target instanceof Saddleable saddleable && saddleable.isSaddleable()) {
            return true;
        }

        // 2. NPCs (Villagers, Wandering Traders) - right click opens trade menu
        if (target instanceof Npc) {
            if (target instanceof Villager villager) {
                if (!villager.isSleeping()) {
                    return true;
                }
            } else {
                return true;
            }
        }

        // 3. Vehicles and Rideables (Boat, Minecart, ContainerEntity)
        if (target instanceof Boat || target instanceof AbstractMinecart || target instanceof ContainerEntity) {
            if (!player.isShiftKeyDown() || target instanceof ContainerEntity) {
                return true;
            }
        }

        // 4. Equines & Mounts (Horse, Donkey, Mule, Llama, Camel, Pig/Strider with saddle)
        if (target instanceof AbstractHorse horse) {
            if (!player.isShiftKeyDown() || horse.isTamed()) {
                return true;
            }
            if (horse.isFood(held)) {
                return true;
            }
        }
        if (target instanceof Camel) {
            return true;
        }
        if (target instanceof Pig pig && pig.isSaddled() && !player.isShiftKeyDown()) {
            return true;
        }
        if (target instanceof Strider strider && strider.isSaddled() && !player.isShiftKeyDown()) {
            return true;
        }

        // 5. Pets (TamableAnimal like Wolf, Cat) and Parrots
        if (target instanceof TamableAnimal tamable) {
            if (tamable.isTame() && tamable.isOwnedBy(player)) {
                // Toggles sitting / standing
                return true;
            }
            if (!tamable.isTame() && (held.is(Items.BONE) || held.is(Items.COD) || held.is(Items.SALMON))) {
                return true;
            }
            if (tamable.isFood(held)) {
                return true;
            }
        }
        if (target instanceof Parrot parrot) {
            if (parrot.isTame() && parrot.isOwnedBy(player)) {
                return true;
            }
            if (!parrot.isTame() && held.is(net.minecraft.tags.ItemTags.VILLAGER_PLANTABLE_SEEDS)) {
                return true;
            }
        }

        // 6. Livestock & Animals (Breeding, feeding baby)
        if (target instanceof Animal animal) {
            if (animal.isFood(held)) {
                return true;
            }
        }

        // 7. Sheep (Shearing with shears, Dyeing with dye)
        if (target instanceof Sheep sheep) {
            if (held.is(Items.SHEARS) && sheep.readyForShearing()) {
                return true;
            }
            if (held.getItem() instanceof DyeItem) {
                return true;
            }
        }

        // 8. Cows / Mooshrooms / Goats (Milking with bucket, stew with bowl)
        if (target instanceof Cow cow) {
            if (held.is(Items.BUCKET) && !cow.isBaby()) {
                return true;
            }
            if (target instanceof MushroomCow mooshroom) {
                if (held.is(Items.BOWL) || (held.is(Items.SHEARS) && mooshroom.readyForShearing())) {
                    return true;
                }
            }
        }
        if (target instanceof Goat goat && held.is(Items.BUCKET) && !goat.isBaby()) {
            return true;
        }

        // 9. Armor Stands & Item Frames
        if (target instanceof ArmorStand || target instanceof ItemFrame) {
            return true;
        }

        // 10. Allay
        if (target instanceof Allay) {
            return true;
        }

        // 11. Iron Golem repair with iron ingot
        if (target instanceof IronGolem golem) {
            if (held.is(Items.IRON_INGOT) && golem.getHealth() < golem.getMaxHealth()) {
                return true;
            }
        }

        return false;
    }
}
