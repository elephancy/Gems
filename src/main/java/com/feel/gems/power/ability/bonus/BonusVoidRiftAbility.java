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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Void Rift - rift that damages enemies who touch it.
 */
public final class BonusVoidRiftAbility implements GemAbility {
    private static final List<Rift> RIFTS = new ArrayList<>();

    @Override
    public Identifier id() {
        return PowerIds.BONUS_VOID_RIFT;
    }

    @Override
    public String name() {
        return "Void Rift";
    }

    @Override
    public String description() {
        return "Tear open a rift that damages enemies who touch it.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().voidRiftCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        var cfg = GemsBalance.v().bonusPool();
        Vec3 pos = player.position();
        long until = world.getGameTime() + cfg.voidRiftDurationSeconds * 20L;
        RIFTS.add(new Rift(world, pos, cfg.voidRiftRadiusBlocks, cfg.voidRiftDamagePerSecond, until, player));
        world.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + 0.5, pos.z, 40, 0.6, 0.4, 0.6, 0.05);
        return true;
    }

    public static void tick(ServerLevel world) {
        if (RIFTS.isEmpty()) {
            return;
        }
        long now = world.getGameTime();
        Iterator<Rift> iter = RIFTS.iterator();
        while (iter.hasNext()) {
            Rift rift = iter.next();
            if (rift.world != world) {
                continue;
            }
            if (now > rift.until) {
                iter.remove();
                continue;
            }
            if (now % 20 != 0) {
                continue;
            }
            AABB box = new AABB(rift.pos, rift.pos).inflate(rift.radius);
            List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive());
            for (LivingEntity entity : entities) {
                if (entity instanceof ServerPlayer other) {
                    if (GemTrust.isTrusted(rift.owner, other) || VoidImmunity.shouldBlockEffect(rift.owner, other)) {
                        continue;
                    }
                }
                entity.hurtServer(world, rift.owner.damageSources().indirectMagic(rift.owner, rift.owner), rift.damagePerSecond);
            }
            world.sendParticles(ParticleTypes.REVERSE_PORTAL, rift.pos.x, rift.pos.y + 0.2, rift.pos.z, 10, 0.4, 0.2, 0.4, 0.02);
        }
    }

    private record Rift(ServerLevel world, Vec3 pos, int radius, float damagePerSecond, long until, ServerPlayer owner) {
    }
}
