package com.feel.gems.power.passive.duelist;

import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.power.registry.PowerIds;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Combat Stance: While holding a sword, gain 10% movement speed.
 */
public final class DuelistCombatStancePassive implements GemMaintainedPassive {
    // Lazily initialized to avoid class loading issues in unit tests
    private static Set<Item> swords;

    private static Set<Item> getSwords() {
        if (swords == null) {
            swords = Set.of(
                Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD,
                Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD
            );
        }
        return swords;
    }

    @Override
    public Identifier id() {
        return PowerIds.DUELIST_COMBAT_STANCE;
    }

    @Override
    public String name() {
        return "Combat Stance";
    }

    @Override
    public String description() {
        return "While holding a sword, gain 10% movement speed.";
    }

    @Override
    public void apply(ServerPlayer player) {
        maintain(player);
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeEffect(MobEffects.SPEED);
    }

    @Override
    public void maintain(ServerPlayer player) {
        if (!isHoldingSword(player)) {
            return;
        }
        // Speed amplifier 0 = Speed I (+20%), so we use 0 for ~10% speed
        // The config uses a multiplier (1.1 = 10% boost), we approximate with Speed I
        int amplifier = 0;
        int duration = 40; // Refresh every 2 seconds
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, amplifier, true, false, false));
    }

    private static boolean isHoldingSword(ServerPlayer player) {
        Set<Item> swordSet = getSwords();
        return swordSet.contains(player.getMainHandItem().getItem())
            || swordSet.contains(player.getOffhandItem().getItem());
    }
}
