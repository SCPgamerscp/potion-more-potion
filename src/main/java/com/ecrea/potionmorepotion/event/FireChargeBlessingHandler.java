package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import com.ecrea.potionmorepotion.network.FireBreathPacket;
import com.ecrea.potionmorepotion.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side handler for the Fire Charge Blessing flamethrower.
 * Detects right-click (keyUse) hold every tick and sends a FireBreathPacket to the server.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID, value = Dist.CLIENT)
public class FireChargeBlessingHandler {

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

        // Do not fire if any GUI/screen (chat, inventory, pause, etc.) is open
        if (mc.screen != null) {
            return;
        }

        // Check if player has active fire_charge_blessing
        var effectObj = ModMobEffects.EFFECTS.get("fire_charge_blessing");
        if (effectObj == null || !player.hasEffect(effectObj.get())) {
            return;
        }

        // Check if the use key (right click by default) is currently held down
        if (!mc.options.keyUse.isDown()) {
            return;
        }

        // Check if player is using items with action (e.g. shield blocking, bow, food)
        if (player.isUsingItem()) {
            ItemStack usingItem = player.getUseItem();
            UseAnim anim = usingItem.getUseAnimation();
            if (anim != UseAnim.NONE) {
                return;
            }
        }

        // Send breath packet to server for official projectile spawning
        ModMessages.sendToServer(new FireBreathPacket());

        // Spawn client-side visual flame/smoke particles from player eyes
        RandomSource random = player.level().getRandom();
        Vec3 forward = player.getLookAngle().normalize();
        double spawnX = player.getX() + forward.x * 0.6;
        double spawnY = player.getEyeY() - 0.15 + forward.y * 0.6;
        double spawnZ = player.getZ() + forward.z * 0.6;

        float spread = 0.15F;
        double dx = forward.x + (random.nextDouble() - 0.5) * spread;
        double dy = forward.y + (random.nextDouble() - 0.5) * spread;
        double dz = forward.z + (random.nextDouble() - 0.5) * spread;

        player.level().addParticle(ParticleTypes.FLAME, spawnX, spawnY, spawnZ, dx * 0.3, dy * 0.3, dz * 0.3);
        if (random.nextInt(3) == 0) {
            player.level().addParticle(ParticleTypes.SMOKE, spawnX, spawnY, spawnZ, dx * 0.15, dy * 0.15, dz * 0.15);
        }
    }
}
