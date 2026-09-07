package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import com.ecrea.potionmorepotion.network.ModMessages;
import com.ecrea.potionmorepotion.network.SnowballBreathPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side handler for the Snowball Blessing blizzard thrower.
 * Detects right-click (keyUse) hold every tick and sends a SnowballBreathPacket to the server.
 */
@Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID, value = Dist.CLIENT)
public class SnowballBlessingHandler {

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

        // Do not fire if any GUI/screen is open
        if (mc.screen != null) {
            return;
        }

        // Check if player has active snowball_blessing
        var effectObj = ModMobEffects.EFFECTS.get("snowball_blessing");
        if (effectObj == null || !player.hasEffect(effectObj.get())) {
            return;
        }

        // Check if use key (right click by default) is currently held down
        if (!mc.options.keyUse.isDown()) {
            return;
        }

        // Check if breath should be suppressed (block placement, chest/door/crafting table interactions, shield/bow/food)
        if (com.ecrea.potionmorepotion.util.BlessingInteractionHelper.shouldSuppressBreath(mc, player)) {
            return;
        }

        // Send breath packet to server
        ModMessages.sendToServer(new SnowballBreathPacket());

        // Spawn client-side visual snowflake / snowball particles
        RandomSource random = player.level().getRandom();
        Vec3 forward = player.getLookAngle().normalize();
        double spawnX = player.getX() + forward.x * 0.6;
        double spawnY = player.getEyeY() - 0.15 + forward.y * 0.6;
        double spawnZ = player.getZ() + forward.z * 0.6;

        float spread = 0.20F;
        for (int i = 0; i < 2; i++) {
            double dx = forward.x + (random.nextDouble() - 0.5) * spread;
            double dy = forward.y + (random.nextDouble() - 0.5) * spread;
            double dz = forward.z + (random.nextDouble() - 0.5) * spread;
            player.level().addParticle(ParticleTypes.SNOWFLAKE, spawnX, spawnY, spawnZ, dx * 0.4, dy * 0.4, dz * 0.4);
        }

        if (random.nextBoolean()) {
            double dx = forward.x + (random.nextDouble() - 0.5) * spread;
            double dy = forward.y + (random.nextDouble() - 0.5) * spread;
            double dz = forward.z + (random.nextDouble() - 0.5) * spread;
            player.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SNOWBALL)),
                    spawnX, spawnY, spawnZ, dx * 0.2, dy * 0.2, dz * 0.2);
        }
    }
}
