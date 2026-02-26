package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityTreasureHunterLootMixin {
    @Unique
    private static final ThreadLocal<Boolean> gems$treasureLoot = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Shadow
    protected abstract void dropFromLootTable(ServerLevel world, DamageSource damageSource, boolean causedByPlayer);

    @Inject(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V", at = @At("TAIL"))
    private void gems$treasureHunterLoot(ServerLevel world, DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
        if (!causedByPlayer) {
            return;
        }
        if (Boolean.TRUE.equals(gems$treasureLoot.get())) {
            return;
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_TREASURE_HUNTER)) {
            return;
        }
        float chance = GemsBalance.v().bonusPool().treasureHunterDropBoostPercent / 100.0f;
        if (chance <= 0.0f || player.getRandom().nextFloat() > chance) {
            return;
        }
        try {
            gems$treasureLoot.set(Boolean.TRUE);
            dropFromLootTable(world, source, true);
        } finally {
            gems$treasureLoot.set(Boolean.FALSE);
        }
    }
}
