package com.ecrea.potionmorepotion.network;

import com.ecrea.potionmorepotion.effect.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Packet sent from client to server every tick while the player holds right-click
 * with the Lightning Blessing (Blessing of Lightning) active.
 * Spawns a lightning bolt at the target location up to 100 blocks away with zero ground fire blocks,
 * full item/XP protection, mob transformations, and player-attributed damage.
 */
public class LightningAttackPacket {

    private final double targetX;
    private final double targetY;
    private final double targetZ;

    public LightningAttackPacket(double targetX, double targetY, double targetZ) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    public LightningAttackPacket(FriendlyByteBuf buf) {
        this.targetX = buf.readDouble();
        this.targetY = buf.readDouble();
        this.targetZ = buf.readDouble();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeDouble(this.targetX);
        buf.writeDouble(this.targetY);
        buf.writeDouble(this.targetZ);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            // Verify active lightning_blessing
            var effectObj = ModMobEffects.EFFECTS.get("lightning_blessing");
            if (effectObj == null || !player.hasEffect(effectObj.get())) {
                return;
            }

            // Prioritize shield, bow, food, etc.
            if (player.isUsingItem()) {
                ItemStack usingItem = player.getUseItem();
                UseAnim anim = usingItem.getUseAnimation();
                if (anim != UseAnim.NONE) {
                    return;
                }
            }

            // Check range limit (up to ~120 blocks)
            if (player.distanceToSqr(this.targetX, this.targetY, this.targetZ) > 120.0 * 120.0) {
                return;
            }

            ServerLevel level = player.serverLevel();

            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) {
                return;
            }

            bolt.moveTo(this.targetX, this.targetY, this.targetZ);
            bolt.setCause(player);

            // Ground fire blocks are ALWAYS zero: visualOnly completely prevents vanilla LightningBolt.spawnFire()!
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);

            // Extinguish any pre-existing fire in the area
            BlockPos strikePos = BlockPos.containing(this.targetX, this.targetY, this.targetZ);
            cleanFireAround(level, strikePos);

            // Query all entities in the impact box (including non-living entities like EndCrystal, PartEntity, Boat, Minecart)
            AABB box = new AABB(this.targetX - 3.0D, this.targetY - 3.0D, this.targetZ - 3.0D,
                                this.targetX + 3.0D, this.targetY + 9.0D, this.targetZ + 3.0D);
            List<Entity> list = level.getEntities(bolt, box, e -> !e.isRemoved());

            var damageTypeHolder = level.registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.LIGHTNING_BOLT);
            DamageSource source = new DamageSource(damageTypeHolder, bolt, player);
            var lightningObj = ModMobEffects.EFFECTS.get("lightning_blessing");

            for (Entity target : list) {
                // 1. Completely protect dropped items and experience orbs from lightning, fire, and explosions
                if (target instanceof ItemEntity itemEntity) {
                    itemEntity.setInvulnerable(true);
                    itemEntity.clearFire();
                    continue;
                }
                if (target instanceof ExperienceOrb orb) {
                    orb.setInvulnerable(true);
                    orb.clearFire();
                    continue;
                }

                // 2. Skip caster and players/allies with active lightning blessing
                if (target == player) {
                    continue;
                }
                if (target instanceof LivingEntity living && lightningObj != null && living.hasEffect(lightningObj.get())) {
                    continue;
                }

                // 3. If living entity: trigger mob transformations (Charged Creeper, Piglin, Witch, Mooshroom)
                // and 8-second burning debuff on enemy
                if (target instanceof LivingEntity living) {
                    living.thunderHit(level, bolt);
                    living.setSecondsOnFire(8);
                    living.setLastHurtByPlayer(player);
                }

                // 4. Deal player-attributed lightning damage (works on LivingEntity, EndCrystal, PartEntity, Boat, Minecart, etc.!)
                target.hurt(source, bolt.getDamage());
            }
        });
        return true;
    }

    private static void cleanFireAround(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -2, -3), center.offset(3, 4, 3))) {
            if (level.getBlockState(pos).getBlock() instanceof BaseFireBlock) {
                level.removeBlock(pos, false);
            }
        }
    }
}
