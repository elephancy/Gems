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
 * Reach Extend - Extended melee and block interaction range.
 * Implementation via attribute modifier.
 */
public final class BonusReachExtendPassive implements GemPassive {
    private static final Identifier MODIFIER_ID_INTERACT = Identifier.fromNamespaceAndPath("gems", "bonus_reach_extend_interact");
    private static final Identifier MODIFIER_ID_ATTACK = Identifier.fromNamespaceAndPath("gems", "bonus_reach_extend_attack");
    @Override
    public Identifier id() {
        return PowerIds.BONUS_REACH_EXTEND;
    }

    @Override
    public String name() {
        return "Reach Extend";
    }

    @Override
    public String description() {
        return "Extend melee and block interaction range by 1.5 blocks.";
    }

    @Override
    public void apply(ServerPlayer player) {
        double bonus = GemsBalance.v().bonusPool().reachExtendBlocks;
        AttributeInstance interact = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (interact != null && interact.getModifier(MODIFIER_ID_INTERACT) == null) {
            interact.addPermanentModifier(new AttributeModifier(
                    MODIFIER_ID_INTERACT, bonus, AttributeModifier.Operation.ADD_VALUE));
        }
        AttributeInstance blockInteract = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (blockInteract != null && blockInteract.getModifier(MODIFIER_ID_ATTACK) == null) {
            blockInteract.addPermanentModifier(new AttributeModifier(
                    MODIFIER_ID_ATTACK, bonus, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    @Override
    public void remove(ServerPlayer player) {
        AttributeInstance interact = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (interact != null) {
            interact.removeModifier(MODIFIER_ID_INTERACT);
        }
        AttributeInstance blockInteract = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (blockInteract != null) {
            blockInteract.removeModifier(MODIFIER_ID_ATTACK);
        }
    }
}
