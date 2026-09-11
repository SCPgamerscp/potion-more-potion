package com.ecrea.potionmorepotion.util;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.item.SaddleItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.SpawnEggItem;
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
 * 1. Entity and block placement (holding BlockItem, SpawnEggItem, BoatItem, MinecartItem, ArmorStandItem, etc.)
 * 2. World interactions (buckets, bonemeal, tilling, pathing, stripping, etc.)
 * 3. Block interactions (chests, doors, crafting tables, anvils, etc.)
 * 4. Entity interactions (trading, mounting, feeding/breeding, shearing, milking, sitting pets, armor stands, etc.)
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
            if (event.isCanceled() || event.getCancellationResult().consumesAction() ||
                    canInteractWithEntity(localPlayer, event.getTarget(), event.getHand())) {
                suppressUntilGameTime = event.getLevel().getGameTime() + 10L;
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof LocalPlayer localPlayer) {
            if (event.isCanceled() || event.getCancellationResult().consumesAction() ||
                    canInteractWithEntity(localPlayer, event.getTarget(), event.getHand())) {
                suppressUntilGameTime = event.getLevel().getGameTime() + 10L;
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof LocalPlayer localPlayer) {
            if (event.isCanceled() || event.getCancellationResult().consumesAction() ||
                    isPlacementOrWorldUseItem(event.getItemStack())) {
                suppressUntilGameTime = event.getLevel().getGameTime() + 10L;
            }
        }
    }

    public static boolean isPlacementOrWorldUseItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item instanceof BlockItem
                || item instanceof SpawnEggItem
                || item instanceof BoatItem
                || item instanceof MinecartItem
                || item instanceof ArmorStandItem
                || item instanceof EndCrystalItem
                || item instanceof HangingEntityItem
                || item instanceof BucketItem
                || item instanceof SolidBucketItem
                || item instanceof MobBucketItem
                || item instanceof BoneMealItem
                || item instanceof FlintAndSteelItem
                || item instanceof FireChargeItem
                || item instanceof HoeItem
                || item instanceof ShovelItem
                || item instanceof AxeItem
                || item instanceof ShearsItem;
    }

    public static boolean shouldSuppressBreath(Minecraft mc, LocalPlayer player) {
        // 1. Actively using an item with action (shield blocking, bow pulling, eating, drinking)
        if (player.isUsingItem()) {
            ItemStack usingItem = player.getUseItem();
            if (usingItem.getUseAnimation() != UseAnim.NONE) {
                return true;
            }
        }

        // 2. Recent interaction buffer (suppress for 10 ticks = 0.5s after an interaction occurred)
        if (mc.level != null && mc.level.getGameTime() < suppressUntilGameTime) {
            return true;
        }

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        // 3. Prioritize placing entities / blocks or using tools in the world (boats, spawn eggs, buckets, blocks)
        if (isPlacementOrWorldUseItem(main) || isPlacementOrWorldUseItem(off)) {
            HitResult hit = mc.hitResult;
            if (hit != null && (hit.getType() == HitResult.Type.BLOCK ||
                    main.getItem() instanceof BoatItem || off.getItem() instanceof BoatItem ||
                    main.getItem() instanceof BucketItem || off.getItem() instanceof BucketItem)) {
                return true;
            }
        }

        HitResult hit = mc.hitResult;
        if (hit == null) {
            return false;
        }

        // 4. Interacting with interactable entities (Trading, mounting, petting, shearing, feeding, etc.)
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (canInteractWithEntity(player, target, InteractionHand.MAIN_HAND) ||
                    canInteractWithEntity(player, target, InteractionHand.OFF_HAND)) {
                return true;
            }
        }

        // 5. Interacting with blocks
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
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
        if (held.getItem() instanceof SpawnEggItem) {
            // Spawns baby of matching mob or spawns entity on click
            return true;
        }
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

        // 12. Curing Zombie Villager with Golden Apple
        if (target instanceof ZombieVillager zombieVillager && held.is(Items.GOLDEN_APPLE)) {
            if (zombieVillager.hasEffect(MobEffects.WEAKNESS)) {
                return true;
            }
        }

        // 13. Bartering with Piglin with Gold Ingot
        if (target instanceof Piglin piglin && held.is(Items.GOLD_INGOT) && piglin.isAdult()) {
            return true;
        }

        // 14. Igniting Creeper with Flint and Steel or Fire Charge
        if (target instanceof Creeper && (held.getItem() instanceof FlintAndSteelItem || held.getItem() instanceof FireChargeItem)) {
            return true;
        }

        return false;
    }
}
