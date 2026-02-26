package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Storm Call - summon a lightning storm in an area with random strikes.
 */
public final class BonusStormCallAbility implements GemAbility {
    private static final List<Storm> STORMS = new ArrayList<>();

    @Override
    public Identifier id() {
        return PowerIds.BONUS_STORM_CALL;
    }

    @Override
    public String name() {
        return "Storm Call";
    }

    @Override
    public String description() {
        return "Summon a lightning storm in an area with random strikes.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().stormCallCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        var cfg = GemsBalance.v().bonusPool();
        Vec3 center = player.position();
        long until = world.getGameTime() + cfg.stormCallDurationSeconds * 20L;
        STORMS.add(new Storm(world, center, cfg.stormCallRadiusBlocks, cfg.stormCallDamagePerStrike,
                cfg.stormCallStrikesPerSecond, until, player));
        world.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.5, center.z, 20, 1.0, 0.3, 1.0, 0.02);
        return true;
    }

    public static void tick(ServerLevel world) {
        if (STORMS.isEmpty()) {
            return;
        }
        long now = world.getGameTime();
        Iterator<Storm> iter = STORMS.iterator();
        while (iter.hasNext()) {
            Storm storm = iter.next();
            if (storm.world != world) {
                continue;
            }
            if (now > storm.until) {
                iter.remove();
                continue;
            }
            if (now % 20 != 0) {
                continue;
            }
            int strikes = Math.max(1, storm.strikesPerSecond);
            for (int i = 0; i < strikes; i++) {
                double rx = (world.getRandom().nextDouble() * 2.0 - 1.0) * storm.radius;
                double rz = (world.getRandom().nextDouble() * 2.0 - 1.0) * storm.radius;
                Vec3 strike = storm.center.add(rx, 0.0, rz);
                LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
                if (lightning != null) {
                    lightning.snapTo(strike.x, strike.y, strike.z);
                    world.addFreshEntity(lightning);
                }
                AABB box = new AABB(strike, strike).inflate(2.0);
                List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive());
                for (LivingEntity entity : entities) {
                    if (entity instanceof ServerPlayer other) {
                        if (GemTrust.isTrusted(storm.owner, other) || VoidImmunity.shouldBlockEffect(storm.owner, other)) {
                            continue;
                        }
                    }
                    entity.hurtServer(world, storm.owner.damageSources().indirectMagic(storm.owner, storm.owner), storm.damagePerStrike);
                }
            }
        }
    }

    private record Storm(ServerLevel world, Vec3 center, int radius, float damagePerStrike, int strikesPerSecond,
                         long until, ServerPlayer owner) {
    }
}
