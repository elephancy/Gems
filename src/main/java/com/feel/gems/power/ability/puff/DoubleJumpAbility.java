package com.feel.gems.power.ability.puff;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;



public final class DoubleJumpAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.DOUBLE_JUMP;
    }

    @Override
    public String name() {
        return "Double Jump";
    }

    @Override
    public String description() {
        return "Launches you upward; usable midair.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().puff().doubleJumpCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (isGrounded(player)) {
            player.sendSystemMessage(Component.translatable("gems.ability.puff.double_jump.jump_first"), true);
            return false;
        }
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x, GemsBalance.v().puff().doubleJumpVelocityY(), v.z);
        player.needsSync = true;
        AbilityFeedback.syncVelocity(player);
        AbilityFeedback.burst(player, ParticleTypes.CLOUD, 12, 0.2D);
        AbilityFeedback.sound(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.7F, 1.5F);
        return true;
    }

    private static boolean isGrounded(ServerPlayer player) {
        // GameTests (and some teleports) can leave `isOnGround()` stale; rely on an under-foot raycast so:
        // - standing on a block => grounded, even if `isOnGround()` is false
        // - teleported into midair => not grounded, even if `isOnGround()` is true
        var world = player.level();
        Vec3 start = player.position().add(0.0D, 0.05D, 0.0D);
        Vec3 end = start.add(0.0D, -0.6D, 0.0D);
        HitResult hit = world.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK;
    }
}
