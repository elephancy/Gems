package com.feel.gems.power.ability.trickster;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.state.PlayerStateManager;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/**
 * Puppet Master - briefly control an enemy's movement for 3s.
 * For mobs: makes them attack their own allies.
 * For players: marks them as puppeted (movement control via runtime).
 */
public final class TricksterPuppetMasterAbility implements GemAbility {
    public static final String PUPPET_TARGET_KEY = "trickster_puppet_target";
    public static final String PUPPET_END_KEY = "trickster_puppet_end";

    @Override
    public Identifier id() {
        return PowerIds.TRICKSTER_PUPPET_MASTER;
    }

    @Override
    public String name() {
        return "Puppet Master";
    }

    @Override
    public String description() {
        return "Briefly control an enemy's movement for 3s.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().trickster().puppetMasterCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        int range = GemsBalance.v().trickster().puppetMasterRangeBlocks();
        int durationTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.TRICKSTER, GemsBalance.v().trickster().puppetMasterDurationTicks());

        // Raycast to find target (any living entity), fallback to nearest if raycast misses.
        LivingEntity target = Targeting.raycastLiving(player, range);
        if (target == null) {
            AABB searchBox = player.getBoundingBox().inflate(range);
            double bestDist = Double.MAX_VALUE;
            for (LivingEntity candidate : world.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != player && e.isAlive())) {
                if (candidate instanceof ServerPlayer other && !VoidImmunity.canBeTargeted(player, other)) {
                    continue;
                }
                double dist = player.distanceToSqr(candidate);
                if (dist < bestDist) {
                    bestDist = dist;
                    target = candidate;
                }
            }
        }
        if (target == null) {
            AbilityFeedback.sound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        // Handle mob targets - make them attack their allies
        if (target instanceof Mob mob) {
            AABB searchBox = mob.getBoundingBox().inflate(16);

            // Prefer targeting other mobs so the effect looks like "turning on allies".
            List<Mob> nearbyMobs = world.getEntitiesOfClass(
                    Mob.class,
                    searchBox,
                    e -> e != mob && e.isAlive()
            );

            if (!nearbyMobs.isEmpty()) {
                Mob newTarget = nearbyMobs.get(world.getRandom().nextInt(nearbyMobs.size()));
                mob.setTarget(newTarget);
            } else {
                // Fallback: pick any other living entity so the ability still does something in a solo encounter.
                List<LivingEntity> nearbyLiving = world.getEntitiesOfClass(
                        LivingEntity.class,
                        searchBox,
                        e -> e != mob && e != player && e.isAlive()
                );
                mob.setTarget(nearbyLiving.isEmpty() ? null : nearbyLiving.get(world.getRandom().nextInt(nearbyLiving.size())));
            }

            TricksterPuppetRuntime.setPuppetedMob(mob, player.getUUID(), durationTicks);

            AbilityFeedback.burstAt(world, mob.position().add(0, 2, 0), ParticleTypes.ENCHANT, 25, 0.8D);
            AbilityFeedback.sound(player, SoundEvents.VEX_CHARGE, 1.0F, 0.6F);
            return true;
        }

        // Handle player targets
        if (target instanceof ServerPlayer targetPlayer) {
            if (!VoidImmunity.canBeTargeted(player, targetPlayer)) {
                AbilityFeedback.sound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
                return false;
            }
            long endTime = world.getGameTime() + durationTicks;
            PlayerStateManager.setPersistent(player, PUPPET_TARGET_KEY, targetPlayer.getStringUUID());
            PlayerStateManager.setPersistent(player, PUPPET_END_KEY, String.valueOf(endTime));

            // Mark target as being puppeted
            TricksterPuppetRuntime.setPuppeted(targetPlayer, player.getUUID(), durationTicks);

            AbilityFeedback.burstAt(world, targetPlayer.position().add(0, 2, 0), ParticleTypes.ENCHANT, 25, 0.8D);
            AbilityFeedback.sound(player, SoundEvents.VEX_CHARGE, 1.0F, 0.6F);
            AbilityFeedback.sound(targetPlayer, SoundEvents.VEX_CHARGE, 1.0F, 0.6F);
            return true;
        }

        AbilityFeedback.sound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
        return false;
    }

    public static ServerPlayer getPuppetTarget(ServerPlayer puppeteer) {
        String targetStr = PlayerStateManager.getPersistent(puppeteer, PUPPET_TARGET_KEY);
        if (targetStr == null || targetStr.isEmpty()) return null;

        String endStr = PlayerStateManager.getPersistent(puppeteer, PUPPET_END_KEY);
        if (endStr == null) return null;

        long endTime = Long.parseLong(endStr);
        if (puppeteer.level().getGameTime() > endTime) {
            clearPuppet(puppeteer);
            return null;
        }

        try {
            java.util.UUID targetId = java.util.UUID.fromString(targetStr);
            return puppeteer.level().getServer().getPlayerList().getPlayer(targetId);
        } catch (Exception e) {
            return null;
        }
    }

    public static void clearPuppet(ServerPlayer puppeteer) {
        PlayerStateManager.clearPersistent(puppeteer, PUPPET_TARGET_KEY);
        PlayerStateManager.clearPersistent(puppeteer, PUPPET_END_KEY);
    }
}
