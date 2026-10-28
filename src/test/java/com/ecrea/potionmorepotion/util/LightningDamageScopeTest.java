package com.ecrea.potionmorepotion.util;

import java.util.concurrent.atomic.AtomicReference;

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
            AtomicReference<Throwable> otherFailure = new AtomicReference<>();
            Thread other = new Thread(() -> {
                try {
                    check(!LightningDamageScope.isActive(), "other threads must not inherit suppression");
                } catch (Throwable failure) {
                    otherFailure.set(failure);
                }
            });
            other.start();
            try {
                other.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
            if (otherFailure.get() != null) {
                throw new AssertionError("thread isolation failed", otherFailure.get());
            }
        });
        check(!LightningDamageScope.isActive(), "normal lightning must remain enabled afterward");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
