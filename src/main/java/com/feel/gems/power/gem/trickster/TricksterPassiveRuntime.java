package com.feel.gems.power.gem.trickster;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import java.util.Random;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileItem;

/**
 * Trickster passive runtime implementations.
 */
public final class TricksterPassiveRuntime {
    private static final Random RANDOM = new Random();

    private TricksterPassiveRuntime() {}

    // ========== Sleight of Hand ==========
    // 20% chance to not consume items when using throwables.

    public static boolean shouldNotConsumeThrowable(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.TRICKSTER_SLEIGHT_OF_HAND)) {
            return false;
        }
        float chance = GemsBalance.v().trickster().sleightOfHandChance();
        return RANDOM.nextFloat() < chance;
    }

    // ========== Chaos Agent ==========
    // Your abilities have randomized bonus effects (can be beneficial or detrimental).
    // This is handled per-ability when cast.

    public static boolean hasChaosAgent(ServerPlayer player) {
        return GemPowers.isPassiveActive(player, PowerIds.TRICKSTER_CHAOS_AGENT);
    }

    /**
     * Roll a random chaos effect. Returns a value between 0.5 and 1.5 as a damage/duration multiplier.
     */
    public static float rollChaosMultiplier() {
        return 0.5f + RANDOM.nextFloat(); // 0.5 to 1.5
    }

    /**
     * Roll whether to apply a bonus effect (50% chance for beneficial, 50% for detrimental).
     */
    public static ChaosEffect rollChaosEffect() {
        float roll = RANDOM.nextFloat();
        if (roll < 0.25f) return ChaosEffect.BENEFICIAL_SPEED;
        if (roll < 0.5f) return ChaosEffect.BENEFICIAL_STRENGTH;
        if (roll < 0.75f) return ChaosEffect.DETRIMENTAL_SLOW;
        return ChaosEffect.DETRIMENTAL_WEAKNESS;
    }

    public enum ChaosEffect {
        BENEFICIAL_SPEED,
        BENEFICIAL_STRENGTH,
        DETRIMENTAL_SLOW,
        DETRIMENTAL_WEAKNESS,
        NONE
    }

    // ========== Slippery ==========
    // 25% chance to ignore slowing effects.

    public static boolean shouldIgnoreSlow(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.TRICKSTER_SLIPPERY)) {
            return false;
        }
        float chance = GemsBalance.v().trickster().slipperyChance();
        return RANDOM.nextFloat() < chance;
    }

    /**
     * Check if the player has a slowing effect and should try to remove it.
     */
    public static void tryRemoveSlowEffects(ServerPlayer player) {
        if (shouldIgnoreSlow(player)) {
            player.removeEffect(MobEffects.SLOWNESS);
        }
    }

    public static void applyChaosEffect(ServerPlayer player) {
        if (!hasChaosAgent(player)) {
            return;
        }
        int duration = 100;
        ChaosEffect effect = rollChaosEffect();
        switch (effect) {
            case BENEFICIAL_SPEED -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.SPEED, duration, 1, false, false));
            case BENEFICIAL_STRENGTH -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.STRENGTH, duration, 0, false, false));
            case DETRIMENTAL_SLOW -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.SLOWNESS, duration, 0, false, false));
            case DETRIMENTAL_WEAKNESS -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, false));
            default -> {
            }
        }
    }

    public static boolean isThrowable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var item = stack.getItem();
        if (item instanceof ProjectileItem) {
            return true;
        }
        return item == Items.ENDER_PEARL || item == Items.EGG || item == Items.SNOWBALL
                || item == Items.EXPERIENCE_BOTTLE || item == Items.SPLASH_POTION
                || item == Items.LINGERING_POTION;
    }
}
