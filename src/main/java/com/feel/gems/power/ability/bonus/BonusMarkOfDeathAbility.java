package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.trust.GemTrust;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Mark of Death - mark a target to take bonus damage from all sources.
 */
public final class BonusMarkOfDeathAbility implements GemAbility {
    private static final Map<UUID, Long> MARKED = new HashMap<>();

    @Override
    public Identifier id() {
        return PowerIds.BONUS_MARK_OF_DEATH;
    }

    @Override
    public String name() {
        return "Mark of Death";
    }

    @Override
    public String description() {
        return "Mark a target to take bonus damage from all sources.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().markOfDeathCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int range = GemsBalance.v().bonusPool().markOfDeathRangeBlocks;
        LivingEntity target = Targeting.raycastLiving(player, range);
        if (target == null || target == player) {
            return false;
        }
        if (target instanceof ServerPlayer other) {
            if (GemTrust.isTrusted(player, other) || VoidImmunity.shouldBlockEffect(player, other)) {
                return false;
            }
        }
        int duration = GemsBalance.v().bonusPool().markOfDeathDurationSeconds * 20;
        long until = player.level().getGameTime() + Math.max(0, duration);
        MARKED.put(target.getUUID(), until);
        return true;
    }

    public static boolean isMarked(LivingEntity target) {
        if (target == null) {
            return false;
        }
        Long until = MARKED.get(target.getUUID());
        if (until == null) {
            return false;
        }
        if (target.level().getGameTime() > until) {
            MARKED.remove(target.getUUID());
            return false;
        }
        return true;
    }

    public static float getDamageMultiplier() {
        return 1.0f + (GemsBalance.v().bonusPool().markOfDeathBonusDamagePercent / 100.0f);
    }
}
