package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Overflowing Vitality - +4 max hearts permanently.
 */
public final class BonusOverflowingVitalityPassive implements GemPassive {
    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath("gems", "bonus_overflowing_vitality");
    private static final double BONUS_HEALTH = 8.0; // 4 hearts = 8 HP

    @Override
    public Identifier id() {
        return PowerIds.BONUS_OVERFLOWING_VITALITY;
    }

    @Override
    public String name() {
        return "Overflowing Vitality";
    }

    @Override
    public String description() {
        return "Gain 4 additional hearts of max health.";
    }

    @Override
    public void apply(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null && attr.getModifier(MODIFIER_ID) == null) {
            attr.addPermanentModifier(new AttributeModifier(
                    MODIFIER_ID, BONUS_HEALTH, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    @Override
    public void remove(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.removeModifier(MODIFIER_ID);
        }
    }
}
