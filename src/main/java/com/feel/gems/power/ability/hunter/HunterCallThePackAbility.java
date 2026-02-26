package com.feel.gems.power.ability.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.entity.HunterPackEntity;
import com.feel.gems.entity.ModEntities;
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

/**
 * Six-Pack Pain ability - summons 6 player clones that share a health pool.
 * Clones target untrusted players and hostile mobs.
 * When hit, owner gets buffs and attacker gets debuffs.
 */
public final class HunterCallThePackAbility implements GemAbility {
    
    @Override
    public Identifier id() {
        return PowerIds.HUNTER_CALL_THE_PACK;
    }
    
    @Override
    public String name() {
        return "Six-Pack Pain";
    }
    
    @Override
    public String description() {
        return "Summon 6 player clones that share a health pool. They attack untrusted players and hostile mobs. When hit, you get regeneration + a random buff, and the attacker gets debuffed.";
    }
    
    @Override
    public int cooldownTicks() {
        return GemsBalance.v().hunter().sixPackPainCooldownTicks();
    }
    
    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        var cfg = GemsBalance.v().hunter();
        
        int cloneCount = cfg.sixPackPainCloneCount();
        int durationTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.HUNTER, cfg.sixPackPainDurationTicks());
        
        // Generate a shared pack ID for health pooling
        UUID packId = UUID.randomUUID();
        
        // Spawn clones around the player
        for (int i = 0; i < cloneCount; i++) {
            Entity entity = ModEntities.HUNTER_PACK.create(world, EntitySpawnReason.MOB_SUMMONED);
            if (!(entity instanceof HunterPackEntity clone)) continue;
            
            // Calculate spawn position in a circle around the player
            double angle = (2 * Math.PI * i) / cloneCount;
            double radius = 2.0;
            double spawnX = player.getX() + Math.cos(angle) * radius;
            double spawnZ = player.getZ() + Math.sin(angle) * radius;
            
            clone.snapTo(spawnX, player.getY(), spawnZ, player.getYRot() + (360f / cloneCount) * i, 0);
            clone.setOwner(player, packId);
            clone.setMaxLifetime(durationTicks); // Set auto-despawn lifetime
            
            world.addFreshEntity(clone);
            
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
            
            // Spawn particles at clone location
            AbilityFeedback.burstAt(world, clone.position().add(0, 1, 0), ParticleTypes.SMOKE, 10, 0.5);
        }
        
        // Schedule despawn
        HunterCallThePackRuntime.schedulePackDespawn(player.getUUID(), packId, durationTicks);
        
        // Visual and sound feedback
        AbilityFeedback.burstAt(world, player.position().add(0, 1, 0), ParticleTypes.ANGRY_VILLAGER, 20, 1.0);
        AbilityFeedback.sound(player, SoundEvents.RAVAGER_ROAR, 1.2F, 1.5F);
        
        return true;
    }
}
