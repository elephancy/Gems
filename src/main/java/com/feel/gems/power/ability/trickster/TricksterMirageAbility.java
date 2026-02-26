package com.feel.gems.power.ability.trickster;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.entity.ModEntities;
import com.feel.gems.entity.ShadowCloneEntity;
import com.feel.gems.net.payloads.ShadowCloneSyncPayload;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Mirage - create a single illusory copy that mirrors your movements.
 * The clone despawns on hit or when the duration ends, granting you a brief buff.
 */
public final class TricksterMirageAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TRICKSTER_MIRAGE;
    }

    @Override
    public String name() {
        return "Mirage";
    }

    @Override
    public String description() {
        return "Create a mirage clone at your aim point that mirrors your movements and grants a buff when it breaks.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().trickster().mirageCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        var cfg = GemsBalance.v().trickster();
        int durationTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.TRICKSTER, cfg.mirageDurationTicks());
        int rangeBlocks = cfg.mirageRangeBlocks();
        int enabled = cfg.mirageCloneCount();
        if (durationTicks <= 0 || enabled <= 0) {
            return false;
        }

        Vec3 center = player.position();
        Vec3 direction = player.getViewVector(1.0F);
        Vec3 targetPos = center.add(direction.scale(rangeBlocks));
        HitResult hit = player.pick(rangeBlocks, 1.0F, false);
        if (hit.getType() != HitResult.Type.MISS) {
            targetPos = hit.getLocation();
        }
        long endTime = world.getGameTime() + durationTicks;

        Entity entity = ModEntities.SHADOW_CLONE.create(world, EntitySpawnReason.MOB_SUMMONED);
        if (!(entity instanceof ShadowCloneEntity clone)) {
            return false;
        }
        clone.snapTo(targetPos.x, targetPos.y, targetPos.z, player.getYRot(), player.getXRot());
        clone.setOwner(player);
        clone.setMaxLifetime(durationTicks);
        clone.setNoAi(true);
        clone.setNoGravity(true);
        clone.setMirageClone(true);
        world.addFreshEntity(clone);

        UUID cloneUuid = clone.getUUID();
        Vec3 offset = targetPos.subtract(center);
        TricksterMirageRuntime.createMirage(player, cloneUuid, offset, endTime);

        ShadowCloneSyncPayload syncPayload = new ShadowCloneSyncPayload(
                clone.getId(),
                player.getUUID(),
                player.getGameProfile().name()
        );
        for (ServerPlayer tracker : PlayerLookup.tracking(clone)) {
            ServerPlayNetworking.send(tracker, syncPayload);
        }
        ServerPlayNetworking.send(player, syncPayload);

        AbilityFeedback.burstAt(world, targetPos.add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 18, 0.5D);
        AbilityFeedback.burstAt(world, targetPos.add(0, 1.2, 0), ParticleTypes.ENCHANT, 10, 0.6D);
        AbilityFeedback.burstAt(world, targetPos.add(0, 0.8, 0), ParticleTypes.END_ROD, 8, 0.4D);
        AbilityFeedback.burstAt(world, center.add(0, 1.0, 0), ParticleTypes.PORTAL, 20, 0.7D);
        AbilityFeedback.sound(player, SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 1.0F);
        return true;
    }
}
