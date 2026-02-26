package com.feel.gems.power.ability.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class HunterCripplingShotAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HUNTER_CRIPPLING_SHOT;
    }

    @Override
    public String name() {
        return "Crippling Shot";
    }

    @Override
    public String description() {
        return "Ranged attack that reduces target's movement speed by 50% for 8s.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().hunter().cripplingCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        int range = GemsBalance.v().hunter().cripplingRangeBlocks();
        int durationTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.HUNTER, GemsBalance.v().hunter().cripplingDurationTicks());

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

        // Apply crippling effect (Slowness II = 30% slow, Slowness III = 45%, we use custom handling)
        // Using Slowness IV gives ~60% slow which is close to 50%
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, durationTicks, 3, false, true));

        // Visual
        AbilityFeedback.beam(world, player.position().add(0, 1.5, 0), target.position().add(0, 1, 0), ParticleTypes.CRIT, 20);
        AbilityFeedback.burstAt(world, target.position().add(0, 0.5, 0), ParticleTypes.DAMAGE_INDICATOR, 10, 0.3D);
        AbilityFeedback.sound(player, SoundEvents.ARROW_SHOOT, 1.0F, 0.8F);
        AbilityFeedback.sound(target, SoundEvents.ARROW_HIT_PLAYER, 1.0F, 0.6F);
        return true;
    }
}
