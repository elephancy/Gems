package com.feel.gems.power.gem.air;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;


public final class AirMacePassive implements GemMaintainedPassive {
    private static final String TAG_AIR_MACE = "gemsAirMace";
    /** Persistent flag: mace was ever granted to this player (never resets except by admin). */
    private static final String KEY_EVER_GRANTED = "airMaceEverGranted";

    private final Identifier id;
    private final String name;
    private final String description;

    public AirMacePassive(Identifier id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public void apply(ServerPlayer player) {
        maintain(player);
    }

    @Override
    public void maintain(ServerPlayer player) {
        // If player was ever granted a mace, never give another.
        if (isEverGranted(player)) {
            return;
        }
        ItemStack mace = createAirMace(player);
        if (mace.isEmpty()) {
            return;
        }
        player.addItem(mace);
        setEverGranted(player, true);
    }

    @Override
    public void remove(ServerPlayer player) {
        // Intentionally does not remove the mace or reset the flag.
        // One mace ever - flag persists forever.
    }

    public static boolean isHoldingMace(ServerPlayer player) {
        return player.getMainHandItem().is(Items.MACE);
    }

    private static ItemStack createAirMace(ServerPlayer player) {
        ItemStack mace = new ItemStack(Items.MACE);
        CustomData.update(DataComponents.CUSTOM_DATA, mace, nbt -> nbt.putBoolean(TAG_AIR_MACE, true));

        var cfg = GemsBalance.v().air();
        Holder<Enchantment> breach = resolve(player, Enchantments.BREACH);
        Holder<Enchantment> windBurst = resolve(player, Enchantments.WIND_BURST);
        Holder<Enchantment> mending = resolve(player, Enchantments.MENDING);
        Holder<Enchantment> unbreaking = resolve(player, Enchantments.UNBREAKING);
        Holder<Enchantment> fireAspect = resolve(player, Enchantments.FIRE_ASPECT);

        EnchantmentHelper.updateEnchantments(mace, builder -> {
            if (breach != null && cfg.airMaceBreachLevel() > 0) {
                builder.set(breach, cfg.airMaceBreachLevel());
            }
            if (windBurst != null && cfg.airMaceWindBurstLevel() > 0) {
                builder.set(windBurst, cfg.airMaceWindBurstLevel());
            }
            if (mending != null && cfg.airMaceMendingLevel() > 0) {
                builder.set(mending, cfg.airMaceMendingLevel());
            }
            if (unbreaking != null && cfg.airMaceUnbreakingLevel() > 0) {
                builder.set(unbreaking, cfg.airMaceUnbreakingLevel());
            }
            if (fireAspect != null && cfg.airMaceFireAspectLevel() > 0) {
                builder.set(fireAspect, cfg.airMaceFireAspectLevel());
            }
        });
        return mace;
    }

    private static boolean isEverGranted(ServerPlayer player) {
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return data.getBooleanOr(KEY_EVER_GRANTED, false);
    }

    private static void setEverGranted(ServerPlayer player, boolean granted) {
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.putBoolean(KEY_EVER_GRANTED, granted);
    }

    /**
     * Clears the "ever granted" flag, allowing the player to receive a new mace.
     * Only for admin commands.
     */
    public static void clearEverGranted(ServerPlayer player) {
        CompoundTag data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.remove(KEY_EVER_GRANTED);
    }

    private static Holder<Enchantment> resolve(ServerPlayer player, ResourceKey<Enchantment> key) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel world)) {
            return null;
        }
        var registry = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var entry = registry.get(key.identifier());
        return entry.orElse(null);
    }
}
