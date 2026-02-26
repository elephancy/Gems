    package com.feel.gems.state;

    import com.feel.gems.GemsMod;
    import com.feel.gems.GemsModEvents;
    import com.feel.gems.config.GemsDisables;
    import com.feel.gems.config.GemsBalance;
    import com.feel.gems.assassin.AssassinState;
    import com.feel.gems.core.GemId;
    import com.feel.gems.net.GemStateSync;
    import java.util.EnumSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;




    public final class GemPlayerState {
        private static final String KEY_ACTIVE_GEM = "activeGem";
        private static final String KEY_ENERGY = "energy";
        private static final String KEY_ENERGY_CAP_PENALTY = "energyCapPenalty";
        private static final String KEY_MAX_HEARTS = "maxHearts";
        private static final String KEY_OWNED_GEMS = "ownedGems";
        private static final String KEY_GEM_EPOCH = "gemEpoch";
        private static final String KEY_PASSIVES_ENABLED = "passivesEnabled";

        public static final int DEFAULT_ENERGY = 3;
        public static final int MIN_ENERGY = 0;
        public static final int MAX_ENERGY = 10;

        public static final int DEFAULT_MAX_HEARTS = 10;
        public static final int MIN_MAX_HEARTS = 5;
        public static final int MAX_MAX_HEARTS = 20;

        private static final Identifier HEARTS_MODIFIER_ID = Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "max_hearts");

        private GemPlayerState() {
        }

        public static int minMaxHearts() {
            return clamp(GemsBalance.v().systems().minMaxHearts(), 1, MAX_MAX_HEARTS);
        }

        public static void copy(ServerPlayer from, ServerPlayer to) {
            CompoundTag fromData = root(from);
            ((GemsPersistentDataHolder) to).gems$setPersistentData(fromData.copy());
        }

        public static void initIfNeeded(ServerPlayer player) {
            CompoundTag data = root(player);

            if (data.getInt(KEY_ENERGY).isEmpty()) {
                data.putInt(KEY_ENERGY, DEFAULT_ENERGY);
            }
            if (data.getInt(KEY_ENERGY_CAP_PENALTY).isEmpty()) {
                data.putInt(KEY_ENERGY_CAP_PENALTY, 0);
            }
            if (data.getInt(KEY_MAX_HEARTS).isEmpty()) {
                data.putInt(KEY_MAX_HEARTS, Math.max(DEFAULT_MAX_HEARTS, minMaxHearts()));
            }
        if (data.getInt(KEY_GEM_EPOCH).isEmpty()) {
            data.putInt(KEY_GEM_EPOCH, 0);
        }
        if (player instanceof ServerPlayer sp) {
            com.feel.gems.item.GemOwnership.recordOwnerEpoch(sp.level().getServer(), sp.getUUID(), data.getIntOr(KEY_GEM_EPOCH, 0));
        }
            if (data.getBoolean(KEY_PASSIVES_ENABLED).isEmpty()) {
                data.putBoolean(KEY_PASSIVES_ENABLED, true);
            }
            if (data.getString(KEY_ACTIVE_GEM).isEmpty()) {
                GemId assigned = randomGem(player);
                data.putString(KEY_ACTIVE_GEM, assigned.name());
                setOwnedGems(player, EnumSet.of(assigned));
            } else if (data.getList(KEY_OWNED_GEMS).isEmpty()) {
                GemId current = getActiveGem(player);
                setOwnedGems(player, EnumSet.of(current));
            }
        }

        public static GemId getActiveGem(Player player) {
            CompoundTag data = root(player);
            String raw = data.getStringOr(KEY_ACTIVE_GEM, "");
            if (raw.isEmpty()) {
                return GemId.ASTRA;
            }
            try {
                return GemId.valueOf(raw);
            } catch (IllegalArgumentException e) {
                GemsMod.LOGGER.warn("Unknown gem id '{}' in player data; defaulting to ASTRA", raw);
                return GemId.ASTRA;
            }
        }

        public static void setActiveGem(Player player, GemId gem) {
            CompoundTag data = root(player);
            GemId prev = getActiveGem(player);
            if (prev != gem && player instanceof ServerPlayer sp) {
                com.feel.gems.augment.AugmentRuntime.clearActiveGemAugments(sp);
                if (prev == GemId.SPY && gem != GemId.SPY) {
                    com.feel.gems.power.gem.spy.SpySystem.clearOnGemSwitchAway(sp);
                }
            }
            data.putString(KEY_ACTIVE_GEM, gem.name());
            addOwnedGem(player, gem);
            if (player instanceof ServerPlayer sp) {
                com.feel.gems.mastery.TitleDisplay.refresh(sp);
                var server = sp.level().getServer();
                if (server != null) {
                    GemsModEvents.unlockStartingRecipes(server, sp);
                }
            }
        }

        public static void resetToNew(ServerPlayer player, GemId gem) {
            CompoundTag data = root(player);
            data.putInt(KEY_ENERGY, DEFAULT_ENERGY);
            data.putInt(KEY_MAX_HEARTS, Math.max(DEFAULT_MAX_HEARTS, minMaxHearts()));
            data.putString(KEY_ACTIVE_GEM, gem.name());
            setOwnedGems(player, EnumSet.of(gem));
        }

        public static EnumSet<GemId> getOwnedGems(Player player) {
            CompoundTag data = root(player);
            if (data.getList(KEY_OWNED_GEMS).isEmpty()) {
                return EnumSet.of(getActiveGem(player));
            }
            ListTag list = data.getListOrEmpty(KEY_OWNED_GEMS);
            EnumSet<GemId> result = EnumSet.noneOf(GemId.class);
            for (int i = 0; i < list.size(); i++) {
                String raw = list.getStringOr(i, "");
                try {
                    result.add(GemId.valueOf(raw));
                } catch (IllegalArgumentException ignored) {
                    // Ignore unknown ids for forward-compatibility.
                }
            }
            if (result.isEmpty()) {
                result.add(getActiveGem(player));
            }
            return result;
        }

        public static boolean addOwnedGem(Player player, GemId gem) {
            EnumSet<GemId> owned = getOwnedGems(player);
            boolean changed = owned.add(gem);
            if (changed) {
                setOwnedGems(player, owned);
            }
            return changed;
        }

        public static int getEnergy(Player player) {
            CompoundTag data = root(player);
            return clamp(data.getIntOr(KEY_ENERGY, DEFAULT_ENERGY), MIN_ENERGY, getMaxEnergy(player));
        }

        public static int getGemEpoch(Player player) {
            CompoundTag data = root(player);
            if (data.getInt(KEY_GEM_EPOCH).isEmpty()) {
                data.putInt(KEY_GEM_EPOCH, 0);
                return 0;
            }
            return data.getIntOr(KEY_GEM_EPOCH, 0);
        }

    public static int bumpGemEpoch(Player player) {
        CompoundTag data = root(player);
        int next = getGemEpoch(player) + 1;
        data.putInt(KEY_GEM_EPOCH, next);
        if (player instanceof ServerPlayer sp) {
            com.feel.gems.item.GemOwnership.recordOwnerEpoch(sp.level().getServer(), sp.getUUID(), next);
        }
        return next;
    }

        public static int setEnergy(Player player, int energy) {
            int prev = getEnergy(player);
            int clamped = clamp(energy, MIN_ENERGY, getMaxEnergy(player));
            root(player).putInt(KEY_ENERGY, clamped);
            if (player instanceof ServerPlayer sp) {
                com.feel.gems.power.runtime.GemPowers.sync(sp);
                if (GemsBalance.v().loadouts().enabled()) {
                    int unlock = GemsBalance.v().loadouts().unlockEnergy();
                    boolean crossed = (prev >= unlock && clamped < unlock)
                            || (prev < unlock && clamped >= unlock);
                    if (crossed) {
                        GemStateSync.send(sp);
                    }
                }
            }
            return clamped;
        }

        public static int addEnergy(Player player, int delta) {
            return setEnergy(player, getEnergy(player) + delta);
        }

        public static boolean arePassivesEnabled(Player player) {
            CompoundTag data = root(player);
            return data.getBoolean(KEY_PASSIVES_ENABLED).orElseGet(() -> {
                data.putBoolean(KEY_PASSIVES_ENABLED, true);
                return true;
            });
        }

        public static void setPassivesEnabled(ServerPlayer player, boolean enabled) {
            root(player).putBoolean(KEY_PASSIVES_ENABLED, enabled);
            com.feel.gems.power.runtime.GemPowers.sync(player);
        }

        public static int getEnergyCapPenalty(Player player) {
            CompoundTag data = root(player);
            return clamp(data.getIntOr(KEY_ENERGY_CAP_PENALTY, 0), 0, MAX_ENERGY);
        }

        public static int getMaxEnergy(Player player) {
            return Math.max(MIN_ENERGY, MAX_ENERGY - getEnergyCapPenalty(player));
        }

        /**
         * Applies a permanent reduction to the player's maximum energy cap and clamps current energy down if needed.
         *
         * <p>This is used by certain abilities as a permanent "life" cost.</p>
         */
        public static int addEnergyCapPenalty(Player player, int delta) {
            CompoundTag data = root(player);
            int next = clamp(getEnergyCapPenalty(player) + delta, 0, MAX_ENERGY);
            data.putInt(KEY_ENERGY_CAP_PENALTY, next);
            // Ensure current energy respects the new cap.
            setEnergy(player, getEnergy(player));
            return next;
        }

        public static int setEnergyCapPenalty(Player player, int penalty) {
            CompoundTag data = root(player);
            int clamped = clamp(penalty, 0, MAX_ENERGY);
            data.putInt(KEY_ENERGY_CAP_PENALTY, clamped);
            setEnergy(player, getEnergy(player));
            return clamped;
        }

        public static int getMaxHearts(Player player) {
            CompoundTag data = root(player);
            return clamp(data.getIntOr(KEY_MAX_HEARTS, Math.max(DEFAULT_MAX_HEARTS, minMaxHearts())), minMaxHearts(), MAX_MAX_HEARTS);
        }

        public static int setMaxHearts(Player player, int hearts) {
            int clamped = clamp(hearts, minMaxHearts(), MAX_MAX_HEARTS);
            root(player).putInt(KEY_MAX_HEARTS, clamped);
            return clamped;
        }

        public static int addMaxHearts(Player player, int delta) {
            return setMaxHearts(player, getMaxHearts(player) + delta);
        }

        public static void applyMaxHearts(ServerPlayer player) {
            AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth == null) {
                return;
            }
            maxHealth.removeModifier(HEARTS_MODIFIER_ID);

            int hearts = AssassinState.isAssassin(player)
                    ? AssassinState.getAssassinHeartsForAttribute(player)
                    : getMaxHearts(player);
            double bonusHealth = (hearts - DEFAULT_MAX_HEARTS) * 2.0D;
            if (bonusHealth != 0.0D) {
                maxHealth.addPermanentModifier(new AttributeModifier(
                        HEARTS_MODIFIER_ID,
                        bonusHealth,
                        AttributeModifier.Operation.ADD_VALUE
                ));
            }

            float newMax = (float) maxHealth.getValue();
            if (player.getHealth() > newMax) {
                player.setHealth(newMax);
            }
        }

        private static void setOwnedGems(Player player, EnumSet<GemId> owned) {
            ListTag list = new ListTag();
            for (GemId gem : owned) {
                list.add(StringTag.valueOf(gem.name()));
            }
            root(player).put(KEY_OWNED_GEMS, list);
        }

        public static void setOwnedGemsExact(Player player, EnumSet<GemId> owned) {
            if (owned == null || owned.isEmpty()) {
                owned = EnumSet.of(getActiveGem(player));
            }
            setOwnedGems(player, owned);
        }

        private static GemId randomGem(Player player) {
            GemId[] values = GemId.values();
            java.util.ArrayList<GemId> allowed = new java.util.ArrayList<>(values.length);
            for (GemId gem : values) {
                // Special gems shouldn't be randomly assigned on first join.
                if (gem == GemId.VOID || gem == GemId.CHAOS || gem == GemId.PRISM) {
                    continue;
                }
                if (!GemsDisables.isGemDisabled(gem)) {
                    allowed.add(gem);
                }
            }
            if (allowed.isEmpty()) {
                return GemId.ASTRA;
            }
            return allowed.get(player.getRandom().nextInt(allowed.size()));
        }

        private static CompoundTag root(Player player) {
            return ((GemsPersistentDataHolder) player).gems$getPersistentData();
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }
