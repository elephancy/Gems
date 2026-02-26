package com.feel.gems.power.ability.life;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.stats.GemsStats;
import com.feel.gems.state.PlayerStateManager;
import com.feel.gems.util.GemsTickScheduler;
import com.feel.gems.util.GemsTime;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.trust.GemTrust;



public final class LifeSwapAbility implements GemAbility {
    private static final String LIFE_SWAP_TOKEN_KEY = "lifeSwapToken";
    private static final String LIFE_SWAP_PARTNER_KEY = "lifeSwapPartner";
    private static final String LIFE_SWAP_ORIGINAL_HEALTH_KEY = "lifeSwapOriginalHealth";
    @Override
    public Identifier id() {
        return PowerIds.LIFE_SWAP;
    }

    @Override
    public String name() {
        return "Life Swap";
    }

    @Override
    public String description() {
        return "Swap health with a target you can see.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().life().lifeSwapCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        float minHearts = GemsBalance.v().life().lifeSwapMinHearts();
        if (player.getHealth() < minHearts * 2.0F) {
            player.sendSystemMessage(Component.translatable("gems.ability.life.life_swap.min_hearts", minHearts), true);
            return false;
        }

        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().life().lifeSwapRangeBlocks());
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

        float playerHealth = player.getHealth();
        float targetHealth = target.getHealth();
        player.setHealth(Math.min(player.getMaxHealth(), targetHealth));
        target.setHealth(Math.min(target.getMaxHealth(), playerHealth));

        if (target instanceof ServerPlayer other) {
            int reswapTicks = GemsBalance.v().life().lifeSwapReswapTicks();
            if (reswapTicks > 0) {
                var server = player.level().getServer();
                if (server != null) {
                    int playerDeaths = GemsStats.deaths(player);
                    int otherDeaths = GemsStats.deaths(other);
                    String token = player.getStringUUID() + ":" + other.getStringUUID() + ":" + GemsTime.now(player);
                    PlayerStateManager.setPersistent(player, LIFE_SWAP_TOKEN_KEY, token);
                    PlayerStateManager.setPersistent(other, LIFE_SWAP_TOKEN_KEY, token);
                    PlayerStateManager.setPersistent(player, LIFE_SWAP_PARTNER_KEY, other.getStringUUID());
                    PlayerStateManager.setPersistent(other, LIFE_SWAP_PARTNER_KEY, player.getStringUUID());
                    PlayerStateManager.setPersistent(player, LIFE_SWAP_ORIGINAL_HEALTH_KEY, Float.toString(playerHealth));
                    PlayerStateManager.setPersistent(other, LIFE_SWAP_ORIGINAL_HEALTH_KEY, Float.toString(targetHealth));
                    java.util.UUID playerId = player.getUUID();
                    java.util.UUID otherId = other.getUUID();

                    GemsTickScheduler.schedule(server, reswapTicks, srv -> {
                        ServerPlayer p = srv.getPlayerList().getPlayer(playerId);
                        ServerPlayer t = srv.getPlayerList().getPlayer(otherId);
                        if (p == null || t == null) {
                            return;
                        }
                        if (!token.equals(PlayerStateManager.getPersistent(p, LIFE_SWAP_TOKEN_KEY))) {
                            return;
                        }
                        if (!token.equals(PlayerStateManager.getPersistent(t, LIFE_SWAP_TOKEN_KEY))) {
                            return;
                        }
                        if (!otherId.toString().equals(PlayerStateManager.getPersistent(p, LIFE_SWAP_PARTNER_KEY))) {
                            return;
                        }
                        if (!playerId.toString().equals(PlayerStateManager.getPersistent(t, LIFE_SWAP_PARTNER_KEY))) {
                            return;
                        }
                        if (GemsStats.deaths(p) != playerDeaths || GemsStats.deaths(t) != otherDeaths) {
                            return;
                        }

                        float pOriginal = parseStoredHealth(PlayerStateManager.getPersistent(p, LIFE_SWAP_ORIGINAL_HEALTH_KEY));
                        float tOriginal = parseStoredHealth(PlayerStateManager.getPersistent(t, LIFE_SWAP_ORIGINAL_HEALTH_KEY));
                        if (!Float.isNaN(pOriginal) && !Float.isNaN(tOriginal)) {
                            p.setHealth(Math.min(p.getMaxHealth(), pOriginal));
                            t.setHealth(Math.min(t.getMaxHealth(), tOriginal));
                        }

                        PlayerStateManager.clearPersistent(p, LIFE_SWAP_TOKEN_KEY);
                        PlayerStateManager.clearPersistent(p, LIFE_SWAP_PARTNER_KEY);
                        PlayerStateManager.clearPersistent(p, LIFE_SWAP_ORIGINAL_HEALTH_KEY);
                        PlayerStateManager.clearPersistent(t, LIFE_SWAP_TOKEN_KEY);
                        PlayerStateManager.clearPersistent(t, LIFE_SWAP_PARTNER_KEY);
                        PlayerStateManager.clearPersistent(t, LIFE_SWAP_ORIGINAL_HEALTH_KEY);
                    });
                }
            }
        }

        AbilityFeedback.sound(player, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 0.7F, 1.4F);
        AbilityFeedback.burst(player, ParticleTypes.HEART, 10, 0.25D);
        AbilityFeedback.burstAt(player.level(), target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D), ParticleTypes.HEART, 10, 0.25D);
        player.sendSystemMessage(Component.translatable("gems.ability.life.life_swap.activated"), true);
        return true;
    }

    private static float parseStoredHealth(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Float.NaN;
        }
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }
}
