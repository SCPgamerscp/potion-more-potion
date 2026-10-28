package com.ecrea.potionmorepotion.util;

/**
 * Plain Java regression checks for the synthetic-lightning scope.
 *
 * Run without Minecraft/Forge dependencies:
 * javac -d out src/main/java/com/ecrea/potionmorepotion/util/LightningDamageScope.java src/test/java/com/ecrea/potionmorepotion/util/LightningDamageScopeTest.java
 * java -cp out com.ecrea.potionmorepotion.util.LightningDamageScopeTest
 */
public final class LightningDamageScopeTest {
    private LightningDamageScopeTest() {}

    public static void main(String[] args) {
        check(!LightningDamageScope.isActive(), "natural lightning must not be suppressed");
        LightningDamageScope.runWithoutVanillaHitDamage(() -> {
            check(LightningDamageScope.isActive(), "synthetic thunderHit must be scoped");
            LightningDamageScope.runWithoutVanillaHitDamage(() ->
                    check(LightningDamageScope.isActive(), "nested scope must remain active"));
            check(LightningDamageScope.isActive(), "nested scope must restore outer state");
        });
        check(!LightningDamageScope.isActive(), "scope must be cleared after the hit");

        try {
            LightningDamageScope.runWithoutVanillaHitDamage(() -> {
                throw new IllegalStateException("simulated thunderHit failure");
            });
            throw new AssertionError("expected failure");
        } catch (IllegalStateException expected) {
            check(!LightningDamageScope.isActive(), "scope must clear on exception");
        }

        LightningDamageScope.runWithoutVanillaHitDamage(() -> {
            Thread other = new Thread(() ->
                    check(!LightningDamageScope.isActive(), "other threads must not inherit suppression"));
            other.start();
            try {
                other.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        });
        check(!LightningDamageScope.isActive(), "normal lightning must remain enabled afterward");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
