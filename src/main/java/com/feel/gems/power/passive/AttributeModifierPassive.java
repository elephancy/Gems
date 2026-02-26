package com.feel.gems.power.passive;

import com.feel.gems.power.api.GemPassive;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;




public final class AttributeModifierPassive implements GemPassive {
    private final Identifier id;
    private final String name;
    private final String description;
    private final Holder<Attribute> attribute;
    private final AttributeModifier.Operation operation;
    private final double value;

    public AttributeModifierPassive(
            Identifier id,
            String name,
            String description,
            Holder<Attribute> attribute,
            AttributeModifier.Operation operation,
            double value
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.attribute = attribute;
        this.operation = operation;
        this.value = value;
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
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        instance.addPermanentModifier(new AttributeModifier(id, value, operation));
    }

    @Override
    public void remove(ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
    }
}
