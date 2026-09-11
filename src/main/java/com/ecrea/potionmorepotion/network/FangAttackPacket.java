package com.ecrea.potionmorepotion.network;

import com.ecrea.potionmorepotion.effect.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server every tick while the player holds right-click
 * with the Fang Blessing (Blessing of Fangs) active.
 * Summons a cluster of Evoker Fangs in a 5-block diameter circle at the target location.
 */
public class FangAttackPacket {

    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final boolean snapToGround;

    public FangAttackPacket(double targetX, double targetY, double targetZ, boolean snapToGround) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.snapToGround = snapToGround;
    }

    public FangAttackPacket(FriendlyByteBuf buf) {
        this.targetX = buf.readDouble();
        this.targetY = buf.readDouble();
        this.targetZ = buf.readDouble();
        this.snapToGround = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeDouble(this.targetX);
        buf.writeDouble(this.targetY);
        buf.writeDouble(this.targetZ);
        buf.writeBoolean(this.snapToGround);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            // Verify the player has active fang_blessing
            var effectObj = ModMobEffects.EFFECTS.get("fang_blessing");
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
            RandomSource random = level.getRandom();

            // Sound effect at target location
            level.playSound(null, this.targetX, this.targetY, this.targetZ,
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS,
                    0.4F, 1.2F + (random.nextFloat() - random.nextFloat()) * 0.2F);

            // 1. Center fang
            spawnFang(level, this.targetX, this.targetY, this.targetZ, random.nextFloat() * 360.0F, 0, player, this.snapToGround);

            // 2. Inner ring (radius ~1.2 blocks, 4 fangs)
            double innerRadius = 1.2;
            for (int i = 0; i < 4; i++) {
                double angle = (i * (Math.PI / 2.0)) + (random.nextDouble() - 0.5) * 0.3;
                double fx = this.targetX + Math.cos(angle) * (innerRadius + (random.nextDouble() - 0.5) * 0.3);
                double fz = this.targetZ + Math.sin(angle) * (innerRadius + (random.nextDouble() - 0.5) * 0.3);
                spawnFang(level, fx, this.targetY, fz, random.nextFloat() * 360.0F, 0, player, this.snapToGround);
            }

            // 3. Outer ring (radius ~2.3 blocks, 8 fangs covering 5-block diameter)
            double outerRadius = 2.3;
            for (int i = 0; i < 8; i++) {
                double angle = (i * (Math.PI / 4.0)) + (random.nextDouble() - 0.5) * 0.2;
                double fx = this.targetX + Math.cos(angle) * (outerRadius + (random.nextDouble() - 0.5) * 0.3);
                double fz = this.targetZ + Math.sin(angle) * (outerRadius + (random.nextDouble() - 0.5) * 0.3);
                spawnFang(level, fx, this.targetY, fz, random.nextFloat() * 360.0F, 0, player, this.snapToGround);
            }
        });
        return true;
    }

    private static void spawnFang(ServerLevel level, double x, double targetY, double z, float yRot, int warmupDelay, ServerPlayer player, boolean snapToGround) {
        double groundY = targetY;

        if (snapToGround) {
            BlockPos blockpos = BlockPos.containing(x, targetY, z);
            // Search from 3 blocks above to 6 blocks below for solid ground surface
            for (int dy = 3; dy >= -6; dy--) {
                BlockPos checkPos = blockpos.above(dy);
                BlockPos belowPos = checkPos.below();
                BlockState belowState = level.getBlockState(belowPos);
                BlockState currentState = level.getBlockState(checkPos);

                if (belowState.isFaceSturdy(level, belowPos, Direction.UP)) {
                    double d0 = 0.0D;
                    VoxelShape shape = currentState.getCollisionShape(level, checkPos);
                    if (!shape.isEmpty()) {
                        d0 = shape.max(Direction.Axis.Y);
                    }
                    groundY = (double) belowPos.getY() + 1.0D + d0;
                    break;
                }
            }
        }

        EvokerFangs fangs = new EvokerFangs(level, x, groundY, z, yRot, warmupDelay, player);
        level.addFreshEntity(fangs);
    }
}
