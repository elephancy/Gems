package com.feel.gems.power.ability.reaper;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.entity.ModEntities;
import com.feel.gems.entity.ShadowCloneEntity;
import com.feel.gems.net.payloads.ShadowCloneSyncPayload;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;



public final class ReaperShadowCloneAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.REAPER_SHADOW_CLONE;
    }

    @Override
    public String name() {
        return "Shadow Clone";
    }

    @Override
    public String description() {
        return "Summons invincible decoys around you that vanish after a short time.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().reaper().shadowCloneCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        var cfg = GemsBalance.v().reaper();
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.REAPER, cfg.shadowCloneDurationTicks());
        int count = cfg.shadowCloneCount();
        if (duration <= 0 || count <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.reaper.shadow_clone.disabled"), true);
            return false;
        }

        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        
        List<java.util.UUID> spawned = new ArrayList<>();
        float yaw = player.getYRot();
        double baseRadius = 1.5D;
        
        for (int i = 0; i < count; i++) {
            Entity entity = ModEntities.SHADOW_CLONE.create(world, EntitySpawnReason.MOB_SUMMONED);
            if (!(entity instanceof ShadowCloneEntity clone)) {
                continue;
            }
            
            double angle = (Math.PI * 2.0D) * (i / (double) count);
            double offset = baseRadius + (world.getRandom().nextDouble() * 0.6D);
            double dx = Math.cos(angle) * offset;
            double dz = Math.sin(angle) * offset;
            Vec3 pos = new Vec3(player.getX() + dx, player.getY(), player.getZ() + dz);

            clone.snapTo(pos.x, pos.y, pos.z, yaw + (world.getRandom().nextFloat() - 0.5F) * 30F, 0.0F);
            clone.setOwner(player);
            clone.setMaxLifetime(duration); // Set auto-despawn lifetime

            world.addFreshEntity(clone);
            spawned.add(clone.getUUID());
            
            // Send sync payload to all nearby players for skin rendering
            ShadowCloneSyncPayload syncPayload = new ShadowCloneSyncPayload(
                    clone.getId(),
                    player.getUUID(),
                    player.getGameProfile().name()
            );
            for (ServerPlayer tracker : PlayerLookup.tracking(clone)) {
                ServerPlayNetworking.send(tracker, syncPayload);
            }
            // Also send to the caster
            ServerPlayNetworking.send(player, syncPayload);
        }

        if (spawned.isEmpty()) {
            player.sendSystemMessage(Component.translatable("gems.ability.reaper.shadow_clone.failed"), true);
            return false;
        }

        AbilityRuntime.startReaperShadowClone(player, spawned, duration);
        AbilityFeedback.sound(player, SoundEvents.ENDERMAN_TELEPORT, 0.6F, 0.6F);
        player.sendSystemMessage(Component.translatable("gems.ability.reaper.shadow_clone.spawned"), true);
        return true;
    }
}
