package com.ecrea.potionmorepotion.event;

import com.ecrea.potionmorepotion.PotionMorePotionMod;
import com.ecrea.potionmorepotion.effect.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Handles the Blessing of Creative (creative_blessing):
 * 1. Temporarily switches player to CREATIVE mode (creative flight, instant block breaking, creative inventory).
 * 2. Allows player to take damage with 50% blessing reduction and regeneration (invulnerable = false).
 * 3. Enforces nearby hostile mobs (Enemy) to target and attack the player.
 * 4. Renders survival HUD (hearts and food level) on the client even while in creative mode.
 * 5. Safely restores the previous game mode upon effect expiration.
 */
public class CreativeBlessingHandler {

    public static final String NBT_PREV_GAMEMODE = "pmp_prev_gamemode";

    @Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID)
    public static class ServerEvents {

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
                return;
            }

            if (!(event.player instanceof ServerPlayer player)) {
                return;
            }

            var creativeObj = ModMobEffects.EFFECTS.get("creative_blessing");
            boolean hasEffect = creativeObj != null && player.hasEffect(creativeObj.get());

            if (hasEffect) {
                // 1. Transition to CREATIVE mode if not already
                if (!player.gameMode.isCreative()) {
                    player.getPersistentData().putInt(NBT_PREV_GAMEMODE, player.gameMode.getGameModeForPlayer().getId());
                    player.setGameMode(GameType.CREATIVE);
                }

                // Ensure full creative invulnerability is active
                if (!player.getAbilities().invulnerable) {
                    player.getAbilities().invulnerable = true;
                    player.onUpdateAbilities();
                }

                // 2. Force nearby hostile mobs to target the player
                if (player.tickCount % 5 == 0) {
                    AABB searchBox = player.getBoundingBox().inflate(32.0D);
                    List<Mob> nearbyHostiles = player.serverLevel().getEntitiesOfClass(Mob.class, searchBox,
                            mob -> mob instanceof Enemy && mob.isAlive());
                    for (Mob mob : nearbyHostiles) {
                        if (mob.getTarget() != player) {
                            mob.setTarget(player);
                        }
                    }
                }
            } else {
                // 3. Restore previous game mode when effect ends
                if (player.getPersistentData().contains(NBT_PREV_GAMEMODE)) {
                    int prevId = player.getPersistentData().getInt(NBT_PREV_GAMEMODE);
                    GameType prevMode = GameType.byId(prevId);
                    player.getPersistentData().remove(NBT_PREV_GAMEMODE);

                    if (player.gameMode.isCreative()) {
                        player.setGameMode(prevMode);
                        player.fallDistance = 0.0F;
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
            // Prevent hostile mobs from clearing their target if their target has creative blessing
            if (event.getNewTarget() == null && event.getOriginalTarget() instanceof Player player) {
                var creativeObj = ModMobEffects.EFFECTS.get("creative_blessing");
                if (creativeObj != null && player.hasEffect(creativeObj.get())) {
                    event.setNewTarget(player);
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = PotionMorePotionMod.MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {

        /**
         * Renders the survival health and food bar above the hotbar even when in creative mode.
         */
        @SubscribeEvent
        public static void onRenderGuiOverlayPost(RenderGuiOverlayEvent.Post event) {
            if (!VanillaGuiOverlay.HOTBAR.type().equals(event.getOverlay())) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || mc.options.hideGui) {
                return;
            }

            var creativeObj = ModMobEffects.EFFECTS.get("creative_blessing");
            if (creativeObj == null || !player.hasEffect(creativeObj.get())) {
                return;
            }

            if (mc.gui instanceof ForgeGui forgeGui && !forgeGui.shouldDrawSurvivalElements()) {
                int width = event.getWindow().getGuiScaledWidth();
                int height = event.getWindow().getGuiScaledHeight();

                forgeGui.setupOverlayRenderState(true, false);
                forgeGui.renderExperienceBar(event.getGuiGraphics(), width / 2 - 91);
                forgeGui.renderHealth(width, height, event.getGuiGraphics());
                forgeGui.renderFood(width, height, event.getGuiGraphics());
            }
        }
    }
}
