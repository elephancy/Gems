package com.feel.gems.power.gem.beacon;

import com.feel.gems.bonus.PrismSelectionsState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.trust.GemTrust;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;


public final class BeaconAuraRuntime {
    private static final String KEY_AURA_TYPE = "beaconAuraType";

    public enum AuraType {
        SPEED(PowerIds.BEACON_AURA_SPEED, "Speed", MobEffects.SPEED),
        HASTE(PowerIds.BEACON_AURA_HASTE, "Haste", MobEffects.HASTE),
        RESISTANCE(PowerIds.BEACON_AURA_RESISTANCE, "Resistance", MobEffects.RESISTANCE),
        JUMP(PowerIds.BEACON_AURA_JUMP, "Jump Boost", MobEffects.JUMP_BOOST),
        STRENGTH(PowerIds.BEACON_AURA_STRENGTH, "Strength", MobEffects.STRENGTH),
        REGEN(PowerIds.BEACON_AURA_REGEN, "Regeneration", MobEffects.REGENERATION);

        private final Identifier id;
        private final String label;
        private final Holder<MobEffect> effect;

        AuraType(Identifier id, String label, Holder<MobEffect> effect) {
            this.id = id;
            this.label = label;
            this.effect = effect;
        }

        public Identifier id() {
            return id;
        }

        public String label() {
            return label;
        }

        public Holder<MobEffect> effect() {
            return effect;
        }

        public static AuraType fromId(Identifier id) {
            for (AuraType type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return null;
        }
    }

    private BeaconAuraRuntime() {
    }

    public static void setActive(ServerPlayer player, AuraType type) {
        AuraType prev = activeType(player);
        if (type == null) {
            if (prev != null) {
                clearAuraEffects(player, prev);
            }
            clear(player);
            return;
        }
        if (prev != null && prev != type) {
            clearAuraEffects(player, prev);
        }
        CompoundTag nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        nbt.putString(KEY_AURA_TYPE, type.id().toString());
    }

    public static AuraType activeType(ServerPlayer player) {
        CompoundTag nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        Identifier id = Identifier.tryParse(nbt.getStringOr(KEY_AURA_TYPE, ""));
        return AuraType.fromId(id);
    }

    public static void tickEverySecond(ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);
        AuraType type = activeType(player);
        if (type == null) {
            clear(player);
            return;
        }

        if (GemPlayerState.getEnergy(player) <= 0) {
            clear(player);
            return;
        }
        GemId activeGem = GemPlayerState.getActiveGem(player);
        if (activeGem != GemId.BEACON) {
            if (activeGem != GemId.PRISM || !PrismSelectionsState.hasAbility(player, type.id())) {
                clear(player);
                return;
            }
        }

        int radius = GemsBalance.v().beacon().auraRadiusBlocks();
        int refresh = GemsBalance.v().beacon().auraRefreshTicks();
        if (radius <= 0 || refresh <= 0) {
            return;
        }
        int amplifier = amplifierFor(type);

        ServerLevel world = player.level();
        AABB box = new AABB(player.blockPosition()).inflate(radius);
        for (net.minecraft.world.entity.LivingEntity other : world.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box, e -> e.isAlive())) {
            if (other instanceof ServerPlayer otherPlayer && VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                other.removeEffect(type.effect());
                continue;
            }
            boolean trusted = other instanceof ServerPlayer otherPlayer && (GemTrust.isTrusted(player, otherPlayer) || otherPlayer == player);
            if (trusted) {
                other.addEffect(new MobEffectInstance(type.effect(), refresh, amplifier, true, false, false));
            } else {
                // Ensure enemies do not retain the positive aura from prior ticks or sources.
                other.removeEffect(type.effect());
                other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, refresh, 0, true, false, false));
                other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, refresh, 0, true, false, false));
            }
        }
    }

    private static int amplifierFor(AuraType type) {
        var cfg = GemsBalance.v().beacon();
        return switch (type) {
            case SPEED -> cfg.auraSpeedAmplifier();
            case HASTE -> cfg.auraHasteAmplifier();
            case RESISTANCE -> cfg.auraResistanceAmplifier();
            case JUMP -> cfg.auraJumpAmplifier();
            case STRENGTH -> cfg.auraStrengthAmplifier();
            case REGEN -> cfg.auraRegenAmplifier();
        };
    }

    private static void clear(ServerPlayer player) {
        CompoundTag nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        nbt.remove(KEY_AURA_TYPE);
    }

    private static void clearAuraEffects(ServerPlayer player, AuraType type) {
        if (type == null) {
            return;
        }
        int radius = GemsBalance.v().beacon().auraRadiusBlocks();
        ServerLevel world = player.level();
        AABB box = new AABB(player.blockPosition()).inflate(radius);
        for (net.minecraft.world.entity.LivingEntity other : world.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box, e -> e.isAlive())) {
            other.removeEffect(type.effect());
        }
    }
}
