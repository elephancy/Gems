package com.feel.gems.power.ability.reaper;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.trust.GemTrust;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;



public final class ReaperDeathOathAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.REAPER_DEATH_OATH;
    }

    @Override
    public String name() {
        return "Death Oath";
    }

    @Override
    public String description() {
        return "Bind yourself to a target; you lose health over time but deal bonus damage to them.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().reaper().deathOathCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int range = GemsBalance.v().reaper().deathOathRangeBlocks();
        LivingEntity target = Targeting.raycastLiving(player, range);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("gems.message.no_target"), true);
            return false;
        }
        if (target instanceof ServerPlayer other && GemTrust.isTrusted(player, other)) {
            player.sendSystemMessage(Component.translatable("gems.message.target_trusted"), true);
            return false;
        }
        if (target instanceof ServerPlayer other && !VoidImmunity.canBeTargeted(player, other)) {
            player.sendSystemMessage(Component.translatable("gems.message.target_immune"), true);
            return false;
        }
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.REAPER, GemsBalance.v().reaper().deathOathDurationTicks());
        if (duration <= 0) {
            player.sendSystemMessage(Component.translatable("gems.ability.reaper.death_oath.disabled"), true);
            return false;
        }
        AbilityRuntime.startReaperDeathOath(player, target.getUUID(), duration);
        player.sendSystemMessage(Component.translatable("gems.ability.reaper.death_oath.bound", target.getName().getString()), true);
        return true;
    }
}
