package com.ecrea.potionmorepotion.util;

/**
 * Narrow guard for Lightning Blessing's explicit LivingEntity.thunderHit() calls.
 *
 * Vanilla thunderHit() performs mob conversions as well as dealing an
 * unattributed lightning hit. LightningAttackPacket separately applies a
 * player-credited lightning hit, so only that duplicate vanilla hit needs
 * cancellation. Ordinary weather/command lightning must remain unaffected.
 *
 * Scoping to the server thread and restoring the previous state in finally
 * prevents one spell cast from suppressing unrelated strikes later on.
 */
public final class LightningDamageScope {
    private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<>();

    private LightningDamageScope() {}

    public static boolean isActive() {
        return DEPTH.get() != null;
    }

    public static void runWithoutVanillaHitDamage(Runnable action) {
        Integer previous = DEPTH.get();
        DEPTH.set(previous == null ? 1 : previous + 1);
        try {
            action.run();
        } finally {
            if (previous == null) {
                DEPTH.remove();
            } else {
                DEPTH.set(previous);
            }
        }
    }
}
