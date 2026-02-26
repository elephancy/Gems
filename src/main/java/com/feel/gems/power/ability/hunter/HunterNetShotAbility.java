package com.feel.gems.power.ability.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.feel.gems.power.util.Targeting;

public final class HunterNetShotAbility implements GemAbility {
    private static final String KEY_NET_UNTIL = "hunterNetShotUntil";

    @Override
    public Identifier id() {
        return PowerIds.HUNTER_NET_SHOT;
    }

    @Override
    public String name() {
        return "Net Shot";
    }

    @Override
    public String description() {
        return "Fire a net that slows and grounds enemies (disables flight/elytra).";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().hunter().netShotCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        int range = GemsBalance.v().hunter().netShotRangeBlocks();
        int slowTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.HUNTER, GemsBalance.v().hunter().netShotSlowTicks());

        // Raycast to find target
        ServerPlayer target = Targeting.raycastPlayer(player, range);
        if (target == null) {
            AbilityFeedback.sound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        if (!VoidImmunity.canBeTargeted(player, target)) {
            AbilityFeedback.sound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        // Apply slowness effect
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slowTicks, 2, false, true));

        // Ground the target (cancel any upward velocity)
        target.setDeltaMovement(target.getDeltaMovement().x * 0.5, Math.min(target.getDeltaMovement().y, -0.5), target.getDeltaMovement().z * 0.5);
        target.needsSync = true;

        // Mark target as netted (disables elytra activation for duration)
        // The LivingEntityElytraNetMixin will block canGlide() to prevent elytra activation
        long endTime = world.getGameTime() + slowTicks;
        CompoundTag nbt = ((GemsPersistentDataHolder) target).gems$getPersistentData();
        nbt.putLong(KEY_NET_UNTIL, endTime);

        // Visual effect - net particles and radius ring showing net area
        double effectRadius = 2.0;
        AbilityFeedback.ring(world, target.position().add(0.0D, 0.15D, 0.0D), effectRadius, ParticleTypes.CLOUD, 16);
        AbilityFeedback.burstAt(world, target.position().add(0, 1, 0), ParticleTypes.CLOUD, 30, 0.8D);
        AbilityFeedback.sound(player, SoundEvents.FISHING_BOBBER_THROW, 1.0F, 0.6F);
        AbilityFeedback.sound(target, SoundEvents.COBWEB_PLACE, 1.0F, 1.0F);
        return true;
    }

    /**
     * Check if a player is currently netted (elytra disabled).
     */
    public static boolean isNetted(ServerPlayer player) {
        CompoundTag nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        long until = nbt.getLong(KEY_NET_UNTIL).orElse(0L);
        return until > 0 && player.level().getGameTime() < until;
    }
}
