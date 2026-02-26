package com.feel.gems.power.ability.trickster;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.util.GemsTeleport;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Shadow Swap - instantly swap places with the target you're looking at.
 */
public final class TricksterShadowSwapAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TRICKSTER_SHADOW_SWAP;
    }

    @Override
    public String name() {
        return "Shadow Swap";
    }

    @Override
    public String description() {
        return "Instantly swap places with the entity you're looking at.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().trickster().shadowSwapCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();

        // Find target entity
        double range = GemsBalance.v().trickster().shadowSwapRangeBlocks();
        LivingEntity target = Targeting.raycastLiving(player, range);
        if (target == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gems.message.no_target_in_range").withStyle(net.minecraft.ChatFormatting.RED), true);
            return false;
        }

        Vec3 playerPos = player.position();
        Vec3 targetPos = target.position();
        float playerYaw = player.getYRot();
        float playerPitch = player.getXRot();

        // Teleport player to target's position
        GemsTeleport.teleport(player, world, targetPos.x, targetPos.y, targetPos.z, playerYaw, playerPitch);

        // Teleport target to player's old position
        if (target instanceof ServerPlayer targetPlayer) {
            if (!VoidImmunity.canBeTargeted(player, targetPlayer)) {
                // Teleport player back to original position since swap failed
                GemsTeleport.teleport(player, world, playerPos.x, playerPos.y, playerPos.z, playerYaw, playerPitch);
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gems.message.target_immune").withStyle(net.minecraft.ChatFormatting.RED), true);
                return false;
            }
            GemsTeleport.teleport(targetPlayer, world, playerPos.x, playerPos.y, playerPos.z, targetPlayer.getYRot(), targetPlayer.getXRot());
        } else {
            target.teleportTo(world, playerPos.x, playerPos.y, playerPos.z, java.util.Set.of(), target.getYRot(), target.getXRot(), false);
        }

        AbilityFeedback.burstAt(world, playerPos.add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 25, 0.6D);
        AbilityFeedback.burstAt(world, targetPos.add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 25, 0.6D);
        AbilityFeedback.sound(player, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.8F);
        return true;
    }
}
