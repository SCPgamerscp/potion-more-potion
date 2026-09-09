package com.ecrea.potionmorepotion.network;

import com.ecrea.potionmorepotion.effect.ModMobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server every tick while the player holds right-click
 * with the Potion Blessing active.
 * Spawns 3 Splash Potions of Harming II (or Healing II when sneaking) in a conical spread.
 */
public class PotionBreathPacket {

    public PotionBreathPacket() {
    }

    public PotionBreathPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            if (!player.hasEffect(ModMobEffects.EFFECTS.get("potion_blessing").get())) {
                return;
            }

            if (player.isUsingItem()) {
                ItemStack usingItem = player.getUseItem();
                UseAnim anim = usingItem.getUseAnimation();
                if (anim != UseAnim.NONE) {
                    return;
                }
            }

            ServerLevel level = player.serverLevel();
            RandomSource random = level.getRandom();

            Vec3 forward = player.getLookAngle().normalize();
            Vec3 up = new Vec3(0, 1, 0);
            Vec3 right = forward.cross(up).normalize();
            if (right.lengthSqr() < 1e-4) {
                right = new Vec3(1, 0, 0);
            }
            Vec3 trueUp = right.cross(forward).normalize();

            float spread = 0.20F;
            float speed = 1.4F;

            boolean isSneaking = player.isShiftKeyDown() || player.isCrouching();
            ItemStack potionStack = PotionUtils.setPotion(
                    new ItemStack(Items.SPLASH_POTION),
                    isSneaking ? Potions.STRONG_HEALING : Potions.STRONG_HARMING
            );

            // Spawn 3 splash potions per tick in conical spread
            for (int i = 0; i < 3; i++) {
                double angle = random.nextDouble() * 2.0 * Math.PI;
                double radius = Math.sqrt(random.nextDouble()) * spread;
                double dx = Math.cos(angle) * radius;
                double dy = Math.sin(angle) * radius;

                Vec3 dir = forward.add(right.scale(dx)).add(trueUp.scale(dy)).normalize();

                ThrownPotion thrownPotion = new ThrownPotion(level, player);
                thrownPotion.setItem(potionStack);

                double spawnX = player.getX() + forward.x * 0.7;
                double spawnY = player.getEyeY() - 0.15 + forward.y * 0.7;
                double spawnZ = player.getZ() + forward.z * 0.7;
                thrownPotion.setPos(spawnX, spawnY, spawnZ);
                thrownPotion.shoot(dir.x, dir.y, dir.z, speed, 0.0F);

                level.addFreshEntity(thrownPotion);
            }

            // Audio & visual particles
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
                    0.35F, 0.9F + (random.nextFloat() - random.nextFloat()) * 0.2F);

            double spawnX = player.getX() + forward.x * 0.7;
            double spawnY = player.getEyeY() - 0.15 + forward.y * 0.7;
            double spawnZ = player.getZ() + forward.z * 0.7;
            level.sendParticles(isSneaking ? ParticleTypes.HEART : ParticleTypes.INSTANT_EFFECT,
                    spawnX, spawnY, spawnZ,
                    4, forward.x * 0.2, forward.y * 0.2, forward.z * 0.2, 0.08);
        });

        return true;
    }
}
