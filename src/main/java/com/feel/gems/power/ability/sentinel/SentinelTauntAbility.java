package com.feel.gems.power.ability.sentinel;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.PlayerStateManager;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

public final class SentinelTauntAbility implements GemAbility {
    public static final String TAUNT_ACTIVE_KEY = "sentinel_taunt_active";
    public static final String TAUNT_END_KEY = "sentinel_taunt_end";

    @Override
    public Identifier id() {
        return PowerIds.SENTINEL_TAUNT;
    }

    @Override
    public String name() {
        return "Taunt";
    }

    @Override
    public String description() {
        return "Force nearby enemies to target you for 5s; gain damage reduction during taunt.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().sentinel().tauntCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        int radius = GemsBalance.v().sentinel().tauntRadiusBlocks();
        int durationTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.SENTINEL, GemsBalance.v().sentinel().tauntDurationTicks());

        long endTime = world.getGameTime() + durationTicks;
        PlayerStateManager.setPersistent(player, TAUNT_ACTIVE_KEY, "true");
        PlayerStateManager.setPersistent(player, TAUNT_END_KEY, String.valueOf(endTime));

        // Apply taunt effect to nearby enemies.
        AABB box = player.getBoundingBox().inflate(radius);
        for (Entity e : world.getEntities(player, box, ent -> ent instanceof LivingEntity)) {
            LivingEntity target = (LivingEntity) e;
            if (target == player) {
                continue;
            }
            if (target instanceof ServerPlayer targetPlayer) {
                if (GemTrust.isTrusted(player, targetPlayer)) {
                    continue;
                }
                if (!VoidImmunity.canBeTargeted(player, targetPlayer)) {
                    continue;
                }
                // Mark them as taunted by this player.
                SentinelTauntRuntime.applyTaunt(targetPlayer, player.getUUID(), durationTicks);
                continue;
            }
            if (target instanceof Mob mob) {
                if (!(mob instanceof Monster) && !(mob instanceof NeutralMob)) {
                    continue;
                }
                mob.setTarget(player);
                mob.setLastHurtByMob(player);
                if (mob instanceof NeutralMob angerable) {
                    angerable.setPersistentAngerTarget(EntityReference.of(player));
                }
            }
        }

        // Grant resistance to self
        int resistanceAmplifier = (int) ((1.0F - GemsBalance.v().sentinel().tauntDamageReduction()) * 4); // Rough conversion
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, durationTicks, resistanceAmplifier, false, true));

        AbilityFeedback.ring(world, player.position().add(0, 1, 0), radius, ParticleTypes.ANGRY_VILLAGER, 24);
        AbilityFeedback.sound(player, SoundEvents.RAVAGER_ROAR, 1.0F, 1.2F);
        return true;
    }

    public static boolean isTaunting(ServerPlayer player) {
        String active = PlayerStateManager.getPersistent(player, TAUNT_ACTIVE_KEY);
        if (!"true".equals(active)) return false;

        String endStr = PlayerStateManager.getPersistent(player, TAUNT_END_KEY);
        if (endStr == null) return false;

        long endTime = Long.parseLong(endStr);
        return player.level().getGameTime() < endTime;
    }
}
