package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import com.ecrea.potionmorepotion.network.FangAttackPacket;
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
 * Client-side handler for the Fang Blessing (Blessing of Fangs).
 * Detects right-click (keyUse) hold every tick, calculates target location up to 100 blocks away,
 * and sends FangAttackPacket to the server to summon a cluster of Evoker Fangs in a 5-block diameter area.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID, value = Dist.CLIENT)
public class FangBlessingHandler {

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

        var effectObj = ModMobEffects.EFFECTS.get("fang_blessing");
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

        boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;
        Vec3 targetVec = hitBlock ? blockHit.getLocation() : reachVec;
        boolean hitEntity = false;

        AABB box = player.getBoundingBox().expandTowards(lookVec.scale(100.0D)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                mc.level, player, eyePos, reachVec, box,
                e -> !e.isSpectator() && e.isPickable());

        if (entityHit != null) {
            Vec3 hitPos = entityHit.getLocation();
            var clipOpt = entityHit.getEntity().getBoundingBox().inflate(0.3D).clip(eyePos, reachVec);
            if (clipOpt.isPresent()) {
                hitPos = clipOpt.get();
            }
            double entityDistSqr = eyePos.distanceToSqr(hitPos);
            double blockDistSqr = eyePos.distanceToSqr(targetVec);
            if (!hitBlock || entityDistSqr < blockDistSqr) {
                targetVec = hitPos;
                hitEntity = true;
            }
        }

        // Only snap to ground when directly hitting a solid block and NOT targeting an entity or open air
        boolean snapToGround = hitBlock && !hitEntity;
        ModMessages.sendToServer(new FangAttackPacket(targetVec.x, targetVec.y, targetVec.z, snapToGround));

        // Client visual particles: magic spell particles in front of player
        for (int i = 0; i < 2; i++) {
            double px = player.getX() + lookVec.x * 0.8 + (player.level().random.nextDouble() - 0.5) * 0.4;
            double py = player.getEyeY() - 0.2 + lookVec.y * 0.8 + (player.level().random.nextDouble() - 0.5) * 0.4;
            double pz = player.getZ() + lookVec.z * 0.8 + (player.level().random.nextDouble() - 0.5) * 0.4;
            player.level().addParticle(ParticleTypes.ENCHANT, px, py, pz, 0, 0, 0);
        }
    }
}
