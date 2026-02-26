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
 * Attack Speed - Increased melee attack speed.
 */
public final class BonusAttackSpeedPassive implements GemPassive {
    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath("gems", "bonus_attack_speed");
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ATTACK_SPEED;
    }

    @Override
    public String name() {
        return "Attack Speed";
    }

    @Override
    public String description() {
        return "Gain 15% melee attack speed.";
    }

    @Override
    public void apply(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr != null && attr.getModifier(MODIFIER_ID) == null) {
            double bonus = GemsBalance.v().bonusPool().attackSpeedBoostPercent / 100.0f;
            attr.addPermanentModifier(new AttributeModifier(
                    MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    @Override
    public void remove(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr != null) {
            attr.removeModifier(MODIFIER_ID);
        }
    }
}
