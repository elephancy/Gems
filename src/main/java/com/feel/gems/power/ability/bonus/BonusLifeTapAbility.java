package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.chaos.ChaosSlotRuntime;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.net.GemCooldownSync;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.core.GemId;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class BonusLifeTapAbility implements GemAbility {
    private static final String KEY_COOLDOWNS = "cooldowns";

    @Override
    public Identifier id() {
        return PowerIds.BONUS_LIFE_TAP;
    }

    @Override
    public String name() {
        return "Life Tap";
    }

    @Override
    public String description() {
        return "Sacrifice health to reduce your current cooldowns.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().lifeTapCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();

        float cost = Math.max(0.0F, GemsBalance.v().bonusPool().lifeTapHealthCost);
        if (player.getHealth() <= cost) {
            return false;
        }

        player.setHealth(Math.max(0.0F, player.getHealth() - cost));

        float reductionPercent = Math.max(0.0F, GemsBalance.v().bonusPool().lifeTapCooldownReductionPercent);
        if (reductionPercent > 0.0F) {
            if (reduceCooldowns(player, reductionPercent)) {
                GemCooldownSync.send(player);
                GemStateSync.sendBonusAbilitiesSync(player);
                if (GemPlayerState.getActiveGem(player) == GemId.PRISM) {
                    GemStateSync.sendPrismAbilitiesSync(player);
                }
            }
        }

        world.sendParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY() + 1, player.getZ(), 18, 0.5, 1, 0.5, 0.1);
        return true;
    }

    private static boolean reduceCooldowns(ServerPlayer player, float reductionPercent) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag cooldowns = root.getCompound(KEY_COOLDOWNS).orElse(null);
        float clamped = Math.min(100.0F, Math.max(0.0F, reductionPercent));
        if (clamped <= 0.0F) {
            return false;
        }

        long now = GemsTime.now(player);
        float factor = 1.0F - (clamped / 100.0F);
        String selfKey = PowerIds.BONUS_LIFE_TAP.toString();
        boolean changed = false;

        if (cooldowns != null && !cooldowns.keySet().isEmpty()) {
            for (String key : java.util.List.copyOf(cooldowns.keySet())) {
                if (selfKey.equals(key)) {
                    continue;
                }
                long nextAllowed = cooldowns.getLongOr(key, 0L);
                if (nextAllowed <= now) {
                    continue;
                }
                long remaining = nextAllowed - now;
                long reducedRemaining = (long) Math.ceil(remaining * factor);
                long updated = now + Math.max(0L, reducedRemaining);
                if (updated != nextAllowed) {
                    cooldowns.putLong(key, updated);
                    changed = true;
                }
            }
            if (changed) {
                root.put(KEY_COOLDOWNS, cooldowns);
            }
        }

        boolean chaosChanged = ChaosSlotRuntime.reduceCooldowns(player, factor);
        return changed || chaosChanged;
    }
}
