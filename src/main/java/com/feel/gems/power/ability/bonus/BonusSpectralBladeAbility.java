package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.astra.SoulSummons;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTime;
import com.feel.gems.legendary.HypnoControl;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.AABB;

/**
 * Spectral Blade - Summon a ghostly sword (vex) that attacks nearby enemies.
 */
public final class BonusSpectralBladeAbility implements GemAbility {
    private static final int BLADE_LIFETIME = 200; // 10 seconds
    private static final double TARGET_RANGE = 16.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_SPECTRAL_BLADE;
    }

    @Override
    public String name() {
        return "Spectral Blade";
    }

    @Override
    public String description() {
        return "Summon a spectral blade that attacks nearby enemies for 10s.";
    }

    @Override
    public int cooldownTicks() {
        return 400; // 20 seconds
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();

        // Spawn a vex as the spectral blade
        Vex blade = EntityTypes.VEX.create(world, EntitySpawnReason.MOB_SUMMONED);
        if (blade == null) {
            return false;
        }

        blade.setPos(player.getX(), player.getY() + 1, player.getZ());
        blade.setOwner(null); // No evoker owner
        blade.setLimitedLife(BLADE_LIFETIME);
        blade.addTag("gems_spectral_blade");
        blade.addTag("owner:" + player.getStringUUID());
        SummonerSummons.tuneControlledMob(blade);
        SummonerSummons.mark(blade, player.getUUID(), GemsTime.now(player) + BLADE_LIFETIME);

        // Find a valid target - hostile mob or enemy player
        LivingEntity target = findTarget(player, world);
        if (target != null) {
            blade.setTarget(target);
        }

        world.addFreshEntity(blade);

        world.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(),
                20, 0.5, 0.5, 0.5, 0.1);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.VEX_CHARGE, SoundSource.PLAYERS, 1.0f, 1.5f);
        return true;
    }

    /**
     * Find a valid target for the spectral blade - prioritize players, then hostile mobs.
     */
    private static LivingEntity findTarget(ServerPlayer owner, ServerLevel world) {
        AABB searchBox = owner.getBoundingBox().inflate(TARGET_RANGE);

        LivingEntity threat = normalizeTarget(owner, owner.getLastHurtByMob());
        if (threat != null) {
            return threat;
        }
        LivingEntity nearest = null;
        double nearestSq = Double.MAX_VALUE;
        for (Mob mob : world.getEntitiesOfClass(Mob.class, searchBox, m -> m.getTarget() == owner)) {
            LivingEntity candidate = normalizeTarget(owner, mob);
            if (candidate == null) {
                continue;
            }
            double distSq = owner.distanceToSqr(candidate);
            if (distSq < nearestSq) {
                nearestSq = distSq;
                nearest = candidate;
            }
        }
        if (nearest != null) {
            return nearest;
        }

        // Then look for enemy players
        for (Entity e : world.getEntities(owner, searchBox)) {
            if (e instanceof ServerPlayer target) {
                LivingEntity candidate = normalizeTarget(owner, target);
                if (candidate != null) {
                    return candidate;
                }
            }
        }

        // Then look for hostile mobs
        for (Entity e : world.getEntities(owner, searchBox)) {
            if (e instanceof Monster hostile) {
                LivingEntity candidate = normalizeTarget(owner, hostile);
                if (candidate != null) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private static LivingEntity normalizeTarget(ServerPlayer owner, LivingEntity target) {
        if (target == null || !target.isAlive() || target.level() != owner.level()) {
            return null;
        }
        double maxSq = TARGET_RANGE * TARGET_RANGE;
        if (owner.distanceToSqr(target) > maxSq) {
            return null;
        }
        if (target instanceof ServerPlayer player) {
            if (player == owner || GemTrust.isTrusted(owner, player) || !VoidImmunity.canBeTargeted(owner, player)) {
                return null;
            }
        }
        if (target instanceof Mob mob) {
            UUID summonOwner = SummonerSummons.ownerUuid(mob);
            if (summonOwner != null && summonOwner.equals(owner.getUUID()) && SummonerSummons.isSummon(mob)) {
                return null;
            }
            UUID soulOwner = SoulSummons.ownerUuid(mob);
            if (soulOwner != null && soulOwner.equals(owner.getUUID()) && SoulSummons.isSoul(mob)) {
                return null;
            }
            UUID hypnoOwner = HypnoControl.ownerUuid(mob);
            if (hypnoOwner != null && hypnoOwner.equals(owner.getUUID()) && HypnoControl.isHypno(mob)) {
                return null;
            }
        }
        return target;
    }

    /**
     * Check if an entity is a spectral blade owned by a specific player.
     */
    public static boolean isOwnedBy(Vex vex, ServerPlayer player) {
        return vex.entityTags().contains("gems_spectral_blade")
                && vex.entityTags().contains("owner:" + player.getStringUUID());
    }
}
