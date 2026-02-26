package com.feel.gems.power.ability.pillager;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;



public final class PillagerWarhornAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.PILLAGER_WARHORN;
    }

    @Override
    public String name() {
        return "Warhorn";
    }

    @Override
    public String description() {
        return "Buff nearby allies and slow enemies with a rallying horn blast.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().pillager().warhornCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        var cfg = GemsBalance.v().pillager();
        int radius = cfg.warhornRadiusBlocks();
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.PILLAGER, cfg.warhornDurationTicks());
        if (radius <= 0 || duration <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.pillager.warhorn.disabled"), true);
            return false;
        }
        ServerLevel world = player.level();
        AABB box = new AABB(player.position(), player.position()).inflate(radius);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);
        int allySpeed = cfg.warhornAllySpeedAmplifier();
        int allyResist = cfg.warhornAllyResistanceAmplifier();
        int enemySlow = cfg.warhornEnemySlownessAmplifier();
        int enemyWeak = cfg.warhornEnemyWeaknessAmplifier();

        for (LivingEntity living : targets) {
            if (living == player) {
                applyAlly(player, duration, allySpeed, allyResist);
                continue;
            }
            if (living instanceof ServerPlayer other && GemTrust.isTrusted(player, other)) {
                applyAlly(other, duration, allySpeed, allyResist);
                continue;
            }
            if (living instanceof ServerPlayer other && !VoidImmunity.canBeTargeted(player, other)) {
                continue;
            }
            applyEnemy(living, duration, enemySlow, enemyWeak);
        }

        AbilityFeedback.burst(player, ParticleTypes.CLOUD, 14, 0.35D);
        AbilityFeedback.sound(player, SoundEvents.RAID_HORN, 0.9F, 1.0F);
        player.sendSystemMessage(Component.translatable("gems.ability.pillager.warhorn.sounded"), true);
        return true;
    }

    private static void applyAlly(LivingEntity living, int duration, int speedAmp, int resistAmp) {
        if (speedAmp >= 0) {
            living.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, speedAmp, true, false, false));
        }
        if (resistAmp >= 0) {
            living.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, resistAmp, true, false, false));
        }
    }

    private static void applyEnemy(LivingEntity living, int duration, int slowAmp, int weakAmp) {
        if (slowAmp >= 0) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, slowAmp, true, false, false));
        }
        if (weakAmp >= 0) {
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, weakAmp, true, false, false));
        }
    }
}
