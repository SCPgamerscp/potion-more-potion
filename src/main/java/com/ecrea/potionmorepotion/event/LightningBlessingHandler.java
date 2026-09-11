package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import com.ecrea.potionmorepotion.network.LightningAttackPacket;
import com.ecrea.potionmorepotion.network.ModMessages;
import com.ecrea.potionmorepotion.util.BlessingInteractionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side handler for the Lightning Blessing (Blessing of Lightning).
 * Detects right-click (keyUse) hold every tick, calculates target location up to 100 blocks away,
 * and sends LightningAttackPacket to the server to strike lightning bolts at the target every tick.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID, value = Dist.CLIENT)
public class LightningBlessingHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null) {
            return;
        }

        var effectObj = ModMobEffects.EFFECTS.get("lightning_blessing");
        if (effectObj == null || !player.hasEffect(effectObj.get())) {
            return;
        }

        if (!mc.options.keyUse.isDown()) {
            return;
        }

        if (BlessingInteractionHelper.shouldSuppressBreath(mc, player)) {
            return;
        }

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 reachVec = eyePos.add(lookVec.scale(100.0D));

        ClipContext clip = new ClipContext(eyePos, reachVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult blockHit = mc.level.clip(clip);

        Vec3 targetVec = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : reachVec;

        AABB box = player.getBoundingBox().expandTowards(lookVec.scale(100.0D)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                mc.level, player, eyePos, reachVec, box,
                e -> !e.isSpectator() && e.isPickable());

        if (entityHit != null) {
            double entityDistSqr = eyePos.distanceToSqr(entityHit.getLocation());
            double blockDistSqr = eyePos.distanceToSqr(targetVec);
            if (entityDistSqr < blockDistSqr) {
                targetVec = entityHit.getLocation();
            }
        }

        boolean isSneaking = player.isShiftKeyDown() || player.isCrouching();
        ModMessages.sendToServer(new LightningAttackPacket(targetVec.x, targetVec.y, targetVec.z, isSneaking));

        // Client visual particles: electric spark particles in front of player
        for (int i = 0; i < 2; i++) {
            double px = player.getX() + lookVec.x * 0.8 + (player.level().random.nextDouble() - 0.5) * 0.4;
            double py = player.getEyeY() - 0.2 + lookVec.y * 0.8 + (player.level().random.nextDouble() - 0.5) * 0.4;
            double pz = player.getZ() + lookVec.z * 0.8 + (player.level().random.nextDouble() - 0.5) * 0.4;
            player.level().addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz,
                    (player.level().random.nextDouble() - 0.5) * 0.2,
                    player.level().random.nextDouble() * 0.2,
                    (player.level().random.nextDouble() - 0.5) * 0.2);
        }
    }
}
