package com.feel.gems.power.ability.duelist;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

public final class DuelistParryAbility implements GemAbility {
    private static final String PARRY_KEY = "duelist_parry_window";

    @Override
    public Identifier id() {
        return PowerIds.DUELIST_PARRY;
    }

    @Override
    public String name() {
        return "Parry";
    }

    @Override
    public String description() {
        return "Brief window to deflect incoming melee attacks; successful parry stuns the attacker.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().duelist().parryCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int windowTicks = GemsBalance.v().duelist().parryWindowTicks();
        PlayerStateManager.setTemporary(player, PARRY_KEY, windowTicks);

        AbilityFeedback.burstAt(player.level(), player.position().add(0.0D, 1.0D, 0.0D),
                ParticleTypes.ENCHANT, 20, 0.5D);
        AbilityFeedback.sound(player, SoundEvents.SHIELD_BLOCK, 1.0F, 1.5F);
        return true;
    }

    public static boolean isParrying(ServerPlayer player) {
        return PlayerStateManager.getTemporary(player, PARRY_KEY) > 0;
    }

    public static void consumeParry(ServerPlayer player) {
        PlayerStateManager.clearTemporary(player, PARRY_KEY);
    }
}
