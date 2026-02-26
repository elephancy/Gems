package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.entity.HunterPackEntity;
import com.feel.gems.entity.ModEntities;
import com.feel.gems.net.payloads.ShadowCloneSyncPayload;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Phantasm - create a decoy that taunts enemies and explodes on death.
 */
public final class BonusPhantasmAbility implements GemAbility {
    private static final Map<UUID, Phantasm> PHANTASMS = new HashMap<>();

    @Override
    public Identifier id() {
        return PowerIds.BONUS_PHANTASM;
    }

    @Override
    public String name() {
        return "Phantasm";
    }

    @Override
    public String description() {
        return "Create a decoy that taunts enemies and explodes on death.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().phantasmCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        Entity entity = ModEntities.HUNTER_PACK.create(world, EntitySpawnReason.MOB_SUMMONED);
        if (!(entity instanceof HunterPackEntity clone)) {
            return false;
        }
        UUID packId = UUID.randomUUID();
        clone.setOwner(player, packId);
        int duration = GemsBalance.v().bonusPool().phantasmDurationSeconds * 20;
        clone.setMaxLifetime(duration);
        clone.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        world.addFreshEntity(clone);

        AABB box = clone.getBoundingBox().inflate(12);
        for (Mob mob : world.getEntitiesOfClass(Mob.class, box, m -> m.isAlive())) {
            mob.setTarget(clone);
        }
        PHANTASMS.put(clone.getUUID(), new Phantasm(world, clone.getUUID(), player.getUUID(), world.getGameTime() + duration, clone.position()));
        world.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.5, 0.4, 0.5, 0.02);

        ShadowCloneSyncPayload syncPayload = new ShadowCloneSyncPayload(
                clone.getId(),
                player.getUUID(),
                player.getGameProfile().name()
        );
        for (ServerPlayer tracker : PlayerLookup.tracking(clone)) {
            ServerPlayNetworking.send(tracker, syncPayload);
        }
        ServerPlayNetworking.send(player, syncPayload);
        return true;
    }

    public static void tick(ServerLevel world) {
        if (PHANTASMS.isEmpty()) {
            return;
        }
        long now = world.getGameTime();
        Iterator<Map.Entry<UUID, Phantasm>> iter = PHANTASMS.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            Phantasm phantom = entry.getValue();
            if (phantom.world != world) {
                continue;
            }
            Entity clone = world.getEntity(phantom.cloneUuid);
            if (clone != null) {
                phantom.lastPos = clone.position();
            }
            boolean expired = now > phantom.until;
            boolean dead = clone == null || !clone.isAlive();
            if (!expired && !dead) {
                continue;
            }
            explode(phantom, clone);
            iter.remove();
        }
    }

    private static void explode(Phantasm phantom, Entity clone) {
        ServerLevel world = phantom.world;
        Vec3 pos = clone != null ? clone.position() : phantom.lastPos;
        if (pos == null) {
            return;
        }
        float power = GemsBalance.v().bonusPool().phantasmExplosionDamage;
        world.explode(clone, pos.x, pos.y, pos.z, power, Level.ExplosionInteraction.MOB);
        if (clone != null) {
            clone.discard();
        }
        clearTargets(world, phantom.cloneUuid, pos);
    }

    private static void clearTargets(ServerLevel world, UUID cloneUuid, Vec3 center) {
        AABB box = new AABB(center, center).inflate(32.0D);
        for (Mob mob : world.getEntitiesOfClass(Mob.class, box, m -> m.getTarget() != null)) {
            Entity target = mob.getTarget();
            if (target != null && cloneUuid.equals(target.getUUID())) {
                mob.setTarget(null);
            }
        }
    }

    private static final class Phantasm {
        final ServerLevel world;
        final UUID cloneUuid;
        final UUID ownerUuid;
        final long until;
        Vec3 lastPos;

        private Phantasm(ServerLevel world, UUID cloneUuid, UUID ownerUuid, long until, Vec3 lastPos) {
            this.world = world;
            this.cloneUuid = cloneUuid;
            this.ownerUuid = ownerUuid;
            this.until = until;
            this.lastPos = lastPos;
        }
    }
}
