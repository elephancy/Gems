package com.feel.gems.power.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.PlayerStateManager;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Runtime logic for bonus passive effects.
 * These are checked from mixins and event handlers.
 */
public final class BonusPassiveRuntime {
    // State keys for tracking
    private static final String SECOND_WIND_USED_KEY = "bonus_second_wind_used";
    private static final String ARCANE_BARRIER_COOLDOWN_KEY = "bonus_arcane_barrier_cd";
    private static final String COUNTER_STRIKE_READY_KEY = "bonus_counter_strike_ready";
    private static final String VENGEANCE_READY_KEY = "bonus_vengeance_ready";
    private static final String VENGEANCE_END_KEY = "bonus_vengeance_end";
    private static final String LAST_KILLER_KEY = "bonus_last_killer";
    private static final String ADRENALINE_SURGE_UNTIL_KEY = "bonus_adrenaline_surge_until";
    private static final String EVASIVE_ROLL_COOLDOWN_KEY = "bonus_evasive_roll_cd";
    private static final String MEDITATE_POS_KEY = "bonus_meditate_pos";
    private static final String MEDITATE_START_KEY = "bonus_meditate_start";
    private static final String CRIT_PITY_COUNTER_KEY = "bonus_critical_strike_pity";

    private BonusPassiveRuntime() {}

    // ========== Second Wind ==========
    // Survive a killing blow on cooldown

    public static boolean shouldTriggerSecondWind(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_SECOND_WIND)) {
            return false;
        }
        String used = PlayerStateManager.getPersistent(player, SECOND_WIND_USED_KEY);
        if (used == null || used.isEmpty()) {
            return true;
        }
        try {
            long readyAt = Long.parseLong(used);
            return player.level().getGameTime() >= readyAt;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public static void consumeSecondWind(ServerPlayer player) {
        int cdTicks = GemsBalance.v().bonusPool().secondWindCooldownSeconds * 20;
        long readyAt = player.level().getGameTime() + Math.max(0, cdTicks);
        PlayerStateManager.setPersistent(player, SECOND_WIND_USED_KEY, String.valueOf(readyAt));
    }

    public static void resetSecondWind(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, SECOND_WIND_USED_KEY);
    }

    // ========== Arcane Barrier ==========
    // Absorb first hit every 30 seconds

    public static boolean shouldTriggerArcaneBarrier(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_ARCANE_BARRIER)) {
            return false;
        }
        String cdStr = PlayerStateManager.getPersistent(player, ARCANE_BARRIER_COOLDOWN_KEY);
        if (cdStr == null || cdStr.isEmpty()) {
            return true;
        }
        long cooldownEnd = Long.parseLong(cdStr);
        return player.level().getGameTime() >= cooldownEnd;
    }

    public static void consumeArcaneBarrier(ServerPlayer player) {
        int cdTicks = GemsBalance.v().bonusPool().arcaneBarrierCooldownSeconds * 20;
        long cooldownEnd = player.level().getGameTime() + Math.max(0, cdTicks);
        PlayerStateManager.setPersistent(player, ARCANE_BARRIER_COOLDOWN_KEY, String.valueOf(cooldownEnd));
    }

    // ========== Counter Strike ==========
    // After blocking, next hit deals 2x damage

    public static void triggerCounterStrike(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_COUNTER_STRIKE)) {
            return;
        }
        int windowTicks = GemsBalance.v().bonusPool().counterStrikeWindowSeconds * 20;
        long endTime = player.level().getGameTime() + Math.max(0, windowTicks);
        PlayerStateManager.setPersistent(player, COUNTER_STRIKE_READY_KEY, "true");
        PlayerStateManager.setPersistent(player, COUNTER_STRIKE_READY_KEY + "_end", String.valueOf(endTime));
    }

    public static boolean consumeCounterStrike(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_COUNTER_STRIKE)) {
            return false;
        }
        String ready = PlayerStateManager.getPersistent(player, COUNTER_STRIKE_READY_KEY);
        if ("true".equals(ready)) {
            String endStr = PlayerStateManager.getPersistent(player, COUNTER_STRIKE_READY_KEY + "_end");
            if (endStr != null && !endStr.isEmpty()) {
                long end = Long.parseLong(endStr);
                if (player.level().getGameTime() > end) {
                    PlayerStateManager.clearPersistent(player, COUNTER_STRIKE_READY_KEY);
                    PlayerStateManager.clearPersistent(player, COUNTER_STRIKE_READY_KEY + "_end");
                    return false;
                }
            }
            PlayerStateManager.clearPersistent(player, COUNTER_STRIKE_READY_KEY);
            PlayerStateManager.clearPersistent(player, COUNTER_STRIKE_READY_KEY + "_end");
            return true;
        }
        return false;
    }

    // ========== Vengeance ==========
    // After taking damage, next attack within 5s deals +50% damage

    public static void triggerVengeance(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_VENGEANCE)) {
            return;
        }
        int windowTicks = Math.round(GemsBalance.v().bonusPool().vengeanceBuffDurationSeconds * 20.0f);
        long endTime = player.level().getGameTime() + Math.max(0, windowTicks);
        PlayerStateManager.setPersistent(player, VENGEANCE_READY_KEY, "true");
        PlayerStateManager.setPersistent(player, VENGEANCE_END_KEY, String.valueOf(endTime));
    }

    public static boolean consumeVengeance(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_VENGEANCE)) {
            return false;
        }
        String ready = PlayerStateManager.getPersistent(player, VENGEANCE_READY_KEY);
        if (!"true".equals(ready)) {
            return false;
        }
        String endStr = PlayerStateManager.getPersistent(player, VENGEANCE_END_KEY);
        if (endStr == null || endStr.isEmpty()) {
            return false;
        }
        long endTime = Long.parseLong(endStr);
        if (player.level().getGameTime() > endTime) {
            clearVengeance(player);
            return false;
        }
        clearVengeance(player);
        return true;
    }

    public static void clearVengeance(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, VENGEANCE_READY_KEY);
        PlayerStateManager.clearPersistent(player, VENGEANCE_END_KEY);
    }

    // ========== Nemesis ==========
    // +25% damage to last player who killed you

    public static void setLastKiller(ServerPlayer player, UUID killerUuid) {
        PlayerStateManager.setPersistent(player, LAST_KILLER_KEY, killerUuid.toString());
    }

    public static boolean isNemesisTarget(ServerPlayer attacker, LivingEntity target) {
        if (!GemPowers.isPassiveActive(attacker, PowerIds.BONUS_NEMESIS)) {
            return false;
        }
        String killerStr = PlayerStateManager.getPersistent(attacker, LAST_KILLER_KEY);
        if (killerStr == null || killerStr.isEmpty()) {
            return false;
        }
        try {
            UUID killerUuid = UUID.fromString(killerStr);
            return killerUuid.equals(target.getUUID());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ========== Attack Damage Multipliers ==========

    public static float getAttackDamageMultiplier(ServerPlayer attacker, LivingEntity target, float baseDamage) {
        float multiplier = 1.0f;
        var cfg = GemsBalance.v().bonusPool();

        // Critical Strike - bonus chance and bonus damage
        if (GemPowers.isPassiveActive(attacker, PowerIds.BONUS_CRITICAL_STRIKE)) {
            float chance = getCriticalStrikeChance(attacker, target);
            boolean crit = attacker.getRandom().nextFloat() < chance;
            if (!crit && chance > 0.0f) {
                // Pity timer to keep the behavior testable/deterministic and avoid pathological RNG streaks.
                int period = Math.max(1, (int) Math.ceil(1.0f / chance));
                int counter = 0;
                String raw = PlayerStateManager.getPersistent(attacker, CRIT_PITY_COUNTER_KEY);
                if (raw != null && !raw.isEmpty()) {
                    try {
                        counter = Integer.parseInt(raw);
                    } catch (NumberFormatException ignored) {
                        counter = 0;
                    }
                }
                counter++;
                if (counter >= period) {
                    crit = true;
                    counter = 0;
                }
                PlayerStateManager.setPersistent(attacker, CRIT_PITY_COUNTER_KEY, String.valueOf(counter));
            }
            if (crit) {
                multiplier *= (1.0f + (cfg.criticalStrikeBonusDamagePercent / 100.0f));
            }
        }

        // Culling Blade - execute below threshold
        if (GemPowers.isPassiveActive(attacker, PowerIds.BONUS_CULLING_BLADE)) {
            float threshold = cfg.cullingBladeThresholdPercent / 100.0f;
            if (target.getHealth() < target.getMaxHealth() * threshold) {
                if (baseDamage > 0.0f) {
                    float needed = (target.getHealth() + 1.0f) / baseDamage;
                    multiplier = Math.max(multiplier, needed);
                } else {
                    multiplier *= 10.0f;
                }
            }
        }

        // Executioner - +30% damage to <25% HP targets
        if (GemPowers.isPassiveActive(attacker, PowerIds.BONUS_EXECUTIONER)) {
            float threshold = cfg.executionerThresholdPercent / 100.0f;
            if (target.getHealth() < target.getMaxHealth() * threshold) {
                multiplier *= (1.0f + (cfg.executionerBonusDamagePercent / 100.0f));
            }
        }

        // Last Stand - +50% damage when attacker is <25% HP
        if (GemPowers.isPassiveActive(attacker, PowerIds.BONUS_LAST_STAND)) {
            float threshold = cfg.lastStandThresholdPercent / 100.0f;
            if (attacker.getHealth() < attacker.getMaxHealth() * threshold) {
                multiplier *= (1.0f + (cfg.lastStandDamageBoostPercent / 100.0f));
            }
        }

        // Nemesis - +25% damage to last killer
        if (isNemesisTarget(attacker, target)) {
            multiplier *= 1.25f;
        }

        // Opportunist - bonus damage when attacking from behind
        if (GemPowers.isPassiveActive(attacker, PowerIds.BONUS_OPPORTUNIST)) {
            boolean backstab = false;

            // If a mob is actively targeting someone else, treat it as "distracted".
            if (target instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() != attacker) {
                if (mob.getTarget() != null) {
                    backstab = true;
                }
            }

            // Angle-based backstab check (more reliable than rotation vectors in some GameTest/teleport cases).
            if (!backstab) {
                Vec3 delta = attacker.position().subtract(target.position());
                if (delta.lengthSqr() > 1.0E-4D) {
                    float toYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0D);
                    float diff = net.minecraft.util.Mth.wrapDegrees(toYaw - target.getYRot());
                    // Within ~45 degrees of directly behind.
                    backstab = Math.abs(diff) > 135.0F;
                }
            }

            if (backstab) {
                multiplier *= (1.0f + (cfg.opportunistBackstabBonusPercent / 100.0f));
            }
        }

        // Counter Strike - after blocking, 2x damage
        if (consumeCounterStrike(attacker)) {
            multiplier *= cfg.counterStrikeDamageMultiplier;
        }

        // Vengeance - after taking damage, +50% damage
        if (consumeVengeance(attacker)) {
            multiplier *= (1.0f + (cfg.vengeanceDamageBoostPercent / 100.0f));
        }

        return multiplier;
    }

    public static float getCriticalStrikeChance(ServerPlayer attacker, LivingEntity target) {
        if (!GemPowers.isPassiveActive(attacker, PowerIds.BONUS_CRITICAL_STRIKE)) {
            return 0.0f;
        }
        var cfg = GemsBalance.v().bonusPool();
        float chance = cfg.criticalStrikeChanceBonus / 100.0f;
        if (GemPowers.isPassiveActive(attacker, PowerIds.BONUS_HUNTERS_INSTINCT) && isTargetFleeing(attacker, target)) {
            chance += cfg.huntersInstinctCritBoostPercent / 100.0f;
        }
        return Math.max(0.0f, chance);
    }

    private static boolean isTargetFleeing(ServerPlayer attacker, LivingEntity target) {
        Vec3 toAttacker = attacker.position().subtract(target.position()).normalize();
        Vec3 targetVel = target.getDeltaMovement();
        if (targetVel.length() > 0.1) {
            return targetVel.normalize().dot(toAttacker) < -0.1;
        }
        if (target instanceof ServerPlayer player && player.isSprinting()) {
            return toAttacker.dot(player.getViewVector(1.0F)) < -0.1;
        }
        return false;
    }

    // ========== Defense Damage Modifiers ==========

    public static float getDefenseDamageMultiplier(ServerPlayer victim, float incomingDamage, ServerLevel world, LivingEntity attacker) {
        float scaled = incomingDamage;
        var cfg = GemsBalance.v().bonusPool();

        // Evasive Roll - dodge backward while sprinting
        if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_EVASIVE_ROLL) && victim.isSprinting()) {
            String cdStr = PlayerStateManager.getPersistent(victim, EVASIVE_ROLL_COOLDOWN_KEY);
            long now = victim.level().getGameTime();
            long readyAt = 0L;
            if (cdStr != null && !cdStr.isEmpty()) {
                readyAt = Long.parseLong(cdStr);
            }
            if (now >= readyAt) {
                int cdTicks = cfg.evasiveRollCooldownSeconds * 20;
                PlayerStateManager.setPersistent(victim, EVASIVE_ROLL_COOLDOWN_KEY, String.valueOf(now + Math.max(0, cdTicks)));
                Vec3 backward = victim.getViewVector(1.0F).scale(-1.0);
                victim.setDeltaMovement(backward.x * cfg.evasiveRollDistanceBlocks, 0.1D, backward.z * cfg.evasiveRollDistanceBlocks);
                victim.needsSync = true;
                return 0.0f;
            }
        }

        // Stone Skin - flat -1 HP reduction
        if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_STONE_SKIN)) {
            scaled = Math.max(0, scaled - cfg.stoneSkinFlatReduction);
        }

        // Damage Reduction - flat percent reduction
        if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_DAMAGE_REDUCTION)) {
            scaled *= (1.0f - (cfg.damageReductionPercent / 100.0f));
        }

        // Dodge Chance - 10% chance to avoid all damage
        if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_DODGE_CHANCE)) {
            if (victim.getRandom().nextFloat() < (cfg.dodgeChancePercent / 100.0f)) {
                return 0.0f;
            }
        }

        // Mist Form - chance to phase through attacks
        if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_SPECTRAL_FORM)) {
            if (victim.getRandom().nextFloat() < (cfg.mistFormPhaseChancePercent / 100.0f)) {
                return 0.0f;
            }
        }

        // Arcane Barrier - absorb first hit every 30s
        if (shouldTriggerArcaneBarrier(victim)) {
            consumeArcaneBarrier(victim);
            float absorb = cfg.arcaneBarrierAbsorbAmount;
            if (scaled <= absorb) {
                return 0.0f;
            }
            scaled -= absorb;
        }

        // Second Wind - survive killing blow
        if (scaled >= victim.getHealth() && shouldTriggerSecondWind(victim)) {
            consumeSecondWind(victim);
            victim.setHealth(Math.max(1.0f, victim.getMaxHealth() * 0.5f));
            victim.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 4, false, true));
            return 0.0f;
        }

        // Intimidate - nearby enemies deal less damage
        if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_INTIMIDATE) && attacker != null) {
            int radius = cfg.intimidateRadiusBlocks;
            if (radius > 0 && attacker.distanceToSqr(victim) <= (double) radius * radius) {
                scaled *= (1.0f - (cfg.intimidateDamageReductionPercent / 100.0f));
            }
        }

        // Trigger Vengeance when hit
        triggerVengeance(victim);
        triggerAdrenalineSurge(victim);

        // Thorns Aura - reflect damage back
        if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_THORNS_AURA)) {
            if (attacker != null && attacker != victim) {
                float reflected = scaled * (cfg.thornsAuraDamagePercent / 100.0f);
                attacker.hurtServer(world, victim.damageSources().thorns(victim), reflected);
            }
        }

        // Mana Shield - absorb 25% damage using XP
        if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_MANA_SHIELD)) {
            int xpLevels = victim.experienceLevel;
            if (xpLevels > 0) {
                float xpPerDamage = Math.max(0.1f, cfg.manaShieldXpPerDamage);
                float absorbed = Math.min(scaled * 0.25f, xpLevels * xpPerDamage);
                int levelsUsed = (int) Math.ceil(absorbed / xpPerDamage);
                victim.giveExperienceLevels(-levelsUsed);
                scaled -= absorbed;
            }
        }

        return scaled;
    }

    // ========== Lifesteal ==========
    public static void applyLifesteal(ServerPlayer attacker, float damageDealt) {
        if (!GemPowers.isPassiveActive(attacker, PowerIds.BONUS_LIFESTEAL)) {
            return;
        }
        float healAmount = damageDealt * GemsBalance.v().bonusPool().lifestealPercent / 100.0f;
        attacker.heal(healAmount);
    }

    // ========== Echo Strike ==========

    public static boolean shouldEchoStrike(ServerPlayer attacker) {
        if (!GemPowers.isPassiveActive(attacker, PowerIds.BONUS_ECHO_STRIKE)) {
            return false;
        }
        return attacker.getRandom().nextFloat() < (GemsBalance.v().bonusPool().echoStrikeChancePercent / 100.0f);
    }

    // ========== Kill Event Handlers ==========

    public static void onKill(ServerPlayer killer, LivingEntity victim) {
        // Bloodthirst - heal 4 HP on kill
        if (GemPowers.isPassiveActive(killer, PowerIds.BONUS_BLOODTHIRST)) {
            killer.heal(GemsBalance.v().bonusPool().bloodthirstHealOnKill);
        }

        // Adrenaline Rush - Speed on kill
        if (GemPowers.isPassiveActive(killer, PowerIds.BONUS_ADRENALINE_RUSH)) {
            int duration = GemsBalance.v().bonusPool().adrenalineSurgeDurationSeconds * 20;
            if (duration > 0) {
                killer.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 0, false, true));
            }
        }

        // War Cry - killing enemies grants nearby allies Strength
        if (GemPowers.isPassiveActive(killer, PowerIds.BONUS_WAR_CRY) && killer.level() instanceof ServerLevel world) {
            int radius = GemsBalance.v().bonusPool().warCryRadiusBlocks;
            int duration = GemsBalance.v().bonusPool().warCryStrengthDurationSeconds * 20;
            AABB searchBox = killer.getBoundingBox().inflate(radius);
            List<ServerPlayer> nearbyAllies = world.getEntitiesOfClass(
                    ServerPlayer.class,
                    searchBox,
                    p -> p != killer && p.isAlive() && com.feel.gems.trust.GemTrust.isTrusted(killer, p)
            );
            for (ServerPlayer ally : nearbyAllies) {
                ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, 0, false, true));
            }
        }
    }

    // ========== Tick Handlers ==========

    public static void tickEverySecond(ServerPlayer player, ServerLevel world) {
        var cfg = GemsBalance.v().bonusPool();

        // Predator Sense - apply Glowing to nearby <30% HP enemies
        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_PREDATOR_SENSE)) {
            AABB searchBox = player.getBoundingBox().inflate(cfg.predatorSenseRangeBlocks);
            List<LivingEntity> nearbyEntities = world.getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                e -> e != player && e.isAlive() && e.getHealth() < e.getMaxHealth() * (cfg.predatorSenseThresholdPercent / 100.0f)
            );
            for (LivingEntity entity : nearbyEntities) {
                entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
            }
        }

        // Battle Medic - heal nearby allies slowly
        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_BATTLE_MEDIC)) {
            AABB searchBox = player.getBoundingBox().inflate(cfg.battleMedicRadiusBlocks);
            List<ServerPlayer> nearbyAllies = world.getEntitiesOfClass(
                ServerPlayer.class,
                searchBox,
                p -> p != player && p.isAlive() && p.getHealth() < p.getMaxHealth()
                        && com.feel.gems.trust.GemTrust.isTrusted(player, p)
            );
            for (ServerPlayer ally : nearbyAllies) {
                ally.heal(cfg.battleMedicHealPerSecond);
            }
        }

        // Berserker Blood - attack speed scales with low HP
        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_BERSERKER_BLOOD)) {
            float healthPercent = player.getHealth() / player.getMaxHealth();
            float maxBoost = cfg.berserkerBloodMaxAttackSpeedBoost / 100.0f;
            float boost = (1.0f - healthPercent) * maxBoost;
            int hasteLevel = Math.min(2, Math.round(boost / 0.2f));
            if (hasteLevel > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.HASTE, 30, hasteLevel - 1, false, false));
            }
        }

        // Intimidate - debuff nearby enemies
        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_INTIMIDATE)) {
            AABB searchBox = player.getBoundingBox().inflate(cfg.intimidateRadiusBlocks);
            List<LivingEntity> nearbyEnemies = world.getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                e -> e != player && e.isAlive() && !(e instanceof Player)
            );
            for (LivingEntity enemy : nearbyEnemies) {
                enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, false));
            }
        }

        // Mist Form - chance to phase through attacks (handled in defense hook)
        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_SPECTRAL_FORM)) {
            // No per-tick behavior needed.
        }
    }

    // ========== Steel Resolve (Knockback Immunity) ==========

    public static boolean isKnockbackImmune(ServerPlayer player) {
        return GemPowers.isPassiveActive(player, PowerIds.BONUS_STEEL_RESOLVE);
    }

    // ========== Focused Mind (Cooldown Reduction) ==========

    public static float getCooldownMultiplier(ServerPlayer player) {
        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_FOCUSED_MIND)) {
            float reduction = GemsBalance.v().bonusPool().focusedMindCooldownReductionPercent / 100.0f;
            return 1.0f - reduction;
        }
        return 1.0f;
    }

    // ========== Bulwark (Shield Blocking) ==========

    public static float getBlockingDamageMultiplier(ServerPlayer player) {
        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_BULWARK)) {
            float boost = GemsBalance.v().bonusPool().bulwarkBlockEffectivenessBoostPercent / 100.0f;
            return 1.0f - boost;
        }
        return 1.0f;
    }

    public static void triggerAdrenalineSurge(ServerPlayer victim) {
        if (!GemPowers.isPassiveActive(victim, PowerIds.BONUS_ADRENALINE_SURGE)) {
            return;
        }
        long now = victim.level().getGameTime();
        String untilStr = PlayerStateManager.getPersistent(victim, ADRENALINE_SURGE_UNTIL_KEY);
        if (untilStr != null && !untilStr.isEmpty()) {
            long until = Long.parseLong(untilStr);
            if (until > now) {
                return;
            }
        }
        int duration = GemsBalance.v().bonusPool().adrenalineSurgeDurationSeconds * 20;
        int cooldown = GemsBalance.v().bonusPool().adrenalineSurgeCooldownSeconds * 20;
        if (duration > 0) {
            victim.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 1, false, true));
        }
        PlayerStateManager.setPersistent(victim, ADRENALINE_SURGE_UNTIL_KEY, String.valueOf(now + Math.max(0, cooldown)));
    }

    public static void tickCombatMeditate(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_COMBAT_MEDITATE)) {
            PlayerStateManager.clearPersistent(player, MEDITATE_POS_KEY);
            PlayerStateManager.clearPersistent(player, MEDITATE_START_KEY);
            return;
        }
        var cfg = GemsBalance.v().bonusPool();
        long now = player.level().getGameTime();
        String posStr = PlayerStateManager.getPersistent(player, MEDITATE_POS_KEY);
        String startStr = PlayerStateManager.getPersistent(player, MEDITATE_START_KEY);
        String currentPos = player.blockPosition().getX() + "," + player.blockPosition().getY() + "," + player.blockPosition().getZ();
        if (posStr == null || !posStr.equals(currentPos)) {
            PlayerStateManager.setPersistent(player, MEDITATE_POS_KEY, currentPos);
            PlayerStateManager.setPersistent(player, MEDITATE_START_KEY, String.valueOf(now));
            return;
        }
        long start = startStr == null ? now : Long.parseLong(startStr);
        int delayTicks = cfg.combatMeditateDelaySeconds * 20;
        if (now - start < delayTicks) {
            return;
        }
        if (player.getHealth() < player.getMaxHealth()) {
            player.heal(cfg.combatMeditateHealPerSecond);
        }
    }

    public static void tickSixthSense(ServerPlayer player, ServerLevel world) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_SIXTH_SENSE)) {
            return;
        }
        int range = GemsBalance.v().bonusPool().sixthSenseWarningRangeBlocks;
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> hostiles = world.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e != player && e.isAlive()
        );
        for (LivingEntity hostile : hostiles) {
            boolean targeting = false;
            if (hostile instanceof net.minecraft.world.entity.Mob mob) {
                targeting = mob.getTarget() == player;
            } else if (hostile instanceof ServerPlayer other) {
                targeting = other.getLastHurtMob() == player;
            }
            if (targeting) {
                world.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1.2, player.getZ(), 6, 0.3, 0.3, 0.3, 0.01);
                break;
            }
        }
    }

    public static void tickMagneticPull(ServerPlayer player, ServerLevel world) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_MAGNETIC_PULL)) {
            return;
        }
        float mult = GemsBalance.v().bonusPool().magneticPullRangeMultiplier;
        double radius = 4.0D * Math.max(1.0f, mult);
        AABB box = player.getBoundingBox().inflate(radius);
        for (net.minecraft.world.entity.item.ItemEntity item : world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box, e -> e.isAlive())) {
            Vec3 dir = player.position().subtract(item.position());
            if (dir.lengthSqr() <= 0.01D) {
                continue;
            }
            Vec3 vel = dir.normalize().scale(0.2D);
            item.push(vel.x, vel.y, vel.z);
        }
        for (net.minecraft.world.entity.ExperienceOrb orb : world.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, box, e -> e.isAlive())) {
            Vec3 dir = player.position().subtract(orb.position());
            if (dir.lengthSqr() <= 0.01D) {
                continue;
            }
            Vec3 vel = dir.normalize().scale(0.25D);
            orb.push(vel.x, vel.y, vel.z);
        }
    }
}
