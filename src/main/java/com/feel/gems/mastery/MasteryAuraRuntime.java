package com.feel.gems.mastery;

import com.feel.gems.core.GemId;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles mastery aura particle effects.
 * Particles are spawned around players with selected auras.
 */
public final class MasteryAuraRuntime {
    private static final double AURA_RADIUS = 0.6;
    private static final int PARTICLES_PER_TICK = 2;

    private MasteryAuraRuntime() {
    }

    /**
     * Called every second to spawn aura particles for players with selected auras.
     */
    public static void tick(ServerPlayer player) {
        MasteryReward aura = GemMastery.getSelectedAuraReward(player);
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        double posX = player.getX();
        double posY = player.getY() + 1.0;
        double posZ = player.getZ();

        ParticleOptions particle;
        if (aura != null) {
            particle = getParticleForAura(aura);
        } else {
            particle = getParticleForTitle(player);
        }
        if (particle == null) {
            return;
        }

        // Spawn particles in a circle around the player
        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            double angle = world.getRandom().nextDouble() * Math.PI * 2;
            double offsetX = Math.cos(angle) * AURA_RADIUS;
            double offsetZ = Math.sin(angle) * AURA_RADIUS;
            double offsetY = (world.getRandom().nextDouble() - 0.5) * 1.5;

            world.sendParticles(
                    particle,
                    posX + offsetX,
                    posY + offsetY,
                    posZ + offsetZ,
                    1,
                    0.02, 0.02, 0.02,
                    0.0
            );
        }
    }

    private static ParticleOptions getParticleForTitle(ServerPlayer player) {
        ChatFormatting color = TitleDisplay.titleColor(player);
        Integer rgb = color == null ? null : net.minecraft.network.chat.TextColor.fromLegacyFormat(color).getValue();
        if (rgb == null) {
            return null;
        }
        return new DustParticleOptions(rgb, 1.0F);
    }

    private static ParticleOptions getParticleForAura(MasteryReward aura) {
        String id = aura.id();

        // Extract gem from aura id (e.g., "astra_aura_spark")
        GemId gem = extractGem(id);

        if (gem == null) {
            return ParticleTypes.END_ROD;
        }

        // Use different particle types based on gem theme
        return getGemParticle(gem);
    }

    private static ParticleOptions getGemParticle(GemId gem) {
        return switch (gem) {
            case FIRE -> ParticleTypes.FLAME;
            case LIFE -> ParticleTypes.HAPPY_VILLAGER;
            case VOID -> ParticleTypes.PORTAL;
            case FLUX -> ParticleTypes.ELECTRIC_SPARK;
            case SPACE -> ParticleTypes.REVERSE_PORTAL;
            case REAPER -> ParticleTypes.SOUL;
            case CHAOS -> ParticleTypes.ENCHANT;
            case PRISM -> ParticleTypes.END_ROD;
            case TERROR -> ParticleTypes.SMOKE;
            case ASTRA, BEACON, WEALTH -> ParticleTypes.END_ROD;
            case SPY, TRICKSTER -> ParticleTypes.WITCH;
            case SUMMONER, PILLAGER -> ParticleTypes.CRIT;
            default -> ParticleTypes.ENCHANTED_HIT;
        };
    }

    private static GemId extractGem(String auraId) {
        for (GemId gem : GemId.values()) {
            String prefix = gem.name().toLowerCase() + "_aura_";
            if (auraId.startsWith(prefix)) {
                return gem;
            }
        }
        return null;
    }
}
