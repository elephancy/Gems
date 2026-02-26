package com.feel.gems.mixin;

import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.feel.gems.config.GemsBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(LivingEntity.class)
public abstract class LivingEntityRichRushLootMixin {
    @Unique
    private static final ThreadLocal<Boolean> gems$inDoubleLoot = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Shadow
    protected abstract void dropFromLootTable(ServerLevel world, DamageSource damageSource, boolean causedByPlayer);

    @Inject(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V", at = @At("TAIL"))
    private void gems$richRushDoubleLoot(ServerLevel world, DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
        if (!causedByPlayer) {
            return;
        }
        if (Boolean.TRUE.equals(gems$inDoubleLoot.get())) {
            return;
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!AbilityRuntime.isRichRushActive(player)) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        int rolls = GemsBalance.v().wealth().richRushLootRolls();
        if (rolls < 1) {
            rolls = 1;
        }
        try {
            gems$inDoubleLoot.set(Boolean.TRUE);
            for (int i = 1; i < rolls; i++) {
                dropFromLootTable(world, source, true);
            }
        } finally {
            gems$inDoubleLoot.set(Boolean.FALSE);
        }
        if (self instanceof WitherSkeleton) {
            int count = GemsBalance.v().wealth().richRushWitherSkullGuarantee();
            if (count > 0) {
                self.spawnAtLocation(world, new ItemStack(Items.WITHER_SKELETON_SKULL, count));
            }
        } else if (self instanceof Shulker) {
            int count = GemsBalance.v().wealth().richRushShulkerShellGuarantee();
            if (count > 0) {
                self.spawnAtLocation(world, new ItemStack(Items.SHULKER_SHELL, count));
            }
        } else if (self instanceof WitherBoss) {
            int count = GemsBalance.v().wealth().richRushNetherStarGuarantee();
            int extra = Math.max(0, count - 1);
            if (extra > 0) {
                self.spawnAtLocation(world, new ItemStack(Items.NETHER_STAR, extra));
            }
        }
    }
}

