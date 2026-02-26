package com.feel.gems.power.ability.life;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;



public final class VitalityVortexAbility implements GemAbility {
    private enum VortexMode {
        DEFAULT("Default"),
        AQUATIC("Aquatic"),
        INFERNAL("Infernal"),
        SCULK("Sculk"),
        VERDANT("Verdant"),
        END("End");

        private final String label;

        VortexMode(String label) {
            this.label = label;
        }
    }

    @Override
    public Identifier id() {
        return PowerIds.VITALITY_VORTEX;
    }

    @Override
    public String name() {
        return "Vitality Vortex";
    }

    @Override
    public String description() {
        return "Area pulse that buffs trusted players and weakens enemies based on your surroundings.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().life().vitalityVortexCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        VortexMode mode = detectMode(world, player);
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.LIFE, GemsBalance.v().life().vitalityVortexDurationTicks());
        int radius = GemsBalance.v().life().vitalityVortexRadiusBlocks();
        AABB box = new AABB(player.blockPosition()).inflate(radius);
        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            boolean ally = other instanceof ServerPlayer otherPlayer && GemTrust.isTrusted(player, otherPlayer);
            if (ally) {
                applyAlly(mode, other, duration);
            } else {
                if (other instanceof ServerPlayer otherPlayer && !VoidImmunity.canBeTargeted(player, otherPlayer)) {
                    continue;
                }
                applyEnemy(mode, other, duration);
            }
        }

        AbilityFeedback.sound(player, SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.1F);
        AbilityFeedback.burst(player, particleFor(mode), 20, 0.35D);
        player.sendSystemMessage(Component.translatable("gems.ability.life.vitality_vortex.mode", mode.label), true);
        return true;
    }

    private static net.minecraft.core.particles.ParticleOptions particleFor(VortexMode mode) {
        return switch (mode) {
            case AQUATIC -> ParticleTypes.BUBBLE;
            case INFERNAL -> ParticleTypes.FLAME;
            case SCULK -> ParticleTypes.SCULK_SOUL;
            case VERDANT -> ParticleTypes.HAPPY_VILLAGER;
            case END -> ParticleTypes.PORTAL;
            default -> ParticleTypes.HEART;
        };
    }

    private static void applyAlly(VortexMode mode, LivingEntity target, int duration) {
        float heal = GemsBalance.v().life().vitalityVortexAllyHeal();
        switch (mode) {
            case AQUATIC -> {
                target.heal(heal);
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, duration, 0, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, duration, 0, true, false, false));
            }
            case INFERNAL -> {
                target.heal(heal);
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, 0, true, false, false));
            }
            case SCULK -> {
                target.heal(heal);
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 0, true, false, false));
            }
            case VERDANT -> {
                target.heal(heal);
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 0, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0, true, false, false));
            }
            case END -> {
                target.heal(heal);
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 0, true, false, false));
            }
            default -> {
                target.heal(heal);
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 0, true, false, false));
            }
        }
    }

    private static void applyEnemy(VortexMode mode, LivingEntity target, int duration) {
        switch (mode) {
            case AQUATIC -> {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 1, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, duration, 0, true, false, false));
            }
            case INFERNAL -> {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, 0, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, true, false, false));
            }
            case SCULK -> {
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, duration, 0, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, duration, 1, true, false, false));
            }
            case VERDANT -> {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 1, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 0, true, false, false));
            }
            case END -> {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 1, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, true, false, false));
            }
            default -> {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 0, true, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, true, false, false));
            }
        }
    }

    private static VortexMode detectMode(ServerLevel world, ServerPlayer player) {
        if (world.dimension() == Level.NETHER) {
            return VortexMode.INFERNAL;
        }
        if (world.dimension() == Level.END) {
            return VortexMode.END;
        }

        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int water = 0;
        int lava = 0;
        int sculk = 0;
        int verdant = 0;

        int r = GemsBalance.v().life().vitalityVortexScanRadiusBlocks();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = world.getBlockState(pos);
                    var fluid = state.getFluidState();
                    if (fluid.is(FluidTags.LAVA)) {
                        lava++;
                    } else if (fluid.is(FluidTags.WATER)) {
                        water++;
                    }

                    if (isSculk(state)) {
                        sculk++;
                    } else if (isVerdant(state)) {
                        verdant++;
                    }
                }
            }
        }

        if (lava > 0) {
            return VortexMode.INFERNAL;
        }
        if (water > 0 || player.isUnderWater()) {
            return VortexMode.AQUATIC;
        }
        if (sculk > 0) {
            return VortexMode.SCULK;
        }
        if (verdant >= GemsBalance.v().life().vitalityVortexVerdantThreshold()) {
            return VortexMode.VERDANT;
        }
        return VortexMode.DEFAULT;
    }

    private static boolean isSculk(BlockState state) {
        return state.is(Blocks.SCULK)
                || state.is(Blocks.SCULK_VEIN)
                || state.is(Blocks.SCULK_SENSOR)
                || state.is(Blocks.CALIBRATED_SCULK_SENSOR)
                || state.is(Blocks.SCULK_SHRIEKER)
                || state.is(Blocks.SCULK_CATALYST);
    }

    private static boolean isVerdant(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.FLOWERS)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.VINE);
    }
}
