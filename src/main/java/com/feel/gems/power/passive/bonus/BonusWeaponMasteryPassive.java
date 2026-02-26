package com.feel.gems.power.passive.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Weapon Mastery - Deal increased damage with all weapons.
 * Implementation via attribute modifier.
 */
public final class BonusWeaponMasteryPassive implements GemPassive {
    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath("gems", "bonus_weapon_mastery");
    @Override
    public Identifier id() {
        return PowerIds.BONUS_WEAPON_MASTERY;
    }

    @Override
    public String name() {
        return "Weapon Mastery";
    }

    @Override
    public String description() {
        return "Gain +1 attack damage with all weapons.";
    }

    @Override
    public void apply(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null && attr.getModifier(MODIFIER_ID) == null) {
            attr.addPermanentModifier(new AttributeModifier(
                    MODIFIER_ID,
                    GemsBalance.v().bonusPool().weaponMasteryBonusDamage,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    @Override
    public void remove(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) {
            attr.removeModifier(MODIFIER_ID);
        }
    }
}
