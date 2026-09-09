package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import com.ecrea.potionmorepotion.network.DragonBreathPacket;
import com.ecrea.potionmorepotion.network.ModMessages;
import com.ecrea.potionmorepotion.util.BlessingInteractionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side handler for the Ender Dragon Blessing dragon fireball thrower.
 * Detects right-click (keyUse) hold every tick and sends a DragonBreathPacket to the server.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID, value = Dist.CLIENT)
public class DragonBlessingHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        if (mc.screen != null) {
            return;
        }

        var effectObj = ModMobEffects.EFFECTS.get("ender_dragon_blessing");
        if (effectObj == null || !player.hasEffect(effectObj.get())) {
            return;
        }

        if (!mc.options.keyUse.isDown()) {
            return;
        }

        if (BlessingInteractionHelper.shouldSuppressBreath(mc, player)) {
            return;
        }

        ModMessages.sendToServer(new DragonBreathPacket());

        // Spawn client-side visual particles
        RandomSource random = player.level().getRandom();
        Vec3 forward = player.getLookAngle().normalize();
        double spawnX = player.getX() + forward.x * 0.6;
        double spawnY = player.getEyeY() - 0.15 + forward.y * 0.6;
        double spawnZ = player.getZ() + forward.z * 0.6;

        float spread = 0.20F;
        for (int i = 0; i < 3; i++) {
            double dx = forward.x + (random.nextDouble() - 0.5) * spread;
            double dy = forward.y + (random.nextDouble() - 0.5) * spread;
            double dz = forward.z + (random.nextDouble() - 0.5) * spread;
            player.level().addParticle(ParticleTypes.DRAGON_BREATH,
                    spawnX, spawnY, spawnZ, dx * 0.3, dy * 0.3, dz * 0.3);
        }
    }
}
