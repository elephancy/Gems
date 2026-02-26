package com.feel.gems.mixin;

import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.trust.GemTrust;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.AABB;

/**
 * Mixin to prevent spectral blade vexes from attacking their owner.
 * Targets MobEntity since that's where setTarget is defined.
 */
@Mixin(Mob.class)
public abstract class VexEntitySpectralBladeMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void gems$preventSpectralBladeOwnerTargeting(LivingEntity target, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        
        // Only handle vexes
        if (!(self instanceof Vex)) {
            return;
        }
        
        if (target == null) {
            return;
        }
        
        Set<String> tags = self.entityTags();
        
        // Check if this is a spectral blade
        if (!tags.contains("gems_spectral_blade")) {
            return;
        }
        
        // Find the owner tag
        String ownerUuid = null;
        for (String tag : tags) {
            if (tag.startsWith("owner:")) {
                ownerUuid = tag.substring(6);
                break;
            }
        }
        
        if (ownerUuid == null) {
            return;
        }
        
        // Prevent targeting the owner
        if (target instanceof ServerPlayer player) {
            if (player.getStringUUID().equals(ownerUuid)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void gems$retargetSpectralBlade(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (!(self instanceof Vex vex)) {
            return;
        }
        if (!(self.level() instanceof ServerLevel world)) {
            return;
        }
        Set<String> tags = self.entityTags();
        if (!tags.contains("gems_spectral_blade")) {
            return;
        }
        if (world.getGameTime() % 10 != 0) {
            return;
        }

        String ownerUuid = null;
        for (String tag : tags) {
            if (tag.startsWith("owner:")) {
                ownerUuid = tag.substring(6);
                break;
            }
        }
        if (ownerUuid == null) {
            return;
        }
        ServerPlayer owner = world.getServer().getPlayerList().getPlayer(java.util.UUID.fromString(ownerUuid));
        if (owner == null) {
            return;
        }

        LivingEntity currentTarget = vex.getTarget();
        if (currentTarget != null && currentTarget.isAlive()) {
            if (currentTarget != owner && !(currentTarget instanceof ServerPlayer player && GemTrust.isTrusted(owner, player))) {
                return;
            }
        }

        AABB searchBox = vex.getBoundingBox().inflate(16.0);
        LivingEntity selected = null;
        for (Entity entity : world.getEntities(owner, searchBox)) {
            if (entity instanceof ServerPlayer target) {
                if (!target.isAlive()) {
                    continue;
                }
                if (GemTrust.isTrusted(owner, target)) {
                    continue;
                }
                if (!VoidImmunity.canBeTargeted(owner, target)) {
                    continue;
                }
                selected = target;
                break;
            }
        }
        if (selected == null) {
            for (Entity entity : world.getEntities(owner, searchBox)) {
                if (entity instanceof Monster hostile && hostile.isAlive()) {
                    selected = hostile;
                    break;
                }
            }
        }
        if (selected != null) {
            vex.setTarget(selected);
        }
    }
}
