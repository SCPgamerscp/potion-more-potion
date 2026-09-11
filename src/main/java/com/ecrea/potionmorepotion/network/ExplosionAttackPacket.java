package com.ecrea.potionmorepotion.network;

import com.ecrea.potionmorepotion.effect.ModMobEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Packet sent from client to server every tick while the player holds right-click
 * with the Explosion Blessing active.
 * Causes an explosion with radius 6.0F at the target location up to 100 blocks away.
 * If the player is sneaking, terrain destruction is suppressed (ExplosionInteraction.NONE).
 * Otherwise, terrain is broken (ExplosionInteraction.BLOCK).
 * Dropped items and experience orbs are fully protected from destruction.
 */
public class ExplosionAttackPacket {

    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final boolean isSneaking;

    public ExplosionAttackPacket(double targetX, double targetY, double targetZ, boolean isSneaking) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.isSneaking = isSneaking;
    }

    public ExplosionAttackPacket(FriendlyByteBuf buf) {
        this.targetX = buf.readDouble();
        this.targetY = buf.readDouble();
        this.targetZ = buf.readDouble();
        this.isSneaking = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeDouble(this.targetX);
        buf.writeDouble(this.targetY);
        buf.writeDouble(this.targetZ);
        buf.writeBoolean(this.isSneaking);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            // Verify the player has active explosion_blessing
            var effectObj = ModMobEffects.EFFECTS.get("explosion_blessing");
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

            // Protect existing items and XP in blast radius before explosion
            AABB blastBox = new AABB(this.targetX - 9.0D, this.targetY - 9.0D, this.targetZ - 9.0D,
                                     this.targetX + 9.0D, this.targetY + 9.0D, this.targetZ + 9.0D);
            List<Entity> preEntities = level.getEntities((Entity) null, blastBox,
                    e -> e instanceof ItemEntity || e instanceof ExperienceOrb);
            for (Entity e : preEntities) {
                e.setInvulnerable(true);
                e.clearFire();
            }

            // Determine block interaction: Sneaking = NONE (Terrain Protection), Normal = BLOCK (Destruction)
            Level.ExplosionInteraction interaction = this.isSneaking
                    ? Level.ExplosionInteraction.NONE
                    : Level.ExplosionInteraction.BLOCK;

            // Trigger explosion with radius 6.0F, no fire blocks, player attribution
            level.explode(player, this.targetX, this.targetY, this.targetZ, 6.0F, false, interaction);

            // Protect newly dropped items / XP from block destruction so subsequent explosions don't destroy them
            List<Entity> postEntities = level.getEntities((Entity) null, blastBox,
                    e -> e instanceof ItemEntity || e instanceof ExperienceOrb);
            for (Entity e : postEntities) {
                e.setInvulnerable(true);
                e.clearFire();
            }
        });
        return true;
    }
}
