package com.ecrea.potionmorepotion.network;

import com.ecrea.potionmorepotion.effect.ModMobEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server every tick while holding left-click
 * with the Ignore I-Frames Blessing active and aiming at an entity.
 * Executes 100 attacks (ignoring invulnerability frames) in a single tick.
 */
public class RapidAttackPacket {

    private final int targetId;

    public RapidAttackPacket(int targetId) {
        this.targetId = targetId;
    }

    public RapidAttackPacket(FriendlyByteBuf buf) {
        this.targetId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.targetId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            var effectObj = ModMobEffects.EFFECTS.get("ignore_iframes_blessing");
            if (effectObj == null || !player.hasEffect(effectObj.get())) {
                return;
            }

            Entity target = player.serverLevel().getEntity(this.targetId);
            if (target == null || !target.isAlive() || !target.isAttackable() || target == player) {
                return;
            }

            // Verify distance (within 6 blocks)
            if (player.distanceToSqr(target) > 36.0D) {
                return;
            }

            // Perform 100 attacks in this single tick
            for (int i = 0; i < 100; i++) {
                if (!target.isAlive()) {
                    break;
                }

                // Reset invulnerableTime for direct living entities
                if (target instanceof LivingEntity living) {
                    living.invulnerableTime = 0;
                }
                // Reset invulnerableTime for multipart parent (e.g., EnderDragon)
                if (target instanceof net.minecraftforge.entity.PartEntity<?> part &&
                        part.getParent() instanceof LivingEntity parentLiving) {
                    if (!parentLiving.isAlive()) {
                        break;
                    }
                    parentLiving.invulnerableTime = 0;
                }

                player.attack(target);

                if (target instanceof LivingEntity living) {
                    living.invulnerableTime = 0;
                }
                if (target instanceof net.minecraftforge.entity.PartEntity<?> part &&
                        part.getParent() instanceof LivingEntity parentLiving) {
                    parentLiving.invulnerableTime = 0;
                }
            }
        });

        return true;
    }
}
